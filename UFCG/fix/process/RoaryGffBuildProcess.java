/*
 * process.RoaryGffBuilder
 * Runnable: RGB.jar [AUG] [FNA] [OUT]
 * 		[AUG] : AUGUSTUS prediction result (GFF3)
 * 		[FNA] : Genome assembly (FASTA)
 * 		[OUT] : Output file for Roary run (GFF3)
 * 
 * Description:
 * 		1. Read GFF3 formatted AUGUSTUS prediction results
 * 			1-1. Store each gene as an object
 * 		2. Read FASTA formatted genome assembly and store by reads
 * 		3. Run through genes
 * 			3-1. Grab exon region from the assembly and create cDNA sequence
 * 			3-2. Compare the cDNA sequence with protein product
 * 				3-2-1. Fix the sequence if required
 * 			3-3. Append cDNA sequence to fixed contig
 * 			3-4. Fix GFF information according to the fixed contig
 * 		4. Write a new GFF3 file using new data
 * 			[CONTIG] AUGUSTUS:3.3.2 [CDS] [START] [END] . [STRAND] 0 ID=[ACCESS]_[N];inference=ab initio prediction:AUGUSTUS:3.3.2;locus_tag=[ACCESS]_[N];product=fungal protein
 */

package process;

import envs.config.*;
import envs.toolkit.*;
import java.util.*;

public class RoaryGffBuildProcess {
	public final String APATH, FPATH, OPATH;
	private String acc;
	
	public RoaryGffBuildProcess(String aug, String fna, String out) {
		this.APATH = aug;
		this.FPATH = fna;
		this.OPATH = out;
		this.acc = fna.substring(fna.lastIndexOf('/') + 1).split(".fna")[0];
	}
	
	private int predictIter = 1;
	private class Prediction {
		final int ID;
		String contig = ""; 			// contig ID
		List<Integer> slocs, elocs; 	// CDS, stop codon start and end locations
		String pseq = ""; 				// protein sequence
		String strand = ""; 			// strand direction
		String rawSeq = "", fixSeq = "";// cDNA sequences
		int fsloc, feloc;				// fixed CDS start, end location
				
		private Prediction() {
			this.ID = predictIter++;
			this.slocs = new ArrayList<Integer>();
			this.elocs = new ArrayList<Integer>();
		}
		
		private void feed(String line) {
			if(line.startsWith("#")) {
				if(pseq.length() > 0) {
					String seq = line.substring(2);
					if(seq.contains("]")) seq = seq.substring(0, seq.lastIndexOf("]"));
					this.pseq += seq;
				}
				else if(line.contains("]")) pseq = line.substring(line.lastIndexOf("[") + 1, line.lastIndexOf("]"));
				else pseq = line.substring(line.lastIndexOf("[") + 1);
			}
			else {
				String type = line.split("\t")[2];
				if(type.equals("gene")) {
					this.contig = line.split("\t")[0];
					this.strand = line.split("\t")[6];
				}
				else if(type.equals("CDS") || type.equals("stop_codon")) {
					this.slocs.add(Integer.parseInt(line.split("\t")[3]));
					this.elocs.add(Integer.parseInt(line.split("\t")[4]));
				}
			}
		}
	}
	
	List<Prediction> predictions = new ArrayList<Prediction>();
	List<String> uniqueContigs = new ArrayList<String>();
	
	Map<String, Integer> contigMap = new HashMap<String, Integer>();
	List<String> rawContigSequences = new ArrayList<String>();
	private String subContig(String contig, int st, int ed) {
		String sub = "";
		int iter = contigMap.get(contig);
		int bsize = rawContigSequences.get(++iter).length();
		
		int stx = (st - 1) / bsize, sty = (st - 1) % bsize;
		int edx = (ed - 1) / bsize, edy = (ed - 1) % bsize;
		
		if(stx == edx) return rawContigSequences.get(iter + stx).substring(sty, edy + 1);
		else {
			sub += rawContigSequences.get(iter + stx).substring(sty);
			for(int x = stx + 1; x < edx; x++) sub += rawContigSequences.get(iter + x);
			sub += rawContigSequences.get(iter + edx).substring(0, edy + 1);
		}
		return sub;
	}
	
