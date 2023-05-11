package module;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import envs.config.GenericConfig;
import envs.config.PathConfig;
import envs.toolkit.ANSIHandler;
import envs.toolkit.Prompt;
import pipeline.ExceptionHandler;
import pipeline.UFCGMainPipeline;
import tree.TreeBuilder;
import tree.tools.AlignMode;
import tree.tools.PhylogenyTool;
import tree.tools.Arguments;
import tree.tools.LabelReplacer;

public class TreeModule {
	String[] parameters = null;
	List<String> paramList = null;
	HashMap<String, String> programPath = null;
	List<String> outputLabels = null;

	public static void run(String[] args) {
		TreeModule proc = new TreeModule();
		proc.parameters = args;
		proc.paramList = Arrays.asList(args);

		String method = proc.paramList.get(0); // Define module

		// Help route
		if (proc.paramList.contains("-h") || proc.paramList.contains("--help")) {
			proc.printTreeHelp();
		}

		proc.getProgramPath();

		if (method.equals("align")) {
			proc.align();
		} else {
			ExceptionHandler.pass(method);
			ExceptionHandler.handle(ExceptionHandler.UNKNOWN_MODULE);
		}
	}

	private void align() {
		String ucgDirectory;
		String outDirectory = "." + File.separator;

		String mafftPath = programPath.get("mafft");
		String raxmlPath = programPath.get("raxml");
		String fasttreePath = programPath.get("fasttree");
		String iqtreePath = programPath.get("iqtree");

		PhylogenyTool phylogenyTool = PhylogenyTool.iqtree;

		int nThreads = 1;
		AlignMode alignMode = AlignMode.protein;
		int filtering = 50;
		String model = null;
		int gsi_threshold = 95;
		int executorLimit = 20;
		boolean allowMultiple;

		Arguments arg = new Arguments(parameters);

		ucgDirectory = arg.get("-ucg_dir");

		if(arg.get("-out_dir")!=null && !arg.get("-out_dir").equals("")) {
			outDirectory = arg.get("-out_dir");
		}

		String align = arg.get("-a");
		if(align!=null && !align.equals("")) {
			switch (align) {
				case "nucleotide":
					alignMode = AlignMode.nucleotide;
					break;
				case "codon":
					alignMode = AlignMode.codon;
					break;
				case "codon12":
					alignMode = AlignMode.codon12;
					break;
				case "protein":
				//	alignMode = AlignMode.protein;
					break;
				default:
					ExceptionHandler.pass(align);
					ExceptionHandler.handle(ExceptionHandler.INVALID_ALIGN_MODE);
					break;
			}
		}

		try {
			if (arg.get("-t")!=null) {

				nThreads = Integer.parseInt(arg.get("-t"));

				if (nThreads < 1) {
					ExceptionHandler.pass(nThreads);
					ExceptionHandler.handle(ExceptionHandler.INVALID_VALUE);
				}
			}

			if (arg.get("-f")!=null) {
				filtering = Integer.parseInt(arg.get("-f"));
				if (filtering > 100 || filtering < 0) {
					ExceptionHandler.pass(filtering);
					ExceptionHandler.handle(ExceptionHandler.INVALID_VALUE);
				}
			}

		}catch(NumberFormatException e) {
			ExceptionHandler.handle(e);
		}

		if (arg.get("-raxml") != null) phylogenyTool = PhylogenyTool.raxml;
		if (arg.get("-fasttree") != null) phylogenyTool = PhylogenyTool.fasttree;
		if (arg.get("-iqtree") != null) phylogenyTool = PhylogenyTool.iqtree;

		validateParametersAlign(phylogenyTool, alignMode);

		if(arg.get("-m")!=null && !arg.get("-m").equals("")) {
			model = arg.get("-m");
		}

		if (arg.get("-gsi_threshold") != null) {
			try {
				gsi_threshold = Integer.parseInt(arg.get("-gsi_threshold"));
			} catch (NumberFormatException e) {
				ExceptionHandler.handle(e);
			}
		}
		if (gsi_threshold > 100 || gsi_threshold < 0) {
			ExceptionHandler.pass(gsi_threshold);
			ExceptionHandler.handle(ExceptionHandler.INVALID_VALUE);
		}
		if(parameters.length==0||outDirectory==null||ucgDirectory==null) {
			ExceptionHandler.handle(ExceptionHandler.UNEXPECTED_ERROR);
		}
		// labels
		outputLabels = new ArrayList<>();

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
			ExceptionHandler.pass(outputLabels);
			ExceptionHandler.handle(ExceptionHandler.INVALID_LEAF_FORMAT);
		}

