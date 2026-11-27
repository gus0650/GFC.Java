package gfc.microedition.media;

import gfc.microedition.media.control.ToneControl;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Renders MIDP tone sequences (see ToneControl) into 16 bit mono PCM.
 */
final class ToneSynth {

	static final float SAMPLE_RATE = 22050f;

	private ToneSynth() { }

	/** Renders a single note. */
	static byte[] renderTone(int note, int durationMs, int volume) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		writeNote(out, note, durationMs, volume);
		return out.toByteArray();
	}

	/** Renders a complete tone sequence. */
	static byte[] renderSequence(byte[] seq) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		if (seq == null) return out.toByteArray();

		int[] state = { 30, 64, 100 };  // tempo (x4 = bpm), resolution, volume
		Map<Integer, int[]> blocks = new HashMap<>();  // block number -> {start, end}

		//first pass: header and block definitions
		int i = 0;
		while (i + 1 < seq.length) {
			byte cmd = seq[i];
			if (cmd == ToneControl.VERSION) { i += 2; }
			else if (cmd == ToneControl.TEMPO) { state[0] = seq[i + 1]; i += 2; }
			else if (cmd == ToneControl.RESOLUTION) { state[1] = seq[i + 1]; i += 2; }
			else if (cmd == ToneControl.BLOCK_START) {
				int nr = seq[i + 1];
				int start = i + 2;
				int j = start;
				while (j + 1 < seq.length && !(seq[j] == ToneControl.BLOCK_END && seq[j + 1] == nr)) j += 2;
				blocks.put(nr, new int[] { start, j });
				i = j + 2;
			}
			else break;
		}

		play(seq, i, seq.length, state, blocks, out, 0);
		return out.toByteArray();
	}

	private static void play(byte[] seq, int from, int to, int[] state, Map<Integer, int[]> blocks,
			ByteArrayOutputStream out, int depth) {
		if (depth > 8) return;  // guard against recursive blocks

		int i = from;
		while (i + 1 < to) {
			byte cmd = seq[i];
			byte arg = seq[i + 1];

			if (cmd == ToneControl.SET_VOLUME) {
				state[2] = arg;
			}
			else if (cmd == ToneControl.TEMPO) {
				state[0] = arg;
			}
			else if (cmd == ToneControl.RESOLUTION) {
				state[1] = arg;
			}
			else if (cmd == ToneControl.PLAY_BLOCK) {
				int[] b = blocks.get((int) arg);
				if (b != null) play(seq, b[0], b[1], state, blocks, out, depth + 1);
			}
			else if (cmd == ToneControl.REPEAT) {
				int count = arg;
				if (i + 3 < to) {
					byte note = seq[i + 2];
					int ms = durationMs(seq[i + 3], state);
					for (int r = 0; r < count; r++) writeNote(out, note, ms, state[2]);
					i += 2;
				}
			}
			else if (cmd == ToneControl.SILENCE || cmd >= 0) {
				writeNote(out, cmd, durationMs(arg, state), state[2]);
			}
			// VERSION, BLOCK_START/END inside the body are ignored

			i += 2;
		}
	}

	private static int durationMs(int units, int[] state) {
		int bpm = Math.max(1, state[0]) * 4;
		int res = Math.max(1, state[1]);
		// one whole note = 4 beats
		return (int) (units * 4L * 60000L / (res * (long) bpm));
	}

	private static void writeNote(ByteArrayOutputStream out, int note, int durationMs, int volume) {
		int samples = (int) (SAMPLE_RATE * durationMs / 1000f);
		double amp = 32767 * 0.3 * Math.max(0, Math.min(100, volume)) / 100.0;
		boolean silent = note < 0;
		double freq = silent ? 0 : 440.0 * Math.pow(2, (note - 69) / 12.0);
		int fade = Math.min(samples / 2, (int) (SAMPLE_RATE * 0.005));  // 5 ms fade to avoid clicks

		for (int s = 0; s < samples; s++) {
			double v = 0;
			if (!silent) {
				// square wave, like a handset buzzer
				v = Math.sin(2 * Math.PI * freq * s / SAMPLE_RATE) >= 0 ? amp : -amp;
				if (s < fade) v *= s / (double) fade;
				else if (s > samples - fade) v *= (samples - s) / (double) fade;
			}
			short sv = (short) v;
			out.write(sv & 0xff);
			out.write((sv >> 8) & 0xff);
		}
	}
}
