package entity;

import java.util.ArrayList;
import java.util.List;

public class GffLocationEntity {
	public ProfilePredictionEntity refProfile;
	public String ctgPath;
	public int trxHead, trxTail;
	public List<Integer> intronHeads, intronTails;
	public boolean fwd;
	
	private String getType(String str) {return str.split("\t")[2];}
	private int    getHead(String str) {return Integer.parseInt(str.split("\t")[3]);}
	private int    getTail(String str) {return Integer.parseInt(str.split("\t")[4]);}
	private String getSign(String str) {return str.split("\t")[6];}
	
	public GffLocationEntity(ProfilePredictionEntity ref, String ctgPath, String trxStr) {
		this.refProfile = ref;
		this.ctgPath = ctgPath;
		this.trxHead = getHead(trxStr);
		this.trxTail = getTail(trxStr);
		this.fwd = getSign(trxStr).equals("+");
		
		this.intronHeads = new ArrayList<>();
		this.intronTails = new ArrayList<>();
	}
	
	public void feed(String str) {
		if(getType(str).equals("intron")) {
			this.intronHeads.add(getHead(str));
			this.intronTails.add(getTail(str));
		}
	}
}
