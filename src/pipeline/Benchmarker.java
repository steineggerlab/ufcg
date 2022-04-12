/*
 * UUCGf Benchmarker
 * 
 * Usage
 * 		java -jar BenchUUCGf.jar [ASSEMBLY] [OUTPUT] 	- Test given file and print results on output file
 */
package pipeline;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import entity.BlockProfileEntity;
import entity.ProfilePredictionEntity;

import envs.config.GenericConfig;
import envs.config.PathConfig;

import envs.toolkit.FileStream;
import envs.toolkit.Prompt;
import envs.toolkit.TimeKeeper;

import process.ContigDragProcess;
import process.FastBlockSearchProcess;
import process.GenePredictionProcess;
import process.MMseqsEasySearchProcess;

public class Benchmarker {
	public final String PATH;
	public Benchmarker(String seqPath) {
		this.PATH = seqPath;
	}
	
	// run process and return elapsed time
	int mark(String gene) throws java.io.IOException {
		Long itime = System.currentTimeMillis();
		
		BlockProfileEntity bp = FastBlockSearchProcess.handle(PATH, PathConfig.ModelPath + gene + ".hmm", gene);
		
		Long dragtime = System.currentTimeMillis();
		List<String> ctgPaths = ContigDragProcess.drag(PATH, bp);
		dragtime = System.currentTimeMillis() - dragtime;
		
		ProfilePredictionEntity pp = GenePredictionProcess.blockPredict(bp, ctgPaths);
		if(pp.nseq() > 0) MMseqsEasySearchProcess.validate(pp, PathConfig.SeqPath + gene + ".fa", PathConfig.TempPath);
		
		if(!pp.valid()) return -1;
		return Integer.valueOf(String.valueOf(System.currentTimeMillis() - itime - dragtime));
	}
	
	
	public static void main(String[] args) throws Exception {
		if(args.length != 2) {
			System.err.println("USAGE : java -jar BenchUUCGf.jar [ASSEMBLY] [OUTPUT]");
			System.exit(1);
		}
		
//		GenericConfig.DEV = true;
//		GenericConfig.VERB = true;
		GenericConfig.TSTAMP = true;
		GenericConfig.ACCESS = "bench";
		GenericConfig.setHeader("BENCH");
		
		Benchmarker bench = new Benchmarker(args[0]);
		Prompt.print("Benchmarking assembly : " + args[0]);
		
		Map<String, Integer> map = new HashMap<String, Integer>();
		long sum = 0;
		int cnt = 0;
		
		List<String> geneSet = new ArrayList<String>();
		geneSet.addAll(Arrays.asList(GenericConfig.FCG_REF));
		Collections.shuffle(geneSet);
		
		FileStream.init();
		for(String cg : geneSet) {		
			map.put(cg, bench.mark(cg));
			
			if(map.get(cg) < 0) {
				Prompt.print(String.format("GENE : %s\tNOT FOUND", cg));
				continue;
			}
			
			Prompt.print(String.format("GENE : %s\tTIME : %d ms", cg, map.get(cg))); 
			sum += map.get(cg);
			cnt++;
		}
		FileStream.wipeOut();
		
		Prompt.print("Elapsed wall-clock time : " + TimeKeeper.format(sum));
		
		Prompt.print("Recording normalized time records on : " + args[1]);
		FileStream stream = new FileStream(args[1], 'w');
		for(String cg : GenericConfig.FCG_REF) {
			if(map.get(cg) < 0) continue;
			stream.println(String.format("%s\t%d", cg, map.get(cg) * cnt * 100 / sum));
		}
		stream.close();
		
//		for(String cg : GenericConfig.FCG_REF) System.out.println(String.format("%s\t%d", cg, map.get(cg))); 
	}
}
