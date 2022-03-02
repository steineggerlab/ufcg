package parser;

import entity.GffLocationEntity;
import envs.config.GenericConfig;
import envs.toolkit.FileStream;
import envs.toolkit.Prompt;

public class FastaContigParser {
	private static char reverse(char base){
		switch(base){
			case 'A' : return 'T';
			case 'T' : return 'A';
			case 'C' : return 'G';
			case 'G' : return 'C';
			default : return 'X';
		}	
	}

	public static String revcomp(String seq){
		String rev = "";

		for(int i = seq.length(); i > 0; i--){
			rev += reverse(seq.charAt(i - 1));
		}

		return rev;
	}
	
	public static String parse(GffLocationEntity loc) throws java.io.IOException {
		Prompt.talk(String.format("Extracting cDNA sequence of gene %s...", loc.refProfile.refBlock.cg));
		String seq = "";
		FileStream ctgStream = new FileStream(loc.ctgPath, 'r');
		String buf = ctgStream.readLine();
		while(buf.startsWith(">")) buf = ctgStream.readLine();
		
		int bsize = buf.length(), offset = 1, bloc;
		while(offset + bsize < loc.trxHead) {
			buf = ctgStream.readLine();
			offset += bsize;
		}
		
		seq += buf;
		for(bloc = offset; bloc < loc.trxTail; bloc += bsize) seq += ctgStream.readLine();
		ctgStream.close();
		
		String gDNA = seq.substring(loc.trxHead - offset, loc.trxTail - offset + 1);
		
		String cDNA = gDNA;
		if(loc.intronHeads.size() > 0 && !GenericConfig.INTRON) {
			cDNA = seq.substring(loc.trxHead - offset, loc.intronHeads.get(0) - offset);
			for(int i = 0; i < loc.intronHeads.size() - 1; i++) {
				cDNA += seq.substring(loc.intronTails.get(i) - offset + 1, loc.intronHeads.get(i + 1) - offset);
			}
			cDNA += seq.substring(loc.intronTails.get(loc.intronHeads.size() - 1) - offset + 1, loc.trxTail - offset + 1);
		}
		
		return loc.fwd ? cDNA : revcomp(cDNA);
	}
}
