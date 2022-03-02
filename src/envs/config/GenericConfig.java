package envs.config;

import pipeline.ExceptionHandler;
import pipeline.UFCGMainPipeline;
import envs.toolkit.ANSIHandler;
import envs.toolkit.Prompt;
import java.util.Arrays;
import java.util.Random;

public class GenericConfig {
	/* Running project status */
	public static String PHEAD = ""; 		// Prompt header
	public static int HLEN = 0;				// Prompt maximum header length
	private static boolean custom_hlen = false;
	public static int MODULE = 0; // module
	
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
		case UFCGMainPipeline.MODULE_TREE_FIX: return "tree-fix";
		default: return "";
		}
	}
	
	public static final String SESSION_UID = Long.toHexString(new Random().nextLong());
	public static String TEMP_HEADER = SESSION_UID + "_";
	
	public static boolean DEV = false;      // Developer mode
	public static boolean VERB = false; 	// Program verbosity
	public static boolean NOCOLOR = false;  // No color escapes
	public static boolean TSTAMP = true;   // Print timestamp
	public static boolean INTRON = false;   // Include intron
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
			
			setThreadPoolSize(size);
			return 0;
		}
		catch(NumberFormatException nfe) {
			ExceptionHandler.pass(val + " (Numerical value expected)");
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
			ExceptionHandler.pass(val + " (Numerical value expected)");
			ExceptionHandler.handle(ExceptionHandler.INVALID_VALUE);
			return 1;
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
			ExceptionHandler.pass(val + " (Numerical value expected)");
			ExceptionHandler.handle(ExceptionHandler.INVALID_VALUE);
			return 1;
		}
	}
	
	// Reference Fungal Core Gene
