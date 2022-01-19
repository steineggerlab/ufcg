package module;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import tree.TreeBuilder;
import tree.tools.AlignMode;
import tree.tools.PhylogenyTool;
import tree.tools.Arguments;
import tree.tools.LabelReplacer;

public class TreeModule {

//	final String version = "1.0";
	final String[] method = { "align", "replace" };
	String[] parameters = null;
	List<String> paramList = null;
	HashMap<String, String> programPath = null;
	List<String> outputLabels = null;
	
	

	public static void main(String[] args) {
		TreeModule proc = new TreeModule();
		proc.parameters = args;
		proc.paramList = Arrays.asList(args);
		
		proc.getProgramPath();

		/*
		 * METHOD - align, replace
		 */
		if (proc.paramList.contains("-h") || proc.paramList.contains("--help")) {
			proc.printHelpMessage();
			System.exit(1);
		}
		
		String method = null;
		
		try {
			method = proc.parameters[0];
		}catch(IndexOutOfBoundsException e) {
			System.err.println("Error : Enter align or replace for the first parameter. Enter -h for detailed options.");
			System.err.println("Exit.");
			System.exit(1);
		}
		
		switch (method) {
		case "align":
			proc.align();
			break;
		case "replace":
			proc.replace();
			break;
		default :
			System.err.println("Error : Enter proper parameters (align or replace)");
			System.err.println("Exit.");
			System.exit(1);
		}
	}

	private void align() {
		System.out.println("     ------------------------------------");
		System.out.println("       UFCG tree module");
		System.out.println("     ------------------------------------");
		System.out.println();
		
		String ucgDirectory = null;
		String outDirectory = "output" + File.separator;
		
		String mafftPath = programPath.get("mafft");
		String raxmlPath = programPath.get("raxml");
		String fastTreePath = programPath.get("fasttree");
		PhylogenyTool phylogenyTool = PhylogenyTool.raxml;
		
		String runOutDirName = "";
		int nThreads = 1;
		AlignMode alignMode = AlignMode.codon;
		int filtering = 50;
		String model = null;
		int gsi_threshold = 95;
		
		Arguments arg = new Arguments(parameters);
		
		ucgDirectory = arg.get("-ucg_dir");
		
		if(arg.get("-out_dir")!=null && !arg.get("-out_dir").equals("")) {
			outDirectory = arg.get("-out_dir");
		}
		if(arg.get("-run_id")!=null && !arg.get("-run_id").equals("")) {
			runOutDirName = arg.get("-run_id");
		}
		
		String align = arg.get("-a");
		if(align!=null && !align.equals("")) {
			if(align.equals("nucleotide")) {
				alignMode = AlignMode.nucleotide;
			}else if(align.equals("codon")) {
				alignMode = AlignMode.codon;
			}else if(align.equals("codon12")) {
				alignMode = AlignMode.codon12;
			}else if(align.equals("protein")) {
				alignMode = AlignMode.protein;
			}else {
				System.err.println("Error!");
				System.err.println("Invalid align mode.");
				System.err.println("Exit!");
				System.exit(1);
			}
		}
		
		try {
			if (arg.get("-t")!=null) {
				
				nThreads = Integer.parseInt(arg.get("-t"));

				if (nThreads < 1) {
					System.exit(1);
				}
			}

			if (arg.get("-f")!=null) {
				filtering = Integer.parseInt(arg.get("-f"));
			}
			
		}catch(NumberFormatException e) {
			System.err.println("Error occurred!");
			System.err.println(e.getMessage());
			System.err.println("Exit.");
			System.exit(1);
		}	
		
		if (arg.get("-fasttree")!=null) {
			phylogenyTool = PhylogenyTool.fasttree;
		}
		
		validateParametersAlign(phylogenyTool, alignMode);
		
		if(arg.get("-m")!=null && !arg.get("-m").equals("")) {
			model = arg.get("-m");
		}
		
		if (arg.get("-gsi_threshold") != null) {
			try {
				gsi_threshold = Integer.valueOf(arg.get("-gsi_threshold"));
			} catch (NumberFormatException e) {
				System.err.println("Error : Enter a value between 0 and 100 for the parameter -gsi_threshold.");
				System.err.println("Exit!");
				System.exit(1);
			}

		}
		if (gsi_threshold > 100 || gsi_threshold < 0) {
			System.err.println("Error : Enter a value between 0 and 100 for the parameter -gsi_threshold.");
			System.err.println("Exit!");
			System.exit(1);
		}
		
		if(parameters.length==0||outDirectory==null||ucgDirectory==null) {
			printHelpMessage();
			System.exit(1);
		}
		// labels
		outputLabels = new ArrayList<String>();
		
		String[] leafOpt = arg.get("-leaf").split(",");
		List<String> leafOptList = Arrays.asList(leafOpt);
		
		if(leafOptList.contains("uid")) {
			outputLabels.add("uid");
		}
		if(leafOptList.contains("label")) {
			outputLabels.add("label");
		}
		if(leafOptList.contains("acc")) {
			outputLabels.add("acc");
		}
		if(leafOptList.contains("taxon")) {
			outputLabels.add("taxon");
		}
		if(leafOptList.contains("strain")) {
			outputLabels.add("strain");
		}
		if(leafOptList.contains("type")) {
			outputLabels.add("type");
		}
		if(leafOptList.contains("taxonomy")) {
			outputLabels.add("taxonomy");
		}
		
		if(outputLabels.size()==0) {
			System.err.println("Error : Enter at least one labeling option");
			System.err.println("Exit!");
			System.exit(1);
		}
		
		TreeBuilder proc = new TreeBuilder(ucgDirectory, outDirectory, runOutDirName, mafftPath, raxmlPath, fastTreePath, alignMode, filtering, model, gsi_threshold, outputLabels);

		try {
			proc.jsonsToTree(nThreads, phylogenyTool);
		}catch (IOException e) {
			System.err.println("Error occurred.");
			System.err.println(e.getMessage());
			System.err.println("Exit.");
			System.exit(1);
		}
	}


