package entity;

import java.util.List;

import envs.config.GenericConfig;
import envs.config.PathConfig;
import envs.toolkit.FileStream;
import envs.toolkit.Prompt;
//import process.HmmsearchProcess;
import process.MMseqsEasySearchProcess;

import java.util.ArrayList;
import java.util.Collections;

public class MMseqsSearchResultEntity {
	private class M8Entry implements Comparable<M8Entry> {
		String query, contig;
		Integer start, end;
		Double evalue;
		Integer score;
		Integer loc;
		
/*		M8Entry(String contig, Integer start, Integer end){
			this.contig = contig;
			this.start = start;
			this.end = end;
		}*/
		
		M8Entry(String buf) {
			String[] split = buf.split("\t");
			this.query = split[0];
			this.contig = split[1];
			this.start = Integer.parseInt(split[8]);
			this.end = Integer.parseInt(split[9]);
			this.evalue = Double.parseDouble(split[10]);
			this.score = Integer.parseInt(split[11]);
		}
		
		M8Entry(String buf, int uoff, int doff){
			String[] split = buf.split("\t");
			this.query = split[0];
			this.contig = split[1];
			this.start = Integer.parseInt(split[8]) - uoff;
			this.end = Integer.parseInt(split[9]) + doff;
			this.evalue = Double.parseDouble(split[10]);
			this.score = Integer.parseInt(split[11]);
		}
		
		M8Entry(String buf, int uoff, int doff, boolean abs){
			String[] split = buf.split("\t");
			this.query = split[0];
			this.contig = split[1];
			
			int mid = (Integer.parseInt(split[8]) + Integer.parseInt(split[9])) / 2;
			this.start = mid - uoff;
			this.end = mid + doff;
			
			this.evalue = Double.parseDouble(split[10]);
			this.score = Integer.parseInt(split[11]);
		}
		
		void setLoc(int loc) {this.loc = loc;}
		String getContig() {return this.contig;}
		int getStart() {return this.start;}
		int getEnd() {return this.end;}
		double getEvalue() {return this.evalue;}
		int getScore() {return this.score;}
		
		public int compareTo(M8Entry cmp) {
			if(evalue < cmp.evalue) return -1;
			if(evalue > cmp.evalue) return 1;
			return 0;
		}
	}
	
	public final String task, srcPath, resPath;
	protected List<M8Entry> entries = null;
	
	public MMseqsSearchResultEntity(String task, String srcPath, String resPath) {
		this.task = task;
		this.srcPath = srcPath;
		this.resPath = resPath;
		this.entries = new ArrayList<M8Entry>();
	}
	
	public void add(String buf) {
		this.entries.add(new M8Entry(buf));
	}
	public void add(String buf, int uoff, int doff) {
		this.entries.add(new M8Entry(buf, uoff, doff));
	}
	public void add(String buf, int uoff, int doff, boolean abs) {
		if(abs) this.entries.add(new M8Entry(buf, uoff, doff, abs));
		else this.entries.add(new M8Entry(buf, uoff, doff));
	}
	
	public String getContig(int index) {
		return this.entries.get(index).getContig();
	}
	public int getStart(int index) {
		return this.entries.get(index).getStart();
	}
	public int getEnd(int index) {
		return this.entries.get(index).getEnd();
	}
	public double getEvalue(int index) {
		return this.entries.get(index).getEvalue();
	}
	public int getScore(int index) {
		return this.entries.get(index).getScore();
	}
	public int size() {
		return this.entries.size();
	}
	
	private int overlapPct(int sx, int ex, int sy, int ey) {
		if(ex <= sy) return 0;
		if(sx >= ey) return 0;
		
		// calculate overlapping length and total length (shorter)
		int olen = (ex < ey ? ex : ey) - (sx > sy ? sx : sy);
		int tlen = (ex - sx) < (ey - sy) ? (ex - sx) : (ey - sy);
		return olen * 100 / tlen;
	}
	
	// assign locations before reduction
	public void assignLocs() {
		for(M8Entry entry : entries) {
			entry.setLoc(Integer.parseInt(entry.query.substring(entry.query.lastIndexOf("_") + 2)));
		}
	}
	
	// remove overlapping entries
	public void reduce() {
		List<M8Entry> reducedEntries = new ArrayList<M8Entry>();
		for(M8Entry entry : entries) {
			boolean reducible = false;
			for(M8Entry reducedEntry : reducedEntries) {
				if(entry.getContig().equals(reducedEntry.getContig())) {
					int pct = overlapPct(entry.getStart(), entry.getEnd(), reducedEntry.getStart(), reducedEntry.getEnd());
					if(pct > 0) {
						reducible = true;
						if(pct < 50) if(GenericConfig.VERB) Prompt.print_univ("WARN", "Vaguely overlapping entries detected at contig " + entry.getContig(), 'y');
						break;
					}
				}
			}
			if(reducible) continue;
			reducedEntries.add(entry);
		}
		this.entries = reducedEntries;
	}
	
	// collect top hits and sort by e-value
	public void purify() {
		if(size() == 0) return;
		
		List<M8Entry> collection = new ArrayList<M8Entry>();
		collection.add(entries.get(0));
		
		String qbuf = entries.get(0).query;
		for(M8Entry entry : entries) {
			if(entry.query.equals(qbuf)) continue;
			qbuf = entry.query;
			collection.add(entry);
		}
		
		Collections.sort(collection);
		this.entries = collection;
	}
	
	// assign purified results to profile entity
	public void assignPurified(ProfilePredictionEntity pp) {
		if(size() == 0) return;
		
		pp.setOpt(0);
		for(M8Entry entry : entries) {
			pp.addEvalue(entry.getEvalue());
			pp.addScore(entry.getScore());
			
			if(pp.getType() == ProfilePredictionEntity.TYPE_NUC) {
				pp.addGene("*");
				pp.addGseq(pp.getSeq(entry.loc));
			}
			else {
				int loc = entry.loc;
				String prtn = pp.getSeq(loc), orf = pp.getDna(loc), valid = null;
				if(!GenericConfig.INTRON) {
					if((valid = MMseqsEasySearchProcess.checkORF(pp, prtn, orf)) == null) continue;
				}
				else valid = orf;
				pp.addGene(prtn);
				pp.addGseq(valid);
			}
		}
	}
	
	public void remove() {
		FileStream.wipe(resPath);
	}
	public void remove(boolean force) {
		boolean tic = PathConfig.TempIsCustom;
		if(force) PathConfig.TempIsCustom = false;
		remove();
		PathConfig.TempIsCustom = tic;
	}
}

