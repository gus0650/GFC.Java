package gfc.microedition.lcdui;

import gfc.microedition.midlet.*;
import gfc.widgets.*;


/**
 * A text field that is edited with the keyboard of the PC:
 * typed characters are inserted at the caret, Backspace deletes, cursor left/right move the caret.
 * <p>
 * The text is shown in one line, with "_" as caret ("-" if the font has no "_"). PASSWORD shows "*" instead of the characters.
 * <p>
 * Note: keys only reach a TextField inside a TextBox. In a Form, keys are not passed on to items yet.
 * <p>
 * The arcade style input (cycling through the letters) is still available as gfc.widgets.TextInput.
 *
 * @see javax.microedition.lcdui.TextField
 * @author Gustaf Stechmann
 */
public class TextField extends Item {

	public final static int
		ANY				= 0,
		EMAILADDR 		= 1,
		NUMERIC 		= 2,
		PHONENUMBER 	= 3,
		URL 			= 4,
		DECIMAL 		= 5,
		CONSTRAINT_MASK = 0xFFFF,

		PASSWORD 				= 0x10000,
		UNEDITABLE 				= 0x20000,
		SENSITIVE 				= 0x40000,
		NON_PREDICTIVE 			= 0x80000,
		INITIAL_CAPS_WORD 		= 0x100000,
		INITIAL_CAPS_SENTENCE 	= 0x200000;

	//caret: "_", or "-" if the font has no "_" (the default font ends with "Z")
	static char getCaret() {
		Font f = DisplayProperties.getFont();
		if (f == null || f.hasChar('_')) return '_';
		return '-';
	}

	private int				tf_constraints;
	private int 			tf_max_size;
	private StringBuffer 	tf_text 	= new StringBuffer();
	private int 			tf_caret 	= 0;

	private Label 			tf_label;


	public TextField( String label, String text, int maxSize, int constraints ) {
		super(label);

		if (maxSize <= 0) throw new IllegalArgumentException("maxSize must be > 0");

		tf_max_size 	= maxSize;
		tf_constraints 	= constraints;

		tf_label = new Label( 0, 0, getScreen(), "", DisplayProperties.getFont(), DisplayProperties.getPassiveColor() );
		setWidget(tf_label);

		setString(text);

		if (Boolean.parseBoolean(MIDlet.GetAppProperty("debug"))) System.out.println("TextField()");
	}


	//------- keyboard input, called by TextBox

	/** A character was typed on the keyboard. */
	void keyTyped(char c) {
		if (isUneditable()) 					return;
		if (c < 32 || c == 127) 				return;		//control characters (backspace, return ...)
		if (tf_text.length() >= tf_max_size) 	return;		//full

		String test = tf_text.toString().substring(0, tf_caret) + c + tf_text.toString().substring(tf_caret);
		if (!isValid(test, tf_constraints)) 	return;		//e.g. a letter in a NUMERIC field

		tf_text.insert(tf_caret, c);
		tf_caret++;
		update();
	}

	/** Backspace: delete the character before the caret. */
	void deleteBeforeCaret() {
		if (isUneditable()) return;
		if (tf_caret == 0) 	return;

		tf_text.deleteCharAt(tf_caret - 1);
		tf_caret--;
		update();
	}

	void caretLeft() {
		if (tf_caret > 0) tf_caret--;
		update();
	}

	void caretRight() {
		if (tf_caret < tf_text.length()) tf_caret++;
		update();
	}


	//------- MIDP methods

	public void delete( int offset, int length ) {
		if (offset < 0 || length < 0 || offset + length > tf_text.length()) throw new StringIndexOutOfBoundsException("delete " + offset + "," + length);

		String n = tf_text.toString().substring(0, offset) + tf_text.toString().substring(offset + length);
		checkValid(n);

		tf_text.delete(offset, offset + length);

		//caret stays at the same character
		if (tf_caret > offset + length) tf_caret -= length;
		else if (tf_caret > offset) 	tf_caret = offset;

		update();
	}

	public int getCaretPosition() {
		return tf_caret;
	}

