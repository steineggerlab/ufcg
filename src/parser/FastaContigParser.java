package parser;

import entity.GffLocationEntity;
import entity.MMseqsSearchResultEntity;
import envs.config.GenericConfig;
import envs.toolkit.FileStream;
import envs.toolkit.Prompt;

public class FastaContigParser {
	private final static char[] rc = {
			'T', 'V', 'G', 'H', 'N', 'N', 'C', 'D', 'N', 'N',
			'M', 'N', 'K', 'N', 'N', 'N', 'N', 'Y', 'S', 'A',
			'A', 'B', 'W', 'N', 'R', 'N',
			'N', 'N', 'N', 'N', 'N', 'N',
			't', 'v', 'g', 'h', 'n', 'n', 'c', 'd', 'n', 'n',
			'm', 'n', 'k', 'n', 'n', 'n', 'n', 'y', 's', 'a',
			'a', 'b', 'w', 'n', 'r', 'n',
	};
	private static char reverse(char base){
		return rc[base - 'A'];
	}

	public static String revcomp(String seq){
		StringBuilder rev = new StringBuilder();

		for(int i = seq.length(); i > 0; i--){
			rev.append(reverse(seq.charAt(i - 1)));
		}

		return rev.toString();
	}
	
	public static String parse(GffLocationEntity loc) throws java.io.IOException {
		Prompt.talk(String.format("Extracting cDNA sequence of gene %s...", loc.refProfile.refBlock.cg));
		StringBuilder seq = new StringBuilder();
		FileStream ctgStream = new FileStream(loc.ctgPath, 'r');
		String buf = ctgStream.readLine();
		while(buf.startsWith(">")) buf = ctgStream.readLine();
		
		int bsize = buf.length(), offset = 1, bloc;
		while(offset + bsize < loc.trxHead) {
			buf = ctgStream.readLine();
			offset += bsize;
		}
		
		seq.append(buf);
		for(bloc = offset; bloc < loc.trxTail; bloc += bsize) seq.append(ctgStream.readLine());
		ctgStream.close();

		StringBuilder cDNA = new StringBuilder(seq.substring(loc.trxHead - offset, loc.trxTail - offset + 1));
		if(loc.intronHeads.size() > 0 && !GenericConfig.INTRON) {
			cDNA = new StringBuilder(seq.substring(loc.trxHead - offset, loc.intronHeads.get(0) - offset));
			for(int i = 0; i < loc.intronHeads.size() - 1; i++) {
				cDNA.append(seq.substring(loc.intronTails.get(i) - offset + 1, loc.intronHeads.get(i + 1) - offset));
			}
			cDNA.append(seq.substring(loc.intronTails.get(loc.intronHeads.size() - 1) - offset + 1, loc.trxTail - offset + 1));
		}
		
		return loc.fwd ? cDNA.toString() : revcomp(cDNA.toString());
	}
	
	public static String parse(MMseqsSearchResultEntity res, int index) throws java.io.IOException {
		StringBuilder seq = new StringBuilder();
		String buf;

		String contig = res.getContig(index);
		int start = res.getStart(index), end = res.getEnd(index);
		
		/* estimate contig length */
		int len = 0;
		
		// find contig header
		FileStream stream = new FileStream(res.srcPath, 'r');
		while(!stream.readLine().contains(contig));
		
		// trace length
		for(buf = stream.readLine(); buf != null; buf = stream.readLine()) {
			if(buf.contains(">")) break;
			len += buf.length();
		}
		
		/* trim offset */
		if(start < 0) start = 0;
		if(end > len) end = len;
		
		/* extract sequence block */
		stream = new FileStream(res.srcPath, 'r');
		while(!stream.readLine().contains(contig));
		
		buf = stream.readLine();
		int sbuf = 0, ebuf = buf.length(), bsize = buf.length();
		// end-point
		while (sbuf < end) {
			if (ebuf >= start) { // mid-point
				int s = 0, e = buf.length();
				if (start > sbuf) s = start - sbuf; // handle starting fragment
				if (end < ebuf) e = bsize - ebuf + end; // handle ending fragment
				seq.append(buf, s, e);
			}
			buf = stream.readLine();
			sbuf += bsize;
			ebuf += bsize;
		}
		stream.close();
		
		/* sanity check
		System.out.println(seq);
		System.out.println("");
		
		String full = "";
		stream = new FileStream(res.path, 'r');
		while(!(buf = stream.readLine()).contains(contig));
		for(buf = stream.readLine(); buf != null; buf = stream.readLine()) {
			if(buf.contains(">")) break;
			full += buf;
		}
		stream.close();
		
		System.out.println(String.format("Ground truth [%d - %d]", start, end));
		System.out.println(full.substring(start, end));
		System.out.println("");
		*/
		
		return seq.toString();
	}
}
