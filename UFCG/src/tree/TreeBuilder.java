package tree;

//import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;

import tree.tools.DetectedGeneDomain;
import tree.tools.GeneSetByGenomeDomain;
import tree.tools.AlignMode;
import tree.tools.PhylogenyTool;
import tree.tools.FastaSeq;
import tree.tools.FastaSeqList;
import tree.tools.StreamGobbler;
import tree.tools.FileUtils;
import tree.tools.ReplaceAcc;
import tree.tools.MafftForGeneSet;

public class TreeBuilder {
	private String geneSetJsonsDirectory = null;
	private String outDirectory = null;
	private String runOutDirName = null;
	
	private String mafftPath = null;
	private String raxmlPath = null;
	private String fastTreePath = null;
	
	private AlignMode alignMode = AlignMode.codon;
	private int filtering = 50; // removes the column which has more than 50% gaps
	
	private ArrayList<Long> genomeList = null;// Genomes used for the analysis
	private HashMap<String, String> replaceMap = null;
	private ArrayList<String> targetGenes = null;
	private HashSet<String> usedGenes = null;
	
	// output files
	private String concatenatedSeqFileName = null;
	private String treeZzFileName = null;
	private String treeLabelFileName = null;
	
	public TreeBuilder(String ucgDirectory, String outDirectory, String mafftPath,
			String raxmlPath, String fastTreePath) {
		if (!ucgDirectory.endsWith(File.separator)) {
			ucgDirectory = ucgDirectory + File.separator;
		}
		if (!outDirectory.endsWith(File.separator)) {
			outDirectory = outDirectory + File.separator;
		}
		this.geneSetJsonsDirectory = ucgDirectory;
		this.outDirectory = outDirectory;
		this.runOutDirName = String.valueOf(new Timestamp(System.currentTimeMillis()).getTime()) + File.separator;
		this.mafftPath = mafftPath;
		this.raxmlPath = raxmlPath;
		this.fastTreePath = fastTreePath;
		
		this.genomeList = new ArrayList<>();
		this.replaceMap = new HashMap<>();
		this.targetGenes = new ArrayList<>();
		this.usedGenes = new HashSet<String>();
		this.concatenatedSeqFileName = outDirectory + runOutDirName + "aligned_concatenated.zZ.fasta";
		this.treeZzFileName = outDirectory + runOutDirName + "concatenated." + alignMode + "." + filtering + ".zZ.nwk";
		this.treeLabelFileName = outDirectory + runOutDirName + "concatenated." + alignMode + "." + filtering + ".label.nwk";
	}
	
	public TreeBuilder(String ucgDirectory, String outDirectory, String runOutDirName, String mafftPath,
			String raxmlPath, String fastTreePath, AlignMode alignMode) {
		
		this(ucgDirectory, outDirectory, mafftPath, raxmlPath, fastTreePath);
		
		if(runOutDirName!=null) {
			if (!runOutDirName.endsWith(File.separator)) {
				runOutDirName = runOutDirName + File.separator;
			}
			this.runOutDirName = runOutDirName;
		}
		this.concatenatedSeqFileName = this.outDirectory + this.runOutDirName + "aligned_concatenated.zZ.fasta";
		this.treeZzFileName = this.outDirectory + this.runOutDirName + "concatenated." + alignMode + "." + filtering + ".zZ.nwk";
		this.treeLabelFileName = this.outDirectory + this.runOutDirName + "concatenated." + alignMode + "." + filtering + ".label.nwk";
	}
	
	public TreeBuilder(String ucgDirectory,String outDirectory, String runOutDirName, String mafftPath, 
			                        String raxmlPath, String fastTreePath, 
			                        AlignMode alignMode, int filtering) {
		this(ucgDirectory, outDirectory, runOutDirName, mafftPath, raxmlPath, fastTreePath, alignMode);
		
		if (filtering <= 0 || filtering > 100) {
			System.err.print("filtering must be a value between 1~100. Exit!");
			System.exit(1);
		}
		this.alignMode = alignMode;
		this.filtering = filtering;
	}
	
	public void jsonsToTree(int nThreads, PhylogenyTool tool) throws IOException{
		
		checkThirdPartyPrograms(tool);
		
		readJsonsToFastaFiles();
		alignGenes(nThreads); // 
		concatenateAlignedGenesRemoveGaps(); 
		inferTree(tool, nThreads); // fasttree or raxml
		replaceLabel();
	}
	
