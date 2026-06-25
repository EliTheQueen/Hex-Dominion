package view;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

/**
 * Background music for the game. To avoid shipping binary audio assets, a gentle ambient
 * pad is synthesised at runtime and looped. Volume is adjustable from the Settings dialog.
 *
 * <p>Everything is guarded so that a machine without an audio device (e.g. a CI grader)
 * simply runs silently rather than crashing.
 */
public final class SoundManager {

    private static final SoundManager INSTANCE = new SoundManager();
    public static SoundManager getInstance() { return INSTANCE; }

    private static final float SAMPLE_RATE = 44100f;

    private Clip musicClip;
    private FloatControl gain;
    private int volume = 45;     // 0..100
    private boolean muted = false;
    private boolean started = false;

    private SoundManager() { }

    /** Builds and starts looping the ambient track. Safe to call more than once. */
    public synchronized void startMusic() {
        if (started) return;
        started = true;
        try {
            byte[] pcm = synthesizeAmbientLoop();
            AudioFormat format = new AudioFormat(SAMPLE_RATE, 16, 1, true, false);
            musicClip = AudioSystem.getClip();
            musicClip.open(format, pcm, 0, pcm.length);
            if (musicClip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                gain = (FloatControl) musicClip.getControl(FloatControl.Type.MASTER_GAIN);
            }
            applyVolume();
            musicClip.loop(Clip.LOOP_CONTINUOUSLY);
        } catch (Exception e) {
            // No audio device / unsupported format — run silently.
            musicClip = null;
            gain = null;
        }
    }

    public synchronized void setVolume(int v) {
        volume = Math.max(0, Math.min(100, v));
        applyVolume();
    }

    public synchronized int getVolume() { return volume; }

    public synchronized void setMuted(boolean m) {
        muted = m;
        applyVolume();
    }

    public synchronized boolean isMuted() { return muted; }

    private void applyVolume() {
        if (gain == null) return;
        float linear = (muted ? 0 : volume) / 100f;
        float dB;
        if (linear <= 0.0001f) {
            dB = gain.getMinimum();
        } else {
            dB = (float) (20.0 * Math.log10(linear));
            dB = Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), dB));
        }
        gain.setValue(dB);
    }

    /**
     * Generates a few seconds of a soft, slowly pulsing chord (A major pad) so the loop has
     * gentle movement without being distracting.
     */
    private byte[] synthesizeAmbientLoop() {
        // A calm I–vi–IV–V progression in C major, played as a soft music-box arpeggio over a
        // quiet sustained pad. Each note has a gentle bell envelope, so it sounds smooth and warm.
        double chordDur = 4.0;
        double[][] chords = {
                {261.63, 329.63, 392.00}, // C  major  (C4 E4 G4)
                {220.00, 261.63, 329.63}, // A  minor  (A3 C4 E4)
                {174.61, 220.00, 261.63}, // F  major  (F3 A3 C4)
                {196.00, 246.94, 293.66}, // G  major  (G3 B3 D4)
        };

        int n = (int) (SAMPLE_RATE * chordDur * chords.length);
        double[] mix = new double[n];

        for (int c = 0; c < chords.length; c++) {
            int chordStart = (int) (c * chordDur * SAMPLE_RATE);
            double[] chord = chords[c];

            // Soft sustained pad: the whole chord, very quiet, with a slow swell.
            for (double f : chord) {
                addPad(mix, chordStart, (int) (chordDur * SAMPLE_RATE), f, 0.05);
            }
            // Gentle bass note an octave below the root.
            addPad(mix, chordStart, (int) (chordDur * SAMPLE_RATE), chord[0] / 2.0, 0.06);

            // Music-box arpeggio: step through the chord tones (up and back) as soft bells.
            int[] pattern = {0, 1, 2, 1, 0, 2, 1, 2};
            double step = chordDur / pattern.length;
            for (int s = 0; s < pattern.length; s++) {
                int noteStart = chordStart + (int) (s * step * SAMPLE_RATE);
                double freq = chord[pattern[s]] * 2.0; // one octave up for a bright, gentle voice
                addBell(mix, noteStart, (int) (step * 1.6 * SAMPLE_RATE), freq, 0.14);
            }
        }

        // Normalise to a comfortable level and fade the loop edges to avoid any click.
        double max = 1e-9;
        for (double v : mix) max = Math.max(max, Math.abs(v));
        double scale = 0.55 / max;
        int fade = (int) (SAMPLE_RATE * 0.04);

        byte[] data = new byte[n * 2];
        for (int i = 0; i < n; i++) {
            double edge = Math.min(1.0, Math.min(i, n - 1 - i) / (double) fade);
            int v = (int) (mix[i] * scale * edge * Short.MAX_VALUE);
            v = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, v));
            data[i * 2] = (byte) (v & 0xff);
            data[i * 2 + 1] = (byte) ((v >> 8) & 0xff);
        }
        return data;
    }

    /** Adds a soft sustained tone (pad) with a slow swell in/out — warm background texture. */
    private void addPad(double[] mix, int start, int len, double freq, double amp) {
        for (int i = 0; i < len && start + i < mix.length; i++) {
            double t = i / SAMPLE_RATE;
            double env = Math.sin(Math.PI * i / len);          // smooth swell across the note
            double tremolo = 0.85 + 0.15 * Math.sin(2 * Math.PI * 0.4 * t);
            double s = Math.sin(2 * Math.PI * freq * t) + 0.25 * Math.sin(2 * Math.PI * 2 * freq * t);
            mix[start + i] += amp * env * tremolo * s;
        }
    }

    /** Adds a bell/music-box note: quick attack, smooth exponential decay, soft harmonics. */
    private void addBell(double[] mix, int start, int len, double freq, double amp) {
        double attack = SAMPLE_RATE * 0.008;
        double tau = len / 3.2;
        for (int i = 0; i < len && start + i < mix.length; i++) {
            double t = i / SAMPLE_RATE;
            double env = (i < attack) ? (i / attack) : Math.exp(-(i - attack) / tau);
            double s = Math.sin(2 * Math.PI * freq * t)
                     + 0.35 * Math.sin(2 * Math.PI * 2 * freq * t)
                     + 0.12 * Math.sin(2 * Math.PI * 3 * freq * t);
            mix[start + i] += amp * env * s;
        }
    }
}
