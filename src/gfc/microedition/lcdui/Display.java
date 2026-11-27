package gfc.microedition.lcdui;

import gfc.graphics.Color;
import gfc.microedition.midlet.*;
import java.awt.event.*;
import java.util.HashMap;
import javax.swing.JPanel;

//final public class Display extends java.awt.Canvas implements MouseListener, KeyListener {
final public class Display extends JPanel implements MouseListener, MouseMotionListener, KeyListener  {
//using JPanel because it has paintImmediately(x,y,w,h);

	static final long serialVersionUID = 1L;
	
	Displayable current;
	Displayable previous;

	PaintThread painter = new PaintThread();
	gfc.microedition.lcdui.Graphics g = new Graphics();

	static Font font;

	//key codes as sent by Nokia handsets. digits, * and # use their character code (see Canvas.KEY_NUM0..9)
	public final static int
		KEY_UP    		= -1,	//cursor keys
		KEY_DOWN   		= -2,
		KEY_LEFT  		= -3,
		KEY_RIGHT 		= -4,
		KEY_FIRE  		= -5,	//center of the cursor keys
		KEY_SOFT_LEFT 	= -6,
		KEY_SOFT_RIGHT 	= -7,
		KEY_CLEAR 		= -8;

	//keys that are currently held down: awt key code -> MIDP key code
	HashMap<Integer, Integer> keys_down = new HashMap<Integer, Integer>();

	//game action bits for GameCanvas.getKeyStates()
	static int key_states;
	static int key_states_latched;
		
	public Display(java.awt.Frame frame) {
		setFocusable(true);
		
		addMouseListener(this);
		addMouseMotionListener(this);
		addKeyListener(this);
		
		setSize(frame.getWidth(), frame.getHeight());

		frame.add(this);
		
		painter.start();

		font = Font.loadFromProperties();
		if (font == null) System.err.println("WARNING in Display(): no bitmap font configured, using awt font");
	}

	public static Display getDisplay(MIDlet m) {
		return m.getDisplay();
	}
	
	public void setCurrent( Displayable s ) {
		if (s == current) {
			System.err.println("ERROR in Display.setCurrent(): this Screen is already the current one");
			return;
		}
		
		/* deactivate old Screen */
		if (current != null) {
			current.setInactive();
			current.hideNotify();

			previous = current;
		}

		/* activate new Screen */
		current = s;
		current.setActive(this);
		current.showNotify();

		repaint();
	}
	
	public void setPrevious() {
		if (current == null)
			return;
		
		if (previous == null)
			return;

		setCurrent(previous);
	}

	protected void showNotify() {
		if (current == null)	return;
		current.showNotify();
	}

	protected void hideNotify() {
		if (current == null)	return;
		current.hideNotify();
	}

	public Displayable getCurrent() {
		return current;
	}

	public void paint( Graphics g ) {
		if (current == null) 	return;

		//screens get title, ticker and command buttons; a Canvas paints the whole screen itself
		if (current instanceof Screen) 	current.draw( g );
		else 							current.paint( g );
	}


	//------- input events from AWT

	/**
	 * Translates an AWT key into a MIDP key code (Nokia layout):
	 * <pre>
	 * cursor keys      -> KEY_UP, KEY_DOWN, KEY_LEFT, KEY_RIGHT
	 * return, space    -> KEY_FIRE
	 * F1, F2           -> KEY_SOFT_LEFT, KEY_SOFT_RIGHT
	 * backspace        -> KEY_CLEAR
	 * 0..9, numpad 0..9, * and # -> Canvas.KEY_NUM0..9, KEY_STAR, KEY_POUND
	 * </pre>
	 * @return MIDP key code, or 0 if the key has no meaning on a handset
	 */
	static int translateKey( KeyEvent ke ) {
		int k = ke.getKeyCode();

		switch (k) {
			case KeyEvent.VK_UP: 			return KEY_UP;
			case KeyEvent.VK_DOWN: 			return KEY_DOWN;
			case KeyEvent.VK_LEFT: 			return KEY_LEFT;
			case KeyEvent.VK_RIGHT: 		return KEY_RIGHT;
			case KeyEvent.VK_KP_UP: 		return KEY_UP;		//numpad with NumLock off
			case KeyEvent.VK_KP_DOWN: 		return KEY_DOWN;
			case KeyEvent.VK_KP_LEFT: 		return KEY_LEFT;
			case KeyEvent.VK_KP_RIGHT: 		return KEY_RIGHT;
			case KeyEvent.VK_BEGIN: 		return KEY_FIRE;	//numpad 5 with NumLock off
			case KeyEvent.VK_CLEAR: 		return KEY_FIRE;
			case KeyEvent.VK_ENTER: 		return KEY_FIRE;
			case KeyEvent.VK_SPACE: 		return KEY_FIRE;
			case KeyEvent.VK_F1: 			return KEY_SOFT_LEFT;
			case KeyEvent.VK_F2: 			return KEY_SOFT_RIGHT;
			case KeyEvent.VK_BACK_SPACE: 	return KEY_CLEAR;
			case KeyEvent.VK_MULTIPLY: 		return Canvas.KEY_STAR;
		}

		//numpad
		if ( (k >= KeyEvent.VK_NUMPAD0) & (k <= KeyEvent.VK_NUMPAD9) ) return Canvas.KEY_NUM0 + (k - KeyEvent.VK_NUMPAD0);

		//number row
		if ( (k >= KeyEvent.VK_0) & (k <= KeyEvent.VK_9) & !ke.isShiftDown() ) return Canvas.KEY_NUM0 + (k - KeyEvent.VK_0);

		//* and # depend on the keyboard layout, so use the typed character
		char c = ke.getKeyChar();
		if (c == '*') return Canvas.KEY_STAR;
		if (c == '#') return Canvas.KEY_POUND;

		return 0;
	}

