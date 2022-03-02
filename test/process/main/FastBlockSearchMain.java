package process.main;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import entity.BlockProfileEntity;
import envs.config.GenericConfig;
import envs.config.PathConfig;
import envs.toolkit.FileStream;
import envs.toolkit.Shell;
import parser.FastBlockSearchParser;
import wrapper.FastBlockSearchWrapper;

public class FastBlockSearchMain {
	private static boolean _main = false;
	
	// handling routine for a specific core gene
	public static BlockProfileEntity handle(String seqPath, String famPath, String cg) throws java.io.IOException {
		BlockProfileEntity bp = new BlockProfileEntity(cg, famPath);
		
		if(_main) {
			FastBlockSearchWrapper.runFastBlockSearch(seqPath, famPath, "/tmp/tmp.blk");
			FastBlockSearchParser.parse(bp, "/tmp/tmp.blk");
			Shell.exec("rm /tmp/tmp.blk");
			return bp;
		}
		
		String outPath = String.format("%s%s%s_%s.blk", PathConfig.TempPath, GenericConfig.TEMP_HEADER, GenericConfig.ACCESS, cg);
		FileStream.isTemp(outPath);
		FastBlockSearchWrapper.runFastBlockSearch(seqPath, famPath, outPath);
		FastBlockSearchParser.parse(bp, outPath);
		FileStream.wipe(outPath);
		
		return bp;
	}
	
	/*
	 * Runnable : ProcessFastBlockSearch.jar
	 * Arguments :
	 * 		-i <SEQ> 		(Required) : Input sequence file path
	 * 		-p <PRFL>		(Required) : Block profile file path
	 * 		-o <OUT>		(Required) : Output file path
	 * 		--cutoff <C> 	(Optional) : Custom cutoff (default : 0.5)
	 * 		--binary <PATH>	(Optional) : fastBlockSearch binary path
	 * Output : 
	 * 		TSV formatted block search result
	 */
	public static void main(String[] args) throws Exception {
		_main = true;
		String IARG, PARG, OARG;
		
		Options opts = new Options();
		opts.addOption("i", true, "Input sequence file path");
		opts.addOption("p", true, "Block profile file path");
		opts.addOption("o", true, "Output file path");
		opts.addOption("cutoff", true, "Custom cutoff (default : 0.5)");
		opts.addOption("binary", false, "Specify fastBlockSearch binary path");
		opts.addOption("v", false, "Program verbosity");
		
		CommandLineParser clp = new DefaultParser();
		CommandLine cmd = clp.parse(opts, args);
		if(!cmd.hasOption("i") || !cmd.hasOption("p") || !cmd.hasOption("o")) {
			new HelpFormatter().printHelp("Fungible.process.FastBlockSearch", opts);
			return;
		}
		
		IARG = cmd.getOptionValue("i");
		PARG = cmd.getOptionValue("p");
		OARG = cmd.getOptionValue("o");
		if(cmd.hasOption("cutoff")) GenericConfig.setFastBlockSearchCutoff(
				Double.parseDouble(cmd.getOptionValue("cutoff")));
		if(cmd.hasOption("binary")) PathConfig.setFastBlockSearchPath(cmd.getOptionValue("binary"));
		if(cmd.hasOption("v")) GenericConfig.VERB = true;
		
		BlockProfileEntity bp = handle(IARG, PARG, null);
		
		FileStream outStream = new FileStream(OARG, 'w');
		outStream.println("Contig\tStart\tEnd");
		for(int i = 0; i < bp.getCnt(); i++) {
			outStream.println(String.format("%s\t%d\t%d", bp.getCtg(i), bp.getSpos(i), bp.getEpos(i)));
		}
		outStream.close();
	}
}
