package wrapper;

import envs.toolkit.ExecHandler;

public class HmmbuildWrapper extends ExecHandler {
	public HmmbuildWrapper() {
		super.init("hmmbuild");
	}
	
	void setHmm(String hmm) {
		addArg(hmm);
	}
	void setMsa(String msa) {
		addArg(msa);
	}
	
	public static void runHmmbuild(String hmm, String msa) {
		HmmbuildWrapper hb = new HmmbuildWrapper();
		hb.setHmm(hmm);
		hb.setMsa(msa);
		hb.exec();
	}
}
