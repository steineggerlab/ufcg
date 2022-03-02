package process.main;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import envs.config.GenericConfig;
import envs.toolkit.FileStream;
import envs.toolkit.Prompt;

public class ContigDragMain {
	public static boolean drag(String seqPath, String ctgName, String outPath) throws java.io.IOException {
		FileStream seqStream = new FileStream(seqPath, 'r');
		String line = seqStream.readLine();
		
		while(true) {
			if(line.contains(ctgName)) {
				FileStream outStream = new FileStream(outPath, 'w');
				outStream.println(line);
				
				line = seqStream.readLine();
				while(line != null) {
					if(line.startsWith(">")) break;
					
					outStream.println(line);
					line = seqStream.readLine();	
				}
				
				outStream.close();
				break;
			}
			else {
				line = seqStream.readLine();
				while(line != null) {
					if(line.startsWith(">")) break;
					line = seqStream.readLine();
				}
			}
			
			if(line == null) {
				Prompt.print("Contig not found.");
				seqStream.close();
				return false;
			}
		}
		
		seqStream.close();
		return true;
	}
	
	/* 
	 * Runnable : ProcessContigDrag.jar
	 * Arguments :
	 * 		-a <ASSEM> 		(Required) : Input assembly file path
	 * 		-c <CTG>		(Required) : Contig name
	 * 		-o <OUT>		(Required) : Output file path
	 * 		
	 * Output : Extracted contig
	 */
	public static void main(String[] args) throws Exception {
		GenericConfig.setHeader("CtgDrag");
		String AARG, CARG, OARG;
		
		Options opts = new Options();
		opts.addOption("a", true, "Input assembly file path");
		opts.addOption("c", true, "Contig name");
		opts.addOption("o", true, "Output file path");
		
		CommandLineParser clp = new DefaultParser();
		CommandLine cmd = clp.parse(opts, args);
		if(!cmd.hasOption("a") || !cmd.hasOption("c") || !cmd.hasOption("o")) {
			new HelpFormatter().printHelp("UUCGf.process.ContigDrag", opts);
			return;
		}
		
		AARG = cmd.getOptionValue("a");
		CARG = cmd.getOptionValue("c");
		OARG = cmd.getOptionValue("o");
		
		drag(AARG, CARG, OARG);
//		if(drag(AARG, CARG, OARG)) Prompt.print("Contig written on : " + OARG);
	}
}
