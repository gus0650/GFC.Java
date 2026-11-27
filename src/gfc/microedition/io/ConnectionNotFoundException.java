package gfc.microedition.io;

import java.io.IOException;

/**
 * Thrown by Connector.open() if the protocol is not supported.
 * 
 * @see javax.microedition.io.ConnectionNotFoundException
 */
public class ConnectionNotFoundException extends IOException {

	static final long serialVersionUID = 1L;

	public ConnectionNotFoundException() {
		super();
	}

	public ConnectionNotFoundException(String s) {
		super(s);
	}
}
