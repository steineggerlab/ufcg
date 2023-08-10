package pipeline;

import envs.toolkit.Prompt;
import module.AlignModule;
import module.ConvertModule;
import module.ProfileModule;
import module.ProfileProModule;
import module.ProfileRnaModule;
import module.PruneModule;
import module.TrainModule;
import module.TreeModule;
import module.DownloadModule;

import java.util.Arrays;
import java.util.List;

public class ModuleHandler {
	private final int module;
	private final String[] args;
	public final boolean help;
	
	public ModuleHandler(int module, String[] args) {
		this.module = module;
		this.args = args;
		List<String> argList = Arrays.asList(args);
		if(argList.contains("-h") || argList.contains("-help") || argList.contains("--help")) {
			Prompt.SUPPRESS = true;
			help = true;
		} else if((module == UFCGMainPipeline.MODULE_PROFILE || module == UFCGMainPipeline.MODULE_PROFILE_RNA) && (argList.contains("-hh") || argList.contains("--hh"))) {
			Prompt.SUPPRESS = true;
			help = true;
		} else {
			help = false;
		}
		if(module == UFCGMainPipeline.MODULE_DOWNLOAD && argList.contains("-c")) {
			Prompt.SUPPRESS = true;
		}
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
		TreeModule.run(args);
	}
	private void handle_prune() {
		Prompt.talk("UFCG prune v" + UFCGMainPipeline.VERSION);
		PruneModule.run(args);
	}
	private void handle_download(){
		Prompt.talk("UFCG download v" + UFCGMainPipeline.VERSION);
		DownloadModule.run(args);
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
		case UFCGMainPipeline.MODULE_DOWNLOAD:		handle_download(); break;
		default: break;
		}
	}
}
