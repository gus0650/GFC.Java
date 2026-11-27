package gfc.microedition.lcdui;

import gfc.microedition.midlet.*;
import gfc.widgets.*;
import java.util.Vector;


/**
 * used by Form
 * 
 * @author Gus
 */
class ItemGroup extends Item {

	private Panel 	ig_panel;
	private Vector 	ig_choice_items = new Vector();
	
	
	public ItemGroup( String label ) {
		super(label);
		
		ig_panel = new Panel( 0, 0, getScreen() );
        ig_panel.setLayout( Layout.createTableLayout(1, 32) );

		setWidget(ig_panel);
		
		if (Boolean.parseBoolean(MIDlet.GetAppProperty("debug"))) System.out.println("ItemGroup()");
	}
	
	
	public int append( Item item ) {
		if (Boolean.parseBoolean(MIDlet.GetAppProperty("debug"))) System.out.println("ChoiceGroup.append()");

		ig_panel.append( item.getWidget() );

		ig_panel.selectFirst();
		
        ig_choice_items.addElement( item );

		return 1;
	}
	
	
	public void delete( int elementNum ) {
		if (elementNum < 0 || elementNum >= ig_choice_items.size()) throw new IndexOutOfBoundsException("item " + elementNum);

		//remove from the panel too -- was only removed from the list and still shown
		Item item = (Item)ig_choice_items.elementAt(elementNum);
		ig_panel.remove( item.getWidget() );
		ig_choice_items.removeElementAt(elementNum);
	}

	void insert( int item_id, Item item ) {
		if (item_id < 0 || item_id > ig_choice_items.size()) throw new IndexOutOfBoundsException("item " + item_id);

		ig_panel.insert( item.getWidget(), item_id );
		ig_choice_items.insertElementAt(item, item_id);
	}

	void set( int item_id, Item item ) {
		if (item_id < 0 || item_id >= ig_choice_items.size()) throw new IndexOutOfBoundsException("item " + item_id);

		Item old = (Item)ig_choice_items.elementAt(item_id);
		ig_panel.remove( old.getWidget() );
		ig_panel.insert( item.getWidget(), item_id );
		ig_choice_items.setElementAt(item, item_id);
	}
	
	int size() {
		return ig_choice_items.size();
	}
}
