package module;

import envs.config.GenericConfig;
import envs.config.PathConfig;
import envs.toolkit.ANSIHandler;
import envs.toolkit.Prompt;
//import envs.toolkit.TimeKeeper;
import envs.toolkit.Shell;
import pipeline.ExceptionHandler;
import envs.toolkit.FileStream;

import entity.BlockProfileEntity;
import entity.ProfilePredictionEntity;
import entity.QueryEntity;

import process.FastBlockSearchProcess;
import process.ContigDragProcess;
import process.GenePredictionProcess;
import process.HmmsearchProcess;
import process.JsonBuildProcess;

import wrapper.FastBlockSearchWrapper;
import wrapper.AugustusWrapper;
import wrapper.HmmsearchWrapper;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.commons.cli.MissingArgumentException;

public class ProfileModule {
	
	/* Argument parsing route */
	private static int parseArgument(String[] args) throws ParseException {
		/* option argument setup */
		Options opts = new Options();
		opts.addOption("h", "help", false, "helper route");
		opts.addOption("u", "inter", false, "interactive route");
		
		opts.addOption("i", "input", true, "input path");
		opts.addOption("o", "output", true, "output path");
		opts.addOption("k", "keep", true, "intermediate path");
		opts.addOption("f", "force", false, "force delete");
		opts.addOption("t", "thread",  true, "number of cpu threads");
		
		opts.addOption(null, "fastblocksearch", true, "fastBlockSearch binary");
		opts.addOption(null, "augustus", true, "AUGUSTUS binary");
		opts.addOption(null, "hmmsearch", true, "hmmsearch binary");
		
		opts.addOption("m", "metadata", true, "metadata path");
		opts.addOption(null, "metainfo", true, "single file metadata information");
//		opts.addOption("n", "intron", false, "include intron sequences");
		opts.addOption(null, "profile", true, "core gene profile path");
		opts.addOption(null, "ppxcfg", true, "AUGUSTUS-PPX config path");
		opts.addOption("v", "verbose", false, "verbosity");
		
		opts.addOption(null, "fbscutoff", true, "fastBlockSearch cutoff");
		opts.addOption(null, "augoffset", true, "AUGUSTUS prediction offset");
		opts.addOption(null, "hmmscore", true, "hmmsearch score cutoff");
		opts.addOption(null, "corelist", true, "custom core gene list");
		
		opts.addOption(null, "notime", false, "no timestamp with prompt");
		opts.addOption(null, "nocolor", false, "disable ANSI escapes");
		opts.addOption("v", "verbose", false, "verbosity");
		opts.addOption(null, "developer", false, "developer tool");
		
		/* parse argument with CommandLineParser */
		CommandLineParser clp = new DefaultParser();
		CommandLine cmd = null;
		try{ cmd = clp.parse(opts, args); }
		catch(UnrecognizedOptionException uoe) {
			ExceptionHandler.pass(uoe.getOption());
			ExceptionHandler.handle(ExceptionHandler.UNKNOWN_OPTION);
		}
		catch(MissingArgumentException mae) {
			ExceptionHandler.pass(mae.getOption().getOpt() != null ?
					mae.getOption().getOpt() :
					mae.getOption().getLongOpt());
			ExceptionHandler.handle(ExceptionHandler.MISSING_ARGUMENT);
		}
		if(cmd.hasOption("developer")) {
			GenericConfig.DEV = true;
			GenericConfig.VERB = true;
			GenericConfig.TSTAMP = true;
		}
		
		/* parse user friendly options; return ID and finish routine if necessary */
		if(cmd.hasOption("v"))		 GenericConfig.VERB = true;
		if(cmd.hasOption("u"))		 GenericConfig.INTERACT = true;
		if(cmd.hasOption("notime"))  GenericConfig.TSTAMP = false;
		if(cmd.hasOption("nocolor")) GenericConfig.NOCOLOR = true;
		if(cmd.hasOption("h"))		 return -1;
		if(cmd.hasOption("f"))		 GenericConfig.FORCE = true;
		
		if(GenericConfig.INTERACT) return 1; 
		else {
			Prompt.debug(ANSIHandler.wrapper("Developer mode activated.", 'Y'));
			Prompt.talk("Verbose option check.");
			if(GenericConfig.TSTAMP) Prompt.talk("Timestamp printing option check."); 
		}
		
		/* parse general I/O options */
		if(cmd.hasOption("i"))
			PathConfig.setInputPath(cmd.getOptionValue("i"));
		else ExceptionHandler.handle(ExceptionHandler.NO_INPUT);
		if(cmd.hasOption("o"))
			PathConfig.setOutputPath(cmd.getOptionValue("o"));
		else ExceptionHandler.handle(ExceptionHandler.NO_OUTPUT);
		if(cmd.hasOption("k"))
			PathConfig.setTempPath(cmd.getOptionValue("k"));
		if(cmd.hasOption("t"))
			GenericConfig.setThreadPoolSize(cmd.getOptionValue("t"));
		
		/* parse dependency options */
		if(cmd.hasOption("fastblocksearch"))
			PathConfig.setFastBlockSearchPath(cmd.getOptionValue("fastblocksearch"));
		if(cmd.hasOption("augustus"))
			PathConfig.setAugustusPath(cmd.getOptionValue("augustus"));
		if(cmd.hasOption("hmmsearch"))
			PathConfig.setHmmsearchPath(cmd.getOptionValue("hmmsearch"));
		
		/* parse configuration options */
		if(cmd.hasOption("m"))
			PathConfig.setMetaPath(cmd.getOptionValue("m"));
//		if(cmd.hasOption("n")) {
//			Prompt.talk("Including intron to the result DNA sequences.");
//			GenericConfig.INTRON = true;
//		}
		if(cmd.hasOption("metainfo")) {
			// check confilct
			if(cmd.hasOption("m") || PathConfig.InputIsFolder) ExceptionHandler.handle(ExceptionHandler.METAINFO_CONFLICT);
			PathConfig.MetaString = cmd.getOptionValue("metainfo");
		}
		if(cmd.hasOption("profile"))
			PathConfig.setProfilePath(cmd.getOptionValue("profile"));
		if(cmd.hasOption("ppxcfg"))
			PathConfig.setAugustusConfig(cmd.getOptionValue("ppxcfg"));
		
		/* parse advanced options */
		if(cmd.hasOption("fbscutoff"))
			GenericConfig.setFastBlockSearchCutoff(cmd.getOptionValue("fbscutoff"));
		if(cmd.hasOption("augoffset"))
			GenericConfig.setAugustusPredictionOffset(cmd.getOptionValue("augoffset"));
		if(cmd.hasOption("hmmscore"))
			GenericConfig.setHmmsearchScoreCutoff(cmd.getOptionValue("hmmscore"));
		if(cmd.hasOption("corelist"))
			GenericConfig.setCustomCoreList(cmd.getOptionValue("corelist"));
		
		/* successfully parsed */
		Prompt.talk(ANSIHandler.wrapper("SUCCESS", 'g') + " : Option parsing");
		return 0;
	}
	
