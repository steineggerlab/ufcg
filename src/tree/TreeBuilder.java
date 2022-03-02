package tree;

//import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
//import java.nio.file.Files;
//import java.nio.file.Path;
//import java.nio.file.Paths;
//import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONObject;

import envs.config.GenericConfig;
import envs.config.PathConfig;
import envs.toolkit.ANSIHandler;
import envs.toolkit.Prompt;
import envs.toolkit.Shell;

import org.json.JSONException;
import lbj.BranchAnalysis;
import pipeline.ExceptionHandler;
import tree.tools.DetectedGeneDomain;
import tree.tools.GeneSetByGenomeDomain;
import tree.tools.AlignMode;
import tree.tools.PhylogenyTool;
import tree.tools.ProcessGobbler;
import tree.tools.FastaSeq;
import tree.tools.FastaSeqList;
import tree.tools.StreamGobbler;
import tree.tools.FileUtils;
import tree.tools.MafftForGeneSet;
import tree.tools.LabelReplacer;

/*
 *  1. read jsons
 *  2. align & filtering
 *  3. tree inference
 */
public class TreeBuilder{

private String ucgDirectory = null;
private String outDirectory = null;
private String runOutDirName = null;

private String mafftPath = null;
private String raxmlPath = null;
private String fasttreePath = null;
private String iqtreePath = null;

private AlignMode alignMode = AlignMode.codon;
private int filtering = 50; // removes the column which has more than 50% gaps
private String model = null;
private int gsi_threshold = 95;
private List<String> outputLabels = null;

private ArrayList<Long> genomeList = null;// Genomes used for the analysis
private HashMap<String, String> replaceMap = null;
private ArrayList<String> targetGenes = null;
private HashSet<String> usedGenes = null;

private String treeZzFileName = null;
private String treeZzGsiFileName = null;

// output files
private String concatenatedSeqFileName = null;
private String concatenatedSeqLabelFileName = null;
private String treeLabelFileName = null;
private String treeLabelGsiFileName = null;
private String allGeneTreesFile = null;
private String logFileName = null;
private String trmFile = null;

public TreeBuilder(String ucgDirectory, String outDirectory, String runOutDirName, String mafftPath, 
        String raxmlPath, String fasttreePath, String iqtreePath,
        AlignMode alignMode, int filtering, String model, int gsi_threshold, List<String> outputLabels) {
	
	if (!ucgDirectory.endsWith(File.separator)) {
		ucgDirectory = ucgDirectory + File.separator;
	}
	this.ucgDirectory = ucgDirectory;
	
	if (!outDirectory.endsWith(File.separator)) {
		outDirectory = outDirectory + File.separator;
	}
	this.outDirectory = outDirectory;
	
	this.runOutDirName = GenericConfig.SESSION_UID + File.separator;
	if(runOutDirName!=null && !runOutDirName.equals("")) {
		if (!runOutDirName.endsWith(File.separator)) {
			runOutDirName = runOutDirName + File.separator;
		}
		this.runOutDirName = runOutDirName;
	}
	
	this.mafftPath = mafftPath;
	this.raxmlPath = raxmlPath;
	this.fasttreePath = fasttreePath;
	this.iqtreePath = iqtreePath;
	this.genomeList = new ArrayList<>();
	this.replaceMap = new HashMap<>();
	this.targetGenes = new ArrayList<>();
	this.usedGenes = new HashSet<String>();

	this.concatenatedSeqFileName = this.outDirectory + this.runOutDirName + "aligned_concatenated.zZ.fasta";
	this.concatenatedSeqLabelFileName = this.outDirectory + this.runOutDirName + "aligned_concatenated.fasta";
	this.treeZzFileName = this.outDirectory + this.runOutDirName + "concatenated." + alignMode + ".zZ.nwk";
	this.treeLabelFileName = this.outDirectory + this.runOutDirName + "concatenated" + ".nwk";
	this.allGeneTreesFile = this.outDirectory + this.runOutDirName + "all_genetrees.txt";
	this.logFileName = this.outDirectory + this.runOutDirName + runOutDirName.replace(File.separator, "") + ".log";
	this.trmFile = this.outDirectory + this.runOutDirName + runOutDirName.replace(File.separator, "") + ".trm";
	this.alignMode = alignMode;
	this.filtering = filtering;
	
	this.model = model;
	this.gsi_threshold = gsi_threshold;
	this.outputLabels = outputLabels;
}

public void jsonsToTree(int nThreads, PhylogenyTool tool) throws IOException{
	checkThirdPartyPrograms(tool);
	readJsonsToFastaFiles();
	alignGenes(nThreads); // 
	concatenateAlignedGenesRemoveGaps(); 
	inferTree(tool, nThreads); // fasttree or raxml
	inferGeneTrees(tool, nThreads);
	calculateGsi();
	replaceLabel();
	cleanFiles();
}

void checkThirdPartyPrograms(PhylogenyTool phylogenyTool) {
	testMafft(mafftPath);
	if(phylogenyTool.equals(PhylogenyTool.fasttree)) testFasttree(fasttreePath);
	if(phylogenyTool.equals(PhylogenyTool.raxml)) testRaxml(raxmlPath);
	if(phylogenyTool.equals(PhylogenyTool.iqtree)) testIqtree(iqtreePath);
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
		Prompt.talk("Running command : " + mafftPath + " -h");
		Process testMafft = new ProcessBuilder(argMafft).start();

		BufferedReader stdOut = new BufferedReader(new InputStreamReader(testMafft.getInputStream()));
		BufferedReader stdError = new BufferedReader(new InputStreamReader(testMafft.getErrorStream()));

		String s;

		while ((s = stdOut.readLine()) != null) {
			if (s.contains("MAFFT")) {
				mafft = true;
				break;
			}
		}
		while ((s = stdError.readLine()) != null) {
			if (s.contains("MAFFT")) {
				mafft = true;
				break;
			}
		}

		stdOut.close();
		stdError.close();

		try {
			testMafft.waitFor();
		} catch (InterruptedException e) {
			ExceptionHandler.handle(e);
		}

	} catch (IOException e) {
		ExceptionHandler.handle(e);
	}

