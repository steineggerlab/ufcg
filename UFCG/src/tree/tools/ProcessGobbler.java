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
	public void setExitValue(int exitValue) {
		this.exitValue = exitValue;
	}
	public String getLog() {
		return log;
	}
	public void setLog(String log) {
		this.log = log;
	}
	
	
}
