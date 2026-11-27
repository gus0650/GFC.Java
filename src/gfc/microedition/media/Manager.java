package gfc.microedition.media;

import java.io.*;

/**
 * Desktop replacement for javax.microedition.media.Manager.
 * <p>
 * Sampled audio (WAV, AIFF, AU) is played via javax.sound.sampled.
 * Tone sequences (ToneControl) are synthesized into PCM and played the same way.
 * MIDI and other formats are not supported yet: those players are created but stay silent.
 */
public final class Manager {

	public static final String TONE_DEVICE_LOCATOR = "device://tone";
	public static final String MIDI_DEVICE_LOCATOR = "device://midi";

	private Manager() { }

	public static String[] getSupportedContentTypes(String protocol) {
		return new String[] { "audio/x-wav", "audio/wav", "audio/basic", "audio/x-aiff", "audio/x-tone-seq" };
	}

	public static String[] getSupportedProtocols(String contentType) {
		return new String[] { "device", "file", "resource" };
	}

	public static Player createPlayer(InputStream stream, String type) throws IOException, MediaException {
		if (stream == null) throw new IllegalArgumentException("stream is null");

		//read everything so the player can be restarted and looped
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] buf = new byte[8192];
		int n;
		while ((n = stream.read(buf)) > 0) bos.write(buf, 0, n);

		return new SampledPlayer(bos.toByteArray(), type);
	}

	public static Player createPlayer(String locator) throws IOException, MediaException {
		if (locator == null) throw new IllegalArgumentException("locator is null");

		if (locator.equals(TONE_DEVICE_LOCATOR)) {
			return new SampledPlayer(null, "audio/x-tone-seq");
		}

		//resource or file path
		String path = locator.startsWith("file://") ? locator.substring(7) : locator;
		InputStream is = Manager.class.getResourceAsStream(path.startsWith("/") ? path : "/" + path);
		if (is == null) {
			File f = new File(path);
			if (!f.exists()) throw new MediaException("cannot open " + locator);
			is = new FileInputStream(f);
		}

		try {
			return createPlayer(is, guessType(path));
		}
		finally {
			is.close();
		}
	}

	/** Plays a single tone. note: MIDI note number (C4 = 60), duration in ms, volume 0..100. */
	public static void playTone(int note, int duration, int volume) throws MediaException {
		if (note < 0 || note > 127 || duration <= 0) throw new IllegalArgumentException();

		final byte[] pcm = ToneSynth.renderTone(note, duration, volume);
		Thread t = new Thread(() -> SampledPlayer.playOnce(pcm), "GFC playTone");
		t.setDaemon(true);
		t.start();
	}

	private static String guessType(String path) {
		String p = path.toLowerCase();
		if (p.endsWith(".wav")) return "audio/x-wav";
		if (p.endsWith(".au")) return "audio/basic";
		if (p.endsWith(".aif") || p.endsWith(".aiff")) return "audio/x-aiff";
		if (p.endsWith(".mid") || p.endsWith(".midi")) return "audio/midi";
		return "application/octet-stream";
	}
}
