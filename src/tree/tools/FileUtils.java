package tree.tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

public class FileUtils {

	private static final int COMPRESSION_LEVEL = 8;
	private static final int BUFFER_SIZE = 1024 * 2;
	
	public static boolean gunzipIt(String inputFilePath, String outFilePath) {

		byte[] buffer = new byte[40960];

		try {

			GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(inputFilePath));

			FileOutputStream out = new FileOutputStream(outFilePath);

			int len;
			while ((len = gzis.read(buffer)) > 0) {
				out.write(buffer, 0, len);
			}

			gzis.close();
			out.close();
		} catch (IOException ex) {
			return false;
		}
		return true;
	}

	public static boolean gunzipIt(String inputFilePath, String outFilePath, boolean inputDelete) {

		byte[] buffer = new byte[40960];
		GZIPInputStream gzis = null;
		FileOutputStream out = null;

		try {

			gzis = new GZIPInputStream(new FileInputStream(inputFilePath));

			out = new FileOutputStream(outFilePath);

			int len;
			while ((len = gzis.read(buffer)) > 0) {
				out.write(buffer, 0, len);
			}

		} catch (IOException ex) {
			return false;
		} finally {
			IOUtils.closeQuietly(gzis);
			IOUtils.closeQuietly(out);
		}

		if (inputDelete) {
			File zipFile = new File(inputFilePath);
			File unZipFile = new File(outFilePath);
			if (zipFile.exists() && unZipFile.exists()) {
				zipFile.delete();
			}
		}
		return true;
	}

	public static String createMonthDirString() {
		return new SimpleDateFormat("yyyy-MM-dd").format(System.currentTimeMillis());
	}

	public static byte[] file2bytes(File f) {
		FileInputStream fileInputStream = null;

		byte[] bFile = new byte[(int) f.length()];

		try {
			// convert file into array of bytes
			fileInputStream = new FileInputStream(f);
			fileInputStream.read(bFile);
			fileInputStream.close();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return bFile;
	}

	public static boolean bytes2file(byte[] data, String file_name) {
		FileOutputStream fileOuputStream = null;

		try {
			fileOuputStream = new FileOutputStream(file_name);
			fileOuputStream.write(data);
			fileOuputStream.close();
		} catch (Exception e) {
			return false;
		}

		return true;
	}

	public static void main(String[] args) {
		System.out.println(FileUtils.createMonthDirString());
	}

	public static boolean isGZipFile(File file) {
		InputStream in = null;
		try {
			in = new FileInputStream(file);
		} catch (FileNotFoundException e1) {
			IOUtils.closeQuietly(in);
			return false;
		}

		if (!in.markSupported()) {
			in = new BufferedInputStream(in);
		}
		in.mark(2);
		int magic = 0;
		try {
			magic = in.read() & 0xff | ((in.read() << 8) & 0xff00);
			in.reset();
		} catch (IOException e) {
			e.printStackTrace(System.err);
			return false;
		} finally {
			IOUtils.closeQuietly(in);
		}
		return magic == GZIPInputStream.GZIP_MAGIC;
	}

	public static boolean isFastaFile(File rawFile) {
		BufferedReader br = null;
		String firstLine = null;
		try {
			FileReader fr = new FileReader(rawFile);
			br = new BufferedReader(fr);
			firstLine = br.readLine();
			if (StringUtils.isEmpty(firstLine)) {
				return false;
			}
		} catch (Exception ex) {
			return false;
		} finally {
			IOUtils.closeQuietly(br);
		}

		if (firstLine.startsWith(">")) {
			return true;
		} else if (firstLine.startsWith("@")) {
			return false;
		} else {
			return false;
		}
	}

	public static boolean isFastqFile(File rawFile) {
		BufferedReader br = null;
		String firstLine = null;
		try {
			FileReader fr = new FileReader(rawFile);
			br = new BufferedReader(fr);
			firstLine = br.readLine();
			if (StringUtils.isEmpty(firstLine)) {
				return false;
			}
		} catch (Exception ex) {
			return false;
		} finally {
			IOUtils.closeQuietly(br);
		}

		if (firstLine.startsWith(">")) {
			return false;
		} else if (firstLine.startsWith("@")) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isBlastDbDNA(String fasta_file_name) {
		File f = new File(fasta_file_name + ".nsq");
		if (!f.exists())
			return false;
		if (f.length() > 0)
			return true;
		return false;
	}

	public static boolean isBlastDbProtein(String fasta_file_name) {
		File f = new File(fasta_file_name + ".psq");
		if (!f.exists())
			return false;
		if (f.length() > 0)
			return true;
		return false;
	}	

	/**
	 * �??��?�� ?��?���? Zip ?��?���? ?��축한?��.
	 * 
	 * @param sourcePath
	 *            - ?���? ???�� ?��?��?���?
	 * @param output
	 *            - ???�� zip ?��?�� ?���?
	 * @throws Exception
	 */
	public static void zip(String sourcePath, String output) throws Exception {

		// ?���? ???��(sourcePath)?�� ?��?��?��리나 ?��?��?�� ?��?���? 리턴?��?��.
		File sourceFile = new File(sourcePath);
		if (!sourceFile.isFile() && !sourceFile.isDirectory()) {
			throw new Exception("?���? ???��?�� ?��?��?�� 찾을 ?���? ?��?��?��?��.");
		}

		// output ?�� ?��?��?���? zip?�� ?��?���? 리턴?��?��.
		if (!(StringUtils.substringAfterLast(output, ".")).equalsIgnoreCase("zip")) {
			throw new Exception("?���? ?�� ???�� ?��?��명의 ?��?��?���? ?��?��?��?��?��");
		}

		FileOutputStream fos = null;
		BufferedOutputStream bos = null;
		ZipOutputStream zos = null;

		try {
			fos = new FileOutputStream(output); // FileOutputStream
			bos = new BufferedOutputStream(fos); // BufferedStream
			zos = new ZipOutputStream(bos); // ZipOutputStream
			zos.setLevel(COMPRESSION_LEVEL); // ?���? ?���? - 최�? ?��축률?? 9, ?��?��?�� 8
			zipEntry(sourceFile, sourcePath, zos); // Zip ?��?�� ?��?��
			zos.finish(); // ZipOutputStream finish
		} finally {
			if (zos != null) {
				zos.close();
			}
			if (bos != null) {
				bos.close();
			}
			if (fos != null) {
				fos.close();
			}
		}
	}


	private static void zipEntry(File sourceFile, String sourcePath, ZipOutputStream zos) throws Exception {
		if (sourceFile.isDirectory()) {
			if (sourceFile.getName().equalsIgnoreCase(".metadata")) {
				return;
			}
			File[] fileArray = sourceFile.listFiles(); 
			for (int i = 0; i < fileArray.length; i++) {
				zipEntry(fileArray[i], sourcePath, zos); 
			}
		} else {
			BufferedInputStream bis = null;
			try {
				String zipEntryName = sourceFile.getName();

				bis = new BufferedInputStream(new FileInputStream(sourceFile));
				ZipEntry zentry = new ZipEntry(zipEntryName);
				zentry.setTime(sourceFile.lastModified());
				zos.putNextEntry(zentry);

				byte[] buffer = new byte[BUFFER_SIZE];
				int cnt = 0;
				while ((cnt = bis.read(buffer, 0, BUFFER_SIZE)) != -1) {
					zos.write(buffer, 0, cnt);
				}
				zos.closeEntry();
			} finally {
				if (bis != null) {
					bis.close();
				}
			}
		}
	}

	public static void unzip(File zipFile, File targetDir, boolean fileNameToLowerCase) throws Exception {
		FileInputStream fis = null;
		ZipInputStream zis = null;
		ZipEntry zentry = null;

		try {
			fis = new FileInputStream(zipFile); // FileInputStream
			zis = new ZipInputStream(fis); // ZipInputStream

			while ((zentry = zis.getNextEntry()) != null) {
				String fileNameToUnzip = zentry.getName();
				if (fileNameToLowerCase) { // fileName toLowerCase
					fileNameToUnzip = fileNameToUnzip.toLowerCase();
				}

				File targetFile = new File(targetDir, fileNameToUnzip);

				if (zentry.isDirectory()) {// Directory ?�� 경우
					targetFile.mkdir(); // ?��?��?���? ?��?��
				} else { // File ?�� 경우
					// parent Directory ?��?��
					targetFile.mkdir();
					unzipEntry(zis, targetFile);
				}
			}
		} finally {
			if (zis != null) {
				zis.close();
			}
			if (fis != null) {
				fis.close();
			}
		}
	}

	/**
	 * Zip ?��?��?�� ?�� �? ?��?��리의 ?��축을 ?��?��.
	 *
	 * @param zis
	 *            - Zip Input Stream
	 * @param filePath
	 *            - ?���? ??�? ?��?��?�� 경로
	 * @return
	 * @throws Exception
	 */
	protected static File unzipEntry(ZipInputStream zis, File targetFile) throws Exception {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(targetFile);

			byte[] buffer = new byte[BUFFER_SIZE];
			int len = 0;
			while ((len = zis.read(buffer)) != -1) {
				fos.write(buffer, 0, len);
			}
		} finally {
			if (fos != null) {
				fos.close();
			}
		}
		return targetFile;
	}

	static public String readTextFile2StringWithCR(String filename)  // with carrige return
	{
		StringBuffer sb = new StringBuffer("");
		
		try {
			FileReader fr = new FileReader(new File(filename));
			BufferedReader br = new BufferedReader(fr);
		String line = null;
		while ((line = br.readLine()) != null)
		{
			sb.append(line+"\n");
		}
		br.close();
		fr.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new String(sb);
	}
	
	static public String readTextFile2StringWithCR(String filename,char CR_char)  // with carrige return
	{
		StringBuffer sb = new StringBuffer("");
		
		try {
			FileReader fr = new FileReader(new File(filename));
			BufferedReader br = new BufferedReader(fr);
		String line = null;
		while ((line = br.readLine()) != null)
		{
			sb.append(line+CR_char);
		}
		br.close();
		fr.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new String(sb);
	}

}
