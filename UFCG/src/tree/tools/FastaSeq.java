package tree.tools;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class FastaSeq implements Comparable<FastaSeq> {
	public String title = "";
	public String acc = "";  // accession if applicable
	public int gi = -1;      // ncbi gi if applicable
	public String sequence = "";
	public String quality = "";
	public int noAddedSeq = 0; // for added sequence number - special use
	public int length = -1;
	public int cluster = -1;  // cluster or OTU
	public int index = -1; // index (from 1)
	public double coverage = -1;
	public int n_reads = -1;  //number of reads
	public String scaffold = null;  //scaffold string
	
	public FastaSeq()
	{
		
	}
	
	public FastaSeq(FastaSeq fa)
	{
		this.title = fa.title;
		this.acc = fa.acc;
		this.sequence = fa.sequence;
		this.noAddedSeq = fa.noAddedSeq;
	}

	public FastaSeq(String  title, String sequence)
	{
		this.title = title;
		this.acc = "";
		this.sequence = sequence;
		this.noAddedSeq = 0;
	}
	
	public int compareTo(FastaSeq o) {
	    if (!(o instanceof FastaSeq))
	        throw new ClassCastException("A FastaSeq object expected.");
	    if (this.length < o.length) return 1;
	    if (this.length > o.length) return -1;
		return 0;
	}
	
	public FastaSeq(String  title, String sequence, int noAddedSeq)
	{
		this.title = title;
		this.acc = "";
		
		this.sequence = sequence;
		this.noAddedSeq = noAddedSeq;
	}
	
	public void setSeq(String title,String sequence)
	{
		this.title = title;
		this.sequence = sequence;
		this.acc = "";
	}
	
	public void setSeq(String title,String sequence,int noAddedSeq)
	{
		this.title = title;
		this.sequence = sequence;
		this.noAddedSeq = noAddedSeq;
		this.acc = "";
	}
	public String getFastaFormat()
	{
		return new String(">"+title+"\n"+sequence);
	}
	public void print()
	{
		System.out.println(getFastaFormat());
	}
	public void writeAsBlastInfile(String fileName) throws IOException
	{
		FileWriter fw = new FileWriter(new File(fileName));
		fw.write(">"+title+"\n"+sequence);
		fw.close();
	}

	
	public void parseNcbi(String title){
		this.title = title;
		//>gi|145902672|gb|CP000002.3| Bacillus licheniformis ATCC 14580, complete genome
		if (title==null) return;
		String s[] = title.split("\\|");
		if (s.length>1) {
			if (s[0].equals("gi")) gi = Integer.valueOf(s[1]);
			if (s[0].equals(">gi")) gi = Integer.valueOf(s[1]);
		}
		if (s.length>3) {
			if (s[2].equalsIgnoreCase("gb")) acc = s[3];
			if (s[2].equalsIgnoreCase("emb")) acc = s[3];
			if (s[2].equalsIgnoreCase("dbj")) acc = s[3];
			if (s[2].equalsIgnoreCase("ref")) acc = s[3];
			if (s[2].equalsIgnoreCase("tpg")) acc = s[3];
		}
	}
	
}