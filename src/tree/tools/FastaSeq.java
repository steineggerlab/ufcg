package tree.tools;

public class FastaSeq implements Comparable<FastaSeq> {
	public String title = "";
	public String acc = "";  // accession if applicable
	public String sequence = "";
	public int noAddedSeq = 0; // for added sequence number - special use
	public int length = -1;

	
	public FastaSeq()
	{
		
	}

	public int compareTo(FastaSeq o) {
	    if (o == null)
	        throw new ClassCastException("A FastaSeq object expected.");
		return Integer.compare(o.length, this.length);
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
		return ">" + title + "\n" + sequence;
	}
	public void print()
	{
		System.out.println(getFastaFormat());
	}


}