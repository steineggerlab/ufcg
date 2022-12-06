package envs.toolkit;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.List;
import java.util.ArrayList;
//import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;

import envs.config.PathConfig;

public class FileStream {
	private static class Counter {
		int cnt = 1;
		boolean handled = false;
		void incr() {cnt++;}
		void decr() {cnt--;}
		void handle() {handled = true;}
	}
	
	// store and wipe out TEMPORARY WRITTEN file streams
	private static List<String>  TMP_PATHS;
	private static List<Counter> TMP_STATUS;
	private static Map<String, Integer> PATH_MAP;
	private static int PATH_MAP_ITER = 0;
	
	public static void init() {
		TMP_PATHS     = new ArrayList<>();
		TMP_STATUS    = new ArrayList<>();
		PATH_MAP 	  = new HashMap<>();
		PATH_MAP_ITER = 0;
	}
	
	public static void isTemp(String path) {
		try {
			if(TMP_PATHS.contains(path)) {
				if(PATH_MAP.get(path) >= TMP_STATUS.size()) return;
				TMP_STATUS.get(PATH_MAP.get(path)).incr();
			}
			else {
				TMP_PATHS.add(path);
				TMP_STATUS.add(new Counter());
				PATH_MAP.put(path, PATH_MAP_ITER++);
			}
		}
		catch(NullPointerException | IndexOutOfBoundsException ignored) {}
	}
	public static void isTemp(FileStream stream) {
		isTemp(stream.PATH);
	}

	public static void wipe(String path) {
		if(PathConfig.TempIsCustom) return;
		try {
			if(!PATH_MAP.containsKey(path)) return;
			if(PATH_MAP.get(path) >= TMP_STATUS.size()) return;
			TMP_STATUS.get(PATH_MAP.get(path)).decr();
			if(TMP_STATUS.get(PATH_MAP.get(path)).cnt == 0) {
				// Shell.exec("rm " + path);
				File file = new File(path);
				if(file.delete()) TMP_STATUS.get(PATH_MAP.get(path)).handle();
				else Prompt.debug("Failed to delete file: " + path);
			}
		}
		catch(NullPointerException | IndexOutOfBoundsException ignored) {}
	}
	public static void wipe(FileStream stream) {
		wipe(stream.PATH);
	}
	public static void wipe(String path, boolean force) {
		if(force) {
			File file = new File(path);
			if(file.delete()){
				if(PathConfig.TempIsCustom) TMP_STATUS.get(PATH_MAP.get(path)).handle();
			}
			else Prompt.debug("Failed to delete file: " + path);
		}
		else wipe(path);
	}
	public static void wipe(FileStream stream, boolean force) {
		wipe(stream.PATH, force);
	}
	
	public static void wipeOut() {
/*		while(!ACTIVE_STREAM.isEmpty()) {
			try{
				ACTIVE_STREAM.get(0).close();
			}
			catch(IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
		}*/
		if(PathConfig.TempIsCustom) return;
		for(String path : TMP_PATHS) {
			try {
				if(path == null) continue;
				if(!PATH_MAP.containsKey(path)) continue;
				if(PATH_MAP.get(path) >= TMP_STATUS.size()) continue;
				if(!TMP_STATUS.get(PATH_MAP.get(path)).handled) wipe(path);
			} catch(NullPointerException | IndexOutOfBoundsException ignored) {}
		} 
		init();
	}

	/*
	public static String filesToWipe() {
		String res = "";
		try {
			if(PathConfig.TempIsCustom) return res;
			if(TMP_PATHS == null) return res;
			for(String path : TMP_PATHS) {
				if(PATH_MAP.get(path) >= TMP_STATUS.size()) continue;
				if(!TMP_STATUS.get((int) PATH_MAP.get(path)).handled) res += path + " ";
			}
			return res;
		} catch(NullPointerException | IndexOutOfBoundsException e) {return res;}
	}
	*/
	
//	public static List<FileStream> ACTIVE_STREAM = new LinkedList<FileStream>();
	public BufferedReader	reader = null;
	public PrintWriter 		writer = null;
	public final String 	PATH;
	
	public FileStream(String path, char type) throws IOException {
		this.PATH = path;
		switch(type){
			case 'i': case 'r':
				reader = new BufferedReader(new FileReader(path)); break;
			case 'o': case 'w':
				writer = new PrintWriter(path); break;
			case 'a':
				writer = new PrintWriter(new FileWriter(path, true)); break;
			default :
				System.err.println("Unsupported file stream type : " + type);
		}
//		ACTIVE_STREAM.add(this);
	}

	public void close() throws IOException {
		if(reader != null) reader.close();
		if(writer != null) writer.close();
//		ACTIVE_STREAM.remove(this);
	}
	
	public String readLine() throws IOException {
		if(reader == null) return null;
		return reader.readLine();
	}
	
	public void println(Object o) throws IOException {
		if(writer == null) return;
		writer.println(o);
	}
	public void print(Object o) throws IOException {
		if(writer == null) return;
		writer.print(o);
	}
	
	public void isTemp() {isTemp(this);}
	public void wipe() {wipe(this);}
	public void wipe(boolean force) {wipe(this, force);}
}
