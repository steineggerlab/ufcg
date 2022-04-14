package parser;

import java.util.ArrayList;
import java.util.List;

import entity.BlockProfileEntity;
import envs.toolkit.FileStream;

public class FastBlockSearchParser {
	public static void parse(BlockProfileEntity bp, String blockPath) throws java.io.IOException {
		bp.setBlkPath(blockPath);
		FileStream blockStream = new FileStream(blockPath, 'r');
		
		String line, cbuf = null, sbuf = null;
		List<Integer> pos = new ArrayList<Integer>();
		while((line = blockStream.readLine()) != null) {
			if(line.startsWith("Hits")) cbuf = line.split(" ")[3];
			if(line.startsWith("Score")) sbuf = line.split(":")[1];
			if(line.startsWith("Mult.")) {
				pos.add(Integer.parseInt(blockStream.readLine().split("\t")[0]));
				while(!(line = blockStream.readLine()).startsWith("-")) {
					pos.add(Integer.parseInt(line.split("\t")[0]));
				}
				// Prompt.test(String.format("Updating : %s [%d - %d]", cbuf, pos.get(0), pos.get(pos.size() - 1)));
				bp.update(cbuf, pos, Double.parseDouble(sbuf));
				pos = new ArrayList<Integer>();
			}
		}
		
		blockStream.close();
	}
}
