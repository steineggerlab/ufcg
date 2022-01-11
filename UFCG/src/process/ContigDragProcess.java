package process;

import envs.config.GenericConfig;
import envs.config.PathConfig;
import envs.toolkit.ANSIHandler;
import envs.toolkit.FileStream;
import envs.toolkit.Prompt;
import entity.BlockProfileEntity;

import java.util.List;
import java.util.ArrayList;

public class ContigDragProcess {
	static List<String> dragged = new ArrayList<String>();
	
	// drag contigs from block profile, and return their locations
	public static List<String> drag(String seqPath, BlockProfileEntity bp) throws java.io.IOException {
		List<String> ctgPaths = new ArrayList<String>();
		FileStream seqStream = new FileStream(seqPath, 'r');
		String line = seqStream.readLine();
		
		int ci = 0;
		while(ci < bp.getCnt()) {
			if(line.contains(bp.getCtg(ci))) {
				String ctg = bp.getCtg(ci);
				String ctgPath = String.format("%s%s%s_%s.fna",
						PathConfig.TempPath, GenericConfig.TEMP_HEADER, GenericConfig.ACCESS, ctg);
				
				if(!dragged.contains(ctg)) {
					dragged.add(ctg);
					
					FileStream outStream = new FileStream(ctgPath, 'w');
					outStream.isTemp();
					Prompt.talk("Dragging block-containing contig " + bp.getCtg(ci) +
							" from the assembly on : " + ANSIHandler.wrapper(outStream.PATH, 'y'));
					
					outStream.println(line);				
					line = seqStream.readLine();
					while(line != null) {
						if(line.startsWith(">")) break;
						outStream.println(line);
						line = seqStream.readLine();	
					}
				
					outStream.close();	
				}
				
				// handle multiple block containing contig
				ctgPaths.add(ctgPath);
				while(++ci < bp.getCnt()) {
					if(!ctg.equals(bp.getCtg(ci))) break;
					ctgPaths.add(ctgPath);
				}
			}
			else {
				line = seqStream.readLine();
				while(line != null) {
					if(line.startsWith(">")) break;
					line = seqStream.readLine();
				}
			}
			
			if(line == null) {
				// handle exception
				break;
			}
		}
		
		seqStream.close();
		return ctgPaths;
	}
}
