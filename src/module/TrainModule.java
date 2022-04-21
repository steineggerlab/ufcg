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
import pipeline.UFCGMainPipeline;
import wrapper.AugustusWrapper;
import wrapper.FastBlockSearchWrapper;
import wrapper.MMseqsWrapper;

public class TrainModule {
	private static int parseArgument(String[] args) throws ParseException {
		/* option argument setup */
		Options opts = new Options();
		opts.addOption("h", "help", false, "helper route");
		
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

		Prompt.debug(ANSIHandler.wrapper("Developer mode activated.", 'Y'));
		Prompt.talk("Verbose option check.");
		if(GenericConfig.TSTAMP) Prompt.talk("Timestamp printing option check."); 
		
		/* parse general I/O options */
		
		
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
		
		Prompt.talk(ANSIHandler.wrapper("SUCCESS", 'g') + " : Dependency solving");
	}
	
	private static void printManual() {
		System.out.println(ANSIHandler.wrapper(" UFCG - train", 'G'));
		System.out.println(ANSIHandler.wrapper(" Train and generate sequence model of fungal markers", 'g'));
		System.out.println("");
		
		System.out.println(ANSIHandler.wrapper("\n USAGE :", 'Y') + " java -jar UFCG.jar train -i <MARKER> -g <GENOME> -o <OUTPUT> -s <TYPE> [...]");
		System.out.println("");
		
		System.out.println(ANSIHandler.wrapper("\n Required options", 'Y'));
		System.out.println(ANSIHandler.wrapper(" Argument       Description", 'c'));
		System.out.println(ANSIHandler.wrapper(" -i STR         Input marker sequences in FASTA format", 'x'));
		System.out.println(ANSIHandler.wrapper(" -g STR         Directory containing genome sequences in FASTA format", 'x'));
		System.out.println(ANSIHandler.wrapper(" -o STR         Output directory", 'x'));
		System.out.println(ANSIHandler.wrapper(" -s STR         Sequence type {NUC, PRO}", 'x'));
		System.out.println("");
		
		System.out.println(ANSIHandler.wrapper("\n Configurations", 'y'));
		System.out.println(ANSIHandler.wrapper(" Argument       Description", 'c'));
		System.out.println(ANSIHandler.wrapper(" -n INT         Number of training iteration; type 0 to iterate until convergence [0]", 'x'));
		System.out.println(ANSIHandler.wrapper(" -t STR         Directory to write temporary files [/tmp]", 'x'));
		System.out.println("");
		
		UFCGMainPipeline.printGeneral();
		
		System.out.println(ANSIHandler.wrapper("\n Following binaries should be on the enviornment PATH: ", 'y'));
		System.out.println(ANSIHandler.wrapper(" Binary               Required by", 'c'));
		System.out.println(ANSIHandler.wrapper(" fastBlockSearch      profile", 'x'));
		System.out.println(ANSIHandler.wrapper(" augustus             profile", 'x'));
		System.out.println(ANSIHandler.wrapper(" mmseqs               profile", 'x'));
		System.out.println(ANSIHandler.wrapper(" mafft                align", 'x'));
		System.out.println(ANSIHandler.wrapper(" prepareAlign         train", 'x'));
		System.out.println(ANSIHandler.wrapper(" msa2prfl.pl          train", 'x'));
		System.out.println("");
		
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
