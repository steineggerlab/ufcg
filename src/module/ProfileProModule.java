package module;

import java.io.File;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;

import entity.ProfilePredictionEntity;
import entity.QueryEntity;
import envs.config.GenericConfig;
import envs.config.PathConfig;
import envs.toolkit.ANSIHandler;
import envs.toolkit.FileStream;
import envs.toolkit.Prompt;
import envs.toolkit.TimeKeeper;
import pipeline.ExceptionHandler;
import pipeline.UFCGMainPipeline;
import process.JsonBuildProcess;
import process.MMseqsEasySearchProcess;
import wrapper.MMseqsWrapper;

public class ProfileProModule {
	static List<String> SEQS = null;
	
	private static int parseArgument(String[] args) throws ParseException {
		/* option argument setup */
		Options opts = new Options();
		opts.addOption("h", "help", false, "helper route");
		
		opts.addOption("i", "input", true, "input path");
		opts.addOption("o", "output", true, "output path");
		
//		opts.addOption("s", "set", true, "set of genes to extract");
		opts.addOption("w", "write", true, "intermediate path");
		opts.addOption("k", "keep", true, "keep temp files");
		opts.addOption("f", "force", true, "force delete");
		opts.addOption("t", "thread",  true, "number of cpu threads");

		opts.addOption(null, "mmseqs", true, "MMseqs2 binary");
		
		opts.addOption(null, "info", true, "single file metadata information");
		opts.addOption(null, "seqpath", true, "gene sequence path");

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
		
		if(cmd.hasOption("i"))
			PathConfig.setInputPath(cmd.getOptionValue("i"));
		else ExceptionHandler.handle(ExceptionHandler.NO_INPUT);
		if(cmd.hasOption("o"))
			PathConfig.setOutputPath(cmd.getOptionValue("o"));
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
		if(cmd.hasOption("mmseqs"))
			PathConfig.setMMseqsPath(cmd.getOptionValue("mmseqs"));
		
		/* parse configuration options */
		if(cmd.hasOption("info")) 
			PathConfig.MetaString = cmd.getOptionValue("info");
		
		if(cmd.hasOption("seqpath")) {
			PathConfig.setSeqPath(cmd.getOptionValue("seqpath"));
		}
		
		/* parse advanced options */
		if(cmd.hasOption("evalue"))
			GenericConfig.setEvalueCutoff(cmd.getOptionValue("evalue"));
		
		/* successfully parsed */
		Prompt.talk(ANSIHandler.wrapper("SUCCESS", 'g') + " : Option parsing");
		return 0;
	}
	
	private static void solveDependency() {
		Prompt.talk("Solving dependencies...");
		
		if(MMseqsWrapper.solve()) {
			ExceptionHandler.pass(PathConfig.MMseqsPath);
			ExceptionHandler.handle(ExceptionHandler.DEPENDENCY_UNSOLVED);
		}
		if(PathConfig.checkSeqPath()) {
			ExceptionHandler.handle(ExceptionHandler.INVALID_SEQ_PATH);
		}
		
		Prompt.talk(ANSIHandler.wrapper("SUCCESS", 'g') + " : Dependency solving");
	}
	
	private static void printManual() {
		System.out.println(ANSIHandler.wrapper(" UFCG - profile-pro", 'G'));
		System.out.println(ANSIHandler.wrapper(" Extract UFCG profile from Fungal proteome", 'g'));
		System.out.println();
		
		System.out.println(ANSIHandler.wrapper("\n USAGE :", 'Y') + " java -jar UFCG.jar profile-pro -i <INPUT> -o <OUTPUT> [...]");
		System.out.println();
		
		System.out.println(ANSIHandler.wrapper("\n Required options", 'Y'));
		System.out.println(ANSIHandler.wrapper(" Argument       Description", 'c'));
		System.out.println(ANSIHandler.wrapper(" -i STR         File containing fungal protein sequences", 'x'));
		System.out.println(ANSIHandler.wrapper(" -o STR         Output directory", 'x'));
		System.out.println();
		
		System.out.println(ANSIHandler.wrapper("\n Runtime configurations", 'y'));
		System.out.println(ANSIHandler.wrapper(" Argument       Description", 'c'));
		System.out.println(ANSIHandler.wrapper(" -w STR         Directory to write the temporary files [/tmp]", 'x'));
		System.out.println(ANSIHandler.wrapper(" -k BOOL        Keep the temporary products [0]", 'x'));
		System.out.println(ANSIHandler.wrapper(" -f BOOL        Force to overwrite the existing files [0]", 'x'));
		System.out.println(ANSIHandler.wrapper(" -t INT         Number of CPU threads to use [1]", 'x'));
		System.out.println();
		
		System.out.println(ANSIHandler.wrapper("\n Advanced options", 'y'));
		System.out.println(ANSIHandler.wrapper(" Argument          Description", 'c'));
		System.out.println(ANSIHandler.wrapper(" --info STR        Comma-separated metadata string (Filename*, Label*, Accession*, Taxon, NCBI, Strain, Taxonomy)", 'x'));
		System.out.println(ANSIHandler.wrapper(" --mmseqs STR      Path to MMseqs2 binary [mmseqs]", 'x'));
		System.out.println(ANSIHandler.wrapper(" --seqpath STR     Path to the directory containing gene sequences [./config/seq]", 'x'));
		System.out.println(ANSIHandler.wrapper(" --evalue FLOAT    E-value cutoff for validation [1e-3]", 'x'));
		System.out.println();
		
		UFCGMainPipeline.printGeneral();
		
		System.out.println(ANSIHandler.wrapper("\n Notes", 'y'));
		System.out.println(" * Currently, profile-pro module is only capable of extracting UFCG markers. (-s PRO)");
		System.out.println();
		
		System.exit(0);
	}
	
