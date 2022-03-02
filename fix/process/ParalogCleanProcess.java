package process;

import parser.JsonProfileParser;
import envs.config.GenericConfig;

import java.util.*;
import org.json.simple.*;

public class ParalogCleanProcess {
	private static List<String> paralogs = null;
	
	@SuppressWarnings("unchecked")
	private static void clean(JsonProfileParser jpp, String oPath) throws java.io.IOException {
		paralogs = new ArrayList<String>();
		paralogs.add("RPL2A");
		
		// handle data object
		JSONObject fixedData = new JSONObject();
		int rSgl = 0, rMul = 0, rDet = 0;
		for(Object key : jpp.dataObject.keySet()) {
			if(paralogs.contains((String) key)){
				int ngene = ((JSONArray) jpp.dataObject.get(key)).size();
				
				rDet += ngene;
				if(ngene > 1) rMul++;
				else if(ngene > 0) rSgl++;
			}
			else fixedData.put(key, jpp.dataObject.get(key));
		}
		
		
		// handle run object
		JSONObject fixedRun = new JSONObject();
		for(Object key : jpp.runObject.keySet()) {
			if(((String) key).equals("n_target_genes"))					fixedRun.put(key, ((Long) jpp.runObject.get(key)) - paralogs.size());
			else if(((String) key).equals("n_single_copy_genes"))		fixedRun.put(key, ((Long) jpp.runObject.get(key)) - rSgl);
			else if(((String) key).equals("n_multiple_copy_genes"))		fixedRun.put(key, ((Long) jpp.runObject.get(key)) - rMul);
			else if(((String) key).equals("n_total_detected_genes"))	fixedRun.put(key, ((Long) jpp.runObject.get(key)) - rDet);
			else if(((String) key).equals("target_gene_set"))			fixedRun.put(key, GenericConfig.geneString());
			else fixedRun.put(key, jpp.runObject.get(key));
		}
		
		// build new JSON file
		JSONObject fixedRoot = new JSONObject();
		fixedRoot.put("genome_info", jpp.genomeObject);
		fixedRoot.put("run_info", fixedRun);
		fixedRoot.put("data", fixedData);
		
		JsonBuildProcess.build(fixedRoot, oPath);
	}
	
	public static void main(String[] args) throws Exception {
		if(args.length != 2) {
			System.err.println("USAGE : ParalogCleaner.jar [UCG_IN] [UCG_OUT]");
			System.exit(1);
		}
		
		String iPath = args[0], oPath = args[1];
		JsonProfileParser jpp = new JsonProfileParser();
		jpp.parse(iPath);
		
		clean(jpp, oPath);
	}
}
