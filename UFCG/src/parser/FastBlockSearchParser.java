package parser;

import entity.BlockProfileEntity;
import envs.toolkit.FileStream;

public class FastBlockSearchParser {
	public static void parse(BlockProfileEntity bp, String blockPath) throws java.io.IOException {
		FileStream blockStream = new FileStream(blockPath, 'r');
		
		String line, cbuf = null;
		while((line = blockStream.readLine()) != null) {
			if(line.startsWith("Hits")) cbuf = line.split(" ")[3];
			if(line.startsWith("Mult.")) {
				int spos = Integer.parseInt(blockStream.readLine().split("\t")[0]), epos = -1;
				while(!(line = blockStream.readLine()).startsWith("-")) {
					epos = Integer.parseInt(line.split("\t")[0]);
				}
				
				if(epos >= 0) bp.update(cbuf, spos, epos);
				else bp.update(cbuf, spos, spos);
			}
		}
		
		blockStream.close();
	}
}
