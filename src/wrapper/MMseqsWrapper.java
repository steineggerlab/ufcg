package wrapper;

import envs.config.PathConfig;
import envs.toolkit.ExecHandler;
import envs.toolkit.Shell;

public class MMseqsWrapper extends ExecHandler {
	public MMseqsWrapper() {
		super.init(PathConfig.MMseqsPath);
	}
	
	public void setSearchType(int type) {
		super.addArg("--search-type", String.valueOf(type));
	}
	
	public void setEvalue(double evalue) {
		super.addArg("-e", String.valueOf(evalue));
	}
	
	public void setSens(double sens) {
		super.addArg("-s", String.valueOf(sens));
	}
	
	public void setThreads(int threads) {
		super.addArg("--threads", String.valueOf(threads));
	}
	
	public void setCoverage(double cov) {
		super.addArg("--cov-mode", "0");
		super.addArg("-c", String.valueOf(cov));
	}
	
	public void setFormatOutput(String formatOutput) {
		super.addArg("--format-output", formatOutput);
	}
	
	void setModule(String module) {
		super.addArg(module);
	}
	
	// add file or db
	void addFile(String file) {
		super.addArg(file);
	}
	
	public void setEasySearch(String query, String target, String result, String tmpdir) {
		setModule("easy-search");
		addFile(query);
		addFile(target);
		addFile(result);
		addFile(tmpdir);
	}
	
	public void setCreatedb(String seq, String db) {
		setModule("createdb");
		addFile(seq);
		addFile(db);
	}
	
	public void setRmdb(String db) {
		setModule("rmdb");
		addFile(db);
	}
	
	public void setConcatdbs(String idb, String jdb, String odb) {
		setModule("concatdbs");
		addFile(idb);
		addFile(jdb);
		addFile(odb);
	}
	
	public void setSearch(String qdb, String tdb, String adb, String tmpdir) {
		setModule("search");
		addFile(qdb);
		addFile(tdb);
		addFile(adb);
		addFile(tmpdir);
	}
	
	public void setConvertalis(String qdb, String tdb, String adb, String out) {
		setModule("convertalis");
		addFile(qdb);
		addFile(tdb);
		addFile(adb);
		addFile(out);
	}
	
	public void setConvert2Fasta(String db, String fa) {
		setModule("convert2fasta");
		addFile(db);
		addFile(fa);
	}
	
	public String[] exec() {
		return super.exec();
	}
	
	// solve dependency
	public static boolean solve() {
		String cmd = PathConfig.MMseqsPath + " 2>&1";
		String[] raw = Shell.exec(cmd);
		if(raw[0].contains("not found")) return false;
		for(int error_loc = 0; !raw[error_loc].contains("MMseqs2"); error_loc++) {
			if(error_loc + 1 == raw.length) return false;
		}
		return true;
	}
}
