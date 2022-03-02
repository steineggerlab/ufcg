/*
 * Runnable: iCore.jar
 * 
 * Usage:
 * 		-a, --msa [FA]	: Multiple sequence alignment of core gene candidate
 * 		-s, --seq [DIR]	: Directory containing sequences from search space
 * 		-o, --out [DIR]	: Directory for output files
 * 		-i, --itr [N]	: Counts of iteration
 * 		-c, --cpu [N]	: Number of CPU thread to use
 * 
 * Output:
 * 		task.fa		: Iteratively searched core gene sequences in FASTA format
 * 		task.msa	: Multiple sequence alignment of core genes
 * 		task.blk	: AUGUSTUS-PPX block profile
 * 		task.hmm	: HMMER profile HMM
 * 		task.tsv	: Single-lined TSV file about the growth of seed
 * 
 */
package pipeline;

import entity.*;
import process.*;
import envs.config.*;
import envs.toolkit.*;
import wrapper.*;

import org.apache.commons.cli.*;
import java.util.*;
import java.util.concurrent.*;
import java.io.IOException;

public class IterativeCoreSearchPipeline {
	private final String[] sysArgs;
	public String msaPath, seqDir, outDir;
	public List<String> seqPaths = new ArrayList<String>();
	public int nIter = 0, nThread = 0;
	private String msaID = "ID";
	private List<Integer> seedGrowth = new ArrayList<Integer>();
	
	public IterativeCoreSearchPipeline(String[] args) {
		this.sysArgs = args;
	}
	
	private void parseArgs() throws ParseException {
		Options opts = new Options();
		opts.addOption("a", "msa", true, "Multiple sequence alignment of core gene candidate");
		opts.addOption("s", "seq", true, "Directory containing sequences from search space");
		opts.addOption("o", "out", true, "Directory for output files");
		opts.addOption("i", "itr", true, "Counts of iteration");
		opts.addOption("c", "cpu", true, "Number of CPU thread to use");
		
		CommandLineParser clp = new DefaultParser();
		CommandLine       cmd = clp.parse(opts, sysArgs);
		
		if(cmd.hasOption("a") & cmd.hasOption("s") & cmd.hasOption("o") & cmd.hasOption("i") & cmd.hasOption("c")) {
			this.msaPath = cmd.getOptionValue("a");
			this.seqDir  = cmd.getOptionValue("s");
			this.outDir  = cmd.getOptionValue("o");
			
			if(!this.seqDir.endsWith("/")) this.seqDir += "/";
			if(!this.outDir.endsWith("/")) this.outDir += "/";
			
			this.nIter   = Integer.parseInt(cmd.getOptionValue("i"));
			this.nThread = Integer.parseInt(cmd.getOptionValue("c"));
		}
		else {
			new HelpFormatter().printHelp("iCore.jar", opts);
			System.exit(0);
		}
		
		this.msaID = this.msaPath.substring(this.msaPath.lastIndexOf("/") + 1, this.msaPath.lastIndexOf("."));
		GenericConfig.ACCESS = msaID;
	}
	
	private int identifySequences() throws IOException {
		String tmpListPath = "/tmp/" + GenericConfig.SESSION_UID + "_seqs.list";
		String[] cmd = {"/bin/bash", "-c", "ls -1 " + this.seqDir + " > " + tmpListPath};
		Shell.exec(cmd);
		
		FileStream tmpListStream = new FileStream(tmpListPath, 'r');
		String buf;
		while((buf = tmpListStream.readLine()) != null) this.seqPaths.add(buf);
		tmpListStream.close();
		Shell.exec("rm " + tmpListPath);
		
		return this.seqPaths.size();
	}
	
	private String tmpAlignPath = "/tmp/" + GenericConfig.SESSION_UID + ".pa",
				   tmpBlockPath = "/tmp/" + GenericConfig.SESSION_UID + ".blk",
				   tmpHmmPath   = "/tmp/" + GenericConfig.SESSION_UID + ".hmm";
	private void buildTempProfiles(String msaPath) {	
		PrepareAlignWrapper.runPrepareAlign(msaPath, tmpAlignPath);
		Msa2PrflWrapper.runMsa2Prfl(msaID, msaID, tmpAlignPath, tmpBlockPath);
		HmmbuildWrapper.runHmmbuild(tmpHmmPath, tmpAlignPath);
	}
	