	/* Dependency solving route; skipped in interactive mode */
	private static void solveDependency() {
		Prompt.talk("Solving dependencies...");
		
		if(!FastBlockSearchWrapper.solve()) {
			ExceptionHandler.pass(PathConfig.FastBlockSearchPath);
			ExceptionHandler.handle(ExceptionHandler.DEPENDENCY_UNSOLVED);
		}
		if(!AugustusWrapper.solve()) {
			ExceptionHandler.pass(PathConfig.AugustusPath);
			ExceptionHandler.handle(ExceptionHandler.DEPENDENCY_UNSOLVED);
		}
		if(!AugustusWrapper.checkConfigPath()) {
			ExceptionHandler.handle(ExceptionHandler.CONFIG_PATH_UNDEFINED);
		}
		if(!AugustusWrapper.checkConfigFile()) {
			ExceptionHandler.handle(ExceptionHandler.INVALID_PPX_CONFIG);
		}
		if(!HmmsearchWrapper.solve()) {
			ExceptionHandler.pass(PathConfig.HmmsearchPath);
			ExceptionHandler.handle(ExceptionHandler.DEPENDENCY_UNSOLVED);
		}
		if(!PathConfig.checkProfilePath()) {
			ExceptionHandler.handle(ExceptionHandler.INVALID_PROFILE_PATH);
		}
		
		Prompt.talk(ANSIHandler.wrapper("SUCCESS", 'g') + " : Dependency solving");
	}
	
