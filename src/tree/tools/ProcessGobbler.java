package tree.tools;

public class ProcessGobbler {
	int exitValue;
	String log;
	
	public ProcessGobbler(int exitValue, String log) {
		super();
		this.exitValue = exitValue;
		this.log = log;
	}
	public int getExitValue() {
		return exitValue;
	}

	public String getLog() {
		if(log == null) return "NULL";
		return log;
	}


}
