package entity;

import envs.config.GenericConfig;
import envs.config.PathConfig;
import envs.toolkit.ANSIHandler;
import envs.toolkit.FileStream;
import envs.toolkit.Prompt;
import parser.FastaContigParser;
import pipeline.ExceptionHandler;

import java.util.List;
import java.util.ArrayList;

public class ProfilePredictionEntity {
	public final BlockProfileEntity refBlock;
	protected List<String> predSeqs;
	protected List<GffLocationEntity> gffLocs;
	protected int opt = -1;
	
	protected List<String> predGenes;
	protected List<String> predGseqs;
	protected List<Double> evalues, scores;
	
	public ProfilePredictionEntity(BlockProfileEntity bp) {
		this.refBlock = bp;
		this.predSeqs = new ArrayList<String>();
		this.gffLocs  = new ArrayList<GffLocationEntity>();
		
		this.predGenes = new ArrayList<String>(); // predicted protein sequences
		this.predGseqs = new ArrayList<String>(); // predicted DNA sequences
		this.evalues   = new ArrayList<Double>();
		this.scores    = new ArrayList<Double>();
	}
	
	/* generate list of empty profile entities */
	public List<ProfilePredictionEntity> generateProfiles() {
		List<ProfilePredictionEntity> profiles = new ArrayList<ProfilePredictionEntity>();
		
		for(String cg : GenericConfig.FCG) {
			profiles.add(new ProfilePredictionEntity(new BlockProfileEntity(cg, null)));
		}
		
		return profiles;
	}
	
	public void addSeq(String seq) {predSeqs.add(seq);}
	public void addLoc(GffLocationEntity loc) {gffLocs.add(loc);}
	public void addGene(String seq) {predGenes.add(seq);}
	public void addGseq(String seq) {predGseqs.add(seq);}
	public void addEvalue(double eval) {evalues.add(eval);}
	public void addScore(double score) {scores.add(score);}
	public void setOpt(int opt) {this.opt = opt;}
	
	private String expPath = null;
	public String export() { // Export predicted sequences in FASTA format
		expPath = String.format("%s%s%s_%s.fasta",
				PathConfig.TempPath, GenericConfig.TEMP_HEADER, GenericConfig.ACCESS, refBlock.cg);
		Prompt.talk("Exporting predicted genes to FASTA file : " + ANSIHandler.wrapper(expPath, 'y'));
		
		try {
			FileStream expStream = new FileStream(expPath, 'w');
			expStream.isTemp();
			for(int i = 0; i < predSeqs.size(); i++) {
				expStream.println(String.format(">%s_%s_g%d",
						GenericConfig.ACCESS, refBlock.cg, i));
				expStream.println(predSeqs.get(i));
			}
			expStream.close();
		} catch(java.io.IOException e) {ExceptionHandler.handle(e);}
		
		return expPath;
	}
	
	public void remove() {
		FileStream.wipe(expPath);
		expPath = null;
	}
	public void remove(boolean force) {
		boolean tic = PathConfig.TempIsCustom;
		if(force) PathConfig.TempIsCustom = false;
		remove();
		PathConfig.TempIsCustom = tic;
	}
	
	public int nseq() {return predSeqs.size();}
	public String getSeq(int idx) {return predSeqs.get(idx);}
	public String getDna(int idx) {
		try{
			return FastaContigParser.parse(gffLocs.get(idx));
		}
		catch(java.io.IOException e) {ExceptionHandler.handle(e);}
		return null;
	}
	
	public String getOptSeq() {
		if(opt < 0) return null;
		return predSeqs.get(opt);
	}
	public boolean valid() {return opt >= 0;}
	public boolean multiple() {return predGenes.size() > 1;}
}
