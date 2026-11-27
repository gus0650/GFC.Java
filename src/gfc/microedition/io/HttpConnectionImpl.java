package gfc.microedition.io;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

/**
 * HttpConnection on top of java.net.HttpURLConnection.
 * Also used for https, that's why it implements HttpsConnection.
 */
class HttpConnectionImpl implements HttpsConnection {

	private final String 			url;
	private final URL 				jurl;
	private final HttpURLConnection conn;
	private final int 				mode;

	private boolean 	closed 			= false;
	private boolean 	stream_opened 	= false;


	HttpConnectionImpl(String url, int mode, boolean timeouts) throws IOException {
		this.url 	= url;
		this.mode 	= mode;

		try {
			jurl = URI.create(url).toURL();
		}
		catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("ERROR in Connector.open(): malformed URL - " + url);
		}

		conn = (HttpURLConnection)jurl.openConnection();
		conn.setRequestMethod(GET);		//default on the handset
		conn.setInstanceFollowRedirects(false);	//handsets don't follow redirects

		if (timeouts) {
			conn.setConnectTimeout(Connector.TIMEOUT);
			conn.setReadTimeout(Connector.TIMEOUT);
		}
	}

	private void checkOpen() throws IOException {
		if (closed) throw new IOException("connection is closed");
	}


	//--- Connection

	public void close() throws IOException {
		closed = true;

		//streams that are already open keep working, like on the handset
		if (!stream_opened) conn.disconnect();
	}


	//--- InputConnection

	public InputStream openInputStream() throws IOException {
		checkOpen();
		if ((mode & Connector.READ) == 0) throw new IOException("connection was not opened for reading");

		stream_opened = true;

		//on the handset the body of an error page can be read too, java throws an exception instead
		try {
			return conn.getInputStream();
		}
		catch (IOException e) {
			InputStream err = conn.getErrorStream();
			if (err == null) throw e;
			return err;
		}
	}

	public DataInputStream openDataInputStream() throws IOException {
		return new DataInputStream( openInputStream() );
	}


	//--- OutputConnection

	public OutputStream openOutputStream() throws IOException {
		checkOpen();
		if ((mode & Connector.WRITE) == 0) throw new IOException("connection was not opened for writing");

		stream_opened = true;
		conn.setDoOutput(true);

		return conn.getOutputStream();
	}

	public DataOutputStream openDataOutputStream() throws IOException {
		return new DataOutputStream( openOutputStream() );
	}


	//--- ContentConnection

	public String getType() {
		return conn.getContentType();
	}

	public String getEncoding() {
		return conn.getContentEncoding();
	}

	public long getLength() {
		return conn.getContentLengthLong();
	}


	//--- HttpConnection: URL

	public String getURL() {
		return url;
	}

	public String getProtocol() {
		return jurl.getProtocol();
	}

	public String getHost() {
		return jurl.getHost();
	}

	public String getFile() {
		String f = jurl.getPath();
		return (f == null || f.length() == 0) ? null : f;
	}

	public String getRef() {
		return jurl.getRef();
	}

	public String getQuery() {
		return jurl.getQuery();
	}

	public int getPort() {
		int p = jurl.getPort();
		if (p != -1) return p;

		return getProtocol().equals("https") ? 443 : 80;
	}


	//--- HttpConnection: request

	public String getRequestMethod() {
		return conn.getRequestMethod();
	}

	public void setRequestMethod(String method) throws IOException {
		checkOpen();
		conn.setRequestMethod(method);
	}

	public String getRequestProperty(String key) {
		try {
			return conn.getRequestProperty(key);
		}
		catch (IllegalStateException e) {
			return null;	//already connected
		}
	}

	public void setRequestProperty(String key, String value) throws IOException {
		checkOpen();

		try {
			conn.setRequestProperty(key, value);
		}
		catch (IllegalStateException e) {
			throw new IOException("ERROR in HttpConnection.setRequestProperty(): already connected");
		}
	}


	//--- HttpConnection: response

	public int getResponseCode() throws IOException {
		checkOpen();
		return conn.getResponseCode();
	}

	public String getResponseMessage() throws IOException {
		checkOpen();
		return conn.getResponseMessage();
	}

	public long getExpiration() throws IOException {
		checkOpen();
		return conn.getExpiration();
	}

	public long getDate() throws IOException {
		checkOpen();
		return conn.getDate();
	}

	public long getLastModified() throws IOException {
		checkOpen();
		return conn.getLastModified();
	}

	public String getHeaderField(String name) throws IOException {
		checkOpen();
		return conn.getHeaderField(name);
	}

	//java counts the status line as header 0, MIDP starts with the first real header
	private int headerOffset() {
		return (conn.getHeaderFieldKey(0) == null) ? 1 : 0;
	}

	public String getHeaderField(int n) throws IOException {
		checkOpen();
		return conn.getHeaderField( n + headerOffset() );
	}

	public String getHeaderFieldKey(int n) throws IOException {
		checkOpen();
		return conn.getHeaderFieldKey( n + headerOffset() );
	}

	public int getHeaderFieldInt(String name, int def) throws IOException {
		checkOpen();
		return conn.getHeaderFieldInt(name, def);
	}

	public long getHeaderFieldDate(String name, long def) throws IOException {
		checkOpen();
		return conn.getHeaderFieldDate(name, def);
	}
}
