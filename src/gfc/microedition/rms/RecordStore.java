package gfc.microedition.rms;

import gfc.microedition.midlet.*;
import java.io.*;
import java.util.*;

/**
 * Persistent storage like on the handset.
 * <p>
 * Each RecordStore is saved as one file in the folder
 * <pre>
 * [user home]/.gfc/[MIDlet class]/[store name].rms
 * </pre>
 * The folder can be changed with rms.dir in midlet.properties, the size limit with rms.size (default 1 MB).
 * The file is written after every change, so nothing is lost if the game is killed.
 *
 * @see javax.microedition.rms.RecordStore
 * @author Gustaf Stechmann
 */
public class RecordStore {

	public static final int
		AUTHMODE_PRIVATE 	= 0,
		AUTHMODE_ANY 		= 1;

	//file format
	private static final int 	MAGIC 			= 0x47464352;	//"GFCR"
	private static final int 	FILE_VERSION 	= 1;
	private static final int 	DEFAULT_SIZE 	= 1024 * 1024;

	//stores that are currently open: name -> store
	private static final Hashtable<String, RecordStore> open_stores = new Hashtable<String, RecordStore>();

	private final String 	name;
	private final File 		file;

	private TreeMap<Integer, byte[]> records = new TreeMap<Integer, byte[]>();
	private int 	next_id 		= 1;
	private int 	version 		= 0;
	private long 	last_modified 	= 0;
	private int 	authmode 		= AUTHMODE_PRIVATE;
	private boolean writable 		= false;

	private int 	open_count 		= 0;

	private Vector<RecordListener> recordListener = new Vector<RecordListener>();


	private RecordStore(String name, File file) {
		this.name = name;
		this.file = file;
	}


	//------- opening and closing

	public static RecordStore openRecordStore(String recordStoreName, boolean createIfNecessary)
			throws RecordStoreException, RecordStoreFullException, RecordStoreNotFoundException {
		return openRecordStore(recordStoreName, createIfNecessary, AUTHMODE_PRIVATE, false);
	}

	public static synchronized RecordStore openRecordStore(String recordStoreName, boolean createIfNecessary, int authmode, boolean writable)
			throws RecordStoreException, RecordStoreFullException, RecordStoreNotFoundException {
		if (Boolean.parseBoolean(MIDlet.GetAppProperty("debug"))) System.out.println("RecordStore.openRecordStore() " + recordStoreName);

		checkName(recordStoreName);

		//already open -> same object, like on the handset
		RecordStore rs = open_stores.get(recordStoreName);
		if (rs != null) {
			rs.open_count++;
			return rs;
		}

		File f 	= getFile(recordStoreName);
		rs 		= new RecordStore(recordStoreName, f);

		if (f.exists()) {
			rs.load();
		}
		else {
			if (!createIfNecessary) throw new RecordStoreNotFoundException("ERROR in RecordStore.openRecordStore(): not found - " + recordStoreName);

			rs.authmode 		= authmode;
			rs.writable 		= writable;
			rs.last_modified 	= System.currentTimeMillis();
			rs.save();
		}

		rs.open_count = 1;
		open_stores.put(recordStoreName, rs);

		return rs;
	}

	/**
	 * Opens a store of another MIDlet suite. On the desktop there is only one suite, so this opens our own store.
	 */
	public static RecordStore openRecordStore(String recordStoreName, String vendorName, String suiteName)
			throws RecordStoreException, RecordStoreNotFoundException {
		return openRecordStore(recordStoreName, false);
	}

	public synchronized void closeRecordStore() throws RecordStoreNotOpenException, RecordStoreException {
		checkOpen();

		open_count--;

		//really closed when every openRecordStore() has its closeRecordStore()
		if (open_count == 0) {
			synchronized (RecordStore.class) {
				open_stores.remove(name);
			}
			recordListener.removeAllElements();
		}
	}

	public static synchronized void deleteRecordStore(String recordStoreName)
			throws RecordStoreException, RecordStoreNotFoundException {
		checkName(recordStoreName);

		if (open_stores.containsKey(recordStoreName)) throw new RecordStoreException("ERROR in RecordStore.deleteRecordStore(): store is still open - " + recordStoreName);

		File f = getFile(recordStoreName);
		if (!f.exists()) throw new RecordStoreNotFoundException("ERROR in RecordStore.deleteRecordStore(): not found - " + recordStoreName);

		if (!f.delete()) throw new RecordStoreException("ERROR in RecordStore.deleteRecordStore(): could not delete " + f);
	}

