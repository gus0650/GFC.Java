package gfc.microedition.lcdui;

//import java.awt.Toolkit;
import gfc.microedition.midlet.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;


public class Image {

//	private java.awt.Image awtimage;
	BufferedImage awtimage;

	boolean mutable = false;
	
	
	/*
	private Image (java.awt.Image i) {
		awtimage = i;
	}
	*/
	
	private Image (BufferedImage i) {
		awtimage = i;
	}
	
	public static Image createImage(String filename) throws IOException, IllegalArgumentException {
		if (Boolean.parseBoolean(MIDlet.GetAppProperty("debug"))) System.out.println("Image.createImage "+filename);
		
		if ((filename == null) || (filename.length() == 0)) throw new IllegalArgumentException("no filename");

		/*
		//this code works only in applet context
		java.awt.Image i = applet.getImage(applet.getCodeBase(), filename);
		*/

		/*
		//this code works only in application context BUT NOT from inside a .jar!
		Toolkit tk = Toolkit.getDefaultToolkit();
		java.awt.Image i = tk.createImage( filename );
		tk.prepareImage(i, -1, -1, null);
		*/

		/*
		//this code works in both applet and application context, but has PNG parsing problems!
		InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
		byte[] b = new byte[is.available()];
		if ( b.length == 0 ) throw new IOException("ERROR in Image.create(): it seems that the file was not properly opened - " + filename );
		is.read(b);
		is.close();
		Toolkit tk = Toolkit.getDefaultToolkit();
		java.awt.Image i = tk.createImage( b );
		*/

		/*
		//third version
		Toolkit tk = Toolkit.getDefaultToolkit();
		java.awt.Image i = tk.getImage( tk.getClass().getResource( filename ) );
		tk.prepareImage(i, -1, -1, null);
		*/
		
		/*
		//optional: make sure image is fully read before continuing
		MediaTracker mediaTracker = new MediaTracker( );
		mediaTracker.addImage(i, 0);
		try	{
			mediaTracker.waitForID(0);
		} catch (Exception e) {
			e.printStackTrace();
		}
		*/
		
		/*
		//fourth version -- works, but NOT from inside a .jar
		BufferedImage i = null;
		 try {                
	          i = ImageIO.read(new File( filename ));
	       } catch (IOException ex) {
	            ex.printStackTrace();
	       }
		*/

		//fifth version -- like MIDP: load from classpath (also from inside a .jar),
		//fall back to the file system so that "res/duke.jpg" still works from the project folder
		InputStream is = openResource(filename);
		if (is == null) throw new IOException("ERROR in Image.createImage(): file not found - " + filename );

		BufferedImage i = null;
		try {
			i = ImageIO.read(is);
		}
		finally {
			is.close();
		}

		if ( i == null ) throw new IOException("ERROR in Image.createImage(): unknown image format - " + filename );

		if (Boolean.parseBoolean(MIDlet.GetAppProperty("debug"))) System.out.println("Image.load(): w=" + i.getWidth() + " h=" + i.getHeight());
		
		return new Image( toARGB(i) );
	}

	/**
	 * Opens a resource the MIDP way: "/res/img.png" and "res/img.png" are both looked up in the classpath.
	 * If not found there, the file system (relative to the working directory) is tried.
	 */
	static InputStream openResource(String filename) throws IOException {
		String path = filename.startsWith("/") ? filename : "/" + filename;

		InputStream is = Image.class.getResourceAsStream(path);
		if (is != null) return is;

		File f = new File( filename.startsWith("/") ? filename.substring(1) : filename );
		if (!f.exists()) f = new File(filename);
		if (!f.exists()) return null;

		return new FileInputStream(f);
	}

	/** Converts any BufferedImage to TYPE_INT_ARGB, so that drawing, getRGB and transforms behave the same for all images. */
	static BufferedImage toARGB(BufferedImage i) {
		if (i.getType() == BufferedImage.TYPE_INT_ARGB) return i;

		BufferedImage n = new BufferedImage( i.getWidth(), i.getHeight(), BufferedImage.TYPE_INT_ARGB );
		java.awt.Graphics g = n.getGraphics();
		g.drawImage( i, 0, 0, null );
		g.dispose();

		return n;
	}

