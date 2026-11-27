package gfc.microedition.lcdui;

import gfc.graphics.Color;
import gfc.microedition.midlet.MIDlet;

/**
 * Central access to the look-and-feel settings of the GFC MIDP wrapper.
 * <p>
 * The values are read from midlet.properties (TEXT_*, TICKER_SPEED, COL_*);
 * colors, font and screen size are delegated to {@link Display}.
 */
public final class DisplayProperties {

	private DisplayProperties() { }

	//--- texts

	public static final String
		TEXT_DISMISS	= getText("TEXT_DISMISS", "DISMISS"),
		TEXT_MORE		= getText("TEXT_MORE", "MORE"),
		TEXT_OK			= getText("TEXT_OK", "OK"),
		TEXT_SELECT		= getText("TEXT_SELECT", "SELECT"),
		TEXT_MENU_OPEN	= getText("TEXT_MENU_OPEN", "MENU..."),
		TEXT_MENU_CLOSE	= getText("TEXT_MENU_CLOSE", "CLOSE");

	//--- behaviour

	public static final int TICKER_SPEED = getInt("TICKER_SPEED", 1);

	/** Key that triggers/selects the current item (in addition to KEY_NUM5). */
	public static final int KEY_TRIGGER = Display.KEY_FIRE;

	//--- delegates to Display

	public static Font getFont() {
		return Display.getGFCFont();
	}

	public static Color getActiveColor() {
		return Display.getActiveColor();
	}

	public static Color getPassiveColor() {
		return Display.getPassiveColor();
	}

	public static int getScreenWidth() {
		return Display.getDisplayWidth();
	}

	public static int getScreenHeight() {
		return Display.getDisplayHeight();
	}

	//--- helpers

	/** Reads a property and strips surrounding quotes and a trailing semicolon. */
	private static String getRaw(String key) {
		String v = MIDlet.GetAppProperty(key);
		if (v == null) return null;

		v = v.trim();
		if (v.endsWith(";")) v = v.substring(0, v.length() - 1).trim();
		if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) v = v.substring(1, v.length() - 1);
		return v;
	}

	private static String getText(String key, String fallback) {
		String v = getRaw(key);
		return (v == null) ? fallback : v;
	}

	private static int getInt(String key, int fallback) {
		String v = getRaw(key);
		if (v == null) return fallback;
		try {
			return Integer.parseInt(v);
		}
		catch (NumberFormatException e) {
			System.err.println("WARNING in DisplayProperties: " + key + " is not a number: " + v);
			return fallback;
		}
	}
}