	if (!mafft) {
		ExceptionHandler.pass("mafft");
		ExceptionHandler.handle(ExceptionHandler.DEPENDENCY_UNSOLVED);
	}
}
private void testFasttree(String fasttreePath) {
	// fasttree 2.1.10 SSE3
	boolean fasttree = false;

	List<String> argfasttree = new ArrayList<>();
	argfasttree.add(fasttreePath);
	argfasttree.add("-help");

	try {
		Prompt.talk("Running command : " + fasttreePath + " -help");
		Process testfasttree = new ProcessBuilder(argfasttree).start();

		BufferedReader stdOut = new BufferedReader(new InputStreamReader(testfasttree.getInputStream()));
		BufferedReader stdError = new BufferedReader(new InputStreamReader(testfasttree.getErrorStream()));
//		BufferedOutputStream stdin = new BufferedOutputStream(testfasttree.getOutputStream());

		String s;

		while ((s = stdOut.readLine()) != null) {
			if (s.contains("Common")) {
				fasttree = true;
				break;
			}
		}

		while ((s = stdError.readLine()) != null) {
			if (s.contains("Common")) {
				fasttree = true;
				break;
			}
		}

		stdOut.close();
		stdError.close();

		try {
			testfasttree.waitFor();
		} catch (InterruptedException e) {
			ExceptionHandler.handle(e);
		}

	} catch (IOException e) {
		ExceptionHandler.handle(e);
	}

	if (!fasttree) {
		ExceptionHandler.pass("fasttree");
		ExceptionHandler.handle(ExceptionHandler.DEPENDENCY_UNSOLVED);
	}
}
private void testRaxml(String raxmlPath) {
	boolean raxml = false;

	List<String> argRaxml = new ArrayList<>();
	argRaxml.add(raxmlPath);
	argRaxml.add("-h");

	try {
		Prompt.talk("Running command : " + raxmlPath + " -h");
		Process testRaxml = new ProcessBuilder(argRaxml).start();

		BufferedReader stdOut = new BufferedReader(new InputStreamReader(testRaxml.getInputStream()));
		BufferedReader stdError = new BufferedReader(new InputStreamReader(testRaxml.getErrorStream()));
//		BufferedOutputStream stdin = new BufferedOutputStream(testRaxml.getOutputStream());

		String s;

		while ((s = stdOut.readLine()) != null) {
			if (s.contains("This is RAxML")) {
				raxml = true;
				break;
			}
		}

		while ((s = stdError.readLine()) != null) {
			if (s.contains("This is RAxML")) {
				raxml = true;
				break;
			}
		}

		stdOut.close();
		stdError.close();

		try {
			testRaxml.waitFor();
		} catch (InterruptedException e) {
			ExceptionHandler.handle(e);
		}

	} catch (IOException e) {
		ExceptionHandler.handle(e);
	}

	if (!raxml) {
		ExceptionHandler.pass("RAxML");
		ExceptionHandler.handle(ExceptionHandler.DEPENDENCY_UNSOLVED);
	}
}
private void testIqtree(String fasttreePath) {
	boolean iqtree = false;

	List<String> argiqtree = new ArrayList<>();
	argiqtree.add(iqtreePath);
	argiqtree.add("-h");

	try {
		Prompt.talk("Running command : " + iqtreePath + " -h");
		Process testiqtree = new ProcessBuilder(argiqtree).start();

		BufferedReader stdOut = new BufferedReader(new InputStreamReader(testiqtree.getInputStream()));
		BufferedReader stdError = new BufferedReader(new InputStreamReader(testiqtree.getErrorStream()));
//		BufferedOutputStream stdin = new BufferedOutputStream(testfasttree.getOutputStream());

		String s;

		while ((s = stdOut.readLine()) != null) {
			if (s.contains("IQ-TREE")) {
				iqtree = true;
				break;
			}
		}
		while ((s = stdError.readLine()) != null) {
			if (s.contains("IQ-TREE")) {
				iqtree = true;
				break;
			}
		}

		stdOut.close();
		stdError.close();

		try {
			testiqtree.waitFor();
		} catch (InterruptedException e) {
			ExceptionHandler.handle(e);
		}

	} catch (IOException e) {
		ExceptionHandler.handle(e);
	}

	if (!iqtree) {
		ExceptionHandler.pass("iqtree");
		ExceptionHandler.handle(ExceptionHandler.DEPENDENCY_UNSOLVED);
	}
}

void readJsonsToFastaFiles() throws IOException{
	
	checkPathDirectory();
	
	// ucg files
	File dir = new File(ucgDirectory);

	if(!dir.exists()) {
		ExceptionHandler.pass(dir);
		ExceptionHandler.handle(ExceptionHandler.INVALID_DIRECTORY);
	}
	
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
	HashMap<String, Integer> labelCountMap = new HashMap<String, Integer>();
	HashSet<Long> uidSet = new HashSet<>();
	
	for(GeneSetByGenomeDomain domain : geneSetsDomainList) {
		long uid = domain.getUid();
		genomeList.add(uid);
		
		if(!uidSet.add(uid)) {
			ExceptionHandler.pass("Duplicated UIDs exist.");
			ExceptionHandler.handle(ExceptionHandler.ERROR_WITH_MESSAGE);
		}
		
		String label = "";
		
		if (outputLabels.contains("uid")) {
			label = label + "|" + domain.getUid();
		}
		if (outputLabels.contains("acc")) {
			label = label + "|" + domain.getAccession();
		}
		if (outputLabels.contains("label")) {
			label = label + "|" + domain.getLabel();
		}
		if (outputLabels.contains("taxon")) {
			label = label + "|" + domain.getTaxonName();
		}
		if (outputLabels.contains("strain")) {
			label = label + "|" + domain.getStrainName();
		}
		if (outputLabels.contains("taxonomy")) {
			label = label + "|" + domain.getTaxonomy();
		}
		if (outputLabels.contains("type")) {
			if(domain.getIsTpyeStrain()!=null&&domain.getIsTpyeStrain()) {
				label = label + "|TYPE";
			}
		}
		
		if (label.startsWith("|")) {
			label = label.substring(1);
		}

		if(labelCountMap.containsKey(label)){
			labelCountMap.put(label, labelCountMap.get(label)+1);
		}else{
			labelCountMap.put(label, 1);
		}
		
		if(labelCountMap.get(label)!=1){
			label = label + "_" + labelCountMap.get(label);
		}
		
		replaceMap.put(String.valueOf(uid), label);
	}
	
	writeGenomeInfoToLogFile(geneSetsDomainList);
	
	// 3. retrieve fasta files
	
	if(alignMode.equals(AlignMode.codon)||alignMode.equals(AlignMode.codon12)) {
		retrieveFastaNucProFiles(geneSetsDomainList);
	}else if(alignMode.equals(AlignMode.nucleotide)||alignMode.equals(AlignMode.protein)) {
		retrieveFastaFiles(geneSetsDomainList);
	}
}

private void checkPathDirectory() {
	
	File path = new File(outDirectory);
	
	if(!path.exists()) {
		if(!path.mkdir()) {
			ExceptionHandler.pass(outDirectory + " doesn't exist and can't be created.");
			ExceptionHandler.handle(ExceptionHandler.ERROR_WITH_MESSAGE);
		}
	}
	
	if(new File(outDirectory + runOutDirName).exists()) {
		ExceptionHandler.pass("Run id '" + runOutDirName.replace(File.separator, "") + "' already exists.");
		ExceptionHandler.handle(ExceptionHandler.ERROR_WITH_MESSAGE);
	}
	
	if(!new File(outDirectory).canWrite()) {
		ExceptionHandler.pass("Cannot write files to " + outDirectory + ". Please check the permission.");
		ExceptionHandler.handle(ExceptionHandler.ERROR_WITH_MESSAGE);
	}else {
		new File(outDirectory + runOutDirName).mkdir();
	}
	
	
}

void alignGenes(int nThreads) {
	
	int geneNum = usedGenes.size();
	
	Prompt.print("Aligning genes...");
	Prompt.talk("Total # of genes to be aligned : " + geneNum);
	
	align(nThreads);
	
	Prompt.talk("Gene alignment finished.");
	
	// convert to codon
	if(alignMode.equals(AlignMode.codon)||alignMode.equals(AlignMode.codon12)) {
		proAlignToCodon();
	}		

}


