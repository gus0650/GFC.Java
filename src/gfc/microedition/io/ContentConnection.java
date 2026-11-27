package gfc.microedition.io;

/**
 * @see javax.microedition.io.ContentConnection
 */
public interface ContentConnection extends StreamConnection {
	String 	getType();
	String 	getEncoding();
	long 	getLength();
}
