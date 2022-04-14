package entity;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class BlockProfileEntity {
	private class HitEntry implements Comparable<HitEntry>{
		private String ctg;
		private Integer[] pos;
		private Double score;
		
		HitEntry(String ctg, List<Integer> pos, double score){
			this.ctg = ctg;
			this.pos = Arrays.copyOf(pos.toArray(), pos.size(), Integer[].class);
			this.score = score;
		}
		
		String getCtg() {return this.ctg;}
		int getSpos() {return this.pos[0];}
		int getEpos() {return this.pos[pos.length - 1];}
		int getMedian() {return this.pos[pos.length / 2];}
		double getScore() {return this.score;}
		
		String getPosString() {
			String pstr = "";
			for(int p : pos) pstr += String.valueOf(p) + " ";
			return pstr;
		}

		@Override
		public int compareTo(HitEntry hit) {
			if(this.score < hit.score) return -1;
			if(this.score > hit.score) return 1;
			return 0;
		}
	}
	
	public final String cg;
	protected String famPath, blkPath;
	protected List<HitEntry> hits;
	
	public boolean isValid(){return hits.size() > 0;}
	public boolean isSingle(){return hits.size() == 1;}

	public BlockProfileEntity(String cg, String famPath) {
		this.cg = cg;
		this.famPath = famPath;
		this.hits = new ArrayList<HitEntry>();
	}
	
	public void setBlkPath(String blkPath) {this.blkPath = blkPath;}
	public void update(String ctg, List<Integer> pos, double score) {
		this.hits.add(new HitEntry(ctg, pos, score));
	}
	
	public String getBlkPath() {return blkPath;}
	public String getFamPath() {return famPath;}
	public int getCnt() {return hits.size();}
	public int size() {return hits.size();}
	public String getCtg(int idx) {return hits.get(idx).getCtg();}
	public int getSpos(int idx) {return hits.get(idx).getSpos();}
	public int getEpos(int idx) {return hits.get(idx).getEpos();}
	public int getMedian(int idx) {return hits.get(idx).getMedian();}
	public double getScore(int idx) {return hits.get(idx).getScore();}
	public String getPosString(int idx) {return hits.get(idx).getPosString();}
	
	// reduce hit entries to count by scores
	public void reduce(int count) {
		Collections.sort(this.hits, Collections.reverseOrder());
		
		int size = count < getCnt() ? count : getCnt();
		List<HitEntry> reduced = new ArrayList<HitEntry>();
		for(int i = 0; i < size; i++) reduced.add(this.hits.get(i));
		this.hits = reduced;
	}
}
