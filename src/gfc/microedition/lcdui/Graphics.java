package gfc.microedition.lcdui;

import gfc.graphics.*;
import gfc.microedition.midlet.*;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;


public class Graphics {

	java.awt.Graphics awtgraphics;

	public static final int HCENTER 	= 1;
	public static final int VCENTER 	= 2;
	public static final int LEFT 		= 4;
	public static final int RIGHT 		= 8;
	public static final int TOP 		= 16;
	public static final int BOTTOM 		= 32;
	public static final int BASELINE 	= 64;

	public static final int SOLID 		= 0;
	public static final int DOTTED 		= 1;

	//same values as in Sprite, needed by drawRegion()
	static final int
		TRANS_NONE 				= 0,
		TRANS_MIRROR_ROT180 	= 1,
		TRANS_MIRROR 			= 2,
		TRANS_ROT180 			= 3,
		TRANS_MIRROR_ROT270 	= 4,
		TRANS_ROT90 			= 5,
		TRANS_ROT270 			= 6,
		TRANS_MIRROR_ROT90 		= 7;

	int trans_x, trans_y;

	Font font;

	int stroke_style = SOLID;

	static final Stroke STROKE_SOLID 	= new BasicStroke(1);
	static final Stroke STROKE_DOTTED 	= new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[] {1, 1}, 0);

	void dispose() {
		awtgraphics.dispose();
		awtgraphics = null;
	}

	void set(java.awt.Graphics g) {
		if (g == null) throw new NullPointerException();
		awtgraphics = g;

		//awt graphics object starts without translation
		trans_x = 0;
		trans_y = 0;

		if (stroke_style == DOTTED) setStrokeStyle(DOTTED);
	}

	// --clip

	public int getClipX() {
		java.awt.Rectangle r = awtgraphics.getClipBounds();
		if (r == null) return 0;
		return r.x;
	}

	public int getClipY() {
		java.awt.Rectangle r = awtgraphics.getClipBounds();
		if (r == null) return 0;
		return r.y;
	}

	public int getClipWidth() {
		java.awt.Rectangle r = awtgraphics.getClipBounds();
		if (r == null) return Display.getDisplayWidth();
		return r.width;
	}

	public int getClipHeight() {
		java.awt.Rectangle r = awtgraphics.getClipBounds();
		if (r == null) return Display.getDisplayHeight();
		return r.height;
	}

	public void clipRect(int x, int y, int w, int h) {
		awtgraphics.clipRect(x,y,w,h);
	}

	public void setClip(int x, int y, int w, int h) {
		awtgraphics.setClip(x,y,w,h);
	}

	// --color

	public void setColor(int rgb) {
		awtgraphics.setColor( new java.awt.Color(rgb & 0xFFFFFF) );
	}

	public void setColor(int r, int g, int b) {
		if ( (r < 0) | (r > 255) | (g < 0) | (g > 255) | (b < 0) | (b > 255) ) throw new IllegalArgumentException("color component out of range");

		awtgraphics.setColor( new java.awt.Color(r, g, b) );
	}

	public void setColor(Color col) {
		if (col == null) return;

		awtgraphics.setColor( col.awtcolor );
	}

	public int getColor() {
		return awtgraphics.getColor().getRGB() & 0xFFFFFF;
	}

	public int getRedComponent() {
		return awtgraphics.getColor().getRed();
	}

	public int getGreenComponent() {
		return awtgraphics.getColor().getGreen();
	}

	public int getBlueComponent() {
		return awtgraphics.getColor().getBlue();
	}

	public void setGrayScale(int value) {
		if ( (value < 0) | (value > 255) ) throw new IllegalArgumentException("gray value out of range");

		setColor(value, value, value);
	}

	public int getGrayScale() {
		//same weighting as most handsets
		return ( getRedComponent() * 30 + getGreenComponent() * 59 + getBlueComponent() * 11 ) / 100;
	}

	public int getDisplayColor(int color) {
		//desktop displays show all colors
		return color & 0xFFFFFF;
	}

	// --stroke

	public void setStrokeStyle(int style) {
		if ( (style != SOLID) & (style != DOTTED) ) throw new IllegalArgumentException("unknown stroke style");

		stroke_style = style;

		if (awtgraphics instanceof Graphics2D) {
			((Graphics2D)awtgraphics).setStroke( style == DOTTED ? STROKE_DOTTED : STROKE_SOLID );
		}
	}

	public int getStrokeStyle() {
		return stroke_style;
	}

	// --images

	/** Convenience overload: draws the image with its top left corner at (x,y). */
	public void drawImage(Image i, int x, int y) {
		drawImage(i, x, y, TOP | LEFT);
	}

	public void drawImage(Image i, int x, int y, int anchor) {
		if (i == null) throw new NullPointerException();

		x = anchorX( x, i.getWidth(), anchor );
		y = anchorY( y, i.getHeight(), anchor );

		awtgraphics.drawImage( i.getAWTImage(), x, y, null );
	}

	/**
	 * Draws a region of an image, optionally mirrored and/or rotated.
	 * The transform values are the TRANS_* constants of Sprite.
	 */
	public void drawRegion(Image src, int x_src, int y_src, int width, int height, int transform, int x_dest, int y_dest, int anchor) {
		if (src == null) throw new NullPointerException();
		if ( (transform < 0) | (transform > 7) ) throw new IllegalArgumentException("unknown transform " + transform);
		if ( (x_src < 0) | (y_src < 0) | (x_src + width > src.getWidth()) | (y_src + height > src.getHeight()) ) throw new IllegalArgumentException("region exceeds source image");
		if ( (width <= 0) | (height <= 0) ) return;

		//rotation by 90 or 270 degrees swaps width and height
		boolean swap 	= (transform >= TRANS_MIRROR_ROT270);
		int w 			= swap ? height : width;
		int h 			= swap ? width : height;

		x_dest = anchorX( x_dest, w, anchor );
		y_dest = anchorY( y_dest, h, anchor );

		BufferedImage region = src.awtimage.getSubimage( x_src, y_src, width, height );

		//simple case: no transformation
		if (transform == TRANS_NONE) {
			awtgraphics.drawImage( region, x_dest, y_dest, null );
			return;
		}

		AffineTransform at = new AffineTransform();
		at.translate( x_dest, y_dest );
		at.concatenate( getTransform(transform, width, height) );

		((Graphics2D)awtgraphics).drawImage( region, at, null );
	}

	/**
	 * Returns the AffineTransform that maps a width*height region onto its transformed area,
	 * which starts at (0,0).
	 */
	static AffineTransform getTransform(int transform, int width, int height) {
		//AffineTransform(m00, m10, m01, m11, m02, m12): x' = m00*x + m01*y + m02, y' = m10*x + m11*y + m12
		switch (transform) {
			case TRANS_NONE: 			return new AffineTransform(  1,  0,  0,  1,     0,      0 );
			case TRANS_MIRROR: 			return new AffineTransform( -1,  0,  0,  1, width,      0 );
			case TRANS_MIRROR_ROT180: 	return new AffineTransform(  1,  0,  0, -1,     0, height );
			case TRANS_ROT180: 			return new AffineTransform( -1,  0,  0, -1, width, height );
			case TRANS_ROT90: 			return new AffineTransform(  0,  1, -1,  0, height,     0 );
			case TRANS_ROT270: 			return new AffineTransform(  0, -1,  1,  0,     0, width );
			case TRANS_MIRROR_ROT90: 	return new AffineTransform(  0, -1, -1,  0, height, width );
			case TRANS_MIRROR_ROT270: 	return new AffineTransform(  0,  1,  1,  0,     0,      0 );
		}
		throw new IllegalArgumentException("unknown transform " + transform);
	}

	/**
	 * Draws an array of ARGB pixels.
	 */
	public void drawRGB(int[] rgbData, int offset, int scanlength, int x, int y, int width, int height, boolean processAlpha) {
		if (rgbData == null) throw new NullPointerException();
		if ( (width <= 0) | (height <= 0) ) return;

		BufferedImage i = new BufferedImage( width, height, processAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB );
		i.setRGB( 0, 0, width, height, rgbData, offset, scanlength );

		awtgraphics.drawImage( i, x, y, null );
	}

	/**
	 * Copies a rectangular area to another position. Only works on the Graphics of a mutable Image.
	 */
	public void copyArea(int x_src, int y_src, int width, int height, int x_dest, int y_dest, int anchor) {
		x_dest = anchorX( x_dest, width, anchor );
		y_dest = anchorY( y_dest, height, anchor );

		awtgraphics.copyArea( x_src, y_src, width, height, x_dest - x_src, y_dest - y_src );
	}

	// --translation

	public void translate(int x, int y) {
		trans_x += x;
		trans_y += y;

		awtgraphics.translate(x,y);
	}

	public int getTranslateX() {
		return trans_x;
	}

	public int getTranslateY() {
		return trans_y;
	}

	// --primitives

	public void drawLine(int x, int y, int x2, int y2) {
		awtgraphics.drawLine(x,y,x2,y2);
	}

	public void drawRect(int x, int y, int w, int h) {
		awtgraphics.drawRect(x,y,w,h);
	}

	public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		awtgraphics.drawRoundRect(x,y,width,height,arcWidth,arcHeight);
	}

	public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		awtgraphics.fillRoundRect(x,y,width,height,arcWidth,arcHeight);
	}

	public void fillRect(int x, int y, int w, int h) {
		awtgraphics.fillRect(x,y,w,h);
	}

	public void drawArc(int x, int y, int w, int h, int a, int b) {
		awtgraphics.drawArc(x,y,w,h,a,b);
	}

	public void fillArc(int x, int y, int w, int h, int a, int b) {
		awtgraphics.fillArc(x,y,w,h,a,b);
	}

	public void fillTriangle(int x1, int y1, int x2, int y2, int x3, int y3) {
		awtgraphics.fillPolygon( new int[] {x1, x2, x3}, new int[] {y1, y2, y3}, 3 );
	}

	// --text

	public void drawString(String s, int x, int y, int anchor) {
		if (s == null) throw new NullPointerException();

		Font f = getFont();

		//no bitmap font available -> use awt font
		if (f == null) {
			java.awt.FontMetrics fm = awtgraphics.getFontMetrics();

			x = anchorX( x, fm.stringWidth(s), anchor );
			if 		((anchor & BASELINE) 	!= 0) 	;	//awt draws at baseline anyway
			else if ((anchor & BOTTOM) 		!= 0) 	y -= fm.getDescent();
			else if ((anchor & VCENTER) 	!= 0) 	y += fm.getAscent() / 2;
			else 									y += fm.getAscent();

			awtgraphics.drawString(s, x, y);
			return;
		}

		x = anchorX( x, f.stringWidth(s), anchor );
		if ((anchor & BASELINE) != 0) 	y -= f.getBaselinePosition();
		else 							y = anchorY( y, f.getHeight(), anchor );

		f.drawString( s, x, y, this );
	}

	public void drawSubstring(String str, int offset, int len, int x, int y, int anchor) {
		drawString( str.substring(offset, offset + len), x, y, anchor );
	}

	public void drawChar(char character, int x, int y, int anchor) {
		drawString( String.valueOf(character), x, y, anchor );
	}

	public void drawChars(char[] data, int offset, int length, int x, int y, int anchor) {
		drawString( new String(data, offset, length), x, y, anchor );
	}

	// --font

	public Font getFont() {
		if (font == null) font = Font.getDefaultFont();

		return font;
	}

	public void setFont(Font font) {
		this.font = font;
	}

	// --anchor helpers

	/** Returns the left x position for an object of given width drawn with the given anchor. */
	static int anchorX(int x, int width, int anchor) {
		if 		((anchor & HCENTER) != 0) 	return x - width / 2;
		else if ((anchor & RIGHT) 	!= 0) 	return x - width;
		else 								return x;
	}

	/** Returns the top y position for an object of given height drawn with the given anchor. */
	static int anchorY(int y, int height, int anchor) {
		if 		((anchor & VCENTER) != 0) 	return y - height / 2;
		else if ((anchor & BOTTOM) 	!= 0) 	return y - height;
		else 								return y;
	}
}