	public void run() throws Exception {
		this.parseArgs(); // parse arguments
		this.identifySequences(); // create list of sequences
		Prompt.print("Iterative core search begins with following seed : " + ANSIHandler.wrapper(this.msaPath, 'y'));
		
		// Run iterative search
		String msaPath = this.msaPath;
		for(int iter = 1; iter <= this.nIter; iter++) {
			Prompt.print("Iteration session start : " + ANSIHandler.wrapper(String.format("%d/%d", iter, this.nIter), 'g'));
			this.buildTempProfiles(msaPath); // create initial profile
			Prompt.print("Temporary profiles built.");
			
			/* Multi-thread core search */
			Prompt.dynamicHeader("Searching profile on genome assemblies... " + ANSIHandler.wrapper("0.00%", 'g') + "done.");
			ExecutorService executorService = Executors.newFixedThreadPool(nThread);
			List<Future<ProfilePredictionEntity>> futures = new ArrayList<Future<ProfilePredictionEntity>>();
			
			for(int i = 0; i < seqPaths.size(); i++) {
				ExtractCoreGene ecg = new ExtractCoreGene(i);
				futures.add(executorService.submit(ecg));
				Thread.sleep(100);
			}
			executorService.shutdown();
					
			List<ProfilePredictionEntity> extProfiles = new ArrayList<ProfilePredictionEntity>();
			int nValid = 0;
			for(Future<ProfilePredictionEntity> future : futures) {
				ProfilePredictionEntity pp = future.get();
				extProfiles.add(pp);
				if(pp.getOptSeq() != null) nValid++;
			}
			
			System.out.println("");
			Prompt.print("Search finished. " + ANSIHandler.wrapper(String.valueOf(nValid), 'g') + " valid core sequences found.");
			seedGrowth.add(nValid);
			
			/* Create new MSA */
			String tmpFasPath = "/tmp/" + GenericConfig.SESSION_UID + ".fa",
				   tmpRawPath = "/tmp/" + GenericConfig.SESSION_UID + ".mafft.msa",
				   tmpMsaPath = "/tmp/" + GenericConfig.SESSION_UID + ".msa";
			
			Prompt.print("Creating new core gene alignment on : " + ANSIHandler.wrapper(tmpMsaPath, 'y'));
			FileStream fasStream = new FileStream(tmpFasPath, 'w');
			for(ProfilePredictionEntity profile : extProfiles) {
				if(profile.getOptSeq() != null) {
					fasStream.println(">" + msaID + "_" + profile.refBlock.cg);
					fasStream.println(profile.getOptSeq());
				}
			}
			fasStream.close();
			
			MafftWrapper.runMafft(tmpFasPath, tmpRawPath);
			MafftGapCleanProcess.clean(tmpRawPath, tmpMsaPath);
			msaPath = tmpMsaPath;
			
			// Halt if the coverage is extremely low (below 10%)
			if(nValid < seqPaths.size() / 10) {
				Prompt.print(ANSIHandler.wrapper("WARNING", 'y') + " : Gene coverage is extremely low. Halting iteration.");
			}
		}		
		Prompt.print("Iteration finished.");
		
		/* Write results */
		Prompt.print("Writing result files...");
		Shell.exec("cp /tmp/" + GenericConfig.SESSION_UID + ".fa" + " " + outDir + msaID + ".fa");
		
		this.buildTempProfiles(msaPath);	
		Shell.exec("cp " + tmpAlignPath + " " + outDir + msaID + ".msa");
		Shell.exec("cp " + tmpBlockPath + " " + outDir + msaID + ".blk");
		Shell.exec("cp " + tmpHmmPath   + " " + outDir + msaID + ".hmm");
		
		FileStream tsvStream = new FileStream(outDir + msaID + ".tsv", 'w');
		String tsvLine = msaID;
		for(int seedSize : seedGrowth) tsvLine += "\t" + String.valueOf(seedSize);
		tsvStream.println(tsvLine);
		tsvStream.close();
		
		/* Report */
		String growth = "";
		for(int seedSize : seedGrowth) growth += " > " + String.valueOf(seedSize);
		Prompt.print("-------------------------------------------------------------------");
		Prompt.print("                            R E P O R T                            ");
		Prompt.print("* SEED     : " + ANSIHandler.wrapper(this.msaPath, 'y'));
		Prompt.print("* COVERAGE : " + ANSIHandler.wrapper(String.format("%.2f%%",
				(double) seedGrowth.get(seedGrowth.size() - 1) * 100 / seqPaths.size()), 'y'));
		Prompt.print("* GROWTH   : " + growth.substring(3));
		Prompt.print("-------------------------------------------------------------------");
		
		FileStream.wipeOut();
	}
	
	private class ExtractCoreGene implements Callable<ProfilePredictionEntity> {
		private String seqPath, acc;
		private double prog;
		public ExtractCoreGene(int idx) {
			this.seqPath = seqDir + seqPaths.get(idx);
			this.acc = seqPaths.get(idx).substring(0, seqPaths.get(idx).lastIndexOf("."));
			this.prog = (double) (idx + 1) / seqPaths.size() * 100;
		}
		
		public ProfilePredictionEntity call() throws IOException {
			Prompt.dynamic("\r");
			Prompt.dynamicHeader("Searching profile on genome assemblies... " + 
					ANSIHandler.wrapper(String.format("%.2f%%", prog), 'g') + " done.");
			BlockProfileEntity bp = FastBlockSearchProcess.handle(seqPath, tmpBlockPath, acc);
			List<String> ctgPaths = ContigDragProcess.drag(seqPath, bp);
			ProfilePredictionEntity pp = GenePredictionProcess.blockPredict(bp, ctgPaths);
			if(pp.nseq() > 0) HmmsearchProcess.search(pp, tmpHmmPath);
			for(String ctgPath : ctgPaths) FileStream.wipe(ctgPath, true);
			return pp;
		}
	}
	
	public static void main(String[] args) throws Exception {
		GenericConfig.TSTAMP = true;
//		GenericConfig.DEV = true;
//		GenericConfig.VERB = true;
//		PathConfig.TempIsCustom = true;
		PathConfig.AugustusConfig = "/jc/fungi/UUCGf/config/ppx.cfg";
		
		GenericConfig.setHeader("ICORE");
		IterativeCoreSearchPipeline iCore = new IterativeCoreSearchPipeline(args);
		iCore.run();
	}
}
