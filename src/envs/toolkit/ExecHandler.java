package envs.toolkit;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public abstract class ExecHandler {
	protected String job = null;
	protected List<String> args = null;
	protected Map<String, String> argMap = null;
	
	protected void init(String job) {
		this.job = job;
		this.args = new ArrayList<String>();
		this.argMap = new HashMap<String, String>();
	}
	
	protected void addArg(String opt, String val) {
		args.add(opt);
		argMap.put(opt, " " + val);
	}
	protected void addArg(String val) {
		args.add(val);
		argMap.put(val, "");
	}
	
	private String buildCmd() {
		String cmd = job;
		for(String arg : args) {
			cmd += " " + arg + argMap.get(arg);
		}
		return cmd;
	}
	
	protected String[] exec() {
		String carr = buildCmd();
		return Shell.exec(carr);
	}
}
