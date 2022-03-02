package wrapper;

import envs.toolkit.ExecHandler;

public class Msa2PrflWrapper extends ExecHandler {
	public Msa2PrflWrapper() {
		super.init("msa2prfl.pl");
	}
	
	void setName(String name) {
		addArg("--setname=" + name);
	}
	void setAcc(String acc) {
		addArg("--setacc=" + acc);
	}
	void setMaxEntropy(double maxEntropy) {
		addArg(String.format("--max_entropy=%.1f", maxEntropy));
	}
	void setMaxEntropy() {
		setMaxEntropy(0.75);
	}
	void setMsaPath(String msaPath) {
		addArg(msaPath);
	}
	void setOutPath(String outPath) {
		addArg(">", outPath);
	}
	
	public static void runMsa2Prfl(String name, String acc, String msaPath, String outPath) {
		Msa2PrflWrapper m2p = new Msa2PrflWrapper();
		m2p.setName(name);
		m2p.setAcc(acc);
		m2p.setMaxEntropy();
		m2p.setMsaPath(msaPath);
		m2p.setOutPath(outPath);
		m2p.exec();
	}
}
