import gfc.audio.*;
import gfc.graphics.particles.effects.*;
import gfc.microedition.io.*;
import gfc.microedition.lcdui.*;
import gfc.microedition.lcdui.game.*;
import gfc.microedition.media.*;
import gfc.microedition.midlet.*;
import gfc.microedition.rms.*;
import java.io.*;
import java.util.Vector;

/**
 * Showcase of everything GFC.Java can do.
 * <p>
 * A menu leads to one demo per topic. Every demo goes back to the menu with F1 (left soft key, BACK).
 * <pre>
 * keys:  cursor keys       = cursor keys of the handset
 *        Return, Space     = fire
 *        F1, F2            = left / right soft key
 *        0..9, * #         = number keys
 * </pre>
 * Not shown: the parts that are not implemented (see TODO.txt) -- they throw an UnsupportedOperationException.
 *
 * @author Gustaf Stechmann
 */
public class TestMIDlet extends MIDlet implements CommandListener {

	static TestMIDlet	midlet;

	Display 			display;
	List 				menu;

	static final Command
		BACK_COMMAND 	= new Command("BACK", 	Command.BACK, 1),
		EXIT_COMMAND 	= new Command("EXIT", 	Command.EXIT, 1),
		OK_COMMAND 		= new Command("OK", 	Command.OK, 1);

	static final String[] DEMOS = {
		"GRAPHICS",
		"IMAGES",
		"TEXT",
		"KEYS + POINTER",
		"GAME: SPRITES + TILES",
		"PARTICLES",
		"SOUND + MUSIC",
		"SAVE GAME (RMS)",
		"FORM",
		"TEXTBOX",
		"LISTS",
		"ALERT",
		"COMMAND MENU",
		"DEVICE INFO",
		"NETWORK",
	};


	public TestMIDlet() {
		System.out.println("TestMIDlet created");
		midlet = this;
	}

	@Override
	protected void startApp() {
		System.out.println("TestMIDlet started");

		display = Display.getDisplay(this);

		if (menu == null) {
			menu = new List("GFC.JAVA SHOWCASE", List.IMPLICIT, DEMOS, null);
			menu.addCommand(EXIT_COMMAND);
			menu.setCommandListener(this);
			menu.setTicker( new Ticker("+++ WELCOME TO GFC.JAVA +++ CURSOR KEYS TO CHOOSE, RETURN TO START +++ F1 = BACK +++") );
		}

		display.setCurrent(menu);
	}

	@Override
	protected void pauseApp() {
		System.out.println("TestMIDlet paused");
		Audible.stopMusic();
	}

	@Override
	protected void destroyApp(boolean unconditional) {
		System.out.println("TestMIDlet destroyed");
		Audible.stopMusic();
	}


	//--- menu

	public void commandAction(Command c, Displayable d) {
		if (c == EXIT_COMMAND) {
			destroyApp(true);
			notifyDestroyed();
			return;
		}

		if (c == List.SELECT_COMMAND && d == menu) {
			showDemo( menu.getSelectedIndex() );
			return;
		}

		if (c == BACK_COMMAND) {
			backToMenu();
		}
	}

	void backToMenu() {
		Audible.stopMusic();
		display.setCurrent(menu);
	}

	void showDemo(int index) {
		Displayable demo 	= null;
		boolean own_listener 	= false;	//demos with their own CommandListener

		switch (index) {
			case 0: 	demo = new GraphicsDemo(); 	break;
			case 1: 	demo = new ImageDemo(); 	break;
			case 2: 	demo = new TextDemo(); 		break;
			case 3: 	demo = new InputDemo(); 	break;
			case 4: 	demo = new GameDemo(); 		break;
			case 5: 	demo = new ParticleDemo(); 	break;
			case 6: 	demo = new SoundDemo(); 	own_listener = true; break;
			case 7: 	demo = new SaveDemo(); 		break;
			case 8: 	demo = createForm(); 		break;
			case 9: 	demo = createTextBox(); 	own_listener = true; break;
			case 10: 	demo = createLists(); 		own_listener = true; break;
			case 11:
				showAlert("ALERT", "THIS ALERT GOES AWAY BY ITSELF AFTER 3 SECONDS, OR PRESS F1.", 3000);
				return;
			case 12: 	demo = new CommandMenuDemo(); 	own_listener = true; break;
			case 13: 	demo = new DeviceDemo(); 		break;
			case 14: 	demo = new NetworkDemo(); 		break;
		}

		if (demo == null) return;

		//every demo goes back with F1
		demo.addCommand(BACK_COMMAND);
		if (!own_listener) demo.setCommandListener(this);

		display.setCurrent(demo);
	}

	/** Alert, then back to the menu (after timeout or DISMISS) */
	void showAlert(String title, String text, int timeout) {
		Alert a = new Alert(title, text, null, AlertType.INFO);
		a.setTimeout(timeout);
		display.setCurrent(a, menu);
	}


