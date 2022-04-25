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

public class GenePredictionProcess {
	// Predict gene from given contig and positions, return path to output gff file
	private static String predict(String ctgPath, String ctg, int loc, String famPath, String cg) {
		String gffPath = String.format("%s%s%s_%s_p%d_%s.gff",
				PathConfig.TempPath, GenericConfig.TEMP_HEADER, GenericConfig.ACCESS, ctg, loc, cg);
		int pst = loc - GenericConfig.AugustusPredictionOffset;
		if(pst < 0) pst = 0;
		int ped = loc + GenericConfig.AugustusPredictionOffset;
		
		AugustusWrapper.runAugustus(ctgPath, pst, ped, famPath, gffPath);
		
		return gffPath;
	}
	
	// convert block profile to protein prediction profiles
	public static ProfilePredictionEntity blockPredict(BlockProfileEntity bp, List<String> ctgPaths){
		ProfilePredictionEntity pp = new ProfilePredictionEntity(bp, ProfilePredictionEntity.TYPE_PRO);
		
		for(int i = 0; i < bp.getCnt(); i++) {
		//	Prompt.test("Prediction window detected : " + bp.getBlkPath());
		//	if(bp.getEpos(i) - bp.getSpos(i) > 20000) Prompt.test(bp.getPosString(i));
			Prompt.talk(String.format("AUGUSTUS is predicting genes... (contig %s, position %d-%d)",
					bp.getCtg(i), bp.getSpos(i), bp.getEpos(i)));
			String gffPath = predict(ctgPaths.get(i), bp.getCtg(i), bp.getMedian(i), bp.getFamPath(), bp.cg);
			FileStream.isTemp(gffPath);
			
			try {
				Prompt.talk("Parsing gene prediction result written on : " + ANSIHandler.wrapper(gffPath, 'y'));
				GffTableParser.parse(pp, ctgPaths.get(i), gffPath);
			} catch(java.io.IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
		//	FileStream.wipe(ctgPaths.get(i));
			FileStream.wipe(gffPath);
		}
		
		return pp;
	}
}
