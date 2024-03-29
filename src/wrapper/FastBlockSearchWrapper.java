package wrapper;

import envs.config.GenericConfig;
import envs.config.PathConfig;
import envs.toolkit.ExecHandler;
import envs.toolkit.Shell;
import pipeline.ExceptionHandler;
import envs.toolkit.Prompt;

public class FastBlockSearchWrapper extends ExecHandler {
	public FastBlockSearchWrapper() {
		super.init(PathConfig.FastBlockSearchPath);
	}
	
	void setCutoff() {
		addArg(String.format("--cutoff=%.1f", GenericConfig.FastBlockSearchCutoff));
	}
	void setSeqPath(String seqPath) {
		addArg(seqPath);
	}
	void setFamPath(String famPath) {
		addArg(famPath);
	}
	void setOutPath(String outPath, String errPath) {
		addArg("1>", outPath);
		addArg("2>", errPath);
	}
	
	private boolean sanityCheck(String outPath, String errPath) {
		String cmd = "head -1 " + outPath;
		String[] raw = Shell.exec(cmd);
		
		if(raw.length < 1) {
			cmd = "tail -2 " + errPath;
			raw = Shell.exec(cmd);
			if(raw[0].contains("Insig")) return true;
		}
		return raw[0].contains("Hits");
	}
	
	public static void runFastBlockSearch(String seqPath, String famPath, String outPath, String errPath) {
		FastBlockSearchWrapper fbs = new FastBlockSearchWrapper();
		fbs.setCutoff();
		fbs.setSeqPath(seqPath);
		fbs.setFamPath(famPath);
		fbs.setOutPath(outPath, errPath);
		fbs.exec();
		
		// fastBlockSearch failure
		if(!fbs.sanityCheck(outPath, errPath)) {
			ExceptionHandler.pass(fbs.buildCmd());
			ExceptionHandler.handle(ExceptionHandler.FAILED_COMMAND);
		}
	}
	
	// solve dependency
	public static boolean solve() {
		String cmd = PathConfig.FastBlockSearchPath + " 2>&1";
		String[] raw = Shell.exec(cmd);
			
		if(raw[0].contains("Usage")) return true;
		else{
			if(!GenericConfig.INTERACT) Prompt.talk("fBSearch", raw[0]);
			return false;
		}
	}
}
