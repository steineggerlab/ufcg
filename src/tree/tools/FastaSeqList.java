package tree.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;

public class FastaSeqList {
	public ArrayList<FastaSeq> list = new ArrayList<FastaSeq>();
	public int getSize() { return list.size(); }
	public FastaSeqList() {}
	
	public int size()
	{
		return list.size();
	}
	public ArrayList<FastaSeq> getRandom(int size){
		if (list.size()<size) size=list.size();
		Collections.shuffle(list);
		ArrayList<FastaSeq> res = new ArrayList<FastaSeq>();
		for (int i=0; i<size; i++) {
			res.add(list.get(i));
		}
		return res;
	}
	
	public void shuffle()
	{
		Collections.shuffle(list);
	}
	public void checkBase()
	{
		int A = 0;
		int G = 0;
		int C = 0;
		int T = 0;
		int a = 0;
		int c = 0;
		int g = 0;
		int t = 0;
		int others = 0;
		StringBuffer others_str = new StringBuffer("");
		for (int i=0; i<list.size() ; i++)
		{
			FastaSeq fs = list.get(i);
			for (int n=0; n<fs.sequence.length(); n++)
			{
				switch (fs.sequence.charAt(n))
				{
				case 'A': A++; break;
				case 'C': C++; break;
				case 'G': G++; break;
				case 'T': T++; break;
				case 'a': a++; break;
				case 'c': c++; break;
				case 'g': g++; break;
				case 't': t++; break;
				default: others++; others_str.append(fs.sequence.charAt(n)); break;
				}
			}
			
		}
		System.out.println("Check base result:");
		System.out.println("A="+A+" C="+C+" G="+G+" T="+T+"  Total = "+(A+C+G+T));
		System.out.println("a="+a+" c="+c+" g="+g+" t="+t+"  Total = "+(a+c+g+t));
		System.out.println("others ="+others);
		System.out.println("others ="+others_str);
		
	}
	public FastaSeq find(String title)
	{
		for (int i=0; i<list.size(); i++)
		{
			if (list.get(i).title.equals(title)) return list.get(i);
		}
		return null;
	}
	
	public int find_index(String title)
	{
		for (int i=0; i<list.size(); i++)
		{
			if (list.get(i).title.equals(title)) return list.get(i).index;
		}
		return -1;
	}
	
	public int find_int(String title)
	{
		for (int i=0; i<list.size(); i++)
		{
			if (list.get(i).title.equals(title)) return i;
		}
		return -1;
	}
	
	public FastaSeq find_part(String str)
	{
		for (int i=0; i<list.size(); i++)
		{
			if (list.get(i).title.indexOf(str)>-1) return list.get(i);
		}
		return null;
	}
	
	public FastaSeq findFastaSeq(String title)
	{
		FastaSeq res = null;
		for (int i=0; i<list.size(); i++)
		{
			if (list.get(i).title.equals(title)) return list.get(i);
		}
		return res;
	}
	
	String extractSeq(String sequence, boolean[] taq)
	{
		StringBuffer res = new StringBuffer("");
		for (int i=0; i<sequence.length(); i++)
		{
			if (taq[i]) res.append(sequence.charAt(i));
		}
		return new String(res);
	}
	
	public FastaSeqList selectACGT()
	{
		FastaSeqList res = new FastaSeqList();
		boolean[] tag = new boolean[list.get(0).sequence.length()];
		for (int i=0; i<tag.length; i++) tag[i]=true;
		for (int i=0; i<list.size(); i++)
		{
			for (int n=0; n<list.get(i).sequence.length(); n++)
			{
				if (!tag[n]) continue;
				switch (list.get(i).sequence.charAt(n))
				{
				case 'A': continue;
				case 'C': continue;
				case 'G': continue;
				case 'T': continue;
				default: tag[n]=false; continue;
				}
			}
		}
		for (int i=0; i<list.size(); i++)
		{
			FastaSeq fa = new FastaSeq(list.get(i).title,extractSeq(list.get(i).sequence,tag),list.get(i).noAddedSeq);
			res.list.add(fa);
		}
		return res;
	}
	
