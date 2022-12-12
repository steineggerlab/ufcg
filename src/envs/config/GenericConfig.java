package envs.config;

import pipeline.ExceptionHandler;
import pipeline.UFCGMainPipeline;
import envs.toolkit.ANSIHandler;
import envs.toolkit.FileStream;
import envs.toolkit.Prompt;
import envs.toolkit.Shell;

import java.util.Arrays;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;

public class GenericConfig {
	public static boolean TEST = false; // Testing?
	
	/* System info */
	public static final String OS = System.getProperty("os.name");
	public static long CPU_COUNT = OS.equals("Linux") ? 
			Long.parseLong(Shell.exec("grep -c proc /proc/cpuinfo", true, 0)[0]):
			Long.parseLong(Shell.exec("sysctl -n hw.ncpu")[0]);
	public static long MEM_SIZE = OS.equals("Linux") ?
			Long.parseLong(Shell.exec("free -b | grep Mem | awk '{print $2}'", true, 0)[0]):
			Long.parseLong(Shell.exec("sysctl -n hw.memsize")[0]);
	
	/* Running project status */
	public static String PHEAD = ""; 		// Prompt header
	public static int HLEN = 0;				// Prompt maximum header length
	private static boolean custom_hlen = false;
	public static int MODULE = 0; // module
	public static boolean DYNAMIC = false; // using dynamic output buffer?

	public static void setHeader(String header) {
		PHEAD = header;
		if(!custom_hlen) HLEN  = header.length();
	}
	public static void setHeaderLength(int len) {
		HLEN  = len;
		custom_hlen = true;
	}
	
	public static void setModule(int module) {
		MODULE = module;
	}
	public static String getModuleName() {
		switch(MODULE) {
		case UFCGMainPipeline.MODULE_PROFILE: return "profile";
		case UFCGMainPipeline.MODULE_PROFILE_RNA: return "profile-rna";
		case UFCGMainPipeline.MODULE_TREE: return "tree";
		case UFCGMainPipeline.MODULE_PRUNE: return "prune";
		case UFCGMainPipeline.MODULE_ALIGN: return "align";
		case UFCGMainPipeline.MODULE_TRAIN: return "train";
		case UFCGMainPipeline.MODULE_CONVERT: return "convert";
		default: return "";
		}
	}
	
	public static final String SESSION_UID = "UFCG_" + Long.toHexString(new Random().nextLong());
	public static String TEMP_HEADER = SESSION_UID + "_";
	
	public static boolean DEV = false;      // Developer mode
	public static boolean VERB = false; 	// Program verbosity
	public static boolean QUIET = false;
	public static boolean NOCOLOR = false;  // No color escapes
	public static boolean TSTAMP = true;   // Print timestamp
	public static boolean INTRON = true;   // Include intron
	public static boolean INTERACT = false; // Interactive mode
	public static boolean FORCE = false;    // Force to delete existing file
	
	/* Runtime custom variables */
	public static String FILENAME = "";
	public static void setFilename(String name) {
		FILENAME = name;
	}
	public static String LABEL = null;
	public static void setLabel(String lab) {
		LABEL = lab;
	}
	public static String ACCESS = "";
	public static void setAccession(String acc) {
		ACCESS = acc;
	}
	public static String TAXON = null;
	public static void setTaxon(String tax) {
		TAXON = tax;
	}
	public static String NCBI = null;
	public static void setNcbi(String name) {
		NCBI = name;
	}
	public static String STRAIN = null;
	public static void setStrain(String str) {
		STRAIN = str;
	}
	public static String TAXONOMY = null;
	public static void setTaxonomy(String txn) {
		TAXONOMY = txn;
	}
	public static void setSystem(String filename, String accession, String label, String taxon, String ncbi, String strain, String taxonomy) {
		setFilename(filename);
		setAccession(accession);
		setLabel(label);
		setTaxon(taxon);
		setNcbi(ncbi);
		setStrain(strain);
		setTaxonomy(taxonomy);
	}
	
