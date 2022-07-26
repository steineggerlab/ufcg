package wrapper;

import envs.config.PathConfig;
import envs.toolkit.ExecHandler;
import envs.toolkit.Shell;

public class TrinityWrapper extends ExecHandler {
	public TrinityWrapper() {
		super.init(PathConfig.TrinityPath);
	}
	
	void setSeqType(String seqType) {
		super.addArg("--seqType", seqType);
	}
	
	void setLeft(String file) {
		super.addArg("--left", file);
	}
	
	void setRight(String file) {
		super.addArg("--right", file);
	}
	
	void setSingle(String file) {
		super.addArg("--single", file);
	}
	
	// convert bytes to gigabytes
	void setMaxMemory(long maxMemory) {
		super.addArg("--max_memory", String.valueOf(maxMemory / (1<<30)) + "G");
	}
	
	void setCPU(int cpu) {
		super.addArg("--CPU", String.valueOf(cpu));
	}
	
	void setOutput(String path) {
		super.addArg("--output", path);
	}
	
	void setRunAsPaired() {
		super.addArg("--run_as_paired");
	}
	
	public static void runSingle(String seqType, String single, long mem, int cpu, String out, boolean paired) {
		TrinityWrapper tr = new TrinityWrapper();
		tr.setSeqType(seqType);
		tr.setSingle(single);
		tr.setMaxMemory(mem);
		tr.setCPU(cpu);
		tr.setOutput(out);
		if(paired) tr.setRunAsPaired();
		tr.addArg("2>/dev/null");
		tr.exec();
	}
	
	public static void runDual(String seqType, String left, String right, long mem, int cpu, String out) {
		TrinityWrapper tr = new TrinityWrapper();
		tr.setSeqType(seqType);
		tr.setLeft(left);
		tr.setRight(right);
		tr.setMaxMemory(mem);
		tr.setCPU(cpu);
		tr.setOutput(out);
		tr.addArg("2>/dev/null");
		tr.exec();
	}
	
	// solve dependency
	public static boolean solve() {
		String cmd = PathConfig.TrinityPath + " 2>&1";
		String[] raw = Shell.exec(cmd);
		if(raw[0].contains("not found")) return false;
		for(int error_loc = 0; !raw[error_loc].contains("Required"); error_loc++) {
			if(error_loc + 1 == raw.length) return false; 
		}
		return true;
	}
}