	private void replace() {
		
		Arguments arg = new Arguments(parameters);

		if (parameters.length < 4) {
			System.err.println("Error : Enter proper parameters.");
			System.err.println("Exit!");
			System.exit(1);
		}
		
		validateParametersreplace();

		String trmFileName = parameters[1];
		String gene = parameters[2];

		File trmFile = new File(trmFileName);

		if (!trmFile.exists()) {
			System.err.println("Error : The file named '" + trmFile + "' doesn't exists!");
			System.exit(1);
		}

		if (!new File(System.getProperty("user.dir")).canWrite()) {
			System.err.println("Error : Cannot write a file in the current working directory.");
			System.exit(1);
		}

		try {
			
			String content = new String (Files.readAllBytes(Paths.get(trmFileName)));
			
			JSONObject jsonObject = new JSONObject(content);

			if(!jsonObject.keySet().contains(gene)) {
				System.err.println("Error : No " + gene + " tree in the trm file.");
				System.err.println("Exit!");
				System.exit(1);
			}
			if(jsonObject.get(gene)==null||jsonObject.get(gene).equals("")) {
				System.err.println("Error : No " + gene + " tree in the trm file.");
				System.err.println("Exit!");
				System.exit(1);
			}
			
			String nwk = (String) jsonObject.get(gene);

			if (nwk == null) {
				System.err.println("The " + gene + " tree doesn't exist!");
				System.exit(1);
			}

			HashMap<String, String> replaceMap = new HashMap<String, String>();
			HashMap<String, Integer> checkLabelName = new HashMap<String, Integer>();

			JSONArray labelLists = (JSONArray) jsonObject.get("list");

			for (int i = 0; i < labelLists.length(); i++) {
				JSONArray labelList = (JSONArray) labelLists.get(i);

				String uid = (String) labelList.get(0);
				String label = (String) labelList.get(1);
				String acc = (String) labelList.get(2);
				String taxon_name = (String) labelList.get(3);
				String strain_name = (String) labelList.get(4);
				String type = (String) labelList.get(5);
				String taxonomy = (String) labelList.get(6);

				String replacedLabel = "";

				if (arg.get("-uid") != null) {
					replacedLabel = replacedLabel + "|" + uid;
				}
				if (arg.get("-acc") != null) {
					replacedLabel = replacedLabel + "|" + acc;
				}
				if (arg.get("-label") != null) {
					replacedLabel = replacedLabel + "|" + label;
				}
				if (arg.get("-taxon") != null) {
					replacedLabel = replacedLabel + "|" + taxon_name;
				}
				if (arg.get("-taxonomy") != null) {
					replacedLabel = replacedLabel + "|" + taxonomy;
				}
				if (arg.get("-strain") != null) {
					replacedLabel = replacedLabel + "|" + strain_name;
				}
				if (arg.get("-type") != null) {
					if (type.equals("true")) {
						replacedLabel = replacedLabel + "|type" ;
					}
				}

				if (replacedLabel.startsWith("|")) {
					replacedLabel = replacedLabel.substring(1);
				}

				if (checkLabelName.containsKey(replacedLabel)) {
					checkLabelName.put(replacedLabel, checkLabelName.get(replacedLabel) + 1);
				} else {
					checkLabelName.put(replacedLabel, 1);
				}

				if (checkLabelName.get(replacedLabel) != 1) {
					replacedLabel = replacedLabel + "_" + checkLabelName.get(replacedLabel);
				}

				replaceMap.put(uid, replacedLabel);

			}

			String treeFileName = "replaced." + gene + ".nwk";

			FileWriter treeFW = new FileWriter(treeFileName);
			treeFW.append(nwk);
			treeFW.flush();
			treeFW.close();

			LabelReplacer lr = new LabelReplacer();
			lr.replace_name(treeFileName, treeFileName, replaceMap);

			System.out.println("The tree file '" + treeFileName + "' with replaced labels was written.");

		} catch (IOException e) {
			System.err.println("Error occurred!");
			System.err.println(e.getMessage());
			System.err.println("Exit!");
			System.exit(1);
		}
	}

