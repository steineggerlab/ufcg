package tree.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import envs.toolkit.Prompt;
import pipeline.ExceptionHandler;

public class LabelReplacer {

public static String replace_name_str(String ori_str, Map<String, String> replaceMap) {

	String[] nodes = ori_str.split("zZ");
	ReplaceAcc ra = new ReplaceAcc();

	for (int i = 1; i < nodes.length; i = i + 2) {
		String uid = nodes[i];
		String label = replaceMap.get(uid);
		ra.add(uid, label);
	}
	return ra.replace(ori_str, true);

}

public static void replace_name(String in_filename, String out_filename, Map<String, String> replaceMap) {
	if (in_filename == null) {
		return;
	}
	try {
		String ori_str = readTextFile2StringWithCR(in_filename);
		String new_str = replace_name_str(ori_str, replaceMap);
		try {
			FileWriter fw = new FileWriter(out_filename);
			fw.write(new_str);
			fw.close();
		} catch (IOException e) {
			ExceptionHandler.handle(e);
		}

	} catch (IOException e) {
		ExceptionHandler.handle(e);
	}

}

public static void replace_name_delete(String in_filename, String out_filename, Map<String, String> replaceMap) {
	Prompt.talk(String.format("Replacing labels... [%s -> %s]", in_filename, out_filename));
	if (in_filename == null) {
		return;
	}
	try {
		String ori_str = readTextFile2StringWithCR(in_filename);
		String new_str = replace_name_str(ori_str, replaceMap);

		File in_file = new File(in_filename);

		if (in_file.getAbsoluteFile().getParentFile().canWrite()) {
			in_file.delete();
		}
		try {
			FileWriter fw = new FileWriter(out_filename);
			fw.write(new_str);
			fw.close();
		} catch (IOException e) {
			ExceptionHandler.handle(e);
		}

	} catch (IOException e) {
		ExceptionHandler.handle(e);
	}

}

static public String readTextFile2StringWithCR(String filename) throws IOException // with carrige return
{
	StringBuilder sb = new StringBuilder();

	FileReader fr = new FileReader(filename);
	BufferedReader br = new BufferedReader(fr);
	String line;
	while ((line = br.readLine()) != null) {
		sb.append(line).append("\n");
	}
	br.close();
	fr.close();

	return new String(sb);
}
}
