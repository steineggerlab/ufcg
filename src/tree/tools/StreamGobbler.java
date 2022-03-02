package tree.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class StreamGobbler extends Thread {
	
	InputStream is;
	String type;
	boolean showMessage = false;
	String log;

	public StreamGobbler(InputStream is, String type, boolean showMessage) {
		this.is = is;
		this.type = type;
		this.showMessage = showMessage;
	}

	public String LogMessage() {
		return log;
	}

	@Override
	public void run() {
		try {
			InputStreamReader isr = new InputStreamReader(is);
			BufferedReader br = new BufferedReader(isr);
			String line = null;
			StringBuffer sb = new StringBuffer();
			while ((line = br.readLine()) != null) {
				sb.append(line);
				if (showMessage)
					System.out.println(type + "> " + line);
			}
			log = sb.toString();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}

		// System.out.println(type+" ended.");
	}
}