		if(arg.get("-x") != null) {
			try {
				executorLimit = Integer.parseInt(arg.get("-x"));
			} catch (NumberFormatException e) {
				ExceptionHandler.handle(e);
			}
		} else {
			executorLimit = nThreads;
		}

		allowMultiple = arg.get("-c") != null;

		assert ucgDirectory != null;
		TreeBuilder proc = new TreeBuilder(ucgDirectory, outDirectory, mafftPath, raxmlPath, fasttreePath, iqtreePath, alignMode, filtering, model, gsi_threshold, outputLabels, executorLimit, allowMultiple);

		try {
			proc.jsonsToTree(nThreads, phylogenyTool);
		}catch (IOException e) {
			ExceptionHandler.handle(e);
		}
	}

	@SuppressWarnings("unused")
	@Deprecated
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

		final String[] validatedOptions = {"-ucg_dir", "-out_dir", "-run_id", "-a", "-t", "-f", "-fasttree", "-iqtree", "-raxml", "-m", "-gsi_threshold", "-leaf", "-x", "-c"};
		final String[] validatedLeaf = {"uid", "acc", "label", "taxon", "strain", "type", "taxonomy"};
		List<String> validatedOptionList = Arrays.asList(validatedOptions);
		List<String> validatedLeafOptionList = Arrays.asList(validatedLeaf);

		Arguments arg = new Arguments(parameters);

		for(String param : paramList) {
			if(param.startsWith("-")&& !validatedOptionList.contains(param)) {
				ExceptionHandler.pass(param);
				ExceptionHandler.handle(ExceptionHandler.UNKNOWN_OPTION);
			}
		}

		// mandatory options
		if(arg.get("-ucg_dir")==null || arg.get("-ucg_dir").equals("")) {
			ExceptionHandler.handle(ExceptionHandler.NO_INPUT);
		}
		if(arg.get("-leaf")==null || arg.get("-leaf").equals("")) {
			ExceptionHandler.handle(ExceptionHandler.NO_LEAF_OPTION);
		}

		if(!new File(arg.get("-ucg_dir")).exists()) {
			ExceptionHandler.pass(arg.get("-ucg_dir"));
			ExceptionHandler.handle(ExceptionHandler.INVALID_DIRECTORY);
		}

		String[] leafOptions = arg.get("-leaf").split(",");

		for(String opt : leafOptions) {
			if(!validatedLeafOptionList.contains(opt)) {
				ExceptionHandler.pass(opt);
				ExceptionHandler.handle(ExceptionHandler.INVALID_LEAF_FORMAT);
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
		String[] options = null;
		if(alignMode.equals(AlignMode.protein)) {
			if(phylogenyTool.equals(PhylogenyTool.raxml)) options = GenericConfig.PROTEIN_RAXML_MODELS;
			if(phylogenyTool.equals(PhylogenyTool.fasttree)) options = GenericConfig.PROTEIN_FASTTREE_MODELS;
			if(phylogenyTool.equals(PhylogenyTool.iqtree)) options = GenericConfig.PROTEIN_IQTREE_MODELS;
		}
		else { // nucleotide
			if(phylogenyTool.equals(PhylogenyTool.raxml)) options = GenericConfig.NUCLEOTIDE_RAXML_MODELS;
			if(phylogenyTool.equals(PhylogenyTool.fasttree)) options = GenericConfig.NUCLEOTIDE_FASTTREE_MODELS;
			if(phylogenyTool.equals(PhylogenyTool.iqtree)) options = GenericConfig.NUCLEOTIDE_IQTREE_MODELS;
		}

		if(!phylogenyTool.equals(PhylogenyTool.iqtree)) {
			if(!Arrays.asList(options).contains(model)) {
				ExceptionHandler.pass(model);
				ExceptionHandler.handle(ExceptionHandler.INVALID_MODEL);
			}
		}
	}

	private void getProgramPath() {
		String mafftPath = null;
		String fasttreePath = null;
		String raxmlPath = null;
		String iqtreePath = null;

		programPath = new HashMap<>();
		try {
			BufferedReader pathBR = new BufferedReader(
					new FileReader(PathConfig.EnvironmentPath + "config/tree.cfg"));

			String line;
			while ((line = pathBR.readLine()) != null) {
				if (line.startsWith("mafft")) {
					mafftPath = line.substring(line.indexOf("=") + 1);
					programPath.put("mafft", mafftPath);
				}
				if (line.startsWith("fasttree")) {
					fasttreePath = line.substring(line.indexOf("=") + 1);
					programPath.put("fasttree", fasttreePath);
				}
				if (line.startsWith("raxml")) {
					raxmlPath = line.substring(line.indexOf("=") + 1);
					programPath.put("raxml", raxmlPath);
				}
				if (line.startsWith("iqtree")) {
					iqtreePath = line.substring(line.indexOf("=") + 1);
					programPath.put("iqtree", iqtreePath);
				}
			}

			pathBR.close();

			if (mafftPath == null || fasttreePath == null || raxmlPath == null || iqtreePath == null) {
				ExceptionHandler.handle(ExceptionHandler.INVALID_PROGRAM_PATH);
			}

		} catch (IOException e) {
			ExceptionHandler.handle(e);
		}
	}

	private void printTreeHelp() {
		System.out.println(ANSIHandler.wrapper(" UFCG - tree", 'G'));
		System.out.println(ANSIHandler.wrapper(" Reconstruct the phylogenetic relationship with UFCG profiles", 'g'));
		System.out.println();

		System.out.println(ANSIHandler.wrapper("\n USAGE:", 'Y') + " ufcg tree -i <INPUT> -l <LABEL> [...]");
		System.out.println();

		System.out.println(ANSIHandler.wrapper("\n Required options", 'Y'));
		System.out.println(ANSIHandler.wrapper(" Argument       Description", 'c'));
		System.out.println(ANSIHandler.wrapper(" -i STR         Input directory containing UFCG profiles ", 'x'));
		System.out.println();

		System.out.println(ANSIHandler.wrapper("\n Additional options", 'y'));
		System.out.println(ANSIHandler.wrapper(" Argument       Description", 'c'));
		System.out.println(ANSIHandler.wrapper(" -l STR         Tree leaf format, comma-separated string containing one or more of the following keywords: [label]", 'x'));
		System.out.println(ANSIHandler.wrapper("                {uid, acc, label, taxon, strain, type, taxonomy}", 'x'));
		System.out.println(ANSIHandler.wrapper(" -o STR         Define output directory [.]", 'x'));
		System.out.println(ANSIHandler.wrapper(" -n STR         Name of this run [random hex string] ", 'x'));
		System.out.println(ANSIHandler.wrapper(" -a STR         Alignment method {nucleotide, codon, codon12, protein} [protein]", 'x'));
		System.out.println(ANSIHandler.wrapper(" -t INT         Number of CPU threads to use [1]", 'x'));
		System.out.println(ANSIHandler.wrapper(" -p STR         Tree building program {raxml, iqtree, fasttree} [iqtree] ", 'x'));
		System.out.println(ANSIHandler.wrapper(" -c             Align multiple copied genes [0]", 'x'));
		System.out.println();

		System.out.println(ANSIHandler.wrapper("\n Advanced options", 'y'));
		System.out.println(ANSIHandler.wrapper(" Argument       Description", 'c'));
		System.out.println(ANSIHandler.wrapper(" -f INT         Gap-rich filter percentage threshold {0 - 100} [50] ", 'x'));
		System.out.println(ANSIHandler.wrapper(" -m STR         ML tree inference model [JTT+ (proteins); GTR+ (nucleotides)] ", 'x'));
		System.out.println(ANSIHandler.wrapper(" -g INT         GSI value threshold {1 - 100} [95] ", 'x'));
		System.out.println(ANSIHandler.wrapper(" -x INT         Maximum number of gene tree executors; lower this if the RAM usage is excessive {1 - threads} [equal to -t]", 'x'));
		System.out.println();
		
		UFCGMainPipeline.printGeneral();
		
		System.exit(0);
	}
}