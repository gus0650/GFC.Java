package gfc.microedition.io;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * SocketConnection on top of java.net.Socket. Only client sockets ("socket://host:port").
 */
class SocketConnectionImpl implements SocketConnection {

	private final Socket 	socket;
	private final int 		mode;


	private SocketConnectionImpl(Socket socket, int mode) {
		this.socket = socket;
		this.mode 	= mode;
	}

	static SocketConnection open(String url, int mode, boolean timeouts) throws IOException {
		//socket://host:port
		String address 	= url.substring( "socket://".length() );
		int colon 		= address.lastIndexOf(':');

		if (colon == -1) throw new IllegalArgumentException("ERROR in Connector.open(): no port in URL - " + url);

		String host = address.substring(0, colon);
		int port;
		try {
			port = Integer.parseInt( address.substring(colon + 1) );
		}
		catch (NumberFormatException e) {
			throw new IllegalArgumentException("ERROR in Connector.open(): illegal port in URL - " + url);
		}

		//"socket://:1234" is a server socket on the handset
		if (host.length() == 0) throw new ConnectionNotFoundException("ERROR in Connector.open(): server sockets are not supported - " + url);

		Socket s = new Socket();
		s.connect( new InetSocketAddress(host, port), timeouts ? Connector.TIMEOUT : 0 );
		if (timeouts) s.setSoTimeout(Connector.TIMEOUT);

		return new SocketConnectionImpl(s, mode);
	}


	private boolean streams_opened = false;

	public void close() throws IOException {
		//MIDP: open streams keep working until they are closed.
		//java closes the socket when one of its streams is closed, so only close it here if no stream was opened
		if (!streams_opened) socket.close();
	}

	public InputStream openInputStream() throws IOException {
		if ((mode & Connector.READ) == 0) throw new IOException("connection was not opened for reading");

		streams_opened = true;
		return socket.getInputStream();
	}

	public DataInputStream openDataInputStream() throws IOException {
		return new DataInputStream( openInputStream() );
	}

	public OutputStream openOutputStream() throws IOException {
		if ((mode & Connector.WRITE) == 0) throw new IOException("connection was not opened for writing");

		streams_opened = true;
		return socket.getOutputStream();
	}

	public DataOutputStream openDataOutputStream() throws IOException {
		return new DataOutputStream( openOutputStream() );
	}


	//--- socket options

	public void setSocketOption(byte option, int value) throws IOException {
		if (value < 0) throw new IllegalArgumentException("negative option value");

		switch (option) {
			case DELAY: 	socket.setTcpNoDelay( value == 0 ); 	break;
			case LINGER: 	socket.setSoLinger( value > 0, value ); break;
			case KEEPALIVE: socket.setKeepAlive( value != 0 ); 		break;
			case RCVBUF: 	socket.setReceiveBufferSize( value ); 	break;
			case SNDBUF: 	socket.setSendBufferSize( value ); 		break;
			default: 		throw new IllegalArgumentException("unknown socket option " + option);
		}
	}

	public int getSocketOption(byte option) throws IOException {
		switch (option) {
			case DELAY: 	return socket.getTcpNoDelay() ? 0 : 1;
			case LINGER: 	return Math.max(0, socket.getSoLinger());
			case KEEPALIVE: return socket.getKeepAlive() ? 1 : 0;
			case RCVBUF: 	return socket.getReceiveBufferSize();
			case SNDBUF: 	return socket.getSendBufferSize();
		}
		throw new IllegalArgumentException("unknown socket option " + option);
	}

	public String getLocalAddress() throws IOException {
		return socket.getLocalAddress().getHostAddress();
	}

	public int getLocalPort() throws IOException {
		return socket.getLocalPort();
	}

	public String getAddress() throws IOException {
		return socket.getInetAddress().getHostAddress();
	}

	public int getPort() throws IOException {
		return socket.getPort();
	}
}
