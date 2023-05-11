package module;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.cli.*;

import envs.config.GenericConfig;
import envs.toolkit.ANSIHandler;
import pipeline.ExceptionHandler;
import pipeline.UFCGMainPipeline;
import tree.TreeBuilder;
import tree.tools.AlignMode;
import tree.tools.PhylogenyTool;

public class TreeModule {
	String ucgDirectory = null;
	String outDirectory = null;
	String mafftPath = "mafft-linsi";
	String raxmlPath = "raxmlHPC-PTHREADS";
	String fasttreePath = "FastTree";
	String iqtreePath = "iqtree";
	AlignMode alignMode = AlignMode.protein;
	Integer filtering = 50;
	PhylogenyTool phylogenyTool = PhylogenyTool.iqtree;
	String model = null;
	Integer gsi_threshold = 95;
	List<String> outputLabels = null;
	Integer executorLimit = 1;
	Boolean allowMultiple = false;

	private void parseArguments(String[] args) {
		/* option argument setup */
		Options opts = new Options();

		opts.addOption("h", "help",		false,	"helper route");
		opts.addOption("i", "input",	true,	"input directory");
		opts.addOption("o", "output",	true,	"output directory");
		// opts.addOption("n", "name",		true,	"runtime name");
		opts.addOption("l", "leaf",		true,	"leaf format");
		opts.addOption("a", "alignment",true,	"alignment type");
		opts.addOption("t", "thread",	true,	"CPU thread");
		opts.addOption("f", "filter",	true,	"gap-rich filter");
		opts.addOption("p", "program",	true,	"tree program");
		opts.addOption("m", "model",	true,	"tree model");
		opts.addOption("g", "gsi",		true,	"gsi-threshold");
		opts.addOption("x", "executor", true,   "executor limit");
		opts.addOption("c", "copy",		false,	"copy number");

		opts.addOption(null, "mafft",   true,	"mafft binary path");
		opts.addOption(null, "raxml",   true,	"raxml binary path");
		opts.addOption(null, "fasttree",true,	"fasttree binary path");
		opts.addOption(null, "iqtree",  true,	"iqtree binary path");

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
		catch(ParseException pe) {
			ExceptionHandler.handle(pe);
		}

		/* apply general options */
		assert cmd != null;
		if(cmd.hasOption("v"))		 GenericConfig.VERB = true;
		if(cmd.hasOption("notime"))  GenericConfig.TSTAMP = false;
		if(cmd.hasOption("nocolor")) GenericConfig.NOCOLOR = true;
		if(cmd.hasOption("developer")) {
			GenericConfig.DEV = true;
			GenericConfig.VERB = true;
			GenericConfig.TSTAMP = true;
		}

		if(cmd.hasOption("h")) printTreeHelp();
		else {
			if(cmd.hasOption("i")) {
				ucgDirectory = cmd.getOptionValue("i");
				if (!ucgDirectory.endsWith(File.separator)) ucgDirectory += File.separator;
			}
			else ExceptionHandler.handle(ExceptionHandler.NO_INPUT);
			if(cmd.hasOption("o")) {
				outDirectory = cmd.getOptionValue("o");
				if (!outDirectory.endsWith(File.separator)) outDirectory += File.separator;
			}
			else ExceptionHandler.handle(ExceptionHandler.NO_OUTPUT);

			outputLabels = new ArrayList<>();
			final String[] validLeaves = {"uid", "acc", "label", "taxon", "strain", "type", "taxonomy"};
			if(cmd.hasOption("l")) {
				ExceptionHandler.pass(cmd.getOptionValue("l"));
				for (String leaf : cmd.getOptionValue("l").split(",")) {
					if (Arrays.asList(validLeaves).contains(leaf)) outputLabels.add(leaf);
					else ExceptionHandler.handle(ExceptionHandler.INVALID_LEAF_FORMAT);
				}
			} else outputLabels.add("label");
			if(outputLabels.size() == 0) ExceptionHandler.handle(ExceptionHandler.INVALID_LEAF_FORMAT);

			if(cmd.hasOption("a")) {
				String mode = cmd.getOptionValue("a");
				switch(mode) {
					case "protein": alignMode = AlignMode.protein; break;
					case "nucleotide": alignMode = AlignMode.nucleotide; break;
					case "codon": alignMode = AlignMode.codon; break;
					case "codon12": alignMode = AlignMode.codon12; break;
					default: ExceptionHandler.pass(mode); ExceptionHandler.handle(ExceptionHandler.INVALID_ALIGN_MODE);
				}
			} else alignMode = AlignMode.protein;

			if(cmd.hasOption("t")) {
				GenericConfig.setThreadPoolSize(cmd.getOptionValue("t"));
			}
			if(cmd.hasOption("f")) {
				try {
					filtering = Integer.parseInt(cmd.getOptionValue("f"));
				} catch(NumberFormatException nfe) {
					ExceptionHandler.pass(cmd.getOptionValue("f") + " (Integer value expected)");
					ExceptionHandler.handle(ExceptionHandler.INVALID_VALUE);
				}
				if (filtering < 0 || filtering > 100) {
					ExceptionHandler.pass(filtering);
					ExceptionHandler.handle(ExceptionHandler.INVALID_VALUE);
				}
			}

			if(cmd.hasOption("p")) {
				String prog = cmd.getOptionValue("p").toLowerCase();
				if(prog.startsWith("iq")) phylogenyTool = PhylogenyTool.iqtree;
				else if(prog.startsWith("ra")) phylogenyTool = PhylogenyTool.raxml;
				else if(prog.startsWith("fa")) phylogenyTool = PhylogenyTool.fasttree;
				else {
					ExceptionHandler.pass(prog);
					ExceptionHandler.handle(ExceptionHandler.INVALID_BINARY);
				}
			}
			if(cmd.hasOption("m")) {
				model = cmd.getOptionValue("m");
				String[] options = null;
				if(alignMode.equals(AlignMode.protein)) {
					if(phylogenyTool.equals(PhylogenyTool.raxml)) options = GenericConfig.PROTEIN_RAXML_MODELS;
					if(phylogenyTool.equals(PhylogenyTool.fasttree)) options = GenericConfig.PROTEIN_FASTTREE_MODELS;
					if(phylogenyTool.equals(PhylogenyTool.iqtree)) options = GenericConfig.PROTEIN_IQTREE_MODELS;
				}
				else { // nucleotide
					if(phylogenyTool.equals(PhylogenyTool.raxml)) options = GenericConfig.NUCLEOTIDE_RAXML_MODELS;
					if(phylogenyTool.equals(PhylogenyTool.fasttree)) options = GenericConfig.NUCLEOTIDE_FASTTREE_MODELS;
					if(phylogenyTool.equals(PhylogenyTool.iqtree)) options = GenericConfig.NUCLEOTIDE_IQTREE_MODELS;
				}
				if(!phylogenyTool.equals(PhylogenyTool.iqtree)) {
					if(!Arrays.asList(options).contains(model)) {
						ExceptionHandler.pass(model);
						ExceptionHandler.handle(ExceptionHandler.INVALID_MODEL);
					}
				}
			}

			if(cmd.hasOption("g")) {
				gsi_threshold = Integer.parseInt(cmd.getOptionValue("g"));
				if (gsi_threshold < 0 || gsi_threshold > 100) {
					ExceptionHandler.pass(gsi_threshold);
					ExceptionHandler.handle(ExceptionHandler.INVALID_VALUE);
				}
			}
			if(cmd.hasOption("x")) {
				executorLimit = Integer.parseInt(cmd.getOptionValue("x"));
				if (executorLimit < 0) {
					ExceptionHandler.pass(executorLimit);
					ExceptionHandler.handle(ExceptionHandler.INVALID_VALUE);
				}
			}
			allowMultiple = cmd.hasOption("c");

			if(cmd.hasOption("mafft")) mafftPath = cmd.getOptionValue("mafft");
			if(cmd.hasOption("raxml")) raxmlPath = cmd.getOptionValue("raxml");
			if(cmd.hasOption("fasttree")) fasttreePath = cmd.getOptionValue("fasttree");
			if(cmd.hasOption("iqtree")) iqtreePath = cmd.getOptionValue("iqtree");
		}
	}

