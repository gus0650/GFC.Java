package gfc.microedition.lcdui;

import gfc.microedition.midlet.*;
import gfc.widgets.*;
import gfc.util.Timer;
import gfc.util.TimerTask;


/**
 * @see javax.microedition.lcdui.Ticker
 * @author Gustaf Stechmann
 * @version alpha
 */
public class Ticker implements Runnable {

	private Scroller 	scroller;

	//the Displayable that shows this ticker, set by Displayable.setTicker()
	private Displayable owner;

	//scroll every 40 ms (25 times per second), TICKER_SPEED = pixels per step
	private static final int INTERVAL = 40;

    private TimerTask 	timerTask 	= new TimerTask(this);
    private Timer 		timer 		= new Timer();


	public Ticker( String text ) {
		
		//one line of text over the whole width, text runs from right to left
		scroller = new Scroller(
				DisplayProperties.getScreenWidth(),
				DisplayProperties.getFont().getHeight() +1,
				0,
				0,
				null,
				text,
				DisplayProperties.getFont(),
				DisplayProperties.getActiveColor(),
				-Math.max( 1, Math.abs( DisplayProperties.TICKER_SPEED ) )
			);
		
        this.timerTask.setInterval( INTERVAL );
	}


	public String getString() {
		return scroller.getString();
	}


	public void setString( String text ) {
		scroller.setString(text);
	}

	
    /**
     * Start scrolling the ticker
     */
    void start() {
    	timer.schedule(timerTask);
    }

    
    /**
     * Stop scrolling the ticker. Ticker text rests in his current
     * scroll-position untill start() is called again.
     */
    void stop() {
        timerTask.cancel();
    }
	
    
	public void run() {
		scroller.scroll();

		//only repaint while the owner is on screen
		if (owner != null && owner.isActive()) owner.repaint();
	}

	void setOwner(Displayable d) {
		owner = d;
	}

	void paint(Graphics g) {
		scroller.paint(g);
	}

	
	int getHeight() {
		return scroller.getHeight();
	}
}