	public int getMaxSeqLength()
	{
		int max = 0;
		for (int i=0; i<list.size(); i++) {
			if (list.get(i).sequence.length() > max) max=list.get(i).sequence.length();
		}
		return max;
		
	}
	public FastaSeqList selectTighten()
	{
		FastaSeqList res = new FastaSeqList();
		int max_length = getMaxSeqLength();
		int list_size = list.size();
		int[] gap_count = new int[max_length];
		for (int i=0; i<gap_count.length; i++) gap_count[i]=0;

		for (int i=0; i<list.size(); i++)
		{
			for (int n=0; n<list.get(i).sequence.length(); n++)
			{
				if (list.get(i).sequence.charAt(n)=='-')  gap_count[n]++;
				if (list.get(i).sequence.charAt(n)=='.')  gap_count[n]++;
			}
		}
		for (int i=0; i<list.size(); i++)
		{
			StringBuffer sb = new StringBuffer();
			for (int n=0; n<list.get(i).sequence.length(); n++)
			{
				if (gap_count[n]==list_size) continue;
				sb.append(list.get(i).sequence.charAt(n));
			}
			FastaSeq fa = new FastaSeq(list.get(i).title,new String(sb),list.get(i).noAddedSeq);
			res.list.add(fa);
		}
		return res;
	}
	
	public FastaSeqList selectVariable()
	{
		FastaSeqList res = new FastaSeqList();
		boolean[] tag = new boolean[list.get(0).sequence.length()];
		for (int i=0; i<tag.length; i++) tag[i]=false;
		for (int n=0; n<tag.length; n++)
		{
			if (tag[n]) continue;
			char firstBase = list.get(0).sequence.charAt(n);
			for (int i=1; i<list.size(); i++)
			{
				char base = list.get(i).sequence.charAt(n);
				if (firstBase!=base) { 
					tag[n]=true; break; 
					}
			}
		}
		//System.out.print(tag[0]);
		for (int i=0; i<list.size(); i++)
		{
			FastaSeq fa = new FastaSeq(list.get(i).title,extractSeq(list.get(i).sequence,tag),list.get(i).noAddedSeq);
			res.list.add(fa);
		}
		return res;
	}
	
	public FastaSeqList selectMajority(double cutoff)  // e.g. 50%
	{
		FastaSeqList res = new FastaSeqList();
		int max_length = getMaxSeqLength();
		int list_size = list.size();
		int[] gap_count = new int[max_length];
		for (int i=0; i<gap_count.length; i++) gap_count[i]=0;

		for (int i=0; i<list.size(); i++)
		{
			for (int n=0; n<list.get(i).sequence.length(); n++)
			{
				if (list.get(i).sequence.charAt(n)=='-')  gap_count[n]++;
				if (list.get(i).sequence.charAt(n)=='.')  gap_count[n]++;
			}
		}
		for (int i=0; i<list.size(); i++)
		{
			StringBuffer sb = new StringBuffer();
			for (int n=0; n<list.get(i).sequence.length(); n++)
			{
				double per_base = (list_size-gap_count[n])*100/list_size;
				if (per_base<cutoff) continue;
				sb.append(list.get(i).sequence.charAt(n));
			}
			FastaSeq fa = new FastaSeq(list.get(i).title,new String(sb),list.get(i).noAddedSeq);
			res.list.add(fa);
		}
		return res;
	}
	
	public boolean write(String fileName)
	{
		boolean res = true;
		res = write(new File(fileName));
		return res;
	}
	
