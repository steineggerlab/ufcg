package process;

import entity.ProfilePredictionEntity;
import entity.JsonProfileEntity;
import envs.toolkit.FileStream;

import java.util.List;
import org.json.simple.JSONObject;

public class JsonBuildProcess {
	private static String jsonStrFix(String jstr) {
		StringBuilder jsonFix = new StringBuilder();
		
		int level = 0;
		for(int i = 0; i < jstr.length(); i++) {		
			char ch = jstr.charAt(i);
			if(ch == '\"') {
				int j = jstr.indexOf('\"', i + 1);
				jsonFix.append(jstr.substring(i, j + 1).replace("\\t", ""));
				i = j;
				continue;
			}
			
			// opener
			if(ch == '{' || ch == '[') {
				jsonFix.append(ch);
				jsonFix.append('\n');
				level++;
				for(int t = 0; t < level; t++) jsonFix.append('\t');
			}
			// closer
			else if(ch == '}' || ch == ']') {
				jsonFix.append('\n');
				level--;
				for(int t = 0; t < level; t++) jsonFix.append('\t');
				jsonFix.append(ch);
			}
			// comma
			else if(ch == ',') {
				jsonFix.append(ch);
				jsonFix.append('\n');
				for(int t = 0; t < level; t++) jsonFix.append('\t');
			}
			// vertical bar
			else if(ch == '|') {
				jsonFix.append(',');
			}
			// default
			else jsonFix.append(ch);
		}
		
		return jsonFix.toString();
	}
	
	public static void build(JSONObject obj, String jsonPath) {
		String jstr = obj.toJSONString();
		try {
			FileStream jsonStream = new FileStream(jsonPath, 'w');
			jsonStream.println(jsonStrFix(jstr));
			jsonStream.close();
		}
		catch(java.io.IOException e) {
			e.printStackTrace(); System.exit(1);
		}
	}
	public static void build(List<ProfilePredictionEntity> pps, String jsonPath) {
		build(new JsonProfileEntity(pps).root, jsonPath);
	}
}