/*	public static final String[] FCG_ALT = {
			"ACC1",   "ALA1",   "ASN1",   "ASP1",   "BMS1",   "BUD7",   "CDC48",  "CHS2",   "CYR1",
			"FZO1",   "GEA2",   "GLT1",   "HIS4",   "HMG1",   "HSP60",  "HSP70",  "HSP104", "INO80",
			"IQG1",   "KAP95",  "KOG1",   "MCM2",   "MCM5",   "MCM7",   "MSH2",   "MSH6",   "MYO2",
			"NOT1",   "PAB1",   "PEX6",   "PIM1",   "PKC1",   "PYR1",   "RAD50",  "RPA1",   "RPB1",
			"RPB2",   "RPN1",   "SEC7",   "SEC18",  "SEC24",  "SIN3",   "SKI2",   "SLA2",   "SMC2",
			"SMC3",   "SPB1",   "SPT6",   "SPT16",  "STT4",   "TAF1",   "TEF1",   "TOR1",   "TUB2",
			"UBA1",   "UTP20",  "VPS13",  "XPO1"
	};*/
	public static final String[] FCG_REF = {
			"ACO1",   "ACO2",   "ACT1",   "CCT2",   "CCT4",   "CCT7",   "CDC21",  "CDC48",  "CPA2",   "DBP1",
			"DBP2",   "DED1",   "ECM10",  "EFT1",   "FAL1",   "FKS1",   "HRR25",  "HSP60",  "ILV2",   "IMD2",
			"IMD3",   "IMD4",   "KAR2",   "KGD1",   "LEU1",   "MCM2",   "MCM7",   "MDH1",   "MTR4",   "NBP35",
			"PHO85",  "PRP43",  "RET1",   "RPB2",   "RPL2B",  "RPL3",   "RPL4A",  "RPL4B",  "RPL8A",  "RPL8B",
			"RPO21",  "RPP0",   "RPS0A",  "RPS0B",  "RPT1",   "RPT2",   "RPT3",   "RPT5",   "RPT6",   "SAM1",
			"SAM2",   "SSA1",   "SSA2",   "SSA3",   "SSA4",   "SSB1",   "SSB2",   "SSC1",   "SUP45",  "TCP1",
			"TEF1",   "TIF1",   "TRP3",   "TUB1",   "TUB2",   "TUB3",   "UBI4",   "URA2"
	};
	// In order considering the calculation time of each gene
	public static final String[] FCG_ORD = {
			"URA2","KAR2","FKS1","SSB1","ACO2","SSA4","SSA3","CPA2","SSB2","UBI4",
			"ACO1","MCM7","PRP43","RPO21","RPB2","SSA1","SSA2","MTR4","RET1","KGD1",
			"RPT5","RPT6","MCM2","EFT1","LEU1","ECM10","ILV2","FAL1","TRP3","IMD4",
			"DED1","RPT2","CCT4","PHO85","DBP1","SSC1","CDC48","MDH1","HRR25","RPT3",
			"TIF1","CCT2","TUB2","IMD2","TCP1","HSP60","IMD3","CCT7","SAM1","RPL8B",
			"RPL4A","RPT1","SAM2","TEF1","RPL8A","TUB1","SUP45","ACT1","RPL4B","TUB3",
			"DBP2","NBP35","RPL3","RPP0","RPS0A","RPS0B","CDC21","RPL2B"
	};
	public static final String[] FCG_COG = {
			"C","C","Z","O","O","O","F","M/D/T","E/F","L","L","L","O","J","L","M","T","O","E/H","T","T","T",
			"O","C","E","L","L","C","L","D","T","J","K","K","J","J","J","J","J","J","K","J","J","J","O","O",
			"O","O","O","H","H","O","O","O","O","O","O","O","J","O","J","L","E","Z","Z","Z","O","E/F"
	};
	public static final String[] FCG_DSC = {
			"Aconitate hydratase, mitochondrial",
			"Homocitrate dehydratase, mitochondrial",
			"Actin",
			"T-complex protein 1 subunit beta",
			"T-complex protein 1 subunit delta",
			"T-complex protein 1 subunit eta",
			"Thymidylate synthase",
			"Cell division control protein 48",
			"Carbamoyl-phosphate synthase arginine-specific large chain",
			"ATP-dependent RNA helicase DBP1",
			"ATP-dependent RNA helicase DBP2",
			"ATP-dependent RNA helicase DED1",
			"Heat shock protein SSC3",
			"Elongation factor 2",
			"ATP-dependent RNA helicase FAL1",
			"1,3-beta-glucan synthase component FKS1",
			"Casein kinase I homolog HRR25",
			"Heat shock protein 60, mitochondrial",
			"Acetolactate synthase catalytic subunit, mitochondrial",
			"Inosine-5'-monophosphate dehydrogenase 2",
			"Inosine-5'-monophosphate dehydrogenase 3",
			"Inosine-5'-monophosphate dehydrogenase 4",
			"Endoplasmic reticulum chaperone BiP",
			"2-oxoglutarate dehydrogenase, mitochondrial",
			"3-isopropylmalate dehydratase",
			"DNA replication licensing factor MCM2",
			"DNA replication licensing factor MCM7",
			"Malate dehydrogenase, mitochondrial",
			"ATP-dependent RNA helicase DOB1",
			"Cytosolic Fe-S cluster assembly factor NBP35",
			"Cyclin-dependent protein kinase PHO85",
			"Pre-mRNA-splicing factor ATP-dependent RNA helicase PRP43",
			"DNA-directed RNA polymerase III subunit RPC2",
			"DNA-directed RNA polymerase II subunit RPB2",
			"60S ribosomal protein L2-B",
			"60S ribosomal protein L3",
			"60S ribosomal protein L4-A",
			"60S ribosomal protein L4-B",
			"60S ribosomal protein L8-A",
			"60S ribosomal protein L8-B",
			"DNA-directed RNA polymerase II subunit RPB1",
			"60S acidic ribosomal protein P0",
			"40S ribosomal protein S0-A",
			"40S ribosomal protein S0-B",
			"26S proteasome regulatory subunit 7 homolog",
			"26S proteasome regulatory subunit 4 homolog",
			"26S proteasome regulatory subunit 6B homolog",
			"26S proteasome regulatory subunit 6A",
			"26S proteasome regulatory subunit 8 homolog",
			"S-adenosylmethionine synthase 1",
			"S-adenosylmethionine synthase 2",
			"Heat shock protein SSA1",
			"Heat shock protein SSA2",
			"Heat shock protein SSA3",
			"Heat shock protein SSA4",
			"Ribosome-associated molecular chaperone SSB1",
			"Ribosome-associated molecular chaperone SSB2",
			"Heat shock protein SSC1, mitochondrial",
			"Eukaryotic peptide chain release factor subunit 1",
			"T-complex protein 1 subunit alpha",
			"Elongation factor 1-alpha",
			"ATP-dependent RNA helicase eIF4A",
			"Multifunctional tryptophan biosynthesis protein",
			"Tubulin alpha-1 chain",
			"Tubulin beta chain",
			"Tubulin alpha-3 chain",
			"Polyubiquitin",
			"Protein URA2"
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
			
	
	public static String[] FCG = FCG_ORD; // core genes for this process
	public static int setCustomCoreList(String list) {
		Prompt.talk("Custom core gene list check : " + ANSIHandler.wrapper(list, 'B'));
		
		if(!list.contains(",")) {
			if(!Arrays.asList(FCG_REF).contains(list)) {
				ExceptionHandler.handle(ExceptionHandler.WRONG_CORE_FORMAT);
				return 1;
			}
		}
		
		String[] split = list.split(",");
		for(String cg : split) {
			if(!Arrays.asList(FCG_REF).contains(cg)) {
				ExceptionHandler.pass(cg);
				ExceptionHandler.handle(ExceptionHandler.INVALID_GENE_NAME);
				return 1;
			}
		}
		
		FCG = split;
		if(VERB) Prompt.print(String.format("Custom gene set containing %d genes confirmed.", FCG.length));
		return 0;
	}
	
	public static String geneString() {
		String gstr = "";
		for(String fcg : FCG_REF) gstr += fcg + ",";
		return gstr.substring(0, gstr.length() - 1);
	}
}
