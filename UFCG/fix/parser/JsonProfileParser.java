package parser;

import envs.toolkit.FileStream;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class JsonProfileParser {
	public JSONObject genomeObject, runObject, dataObject;
	
	public JsonProfileParser() {}
	public void parse(String jsonPath) throws ParseException, java.io.IOException {
		JSONParser jp = new JSONParser();
		
		/* build JSON string and root object */
		FileStream jstream = new FileStream(jsonPath, 'r');
		String jstring = "", buf;
		while((buf = jstream.readLine()) != null) jstring += buf;
		
		JSONObject rootObject = (JSONObject) jp.parse(jstring);
		
		/* parse genome_info data */
		this.genomeObject = (JSONObject) rootObject.get("genome_info");
		this.runObject    = (JSONObject) rootObject.get("run_info");
		this.dataObject   = (JSONObject) rootObject.get("data");
	}
}