	public static int ThreadPoolSize = 1;
	public static void setThreadPoolSize(int size) {
		ThreadPoolSize = size;
	}
	public static int setThreadPoolSize(String val) {
		try {
			Prompt.talk("Custom CPU thread count check : " + ANSIHandler.wrapper(val, 'B'));
			int size = Integer.parseInt(val);
			
			if(size < 1) {
				ExceptionHandler.pass(size);
				ExceptionHandler.handle(ExceptionHandler.INVALID_VALUE);
			}
			
			if(size > CPU_COUNT) {
				Prompt.warn("Given CPU count is larger than the system value. Reducing the count down to " + CPU_COUNT);
				size = (int) CPU_COUNT;
			}
			setThreadPoolSize(size);
			return 0;
		}
		catch(NumberFormatException nfe) {
			ExceptionHandler.pass(val + " (Integer value expected)");
			ExceptionHandler.handle(ExceptionHandler.INVALID_VALUE);
			return 1;
		}
	}
	
	public static double FastBlockSearchCutoff = 0.5;
	public static void setFastBlockSearchCutoff(double cutoff) {
		FastBlockSearchCutoff = cutoff;
	}
	public static int setFastBlockSearchCutoff(String val) {
		try {
			Prompt.talk("Custom fastBlockSearch cutoff check : " + ANSIHandler.wrapper(val, 'B'));
			double cutoff = Double.parseDouble(val);
			
			if(cutoff <= .0) {
				ExceptionHandler.pass(cutoff);
				ExceptionHandler.handle(ExceptionHandler.INVALID_VALUE);
			}
			
			setFastBlockSearchCutoff(cutoff);
			return 0;
		}
		catch(NumberFormatException nfe) {
			ExceptionHandler.pass(val + " (Floating point value expected)");
			ExceptionHandler.handle(ExceptionHandler.INVALID_VALUE);
			return 1;
		}
	}
	
	public static int FastBlockSearchHits = 5;
	public static void setFastBlockSearchHits(int hits) {
		FastBlockSearchHits = hits;
	}
	public static void setFastBlockSearchHits(String val) {
		try {
			Prompt.talk("Custom fastBlockSearch hits check : " + ANSIHandler.wrapper(val, 'B'));
			int hits = Integer.parseInt(val);
			
			if(hits <= 0) {
				ExceptionHandler.pass(hits);
				ExceptionHandler.handle(ExceptionHandler.INVALID_VALUE);
			}
			
			setFastBlockSearchHits(hits);
		}
		catch(NumberFormatException nfe) {
			ExceptionHandler.pass(val + " (Integer value expected)");
			ExceptionHandler.handle(ExceptionHandler.INVALID_VALUE);
		}
	}
	
	public static int AugustusPredictionOffset = 10000;
	public static void setAugustusPredictionOffset(int offset) {
		AugustusPredictionOffset = offset;
	}
	public static int setAugustusPredictionOffset(String val) {
		try {
			Prompt.talk("Custom AUGUSTUS offset window check : " + ANSIHandler.wrapper(val, 'B'));
			int offset = Integer.parseInt(val);
			
			if(offset < 1) {
				ExceptionHandler.pass(offset);
				ExceptionHandler.handle(ExceptionHandler.INVALID_VALUE);
			}
			
			GenericConfig.setAugustusPredictionOffset(offset);
			return 0;
		}
		catch(NumberFormatException nfe) {
			ExceptionHandler.pass(val + " (Integer value expected)");
			ExceptionHandler.handle(ExceptionHandler.INVALID_VALUE);
			return 1;
		}
	}
	