	/**
	 * @return names of all stores, or null if there are none
	 */
	public static String[] listRecordStores() {
		File[] files = getDirectory().listFiles();
		if (files == null) return null;

		Vector<String> names = new Vector<String>();
		for (File f : files) {
			String n = f.getName();
			if (n.endsWith(".rms")) names.addElement( decodeName( n.substring(0, n.length() - 4) ) );
		}

		if (names.size() == 0) return null;

		String[] result = new String[names.size()];
		names.copyInto(result);
		return result;
	}


	//------- properties of the store

	public synchronized void setMode(int authmode, boolean writable) throws RecordStoreException {
		checkOpen();
		if ( (authmode != AUTHMODE_PRIVATE) & (authmode != AUTHMODE_ANY) ) throw new IllegalArgumentException("illegal authmode " + authmode);

		this.authmode = authmode;
		this.writable = writable;
		save();
	}

	public String getName() throws RecordStoreNotOpenException {
		checkOpen();
		return name;
	}

	public synchronized int getVersion() throws RecordStoreNotOpenException {
		checkOpen();
		return version;
	}

	public synchronized int getNumRecords() throws RecordStoreNotOpenException {
		checkOpen();
		return records.size();
	}

	/** Bytes used by the store (approximately the size of the file). */
	public synchronized int getSize() throws RecordStoreNotOpenException {
		checkOpen();
		return calculateSize();
	}

	public synchronized int getSizeAvailable() throws RecordStoreNotOpenException {
		checkOpen();
		return Math.max(0, getMaxSize() - calculateSize());
	}

	public synchronized long getLastModified() throws RecordStoreNotOpenException {
		checkOpen();
		return last_modified;
	}

	public synchronized int getNextRecordID() throws RecordStoreNotOpenException, RecordStoreException {
		checkOpen();
		return next_id;
	}


	//------- records

	public synchronized int addRecord(byte[] data, int offset, int numBytes)
			throws RecordStoreNotOpenException, RecordStoreException, RecordStoreFullException {
		checkOpen();

		byte[] r = copy(data, offset, numBytes);
		if (calculateSize() + r.length + 8 > getMaxSize()) throw new RecordStoreFullException("ERROR in RecordStore.addRecord(): store is full - " + name);

		int id = next_id++;
		records.put(id, r);
		changed();

		notifyRecordAddedListeners(id);
		return id;
	}

	public synchronized void setRecord(int recordId, byte[] newData, int offset, int numBytes)
			throws RecordStoreNotOpenException, InvalidRecordIDException, RecordStoreException, RecordStoreFullException {
		checkOpen();
		byte[] old = getRecordData(recordId);

		byte[] r = copy(newData, offset, numBytes);
		if (calculateSize() - old.length + r.length > getMaxSize()) throw new RecordStoreFullException("ERROR in RecordStore.setRecord(): store is full - " + name);

		records.put(recordId, r);
		changed();

		notifyRecordChangedListeners(recordId);
	}

	public synchronized void deleteRecord(int recordId)
			throws RecordStoreNotOpenException, InvalidRecordIDException, RecordStoreException {
		checkOpen();
		getRecordData(recordId);

		records.remove(recordId);
		changed();

		notifyRecordDeletedListeners(recordId);
	}

	/**
	 * @return a copy of the record, or null if the record is empty
	 */
	public synchronized byte[] getRecord(int recordId)
			throws RecordStoreNotOpenException, InvalidRecordIDException, RecordStoreException {
		checkOpen();
		byte[] r = getRecordData(recordId);

		if (r.length == 0) return null;
		return r.clone();
	}

	/**
	 * Copies the record into buffer.
	 * @return number of bytes copied
	 */
	public synchronized int getRecord(int recordId, byte[] buffer, int offset)
			throws RecordStoreNotOpenException, InvalidRecordIDException, RecordStoreException {
		checkOpen();
		byte[] r = getRecordData(recordId);

		System.arraycopy(r, 0, buffer, offset, r.length);	//throws ArrayIndexOutOfBoundsException if buffer is too small, like on the handset
		return r.length;
	}

