package gfc.microedition.lcdui;

/**
 * Placeholder: DateField is not implemented in GFC.Java.
 * It exists so that games which mention it compile; using it throws an UnsupportedOperationException.
 * 
 * @see javax.microedition.lcdui.DateField
 */
public class DateField extends Item {

	public static final int
		DATE 		= 1,
		TIME 		= 2,
		DATE_TIME 	= 3;

	public DateField( String label, int mode ) {
		super(null);
		throw new UnsupportedOperationException("DateField is not implemented in GFC.Java -- see TODO.txt");
	}

	public DateField( String label, int mode, java.util.TimeZone timeZone ) {
		super(null);
		throw new UnsupportedOperationException("DateField is not implemented in GFC.Java -- see TODO.txt");
	}
}
