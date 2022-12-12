package pipeline;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;

import envs.config.GenericConfig;
//import envs.toolkit.ANSIHandler;
import envs.toolkit.Prompt;
import module.AlignModule;
import module.ConvertModule;
import module.ProfileModule;
import module.ProfileProModule;
import module.ProfileRnaModule;
import module.TrainModule;
import module.TreeModule;

public class ModuleHandler {
	private final int module;
	private final String[] args;
	
	public ModuleHandler(int module, String[] args) {
		this.module = module;
		this.args = args;
	}
	
	private void handle_no_module() {
		UFCGMainPipeline.run(args);
	}
	private void handle_profile() {
		Prompt.talk("UFCG profile v" + UFCGMainPipeline.VERSION);
		ProfileModule.run(args);
	}
	private void handle_profile_rna() {
		Prompt.talk("UFCG profile-rna v" + UFCGMainPipeline.VERSION);
		ProfileRnaModule.run(args);
	}
	private void handle_profile_pro() {
		Prompt.talk("UFCG profile-pro v" + UFCGMainPipeline.VERSION);
		ProfileProModule.run(args);
	}
	private void handle_align() {
		Prompt.talk("UFCG align v" + UFCGMainPipeline.VERSION);
		AlignModule.run(args);
	}
	private void handle_train() {
		Prompt.talk("UFCG train v" + UFCGMainPipeline.VERSION);
		TrainModule.run(args);
	}
	private void handle_convert() {
		Prompt.talk("UFCG convert v" + UFCGMainPipeline.VERSION);
		ConvertModule.run(args);
	}
	private void handle_tree() {
		Prompt.talk("UFCG tree v" + UFCGMainPipeline.VERSION);
		
		/* option argument setup */
		Options opts = new Options();
		
		opts.addOption("h", "help",		false,	"helper route");
		opts.addOption("i", "input",	true,	"input directory");
		opts.addOption("l", "leaf",		true,	"leaf format");
		opts.addOption("o", "output",	true,	"output directory");
		opts.addOption("n", "name",		true,	"runtime name");
		opts.addOption("a", "alignment",true,	"alignment type");
		opts.addOption("t", "thread",	true,	"CPU thread");
		opts.addOption("f", "filter",	true,	"gap-rich filter");
		opts.addOption("p", "program",	true,	"tree program");
		opts.addOption("m", "model",	true,	"tree model");
		opts.addOption("g", "gsi",		true,	"gsi-threshold");
		opts.addOption("x", "executor", true,   "executor limit");
		opts.addOption("c", "copy",		true,	"copy number");

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

		/* prepare arguments compatible to tree module */
		List<String> argList = new ArrayList<>();
		argList.add("align");

		if(cmd.hasOption("h")) argList.add("-h");
		else {
			if(cmd.hasOption("i")) {
				argList.add("-ucg_dir");
				argList.add(cmd.getOptionValue("i"));
			} else ExceptionHandler.handle(ExceptionHandler.NO_INPUT);
			if(cmd.hasOption("l")) {
				argList.add("-leaf");
				argList.add(cmd.getOptionValue("l"));
			} else ExceptionHandler.handle(ExceptionHandler.NO_LEAF_OPTION);
			if(cmd.hasOption("o")) {
				argList.add("-out_dir");
				argList.add(cmd.getOptionValue("o"));
			}
			if(cmd.hasOption("n")) {
				argList.add("-run_id");
				argList.add(cmd.getOptionValue("n"));
			}
			if(cmd.hasOption("a")) {
				argList.add("-a");
				argList.add(cmd.getOptionValue("a"));
			}
			if(cmd.hasOption("t")) {
				argList.add("-t");
				argList.add(cmd.getOptionValue("t"));
			}
			if(cmd.hasOption("f")) {
				argList.add("-f");
				argList.add(cmd.getOptionValue("f"));
			}
			if(cmd.hasOption("p")) {
				String prog = cmd.getOptionValue("p");
				if(!(prog.equals("raxml") || prog.equals("fasttree") || prog.equals("iqtree"))) {
					ExceptionHandler.pass(prog);
					ExceptionHandler.handle(ExceptionHandler.INVALID_BINARY);
				}
				if(prog.equalsIgnoreCase("fasttree")) argList.add("-fasttree");
				if(prog.startsWith("iq") || prog.startsWith("IQ")) argList.add("-iqtree");
				if(prog.startsWith("ra") || prog.startsWith("RA")) argList.add("-raxml");
			}
			if(cmd.hasOption("m")) {
				argList.add("-m");
				argList.add(cmd.getOptionValue("m"));
			}
			if(cmd.hasOption("g")) {
				argList.add("-gsi_threshold");
				argList.add(cmd.getOptionValue("g"));
			}
			if(cmd.hasOption("x")) {
				argList.add("-x");
				argList.add(cmd.getOptionValue("x"));
			}
			if(cmd.hasOption("c")) {
				argList.add("-c");
				argList.add(cmd.getOptionValue("c"));
			}
		}
		
	//	Prompt.debug("Running : " + ANSIHandler.wrapper("tree.jar " + String.join(" ", argList), 'B'));
		TreeModule.run(argList.toArray(new String[0]));
	}
	private void handle_prune() {
		Prompt.talk("UFCG prune v" + UFCGMainPipeline.VERSION);
		
		/* option argument setup */
		Options opts = new Options();
		
		opts.addOption("h", "help",		false,	"helper route");
		opts.addOption("i", "input",	true,	"input file");
		opts.addOption("g", "gene",		true,	"gene to replace");
		opts.addOption("l", "leaf",		true,	"leaf format");
		
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
		
		/* prepare arguments compatible to tree module */
		List<String> argList = new ArrayList<>();
		argList.add("replace");
		
		if(cmd.hasOption("h")) argList.add("-h");
		else {
			if(cmd.hasOption("i")) {
				argList.add(cmd.getOptionValue("i"));
			} else ExceptionHandler.handle(ExceptionHandler.NO_INPUT);
			if(cmd.hasOption("g")) {
				argList.add(cmd.getOptionValue("g"));
			} else ExceptionHandler.handle(ExceptionHandler.NO_GENE_NAME);
			if(cmd.hasOption("l")) {
				String buf = cmd.getOptionValue("l");
				for(String ele : buf.split(",")) argList.add("-" + ele);
			}
		}
		
	//	Prompt.debug("Running : " + ANSIHandler.wrapper("tree.jar " + String.join(" ", argList), 'B'));
		TreeModule.run(argList.toArray(new String[0]));
	}
	
	public void handle() {
		switch(module) {
		case UFCGMainPipeline.NO_MODULE:			handle_no_module(); break;
		case UFCGMainPipeline.MODULE_PROFILE: 		handle_profile(); break;
		case UFCGMainPipeline.MODULE_PROFILE_RNA: 	handle_profile_rna(); break;
		case UFCGMainPipeline.MODULE_PROFILE_PRO: 	handle_profile_pro(); break;
		case UFCGMainPipeline.MODULE_TREE: 			handle_tree(); break;
		case UFCGMainPipeline.MODULE_PRUNE: 		handle_prune(); break;
		case UFCGMainPipeline.MODULE_ALIGN:			handle_align(); break;
		case UFCGMainPipeline.MODULE_TRAIN:			handle_train(); break;
		case UFCGMainPipeline.MODULE_CONVERT:		handle_convert(); break;
		default: break;
		}
	}
}