	public synchronized int getRecordSize(int recordId)
			throws RecordStoreNotOpenException, InvalidRecordIDException, RecordStoreException {
		checkOpen();
		return getRecordData(recordId).length;
	}

	public synchronized RecordEnumeration enumerateRecords(RecordFilter filter, RecordComparator comparator, boolean keepUpdated)
			throws RecordStoreNotOpenException {
		checkOpen();
		return new RecordEnumerationImpl(this, filter, comparator, keepUpdated);
	}


	//------- listeners

	public synchronized void addRecordListener(RecordListener listener) {
		if (listener != null && !recordListener.contains(listener)) recordListener.addElement(listener);
	}

	public synchronized void removeRecordListener(RecordListener listener) {
		recordListener.removeElement(listener);
	}

	private void notifyRecordChangedListeners(int recordId) {
		for (RecordListener rl : new Vector<RecordListener>(recordListener)) rl.recordChanged(this, recordId);
	}

	private void notifyRecordAddedListeners(int recordId) {
		for (RecordListener rl : new Vector<RecordListener>(recordListener)) rl.recordAdded(this, recordId);
	}

	private void notifyRecordDeletedListeners(int recordId) {
		for (RecordListener rl : new Vector<RecordListener>(recordListener)) rl.recordDeleted(this, recordId);
	}


	//------- used by RecordEnumerationImpl

	/** IDs of all records, ascending. */
	synchronized int[] getRecordIDs() {
		int[] ids 	= new int[records.size()];
		int i 		= 0;
		for (Integer id : records.keySet()) ids[i++] = id;
		return ids;
	}

	synchronized byte[] peekRecord(int recordId) {
		return records.get(recordId);
	}


	//------- internals

	private void checkOpen() throws RecordStoreNotOpenException {
		if (open_count <= 0) throw new RecordStoreNotOpenException("ERROR in RecordStore: store is not open - " + name);
	}

	boolean isOpen() {
		return open_count > 0;
	}

	private byte[] getRecordData(int recordId) throws InvalidRecordIDException {
		byte[] r = records.get(recordId);
		if (r == null) throw new InvalidRecordIDException("ERROR in RecordStore: no record with ID " + recordId + " in " + name);
		return r;
	}

	private static byte[] copy(byte[] data, int offset, int numBytes) {
		if (data == null) {
			if (numBytes > 0) throw new NullPointerException();
			return new byte[0];
		}

		byte[] r = new byte[numBytes];
		System.arraycopy(data, offset, r, 0, numBytes);
		return r;
	}

	private void changed() throws RecordStoreException {
		version++;
		last_modified = System.currentTimeMillis();
		save();
	}

	private int calculateSize() {
		int size = 40;	//header
		for (byte[] r : records.values()) size += 8 + r.length;
		return size;
	}

	private static int getMaxSize() {
		String s = MIDlet.GetAppProperty("rms.size");
		if (s == null) return DEFAULT_SIZE;
		return Integer.parseInt(s.trim());
	}

	private static void checkName(String name) {
		if (name == null) throw new NullPointerException();
		if ( (name.length() < 1) | (name.length() > 32) ) throw new IllegalArgumentException("ERROR in RecordStore: name must have 1..32 characters - " + name);
	}


	//------- files

	private static File getDirectory() {
		String dir = MIDlet.GetAppProperty("rms.dir");
		if (dir != null) return new File(dir.trim());

		String app = MIDlet.GetAppProperty("class");
		if (app == null) app = "default";

		return new File( new File(System.getProperty("user.home"), ".gfc"), app );
	}

	private static File getFile(String name) {
		return new File( getDirectory(), encodeName(name) + ".rms" );
	}

	/** store names may contain any character -> letters and digits stay, everything else becomes %XXXX */
	private static String encodeName(String name) {
		StringBuilder sb = new StringBuilder();
		for (char c : name.toCharArray()) {
			if ( (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_' || c == '-' ) sb.append(c);
			else sb.append('%').append( String.format("%04X", (int)c) );
		}
		return sb.toString();
	}

	private static String decodeName(String fname) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < fname.length(); i++) {
			char c = fname.charAt(i);
			if (c == '%' && i + 5 <= fname.length()) {
				sb.append( (char)Integer.parseInt(fname.substring(i + 1, i + 5), 16) );
				i += 4;
			}
			else sb.append(c);
		}
		return sb.toString();
	}

