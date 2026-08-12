// Modified for Minecraft 26.2 by misaki-schulz; see NOTICE.
/**
 * Thanks to VSETH-GECO for the original audio consumer implementation. See the repository NOTICE and dependency
 * licenses for attribution.
 */
package info.u_team.music_player.lavaplayer.output;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormatTools;
import com.sedmelluq.discord.lavaplayer.format.AudioPlayerInputStream;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;

import info.u_team.music_player.lavaplayer.MusicPlayer;

/**
 * Restartable Java Sound output with explicit lifecycle handling and a small watchdog for devices which become stale
 * while Minecraft is minimized, suspended, or an output device is disconnected.
 */
public class AudioOutput {

	private static final Logger LOGGER = LoggerFactory.getLogger(AudioOutput.class);
	private static final long STALL_TIMEOUT_NANOS = TimeUnit.SECONDS.toNanos(10);

	private final MusicPlayer musicPlayer;
	private final AudioFormat format;
	private final DataLine.Info speakerInfo;
	private final Object lineLock = new Object();
	private final AtomicBoolean running = new AtomicBoolean();
	private final AtomicBoolean closed = new AtomicBoolean();
	private final AtomicBoolean lineResetRequested = new AtomicBoolean(true);
	private final AtomicBoolean watchdogStarted = new AtomicBoolean();
	private final AtomicLong lastProgressNanos = new AtomicLong(System.nanoTime());
	private final ScheduledExecutorService watchdog;

	private volatile Thread worker;
	private volatile Mixer mixer;
	private volatile String requestedMixerName = "";
	private volatile SourceDataLine sourceLine;
	private volatile boolean automaticRecovery = true;
	private volatile boolean mono;
	private volatile boolean swapChannels;
	private volatile float balance;
	private volatile float duckingGain = 1F;
	private volatile float transitionGain = 1F;

	public AudioOutput(MusicPlayer musicPlayer) {
		this.musicPlayer = musicPlayer;
		format = AudioDataFormatTools.toAudioFormat(musicPlayer.getAudioDataFormat());
		speakerInfo = new DataLine.Info(SourceDataLine.class, format);
		watchdog = Executors.newSingleThreadScheduledExecutor(runnable -> {
			final Thread thread = new Thread(runnable, "Music Player audio watchdog");
			thread.setDaemon(true);
			return thread;
		});
		setMixer("");
	}

	public void start() {
		if (closed.get() || !running.compareAndSet(false, true)) {
			return;
		}
		lastProgressNanos.set(System.nanoTime());
		worker = new Thread(this::runOutput, "Music Player audio output");
		worker.setDaemon(true);
		worker.start();
		if (watchdogStarted.compareAndSet(false, true)) {
			watchdog.scheduleWithFixedDelay(this::checkForStalledOutput, 2, 2, TimeUnit.SECONDS);
		}
	}

	private void runOutput() {
		final AudioPlayer player = musicPlayer.getAudioPlayer();
		final AudioDataFormat dataFormat = musicPlayer.getAudioDataFormat();
		final AudioInputStream stream = AudioPlayerInputStream.createStream(player, dataFormat, dataFormat.frameDuration(), false);
		final byte[] buffer = new byte[dataFormat.chunkSampleCount * dataFormat.channelCount * 2];
		final long frameDuration = dataFormat.frameDuration();

		try {
			while (running.get() && !Thread.currentThread().isInterrupted()) {
				try {
					if (lineResetRequested.getAndSet(false)) {
						closeLine();
					}
					if (sourceLine == null || !sourceLine.isOpen()) {
						closeLine();
						if (!createLine()) {
							Thread.sleep(500);
							continue;
						}
					}
					if (!player.isPaused()) {
						final int chunkSize = stream.read(buffer);
						if (chunkSize < 0) {
							throw new IllegalStateException("Audio stream ended unexpectedly");
						}
						applyChannelMix(buffer, chunkSize);
						final SourceDataLine line = sourceLine;
						if (line != null) {
							line.write(buffer, 0, chunkSize);
							lastProgressNanos.set(System.nanoTime());
						}
						if (musicPlayer.getOutputConsumer() != null) {
							musicPlayer.getOutputConsumer().accept(buffer, chunkSize);
						}
					} else {
						lastProgressNanos.set(System.nanoTime());
						Thread.sleep(frameDuration);
					}
				} catch (final InterruptedException exception) {
					Thread.currentThread().interrupt();
				} catch (final Exception exception) {
					if (running.get() && !closed.get()) {
						LOGGER.warn("Audio output failed; reopening the selected device", exception);
						closeLine();
						Thread.sleep(500);
					}
				}
			}
		} catch (final InterruptedException exception) {
			Thread.currentThread().interrupt();
		} finally {
			running.set(false);
			closeLine();
		}
	}

	private void checkForStalledOutput() {
		if (closed.get() || !automaticRecovery) {
			return;
		}
		final AudioPlayer player = musicPlayer.getAudioPlayer();
		if (!running.get()) {
			LOGGER.warn("Audio output worker stopped unexpectedly; starting a replacement");
			start();
			return;
		}
		if (player.isPaused() || player.getPlayingTrack() == null) {
			lastProgressNanos.set(System.nanoTime());
			return;
		}
		if (System.nanoTime() - lastProgressNanos.get() > STALL_TIMEOUT_NANOS) {
			LOGGER.warn("Audio output made no progress for 10 seconds; resetting the selected device");
			lineResetRequested.set(true);
			closeLine();
			lastProgressNanos.set(System.nanoTime());
		}
	}

