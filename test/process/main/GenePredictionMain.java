package process.main;

import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import entity.BlockProfileEntity;
import entity.ProfilePredictionEntity;
import envs.config.GenericConfig;
import envs.config.PathConfig;
import envs.toolkit.FileStream;
import envs.toolkit.Prompt;
import parser.GffTableParser;
import wrapper.AugustusWrapper;

public class GenePredictionMain {
	private static boolean _main = false;
	
	// Predict gene from given contig and positions, return path to output gff file
	private static String predict(String ctgPath, String ctg, int spos, int epos, String famPath) {
		String gffPath = _main ? String.format("%s/prediction.gff", PathConfig.TempPath)
				: String.format("%s/%s_%s_p%d.gff", PathConfig.TempPath, GenericConfig.ACCESS, ctg, spos);

		int pst = spos - GenericConfig.AugustusPredictionOffset;
		if(pst < 0) pst = 0;
		int ped = epos + GenericConfig.AugustusPredictionOffset;
		
		AugustusWrapper.runAugustus(ctgPath, pst, ped, famPath, gffPath);
		
		return gffPath;
	}
	
	// convert block profile to protein prediction profiles
	public static ProfilePredictionEntity blockPredict(BlockProfileEntity bp, List<String> ctgPaths){
		ProfilePredictionEntity pp = new ProfilePredictionEntity(bp);
		
		for(int i = 0; i < bp.getCnt(); i++) {
			String gffPath = predict(ctgPaths.get(i), bp.getCtg(i), bp.getSpos(i), bp.getEpos(i), bp.getFam());
			FileStream.isTemp(gffPath);
			
			try {
				GffTableParser.parse(pp, ctgPaths.get(i), gffPath);
			} catch(java.io.IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			FileStream.wipe(ctgPaths.get(i));
			FileStream.wipe(gffPath);
		}
		
		return pp;
	}
	
	/* 
	 * Runnable : ProcessGenePrediction.jar
	 * Arguments :
	 * 		-c <CTG>		(Required) : Input contig file path
	 * 		-f <FAM>		(Required) : Block profile file path
	 * 		-x -y <POS>		(Required) : Prediction start, end position
	 * 		-o <OUT>		(Required) : Output folder path
	 * 		
	 * Output : 
	 * 		prediction.gff : GFF formatted gene prediction result
	 */
	public static void main(String[] args) throws Exception {
		_main = true;
		GenericConfig.setHeader("Augustus");
		
		String CARG, FARG, OARG;
		int XARG, YARG;
		
		Options opts = new Options();
		opts.addOption("c", true, "Input contig file path");
		opts.addOption("f", true, "Block profile file path");
		opts.addOption("x", true, "Prediction start position");
		opts.addOption("y", true, "Prediction end position");
		opts.addOption("o", true, "Output folder path");
		opts.addOption("v", false, "Program verbosity");
		
		CommandLineParser clp = new DefaultParser();
		CommandLine cmd = clp.parse(opts, args);
		if(!cmd.hasOption("c") || !cmd.hasOption("f") || !cmd.hasOption("x") || !cmd.hasOption("y") || !cmd.hasOption("o")) {
			new HelpFormatter().printHelp("Fungible.process.GenePrediction", opts);
			return;
		}
		if(cmd.hasOption("v")) GenericConfig.VERB = true;
		
		CARG = cmd.getOptionValue("c");
		FARG = cmd.getOptionValue("f");
		OARG = cmd.getOptionValue("o");
		XARG = Integer.parseInt(cmd.getOptionValue("x"));
		YARG = Integer.parseInt(cmd.getOptionValue("y"));
		
		GenericConfig.setAugustusPredictionOffset(0);
		PathConfig.setTempPath(OARG);
		
		String gffPath = predict(CARG, null, XARG, YARG, FARG);
		Prompt.print("Prediction result written on : " + envs.toolkit.ANSIHandler.wrapper(gffPath, 'y'));
	}
}
