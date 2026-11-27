import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;
import javax.swing.*;

//this program tries to go into fullscreen and NOT do anti aliasing/interpolation which makes the image look blurry
//https://docs.oracle.com/javase/8/docs/api/java/awt/RenderingHints.html#VALUE_ANTIALIAS_OFF
//
//instead of switching the display mode to 320x200 (which most modern monitors scale up blurry),
//the window covers the whole screen and the image is scaled up with nearest neighbour interpolation.

public class TestFullscreen extends JFrame {

	static final long serialVersionUID = 1L;

	BufferedImage i = null;

	public TestFullscreen() {
		super("TestFullscreen");

		try {
			i = ImageIO.read(new File( "res/320x200.png" ));
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		add(new ImagePanel());

		GraphicsDevice g = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();

		if (g.isFullScreenSupported()) {
			System.out.println("going fullscreen");
			setResizable(false);
			setUndecorated(true);
			g.setFullScreenWindow(this);
		}
		else {
			System.out.println("fullscreen not supported on this device");
			setSize(640, 400);
			setVisible(true);
		}
	}

	class ImagePanel extends JPanel {

		static final long serialVersionUID = 1L;

		@Override
		protected void paintComponent( Graphics graphics ) {
			Graphics2D g = (Graphics2D)graphics;

			// disable anti-aliasing and use nearest neighbour scaling -> sharp pixels
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

			//clear
			g.setColor( Color.BLACK );
			g.fillRect( 0, 0, getWidth(), getHeight() );

			if (i == null) return;

			// largest integer scale factor that fits, image centered
			int scale = Math.max(1, Math.min(getWidth() / i.getWidth(), getHeight() / i.getHeight()));
			int w = i.getWidth() * scale;
			int h = i.getHeight() * scale;
			g.drawImage(i, (getWidth() - w) / 2, (getHeight() - h) / 2, w, h, null);
		}
	}

	public static void main( String[] args ) {
		SwingUtilities.invokeLater(TestFullscreen::new);
	}
}