	public void run() throws Exception {
		/*
		 * Step 1. Read GFF3 formatted AUGUSTUS prediction results
		 */
		Prompt.print("Reading AUGUSTUS predictions on : " + ANSIHandler.wrapper(APATH, 'y'));
		
		FileStream augStream = new FileStream(APATH, 'r');
		String buf;
		while((buf = augStream.readLine()) != null) {
			if(buf.startsWith("# start gene")) {
				// Prompt.talk("Gene found : " + ANSIHandler.wrapper(String.format("%s_g%d", acc, predictIter), 'g'));
				Prediction prediction = new Prediction();
				while(!(buf = augStream.readLine()).startsWith("# end gene")) prediction.feed(buf);
				if(!uniqueContigs.contains(prediction.contig)) uniqueContigs.add(prediction.contig);
				predictions.add(prediction);
			}
		}
		
		Prompt.print(String.format("AUGUSTUS predicted %s genes from %s unique contigs.",
				ANSIHandler.wrapper(String.valueOf(predictions.size()), 'g'),
				ANSIHandler.wrapper(String.valueOf(uniqueContigs.size()), 'g')));
		augStream.close();
		
		/*
		 * Step 2. Read FASTA formatted genome assembly and store by reads
		 */
		Prompt.print("Importing genome sequence on : " + ANSIHandler.wrapper(FPATH, 'y'));
		
		FileStream fnaStream = new FileStream(FPATH, 'r');
		while((buf = fnaStream.readLine()) != null) {
			if(buf.startsWith(">")) {
				String contig = buf.split(" ")[0].substring(1);
				if(uniqueContigs.contains(contig)) contigMap.put(contig, rawContigSequences.size());
			}
			rawContigSequences.add(buf);
		}
		fnaStream.close();
		
		/*
		 * Step 3. Run through genes
		 */
		Map<String, Integer> uniqueContigMap = new HashMap<String, Integer>();
		String[] fixedContigs = new String[uniqueContigs.size()];
		for(int i = 0; i < uniqueContigs.size(); i++) {
			uniqueContigMap.put(uniqueContigs.get(i), i);
			fixedContigs[i] = "";
		}
		
		int xcnt = 0;
		for(Prediction prediction : predictions) {
			// obtain cDNA sequence
			for(int i = 0; i < prediction.slocs.size(); i++) {
				prediction.rawSeq += subContig(prediction.contig, prediction.slocs.get(i), prediction.elocs.get(i));
			}
			
			prediction.fixSeq = OrfHotFixProcess.fixORF(
					prediction.pseq, prediction.rawSeq, prediction.strand, this.acc, "g" + String.valueOf(prediction.ID)).toUpperCase();
			if(prediction.fixSeq.contains("X")) {
				xcnt++;
				prediction.fixSeq = prediction.fixSeq.replace('X', 'N');
			}

			prediction.fsloc = fixedContigs[uniqueContigMap.get(prediction.contig)].length();
			prediction.feloc = prediction.fsloc + prediction.fixSeq.length();
			fixedContigs[uniqueContigMap.get(prediction.contig)] += prediction.fixSeq;
			
			Prompt.dynamic("\r");
			Prompt.dynamicHeader(String.format("Processing... [%d/%d]", prediction.ID, predictions.size()));
		}
		System.out.println("");
		Prompt.print(String.format("%d out of %d genes contain X nucleotide.", xcnt, predictions.size()));
		
		/*
		 * Step 4. Write a new GFF3 file using new data
		 */
		Prompt.print("Writing a GFF file for Roary on : " + ANSIHandler.wrapper(OPATH, 'y'));
		FileStream outStream = new FileStream(OPATH, 'w');
		
		outStream.println("##gff-version 3");
		for(int i = 0; i < uniqueContigs.size(); i++) outStream.println(String.format(
				"##sequence-region %s %d %d",
				uniqueContigs.get(i), 1, fixedContigs[i].length()));
		
		for(Prediction prediction : predictions) {
			outStream.println(String.format(
					"%s\tAUGUSTUS:3.3.2\tCDS\t%d\t%d\t.\t%s\t0\tID=%s_g%05d;inference=ab initio prediction:AUGUSTUS:3.3.2;locus_tag=%s_g%05d;product=fungal protein",
					prediction.contig, prediction.fsloc + 1, prediction.feloc, prediction.strand, this.acc, prediction.ID, this.acc, prediction.ID));
		}
		
		outStream.println("##FASTA");
		for(int i = 0; i < uniqueContigs.size(); i++) {
			outStream.println(">" + uniqueContigs.get(i));
			int j;
			for(j = 0; j + 60 < fixedContigs[i].length(); j += 60) {
				outStream.println(fixedContigs[i].substring(j, j + 60));
			}
			outStream.println(fixedContigs[i].substring(j));
		}
		
		outStream.close();
	}
	
	public static void main(String[] args) throws Exception {
		if(args.length != 3) {
			System.err.println("USAGE : RGB.jar [AUG] [FNA] [OUT]");
			System.err.println("\t[AUG] : AUGUSTUS prediction result (GFF3)");
			System.err.println("\t[FNA] : Genome assembly (FASTA)");
			System.err.println("\t[OUT] : Output file for Roary run (GFF3)");
			System.exit(1);
		}
		
		RoaryGffBuildProcess rgb = new RoaryGffBuildProcess(args[0], args[1], args[2]);
		GenericConfig.setHeader("RoaryGffBuilder");
		GenericConfig.TSTAMP = true;
		rgb.run();
	}
}