	//--- screens

	Form createForm() {
		Form f = new Form("FORM");

		f.append("A FORM SHOWS ITEMS BELOW EACH OTHER.");
		f.append( new StringItem(null, "THIS IS A STRINGITEM. LONG TEXT IS WRAPPED TO THE WIDTH OF THE SCREEN AUTOMATICALLY.") );
		f.append( createDukeImage(48, 32) );

		final Gauge gauge = new Gauge(null, false, 20, 0);
		f.append(gauge);

		//animate the gauge
		new Thread() {
			public void run() {
				for (int i = 0; i <= 20; i++) {
					gauge.setValue(i);
					ThreadSleep(100);
				}
			}
		}.start();

		return f;
	}

	TextBox createTextBox() {
		final TextBox tb = new TextBox("ENTER YOUR NAME", "", 12, TextField.ANY);
		tb.addCommand(OK_COMMAND);
		tb.setCommandListener( new CommandListener() {
			public void commandAction(Command c, Displayable d) {
				if (c == OK_COMMAND) 	showAlert("HELLO", "HELLO " + tb.getString() + "!", 2000);
				else 					backToMenu();
			}
		});
		return tb;
	}

	List createLists() {
		//an EXCLUSIVE list: one of them can be chosen, with RETURN
		final List l = new List("CHOOSE A LEVEL", List.EXCLUSIVE, new String[] { "EASY", "MEDIUM", "HARD" }, null);
		l.addCommand(OK_COMMAND);
		l.setCommandListener( new CommandListener() {
			public void commandAction(Command c, Displayable d) {
				if (c == OK_COMMAND) {
					//then a MULTIPLE list: several can be checked
					List m = new List("OPTIONS (" + l.getSelectedIndex() + ")", List.MULTIPLE, new String[] { "SOUND", "MUSIC", "VIBRATION" }, null);
					m.setSelectedIndex(0, true);
					m.setSelectedIndex(1, true);
					m.addCommand(BACK_COMMAND);
					m.setCommandListener(TestMIDlet.this);
					display.setCurrent(m);
				}
				else backToMenu();
			}
		});
		return l;
	}


	//--- helpers

	static Image duke;

	/** the duke picture from res/, loaded from the classpath like on the handset */
	static Image getDuke() {
		if (duke == null) {
			try {
				duke = Image.createImage("/res/duke.jpg");
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		return duke;
	}

	/** a part of the duke picture, scaled down by drawing every n-th pixel */
	static Image createDukeImage(int w, int h) {
		Image d = getDuke();
		int[] src = new int[d.getWidth() * d.getHeight()];
		d.getRGB(src, 0, d.getWidth(), 0, 0, d.getWidth(), d.getHeight());

		int[] dst = new int[w * h];
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				dst[y * w + x] = src[ (y * d.getHeight() / h) * d.getWidth() + (x * d.getWidth() / w) ];
			}
		}
		return Image.createRGBImage(dst, w, h, false);
	}

	static void ThreadSleep(int ms) {
		try {
			Thread.sleep(ms);
		}
		catch (InterruptedException e) {
		}
	}

	/** clears the screen and writes a title line */
	static void drawTitle(Graphics g, Canvas c, String title) {
		g.setColor(0x102040);
		g.fillRect(0, 0, c.getWidth(), c.getHeight());
		g.setColor(0x305080);
		g.fillRect(0, 0, c.getWidth(), 14);
		g.drawString(title, c.getWidth() / 2, 3, Graphics.HCENTER | Graphics.TOP);
		g.drawString("F1 = BACK", c.getWidth() - 4, c.getHeight() - 4, Graphics.RIGHT | Graphics.BOTTOM);
	}
}


//====================================================================================
// GRAPHICS: primitives, colors, strokes, clipping, translation, drawRGB

class GraphicsDemo extends Canvas {

