package envs.config;

import envs.toolkit.ANSIHandler;
import envs.toolkit.FileStream;
import envs.toolkit.Prompt;
import envs.toolkit.Shell;
import pipeline.ExceptionHandler;

public class PathConfig {
	/* Environment */
	public static String EnvironmentPath = "";
	public static void setEnvironmentPath(String path) {
		EnvironmentPath = path;
		renewProfilePath();
		renewAugustusConfig();
	}
	
	/* Dependent binaries */
	public static String FastBlockSearchPath = "fastBlockSearch";
	public static int setFastBlockSearchPath(String path) {
		try {
			Prompt.talk("fastBlockSearch binary check : " + ANSIHandler.wrapper(path, 'B'));
			String[] exec = Shell.exec("file -b " + path);
			if(!exec[0].contains("exec") && !exec[0].contains("link")) {
				ExceptionHandler.pass(path);
				ExceptionHandler.handle(ExceptionHandler.INVALID_BINARY);
				return 1;
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			ExceptionHandler.handle(ExceptionHandler.EXCEPTION);
			return 2;
		}
		
		FastBlockSearchPath = path;
		return 0;
	}
	
	public static String AugustusPath = "augustus";
	public static int setAugustusPath(String path) {
		try {
			Prompt.talk("AUGUSTUS binary check : " + ANSIHandler.wrapper(path, 'B'));
			String[] exec = Shell.exec("file -b " + path);
			if(!exec[0].contains("exec") && !exec[0].contains("link")) {
				ExceptionHandler.pass(path);
				ExceptionHandler.handle(ExceptionHandler.INVALID_BINARY);
				return 1;
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			ExceptionHandler.handle(ExceptionHandler.EXCEPTION);
			return 2;
		}
		
		AugustusPath = path;
		return 0;
	}
	
	public static String HmmsearchPath = "hmmsearch";
	public static int setHmmsearchPath(String path) {
		try {
			Prompt.talk("hmmsearch binary check : " + ANSIHandler.wrapper(path, 'B'));
			String[] exec = Shell.exec("file -b " + path);
			if(!exec[0].contains("exec") && !exec[0].contains("link")) {
				ExceptionHandler.pass(path);
				ExceptionHandler.handle(ExceptionHandler.INVALID_BINARY);
				return 1;
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			ExceptionHandler.handle(ExceptionHandler.EXCEPTION);
			return 2;
		}
		
		HmmsearchPath = path;
		return 0;
	}
	
	/* Runtime I/O configuration */
	public static String  InputPath = null;
	public static boolean InputIsFolder = false;
	public static int setInputPath(String path) {
		InputIsFolder = false;
		try {
			Prompt.talk("Input file check : " + ANSIHandler.wrapper(path, 'B'));
			String[] exec = Shell.exec("file -b " + path);
			if(exec[0].contains("directory")) InputIsFolder = true;
			Prompt.talk("Input argument : " + ANSIHandler.wrapper(exec[0], 'B'));
			
			if(exec[0].startsWith("cannot")) {
				ExceptionHandler.pass(path);
				ExceptionHandler.handle(ExceptionHandler.INVALID_FILE);
				return 1;
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			ExceptionHandler.handle(ExceptionHandler.EXCEPTION);
			return 2;
		}
		
		InputPath = path;
		if(InputIsFolder) {
			if(!InputPath.endsWith("/")) InputPath += "/";
		}	
		return 0;
	}
	
	public static String OutputPath = null;
	public static int setOutputPath(String path) {		
		try {
			Prompt.talk("Output directory check : " + ANSIHandler.wrapper(path, 'B'));
			String[] exec = Shell.exec("file -b " + path);
			if(!exec[0].contains("directory")) {
				ExceptionHandler.pass(path);
				ExceptionHandler.handle(ExceptionHandler.INVALID_DIRECTORY);
				return 1;
			}
			else if(exec[0].contains("cannot")) {
				ExceptionHandler.pass(path);
				ExceptionHandler.handle(ExceptionHandler.INVALID_DIRECTORY);
				return 1;
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			ExceptionHandler.handle(ExceptionHandler.EXCEPTION);
			return 2;
		}
		
		OutputPath = path;
		if(!OutputPath.endsWith("/")) OutputPath += "/";
		return 0;
	}
	// check output path contains any ucg file
	public static int checkOutputPath() {
		try {
			Prompt.talk("Output directory luggage check");
			String[] exec = Shell.exec("ls -1 " + OutputPath);
			for(String buf : exec) {
				if(buf.contains(".ucg")) return 1;
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			ExceptionHandler.handle(ExceptionHandler.EXCEPTION);
			return 2;
		}
		
		Prompt.talk("Bag is clean. Good to go.");
		return 0;
	}
	
	public static String  TempPath = "/tmp/";
	public static boolean TempIsCustom = false;
	public static int setTempPath(String path) {		
		try {
			Prompt.talk("Temporary directory check : " + ANSIHandler.wrapper(path, 'B'));
			String[] exec = Shell.exec("file -b " + path);
			if(!exec[0].contains("directory")) {
				ExceptionHandler.pass(path);
				ExceptionHandler.handle(ExceptionHandler.INVALID_DIRECTORY);
				return 1;
			}
			else if(exec[0].contains("cannot")) {
				ExceptionHandler.pass(path);
				ExceptionHandler.handle(ExceptionHandler.INVALID_DIRECTORY);
				return 1;
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			ExceptionHandler.handle(ExceptionHandler.EXCEPTION);
			return 2;
		}
		
		TempPath = path;
		TempIsCustom = true;
		if(!TempPath.endsWith("/")) TempPath += "/";
		GenericConfig.TEMP_HEADER = "";				
		
		return 0;
	}
	
	/* Custom configuration files */
	public static String  MetaPath = null;
	public static boolean MetaExists = false;
	public static String  MetaString = null;
	public static int setMetaPath(String path) {		
		try {
			Prompt.talk("Metadata file check : " + ANSIHandler.wrapper(path, 'B'));
			String[] exec = Shell.exec("file -b " + path);
			if(!exec[0].contains("text")) {
				ExceptionHandler.pass(path);
				ExceptionHandler.handle(ExceptionHandler.INVALID_FILE);
				return 1;
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			ExceptionHandler.handle(ExceptionHandler.EXCEPTION);
			return 2;
		}
		
		MetaPath = path;
		MetaExists = true;
		return 0;
	}
	
	public static String ProfilePath = EnvironmentPath + "config/prfl/";
	private static void renewProfilePath() {ProfilePath = EnvironmentPath + "config/prfl/";}
	public static int setProfilePath(String path) {		
		try {
			Prompt.talk("Core gene profile directory check : " + ANSIHandler.wrapper(path, 'B'));
			String[] exec = Shell.exec("file -b " + path);
			if(!exec[0].contains("directory")) {
				ExceptionHandler.pass(path);
				ExceptionHandler.handle(ExceptionHandler.INVALID_DIRECTORY);
				return 1;
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			ExceptionHandler.handle(ExceptionHandler.EXCEPTION);
			return 2;
		}
		
		ProfilePath = path;
		if(!ProfilePath.endsWith("/")) ProfilePath += "/";
		return 0;
	}
	public static boolean checkProfilePath() {
		int[] cnt = new int[GenericConfig.FCG.length];
		for(int i = 0; i < cnt.length; i++) cnt[i] = -2;
		
		String[] cmd = {"/bin/bash", "-c",
				"ls -1 " + ProfilePath + " > " + TempPath + GenericConfig.TEMP_HEADER + "prfl.list"};
		Shell.exec(cmd);
		try {
			FileStream tmpListStream = new FileStream(TempPath + GenericConfig.TEMP_HEADER + "prfl.list", 'r');
			tmpListStream.isTemp();
			String buf;
			while((buf = tmpListStream.readLine()) != null) {
				if(!buf.endsWith(".hmm") && !buf.endsWith(".blk")) continue;
				int loc = 0;
				for(; loc < cnt.length; loc++) if(buf.contains(GenericConfig.FCG[loc])) break;
				if(loc == cnt.length) continue;
				cnt[loc]++;
			}
			tmpListStream.wipe(true);
			
			for(int c : cnt) if(c < 0) return false;
		}
		catch(java.io.IOException e) {
			e.printStackTrace();
			ExceptionHandler.handle(ExceptionHandler.EXCEPTION);
		}
		return true;
	}
	
	public static String AugustusConfig = EnvironmentPath + "config/ppx.cfg";
	private static void renewAugustusConfig() {AugustusConfig = EnvironmentPath + "config/ppx.cfg";}
	public static int setAugustusConfig(String path) {	
		try {
			Prompt.talk("AUGUSTUS-PPX config file check : " + ANSIHandler.wrapper(path, 'B'));
			String[] exec = Shell.exec("file -b " + path);
			if(!exec[0].contains("text")) {
				ExceptionHandler.pass(path);
				ExceptionHandler.handle(ExceptionHandler.INVALID_FILE);
				return 1;
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			ExceptionHandler.handle(ExceptionHandler.EXCEPTION);
			return 2;
		}
		
		AugustusConfig = path;
		return 0;
	}
}
