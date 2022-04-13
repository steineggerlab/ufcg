package wrapper;

import envs.config.GenericConfig;
import envs.config.PathConfig;
import envs.toolkit.ExecHandler;
import envs.toolkit.Shell;
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
	void setOutPath(String outPath) {
		addArg(">", outPath);
		addArg("2>", "/dev/null");
	}
	
	public static void runFastBlockSearch(String seqPath, String famPath, String outPath) {
		FastBlockSearchWrapper fbs = new FastBlockSearchWrapper();
		fbs.setCutoff();
		fbs.setSeqPath(seqPath);
		fbs.setFamPath(famPath);
		fbs.setOutPath(outPath);
		fbs.exec();
	}
	
	// solve dependency
	public static boolean solve() {
		String cmd = PathConfig.FastBlockSearchPath + " /dev/null /dev/null 2>&1";
		String[] raw = Shell.exec(cmd);
			
		if(raw[0].contains("ProfileInsigError")) return true;
		else{
			if(!GenericConfig.INTERACT) Prompt.talk("fBSearch", raw[0]);
			return false;
		}
	}
}