void concatenateAlignedGenesRemoveGaps() {
	
	Prompt.print("Concatenating aligned genes...");
	
	List<String> fileList = new ArrayList<String>();

	for (String gene : usedGenes) {
		fileList.add(alignedFinalGeneFastaFile(gene));
	}
	
	HashMap<String, Integer> geneLengthMap = new HashMap<String, Integer>();

	LinkedHashMap<String, FastaSeqList> geneFastaSeqListMap = new LinkedHashMap<String, FastaSeqList>();

	for (String fileName : fileList) {
		FastaSeqList fsl = new FastaSeqList();
		fsl.importFile(fileName);

		
		// check if all the sequence length are same
		Integer seqLength = null;
		for (FastaSeq fs : fsl.list) {
			
//			if(alignMode.equals(AlignMode.codon)||alignMode.equals(AlignMode.codon12)) {
//				String stopCodon = fs.sequence.substring(fs.sequence.length()-3);
//				System.out.println("stopcodon : " + stopCodon);
//				if(stopCodon.contains("X")) {
//					fs.sequence = fs.sequence.substring(0,fs.sequence.length()-3) + "TAA";
//				}else if(!stopCodon.equals("---")&&!stopCodon.equals("TAA")&&!stopCodon.equals("TAG")&&!stopCodon.equals("TGA")){
//					fs.sequence = fs.sequence + "---";
//				}	
//			
//			}
			
			if (seqLength == null) {
				seqLength = fs.sequence.length();
				
			} else {
				
				if (seqLength != fs.sequence.length()) {
					ExceptionHandler.pass("\"" + fileName + "\" has a different length of sequence.");
					ExceptionHandler.handle(ExceptionHandler.ERROR_WITH_MESSAGE);
				}
			}
		}
		geneLengthMap.put(fileName, seqLength);
		geneFastaSeqListMap.put(fileName, fsl);

	}

//	String tmpFileName = concatenatedSeqFileName + "_tmp";

	try {

//		FileWriter tmpWriter = new FileWriter(tmpFileName, true);
//		BufferedWriter tmpBufferedWriter = new BufferedWriter(tmpWriter);

		String tmpFasta = "";
		
		int count = 0;
		for (Long genomeUid : genomeList) {

			String zZgenomeUid = "zZ" + genomeUid + "zZ";

			StringBuffer concatenatedFasta = new StringBuffer();

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

//			tmpBufferedWriter.append(concatenatedFasta.toString());
			
			tmpFasta = tmpFasta + concatenatedFasta.toString();
		}

//		tmpBufferedWriter.close();
//		tmpWriter.close();



//		String tmpFasta = new String (Files.readAllBytes(Paths.get(tmpFileName)));
//		new File(tmpFileName).delete();

		String filteredFasta = removeGapColumns(tmpFasta);

		FileWriter fw = new FileWriter(concatenatedSeqFileName);
		fw.append(filteredFasta);
		fw.close();
		
	} catch (IOException e) {
		ExceptionHandler.handle(e);
	}
}


void inferTree(PhylogenyTool tool, int nThreads) {
	Prompt.print("Reconstructing the final tree...");
	
	if(tool.equals(PhylogenyTool.raxml)) runRaxml(nThreads);
	if(tool.equals(PhylogenyTool.fasttree)) runFasttree(nThreads);
	if(tool.equals(PhylogenyTool.iqtree)) runIqtree(nThreads); 
	
	Prompt.print("The final tree was written in : " + ANSIHandler.wrapper(treeLabelFileName, 'B'));
}

void inferGeneTrees(PhylogenyTool phylogenyTool, int nThreads) {
	if(phylogenyTool.equals(PhylogenyTool.fasttree)) {
		inferGeneTreesSynchronized(phylogenyTool, nThreads);
	}
	else {
		inferGeneTreesSequentially(phylogenyTool, nThreads);
	}
}
void inferGeneTreesSequentially(PhylogenyTool phylogenyTool, int nThreads) {
	Prompt.print("Reconstructing gene trees...");

	// log info - the length of genes and concatenated sequence

	StringBuffer logSB = new StringBuffer();

	File concatFile = new File(concatenatedSeqFileName);
	FastaSeqList conFsl = new FastaSeqList();
	conFsl.importFile(concatFile);

	logSB.append("//\n\n");
	logSB.append("Length of the concatenated alignment: " + conFsl.list.get(1).sequence.length());
	logSB.append("\n\n");

	logSB.append("Length of gene alignments\n");

	// file exist? -> execute and count ++
	for (String ucg : usedGenes) {

		String alignedGene = alignedFinalGeneFastaFile(ucg);

		File alignedGeneFasta = new File(alignedGene);

		if (alignedGeneFasta.exists()) {

			FastaSeqList fsl = new FastaSeqList();
			fsl.importFile(alignedGeneFasta);

			logSB.append(ucg + ": " + fsl.list.get(0).sequence.length() + "\n");
			
		}
	}

	try {
		FileWriter logFW = new FileWriter(logFileName, true);
		logFW.append(logSB);
		logFW.flush();
		logFW.close();
	} catch (IOException e) {
		ExceptionHandler.handle(e);
	}
	
	Prompt.talk("Total number of gene trees to be reconstructed : " + usedGenes.size());
	
	int counter = 0;
	for(String ucg : usedGenes) {
		if(phylogenyTool.equals(PhylogenyTool.raxml)) {
			runGeneRaxml(alignedFinalGeneFastaFile(ucg), runOutDirName.replace(File.separator, ""), raxmlPath, nThreads, ucg, alignMode, outDirectory, model);
		}
		else if(phylogenyTool.equals(PhylogenyTool.fasttree)) {
			runGeneFasttree(alignedFinalGeneFastaFile(ucg), runOutDirName.replace(File.separator, ""), fasttreePath, nThreads, ucg, alignMode, outDirectory, model);
		}
		else if(phylogenyTool.equals(PhylogenyTool.iqtree)) {
			runGeneIqtree(alignedFinalGeneFastaFile(ucg), runOutDirName.replace(File.separator, ""), iqtreePath, nThreads, ucg, alignMode, outDirectory, model);
		}
		
		String msg = "'" + ucg + "' tree was reconstructed. (" + (++counter) + " / " + usedGenes.size() + ")";
		Prompt.talk(msg);
	}

	Prompt.talk("Gene tree reconstruction finished.");

	// merge gene trees
	StringBuffer mergedTrees = new StringBuffer();

	for (String ucg : usedGenes) {
		String geneTree = outDirectory + runOutDirName + ucg + ".zZ.nwk";

		try {
			File treeFile = new File(geneTree);
			FileReader treeReader = new FileReader(treeFile);
			BufferedReader br = new BufferedReader(treeReader);

			mergedTrees.append(br.readLine()).append("\n");

			br.close();
			treeReader.close();

		} catch (IOException e) {
			ExceptionHandler.handle(e);
		}

	}

	try {
		FileWriter mergeWriter = new FileWriter(allGeneTreesFile);

		mergeWriter.append(mergedTrees);

		mergeWriter.close();

	} catch (IOException e) {
		ExceptionHandler.handle(e);
	}
}
void inferGeneTreesSynchronized(PhylogenyTool phylogenyTool, int nThreads) {
	
	Prompt.print("Reconstructing gene trees...");

	// log info - the length of genes and concatenated sequence

	StringBuffer logSB = new StringBuffer();

	File concatFile = new File(concatenatedSeqFileName);
	FastaSeqList conFsl = new FastaSeqList();
	conFsl.importFile(concatFile);

	logSB.append("//\n\n");
	logSB.append("Length of the concatenated alignment: " + conFsl.list.get(1).sequence.length());
	logSB.append("\n\n");

	logSB.append("Length of gene alignments\n");

	int[] counterTree = { 0 };

	// file exist? -> execute and count ++
	for (String ucg : usedGenes) {

		String alignedGene = alignedFinalGeneFastaFile(ucg);

		File alignedGeneFasta = new File(alignedGene);

		if (alignedGeneFasta.exists()) {

			FastaSeqList fsl = new FastaSeqList();
			fsl.importFile(alignedGeneFasta);

			logSB.append(ucg + ": " + fsl.list.get(0).sequence.length() + "\n");
			
		}
	}

	try {
		FileWriter logFW = new FileWriter(logFileName, true);
		logFW.append(logSB);
		logFW.flush();
		logFW.close();
	} catch (IOException e) {
		ExceptionHandler.handle(e);
	}

	int numOfGenes = usedGenes.size();
	
	Prompt.talk("Total number of gene trees to be reconstructed : " + numOfGenes);

	ExecutorService exeServiceTree = Executors.newFixedThreadPool(nThreads);

	List<Future<ProcessGobbler>> futures = new ArrayList<>();
	
	// infer gene trees
	if (phylogenyTool.equals(PhylogenyTool.fasttree)){
		for (String ucg : usedGenes) {
			Future<ProcessGobbler> f = exeServiceTree.submit(new multipleFastTree(alignedFinalGeneFastaFile(ucg), runOutDirName.replace(File.separator, ""), fasttreePath, counterTree, ucg, alignMode,
					numOfGenes, outDirectory, model));
			futures.add(f);
		}
	}
	
	exeServiceTree.shutdown();

	try {
		for (Future<ProcessGobbler> f : futures) {
			ProcessGobbler processGobbler = f.get();
			if (processGobbler.getExitValue() != 0) {
				ExceptionHandler.pass(processGobbler.getLog());
				ExceptionHandler.handle(ExceptionHandler.ERROR_WITH_MESSAGE);
			}
		}
	
		while (!exeServiceTree.awaitTermination(1, TimeUnit.SECONDS)) {
		}
	} catch(InterruptedException e) {
		ExceptionHandler.handle(e);
	} catch(ExecutionException ex) {
		ExceptionHandler.handle(ex);
	}

	Prompt.talk("Gene tree reconstruction finished.");

	// merge gene trees
	StringBuffer mergedTrees = new StringBuffer();

	for (String ucg : usedGenes) {
		String geneTree = outDirectory + runOutDirName + ucg + ".zZ.nwk";

		try {
			File treeFile = new File(geneTree);
			FileReader treeReader = new FileReader(treeFile);
			BufferedReader br = new BufferedReader(treeReader);

			mergedTrees.append(br.readLine()).append("\n");

			br.close();
			treeReader.close();

		} catch (IOException e) {
			ExceptionHandler.handle(e);
		}

	}

	try {
		FileWriter mergeWriter = new FileWriter(allGeneTreesFile);

		mergeWriter.append(mergedTrees);

		mergeWriter.close();

	} catch (IOException e) {
		ExceptionHandler.handle(e);
	}
}