	/* Manual route; exit with status 0 */
	private static void printManual() {
		System.out.println(
				ANSIHandler.wrapper("Manual - UFCG profile module\n", 'Y') + 
				
				ANSIHandler.wrapper("[General usage]\n\n", 'y') +
				"    Interactive mode : java -jar /path/to/UFCG.jar profile -u\n"+
				"    One-liner mode   : java -jar /path/to/UFCG.jar profile -i <PATH> -o <PATH> [options]\n\n"+
				
				ANSIHandler.wrapper("[Dependencies]\n\n", 'y') +
				ANSIHandler.wrapper("* AUGUSTUS (fastBlockSearch, AUGUSTUS-PPX)\n", 'c') +
				"    UFCG profile module extracts genes using AUGUSTUS gene prediction tool.\n" +
				"    AUGUSTUS is avaliable at: https://github.com/Gaius-Augustus/Augustus\n\n" + 
				
				ANSIHandler.wrapper("* HMMER (hmmsearch)\n", 'c') +
				"    hmmsearch is required to select proper genes among predicted sequences.\n" +
				"    HMMER is avaliable at: http://hmmer.org/\n\n" +
				
				ANSIHandler.wrapper("Note. Binaries should be located in your environmental PATH.\n", 'B') +
				ANSIHandler.wrapper("Otherwise, provide the locations using dependency related options.\n\n", 'B') +

				ANSIHandler.wrapper("[Avaliable options]\n\n", 'y') +
				ANSIHandler.wrapper("* User friendly options\n", 'c') +
				"    -h, --help    : Print this manual\n" + 
//				"    --info        : Print program information\n" + 
//				"    --core        : Print core gene list\n" +
				"    -u, --inter   : User interactive mode\n" +
//				"    -v, --verbose : Make program verbose\n" + 
//				"    --nocolor     : Remove ANSI escapes from the output\n\n" +
				
				ANSIHandler.wrapper("* General I/O\n", 'c') + 
				"    -i, --input  <PATH>  : Single file or directory containing fungal genome assemblies\n" + 
				ANSIHandler.wrapper(
						"        Note. For an input directory, included files must share the extension and the file type.\n"
				, 'B') + 
				"    -o, --output <PATH>  : Directory to store result files\n" + 
				"    -k, --keep   <PATH>  : Directory to keep intermediate files\n" + 
				ANSIHandler.wrapper(
						"        Note. If not given, program will use '/tmp' directory and wipe out all the temporary files as the process finishes.\n"
				, 'B') + 
				"    -f, --force          : Force to overwrite result files in output directory (default = false)\n" + 
				"\n" + 
				ANSIHandler.wrapper("* Dependencies\n", 'c') + 
				"    --fastblocksearch <PATH> : Path to fastBlockSearch binary file\n" + 
				"    --augustus <PATH>        : Path to AUGUSTUS binary file\n" + 
				"    --hmmsearch <PATH>       : Path to hmmsearch binary file\n\n" + 
				
				ANSIHandler.wrapper("* Configuration\n", 'c') + 
				"    -t, --thread <NUMBER> : Number of CPU thread(s) for multithread processing (default = 1)\n" +
				"    -m, --metadata <PATH> : List containing metadata of the assembly file(s)\n" + 
				ANSIHandler.wrapper(
						"        Note. List should be formatted and respectively ordered with proper header. Refer to the sample list in 'sample' directory.\n"
				, 'B') + 
/*				"    -n, --intron          : Include introns from the predicted ORFs to the result sequences\n" +
				ANSIHandler.wrapper(
						"        Note. Including introns may improve the resolution of intra-genus or intra-species taxonomy.\n"
				, 'B') + */
				"    --metainfo <INFO>     : Metadata information for a single file input\n" +
				ANSIHandler.wrapper(
						"        Note. Information should include the seven entries from 'sample/meta_full.tsv' in respective order, seperated by comma.\n" +
						"              Put 'null' or leave a blank to indicate unavailable entries.\n"
				, 'B') + 
				"    --profile  <PATH>     : Path to core genome profiles (default : 'config/prfl')\n" +
				"    --ppxcfg   <PATH>     : Path to AUGUSTUS-PPX config file (default : 'config/ppx.cfg')\n" +
//				"    --timestamp           : Print timestamp in front of the prompt string\n\n" +
								
				ANSIHandler.wrapper("* Advanced options\n", 'c') + 
				"    --fbscutoff <VALUE>   : Customize cutoff value for fastBlockSearch process (default = 0.5)\n" + 
				"    --augoffset <VALUE>   : Customize prediction offset window size for AUGUSTUS process (default = 10000)\n" + 
				"    --hmmscore  <VALUE>   : Customize bitscore cutoff for hmmsearch validation (default = 100)\n" +
				"    --corelist  <LIST>    : Use custom set of fungal core genes\n" + 
				ANSIHandler.wrapper(
						"        Note. List should contain valid fungal core gene names separated by comma.\n"
				, 'B')
//				+ "    --developer           : Activate developer mode (For beta-testing or debugging)\n"
				);
		System.exit(0);
	}
	
