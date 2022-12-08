package module;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;

import java.io.File;
import java.util.*;

import envs.config.GenericConfig;
import envs.config.PathConfig;
import envs.toolkit.ANSIHandler;
import envs.toolkit.Prompt;
import envs.toolkit.Shell;
import pipeline.ExceptionHandler;
import pipeline.UFCGMainPipeline;
//import wrapper.AugustusWrapper;
//import wrapper.FastBlockSearchWrapper;
//import wrapper.MMseqsWrapper;

public class TrainModule {
	static List<String> MARKERS = null, MNAMES = null;
	static Map<String, Integer> NMAP = null;
	static String INPUT = null, OUTPUT = null, TEMP = "/tmp/", SESSION_UID = GenericConfig.SESSION_UID;
	
	static final Integer TYPE_NUC = 0, TYPE_PRO = 1;
	static Integer TYPE = TYPE_PRO;
	static Integer N = -1;
	static String typeStr() {return Objects.equals(TYPE, TYPE_NUC) ? "nuc" : "pro";}

	private static int parseArgument(String[] args) throws ParseException {
		/* option argument setup */
		Options opts = new Options();
		opts.addOption("h", "help", false, "helper route");
		opts.addOption(null, "notime", false, "no timestamp with prompt");
		opts.addOption(null, "nocolor", false, "disable ANSI escapes");
		opts.addOption("v", "verbose", false, "verbosity");
		opts.addOption(null, "developer", false, "developer tool");
		opts.addOption(null, "test", false, "for test");
		
		opts.addOption("i", "input", true, "input marker directory");
		opts.addOption("g", "genome", true, "input genome directory");
		opts.addOption("o", "output", true, "output directory");
//		opts.addOption("s", "seq", true, "sequence type");
		opts.addOption("n", "niter", true, "number of iteration");
		opts.addOption("t", "threads", true, "number of threads");
		opts.addOption("w", "write", true, "tmp directory");
		opts.addOption("c", "checkpoint", true, "checkpoint directory");
		
		opts.addOption(null, "mmseqs", true, "mmseqs binary");
		
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
		
		if(cmd.hasOption("test")) GenericConfig.TEST = true;
		
		/* parse user friendly options; return ID and finish routine if necessary */
		if(cmd.hasOption("v"))       GenericConfig.VERB = true;
		if(cmd.hasOption("notime"))  GenericConfig.TSTAMP = false;
		if(cmd.hasOption("nocolor")) GenericConfig.NOCOLOR = true;
		if(cmd.hasOption("h"))       return -1;

		Prompt.debug(ANSIHandler.wrapper("Developer mode activated.", 'Y'));
		Prompt.talk("Verbose option check.");
		if(GenericConfig.TSTAMP) Prompt.talk("Timestamp printing option check."); 
		
		/* parse general I/O options */
		if(cmd.hasOption("i")) {
			File ifile = new File(cmd.getOptionValue("i"));
			if(!ifile.exists()) {
				ExceptionHandler.pass(ifile.getPath());
				ExceptionHandler.handle(ExceptionHandler.INVALID_DIRECTORY);
			} if(!ifile.isDirectory()) {
				ExceptionHandler.pass(ifile.getPath());
				ExceptionHandler.handle(ExceptionHandler.INVALID_DIRECTORY);
			}
			
			// list input directory and define marker sequences
			MARKERS = new LinkedList<>();
			MNAMES = new LinkedList<>();
			for(File fa : Objects.requireNonNull(ifile.listFiles())) {
				MARKERS.add(fa.getAbsolutePath());
				MNAMES.add(fa.getName().substring(0, fa.getName().lastIndexOf('.')));
			}
			NMAP = new HashMap<>();
			for(int i = 0; i < MNAMES.size(); i++) NMAP.put(MNAMES.get(i), i);
		} else ExceptionHandler.handle(ExceptionHandler.NO_INPUT);
		
		if(cmd.hasOption("g")) {
			PathConfig.setInputPath(cmd.getOptionValue("g"));
			INPUT = PathConfig.InputPath;
		}
		else {
			ExceptionHandler.pass("No reference genomes given.");
			ExceptionHandler.handle(ExceptionHandler.ERROR_WITH_MESSAGE);
		}
		
		if(cmd.hasOption("o")) {
			PathConfig.setOutputPath(cmd.getOptionValue("o"));
			OUTPUT = PathConfig.OutputPath;
		}
		else ExceptionHandler.handle(ExceptionHandler.NO_OUTPUT);
/*		
		if(cmd.hasOption("s")) {
			if(cmd.getOptionValue("s").equalsIgnoreCase("NUC")) TYPE = TYPE_NUC;
			if(cmd.getOptionValue("s").equalsIgnoreCase("PRO")) TYPE = TYPE_PRO;
		} else {
			ExceptionHandler.pass("Sequence type not given.");
			ExceptionHandler.handle(ExceptionHandler.ERROR_WITH_MESSAGE);
		}
		if(TYPE == null) {
			ExceptionHandler.pass("Invalid sequence type given.");
			ExceptionHandler.handle(ExceptionHandler.ERROR_WITH_MESSAGE);
		}
*/		
		if(cmd.hasOption("n")) {
			try {
				N = Integer.parseInt(cmd.getOptionValue("n"));
				if(N == 0) N--;
			} catch (NumberFormatException e) {
				ExceptionHandler.pass(cmd.getOptionValue("n"));
				ExceptionHandler.handle(ExceptionHandler.INVALID_VALUE);
			}
		}
		
		if(cmd.hasOption("t")) GenericConfig.setThreadPoolSize(cmd.getOptionValue("t"));
		if(cmd.hasOption("w")) {
			PathConfig.setTempPath(cmd.getOptionValue("w"));
			TEMP = PathConfig.TempPath;
		}
		if(cmd.hasOption("c")) SESSION_UID = new File(cmd.getOptionValue("c")).getName();
		
		if(cmd.hasOption("mmseqs")) PathConfig.setMMseqsPath(cmd.getOptionValue("mmseqs"));
		
		/* successfully parsed */
		Prompt.talk(ANSIHandler.wrapper("SUCCESS", 'g') + " : Option parsing");
		return 0;
	}
	
/*	private static void solveDependency() {
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
		
		Prompt.talk(ANSIHandler.wrapper("SUCCESS", 'g') + " : Dependency solving");
	}
*/
	private static void printManual() {
		System.out.println(ANSIHandler.wrapper(" UFCG - train", 'G'));
		System.out.println(ANSIHandler.wrapper(" Train and generate sequence model of fungal markers", 'g'));
		System.out.println();
		
		System.out.println(ANSIHandler.wrapper("\n USAGE :", 'Y') + " java -jar UFCG.jar train -i <MARKER> -g <GENOME> -o <OUTPUT> -s <TYPE> [...]");
		System.out.println();
		
		System.out.println(ANSIHandler.wrapper("\n Required options", 'Y'));
		System.out.println(ANSIHandler.wrapper(" Argument       Description", 'c'));
		System.out.println(ANSIHandler.wrapper(" -i STR         Directory containing marker sequences in FASTA format (should be able to build an MSA)", 'x'));
		System.out.println(ANSIHandler.wrapper(" -g STR         Directory containing reference genome sequences in FASTA format", 'x'));
		System.out.println(ANSIHandler.wrapper(" -o STR         Output directory", 'x'));
//		System.out.println(ANSIHandler.wrapper(" -s STR         Sequence type {NUC, PRO}", 'x'));
		System.out.println();
		
		System.out.println(ANSIHandler.wrapper("\n Configurations", 'y'));
		System.out.println(ANSIHandler.wrapper(" Argument       Description", 'c'));
		System.out.println(ANSIHandler.wrapper(" -n INT         Number of training iteration; 0 to iterate until convergence [0]", 'x'));
		System.out.println(ANSIHandler.wrapper(" -t INT         Number of CPU threads to use [1]", 'x'));
		System.out.println(ANSIHandler.wrapper(" -w STR         Directory to write temporary files [/tmp]", 'x'));
		System.out.println(ANSIHandler.wrapper(" -c STR         Checkpoint directory that contains precomputed files", 'x'));
		System.out.println();
		
		UFCGMainPipeline.printGeneral();
		
		System.out.println(ANSIHandler.wrapper("\n Following binaries should be on the enviornment PATH: ", 'y'));
		System.out.println(ANSIHandler.wrapper(" Binary               Required by", 'c'));
		System.out.println(ANSIHandler.wrapper(" fastBlockSearch      profile", 'x'));
		System.out.println(ANSIHandler.wrapper(" augustus             profile", 'x'));
		System.out.println(ANSIHandler.wrapper(" mmseqs               profile", 'x'));
		System.out.println(ANSIHandler.wrapper(" mafft                align", 'x'));
		System.out.println(ANSIHandler.wrapper(" prepareAlign         train", 'x'));
		System.out.println(ANSIHandler.wrapper(" msa2prfl.pl          train", 'x'));
		System.out.println();
		
		System.exit(0);
	}
	
