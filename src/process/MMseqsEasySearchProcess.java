package process;

import java.io.File;

import entity.MMseqsSearchResultEntity;
import entity.ProfilePredictionEntity;
import envs.config.GenericConfig;
import envs.toolkit.FileStream;
import pipeline.ExceptionHandler;
import wrapper.MMseqsWrapper;

public class MMseqsEasySearchProcess {
	static String TASK = null;
	public static void setTask(String task) {TASK = task;}
	
	public static MMseqsSearchResultEntity search(String queryPath, String targetPath, String tmpPath, int uoff, int doff, boolean abs) {
		String resultPath = tmpPath + GenericConfig.SESSION_UID + "_" + targetPath.substring(targetPath.lastIndexOf(File.separator) + 1) + ".m8";
		MMseqsWrapper.runEasySearch(3, queryPath, targetPath, resultPath, tmpPath);
		MMseqsSearchResultEntity res = new MMseqsSearchResultEntity(TASK, targetPath, resultPath);
		
		try {
			FileStream stream = new FileStream(resultPath, 'r');
			stream.isTemp();
			String buf;
			while((buf = stream.readLine()) != null) res.add(buf, uoff, doff, abs);
		} catch(java.io.IOException e) {
			ExceptionHandler.handle(e);
		}
		
		return res;
	}
	public static MMseqsSearchResultEntity search(String queryPath, String targetPath, String tmpPath, double evalue) {
		String resultPath = tmpPath + GenericConfig.SESSION_UID + "_" + targetPath.substring(targetPath.lastIndexOf(File.separator) + 1) + ".m8";
		MMseqsWrapper.runEasySearch(3, evalue, queryPath, targetPath, resultPath, tmpPath);
		MMseqsSearchResultEntity res = new MMseqsSearchResultEntity(TASK, targetPath, resultPath);
		
		try {
			FileStream stream = new FileStream(resultPath, 'r');
			stream.isTemp();
			String buf;
			while((buf = stream.readLine()) != null) res.add(buf);
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
	
	public static void validate(ProfilePredictionEntity pp, String seqPath, String tmpPath) {
		String queryPath = pp.export();
		MMseqsSearchResultEntity res = search(queryPath, seqPath, tmpPath, GenericConfig.EvalueCutoff);
		res.assignLocs();
		res.purify();
		res.assignPurified(pp);
		res.remove();
		pp.remove();
	}
}