	/* Interactive route */
	private static int interactiveRoute(String arg) throws IOException {
		GenericConfig.INTERACT = true;
		BufferedReader stream = new BufferedReader(new InputStreamReader(System.in));
		String buf = null;
		boolean proceed = false;
		
		/* obtain runtime environment */
		String jarPath = ProfileModule.class.getProtectionDomain().getCodeSource().getLocation().getPath();
		String exePath = new File(".").getAbsolutePath();
		String[] jarSplit = jarPath.split("/");
		String[] exeSplit = exePath.split("/");
		int common = 0; while(jarSplit[common].equals(exeSplit[common])) common++;
		
		String runPath = "";
		for(int i = common; i < exeSplit.length - 1; i++) runPath += "../";
		for(int i = common; i < jarSplit.length; i++) runPath += jarSplit[i] + "/";
		String command = "java -jar " + runPath.substring(0, runPath.length() - 1);
		jarPath = jarPath.substring(0, jarPath.lastIndexOf('/') + 1);
		
		/* initiate and get mandatory option */
		Prompt.debug(ANSIHandler.wrapper("Developer mode activated.", 'Y'));
		Prompt.talk("Verbose option check.");
		if(GenericConfig.TSTAMP) Prompt.talk("Timestamp printing option check.");
		Prompt.print(ANSIHandler.wrapper("Welcome to the interactive UFCG profile module.", 'Y'));
		
		// --input
		while(!proceed) {
			Prompt.print_nnc("Enter the file or directory containing fungal genome assemblies (--input) : ");
			buf = stream.readLine();
			if(buf.length() == 0) continue;
			if(PathConfig.setInputPath(buf) == 0) {
				proceed = true;
				command += " --input " + buf;
			}
		}
		proceed = false;
		
		// --output
		while(!proceed) {
			Prompt.print_nnc("Enter the directory to store your result (--output) : ");
			buf = stream.readLine();
			if(buf.length() == 0) continue;
			if(PathConfig.setOutputPath(buf) == 0) {
				proceed = true;
				command += " --output " + buf;
			}
		}
		proceed = false;
		
		// --force
		if(PathConfig.checkOutputPath() > 0) {
			while(!proceed) {
				Prompt.print_nnc("Result files exist in the output directory. Force to overwrite? (--force) (y/n) : ");
				buf = stream.readLine();
				if(buf.startsWith("y") || buf.startsWith("Y")) {
					GenericConfig.FORCE = true;
					proceed = true;
					command += " --force ";
				}
				else if(buf.startsWith("n") || buf.startsWith("N")) {
					GenericConfig.FORCE = false;
					proceed = true;
				}
			}
			proceed = false;
		}
		
		// --temp
		while(!proceed) {
			Prompt.print_nnc("Maintain temporarily created files? (y/n) : ");
			buf = stream.readLine();
			if(buf.startsWith("y") || buf.startsWith("Y")) {
				while(!proceed) {
					Prompt.print_nnc("Enter the directory to store temporary files (--keep) : ");
					buf = stream.readLine();
					if(buf.length() == 0) continue;
					if(PathConfig.setTempPath(buf) == 0) {
						proceed = true;
						command += " --keep " + buf;
					}
				}
			}
			if(buf.startsWith("n") || buf.startsWith("N")) proceed = true;
		}
		proceed = false;
		
		/* solve dependencies; request for an input only if there exists failed dependencies */
		String solvedPath;
		Prompt.print("Solving dependencies...");
		
		// --fastblocksearch
		solvedPath = null;
		while(!proceed) {
			if(FastBlockSearchWrapper.solve()) proceed = true;
			else {
				Prompt.print("Failed to solve : " + PathConfig.FastBlockSearchPath);
				while(!proceed) {
					Prompt.print_nnc("Enter the location of fastBlockSearch binary (--fastblocksearch) : ");
					buf = stream.readLine();
					if(buf.length() == 0) continue;
					if(PathConfig.setFastBlockSearchPath(buf) == 0) {
						solvedPath = buf;
						proceed = true;
					}
				}
				proceed = false;
			}
		}
		proceed = false;
		if(solvedPath != null) command += " --fastblocksearch " + solvedPath;
		
		// --augustus
		solvedPath = null;
		while(!proceed) {
			if(AugustusWrapper.solve()) {
				/* check AUGUSTUS_CONFIG_PATH local variable */
				while(!proceed) {
					if(AugustusWrapper.checkConfigPath()) proceed = true;
					else {
						Prompt.print("Failed to resolve AUGUSTUS_CONFIG_PATH. Locate the proper directory and relaunch the program.");
						System.exit(0);
					}
				}
				proceed = true;
			}
			else {
				Prompt.print("Failed to solve : " + PathConfig.AugustusPath);
				while(!proceed) {
					Prompt.print_nnc("Enter the location of AUGUSTUS binary (--augustus) : ");
					buf = stream.readLine();
					if(buf.length() == 0) continue;
					if(PathConfig.setAugustusPath(buf) == 0) {
						solvedPath = buf;
						proceed = true;
					}
				}
				proceed = false;
			}
		}
		proceed = false;
		if(solvedPath != null) command += " --augustus " + solvedPath;
		
		// --hmmsearch
		solvedPath = null;
		while(!proceed) {
			if(HmmsearchWrapper.solve()) proceed = true;
			else {
				Prompt.print("Failed to solve : " + PathConfig.HmmsearchPath);
				while(!proceed) {
					Prompt.print_nnc("Enter the location of hmmsearch binary (--hmmsearch) : ");
					buf = stream.readLine();
					if(buf.length() == 0) continue;
					if(PathConfig.setHmmsearchPath(buf) == 0) {
						solvedPath = buf;
						proceed = true;
					}
				}
				proceed = false;
			}
		}
		proceed = false;
		if(solvedPath != null) command += " --hmmsearch " + solvedPath;
		
		/* set runtime configurations */
		// --cpu
		while(!proceed) {
			Prompt.print_nnc("Enter the number of CPU thread to use (--thread, default = 1) : ");
			buf = stream.readLine();
			if(buf.length() == 0) continue;
			if(GenericConfig.setThreadPoolSize(buf) == 0) {
				proceed = true;
				command += " --thread " + buf;
			}
		}
		proceed = false;
		
		// --metadata
		while(!proceed) {
			Prompt.print_nnc("Provide metadata for assembly file(s)? (y/n) : ");
			buf = stream.readLine();
			if(buf.startsWith("y") || buf.startsWith("Y")) {
				while(!proceed) {
					Prompt.print_nnc("Enter the file containing metadata list (--metadata) : ");
					buf = stream.readLine();
					if(buf.length() == 0) continue;
					if(PathConfig.setMetaPath(buf) == 0) {
						proceed = true;
						command += " --metadata " + buf;
					}
				}
			}
			if(buf.startsWith("n") || buf.startsWith("N")) {
				Prompt.print("No metadata given. Inferring taxon label from filenames.");
				proceed = true;
			}
		}
		proceed = false;
		
		/* locate config/prfl directory; request custom folder if failed */
		// --profile
		String ptype = Shell.exec("file -b " + jarPath + "config/prfl/")[0];
		boolean flag = ptype.contains("directory") && !ptype.contains("cannot");
		if(!flag) {
			Prompt.print("Default core gene profile directory not found.");
			while(!proceed) {
				Prompt.print_nnc("Enter your custom core gene profile directory (--profile) : ");
				buf = stream.readLine();
				if(buf.length() == 0) continue;
				if(PathConfig.setProfilePath(buf) == 0) {
					proceed = true;
					command += " --profile " + buf;
				}
			}
			proceed = false;
		}
		
		/* profile directory validation */
		if(!PathConfig.checkProfilePath()) {
			ExceptionHandler.handle(ExceptionHandler.INVALID_PROFILE_PATH);
			Prompt.print("Please check the path and content of your core gene profile directory and relaunch the program.\n");
			System.exit(0);
		}
		
		/* locate ppx.cfg file; request custom file if failed */
		// --ppxcfg
		String[] cmd = {"/bin/bash", "-c", "head -1 " + jarPath + "config/ppx.cfg 2>&1"};
		String[] ppx = Shell.exec(cmd);
		flag = !ppx[0].contains("No");
		/* directly use default file if exists, without asking user (for convenience) */
/*		if(flag) {
			while(!proceed) {
				Prompt.print_nnc("Use default AUGUSTUS-PPX config file (config/ppx.cfg)? (y/n) : ");
				buf = stream.readLine();
				if(buf.startsWith("y") || buf.startsWith("Y")) proceed = true;
				if(buf.startsWith("n") || buf.startsWith("N")) {
					flag = false;
					proceed = true;
				}
			}
			proceed = false;
		}
		else Prompt.print("Default AUGUSTUS-PPX config file not found."); */
		if(!flag) {
			Prompt.print("Default AUGUSTUS-PPX config file not found.");
			while(!proceed) {
				Prompt.print_nnc("Enter your custom AUGUSTUS-PPX config file (--ppxcfg) : ");
				buf = stream.readLine();
				if(buf.length() == 0) continue;
				if(PathConfig.setAugustusConfig(buf) == 0) {
					proceed = true;
					command += " --ppxcfg " + buf;
				}
			}
			proceed = false;
		}
		
		/* config file validation */
		if(!AugustusWrapper.checkConfigFile()) {
			ExceptionHandler.handle(ExceptionHandler.INVALID_PPX_CONFIG);
			Prompt.print("Please check the path and content of your config file and relaunch the program.\n");
			System.exit(0);
		}
		
		/* set advanced options if user requires */
		flag = false;
		while(!proceed) {
			Prompt.print_nnc("Configurate advanced options? (y/n) : ");
			buf = stream.readLine();
			if(buf.length() == 0) continue;
			if(buf.startsWith("n") || buf.startsWith("N")) proceed = true;
			if(buf.startsWith("y") || buf.startsWith("Y")) {
				flag = true;
				proceed = true;
			}
		}
		proceed = false;
		
		if(flag) {
			// --verbose
			while(!proceed) {
				if(GenericConfig.VERB) break; 
				Prompt.print_nnc("Make the program chitty-chatty (--verbose)? (y/n) : ");
				buf = stream.readLine();
				if(buf.length() == 0) continue;
				if(buf.startsWith("n") || buf.startsWith("N")) proceed = true;
				if(buf.startsWith("y") || buf.startsWith("Y")) {
					GenericConfig.VERB = true;
					command += " --verbose ";
					proceed = true;
				}
			}
			proceed = false;
			
/*			// --timestamp
			while(!proceed) {
				if(GenericConfig.TSTAMP) break; 
				Prompt.print_nnc("Disable timestamp before the prompt (--timestamp)? (y/n) : ");
				buf = stream.readLine();
				if(buf.length() == 0) continue;
				if(buf.startsWith("n") || buf.startsWith("N")) proceed = true;
				if(buf.startsWith("y") || buf.startsWith("Y")) {
					GenericConfig.TSTAMP = true;
					command += " --timestamp ";
					proceed = true;
				}
			}
			proceed = false;
			
			// --intron
			while(!proceed) {
				Prompt.print_nnc("Include introns from predicted ORFs? (y/n) : ");
				buf = stream.readLine();
				if(buf.length() == 0) continue;
				if(buf.startsWith("n") || buf.startsWith("N")) proceed = true;
				if(buf.startsWith("y") || buf.startsWith("Y")) {
					GenericConfig.INTRON = true;
					command += " --intron ";
					proceed = true;
				}
			}
			proceed = false;
*/			
			// --fbscutoff
			while(!proceed) {
				Prompt.print_nnc("Enter the cutoff value for fastBlockSearch process (--fbscutoff, default = 0.5) : ");
				buf = stream.readLine();
				if(buf.length() == 0) continue;
				if(GenericConfig.setFastBlockSearchCutoff(buf) == 0) {
					proceed = true;
					command += " --fbscutoff " + buf;
				}
			}
			proceed = false;
			
			// --augoffset
			while(!proceed) {
				Prompt.print_nnc("Enter the prediction offset window for AUGUSTUS process (--augoffset, default = 10000) : ");
				buf = stream.readLine();
				if(buf.length() == 0) continue;
				if(GenericConfig.setAugustusPredictionOffset(buf) == 0) {
					proceed = true;
					command += " --augoffset " + buf;
				}
			}
			proceed = false;
			
			// --hmmscore
			while(!proceed) {
				Prompt.print_nnc("Enter the score cutoff for hmmsearch validation (--hmmscore, default = 100.0) : ");
				buf = stream.readLine();
				if(buf.length() == 0) continue;
				if(GenericConfig.setHmmsearchScoreCutoff(buf) == 0) {
					proceed = true;
					command += " --hmmscore " + buf;
				}
			}
			proceed = false;
			
			// --corelist
			while(!proceed) {
				Prompt.print_nnc("Enter your custom set of fungal core genes. Type 'x' to use the default set. (--corelist) : ");
				buf = stream.readLine();
				if(buf.length() == 0) continue;
				if(buf.startsWith("x")) break;
				if(GenericConfig.setCustomCoreList(buf) == 0) {
					proceed = true;
					command += " --corelist " + buf;
				}
			}
			proceed = false;
		}
		
		/* Final confirmation */
		Prompt.print("Following command will be executed : " + ANSIHandler.wrapper(command, 'y'));
		while(!proceed) {
			Prompt.print_nnc("Confirm? Enter y (confirm) / n (reset) / x (exit) : ");
			buf = stream.readLine();
			if(buf.startsWith("y") || buf.startsWith("Y")) proceed = true;
			if(buf.startsWith("n") || buf.startsWith("N")) {
				Prompt.print("Relaunching interactive mode.\n");
				return 1;
			}
			if(buf.startsWith("x") || buf.startsWith("X")) {
				System.out.println("");
				System.exit(0);
			}
		}

		return 0;
	}
	
