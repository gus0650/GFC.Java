package gfc.microedition.io;

import java.io.*;

/**
 * @see javax.microedition.io.OutputConnection
 */
public interface OutputConnection extends Connection {
	OutputStream 		openOutputStream() 		throws IOException;
	DataOutputStream 	openDataOutputStream() 	throws IOException;
}
