package gfc.microedition.io;

import gfc.microedition.midlet.*;
import java.io.*;

/**
 * Opens connections like on the handset.
 * <p>
 * supported:
 * <pre>
 * http://...       -> HttpConnection 	(via java.net.HttpURLConnection)
 * https://...      -> HttpsConnection 	(via java.net.HttpURLConnection)
 * socket://host:port -> SocketConnection (via java.net.Socket)
 * </pre>
 * everything else (file, datagram, sms, bluetooth, server sockets ...) throws a ConnectionNotFoundException.
 * Games normally catch this and continue offline.
 *
 * @see javax.microedition.io.Connector
 * @author Gustaf Stechmann
 */
public class Connector {

	public static final int
		READ 		= 1,
		WRITE 		= 2,
		READ_WRITE 	= 3;

	//timeout in ms, used if a game asks for timeout exceptions
	static final int TIMEOUT = 10000;

	private Connector() { }


	public static Connection open(String name) throws IOException {
		return open(name, READ_WRITE, false);
	}

	public static Connection open(String name, int mode) throws IOException {
		return open(name, mode, false);
	}

	public static Connection open(String name, int mode, boolean timeouts) throws IOException {
		if (Boolean.parseBoolean(MIDlet.GetAppProperty("debug"))) System.out.println("Connector.open() " + name);

		if (name == null) throw new IllegalArgumentException("no URL");
		if ( (mode != READ) & (mode != WRITE) & (mode != READ_WRITE) ) throw new IllegalArgumentException("illegal mode " + mode);

		int colon = name.indexOf(':');
		if (colon < 1) throw new IllegalArgumentException("no protocol in URL " + name);

		String protocol = name.substring(0, colon).toLowerCase();

		if (protocol.equals("http") | protocol.equals("https")) 	return new HttpConnectionImpl(name, mode, timeouts);
		if (protocol.equals("socket")) 								return SocketConnectionImpl.open(name, mode, timeouts);

		throw new ConnectionNotFoundException("ERROR in Connector.open(): protocol not supported - " + protocol);
	}


	//--- shortcuts

	public static InputStream openInputStream(String name) throws IOException {
		return openDataInputStream(name);
	}

	public static DataInputStream openDataInputStream(String name) throws IOException {
		InputConnection c = (InputConnection)open(name, READ);

		//the stream stays usable after the connection is closed
		try {
			return c.openDataInputStream();
		}
		finally {
			c.close();
		}
	}

	public static OutputStream openOutputStream(String name) throws IOException {
		return openDataOutputStream(name);
	}

	public static DataOutputStream openDataOutputStream(String name) throws IOException {
		OutputConnection c = (OutputConnection)open(name, WRITE);

		try {
			return c.openDataOutputStream();
		}
		finally {
			c.close();
		}
	}
}