	/* Profile module runner
	 * Implementation note.
	 * 		All instructions wrapped in a single try-catch statement.
	 * 		Any exception occurred will be caught and thrown to ExceptionHandler.
	 */
	public static void run(String[] args) {
		try {
			/* Environment setup and path definition */
/*			GenericConfig.setHeader("UFCG");
			GenericConfig.setHeaderLength(5);
			String jarPath = ProfileModule.class.getProtectionDomain().getCodeSource().getLocation().getPath();
			PathConfig.setEnvironmentPath(jarPath.substring(0, jarPath.lastIndexOf("/") + 1));
			System.out.println("");
			FileStream.init(); */
			
			/* Argument parsing and route selection */
			switch(parseArgument(args)) {
			case -1: printManual();
			case -4: Prompt.print(GenericConfig.geneString()); break;
			case  1: while(interactiveRoute(args[0]) > 0); break;
			case  0: solveDependency(); break;
			default: System.exit(1);
			}
			
			/* Core pipeline begins here */
			Prompt.talk("Launching UFCG profile module...\n");
			if(GenericConfig.INTERACT && !GenericConfig.VERB) System.out.println("");
//			if(!GenericConfig.INTERACT) printLogo();
			GenericConfig.INTERACT = false;
			
			/* Import metadata and test file integrity */
			QueryEntity.importMetadata();
			int integrity;
			if((integrity = QueryEntity.testIntegrity()) > 0) {
				switch(integrity) {
				case 1: ExceptionHandler.pass("file extensions must be unified."); break;
				case 2: ExceptionHandler.pass("the number of data exceeds the number of files."); break;
				case 3: ExceptionHandler.pass("filenames must correspond to the metadata."); break;
				case 4: ExceptionHandler.pass("file type must be unified."); break;
				}
				ExceptionHandler.handle(ExceptionHandler.INTEGRITY_TEST_FAILED);
			}
			
			/* Create queries with QueryEntity */
			List<QueryEntity> queries = QueryEntity.createQuery();
			Prompt.print(String.format("Queries prepared. %d genome sequences identified.", queries.size()));
			
			for(int q = 0; q < queries.size(); q++) {
				/* Implementation note.
				 * 		Source code below here might be quite messy because of various prompt printing calls.
				 * 		Prompt.dynamic	: Dynamic (single-line) prompts for standard output
				 * 		Prompt.talk		: Prompts for verbose output
				 * 		Prompt.debug	: Prompts for developer mode output
				 */
				
				/* Grab and activate query */
				QueryEntity query = queries.get(q);
				query.activate();
				Prompt.print(String.format("QUERY %d/%d : %s %s",
						q + 1, queries.size(),
						ANSIHandler.wrapper(GenericConfig.ACCESS, 'y'),
						ANSIHandler.wrapper("(" + (QueryEntity.EX_TAXON >= 0 ? GenericConfig.TAXON : GenericConfig.LABEL) + ")", 'B')));
				
				/* Check file pre-existence if force is off */
				if(!GenericConfig.FORCE) {
					if(query.checkResultFileExistence() > 0) {
						Prompt.print("Result file already exists. To overwrite, use --force option.");
						continue;
					}
				}
				
				/* Dynamic progress prompt setup */
				initProg();
				printProg();
				
				/* Iterate through genes and run extract session
				 * Single iteration consists of three phases:
				 * 		1. SEARCH -> 2. PREDICT -> 3. VALIDATE
				 */
				/* Multithreading with ExecutorService (from ver 0.3) */
				nSgl = 0; nMul = 0; nUid = 0;
				List<ProfilePredictionEntity> pps = new ArrayList<ProfilePredictionEntity>();
				
				ExecutorService executorService = Executors.newFixedThreadPool(GenericConfig.ThreadPoolSize);
				List<Future<ProfilePredictionEntity>> futures = new ArrayList<Future<ProfilePredictionEntity>>();
				
				for(int g = 0; g < GenericConfig.FCG.length; g++) {
					CreateProfile creator = new ProfileModule().new CreateProfile(g);
					futures.add(executorService.submit(creator));
					Thread.sleep(500);
				}

				executorService.shutdown();
				for(Future<ProfilePredictionEntity> future : futures) pps.add(future.get());
				
				/* Write the entire result on a single JSON file */
				Prompt.dynamic(ANSIHandler.wrapper(" DONE", 'g') + "\n");
				Prompt.print(String.format("RESULT : [Single: %s ; Duplicated: %s ; Missing: %s]",
						ANSIHandler.wrapper(nSgl, 'g'), ANSIHandler.wrapper(nMul, 'G'), ANSIHandler.wrapper(nUid, 'r')));
				String jsonPath = PathConfig.OutputPath + GenericConfig.ACCESS + ".ucg";
				Prompt.print("Writing results on : " + ANSIHandler.wrapper(jsonPath, 'y'));
				JsonBuildProcess.build(pps, jsonPath);
				
				for(String contig : contigs) FileStream.wipe(contig, true);
				contigs = new ArrayList<String>();
			}
			
			/* Clean up temporary files if exist */
			if(!PathConfig.TempIsCustom) Prompt.talk("Cleaning temporary files up...");
			FileStream.wipeOut();
		}
		catch(Exception e) {
			/* Exception handling route; exit with status 1 */
			// e.printStackTrace();
			ExceptionHandler.handle(e);
		}
		finally {
			Prompt.print("Job finished. Terminating process.\n");
			System.exit(0);
		}
	}
	
