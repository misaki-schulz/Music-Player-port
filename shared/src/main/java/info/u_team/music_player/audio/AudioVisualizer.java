package info.u_team.music_player.audio;

import info.u_team.music_player.lavaplayer.api.output.IOutputConsumer;

/** Small bounded PCM analyzer. The audio callback reuses fixed arrays and never allocates per chunk. */
public final class AudioVisualizer implements IOutputConsumer {

	private static final int SPECTRUM_BANDS = 24;
	private static final int WAVEFORM_POINTS = 64;
	private final float[] samples = new float[512];
	private final float[] spectrum = new float[SPECTRUM_BANDS];
	private final float[] waveform = new float[WAVEFORM_POINTS];

	@Override
	public synchronized void accept(byte[] buffer, int chunkSize) {
		if (chunkSize < 8) return;
		final int frameCount = Math.min(chunkSize / 4, 512);
		for (int frame = 0; frame < frameCount; frame++) {
			final int index = frame * 4;
			final short left = (short) ((buffer[index] << 8) | (buffer[index + 1] & 0xFF));
			final short right = (short) ((buffer[index + 2] << 8) | (buffer[index + 3] & 0xFF));
			samples[frame] = (left + right) / 65536F;
		}

		for (int point = 0; point < WAVEFORM_POINTS; point++) waveform[point] = samples[point * (frameCount - 1) / (WAVEFORM_POINTS - 1)];

		for (int band = 0; band < SPECTRUM_BANDS; band++) {
			final int bin = 1 + band * 3;
			double real = 0D, imaginary = 0D;
			for (int sample = 0; sample < frameCount; sample += 2) {
				final double angle = 2D * Math.PI * bin * sample / frameCount;
				real += samples[sample] * Math.cos(angle);
				imaginary -= samples[sample] * Math.sin(angle);
			}
			final float magnitude = (float) Math.min(1D, Math.hypot(real, imaginary) / Math.max(1D, frameCount / 12D));
			spectrum[band] = spectrum[band] * 0.62F + magnitude * 0.38F;
		}
	}

	public synchronized float[] spectrum() { return spectrum.clone(); }
	public synchronized float[] waveform() { return waveform.clone(); }
}
