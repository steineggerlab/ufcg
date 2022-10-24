package entity;

import envs.config.GenericConfig;
import envs.toolkit.TimeKeeper;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import java.util.List;
import java.util.Random;

public class JsonProfileEntity {
	private final List<ProfilePredictionEntity> profiles;
	public JSONObject root;
	
	@SuppressWarnings("unchecked")
	public JsonProfileEntity(List<ProfilePredictionEntity> pps) {
		profiles = pps;
		root = new JSONObject();
		
		root.put("genome_info", new JSONObject());
		root.put("run_info", new JSONObject());
		root.put("data", new JSONObject());
		
		setGenomeInfo();
		setRunData();
	}
	
	@SuppressWarnings("unchecked")
	private void setGenomeInfo() {
		JSONObject giObj = (JSONObject) root.get("genome_info");
		
		giObj.put("uid", Math.abs(new Random().nextLong()));
		giObj.put("label", GenericConfig.LABEL);
		giObj.put("target_taxon", "Fungi");
		giObj.put("accession", GenericConfig.ACCESS);
		giObj.put("taxon_name", GenericConfig.TAXON);
		giObj.put("ncbi_name", GenericConfig.NCBI);
		giObj.put("strain_name", GenericConfig.STRAIN);
		giObj.put("isTypeStrain", false);
		giObj.put("strain_property", "none");
		giObj.put("taxonomy", GenericConfig.TAXONOMY);
	}
	
	@SuppressWarnings("unchecked")
	private void setRunData() {
		JSONObject riObj = (JSONObject) root.get("run_info");
		
		riObj.put("run_time", TimeKeeper.timeStampExtended());
		riObj.put("n_target_genes", profiles.size());
		
		StringBuilder gset = new StringBuilder(); int ndet = 0, nsgl = 0, nmul = 0;
		JSONObject dObj = (JSONObject) root.get("data");
		for(ProfilePredictionEntity pp : profiles) {
			String cg = pp.getTask();
			gset.append(cg).append(",");
			
			if(pp.opt < 0) continue;
			
			int nseq = pp.predGenes.size();
			ndet += nseq;
			if(nseq == 1) nsgl++;
			else nmul++;
			
			JSONArray cgArr = new JSONArray();
			for(int j = 0; j < nseq; j++) {
				JSONObject cgObj = new JSONObject();
				cgObj.put("protein", pp.predGenes.get(j));
				cgObj.put("dna", pp.predGseqs.get(j));
				cgObj.put("evalue", pp.evalues.get(j));
				cgObj.put("bitscore", pp.scores.get(j));
				cgArr.add(cgObj);
			}
			dObj.put(cg, cgArr);
		}
		
		riObj.put("target_gene_set", gset.substring(0, gset.length() - 1));
		riObj.put("n_total_detected_genes", ndet);
		riObj.put("n_single_copy_genes", nsgl);
		riObj.put("n_multiple_copy_genes", nmul);
		riObj.put("n_paralogs", 0);
	}
}
