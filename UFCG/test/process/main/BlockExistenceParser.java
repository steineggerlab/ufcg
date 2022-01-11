package process.main;

import java.io.IOException;
import java.util.*;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import envs.config.GenericConfig;
import envs.toolkit.FileStream;
import envs.toolkit.Prompt;
import envs.toolkit.Shell;

public class BlockExistenceParser {
	/*
	 * Runnable : BlockParser.jar
	 * Arguments :
	 * 		-i <DIR> 	(Required) : Directory containing BlockExistence results
	 * 		-p <TSV>	(Required) : Path to the pangenome TSV file
	 * 		-l <LIST>	(Required) : List of valid accessions
	 * 		-o <OUT>	(Required) : Output file path
	 * Output : 
	 * 		Parsed results of gene block existence in TSV format
	 * 			- Gene, COG, Ann, Sgl, Mul, Fnd, Mss, Dif, Desc
	 */
	
	protected class Gene {
		protected String name, cog, desc;
		protected Integer ann, sgl, mul, mss;
		
		protected Gene(String tsvLine) {
			String[] tsvVector = tsvLine.split("\t");
			
			this.name = tsvVector[1];
			this.cog  = tsvVector[4];
			this.desc = tsvVector[5];
			
			this.ann  = Integer.parseInt(tsvVector[2]);
			this.sgl  = 0;
			this.mul  = 0;
			this.mss  = 0;
		}
	}
	
	private List<Gene> genes;
	private Map<String, Integer> geneNameMap;
	private List<String> geneNames;
	
	public BlockExistenceParser(String panPath) throws IOException {
		this.genes = new ArrayList<Gene>();
		this.geneNameMap = new HashMap<String, Integer>();
		
		FileStream panStream = new FileStream(panPath, 'r');
		String line = panStream.readLine(); int iter = 0;
		while((line = panStream.readLine()) != null) {
			Gene gene = new Gene(line);
			this.genes.add(gene);
			this.geneNameMap.put(gene.name, iter++);
		}
		panStream.close();
		
		this.geneNames = new ArrayList<String>();
		this.geneNames.addAll(this.geneNameMap.keySet());
		Collections.sort(this.geneNames);
	}
	
	private void updateGenes(String profile) {
		for(int i = 0; i < profile.length(); i++) {
			Gene gene = genes.get(geneNameMap.get(geneNames.get(i)));
			
			switch(profile.charAt(i)) {
			case 'O': gene.mul++; break;
			case 'o': gene.sgl++; break;
			case 'x': gene.mss++; break;
			default:
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		String iPath, pPath, lPath, oPath, buf;
		
		Options opts = new Options();
		opts.addOption("i", true, "Directory containing BlockExistence results");
		opts.addOption("p", true, "Path to the pangenome TSV file");
		opts.addOption("l", true, "List of valid accessions");
		opts.addOption("o", true, "Output file path");
		
		CommandLineParser clp = new DefaultParser();
		CommandLine cmd = clp.parse(opts, args);
		if(!cmd.hasOption("i") || !cmd.hasOption("p") || !cmd.hasOption("o") || !cmd.hasOption("l")) {
			new HelpFormatter().printHelp("Fungible.process.main.BlockExistenceParser", opts);
			return;
		}
		
		iPath = cmd.getOptionValue("i");
		pPath = cmd.getOptionValue("p");
		lPath = cmd.getOptionValue("l");
		oPath = cmd.getOptionValue("o");
		
		GenericConfig.setHeader("BParse");
		GenericConfig.TSTAMP = true;
//		GenericConfig.DEV = true;
//		GenericConfig.VERB = true;
		BlockExistenceParser bep = new BlockExistenceParser(pPath);
		
		List<String> queries = new ArrayList<String>();
		FileStream listStream = new FileStream(lPath, 'r');
		while((buf = listStream.readLine()) != null) queries.add(buf);
		listStream.close();
		
		int proc = 0;
		for(String query : queries) {
			Prompt.dynamic("\r");
			
			// check file existence
			String path = iPath + "/" + query + ".out";
			if(!Shell.exec("file -b " + path)[0].startsWith("ASCII")) {
				Prompt.dynamicHeader(path + ": File not found");
				continue;
			}
			
			FileStream iStream = new FileStream(path, 'r');
			iStream.readLine();
			bep.updateGenes(iStream.readLine());
			iStream.close();
			Prompt.dynamicHeader(path + ": Processed");
			proc++;
		}
		
		System.out.println("");
		Prompt.print(String.format("%d files out of %d queries processed.", proc, queries.size()));
		double ppct = (double) proc / queries.size();
		
		FileStream outStream = new FileStream(oPath, 'w');
		outStream.println("Gene\tCOG\tAnn\tSgl\tMul\tFnd\tMss\tDif\tDesc");
		for(String name : bep.geneNames) {
			Gene gene = bep.genes.get(bep.geneNameMap.get(name));
			outStream.println(String.format("%s\t%s\t%d\t%d\t%d\t%d\t%d\t%f\t%s",
					gene.name, gene.cog, gene.ann, gene.sgl, gene.mul, gene.sgl + gene.mul, gene.mss,
					(double) (gene.sgl + gene.mul - gene.ann * ppct) / (gene.ann * ppct), gene.desc));
		}
		outStream.close();
	}
}