	private void printHelpMessage() {
		System.out.println();
		System.out.println("     -----------------------------------");
		System.out.println("       UFCG tree module");
		System.out.println("     -----------------------------------");
		System.out.println();
		System.out.println("This is a part of pipeline for phylogenomics using extracted core genes (using UUCGp or UUCGf)");
		System.out.println("If you want more information, please visit www.leb.snu.ac.kr/uucg");
		System.out.println();
		System.out.println("The external programs that are used in the UUCG should be installed");
		System.out.println("Paths of the programs should be written in 'programPath' file");
		System.out.println();
		System.out.println("There are two options for the first parameter. align and replace");
		System.out.println("  align   : align and concatenate core genes. And remove gap-rich columns in the alignment");
		System.out.println("            and infere a phylogenetic tree from the concatenated sequence");
		System.out.println("  replace : replace the names of the tree leaves to any format using metadata such as taxonomy, accession etc.");
		System.out.println("            (metadata of genomes should be entered when extract the core genes)");
		System.out.println();
		System.out.println("<---------------------------------------align--------------------------------------->");
		System.out.println("Aligning each UCG and concatenating them and inferring a phylogeny");
		System.out.println("The external program used for phylogeny reconstruction can be FastTree or RAxML");
		System.out.println("ex) java -jar UUCGtree.jar align -ucg_dir [directory] -leaf uid,label");
		System.out.println();
		System.out.println("Mandatory");
		System.out.println("    -ucg_dir    <String> : directory of .ucg files that you want to align sequences and infer the tree");
		System.out.println("    -leaf       <String> : labeling selected metadata for leaves' name in output tree/sequences files");
		System.out.println("                           choose at least one from following options (their metadata must be included in .ucg files)");
		System.out.println("                           'uid', 'acc', 'label', 'taxon', 'strain', 'type', 'taxonomy'");
		System.out.println("                           ex) -leaf uid : include only id in leaves' name");
		System.out.println("                           ex) -leaf acc,label,taxon : include accession, label, taxon_name in leaves' name");
		System.out.println("                           ex) -leaf uid,acc,label,taxon,strain,type,taxonomy : include all of the metadata in leaves' name");
		System.out.println();
		System.out.println("Optional");
		System.out.println("    -out_dir    <String> : directory for saving the output directory (Default : 'output')");
		System.out.println("    -run_id     <String> : run identifier. all of the output files are saved in a directory named as run_id");
		System.out.println("    -a          <String> : alignment sequence type (Default : codon");
		System.out.println("                           nucleotide - use nucleotide sequences");
		System.out.println("                           codon      - use nucleotide sequences that are aligned based on amino acid alignments");
		System.out.println("                           codon12    - use only 1st & 2nd positions of codon");
		System.out.println("                           protein    - use amino acid sequences");
		System.out.println("    -t          <Integer>: use multi-threads (Default : 1)");
		System.out.println("    -f          <Integer>: remove(filter) gap-rich columns in the alignment");
		System.out.println("                           enter a value between 1~100");
		System.out.println("                           (Default: 50)");
		System.out.println("                           ex) 30 : select the positions that have bases of 30% or more ");
		System.out.println("                                   (same as remove the positions composed of more than 70% gap characters)");
		System.out.println("    -fasttree            : use FastTree for phylogeny reconstruction (Default : use RAxML)");
		System.out.println("    -m         <String>  : A model used to infer trees");
		System.out.println("                          (Default : JTT+CAT for a protein alignment / GTR+CAT for a nucleotide alignment)");
		System.out.println("                             --Models (See RAxML or FastTree manual for the detailed information)");
		System.out.println("                       For RAxML - NUCLEOTIDE sequences");
		System.out.println("                                   GTRCAT[X], GTRCATI[X], ASC_GTRCAT[X],");
		System.out.println("                                   GTRGAMMA[X], ASC_GTRGAMMA[X], GTRGAMMAI[X]");
		System.out.println("                                 - AMINO ACID sequences");
		System.out.println("                                   PROTCATmatrixName[F|X], PROTCATImatrixName[F|X],");
		System.out.println("                                   ASC_PROTCATmatrixName[F|X], PROTGAMMAmatrixName[F|X],");
		System.out.println("                                   ASC_PROTGAMMAmatrixName[F|X], PROTGAMMAImatrixName[F|X]");
		System.out.println("                         Available aa matrixName: DAYHOFF, DCMUT, JTT, MTREV, WAG, RTREV, CPREV, VT, \n"
						 + "                                                  BLOSUM62, MTMAM, LG, MTART, MTZOA, PMB, HIVB, HIVW, \n"
						 + "                                                  JTTDCMUT, FLU, STMTREV, DUMMY, DUMMY2, AUTO, LG4M, \n"
						 + "                                                  LG4X, PROT_FILE, GTR_UNLINKED, GTR");
		System.out.println("                         (optional appendix \"F\": Use empirical base frequencies)");
		System.out.println("                         (optional appendix \"X\": Use a ML estimate of base frequencies)");
		System.out.println("                    For FastTree - NUCLEOTIDE sequences");
		System.out.println("                                   JCcat, GTRcat, JCgamma, GTRgamma");
		System.out.println("                                 - AMINO ACID sequences");
		System.out.println("                                   JTTcat, LGcat, WAGcat, JTTgamma, LGgamma, WAGgamma");
		System.out.println("    -gsi_threshold <Integer>: The threshold used for GSI calculations (1~100)");
		System.out.println("                              Even if the exact bipartition doesn't exist in a gene tree, ");
		System.out.println("                              it is regarded as a supported bipartition based on their similiarity.");
		System.out.println("                              It is when the number of genomes more than specified threshold (percentage)");
		System.out.println("                              of all genomes (leaves) support the topology of bipartition.");
		System.out.println("                              A value of 95 or higher is recommended.");
		System.out.println("                              (Default: 95)");
		System.out.println();
		System.out.println("<--------------------------------------replace-------------------------------------->");
		System.out.println("Getting a gene tree or UUCG tree labelled with replaced names using metadata");
		System.out.println("ex) java -jar UUCGtree.jar replace [trm file] UUCG -taxon -strain");
		System.out.println("  Making a UUCG tree file that uses both taxon_name and strain_name as labels");
		System.out.println("ex) java -jar UBCGtree.jar replace [trm file] rpoB -acc -taxon_name -type");
		System.out.println("  Making a rpoB tree file that uses accession, taxon_name and strain_name as labels");
		System.out.println();
		System.out.println("Mandatory");
		System.out.println("<run_id.trm> : A file containing nwk trees and metadata of genome sequences");
		System.out.println("               This file is automatically generated in align step");
		System.out.println("      <gene> : a gene tree or UUCG tree you want to make ");
		System.out.println();
		System.out.println("Optional");
		System.out.println("       -uid  : add uids");
		System.out.println("       -acc  : add accessions");
		System.out.println("     -label  : add labels");
		System.out.println("     -taxon  : add taxon_names");
		System.out.println("    -strain  : add strain_names");
		System.out.println("      -type  : add type_info");
		System.out.println("   -taxonomy : add taxonomy");
		System.out.println();
	}