	protected void paint(Graphics g) {
		TestMIDlet.drawTitle(g, this, "GRAPHICS");

		//lines in all colors
		for (int i = 0; i < 16; i++) {
			g.setColor( (i * 16) << 16 | (255 - i * 16) << 8 | 128 );
			g.drawLine(10, 30 + i * 3, 110, 80 - i * 3);
		}

		//rectangles
		g.setColor(0xFF0000);
		g.fillRect(130, 30, 50, 30);
		g.setColor(0xFFFFFF);
		g.drawRect(130, 30, 50, 30);
		g.setColor(0x00FF00);
		g.fillRoundRect(200, 30, 50, 30, 12, 12);
		g.setColor(0xFFFF00);
		g.drawRoundRect(270, 30, 50, 30, 12, 12);

		//arcs
		g.setColor(0x00FFFF);
		g.fillArc(340, 25, 40, 40, 0, 270);
		g.setColor(0xFF00FF);
		g.drawArc(400, 25, 40, 40, 45, 300);

		//triangle
		g.setColor(0xFF8000);
		g.fillTriangle(10, 150, 60, 100, 110, 150);

		//dotted lines
		g.setStrokeStyle(Graphics.DOTTED);
		g.setColor(0xFFFFFF);
		g.drawRect(130, 100, 120, 50);
		g.drawLine(130, 100, 250, 150);
		g.setStrokeStyle(Graphics.SOLID);

		//gray scale
		for (int i = 0; i < 16; i++) {
			g.setGrayScale(i * 17);
			g.fillRect(270 + i * 10, 100, 10, 50);
		}

		//drawRGB: a color gradient with alpha
		int w = 200, h = 40;
		int[] rgb = new int[w * h];
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int alpha = 255 * x / w;
				rgb[y * w + x] = (alpha << 24) | ((255 * y / h) << 16) | 0x00FF;
			}
		}
		g.setColor(0xFFFFFF);
		g.drawString("DRAWRGB WITH ALPHA", 10, 165, Graphics.LEFT | Graphics.TOP);
		g.fillRect(10, 180, w, h);
		g.drawRGB(rgb, 0, w, 10, 180, w, h, true);

		//clipping: a big circle, only the part inside the clip is drawn
		g.setColor(0xFFFFFF);
		g.drawString("CLIPPING", 270, 165, Graphics.LEFT | Graphics.TOP);
		g.drawRect(269, 179, 101, 41);
		g.setClip(270, 180, 100, 40);
		g.setColor(0x80FF80);
		g.fillArc(290, 160, 80, 80, 0, 360);
		g.setClip(0, 0, getWidth(), getHeight());

		//translation: the same drawing at 3 places
		for (int i = 0; i < 3; i++) {
			g.translate(10 + i * 60, 240);
			g.setColor(0x4080FF);
			g.fillRect(0, 0, 50, 30);
			g.setColor(0xFFFFFF);
			g.drawString("T" + i, 25, 12, Graphics.HCENTER | Graphics.TOP);
			g.translate(-(10 + i * 60), -240);
		}
	}
}


//====================================================================================
// IMAGES: loading from the classpath, drawRegion with all transforms, anchors, mutable images

class ImageDemo extends Canvas {

	static final String[] TRANSFORM_NAMES = {
		"NONE", "MIR180", "MIRROR", "ROT180", "MIR270", "ROT90", "ROT270", "MIR90"
	};

	Image part;
	Image mutable;

	ImageDemo() {
		part = TestMIDlet.createDukeImage(48, 32);

		//a mutable image: starts white, we can draw into it
		mutable = Image.createImage(60, 40);
		Graphics g = mutable.getGraphics();
		g.setColor(0x0000FF);
		g.fillRect(5, 5, 50, 30);
		g.setColor(0xFFFF00);
		g.fillArc(20, 10, 20, 20, 0, 360);
	}

	protected void paint(Graphics g) {
		TestMIDlet.drawTitle(g, this, "IMAGES");

		//all 8 transforms of drawRegion (the same as Sprite.setTransform)
		for (int t = 0; t < 8; t++) {
			int x = 10 + (t % 4) * 120;
			int y = 30 + (t / 4) * 80;
			g.drawRegion(part, 0, 0, part.getWidth(), part.getHeight(), t, x, y, Graphics.TOP | Graphics.LEFT);
			g.setColor(0xFFFFFF);
			g.drawString(TRANSFORM_NAMES[t], x, y + 52, Graphics.LEFT | Graphics.TOP);
		}

		//anchors: the same image anchored at one point in 3 ways
		int ax = 100, ay = 250;
		g.drawImage(part, ax, ay, Graphics.RIGHT | Graphics.BOTTOM);
		g.drawImage(part, ax, ay, Graphics.LEFT | Graphics.TOP);
		g.drawImage(part, ax + 120, ay, Graphics.HCENTER | Graphics.VCENTER);
		g.setColor(0xFF0000);
		g.drawLine(ax - 5, ay, ax + 5, ay);
		g.drawLine(ax, ay - 5, ax, ay + 5);
		g.drawLine(ax + 115, ay, ax + 125, ay);
		g.drawLine(ax + 120, ay - 5, ax + 120, ay + 5);
		g.setColor(0xFFFFFF);
		g.drawString("ANCHORS", 10, 300, Graphics.LEFT | Graphics.TOP);

		//mutable image
		g.drawImage(mutable, 300, 220, Graphics.TOP | Graphics.LEFT);
		g.drawString("MUTABLE IMAGE", 300, 265, Graphics.LEFT | Graphics.TOP);
	}
}


//====================================================================================
// TEXT: bitmap font, anchors, umlauts, line breaks

class TextDemo extends Canvas {

