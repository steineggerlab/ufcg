package module;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
import tree.TreeBuilder;
import tree.tools.AlignMode;

public class AlignModule {
	private static AlignMode alignMode = AlignMode.protein;
	private static String name = GenericConfig.SESSION_UID;
	private static Integer filter = 50;
	private static List<String> leaves = null;
	private static boolean allowMultiple = false;
	/* Argument parsing route */
	private static void parseArgument(String[] args) throws ParseException {
		/* option argument setup */
		Options opts = new Options();

		opts.addOption("h", "help",		false,	"helper route");
		opts.addOption("i", "input",	true,	"input directory");
		opts.addOption("o", "output",	true,	"output directory");
		opts.addOption("l", "label",	true,	"output label");
		// opts.addOption("n", "name",		true,	"name of the run");
		opts.addOption("a", "alignment",true,	"alignment type");
		opts.addOption("t", "thread",	true,	"CPU thread");
		opts.addOption("f", "filter",	true,	"gap-rich filter");
		opts.addOption("c", "copy",		false,	"allow multiple copies");

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

		if(cmd == null) ExceptionHandler.handle(ExceptionHandler.UNEXPECTED_ERROR);
		assert cmd != null;
		if(cmd.hasOption("v"))		 GenericConfig.VERB = true;
		if(cmd.hasOption("notime"))  GenericConfig.TSTAMP = false;
		if(cmd.hasOption("nocolor")) GenericConfig.NOCOLOR = true;
		if(cmd.hasOption("developer")) {
			GenericConfig.DEV = true;
			GenericConfig.VERB = true;
			GenericConfig.TSTAMP = true;
		}

		if(cmd.hasOption("h")) printAlignHelp();
		if(cmd.hasOption("i"))
			PathConfig.setInputPath(cmd.getOptionValue("i"));
		else ExceptionHandler.handle(ExceptionHandler.NO_INPUT);
		if(cmd.hasOption("o"))
			PathConfig.setOutputPath(cmd.getOptionValue("o"));
		else ExceptionHandler.handle(ExceptionHandler.NO_OUTPUT);
		if(cmd.hasOption("t"))
			GenericConfig.setThreadPoolSize(cmd.getOptionValue("t"));

		/* parse configuration options */
		leaves = new ArrayList<>();

		if(cmd.hasOption("l")) {
			leaves = Arrays.asList(cmd.getOptionValue("l").split(","));
			for(String leaf : leaves) {
				if(!(leaf.equals("uid") | leaf.equals("acc") | leaf.equals("label") | leaf.equals("taxon") | leaf.equals("strain") | leaf.equals("type") | leaf.equals("taxonomy"))) {
					ExceptionHandler.pass(leaf);
					ExceptionHandler.handle(ExceptionHandler.INVALID_LEAF_FORMAT);
				}
			}
		}
		else leaves.add("label");

		if(cmd.hasOption("a")) {
			String align = cmd.getOptionValue("a");
			switch (align) {
				case "nucleotide":
					alignMode = AlignMode.nucleotide;
					break;
				case "codon":
					alignMode = AlignMode.codon;
					break;
				case "codon12":
					alignMode = AlignMode.codon12;
					break;
				case "protein":
					alignMode = AlignMode.protein;
					break;
				default:
					ExceptionHandler.pass(align);
					ExceptionHandler.handle(ExceptionHandler.INVALID_ALIGN_MODE);
					break;
			}
		}
		if(cmd.hasOption("-n"))
			name = cmd.getOptionValue("n");
		if(cmd.hasOption("-f")) {
			filter = Integer.parseInt(cmd.getOptionValue("f"));
			if (filter > 100 || filter < 0) {
				ExceptionHandler.pass(filter);
				ExceptionHandler.handle(ExceptionHandler.INVALID_VALUE);
			}
		}
		allowMultiple = cmd.hasOption("-c");

		/* successfully parsed */
		Prompt.talk(ANSIHandler.wrapper("SUCCESS", 'g') + " : Option parsing");
	}

	private static String getMafftPath() {
		String mafftPath = null;
		try {
			BufferedReader pathBR = new BufferedReader(
					new FileReader(PathConfig.EnvironmentPath + "config/tree.cfg"));
			String line;
			while ((line = pathBR.readLine()) != null) {
				if (line.startsWith("mafft")) {
					mafftPath = line.substring(line.indexOf("=") + 1);
				}
			}

			pathBR.close();

			if (mafftPath == null) {
				ExceptionHandler.handle(ExceptionHandler.INVALID_PROGRAM_PATH);
			}
		} catch (IOException e) {
			ExceptionHandler.handle(e);
		}
		return mafftPath;
	}

	public static void run(String[] args) {
		try {
			parseArgument(args);
		} catch(ParseException e) {
			ExceptionHandler.handle(e);
		}

		TreeBuilder module = new TreeBuilder(PathConfig.InputPath, PathConfig.OutputPath, getMafftPath(), null, null, null, alignMode, filter, null, 0, leaves, 0, allowMultiple);

		try {
			module.jsonsToMsa(GenericConfig.ThreadPoolSize);
		} catch(IOException e) {
			ExceptionHandler.handle(e);
		}
	}

	private static void printAlignHelp() {
		System.out.println(ANSIHandler.wrapper(" UFCG - align", 'G'));
		System.out.println(ANSIHandler.wrapper(" Align genes and provide multiple sequence alignments from UFCG profiles", 'g'));
		System.out.println();

		System.out.println(ANSIHandler.wrapper("\n USAGE:", 'Y') + " ufcg align -i <INPUT> -o <OUTPUT> [...]");
		System.out.println();

		System.out.println(ANSIHandler.wrapper("\n Required options", 'Y'));
		System.out.println(ANSIHandler.wrapper(" Argument       Description", 'c'));
		System.out.println(ANSIHandler.wrapper(" -i STR         Input directory containing UFCG profiles", 'x'));
		System.out.println(ANSIHandler.wrapper(" -o STR         Output directory for alignments", 'x'));
		System.out.println();

		System.out.println(ANSIHandler.wrapper("\n Additional options", 'y'));
		System.out.println(ANSIHandler.wrapper(" Argument       Description", 'c'));
		System.out.println(ANSIHandler.wrapper(" -l STR         Label format, comma-separated string containing one or more of the following keywords:", 'x'));
		System.out.println(ANSIHandler.wrapper("                {uid, acc, label, taxon, strain, type, taxonomy} [label]", 'x'));
		System.out.println(ANSIHandler.wrapper(" -n STR         Name of this run [random hex string]", 'x'));
		System.out.println(ANSIHandler.wrapper(" -a STR         Alignment method {nucleotide, codon, codon12, protein} [protein]", 'x'));
		System.out.println(ANSIHandler.wrapper(" -t INT         Number of CPU threads to use [1]", 'x'));
		System.out.println(ANSIHandler.wrapper(" -f INT         Gap-rich filter percentage threshold {0 - 100} [50]", 'x'));
		System.out.println(ANSIHandler.wrapper(" -c             Align multiple copied genes [0]", 'x'));
		System.out.println();
		
		UFCGMainPipeline.printGeneral();
		
		System.exit(0);
	}
}