	private void validateParametersAlign(PhylogenyTool phylogenyTool, AlignMode alignMode) {
		
		final String[] validatedOptions = {"-ucg_dir", "-out_dir", "-run_id", "-a", "-t", "-f", "-fasttree", "-m", "-gsi_threshold", "-leaf"};
		final String[] validatedLeaf = {"uid", "acc", "label", "taxon", "strain", "type", "taxonomy"};
		List<String> validatedOptionList = Arrays.asList(validatedOptions);
		List<String> validatedLeafOptionList = Arrays.asList(validatedLeaf);
		
		Arguments arg = new Arguments(parameters);
		
		for(String param : paramList) {
			if(param.startsWith("-")&& !validatedOptionList.contains(param)) {
				System.err.println("Error!");
				System.err.println("Invalid option " + param);
				System.err.println("Exit!");
				System.exit(1);
			}
		}
		
		// mandatory options
		if(arg.get("-ucg_dir")==null || arg.get("-ucg_dir").equals("")) {
			System.err.println("Error : Enter proper -ucg_dir option.");
			System.err.println("Exit.");
			System.exit(1);
		}
		if(arg.get("-leaf")==null || arg.get("-leaf").equals("")) {
			System.err.println("Error : Enter proper -leaf option.");
			System.err.println("Exit.");
			System.exit(1);
		}
		
		if(!new File(arg.get("-ucg_dir")).exists()) {
			System.err.println("Error : Ucg directory doesn't exist! Enter proper -ucg_dir option.");
			System.err.println("Exit.");
			System.exit(1);
		}
		
		String[] leafOptions = arg.get("-leaf").split(",");
		
		for(String opt : leafOptions) {
			if(!validatedLeafOptionList.contains(opt)) {
				System.err.println("Error : Invalid option for the -leaf");
				System.err.println("Exit!");
				System.exit(1);
			}
		}
		
		String model = null;
		if(arg.get("-m")!=null && !arg.get("-m").equals("")) {
			model = arg.get("-m");
		}
		if(model==null) {
			return;
		}
		
		// check models
		if(alignMode.equals(AlignMode.protein)) {
			if(phylogenyTool.equals(PhylogenyTool.raxml)) {
				String[] options = { "PROTCATDAYHOFF", "PROTCATDCMUT", "PROTCATJTT", "PROTCATMTREV", "PROTCATWAG",
						"PROTCATRTREV", "PROTCATCPREV", "PROTCATVT", "PROTCATBLOSUM62", "PROTCATMTMAM", "PROTCATLG",
						"PROTCATMTART", "PROTCATMTZOA", "PROTCATPMB", "PROTCATHIVB", "PROTCATHIVW",
						"PROTCATJTTDCMUT", "PROTCATFLU", "PROTCATSTMTREV", "PROTCATDUMMY", "PROTCATDUMMY2",
						"PROTCATAUTO", "PROTCATLG4M", "PROTCATLG4X", "PROTCATPROT_FILE", "PROTCATGTR_UNLINKED",
						"PROTCATGTR", "ASC_PROTCATDAYHOFF", "ASC_PROTCATDCMUT", "ASC_PROTCATJTT",
						"ASC_PROTCATMTREV", "ASC_PROTCATWAG", "ASC_PROTCATRTREV", "ASC_PROTCATCPREV",
						"ASC_PROTCATVT", "ASC_PROTCATBLOSUM62", "ASC_PROTCATMTMAM", "ASC_PROTCATLG",
						"ASC_PROTCATMTART", "ASC_PROTCATMTZOA", "ASC_PROTCATPMB", "ASC_PROTCATHIVB",
						"ASC_PROTCATHIVW", "ASC_PROTCATJTTDCMUT", "ASC_PROTCATFLU", "ASC_PROTCATSTMTREV",
						"ASC_PROTCATDUMMY", "ASC_PROTCATDUMMY2", "ASC_PROTCATAUTO", "ASC_PROTCATLG4M",
						"ASC_PROTCATLG4X", "ASC_PROTCATPROT_FILE", "ASC_PROTCATGTR_UNLINKED", "ASC_PROTCATGTR",
						"PROTCATIDAYHOFF", "PROTCATIDCMUT", "PROTCATIJTT", "PROTCATIMTREV", "PROTCATIWAG",
						"PROTCATIRTREV", "PROTCATICPREV", "PROTCATIVT", "PROTCATIBLOSUM62", "PROTCATIMTMAM",
						"PROTCATILG", "PROTCATIMTART", "PROTCATIMTZOA", "PROTCATIPMB", "PROTCATIHIVB",
						"PROTCATIHIVW", "PROTCATIJTTDCMUT", "PROTCATIFLU", "PROTCATISTMTREV", "PROTCATIDUMMY",
						"PROTCATIDUMMY2", "PROTCATIAUTO", "PROTCATILG4M", "PROTCATILG4X", "PROTCATIPROT_FILE",
						"PROTCATIGTR_UNLINKED", "PROTCATIGTR", "PROTGAMMADAYHOFF", "PROTGAMMADCMUT", "PROTGAMMAJTT",
						"PROTGAMMAMTREV", "PROTGAMMAWAG", "PROTGAMMARTREV", "PROTGAMMACPREV", "PROTGAMMAVT",
						"PROTGAMMABLOSUM62", "PROTGAMMAMTMAM", "PROTGAMMALG", "PROTGAMMAMTART", "PROTGAMMAMTZOA",
						"PROTGAMMAPMB", "PROTGAMMAHIVB", "PROTGAMMAHIVW", "PROTGAMMAJTTDCMUT", "PROTGAMMAFLU",
						"PROTGAMMASTMTREV", "PROTGAMMADUMMY", "PROTGAMMADUMMY2", "PROTGAMMAAUTO", "PROTGAMMALG4M",
						"PROTGAMMALG4X", "PROTGAMMAPROT_FILE", "PROTGAMMAGTR_UNLINKED", "PROTGAMMAGTR",
						"ASC_PROTGAMMADAYHOFF", "ASC_PROTGAMMADCMUT", "ASC_PROTGAMMAJTT", "ASC_PROTGAMMAMTREV",
						"ASC_PROTGAMMAWAG", "ASC_PROTGAMMARTREV", "ASC_PROTGAMMACPREV", "ASC_PROTGAMMAVT",
						"ASC_PROTGAMMABLOSUM62", "ASC_PROTGAMMAMTMAM", "ASC_PROTGAMMALG", "ASC_PROTGAMMAMTART",
						"ASC_PROTGAMMAMTZOA", "ASC_PROTGAMMAPMB", "ASC_PROTGAMMAHIVB", "ASC_PROTGAMMAHIVW",
						"ASC_PROTGAMMAJTTDCMUT", "ASC_PROTGAMMAFLU", "ASC_PROTGAMMASTMTREV", "ASC_PROTGAMMADUMMY",
						"ASC_PROTGAMMADUMMY2", "ASC_PROTGAMMAAUTO", "ASC_PROTGAMMALG4M", "ASC_PROTGAMMALG4X",
						"ASC_PROTGAMMAPROT_FILE", "ASC_PROTGAMMAGTR_UNLINKED", "ASC_PROTGAMMAGTR",
						"PROTGAMMAIDAYHOFF", "PROTGAMMAIDCMUT", "PROTGAMMAIJTT", "PROTGAMMAIMTREV", "PROTGAMMAIWAG",
						"PROTGAMMAIRTREV", "PROTGAMMAICPREV", "PROTGAMMAIVT", "PROTGAMMAIBLOSUM62",
						"PROTGAMMAIMTMAM", "PROTGAMMAILG", "PROTGAMMAIMTART", "PROTGAMMAIMTZOA", "PROTGAMMAIPMB",
						"PROTGAMMAIHIVB", "PROTGAMMAIHIVW", "PROTGAMMAIJTTDCMUT", "PROTGAMMAIFLU",
						"PROTGAMMAISTMTREV", "PROTGAMMAIDUMMY", "PROTGAMMAIDUMMY2", "PROTGAMMAIAUTO",
						"PROTGAMMAILG4M", "PROTGAMMAILG4X", "PROTGAMMAIPROT_FILE", "PROTGAMMAIGTR_UNLINKED",
						"PROTGAMMAIGTR", "PROTCATDAYHOFFF", "PROTCATDCMUTF", "PROTCATJTTF", "PROTCATMTREVF",
						"PROTCATWAGF", "PROTCATRTREVF", "PROTCATCPREVF", "PROTCATVTF", "PROTCATBLOSUM62F",
						"PROTCATMTMAMF", "PROTCATLGF", "PROTCATMTARTF", "PROTCATMTZOAF", "PROTCATPMBF",
						"PROTCATHIVBF", "PROTCATHIVWF", "PROTCATJTTDCMUTF", "PROTCATFLUF", "PROTCATSTMTREVF",
						"PROTCATDUMMYF", "PROTCATDUMMY2F", "PROTCATAUTOF", "PROTCATLG4MF", "PROTCATLG4XF",
						"PROTCATPROT_FILEF", "PROTCATGTR_UNLINKEDF", "PROTCATGTRF", "ASC_PROTCATDAYHOFFF",
						"ASC_PROTCATDCMUTF", "ASC_PROTCATJTTF", "ASC_PROTCATMTREVF", "ASC_PROTCATWAGF",
						"ASC_PROTCATRTREVF", "ASC_PROTCATCPREVF", "ASC_PROTCATVTF", "ASC_PROTCATBLOSUM62F",
						"ASC_PROTCATMTMAMF", "ASC_PROTCATLGF", "ASC_PROTCATMTARTF", "ASC_PROTCATMTZOAF",
						"ASC_PROTCATPMBF", "ASC_PROTCATHIVBF", "ASC_PROTCATHIVWF", "ASC_PROTCATJTTDCMUTF",
						"ASC_PROTCATFLUF", "ASC_PROTCATSTMTREVF", "ASC_PROTCATDUMMYF", "ASC_PROTCATDUMMY2F",
						"ASC_PROTCATAUTOF", "ASC_PROTCATLG4MF", "ASC_PROTCATLG4XF", "ASC_PROTCATPROT_FILEF",
						"ASC_PROTCATGTR_UNLINKEDF", "ASC_PROTCATGTRF", "PROTCATIDAYHOFFF", "PROTCATIDCMUTF",
						"PROTCATIJTTF", "PROTCATIMTREVF", "PROTCATIWAGF", "PROTCATIRTREVF", "PROTCATICPREVF",
						"PROTCATIVTF", "PROTCATIBLOSUM62F", "PROTCATIMTMAMF", "PROTCATILGF", "PROTCATIMTARTF",
						"PROTCATIMTZOAF", "PROTCATIPMBF", "PROTCATIHIVBF", "PROTCATIHIVWF", "PROTCATIJTTDCMUTF",
						"PROTCATIFLUF", "PROTCATISTMTREVF", "PROTCATIDUMMYF", "PROTCATIDUMMY2F", "PROTCATIAUTOF",
						"PROTCATILG4MF", "PROTCATILG4XF", "PROTCATIPROT_FILEF", "PROTCATIGTR_UNLINKEDF",
						"PROTCATIGTRF", "PROTGAMMADAYHOFFF", "PROTGAMMADCMUTF", "PROTGAMMAJTTF", "PROTGAMMAMTREVF",
						"PROTGAMMAWAGF", "PROTGAMMARTREVF", "PROTGAMMACPREVF", "PROTGAMMAVTF", "PROTGAMMABLOSUM62F",
						"PROTGAMMAMTMAMF", "PROTGAMMALGF", "PROTGAMMAMTARTF", "PROTGAMMAMTZOAF", "PROTGAMMAPMBF",
						"PROTGAMMAHIVBF", "PROTGAMMAHIVWF", "PROTGAMMAJTTDCMUTF", "PROTGAMMAFLUF",
						"PROTGAMMASTMTREVF", "PROTGAMMADUMMYF", "PROTGAMMADUMMY2F", "PROTGAMMAAUTOF",
						"PROTGAMMALG4MF", "PROTGAMMALG4XF", "PROTGAMMAPROT_FILEF", "PROTGAMMAGTR_UNLINKEDF",
						"PROTGAMMAGTRF", "ASC_PROTGAMMADAYHOFFF", "ASC_PROTGAMMADCMUTF", "ASC_PROTGAMMAJTTF",
						"ASC_PROTGAMMAMTREVF", "ASC_PROTGAMMAWAGF", "ASC_PROTGAMMARTREVF", "ASC_PROTGAMMACPREVF",
						"ASC_PROTGAMMAVTF", "ASC_PROTGAMMABLOSUM62F", "ASC_PROTGAMMAMTMAMF", "ASC_PROTGAMMALGF",
						"ASC_PROTGAMMAMTARTF", "ASC_PROTGAMMAMTZOAF", "ASC_PROTGAMMAPMBF", "ASC_PROTGAMMAHIVBF",
						"ASC_PROTGAMMAHIVWF", "ASC_PROTGAMMAJTTDCMUTF", "ASC_PROTGAMMAFLUF",
						"ASC_PROTGAMMASTMTREVF", "ASC_PROTGAMMADUMMYF", "ASC_PROTGAMMADUMMY2F",
						"ASC_PROTGAMMAAUTOF", "ASC_PROTGAMMALG4MF", "ASC_PROTGAMMALG4XF", "ASC_PROTGAMMAPROT_FILEF",
						"ASC_PROTGAMMAGTR_UNLINKEDF", "ASC_PROTGAMMAGTRF", "PROTGAMMAIDAYHOFFF", "PROTGAMMAIDCMUTF",
						"PROTGAMMAIJTTF", "PROTGAMMAIMTREVF", "PROTGAMMAIWAGF", "PROTGAMMAIRTREVF",
						"PROTGAMMAICPREVF", "PROTGAMMAIVTF", "PROTGAMMAIBLOSUM62F", "PROTGAMMAIMTMAMF",
						"PROTGAMMAILGF", "PROTGAMMAIMTARTF", "PROTGAMMAIMTZOAF", "PROTGAMMAIPMBF",
						"PROTGAMMAIHIVBF", "PROTGAMMAIHIVWF", "PROTGAMMAIJTTDCMUTF", "PROTGAMMAIFLUF",
						"PROTGAMMAISTMTREVF", "PROTGAMMAIDUMMYF", "PROTGAMMAIDUMMY2F", "PROTGAMMAIAUTOF",
						"PROTGAMMAILG4MF", "PROTGAMMAILG4XF", "PROTGAMMAIPROT_FILEF", "PROTGAMMAIGTR_UNLINKEDF",
						"PROTGAMMAIGTRF", "PROTCATDAYHOFFX", "PROTCATDCMUTX", "PROTCATJTTX", "PROTCATMTREVX",
						"PROTCATWAGX", "PROTCATRTREVX", "PROTCATCPREVX", "PROTCATVTX", "PROTCATBLOSUM62X",
						"PROTCATMTMAMX", "PROTCATLGX", "PROTCATMTARTX", "PROTCATMTZOAX", "PROTCATPMBX",
						"PROTCATHIVBX", "PROTCATHIVWX", "PROTCATJTTDCMUTX", "PROTCATFLUX", "PROTCATSTMTREVX",
						"PROTCATDUMMYX", "PROTCATDUMMY2X", "PROTCATAUTOX", "PROTCATLG4MX", "PROTCATLG4XX",
						"PROTCATPROT_FILEX", "PROTCATGTR_UNLINKEDX", "PROTCATGTRX", "ASC_PROTCATDAYHOFFX",
						"ASC_PROTCATDCMUTX", "ASC_PROTCATJTTX", "ASC_PROTCATMTREVX", "ASC_PROTCATWAGX",
						"ASC_PROTCATRTREVX", "ASC_PROTCATCPREVX", "ASC_PROTCATVTX", "ASC_PROTCATBLOSUM62X",
						"ASC_PROTCATMTMAMX", "ASC_PROTCATLGX", "ASC_PROTCATMTARTX", "ASC_PROTCATMTZOAX",
						"ASC_PROTCATPMBX", "ASC_PROTCATHIVBX", "ASC_PROTCATHIVWX", "ASC_PROTCATJTTDCMUTX",
						"ASC_PROTCATFLUX", "ASC_PROTCATSTMTREVX", "ASC_PROTCATDUMMYX", "ASC_PROTCATDUMMY2X",
						"ASC_PROTCATAUTOX", "ASC_PROTCATLG4MX", "ASC_PROTCATLG4XX", "ASC_PROTCATPROT_FILEX",
						"ASC_PROTCATGTR_UNLINKEDX", "ASC_PROTCATGTRX", "PROTCATIDAYHOFFX", "PROTCATIDCMUTX",
						"PROTCATIJTTX", "PROTCATIMTREVX", "PROTCATIWAGX", "PROTCATIRTREVX", "PROTCATICPREVX",
						"PROTCATIVTX", "PROTCATIBLOSUM62X", "PROTCATIMTMAMX", "PROTCATILGX", "PROTCATIMTARTX",
						"PROTCATIMTZOAX", "PROTCATIPMBX", "PROTCATIHIVBX", "PROTCATIHIVWX", "PROTCATIJTTDCMUTX",
						"PROTCATIFLUX", "PROTCATISTMTREVX", "PROTCATIDUMMYX", "PROTCATIDUMMY2X", "PROTCATIAUTOX",
						"PROTCATILG4MX", "PROTCATILG4XX", "PROTCATIPROT_FILEX", "PROTCATIGTR_UNLINKEDX",
						"PROTCATIGTRX", "PROTGAMMADAYHOFFX", "PROTGAMMADCMUTX", "PROTGAMMAJTTX", "PROTGAMMAMTREVX",
						"PROTGAMMAWAGX", "PROTGAMMARTREVX", "PROTGAMMACPREVX", "PROTGAMMAVTX", "PROTGAMMABLOSUM62X",
						"PROTGAMMAMTMAMX", "PROTGAMMALGX", "PROTGAMMAMTARTX", "PROTGAMMAMTZOAX", "PROTGAMMAPMBX",
						"PROTGAMMAHIVBX", "PROTGAMMAHIVWX", "PROTGAMMAJTTDCMUTX", "PROTGAMMAFLUX",
						"PROTGAMMASTMTREVX", "PROTGAMMADUMMYX", "PROTGAMMADUMMY2X", "PROTGAMMAAUTOX",
						"PROTGAMMALG4MX", "PROTGAMMALG4XX", "PROTGAMMAPROT_FILEX", "PROTGAMMAGTR_UNLINKEDX",
						"PROTGAMMAGTRX", "ASC_PROTGAMMADAYHOFFX", "ASC_PROTGAMMADCMUTX", "ASC_PROTGAMMAJTTX",
						"ASC_PROTGAMMAMTREVX", "ASC_PROTGAMMAWAGX", "ASC_PROTGAMMARTREVX", "ASC_PROTGAMMACPREVX",
						"ASC_PROTGAMMAVTX", "ASC_PROTGAMMABLOSUM62X", "ASC_PROTGAMMAMTMAMX", "ASC_PROTGAMMALGX",
						"ASC_PROTGAMMAMTARTX", "ASC_PROTGAMMAMTZOAX", "ASC_PROTGAMMAPMBX", "ASC_PROTGAMMAHIVBX",
						"ASC_PROTGAMMAHIVWX", "ASC_PROTGAMMAJTTDCMUTX", "ASC_PROTGAMMAFLUX",
						"ASC_PROTGAMMASTMTREVX", "ASC_PROTGAMMADUMMYX", "ASC_PROTGAMMADUMMY2X",
						"ASC_PROTGAMMAAUTOX", "ASC_PROTGAMMALG4MX", "ASC_PROTGAMMALG4XX", "ASC_PROTGAMMAPROT_FILEX",
						"ASC_PROTGAMMAGTR_UNLINKEDX", "ASC_PROTGAMMAGTRX", "PROTGAMMAIDAYHOFFX", "PROTGAMMAIDCMUTX",
						"PROTGAMMAIJTTX", "PROTGAMMAIMTREVX", "PROTGAMMAIWAGX", "PROTGAMMAIRTREVX",
						"PROTGAMMAICPREVX", "PROTGAMMAIVTX", "PROTGAMMAIBLOSUM62X", "PROTGAMMAIMTMAMX",
						"PROTGAMMAILGX", "PROTGAMMAIMTARTX", "PROTGAMMAIMTZOAX", "PROTGAMMAIPMBX",
						"PROTGAMMAIHIVBX", "PROTGAMMAIHIVWX", "PROTGAMMAIJTTDCMUTX", "PROTGAMMAIFLUX",
						"PROTGAMMAISTMTREVX", "PROTGAMMAIDUMMYX", "PROTGAMMAIDUMMY2X", "PROTGAMMAIAUTOX",
						"PROTGAMMAILG4MX", "PROTGAMMAILG4XX", "PROTGAMMAIPROT_FILEX", "PROTGAMMAIGTR_UNLINKEDX",
						"PROTGAMMAIGTRX" };
				if(!Arrays.asList(options).contains(model)) {
					System.err.println("Error : Invalid protein model named '" + model + "' for RAxML. Enter a proper model.");
					System.err.println("Exit!");
					System.exit(1);
				}
			}else if(phylogenyTool.equals(PhylogenyTool.fasttree)) {
				if (!model.equalsIgnoreCase("JTTcat") && !model.equalsIgnoreCase("LGcat")
						&& !model.equalsIgnoreCase("WAGcat") && !model.equalsIgnoreCase("JTTgamma")
						&& !model.equalsIgnoreCase("LGgamma") && !model.equalsIgnoreCase("WAGgamma")) {
					System.err.println("Error : Invalid protein model named '" + model + "' for FastTree. Enter a proper model.");
					System.err.println("Exit!");
					System.exit(1);
				}
			}
		}else {
			if (phylogenyTool.equals(PhylogenyTool.raxml)) {
				String[] options = { "GTRCAT", "GTRCATI", "ASC_GTRCAT", "GTRGAMMA", "ASC_GTRGAMMA", "GTRGAMMAI",
						"GTRCATX", "GTRCATIX", "ASC_GTRCATX", "GTRGAMMAX", "ASC_GTRGAMMAX", "GTRGAMMAIX" };

				if(!Arrays.asList(options).contains(model)) {
					System.err.println("Error : Invalid DNA model named '" + model + "' for RAxML. Enter a proper model.");
					System.err.println("Exit!");
					System.exit(1);
				}
			} else {
				if (!model.equalsIgnoreCase("JCcat") && !model.equalsIgnoreCase("GTRcat")
						&& !model.equalsIgnoreCase("JCgamma") && !model.equalsIgnoreCase("GTRgamma")) {
					System.err.println("Error : Invalid DNA model named '" + model + "' for FastTree. Enter a proper model.");
					System.err.println("Exit!");
					System.exit(1);
				}
			}
		}
		
		
	}
	
