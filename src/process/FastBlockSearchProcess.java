package process;

import envs.config.GenericConfig;
import envs.config.PathConfig;
import envs.toolkit.ANSIHandler;
import envs.toolkit.FileStream;
import envs.toolkit.Prompt;
import entity.BlockProfileEntity;
import parser.FastBlockSearchParser;
import wrapper.FastBlockSearchWrapper;

public class FastBlockSearchProcess {
	// handling routine for a specific core gene
	public static BlockProfileEntity handle(String seqPath, String famPath, String cg) throws java.io.IOException {
		BlockProfileEntity bp = new BlockProfileEntity(cg, famPath);

		String outPath = String.format("%s%s%s_%s.blk", PathConfig.TempPath, GenericConfig.TEMP_HEADER, GenericConfig.ACCESS, cg);
		String errPath = String.format("%s%s%s_%s.err", PathConfig.TempPath, GenericConfig.TEMP_HEADER, GenericConfig.ACCESS, cg);
		FileStream.isTemp(outPath);
		FileStream.isTemp(errPath);
		
		Prompt.talk("fastBlockSearch is searching genome to find the blocks with the gene profile...");
		FastBlockSearchWrapper.runFastBlockSearch(seqPath, famPath, outPath, errPath);
		
		Prompt.talk("Parsing block search result written on : " + ANSIHandler.wrapper(outPath, 'y'));
		FastBlockSearchParser.parse(bp, outPath);
		
		Prompt.talk("fastBlockSearch found " + bp.getCnt() + " block(s) containing the target gene.");
		FileStream.wipe(outPath);
		
		return bp;
	}
}
