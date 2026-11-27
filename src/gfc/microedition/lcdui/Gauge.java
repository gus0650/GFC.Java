package gfc.microedition.lcdui;

import gfc.widgets.*;


/**
 * @see javax.microedition.lcdui.Gauge
 * @author Alex Knorr
 */
public class Gauge extends Item {

	int g_value, g_max_value;
	
	final boolean g_interactive;
	
	GaugeX g_gauge;
	

	public Gauge( String label, boolean interactive, int maxValue, int initialValue ) throws IllegalArgumentException {
		super(label);

		g_value 		= initialValue;
		g_max_value 	= maxValue;
		g_interactive	= interactive;
		
		if (initialValue > maxValue) throw new IllegalArgumentException();
		if (interactive) throw new UnsupportedOperationException("interactive Gauge is not implemented in GFC.Java -- see TODO.txt");
		
		g_gauge 			= createWidget();
		
		setWidget( g_gauge );
		
		g_gauge.setValue(initialValue);
	}

	
	public int getMaxValue() {
		return g_max_value;
	}


	public int getValue() {
		return g_value;
	}


	public void setMaxValue( int maxValue ) {
		if (maxValue <= 0) throw new IllegalArgumentException("maxValue must be > 0");

		g_max_value = maxValue;
		if (g_value > maxValue) g_value = maxValue;

		g_gauge.setMaxValue( maxValue );
		g_gauge.setValue( g_value );
	}
	
	public void setValue( int newvalue ) throws IllegalArgumentException {
		//like MIDP: values outside 0..max are clamped
		if (newvalue < 0) 			newvalue = 0;
		if (newvalue > g_max_value) newvalue = g_max_value;

		g_value = newvalue;
		g_gauge.setValue( newvalue );	//was: only stored, the widget did not change
	}

	private GaugeX createWidget() {
		return new GaugeX( 
				100,	//TODO
				10, 	//TODO
				0,
				0,
				getScreen(),
				0,
				g_max_value,
				DisplayProperties.getActiveColor(),
				DisplayProperties.getPassiveColor()
			);
	}

	public boolean isInteractive() {
		return g_interactive;
	}

	
	protected void increaseSliderValue() {
		if (!g_interactive) System.out.println("this Gauge is not interactive!");
		setValue(g_value + 1);
	}


	protected void reduceSliderValue() {
		if (!g_interactive) System.out.println("this Gauge is not interactive!");
		setValue(g_value - 1);
	}
}