package module;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;

import envs.config.GenericConfig;
import envs.config.PathConfig;
import envs.toolkit.ANSIHandler;
import envs.toolkit.Prompt;
import pipeline.ExceptionHandler;
import wrapper.AugustusWrapper;
import wrapper.FastBlockSearchWrapper;
import wrapper.MMseqsWrapper;
import wrapper.TrinityWrapper;

public class ProfileRnaModule {
	private static Boolean PAIRED = null, SINGLE = null;
	private static String PATHL = null, PATHR = null, PATHO = null;
	
	private static int parseArgument(String[] args) throws ParseException {
		/* option argument setup */
		Options opts = new Options();
		opts.addOption("h", "help", false, "helper route");
		
		opts.addOption("p", "paired", true, "paired or unpaired");
		opts.addOption("i", "input", true, "input path");
		opts.addOption("l", "left", true, "left path");
		opts.addOption("r", "right", true, "right path");
		opts.addOption("o", "output", true, "output path");
		
//		opts.addOption("s", "set", true, "set of genes to extract");
		opts.addOption("w", "write", true, "intermediate path");
		opts.addOption("k", "keep", true, "keep temp files");
		opts.addOption("f", "force", true, "force delete");
		opts.addOption("t", "thread",  true, "number of cpu threads");
		
		opts.addOption(null, "trinity", true, "Trinity binary");
		opts.addOption(null, "fastblocksearch", true, "fastBlockSearch binary");
		opts.addOption(null, "augustus", true, "AUGUSTUS binary");
		opts.addOption(null, "mmseqs", true, "MMseqs2 binary");
		
		opts.addOption(null, "info", true, "single file metadata information");
		opts.addOption("n", "intron", true, "include intron sequences");
		opts.addOption(null, "prflpath", true, "gene profile path");
		opts.addOption(null, "seqpath", true, "gene sequence path");
		opts.addOption(null, "ppxcfg", true, "AUGUSTUS-PPX config path");
		
		opts.addOption(null, "fbscutoff", true, "fastBlockSearch cutoff");
		opts.addOption(null, "fbshits", true, "fastBlockSearch hit count");
		opts.addOption(null, "augoffset", true, "AUGUSTUS prediction offset");
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
		if(cmd.hasOption("notime"))  GenericConfig.TSTAMP = false;
		if(cmd.hasOption("nocolor")) GenericConfig.NOCOLOR = true;
		if(cmd.hasOption("h"))       return -1;
		if(cmd.hasOption("f")) if(!cmd.getOptionValue("f").equals("0")) GenericConfig.FORCE = true;

		Prompt.debug(ANSIHandler.wrapper("Developer mode activated.", 'Y'));
		Prompt.talk("Verbose option check.");
		if(GenericConfig.TSTAMP) Prompt.talk("Timestamp printing option check."); 
		
		/* parse general I/O options */
		if(cmd.hasOption("p")) {
			if(cmd.getOptionValue("p").equals("0")) PAIRED = false;
			if(cmd.getOptionValue("p").equals("1")) PAIRED = false;
		}
		if(PAIRED == null) {
			ExceptionHandler.pass("Input type (paired/unpaired) must be specified using -p option.");
			ExceptionHandler.handle(ExceptionHandler.ERROR_WITH_MESSAGE);
		}
		
		if(cmd.hasOption("i")) {
			PathConfig.checkInputFile(cmd.getOptionValue("i"));
			SINGLE = true;
			PATHL = cmd.getOptionValue("i");
		}
		else if(cmd.hasOption("l") & cmd.hasOption("r")) {
			if(!PAIRED) {
				ExceptionHandler.pass("-l/-r option cannot be given with unpaired reads input.");
				ExceptionHandler.handle(ExceptionHandler.ERROR_WITH_MESSAGE);
			}
			PathConfig.checkInputFile(cmd.getOptionValue("l"));
			PathConfig.checkInputFile(cmd.getOptionValue("r"));
			SINGLE = false;
			PATHL = cmd.getOptionValue("l");
			PATHR = cmd.getOptionValue("r");
			
		}
		else ExceptionHandler.handle(ExceptionHandler.NO_INPUT);
		
		if(cmd.hasOption("o"))
			PATHO = cmd.getOptionValue("o");
		else ExceptionHandler.handle(ExceptionHandler.NO_OUTPUT);
		
/*		if(cmd.hasOption("s"))
			GenericConfig.setGeneset(cmd.getOptionValue("s"));
		if(GenericConfig.solveGeneset() != 0) {
			ExceptionHandler.pass(GenericConfig.GENESET);
			ExceptionHandler.handle(ExceptionHandler.INVALID_GENE_SET);
		}
		if(GenericConfig.BUSCO)
			if(GenericConfig.getBuscos() != 0)
				ExceptionHandler.handle(ExceptionHandler.BUSCO_UNSOLVED);
		*/
		if(cmd.hasOption("w"))
			PathConfig.setTempPath(cmd.getOptionValue("w"));
		if(cmd.hasOption("k"))
			if(!cmd.getOptionValue("k").equals("0"))
				PathConfig.TempIsCustom = true;
		if(cmd.hasOption("t"))
			GenericConfig.setThreadPoolSize(cmd.getOptionValue("t"));
		
		/* parse dependency options */
		if(cmd.hasOption("trinity"))
			PathConfig.setTrinityPath(cmd.getOptionValue("trinity"));
		if(cmd.hasOption("fastblocksearch"))
			PathConfig.setFastBlockSearchPath(cmd.getOptionValue("fastblocksearch"));
		if(cmd.hasOption("augustus"))
			PathConfig.setAugustusPath(cmd.getOptionValue("augustus"));
		if(cmd.hasOption("mmseqs"))
			PathConfig.setMMseqsPath(cmd.getOptionValue("mmseqs"));
		
		/* parse configuration options */
		if(cmd.hasOption("n")) {
			if(cmd.getOptionValue("n").equals("0")) {
				Prompt.talk("Excluding intron to the result DNA sequences.");
				GenericConfig.INTRON = false;
			}
		}
		if(cmd.hasOption("info")) 
			PathConfig.MetaString = cmd.getOptionValue("info");
		
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
		if(cmd.hasOption("evalue"))
			GenericConfig.setEvalueCutoff(cmd.getOptionValue("evalue"));
		
		/* successfully parsed */
		Prompt.talk(ANSIHandler.wrapper("SUCCESS", 'g') + " : Option parsing");
		return 0;
	}
	
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
		if(!MMseqsWrapper.solve()) {
			ExceptionHandler.pass(PathConfig.MMseqsPath);
			ExceptionHandler.handle(ExceptionHandler.DEPENDENCY_UNSOLVED);
		}
		if(!TrinityWrapper.solve()) {
			ExceptionHandler.pass(PathConfig.TrinityPath);
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
	
	private static void printManual() {
		System.out.println(ANSIHandler.wrapper(" UFCG - profile-rna", 'G'));
		System.out.println(ANSIHandler.wrapper(" Extract UFCG profile from Fungal RNA-seq reads", 'g'));
		System.out.println("");
		
		System.out.println(ANSIHandler.wrapper("\n USAGE :", 'Y') + " java -jar UFCG.jar profile-rna -p <PAIRED> -i <INPUT> -o <OUTPUT> [...]");
		System.out.println(ANSIHandler.wrapper(  "        ", 'Y') + " java -jar UFCG.jar profile-rna -p 1 -l <LEFT> -r <RIGHT> -o <OUTPUT> [...]");
		System.out.println("");
		
		System.out.println(ANSIHandler.wrapper("\n Required options", 'Y'));
		System.out.println(ANSIHandler.wrapper(" Argument\t\tDescription", 'c'));
		System.out.println(String.format(" %s\t\t\t%s", "-p INT", "Paired or unpaired reads (paired: 1; unpaired: 0)"));
		System.out.println(String.format(" %s\t\t%s", "-i STR *", "File containing single reads in FASTQ/FASTA format"));
		System.out.println(String.format(" %s\t%s", "-l STR -r STR *", "File containing left/right reads in FASTQ/FASTA format"));
		System.out.println(String.format(" %s\t\t\t%s", "-o STR", "Output gene profile path (.ucg extension is recommended)"));
		System.out.println(String.format("* Select one of above"));
		System.out.println("");
		
		System.out.println(ANSIHandler.wrapper("\n Configurations", 'y'));
		System.out.println(ANSIHandler.wrapper(" Argument\t\tDescription", 'c'));
		System.out.println(String.format(" %s\t\t%s", "--info STR", "Comma-separated metadata string (Filename*, Label*, Accession*, Taxon, NCBI, Strain, Taxonomy) [Filename,Filename,Filename]"));
		System.out.println(String.format(" %s\t\t%s", "--trinity STR", "Path to Trinity binary [Trinity]"));
		System.out.println("");
		
		System.out.println(ANSIHandler.wrapper("\n Notes", 'y'));
		System.out.println(" Currently, profile-rna module is only capable of extracting protein markers. (-s PRO)");
		System.out.println(" Other options except -s, -m are shared with the profile module. To check them, run with \"profile -h\".\n");
		
		System.exit(0);
	}
	
	public static void run(String[] args) {
		try {
			switch(parseArgument(args)) {
			case -1: printManual();
			case  0: solveDependency(); break;
			default: System.exit(1);
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
}
