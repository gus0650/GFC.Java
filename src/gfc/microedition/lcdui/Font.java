package gfc.microedition.lcdui;

import gfc.graphics.*;
import gfc.microedition.midlet.*;


/**
 * A fixed-with Font consisting of bitmap letters.
 * @author Gustaf Stechmann
 */
public class Font extends TiledImage {

	public static final int 
	STYLE_PLAIN			= 0,
	STYLE_BOLD 			= 1,
	STYLE_ITALIC 		= 2,
	STLYE_UNDERLINED	= 4;

	public static final int 
	SIZE_SMALL			= 8,
	SIZE_MEDIUM			= 0,
	SIZE_LARGE			= 16;
	
	public static final int 
	FACE_SYSTEM			= 0,
	FACE_MONOSPACE 		= 32,
	FACE_PROPORTIONAL	= 64;
	
	private int offset;
	private int hspacing, vspacing;

	
	/**
	 * Construct a new BitmapFont object
	 * 
	 * @param image - tiled font image to used
	 * @param width - width of one character-tile in pixels
	 * @param height - height of one character-tile in pixels
	 * @param horz_spacing - horizontal spacing
	 * @param vert_spacing - vertical spacing
	 */
	public Font(Image image, int width, int height, int horz_spacing, int vert_spacing, int offset ) {
		super(image, width, height);
		
		this.hspacing 	= horz_spacing;
		this.vspacing 	= vert_spacing;
		this.offset		= offset;
	}


	/**
	 * Draws a String at a given position.
	 */
	void drawString(String text, int x, int y, Graphics g) {
		int start 	= 0;
		int len 	= text.length();
		int end		= start + len;

		int indent_x = x;
		
		//make uppercase if necessary
		if (!hasSmallLetters()) text = text.toUpperCase();
		
		//draw each character
		for (int i = start; i < end; i++) {
			char character 	= text.charAt(i);
			char mask		= ' ';
			
			//white space
			if (character == 32) {
				x = x + tile_width + hspacing;
				continue;
			}

			//line break
			if (character == 10) {
				x = indent_x;
				y = y + tile_height + vspacing;
				continue;
			}

			//Umlaute
			if (character == 'ö') { character = 'o';	mask = '\"'; }
			if (character == 'Ö') { character = 'O';	mask = '\"'; }
			if (character == 'ä') { character = 'a';	mask = '\"'; }
			if (character == 'Ä') { character = 'A';	mask = '\"'; }
			if (character == 'ü') { character = 'u';	mask = '\"'; }
			if (character == 'Ü') { character = 'U';	mask = '\"'; }
			
			//illegal character
			if ( (character-offset > tiles) || (character < offset) ) {
				System.err.println("ERROR in Font.drawSubstring(): character out of range -- " + character );
				continue;
			}
			
			//draw character
			super.paintTile(g, (int)character - offset, x, y);
			if (mask != 0) super.paintTile(g, (int)mask - offset, x, y);
			x = x + tile_width + hspacing;
			
			//kerning
			if (character == 'M') x++;
			if (character == 'I') x--;
		}
	}

	
	/**
	 * Draws a String.
	 */
	public void drawString(String text, Graphics g) {
		drawString(text, 0, 0, g );
	}

	
	public int getFace() {
		return FACE_MONOSPACE;
	}
	
	
	/**
	 * Get the height of 1 letter of this font.
	 */
	public int getHeight() {
		return tile_height + vspacing;
	}
	

	/**
	 * Get the distance from the top of a letter to the baseline.
	 * Bitmap letters sit on the bottom of their tile.
	 */
	public int getBaselinePosition() {
		return tile_height;
	}

	public int getStyle() {
		return STYLE_PLAIN;
	}

	public int getSize() {
		return SIZE_MEDIUM;
	}

	public boolean isPlain() {
		return true;
	}

	public boolean isBold() {
		return false;
	}

	public boolean isItalic() {
		return false;
	}

	public boolean isUnderlined() {
		return false;
	}
	

	/**
	 * Get width of one line of text in pixels
	 * 
	 * @param char_count - number of characters
	 * @return int width in pixels
	 */
	private int stringWidth( int char_count ) {
		return ( (tile_width * char_count) + (hspacing * char_count) - hspacing );
	}

	/**
	 */
	public int stringWidth( String str ) {
		return stringWidth(str.length());
	}

	
	/**
	 * Calculate the maximal count of characters, that can be written on
	 * a line of given width in pixels
	 * 
	 * @param width - width of a line of pixels
	 * @return int characters per line
	 */
	int charsPerLine( int width ) {
		return ( ( width + hspacing ) / ( tile_width + hspacing ) );
	}
	
	
	/** true if this font has a letter for c (space and line break always work) */
	boolean hasChar(char c) {
		if (c == ' ' || c == '\n') return true;
		return (c >= offset) && (c - offset < tiles);
	}


	public boolean hasSmallLetters() {
		return ( tiles >= 90 );
	}

	public int substringWidth(String str, int offset, int length) {
		return stringWidth(length);
	}

	public int charsWidth(char[] ch, int offset, int length) { 
		return stringWidth(length);
	}
	
	public int charWidth(char ch) { 
		return tile_width;
	}
	
	public static Font getDefaultFont() { 
		return Display.getGFCFont();
	}

	
	/**
	 * Loads a bitmap font as described in midlet.properties:
	 * font.image, font.width, font.height, font.hspacing, font.vspacing, font.offset
	 * 
	 * @return the font, or null if no font is configured
	 */
	static Font loadFromProperties() {
		String file = MIDlet.GetAppProperty("font.image");
		if (file == null) return null;

		try {
			Image img = Image.createImage( file.trim() );

			return new Font( img,
				getIntProperty("font.width", 	5),
				getIntProperty("font.height", 	7),
				getIntProperty("font.hspacing", 1),
				getIntProperty("font.vspacing", 1),
				getIntProperty("font.offset", 	33) );
		}
		catch (Exception e) {
			System.err.println("ERROR in Font.loadFromProperties(): font could not be loaded - " + file );
			e.printStackTrace();
			return null;
		}
	}

	private static int getIntProperty(String key, int fallback) {
		String v = MIDlet.GetAppProperty(key);
		if (v == null) return fallback;

		return Integer.parseInt(v.trim());
	}
	
	
	public static Font getFont(int face, int style, int size) { 
		if (Boolean.parseBoolean(MIDlet.GetAppProperty("debug"))) System.err.println("WARNING in Font.getFont(): this method always returns default font" );

		return getDefaultFont();
	}
}
