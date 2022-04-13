package entity;

import java.util.List;

import envs.toolkit.Prompt;

import java.util.ArrayList;
import java.util.Collections;

public class BlockProfileEntity {
	private class HitEntry implements Comparable<HitEntry>{
		private String ctg;
		private Integer spos, epos;
		private Double score;
		
		HitEntry(String ctg, int spos, int epos, double score){
			this.ctg = ctg;
			this.spos = spos;
			this.epos = epos;
			this.score = score;
		}
		
		String getCtg() {return this.ctg;}
		int getSpos() {return this.spos;}
		int getEpos() {return this.epos;}
		double getScore() {return this.score;}

		@Override
		public int compareTo(HitEntry hit) {
			if(this.score < hit.score) return -1;
			if(this.score > hit.score) return 1;
			return 0;
		}
	}
	
	public final String cg;
	protected String famPath;
	protected List<HitEntry> hits;
	
	public boolean isValid(){return hits.size() > 0;}
	public boolean isSingle(){return hits.size() == 1;}

	public BlockProfileEntity(String cg, String famPath) {
		this.cg = cg;
		this.famPath = famPath;
		this.hits = new ArrayList<HitEntry>();
	}
	
	public void update(String ctg, int spos, int epos, double score) {
		this.hits.add(new HitEntry(ctg, spos, epos, score));
	}
	
	public String getFam() {return famPath;}
	public int getCnt() {return hits.size();}
	public String getCtg(int idx) {return hits.get(idx).getCtg();}
	public int getSpos(int idx) {return hits.get(idx).getSpos();}
	public int getEpos(int idx) {return hits.get(idx).getEpos();}
	public double getScore(int idx) {return hits.get(idx).getScore();}
	
	// reduce hit entries to count by scores
	public void reduce(int count) {
		String debug = "";
		for(HitEntry entry : this.hits) debug += String.valueOf(entry.getScore()) + " ";
		Prompt.debug(debug);
		
		Collections.sort(this.hits, Collections.reverseOrder());
		
		int size = count < getCnt() ? count : getCnt();
		List<HitEntry> reduced = new ArrayList<HitEntry>();
		for(int i = 0; i < size; i++) reduced.add(this.hits.get(i));
		this.hits = reduced;
		
		debug = "";
		for(HitEntry entry : this.hits) debug += String.valueOf(entry.getScore()) + " ";
		Prompt.debug(debug);
	}
}