	static List<Status> progress = new LinkedList<Status>();
	private static class Status {
		private String stat;
		private Status() {
			this.stat = ANSIHandler.wrapper("X", 'K');
		}
		private void updateStat(String stat) {
			this.stat = stat;
		}
	}
	
	static void initProg() {
		progress.clear();
		for(int g = 0; g < GenericConfig.FCG.length; g++) progress.add(new Status());
	}
	static void updateProg(int g, String ch) {
		progress.get(g).updateStat(ch);
	}
	
	static void printProg() {
		String build = "PROGRESS : [";
		
		try{
			for(Status s : progress) build += s.stat;
		}
		catch(java.util.ConcurrentModificationException cme) {
			return;
		}
		catch(NullPointerException npe) {
			return;
		}
		
		build += "]";
		Prompt.dynamic("\r");
		Prompt.dynamicHeader(build);
	}
	
	static List<String> contigs = new ArrayList<String>();
	static int nSgl, nMul, nUid;
	private class CreateProfile implements Callable<ProfilePredictionEntity> {	
		private int g;
		public CreateProfile(int gid) {
			this.g = gid;
		}
		
		public ProfilePredictionEntity call() throws Exception {
			/* Prepare gene and set prompt */
			String cg = GenericConfig.FCG[g];
//			Prompt.dynamic(ANSIHandler.wrapper(String.format("%-6s", cg), 'Y') + " >> [" + progress);
			Prompt.talk("Extracting gene " + ANSIHandler.wrapper(cg, 'Y') + 
					String.format(" [%d/%d]", g+1, GenericConfig.FCG.length) + " from the query genome...");

			/* Phase 1. SEARCH */
			updateProg(g, ANSIHandler.wrapper("S", 'p')); printProg();
			Prompt.talk(ANSIHandler.wrapper("[Phase 1 : Searching]", 'p'));
			String seqPath = PathConfig.InputIsFolder ? PathConfig.InputPath + GenericConfig.FILENAME : PathConfig.InputPath;
			/* Run fastBlockSearch on assembly to find gene containing location (contig, position) */
			BlockProfileEntity bp = FastBlockSearchProcess.handle(seqPath, PathConfig.ProfilePath + "uucg_fungi_" + cg + ".blk", cg);
			/* Drag required contigs from assembly and store their paths */
			List<String> ctgPaths = ContigDragProcess.drag(seqPath, bp);
			
			/* Phase 2. PREDICT */
			updateProg(g, ANSIHandler.wrapper("P", 'c')); printProg();
			Prompt.talk(ANSIHandler.wrapper("[Phase 2 : Prediction]", 'c'));
			/* Run AUGUSTUS on the block found in Phase 1 */
			ProfilePredictionEntity pp = GenePredictionProcess.blockPredict(bp, ctgPaths);
			
			/* Phase 3. VALIDATE */
			updateProg(g, ANSIHandler.wrapper("V", 'R')); printProg();
			Prompt.talk(ANSIHandler.wrapper("[Phase 3 : Validation]", 'R'));
			/* Run hmmsearch and find target gene among the predicted genes */
			if(pp.nseq() > 0) HmmsearchProcess.search(pp, PathConfig.ProfilePath + "uucg_fungi_" + cg + ".hmm");
			/* Implementation note.
			 *		Previously, contigs were removed right after AUGUSTUS finishes the prediction.
			 *		However, to extract the cDNA sequence for the gene, contig should remain intact.
			 *		Therefore, contigs will be removed all together after the entire extraction process.
			 */
			for(String ctgPath : ctgPaths) if(!contigs.contains(ctgPath)) contigs.add(ctgPath);
			
			/* Record obtained result */
			String result = "";
			if(pp.valid()) {
				if(pp.multiple()) {
					/* Multiple copies */
					nMul++;
					result = ANSIHandler.wrapper("O", 'G');
					Prompt.talk("Query genome contains " + ANSIHandler.wrapper("duplicated", 'G') +
								" copies of gene " + ANSIHandler.wrapper(cg, 'Y'));
				}
				else {
					/* Single copy */
					nSgl++;
					result = ANSIHandler.wrapper("O", 'g');
					Prompt.talk("Query genome contains a " + ANSIHandler.wrapper("single", 'g') +
							" copy of gene " + ANSIHandler.wrapper(cg, 'Y'));
				}
			}
			else {
				/* Extraction failed */
				nUid++;
				result = ANSIHandler.wrapper("X", 'r');
				Prompt.talk("Query genome is " + ANSIHandler.wrapper("missing", 'r') +
						" gene " + ANSIHandler.wrapper(cg, 'Y'));
			}
			
			updateProg(g, result);
			printProg();
			return pp;
		}
	}
}
