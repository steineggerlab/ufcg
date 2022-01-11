package process.main;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import java.util.List;
import java.util.ArrayList;

import entity.BlockProfileEntity;
import envs.config.GenericConfig;
import envs.config.PathConfig;
import envs.toolkit.FileStream;
import envs.toolkit.Prompt;
import envs.toolkit.Shell;
import parser.FastBlockSearchParser;
import wrapper.FastBlockSearchWrapper;

public class BlockExistenceMain {
	/*
	 * Runnable : BlockExistence.jar
	 * Arguments :
	 * 		-i <SEQ> 		(Required) : Input sequence file path
	 * 		-p <PRFL>		(Required) : Directory containing block profiles
	 * 		-l <LIST>		(Required) : Gene names list path
	 * 		-o <OUT>		(Required) : Output file path
	 * 		--cutoff <C> 	(Optional) : Custom cutoff (default : 0.5)
	 * 		--binary <PATH>	(Optional) : fastBlockSearch binary path
	 * Output : 
	 * 		TSV formatted block search result
	 */
	public static void main(String[] args) throws Exception {
		String IARG, PARG, LARG, OARG;
		
		Options opts = new Options();
		opts.addOption("i", true, "Input sequence file path");
		opts.addOption("p", true, "Directory containing block profiles");
		opts.addOption("l", true, "Output file path");
		opts.addOption("o", true, "Gene names list path");
		opts.addOption("cutoff", true, "Custom cutoff (default : 0.5)");
		opts.addOption("binary", false, "Specify fastBlockSearch binary path");
		opts.addOption("v", false, "Program verbosity");
		
		CommandLineParser clp = new DefaultParser();
		CommandLine cmd = clp.parse(opts, args);
		if(!cmd.hasOption("i") || !cmd.hasOption("p") || !cmd.hasOption("o")) {
			new HelpFormatter().printHelp("Fungible.process.main.BlockExistence", opts);
			return;
		}
		
		IARG = cmd.getOptionValue("i");
		PARG = cmd.getOptionValue("p");
		LARG = cmd.getOptionValue("l");
		OARG = cmd.getOptionValue("o");
		if(cmd.hasOption("cutoff")) GenericConfig.setFastBlockSearchCutoff(
				Double.parseDouble(cmd.getOptionValue("cutoff")));
		if(cmd.hasOption("binary")) PathConfig.setFastBlockSearchPath(cmd.getOptionValue("binary"));
		if(cmd.hasOption("v")) GenericConfig.VERB = true;
		
		GenericConfig.setHeader("BExist");
		GenericConfig.TSTAMP = true;
		String query = IARG.substring(IARG.lastIndexOf("/") + 1);
		
		// read list
		FileStream listStream = new FileStream(LARG, 'r');
		List<String> genes = new ArrayList<String>(); String line;
		while((line = listStream.readLine()) != null) genes.add(line);
		
		String stat = ""; int i = 0, gc = genes.size();
		for(String gene : genes) {
			Prompt.print(String.format("%s > Running FastBlockSearch for gene %s... (%d/%d)", query, gene, ++i, gc));
			String famPath = PARG + "/uucg_fungi_" + gene + ".blk";
			String fbsPath = "/tmp/" + GenericConfig.TEMP_HEADER + gene + ".fbs";
			
			BlockProfileEntity bp = new BlockProfileEntity(gene, famPath);
			FastBlockSearchWrapper.runFastBlockSearch(IARG, famPath, fbsPath);
			FastBlockSearchParser.parse(bp, fbsPath);
			
			/*
			if(bp.getCnt() == 1) stat += "o";
			else if(bp.getCnt() == 0) stat += "x";
			else if(bp.getCnt() > 1) stat += "O";
			else stat += "?";
			*/
			
			stat += gene + "\t" + String.valueOf(bp.getCnt()) + "\n";
			Shell.exec("rm " + fbsPath);
		}
		
		FileStream outStream = new FileStream(OARG, 'w');
		outStream.println("Gene block existence profile of " + IARG);
		outStream.println(stat);
		outStream.close();
	}
}