void calculateGsi() {
	
	Prompt.print("Calculating Gene Support Indices (GSIs) from the gene trees...");

	File ucgJsonDir = new File(ucgDirectory);
	File[] tempUcgJsonFileList = ucgJsonDir.listFiles();

	int genomeNum = 0;

	for (File jsonFile : tempUcgJsonFileList) {
		if (jsonFile.getName().endsWith(".ucg")) {
			genomeNum++;
		}
	}

	// calculate GSI
	BranchAnalysis branchAnalysis = new BranchAnalysis(new File(allGeneTreesFile));
	String tmp = branchAnalysis.markTree(
			new File(treeZzFileName),
			false, true, -1, (int) ((100 - gsi_threshold) * genomeNum / 100));

	treeZzGsiFileName = outDirectory + runOutDirName + "concatenated_gsi_" + usedGenes.size() + ".zZ.nwk";
	treeLabelGsiFileName = outDirectory + runOutDirName + "concatenated_gsi_" + usedGenes.size() + ".nwk";
	
	try {
		FileWriter stFW = new FileWriter(treeZzGsiFileName);
		BufferedWriter stBW = new BufferedWriter(stFW);

		stBW.append(tmp);

		stBW.close();
		stFW.close();

	} catch (IOException e) {
		ExceptionHandler.handle(e);
	}

	// make trm file
	JSONObject trmJson = new JSONObject();

	try {
		FileReader fr = new FileReader(treeZzFileName);
		BufferedReader br = new BufferedReader(fr);

		String ucgNwk = br.readLine();
		
		br.close();
		trmJson.put("UUCG", ucgNwk);

		for (String ucg : usedGenes) {
			FileReader geneFR = new FileReader(outDirectory + runOutDirName + ucg + ".zZ.nwk");
			BufferedReader geneBR = new BufferedReader(geneFR);

			String geneNwk = geneBR.readLine();
			
			geneBR.close();
			trmJson.put(ucg, geneNwk);
		}

		JSONArray listArray = new JSONArray();

		FileReader logFR = new FileReader(logFileName);
		BufferedReader logBR = new BufferedReader(logFR);

		String line;
		boolean list = false;
		while ((line = logBR.readLine()) != null) {
			if (line.startsWith("//")) {
				break;
			} else if (list) {
				String[] metadata = line.split("\t");

				JSONArray ar = new JSONArray();

				for (String data : metadata) {
					ar.put(data);
				}

				listArray.put(ar);

			} else if (line.startsWith("Genomes included in the analysis")) {
				list = true;
				logBR.readLine();
			}
		}
		
		logBR.close();
		trmJson.put("list", listArray);

		FileWriter trmFW = new FileWriter(trmFile);
		trmFW.append(trmJson.toString());
		trmFW.flush();
		trmFW.close();

	} catch (IOException e) {
		ExceptionHandler.handle(e);
	}
}

// Zz -> label
void replaceLabel() {
	// concatenated sequences
	LabelReplacer replacer = new LabelReplacer();
	replacer.replace_name_delete(concatenatedSeqFileName, concatenatedSeqLabelFileName, replaceMap);
	
	// default uucg tree file
	replacer.replace_name_delete(treeZzFileName,treeLabelFileName,replaceMap);
	
	// gsi uucg tree file
	replacer.replace_name_delete(treeZzGsiFileName, treeLabelGsiFileName, replaceMap);
	
	for(String ucg : usedGenes) {
		
		// gene files
		String fastaFile = fastaFileName(ucg);
		String fastaLabelFile = fastaLabelFileName(ucg);
		
		// aligned gene files
		String alignedGene = alignedFinalGeneFastaFile(ucg);
		String alignedLabelGene = alignedFinalGeneFastaLabelFile(ucg);
		
		// gene tree files
		String geneTreeFile = outDirectory + runOutDirName + ucg + ".zZ.nwk";
		String geneTreeLabelFile = outDirectory + runOutDirName + ucg + ".nwk";
		
		replacer.replace_name_delete(fastaFile, fastaLabelFile, replaceMap);
		replacer.replace_name_delete(alignedGene, alignedLabelGene, replaceMap);
		replacer.replace_name_delete(geneTreeFile, geneTreeLabelFile, replaceMap);
	}
	
	if(alignMode.equals(AlignMode.codon)||alignMode.equals(AlignMode.codon12)) {
		for(String gene : usedGenes) {
			String nucLabelFasta = outDirectory + runOutDirName + gene + "_nuc.fasta";
			replacer.replace_name_delete(fastaFileName(gene, AlignMode.nucleotide), nucLabelFasta, replaceMap);
		}
	}
	
	Prompt.print("The final tree marked with GSI was written in : " + ANSIHandler.wrapper(treeLabelGsiFileName, 'B'));
}

