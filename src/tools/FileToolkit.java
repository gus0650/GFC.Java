package gfc.tools;

import java.io.*;
import java.util.Vector;

public class FileToolkit {

	/**
	 * Like allFilesIn(), but only files -- directories themselves can't be read or merged.
	 */
	public static String[] onlyFilesIn(String path) {
		String[] all = allFilesIn(path);
		Vector v = new Vector();
		for (int i = 0; i < all.length; i++) {
			if (new File(all[i]).isFile()) v.add(all[i]);
		}
		String[] files = new String[v.size()];
		v.copyInto(files);
		return files;
	}


	public static String[] allFilesIn(String path) {
		Vector v = new Vector();

		walk(path, v);

		String[] filenames = new String[v.size()];
		v.copyInto(filenames);
		
		return filenames;
	}


	public static void walk(String filename, Vector v) {
		v.add(filename);

		File f = new File(filename);
		if (f.isDirectory()) {
			String list[] = f.list();
			for (int i = 0; i < list.length; i++)
				walk(filename + "/" + list[i], v);
		}
	}
	

	public static StringBuffer readFromFile(String filename) {
		StringBuffer sb = new StringBuffer();
		System.out.print("reading from file " + filename + "\n");
		try {
			//read the file exactly as it is (readLine() added a line break after the last line too)
			RandomAccessFile file = new RandomAccessFile(filename, "r");
			byte[] b = new byte[(int)file.length()];
			file.readFully(b);
			file.close();

			//ISO-8859-1: one char per byte, writeToFile() writes them back unchanged
			sb.append( new String(b, "ISO-8859-1") );
		} catch (IOException e) {
			e.printStackTrace();
		}
		return sb;
	}

	
	public static void writeToFile(StringBuffer sb, String filename) {
		System.out.print("writing to file " + filename + "\n");
		try {
			RandomAccessFile file = new RandomAccessFile(filename, "rw");
			file.setLength(0);	//remove old content that is longer than the new one
			file.writeBytes(sb.toString());
			file.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static boolean existsFile(String f) {
		File file = new File(f);

		return (file.exists());
	}
}