    public boolean write(File f) {
        boolean res = true;
        try {
            FileWriter fw = new FileWriter(f);
            for (int i = 0; i < list.size(); i++) {
                fw.write(">" + list.get(i).title + "\n" + list.get(i).sequence + "\n");
            }
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return res;
    }
	
	public boolean write_FQ(String fileName)
	{
		boolean res = true;
		res = write_FQ(new File(fileName));
		return res;
	}
	
	   public boolean write_FQ(File f)
	    {
	        boolean res = true;
	        try {
	            FileWriter fw = new FileWriter(f);
	            for (int i=0; i<list.size(); i++)
	            {
	                fw.write("@"+list.get(i).title+"\n"+list.get(i).sequence+"\n");
	                fw.write("+" + "\n");
	                fw.write(list.get(i).quality + "\n");
	            }
	            fw.close();
	        } catch (IOException e) {
	            e.printStackTrace();
	            return false;
	        }
	        return res;
	    }
	
	 public boolean genomeFastaWrite(String fileName)
	  {
	    boolean res = true;
	    try {
	      FileWriter fw = new FileWriter(new File(fileName));
	      for (int i=0; i<list.size(); i++)
	      {
	        fw.write(">zZ"+list.get(i).title+"zZ\n"+list.get(i).sequence+"\n");
	      }
	      fw.close();
	    } catch (IOException e) {
	      e.printStackTrace();
	      return false;
	    }
	    return res;
	  }
	  
	
	public boolean writeFile(String full_fn)
	{
		boolean res = true;
		try {
			FileWriter fw = new FileWriter(full_fn);
			for (int i=0; i<list.size(); i++)
			{
				fw.write(">"+list.get(i).title+"\n"+list.get(i).sequence+"\n");
			}
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return res;
	}
	
	   public boolean writeFile(File f)
	    {
	        boolean res = true;
	        try {
	            FileWriter fw = new FileWriter(f);
	            for (int i=0; i<list.size(); i++)
	            {
	                fw.write(">"+list.get(i).title+"\n"+list.get(i).sequence+"\n");
	            }
	            fw.close();
	        } catch (IOException e) {
	            e.printStackTrace();
	            return false;
	        }
	        return res;
	    }
	
	public boolean writeFileWithIndex(File f)  // 
	{
		boolean res = true;
		try {
			FileWriter fw = new FileWriter(f);
			for (int i=0; i<list.size(); i++)
			{
				fw.write(">"+(i+1)+"\n"+list.get(i).sequence+"\n");
			}
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return res;
	}
	
	public String getString()
	{
		StringBuffer res = new StringBuffer("");
		for (int i=0; i<list.size(); i++)
		{
			res.append(">"+list.get(i).title+"\n"+list.get(i).sequence+"\n");
		}
		return new String(res);
	}

	
	public void add(FastaSeq fa)
	{
		list.add(fa);
	}

	

	
	public int findFastaSeqIndex(String title)
	{
		int res = -1;
		for (int i=0; i<list.size(); i++)
		{
			if (list.get(i).title.equals(title)) return i;
		}
		return res;
	}

	public boolean append(FastaSeqList fal)
	{
//		boolean res = true;
		if (list.size() != fal.list.size()) {
			System.out.println("different size! from FastaSeqList.append()");
			return false;
		}
		
		for (int i=0; i<list.size(); i++)
		{
			FastaSeq fa  = fal.findFastaSeq(list.get(i).title);
			if (fa==null) 
			{
				System.out.println("not matching title:"+list.get(i).title+"! from FastaSeqList.append()");
				return false;
			}
			StringBuffer sb = new StringBuffer(list.get(i).sequence);
			sb.append(fa.sequence);
			list.get(i).sequence = new String(sb);
			//System.out.println(list.get(i).sequence);
			list.get(i).noAddedSeq ++;
		}
		
		return true;
	}

	public boolean append_no_check(FastaSeqList fal)
	{
//		boolean res = true;
		if (list.size() != fal.list.size()) {
			System.out.println("different size! from FastaSeqList.append()");
			return false;
		}
		
		for (int i=0; i<fal.list.size(); i++)
		{
			StringBuffer sb = new StringBuffer(list.get(i).sequence);
			sb.append(fal.list.get(i).sequence);
			list.get(i).sequence = new String(sb);
			//System.out.println(list.get(i).sequence);
			list.get(i).noAddedSeq ++;
		}
		
		return true;
	}

    public int importFileQual(String fileName) {
        File f = new File(fileName);
        return importFileQual(f);
    }
	
	public int importFile(String fileName)
	{
		File f = new File(fileName);
		return importFile(f);
	}
	
	public int importFile_clcbio_contig(String fileName)  //>ConsensusfromContig195 Average coverage: 1,317.92
	{
		File f = new File(fileName);
		int res = importFile(f);
		for (int i=0; i<list.size(); i++) {
			if (list.get(i).title!=null) {
				String s[]=list.get(i).title.split("Average coverage: ");
				if (s.length==2) list.get(i).coverage=Double.valueOf(s[1].replaceAll(",", ""));
			}
		}
		return res;
	}
	

	
	public int importFile_FQ(String fileName)
	{
		File f = new File(fileName);
		return importFile_FQ(f);
	}

	public void addSeq(String title,String seqeunce)
	{
		FastaSeq fa = new FastaSeq();
		fa.setSeq(title, seqeunce,1);
		list.add(fa);
	}
	
    public int importFileQual(File f)
    {
        try {
            FileReader fr = new FileReader(f);
            BufferedReader br = new BufferedReader(fr);
            import_br_qual(br);
            br.close();
            fr.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list.size();
    }
	
	public int importFile(File f)
	{
		try {
			FileReader fr = new FileReader(f);
			BufferedReader br = new BufferedReader(fr);
			import_br(br);
			br.close();
			fr.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return list.size();
	}
	
	public int importFile_FQ(File f)
	{
		try {
			FileReader fr = new FileReader(f);
			BufferedReader br = new BufferedReader(fr);
			import_br_FQ(br);
			br.close();
			fr.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return list.size();
	}
	
	public void set_index() // set index value
	{
		for (int i=0; i<list.size(); i++) {
			list.get(i).index=i+1;
		}
	}
	
	public int import_br(BufferedReader br)
	{
		String line=null;
		boolean isFirst = true;
		String tmpTitle = null;
		StringBuffer tmpSeq = new StringBuffer("");
		boolean isFirstLineFound = false;
		try {
			while ((line = br.readLine()) != null) {
				if (isFirst) {
					if (!line.startsWith(">")) {
						tmpSeq.append(line);
						isFirstLineFound=true;
						isFirst = false;
						continue;
					}
				}
				
				if (line.startsWith(">") || line.startsWith("#")) {
					if (isFirst) {
						isFirst = false;
						tmpTitle = line.substring(1);
						isFirstLineFound=true;
						continue;
					} 
					// save previous
					addSeq(tmpTitle,new String(tmpSeq).toUpperCase());
					tmpTitle = line.substring(1);
					tmpSeq=new StringBuffer("");
					}
				else {
					if (!isFirstLineFound) continue;
					tmpSeq=tmpSeq.append(line);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		addSeq(tmpTitle,new String(tmpSeq).toUpperCase());
		return list.size();
	}

	   public int import_br_qual(BufferedReader br)
	    {
	        String line=null;
	        boolean isFirst = true;
	        String tmpTitle = null;
	        StringBuffer tmpSeq = new StringBuffer("");
	        boolean isFirstLineFound = false;
	        try {
	            while ((line = br.readLine()) != null) {
	                if (isFirst) {
	                    if (!line.startsWith(">")) {
	                        tmpSeq.append(" " + line);
	                        isFirstLineFound=true;
	                        isFirst = false;
	                        continue;
	                    }
	                }
	                
	                if (line.startsWith(">") || line.startsWith("#")) {
	                    if (isFirst) {
	                        isFirst = false;
	                        tmpTitle = line.substring(1);
	                        isFirstLineFound=true;
	                        continue;
	                    } 
	                    // save previous
	                    addSeq(tmpTitle,new String(tmpSeq).toUpperCase());
	                    tmpTitle = line.substring(1);
	                    tmpSeq=new StringBuffer("");
	                    }
	                else {
	                    if (!isFirstLineFound) continue;
	                    tmpSeq=tmpSeq.append(" " + line);
	                }
	            }
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	        addSeq(tmpTitle,new String(tmpSeq).trim().toUpperCase());
	        return list.size();
	    }
	
	public int import_br_FQ(BufferedReader br)
	{
		String line=null;
		try {
			while ((line = br.readLine()) != null) {
	      String title = null;
	      StringBuilder sbSeq = new StringBuilder();
	      StringBuilder sbQual = new StringBuilder();
	      
	      if(line.startsWith("@")) {
	        title = line.substring(1);
	        while(!(line=br.readLine()).startsWith("+")){
	          sbSeq.append(line);
	        }
	        
	        while(sbQual.length()!=sbSeq.length()){
	          line=br.readLine();          
	          sbQual.append(line);
	          if(sbQual.length()>sbSeq.length()){
	            System.out.println("Quality length is different with sequence length! Abort!!"); 
	            System.out.println(title);
	            System.exit(0);            
	          }
	        }
	        
	        FastaSeq fa = new FastaSeq();
	        fa.title = title;
	        fa.sequence = sbSeq.toString();
	        fa.quality = sbQual.toString();
	        
	        list.add(fa);
	      } 
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return list.size();
	}

	public int importString(String input_str)
	{
	   	StringReader sr = new StringReader(input_str);
    	BufferedReader br = new BufferedReader(sr);
    	import_br(br);
    	try {
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	return list.size();
	}
	
	public void print()
	{
		System.out.println("FastaSeqList:");
		System.out.println("index\tsize\taddedSeq\tacc\ttitle:");
		for (int i=0; i<list.size(); i++)
		{
			
			System.out.println((i+1)+"\t"+list.get(i).sequence.length()+"\t"+list.get(i).noAddedSeq+"\t"+list.get(i).acc+"\t"+list.get(i).title);
		}
	}
	
	public int countVariableSites()
	{
		int res = 0;
		for (int i=1; i<list.size(); i++)
		{
			FastaSeq fa  = list.get(i);
			for (int n=0; n<list.get(0).sequence.length(); n++)
			{
				if (list.get(0).sequence.charAt(n)!=fa.sequence.charAt(n)) res ++;
			}
		}
		return res;
	}
	
	public void writePhylip(String fname)
	{
		try {
		FileWriter fw = new FileWriter(fname);
		fw.write(list.size()+" "+list.get(0).sequence.length()+"\n");
		for (int i=0; i<list.size(); i++)
		{
			
			fw.write(String.format("%10s ",list.get(i).title));
			fw.write(list.get(i).sequence+"\n");
		}
		fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String getStringNexusDNA()
	{
		StringBuffer res = new StringBuffer("#NEXUS\nbegin data;\ndimensions ntax="+list.size()+" nchar="+list.get(0).sequence.length()+";\n");
		res.append("format datatype=dna interleave=no gap=-;\n");
		res.append("matrix\n");
		for (int i=0; i<list.size(); i++)
		{
			res.append(list.get(i).title.replaceAll("\\s", "_")+"\t"+list.get(i).sequence+"\n");
		}
		res.append(";\nend;\n");
		return new String(res);
	}

	public void writeNexusDNA(String fname)
	{
		try {
		FileWriter fw = new FileWriter(fname);
		fw.write(getStringNexusDNA());
		fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

/*	public boolean generateBLASTDB(String dir, String file_name)
	{
		String full_fn = dir+"/"+file_name;
		if (!writeFile(full_fn)) return false;
		FormatBLASTdb fd = new FormatBLASTdb();
		fd.doDNA(file_name, dir);
		return true;
	}
	
	public boolean generateBLASTDB_concat_seq(String dir, String file_name)  // concat all seq
	{
		String full_fn = dir+"/"+file_name;
		try {
			FileWriter fw = new FileWriter(full_fn);
			
			for (int i=0; i<list.size(); i++)
			{
				if (i==0) fw.write(">"+list.get(i).title+" all seq were concatermerized\n");
				fw.write(list.get(i).sequence+"\n");
			}
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		FormatBLASTdb fd = new FormatBLASTdb();
		fd.doDNA(file_name, dir);
		return true;
	}
*/	
	private static String trimAccVersion(String acc) // NC_1017.1 -> NC_1017
	{
		String[] s = acc.split("\\.");
		return s[0];
	}
	
	public void convert_title2ref_without_version()  //>gi|56707187|ref|NC_006570.1| Francisella tularensis subsp. tularensis Schu 4, complete genome  -> NC_006570
	{
		for (int i=0; i<list.size(); i++)
		{
			String s[] = list.get(i).title.split("\\|ref\\|");
			if (s.length==2) {
				s = s[1].split("\\|");
				s[0]= trimAccVersion(s[0]);
			}
			list.get(i).title=s[0];
		}
	}
	public void convert_title2gb_without_version()  //>gi|56707187|ref|NC_006570.1| Francisella tularensis subsp. tularensis Schu 4, complete genome  -> NC_006570
	{
		for (int i=0; i<list.size(); i++)
		{
			String s[] = list.get(i).title.split("\\|gb\\|");
			if (s.length==2) {
				s = s[1].split("\\|");
				s[0]= trimAccVersion(s[0]);
			}
			list.get(i).title=s[0];
		}
	}
	
	public int getSum() // sum of bases
	{
		int res = 0;
		for (int i=0; i<list.size(); i++)
		{
			res = res + list.get(i).sequence.trim().length();
		}
		return res;
	}
	
	public double cal_coverage_average(){
		double res = 0;
		double total = 0;
		double total_base = 0;
		for (int i=0; i<list.size(); i++) {
			if (list.get(i).coverage==-1) return -1;
			int len=list.get(i).sequence.trim().length();
			total = total + (list.get(i).coverage*len);
			total_base = total_base + len;
		}
		res = total / total_base;
		return res;
	}
	

	public int importFile_454_contig(String fileName)  //>contig00001  length=294   numreads=148
	{
		File f = new File(fileName);
		int res = importFile(f);
		for (int i=0; i<list.size(); i++) {
			if (list.get(i).title!=null) {
				String s[]=list.get(i).title.split("\\s+");
				if (s.length==3) {
					list.get(i).title=s[0];
					list.get(i).length=Integer.valueOf(s[1].replaceAll("length=", ""));
					list.get(i).n_reads=Integer.valueOf(s[2].replaceAll("numreads=", ""));
					//list.get(i).coverage=Double.valueOf(s[1].replaceAll(",", ""));
				}
			}
		}
		return res;
	}
	
	public int importFile_454_contig(String fileName,String alignment_info_filename) throws IOException
	{
		int res = importFile_454_contig(fileName);
		File f = new File(alignment_info_filename);
		FileReader fr = new FileReader(f);
		BufferedReader br = new BufferedReader(fr);
		String line=null;
		boolean is_first = true;
		String contig=null;
		double total=0;
		double n_reads=0;
		while ((line = br.readLine()) != null) {
			if (line.startsWith("Position")) continue;
			if (line.startsWith(">")) {
				String s[]=line.substring(1).split("\\s+");
				if (is_first) { 
					contig=s[0]; 
					is_first=false;
					total=0;
					n_reads=0;
					continue; 
					}
					else {
						double coverage = total/n_reads;
						//System.out.println(contig+"\t"+coverage);
						
						int index=find_int(contig);
						if (index==-1) {
							System.out.println("Warning: "+contig+" not in contig file");
						}
						else {
							list.get(index).coverage=coverage;
						}
						
						
						contig=s[0];
						total=0;
						n_reads=0;
					}
			}
			else {
				String s[]=line.split("\\s+");
				n_reads++;
				total=total+Double.valueOf(s[4]);
			}
		}
		br.close();
		return res;
	}
	
	public void importFile_454_scaffold(String fileName) throws IOException // run importFile_454_contig first
	{
		File f = new File(fileName);
		FileReader fr = new FileReader(f);
		BufferedReader br = new BufferedReader(fr);
		String line=null;
		while ((line = br.readLine()) != null) {
			if (line.startsWith("scaffold")) {
				
				String s[]=line.split("\\s+");
				if (s[5].startsWith("contig")) {
					int index=find_int(s[5]);
					
					if (index==-1) {
						System.out.println("Warning: "+s[5]+" not in contig file");
					}
					list.get(index).scaffold=s[0];
				}
			}
		}
		br.close();
	}
	
	public void set_RAST_input_title(String chunlab_genome_uid)  // change title for RAST input (0001|cn|xxx|ctg|11|)
	{
		for (int i=0; i<list.size(); i++) {
			FastaSeq fa = list.get(i);
			String s[] = fa.title.split("\\|");
			String title = "";
			if (s.length>0) if (s[0].startsWith("contig") && s[1].equals("cv"))  // >  |cv|
			{
				String nummber = String.format("%04d",(i+1));
				title=nummber+"|cn|"+chunlab_genome_uid+"|ctg|"+(i+1);
				for (int n=1; n<s.length; n++) title = title + "|" +s[n];
				fa.title=title;
				list.set(i, fa);
				//System.out.println(title);
			}
		}
	}
	
}
