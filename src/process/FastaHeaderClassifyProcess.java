package process;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import envs.toolkit.FileStream;
import pipeline.ExceptionHandler;

public class FastaHeaderClassifyProcess {
	public static void classify(String ifa, String head, String ofa, boolean accept) {
		try {
			String buf;
			
			// read headers and create hash map
			Map<String, Boolean> headerMap = new HashMap<>();
			FileStream hfs = new FileStream(head, 'r');
			while((buf = hfs.readLine()) != null) headerMap.put(buf.split(" ")[0], true);
			hfs.close();
			
			// read input FASTA and record hits
			FileStream ifs = new FileStream(ifa, 'r');
			FileStream ofs = new FileStream(ofa, 'w');
			while((buf = ifs.readLine()) != null) {
				if(!buf.startsWith(">")) continue;
				if(headerMap.containsKey(buf.split(" ")[0].substring(1))) {
					if(accept) {
						ofs.println(buf);
						ofs.println(ifs.readLine());
					}
				} else if(!accept) {
					ofs.println(buf);
					ofs.println(ifs.readLine());
				}
			}
			
			ifs.close();
			ofs.close();
			headerMap.clear();
			
		} catch(IOException e) {
			ExceptionHandler.handle(e);
		}
	}
}