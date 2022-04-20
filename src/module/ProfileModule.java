package module;

import envs.config.GenericConfig;
import envs.config.PathConfig;
import envs.toolkit.ANSIHandler;
import envs.toolkit.Prompt;
//import envs.toolkit.TimeKeeper;
import envs.toolkit.Shell;
import envs.toolkit.TimeKeeper;
import pipeline.ExceptionHandler;
import pipeline.UFCGMainPipeline;
import envs.toolkit.FileStream;

import entity.BlockProfileEntity;
import entity.MMseqsSearchResultEntity;
import entity.ProfilePredictionEntity;
import entity.QueryEntity;

import process.FastBlockSearchProcess;
import process.ContigDragProcess;
import process.GenePredictionProcess;
//import process.HmmsearchProcess;
import process.JsonBuildProcess;
import process.MMseqsEasySearchProcess;
import wrapper.FastBlockSearchWrapper;
import wrapper.AugustusWrapper;
//import wrapper.HmmsearchWrapper;
import wrapper.MMseqsWrapper;

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
		opts.addOption(null, "hh", false, "advanced helper route");
		opts.addOption("u", "inter", false, "interactive route");
		
		opts.addOption("i", "input", true, "input path");
		opts.addOption("o", "output", true, "output path");
		opts.addOption("s", "set", true, "set of genes to extract");
		opts.addOption("w", "write", true, "intermediate path");
		opts.addOption("k", "keep", true, "keep temp files");
		opts.addOption("f", "force", true, "force delete");
		opts.addOption("t", "thread",  true, "number of cpu threads");
		
		opts.addOption(null, "fastblocksearch", true, "fastBlockSearch binary");
		opts.addOption(null, "augustus", true, "AUGUSTUS binary");
//		opts.addOption(null, "hmmsearch", true, "hmmsearch binary");
		opts.addOption(null, "mmseqs", true, "MMseqs2 binary");
		
		opts.addOption("m", "metadata", true, "metadata path");
		opts.addOption(null, "info", true, "single file metadata information");
		opts.addOption("n", "intron", true, "include intron sequences");
		opts.addOption(null, "modelpath", true, "gene profile path");
		opts.addOption(null, "seqpath", true, "gene sequence path");
		opts.addOption(null, "ppxcfg", true, "AUGUSTUS-PPX config path");
		
		opts.addOption(null, "fbscutoff", true, "fastBlockSearch cutoff");
		opts.addOption(null, "fbshits", true, "fastBlockSearch hit count");
		opts.addOption(null, "augoffset", true, "AUGUSTUS prediction offset");
//		opts.addOption(null, "hmmscore", true, "hmmsearch score cutoff");
		opts.addOption(null, "evalue", true, "e-value cutoff");
		
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
		if(cmd.hasOption("v"))       GenericConfig.VERB = true;
		if(cmd.hasOption("u"))       GenericConfig.INTERACT = true;
		if(cmd.hasOption("notime"))  GenericConfig.TSTAMP = false;
		if(cmd.hasOption("nocolor")) GenericConfig.NOCOLOR = true;
		if(cmd.hasOption("h"))       return -1;
		if(cmd.hasOption("hh"))	     return -2;
		if(cmd.hasOption("f")) if(!cmd.getOptionValue("f").equals("0")) GenericConfig.FORCE = true;
		
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
		
		if(cmd.hasOption("s"))
			GenericConfig.setGeneset(cmd.getOptionValue("s"));
		if(GenericConfig.solveGeneset() != 0) {
			ExceptionHandler.pass(GenericConfig.GENESET);
			ExceptionHandler.handle(ExceptionHandler.INVALID_GENE_SET);
		}
		if(GenericConfig.BUSCO)
			if(GenericConfig.getBuscos() != 0)
				ExceptionHandler.handle(ExceptionHandler.BUSCO_UNSOLVED);
		
		if(cmd.hasOption("w"))
			PathConfig.setTempPath(cmd.getOptionValue("w"));
		if(cmd.hasOption("k"))
			if(!cmd.getOptionValue("k").equals("0"))
				PathConfig.TempIsCustom = true;
		if(cmd.hasOption("t"))
			GenericConfig.setThreadPoolSize(cmd.getOptionValue("t"));
		
		/* parse dependency options */
		if(cmd.hasOption("fastblocksearch"))
			PathConfig.setFastBlockSearchPath(cmd.getOptionValue("fastblocksearch"));
		if(cmd.hasOption("augustus"))
			PathConfig.setAugustusPath(cmd.getOptionValue("augustus"));