	protected void paint(Graphics g) {
		TestMIDlet.drawTitle(g, this, "TEXT");

		Font f = g.getFont();
		g.setColor(0xFFFFFF);

		g.drawString("THE FONT IS A BITMAP: RES/FONT.PNG", 10, 30, Graphics.LEFT | Graphics.TOP);
		g.drawString("HEIGHT " + f.getHeight() + ", WIDTH OF 'HELLO' = " + f.stringWidth("HELLO"), 10, 45, Graphics.LEFT | Graphics.TOP);
		g.drawString("UMLAUTE: ÄÖÜ äöü", 10, 60, Graphics.LEFT | Graphics.TOP);
		g.drawString("LINE ONE\nLINE TWO", 10, 75, Graphics.LEFT | Graphics.TOP);

		//anchors around a cross
		int cx = getWidth() / 2, cy = 180;
		g.setColor(0xFF0000);
		g.drawLine(cx - 150, cy, cx + 150, cy);
		g.drawLine(cx, cy - 40, cx, cy + 40);
		g.setColor(0xFFFFFF);
		g.drawString("RIGHT+BOTTOM", cx - 4, cy, Graphics.RIGHT | Graphics.BOTTOM);
		g.drawString("LEFT+TOP", cx + 4, cy + 2, Graphics.LEFT | Graphics.TOP);
		g.drawString("HCENTER+BASELINE", cx, cy - 20, Graphics.HCENTER | Graphics.BASELINE);

		//drawChar, drawSubstring
		g.drawChar('A', 10, 250, Graphics.LEFT | Graphics.TOP);
		g.drawChars(new char[] { 'B', 'C', 'D' }, 0, 3, 30, 250, Graphics.LEFT | Graphics.TOP);
		g.drawSubstring("XXSUBSTRINGXX", 2, 9, 80, 250, Graphics.LEFT | Graphics.TOP);
	}
}


//====================================================================================
// INPUT: key codes, game actions, key names, pointer

class InputDemo extends Canvas {

	String 	last_key 	= "PRESS A KEY";
	String 	last_action = "";
	int 	repeats 	= 0;
	int 	pointer_x 	= -1, pointer_y = -1;
	boolean dragging 	= false;

	protected void keyPressed(int keyCode) {
		last_key 	= "KEY CODE " + keyCode + " = " + getKeyName(keyCode);
		int a 		= getGameAction(keyCode);
		last_action = (a == 0) ? "NO GAME ACTION" : "GAME ACTION " + a + " " + actionName(a);
		repeats 	= 0;
		repaint();
	}

	protected void keyRepeated(int keyCode) {
		repeats++;
		repaint();
	}

	protected void pointerPressed(int x, int y) {
		pointer_x = x;
		pointer_y = y;
		dragging = true;
		repaint();
	}

	protected void pointerDragged(int x, int y) {
		pointer_x = x;
		pointer_y = y;
		repaint();
	}

	protected void pointerReleased(int x, int y) {
		dragging = false;
		repaint();
	}

	static String actionName(int a) {
		switch (a) {
			case UP: 		return "UP";
			case DOWN: 		return "DOWN";
			case LEFT: 		return "LEFT";
			case RIGHT: 	return "RIGHT";
			case FIRE: 		return "FIRE";
			case GAME_A: 	return "GAME_A";
			case GAME_B: 	return "GAME_B";
			case GAME_C: 	return "GAME_C";
			case GAME_D: 	return "GAME_D";
		}
		return "";
	}

	protected void paint(Graphics g) {
		TestMIDlet.drawTitle(g, this, "KEYS + POINTER");

		g.setColor(0xFFFFFF);
		g.drawString(last_key, 10, 40, Graphics.LEFT | Graphics.TOP);
		g.drawString(last_action, 10, 55, Graphics.LEFT | Graphics.TOP);
		g.drawString("REPEATED: " + repeats, 10, 70, Graphics.LEFT | Graphics.TOP);
		g.drawString("CLICK OR DRAG WITH THE MOUSE", 10, 100, Graphics.LEFT | Graphics.TOP);

		if (pointer_x >= 0) {
			g.setColor(dragging ? 0xFF0000 : 0x00FF00);
			g.fillArc(pointer_x - 5, pointer_y - 5, 10, 10, 0, 360);
			g.setColor(0xFFFFFF);
			g.drawString("POINTER " + pointer_x + "," + pointer_y, 10, 115, Graphics.LEFT | Graphics.TOP);
		}
	}
}


//====================================================================================
// GAME: GameCanvas with game loop, getKeyStates, TiledLayer with animated water,
//       Sprite with animation and mirroring, collision, LayerManager with scrolling

class GameDemo extends GameCanvas implements Runnable {

	static final int TILE = 16;

	//tiles (1-based): 1 = grass, 2 = wall, 3 = water 1, 4 = water 2
	static final int GRASS = 1, WALL = 2, WATER = 3;

	TiledLayer 		ground, walls;
	Sprite 			player;
	LayerManager 	layers;
	int 			water_anim;
	boolean 		running;
	int 			frame;