	public static double HmmsearchScoreCutoff = 100.0;
	public static void setHmmsearchScoreCutoff(double cutoff) {
		HmmsearchScoreCutoff = cutoff;
	}
	public static int setHmmsearchScoreCutoff(String val) {
		try {
			Prompt.talk("Custom hmmsearch score cutoff check : " + ANSIHandler.wrapper(val, 'B'));
			double cutoff = Double.parseDouble(val);
			
			if(cutoff <= .0) {
				ExceptionHandler.pass(cutoff);
				ExceptionHandler.handle(ExceptionHandler.INVALID_VALUE);
			}
			
			setHmmsearchScoreCutoff(cutoff);
			return 0;
		}
		catch(NumberFormatException nfe) {
			ExceptionHandler.pass(val + " (Floating point value expected)");
			ExceptionHandler.handle(ExceptionHandler.INVALID_VALUE);
			return 1;
		}
	}
	
	public static double EvalueCutoff = 1e-3;
	public static void setEvalueCutoff(double cutoff) {
		EvalueCutoff = cutoff;
	}
	public static void setEvalueCutoff(String val) {
		try {
			Prompt.talk("Custom e-value cutoff check : " + ANSIHandler.wrapper(val, 'B'));
			double cutoff = Double.parseDouble(val);
			
			if(cutoff <= .0) {
				ExceptionHandler.pass(cutoff);
				ExceptionHandler.handle(ExceptionHandler.INVALID_VALUE);
			}
			
			setEvalueCutoff(cutoff);
		}
		catch(NumberFormatException nfe) {
			ExceptionHandler.pass(val + " (Floating point value expected)");
			ExceptionHandler.handle(ExceptionHandler.INVALID_VALUE);
		}
	}
	
	// public static double Coverage = 0.8;
	
