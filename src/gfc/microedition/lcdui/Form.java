package gfc.microedition.lcdui;

/**
 * @see javax.microedition.lcdui.Form
 * @author Gustaf Stechmann
 * @version pre-alpha
 */

public class Form extends Screen {

	private ItemStateListener 	f_item_state_listener;
	private ItemGroup			f_item_group;

	
	public Form( String title ) {
		super(title);

		f_item_group = new ItemGroup( null );
		
		setContent(f_item_group);
	}

	
	public int append( Item item ) {
		checkSupported(item);

		item.setScreen(this);
		f_item_group.append( item );
		return f_item_group.size() - 1;	//index of the new item
	}

	
	public int append( String s ) {
		StringItem si = new StringItem( null, s );
		
		int index = append( si );
		posHome();
		return index;
	}


	public int append( Image img ) {
		ImageItem ii = new ImageItem( null, img, 0, null );
		
		int index = append( ii );
		posHome();
		return index;
	}


	public void delete( int item_id ) {
		f_item_group.delete(item_id);
		
		posHome();
	}


	public void insert( int item_id, Item item ) {
		checkSupported(item);

		item.setScreen(this);
		f_item_group.insert(item_id, item);
		
		posHome();
	}


	public void set( int item_id, Item item ) {
		checkSupported(item);

		item.setScreen(this);
		f_item_group.set(item_id, item);
		
		posHome();
	}


	public void setItemStateListener( ItemStateListener item_state_listener ) {
		f_item_state_listener = item_state_listener;
	}


	public int size() {
		return f_item_group.size();
	}
	
/*	
	boolean keyPressedPreprocess(int keyCode) {
		//
		return false;
	}
	
	void keyReleasedPreprocess(int keyCode) {
		//
	}
	
	void keyRepeatedPreprocess(int keyCode) {
		//
	}
	
	void pointerDraggedPreprocess(int x, int y) {
		//
	}
	
	void pointerPressedPreprocess(int x, int y) {
		//
	}
	
	void pointerReleasedPreprocess(int x, int y) {
		//
	}
	
	void hideNotifyPreprocess() {
		//
	}
	
	void showNotifyPreprocess() {
		//
	}
	*/

	/**
	 * Form does not pass keys to its items yet -> items that need input would not work.
	 */
	private void checkSupported( Item item ) {
		if (item instanceof TextField) 		throw new UnsupportedOperationException("TextField in a Form is not implemented in GFC.Java -- see TODO.txt (use a TextBox)");
		if (item instanceof ChoiceGroup) 	throw new UnsupportedOperationException("ChoiceGroup in a Form is not implemented in GFC.Java -- see TODO.txt (use a List)");
	}
}
