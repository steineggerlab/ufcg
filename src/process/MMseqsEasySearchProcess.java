package process;

import java.io.File;
import java.util.Random;

import entity.MMseqsSearchResultEntity;
import entity.ProfilePredictionEntity;
import envs.config.GenericConfig;
import envs.toolkit.ANSIHandler;
import envs.toolkit.FileStream;
import envs.toolkit.GenomeTranslator;
import envs.toolkit.Prompt;
import pipeline.ExceptionHandler;
import wrapper.MMseqsWrapper;

public class MMseqsEasySearchProcess {
	public static String checkORF(ProfilePredictionEntity pp, String prtn, String orf) {
		if(orf.equals("*")) return null;
		
		String valid;
		GenomeTranslator.createMap();
		
		if(!GenomeTranslator.equal(prtn + "*", GenomeTranslator.transeq(orf))) {
			int frame = GenomeTranslator.frame(orf, prtn);
			if(frame < 0) {
				Prompt.talk(ANSIHandler.wrapper("ORF fix failed: ", 'r') + "could not find the valid reading frame.");
				return null;
			}

			valid = orf.substring(frame);
			
			boolean stop = orf.endsWith("TAA") || orf.endsWith("TAG") || orf.endsWith("TGA");
			if(stop && (valid.length() != prtn.length() * 3 + 3)) stop = false;
			
			Prompt.talk(String.format("Invalid ORF detected : %s @ %-6s\tFrame : %s  Stop : %s  Gap : %d", GenericConfig.TAXON, pp.refBlock.cg,
					frame == 0 ? ANSIHandler.wrapper("O", 'g') : ANSIHandler.wrapper("X", 'r'),
					stop ? ANSIHandler.wrapper("O", 'g') : ANSIHandler.wrapper("X", 'r'),
					prtn.length() * 3 + 3 - orf.length()));


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
	
	static String TASK = null;
	public static void setTask(String task) {TASK = task;}
	
	public static MMseqsSearchResultEntity search(String queryPath, String targetPath, String tmpPath, int uoff, int doff, boolean abs, int threads) {
		String resultPath = tmpPath + GenericConfig.SESSION_UID + "_" + targetPath.substring(targetPath.lastIndexOf(File.separator) + 1) + ".m8";
		
		MMseqsWrapper mm = new MMseqsWrapper();
		mm.setEasySearch(queryPath, targetPath, resultPath, tmpPath);
		mm.setSearchType(0);
		mm.setThreads(threads);
		mm.exec();
		
		MMseqsSearchResultEntity res = new MMseqsSearchResultEntity(TASK, targetPath, resultPath);
		
		try {
			FileStream stream = new FileStream(resultPath, 'r');
			stream.isTemp();
			String buf;
			while((buf = stream.readLine()) != null) res.add(buf, uoff, doff, abs);
		} catch(java.io.FileNotFoundException e) {
			if(GenericConfig.VERB) Prompt.warn("Result file not created : " + ANSIHandler.wrapper(resultPath, 'B'));
			return res;
		} catch(java.io.IOException e) {
			ExceptionHandler.handle(e);
		} 
		
		return res;
	}
	public static MMseqsSearchResultEntity search(String queryPath, String targetPath, String tmpPath, double evalue, int threads, double cov) {
		String resultPath = tmpPath + GenericConfig.SESSION_UID + "_" + targetPath.substring(targetPath.lastIndexOf(File.separator) + 1) + ".m8";
		
		MMseqsWrapper mm = new MMseqsWrapper();
		mm.setEasySearch(queryPath, targetPath, resultPath, tmpPath);
		mm.setSearchType(0);
		mm.setEvalue(evalue);
		mm.setCoverage(cov);
		mm.setThreads(threads);
		mm.exec();
		
		MMseqsSearchResultEntity res = new MMseqsSearchResultEntity(TASK, targetPath, resultPath);
		
		try {
			FileStream stream = new FileStream(resultPath, 'r');
			stream.isTemp();
			String buf;
			while((buf = stream.readLine()) != null) res.add(buf);
		} catch(java.io.FileNotFoundException e) {
			// Prompt.test("Result file not created : " + ANSIHandler.wrapper(resultPath, 'B'));
			return res;
		} catch(java.io.IOException e) {
			ExceptionHandler.handle(e);
		}
		
		return res;
	}
	
	public static ProfilePredictionEntity parse(MMseqsSearchResultEntity res) {
		ProfilePredictionEntity pp = new ProfilePredictionEntity(res, ProfilePredictionEntity.TYPE_NUC);
		for(int i = 0; i < res.size(); i++) pp.addSeq(pp.getDna(i));
		return pp;
	}
	
	public static void validate(ProfilePredictionEntity pp, String seqPath, String tmpPath, int threads) {
		String queryPath = pp.export();
		
		// 3-step coverage search
		MMseqsSearchResultEntity res = search(queryPath, seqPath, tmpPath, GenericConfig.EvalueCutoff, threads, 0.8);
		if(res.size() == 0 && GenericConfig.SENS > 1) {
			res = search(queryPath, seqPath, tmpPath, GenericConfig.EvalueCutoff, threads, 0.5);
		}
		if(res.size() == 0 && GenericConfig.SENS > 2) {
			res = search(queryPath, seqPath, tmpPath, GenericConfig.EvalueCutoff, threads, 0.0);
		}
		
		res.assignLocs();
		res.purify();
		res.assignPurified(pp);
		res.remove();
		pp.remove();
	}
}