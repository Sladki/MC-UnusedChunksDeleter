package sladki.chunkdeleter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Report {
	
	private static File report = null;
	private static BufferedWriter writer = null;
	
	public static void createReportFile() {
		Report.report = new File("report.txt");
		try {
			Report.report.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			Report.writer = new BufferedWriter(new FileWriter(Report.report));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static synchronized void addToReport(String info) {
		try {
			Report.writer.write(info + "\n");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void writeReport() {
		try {
			Report.writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