void cleanFiles() {
	if(alignMode.equals(AlignMode.codon)||alignMode.equals(AlignMode.codon12)) {
		for(String gene : usedGenes) {
			File alignedProFasta = new File(alignedFastaFileName(gene));
			if(alignedProFasta.exists()) {
				alignedProFasta.delete();
			}
		}
	}
	
	String[] cmd = {"/bin/bash", PathConfig.EnvironmentPath + "config/clean.sh", outDirectory + runOutDirName};
	Shell.exec(cmd);
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
	
	if(model==null) {
		if(alignMode.equals(AlignMode.protein)) {
			argTree.add("-m");
			argTree.add("PROTCATJTT");
		}else {
			argTree.add("-m");
			argTree.add("GTRCAT");
		}
	}else {
		argTree.add("-m");
		argTree.add(model);
	}
	
	argTree.add("-p");
	argTree.add(String.valueOf(new Random().nextInt(10000)));
	
	argTree.add("-f");
	argTree.add("a");
	
	argTree.add("-x");
	argTree.add(String.valueOf(new Random().nextInt(10000)));
	
	argTree.add("-N");
	argTree.add("100");
	
	
	runPhylogenyToolByProcess(argTree);

	new File("RAxML_bipartitions." + prefix).renameTo(new File(treeZzFileName));
	new File("RAxML_bestTree." + prefix).delete();
	new File("RAxML_bipartitionsBranchLabels." + prefix).delete();
	new File("RAxML_bootstrap." + prefix).delete();
	
	new File("RAxML_info." + prefix).delete();
	new File("RAxML_log." + prefix).delete();
	new File("RAxML_parsimonyTree." + prefix).delete();
	new File("RAxML_result." + prefix).delete();

}
private void runGeneRaxml(String alignedFastaFile, String run_id, String programPath, int nThreads, String ucg, AlignMode alignMode, String outputDir, String model) {
	List<String> argTree = new ArrayList<String>();

	argTree.add(programPath);


	File inputFile = new File(alignedFastaFile);
	if (!inputFile.exists()) {
		ExceptionHandler.pass("Input file '" + alignedFastaFile + "' doesn't exist to run RAxML.");
		ExceptionHandler.handle(ExceptionHandler.ERROR_WITH_MESSAGE);
	}

	argTree.add("-s");
	argTree.add(alignedFastaFile);

	argTree.add("-n");
	argTree.add(run_id + "_" + ucg);
	
	argTree.add("-T");
	argTree.add(Integer.toString(nThreads));

	if (model == null) {
		if (alignMode.equals(AlignMode.protein)) {
			argTree.add("-m");
			argTree.add("PROTCATJTT");

		} else {
			argTree.add("-m");
			argTree.add("GTRCAT");

		}
	} else {
		argTree.add("-m");
		argTree.add(model);
	}

	argTree.add("-p");
	argTree.add(String.valueOf(new Random().nextInt(10000)));
	
	argTree.add("-f");
	argTree.add("a");
	
	argTree.add("-x");
	argTree.add(String.valueOf(new Random().nextInt(10000)));	

	argTree.add("-N");
	argTree.add("100");
	
	runPhylogenyToolByProcess(argTree);

	new File("RAxML_bipartitions." + run_id + "_" + ucg).renameTo(new File(outputDir + run_id + File.separator+ ucg + ".zZ.nwk"));
	new File("RAxML_bestTree." + run_id + "_" + ucg).delete();
	new File("RAxML_bipartitionsBranchLabels." + run_id + "_" + ucg).delete();
	new File("RAxML_bootstrap." + run_id + "_" + ucg).delete();
	
	new File("RAxML_info." + run_id + "_" + ucg).delete();
	new File("RAxML_log." + run_id + "_" + ucg).delete();
	new File("RAxML_parsimonyTree." + run_id + "_" + ucg).delete();
	new File("RAxML_result." + run_id + "_" + ucg).delete();

	File reducedFile = new File(alignedFastaFile + ".reduced");

	if (reducedFile.exists()) {
		reducedFile.delete();
	}
}
private void runFasttree(int nThreads) {
	
	List<String> argTree = new ArrayList<String>();
	
	argTree.add("bash");
	argTree.add("-c");
	
	if(model==null) {
		if (alignMode.equals(AlignMode.protein)) {
			argTree.add(fasttreePath + " " + concatenatedSeqFileName + " > " + treeZzFileName);
		} else {
			argTree.add(fasttreePath + " -nt -gtr < " + concatenatedSeqFileName + " > " + treeZzFileName);
		}
	}else {
		if(alignMode.equals(AlignMode.protein)) {
			if (model.equalsIgnoreCase("JTTcat")) {
				argTree.add(fasttreePath + " " + concatenatedSeqFileName + " > " + treeZzFileName);
			} else if (model.equalsIgnoreCase("LGcat")) {
				argTree.add(fasttreePath + " -lg " + concatenatedSeqFileName + " > " + treeZzFileName);
			} else if (model.equalsIgnoreCase("WAGcat")) {
				argTree.add(fasttreePath + " -wag " + concatenatedSeqFileName + " > " + treeZzFileName);
			} else if (model.equalsIgnoreCase("JTTgamma")) {
				argTree.add(fasttreePath + " -gamma " + concatenatedSeqFileName + " > " + treeZzFileName);
			} else if (model.equalsIgnoreCase("LGgamma")) {
				argTree.add(fasttreePath + " -lg -gamma " + concatenatedSeqFileName + " > " + treeZzFileName);
			} else if (model.equalsIgnoreCase("WAGgamma")) {
				argTree.add(fasttreePath + " -wag -gamma " + concatenatedSeqFileName + " > " + treeZzFileName);
			}
		}else {
			if (model.equalsIgnoreCase("JCcat")) {
				argTree.add(fasttreePath + " -nt " + concatenatedSeqFileName + " > " + treeZzFileName);
			} else if (model.equalsIgnoreCase("GTRcat")) {
				argTree.add(fasttreePath + " -nt -gtr < " + concatenatedSeqFileName + " > " + treeZzFileName);
			} else if (model.equalsIgnoreCase("JCgamma")) {
				argTree.add(fasttreePath + " -nt -gamma < " + concatenatedSeqFileName + " > " + treeZzFileName);
			} else if (model.equalsIgnoreCase("GTRgamma")) {
				argTree.add(fasttreePath + " -nt -gtr -gamma < " + concatenatedSeqFileName + " > " + treeZzFileName);
			}
		}
	}
	
	
	runPhylogenyToolByProcess(argTree);
}
private void runGeneFasttree(String alignedFastaFile, String run_id, String programPath, int nThreads, String ucg, AlignMode alignMode, String outputDir, String model) {
	List<String> argTree = new ArrayList<String>();

	argTree.add("bash");
	argTree.add("-c");

	File inputFile = new File(alignedFastaFile);
	if (!inputFile.exists()) {
		ExceptionHandler.pass("Input file '" + alignedFastaFile + "' doesn't exist to run FastTree.");
		ExceptionHandler.handle(ExceptionHandler.ERROR_WITH_MESSAGE);
	}

	if (model == null) {
		if (alignMode.equals(AlignMode.protein)) {
			argTree.add(programPath + " " + alignedFastaFile + " > " + outputDir
					+ run_id + File.separator + ucg + ".zZ.nwk");

		} else {
			argTree.add(programPath + " -nt -gtr < " + alignedFastaFile + " > "
					+ outputDir + run_id + File.separator + ucg + ".zZ.nwk");

		}
	} else {
		if (alignMode.equals(AlignMode.protein)) {
			if (model.equalsIgnoreCase("JTTcat")) {
				argTree.add(programPath + " " + alignedFastaFile + " > "
						+ outputDir + run_id + File.separator + ucg + ".zZ.nwk");
			} else if (model.equalsIgnoreCase("LGcat")) {
				argTree.add(programPath + " -lg " + alignedFastaFile + " > "
						+ outputDir + run_id + File.separator + ucg + ".zZ.nwk");
			} else if (model.equalsIgnoreCase("WAGcat")) {
				argTree.add(programPath + " -wag " + alignedFastaFile + " > "
						+ outputDir + run_id + File.separator + ucg + ".zZ.nwk");
			} else if (model.equalsIgnoreCase("JTTgamma")) {
				argTree.add(programPath + " -gamma " + alignedFastaFile + " > "
						+ outputDir + run_id + File.separator + ucg + ".zZ.nwk");
			} else if (model.equalsIgnoreCase("LGgamma")) {
				argTree.add(programPath + " -lg -gamma " + alignedFastaFile + " > "
						+ outputDir + run_id + File.separator + ucg + ".zZ.nwk");
			} else if (model.equalsIgnoreCase("WAGgamma")) {
				argTree.add(programPath + " -wag -gamma " + alignedFastaFile + " > "
						+ outputDir + run_id + File.separator + ucg + ".zZ.nwk");
			}

		} else {
			if (model.equalsIgnoreCase("JCcat")) {
				argTree.add(programPath + " -nt " + alignedFastaFile + " > "
						+ outputDir + run_id + File.separator + ucg + ".zZ.nwk");
			} else if (model.equalsIgnoreCase("GTRcat")) {
				argTree.add(programPath + " -nt -gtr < " + alignedFastaFile + " > "
						+ outputDir + run_id + File.separator + ucg + ".zZ.nwk");
			} else if (model.equalsIgnoreCase("JCgamma")) {
				argTree.add(programPath + " -nt -gamma < " + alignedFastaFile + " > "
						+ outputDir + run_id + File.separator + ucg + ".zZ.nwk");
			} else if (model.equalsIgnoreCase("GTRgamma")) {
				argTree.add(programPath + " -nt -gtr -gamma < " + alignedFastaFile + " > "
						+ outputDir + run_id + File.separator + ucg + ".zZ.nwk");
			}

		}

	}
	
	runPhylogenyToolByProcess(argTree);
}
private void runIqtree(int nThreads) {
	List<String> argTree = new ArrayList<String>();
	
	argTree.add(iqtreePath);
	argTree.add("-s");
	argTree.add(concatenatedSeqFileName);
	argTree.add("-T");
	argTree.add(String.valueOf(nThreads));
	argTree.add("--quiet");
	
	argTree.add("-m");
	if(model==null) {
		if(alignMode.equals(AlignMode.protein)) argTree.add("JTT+F+I+G");
		else argTree.add("GTR+F+I+G");
	}
	else argTree.add(model);
	
	runPhylogenyToolByProcess(argTree);
	
	new File(concatenatedSeqFileName + ".treefile").renameTo(new File(treeZzFileName));	
	new File(concatenatedSeqFileName + ".iqtree").delete();
	new File(concatenatedSeqFileName + ".bionj").delete();
	new File(concatenatedSeqFileName + ".log").delete();
	new File(concatenatedSeqFileName + ".mldist").delete();
	new File(concatenatedSeqFileName + ".ckp.gz").delete();
}
private void runGeneIqtree(String alignedFastaFile, String run_id, String programPath, int nThreads, String ucg, AlignMode alignMode, String outputDir, String model) {
	List<String> argTree = new ArrayList<String>();

	argTree.add(programPath);


	File inputFile = new File(alignedFastaFile);
	if (!inputFile.exists()) {
		ExceptionHandler.pass("Input file '" + alignedFastaFile + "' doesn't exist to run IQ-TREE.");
		ExceptionHandler.handle(ExceptionHandler.ERROR_WITH_MESSAGE);
	}

	argTree.add("-s");
	argTree.add(alignedFastaFile);
	argTree.add("-T");
	argTree.add(String.valueOf(nThreads));
	argTree.add("--quiet");
	
	argTree.add("-m");
	if (model == null) {
		if (alignMode.equals(AlignMode.protein)) argTree.add("JTT+F+I+G");
		else argTree.add("GTR+F+I+G");
	} else argTree.add(model);
	
	runPhylogenyToolByProcess(argTree);
	
	new File(alignedFastaFile + ".treefile").renameTo(new File(outputDir + run_id + File.separator+ ucg + ".zZ.nwk"));
	new File(alignedFastaFile + ".iqtree").delete();
	new File(alignedFastaFile + ".bionj").delete();
	new File(alignedFastaFile + ".log").delete();
	new File(alignedFastaFile + ".mldist").delete();
	new File(alignedFastaFile + ".ckp.gz").delete();
}


