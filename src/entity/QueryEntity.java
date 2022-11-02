package entity;

import java.util.List;
import java.util.ArrayList;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import envs.config.GenericConfig;
import envs.config.PathConfig;
import envs.toolkit.ANSIHandler;
import envs.toolkit.FileStream;
import envs.toolkit.Prompt;
import envs.toolkit.Shell;
import pipeline.ExceptionHandler;

import org.apache.commons.io.FileUtils;

public class QueryEntity {
	public static int EX_FILENAME = -1, EX_LABEL = -1, EX_ACCESS = -1, EX_TAXON = -1, EX_NCBI = -1, EX_STRAIN = -1, EX_TAXONOMY = -1;
	private static List<List<String>> METADATA = new ArrayList<>();
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
			switch (header) {
				case "filename":
					EX_FILENAME = i;
					break;
				case "label":
					EX_LABEL = i;
					break;
				case "accession":
					EX_ACCESS = i;
					break;
				case "taxon_name":
					EX_TAXON = i;
					break;
				case "ncbi_name":
					EX_NCBI = i;
					break;
				case "strain_name":
					EX_STRAIN = i;
					break;
				case "taxonomy":
					EX_TAXONOMY = i;
					break;
				default:
					ExceptionHandler.pass(header);
					ExceptionHandler.handle(ExceptionHandler.INVALID_META_HEADER);
					break;
			}
		}
		
		if(EX_FILENAME < 0) {
			ExceptionHandler.pass("filename");
			ExceptionHandler.handle(ExceptionHandler.INSUFFICIENT_METADATA);
		}

		int line = headers.length;
		boolean no_label = false;
		if(EX_LABEL < 0) {
			no_label = true;
			EX_LABEL = line++;
			Prompt.warn("No label column found. Using filename as label.");
		}
		boolean no_access = false;
		if(EX_ACCESS < 0) {
			no_access = true;
			EX_ACCESS = line;
			Prompt.warn("No accession column found. Using filename as accession.");
		}
		
		// parse elements
		while((buf = metaStream.readLine()) != null) {
			if(buf.length() == 0) break;
			List<String> data = new ArrayList<>();
			String[] values = buf.split("\t");
			if(headers.length != values.length) ExceptionHandler.handle(ExceptionHandler.INVALID_METADATA);
			
			for(String value : values) {
				if(value.equals("null") || value.equals("NULL")) data.add(null);
				else data.add(value);
			}
			if(no_label) data.add(data.get(EX_FILENAME).substring(0, data.get(EX_FILENAME).lastIndexOf('.')));
			if(no_access) data.add(data.get(EX_FILENAME).substring(0, data.get(EX_FILENAME).lastIndexOf('.')));
			METADATA.add(data);
		}
		
		Prompt.talk("Metadata file with " + METADATA.size() + " entities successfully imported.");
	}
	
	public static int testIntegrity() throws java.io.IOException {
		/* Input data and metadata integrity test
		 *      Rule 1. There is no rule.
		 * 		[DEPRECATED] Rule 1. Input files must have identical extension.
		 * 		[DEPRECATED] Rule 2. The number of input files must be identical to the number of metadata entities.
		 * 		[DEPRECATED] Rule 3. All filenames must exist in the metadata.
		 * 		[DEPRECATED] Rule 4. Input files must share same file type.
		 */
		Prompt.print("Reading input data...");
		List<String> fnames = new ArrayList<>();

		// fetch filenames
		if(PathConfig.InputIsFolder){
			File dir = new File(PathConfig.InputPath);
			if(!dir.exists()) {
				ExceptionHandler.pass(PathConfig.InputPath);
				ExceptionHandler.handle(ExceptionHandler.INVALID_DIRECTORY);
			}
			for(File file : Objects.requireNonNull(dir.listFiles())) {
				if(file.isFile()) fnames.add(file.getName());
			}
		}
		else {
			File file = new File(PathConfig.InputPath);
			if(!file.exists()) {
				ExceptionHandler.pass(PathConfig.InputPath);
				ExceptionHandler.handle(ExceptionHandler.INVALID_FILE);
			}
			fnames.add(file.getName());
		}
		
		// if metadata not given
		if(!PathConfig.MetaExists) {
			METADATA = new ArrayList<>();
			// if metadata information is given as a string
			if(PathConfig.MetaString != null) {
				EX_FILENAME = 0; EX_LABEL = 1; EX_ACCESS = 2; EX_TAXON = 3; EX_NCBI = 4; EX_STRAIN = 5; EX_TAXONOMY = 6;
				
				String[] metaContainer = PathConfig.MetaString.split(",", -1);
				for(String meta : metaContainer) Prompt.debug(meta);
				if(metaContainer.length < 7) {
					Prompt.warn("Metadata information is improperly formatted.");
					String[] tmpContainer = new String[7];
					System.arraycopy(metaContainer, 0, tmpContainer, 0, metaContainer.length);
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
					metaList.set(EX_LABEL, metaList.get(EX_FILENAME));
					Prompt.warn("No label column found. Using filename as label.");
				}
				if(metaList.get(EX_ACCESS) == null) {
					metaList.set(EX_ACCESS, metaList.get(EX_FILENAME));
					Prompt.warn("No accession column found. Using filename as accession.");
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

		// remove metadata entities that are not in the input files
		List<List<String>> tmpMeta = new ArrayList<>();
		for(List<String> data : METADATA) {
			if(fnames.contains(data.get(EX_FILENAME))) tmpMeta.add(data);
			else Prompt.warn("Metadata entity " + ANSIHandler.wrapper(data.get(EX_FILENAME), 'B') + " is not in the input files.");
		}
		METADATA = tmpMeta;

		// add metadata entities that are not in the metadata
		for(String fname : fnames) {
			boolean found = false;
			for(List<String> data : METADATA) {
				if(data.get(EX_FILENAME).equals(fname)) {
					found = true;
					break;
				}
			}
			if(!found) {
				Prompt.warn("Input file " + ANSIHandler.wrapper(fname, 'B') + " is not in the metadata.");
				String acc = fname.substring(0, fname.lastIndexOf("."));

				List<String> data = new ArrayList<>();
				for(int i = 0; i < 7; i++) data.add("none");
				data.set(EX_FILENAME, fname);
				data.set(EX_LABEL, acc);
				data.set(EX_ACCESS, acc);
				METADATA.add(data);
			}
		}
		
		return 0;
	}
	
	public static List<QueryEntity> createQuery(){
		List<QueryEntity> queries = new ArrayList<>();
		for(List<String> data : METADATA) queries.add(new QueryEntity(data));
		return queries;
	}
	
	public String filename, accession;
	private final String label, taxon, ncbi, strain, taxonomy;
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
		
		// generate temporary directory for query
		File dir = new File(PathConfig.TempPath + File.separator + accession + File.separator);
		if(dir.exists()) {
			if(dir.isFile()) {
				ExceptionHandler.pass("Failed to create directory: " + ANSIHandler.wrapper(dir, 'B'));
				ExceptionHandler.handle(ExceptionHandler.ERROR_WITH_MESSAGE);
			}
			try{
				FileUtils.cleanDirectory(dir);
			} catch (IOException e) {
				ExceptionHandler.handle(e);
			}
		}
		else if(!dir.mkdirs()) {
			ExceptionHandler.pass("Failed to create directory: " + ANSIHandler.wrapper(dir, 'B'));
			ExceptionHandler.handle(ExceptionHandler.ERROR_WITH_MESSAGE);
		}
		PathConfig.OriginalTempPath = PathConfig.TempPath;
		PathConfig.setTempPath(dir.getAbsolutePath());
	}
	
	// deactivate current query
	public void deactivate() {
		// remove temporary directory and restore path
		try {
			if(!PathConfig.TempIsCustom) FileUtils.deleteDirectory(new File(PathConfig.TempPath));
			PathConfig.restoreTempPath();
		} catch (IOException e) {
			ExceptionHandler.handle(e);
		}
	}
}
