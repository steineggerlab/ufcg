package wrapper;

import envs.toolkit.ExecHandler;

public class MafftWrapper extends ExecHandler {
	public MafftWrapper() {
		super.init("mafft");
	}
	
	void setAuto() {
		addArg("--auto");
	}
	void setInput(String in) {
		addArg(in);
	}
	void setOutput(String out) {
		addArg(">", out);
	}
	
	public static void runMafft(String in, String out) {
		MafftWrapper mw = new MafftWrapper();
		mw.setAuto();
		mw.setInput(in);
		mw.setOutput(out);
		mw.exec();
	}
}
