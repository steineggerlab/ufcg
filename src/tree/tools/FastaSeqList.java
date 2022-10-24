package tree.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

public class FastaSeqList {
	public ArrayList<FastaSeq> list = new ArrayList<>();

	public int getSize() {
		return list.size();
	}

	public FastaSeqList() {
	}

	public int size() {
		return list.size();
	}

	public FastaSeq find(String title) {
		for (FastaSeq fastaSeq : list) {
			if (fastaSeq.title.equals(title)) return fastaSeq;
		}
		return null;
	}

	public FastaSeq findFastaSeq(String title) {
		for (FastaSeq fastaSeq : list) {
			if (fastaSeq.title.equals(title)) return fastaSeq;
		}
		return null;
	}

	public boolean write(String fileName) {
		return write(new File(fileName));
	}

	public boolean write(File f) {
		boolean res = true;
		try {
			FileWriter fw = new FileWriter(f);
			for (FastaSeq fastaSeq : list) {
				fw.write(">" + fastaSeq.title + "\n" + fastaSeq.sequence + "\n");
			}
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return res;
	}

	public String getString() {
		StringBuilder res = new StringBuilder();
		for (FastaSeq fastaSeq : list) {
			res.append(">").append(fastaSeq.title).append("\n").append(fastaSeq.sequence).append("\n");
		}
		return new String(res);
	}


	public void add(FastaSeq fa) {
		list.add(fa);
	}

	public boolean append(FastaSeqList fal) {
//		boolean res = true;
		if (list.size() != fal.list.size()) {
			System.out.println("different size! from FastaSeqList.append()");
			return false;
		}

		for (FastaSeq fastaSeq : list) {
			FastaSeq fa = fal.findFastaSeq(fastaSeq.title);
			if (fa == null) {
				System.out.println("not matching title:" + fastaSeq.title + "! from FastaSeqList.append()");
				return false;
			}
			StringBuilder sb = new StringBuilder(fastaSeq.sequence);
			sb.append(fa.sequence);
			fastaSeq.sequence = new String(sb);
			//System.out.println(list.get(i).sequence);
			fastaSeq.noAddedSeq++;
		}

		return true;
	}

	public void importFile(String fileName) {
		File f = new File(fileName);
		importFile(f);
	}

	public void addSeq(String title, String seqeunce) {
		FastaSeq fa = new FastaSeq();
		fa.setSeq(title, seqeunce, 1);
		list.add(fa);
	}

	public void importFile(File f) {
		try {
			FileReader fr = new FileReader(f);
			BufferedReader br = new BufferedReader(fr);
			import_br(br);
			br.close();
			fr.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void import_br(BufferedReader br) {
		String line;
		boolean isFirst = true;
		String tmpTitle = null;
		StringBuilder tmpSeq = new StringBuilder();
		boolean isFirstLineFound = false;
		try {
			while ((line = br.readLine()) != null) {
				if (isFirst) {
					if (!line.startsWith(">")) {
						tmpSeq.append(line);
						isFirstLineFound = true;
						isFirst = false;
						continue;
					}
				}

				if (line.startsWith(">") || line.startsWith("#")) {
					if (isFirst) {
						isFirst = false;
						tmpTitle = line.substring(1);
						isFirstLineFound = true;
						continue;
					}
					// save previous
					addSeq(tmpTitle, new String(tmpSeq).toUpperCase());
					tmpTitle = line.substring(1);
					tmpSeq = new StringBuilder();
				} else {
					if (!isFirstLineFound) continue;
					tmpSeq.append(line);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		addSeq(tmpTitle, new String(tmpSeq).toUpperCase());
	}

	public void importString(String input_str) {
		StringReader sr = new StringReader(input_str);
		BufferedReader br = new BufferedReader(sr);
		import_br(br);
		try {
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void print() {
		System.out.println("FastaSeqList:");
		System.out.println("index\tsize\taddedSeq\tacc\ttitle:");
		for (int i = 0; i < list.size(); i++) {

			System.out.println((i + 1) + "\t" + list.get(i).sequence.length() + "\t" + list.get(i).noAddedSeq + "\t" + list.get(i).acc + "\t" + list.get(i).title);
		}
	}
}