	GameDemo() {
		super(true);	//keys only through getKeyStates()

		Image tiles = createTiles();

		//40 x 30 tiles = 640 x 480, bigger than the screen -> scrolling
		ground 	= new TiledLayer(40, 30, tiles, TILE, TILE);
		walls 	= new TiledLayer(40, 30, tiles, TILE, TILE);
		ground.fillCells(0, 0, 40, 30, GRASS);

		water_anim = ground.createAnimatedTile(WATER);
		ground.fillCells(24, 4, 10, 6, water_anim);

		//a frame of walls and some blocks
		walls.fillCells(0, 0, 40, 1, WALL);
		walls.fillCells(0, 29, 40, 1, WALL);
		walls.fillCells(0, 0, 1, 30, WALL);
		walls.fillCells(39, 0, 1, 30, WALL);
		walls.fillCells(8, 8, 1, 12, WALL);
		walls.fillCells(8, 8, 10, 1, WALL);
		walls.fillCells(14, 18, 12, 1, WALL);

		player = new Sprite(createPlayer(), TILE, TILE);
		player.defineReferencePixel(TILE / 2, TILE / 2);
		player.setRefPixelPosition(60, 60);

		layers = new LayerManager();
		layers.append(player);		//index 0 = on top
		layers.append(walls);
		layers.append(ground);
	}

	protected void showNotify() {
		running = true;
		new Thread(this).start();
	}

	protected void hideNotify() {
		running = false;
	}

	public void run() {
		Graphics g = getGraphics();

		while (running) {
			update();
			draw(g);
			flushGraphics();
			TestMIDlet.ThreadSleep(30);
		}
	}

	void update() {
		frame++;

		//water animation: switch the animated tile every 15 frames
		if (frame % 15 == 0) ground.setAnimatedTile(water_anim, (frame / 15) % 2 == 0 ? WATER : WATER + 1);

		int keys 	= getKeyStates();
		int dx 		= 0, dy = 0;

		if ((keys & LEFT_PRESSED) 	!= 0) dx = -2;
		if ((keys & RIGHT_PRESSED) 	!= 0) dx = 2;
		if ((keys & UP_PRESSED) 	!= 0) dy = -2;
		if ((keys & DOWN_PRESSED) 	!= 0) dy = 2;

		//look in the direction we walk
		if (dx < 0) player.setTransform(Sprite.TRANS_MIRROR);
		if (dx > 0) player.setTransform(Sprite.TRANS_NONE);

		//walk, but not through walls
		player.move(dx, 0);
		if (player.collidesWith(walls, false)) player.move(-dx, 0);
		player.move(0, dy);
		if (player.collidesWith(walls, false)) player.move(0, -dy);

		if ((dx != 0 || dy != 0) && frame % 6 == 0) player.nextFrame();
	}

	void draw(Graphics g) {
		//view window follows the player
		int vw = getWidth(), vh = getHeight() - 16;
		int vx = Math.max(0, Math.min(ground.getWidth()  - vw, player.getRefPixelX() - vw / 2));
		int vy = Math.max(0, Math.min(ground.getHeight() - vh, player.getRefPixelY() - vh / 2));

		layers.setViewWindow(vx, vy, vw, vh);
		layers.paint(g, 0, 16);

		g.setColor(0x000000);
		g.fillRect(0, 0, getWidth(), 16);
		g.setColor(0xFFFFFF);
		g.drawString("CURSOR KEYS = WALK   F1 = BACK", 4, 4, Graphics.LEFT | Graphics.TOP);
		g.drawString("X " + player.getRefPixelX() + " Y " + player.getRefPixelY(), getWidth() - 4, 4, Graphics.RIGHT | Graphics.TOP);
	}

	/** 4 tiles, drawn in code: grass, wall, water, water (2nd frame) */
	static Image createTiles() {
		Image img = Image.createImage(TILE * 4, TILE);
		Graphics g = img.getGraphics();

		//grass
		g.setColor(0x30A030);
		g.fillRect(0, 0, TILE, TILE);
		g.setColor(0x50C050);
		g.fillRect(3, 4, 2, 2);
		g.fillRect(10, 9, 2, 2);

		//wall
		g.setColor(0x808080);
		g.fillRect(TILE, 0, TILE, TILE);
		g.setColor(0x505050);
		g.drawLine(TILE, 7, TILE * 2, 7);
		g.drawLine(TILE + 7, 0, TILE + 7, 7);
		g.drawLine(TILE + 3, 8, TILE + 3, 15);
		g.drawLine(TILE + 12, 8, TILE + 12, 15);

		//water, 2 frames
		for (int i = 0; i < 2; i++) {
			int x = TILE * (2 + i);
			g.setColor(0x2050D0);
			g.fillRect(x, 0, TILE, TILE);
			g.setColor(0x80A0FF);
			g.drawLine(x + 2 + i * 4, 5, x + 7 + i * 4, 5);
			g.drawLine(x + 6 - i * 4, 11, x + 11 - i * 4, 11);
		}

		return Image.createImage(img);	//immutable copy
	}

