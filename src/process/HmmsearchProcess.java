package process;

import envs.config.GenericConfig;
import envs.config.PathConfig;
//import envs.toolkit.ANSIHandler;
import envs.toolkit.FileStream;
//import envs.toolkit.GenomeTranslator;
import envs.toolkit.Prompt;

//import java.util.Random;

import entity.ProfilePredictionEntity;
import wrapper.HmmsearchWrapper;

@Deprecated
public class HmmsearchProcess {
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
				if((valid = MMseqsEasySearchProcess.checkORF(pp, prtn, orf)) == null) continue;
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
