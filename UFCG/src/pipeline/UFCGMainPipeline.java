/*
 *    __  __ _____ _____ _____
 *   / / / // ___// ___// ___/
 *  / / / // /_  / /   / / __
 * / /_/ // __/ / /___/ /_/ /
 * \____//_/    \____/\____/
 *
 *                                 
 * UFCG : Profiling Fungi Genome with Up-to-date Universal Core Gene
 * 
 * Developed by Dongwook Daniel Kim
 * Steinegger Lab, Seoul National University
 * 
 * Contact : endix1029@gmail.com
 * Correspondence :	jchun@snu.ac.kr, martin.steinegger@snu.ac.kr
 * 
 */

package pipeline;

import envs.config.GenericConfig;
import envs.config.PathConfig;
import envs.toolkit.ANSIHandler;
import envs.toolkit.Prompt;
import envs.toolkit.FileStream;

import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.commons.cli.MissingArgumentException;

import sun.misc.Signal;
import sun.misc.SignalHandler;

@SuppressWarnings("restriction")
public class UFCGMainPipeline {
	public static final String VERSION = "1.0";
	public static final String RELEASE_DATE = "Jan. 2022";
	
	/* Print the UFCG logo with version number */
	private static void printLogo() {
		/* No longer print timestamps even if timestamp option is activated. */
//		String header = GenericConfig.TSTAMP ?
//				ANSIHandler.wrapper("[" + TimeKeeper.timeStamp() + "] ", 'c') : " ";
		String header = "";
		System.out.println(header + ANSIHandler.wrapper("    __  __ _____ _____ _____",    'G'));
		System.out.println(header + ANSIHandler.wrapper("   / / / // ___// ___// ___/",    'G'));
		System.out.println(header + ANSIHandler.wrapper("  / / / // /_  / /   / / __",  'G'));
		System.out.println(header + ANSIHandler.wrapper(" / /_/ // __/ / /___/ /_/ /", 'G'));
		System.out.print  (header + ANSIHandler.wrapper(" \\____//_/    \\____/\\____/",  'G'));
		System.out.println(ANSIHandler.wrapper(" v" + VERSION, 'g'));
//		System.out.println(header + ANSIHandler.wrapper("                          /_/   ",      'Y') 
//			+ ANSIHandler.wrapper("by LEB, SNU", 'B'));
//		System.out.println(ANSIHandler.wrapper(" Profiling Fungi with " + String.valueOf(GenericConfig.FCG_REF.length) + 
//				" Up-to-date Fungal Core Genes\n\n", 'Y')
//				+ ANSIHandler.wrapper(" Ver. " + VERSION + " (Released: " + RELEASE_DATE + ")\n\n", 'g')
//				);
		System.out.println("\n");
	}
	
	private static int parseModule(String[] args) {
		Prompt.debug(args[0] + " " + args[1]);
		return 0;
	}
	
	/* Argument parsing route */
	private static int parseArgument(String[] args) throws ParseException {
		/* option argument setup */
		Options opts = new Options();
		
		opts.addOption("h", "help", false, "helper route");
		opts.addOption(null, "info", false, "information route");
		opts.addOption(null, "version", false, "information route");
		opts.addOption(null, "core", false, "core gene route");
		opts.addOption(null, "timestamp", false, "print timestamp with prompt");
		opts.addOption(null, "nocolor", false, "disable ANSI escapes");
		opts.addOption("v", "verbose", false, "verbosity");
		opts.addOption(null, "developer", false, "developer tool");
		opts.addOption(null, "test", false, "for test");
		
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
		if(cmd.hasOption("test")) return -4;
		
		/* parse user friendly options; return ID and finish routine if necessary */
		if(cmd.hasOption("v"))		 GenericConfig.VERB = true;
//		if(cmd.hasOption("u"))		 GenericConfig.INTERACT = true;
		if(cmd.hasOption("nocolor")) GenericConfig.NOCOLOR = true;
		if(cmd.hasOption("h"))		 return -1;
		if(cmd.hasOption("info")) 	 return -2;
		if(cmd.hasOption("version")) return -2;
		if(cmd.hasOption("core"))    return -3;
		if(cmd.hasOption("notime")) GenericConfig.TSTAMP = false;
//		if(cmd.hasOption("f"))		 GenericConfig.FORCE = true;
		
		Prompt.debug(ANSIHandler.wrapper("Developer mode activated.", 'Y'));
		Prompt.talk("Verbose option check.");
		if(GenericConfig.TSTAMP) Prompt.talk("Timestamp printing option check."); 

		/* successfully parsed */
		Prompt.talk(ANSIHandler.wrapper("SUCCESS", 'g') + " : Option parsing");
		return 0;
	}
	

