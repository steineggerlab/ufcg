package module;

import java.io.File;

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
import envs.toolkit.FileStream;
import envs.toolkit.Prompt;
import envs.toolkit.Shell;
import pipeline.ExceptionHandler;
import pipeline.UFCGMainPipeline;
import process.FastaHeaderClassifyProcess;
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
		assert cmd != null;
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
			if(cmd.getOptionValue("p").equals("1")) PAIRED = true;
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
		if(AugustusWrapper.checkConfigFile()) {
			ExceptionHandler.handle(ExceptionHandler.INVALID_PPX_CONFIG);
		}
		if(MMseqsWrapper.solve()) {
			ExceptionHandler.pass(PathConfig.MMseqsPath);
			ExceptionHandler.handle(ExceptionHandler.DEPENDENCY_UNSOLVED);
		}
		if(!TrinityWrapper.solve()) {
			ExceptionHandler.pass(PathConfig.TrinityPath);
			ExceptionHandler.handle(ExceptionHandler.DEPENDENCY_UNSOLVED);
		}
		if(PathConfig.checkModelPath()) {
			ExceptionHandler.handle(ExceptionHandler.INVALID_MODEL_PATH);
		}
		if(PathConfig.checkSeqPath()) {
			ExceptionHandler.handle(ExceptionHandler.INVALID_SEQ_PATH);
		}
		
		Prompt.talk(ANSIHandler.wrapper("SUCCESS", 'g') + " : Dependency solving");
	}
	
	private static void printManual() {
		System.out.println(ANSIHandler.wrapper(" UFCG - profile-rna", 'G'));
		System.out.println(ANSIHandler.wrapper(" Extract UFCG profile from Fungal RNA-seq reads", 'g'));
		System.out.println();
		
		System.out.println(ANSIHandler.wrapper("\n USAGE :", 'Y') + " ufcg profile-rna -p <PAIRED> -i <INPUT> -o <OUTPUT> [...]");
		System.out.println(ANSIHandler.wrapper(  "        ", 'Y') + " ufcg profile-rna -p 1 -l <LEFT> -r <RIGHT> -o <OUTPUT> [...]");
		System.out.println();
		
		System.out.println(ANSIHandler.wrapper("\n Required options", 'Y'));
		System.out.println(ANSIHandler.wrapper(" Argument       Description", 'c'));
		System.out.println(ANSIHandler.wrapper(" -p INT         Paired or unpaired reads (paired: 1; unpaired: 0)", 'x'));
		System.out.println(ANSIHandler.wrapper(" -i STR *       File containing single reads in FASTQ/FASTA format", 'x'));
		System.out.println(ANSIHandler.wrapper(" -l, -r STR *   File containing left/right reads in FASTQ/FASTA format", 'x'));
		System.out.println(ANSIHandler.wrapper(" -o STR         Output directory", 'x'));
		System.out.println(ANSIHandler.wrapper(" * Select one of above", 'x'));
		System.out.println();
		
		System.out.println(ANSIHandler.wrapper("\n Configurations", 'y'));
		System.out.println(ANSIHandler.wrapper(" Argument       Description", 'c'));
		System.out.println(ANSIHandler.wrapper(" --info STR     Comma-separated metadata string (Filename*, Label*, Accession*, Taxon, NCBI, Strain, Taxonomy)", 'x'));
		System.out.println(ANSIHandler.wrapper(" --trinity STR  Path to Trinity binary [Trinity]", 'x'));
		System.out.println();
		
		UFCGMainPipeline.printGeneral();
		
		System.out.println(ANSIHandler.wrapper("\n Notes", 'y'));
		System.out.println(" * Currently, profile-rna module is only capable of extracting protein markers. (-s PRO)");
		System.out.println(" * Other options except -s, -m are shared with the profile module. To check them, run with \"profile -h\".\n");
		
		System.exit(0);
	}
	
	public static void run(String[] args) {
		try {
			switch(parseArgument(args)) {
			case -1: printManual();
			case  0: solveDependency(); break;
			default: System.exit(1);
			}
			
			// mmseqs createdb
			Prompt.print(ANSIHandler.wrapper("STEP 1/5", 'Y') + " : Creating MMseqs2 databases...");
			String pdb = PathConfig.EnvironmentPath + "config/db/mm_pro";
			String sdb = PathConfig.TempPath + GenericConfig.SESSION_UID + "_rna";
			MMseqsWrapper mm;
			if(SINGLE) {
				mm = new MMseqsWrapper();
				mm.setCreatedb(PATHL, sdb);
				mm.exec();
			}
			else {
				String ldb = PathConfig.TempPath + PATHL.substring(PATHL.lastIndexOf(File.separator) + 1);
				String rdb = PathConfig.TempPath + PATHR.substring(PATHR.lastIndexOf(File.separator) + 1);
				
				mm = new MMseqsWrapper();
				mm.setCreatedb(PATHL, ldb);
				mm.exec();
				mm = new MMseqsWrapper();
				mm.setCreatedb(PATHR, rdb);
				mm.exec();
				mm = new MMseqsWrapper();
				mm.setConcatdbs(ldb, rdb, sdb);
				mm.setThreads(GenericConfig.ThreadPoolSize);
				mm.exec();
				mm = new MMseqsWrapper();
				mm.setRmdb(ldb);
				mm.exec();
				mm = new MMseqsWrapper();
				mm.setRmdb(rdb);
				mm.exec();
			}
			
			// mmseqs search
			Prompt.print(ANSIHandler.wrapper("STEP 2/5", 'Y') + " : Running MMseqs2 search...");
			String adb = PathConfig.TempPath + GenericConfig.SESSION_UID + "_ali";
			mm = new MMseqsWrapper();
			mm.setSearch(pdb, sdb, adb, PathConfig.TempPath);
			mm.setSens(1.0);
			mm.setThreads(GenericConfig.ThreadPoolSize);
			mm.exec();
			
			// mmseqs convertails
			String out = PathConfig.TempPath + GenericConfig.SESSION_UID + ".m8";
			mm = new MMseqsWrapper();
			mm.setConvertalis(pdb, sdb, adb, out);
			mm.setThreads(GenericConfig.ThreadPoolSize);
			mm.setFormatOutput("target");
			mm.exec();
			mm = new MMseqsWrapper();
			mm.setRmdb(adb);
			mm.exec();
			
			// mmseqs convert2fasta
			String sfa = PathConfig.TempPath + GenericConfig.SESSION_UID + ".fa";
			mm = new MMseqsWrapper();
			mm.setConvert2Fasta(sdb, sfa);
			mm.exec();
			mm = new MMseqsWrapper();
			mm.setRmdb(sdb);
			mm.exec();
			mm = new MMseqsWrapper();
			mm.setRmdb(sdb + "_h");
			mm.exec();
			
			// extract FASTA entries
			Prompt.print(ANSIHandler.wrapper("STEP 3/5", 'Y') + " : Extracting relevant reads...");
			String head = PathConfig.TempPath + GenericConfig.SESSION_UID + ".head";
			String ofa = PathConfig.TempPath + GenericConfig.SESSION_UID + ".ext.fa";
			Shell.exec(String.format("sort %s | uniq > %s", out, head));
			FileStream.wipe(out, true);
			FastaHeaderClassifyProcess.classify(sfa, head, ofa, true);
			FileStream.wipe(sfa, true);
			FileStream.wipe(head, true);
			
			// run Trinity
			Prompt.print(ANSIHandler.wrapper("STEP 4/5", 'Y') + " : Running Trinity RNA-seq assembly...");
			String trinity = PathConfig.TempPath + GenericConfig.SESSION_UID + "_trinity";
			String afa = PathConfig.TempPath + GenericConfig.SESSION_UID + ".fa";
			TrinityWrapper.runSingle("fa", ofa, GenericConfig.MEM_SIZE / GenericConfig.CPU_COUNT * GenericConfig.ThreadPoolSize, GenericConfig.ThreadPoolSize, trinity, PAIRED);
			FileStream.wipe(ofa, true);
			Shell.exec(String.format("cp %s %s", trinity + File.separator + "Trinity.fasta", afa));
			Shell.exec(String.format("rm -rf %s", trinity));
			
			Prompt.print(ANSIHandler.wrapper("STEP 5/5", 'Y') + " : Launching profile submodule...");
			String meta;
			if(PathConfig.MetaString == null) meta = String.format("%s,%s,%s,,,,", afa, GenericConfig.SESSION_UID, GenericConfig.SESSION_UID);
			else meta = afa + PathConfig.MetaString.substring(PathConfig.MetaString.indexOf(","));
			String[] profileArgs = {
					"profile",
					"-i", afa,
					"-o", PATHO,
					"--info", meta
			};
			ProfileModule.run(profileArgs);
			FileStream.wipe(afa, true);
		}
		catch(Exception e) {
			/* Exception handling route; exit with status 1 */
			// e.printStackTrace();
			ExceptionHandler.handle(e);
		} /*
		finally { 
			Prompt.print("Job finished. Terminating process.\n");
			System.exit(0);
		} */
	}
}
