package sladki.chunkdeleter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;

import net.minecraft.world.level.chunk.storage.RegionFile;

import com.mojang.nbt.CompoundTag;
import com.mojang.nbt.NbtIo;

//Get list of region files
//Read each region file (in separated threads) an read each chunk's "Level" tag
//If "InhabitedTime" of chunk is not lesser than minimal time - copy chunk information in a new "clean" region file

public class UnusedChunksDeleter {
	
	static String regionFolder = "world/region";
	static String regionCleanFolder = "world/region/clean";
	
	static long minimalTime = 0;
	
	private static int chunksCount = 0;
	private static int chunksInhabited = 0;
	private static int currentRegion = 0;
	private static int threadsCount = 1;
	private static int threadsFinished = 0;
	
	private static ArrayList<File> regionFiles = null;
	private static Thread[] threads = null;
	
	public static void main(String[] args) {
		
		if(args.length < 1) {
            printUsageAndExit();
        }
		
		if(args.length == 2) {
			UnusedChunksDeleter.threadsCount = Integer.parseInt(args[1]);
		}

		UnusedChunksDeleter.minimalTime = Long.parseLong(args[0]);
		
		Report.createReportFile();
		Report.addToReport("T " + UnusedChunksDeleter.minimalTime);
		
        cleanRegions();
        
        Report.addToReport("Chunks total: " + UnusedChunksDeleter.chunksCount + "\nChunks deleted: " +
        		(UnusedChunksDeleter.chunksCount-UnusedChunksDeleter.chunksInhabited) +
        		"\nRegions count: " + UnusedChunksDeleter.currentRegion);
        Report.writeReport();
		
        System.out.println("Done!\nTotal chunks: " + UnusedChunksDeleter.chunksCount + "\nChunks deleted: " +
        		(UnusedChunksDeleter.chunksCount-UnusedChunksDeleter.chunksInhabited) +
        		"\nClean region files are in \"world/region/clean\" directory");
    }

    private static void cleanRegions() {
    	UnusedChunksDeleter.regionFiles = new ArrayList<File>();
        System.out.println("Looking for region files");
        
        if(!addRegionFiles(regionFiles)) {
        	return;
        }
        
        int regionFilesCount = regionFiles.size();
        if(regionFilesCount == 0) {
        	System.out.println("Region files doesn't exist. Is the world empty?");
        	return;
        }
        System.out.println("Total count of region files: " + regionFilesCount);
        
        File regionCleanDir = new File(UnusedChunksDeleter.regionCleanFolder);
        if (!regionCleanDir.exists()) {
            boolean result = false;
            try{
            	regionCleanDir.mkdir();
            	result = true;
            } catch(SecurityException se) {
            	System.err.println(se.getMessage());
            	return;
            }        
        }
        
        UnusedChunksDeleter.threads = new Thread[UnusedChunksDeleter.threadsCount];
        for(int thread = 0; thread < UnusedChunksDeleter.threadsCount; thread++) {
        	try {
        		UnusedChunksDeleter.threads[thread] = new Thread(new RegionCleaner());
        		UnusedChunksDeleter.threads[thread].start();
        	} catch (Exception e) {
        		System.err.println("Threads: " + e.getMessage());
        	}
        }
        
        long time = System.currentTimeMillis();
        int regionsCleaned = 1;
        while(UnusedChunksDeleter.threadsFinished != UnusedChunksDeleter.threadsCount) {
			if(regionsCleaned < UnusedChunksDeleter.currentRegion) {
				System.out.println("Regions cleaned: " + regionsCleaned + "/" + regionFilesCount);
				regionsCleaned++;
			}
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
        System.out.println("Regions cleaned: " + regionFilesCount + "/" + regionFilesCount);
        System.out.println("Time: " + (System.currentTimeMillis()-time));
        Report.addToReport("Time: " + (System.currentTimeMillis()-time));
    }
    
    private static boolean addRegionFiles(ArrayList<File> regions) {
    	
    	File regionFolder = null;
    	try {
        	regionFolder = new File(UnusedChunksDeleter.regionFolder);
	        if(!regionFolder.exists()) {
	        	throw new RuntimeException("'world/region' directory doesn't exist");
	        } else if(!regionFolder.isDirectory()) {
	        	throw new RuntimeException("'world/region' is not a directory");
	        }
        } catch (Exception e) {
            System.err.println("Region directory problem: " + e.getMessage());
            System.out.println("");
            printUsageAndExit();
            return false;
        }
    	
    	File[] list = regionFolder.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".mca");
            }
        });
    	
    	if(list != null) {
            for(File file : list) {
                regions.add(file);
            }
        }
    	return true;
    	
    }
    
    private static void printUsageAndExit() {
        System.out.println("Unused chunks deleter for Minecraft worlds. Put in your server or save directory.");
        System.out.println("");
        System.out.println("Usage:");
        System.out.println("\tjava -jar UnusedChunksDeleter.jar <minimal time> <threads>");
        System.out.println("Where:");
        System.out.println("\t<minimal time>\tTime (in ticks, 1 sec is 20 ticks). Chunks with \"Inhabited time\" tag less then minimal time will be deleted");
        System.out.println("\t<threads>\tNumber of threads (optional)");
        System.out.println("Example:");
        System.out.println("\tjava -jar UnusedChunksDeleter.jar 20000 2");
        System.exit(1);
    }
    
    public static synchronized boolean areRegionsLeft() {
    	if(UnusedChunksDeleter.currentRegion < UnusedChunksDeleter.regionFiles.size()) {
    		return true;
    	}
    	return false;
    }
    
    public static synchronized File getRegionFile() {
    	if(UnusedChunksDeleter.currentRegion < UnusedChunksDeleter.regionFiles.size()) {
    		int region = UnusedChunksDeleter.currentRegion;
    		UnusedChunksDeleter.currentRegion++;
    		return UnusedChunksDeleter.regionFiles.get(region);
    	} else {
    		return null;
    	}
    }
    
    public static synchronized void regionStatSend(int chunks, int inhabitedChunks) {
    	UnusedChunksDeleter.chunksCount = UnusedChunksDeleter.chunksCount + chunks;
    	UnusedChunksDeleter.chunksInhabited = UnusedChunksDeleter.chunksInhabited + inhabitedChunks;
    }
    
    public static synchronized void threadFinished() {
    	UnusedChunksDeleter.threadsFinished++;
    }
    
}