package tree.tools;

import java.util.ArrayList;
import java.util.HashMap;

public class ReplaceAcc {

	ArrayList<String> accList = new ArrayList<>();
	ArrayList<String> labelList = new ArrayList<>();
	
	HashMap<String,Integer> hashAcc = new HashMap<>();
	HashMap<String,Integer> hashLabel = new HashMap<>();
	
	int count = 1;
	String flag = "zZ";
	
	public boolean add(String acc,String label) {
		if (hashAcc.get(acc)!=null) {
			System.out.println(acc+" already exists!");
			return false;
		}
		hashAcc.put(acc, accList.size());
		accList.add(acc);
		
		if (hashLabel.get(label)!=null) {
			label=label+"_"+count++;
		}
		if (hashLabel.get(label)!=null) {
			label=label+"_"+count++;
		}
		hashLabel.put(label, labelList.size());
		labelList.add(label);
		
		return true;
	}
	
	public void print() {
		for (int i=0; i<accList.size(); i++) {
			System.out.println((i+1)+"\t"+accList.get(i)+"\t"+labelList.get(i));
		}
	}
	
	public String replace(String text,boolean isNewick) {
		String res = text;
		
		for (int i=0; i<accList.size(); i++) {
			if (isNewick) {
				String label = labelList.get(i).replaceAll("'", "`");
				label = label.replaceAll(" ", "_");
				res = res.replaceFirst(flag+accList.get(i)+flag, label);			
			}
			else res = res.replaceFirst(flag+accList.get(i)+flag, labelList.get(i));
		}
		return res;
	}
}