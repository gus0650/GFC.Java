package gfc.util;

import java.io.*;


/**
 * Utility class that provides static methods for various i/o operations.
 */
final public class IOTool {

	/**
	 * Opens a resource like on the handset: "/res/x.bin" and "res/x.bin" are looked up in the classpath
	 * (also inside a .jar), then in the working directory.
	 * (byte[].class / String.class can't find the game's resources since Java 9.)
	 * 
	 * @return the stream, or null if not found
	 */
	public static InputStream open(String filename) {
		String path = filename.startsWith("/") ? filename : "/" + filename;

		InputStream is = IOTool.class.getResourceAsStream(path);
		if (is != null) return is;

		File f = new File( filename.startsWith("/") ? filename.substring(1) : filename );
		if (!f.exists()) f = new File(filename);

		try {
			if (f.exists()) return new FileInputStream(f);
		}
		catch (IOException e) {
			//not readable
		}
		return null;
	}

	/** skip() may skip less than asked for -- this skips exactly n bytes (or up to the end). */
	private static void skipFully(InputStream is, long n) throws IOException {
		while (n > 0) {
			long k = is.skip(n);
			if (k <= 0) {
				if (is.read() == -1) return;	//end of stream
				k = 1;
			}
			n -= k;
		}
	}

	/** reads until the array is full or the stream ends */
	private static int readFully(InputStream is, byte[] into) throws IOException {
		int done = 0;
		while (done < into.length) {
			int k = is.read(into, done, into.length - done);
			if (k == -1) break;
			done += k;
		}
		return done;
	}
	
	
	public static void loadFile(String filename, byte[] into) {
		//System.out.println("FileTool.load() " + filename);
		
		try {
			DataInputStream dis;
			
			dis = new DataInputStream( open( filename ) );
			dis.readFully(into);
			
			dis.close();
		}
		catch (IOException e) {
			/*config:debug:ON*/e.printStackTrace();
		}
	}

	public static void loadFile(String filename, byte[] into, int skip) {
		//System.out.println("FileTool.load() " + filename);
		
		try {
			DataInputStream dis;
			
			dis = new DataInputStream( open( filename ) );
			
			skipFully(dis, skip);
			
			readFully(dis, into);
			
			dis.close();
		}
		catch (IOException e) {
			/*config:debug:ON*/e.printStackTrace();
		}
	}

	/*
	public static byte[] load(String filename) {
		
		byte[] into = null;
		
		try {
			DataInputStream in;
			
			in = new DataInputStream( open( filename ) );
			
			into = new byte[ in.available() ];

			in.readFully(into);
			
			in.close();
		}
		catch (IOException e) {
			config:debug:ON e.printStackTrace();
		}
		
		return into;
	}
	*/
	
	public static int getFileSize(String filename) {
		//System.out.println("FileTool.getFileSize() " + filename);
		
		long fs = 0;

		try {
			InputStream is;

			is = open( filename );

			//count all bytes (skip() may stop early on some streams)
			byte[] buf = new byte[4096];
			int k;
			while ((k = is.read(buf)) != -1) fs += k;

			is.close();
		}
		catch (IOException e) {
			/*config:debug:ON*/e.printStackTrace();
		}
		
		return (int)fs;
	}
	
	
	/**
	 * Test if a file exists.
	 */
	public static boolean existsFile(String filename) {
    	try {
			InputStream is;

			is = open( filename );
			is.close();
    		
    		return true;
    	}
    	catch (Exception e) {
        	return false;
    	}
	}

	
	/**
	 * Read a chunk of data from a file.<p>
	 * 
	 * The data format of a chunk is 
	 * <li>int - length of chunk
	 * <li>byte[] - chunk data.<p>
	 * 
	 * This kind of file can be generated with the GFCFileConcat tool.
	*/
	public static byte[] readChunk(InputStream is, int chunkNum) {
		int len;
		byte[] data = null;

		try {
			for (int i = 0; i < chunkNum; i++) {
				len = is.read();
				skipFully(is, len);
			}
			
			len = is.read();
			data = new byte[len];
			
			readFully(is, data);
		}
		catch (Exception e) {
			/*config:debug:ON*/e.printStackTrace();
		}

		return data;
	}
	
	
	public static byte[] readChunk(String filename, int chunkNum) {
		byte[] data = null;
		
		try {
			InputStream in;
			
			in = open( filename );
			data = readChunk(in, chunkNum);
			in.close();
		}
		catch (Exception e) {
			/*config:debug:ON*/e.printStackTrace();
		}
		
		return data;
	}

}