	public int getWidth() {
		return awtimage.getWidth(null);
	}
	
	public int getHeight() {
		return awtimage.getHeight(null);
	}

	public Graphics getGraphics() {
		if (!mutable) throw new IllegalStateException("ERROR in Image.getGraphics(): image is immutable");

		Graphics igfx = new Graphics();
		igfx.set( awtimage.getGraphics() );
		
		return igfx;
	}
	
	java.awt.Image getAWTImage() {
		return awtimage;
	}
	
	public boolean isMutable() {
		return mutable;
	}
	
	static public Image createImage(byte[] data, int offset, int length) {
		if (data == null) throw new NullPointerException();

		BufferedImage i = null;
		try {
			i = ImageIO.read( new ByteArrayInputStream( data, offset, length ) );
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		if (i == null) throw new IllegalArgumentException("ERROR in Image.createImage(): data could not be decoded");

		return new Image( toARGB(i) );
	}

	static public Image createImage(InputStream stream) throws IOException {
		if (stream == null) throw new NullPointerException();

		BufferedImage i = ImageIO.read(stream);
		if (i == null) throw new IOException("ERROR in Image.createImage(): stream could not be decoded");

		return new Image( toARGB(i) );
	}
	
	/**
	 * Creates a new mutable image. Like on the handset, it is initially white.
	 */
	static public Image createImage(int w, int h) {
		if ( (w <= 0) | (h <= 0) ) throw new IllegalArgumentException("width and height must be > 0");

		BufferedImage i = new BufferedImage( w, h, BufferedImage.TYPE_INT_ARGB );
		java.awt.Graphics g = i.getGraphics();
		g.setColor( java.awt.Color.WHITE );
		g.fillRect( 0, 0, w, h );
		g.dispose();

		Image img 	= new Image(i);
		img.mutable = true;

		return img;
	}

	/**
	 * Creates an immutable copy of an image.
	 */
	static public Image createImage(Image source) {
		if (!source.mutable) return source;

		return createImage( source, 0, 0, source.getWidth(), source.getHeight(), Graphics.TRANS_NONE );
	}

	/**
	 * Creates an immutable image from a region of another image, optionally mirrored and/or rotated.
	 */
	static public Image createImage(Image image, int x, int y, int width, int height, int transform) {
		boolean swap 	= (transform >= Graphics.TRANS_MIRROR_ROT270);
		int w 			= swap ? height : width;
		int h 			= swap ? width : height;

		BufferedImage i = new BufferedImage( w, h, BufferedImage.TYPE_INT_ARGB );

		Graphics g = new Graphics();
		g.set( i.getGraphics() );
		g.drawRegion( image, x, y, width, height, transform, 0, 0, Graphics.TOP | Graphics.LEFT );
		g.dispose();

		return new Image(i);
	}

	/**
	 * Creates an immutable image from an array of ARGB pixels.
	 */
	static public Image createRGBImage(int[] rgb, int width, int height, boolean processAlpha) {
		if (rgb == null) throw new NullPointerException();
		if ( (width <= 0) | (height <= 0) ) throw new IllegalArgumentException("width and height must be > 0");

		BufferedImage i = new BufferedImage( width, height, BufferedImage.TYPE_INT_ARGB );

		if (processAlpha) {
			i.setRGB( 0, 0, width, height, rgb, 0, width );
		}
		else {
			int[] opaque = new int[width * height];
			for (int n = 0; n < opaque.length; n++) opaque[n] = rgb[n] | 0xFF000000;
			i.setRGB( 0, 0, width, height, opaque, 0, width );
		}

		return new Image(i);
	}

	public void getRGB(int[] rgbData, int offset, int scanlength, int x, int y, int width, int height) {
		awtimage.getRGB( x, y, width, height, rgbData, offset, scanlength );
	}
	
	public void setRGB( int x, int y, int w, int h, int[] rgbArray, int offset, int scansize ) {
		awtimage.setRGB( x, y, w, h, rgbArray, offset, scansize );
	}
}
