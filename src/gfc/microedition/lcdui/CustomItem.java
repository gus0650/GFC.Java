package gfc.microedition.lcdui;

/**
 * Placeholder: CustomItem is not implemented in GFC.Java.
 * It exists so that games which mention it compile; creating one throws an UnsupportedOperationException.
 * 
 * @see javax.microedition.lcdui.CustomItem
 */
public abstract class CustomItem extends Item {

	protected static final int
		TRAVERSE_HORIZONTAL = 1,
		TRAVERSE_VERTICAL 	= 2,
		KEY_PRESS 			= 4,
		KEY_RELEASE 		= 8,
		KEY_REPEAT 			= 0x10,
		POINTER_PRESS 		= 0x20,
		POINTER_RELEASE 	= 0x40,
		POINTER_DRAG 		= 0x80,
		NONE 				= 0;

	protected CustomItem( String label ) {
		super(null);
		throw new UnsupportedOperationException("CustomItem is not implemented in GFC.Java -- see TODO.txt");
	}

	protected abstract int getMinContentWidth();
	protected abstract int getMinContentHeight();
	protected abstract int getPrefContentWidth(int height);
	protected abstract int getPrefContentHeight(int width);
	protected abstract void paint(Graphics g, int w, int h);
}
