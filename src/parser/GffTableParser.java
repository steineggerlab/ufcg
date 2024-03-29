package parser;

import entity.GffLocationEntity;
import entity.ProfilePredictionEntity;
import envs.toolkit.FileStream;

public class GffTableParser {
	public static void parse(ProfilePredictionEntity pp, String ctgPath, String gffPath) throws java.io.IOException {
		FileStream gffStream = new FileStream(gffPath, 'r');
		String line;
		
		while((line = gffStream.readLine()) != null) {
			// gene found
			if(line.contains("start gene")) {
				gffStream.readLine();
				GffLocationEntity loc = new GffLocationEntity(pp, ctgPath, gffStream.readLine());
				while(!(line = gffStream.readLine()).startsWith("#")) loc.feed(line);
				pp.addLoc(loc);
			}
			
			// protein found
			if(line.contains("protein sequence")) {
				StringBuilder seq = new StringBuilder(line.split(" = ")[1].substring(1));
				
				// build up
				if(!seq.toString().contains("]")) {
					String tmp;
					while(!(tmp = gffStream.readLine()).contains("]")) {
						if(tmp.contains("block")) break;
						seq.append(tmp.substring(2));
					}
					seq.append(tmp.substring(2));
				}
				
				// write protein
				pp.addSeq(seq.substring(0, seq.length() - 1));
			}
		}
		
		gffStream.close();
	}
}