	// Reference Fungal Core Gene
/*	public static final String[] FCG_ALT = {
			"ACC1",   "ALA1",   "ASN1",   "ASP1",   "BMS1",   "BUD7",   "CDC48",  "CHS2",   "CYR1",
			"FZO1",   "GEA2",   "GLT1",   "HIS4",   "HMG1",   "HSP60",  "HSP70",  "HSP104", "INO80",
			"IQG1",   "KAP95",  "KOG1",   "MCM2",   "MCM5",   "MCM7",   "MSH2",   "MSH6",   "MYO2",
			"NOT1",   "PAB1",   "PEX6",   "PIM1",   "PKC1",   "PYR1",   "RAD50",  "RPA1",   "RPB1",
			"RPB2",   "RPN1",   "SEC7",   "SEC18",  "SEC24",  "SIN3",   "SKI2",   "SLA2",   "SMC2",
			"SMC3",   "SPB1",   "SPT6",   "SPT16",  "STT4",   "TAF1",   "TEF1",   "TOR1",   "TUB2",
			"UBA1",   "UTP20",  "VPS13",  "XPO1"
	};
	public static final String[] FCG_ALT = {
			"ACO1",   "ACO2",   "ACT1",   "ATP6",   "CCT2",   "CCT4",   "CCT7",   "CCT8",   "CDC21",  "CDC48",
			"CMD1",   "COB",    "COX1",   "COX2",   "COX3",   "CPA2",   "DBP1",   "DBP2",   "ECM10",  "EFT1",
			"FAL1",   "FKS1",   "HRR25",  "HSP60",  "ILV2",   "IMD2",   "KAR2",   "KGD1",   "LEU1",   "MCM2",
			"MCM7",   "MDH1",   "MTR4",   "NBP35",  "NDI1",   "OLI1",   "PAH1",   "PGK1",   "PHO85",  "PRP43",
			"RET1",   "RPB2",   "RPL2B",  "RPL3",   "RPL4A",  "RPL8A",  "RPO21",  "RPP0",   "RPS0A",  "RPT1",
			"RPT2",   "RPT3",   "RPT5",   "RPT6",   "SAM1",   "SAM2",   "SSA1",   "SSA3",   "SSB1",   "SSC1",
			"SUP45",  "TCP1",   "TEF1",   "TIF1",   "TOP1",   "TRP3",   "TSR1",   "TUB1",   "TUB2",   "UBI4",
			"URA2"
	};*/
	public static final String[] FCG_REF = {
			"ACT1",  "ATP6",  "BMS1",  "BRE2",  "CCT8",  "CMD1",  "COB",   "COX1",  "COX2",  "COX3",
			"DIP2",  "DPH5",  "DYS1",  "ELP3",  "ESF1",  "FAP7",  "FRS1",  "HEM12", "HIS4",  "HIS7",
			"ILV1",  "KRE33", "MCM7",  "MET6",  "MIP1",  "MRPL19","MSF1",  "MSS51", "MVD1",  "NCS6",
			"NDI1",  "NOG1",  "NOP14", "OLI1",  "PAH1",  "PGK1",  "POL2",  "PRT1",  "RAD2",  "RLI1",
			"RPB2",  "RPF2",  "RPN1",  "RPO21", "RPP0",  "SDA1",  "SEC21", "SEC26", "SPB1",  "TEF1",
			"TIF5",  "TIM44", "TOP1",  "TRM1",  "TRP3",  "TSR1",  "TUB1",  "TUB2",  "UTP21", "VMA1",
			"ZPR1"
	};
	// In order considering the calculation time of each gene
	public static final String[] FCG_ORD = {
			"POL2",  "RPO21", "MIP1",  "RPB2",  "BMS1",  "VMA1",  "KRE33", "RAD2",  "RPN1",  "SEC26", 
			"DIP2",  "UTP21", "SEC21", "PAH1",  "MCM7",  "SPB1",  "NOP14", "HIS4",  "TSR1",  "TOP1",  
			"SDA1",  "MET6",  "PRT1",  "NOG1",  "ESF1",  "RLI1",  "FRS1",  "ILV1",  "TRM1",  "CCT8",  
			"ELP3",  "HIS7",  "COX1",  "NDI1",  "BRE2",  "ZPR1",  "TRP3",  "MSF1",  "TEF1",  "TUB2",  
			"TUB1",  "MSS51", "TIM44", "PGK1",  "TIF5",  "MVD1",  "DYS1",  "COB",   "ACT1",  "HEM12", 
			"NCS6",  "RPF2",  "RPP0",  "DPH5",  "COX3",  "ATP6",  "COX2",  "FAP7",  "MRPL19","CMD1",  
			"OLI1"
	};

