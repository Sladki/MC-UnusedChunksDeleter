package sladki.chunkdeleter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

import net.minecraft.world.level.chunk.storage.RegionFile;

import com.mojang.nbt.CompoundTag;
import com.mojang.nbt.NbtIo;

class RegionCleaner implements Runnable {

	public void run() {
		
		File region;
		while(UnusedChunksDeleter.areRegionsLeft()) {
			region = UnusedChunksDeleter.getRegionFile();
			if(region != null) {
				cleanRegion(region);
			}
		}
		UnusedChunksDeleter.threadFinished();
	}
	
	private void cleanRegion(File regionFile) {
		int chunksCount = 0;
        int chunksInhabited = 0;
        
		try {
            String name = regionFile.getName();
            String regionXZ = name.replace(".", " ").substring(2, name.length()-4);
            
            RegionFile regionSource = new RegionFile(regionFile);
            RegionFile regionClean = new RegionFile(new File(UnusedChunksDeleter.regionCleanFolder, name));

            for (int x = 0; x < 32; x++) {
                for (int z = 0; z < 32; z++) {
                    if (regionSource.hasChunk(x, z)) {
                    	chunksCount++;
                        DataInputStream regionChunkInputStream = regionSource.getChunkDataInputStream(x, z);
                        if (regionChunkInputStream == null) {
                            System.out.println("Failed to fetch input stream");
                            continue;
                        }
                        CompoundTag chunkData = NbtIo.read(regionChunkInputStream);
                        regionChunkInputStream.close();

                        CompoundTag compound = chunkData.getCompound("Level");
                        {
                            long time = compound.getLong("InhabitedTime");

                            Report.addToReport(regionXZ + " " + x + " " + z + " " + time);
                            if(time >= UnusedChunksDeleter.minimalTime) {
                            	chunksInhabited++;
                            	DataOutputStream chunkDataOutputStream = regionClean.getChunkDataOutputStream(x, z);
                            	NbtIo.write(chunkData, chunkDataOutputStream);
                            	chunkDataOutputStream.close();
                            }
                        }
                    }
                }
            }

            regionSource.close();
            regionClean.close();
        } catch (IOException e) {
            e.printStackTrace();
        }	
	UnusedChunksDeleter.regionStatSend(chunksCount, chunksInhabited);
	
	}
}