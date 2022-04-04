package entity;

import java.util.List;
import java.util.ArrayList;

public class MMSeqsSearchResultEntity {
	private class M8Entry {
		String contig;
		Integer start, end;
		
/*		M8Entry(String contig, Integer start, Integer end){
			this.contig = contig;
			this.start = start;
			this.end = end;
		}*/
		
		M8Entry(String buf) {
			String[] split = buf.split("\t");
			this.contig = split[1];
			this.start = Integer.parseInt(split[8]);
			this.end = Integer.parseInt(split[9]);
		}
		
		M8Entry(String buf, int uoff, int doff){
			String[] split = buf.split("\t");
			this.contig = split[1];
			this.start = Integer.parseInt(split[8]) - uoff;
			this.end = Integer.parseInt(split[9]) + doff;
		}
		
		M8Entry(String buf, int uoff, int doff, boolean abs){
			String[] split = buf.split("\t");
			this.contig = split[1];
			
			int mid = (Integer.parseInt(split[8]) + Integer.parseInt(split[9])) / 2;
			this.start = mid - uoff;
			this.end = mid + doff;
		}
		
		String getContig() {return this.contig;}
		int getStart() {return this.start;}
		int getEnd() {return this.end;}
	}
	
	public final String task, path;
	protected List<M8Entry> entries = null;
	
	public MMSeqsSearchResultEntity(String task, String path) {
		this.task = task;
		this.path = path;
		this.entries = new ArrayList<M8Entry>();
	}
	
	public void add(String buf) {
		this.entries.add(new M8Entry(buf));
	}
	public void add(String buf, int uoff, int doff) {
		this.entries.add(new M8Entry(buf, uoff, doff));
	}
	public void add(String buf, int uoff, int doff, boolean abs) {
		this.entries.add(new M8Entry(buf, uoff, doff, abs));
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
	public int size() {
		return this.entries.size();
	}
}

