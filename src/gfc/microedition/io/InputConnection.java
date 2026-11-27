package gfc.microedition.io;

import java.io.*;

/**
 * @see javax.microedition.io.InputConnection
 */
public interface InputConnection extends Connection {
	InputStream 	openInputStream() 		throws IOException;
	DataInputStream openDataInputStream() 	throws IOException;
}
