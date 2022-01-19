package tree;

import java.io.IOException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import tree.tools.AlignMode;
import tree.tools.PhylogenyTool;

public class TreeModule {
	public static void run(String[] args) {
		
		String outDirectory = null;
		String ucgDirectory = null;
		
		String mafftPath = null;
		String raxmlPath = null;
		String fastTreePath = null;
		PhylogenyTool phylogenyTool = PhylogenyTool.raxml;
		
		String runOutDirName = null;
		int nThreads = 1;
		AlignMode alignMode = AlignMode.codon;
		int filtering = 50;
		
		CommandLineParser parser = new DefaultParser();
		
		Options options = new Options();
		options.addOption("ucg_dir", true, "directory for ucg files");
		options.addOption("out_dir", true, "directory for output files");
		options.addOption("mafft", true, "mafft path");
		options.addOption("raxml", true, "raxml path (Only when raxml is used)");
		options.addOption("fasttree", true, "fasttree path (Only when fasttree is used)");
		options.addOption("run_id", true, "run identifier");
		options.addOption("a", true, "alignment method (default : codon). nucleotide, codon, codon12, protein");
		options.addOption("t", true, "number of threads to be used (default : 1)");
		options.addOption("f", true, "filtering cutoff for gap-containing positions (default: 50). 1~100");
		
		try {
			
			CommandLine line = parser.parse(options, args);
			
			ucgDirectory = line.getOptionValue("ucg_dir");
			outDirectory = line.getOptionValue("out_dir");
			mafftPath = line.getOptionValue("mafft");
			raxmlPath = line.getOptionValue("raxml");
			fastTreePath = line.getOptionValue("fasttree");
			runOutDirName = line.getOptionValue("run_id");
			
			String align = line.getOptionValue("a");
			if(line.hasOption("a")) {
				if(align.equals("nucleotide")) {
					alignMode = AlignMode.nucleotide;
				}else if(align.equals("codon")) {
					alignMode = AlignMode.codon;
				}else if(align.equals("codon12")) {
					alignMode = AlignMode.codon12;
				}else if(align.equals("protein")) {
					alignMode = AlignMode.protein;
				}else {
					System.err.println("Invalid align mode. Exit!");
					System.exit(1);
				}
			}
			
			if(line.hasOption("t")) {
				try {
					nThreads = Integer.parseInt(line.getOptionValue("t"));
					
					if(nThreads<1) {
						System.exit(1);
					}
				}catch(NumberFormatException ex) {
					ex.printStackTrace();
					System.exit(1);
				}
			}
			
			if(line.hasOption("f")) {
				try {
					filtering = Integer.parseInt(line.getOptionValue("f"));
					
				}catch(NumberFormatException ex) {
					ex.printStackTrace();
					System.exit(1);
				}
			}
			
		}catch(ParseException e) {
			e.printStackTrace();
		}
		
		if(args.length==0||outDirectory==null||ucgDirectory==null||mafftPath==null||(raxmlPath==null&&fastTreePath==null)) {
			printMenu();
			System.exit(1);
		}
		
		//ProcPhylogenyForGeneSets proc = new ProcPhylogenyForGeneSets(geneSetJsonsDirectory, outPath, outDirectory, mafftPath, raxmlPath);
		TreeBuilder proc = new TreeBuilder(ucgDirectory, outDirectory, runOutDirName, mafftPath, raxmlPath, fastTreePath, alignMode, filtering);
		
		try {
			proc.jsonsToTree(nThreads, phylogenyTool);
		}catch (IOException e) {
			e.printStackTrace();
		}
		
		
	}
	
	private static void printMenu() {
		System.err.println("-----------------------------");
		System.err.println("Mandatory");
		System.err.println("    -ucg_dir    directory for ucg files");
		System.err.println("    -out_dir    directory for output files");
		System.err.println("    -mafft      mafft path");
		System.err.println("    -raxml      raxml path (Only when raxml is used)");
		System.err.println("    -fasttree   fasttree path (Only when fasttree is used)");
		System.err.println("");
		System.err.println("Optional");
		System.err.println("    -run_id     run identifier");
		System.err.println("    -a          alignment method (default : codon)");
		System.err.println("    -t          number of threads to be used (default : 1)");
		System.err.println("    -f          filtering cutoff for gap-containing positions (default: 50)");
		System.err.println("");
	}
	
	public static void main(String[] args) {
		run(args);
	}
}
