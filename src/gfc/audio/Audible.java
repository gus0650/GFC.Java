package gfc.audio;

import gfc.microedition.media.*;
import gfc.microedition.media.control.*;

abstract public class Audible {

	protected Player 			player;
	protected VolumeControl 	vc;

	protected static Audible	music;
	
	private static int 			vol 	= 50;
	private static boolean 		mute 	= false;
	
	
	public void play() {
		if (player == null) return;	//file could not be loaded (error was printed when loading)

		try {
			setVolume(vol);
			if (vc != null) vc.setMute(mute);
			if (player.getMediaTime() > 0) player.setMediaTime(0);
			player.start();
		} 
		catch (Exception e) { 
			/*config:debug:ON*/e.printStackTrace();
		}
	}
	
	public void stop() {
		if (player == null) return;	//file could not be loaded (error was printed when loading)

		try {
			player.stop();
		} 
		catch (Exception e) { 
			/*config:debug:ON*/e.printStackTrace();
		}
	}
	
	public static void stopMusic() {
		if (music != null) music.stop();
	}

	public static void startMusic() {
		if (music != null) music.play();
	}
	
	public static void setVolume(int v) {
		vol = v;

		if (music != null && music.vc != null) {
			music.vc.setLevel(vol);
		}
	}

	public static void setMute(boolean b) {
		mute = b;

		if (music != null && music.vc != null) {
			music.vc.setMute(mute);
		}
	}
	
	public static boolean isMuted() {
		return mute;
	}
}
