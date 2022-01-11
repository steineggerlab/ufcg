package pipeline;

import envs.config.GenericConfig;
import envs.toolkit.ANSIHandler;
import envs.toolkit.Prompt;
import parser.JsonProfileParser;
import process.JsonBuildProcess;

import org.json.simple.JSONObject;

/*
 * USAGE : java -jar mergeProfile.jar [UCG1] [UCG2]
 * 		Merges two fungi profile ucg files
 */

@SuppressWarnings("unchecked")
public class MergeProfilePipeline {
	private static JSONObject mergeData(JSONObject xData, JSONObject yData) {
		for(Object key : yData.keySet().toArray()) {
			xData.put(key, yData.get(key));
		}
		return xData;
	}
	
	private static JSONObject mergeRun(JSONObject xRun, JSONObject yRun) {
		JSONObject mergedRun = new JSONObject();
		mergedRun.put("n_paralogs", xRun.get("n_paralogs"));
		mergedRun.put("n_target_genes", (Long) xRun.get("n_target_genes") + (Long) yRun.get("n_target_genes"));
		mergedRun.put("n_single_copy_genes", (Long) xRun.get("n_single_copy_genes") + (Long) yRun.get("n_single_copy_genes"));
		mergedRun.put("n_multiple_copy_genes", (Long) xRun.get("n_multiple_copy_genes") + (Long) yRun.get("n_multiple_copy_genes"));
		mergedRun.put("n_total_detected_genes", (Long) xRun.get("n_total_detected_genes") + (Long) yRun.get("n_total_detected_genes"));
		mergedRun.put("run_time", xRun.get("run_time"));
		mergedRun.put("target_gene_set", GenericConfig.geneString());
		
		return mergedRun;
	}
	
	public static void main(String[] args) throws Exception {
		if(args.length != 2) {
			System.err.println("USAGE : java -jar mergeProfile.jar [UCG1] [UCG2]");
			System.exit(1);
		}
		
		GenericConfig.setHeader("MERGE");
		GenericConfig.TSTAMP = true;
		
		Prompt.print(String.format("Merging ucg profiles: %s + %s",
				ANSIHandler.wrapper(args[0], 'y'), ANSIHandler.wrapper(args[1], 'y')));
		
		JsonProfileParser xParser = new JsonProfileParser(), yParser = new JsonProfileParser();
		xParser.parse(args[0]);
		yParser.parse(args[1]);
		
		JSONObject merged = new JSONObject();
		merged.put("genome_info", xParser.genomeObject);
		merged.put("data", mergeData(xParser.dataObject, yParser.dataObject));
		merged.put("run_info", mergeRun(xParser.runObject, yParser.runObject));
		
		JsonBuildProcess.build(merged, xParser.genomeObject.get("accession") + ".ucg");
	}
}
