// Modified for Minecraft 26.2 by misaki-schulz; see NOTICE.
/**
 * Thanks to VSETH-GECO for this amazing audio consumer class for lavaplayer (It is kind of changed) MIT License
 * Copyright (c) 2017 VSETH-GECO Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the
 * Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions: The
 * above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software. THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package info.u_team.music_player.lavaplayer.output;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

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

public class AudioOutput extends Thread {

	private static final Logger LOGGER = LoggerFactory.getLogger(AudioOutput.class);
	
	private final MusicPlayer musicPlayer;
	
	private final AudioFormat format;
	private final DataLine.Info speakerInfo;
	
	private volatile Mixer mixer;
	private final AtomicBoolean lineResetRequested = new AtomicBoolean(true);
	private SourceDataLine sourceLine;
	
	public AudioOutput(MusicPlayer musicPlayer) {
		super("Audio Player");
		this.musicPlayer = musicPlayer;
		format = AudioDataFormatTools.toAudioFormat(musicPlayer.getAudioDataFormat());
		speakerInfo = new DataLine.Info(SourceDataLine.class, format);
		setMixer("");
	}
	
	@Override
	public void run() {
		final AudioPlayer player = musicPlayer.getAudioPlayer();
		final AudioDataFormat dataformat = musicPlayer.getAudioDataFormat();
		final AudioInputStream stream = AudioPlayerInputStream.createStream(player, dataformat, dataformat.frameDuration(), false);
		final byte[] buffer = new byte[dataformat.chunkSampleCount * dataformat.channelCount * 2];
		final long frameDuration = dataformat.frameDuration();

		try {
			while (!isInterrupted()) {
				try {
					if (lineResetRequested.getAndSet(false)) {
						closeLine();
					}
					if (sourceLine == null || !sourceLine.isOpen()) {
						closeLine();
						if (!createLine()) {
							sleep(500);
							continue;
						}
					}
					if (!player.isPaused()) {
						final int chunkSize = stream.read(buffer);
						if (chunkSize >= 0) {
							sourceLine.write(buffer, 0, chunkSize);
							if (musicPlayer.getOutputConsumer() != null) {
								musicPlayer.getOutputConsumer().accept(Arrays.copyOf(buffer, buffer.length), chunkSize);
							}
						} else {
							throw new IllegalStateException("Audiostream ended. This should not happen.");
						}
					} else {
						sourceLine.drain();
						sleep(frameDuration);
					}
				} catch (final InterruptedException ex) {
					throw ex;
				} catch (final Exception ex) {
					LOGGER.warn("Audio output failed; reopening the selected device", ex);
					closeLine();
					sleep(500);
				}
			}
		} catch (final InterruptedException ex) {
			interrupt();
		} finally {
			closeLine();
		}
	}
	
	public void setMixer(String name) {
		mixer = findMixer(name == null ? "" : name, speakerInfo);
		lineResetRequested.set(true);
	}
	
	public String getMixer() {
		if (mixer == null) {
			return null;
		}
		return mixer.getMixerInfo().getName();
	}
	
	public DataLine.Info getSpeakerInfo() {
		return speakerInfo;
	}
	
	private boolean createLine() {
		final Mixer selectedMixer = mixer;
		if (selectedMixer != null) {
			try {
				final SourceDataLine line = (SourceDataLine) selectedMixer.getLine(speakerInfo);
				final AudioDataFormat dataFormat = musicPlayer.getAudioDataFormat();
				line.open(format, dataFormat.chunkSampleCount * dataFormat.channelCount * 2 * 5);
				line.start();
				sourceLine = line;
				LOGGER.info("Audio output device is now {}", selectedMixer.getMixerInfo().getName());
				return true;
			} catch (final LineUnavailableException | IllegalArgumentException | SecurityException ex) {
				LOGGER.warn("Cannot open audio output device {}", selectedMixer.getMixerInfo().getName(), ex);
			}
		}
		return false;
	}
	
	private void closeLine() {
		final SourceDataLine line = sourceLine;
		sourceLine = null;
		if (line != null) {
			try {
				line.flush();
				line.stop();
				line.close();
			} catch (final RuntimeException ex) {
				LOGGER.debug("Cannot cleanly close previous audio output line", ex);
			}
		}
	}
	
	private Mixer findMixer(String name, Line.Info lineInfo) {
		Mixer defaultMixer = null;
		for (final Mixer.Info mixerInfo : AudioSystem.getMixerInfo()) {
			final Mixer mixer = AudioSystem.getMixer(mixerInfo);
			if (mixer.isLineSupported(lineInfo)) {
				if (mixerInfo.getName().equals(name)) {
					return mixer;
				}
				if (defaultMixer == null) {
					defaultMixer = mixer;
				}
			}
		}
		return defaultMixer;
	}
	
	public static boolean hasLinesOpen(Mixer mixer) {
		return mixer.getSourceLines().length != 0 || mixer.getTargetLines().length != 0;
	}
}