	/** 2 frames of a little figure, with transparent background */
	static Image createPlayer() {
		int w = TILE * 2;
		int[] rgb = new int[w * TILE];	//transparent

		for (int f = 0; f < 2; f++) {
			int ox = f * TILE;
			for (int y = 0; y < TILE; y++) {
				for (int x = 0; x < TILE; x++) {
					int c = 0;
					int dx = x - 8, dy = y - 4;
					if (dx * dx + dy * dy <= 9) 					c = 0xFFFFD0A0;	//head
					if (y >= 7 && y <= 11 && x >= 5 && x <= 10) 	c = 0xFFE03030;	//body
					if (x == 11 && y == 3) 							c = 0xFF000000;	//eye (looks right)
					//legs, moving
					boolean leg1 = (f == 0) ? (x == 6) : (x == 5);
					boolean leg2 = (f == 0) ? (x == 9) : (x == 10);
					if (y >= 12 && (leg1 || leg2)) 					c = 0xFF2020A0;
					rgb[y * w + ox + x] = c;
				}
			}
		}
		return Image.createRGBImage(rgb, w, TILE, true);
	}
}


//====================================================================================
// PARTICLES: gfc.graphics explosions

class ParticleDemo extends Canvas implements Runnable {

	Vector 		explosions 	= new Vector();
	boolean 	running;
	java.util.Random random = new java.util.Random();

	static final int[] COLORS = { 0xFFFF00, 0xFFC000, 0xFF8000, 0xFF4000, 0xFF0000, 0x800000 };

	protected void showNotify() {
		running = true;
		new Thread(this).start();
	}

	protected void hideNotify() {
		running = false;
	}

	protected void keyPressed(int keyCode) {
		if (getGameAction(keyCode) == FIRE) explode( 50 + random.nextInt(getWidth() - 100), 50 + random.nextInt(getHeight() - 100) );
	}

	protected void pointerPressed(int x, int y) {
		explode(x, y);
	}

	void explode(int x, int y) {
		Explosion e = new Explosion(6, 25, 4, 6, 4, COLORS);
		e.reset(x, y);
		synchronized (explosions) {
			explosions.addElement(e);
		}
	}

	public void run() {
		while (running) {
			synchronized (explosions) {
				for (int i = explosions.size() - 1; i >= 0; i--) {
					Explosion e = (Explosion)explosions.elementAt(i);
					e.toNextFrame();
					if (!e.hasMoreFrames()) explosions.removeElementAt(i);
				}
			}
			repaint();
			TestMIDlet.ThreadSleep(30);
		}
	}

	protected void paint(Graphics g) {
		g.setColor(0x000000);
		g.fillRect(0, 0, getWidth(), getHeight());
		g.setColor(0xFFFFFF);
		g.drawString("RETURN OR MOUSE CLICK = EXPLOSION   F1 = BACK", getWidth() / 2, 4, Graphics.HCENTER | Graphics.TOP);

		synchronized (explosions) {
			for (int i = 0; i < explosions.size(); i++) {
				((Explosion)explosions.elementAt(i)).paint(g);
			}
		}
	}
}


//====================================================================================
// SOUND: tones, tone sequences, WAV, MIDI music, volume

class SoundDemo extends List implements CommandListener {

	static final String[] ITEMS = {
		"SINGLE TONE (MANAGER.PLAYTONE)",
		"TONE SEQUENCE (GFC.AUDIO.TONE)",
		"SOUND EFFECT (RES/SOUND.WAV)",
		"MUSIC START (RES/MUSIC.MID)",
		"MUSIC STOP",
		"VOLUME 100",
		"VOLUME 30",
		"MUTE ON/OFF",
	};

	Tone 	tone;
	Sound 	sound;
	Music 	music;

	SoundDemo() {
		super("SOUND + MUSIC", List.IMPLICIT, ITEMS, null);
		setCommandListener(this);

		tone 	= new Tone( new byte[] { Tone.C4, 8, Tone.E4, 8, Tone.G4, 8, Tone.C5, 16 }, (byte)80, (byte)30 );
		sound 	= new Sound("/res/sound.wav", "audio/x-wav");
		music 	= new Music("/res/music.mid", "audio/midi", Music.LOOP_INDEFINITELY);
	}

	public void commandAction(Command c, Displayable d) {
		if (c != List.SELECT_COMMAND) {
			TestMIDlet.midlet.commandAction(c, d);	//BACK
			return;
		}

		try {
			switch (getSelectedIndex()) {
				case 0: Manager.playTone(ToneControl_C5(), 300, 80); break;
				case 1: tone.play(); 		break;
				case 2: sound.play(); 		break;
				case 3: music.play(); 		break;
				case 4: Audible.stopMusic(); break;
				case 5: Audible.setVolume(100); break;
				case 6: Audible.setVolume(30); 	break;
				case 7: Audible.setMute( !Audible.isMuted() ); break;
			}
		}
		catch (MediaException e) {
			e.printStackTrace();
		}
	}