	public static void run(String[] args) {
		try {
			switch(parseArgument(args)) {
			case -1: printManual();
			case  0: solveDependency(); break;
			default: System.exit(1);
			}
			
			// system setup
			GenericConfig.setQueryGenes(GenericConfig.FCG, GenericConfig.TARGET_PRO);
			QueryEntity.testIntegrity();
			QueryEntity query = QueryEntity.createQuery().get(0);
			query.activate();
			
			// import sequences
			SEQS = new ArrayList<>();
			FileStream inStream = new FileStream(PathConfig.InputPath, 'r');
			String buf;
			StringBuilder sbuf = new StringBuilder();
			while((buf = inStream.readLine()) != null) {
				if(buf.startsWith(">")) {
					if(sbuf.length() > 0) SEQS.add(sbuf.toString());
					sbuf = new StringBuilder();
				}
				else sbuf.append(buf);
			}
			inStream.close();

			if(!GenericConfig.VERB && !GenericConfig.QUIET) GenericConfig.DYNAMIC = true;
			initProg();
			printProg();
			
			List<ProfilePredictionEntity> pps = new ArrayList<>();
			nSgl = 0; nMul = 0; nUid = 0;
			
			ExecutorService executorService = Executors.newFixedThreadPool(GenericConfig.ThreadPoolSize);
			List<Future<ProfilePredictionEntity>> futures = new ArrayList<>();
			
			for(int g = 0; g < GenericConfig.QUERY_GENES.length; g++) {
				CreateProfile creator = new CreateProfile(g);
				futures.add(executorService.submit(creator));
				Thread.sleep(500);
			}

			executorService.shutdown();
			for(Future<ProfilePredictionEntity> future : futures) pps.add(future.get());
			
			if(!GenericConfig.QUIET) Prompt.dynamic(ANSIHandler.wrapper(" DONE ", 'g') + "\n");
			GenericConfig.DYNAMIC = false;

			Prompt.print(String.format("RESULT : [Single: %s ; Duplicated: %s ; Missing: %s]",
					ANSIHandler.wrapper(nSgl, 'g'), ANSIHandler.wrapper(nMul, 'G'), ANSIHandler.wrapper(nUid, 'r')));
			
			String jsonPath = PathConfig.OutputPath + GenericConfig.ACCESS + ".ucg";
			Prompt.print("Writing results on : " + ANSIHandler.wrapper(jsonPath, 'y'));
			JsonBuildProcess.build(pps, jsonPath);
			
			query.deactivate();
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
	
	static List<Status> progress = new LinkedList<>();
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
		StringBuilder build = new StringBuilder("PROGRESS : [");
		int fin = 0;
		
		try{
			for(Status s : progress) {
				build.append(s.stat);
				if(s.proc == 2) fin++;
			}
		}
		catch(ConcurrentModificationException | NullPointerException cme) {
			return;
		}

		build.append("]");
		build.append(" ETA : ").append(tk.eta(fin, progress.size()));
		if(!GenericConfig.QUIET) Prompt.dynamic("\r");
		if(!GenericConfig.QUIET) Prompt.dynamicHeader(build.toString());
	}
	
	static int nSgl, nMul, nUid;
	private static class CreateProfile implements Callable<ProfilePredictionEntity> {
		private final int g;
		public CreateProfile(int gid) {
			this.g = gid;
		}
		
		public ProfilePredictionEntity call() {
			try {
				/* Prepare gene and set prompt */
				String cg = GenericConfig.FCG[g];
				
				/* VALIDATE */
				updateProg(g, ANSIHandler.wrapper("V", 'R'), 1); printProg();
				Prompt.talk(ANSIHandler.wrapper("[Phase 3 : Validation]", 'R'));
				ProfilePredictionEntity pp = new ProfilePredictionEntity(cg, ProfilePredictionEntity.TYPE_PRO);
				for(String seq : SEQS) pp.addSeq(seq);
				MMseqsEasySearchProcess.validate(pp, PathConfig.SeqPath + "pro" + File.separator + cg + ".fa", PathConfig.TempPath, 1);
				
				/* Record obtained result */
				String result;
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
