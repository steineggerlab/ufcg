package entity;

import java.util.List;
import java.util.ArrayList;

public class BlockProfileEntity {
	public final String cg;
	protected String famPath;
	protected List<String> ctg;
	protected List<Integer> spos, epos;
	
	private int bcnt = 0;
	public boolean isValid(){return bcnt > 0;}
	public boolean isSingle(){return bcnt == 1;}

	public BlockProfileEntity(String cg, String famPath) {
		this.cg = cg;
		this.famPath = famPath;
		this.ctg = new ArrayList<String>();
		this.spos = new ArrayList<Integer>();
		this.epos = new ArrayList<Integer>();
	}
	
	public void update(String c, int s, int e) {
		ctg.add(c);
		spos.add(s);
		epos.add(e);
		bcnt++;
	}
	
	public String getFam() {return famPath;}
	public int getCnt() {return bcnt;}
	public String getCtg(int idx) {return ctg.get(idx);}
	public int getSpos(int idx) {return spos.get(idx);}
	public int getEpos(int idx) {return epos.get(idx);}
}