private void runPhylogenyToolByProcess(List<String> argTree) {
	try {

		Prompt.debug("Running command : " + ANSIHandler.wrapper(String.join(" ", argTree), 'B'));

		Process tree = new ProcessBuilder(argTree).start();

		StreamGobbler outGobbler = new StreamGobbler(tree.getInputStream(), null, false);
		StreamGobbler errorGobbler = new StreamGobbler(tree.getErrorStream(), null, false);

		outGobbler.start();
		errorGobbler.start();

		try {
			tree.waitFor();
		} catch (InterruptedException e) {
			ExceptionHandler.handle(e);
		}

		if (tree.exitValue() != 0) {
			String log = errorGobbler.LogMessage();
			ExceptionHandler.pass(log);
			ExceptionHandler.handle(ExceptionHandler.ERROR_WITH_MESSAGE);
		}
	} catch (IOException e) {
		ExceptionHandler.handle(e);
	}
}


private void align(int nThreads) {
	
	try{
		ExecutorService exeService = Executors.newFixedThreadPool(nThreads);
	
		int counter[] = {0};
		
		List<Future<ProcessGobbler>> futures = new ArrayList<>();
		for(String gene : usedGenes){
			
			String fastaFile = fastaFileName(gene);
			String alignedFastaFile = alignedFastaFileName(gene);
			
			Future<ProcessGobbler> future = exeService.submit(new multipleAlign(mafftPath, alignMode, fastaFile, alignedFastaFile, counter, gene, usedGenes.size(), filtering));
			futures.add(future);
		}
		
		exeService.shutdown();
		
		for(Future<ProcessGobbler> f : futures) {
			ProcessGobbler processGobbler = f.get();
			if(processGobbler.getExitValue()!=0) {
				ExceptionHandler.pass(processGobbler.getLog());
				ExceptionHandler.handle(ExceptionHandler.ERROR_WITH_MESSAGE);
			}
		}
		
		while(!exeService.awaitTermination(1, TimeUnit.SECONDS)){
		}
		
	}catch(InterruptedException e){
		ExceptionHandler.handle(e);
	}catch(ExecutionException ex) {
		ExceptionHandler.handle(ex);
	}
	
}