	/* Manual route; exit with status 0 */
	private static void printManual() {
		System.out.println(
//				ANSIHandler.wrapper(" Manual - UFCG v" + VERSION + "\n\n", 'Y') + 
				
				ANSIHandler.wrapper(" USAGE : ", 'Y') + "java -jar UFCG.jar <module> [<args>]\n\n\n" +
				
				ANSIHandler.wrapper(" Available Modules\n", 'Y') +
				ANSIHandler.wrapper(" Module         Description\n", 'c') +
									" profile        Extract UFCG profile from genome\n"+
									" tree           Build maximum likelihood tree with UFCG profiles\n\n\n"+
				
				ANSIHandler.wrapper(" Miscellaneous\n", 'Y') +
				ANSIHandler.wrapper(" Argument       Description\n", 'c') +
									" --help         Print this manual\n" + 
									" --info         Print program information\n" + 
									" --core         Print core gene list\n" +
									" --verbose      Make program verbose\n" + 
									" --nocolor      Remove ANSI escapes from the output\n" +
									" --notime       Remove timestamp in front of the prompt string\n" +
									" --developer    Activate developer mode (For testing or debugging)\n\n"
				);
		System.exit(0);
	}
	
	/* Information route; exit with status 0 */
	private static void printInfo() {
		System.out.println(
				ANSIHandler.wrapper(" UFCG : Profiling Fungi with " + String.valueOf(GenericConfig.FCG_REF.length) + 
									" Up-to-date Fungal Core Genes\n", 'Y') +
				ANSIHandler.wrapper(" ver. " + VERSION + " (Released: " + RELEASE_DATE + ")\n\n", 'Y') + 
				" Developed by Daniel Dongwook Kim\n" +
				" Steinegger Lab, Seoul National University\n\n" +
				" Contact        : endix1029@snu.ac.kr\n" +
				" Correspondence : jchun@snu.ac.kr\n" +
				"                  martin.steinegger@snu.ac.kr\n"
//				+ "\nFeel free to report any typos, errors, or kind suggestions.\n"
				);
		System.exit(0);
	}
	
	/* Core gene route; exit with status 0 */
	private static void printCore() {
		System.out.println(ANSIHandler.wrapper("  Gene  \tCOG \t    Function", 'Y'));
		for(int i = 0; i < GenericConfig.FCG_REF.length; i++) {
			System.out.print(ANSIHandler.wrapper(String.format("  %-6s\t", GenericConfig.FCG_REF[i]), 'C'));
			System.out.print(String.format("%-4s\t    ", GenericConfig.FCG_COG[i]));
			System.out.println(GenericConfig.FCG_DSC[i]);
		}
		System.out.println("");
		System.exit(0);
	}
	
	/* Main function
	 * Implementation note.
	 * 		All instructions wrapped in a single try-catch statement.
	 * 		Any exception occurred will be caught and thrown to ExceptionHandler.
	 */
	public static void main(String[] args) {
		/* Signal handler implemented with sun.misc.Signal package 
		 * Implementation note.
		 * 		 Handler creates new thread to handle signal -
		 * 		 therefore the running process may not terminate immediately,
		 * 		 and may shortly print some additional prompts between handling and termination.
		 */
		Signal.handle(new Signal("INT"), new SignalHandler() {
			public void handle(Signal sig) {
				System.out.println("");
				Prompt.debug("SIGINT detected");
				GenericConfig.setHeader("SIGNAL");
				Prompt.print("Keyboard interrupt detected. Terminating process.");
				if(GenericConfig.INTERACT) Prompt.print("Use -h option to see the user manual.\n");
				else System.out.println("");
				System.exit(0);
			}
		});
		try {
			/* Environment setup and path definition */
			GenericConfig.setHeader("UFCG");
			GenericConfig.setHeaderLength(5);
			String jarPath = UFCGMainPipeline.class.getProtectionDomain().getCodeSource().getLocation().getPath();
			PathConfig.setEnvironmentPath(jarPath.substring(0, jarPath.lastIndexOf("/") + 1));
			System.out.println("");
			FileStream.init();
			
			/* Module parsing */
			
			
			
			/* Argument parsing */
			switch(parseArgument(args)) {
			case -2: printLogo(); printInfo();
			case -3: printLogo(); printCore();
			case -4: Prompt.print(GenericConfig.geneString()); break;
			default: printLogo(); printManual();
			}
			
			
		}
		catch(Exception e) {
			/* Exception handling route; exit with status 1 */
			e.printStackTrace();
			ExceptionHandler.handle(e);
		}
		finally {
			Prompt.print("Job finished. Terminating process.\n");
			System.exit(0);
		}
	}
}