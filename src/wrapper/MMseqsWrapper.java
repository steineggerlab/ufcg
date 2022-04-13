package wrapper;

import envs.config.PathConfig;
import envs.toolkit.ExecHandler;
import envs.toolkit.Shell;

public class MMseqsWrapper extends ExecHandler {
	public MMseqsWrapper() {
		super.init(PathConfig.MMseqsPath);
	}
	
	void setModule(String module) {
		super.addArg(module);
	}
	
	void setSearchType(int type) {
		super.addArg("--search-type", String.valueOf(type));
	}
	
	void setEvalue(double evalue) {
		super.addArg("-e", String.valueOf(evalue));
	}
	
	// add file or db
	void addFile(String file) {
		super.addArg(file);
	}
	
	public static void runEasySearch(int type, String query, String target, String result, String tmpdir) {
		MMseqsWrapper mm = new MMseqsWrapper();
		mm.setModule("easy-search");
		mm.setSearchType(type);
		mm.addFile(query);
		mm.addFile(target);
		mm.addFile(result);
		mm.addFile(tmpdir);
		mm.exec();
	}
	public static void runEasySearch(int type, double evalue, String query, String target, String result, String tmpdir) {
		MMseqsWrapper mm = new MMseqsWrapper();
		mm.setModule("easy-search");
		mm.setSearchType(type);
		mm.setEvalue(evalue);
		mm.addFile(query);
		mm.addFile(target);
		mm.addFile(result);
		mm.addFile(tmpdir);
		mm.exec();
	}
	
	// solve dependency
	public static boolean solve() {
		String cmd = PathConfig.MMseqsPath + " 2>&1";
		String[] raw = Shell.exec(cmd);
		if(raw[0].contains("not found")) return false;
		for(int error_loc = 0; !raw[error_loc].contains("MMseqs2"); error_loc++) {
			if(error_loc + 1 == raw.length) return false; 
		}
		return true;
	}
}
