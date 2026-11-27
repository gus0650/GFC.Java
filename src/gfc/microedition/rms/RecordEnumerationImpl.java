package gfc.microedition.rms;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Walks through the records of a RecordStore, optionally filtered and sorted.
 * Without a comparator the records come in the order they were added (ascending IDs).
 */
class RecordEnumerationImpl implements RecordEnumeration, RecordListener {

	private RecordStore 		store;
	private RecordFilter 		filter;
	private RecordComparator 	comparator;
	private boolean 			keep_updated;

	private int[] 	ids;
	private int 	index;		//position between elements: next is ids[index], previous is ids[index-1]

	private boolean destroyed = false;


	RecordEnumerationImpl(RecordStore store, RecordFilter filter, RecordComparator comparator, boolean keepUpdated) {
		this.store 		= store;
		this.filter 	= filter;
		this.comparator = comparator;

		rebuild();
		keepUpdated(keepUpdated);
	}


	public int numRecords() {
		checkDestroyed();
		return ids.length;
	}

	public boolean hasNextElement() {
		checkDestroyed();
		return index < ids.length;
	}

	public boolean hasPreviousElement() {
		checkDestroyed();
		return index > 0;
	}

	public int nextRecordId() throws InvalidRecordIDException {
		checkDestroyed();
		if (index >= ids.length) throw new InvalidRecordIDException("ERROR in RecordEnumeration.nextRecordId(): no more records");

		return ids[index++];
	}

	public int previousRecordId() throws InvalidRecordIDException {
		checkDestroyed();
		if (index <= 0) throw new InvalidRecordIDException("ERROR in RecordEnumeration.previousRecordId(): no previous record");

		return ids[--index];
	}

	public byte[] nextRecord() throws InvalidRecordIDException, RecordStoreNotOpenException, RecordStoreException {
		return store.getRecord( nextRecordId() );
	}

	public byte[] previousRecord() throws InvalidRecordIDException, RecordStoreNotOpenException, RecordStoreException {
		return store.getRecord( previousRecordId() );
	}

	public void reset() {
		checkDestroyed();
		index = 0;
	}

	public void rebuild() {
		checkDestroyed();

		int[] all 	= store.getRecordIDs();
		int count 	= 0;

		//filter
		Integer[] matching = new Integer[all.length];
		for (int id : all) {
			if (filter != null && !filter.matches( dataOf(id) )) continue;
			matching[count++] = id;
		}
		matching = Arrays.copyOf(matching, count);

		//sort
		if (comparator != null) {
			Arrays.sort(matching, new Comparator<Integer>() {
				public int compare(Integer a, Integer b) {
					return comparator.compare( dataOf(a), dataOf(b) );
				}
			});
		}

		ids = new int[count];
		for (int i = 0; i < count; i++) ids[i] = matching[i];

		index = 0;
	}

	private byte[] dataOf(int id) {
		byte[] r = store.peekRecord(id);
		return (r == null) ? new byte[0] : r.clone();
	}

	public void keepUpdated(boolean keepUpdated) {
		checkDestroyed();

		if (keepUpdated == keep_updated) return;
		keep_updated = keepUpdated;

		if (keepUpdated) {
			store.addRecordListener(this);
			rebuild();
		}
		else {
			store.removeRecordListener(this);
		}
	}

	public boolean isKeptUpdated() {
		checkDestroyed();
		return keep_updated;
	}

	public void destroy() {
		checkDestroyed();

		if (keep_updated) store.removeRecordListener(this);
		destroyed = true;
	}

	private void checkDestroyed() {
		if (destroyed) throw new IllegalStateException("RecordEnumeration was destroyed");
	}


	//--- RecordListener: only registered with keepUpdated(true)

	public void recordAdded(RecordStore recordStore, int recordId) {
		rebuild();
	}

	public void recordChanged(RecordStore recordStore, int recordId) {
		rebuild();
	}

	public void recordDeleted(RecordStore recordStore, int recordId) {
		rebuild();
	}
}
