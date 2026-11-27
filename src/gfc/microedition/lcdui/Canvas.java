
package gfc.microedition.lcdui;

/**
 * @see javax.microedition.lcdui.Canvas
 * @author Gustaf Stechmann
 */
public abstract class Canvas extends Displayable {

	public final static int
		UP    		= 1,
		DOWN   		= 6,
		LEFT  		= 2,
		RIGHT 		= 5,
		FIRE   		= 8,
		GAME_A 		= 9,
		GAME_B 		= 10,
		GAME_C 		= 11,
		GAME_D 		= 12,
		KEY_NUM0 	= 48,
		KEY_NUM1 	= 49,
		KEY_NUM2 	= 50,
		KEY_NUM3 	= 51,
		KEY_NUM4 	= 52,
		KEY_NUM5 	= 53,
		KEY_NUM6 	= 54,
		KEY_NUM7 	= 55,
		KEY_NUM8 	= 56,
		KEY_NUM9 	= 57,
		KEY_STAR  	= 42,
		KEY_POUND 	= 35;

	//set by GameCanvas: game keys are only available through getKeyStates()
	private boolean suppress_game_keys = false;


	//note: in MIDP these methods are protected. they must stay protected here,
	//otherwise games that override them as protected would not compile.

	@Override
	protected abstract void paint(Graphics g);


	public boolean hasPointerEvents() {
		return true;
	}

	public boolean hasPointerMotionEvents() {
		return true;
	}

	public boolean hasRepeatEvents() {
		return true;	//awt repeats held keys
	}

	public boolean isDoubleBuffered() {
		return true; //double buffering is handled by AWT
	}

	protected void keyPressed(int keyCode) {}

	protected void keyReleased(int keyCode) {}

	protected void keyRepeated(int keyCode) {}

	protected void pointerPressed(int x, int y) {}

	protected void pointerReleased(int x, int y) {}

	protected void pointerDragged(int x, int y) {}

	@Override
	protected void hideNotify() {}

	@Override
	protected void showNotify() {}


	//--- repaint

	public final void repaint(int x, int y, int width, int height) {
		if (display == null) return;
		super.repaint(x, y, width, height);
	}

	/** Paints all pending repaint requests immediately. */
	public final void serviceRepaints() {
		if (display == null) return;
		display.threadedRepaint();
	}

	/** a Canvas always covers the whole screen (no title, ticker or soft key area is shown) */
	@Override
	public int getHeight() {
		return Display.getDisplayHeight();
	}

	public void setFullScreenMode(boolean mode) {
		//nop -- a Canvas always covers the whole window
	}


	//--- input from Display

	@Override
	boolean keyPressedPreprocess(int keyCode) {
		//soft keys trigger commands, if there are any on this canvas
		if (softKeyPressed(keyCode)) return true;

		if (isSuppressed(keyCode)) return true;

		keyPressed(keyCode);
		return true;
	}

	@Override
	void keyReleasedPreprocess(int keyCode) {
		if (isSuppressed(keyCode)) return;

		keyReleased(keyCode);
	}

	@Override
	void keyRepeatedPreprocess(int keyCode) {
		if (isSuppressed(keyCode)) return;

		keyRepeated(keyCode);
	}

	@Override
	void pointerPressedPreprocess(int x, int y) {
		pointerPressed(x, y);
	}

	@Override
	void pointerReleasedPreprocess(int x, int y) {
		pointerReleased(x, y);
	}

	@Override
	void pointerDraggedPreprocess(int x, int y) {
		pointerDragged(x, y);
	}

	/** Used by GameCanvas. */
	protected final void setSuppressGameKeyEvents(boolean suppress) {
		suppress_game_keys = suppress;
	}

	private boolean isSuppressed(int keyCode) {
		return suppress_game_keys && (keyToGameAction(keyCode) > 0);
	}


	//--- keys

	/**
	 * Maps a key code to a game action. Works for the number keys and the cursor keys.
	 */
	static int keyToGameAction(int keyCode) {
		if (keyCode == Display.KEY_UP) 		return UP;
		if (keyCode == Display.KEY_DOWN) 	return DOWN;
		if (keyCode == Display.KEY_LEFT) 	return LEFT;
		if (keyCode == Display.KEY_RIGHT) 	return RIGHT;
		if (keyCode == Display.KEY_FIRE) 	return FIRE;

		if (keyCode == KEY_POUND) 	return GAME_A;
		if (keyCode == KEY_STAR) 	return GAME_B;
		if (keyCode == KEY_NUM0) 	return GAME_C;
		if (keyCode == KEY_NUM9) 	return GAME_D;
		if (keyCode == KEY_NUM2) 	return UP;
		if (keyCode == KEY_NUM8) 	return DOWN;
		if (keyCode == KEY_NUM4) 	return LEFT;
		if (keyCode == KEY_NUM6) 	return RIGHT;
		if (keyCode == KEY_NUM5) 	return FIRE;

		return 0;
	}

	public int getGameAction(int keyCode) {
		return keyToGameAction(keyCode);
	}

	public int getKeyCode(int gameAction) {
		if (gameAction == GAME_A) 	return KEY_POUND;
		if (gameAction == GAME_B) 	return KEY_STAR;
		if (gameAction == GAME_C) 	return KEY_NUM0;
		if (gameAction == GAME_D) 	return KEY_NUM9;
		if (gameAction == UP) 		return Display.KEY_UP;
		if (gameAction == DOWN) 	return Display.KEY_DOWN;
		if (gameAction == LEFT) 	return Display.KEY_LEFT;
		if (gameAction == RIGHT) 	return Display.KEY_RIGHT;
		if (gameAction == FIRE) 	return Display.KEY_FIRE;

		throw new IllegalArgumentException("unknown game action " + gameAction);
	}

	public String getKeyName(int keyCode) {
		if ( (keyCode >= KEY_NUM0) & (keyCode <= KEY_NUM9) ) return String.valueOf( (char)keyCode );

		if (keyCode == KEY_STAR) 				return "*";
		if (keyCode == KEY_POUND) 				return "#";
		if (keyCode == Display.KEY_UP) 			return "UP";
		if (keyCode == Display.KEY_DOWN) 		return "DOWN";
		if (keyCode == Display.KEY_LEFT) 		return "LEFT";
		if (keyCode == Display.KEY_RIGHT) 		return "RIGHT";
		if (keyCode == Display.KEY_FIRE) 		return "SELECT";
		if (keyCode == Display.KEY_SOFT_LEFT) 	return "SOFT1";
		if (keyCode == Display.KEY_SOFT_RIGHT) 	return "SOFT2";
		if (keyCode == Display.KEY_CLEAR) 		return "CLEAR";

		throw new IllegalArgumentException("unknown key code " + keyCode);
	}
}