	static int ToneControl_C5() {
		return gfc.microedition.media.control.ToneControl.C4 + 12;
	}
}


//====================================================================================
// SAVE GAME: RecordStore -- survives a restart

class SaveDemo extends Canvas {

	static final String STORE = "SHOWCASE";

	int 	starts;
	Vector 	entries = new Vector();
	String 	error;

	SaveDemo() {
		countStart();
		load();
	}

	/** record 1 = how often this demo was opened */
	void countStart() {
		try {
			RecordStore rs = RecordStore.openRecordStore(STORE, true);

			if (rs.getNumRecords() == 0) {
				rs.addRecord( intToBytes(0), 0, 4 );
			}
			starts = bytesToInt( rs.getRecord(1) ) + 1;
			rs.setRecord(1, intToBytes(starts), 0, 4);

			rs.closeRecordStore();
		}
		catch (RecordStoreException e) {
			error = e.getMessage();
		}
	}

	void load() {
		entries.removeAllElements();
		try {
			RecordStore rs = RecordStore.openRecordStore(STORE, true);

			//all other records: text entries, oldest first
			RecordEnumeration en = rs.enumerateRecords(null, null, false);
			while (en.hasNextElement()) {
				int id = en.nextRecordId();
				if (id == 1) continue;
				byte[] b = rs.getRecord(id);
				entries.addElement( "#" + id + " " + (b == null ? "" : new String(b)) );
			}
			en.destroy();

			rs.closeRecordStore();
		}
		catch (RecordStoreException e) {
			error = e.getMessage();
		}
	}

	protected void keyPressed(int keyCode) {
		try {
			if (getGameAction(keyCode) == FIRE) {
				//add an entry
				RecordStore rs = RecordStore.openRecordStore(STORE, true);
				byte[] b = ("SAVED AT " + new java.util.Date().toString().substring(11, 19)).getBytes();
				rs.addRecord(b, 0, b.length);
				rs.closeRecordStore();
			}
			else if (keyCode == KEY_POUND) {
				//delete everything, then start counting again
				RecordStore.deleteRecordStore(STORE);
				countStart();
			}
			else return;
		}
		catch (RecordStoreException e) {
			error = e.getMessage();
		}

		load();
		repaint();
	}

	protected void paint(Graphics g) {
		TestMIDlet.drawTitle(g, this, "SAVE GAME (RMS)");
		g.setColor(0xFFFFFF);

		g.drawString("THIS DEMO WAS OPENED " + starts + " TIMES (ALSO AFTER A RESTART)", 10, 30, Graphics.LEFT | Graphics.TOP);
		g.drawString("RETURN = SAVE AN ENTRY   # = DELETE ALL", 10, 45, Graphics.LEFT | Graphics.TOP);
		g.drawString("STORES: " + join(RecordStore.listRecordStores()), 10, 60, Graphics.LEFT | Graphics.TOP);

		for (int i = 0; i < entries.size() && i < 15; i++) {
			g.drawString( (String)entries.elementAt(i), 20, 85 + i * 14, Graphics.LEFT | Graphics.TOP );
		}

		if (error != null) {
			g.setColor(0xFF8080);
			g.drawString(error, 10, getHeight() - 30, Graphics.LEFT | Graphics.TOP);
		}
	}

	static String join(String[] s) {
		if (s == null) return "-";
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < s.length; i++) sb.append(i > 0 ? ", " : "").append(s[i]);
		return sb.toString();
	}

	static byte[] intToBytes(int v) {
		return new byte[] { (byte)(v >> 24), (byte)(v >> 16), (byte)(v >> 8), (byte)v };
	}

	static int bytesToInt(byte[] b) {
		return (b[0] & 0xFF) << 24 | (b[1] & 0xFF) << 16 | (b[2] & 0xFF) << 8 | (b[3] & 0xFF);
	}
}


//====================================================================================
// COMMAND MENU: more than one command on a soft key opens a menu

class CommandMenuDemo extends Canvas implements CommandListener {

	String last = "NOTHING CHOSEN YET";

	static final Command
		HELP 	= new Command("HELP", 	Command.HELP, 1),
		INFO 	= new Command("INFO", 	Command.SCREEN, 1),
		ABOUT 	= new Command("ABOUT", 	Command.OK, 1);

	CommandMenuDemo() {
		addCommand(HELP);
		addCommand(INFO);
		addCommand(ABOUT);
		setCommandListener(this);
	}

	public void commandAction(Command c, Displayable d) {
		if (c == TestMIDlet.BACK_COMMAND) {
			TestMIDlet.midlet.backToMenu();
			return;
		}
		last = "YOU CHOSE " + c.getLabel();
		repaint();
	}