//		if(cmd.hasOption("hmmsearch"))
//			PathConfig.setHmmsearchPath(cmd.getOptionValue("hmmsearch"));
		if(cmd.hasOption("mmseqs"))
			PathConfig.setMMseqsPath(cmd.getOptionValue("mmseqs"));
		
		/* parse configuration options */
		if(cmd.hasOption("m"))
			PathConfig.setMetaPath(cmd.getOptionValue("m"));
		if(cmd.hasOption("n")) {
			if(cmd.getOptionValue("n").equals("0")) {
				Prompt.talk("Excluding intron to the result DNA sequences.");
				GenericConfig.INTRON = false;
			}
		}
		if(cmd.hasOption("info")) {
			// check confilct
			if(cmd.hasOption("m") || PathConfig.InputIsFolder) ExceptionHandler.handle(ExceptionHandler.METAINFO_CONFLICT);
			PathConfig.MetaString = cmd.getOptionValue("info");
		}
		
		if(cmd.hasOption("modelpath")) {
			PathConfig.setModelPath(cmd.getOptionValue("modelpath"));
		}
		if(cmd.hasOption("seqpath")) {
			PathConfig.setSeqPath(cmd.getOptionValue("seqpath"));
		}
		
		if(cmd.hasOption("ppxcfg"))
			PathConfig.setAugustusConfig(cmd.getOptionValue("ppxcfg"));
		
		/* parse advanced options */
		if(cmd.hasOption("fbscutoff"))
			GenericConfig.setFastBlockSearchCutoff(cmd.getOptionValue("fbscutoff"));
		if(cmd.hasOption("fbshits"))
			GenericConfig.setFastBlockSearchHits(cmd.getOptionValue("fbshits"));
		if(cmd.hasOption("augoffset"))
			GenericConfig.setAugustusPredictionOffset(cmd.getOptionValue("augoffset"));
//		if(cmd.hasOption("hmmscore"))
//			GenericConfig.setHmmsearchScoreCutoff(cmd.getOptionValue("hmmscore"));
		if(cmd.hasOption("evalue"))
			GenericConfig.setEvalueCutoff(cmd.getOptionValue("evalue"));
