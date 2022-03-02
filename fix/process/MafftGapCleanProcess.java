package process;

import envs.toolkit.*;
import java.util.*;
import java.io.IOException;

public class MafftGapCleanProcess {
	private class MafftPair {
		protected String taxon, mseq; // mafft aligned sequence
		
		public MafftPair(String taxon) {
			this.taxon = taxon;
			this.mseq  = "";
		}
		
		protected void appendLine(String line) {
			this.mseq += line;
		}
	}
	
	private List<MafftPair> mafftList;
	private int alen; // aligned sequence length
	private static final int FASTA_LSIZE = 60; // FASTA format single line character count
	
	private boolean integrityTest() {
		int stval = this.mafftList.get(0).mseq.length();
		
		Prompt.talk("Testing file integrity...");
		for(MafftPair mp : mafftList) {
			if(mp.mseq.length() != stval) {
				Prompt.print(String.format("%s - Query %s has sequence with %d characters; expected : %d", 
						ANSIHandler.wrapper("FAILED", 'r'), mp.taxon.substring(1), mp.mseq.length(), stval));
				return false;
			}
		}
		
//		if(VERB) System.out.println(ANSIHandler.wrapper("Passed", 'g'));
		alen = stval;
		return true;
	}
	
	private boolean[] createGapVector() {
		Prompt.talk("Creating gap vector...");
		
		boolean[] gvec = new boolean[alen];
		
		int[] gcount = new int[alen];
		for(MafftPair mp : mafftList) {
			for(int i = 0; i < alen; i++) {
				if(mp.mseq.charAt(i) == '-') gcount[i]++;
			}
		}
		
		int spc = mafftList.size();
		for(int i = 0; i < alen; i++) gvec[i] = gcount[i] < spc / 2;
		
		return gvec;
	}
	
	private void writeAlign(FileStream outStream, boolean[] gvec) throws IOException {
		for(MafftPair mp : mafftList) {
			outStream.println(">" + mp.taxon);
			
			String cleanAlign = "";
			for(int i = 0; i < alen; i++) {
				if(gvec[i]) cleanAlign += mp.mseq.charAt(i);
				if(cleanAlign.length() == FASTA_LSIZE) {
					outStream.println(cleanAlign);
					cleanAlign = "";
				}
			}
			if(cleanAlign.length() > 0) outStream.println(cleanAlign);
		}
		
		outStream.close();
		Prompt.talk("Gap cleaned alignment written on " + ANSIHandler.wrapper(outStream.PATH, 'y'));
	}
	
	private void createList(FileStream inStream) throws IOException {
		this.mafftList = new ArrayList<MafftPair>();
		
		String buf; MafftPair pair = null;
		while((buf = inStream.readLine()) != null) {
			if(buf.startsWith(">")) {
				pair = new MafftPair(buf.substring(1));
				mafftList.add(pair);
			}
			else pair.appendLine(buf);
		}
	}
	
	public static void clean(String inPath, String outPath) throws IOException {
		FileStream inStream  = new FileStream(inPath, 'r'),
				   outStream = new FileStream(outPath, 'w');
		
		MafftGapCleanProcess mgc = new MafftGapCleanProcess();
		mgc.createList(inStream);
		inStream.close();
		
		if(!mgc.integrityTest()) return;
		boolean[] gvec = mgc.createGapVector();
		mgc.writeAlign(outStream, gvec);
		outStream.close();
	}
}