	void readJsonsToFastaFiles() throws IOException{
		
		checkPathDirectory();
		
		// ucg files
		File dir = new File(geneSetJsonsDirectory);

		File[] files = dir.listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".ucg");
			}
		});
		
		// 1. check all of the ucg has same target gene set
		checkIfSameTargetGeneSets(files);
		
		// 2. jsons to geneSetByGenomeDomains
		List<GeneSetByGenomeDomain> geneSetsDomainList = jsonsToGeneSetDomains(files);
		HashMap<String, Integer> uidLabelMap = new HashMap<String, Integer>();
		
		for(GeneSetByGenomeDomain domain : geneSetsDomainList) {
			long uid = domain.getUid();			
			String label = domain.getLabel() + " " + domain.getStrainName();//+ domain.getTaxonomy();
			genomeList.add(uid);
			
			if(uidLabelMap.containsKey(label)){
				uidLabelMap.put(label, uidLabelMap.get(label)+1);
			}else{
				uidLabelMap.put(label, 1);
			}
			
			if(uidLabelMap.get(label)!=1){
				label = label + "_" + uidLabelMap.get(label);
			}
			
			replaceMap.put(String.valueOf(uid), label);
		}
		
		// 3. retrieve fasta files
		
		if(alignMode.equals(AlignMode.codon)||alignMode.equals(AlignMode.codon12)) {
			retrieveFastaNucProFiles(geneSetsDomainList);
		}else if(alignMode.equals(AlignMode.nucleotide)||alignMode.equals(AlignMode.protein)) {
			retrieveFastaFiles(geneSetsDomainList);
		}
	}
	
	void alignGenes(int nThreads) {
		
		int geneNum = usedGenes.size();
		
		System.out.println();
		System.out.println("Aligning each gene..");
		System.out.println("\nTotal # of genes to be aligned : " + geneNum);
		
		align(nThreads);
		
		System.out.println();
		System.out.println("MSA is finished!");
		System.out.println();
		
		// convert to codon
		if(alignMode.equals(AlignMode.codon)||alignMode.equals(AlignMode.codon12)) {
			proAlignToCodon();
		}		

	}

	
	void concatenateAlignedGenesRemoveGaps() {
		StringBuffer concatenatedFasta = new StringBuffer();
		
		System.out.println();
		System.out.println("Concatenating aligned genes..");
		
		List<String> fileList = new ArrayList<String>();

		for (String gene : usedGenes) {
			fileList.add(alignedFinalGeneFastaFile(gene));
		}
		
		HashMap<String, Integer> geneLengthMap = new HashMap<String, Integer>();

		LinkedHashMap<String, FastaSeqList> geneFastaSeqListMap = new LinkedHashMap<String, FastaSeqList>();

		if(alignMode.equals(AlignMode.codon)||alignMode.equals(AlignMode.codon12)) {
			for (String fileName : fileList) {
				FastaSeqList fsl = new FastaSeqList();
				fsl.importFile(fileName);

				// check if all the sequence length are same
				Integer seqLength = null;
				for (FastaSeq fs : fsl.list) {
						
					String stopCodon = fs.sequence.substring(fs.sequence.length()-3);
					if(!stopCodon.equals("TAA")&&!stopCodon.equals("TAG")&&!stopCodon.equals("TGA")){
						fs.sequence = fs.sequence + "---";
					}
					
					if (seqLength == null) {
						seqLength = fs.sequence.length();
					} else {
						if (seqLength != fs.sequence.length()) {
							System.out.println("Error : \"" + fileName + "\" has different sequence length.");
							System.exit(1);
						}
					}
				}
				geneLengthMap.put(fileName, seqLength);
				geneFastaSeqListMap.put(fileName, fsl);

			}
		}else {
			for (String fileName : fileList) {
				FastaSeqList fsl = new FastaSeqList();
				fsl.importFile(fileName);

				// check if all the sequence length are same
				Integer seqLength = null;
				for (FastaSeq fs : fsl.list) {
					if (seqLength == null) {
						seqLength = fs.sequence.length();
					} else {
						if (seqLength != fs.sequence.length()) {
							System.out.println("Error : \"" + fileName + "\" has different sequence length.");
							System.exit(1);
						}
					}
				}
				geneLengthMap.put(fileName, seqLength);
				geneFastaSeqListMap.put(fileName, fsl);

			}
		}
		

		int count = 0;
		for (Long genomeUid : genomeList) {
			
			String zZgenomeUid = "zZ" + genomeUid + "zZ";
			
			if (count == 0) {
				concatenatedFasta.append(">" + zZgenomeUid + "\n");
			} else {
				concatenatedFasta.append("\n>" + zZgenomeUid + "\n");
			}
			count++;

			for (String key : geneFastaSeqListMap.keySet()) {
				
				if (geneFastaSeqListMap.get(key).find(String.valueOf(zZgenomeUid)) == null) {
					StringBuffer gaps = new StringBuffer();
					for (int i = 0; i < geneLengthMap.get(key); i++) {
						gaps.append("-");
					}
					concatenatedFasta.append(gaps.toString());
				} else {
					concatenatedFasta.append(geneFastaSeqListMap.get(key).find(zZgenomeUid).sequence);
				}
			}
		}
		
		String filteredFasta = removeGapColumns(concatenatedFasta.toString());

		try{
			FileWriter fw = new FileWriter(concatenatedSeqFileName);
			fw.append(filteredFasta);
			fw.close();
		}catch(IOException e){
			e.getMessage();
			System.err.println("Error : Cannot write a file in the directory '" + outDirectory + runOutDirName);
			System.exit(1);
		}
	}

	
	void inferTree(PhylogenyTool tool, int nThreads) {
		
		System.out.println();
		System.out.println("Reconstructing the final tree..");
		
		if(tool.equals(PhylogenyTool.raxml)) {
			runRaxml(nThreads);
		}else if(tool.equals(PhylogenyTool.fasttree)) {
			runFasttree(nThreads);
		}
		
	}
	
	void replaceLabel() {
		LabelReplacer replacer = new LabelReplacer();
		replacer.replace_name(treeZzFileName,treeLabelFileName,replaceMap);
	}

	private void runRaxml(int nThreads) {
		
		List<String> argTree = new ArrayList<String>();
		
		String prefix = runOutDirName.substring(0, runOutDirName.length()-1);
		
		argTree.add(raxmlPath);
		argTree.add("-s");
		argTree.add(concatenatedSeqFileName);

		argTree.add("-n");
		argTree.add(prefix);

		argTree.add("-T");
		argTree.add(Integer.toString(nThreads));
		
		if(alignMode.equals(AlignMode.protein)) {
			argTree.add("-m");
			argTree.add("PROTCATJTT");
		}else {
			argTree.add("-m");
			argTree.add("GTRCAT");
		}
		
		argTree.add("-p");
		argTree.add("123");
		
		runPhylogenyToolByProcess(argTree, PhylogenyTool.raxml);

		new File("RAxML_bestTree." + prefix).renameTo(new File(treeZzFileName));
		new File("RAxML_info." + prefix).delete();
		new File("RAxML_log." + prefix).delete();
		new File("RAxML_parsimonyTree." + prefix).delete();
		new File("RAxML_result." + prefix).delete();

	}
	private void runFasttree(int nThreads) {
		
		List<String> argTree = new ArrayList<String>();
		
		argTree.add("bash");
		argTree.add("-c");
		
		if (alignMode.equals(AlignMode.protein)) {
			argTree.add(fastTreePath + " " + concatenatedSeqFileName + " > " + treeZzFileName);
		} else {
			argTree.add(fastTreePath + " -nt -gtr < " + concatenatedSeqFileName + " > " + treeZzFileName);
		}
		
		runPhylogenyToolByProcess(argTree, PhylogenyTool.fasttree);
	}
	
	private void runPhylogenyToolByProcess(List<String> argTree, PhylogenyTool phylogenyTool) {
		try {

			System.out.println(argTree);

			Process tree = new ProcessBuilder(argTree).start();

			StreamGobbler outGobbler = new StreamGobbler(tree.getInputStream(), null, false);
			StreamGobbler errorGobbler = new StreamGobbler(tree.getErrorStream(), null, false);

			outGobbler.start();
			errorGobbler.start();

			try {
				tree.waitFor();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			if (tree.exitValue() != 0) {
				String log = errorGobbler.LogMessage();
				System.err.println(log);
				if (phylogenyTool.equals(PhylogenyTool.raxml)) {
					System.err.println("Error occured during running RAxML.");
				}else {
					System.err.println("Error occured during running FastTree.");
				}
				System.exit(1);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void checkPathDirectory() {
		
		File path = new File(outDirectory);
		
		if(!path.exists()) {
			if(!path.mkdir()) {
				System.err.println(outDirectory + " doesn't exist and can't be created.");
				System.exit(1);
			}
		}
		
		if(!new File(outDirectory).canWrite()) {
			System.err.println("Cannot write files to " + outDirectory + ". Check the permission.");
			System.exit(1);
		}else {
			new File(outDirectory + runOutDirName).mkdir();
		}
	}
	
	void checkThirdPartyPrograms(PhylogenyTool phylogenyTool) {
		if(phylogenyTool.equals(PhylogenyTool.fasttree)) {
			testMafft(mafftPath);
			testFasttree(fastTreePath);
		}else if(phylogenyTool.equals(PhylogenyTool.raxml)) {
			testMafft(mafftPath);
			testRaxml(raxmlPath);
		}
	}
	
	private void align(int nThreads) {
		
		try{
			ExecutorService exeService = Executors.newFixedThreadPool(nThreads);
		
			int counter[] = {0};
			for(String gene : usedGenes){
				
				String fastaFile = fastaFileName(gene);
				String alignedFastaFile = alignedFastaFileName(gene);
				
				exeService.execute(new multipleAlign(mafftPath, alignMode, fastaFile, alignedFastaFile, counter, gene, usedGenes.size(), filtering));
		
			}
			System.out.println();
			
			exeService.shutdown();
			
			while(!exeService.awaitTermination(1, TimeUnit.SECONDS)){
			}
			
		}catch(InterruptedException e){
			System.err.println(e.getMessage());
			System.exit(1);
		}
		
	}
	
	private void proAlignToCodon() {
		for(String gene : usedGenes) {
			alignedFastaFileName(gene);
		}
		System.out.println();
		System.out.println("Converting protein alignments to DNA(codon) alignments..");
		
		for(String gene : usedGenes){
			String nucFile = fastaFileName(gene, AlignMode.nucleotide);
			String alignedProFile = alignedFastaFileName(gene);
			
			FastaSeqList nucFasta = new FastaSeqList();
			nucFasta.importFile(nucFile);
			
			FastaSeqList alignedProFasta= new FastaSeqList();
			alignedProFasta.importFile(alignedProFile);
								
			for(int i=0; i<alignedProFasta.list.size();i++){
				FastaSeq alignedFastaSeq = alignedProFasta.list.get(i);
				
				FastaSeq dnaFastaSeq = nucFasta.find(alignedFastaSeq.title);
				
				char[] alignedSeq = alignedFastaSeq.sequence.toCharArray();
				String dnaSeq = dnaFastaSeq.sequence;
				StringBuffer alignedCodonSB = new StringBuffer(dnaSeq);
				
				for(int k=0; k<alignedSeq.length;k++){
					char ch = alignedSeq[k];
					if(ch=='-'){
						int index = 3*k;
						alignedCodonSB.insert(index, "---");
					}
				}
				if(alignMode.equals(AlignMode.codon)) {
					nucFasta.find(alignedFastaSeq.title).sequence = alignedCodonSB.toString();
				}else if(alignMode.equals(AlignMode.codon12)) {
					char[] seqArray = alignedCodonSB.toString().toCharArray();
					StringBuffer codon12SB = new StringBuffer();
					
					for(int k=0;k<seqArray.length;k++){
						if(k%3!=2){
							codon12SB.append(seqArray[k]);
						}
					}
					
					nucFasta.find(alignedFastaSeq.title).sequence = codon12SB.toString();
				}
			}	
			// write aligned bcgDNAfile
			nucFasta.write(alignedCodonFile(gene));
		}
	}
	
	private String removeGapColumns(String concatFasta) {
		
		FastaSeqList conFastaSeqList = new FastaSeqList();
		
		conFastaSeqList.importString(concatFasta);
		
		int numOfGenome = conFastaSeqList.list.size();
		
		int seqLength = conFastaSeqList.list.get(0).sequence.length();
		
		int[] numGaps = new int[seqLength];
		
		Arrays.fill(numGaps, 0);
			
		for(FastaSeq fastaSeq : conFastaSeqList.list){
			String seq = fastaSeq.sequence;
			
			for(int pos=0;pos<seq.length();pos++){
				if(fastaSeq.sequence.charAt(pos)=='-'){
					numGaps[pos]++;
				}
			}
		}
		
		double percentageFilter = (double) 1  -  (double) filtering/(double)100;
		
		for(FastaSeq fastaSeq : conFastaSeqList.list){
			String seq = fastaSeq.sequence;
			char[] seqArray = seq.toCharArray();
			StringBuffer seqSB = new StringBuffer();
			
			for(int pos=0;pos<seq.length();pos++){
				if(numGaps[pos] > ((float) numOfGenome * percentageFilter)){
					continue;
				}else{
					seqSB.append(seqArray[pos]);
				}
			}
		
			fastaSeq.sequence = seqSB.toString();

		}
		
		return conFastaSeqList.getString();
		
	}
	
	private void retrieveFastaNucProFiles(List<GeneSetByGenomeDomain> geneSetsDomainList) throws IOException {	
		
		for (String gene : targetGenes) {

			StringBuffer sbNuc = new StringBuffer();
			StringBuffer sbPro = new StringBuffer();
			
			int nuc = 0;
			int pro = 0;
			
			for (GeneSetByGenomeDomain geneSetsDomain : geneSetsDomainList) {

				long uid = geneSetsDomain.getUid();

				HashMap<String, ArrayList<DetectedGeneDomain>> dataMap = geneSetsDomain.getDataMap();
				if (dataMap.containsKey(gene)) {
					if(dataMap.get(gene).size()!=0) {
						DetectedGeneDomain geneDomain = dataMap.get(gene).get(0);
						String nucSeq = geneDomain.getDna();
						String proSeq = geneDomain.getProtein();

						if (nucSeq != null) {
							sbNuc.append(">zZ" + uid + "zZ\n");
							sbNuc.append(nucSeq);
							sbNuc.append("\n");
							nuc++;
						}
						if (proSeq != null) {
							sbPro.append(">zZ" + uid + "zZ\n");
							sbPro.append(proSeq);
							sbPro.append("\n");
							pro++;
						}

					}
				}
			}
			
			if(nuc<4 && pro<4) {
				System.err.println("Less than 4 species have '" + gene + "', this sequence is excluded in further analysis.");
			}
			
			if (sbNuc.length() != 0 && nuc>3) {
				FileWriter fileNucWriter = new FileWriter(fastaFileName(gene, AlignMode.nucleotide));
				fileNucWriter.append(sbNuc.toString());
				fileNucWriter.close();
				usedGenes.add(gene);
			}

			if (sbPro.length() != 0 && pro>3) {
				FileWriter fileProWriter = new FileWriter(fastaFileName(gene, AlignMode.protein));
				fileProWriter.append(sbPro.toString());
				fileProWriter.close();
				usedGenes.add(gene);
			}

		}
	}
	private void retrieveFastaFiles(List<GeneSetByGenomeDomain> geneSetsDomainList) throws IOException {
		
		for (String gene : targetGenes) {

			StringBuffer sb = new StringBuffer();

			int num = 0;
			
			for (GeneSetByGenomeDomain geneSetsDomain : geneSetsDomainList) {

				long uid = geneSetsDomain.getUid();

				HashMap<String, ArrayList<DetectedGeneDomain>> dataMap = geneSetsDomain.getDataMap();
				if (dataMap.containsKey(gene)) {
					if(dataMap.get(gene).size()!=0) {
						DetectedGeneDomain geneDomain = dataMap.get(gene).get(0);
						
						String seq = null;
						
						if(alignMode.equals(AlignMode.nucleotide)) {
							seq = geneDomain.getDna();
						}else if(alignMode.equals(AlignMode.protein)) {
							seq = geneDomain.getProtein();
						}
						
						if (seq != null) {
							sb.append(">zZ" + uid + "zZ\n");
							sb.append(seq);
							sb.append("\n");
							num++;
						}
					}
				}
			}
			
			if(num<4) {
				System.err.println("Less than 4 species have '" + gene + "', this sequence is excluded in further analysis.");
			}
			
			if (sb.length() != 0 && num>3) {
				FileWriter fileNucWriter = new FileWriter(fastaFileName(gene));
				fileNucWriter.append(sb.toString());
				fileNucWriter.close();
				usedGenes.add(gene);
			}
		}
	}

	private List<GeneSetByGenomeDomain> jsonsToGeneSetDomains(File[] files) {
		List<GeneSetByGenomeDomain> geneSetsDomainList = new ArrayList<GeneSetByGenomeDomain>();

		for (File file : files) {

			String filePath = file.getAbsolutePath();
			String geneSetJson = FileUtils.readTextFile2StringWithCR(filePath);
			GeneSetByGenomeDomain geneSetDomain = GeneSetByGenomeDomain.jsonToDomain(geneSetJson);
			geneSetsDomainList.add(geneSetDomain);

		}
		
		return geneSetsDomainList;
	}
	private boolean checkIfSameTargetGeneSets(File[] files) throws JSONException {
		
		System.out.println(files.length + " gene set files are detected.");
		
		if(files.length<3) {
			System.err.println("Too few species! Exit.");
			System.exit(1);
		}
		
		boolean same = false;
		
		File refFile = files[0];
		//System.out.println(refFile.getAbsolutePath());
		ArrayList<String> refTargetGenes = fileToTargetGeneList(refFile);
		
		targetGenes = refTargetGenes;
		
		for(File geneSetFile : files) {
			ArrayList<String> targetGenes = fileToTargetGeneList(geneSetFile);
			if(!sameTargetGenes(refTargetGenes, targetGenes)) {
				System.err.println("Gene set files have different target genes.");
				System.exit(1);
			}
		}
		same = true;
		
		return same;
	}
	
	private ArrayList<String> fileToTargetGeneList(File geneSetJsonFile) throws JSONException {
		String geneSetFile = geneSetJsonFile.getAbsolutePath();
		String geneSetJson = FileUtils.readTextFile2StringWithCR(geneSetFile);
		GeneSetByGenomeDomain geneSetByGenomeDomain = GeneSetByGenomeDomain.jsonToDomain(geneSetJson);
		String geneSet = geneSetByGenomeDomain.getTargetGeneSet();
		String[] targetGenes = geneSet.split(",");
		ArrayList<String> list = new ArrayList<String>(Arrays.asList(targetGenes));
		return list;
	}
	
	private boolean sameTargetGenes(ArrayList<String> ref, ArrayList<String> target) {
		
		if(ref.size()!=target.size()) {
			return false;
		}
		
		for(String gene : ref) {
			if(!target.contains(gene)) {
				return false;
			}
		}
		
		return true;
	}
	
	private String fastaFileName(String gene) {
		
		String fasta = null;
		
		if(alignMode.equals(AlignMode.nucleotide)) {
			fasta = outDirectory + runOutDirName + gene + "_nuc.zZ.fasta";
		}else if(alignMode.equals(AlignMode.protein)||alignMode.equals(AlignMode.codon)||alignMode.equals(AlignMode.codon12)) {
			fasta = outDirectory + runOutDirName + gene + "_pro.zZ.fasta";
		}
		
		return fasta;
		
	}
	
	private String fastaFileName(String gene, AlignMode alignMode) {
		
		String fasta = null;
		
		if(alignMode.equals(AlignMode.nucleotide)) {
			fasta = outDirectory + runOutDirName + gene + "_nuc.zZ.fasta";
		}else if(alignMode.equals(AlignMode.protein)||alignMode.equals(AlignMode.codon)||alignMode.equals(AlignMode.codon12)) {
			fasta = outDirectory + runOutDirName + gene + "_pro.zZ.fasta";
		}
		
		return fasta;
		
	}
	
	private String alignedFastaFileName(String gene) {
		
		String fasta = null;
		
		if(alignMode.equals(AlignMode.nucleotide)) {
			fasta = outDirectory + runOutDirName + "aligned_" + gene + "_nuc.zZ.fasta";
		}else if(alignMode.equals(AlignMode.protein)||alignMode.equals(AlignMode.codon12)||alignMode.equals(AlignMode.codon)) {
			fasta = outDirectory + runOutDirName + "aligned_" + gene + "_pro.zZ.fasta";
		}
		
		return fasta;
		
	}
	
	private String alignedCodonFile(String gene) {
		String fasta = outDirectory + runOutDirName + "aligned_" + gene + "_codon.zZ.fasta";
		return fasta;
	}
	private String alignedCodon12File(String gene) {
		String fasta = outDirectory + runOutDirName + "aligned_" + gene + "_codon12.zZ.fasta";
		return fasta;
	}
	private String alignedFinalGeneFastaFile(String gene) {
		String fasta = null;
		
		if(alignMode.equals(AlignMode.nucleotide)) {
			fasta = outDirectory + runOutDirName + "aligned_" + gene + "_nuc.zZ.fasta";
		}else if(alignMode.equals(AlignMode.protein)) {
			fasta = outDirectory + runOutDirName + "aligned_" + gene + "_pro.zZ.fasta";
		}else if(alignMode.equals(AlignMode.codon)) {
			fasta = alignedCodonFile(gene);
		}else if(alignMode.equals(AlignMode.codon12)) {
			fasta = alignedCodon12File(gene);
		}
			
		return fasta;
	}
	private void testMafft(String mafftPath) {
		// mafft v.7.310
		// mafft test
		boolean mafft = false;

		// test external programs
		List<String> argMafft = new ArrayList<>();
		argMafft.add(mafftPath);
		argMafft.add("-h");

		try {

			Process testMafft = new ProcessBuilder(argMafft).start();

			BufferedReader stdOut = new BufferedReader(new InputStreamReader(testMafft.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(testMafft.getErrorStream()));

			String s;

			while ((s = stdOut.readLine()) != null) {
			}
			while ((s = stdError.readLine()) != null) {
				if (s.contains("  MAFFT ")) {
					mafft = true;
				}
			}

			stdOut.close();
			stdError.close();

			try {
				testMafft.waitFor();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		} catch (IOException e) {
			System.err.println(e.getMessage());
		}

		if (!mafft) {
			System.err.println(
					"Error : Check if MAFFT v7.310 is properly installed and check the program path in the 'programPath' file");
			System.exit(1);
		}
	}
	private void testFasttree(String fasttreePath) {
		// FastTree 2.1.10 SSE3
		boolean fasttree = false;

		List<String> argFasttree = new ArrayList<>();
		argFasttree.add(fasttreePath);
		argFasttree.add("-help");

		try {

			Process testFasttree = new ProcessBuilder(argFasttree).start();

			BufferedReader stdOut = new BufferedReader(new InputStreamReader(testFasttree.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(testFasttree.getErrorStream()));
//			BufferedOutputStream stdin = new BufferedOutputStream(testFasttree.getOutputStream());

			String s;

			while ((s = stdOut.readLine()) != null) {
			}

			while ((s = stdError.readLine()) != null) {
				if (s.contains("FastTree ")) {
					fasttree = true;
				}
			}

			stdOut.close();
			stdError.close();

			try {
				testFasttree.waitFor();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		} catch (IOException e) {
			System.err.println(e.getMessage());
		}

		if (!fasttree) {
			System.err.println(
					"Error : Check if FastTree version 2.1.10 SSE3 is properly installed and check the program path in the 'programPath' file");
			System.exit(1);
		}
	}

	private void testRaxml(String raxmlPath) {
		boolean raxml = false;

		List<String> argRaxml = new ArrayList<>();
		argRaxml.add(raxmlPath);
		argRaxml.add("-h");

		try {

			Process testRaxml = new ProcessBuilder(argRaxml).start();

			BufferedReader stdOut = new BufferedReader(new InputStreamReader(testRaxml.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(testRaxml.getErrorStream()));
//			BufferedOutputStream stdin = new BufferedOutputStream(testRaxml.getOutputStream());

			String s;

			while ((s = stdOut.readLine()) != null) {
				if (s.contains("This is RAxML version 8")) {
					raxml = true;
				}
			}

			while ((s = stdError.readLine()) != null) {
			}

			stdOut.close();
			stdError.close();

			try {
				testRaxml.waitFor();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		} catch (IOException e) {
			System.err.println(e.getMessage());
		}

		if (!raxml) {
			System.err.println(
					"Error : Check if RAxML version 8 is properly installed and check the program path in the 'programPath' file");
			System.exit(1);
		}
	}
}

class multipleAlign implements Runnable{
	
	String mafftPath;
	AlignMode alignMode;
	String fastaFile;
	String alignedFastaFile;
	int[] counter;
	String gene;
	int geneNum;
	double filter;
	
	public multipleAlign(String mafftPath, AlignMode alignMode, String fastaFile, String alignedFastaFile, int[] counter, String gene, int geneNum, double filter) {
		
		this.mafftPath = mafftPath;
		this.alignMode = alignMode;
		this.fastaFile = fastaFile;
		this.alignedFastaFile = alignedFastaFile;
		this.counter = counter;
		this.gene = gene;
		this.geneNum = geneNum;
		this.filter = filter;
	}
	
	public static synchronized void updateCounter(String bcg, int[] counter, int bcgNum) {
		counter[0]++;
		String msg = bcg + " alignment was completed. (" + counter[0] + " / " + bcgNum + ")";
		System.out.println(msg);
	}

	
	public void run(){
		
		MafftForGeneSet mw = new MafftForGeneSet(mafftPath, alignMode);
		if(alignMode.equals(AlignMode.codon)||alignMode.equals(AlignMode.codon12)){
			mw.setInputOutput(fastaFile, alignedFastaFile);
		}else{
			mw.setInputOutput(fastaFile, alignedFastaFile);
		}
		
		try {
			mw.execute();
		}catch(InterruptedException e) {
			System.err.println("Error occurred!");
			System.err.println(e.getMessage());
			System.exit(1);
		}catch(IOException ex) {
			System.err.println("Error occurred!");
			System.err.println(ex.getMessage());
			System.exit(1);
		}
		
		
		// finished
		updateCounter(gene, counter, geneNum);
		
	}
}

class LabelReplacer {

	public String replace_name_str(String ori_str, HashMap<String, String> replaceMap) {

		String[] nodes = ori_str.split("zZ");
		ReplaceAcc ra = new ReplaceAcc();

		for (int i = 1; i < nodes.length; i = i + 2) {
			String uid = nodes[i];
			String label = replaceMap.get(uid);
			ra.add(uid + "", label);
		}
		return ra.replace(ori_str, true);

	}

	public void replace_name(String in_filename, String out_filename, HashMap<String, String> replaceMap) {
		if (in_filename == null) {
			return;
		}
		try {
			String ori_str = readTextFile2StringWithCR(in_filename);
			String new_str = replace_name_str(ori_str, replaceMap);
			try {
				FileWriter fw = new FileWriter(out_filename);
				fw.write(new_str);
				fw.close();
			} catch (IOException e) {
				System.err.println("Error : Cannot write file");
				System.exit(1);
			}

		} catch (IOException e) {
			System.err.println("Error : Cannot read file");
			System.exit(1);
		}

	}

	public void replace_name_delete(String in_filename, String out_filename, HashMap<String, String> replaceMap) {
		if (in_filename == null) {
			return;
		}
		try {
			String ori_str = readTextFile2StringWithCR(in_filename);
			String new_str = replace_name_str(ori_str, replaceMap);

			File in_file = new File(in_filename);

			if (in_file.getAbsoluteFile().getParentFile().canWrite()) {
				in_file.delete();
			}
			try {
				FileWriter fw = new FileWriter(out_filename);
				fw.write(new_str);
				fw.close();
			} catch (IOException e) {
				System.err.println("Error : Cannot write file");
				System.exit(1);
			}

		} catch (IOException e) {
			System.err.println("Error : Cannot read file");
			System.exit(1);
		}

	}

	static public String readTextFile2StringWithCR(String filename) throws IOException // with carrige return
	{
		StringBuffer sb = new StringBuffer("");

		FileReader fr = new FileReader(new File(filename));
		BufferedReader br = new BufferedReader(fr);
		String line = null;
		while ((line = br.readLine()) != null) {
			sb.append(line + "\n");
		}
		br.close();
		fr.close();

		return new String(sb);
	}
}
