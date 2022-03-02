package wrapper;

import envs.toolkit.ExecHandler;

public class PrepareAlignWrapper extends ExecHandler {
	public PrepareAlignWrapper() {
		super.init("prepareAlign");
	}
	
	void setMsaPath(String msaPath) {
		addArg("<", msaPath);
	}
	void setOutPath(String outPath) {
		addArg(">", outPath);
	}
	
	public static void runPrepareAlign(String msaPath, String outPath) {
		PrepareAlignWrapper pa = new PrepareAlignWrapper();
		pa.setMsaPath(msaPath);
		pa.setOutPath(outPath);
		pa.exec();
	}
}