	public static void run(String[] args) {
		TreeModule proc = new TreeModule();
		proc.parseArguments(args);
		proc.align();
	}

	private void align() {
		TreeBuilder proc = new TreeBuilder(ucgDirectory, outDirectory, mafftPath, raxmlPath, fasttreePath, iqtreePath, alignMode, filtering, model, gsi_threshold, outputLabels, executorLimit, allowMultiple);
		try {
			proc.jsonsToTree(GenericConfig.ThreadPoolSize, phylogenyTool);
		}catch (IOException e) {
			ExceptionHandler.handle(e);
		}
	}

	private void printTreeHelp() {
		System.out.println(ANSIHandler.wrapper(" UFCG - tree", 'G'));
		System.out.println(ANSIHandler.wrapper(" Reconstruct the phylogenetic relationship with UFCG profiles", 'g'));
		System.out.println();

		System.out.println(ANSIHandler.wrapper("\n USAGE:", 'Y') + " ufcg tree -i <INPUT> -l <LABEL> [...]");
		System.out.println();

		System.out.println(ANSIHandler.wrapper("\n Required options", 'Y'));
		System.out.println(ANSIHandler.wrapper(" Argument        Description", 'c'));
		System.out.println(ANSIHandler.wrapper(" -i STR          Input directory containing UFCG profiles ", 'x'));
		System.out.println(ANSIHandler.wrapper(" -o STR          Output directory", 'x'));
		System.out.println();

		System.out.println(ANSIHandler.wrapper("\n Additional options", 'y'));
		System.out.println(ANSIHandler.wrapper(" Argument        Description", 'c'));
		System.out.println(ANSIHandler.wrapper(" -l STR          Tree leaf format, comma-separated string containing one or more of the following keywords: [label]", 'x'));
		System.out.println(ANSIHandler.wrapper("                 {uid, acc, label, taxon, strain, type, taxonomy}", 'x'));
		// System.out.println(ANSIHandler.wrapper(" -n STR         Name of this run [random hex string] ", 'x'));
		System.out.println(ANSIHandler.wrapper(" -a STR          Alignment method {nucleotide, codon, codon12, protein} [protein]", 'x'));
		System.out.println(ANSIHandler.wrapper(" -t INT          Number of CPU threads to use [1]", 'x'));
		System.out.println(ANSIHandler.wrapper(" -p STR          Tree building program {raxml, iqtree, fasttree} [iqtree] ", 'x'));
		System.out.println(ANSIHandler.wrapper(" -c              Align multiple copied genes [0]", 'x'));
		System.out.println();

		System.out.println(ANSIHandler.wrapper("\n Dependencies", 'y'));
		System.out.println(ANSIHandler.wrapper(" Argument        Description", 'c'));
		System.out.println(ANSIHandler.wrapper(" --mafft STR     Path to MAFFT binary [mafft-linsi]", 'x'));
		System.out.println(ANSIHandler.wrapper(" --iqtree STR    Path to IQ-TREE binary [iqtree]", 'x'));
		System.out.println(ANSIHandler.wrapper(" --raxml STR     Path to RAxML binary [raxmlHPC-PTHREADS]", 'x'));
		System.out.println(ANSIHandler.wrapper(" --fasttree STR  Path to FastTree binary [FastTree]", 'x'));
		System.out.println();

		System.out.println(ANSIHandler.wrapper("\n Advanced options", 'y'));
		System.out.println(ANSIHandler.wrapper(" Argument        Description", 'c'));
		System.out.println(ANSIHandler.wrapper(" -f INT          Gap-rich filter percentage threshold {0 - 100} [50] ", 'x'));
		System.out.println(ANSIHandler.wrapper(" -m STR          ML tree inference model [JTT+ (proteins); GTR+ (nucleotides)] ", 'x'));
		System.out.println(ANSIHandler.wrapper(" -g INT          GSI value threshold {1 - 100} [95] ", 'x'));
		System.out.println(ANSIHandler.wrapper(" -x INT          Maximum number of gene tree executors; lower this if the RAM usage is excessive {1 - threads} [equal to -t]", 'x'));
		System.out.println();
		
		UFCGMainPipeline.printGeneral();
		
		System.exit(0);
	}
}