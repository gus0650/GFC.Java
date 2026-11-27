package gfc.tools;

import java.io.*;

public class CRCTool {

	public static void main(String[] args) {
		//if (args.length < 1 || args.length > 1) throw new IllegalArgumentException(HELP);
		
		byte[] test = null;
		byte[] data = null;
		String source_filename 	= args[0];

		try {
			//get data from file
			System.out.println("extracting palette data chunk from file " + source_filename + "... ");

			File source_file 	= new File(source_filename);
			FileInputStream fis = new FileInputStream( source_file );
			
			data 				= PNGTool.extractChunk( fis, "PLTE" );
			fis.close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		if (data == null) {
			System.err.println("no PLTE chunk found");
			return;
		}

		//extractChunk() returns the chunk data followed by the 4 byte CRC
		byte[] chunk = new byte[data.length - 4];
		System.arraycopy(data, 0, chunk, 0, chunk.length);

		long stored = 0;
		for (int i = data.length - 4; i < data.length; i++) {
			stored = (stored << 8) | (data[i] & 0xFF);
		}

		long calculated = calculateCRC32("PLTE", chunk);

		System.out.println("stored CRC:     " + Long.toHexString(stored));
		System.out.println("calculated CRC: " + Long.toHexString(calculated));
		System.out.println(stored == calculated ? "CRC ok" : "CRC WRONG");
	}

	/**
	 * CRC-32 of a PNG chunk: covers the chunk type and the chunk data (not the length).
	 * (was a CRC-8 over an empty array)
	 */
	static long calculateCRC32(String chunk_type, byte[] chunk_data) {
		java.util.zip.CRC32 crc = new java.util.zip.CRC32();
		crc.update( chunk_type.getBytes() );
		crc.update( chunk_data );
		return crc.getValue();
	}

	/** old experiment: a CRC-8 (polynomial 0x1D), not used */
	static byte calculateCRC32(byte[] in) {
		int i, j;
		int len = in.length;
		
		byte out = (byte) 0xC7;// bit-swapped 0xE3;

		for (j = 0; j < len; j++) {
			out = (byte) (out ^ (in[j] & 0xFF));
			for (i = 0; i < 8; i++) {
				if ((out & 0x80) != 0)
					out = (byte) ((out << 1) ^ 0x1D);
				else
					out = (byte) (out << 1);
			}
		}
		
		return out;
	}
}
