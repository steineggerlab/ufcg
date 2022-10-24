package tree.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MafftForGeneSet {

	AlignMode alignMode;
	String ProgramPath;

	private final List<String> arguments = new ArrayList<>();

	public MafftForGeneSet(String programPath, AlignMode alignMode) {
		this.alignMode = alignMode;
		this.ProgramPath = programPath;
		arguments.add("bash");
		arguments.add("-c");
	}

	public void setInputOutput(String input, String output) {
		arguments.add(ProgramPath + " --thread 1 " + input + " > " + output);
	}

	public ProcessGobbler execute() throws IOException, InterruptedException{

		Process mafft = new ProcessBuilder(arguments).start();

		StreamGobbler stdOut = new StreamGobbler(mafft.getInputStream(), null, false);
		StreamGobbler stdError = new StreamGobbler(mafft.getErrorStream(), null, false);

		stdOut.start();
		stdError.start();

		mafft.waitFor();

		int exitValue = mafft.exitValue();

		return new ProcessGobbler(exitValue, stdError.LogMessage());
	}
	
	@Override
	public String toString() {
		return String.join(" ", arguments);
	}
}