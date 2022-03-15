package process;

import envs.config.GenericConfig;
import envs.config.PathConfig;
import envs.toolkit.ANSIHandler;
import envs.toolkit.FileStream;
import envs.toolkit.Prompt;
import entity.BlockProfileEntity;
import entity.ProfilePredictionEntity;
import parser.GffTableParser;
import wrapper.AugustusWrapper;

import java.util.List;
import java.io.IOException;

public class GenePredictionProcess {
	// Predict gene from given contig and positions, return path to output gff file
	private static String predict(String ctgPath, String ctg, int spos, int epos, String famPath, String cg) {
		String gffPath = String.format("%s%s%s_%s_p%d_%s.gff",
				PathConfig.TempPath, GenericConfig.TEMP_HEADER, GenericConfig.ACCESS, ctg, spos, cg);
		int pst = spos - GenericConfig.AugustusPredictionOffset;
		if(pst < 0) pst = 0;
		int ped = epos + GenericConfig.AugustusPredictionOffset;
		
		try {
			// Extract sequence from offset-including sequence block
			Prompt.debug("Extracting sequence block...");
			
			String cseq = "";	
			
			FileStream ctgStream = new FileStream(ctgPath, 'r');
			String buf = ctgStream.readLine();
			int ist = 0, ied = 0;
			boolean flag = false;
			while((buf = ctgStream.readLine()) != null) {
				ied += buf.length();
				
				if(pst <= ist) flag = true;
				if(ied > ped) flag = false;
				if(flag) cseq += buf;
				
				ist += buf.length();
			}
			ctgStream.close();
			
			Prompt.debug("Expected sequence length =  " + String.valueOf(ped - pst + 1) + " bps");
			Prompt.debug("Imported sequence length =  " + String.valueOf(cseq.length()) + " bps");
			
			String blkPath = String.format("%s%sprobe_%s_%s_%s_p%d.block.fasta", PathConfig.OutputPath, GenericConfig.TEMP_HEADER, cg, GenericConfig.ACCESS, ctg, spos);
			FileStream blkStream = new FileStream(blkPath, 'w');
			blkStream.println(String.format(">%s_%s_%s_p%d", cg, GenericConfig.ACCESS, ctg, spos));
			blkStream.println(cseq);
			
			blkStream.close();
		} catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		
		
		AugustusWrapper.runAugustus(ctgPath, pst, ped, famPath, gffPath);
		
		return gffPath;
	}
	
	// convert block profile to protein prediction profiles
	public static ProfilePredictionEntity blockPredict(BlockProfileEntity bp, List<String> ctgPaths){
		ProfilePredictionEntity pp = new ProfilePredictionEntity(bp);
		
		for(int i = 0; i < bp.getCnt(); i++) {
			Prompt.talk(String.format("AUGUSTUS is predicting genes... (contig %s, position %d-%d)",
					bp.getCtg(i), bp.getSpos(i), bp.getEpos(i)));
			String gffPath = predict(ctgPaths.get(i), bp.getCtg(i), bp.getSpos(i), bp.getEpos(i), bp.getFam(), bp.cg);
			FileStream.isTemp(gffPath);
			
			try {
				Prompt.talk("Parsing gene prediction result written on : " + ANSIHandler.wrapper(gffPath, 'y'));
				GffTableParser.parse(pp, ctgPaths.get(i), gffPath);
			} catch(IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
		//	FileStream.wipe(ctgPaths.get(i));
			FileStream.wipe(gffPath);
		}
		
		return pp;
	}
}
