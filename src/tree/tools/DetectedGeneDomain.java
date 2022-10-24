package tree.tools;

import java.io.Serializable;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

public class DetectedGeneDomain implements Serializable {
	private static final long serialVersionUID = 830018460048146372L;

//	private static final String[] keyList = {"protein","dna","evalue","bitscore"};

//	private String geneName = null;

	private String dna = null;
	private String protein = null;

	public DetectedGeneDomain(JSONObject jsonObject) {

		try {
			String key;
			Iterator<String> iterator = jsonObject.keys();

			while (iterator.hasNext()) {
				key = iterator.next();
				switch (key) {
					case "protein":
						this.protein = (String) jsonObject.get(key);
						break;

					case "dna":
						this.dna = (String) jsonObject.get(key);
						break;

					case "evalue":
					case "bitscore":
						break;
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

	}

	/*
	 *  getters & setters
	 */
	/*
	public String getGeneName() {return geneName;}
	public void setGeneName(String geneName) {
		this.geneName = geneName;
	}
	public double geteValue() {
		return eValue;
	}
	public void seteValue(double eValue) {
		this.eValue = eValue;
	}
	public double getBitScore() {
		return bitScore;
	}
	public void setBitScore(double bitScore) {
		this.bitScore = bitScore;
	}
	*/
	public String getDna() {
		return dna;
	}

	// public void setDna(String dna) {this.dna = dna;}
	public String getProtein() {
		return protein;
	}

	public void setProtein(String protein) {
		this.protein = protein;
	}
	
	/*
	public JSONObject toJsonObject(){
		
		JSONObject detectedGeneJO = new JSONObject();
		
		try {
			detectedGeneJO.put("protein", protein);
			detectedGeneJO.put("dna", dna);
			detectedGeneJO.put("eValue", eValue);
			detectedGeneJO.put("bitscore", bitScore);
		}catch(JSONException e) {
			e.printStackTrace();
		}	
		
		return detectedGeneJO;
		
	}
	*/
}