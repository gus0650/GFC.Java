package gfc.microedition.midlet;

import gfc.microedition.io.ConnectionNotFoundException;
import gfc.microedition.lcdui.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public abstract class MIDlet extends Frame {

	Display display;
	static Properties properties = new Properties();
	
	public MIDlet() {
		super("UNDEF");

		System.out.println("MIDlet()");

		String workingDir = System.getProperty("user.dir");
		System.out.println("current working directory: " + workingDir);

		//window events -> MIDlet life cycle
		addWindowListener(new WindowAdapter() {
			//X button or Alt-F4
			public void windowClosing(WindowEvent e) {
				try {
					destroyApp(true);
				}
				catch (MIDletStateChangeException ex) {
					//unconditional destroy -- exception is ignored, like on the handset
				}
				notifyDestroyed();
			}

			//minimized -> pause, like an incoming call on the handset
			public void windowIconified(WindowEvent e) {
				if (paused) return;

				paused = true;
				pauseApp();
			}

			public void windowDeiconified(WindowEvent e) {
				resume();
			}
		});

		//create display
		display = new Display((Frame)this);
		add(display);

		//go fullscreen

		if (Boolean.parseBoolean(GetAppProperty("window.fullscreen"))) {
			GraphicsDevice g = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

			if (g.isFullScreenSupported()) {
				System.out.println("going fullscreen");
				setResizable(false);
				setUndecorated(true);
				g.setFullScreenWindow(this);
			}
			else {
				System.out.println("fullscreen not supported on this device");
			}

			//the screen resolution is NOT changed any more:
			//monitors scale small resolutions up blurry. Display scales the game itself, with sharp pixels.
		}
	}	 

	public static void main( String[] args ) {
		//read properties from file
		
		try {
			//this resource loading mechanism works from inside a JAR (both applet or application context)
			InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("midlet.properties");
			properties.load( is ) ;
	    }
		catch (Exception e) {
			e.printStackTrace();
		}
		
		//initialize Application
		Object o = null;
		
		try {
			Class c = Class.forName( properties.getProperty("class") );
			o = c.newInstance();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		MIDlet app = (MIDlet)o;

		//set Window properties
		app.setTitle(properties.getProperty("window.title"));

		if (!Boolean.parseBoolean(GetAppProperty("window.fullscreen"))) {
			//window = handset screen x scale factor (window.scale in midlet.properties)
			int scale = Display.getWindowScale();
			app.display.setPreferredSize( new Dimension(
				Display.getDisplayWidth()  * scale,
				Display.getDisplayHeight() * scale ) );
			app.pack();
			app.setLocationRelativeTo(null);	//center on screen
		}

		app.setVisible(true);
		app.display.requestFocusInWindow();

		app.resume();
	}
	
	//overriding update method to avoid flicker
	@Override
	final public void update( java.awt.Graphics g ) {
		paint( g );
	}

	//--- custom methods

	boolean paused = true;

	/** Calls startApp(), on first start and when coming back from pause. */
	void resume() {
		if (!paused) return;

		try {
			paused = false;
			startApp();
		}
		catch (MIDletStateChangeException e) {
			//MIDlet does not want to start now
			paused = true;
			System.err.println("WARNING in MIDlet.resume(): startApp() refused to start -- " + e.getMessage());
		}
	}

	public Display getDisplay() {
		return display;
	}


	//---inherited from MIDlet

	final protected void notifyStarted() { }

	/** The MIDlet tells us it has paused itself. */
	public final void notifyPaused() {
		paused = true;
	}

	/** The MIDlet tells us it is finished -> close the window and quit. */
	public final void notifyDestroyed() {
		dispose();
		System.exit(0);
	}

	/** The MIDlet wants to be started again after pausing itself. */
	public final void resumeRequest() {
		java.awt.EventQueue.invokeLater( () -> resume() );
	}

	abstract protected void startApp() throws MIDletStateChangeException;
	abstract protected void pauseApp();
	abstract protected void destroyApp(boolean unconditional) throws MIDletStateChangeException;

	/** MIDP version: reads a property of the MIDlet (from midlet.properties). */
	public final String getAppProperty(String key) {
		return properties.getProperty(key);
	}

	/**
	 * Opens a URL in the desktop's browser (the handset would open its WAP browser).
	 * "tel:" URLs can't be handled on the desktop.
	 * 
	 * @return false -- the request is handled while the MIDlet keeps running
	 */
	public final boolean platformRequest(String url) throws ConnectionNotFoundException {
		if (url == null || url.length() == 0) return false;	//empty URL cancels pending requests

		if (url.startsWith("tel:")) throw new ConnectionNotFoundException("ERROR in MIDlet.platformRequest(): phone calls are not supported - " + url);

		try {
			if (!java.awt.Desktop.isDesktopSupported()) throw new ConnectionNotFoundException("ERROR in MIDlet.platformRequest(): no browser available");

			java.awt.Desktop.getDesktop().browse( new java.net.URI(url) );
		}
		catch (ConnectionNotFoundException e) {
			throw e;
		}
		catch (Exception e) {
			throw new ConnectionNotFoundException("ERROR in MIDlet.platformRequest(): could not open - " + url);
		}

		return false;
	}

	/** All permissions are granted on the desktop. */
	public final int checkPermission(String permission) {
		return 1;
	}

	public static String GetAppProperty(String s) {
		return properties.getProperty(s);
	}
}
