package gfc.microedition.lcdui;

import gfc.microedition.midlet.*;

/**
 * A screen for entering text with the keyboard of the PC:
 * type characters, Backspace deletes, cursor left/right move the caret,
 * F1/F2 (soft keys) trigger the commands.
 *
 * @see javax.microedition.lcdui.TextBox
 * @author Gustaf Stechmann
 */

public class TextBox extends Screen {

	public static final Command OK_COMMAND = new Command(DisplayProperties.TEXT_OK, Command.OK, 0 );

	//one text field per TextBox (was static -- all TextBoxes shared one field)
	private TextField 	tb_textfield;


	public TextBox(String title, String text, int maxSize, int constraints) {
		super(title);

		tb_textfield 	= new TextField( "", text, maxSize, constraints );

		setContent(tb_textfield);

		if (Boolean.parseBoolean(MIDlet.GetAppProperty("debug"))) System.out.println("TextBox()");
	}


	//------- keyboard

	@Override
	boolean keyPressedPreprocess(int keyCode) {
		//soft keys trigger the commands
		if (softKeyPressed(keyCode)) return true;

		if 		(keyCode == Display.KEY_CLEAR) 	tb_textfield.deleteBeforeCaret();
		else if (keyCode == Display.KEY_LEFT) 	tb_textfield.caretLeft();
		else if (keyCode == Display.KEY_RIGHT) 	tb_textfield.caretRight();
		else 									return true;	//characters come via keyTypedPreprocess()

		changed();
		return true;
	}

	@Override
	void keyRepeatedPreprocess(int keyCode) {
		//holding backspace or the cursor keys repeats them
		if (keyCode == Display.KEY_CLEAR || keyCode == Display.KEY_LEFT || keyCode == Display.KEY_RIGHT) keyPressedPreprocess(keyCode);
	}

	@Override
	void keyTypedPreprocess(char c) {
		tb_textfield.keyTyped(c);
		changed();
	}

	/** the text has a new width -> center it again */
	private void changed() {
		posHome();
		repaint();
	}


	//------- MIDP methods, all passed on to the text field

	public String getString() {
		return tb_textfield.getString();
	}

	public void setString(String s) {
		if (Boolean.parseBoolean(MIDlet.GetAppProperty("debug"))) System.out.println("TextBox.setString()");

		tb_textfield.setString(s);

		changed();
	}

	public int getConstraints() {
		return tb_textfield.getConstraints();
	}

	public void setConstraints(int constraints) {
		tb_textfield.setConstraints(constraints);
		changed();
	}

	public void delete(int offset, int length) {
		tb_textfield.delete(offset, length);
		changed();
	}

	public int getCaretPosition() {
		return tb_textfield.getCaretPosition();
	}

	public int getChars(char[] data) {
		return tb_textfield.getChars(data);
	}

	public void setChars(char[] data, int offset, int length) {
		tb_textfield.setChars(data, offset, length);
		changed();
	}

	public void insert(String src, int position) {
		tb_textfield.insert(src, position);
		changed();
	}

	public void insert(char[] data, int offset, int length, int position) {
		tb_textfield.insert(data, offset, length, position);
		changed();
	}

	public int getMaxSize() {
		return tb_textfield.getMaxSize();
	}

	public int setMaxSize(int maxSize) {
		int m = tb_textfield.setMaxSize(maxSize);
		changed();
		return m;
	}

	public int size() {
		return tb_textfield.size();
	}

	public void setInitialInputMode(String characterSubset) {
		tb_textfield.setInitialInputMode(characterSubset);
	}
}
