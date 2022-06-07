package entity;

import java.util.List;
import java.util.ArrayList;

import envs.config.GenericConfig;
import envs.config.PathConfig;
import envs.toolkit.ANSIHandler;
import envs.toolkit.FileStream;
import envs.toolkit.Prompt;
import envs.toolkit.Shell;
import pipeline.ExceptionHandler;

public class QueryEntity {
	public static int EX_FILENAME = -1, EX_LABEL = -1, EX_ACCESS = -1, EX_TAXON = -1, EX_NCBI = -1, EX_STRAIN = -1, EX_TAXONOMY = -1;
	private static List<List<String>> METADATA = new ArrayList<List<String>>();
	public static void importMetadata() throws java.io.IOException {
		if(!PathConfig.MetaExists) {
			Prompt.talk("No metadata file given.");
			return;
		}
		
		Prompt.talk("Importing given metadata file : " + ANSIHandler.wrapper(PathConfig.MetaPath, 'B'));
		FileStream metaStream = new FileStream(PathConfig.MetaPath, 'r');
		String buf = metaStream.readLine();
		
		// parse header
		String[] headers = buf.split("\t");
		for(int i = 0; i < headers.length; i++) {
			String header = headers[i];
			if(header.equals("filename"))			EX_FILENAME = i;
			else if(header.equals("label")) 		EX_LABEL = i;
			else if(header.equals("accession")) 	EX_ACCESS = i;
			else if(header.equals("taxon_name")) 	EX_TAXON = i;
			else if(header.equals("ncbi_name")) 	EX_NCBI = i;
			else if(header.equals("strain_name")) 	EX_STRAIN = i;
			else if(header.equals("taxonomy")) 		EX_TAXONOMY = i;
			else {
				ExceptionHandler.pass(header);
				ExceptionHandler.handle(ExceptionHandler.INVALID_META_HEADER);
			}
		}
		
		if(EX_FILENAME < 0) {
			ExceptionHandler.pass("filename");
			ExceptionHandler.handle(ExceptionHandler.INSUFFICIENT_METADATA);
		}
		if(EX_LABEL < 0) {
			ExceptionHandler.pass("label");
			ExceptionHandler.handle(ExceptionHandler.INSUFFICIENT_METADATA);
		}
		if(EX_ACCESS < 0) {
			ExceptionHandler.pass("accession");
			ExceptionHandler.handle(ExceptionHandler.INSUFFICIENT_METADATA);
		}
		
		// parse elements
		while((buf = metaStream.readLine()) != null) {
			if(buf.length() == 0) break;
			List<String> data = new ArrayList<String>();
			String[] values = buf.split("\t");
			if(headers.length != values.length) ExceptionHandler.handle(ExceptionHandler.INVALID_METADATA);
			
			for(String value : values) {
				if(value.equals("null") || value.equals("NULL")) data.add(null);
				else data.add(value);
			}
			METADATA.add(data);
		}
		
		Prompt.talk("Metadata file with " + String.valueOf(METADATA.size()) + " entities successfully imported.");
	}
	
