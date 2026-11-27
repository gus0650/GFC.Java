package gfc.microedition.media;

import gfc.microedition.media.control.VolumeControl;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import javax.sound.midi.*;

/**
 * Player for MIDI music, based on javax.sound.midi (sequencer + the software synthesizer of the JDK).
 * If no synthesizer is available the player stays silent instead of throwing, so games keep running.
 */
final class MidiPlayer implements Player, VolumeControl {

	private final byte[] 	data;

	private Sequence 		sequence;
	private Sequencer 		sequencer;
	private Synthesizer 	synth;
	private Receiver 		synth_receiver;

	private int 		state 		= UNREALIZED;
	private int 		loopCount 	= 1;
	private int 		level 		= 100;
	private boolean 	muted 		= false;
	private final List<PlayerListener> listeners = new ArrayList<>();


	MidiPlayer(byte[] data) {
		this.data = data;
	}

	/** Standard MIDI files start with "MThd". */
	static boolean isMidi(byte[] data) {
		return data.length >= 4 && data[0] == 'M' && data[1] == 'T' && data[2] == 'h' && data[3] == 'd';
	}


	//--- Player

	public void realize() throws MediaException {
		checkClosed();
		if (state >= REALIZED) return;

		try {
			sequence = MidiSystem.getSequence( new ByteArrayInputStream(data) );
		}
		catch (Exception e) {
			throw new MediaException("ERROR in Player.realize(): not a valid MIDI file - " + e.getMessage());
		}

		state = REALIZED;
	}

	public void prefetch() throws MediaException {
		checkClosed();
		if (state < REALIZED) realize();
		if (state >= PREFETCHED) return;

		openSequencer();
		state = PREFETCHED;
	}

	public void start() throws MediaException {
		checkClosed();
		if (state == STARTED) return;
		prefetch();

		if (sequencer != null) {
			applyVolume();

			//end reached -> start from the beginning
			if (sequencer.getTickPosition() >= sequencer.getTickLength()) sequencer.setTickPosition(0);

			sequencer.setLoopStartPoint(0);
			sequencer.setLoopEndPoint(-1);
			sequencer.setLoopCount( loopCount == -1 ? Sequencer.LOOP_CONTINUOUSLY : loopCount - 1 );
			sequencer.start();
		}

		state = STARTED;
		fire(PlayerListener.STARTED, Long.valueOf(getMediaTime()));
	}

	public void stop() throws MediaException {
		checkClosed();
		if (state != STARTED) return;

		if (sequencer != null) {
			sequencer.stop();
			allNotesOff();
		}

		state = PREFETCHED;
		fire(PlayerListener.STOPPED, Long.valueOf(getMediaTime()));
	}

	public void deallocate() {
		if (state == CLOSED) return;

		if (sequencer != null) {
			sequencer.stop();
			sequencer.close();
			sequencer = null;
		}
		if (synth != null) {
			synth.close();
			synth = null;
			synth_receiver = null;
		}

		if (state > REALIZED) state = REALIZED;
	}

	public void close() {
		if (state == CLOSED) return;

		deallocate();
		state = CLOSED;
		fire(PlayerListener.CLOSED, null);
	}

	public long setMediaTime(long now) throws MediaException {
		checkClosed();
		if (sequencer == null) return 0;

		long t = Math.max(0, Math.min(now, sequencer.getMicrosecondLength()));
		sequencer.setMicrosecondPosition(t);
		return t;
	}

	public long getMediaTime() {
		return sequencer == null ? 0 : sequencer.getMicrosecondPosition();
	}

	public long getDuration() {
		if (sequence == null) return TIME_UNKNOWN;
		return sequence.getMicrosecondLength();
	}

	public int getState() {
		return state;
	}

	public String getContentType() {
		return "audio/midi";
	}

	public void setLoopCount(int count) {
		if (count == 0) throw new IllegalArgumentException("loop count must not be 0");
		if (state == STARTED) throw new IllegalStateException("setLoopCount while playing");

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

		return null;
	}

	public Control[] getControls() {
		return new Control[] { this };
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


	//--- internals

	private void checkClosed() {
		if (state == CLOSED) throw new IllegalStateException("player is closed");
	}

	private void openSequencer() {
		try {
			//own synthesizer per player, so that the volume can be set via the synthesizer
			synth = MidiSystem.getSynthesizer();
			synth.open();
			synth_receiver = synth.getReceiver();

			sequencer = MidiSystem.getSequencer(false);
			sequencer.getTransmitter().setReceiver(synth_receiver);
			sequencer.open();
			sequencer.setSequence(sequence);

			//end of song
			sequencer.addMetaEventListener( new MetaEventListener() {
				public void meta(MetaMessage msg) {
					if (msg.getType() != 47) return;
					if (state != STARTED) return;

					state = PREFETCHED;
					fire(PlayerListener.END_OF_MEDIA, Long.valueOf(getMediaTime()));
				}
			});
		}
		catch (Exception e) {
			//no sound device: stay silent
			System.err.println("WARNING in Player: MIDI not available: " + e);

			if (sequencer != null) sequencer.close();
			if (synth != null) synth.close();
			sequencer 		= null;
			synth 			= null;
			synth_receiver 	= null;
		}
	}

	/**
	 * Sets the master volume of the synthesizer (universal SysEx "master volume"),
	 * so that the volume changes inside the song still work.
	 */
	private void applyVolume() {
		if (synth_receiver == null) return;

		int v = muted ? 0 : (level * 16383) / 100;		//14 bit

		try {
			byte[] msg = { (byte)0xF0, 0x7F, 0x7F, 0x04, 0x01, (byte)(v & 0x7F), (byte)((v >> 7) & 0x7F), (byte)0xF7 };
			synth_receiver.send( new SysexMessage(msg, msg.length), -1 );
		}
		catch (InvalidMidiDataException e) {
			//ignore, volume is cosmetic
		}
	}

	/** Stops hanging notes after stop(). */
	private void allNotesOff() {
		if (synth == null) return;

		for (MidiChannel c : synth.getChannels()) {
			if (c != null) c.allNotesOff();
		}
	}

	private void fire(String event, Object data) {
		for (PlayerListener l : new ArrayList<>(listeners)) {
			l.playerUpdate(this, event, data);
		}
	}
}