	public static final String[] FCG_COG = {
			"Z","C","J","B/K","O","T","C","C","C","C",
			"A","J","O","B/K","S","F","J","H","E","E",
			"E","R","L","E","L","J","J","O","I","R",
			"C","R","J","C","N/I","G","L","J","L","A",
			"K","J","O","K","J","D/Z","U","U","A/R","J",
			"J","U","L","J","E","S","Z","Z","R","H",
			"R"
	};
	public static final String[] FCG_DSC = {
			"Actin",
			"F1F0 ATP synthase subunit",
			"Ribosome biogenesis protein",
			"COMPASS component",
			"Chaperonin-containing T-complex subunit",
			"Calmodulin",
			"Cytochrome b",
			"Cytochrome c oxidase subunit",
			"Cytochrome c oxidase subunit",
			"Cytochrome c oxidase subunit",
			"U3 small nucleolar RNA-associated protein 12",
			"Diphthine methyl ester synthase",
			"Deoxyhypusine synthase",
			"Elongator complex protein 3",
			"Pre-rRNA-processing protein",
			"Adenylate kinase isoenzyme 6 homolog",
			"Phenylalanine--tRNA ligase beta subunit",
			"Uroporphyrinogen decarboxylase",
			"Histidine biosynthesis trifunctional protein",
			"Imidazole glycerol phosphate synthase",
			"Threonine dehydratase",
			"RNA cytidine acetyltransferase",
			"Mini-chromosome maintenance complex subunit",
			"5-methyltetrahydropteroyltriglutamate--homocysteine methyltransferase",
			"DNA polymerase gamma",
			"54S ribosomal protein L19",
			"Phenylalanine--tRNA ligase",
			"Mitochondrial splicing suppressor protein 51",
			"Diphosphomevalonate decarboxylase",
			"Cytoplasmic tRNA 2-thiolation protein 1",
			"NADH-ubiquinone reductase",
			"Nucleolar GTP-binding protein 1",
			"Nucleolar complex protein 14",
			"F0 ATP synthase subunit",
			"Phosphatidate phosphatase",
			"Phosphoglycerate kinase",
			"DNA polymerase epsilon catalytic subunit A",
			"Eukaryotic translation initiation factor 3 subunit B",
			"DNA repair protein",
			"Translation initiation factor",
			"DNA-directed RNA polymerase II core subunit",
			"Ribosome biogenesis protein",
			"26S proteasome regulatory subunit",
			"DNA-directed RNA polymerase II core subunit",
			"60S acidic ribosomal protein P0",
			"Severe depolymerization of actin protein 1",
			"Coatomer subunit gamma",
			"Coatomer subunit beta",
			"27S pre-rRNA (guanosine(2922)-2'-O)-methyltransferase",
			"Translation elongation factor EF-1 alpha",
			"Eukaryotic translation initiation factor 5",
			"Mitochondrial import inner membrane translocase subunit",
			"DNA topoisomerase 1",
			"tRNA (guanine(26)-N(2))-dimethyltransferase",
			"Multifunctional tryptophan biosynthesis protein",
			"Ribosome maturation factor",
			"Alpha-tubulin",
			"Beta-tubulin",
			"U3 small nucleolar RNA-associated protein 21",
			"V-type proton ATPase catalytic subunit A",
			"Zinc finger protein"
	};
	
	// Tree models
	public static final String[] PROTEIN_RAXML_MODELS = { 
		"PROTCATDAYHOFF", "PROTCATDCMUT", "PROTCATJTT", "PROTCATMTREV", "PROTCATWAG",
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
		"PROTGAMMAIGTRX"
	},
	PROTEIN_FASTTREE_MODELS = {
		"JTTcat", "LGcat", "WAGcat", "JTTgamma", "LGgamma", "WAGgamma"
	},
	PROTEIN_IQTREE_MODELS = {
			
	},
	NUCLEOTIDE_RAXML_MODELS = {
		"GTRCAT", "GTRCATI", "ASC_GTRCAT", "GTRGAMMA", "ASC_GTRGAMMA", "GTRGAMMAI",
		"GTRCATX", "GTRCATIX", "ASC_GTRCATX", "GTRGAMMAX", "ASC_GTRGAMMAX", "GTRGAMMAIX"
	},
	NUCLEOTIDE_FASTTREE_MODELS = {
		"JCcat", "GTRcat", "JCgamma", "GTRgamma"
	},
	NUCLEOTIDE_IQTREE_MODELS = {
			
	};
			
	/* Gene set definition */
	public static String GENESET = "PRO";
	public static String[] FCG = FCG_ORD; // core genes for this process
	public static void setGeneset(String geneset) {GENESET = geneset;}
	
	public static final int TARGET_PRO = 0x01,
							TARGET_BUSCO = 0x02;
	
	public static boolean NUC = false, PRO = false, BUSCO = false;
	public static int TARGET = 0;
	public static int solveGeneset() {
		List<String> pros = new ArrayList<>(); // custom protein marker set
		for(String ele : GENESET.split(",")) {
			switch (ele) {
				case "NUC":
					NUC = true;
					break;
				case "PRO":
					PRO = true;
					break;
				case "BUSCO":
					BUSCO = true;
					break;
				// else if(!Arrays.asList(FCG_REF).contains(ele)) return 1; // not allowing non-core protein markers
				default:
					PRO = true;
					pros.add(ele);
					break;
			}
		}
		
		if(!(NUC | PRO | BUSCO)) return 1; // invalid if nothing is detected
		if(pros.size() > 0) FCG = Arrays.copyOf(pros.toArray(), pros.toArray().length, String[].class); // use custom proteins if detected
		return 0;
	}
	
