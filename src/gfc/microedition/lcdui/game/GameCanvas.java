package gfc.microedition.lcdui.game;

import  gfc.microedition.lcdui.*;

/**
 * @see javax.microedition.lcdui.game.GameCanvas
 * @author Gustaf Stechmann
 */
public abstract class GameCanvas extends Canvas {

	public static final int
		UP_PRESSED      = 1 << Canvas.UP,		//0x0002
		DOWN_PRESSED    = 1 << Canvas.DOWN,		//0x0040
		LEFT_PRESSED    = 1 << Canvas.LEFT,		//0x0004
		RIGHT_PRESSED   = 1 << Canvas.RIGHT,	//0x0020
		FIRE_PRESSED    = 1 << Canvas.FIRE,		//0x0100
		GAME_A_PRESSED  = 1 << Canvas.GAME_A,	//0x0200
		GAME_B_PRESSED  = 1 << Canvas.GAME_B,	//0x0400
		GAME_C_PRESSED  = 1 << Canvas.GAME_C,	//0x0800
		GAME_D_PRESSED  = 1 << Canvas.GAME_D;	//0x1000

	//offscreen buffer, created on first use
	private Image buffer;
	private Graphics buffer_graphics;


	protected GameCanvas(boolean suppressKeyEvents) {
		setSuppressGameKeyEvents(suppressKeyEvents);
	}


	/**
	 * Returns the Graphics of the offscreen buffer. Call flushGraphics() to show it.
	 */
	protected Graphics getGraphics() {
		if (buffer == null) {
			buffer 			= Image.createImage( Display.getDisplayWidth(), Display.getDisplayHeight() );
			buffer_graphics = buffer.getGraphics();
		}

		return buffer_graphics;
	}

	/**
	 * Returns the game keys that are held down or were pressed since the last call.
	 */
	public int getKeyStates() {
		return Display.getKeyStates();
	}

	/**
	 * Shows the offscreen buffer on the screen, right now.
	 */
	public void flushGraphics() {
		if (display == null) return;

		display.threadedRepaint();
	}

	public void flushGraphics(int x, int y, int width, int height) {
		if (display == null) return;

		display.threadedRepaint(x, y, width, height);
	}

	/**
	 * Paints the offscreen buffer.
	 */
	@Override
	public void paint(Graphics g) {
		if (buffer == null) return;

		g.drawImage( buffer, 0, 0, Graphics.TOP | Graphics.LEFT );
	}
}
