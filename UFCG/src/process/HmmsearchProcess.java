package process;

import envs.config.GenericConfig;
import envs.config.PathConfig;
import envs.toolkit.ANSIHandler;
import envs.toolkit.FileStream;
import envs.toolkit.GenomeTranslator;
import envs.toolkit.Prompt;

import java.util.Random;

import entity.ProfilePredictionEntity;
import wrapper.HmmsearchWrapper;

public class HmmsearchProcess {
	private static String checkORF(ProfilePredictionEntity pp, String prtn, String orf) {
		String valid = null;
		GenomeTranslator.createMap();
		
		if(!GenomeTranslator.equal(prtn + "*", GenomeTranslator.transeq(orf))) {
			int frame = GenomeTranslator.frame(orf, prtn);
			valid = orf.substring(frame);
			
			boolean stop = orf.endsWith("TAA") || orf.endsWith("TAG") || orf.endsWith("TGA");
			if(stop && (valid.length() != prtn.length() * 3 + 3)) stop = false;
			
			Prompt.talk(String.format("Invalid ORF detected : %s @ %-6s\tFrame : %s  Stop : %s  Gap : %d", GenericConfig.TAXON, pp.refBlock.cg,
					frame == 0 ? ANSIHandler.wrapper("O", 'g') : ANSIHandler.wrapper("X", 'r'),
					stop ? ANSIHandler.wrapper("O", 'g') : ANSIHandler.wrapper("X", 'r'),
					prtn.length() * 3 + 3 - orf.length()));
			
			if(frame < 0) {
				Prompt.talk(ANSIHandler.wrapper("ORF fix failed: ", 'r') + "could not find the valid reading frame.");
				return null;
			}
			if(!stop) {
				if(valid.length() < prtn.length() * 3) {
					Prompt.talk(ANSIHandler.wrapper("ORF fix failed: ", 'r') + "could not find the locus for a stop codon.");
				}
				valid = valid.substring(0, prtn.length() * 3);
				switch(new Random().nextInt(3)) {
				case 0: valid += "TAA"; break;
				case 1: valid += "TAG"; break;
				case 2: valid += "TGA"; break;
				}
			}
			
			String tsln = GenomeTranslator.transeq(valid);
			if(GenomeTranslator.equal(prtn + "*",  tsln)) Prompt.talk(ANSIHandler.wrapper("ORF successfully fixed", 'w'));
			else {
				Prompt.talk(ANSIHandler.wrapper("ORF fix failed: ", 'r') + "fixed ORF does not produce the valid product.");
				return null;
			}
		}
		else valid = orf;
		
		return valid;
	}
	
	public static void search(ProfilePredictionEntity pp, String hmmPath) throws java.io.IOException {
		String seqPath = pp.export();
		String tblPath = String.format("%s%s%s_%s.tbl",
				PathConfig.TempPath, GenericConfig.TEMP_HEADER, GenericConfig.ACCESS, pp.refBlock.cg);
		
		Prompt.talk("hmmsearch is evaluating predicted genes...");
		HmmsearchWrapper.runHmmsearchTable(tblPath, hmmPath, seqPath);
		pp.remove();
		
		FileStream tblStream = new FileStream(tblPath, 'r');
		tblStream.isTemp();
		tblStream.readLine();
		tblStream.readLine();
		tblStream.readLine();
		
		boolean optFlag = true;
		String line = null;
		while(!(line = tblStream.readLine()).startsWith("#")) {
			String tgt = line.split(" ")[0];
			int loc = Integer.parseInt(tgt.substring(tgt.lastIndexOf("_") + 2));
			
			/* ORF validation */
			String prtn = pp.getSeq(loc), orf = pp.getDna(loc), valid = null;
			if(!GenericConfig.INTRON) {
				if((valid = checkORF(pp, prtn, orf)) == null) continue;
			}
			else valid = orf;
			
			pp.addGene(prtn);
			pp.addGseq(valid);
			if(optFlag) {
				optFlag = false;
				pp.setOpt(loc);
			}
			
			double eval  = Double.parseDouble(line.split("\\s+")[4]);
			double score = Double.parseDouble(line.split("\\s+")[5]);
			pp.addEvalue(eval);
			pp.addScore(score);
		}
		
		tblStream.close();
		tblStream.wipe();
	}
}