	public static String[] QUERY_GENES = null; // query genes
	public static void setQueryGenes(String[] q, int target) {QUERY_GENES = q; TARGET = target;}
	
	public static String[] BUSCOS = null;
	public static int getBuscos() {
		List<String> bids = new ArrayList<>();
		
		// get list from sequence directory
		String cmd = "ls -1 " + PathConfig.SeqPath + "busco > " + PathConfig.TempPath + GenericConfig.TEMP_HEADER + "busco.seq.list";
		Shell.exec(cmd);
		try {
			FileStream tmpListStream = new FileStream(PathConfig.TempPath + GenericConfig.TEMP_HEADER + "busco.seq.list", 'r');
			tmpListStream.isTemp();
			String buf;
			while((buf = tmpListStream.readLine()) != null) {
				if(!buf.endsWith(".fa")) continue;
				bids.add(buf.substring(0, buf.indexOf('.')));
			}
			
			tmpListStream.wipe(true);
			BUSCOS = Arrays.copyOf(bids.toArray(), bids.toArray().length, String[].class);
		}
		catch(java.io.IOException e) {
			e.printStackTrace();
			ExceptionHandler.handle(ExceptionHandler.EXCEPTION);
		}
		
		// validate list using model directory
		cmd = "ls -1 " + PathConfig.ModelPath + "busco > " + PathConfig.TempPath + GenericConfig.TEMP_HEADER + "busco.model.list";
		Shell.exec(cmd);
		try {
			int[] cnt = new int[BUSCOS.length];
			Arrays.fill(cnt, -1);
			
			FileStream tmpListStream = new FileStream(PathConfig.TempPath + GenericConfig.TEMP_HEADER + "busco.model.list", 'r');
			tmpListStream.isTemp();
			String buf;
			while((buf = tmpListStream.readLine()) != null) {
				if(!buf.endsWith(".hmm")) continue;
				int loc = 0;
				for(; loc < cnt.length; loc++) if(buf.contains(BUSCOS[loc])) break;
				if(loc == cnt.length) continue;
				cnt[loc]++;
			}
			tmpListStream.wipe(true);
			
			for(int c : cnt) if(c < 0) return 1;
		}
		catch(java.io.IOException e) {
			e.printStackTrace();
			ExceptionHandler.handle(ExceptionHandler.EXCEPTION);
		}
		
		Prompt.talk("Number of BUSCOs to extract : " + ANSIHandler.wrapper(String.valueOf(BUSCOS.length), 'B'));
		return 0;
	}
	
/*	public static int setCustomCoreList(String list) {
		Prompt.talk("Custom core gene list check : " + ANSIHandler.wrapper(list, 'B'));
		
		if(!list.contains(",")) {
			if(!Arrays.asList(FCG_REF).contains(list)) {
				ExceptionHandler.handle(ExceptionHandler.WRONG_CORE_FORMAT);
				return 1;
			}
		}
		
		String[] split = list.split(",");
//		for(String cg : split) {
//			if(!Arrays.asList(FCG_REF).contains(cg)) {
//				ExceptionHandler.pass(cg);
//				ExceptionHandler.handle(ExceptionHandler.INVALID_GENE_NAME);
//				return 1;
//			}
//		}
		
		FCG = split;
		if(VERB) Prompt.print(String.format("Custom gene set containing %d genes confirmed.", FCG.length));
		return 0;
	} */
	
	public static String geneString() {
		StringBuilder gstr = new StringBuilder();
		for(String fcg : FCG_REF) gstr.append(fcg).append(",");
		return gstr.substring(0, gstr.length() - 1);
	}
}
