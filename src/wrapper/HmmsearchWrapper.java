package wrapper;

import envs.config.GenericConfig;
import envs.config.PathConfig;
import envs.toolkit.ExecHandler;
import envs.toolkit.Shell;

@Deprecated
public class HmmsearchWrapper extends ExecHandler {
	public HmmsearchWrapper() {
		super.init(PathConfig.HmmsearchPath);
	}
	
	void setCutoff(double cutoff) {
		addArg("-T", String.valueOf(cutoff));
	}
	void setTblPath(String tblPath) {
		addArg("--tblout", tblPath);
	}
	void setOutPath(String outPath) {
		addArg("-o", outPath);
	}
	void setHmm(String hmm) {
		addArg(hmm);
	}
	void setSeq(String seq) {
		addArg(seq);
	}
	
	public static void runHmmsearchTable(String tblPath, String hmm, String seq) {
		HmmsearchWrapper hs = new HmmsearchWrapper();
		hs.setCutoff(GenericConfig.HmmsearchScoreCutoff);
		hs.setTblPath(tblPath);
		hs.setOutPath("/dev/null");
		hs.setHmm(hmm);
		hs.setSeq(seq);
		hs.exec();
	}
	
	public static void runHmmsearchStd(String outPath, String hmm, String seq) {
		HmmsearchWrapper hs = new HmmsearchWrapper();
		hs.setCutoff(GenericConfig.HmmsearchScoreCutoff);
		hs.setOutPath(outPath);
		hs.setHmm(hmm);
		hs.setSeq(seq);
		hs.exec();
	}
	
	// solve dependency
	public static boolean solve() {
		String[] cmd = {"/bin/bash", "-c", PathConfig.HmmsearchPath + " /dev/null /dev/null 2>&1"};
		String[] raw = Shell.exec(cmd);
		
		if(raw[0].contains("not found")) return false;
		else return true;
	}
}
