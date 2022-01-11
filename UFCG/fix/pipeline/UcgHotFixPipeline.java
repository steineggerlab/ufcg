package pipeline;

import parser.JsonProfileParser;
import process.JsonBuildProcess;
import process.OrfHotFixProcess;

import org.json.simple.JSONObject;

import envs.config.GenericConfig;
import envs.toolkit.Prompt;

/*
 * USAGE :
 * 		java -jar ucgHotFix.jar [ucg_in] [ucg_out]
 */
@SuppressWarnings("unchecked")
public class UcgHotFixPipeline {
	public static void main(String[] args) throws Exception {
		if(args.length != 2) {
			System.err.println("USAGE : java -jar ucgHotFix.jar [ucg_in] [ucg_out]");
			System.exit(1);
		}
		
		GenericConfig.setHeader("UHFIX");
		GenericConfig.TSTAMP = true;
		
		String ipath = args[0], opath = args[1];
		Prompt.print(String.format("Fixing UCG profile : %s...", ipath));
		
		JsonProfileParser jp = new JsonProfileParser();
		jp.parse(ipath);
		
		JSONObject fixedRoot = new JSONObject();
		fixedRoot.put("genome_info", OrfHotFixProcess.fixGenome(jp.genomeObject));
		fixedRoot.put("run_info", OrfHotFixProcess.fixRun(jp.runObject));
		fixedRoot.put("data", OrfHotFixProcess.fixData(jp.dataObject));
		
		Prompt.print(String.format("Writing fixed profile on : %s...", opath));
		JsonBuildProcess.build(fixedRoot, opath);
	}
}
