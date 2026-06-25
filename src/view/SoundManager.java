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
        double durationSec = 8.0;
        int n = (int) (SAMPLE_RATE * durationSec);
        byte[] data = new byte[n * 2];

        double[] freqs = {110.00, 164.81, 220.00, 277.18}; // A2, E3, A3, C#4
        double[] weights = {0.5, 0.32, 0.28, 0.2};

        for (int i = 0; i < n; i++) {
            double t = i / (double) SAMPLE_RATE;
            // Slow tremolo so the pad breathes (one full cycle across the loop = seamless).
            double lfo = 0.6 + 0.4 * Math.sin(2 * Math.PI * (i / (double) n));
            double sample = 0;
            for (int k = 0; k < freqs.length; k++) {
                sample += weights[k] * Math.sin(2 * Math.PI * freqs[k] * t);
            }
            // Gentle fade in/out at the very edges to avoid a loop click.
            double edge = Math.min(1.0, Math.min(i, n - 1 - i) / (SAMPLE_RATE * 0.05));
            sample *= lfo * edge * 0.22;
            int v = (int) (sample * Short.MAX_VALUE);
            v = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, v));
            data[i * 2] = (byte) (v & 0xff);
            data[i * 2 + 1] = (byte) ((v >> 8) & 0xff);
        }
        return data;
    }
}