	private void validateParametersreplace() {
		
		final String[] validatedLeaf = {"-uid", "-acc", "-label", "-taxon", "-strain", "-type", "-taxonomy"};
		List<String> validatedLeafOptionList = Arrays.asList(validatedLeaf);
		
//		Arguments arg = new Arguments(parameters);
		
		for(String param : paramList) {
			if(param.startsWith("-")&& !validatedLeafOptionList.contains(param)) {
				System.err.println("Error!");
				System.err.println("Invalid option " + param);
				System.err.println("Exit!");
				System.exit(1);
			}
		}
	}



	private void getProgramPath() {

//		String prodigalPath = null;
//		String hmmsearchPath = null;
		String mafftPath = null;
		String fasttreePath = null;
		String raxmlPath = null;

		programPath = new HashMap<>();
		try {
			File jar = new File(TreeModule.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
			String jarDir = jar.getParent() + File.separator;
			BufferedReader pathBR = new BufferedReader(
					new FileReader(jarDir + "programPath"));

			String line;
			while ((line = pathBR.readLine()) != null) {
/*
				if (line.startsWith("prodigal=")) {
					prodigalPath = line.substring(line.indexOf("=") + 1);
					programPath.put("prodigal", prodigalPath);
				} else if (line.startsWith("hmmsearch=")) {
					hmmsearchPath = line.substring(line.indexOf("=") + 1);
					programPath.put("hmmsearch", hmmsearchPath);
*/				if (line.startsWith("mafft=")) {
					mafftPath = line.substring(line.indexOf("=") + 1);
					programPath.put("mafft", mafftPath);
				} else if (line.startsWith("fasttree=")) {
					fasttreePath = line.substring(line.indexOf("=") + 1);
					programPath.put("fasttree", fasttreePath);
				} else if (line.startsWith("raxml")) {
					raxmlPath = line.substring(line.indexOf("=") + 1);
					programPath.put("raxml", raxmlPath);
				}
			}

			pathBR.close();

			if (mafftPath == null || fasttreePath == null
					|| raxmlPath == null) {
				System.err.println("Error : The external program path is not properly set. Check the path file.");
				System.exit(1);
			}

		} catch (IOException e) {
			System.err.println("Error occurred!");
			System.err.println(e.getMessage());
			System.exit(1);
		} catch (URISyntaxException ex) {
			System.err.println("Error occurred!");
			System.err.println(ex.getMessage());
			System.exit(1);
		}
	}

}