	public void setMixer(String name) {
		requestedMixerName = name == null ? "" : name;
		lineResetRequested.set(true);
	}

	public void setAutomaticRecovery(boolean enabled) {
		automaticRecovery = enabled;
		if (enabled) {
			lastProgressNanos.set(System.nanoTime());
		}
	}

	public void setChannelMix(boolean mono, float balance, boolean swapChannels) {
		this.mono = mono;
		this.balance = Math.clamp(balance, -1F, 1F);
		this.swapChannels = swapChannels;
	}

	public void setDuckingGain(float gain) { duckingGain = Math.clamp(gain, 0F, 1F); }
	public void setTransitionGain(float gain) { transitionGain = Math.clamp(gain, 0F, 1F); }

	private void applyChannelMix(byte[] buffer, int chunkSize) {
		final boolean monoNow = mono;
		final boolean swapNow = swapChannels;
		final float balanceNow = balance;
		final float masterGain = duckingGain * transitionGain;
		if (!monoNow && !swapNow && Math.abs(balanceNow) < 0.001F && Math.abs(masterGain - 1F) < 0.001F) return;
		final float leftGain = (balanceNow > 0F ? 1F - balanceNow : 1F) * masterGain;
		final float rightGain = (balanceNow < 0F ? 1F + balanceNow : 1F) * masterGain;
		for (int index = 0; index + 3 < chunkSize; index += 4) {
			int left = (short) (((buffer[index] & 0xFF) << 8) | (buffer[index + 1] & 0xFF));
			int right = (short) (((buffer[index + 2] & 0xFF) << 8) | (buffer[index + 3] & 0xFF));
			if (monoNow) {
				final int mixed = (left + right) / 2;
				left = mixed;
				right = mixed;
			}
			if (swapNow) {
				final int previousLeft = left;
				left = right;
				right = previousLeft;
			}
			left = Math.round(left * leftGain);
			right = Math.round(right * rightGain);
			buffer[index] = (byte) (left >>> 8);
			buffer[index + 1] = (byte) left;
			buffer[index + 2] = (byte) (right >>> 8);
			buffer[index + 3] = (byte) right;
		}
	}

	public String getMixer() {
		final Mixer selectedMixer = mixer;
		return selectedMixer == null ? null : selectedMixer.getMixerInfo().getName();
	}

	public DataLine.Info getSpeakerInfo() {
		return speakerInfo;
	}

	private boolean createLine() {
		final Mixer selectedMixer = findMixer(requestedMixerName, speakerInfo);
		mixer = selectedMixer;
		if (selectedMixer == null || closed.get()) {
			return false;
		}
		try {
			final SourceDataLine line = (SourceDataLine) selectedMixer.getLine(speakerInfo);
			final AudioDataFormat dataFormat = musicPlayer.getAudioDataFormat();
			line.open(format, dataFormat.chunkSampleCount * dataFormat.channelCount * 2 * 5);
			line.start();
			synchronized (lineLock) {
				if (closed.get()) {
					line.close();
					return false;
				}
				sourceLine = line;
			}
			lastProgressNanos.set(System.nanoTime());
			LOGGER.info("Audio output device is now {}", selectedMixer.getMixerInfo().getName());
			return true;
		} catch (final LineUnavailableException | IllegalArgumentException | SecurityException exception) {
			LOGGER.warn("Cannot open audio output device {}", selectedMixer.getMixerInfo().getName(), exception);
			return false;
		}
	}

	private void closeLine() {
		final SourceDataLine line;
		synchronized (lineLock) {
			line = sourceLine;
			sourceLine = null;
		}
		if (line != null) {
			try {
				line.flush();
				line.stop();
				line.close();
			} catch (final RuntimeException exception) {
				LOGGER.debug("Cannot cleanly close previous audio output line", exception);
			}
		}
	}

	private Mixer findMixer(String name, Line.Info lineInfo) {
		Mixer defaultMixer = null;
		for (final Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
			final Mixer candidate = AudioSystem.getMixer(mixerInfo);
			if (candidate.isLineSupported(lineInfo)) {
				if (mixerInfo.getName().equals(name)) {
					return candidate;
				}
				if (defaultMixer == null) {
					defaultMixer = candidate;
				}
			}
		}
		return defaultMixer;
	}

	public void shutdown() {
		if (!closed.compareAndSet(false, true)) {
			return;
		}
		running.set(false);
		watchdog.shutdownNow();
		closeLine();
		final Thread outputWorker = worker;
		if (outputWorker != null) {
			outputWorker.interrupt();
			if (outputWorker != Thread.currentThread()) {
				try {
					outputWorker.join(1500);
				} catch (final InterruptedException exception) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	public static boolean hasLinesOpen(Mixer mixer) {
		return mixer.getSourceLines().length != 0 || mixer.getTargetLines().length != 0;
	}
}