private void proAlignToCodon() {

	Prompt.print("Converting protein alignments to DNA(codon) alignments...");
	
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
			
			String stopCodon = dnaSeq.substring(dnaSeq.length()-3);
			if(stopCodon.contains("X")) {
				dnaSeq= dnaSeq.substring(0,dnaSeq.length()-3) + "TAA";
			}else if(!stopCodon.equals("TAA")&&!stopCodon.equals("TAG")&&!stopCodon.equals("TGA")){
				dnaSeq = dnaSeq + "TAA";
			}	
			
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
				// write aligned bcgDNAfile
				nucFasta.write(alignedCodonFile(gene));
				
			}else if(alignMode.equals(AlignMode.codon12)) {
				char[] seqArray = alignedCodonSB.toString().toCharArray();
				StringBuffer codon12SB = new StringBuffer();
				
				for(int k=0;k<seqArray.length;k++){
					if(k%3!=2){
						codon12SB.append(seqArray[k]);
					}
				}
				
				nucFasta.find(alignedFastaSeq.title).sequence = codon12SB.toString();
				// write aligned bcgDNAfile
				nucFasta.write(alignedCodon12File(gene));
			}
		}	
		
		
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

					if(nucSeq==null) {
						String label = geneSetsDomain.getLabel();
						ExceptionHandler.pass(label + " has no DNA sequence! DNA sequence is needed to be aligned with their sequences.");
						ExceptionHandler.handle(ExceptionHandler.ERROR_WITH_MESSAGE);
					}
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
			Prompt.print(ANSIHandler.wrapper("WARNING", 'Y') + " : Less than 4 species have '" + gene + "'. This gene will be excluded");
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
			Prompt.print(ANSIHandler.wrapper("WARNING", 'Y') + " : Less than 4 species have '" + gene + "'. This gene will be excluded");
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
	
	Prompt.print(files.length + " ucg files are detected.");
	
	if(files.length<3) {
		ExceptionHandler.pass("Three or more .ucg profiles are required.");
		ExceptionHandler.handle(ExceptionHandler.ERROR_WITH_MESSAGE);
	}
	
	boolean same = false;
	
	File refFile = files[0];
	//System.out.println(refFile.getAbsolutePath());
	ArrayList<String> refTargetGenes = fileToTargetGeneList(refFile);
	
	targetGenes = refTargetGenes;
	
	for(File geneSetFile : files) {
		ArrayList<String> targetGenes = fileToTargetGeneList(geneSetFile);
		if(!sameTargetGenes(refTargetGenes, targetGenes)) {
			ExceptionHandler.pass("Gene sets are not identical across the profiles.");
			ExceptionHandler.handle(ExceptionHandler.ERROR_WITH_MESSAGE);
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
private String fastaLabelFileName(String gene) {
	
	String fasta = null;
	
	if(alignMode.equals(AlignMode.nucleotide)) {
		fasta = outDirectory + runOutDirName + gene + "_nuc.fasta";
	}else if(alignMode.equals(AlignMode.protein)||alignMode.equals(AlignMode.codon)||alignMode.equals(AlignMode.codon12)) {
		fasta = outDirectory + runOutDirName + gene + "_pro.fasta";
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
private String alignedFinalGeneFastaLabelFile(String gene) {
	
	String fasta = outDirectory + runOutDirName + "aligned_" + gene + ".fasta";
	
//	if(alignMode.equals(AlignMode.nucleotide)) {
//		fasta = outDirectory + runOutDirName + "aligned_" + gene + "_nuc.fasta";
//	}else if(alignMode.equals(AlignMode.protein)) {
//		fasta = outDirectory + runOutDirName + "aligned_" + gene + "_pro.fasta";
//	}else if(alignMode.equals(AlignMode.codon)) {
//		fasta = outDirectory + runOutDirName + "aligned_" + gene + "_codon.fasta";
//	}else if(alignMode.equals(AlignMode.codon12)) {
//		fasta = outDirectory + runOutDirName + "aligned_" + gene + "_codon12.label.fasta";
//	}
		
	return fasta;
}



private void writeGenomeInfoToLogFile(List<GeneSetByGenomeDomain> geneSetByGenomeDomains) {
	
	StringBuffer logSB = new StringBuffer();
	
	// info of the run
	logSB.append("Run ID: " + runOutDirName.replace(File.separator, "") + "\n");
	logSB.append("Genomes produced: " + geneSetByGenomeDomains.size() + "\n");
	logSB.append("Alignment mode: " + alignMode + "\n");
	logSB.append("Filtered by: " + filtering + "%\n\n");
	logSB.append("Genomes included in the analysis\n");
	logSB.append("uid\tlabel\tacc\ttaxon_name\tstrain_name\ttype\ttaxonomy\tUUCG\n");
	
	for(GeneSetByGenomeDomain dom : geneSetByGenomeDomains) {
		long uid = dom.getUid();
		String label = dom.getLabel();
		String acc = dom.getAccession();
		String taxon_name = dom.getTaxonName();
		String strain_name = dom.getStrainName();
		Boolean type = dom.getIsTpyeStrain();
		String taxonomy = dom.getTaxonomy();
		int numDetectedGenes = dom.getTotalDetectedGenes();
		
		logSB.append(uid + "\t" + label + "\t" + acc + "\t" + taxon_name + "\t" + strain_name + "\t" + type + "\t" + taxonomy + "\t"+ numDetectedGenes + "\n");
	}
	
	try {
		FileWriter fw = new FileWriter(logFileName, true);
		fw.append(logSB);
		fw.close();
	}catch(IOException e) {
		ExceptionHandler.handle(e);
	}
}
}

class multipleAlign implements Callable<ProcessGobbler>{

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
	Prompt.talk(msg);
}


public ProcessGobbler call() throws IOException, InterruptedException{
	
	MafftForGeneSet mw = new MafftForGeneSet(mafftPath, alignMode);
	if(alignMode.equals(AlignMode.codon)||alignMode.equals(AlignMode.codon12)){
		mw.setInputOutput(fastaFile, alignedFastaFile);
	}else{
		mw.setInputOutput(fastaFile, alignedFastaFile);
	}
	
	Prompt.debug("Running command : " + ANSIHandler.wrapper(mw.toString(), 'B'));
	ProcessGobbler processGobbler = mw.execute();
	
	// finished
	updateCounter(gene, counter, geneNum);

	return processGobbler;
}
}

//used to infer gene trees using multi core
class multipleFastTree implements Callable<ProcessGobbler> {
String alignedFastaFile;
String run_id;
String programPath;
int[] counter;
String ucg;
AlignMode alignMode;
int ucgNum;
String outputDir;
String model;

public multipleFastTree(String alignedFastaFile, String run_id, String programPath, int[] counter, String ucg, AlignMode alignMode,
		 int ucgNum, String outputDir, String model) {
	this.alignedFastaFile = alignedFastaFile;
	this.run_id = run_id;
	this.programPath = programPath;
	this.counter = counter;
	this.ucg = ucg;
	this.alignMode = alignMode;
	this.ucgNum = ucgNum;
	this.outputDir = outputDir;
	this.model = model;
}

public static synchronized void updateCounter(String ucg, int[] counter, int ucgNum) {
	counter[0]++;
	String msg = "'" + ucg + "' tree was reconstructed. (" + counter[0] + " / " + ucgNum + ")";
	Prompt.talk(msg);
}

public ProcessGobbler call() throws IOException{
	List<String> argTree = new ArrayList<String>();

	argTree.add("bash");
	argTree.add("-c");

	File inputFile = new File(alignedFastaFile);
	if (!inputFile.exists()) {
		ProcessGobbler processGobbler = new ProcessGobbler(1, "Error : Input file '" + alignedFastaFile + "' doesn't exist to run FastTree.");
		return processGobbler;
	}

	if (model == null) {
		if (alignMode.equals(AlignMode.protein)) {
			argTree.add(programPath + " " + alignedFastaFile + " > " + outputDir
					+ run_id + File.separator + ucg + ".zZ.nwk");

		} else {
			argTree.add(programPath + " -nt -gtr < " + alignedFastaFile + " > "
					+ outputDir + run_id + File.separator + ucg + ".zZ.nwk");

		}
	} else {
		if (alignMode.equals(AlignMode.protein)) {
			if (model.equalsIgnoreCase("JTTcat")) {
				argTree.add(programPath + " " + alignedFastaFile + " > "
						+ outputDir + run_id + File.separator + ucg + ".zZ.nwk");
			} else if (model.equalsIgnoreCase("LGcat")) {
				argTree.add(programPath + " -lg " + alignedFastaFile + " > "
						+ outputDir + run_id + File.separator + ucg + ".zZ.nwk");
			} else if (model.equalsIgnoreCase("WAGcat")) {
				argTree.add(programPath + " -wag " + alignedFastaFile + " > "
						+ outputDir + run_id + File.separator + ucg + ".zZ.nwk");
			} else if (model.equalsIgnoreCase("JTTgamma")) {
				argTree.add(programPath + " -gamma " + alignedFastaFile + " > "
						+ outputDir + run_id + File.separator + ucg + ".zZ.nwk");
			} else if (model.equalsIgnoreCase("LGgamma")) {
				argTree.add(programPath + " -lg -gamma " + alignedFastaFile + " > "
						+ outputDir + run_id + File.separator + ucg + ".zZ.nwk");
			} else if (model.equalsIgnoreCase("WAGgamma")) {
				argTree.add(programPath + " -wag -gamma " + alignedFastaFile + " > "
						+ outputDir + run_id + File.separator + ucg + ".zZ.nwk");
			}

		} else {
			if (model.equalsIgnoreCase("JCcat")) {
				argTree.add(programPath + " -nt " + alignedFastaFile + " > "
						+ outputDir + run_id + File.separator + ucg + ".zZ.nwk");
			} else if (model.equalsIgnoreCase("GTRcat")) {
				argTree.add(programPath + " -nt -gtr < " + alignedFastaFile + " > "
						+ outputDir + run_id + File.separator + ucg + ".zZ.nwk");
			} else if (model.equalsIgnoreCase("JCgamma")) {
				argTree.add(programPath + " -nt -gamma < " + alignedFastaFile + " > "
						+ outputDir + run_id + File.separator + ucg + ".zZ.nwk");
			} else if (model.equalsIgnoreCase("GTRgamma")) {
				argTree.add(programPath + " -nt -gtr -gamma < " + alignedFastaFile + " > "
						+ outputDir + run_id + File.separator + ucg + ".zZ.nwk");
			}

		}

	}
	
	Prompt.debug("Running command : " + ANSIHandler.wrapper(String.join(" ", argTree), 'B'));
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

	int exitValue = tree.exitValue();
	String errorLog = errorGobbler.LogMessage();
	
	// finished
	updateCounter(ucg, counter, ucgNum);

	ProcessGobbler processGobbler = new ProcessGobbler(exitValue, errorLog);
	return processGobbler;
}

}

/*
//for RAxML
class multipleRaxml implements Callable<ProcessGobbler> {
String alignedFastaFile;
String run_id;
String programPath;
int[] counter;
String ucg;
AlignMode alignMode;
int ucgNum;
String outputDir;
String model;

public multipleRaxml(String alignedFastaFile, String run_id, String programPath, int[] counter, String ucg,
	 AlignMode alignMode, int ucgNum, String outputDir, String model) {
	this.alignedFastaFile = alignedFastaFile;
	this.run_id = run_id;
	this.programPath = programPath;
	this.counter = counter;
	this.ucg = ucg;
	this.alignMode = alignMode;
	this.ucgNum = ucgNum;
	this.outputDir = outputDir;
	this.model = model;
}

public static synchronized void updateCounter(String ucg, int[] counter, int ucgNum) {
	counter[0]++;
	String msg = "'" + ucg + "' tree was reconstructed. (" + counter[0] + " / " + ucgNum + ")";
	Prompt.talk(msg);
}

public ProcessGobbler call() throws IOException{
	List<String> argTree = new ArrayList<String>();

	argTree.add(programPath);


	File inputFile = new File(alignedFastaFile);
	if (!inputFile.exists()) {
		return new ProcessGobbler(1, "Error : Input file '" + alignedFastaFile + "' doesn't exist to run RAxML.");
	}

	argTree.add("-s");
	argTree.add(alignedFastaFile);

	argTree.add("-n");
	argTree.add(run_id + "_" + ucg);

	if (model == null) {
		if (alignMode.equals(AlignMode.protein)) {
			argTree.add("-m");
			argTree.add("PROTCATJTT");

		} else {
			argTree.add("-m");
			argTree.add("GTRCAT");

		}
	} else {
		argTree.add("-m");
		argTree.add(model);
	}

	argTree.add("-p");
	argTree.add(String.valueOf(new Random().nextInt(10000)));
	
	argTree.add("-f");
	argTree.add("a");
	
	argTree.add("-x");
	argTree.add(String.valueOf(new Random().nextInt(10000)));	

	argTree.add("-N");
	argTree.add("100");
	
	Prompt.debug("Running command : " + ANSIHandler.wrapper(String.join(" ", argTree), 'B'));
	Process tree = new ProcessBuilder(argTree).start();

	StreamGobbler outGobbler = new StreamGobbler(tree.getInputStream(), null, false);
	StreamGobbler errorGobbler = new StreamGobbler(tree.getErrorStream(), null, false);

	outGobbler.start();
	errorGobbler.start();

	try {
		tree.waitFor();
	} catch (InterruptedException e) {
		System.err.println(e.getMessage());
		System.exit(1);
	}

	int exitValue = tree.exitValue();
	String errorLog = errorGobbler.LogMessage();

	ProcessGobbler processGobbler = new ProcessGobbler(exitValue, errorLog);

	new File("RAxML_bipartitions." + run_id + "_" + ucg).renameTo(new File(outputDir + run_id + File.separator+ ucg + ".zZ.nwk"));
	new File("RAxML_bestTree." + run_id + "_" + ucg).delete();
	new File("RAxML_bipartitionsBranchLabels." + run_id + "_" + ucg).delete();
	new File("RAxML_bootstrap." + run_id + "_" + ucg).delete();
	
	new File("RAxML_info." + run_id + "_" + ucg).delete();
	new File("RAxML_log." + run_id + "_" + ucg).delete();
	new File("RAxML_parsimonyTree." + run_id + "_" + ucg).delete();
	new File("RAxML_result." + run_id + "_" + ucg).delete();

	File reducedFile = new File(alignedFastaFile + ".reduced");

	if (reducedFile.exists()) {
		reducedFile.delete();
	}

	// finished
	updateCounter(ucg, counter, ucgNum);

	return processGobbler;
}
}

class multipleIqTree implements Callable<ProcessGobbler> {
String alignedFastaFile;
String run_id;
String programPath;
int[] counter;
String ucg;
AlignMode alignMode;
int ucgNum;
String outputDir;
String model;

public multipleIqTree(String alignedFastaFile, String run_id, String programPath, int[] counter, String ucg,
	 AlignMode alignMode, int ucgNum, String outputDir, String model) {
	this.alignedFastaFile = alignedFastaFile;
	this.run_id = run_id;
	this.programPath = programPath;
	this.counter = counter;
	this.ucg = ucg;
	this.alignMode = alignMode;
	this.ucgNum = ucgNum;
	this.outputDir = outputDir;
	this.model = model;
}

public static synchronized void updateCounter(String ucg, int[] counter, int ucgNum) {
	counter[0]++;
	String msg = "'" + ucg + "' tree was reconstructed. (" + counter[0] + " / " + ucgNum + ")";
	Prompt.talk(msg);
}

public ProcessGobbler call() throws IOException{
	List<String> argTree = new ArrayList<String>();

	argTree.add(programPath);


	File inputFile = new File(alignedFastaFile);
	if (!inputFile.exists()) {
		return new ProcessGobbler(1, "Error : Input file '" + alignedFastaFile + "' doesn't exist to run IQ-TREE.");
	}

	argTree.add("-s");
	argTree.add(alignedFastaFile);
	
	argTree.add("-m");
	if (model == null) {
		if (alignMode.equals(AlignMode.protein)) argTree.add("JTT+F+I+G");
		else argTree.add("GTR+F+I+G");
	} else argTree.add(model);
	
	Prompt.debug("Running command : " + ANSIHandler.wrapper(String.join(" ", argTree), 'B'));
	Process tree = new ProcessBuilder(argTree).start();

	StreamGobbler outGobbler = new StreamGobbler(tree.getInputStream(), null, false);
	StreamGobbler errorGobbler = new StreamGobbler(tree.getErrorStream(), null, false);

	outGobbler.start();
	errorGobbler.start();

	try {
		tree.waitFor();
	} catch (InterruptedException e) {
		System.err.println(e.getMessage());
		System.exit(1);
	}

	int exitValue = tree.exitValue();
	String errorLog = errorGobbler.LogMessage();

	ProcessGobbler processGobbler = new ProcessGobbler(exitValue, errorLog);
	
	
	new File(alignedFastaFile + ".treefile").renameTo(new File(outputDir + run_id + File.separator+ ucg + ".zZ.nwk"));
	new File(alignedFastaFile + ".iqtree").delete();
	new File(alignedFastaFile + ".bionj").delete();
	new File(alignedFastaFile + ".log").delete();
	new File(alignedFastaFile + ".mldist").delete();
	new File(alignedFastaFile + ".ckp.gz").delete();

	// finished
	updateCounter(ucg, counter, ucgNum);

	return processGobbler;
}
}
*/