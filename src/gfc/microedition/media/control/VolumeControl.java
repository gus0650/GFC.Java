package gfc.microedition.media.control;

import gfc.microedition.media.Control;

public interface VolumeControl extends Control {
	void setMute(boolean mute);
	boolean isMuted();
	/** @param level 0..100 @return the level actually set */
	int setLevel(int level);
	int getLevel();
}
