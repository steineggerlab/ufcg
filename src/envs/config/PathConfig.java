package envs.config;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import envs.toolkit.ANSIHandler;
import envs.toolkit.FileStream;
import envs.toolkit.Prompt;
import envs.toolkit.Shell;
import pipeline.ExceptionHandler;
import pipeline.UFCGMainPipeline;

public class PathConfig {
	/* Environment */
	public static final String CurrPath = Paths.get("").toAbsolutePath() + File.separator;
	public static final String JarPath = new File(UFCGMainPipeline.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getParent() + File.separator;
	public static String EnvironmentPath = "";
	public static boolean EnvironmentPathSet = false;
	public static int setEnvironmentPath(String path) {
		if(path.charAt(path.length() - 1) != File.separatorChar) path += File.separatorChar;
		EnvironmentPath = path;
		boolean renew = renewModelPath() > 0;
		renew &= renewSeqPath() > 0;
		renew &= renewAugustusConfig() > 0;
		if(renew) return 1;
		EnvironmentPathSet = true;
		return 0;
	}
	
	/* Dependent binaries */
	public static String FastBlockSearchPath = "fastBlockSearch";
	public static int setFastBlockSearchPath(String path) {
		try {
			Prompt.talk("fastBlockSearch binary check : " + ANSIHandler.wrapper(path, 'B'));
			String[] exec = Shell.exec("file -b " + path);
			if(exec[0].contains("link")) {
				// symbolic link given, get the real path
				Path link = Files.readSymbolicLink(Paths.get(path));
				Path real = link.toRealPath().toAbsolutePath();
				Prompt.talk("Symbolic link detected : " + ANSIHandler.wrapper(path, 'B') + " -> " + ANSIHandler.wrapper(real.toString(), 'B'));
				return setFastBlockSearchPath(real.toString());
			}
			if(!exec[0].contains("exec")) {
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
			if(exec[0].contains("link")) {
				// symbolic link given, get the real path
				Path link = Files.readSymbolicLink(Paths.get(path));
				Path real = link.toRealPath().toAbsolutePath();
				Prompt.talk("Symbolic link detected : " + ANSIHandler.wrapper(path, 'B') + " -> " + ANSIHandler.wrapper(real.toString(), 'B'));
				return setAugustusPath(real.toString());
			}
			if(!exec[0].contains("exec")) {
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
	@Deprecated
	public static int setHmmsearchPath(String path) {
		try {
			Prompt.talk("hmmsearch binary check : " + ANSIHandler.wrapper(path, 'B'));
			String[] exec = Shell.exec("file -b " + path);
			if(exec[0].contains("link")) {
				// symbolic link given, get the real path
				Path link = Files.readSymbolicLink(Paths.get(path));
				Path real = link.toRealPath().toAbsolutePath();
				Prompt.talk("Symbolic link detected : " + ANSIHandler.wrapper(path, 'B') + " -> " + ANSIHandler.wrapper(real.toString(), 'B'));
				return setHmmsearchPath(real.toString());
			}
			if(!exec[0].contains("exec")) {
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
	
	public static String MMseqsPath = "mmseqs";
	public static int setMMseqsPath(String path) {
		try {
			Prompt.talk("MMseqs binary check : " + ANSIHandler.wrapper(path, 'B'));
			String[] exec = Shell.exec("file -b " + path);
			if(exec[0].contains("link")) {
				// symbolic link given, get the real path
				Path link = Files.readSymbolicLink(Paths.get(path));
				Path real = link.toRealPath().toAbsolutePath();
				Prompt.talk("Symbolic link detected : " + ANSIHandler.wrapper(path, 'B') + " -> " + ANSIHandler.wrapper(real.toString(), 'B'));
				return setMMseqsPath(real.toString());
			}
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
		
		MMseqsPath = path;
		return 0;
	}
	
	public static String TrinityPath = "Trinity";
	public static void setTrinityPath(String path) {
		try {
			Prompt.talk("Trinity binary check : " + ANSIHandler.wrapper(path, 'B'));
			String[] exec = Shell.exec("file -b " + path);
			if(exec[0].contains("link")) {
				// symbolic link given, get the real path
				Path link = Files.readSymbolicLink(Paths.get(path));
				Path real = link.toRealPath().toAbsolutePath();
				Prompt.talk("Symbolic link detected : " + ANSIHandler.wrapper(path, 'B') + " -> " + ANSIHandler.wrapper(real.toString(), 'B'));
				setTrinityPath(real.toString());
				return;
			}
			if(!exec[0].contains("exec") && !exec[0].contains("link")) {
				ExceptionHandler.pass(path);
				ExceptionHandler.handle(ExceptionHandler.INVALID_BINARY);
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			ExceptionHandler.handle(ExceptionHandler.EXCEPTION);
		}
		
		TrinityPath = path;
	}
	
	/* Runtime I/O configuration */
	public static String  InputPath = null;
	public static boolean InputIsFolder = false;
	public static int setInputPath(String path) {
		InputIsFolder = false;
		try {
			Prompt.talk("Input file check : " + ANSIHandler.wrapper(path, 'B'));
			String[] exec = Shell.exec("file -b " + path);
			if(exec[0].contains("link")) {
				// symbolic link given, get the real path
				Path link = Files.readSymbolicLink(Paths.get(path));
				Path real = link.toRealPath().toAbsolutePath();
				Prompt.talk("Symbolic link detected : " + ANSIHandler.wrapper(path, 'B') + " -> " + ANSIHandler.wrapper(real.toString(), 'B'));
				return setInputPath(real.toString());
			}

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
	public static void checkInputFile(String path) {
		try {
			Prompt.talk("Input file check : " + ANSIHandler.wrapper(path, 'B'));
			String[] exec = Shell.exec("file -b " + path);
			Prompt.talk("Input argument : " + ANSIHandler.wrapper(exec[0], 'B'));
			if(exec[0].contains("link")) {
				// symbolic link given, get the real path
				Path link = Files.readSymbolicLink(Paths.get(path));
				Path real = link.toRealPath().toAbsolutePath();
				Prompt.talk("Symbolic link detected : " + ANSIHandler.wrapper(path, 'B') + " -> " + ANSIHandler.wrapper(real.toString(), 'B'));
				checkInputFile(real.toString());
				return;
			}
			if(exec[0].contains("directory") | exec[0].startsWith("cannot")) {
				ExceptionHandler.pass(path);
				ExceptionHandler.handle(ExceptionHandler.INVALID_FILE);
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			ExceptionHandler.handle(ExceptionHandler.EXCEPTION);
		}
	}
	
	public static String OutputPath = null;
	public static int setOutputPath(String path) {		
		try {
			Prompt.talk("Output directory check : " + ANSIHandler.wrapper(path, 'B'));
			File output = new File(path);
			if(output.exists()) {
				if(!output.canWrite()) {
					ExceptionHandler.pass(path);
					ExceptionHandler.handle(ExceptionHandler.INVALID_DIRECTORY);
					return 1;
				}
			} else if(!output.mkdir()) {
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
	
	public static String  OriginalTempPath = "/tmp/";
	public static String  TempPath = "/tmp/";
	public static boolean TempIsCustom = false;
	public static int setTempPath(String path) {		
		try {
			Prompt.talk("Temporary directory check : " + ANSIHandler.wrapper(path, 'B'));
			String[] exec = Shell.exec("file -b " + path);
			if(exec[0].contains("link")) {
				// symbolic link given, get the real path
				Path link = Files.readSymbolicLink(Paths.get(path));
				Path real = link.toRealPath().toAbsolutePath();
				Prompt.talk("Symbolic link detected : " + ANSIHandler.wrapper(path, 'B') + " -> " + ANSIHandler.wrapper(real.toString(), 'B'));
				return setTempPath(real.toString());
			}
			File output = new File(path);
			if(output.exists()) {
				if(!output.canWrite()) {
					ExceptionHandler.pass(path);
					ExceptionHandler.handle(ExceptionHandler.INVALID_DIRECTORY);
				}
			} else if(!output.mkdir()) {
				ExceptionHandler.pass(path);
				ExceptionHandler.handle(ExceptionHandler.INVALID_DIRECTORY);
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			ExceptionHandler.handle(ExceptionHandler.EXCEPTION);
			return 2;
		}
		
		TempPath = path;
		if(!TempPath.endsWith("/")) TempPath += "/";
		GenericConfig.TEMP_HEADER = "";				
		
		return 0;
	}
	public static void restoreTempPath() {TempPath = OriginalTempPath;}
	
	/* Custom configuration files */
	public static String  MetaPath = null;
	public static boolean MetaExists = false;
	public static String  MetaString = null;
	public static int setMetaPath(String path) {		
		try {
			Prompt.talk("Metadata file check : " + ANSIHandler.wrapper(path, 'B'));
			String[] exec = Shell.exec("file -b " + path);
			if(exec[0].contains("link")) {
				// symbolic link given, get the real path
				Path link = Files.readSymbolicLink(Paths.get(path));
				Path real = link.toRealPath().toAbsolutePath();
				Prompt.talk("Symbolic link detected : " + ANSIHandler.wrapper(path, 'B') + " -> " + ANSIHandler.wrapper(real.toString(), 'B'));
				setMetaPath(real.toString());
				return 0;
			}
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
	
	public static String ModelPath = EnvironmentPath + "config/model/";
	private static int renewModelPath() {
		ModelPath = EnvironmentPath + "config/model/";
		if(!new File(ModelPath + "pro/" + GenericConfig.FCG_REF[0] + ".hmm").exists()) return 1;
		return 0;
	}
	public static int setModelPath(String path) {		
		try {
			Prompt.talk("Gene profile directory check : " + ANSIHandler.wrapper(path, 'B'));
			File output = new File(path);
			if(!output.exists()) {
				ExceptionHandler.pass(path);
				ExceptionHandler.handle(ExceptionHandler.INVALID_DIRECTORY);
				return 1;
			} else if(!output.canRead()) {
				ExceptionHandler.pass(path);
				ExceptionHandler.handle(ExceptionHandler.INVALID_DIRECTORY);
				return 1;
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			ExceptionHandler.handle(ExceptionHandler.EXCEPTION);
			return 1;
		}
		
		ModelPath = path;
		if(!ModelPath.endsWith("/")) ModelPath += "/";
		return 0;
	}
	public static boolean checkModelPath() {
		int[] cnt = new int[GenericConfig.FCG.length];
		Arrays.fill(cnt, -1);
		
		String cmd = "ls -1 " + ModelPath + "pro > " + TempPath + GenericConfig.TEMP_HEADER + "model.list";
		Shell.exec(cmd);
		try {
			FileStream tmpListStream = new FileStream(TempPath + GenericConfig.TEMP_HEADER + "model.list", 'r');
			tmpListStream.isTemp();
			String buf;
			while((buf = tmpListStream.readLine()) != null) {
				if(!buf.endsWith(".hmm")) continue;
				int loc = 0;
				for(; loc < cnt.length; loc++) if(buf.substring(0, buf.lastIndexOf(".")).equals(GenericConfig.FCG[loc])) break;
				if(loc == cnt.length) continue;
				cnt[loc]++;
			}
			tmpListStream.wipe(true);
			
			for(int c : cnt) if(c < 0) return true;
		}
		catch(java.io.IOException e) {
			e.printStackTrace();
			ExceptionHandler.handle(ExceptionHandler.EXCEPTION);
		}
		return false;
	}
	
	public static String SeqPath = EnvironmentPath + "config/seq/";
	private static int renewSeqPath() {
		SeqPath = EnvironmentPath + "config/seq/";
		if(!new File(SeqPath + "pro/" + GenericConfig.FCG_REF[0] + ".fa").exists()) return 1;
		return 0;
	}
	public static int setSeqPath(String path) {
		try {
			Prompt.talk("Gene sequence directory check : " + ANSIHandler.wrapper(path, 'B'));
			File output = new File(path);
			if(!output.exists()) {
				ExceptionHandler.pass(path);
				ExceptionHandler.handle(ExceptionHandler.INVALID_DIRECTORY);
				return 1;
			} else if(!output.canRead()) {
				ExceptionHandler.pass(path);
				ExceptionHandler.handle(ExceptionHandler.INVALID_DIRECTORY);
				return 1;
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			ExceptionHandler.handle(ExceptionHandler.EXCEPTION);
			return 1;
		}
		
		SeqPath = path;
		if(!SeqPath.endsWith("/")) SeqPath += "/";
		return 0;
	}
	public static boolean checkSeqPath() {
		int[] cnt = new int[GenericConfig.FCG.length];
		Arrays.fill(cnt, -1);
		
		String cmd = "ls -1 " + SeqPath + "pro > " + TempPath + GenericConfig.TEMP_HEADER + "seq.list";
		Shell.exec(cmd);
		try {
			FileStream tmpListStream = new FileStream(TempPath + GenericConfig.TEMP_HEADER + "seq.list", 'r');
			tmpListStream.isTemp();
			String buf;
			while((buf = tmpListStream.readLine()) != null) {
				if(!buf.endsWith(".fa")) continue;
				int loc = 0;
				for(; loc < cnt.length; loc++) if(buf.substring(0, buf.lastIndexOf(".")).equals(GenericConfig.FCG[loc])) break;
				if(loc == cnt.length) continue;
				cnt[loc]++;
			}
			tmpListStream.wipe(true);
			
			for(int c : cnt) if(c < 0) return true;
		}
		catch(java.io.IOException e) {
			e.printStackTrace();
			ExceptionHandler.handle(ExceptionHandler.EXCEPTION);
		}
		return false;
	}
	
	public static String AugustusConfig = EnvironmentPath + "config/ppx.cfg";
	private static int renewAugustusConfig() {
		AugustusConfig = EnvironmentPath + "config/ppx.cfg";
		if(!new File(AugustusConfig).exists()) return 1;
		return 0;
	}
	public static int setAugustusConfig(String path) {	
		try {
			Prompt.talk("AUGUSTUS-PPX config file check : " + ANSIHandler.wrapper(path, 'B'));
			String[] exec = Shell.exec("file -b " + path);
			if(exec[0].contains("link")) {
				// symbolic link given, get the real path
				Path link = Files.readSymbolicLink(Paths.get(path));
				Path real = link.toRealPath().toAbsolutePath();
				Prompt.talk("Symbolic link detected : " + ANSIHandler.wrapper(path, 'B') + " -> " + ANSIHandler.wrapper(real.toString(), 'B'));
				setAugustusConfig(real.toString());
				return 0;
			}
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