	private void load() throws RecordStoreException {
		DataInputStream in = null;
		try {
			in = new DataInputStream( new BufferedInputStream( new FileInputStream(file) ) );

			if (in.readInt() != MAGIC) 			throw new RecordStoreException("ERROR in RecordStore: not a record store file - " + file);
			if (in.readInt() != FILE_VERSION) 	throw new RecordStoreException("ERROR in RecordStore: unknown file version - " + file);

			next_id 		= in.readInt();
			version 		= in.readInt();
			last_modified 	= in.readLong();
			authmode 		= in.readInt();
			writable 		= in.readBoolean();

			int count = in.readInt();
			for (int i = 0; i < count; i++) {
				int id 		= in.readInt();
				byte[] r 	= new byte[in.readInt()];
				in.readFully(r);
				records.put(id, r);
			}
		}
		catch (IOException e) {
			throw new RecordStoreException("ERROR in RecordStore: could not read " + file + " - " + e.getMessage());
		}
		finally {
			if (in != null) try { in.close(); } catch (IOException e) { }
		}
	}

	private void save() throws RecordStoreException {
		File dir = file.getParentFile();
		if (!dir.exists() && !dir.mkdirs()) throw new RecordStoreException("ERROR in RecordStore: could not create folder " + dir);

		//write to a temporary file first, so a crash never leaves a broken store
		File tmp = new File(dir, file.getName() + ".tmp");

		DataOutputStream out = null;
		try {
			out = new DataOutputStream( new BufferedOutputStream( new FileOutputStream(tmp) ) );

			out.writeInt(MAGIC);
			out.writeInt(FILE_VERSION);
			out.writeInt(next_id);
			out.writeInt(version);
			out.writeLong(last_modified);
			out.writeInt(authmode);
			out.writeBoolean(writable);

			out.writeInt(records.size());
			for (Map.Entry<Integer, byte[]> e : records.entrySet()) {
				out.writeInt(e.getKey());
				out.writeInt(e.getValue().length);
				out.write(e.getValue());
			}

			out.close();
			out = null;

			if (file.exists() && !file.delete()) throw new IOException("could not replace " + file);
			if (!tmp.renameTo(file)) throw new IOException("could not rename " + tmp);
		}
		catch (IOException e) {
			throw new RecordStoreException("ERROR in RecordStore: could not write " + file + " - " + e.getMessage());
		}
		finally {
			if (out != null) try { out.close(); } catch (IOException e) { }
		}
	}


	//------- helpers for games (kept from GFC)

	static int getInt(byte data[], int offset) {
		int r = data[offset++];
		r = r << 8 | data[offset++] & 0xff;
		r = r << 8 | data[offset++] & 0xff;
		r = r << 8 | data[offset++] & 0xff;
		return r;
	}

	static long getLong(byte data[], int offset) {
		long r = data[offset++];
		r = r << 8 | (long) data[offset++] & 255L;
		r = r << 8 | (long) data[offset++] & 255L;
		r = r << 8 | (long) data[offset++] & 255L;
		r = r << 8 | (long) data[offset++] & 255L;
		r = r << 8 | (long) data[offset++] & 255L;
		r = r << 8 | (long) data[offset++] & 255L;
		r = r << 8 | (long) data[offset++] & 255L;
		return r;
	}

	static int putInt(int i, byte data[], int offset) {
		data[offset++] = (byte) (i >> 24 & 0xff);
		data[offset++] = (byte) (i >> 16 & 0xff);
		data[offset++] = (byte) (i >> 8 & 0xff);
		data[offset] = (byte) (i & 0xff);
		return 4;
	}

	static int putLong(long l, byte data[], int offset) {
		data[offset++] = (byte) (int) (l >> 56 & 255L);
		data[offset++] = (byte) (int) (l >> 48 & 255L);
		data[offset++] = (byte) (int) (l >> 40 & 255L);
		data[offset++] = (byte) (int) (l >> 32 & 255L);
		data[offset++] = (byte) (int) (l >> 24 & 255L);
		data[offset++] = (byte) (int) (l >> 16 & 255L);
		data[offset++] = (byte) (int) (l >> 8 & 255L);
		data[offset] = (byte) (int) (l & 255L);
		return 8;
	}
}
