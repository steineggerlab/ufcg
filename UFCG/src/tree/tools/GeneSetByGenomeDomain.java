package tree.tools;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;




public class GeneSetByGenomeDomain implements Serializable{


	/**
	 * 
	 */
	private static final long serialVersionUID = 4383968253887214194L;
	/*
	 * metadata about genome sequence
	 */
	private Long uid = Math.abs(new Random().nextLong());
	private String label = null;
	private String targetTaxon = null;
	private String accession = null;
	private String taxonName = null;
	private String ncbiName = null;
	private String strainName = null;
	private Boolean isTpyeStrain = null;
	private String strainProperty = null;
	private String taxonomy = null;
	
	/*
	 * about run
	 */
	private String runTime = null;
	private int targetGenes = 0;
	private String usedHmmProfile = null;
	private String targetGeneSet = null;
	private int totalDetectedGenes = 0;
	private int singleCopyGenes = 0;
	private int multipleCopyGenes = 0;
	private int paralogs = 0;
	
	/*
	 * data
	 */
	private HashMap<String, ArrayList<DetectedGeneDomain>> dataMap = new HashMap<>();
	
	

	/*
	 * formatting JSON
	 */
	public JSONObject toJsonObject(){
		

		JSONObject genomeInfoJO = new JSONObject();
		
		JSONObject runInfoJO = new JSONObject();
		
		JSONObject dataJO = new JSONObject();

		try {

			putJsonObjectValueNull(genomeInfoJO, "uid", uid);
			putJsonObjectValueNull(genomeInfoJO, "label", label);
			putJsonObjectValueNull(genomeInfoJO, "target_taxon", targetTaxon);
			putJsonObjectValueNull(genomeInfoJO, "accession", accession);
			putJsonObjectValueNull(genomeInfoJO, "taxon_name", taxonName);
			putJsonObjectValueNull(genomeInfoJO, "ncbi_name", ncbiName);
			putJsonObjectValueNull(genomeInfoJO, "strain_name", strainName);
			putJsonObjectValueNull(genomeInfoJO, "isTypeStrain", isTpyeStrain);
			putJsonObjectValueNull(genomeInfoJO, "strain_property", strainProperty);
			putJsonObjectValueNull(genomeInfoJO, "taxonomy", taxonomy);

			runInfoJO.put("n_paralogs", paralogs);
			runInfoJO.put("n_multiple_copy_genes", multipleCopyGenes);
			runInfoJO.put("n_single_copy_genes", singleCopyGenes);
			runInfoJO.put("n_total_detected_genes", totalDetectedGenes);
			runInfoJO.put("target_gene_set", targetGeneSet);
			runInfoJO.put("used_hmm_profile", usedHmmProfile);
			runInfoJO.put("n_target_genes", targetGenes);
			runInfoJO.put("run_time", runTime);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
			
		for(String gene : dataMap.keySet()){
			
			ArrayList<DetectedGeneDomain> dgdL = dataMap.get(gene);
			
			JSONArray dgdJL = new JSONArray();
			
			for(DetectedGeneDomain dgd: dgdL){
				JSONObject dgdJO = dgd.toJsonObject();
				dgdJL.put(dgdJO);
			}
			
			try {
				dataJO.put(gene, dgdJL);
			}catch(JSONException e2) {
				e2.printStackTrace();
			}
			
		}

		
		JSONObject geneSetByGenomeJO = new JSONObject();
		
		try {
			geneSetByGenomeJO.put("genome_info", genomeInfoJO);
			geneSetByGenomeJO.put("run_info", runInfoJO);
			geneSetByGenomeJO.put("data", dataJO);
		}catch(JSONException e3) {
			e3.printStackTrace();
		}
		
		return geneSetByGenomeJO;
	}

	
	public static GeneSetByGenomeDomain jsonToDomain(String geneSetJson) throws JSONException{
		
		GeneSetByGenomeDomain geneSet = new GeneSetByGenomeDomain();
		
		JSONObject geneSetJO = stringToJsonObject(geneSetJson);
		
		geneSet = jsonToDomain(geneSetJO);
		
		return geneSet;
	}
	
	public static GeneSetByGenomeDomain jsonToDomain(JSONObject geneSetJO) throws JSONException{
		
		GeneSetByGenomeDomain geneSet = new GeneSetByGenomeDomain();
		
		
		JSONObject genomeInfoJO = (JSONObject) geneSetJO.get("genome_info");
		
		JSONObject runInfoJO = (JSONObject) geneSetJO.get("run_info");
		
		JSONObject dataJO = (JSONObject) geneSetJO.get("data");
		
		//Mandatory
		Object object = genomeInfoJO.get("uid");
			long uid = 0;
		if(object instanceof Integer) {
			uid = (long) (int) object;
			geneSet.setUid(uid);
		}else {
			geneSet.setUid((long)genomeInfoJO.get("uid"));
		}
		
		geneSet.setLabel((String)genomeInfoJO.get("label"));
		
		
		//Optional
		if(containKey(genomeInfoJO, "target_taxon")) {
			Object target_taxon = genomeInfoJO.get("target_taxon");
			if(target_taxon==JSONObject.NULL) {
				geneSet.setTargetTaxon(null);
			}else {
				geneSet.setTargetTaxon((String)target_taxon);
			}
			
		}
		
		if(containKey(genomeInfoJO,"accession")) {
			Object accession = genomeInfoJO.get("accession");
			if(accession==JSONObject.NULL) {
				geneSet.setAccession(null);
			}else {
				geneSet.setAccession((String)accession);
			}
			
		}
		if(containKey(genomeInfoJO,"taxon_name")) {
			Object taxon_name = genomeInfoJO.get("taxon_name");
			if(taxon_name==JSONObject.NULL) {
				geneSet.setTaxonName(null);
			}else {
				geneSet.setTaxonName((String)taxon_name);
			}
			
		}
		if(containKey(genomeInfoJO,"ncbi_name")) {
			Object ncbi_name = genomeInfoJO.get("ncbi_name");
			if(ncbi_name==JSONObject.NULL) {
				geneSet.setNcbiName(null);
			}else {
				geneSet.setNcbiName((String)ncbi_name);
			}
			
		}
		if(containKey(genomeInfoJO,"strain_name")) {
			Object strain_name = genomeInfoJO.get("strain_name");
			if(strain_name==JSONObject.NULL) {
				geneSet.setStrainName(null);
			}else {
				geneSet.setStrainName((String)strain_name);
			}
			
		}
		if (containKey(genomeInfoJO, "isTypeStrain")) {
			Object isTypeStrain = genomeInfoJO.get("isTypeStrain");
			if(isTypeStrain==JSONObject.NULL) {
				geneSet.setIsTpyeStrain(null);
			}else {
				geneSet.setIsTpyeStrain((boolean)isTypeStrain);
			}
			
		}
		if (containKey(genomeInfoJO, "strain_property")) {
			Object strain_property = genomeInfoJO.get("strain_property");
			if(strain_property==JSONObject.NULL) {
				geneSet.setStrainPproperty(null);
			}else {
				geneSet.setStrainPproperty((String)strain_property);
			}
			
		}
		if (containKey(genomeInfoJO, "taxonomy")) {
			
			Object taxonomy = genomeInfoJO.get("taxonomy");
			if(taxonomy==JSONObject.NULL) {
				geneSet.setTaxonomy(null);
			}else {
				geneSet.setTaxonomy((String)taxonomy);
			}
			
		}
		
		
		
		if (containKey(runInfoJO, "run_time")) {
			geneSet.setRunTime((String)runInfoJO.get("run_time"));
		}
		if (containKey(runInfoJO, "run_time")) {
			geneSet.setTargetGenes((int) runInfoJO.get("n_target_genes"));	
		}
		if (containKey(runInfoJO, "target_gene_set")) {
			geneSet.setTargetGeneSet((String) runInfoJO.get("target_gene_set"));
		}
		if (containKey(runInfoJO, "n_total_detected_genes")) {
			geneSet.setTotalDetectedGenes((int) runInfoJO.get("n_total_detected_genes"));
		}
		if (containKey(runInfoJO, "n_single_copy_genes")) {
			geneSet.setSingleCopyGenes((int) runInfoJO.get("n_single_copy_genes"));
		}
		if (containKey(runInfoJO, "n_multiple_copy_genes")) {
			geneSet.setMultipleCopyGenes((int) runInfoJO.get("n_multiple_copy_genes"));
		}
		if (containKey(runInfoJO, "n_paralogs")) {
			geneSet.setParalogs((int) runInfoJO.get("n_paralogs"));
		}
		if (containKey(runInfoJO, "used_hmm_profile")) {
			geneSet.setUsedHmmProfile((String) runInfoJO.get("used_hmm_profile"));
		}
		
		HashMap<String, ArrayList<DetectedGeneDomain>> dataMap = new HashMap<String, ArrayList<DetectedGeneDomain>>();
		
		Iterator<String> dataIterator = dataJO.keys();
		
		while(dataIterator.hasNext()) {
			String geneName = (String) dataIterator.next();
			JSONArray jsonArray = (JSONArray) dataJO.get(geneName);
			
			ArrayList<DetectedGeneDomain> detectedGeneDomainList = new ArrayList<DetectedGeneDomain>();
			
			for(int i=0; i<jsonArray.length(); i++) {
				JSONObject jsonObject = (JSONObject) jsonArray.get(i);
				DetectedGeneDomain detectedGeneDomain = new DetectedGeneDomain(jsonObject);
				detectedGeneDomainList.add(detectedGeneDomain);
			}
			
			dataMap.put(geneName, detectedGeneDomainList);
			
		}
		
		geneSet.setDataMap(dataMap);
		
		return geneSet;
		
	}
	
	
	private static JSONObject stringToJsonObject(String geneSetJson) throws JSONException{
		
		JSONObject geneSetJO = new JSONObject(geneSetJson);
		
		return geneSetJO;
		
	}
	
	private static boolean containKey(JSONObject jsonObject, String key) {
		return jsonObject.keySet().contains(key);
	}
	
	private static void putJsonObjectValueNull(JSONObject jsonObject, String key, Object object) {
		if(object!=null) {
			jsonObject.put(key, object);
		}else {
			jsonObject.put(key, JSONObject.NULL);
		}
	}

	public String getStrainProperty() {
		return strainProperty;
	}


	public void setStrainProperty(String strainProperty) {
		this.strainProperty = strainProperty;
	}


	public String getUsedHmmProfile() {
		return usedHmmProfile;
	}


	public void setUsedHmmProfile(String usedHmmProfile) {
		this.usedHmmProfile = usedHmmProfile;
	}


	public Long getUid() {
		return uid;
	}

	public void setUid(Long uid) {
		this.uid = uid;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getTargetTaxon() {
		return targetTaxon;
	}

	public void setTargetTaxon(String targetTaxon) {
		this.targetTaxon = targetTaxon;
	}

	public String getAccession() {
		return accession;
	}

	public void setAccession(String accession) {
		this.accession = accession;
	}

	public String getTaxonName() {
		return taxonName;
	}

	public void setTaxonName(String taxonName) {
		this.taxonName = taxonName;
	}

	public String getNcbiName() {
		return ncbiName;
	}

	public void setNcbiName(String ncbiName) {
		this.ncbiName = ncbiName;
	}

	public String getStrainName() {
		return strainName;
	}

	public void setStrainName(String strainName) {
		this.strainName = strainName;
	}

	public Boolean getIsTpyeStrain() {
		return isTpyeStrain;
	}

	public void setIsTpyeStrain(Boolean isTpyeStrain) {
		this.isTpyeStrain = isTpyeStrain;
	}

	public String getStrainPproperty() {
		return strainProperty;
	}

	public void setStrainPproperty(String strainPproperty) {
		this.strainProperty = strainPproperty;
	}

	public String getTaxonomy() {
		return taxonomy;
	}

	public void setTaxonomy(String taxonomy) {
		this.taxonomy = taxonomy;
	}

	public String getRunTime() {
		return runTime;
	}

	public void setRunTime(String runTime) {
		this.runTime = runTime;
	}

	public int getTargetGenes() {
		return targetGenes;
	}

	public void setTargetGenes(int targetGenes) {
		this.targetGenes = targetGenes;
	}

	public String getTargetGeneSet() {
		return targetGeneSet;
	}

	public void setTargetGeneSet(String targetGeneSet) {
		this.targetGeneSet = targetGeneSet;
	}

	public int getTotalDetectedGenes() {
		return totalDetectedGenes;
	}

	public void setTotalDetectedGenes(int totalDetectedGenes) {
		this.totalDetectedGenes = totalDetectedGenes;
	}

	public int getSingleCopyGenes() {
		return singleCopyGenes;
	}

	public void setSingleCopyGenes(int singleCopyGenes) {
		this.singleCopyGenes = singleCopyGenes;
	}

	public int getMultipleCopyGenes() {
		return multipleCopyGenes;
	}

	public void setMultipleCopyGenes(int multipleCopyGenes) {
		this.multipleCopyGenes = multipleCopyGenes;
	}

	public int getParalogs() {
		return paralogs;
	}

	public void setParalogs(int paralogs) {
		this.paralogs = paralogs;
	}

	public HashMap<String, ArrayList<DetectedGeneDomain>> getDataMap() {
		return dataMap;
	}

	public void setDataMap(HashMap<String, ArrayList<DetectedGeneDomain>> dataMap) {
		this.dataMap = dataMap;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

}
