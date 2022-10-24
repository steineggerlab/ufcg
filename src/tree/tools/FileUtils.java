package tree.tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class FileUtils {
	static public String readTextFile2StringWithCR(String filename)  // with carrige return
	{
		StringBuilder sb = new StringBuilder();
		
		try {
			FileReader fr = new FileReader(filename);
			BufferedReader br = new BufferedReader(fr);
		String line;
		while ((line = br.readLine()) != null)
		{
			sb.append(line).append("\n");
		}
		br.close();
		fr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new String(sb);
	}

}