	/** Bit for GameCanvas.getKeyStates(), see GameCanvas.UP_PRESSED etc. */
	static int gameActionBit( int keyCode ) {
		int action = Canvas.keyToGameAction(keyCode);
		if (action <= 0) return 0;

		return 1 << action;
	}

	/** Used by GameCanvas.getKeyStates(): returns held keys plus keys pressed since the last call. */
	public static int getKeyStates() {
		int states = key_states | key_states_latched;
		key_states_latched = 0;

		return states;
	}
	
	@Override
	public void keyPressed( KeyEvent ke ) {
		int k = translateKey(ke);
		if (k == 0) return;

		//awt sends repeated keyPressed events while a key is held down
		boolean repeated = keys_down.containsKey( ke.getKeyCode() );
		keys_down.put( ke.getKeyCode(), k );

		key_states 			|= gameActionBit(k);
		key_states_latched 	|= gameActionBit(k);

		if (current == null)	return;

		if (repeated) 	current.keyRepeatedPreprocess(k);
		else 			current.keyPressedPreprocess(k);
	}

	@Override
	public void keyReleased( KeyEvent ke ) {
		//use the code from keyPressed, the typed character may have changed meanwhile (shift released)
		Integer k = keys_down.remove( ke.getKeyCode() );
		if (k == null) return;

		key_states &= ~gameActionBit(k);

		if (current == null)	return;
		current.keyReleasedPreprocess(k);
	}

	@Override
	public void keyTyped( KeyEvent ke ) {
		//nop -- awt delivers no key code here, keyPressed() handles everything
	}

	@Override
	public void mousePressed( MouseEvent e ) {
		requestFocusInWindow();

		if (current == null)	return;
		current.pointerPressedPreprocess( e.getX(), e.getY() );
	}

	@Override
	public void mouseReleased( MouseEvent e ) {
		if (current == null)	return;
		current.pointerReleasedPreprocess( e.getX(), e.getY() );
	}

	@Override
	public void mouseDragged( MouseEvent e ) {
		if (current == null)	return;
		current.pointerDraggedPreprocess( e.getX(), e.getY() );
	}

	@Override
	public void mouseMoved( MouseEvent e ) { 
		//nop -- handsets have no mouse pointer
	}

	@Override
	public void mouseExited( MouseEvent e ) { 
		//nop
	}

	@Override
	public void mouseEntered( MouseEvent e ) {
		//nop
	}

	@Override
	public void mouseClicked( MouseEvent e ) {
		//nop
	}

	
	//------painting
	
	public void threadedRepaint() {
		//System.out.println("threadedRepaint()");
		painter.fullClip();
		painter.run();
	}

	public void threadedRepaint(int x, int y, int w, int h) {
		//System.out.println("threadedRepaint() xywh");
		painter.fullClip();		//painter.setClip(x, y, w, h);
		painter.run();
	}

	class PaintThread extends Thread {
		
		private int x, y, w, h;
		
		public void run() {
			repaint();
			paintImmediately(x,y,w,h);
		}
		
		void setClip(int x, int y, int w, int h) {
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
		}
		
		void fullClip() {
			x = 0;
			y = 0;
			w = getWidth();
			h = getHeight();
		}
	}
	
	public void paint(java.awt.Graphics g) {
		this.g.set(g);
		paint(this.g );
	}


	//----- custom methods

	static Color acol = new Color(
		Integer.parseInt(MIDlet.GetAppProperty("COL_ACTIVE")) );
	public static Color getActiveColor() {
		return acol;
	}

	static Color pcol = new Color(
		Integer.parseInt(MIDlet.GetAppProperty("COL_PASSIVE")) );
	public static Color getPassiveColor() {
		return pcol;
	}

	public static Font getGFCFont() {
		return font;
	}

	//note: WIDTH and HEIGHT are constants inherited from awt (ImageObserver), not the screen size!
	public static int getDisplayWidth() {
		return Integer.parseInt( MIDlet.GetAppProperty("window.width") );
	}

	public static int getDisplayHeight() {
		return Integer.parseInt( MIDlet.GetAppProperty("window.height") );
	}

	public static int getDefaultBorderVMargin() {
		return Integer.parseInt(MIDlet.GetAppProperty("BORDERV"));
	}

	public static int getDefaultBorderHMargin() {
		return Integer.parseInt(MIDlet.GetAppProperty("BORDERH"));
	}

	public static Image getDefaultMenuitemInactive(){
		return null; //TODO
	}

	public static Image getDefaultMenuitemActive(){
		return null; //TODO
	}

}