	public static int testIntegrity() throws java.io.IOException {
		/* Input data and metadata integrity test
		 * 		Rule 1. Input files must have identical extension.
		 * 		Rule 2. The number of input files must be identical to the number of metadata entities.
		 * 		Rule 3. All filenames must exist in the metadata.
		 * 		Rule 4. Input files must share same file type.
		 */
		Prompt.print("Reading input data...");
		List<String> fnames = new ArrayList<String>();
		
		// fetch filenames from input directory
		if(PathConfig.InputIsFolder) {
			String cmd = "ls -1 " + PathConfig.InputPath + " > " + PathConfig.TempPath + GenericConfig.TEMP_HEADER + "file.list";
			Shell.exec(cmd);
			FileStream tmpListStream = new FileStream(PathConfig.TempPath + GenericConfig.TEMP_HEADER + "file.list", 'r');
			tmpListStream.isTemp();
			String buf;
			while((buf = tmpListStream.readLine()) != null) fnames.add(buf);
			tmpListStream.close();
			tmpListStream.wipe(true);
			
			// Rule 1
			String ext = fnames.get(0).substring(fnames.get(0).lastIndexOf(".") + 1);
			for(String fname : fnames) {
				if(!fname.endsWith(ext)) return 1;
			}
		}
		else fnames.add(PathConfig.InputPath.substring(PathConfig.InputPath.lastIndexOf("/") + 1));
		
		// if metadata not given
		if(!PathConfig.MetaExists) {
			METADATA = new ArrayList<List<String>>();
			// if metadata information is given as a string
			if(PathConfig.MetaString != null) {
				EX_FILENAME = 0; EX_LABEL = 1; EX_ACCESS = 2; EX_TAXON = 3; EX_NCBI = 4; EX_STRAIN = 5; EX_TAXONOMY = 6;
				
				String[] metaContainer = PathConfig.MetaString.split(",", -1);
				for(String meta : metaContainer) Prompt.debug(meta);
				if(metaContainer.length < 7) {
					Prompt.warn("Metadata information is improperly formatted.");
					String[] tmpContainer = new String[7];
					for(int i = 0; i < metaContainer.length; i++) tmpContainer[i] = metaContainer[i];
					for(int i = metaContainer.length; i < 7; i++) tmpContainer[i] = "";
					metaContainer = tmpContainer;
				}
				else if(metaContainer.length > 7) ExceptionHandler.handle(ExceptionHandler.INVALID_METAINFO);

				metaContainer[0] = PathConfig.InputPath.substring(PathConfig.InputPath.lastIndexOf("/") + 1);
				for(int i = 1; i < 7; i++) {
					if(metaContainer[i].length() == 0 || metaContainer[i].equals("null"))
						metaContainer[i] = null;
				}
				
				List<String> metaList = java.util.Arrays.asList(metaContainer);
				if(metaList.get(EX_LABEL) == null) {
					ExceptionHandler.pass("label");
					ExceptionHandler.handle(ExceptionHandler.INSUFFICIENT_METADATA);
				}
				if(metaList.get(EX_ACCESS) == null) {
					ExceptionHandler.pass("accession");
					ExceptionHandler.handle(ExceptionHandler.INSUFFICIENT_METADATA);
				}
				
				METADATA.add(metaList);
			}
			else { // if not, create temporary one
				Prompt.talk("Creating temporary metadata from filenames : " +
						ANSIHandler.wrapper(PathConfig.TempPath + GenericConfig.TEMP_HEADER + "meta_tmp.tsv", 'B'));
			
				FileStream tmpMetaStream = new FileStream(PathConfig.TempPath + GenericConfig.TEMP_HEADER + "meta_tmp.tsv", 'w');
				tmpMetaStream.isTemp();
			
				tmpMetaStream.println("filename\tlabel\taccession");
				for(String fname : fnames) {
					String acc = fname.substring(0, fname.lastIndexOf("."));
					tmpMetaStream.println(String.format("%s\t%s\t%s", fname, acc, acc));
				}
				tmpMetaStream.close();
			
				PathConfig.MetaExists = true;
				PathConfig.setMetaPath(tmpMetaStream.PATH);
				importMetadata();
				tmpMetaStream.wipe(true);
			}
		}

		// Rule 2
		if(fnames.size() < METADATA.size()) return 2;
		
		String ftype = Shell.exec("file -b " + PathConfig.InputPath + (PathConfig.InputIsFolder ? fnames.get(0) : ""))[0];
		for(List<String> data : METADATA) {
			if(!fnames.contains(data.get(EX_FILENAME))) return 3; // Rule 3
			if(!Shell.exec("file -b " + PathConfig.InputPath + (PathConfig.InputIsFolder ? data.get(EX_FILENAME) : ""))[0].equals(ftype)) return 4; // Rule 4
		}
		
		return 0;
	}
	
	public static List<QueryEntity> createQuery(){
		List<QueryEntity> queries = new ArrayList<QueryEntity>();
		for(List<String> data : METADATA) queries.add(new QueryEntity(data));
		return queries;
	}
	
	public String filename, accession;
	private String label, taxon, ncbi, strain, taxonomy;
	public QueryEntity(List<String> data) {
		this.filename  = data.get(EX_FILENAME);
		this.label     = data.get(EX_LABEL);
		this.accession = data.get(EX_ACCESS);
		this.taxon     = EX_TAXON    >= 0 ? data.get(EX_TAXON) 	  : "none";
		this.ncbi      = EX_NCBI     >= 0 ? data.get(EX_NCBI)	  : "none";
		this.strain    = EX_STRAIN   >= 0 ? data.get(EX_STRAIN)	  : "none";
		this.taxonomy  = EX_TAXONOMY >= 0 ? data.get(EX_TAXONOMY) : "none";
	}
	
	private void initiateSystem() {
		GenericConfig.setSystem("", "", "none", "none", "none", "none", "none");
		FileStream.init();
	}
	
	public int checkResultFileExistence() {
		String[] exec = Shell.exec("file -b " + PathConfig.OutputPath + this.accession + ".ucg");
		if(exec[0].contains("text")) return 1;
		if(exec[0].contains("JSON")) return 1;
		if(exec[0].contains("data")) return 1;
		return 0;
	}
	
	// activate current query
	public void activate() {
		initiateSystem();
		GenericConfig.setSystem(filename, accession, label, taxon, ncbi, strain, taxonomy);
	}
}