	protected void paint(Graphics g) {
		TestMIDlet.drawTitle(g, this, "COMMAND MENU");
		g.setColor(0xFFFFFF);
		g.drawString("THIS CANVAS HAS 3 COMMANDS ON THE RIGHT SOFT KEY.", 10, 40, Graphics.LEFT | Graphics.TOP);
		g.drawString("PRESS F2 TO OPEN THE MENU, CHOOSE WITH CURSOR + RETURN.", 10, 55, Graphics.LEFT | Graphics.TOP);
		g.drawString(last, 10, 90, Graphics.LEFT | Graphics.TOP);
	}
}


//====================================================================================
// DEVICE INFO: Display and MIDlet properties

class DeviceDemo extends Canvas {

	protected void paint(Graphics g) {
		TestMIDlet.drawTitle(g, this, "DEVICE INFO");
		Display d = Display.getDisplay(TestMIDlet.midlet);

		String[] lines = {
			"SCREEN: " + getWidth() + " X " + getHeight(),
			"COLOR: " + d.isColor() + ", " + d.numColors() + " COLORS, " + d.numAlphaLevels() + " ALPHA LEVELS",
			"POINTER: " + hasPointerEvents() + ", MOTION: " + hasPointerMotionEvents() + ", KEY REPEAT: " + hasRepeatEvents(),
			"VIBRATE: " + d.vibrate(100) + " (NO VIBRATOR ON A PC)",
			"BACKLIGHT: " + d.flashBacklight(100),
			"WINDOW TITLE (GETAPPPROPERTY): " + TestMIDlet.midlet.getAppProperty("window.title"),
			"MIDLET CLASS: " + TestMIDlet.midlet.getAppProperty("class"),
			"KEY FOR FIRE: " + getKeyCode(FIRE) + " = " + getKeyName(getKeyCode(FIRE)),
			"JAVA: " + System.getProperty("java.version"),
		};

		g.setColor(0xFFFFFF);
		for (int i = 0; i < lines.length; i++) {
			g.drawString(lines[i], 10, 30 + i * 15, Graphics.LEFT | Graphics.TOP);
		}
	}
}


//====================================================================================
// NETWORK: HttpConnection and platformRequest

class NetworkDemo extends Canvas implements Runnable {

	static final String URL 	= "http://example.com/";
	static final String GITHUB 	= "https://github.com/gus0650/GFC.Java";

	String 	status 	= "RETURN = LOAD " + URL;
	String 	result 	= "";

	protected void keyPressed(int keyCode) {
		if (getGameAction(keyCode) == FIRE) {
			status = "LOADING...";
			result = "";
			repaint();
			new Thread(this).start();	//never block the painting thread with network access
		}
		else if (keyCode == KEY_POUND) {
			try {
				TestMIDlet.midlet.platformRequest(GITHUB);
				status = "BROWSER OPENED";
			}
			catch (ConnectionNotFoundException e) {
				status = "NO BROWSER: " + e.getMessage();
			}
			repaint();
		}
	}

	public void run() {
		HttpConnection c = null;
		try {
			c = (HttpConnection)Connector.open(URL, Connector.READ, true);
			c.setRequestProperty("User-Agent", "GFC.Java");

			int code 	= c.getResponseCode();
			//read() may deliver only a part -> read until 400 bytes or the end
			InputStream in = c.openInputStream();
			byte[] b 	= new byte[400];
			int n 		= 0;
			int k;
			while (n < b.length && (k = in.read(b, n, b.length - n)) != -1) n += k;
			in.close();

			status = "HTTP " + code + " " + c.getResponseMessage() + ", " + c.getType();
			result = (n > 0) ? new String(b, 0, n) : "";
		}
		catch (IOException e) {
			status = "ERROR: " + e.getMessage();
		}
		finally {
			try { if (c != null) c.close(); } catch (IOException e) { }
		}
		repaint();
	}

	protected void paint(Graphics g) {
		TestMIDlet.drawTitle(g, this, "NETWORK");
		g.setColor(0xFFFFFF);
		g.drawString(status, 10, 30, Graphics.LEFT | Graphics.TOP);
		g.drawString("# = OPEN THE GFC.JAVA PAGE IN THE BROWSER", 10, 45, Graphics.LEFT | Graphics.TOP);

		//the first characters of the page, line by line
		String[] lines = gfc.util.StringTool.linesAsArray(result);
		for (int i = 0; i < lines.length && i < 14; i++) {
			String l = printable( lines[i].trim() );
			if (l.length() > 80) l = l.substring(0, 80);
			g.drawString(l, 10, 70 + i * 14, Graphics.LEFT | Graphics.TOP);
		}
	}

	/** the bitmap font has the characters from "!" to "\\" -- everything else becomes "?" */
	static String printable(String s) {
		StringBuffer sb = new StringBuffer();
		String u = s.toUpperCase();
		for (int i = 0; i < u.length(); i++) {
			char c = u.charAt(i);
			sb.append( (c == ' ' || (c >= '!' && c <= '\\')) ? c : '?' );
		}
		return sb.toString();
	}
}
