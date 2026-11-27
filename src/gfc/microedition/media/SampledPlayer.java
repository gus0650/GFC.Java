package gfc.microedition.media;

import gfc.microedition.media.control.ToneControl;
import gfc.microedition.media.control.VolumeControl;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import javax.sound.sampled.*;

/**
 * Player implementation based on javax.sound.sampled.Clip.
 * Used for sampled audio and for synthesized tone sequences.
 * If audio is unavailable (no sound device, unsupported format) the player stays silent
 * instead of throwing, so games keep running.
 */
final class SampledPlayer implements Player, VolumeControl, ToneControl {

	private final String contentType;
	private byte[] data;            // encoded audio file, or null for tone sequences
	private byte[] toneSequence;    // only for tone players

	private Clip clip;
	private int state = UNREALIZED;
	private int loopCount = 1;
	private int level = 100;
	private boolean muted = false;
	private final List<PlayerListener> listeners = new ArrayList<>();

	SampledPlayer(byte[] data, String contentType) {
		this.data = data;
		this.contentType = contentType;
	}

	private boolean isTonePlayer() {
		return data == null;
	}

	//--- Player

	public void realize() throws MediaException {
		checkClosed();
		if (state < REALIZED) state = REALIZED;
	}

	public void prefetch() throws MediaException {
		checkClosed();
		if (state < REALIZED) realize();
		if (clip == null) openClip();
		if (state < PREFETCHED) state = PREFETCHED;
	}

	public void start() throws MediaException {
		checkClosed();
		if (state == STARTED) return;
		prefetch();

		if (clip != null) {
			applyVolume();
			if (clip.getFramePosition() >= clip.getFrameLength()) clip.setFramePosition(0);
			if (loopCount == -1) clip.loop(Clip.LOOP_CONTINUOUSLY);
			else if (loopCount > 1) clip.loop(loopCount - 1);
			else clip.start();
		}
		state = STARTED;
		fire(PlayerListener.STARTED, Long.valueOf(getMediaTime()));
	}

	public void stop() throws MediaException {
		checkClosed();
		if (state != STARTED) return;
		if (clip != null) clip.stop();
		state = PREFETCHED;
		fire(PlayerListener.STOPPED, Long.valueOf(getMediaTime()));
	}

	public void deallocate() {
		if (state == CLOSED) return;
		if (clip != null) {
			clip.stop();
			clip.close();
			clip = null;
		}
		if (state > REALIZED) state = REALIZED;
	}

	public void close() {
		if (state == CLOSED) return;
		deallocate();
		state = CLOSED;
		data = null;
		fire(PlayerListener.CLOSED, null);
	}

	public long setMediaTime(long now) throws MediaException {
		checkClosed();
		if (clip == null) return 0;
		long t = Math.max(0, Math.min(now, clip.getMicrosecondLength()));
		clip.setMicrosecondPosition(t);
		return t;
	}

	public long getMediaTime() {
		return clip == null ? 0 : clip.getMicrosecondPosition();
	}

	public long getDuration() {
		return clip == null ? TIME_UNKNOWN : clip.getMicrosecondLength();
	}

	public int getState() {
		if (state == STARTED && clip != null && !clip.isRunning()
				&& clip.getFramePosition() >= clip.getFrameLength()) {
			state = PREFETCHED;
			fire(PlayerListener.END_OF_MEDIA, Long.valueOf(getMediaTime()));
		}
		return state;
	}

	public String getContentType() {
		return contentType;
	}

	public void setLoopCount(int count) {
		if (count == 0) throw new IllegalArgumentException("loop count must not be 0");
		loopCount = count;
	}

	public void addPlayerListener(PlayerListener l) {
		if (l != null && !listeners.contains(l)) listeners.add(l);
	}

	public void removePlayerListener(PlayerListener l) {
		listeners.remove(l);
	}

	//--- Controllable

	public Control getControl(String controlType) {
		if (controlType == null) throw new IllegalArgumentException();
		String t = controlType.substring(controlType.lastIndexOf('.') + 1);
		if (t.equals("VolumeControl")) return this;
		if (t.equals("ToneControl") && isTonePlayer()) return this;
		return null;
	}

	public Control[] getControls() {
		return isTonePlayer() ? new Control[] { this } : new Control[] { (VolumeControl) this };
	}

	//--- VolumeControl

	public void setMute(boolean mute) {
		muted = mute;
		applyVolume();
	}

	public boolean isMuted() {
		return muted;
	}

	public int setLevel(int l) {
		level = Math.max(0, Math.min(100, l));
		applyVolume();
		fire(PlayerListener.VOLUME_CHANGED, this);
		return level;
	}

	public int getLevel() {
		return level;
	}

	//--- ToneControl

	public void setSequence(byte[] sequence) {
		if (state >= PREFETCHED) throw new IllegalStateException("setSequence after prefetch");
		toneSequence = sequence;
	}

	//--- internals

	private void checkClosed() {
		if (state == CLOSED) throw new IllegalStateException("player is closed");
	}

	private void openClip() {
		try {
			AudioInputStream ais;
			if (isTonePlayer()) {
				byte[] pcm = ToneSynth.renderSequence(toneSequence);
				ais = pcmStream(pcm);
			}
			else {
				ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(data));
			}
			Clip c = AudioSystem.getClip();
			c.open(ais);
			clip = c;
		}
		catch (Exception e) {
			// no sound device or unsupported format (e.g. MIDI): stay silent
			System.err.println("WARNING in Player: audio not available (" + contentType + "): " + e);
			clip = null;
		}
	}

	private static AudioInputStream pcmStream(byte[] pcm) {
		AudioFormat fmt = new AudioFormat(ToneSynth.SAMPLE_RATE, 16, 1, true, false);
		return new AudioInputStream(new ByteArrayInputStream(pcm), fmt, pcm.length / 2);
	}

	private void applyVolume() {
		if (clip == null) return;
		try {
			if (clip.isControlSupported(BooleanControl.Type.MUTE)) {
				((BooleanControl) clip.getControl(BooleanControl.Type.MUTE)).setValue(muted);
			}
			if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
				FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
				float db = (level <= 0 || muted) ? gain.getMinimum() : (float) (20 * Math.log10(level / 100.0));
				gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db)));
			}
		}
		catch (Exception e) {
			// ignore, volume is cosmetic
		}
	}

	private void fire(String event, Object data) {
		for (PlayerListener l : new ArrayList<>(listeners)) {
			l.playerUpdate(this, event, data);
		}
	}

	/** Plays raw tone PCM once, blocking until finished. Used by Manager.playTone(). */
	static void playOnce(byte[] pcm) {
		try {
			Clip c = AudioSystem.getClip();
			c.open(pcmStream(pcm));
			c.start();
			Thread.sleep(Math.max(1, c.getMicrosecondLength() / 1000) + 50);
			c.close();
		}
		catch (Exception e) {
			System.err.println("WARNING in Manager.playTone(): audio not available: " + e);
		}
	}
}