	public int getChars( char[] data ) {
		if (data.length < tf_text.length()) throw new ArrayIndexOutOfBoundsException("array too small");

		tf_text.getChars(0, tf_text.length(), data, 0);
		return tf_text.length();
	}

	public int getConstraints() {
		return tf_constraints;
	}

	public int getMaxSize() {
		return tf_max_size;
	}

	public String getString() {
		return tf_text.toString();
	}

	public void insert( char[] data, int offset, int length, int position ) {
		insert( new String(data, offset, length), position );
	}

	public void insert( String src, int position ) {
		if (src == null) throw new NullPointerException();

		//like MIDP: position is clamped to 0..size()
		position = Math.max(0, Math.min(position, tf_text.length()));

		String n = tf_text.toString().substring(0, position) + src + tf_text.toString().substring(position);
		if (n.length() > tf_max_size) throw new IllegalArgumentException("text is longer than maxSize");
		checkValid(n);

		tf_text.insert(position, src);
		if (tf_caret >= position) tf_caret += src.length();

		update();
	}

	public void setChars( char[] data, int offset, int length ) {
		setString( data == null ? null : new String(data, offset, length) );
	}

	public void setConstraints( int constraints ) {
		//text that does not fit the new constraints is removed, like on the handset
		if (!isValid(tf_text.toString(), constraints)) {
			tf_text.setLength(0);
			tf_caret = 0;
		}

		tf_constraints = constraints;
		update();
	}

	public void setInitialInputMode( String characterSubset ) {
		//nop -- the PC keyboard has no input modes
	}

	public int setMaxSize( int maxSize ) {
		if (maxSize <= 0) throw new IllegalArgumentException("maxSize must be > 0");

		tf_max_size = maxSize;

		//cut text that is too long
		if (tf_text.length() > maxSize) {
			tf_text.setLength(maxSize);
			if (tf_caret > maxSize) tf_caret = maxSize;
			update();
		}

		return tf_max_size;
	}

	public void setString( String text ) {
		if (text == null) text = "";
		if (text.length() > tf_max_size) throw new IllegalArgumentException("text is longer than maxSize");
		checkValid(text);

		tf_text.setLength(0);
		tf_text.append(text);
		tf_caret = text.length();

		update();
	}

	public int size() {
		return tf_text.length();
	}


	//------- internals

	private boolean isUneditable() {
		return (tf_constraints & UNEDITABLE) != 0;
	}

	private void checkValid(String text) {
		if (!isValid(text, tf_constraints)) throw new IllegalArgumentException("text does not match the constraints: " + text);
	}

	/** Checks the text against the input constraint (NUMERIC, DECIMAL, PHONENUMBER ...). */
	static boolean isValid(String text, int constraints) {
		switch (constraints & CONSTRAINT_MASK) {
			case NUMERIC:
				//optional minus at the start, then digits
				for (int i = 0; i < text.length(); i++) {
					char c = text.charAt(i);
					if (c == '-' && i == 0) continue;
					if (c < '0' || c > '9') return false;
				}
				return true;

			case DECIMAL:
				boolean point = false;
				for (int i = 0; i < text.length(); i++) {
					char c = text.charAt(i);
					if (c == '-' && i == 0) continue;
					if (c == '.' && !point) { point = true; continue; }
					if (c < '0' || c > '9') return false;
				}
				return true;

			case PHONENUMBER:
				for (int i = 0; i < text.length(); i++) {
					if ("0123456789+*#pw".indexOf( text.charAt(i) ) == -1) return false;
				}
				return true;

			default:	//ANY, EMAILADDR, URL
				return true;
		}
	}

	/** Shows the text, with caret. */
	private void update() {
		StringBuffer shown = new StringBuffer();

		if ((tf_constraints & PASSWORD) != 0) {
			for (int i = 0; i < tf_text.length(); i++) shown.append('*');
		}
		else {
			shown.append(tf_text);
		}

		if (!isUneditable()) shown.insert(tf_caret, getCaret());

		tf_label.setString( shown.toString() );
	}
}
