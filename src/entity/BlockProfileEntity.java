package entity;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class BlockProfileEntity {
	private static class HitEntry implements Comparable<HitEntry>{
		private final String ctg;
		private final Integer[] pos;
		private final Double score;
		
		HitEntry(String ctg, List<Integer> pos, double score){
			this.ctg = ctg;
			this.pos = Arrays.copyOf(pos.toArray(), pos.size(), Integer[].class);
			this.score = score;
		}
		
		String getCtg() {return this.ctg;}
		int getSpos() {return this.pos[0];}
		int getEpos() {return this.pos[pos.length - 1];}
		int getMedian() {return this.pos[pos.length / 2];}
		// double getScore() {return this.score;}

		/*
		String getPosString() {
			StringBuilder pstr = new StringBuilder();
			for(int p : pos) pstr.append(p).append(" ");
			return pstr.toString();
		}
		*/

		@Override
		public int compareTo(HitEntry hit) {
			return this.score.compareTo(hit.score);
		}
	}
	
	public final String cg;
	protected String famPath, blkPath;
	protected List<HitEntry> hits;
	
	public boolean isValid(){return hits.size() > 0;}
	// public boolean isSingle(){return hits.size() == 1;}

	public BlockProfileEntity(String cg, String famPath) {
		this.cg = cg;
		this.famPath = famPath;
		this.hits = new ArrayList<>();
	}
	
	public void setBlkPath(String blkPath) {this.blkPath = blkPath;}
	public void update(String ctg, List<Integer> pos, double score) {
		this.hits.add(new HitEntry(ctg, pos, score));
	}
	
	// public String getBlkPath() {return blkPath;}
	public String getFamPath() {return famPath;}
	public int getCnt() {return hits.size();}
	public int size() {return hits.size();}
	public String getCtg(int idx) {return hits.get(idx).getCtg();}
	public int getSpos(int idx) {return hits.get(idx).getSpos();}
	public int getEpos(int idx) {return hits.get(idx).getEpos();}
	public int getMedian(int idx) {return hits.get(idx).getMedian();}
	// public double getScore(int idx) {return hits.get(idx).getScore();}
	// public String getPosString(int idx) {return hits.get(idx).getPosString();}
	
	// reduce hit entries to count by scores
	public void reduce(int count) {
		this.hits.sort(Collections.reverseOrder());
		
		int size = Math.min(count, getCnt());
		List<HitEntry> reduced = new ArrayList<>();
		for(int i = 0; i < size; i++) reduced.add(this.hits.get(i));
		this.hits = reduced;
	}
}