	public static void run(String[] args) {
		try {
			switch(parseArgument(args)) {
			case -1: printManual();
			case  0: break;
			default: System.exit(1);
			}
			
			// initial setup
			int n = 0;
			if(!new File(PathConfig.TempPath + SESSION_UID).exists()) new File(PathConfig.TempPath + SESSION_UID).mkdir();
			else if(SESSION_UID.equals(GenericConfig.SESSION_UID)){
				ExceptionHandler.pass("Failed to create temporary directory.");
				ExceptionHandler.handle(ExceptionHandler.ERROR_WITH_MESSAGE);
			}
			String tmp = PathConfig.TempPath + SESSION_UID + File.separator + "profile";
			new File(tmp).mkdir();
			
			String[] finalSeqs = new String[MARKERS.size()], finalHmms = new String[MARKERS.size()];
			String[] seqs = new String[MARKERS.size()], hmms = new String[MARKERS.size()];
			Prompt.print("Producing initial models...");
			String dir = PathConfig.TempPath + SESSION_UID + File.separator + "iter" + n + File.separator;
			new File(dir).mkdir();
			new File(dir + typeStr()).mkdir();
			for(int i = 0; i < MARKERS.size(); i++) {
				String MARKER = MARKERS.get(i), MNAME = MNAMES.get(i);
				
				seqs[i] = dir + typeStr() + File.separator + MNAME + ".fa";
				hmms[i] = dir + typeStr() + File.separator + MNAME + ".hmm";
				
				if(!new File(seqs[i]).exists())
					Shell.exec(String.format("cp %s %s", MARKER, seqs[i]));
				if(!new File(dir + MNAME + ".ali.fa").exists())
					Shell.exec(String.format("mafft --auto --thread %d %s > %s", GenericConfig.ThreadPoolSize, seqs[i], dir + MNAME + ".ali.fa"));
				if(!new File(dir + MNAME + ".pa.fa").exists())
					Shell.exec(String.format("prepareAlign < %s > %s 2> /dev/null", dir + MNAME + ".ali.fa", dir + MNAME + ".pa.fa"));
				if(!new File(hmms[i]).exists())
					Shell.exec(String.format("msa2prfl.pl < %s > %s 2> /dev/null", dir + MNAME + ".pa.fa", hmms[i]));
			}
			
			// iteration
			while(n++ != N) {
				new File((dir + "ucg")).mkdir();
				Prompt.print("Running iteration " + n + "...");
				
				// store initial counts
				int[] scnts = new int[MARKERS.size()];
				for(int i = 0; i < MARKERS.size(); i++) {
					scnts[i] = Integer.parseInt(Shell.exec("grep '^>' " + seqs[i] + " | wc -l", true, 0)[0]);
					Prompt.talk(String.format("[ITER %d/%s; TASK %s] : %d sequences defined", n, N > 0 ? String.valueOf(N) : "inf", MNAMES.get(i), scnts[i]));
				}
				
				// run profile submodule
				String[] profileArgs = {
						"profile", 
						"-i", PathConfig.InputPath,
						"-o", dir + "ucg",
						"-t", String.valueOf(GenericConfig.ThreadPoolSize),
						"-w", tmp,
						"--seqpath", dir,
						"--modelpath", dir,
						"-s", String.join(",", MNAMES)
					};
				Prompt.talk(String.format("[ITER %d/%s] : Running profile submodule...", n, N > 0 ? String.valueOf(N) : "inf"));
				Prompt.debug(String.join(" ", profileArgs));
				ProfileModule.run(profileArgs);
					
				// run align submodule
				if(!new File(dir + "msa").exists()) {
					String[] alignArgs = {
							"align", 
							"-i", dir + "ucg",
							"-o", dir,
							"-n", "msa",
							"-a", "protein",
							"-t", String.valueOf(GenericConfig.ThreadPoolSize),
							"-f", "100"
						};
					Prompt.talk(String.format("[ITER %d/%s] : Running align submodule...", n, N > 0 ? String.valueOf(N) : "inf"));
					Prompt.debug(String.join(" ", alignArgs));
					AlignModule.run(alignArgs);
				}
				
				// reset paths
				if(!GenericConfig.DEV) Prompt.SUPPRESS = true; 
				PathConfig.setInputPath(INPUT);
				PathConfig.setOutputPath(OUTPUT);
				PathConfig.setTempPath(TEMP);
				PathConfig.MetaExists = false;
				Prompt.SUPPRESS = false; 
					
				// produce profile
				String next = PathConfig.TempPath + SESSION_UID + File.separator + "iter" + n + File.separator;
				new File(next).mkdir();
				new File(next + typeStr()).mkdir();
				
				List<Integer> remove = new LinkedList<>();
				for(int i = 0; i < MARKERS.size(); i++) {
					String MNAME = MNAMES.get(i);
					String nseq = next + typeStr() + File.separator + MNAME + ".fa";
					String nhmm = next + typeStr() + File.separator + MNAME + ".hmm";
					
					if(!new File(nseq).exists())
						Shell.exec(String.format("cp %s %s", dir + "msa" + File.separator + MNAME + "_pro.fasta", nseq));
					if(!new File(next + MNAME + ".ali.fa").exists())
						Shell.exec(String.format("cp %s %s", dir + "msa" + File.separator + "aligned_" + MNAME + ".fasta", next + MNAME + ".ali.fa"));
					if(!new File(next + MNAME + ".pa.fa").exists())
						Shell.exec(String.format("prepareAlign < %s > %s 2> /dev/null", next + MNAME + ".ali.fa", next + MNAME + ".pa.fa"));
					if(!new File(nhmm).exists())
						Shell.exec(String.format("msa2prfl.pl < %s > %s 2> /dev/null", next + MNAME + ".pa.fa", nhmm));
					
					// compare sequence count
					String grep = Shell.exec("grep '^>' " + nseq + " | wc -l", true, 0)[0];
					int ecnt;
					try{ 
						ecnt = Integer.parseInt(grep);
					} catch(NumberFormatException e) {
						ecnt = -1;
					}
					
					if(scnts[i] > ecnt) { // decreased
						Prompt.talk(String.format("[ITER %d/%s; TASK %s] : Sequence count decreased. Iteration revoked.", n, N > 0 ? String.valueOf(N) : "inf", MNAME));
						finalSeqs[NMAP.get(MNAME)] = seqs[i];
						finalHmms[NMAP.get(MNAME)] = hmms[i];
						remove.add(i);
					}
					else {
						if(scnts[i] == ecnt) { // converged
							Prompt.talk(String.format("[ITER %d/%s; TASK %s] : Sequence count converged.", n, N > 0 ? String.valueOf(N) : "inf", MNAME));
							remove.add(i);
						}
						finalSeqs[NMAP.get(MNAME)] = nseq;
						finalHmms[NMAP.get(MNAME)] = nhmm;
					}
				}
				
				// remove decreased/converged entries
				for(int i = remove.size() - 1; i >= 0; i--) {
					MARKERS.remove((int) remove.get(i));
					MNAMES.remove((int) remove.get(i));
				}
				if(MARKERS.size() == 0) break;
				
				// prepare next iteration
				seqs = new String[MARKERS.size()];
				hmms = new String[MARKERS.size()];
				for(int i = 0; i < MARKERS.size(); i++) {
					seqs[i] = next + typeStr() + File.separator + MNAMES.get(i) + ".fa";
					hmms[i] = next + typeStr() + File.separator + MNAMES.get(i) + ".hmm";
				}
				dir = next;
			}
				
			
			for(String MNAME : NMAP.keySet()) {
				Shell.exec(String.format("cp %s %s", finalSeqs[NMAP.get(MNAME)], PathConfig.OutputPath + MNAME + ".fa"));
				Shell.exec(String.format("cp %s %s", finalHmms[NMAP.get(MNAME)], PathConfig.OutputPath + MNAME + ".hmm"));
			}
			Shell.exec("rm -rf " + PathConfig.TempPath + SESSION_UID);
		}
		catch(Exception e) {
			/* Exception handling route; exit with status 1 */
			// e.printStackTrace();
			ExceptionHandler.handle(e);
		}
	}
}
