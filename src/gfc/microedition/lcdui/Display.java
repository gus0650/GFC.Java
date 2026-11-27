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
	
	/**
	 * Shows the alert, then nextDisplayable -- after the alert is dismissed or its timeout has passed.
	 */
	public void setCurrent( Alert alert, Displayable nextDisplayable ) {
		if (alert == null || nextDisplayable == null) 	throw new NullPointerException();
		if (nextDisplayable instanceof Alert) 			throw new IllegalArgumentException("nextDisplayable must not be an Alert");

		setCurrent(alert);

		//DISMISS goes back to "previous" -> make that the next displayable
		previous = nextDisplayable;

		//timeout
		final int timeout = alert.getTimeout();
		if (timeout == Alert.FOREVER || timeout <= 0) return;

		final Alert a 			= alert;
		final Displayable next 	= nextDisplayable;

		javax.swing.Timer t = new javax.swing.Timer( timeout, e -> {
			//only if the alert is still on screen
			if (current == a) setCurrent(next);
		});
		t.setRepeats(false);
		t.start();
	}

	/**
	 * Shows the Form that contains item.
	 */
	public void setCurrentItem( Item item ) {
		if (item == null) throw new NullPointerException();

		Screen s = item.getScreen();
		if (s == null) 		throw new IllegalStateException("item is not owned by a Form");
		if (s instanceof Alert) throw new IllegalStateException("item is owned by an Alert");

		if (s != current) setCurrent(s);

		//TODO scroll to the item (Screen has no "flip to item" yet)
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


	//------- device properties

	public final static int
		LIST_ELEMENT 		= 1,	//for getBestImageWidth/Height
		CHOICE_GROUP_ELEMENT = 2,
		ALERT 				= 3;

	public boolean isColor() {
		return true;
	}

	/** Number of colors (or gray levels on a b/w handset). The desktop has 24 bit. */
	public int numColors() {
		return 1 << 24;
	}

	public int numAlphaLevels() {
		return 256;
	}

	/** The desktop has no vibrator. @return false, like a handset without vibrator */
	public boolean vibrate(int duration) {
		if (duration < 0) throw new IllegalArgumentException("negative duration");
		return false;
	}

	/** The desktop has no backlight to flash. @return false */
	public boolean flashBacklight(int duration) {
		if (duration < 0) throw new IllegalArgumentException("negative duration");
		return false;
	}

	/**
	 * Runs r after all pending repaints, in the painting thread (the awt event thread).
	 * Games use this for their game loop.
	 */
	public void callSerially(Runnable r) {
		if (r == null) return;
		java.awt.EventQueue.invokeLater(r);
	}

	public int getBestImageWidth(int imageType) {
		checkImageType(imageType);
		return (imageType == ALERT) ? getDisplayWidth() : getGFCFont() == null ? 16 : getGFCFont().getHeight() * 2;
	}

	public int getBestImageHeight(int imageType) {
		checkImageType(imageType);
		return (imageType == ALERT) ? getDisplayHeight() / 2 : getGFCFont() == null ? 16 : getGFCFont().getHeight() * 2;
	}

	private static void checkImageType(int imageType) {
		if ( (imageType < LIST_ELEMENT) | (imageType > ALERT) ) throw new IllegalArgumentException("unknown image type " + imageType);
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

	boolean pointer_down = false;

	@Override
	public void mousePressed( MouseEvent e ) {
		requestFocusInWindow();

		if (current == null)	return;
		if (!isOnDisplay( e.getX(), e.getY() )) return;	//click on the black border

		pointer_down = true;
		current.pointerPressedPreprocess( toDisplayX(e.getX()), toDisplayY(e.getY()) );
	}

	@Override
	public void mouseReleased( MouseEvent e ) {
		if (current == null)	return;
		if (!pointer_down) 		return;

		pointer_down = false;
		current.pointerReleasedPreprocess( toDisplayX(e.getX()), toDisplayY(e.getY()) );
	}

	@Override
	public void mouseDragged( MouseEvent e ) {
		if (current == null)	return;
		if (!pointer_down) 		return;

		//keep the pointer on the handset screen while dragging
		int x = Math.max( 0, Math.min( getDisplayWidth()  - 1, toDisplayX(e.getX()) ) );
		int y = Math.max( 0, Math.min( getDisplayHeight() - 1, toDisplayY(e.getY()) ) );
		current.pointerDraggedPreprocess( x, y );
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
	
	//------ scaling
	//
	//the game always paints into a buffer of window.width x window.height (the handset screen).
	//the buffer is scaled up by a whole factor with nearest neighbour -> sharp pixels, black borders.
	//the screen resolution is not changed any more, monitors scale small resolutions up blurry.

	java.awt.image.BufferedImage buffer;

	int scale = 1;			//current scale factor
	int offset_x, offset_y;	//position of the scaled buffer in the window

	public void paint(java.awt.Graphics awtg) {
		int w = getDisplayWidth();
		int h = getDisplayHeight();

		if (buffer == null || buffer.getWidth() != w || buffer.getHeight() != h) {
			buffer = new java.awt.image.BufferedImage( w, h, java.awt.image.BufferedImage.TYPE_INT_RGB );
		}

		//let the game paint into the buffer
		java.awt.Graphics2D bg = buffer.createGraphics();
		this.g.set(bg);
		paint(this.g);
		bg.dispose();

		//largest whole factor that fits into the window; if the window is too small, shrink to fit
		calculateScale();

		java.awt.Graphics2D g2 = (java.awt.Graphics2D)awtg;
		g2.setRenderingHint( java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR );

		//black borders
		g2.setColor( java.awt.Color.BLACK );
		g2.fillRect( 0, 0, getWidth(), getHeight() );

		if (scale > 0) {
			g2.drawImage( buffer, offset_x, offset_y, w * scale, h * scale, null );
		}
		else {
			//window smaller than the handset screen
			double f = Math.min( getWidth() / (double)w, getHeight() / (double)h );
			g2.drawImage( buffer, offset_x, offset_y, (int)(w * f), (int)(h * f), null );
		}
	}

	void calculateScale() {
		int w = getDisplayWidth();
		int h = getDisplayHeight();

		scale = Math.min( getWidth() / w, getHeight() / h );

		if (scale > 0) {
			offset_x = ( getWidth()  - w * scale ) / 2;
			offset_y = ( getHeight() - h * scale ) / 2;
		}
		else {
			double f = Math.min( getWidth() / (double)w, getHeight() / (double)h );
			offset_x = (int)( getWidth()  - w * f ) / 2;
			offset_y = (int)( getHeight() - h * f ) / 2;
		}
	}

	/** window x -> handset x */
	int toDisplayX(int x) {
		if (scale > 0) return (x - offset_x) / scale;
		double f = Math.min( getWidth() / (double)getDisplayWidth(), getHeight() / (double)getDisplayHeight() );
		return (int)( (x - offset_x) / f );
	}

	/** window y -> handset y */
	int toDisplayY(int y) {
		if (scale > 0) return (y - offset_y) / scale;
		double f = Math.min( getWidth() / (double)getDisplayWidth(), getHeight() / (double)getDisplayHeight() );
		return (int)( (y - offset_y) / f );
	}

	/** true if the window position is on the handset screen and not on the black border */
	boolean isOnDisplay(int x, int y) {
		int dx = toDisplayX(x);
		int dy = toDisplayY(y);
		return (x >= offset_x) && (y >= offset_y) && (dx < getDisplayWidth()) && (dy < getDisplayHeight());
	}

	/**
	 * Window scale from midlet.properties: window.scale=2, or "auto" for the largest factor
	 * that fits on the screen (80% of it, so the window does not cover everything).
	 */
	public static int getWindowScale() {
		String s = MIDlet.GetAppProperty("window.scale");

		if (s != null && !s.trim().equalsIgnoreCase("auto")) return Math.max(1, Integer.parseInt(s.trim()));

		java.awt.Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
		int f = Math.min( (screen.width * 8 / 10) / getDisplayWidth(), (screen.height * 8 / 10) / getDisplayHeight() );

		return Math.max(1, f);
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

	//menu items are drawn as boxes, no image files needed.
	//inactive: outline only, active: filled box. colors from midlet.properties (COL_ACTIVE)

	static Image menuitem_inactive, menuitem_active;

	public static Image getDefaultMenuitemInactive(){
		if (menuitem_inactive == null) menuitem_inactive = createMenuitemBox(false);
		return menuitem_inactive;
	}

	public static Image getDefaultMenuitemActive(){
		if (menuitem_active == null) menuitem_active = createMenuitemBox(true);
		return menuitem_active;
	}

	private static Image createMenuitemBox(boolean active) {
		int w = getDisplayWidth() - getDefaultBorderHMargin() * 2;
		int h = (getGFCFont() == null ? 12 : getGFCFont().getHeight()) + 4;

		int col 	= 0xFF000000 | getActiveColor().getRGB();
		int[] rgb 	= new int[w * h];	//all transparent

		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				boolean border = (x == 0) | (y == 0) | (x == w - 1) | (y == h - 1);
				boolean corner = ((x == 0) | (x == w - 1)) & ((y == 0) | (y == h - 1));

				if (corner) 		continue;	//rounded look
				if (border | active) rgb[y * w + x] = col;
			}
		}

		return Image.createRGBImage(rgb, w, h, true);
	}

}