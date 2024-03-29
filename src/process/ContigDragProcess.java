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
	static List<String> dragged = new ArrayList<>();

	private static String match(String line, List<String> ctgs) {
		String cmp = line.substring(1).split(" ")[0].split("\t")[0];
		for(String ctg : ctgs) if(cmp.equals(ctg)) return ctg;
		return null;
	}
	
	// drag contigs from block profile, and return their locations
	public static List<String> drag(String seqPath, BlockProfileEntity bp) throws java.io.IOException {
		FileStream seqStream = new FileStream(seqPath, 'r');
		String line = seqStream.readLine();
		
		List<String> ctgs = new ArrayList<>();
		for(int i = 0; i < bp.size(); i++) if(!ctgs.contains(bp.getCtg(i))) ctgs.add(bp.getCtg(i));
		
		int ci = 0;
		while(ci < ctgs.size()) {
			String ctg = match(line, ctgs);
			if(ctg != null) {
				String ctgPath = String.format("%s%s%s_%s.fna",
						PathConfig.TempPath, GenericConfig.TEMP_HEADER, GenericConfig.ACCESS, ctg);
				
				if(!dragged.contains(ctg)) {
					dragged.add(ctg);
					
					FileStream outStream = new FileStream(ctgPath, 'w');
					outStream.isTemp();
					Prompt.talk("Dragging block-containing contig " + ctg +
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
				else {
					line = seqStream.readLine();
					while(line != null) {
						if(line.startsWith(">")) break;
						line = seqStream.readLine();
					}
				}
				ci++;
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
		
		List<String> ctgPaths = new ArrayList<>();
		for(int i = 0; i < bp.size(); i++) ctgPaths.add(String.format("%s%s%s_%s.fna",
						PathConfig.TempPath, GenericConfig.TEMP_HEADER, GenericConfig.ACCESS, bp.getCtg(i)));
		return ctgPaths;
	}
	
	public static void clean() {dragged = new ArrayList<>();}
}