//		if(cmd.hasOption("corelist"))
//			GenericConfig.setCustomCoreList(cmd.getOptionValue("corelist"));
		
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
//		if(!HmmsearchWrapper.solve()) {
//			ExceptionHandler.pass(PathConfig.HmmsearchPath);
//			ExceptionHandler.handle(ExceptionHandler.DEPENDENCY_UNSOLVED);
//		}
		if(!MMseqsWrapper.solve()) {
			ExceptionHandler.pass(PathConfig.MMseqsPath);
			ExceptionHandler.handle(ExceptionHandler.DEPENDENCY_UNSOLVED);
		}
		if(!PathConfig.checkModelPath()) {
			ExceptionHandler.handle(ExceptionHandler.INVALID_MODEL_PATH);
		}
		if(!PathConfig.checkSeqPath()) {
			ExceptionHandler.handle(ExceptionHandler.INVALID_SEQ_PATH);
		}
		
		Prompt.talk(ANSIHandler.wrapper("SUCCESS", 'g') + " : Dependency solving");
	}
	
	/* Manual route; exit with status 0 */
	private static void printManual() {
		/*
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
				"    -n, --intron          : Include introns from the predicted ORFs to the result sequences\n" +
				ANSIHandler.wrapper(
						"        Note. Including introns may improve the resolution of intra-genus or intra-species taxonomy.\n"
				, 'B') + 
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
		*/
		
		System.out.println(ANSIHandler.wrapper(" UFCG - profile", 'G'));
		System.out.println(ANSIHandler.wrapper(" Extract UFCG profile from Fungal whole genome sequences", 'g'));
		System.out.println("");
	
		System.out.println(ANSIHandler.wrapper("\n INTERACTIVE MODE :", 'Y') + " java -jar UFCG.jar profile -u");
		System.out.println(ANSIHandler.wrapper(" ONE-LINER MODE   :", 'Y') + " java -jar UFCG.jar profile -i <INPUT> -o <OUTPUT> [...]");
		System.out.println("");
		
		System.out.println(ANSIHandler.wrapper("\n Required options", 'Y'));
		System.out.println(ANSIHandler.wrapper(" Argument       Description", 'c'));
		System.out.println(ANSIHandler.wrapper(" -i STR         Input directory containing fungal genome assemblies", 'x'));
		System.out.println(ANSIHandler.wrapper(" -o STR         Output directory to store the result files", 'x'));
		System.out.println("");
		
		System.out.println(ANSIHandler.wrapper("\n Runtime configurations", 'y'));
		System.out.println(ANSIHandler.wrapper(" Argument       Description", 'c'));
		System.out.println(ANSIHandler.wrapper(" -s STR         Set of markers to extract - see advanced options for details [PRO]", 'x'));
		System.out.println(ANSIHandler.wrapper(" -w STR         Directory to write the temporary files [/tmp]", 'x'));
		System.out.println(ANSIHandler.wrapper(" -k BOOL        Keep the temporary products [0]", 'x'));
		System.out.println(ANSIHandler.wrapper(" -f BOOL        Force to overwrite the existing files [0]", 'x'));
		System.out.println(ANSIHandler.wrapper(" -t INT         Number of CPU threads to use [1]", 'x'));
		System.out.println(ANSIHandler.wrapper(" -m STR         File to the list containing metadata", 'x'));
		System.out.println(ANSIHandler.wrapper(" -n BOOL        Include introns from the predicted ORFs to the result sequences [1]", 'x'));
		System.out.println("");
		
		UFCGMainPipeline.printGeneral();
		
		System.out.println(" To see the advanced options, run with \"profile -hh\".\n");
		
		System.exit(0);
	}
	
	public static void printManualAdvanced() {
		System.out.println(ANSIHandler.wrapper(" UFCG - profile", 'G'));
		System.out.println(ANSIHandler.wrapper(" Extract UFCG profile from Fungal whole genome sequences", 'g'));
		System.out.println("");
		
		System.out.println(ANSIHandler.wrapper("\n Defining set of markers", 'y'));
		System.out.println(ANSIHandler.wrapper(" Name      Description", 'c'));
		System.out.println(ANSIHandler.wrapper(" NUC       Extract nucleotide marker sequences (Partial SSU/ITS1/5.8S/ITS2/Partial LSU)", 'x'));
		System.out.println(ANSIHandler.wrapper(" PRO       Extract protein marker sequences (Run " + ANSIHandler.wrapper("java -jar UFCG.jar --core", 'B') + " to see the full list)", 'x'));
		System.out.println(ANSIHandler.wrapper(" BUSCO     Extract BUSCO sequences (758 orthologs from fungi_odb10)", 'x'));
		System.out.println("");
		System.out.println(ANSIHandler.wrapper(" * Provide a comma-separated string consists of following sets (ex: NUC,PRO / PRO,BUSCO etc.)", 'x'));
		System.out.println(ANSIHandler.wrapper(" * Use specific gene names to extract custom set of markers (ex: ACT1,TEF1,TUB1 / NUC,CMD1,RPB2)", 'x'));
		System.out.println("");
		
		System.out.println(ANSIHandler.wrapper("\n Dependencies", 'y'));
		System.out.println(ANSIHandler.wrapper(" Argument                 Description", 'c'));
		System.out.println(ANSIHandler.wrapper(" --fastblocksearch STR    Path to fastBlockSearch binary [fastBlockSearch]", 'x'));
		System.out.println(ANSIHandler.wrapper(" --augustus STR           Path to AUGUSTUS binary [augustus]", 'x'));
		System.out.println(ANSIHandler.wrapper(" --mmseqs STR             Path to MMseqs2 binary [mmseqs]", 'x'));
		System.out.println("");
		
		System.out.println(ANSIHandler.wrapper("\n Advanced options", 'y'));
		System.out.println(ANSIHandler.wrapper(" Argument                 Description", 'c'));
		System.out.println(ANSIHandler.wrapper(" --info STR               Comma-separated metadata string for a single file input", 'x'));
		System.out.println(ANSIHandler.wrapper(" --modelpath STR          Path to the directory containing gene block profile models [./config/model]", 'x'));
		System.out.println(ANSIHandler.wrapper(" --seqpath STR            Path to the directory containing gene sequences [./config/seq]", 'x'));
		System.out.println(ANSIHandler.wrapper(" --ppxcfg STR             Path to the AUGUSTUS-PPX config file [./config/ppx.cfg]", 'x'));
		System.out.println(ANSIHandler.wrapper(" --fbscutoff FLOAT        Cutoff value for fastBlockSearch process [0.5]", 'x'));
		System.out.println(ANSIHandler.wrapper(" --fbshits INT            Use this amount of top hits from fastBlockSearch results [5]", 'x'));
		System.out.println(ANSIHandler.wrapper(" --augoffset INT          Prediction offset window size for AUGUSTUS process [10000]", 'x'));
		System.out.println(ANSIHandler.wrapper(" --evalue FLOAT           E-value cutoff for validation [1e-30]", 'x'));
		System.out.println("");
		
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
		
		// --set
		while(!proceed) {
			Prompt.print_nnc("Enter the set of genes to use (--set) : ");
			buf = stream.readLine();
			if(buf.length() == 0) continue;
			GenericConfig.setGeneset(buf);
			if(GenericConfig.solveGeneset() == 0) {
				proceed = true;
				command += " --set " + buf;
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
/*		
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
*/		
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
		
		/* locate config/model directory; request custom folder if failed */
		// --modelpath
		String ptype = Shell.exec("file -b " + jarPath + "config/model/")[0];
		boolean flag = ptype.contains("directory") && !ptype.contains("cannot");
		if(!flag) {
			Prompt.print("Default core gene profile directory not found.");
			while(!proceed) {
				Prompt.print_nnc("Enter your custom core gene profile directory (--modelpath) : ");
				buf = stream.readLine();
				if(buf.length() == 0) continue;
				if(PathConfig.setModelPath(buf) == 0) {
					proceed = true;
					command += " --modelpath " + buf;
				}
			}
			proceed = false;
		}
		
		/* profile directory validation */
		if(!PathConfig.checkModelPath()) {
			ExceptionHandler.handle(ExceptionHandler.INVALID_MODEL_PATH);
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
*/			
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
			case -2: printManualAdvanced();
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
				
				List<ProfilePredictionEntity> pps = new ArrayList<ProfilePredictionEntity>();
				/* Step 1. Nucleotide barcode prediction */
				if(GenericConfig.NUC) {
					Prompt.print("Extracting nucleotide markers...");
					
					// Run MMseqs easy-search
					MMseqsEasySearchProcess.setTask("ITS");
					MMseqsSearchResultEntity res = MMseqsEasySearchProcess.search(
							PathConfig.SeqPath + "nuc" + File.separator + "ITS.fa",
							PathConfig.InputIsFolder ? PathConfig.InputPath + GenericConfig.FILENAME : PathConfig.InputPath,
							PathConfig.TempPath, 500, 500, true, GenericConfig.ThreadPoolSize);
					res.reduce();
					ProfilePredictionEntity pp = MMseqsEasySearchProcess.parse(res);
					res.remove();
					if(pp.nseq() > 0) MMseqsEasySearchProcess.validate(pp, PathConfig.SeqPath + "nuc" + File.separator + "ITS.fa", PathConfig.TempPath, GenericConfig.ThreadPoolSize);
					pps.add(pp);
					
					if(pp.valid()) Prompt.print(ANSIHandler.wrapper("SUCCESS", 'g') + " : ITS sequence successfully extracted.");
					else Prompt.print(ANSIHandler.wrapper("FAILED", 'r') + " : ITS sequence not found.");
				}
				
				/* Step 2. Protein barcode prediction */
				if(GenericConfig.PRO) {
					Prompt.print("Extracting protein markers...");
					GenericConfig.setQueryGenes(GenericConfig.FCG, GenericConfig.TARGET_PRO);
					
					/* Dynamic progress prompt setup */
					initProg();
					printProg();
					
					/* Iterate through genes and run extract session
					 * Single iteration consists of three phases:
					 * 		1. SEARCH -> 2. PREDICT -> 3. VALIDATE
					 */
					/* Multithreading with ExecutorService (from ver 0.3) */
					nSgl = 0; nMul = 0; nUid = 0;
					
					ExecutorService executorService = Executors.newFixedThreadPool(GenericConfig.ThreadPoolSize);
					List<Future<ProfilePredictionEntity>> futures = new ArrayList<Future<ProfilePredictionEntity>>();
					
					for(int g = 0; g < GenericConfig.QUERY_GENES.length; g++) {
						CreateProfile creator = new ProfileModule().new CreateProfile(g);
						futures.add(executorService.submit(creator));
						Thread.sleep(500);
					}
	
					executorService.shutdown();
					for(Future<ProfilePredictionEntity> future : futures) pps.add(future.get());
					
					Prompt.dynamic(ANSIHandler.wrapper(" DONE                ", 'g') + "\n");
					Prompt.print(String.format("RESULT : [Single: %s ; Duplicated: %s ; Missing: %s]",
							ANSIHandler.wrapper(nSgl, 'g'), ANSIHandler.wrapper(nMul, 'G'), ANSIHandler.wrapper(nUid, 'r')));
					
				//	for(String contig : contigs) FileStream.wipe(contig, true);
				//	contigs = new ArrayList<String>();
				}
				
				/* Step 3. BUSCO prediction */
				if(GenericConfig.BUSCO) {
					Prompt.print("Extracting BUSCOs...");
					GenericConfig.setQueryGenes(GenericConfig.BUSCOS, GenericConfig.TARGET_BUSCO);
					
					/* Dynamic progress prompt setup */
					initProg();
					printProg();
					
					/* Iterate through genes and run extract session
					 * Single iteration consists of three phases:
					 * 		1. SEARCH -> 2. PREDICT -> 3. VALIDATE
					 */
					/* Multithreading with ExecutorService (from ver 0.3) */
					nSgl = 0; nMul = 0; nUid = 0;
					
					ExecutorService executorService = Executors.newFixedThreadPool(GenericConfig.ThreadPoolSize);
					List<Future<ProfilePredictionEntity>> futures = new ArrayList<Future<ProfilePredictionEntity>>();
					
					for(int g = 0; g < GenericConfig.QUERY_GENES.length; g++) {
						CreateProfile creator = new ProfileModule().new CreateProfile(g);
						futures.add(executorService.submit(creator));
						Thread.sleep(500);
					}
	
					executorService.shutdown();
					for(Future<ProfilePredictionEntity> future : futures) pps.add(future.get());
					
					Prompt.dynamic(ANSIHandler.wrapper(" DONE               ", 'g') + "\n");
					Prompt.print(String.format("RESULT : [Single: %s ; Duplicated: %s ; Missing: %s]",
							ANSIHandler.wrapper(nSgl, 'g'), ANSIHandler.wrapper(nMul, 'G'), ANSIHandler.wrapper(nUid, 'r')));
					
					for(String contig : contigs) FileStream.wipe(contig, true);
					contigs = new ArrayList<String>();
				}
				
				/* Write the entire result on a single JSON file */
				String jsonPath = PathConfig.OutputPath + GenericConfig.ACCESS + ".ucg";
				Prompt.print("Writing results on : " + ANSIHandler.wrapper(jsonPath, 'y'));
				JsonBuildProcess.build(pps, jsonPath);
				
				if(!PathConfig.TempIsCustom) Prompt.talk("Cleaning temporary files up...");
				Prompt.test(FileStream.filesToWipe());
				FileStream.wipeOut();
			}
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
	static TimeKeeper tk = null;
	private static class Status {
		private String stat;
		private Integer proc; // 0: pending; 1: processing; 2: finished
		
		private Status() {
			this.stat = ANSIHandler.wrapper("X", 'K');
			this.proc = 0;
		}
		private void updateStat(String stat, int proc) {
			this.stat = stat;
			this.proc = proc;
		}
	}
	
	static void initProg() {
		progress.clear();
		for(int g = 0; g < GenericConfig.QUERY_GENES.length; g++) progress.add(new Status());
		tk = new TimeKeeper();
	}
	static void updateProg(int g, String ch, Integer proc) {
		progress.get(g).updateStat(ch, proc);
	}
	
	static void printProg() {
		if(GenericConfig.QUERY_GENES.length > 100) {
			printProgSimple();
			return;
		}
		
		String build = "PROGRESS : [";
		int fin = 0;
		
		try{
			for(Status s : progress) {
				build += s.stat;
				if(s.proc == 2) fin++;
			}
		}
		catch(java.util.ConcurrentModificationException cme) {
			return;
		}
		catch(NullPointerException npe) {
			return;
		}
		
		build += "]";
		build += " ETA : " + tk.eta(fin, progress.size()) + "        ";
		Prompt.dynamic("\r");
		Prompt.dynamicHeader(build);
	}
	
	static void printProgSimple() {
		String build = "PROGRESS : [";
		int pend = 0, proc = 0, fin = 0;
		try{
			for(Status s : progress) {
				switch(s.proc) {
				case 0: pend++; break;
				case 1: proc++; break;
				case 2: fin++; break;
				}
			}
			
			build += "Pending : " + ANSIHandler.wrapper(String.valueOf(pend), 'K');
			build += " / Processing : " + ANSIHandler.wrapper(String.valueOf(proc), 'Y');
			build += " / Finished : " + ANSIHandler.wrapper(String.valueOf(fin), 'G');
		}
		catch(java.util.ConcurrentModificationException cme) {
			return;
		}
		catch(NullPointerException npe) {
			return;
		}
		
		build += "]";
		build += " ETA : " + tk.eta(fin, progress.size()) + "        ";
		
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
		
		public ProfilePredictionEntity call() {
			try {
				/* Prepare gene and set prompt */
				String cg = GenericConfig.QUERY_GENES[g];
				String dir = null;
				switch(GenericConfig.TARGET) {
				case GenericConfig.TARGET_PRO: dir = "pro"; break;
				case GenericConfig.TARGET_BUSCO: dir = "busco"; break;
				}
				
	//			Prompt.dynamic(ANSIHandler.wrapper(String.format("%-6s", cg), 'Y') + " >> [" + progress);
				Prompt.talk("Extracting gene " + ANSIHandler.wrapper(cg, 'Y') + 
						String.format(" [%d/%d]", g+1, GenericConfig.QUERY_GENES.length) + " from the query genome...");
	
				/* Phase 1. SEARCH */
				updateProg(g, ANSIHandler.wrapper("S", 'p'), 1); printProg();
				Prompt.talk(ANSIHandler.wrapper("[Phase 1 : Searching]", 'p'));
				String seqPath = PathConfig.InputIsFolder ? PathConfig.InputPath + GenericConfig.FILENAME : PathConfig.InputPath;
				/* Run fastBlockSearch on assembly to find gene containing location (contig, position) */
				BlockProfileEntity bp = FastBlockSearchProcess.handle(seqPath, PathConfig.ModelPath + dir + File.separator + cg + ".hmm", cg);
				bp.reduce(GenericConfig.FastBlockSearchHits);
				/* Drag required contigs from assembly and store their paths */
				List<String> ctgPaths = ContigDragProcess.drag(seqPath, bp);
				
				/* Phase 2. PREDICT */
				updateProg(g, ANSIHandler.wrapper("P", 'c'), 1); printProg();
				Prompt.talk(ANSIHandler.wrapper("[Phase 2 : Prediction]", 'c'));
				/* Run AUGUSTUS on the block found in Phase 1 */
				ProfilePredictionEntity pp = GenePredictionProcess.blockPredict(bp, ctgPaths);
				
				/* Phase 3. VALIDATE */
				updateProg(g, ANSIHandler.wrapper("V", 'R'), 1); printProg();
				Prompt.talk(ANSIHandler.wrapper("[Phase 3 : Validation]", 'R'));
				/* Run hmmsearch and find target gene among the predicted genes */
				if(pp.nseq() > 0) MMseqsEasySearchProcess.validate(pp, PathConfig.SeqPath + dir + File.separator + cg + ".fa", PathConfig.TempPath, 1);
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
				
				updateProg(g, result, 2);
				printProg();
				return pp;
			} catch(Exception e) {
				ExceptionHandler.handle(e);
			}
			
			return null;
		}
	}
}
