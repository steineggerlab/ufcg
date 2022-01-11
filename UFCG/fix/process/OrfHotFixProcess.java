package process;

import envs.config.GenericConfig;
import envs.toolkit.ANSIHandler;
import envs.toolkit.Prompt;
import parser.FastaContigParser;
import envs.toolkit.GenomeTranslator;

import java.util.Random;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

/*
 * Hot-fix process for UCG files produced with UUCGf version < 0.2.2
 * 
 * Scans input UCG profile and tests ORF-protein integrity.
 * If the ORF is invalid, fix the frame by correlating protein sequence with codon table.
 * */
@SuppressWarnings("unchecked")
public class OrfHotFixProcess {
	private static String LABEL = null, GENE = null;
	
	private static void failed(int reason) {
		Prompt.print_nnc(ANSIHandler.wrapper("FIX FAILED: ", 'r'));
		switch(reason) {
		case 1: System.out.println("could not find the valid reading frame."); break;
		case 2: System.out.println("could not find the locus for a stop codon."); break;
		case 3: System.out.println("fixed ORF does not produce the valid product."); break;
		}
		System.exit(1);
	}
	
	private static String checkORF(String prtn, String orf) {
		String valid = null;
		GenomeTranslator.createMap();
		
		if(!GenomeTranslator.equal(prtn + "*", GenomeTranslator.transeq(orf))) {
			int frame = GenomeTranslator.frame(orf, prtn);
			if(frame < 0) failed(1);
			valid = orf.substring(frame);
			
			boolean stop = orf.endsWith("TAA") || orf.endsWith("TAG") || orf.endsWith("TGA");
			if(stop && (valid.length() != prtn.length() * 3 + 3)) stop = false;
			
			Prompt.print(String.format("Invalid ORF detected : %s @ %-6s\tFrame : %s  Stop : %s  Gap : %d", LABEL, GENE,
					frame == 0 ? ANSIHandler.wrapper("O", 'g') : ANSIHandler.wrapper("X", 'r'),
					stop ? ANSIHandler.wrapper("O", 'g') : ANSIHandler.wrapper("X", 'r'),
					prtn.length() * 3 + 3 - orf.length()));
			
			if(!stop) {
				if(valid.length() < prtn.length() * 3) failed(2);
				valid = valid.substring(0, prtn.length() * 3);
				switch(new Random().nextInt(3)) {
				case 0: valid += "TAA"; break;
				case 1: valid += "TAG"; break;
				case 2: valid += "TGA"; break;
				}
			}
			
			String tsln = GenomeTranslator.transeq(valid);
			if(GenomeTranslator.equal(prtn + "*",  tsln)) Prompt.print(ANSIHandler.wrapper("ORF successfully fixed", 'g'));
			else failed(3);
		}
		else valid = orf;
		
		return valid;
	}
	
	static String removeX(String prtn, String orf, String strand) {
		String xrm = "";
		prtn += "*";
		if(strand.equals("-")) orf = FastaContigParser.revcomp(orf);
		
		for(int i = 0; i*3 < orf.length(); i++) {
			String codon = orf.substring(i*3, i*3 + 3);
			if(codon.contains("X")) xrm += GenomeTranslator.rcodon(prtn.substring(i, i+1));
			else xrm += codon;
		}
		
		return strand.equals("-") ? FastaContigParser.revcomp(xrm) : xrm;
	}
	
	static String fixORF(String prtn, String orf, String strand, String label, String gene) {
		LABEL = label;
		GENE = gene;
		
		String fix;
		if(strand.equals("-")) fix = checkORF(prtn, FastaContigParser.revcomp(orf));
		else fix = checkORF(prtn, orf);
		
		if(strand.equals("-")) return FastaContigParser.revcomp(fix);
		else return fix;
	}
	
	public static JSONObject fixGenome(JSONObject genomeObject) {
		JSONObject fixedObject = new JSONObject();
		
		for(Object key : genomeObject.keySet().toArray()) {
			if(((String) key).equals("label")) LABEL = (String) genomeObject.get(key);
			if(genomeObject.get(key) == null) {
				fixedObject.put(key, "none");
			}
			else fixedObject.put(key, genomeObject.get(key));
		}
		return fixedObject;
	}
	
	public static JSONObject fixRun(JSONObject runObject) {
		JSONObject fixedObject = new JSONObject();
		
		for(Object key : runObject.keySet().toArray()) {
			if(((String) key).equals("n_target_genes")) fixedObject.put(key, 53);
			else if(((String) key).equals("target_gene_set")) {
				String gset = "";
				for(String g : GenericConfig.FCG_REF) gset += g + ",";
				fixedObject.put(key, gset.substring(0, gset.length() - 1));
			}
			else fixedObject.put(key, runObject.get(key));
		}
		return fixedObject;
	}
	
	public static JSONObject fixData(JSONObject dataObject) {
		JSONObject fixedObject = new JSONObject();
		
		for(Object key : dataObject.keySet().toArray()) {
			GENE = (String) key;
			JSONArray arr = (JSONArray) dataObject.get(key);
			JSONArray fix = new JSONArray();
			
			for(Object ele : arr) {
				JSONObject eobj = (JSONObject) ele;
				String orf  = (String) eobj.get("dna");
				String prtn = (String) eobj.get("protein");
				Object eval = eobj.get("evalue");
				Object bits = eobj.get("bitscore");
				
				String ofix = checkORF(prtn, orf);
				JSONObject efix = new JSONObject();
				efix.put("dna",      ofix);
				efix.put("protein",  prtn);
				efix.put("evalue",   eval);
				efix.put("bitscore", bits);
				
				fix.add(efix);
			}
			
			fixedObject.put(key, fix);
		}
		
		return fixedObject;
	}
}
