package envs.toolkit;

import java.util.*;

public class GenomeTranslator {
	public static final String[] CODON = {
			"TTT", "TTC", "TTA", "TTG", "TCT", "TCC", "TCA", "TCG", "TAT", "TAC", "TAA", "TAG", "TGT", "TGC", "TGA", "TGG",
			"CTT", "CTC", "CTA", "CTG", "CCT", "CCC", "CCA", "CCG", "CAT", "CAC", "CAA", "CAG", "CGT", "CGC", "CGA", "CGG",
			"ATT", "ATC", "ATA", "ATG", "ACT", "ACC", "ACA", "ACG", "AAT", "AAC", "AAA", "AAG", "AGT", "AGC", "AGA", "AGG",
			"GTT", "GTC", "GTA", "GTG", "GCT", "GCC", "GCA", "GCG", "GAT", "GAC", "GAA", "GAG", "GGT", "GGC", "GGA", "GGG"
	};
	
	public static final String[] AMINO = {
			"F", "F", "L", "L", "S", "S", "S", "S", "Y", "Y", "*", "*", "C", "C", "*", "W",
			"L", "L", "L", "L", "P", "P", "P", "P", "H", "H", "Q", "Q", "R", "R", "R", "R",
			"I", "I", "I", "M", "T", "T", "T", "T", "N", "N", "K", "K", "S", "S", "R", "R",
			"V", "V", "V", "V", "A", "A", "A", "A", "D", "D", "E", "E", "G", "G", "G", "G"
	};
	
	public static Map<String, String> TSLN = new HashMap<>();
	private static boolean mapCreated = false;
	public static void createMap() {
		if(mapCreated) return;
		for(int i = 0; i < CODON.length; i++) TSLN.put(CODON[i], AMINO[i]);
		mapCreated = true;
	}
	
	private static boolean valid(char base) {
		switch(base) {
		case 'T':
		case 'C':
		case 'A':
		case 'G': 
		case 'X': return true;
		default : return false;
		}
	}
	
	public static String translate(String codon) {
		if(!mapCreated) createMap();
		codon = codon.toUpperCase();
		if(codon.length() != 3) return "#";
		for(int i = 0; i < 3; i++) if(!valid(codon.charAt(i))) return "?";
		if(codon.contains("X")) return "X";
		return TSLN.get(codon);
	}
	
	public static String transeq(String seq) {
		StringBuilder tsln = new StringBuilder(); int i;
		for(i = 3; i < seq.length(); i += 3) tsln.append(translate(seq.substring(i - 3, i)));
		return tsln + translate(seq.substring(i - 3));
	}
	
	public static boolean equal(String seqx, String seqy) {
		if(seqx.length() != seqy.length()) return false;
		for(int i = 0; i < seqx.length(); i++) {
			if(seqx.charAt(i) != 'X' && seqy.charAt(i) != 'X' && seqx.charAt(i) != seqy.charAt(i)) return false;
		}
		return true;
	}
	
	// return valid reading frame
	public static int frame(String orf, String prtn) {
		int half = prtn.length() / 2;
		
		String tpref, ppref = prtn.substring(0, half);
		
		tpref = transeq(orf.substring(0, 3 * half));
		if(equal(tpref, ppref)) return 0;
		tpref = transeq(orf.substring(1, 3 * half + 1));
		if(equal(tpref, ppref)) return 1;
		tpref = transeq(orf.substring(2, 3 * half + 2));
		if(equal(tpref, ppref)) return 2;
		
		return -1;
	}
	
	// return random codon encoding amino acid
	/*
	public static String rcodon(String amino) {
		List<String> avail = new ArrayList<>();
		for(int i = 0; i < AMINO.length; i++) {
			if(amino.equals(AMINO[i])) avail.add(CODON[i]);
		}
		
		return avail.get(new Random().nextInt(avail.size()));
	}
	*/
}
