package pipeline;

import java.util.ArrayList;
import java.util.List;

import envs.toolkit.Prompt;
import module.ProfileModule;

public class ModuleHandler {
	private final int module;
	private final String[] args;
	
	public ModuleHandler(int module, String[] args) {
		this.module = module;
		this.args = args;
	}
	
	private String[] parse() {
		List<String> argList = new ArrayList<String>();
		
		
		return argList.toArray(new String[argList.size()]);
	}
	
	private void handle_no_module() {
		UFCGMainPipeline.run(args);
	}
	private void handle_profile() {
		ProfileModule.run(args);
	}
	private void handle_profile_rna() {
		
	}
	private void handle_tree() {
		
	}
	private void handle_tree_fix() {
		
	}
	
	public void handle() {
		switch(module) {
		case UFCGMainPipeline.NO_MODULE:			handle_no_module(); break;
		case UFCGMainPipeline.MODULE_PROFILE: 		handle_profile(); break;
		case UFCGMainPipeline.MODULE_PROFILE_RNA: 	handle_profile_rna(); break;
		case UFCGMainPipeline.MODULE_TREE: 			handle_tree(); break;
		case UFCGMainPipeline.MODULE_TREE_FIX: 		handle_tree_fix(); break;
		default: break;
		}
	}
}
