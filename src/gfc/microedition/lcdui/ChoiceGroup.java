package gfc.microedition.lcdui;

import gfc.microedition.midlet.*;
import gfc.widgets.*;
import java.util.Vector;


/**
 * @see javax.microedition.lcdui.ChoiceGroup
 * @author Gustaf Stechmann
 */
public class ChoiceGroup extends Item implements Choice {

	private int		cg_type;
	private Panel 	cg_panel;
	private Vector 	cg_choice_items = new Vector();
	

	public ChoiceGroup( String label, int choiceType ) {
		super(label);
		
		cg_type = choiceType;
		
        cg_panel = new Panel( 0, 0, getScreen() );
        cg_panel.setLayout( Layout.createTableLayout(1, 32) );

		setWidget(cg_panel);

		if (Boolean.parseBoolean(MIDlet.GetAppProperty("debug"))) System.out.println("ChoiceGroup()");
	}

	
	void notifyPressed() {
		if (Boolean.parseBoolean(MIDlet.GetAppProperty("debug"))) System.out.println("ChoiceGroup.notifyPressed()");
		
		//the highlighted element (getSelectedIndex() returns the checked one for EXCLUSIVE)
		ChoiceItem current = ((ChoiceItem)cg_choice_items.elementAt( cg_panel.getSelectedIndex() ));

		switch (cg_type) {
			case EXCLUSIVE:
				//deactivate others
				deselectAll();
				
				//activate current
				current.setChecked(true);

				break;

			case IMPLICIT:
				//nop -- but COMMAND_SELECT is triggered in commandAction()
				break;
		
			case MULTIPLE:
				//check / uncheck current
				current.setChecked(!current.isChecked());
				break;
		}
	}
	
	
	void selectPrevious() {
		if (Boolean.parseBoolean(MIDlet.GetAppProperty("debug"))) System.out.println("ChoiceGroup.selectPrevious()");

		cg_panel.selectPrevious();
	}
	
	
	void selectNext() {
		if (Boolean.parseBoolean(MIDlet.GetAppProperty("debug"))) System.out.println("ChoiceGroup.selectNext()");

		cg_panel.selectNext();
	}
	
	
	public int append( String stringPart, Image imagePart ) {
		if (Boolean.parseBoolean(MIDlet.GetAppProperty("debug"))) System.out.println("ChoiceGroup.append()");

		ChoiceItem ci = new ChoiceItem( 
				null, 
				getScreen(), 
				stringPart, 
				cg_type 
			);
		
		cg_panel.append( ci.getWidget() );

		cg_panel.selectFirst();
		
        cg_choice_items.addElement( ci );
		
        if (cg_type == Choice.EXCLUSIVE) {
            deselectAll();
            ChoiceItem cifirst = (ChoiceItem)cg_choice_items.firstElement();
            cifirst.setChecked(true);
        }
		
		return cg_choice_items.size() - 1;	//index of the new element
	}
	
	
	public void delete( int elementNum ) {
		if (Boolean.parseBoolean(MIDlet.GetAppProperty("debug"))) System.out.println("ChoiceGroup.delete()");

		cg_panel.remove( cg_panel.getWidgetAt(elementNum) );
		cg_choice_items.removeElementAt(elementNum);	//TODO untested
	}


	public Image getImage( int elementNum ) {
		throw new UnsupportedOperationException("ChoiceGroup.getImage() is not implemented in GFC.Java -- see TODO.txt");
	}


	public int getSelectedFlags( boolean[] selectedArray_return ) {
		throw new UnsupportedOperationException("ChoiceGroup.getSelectedFlags() is not implemented in GFC.Java -- see TODO.txt");
	}


	public int getSelectedIndex() {
		if (Boolean.parseBoolean(MIDlet.GetAppProperty("debug"))) System.out.println("ChoiceGroup.getSelectedIndex()");

		//MIDP: MULTIPLE has no single selection, EXCLUSIVE returns the checked element
		if (cg_type == Choice.MULTIPLE) return -1;

		if (cg_type == Choice.EXCLUSIVE) {
			for (int i = 0; i < cg_choice_items.size(); i++) {
				if (((ChoiceItem)cg_choice_items.elementAt(i)).isChecked()) return i;
			}
			return -1;
		}

		return cg_panel.getSelectedIndex();
	}


	public String getString( int elementNum ) {
		throw new UnsupportedOperationException("ChoiceGroup.getString() is not implemented in GFC.Java -- see TODO.txt");
	}


	public void insert( int elementNum, String stringElement, Image imageElement ) {
		throw new UnsupportedOperationException("ChoiceGroup.insert() is not implemented in GFC.Java -- see TODO.txt");
	}


	public boolean isSelected( int elementNum ) {
		throw new UnsupportedOperationException("ChoiceGroup.isSelected() is not implemented in GFC.Java -- see TODO.txt");
	}


	public void set( int elementNum, String stringElement, Image imageElement ) {
		throw new UnsupportedOperationException("ChoiceGroup.set() is not implemented in GFC.Java -- see TODO.txt");
	}


	public void setSelectedFlags( boolean[] selectedArray ) {
		throw new UnsupportedOperationException("ChoiceGroup.setSelectedFlags() is not implemented in GFC.Java -- see TODO.txt");
	}


	public void setSelectedIndex( int elementNum, boolean selected ) {
		if (Boolean.parseBoolean(MIDlet.GetAppProperty("debug"))) System.out.println("ChoiceGroup.setSelectedIndex()");

		ChoiceItem ci = ((ChoiceItem)cg_choice_items.elementAt( elementNum ));

		if (cg_type == Choice.MULTIPLE) {
			//MULTIPLE: check or uncheck just this element (selected was ignored)
			ci.setChecked(selected);
			return;
		}

		//IMPLICIT / EXCLUSIVE: only "true" changes something -- this element becomes the selected one
		if (!selected) return;

		if (cg_type == Choice.EXCLUSIVE) {
			deselectAll();
			ci.setChecked(true);
		}

		//move the highlight there too
		cg_panel.selectFirst();
		for (int i = 0; i < elementNum; i++) cg_panel.selectNext();
	}


	public int size() {
		return cg_panel.numWidgets();
	}
	
	
	Item getItem(int index) {
		return (Item)cg_choice_items.elementAt(index);
	}
	
	
	private void deselectAll() {
		for (int i = 0; i < cg_choice_items.size(); i++) {
			ChoiceItem ci = (ChoiceItem)cg_choice_items.elementAt(i);
			ci.setChecked(false);
		}
	}
}