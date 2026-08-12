package info.u_team.music_player.lavaplayer;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.sound.sampled.DataLine.Info;

import com.github.natanbc.lavadsp.timescale.TimescalePcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.AudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.equalizer.Equalizer;
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.format.Pcm16AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration.ResamplingQuality;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.AllocatingAudioFrameBuffer;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;

import info.u_team.music_player.lavaplayer.api.IMusicPlayer;
import info.u_team.music_player.lavaplayer.api.output.IOutputConsumer;
import info.u_team.music_player.lavaplayer.api.queue.ITrackManager;
import info.u_team.music_player.lavaplayer.api.search.ITrackSearch;
import info.u_team.music_player.lavaplayer.output.AudioOutput;
import info.u_team.music_player.lavaplayer.queue.TrackManager;
import info.u_team.music_player.lavaplayer.search.TrackSearch;
import info.u_team.music_player.lavaplayer.sources.AudioSources;
import info.u_team.music_player.lavaplayer.util.ObservableValue;

public class MusicPlayer implements IMusicPlayer {
	
	private final AudioPlayerManager audioPlayerManager;
	private final AudioDataFormat audioDataFormat;
	private final AudioPlayer audioPlayer;
	private final AudioOutput audioOutput;
	
	private final TrackSearch trackSearch;
	private final TrackManager trackManager;
	
	private volatile IOutputConsumer outputConsumer;
	
	private final ObservableValue<Float> speed;
	private final ObservableValue<Float> pitch;
	private volatile boolean equalizerEnabled;
	private volatile boolean bassBoost;
	private volatile float[] equalizerGains = new float[10];
	private volatile float[] equalizerPositions = defaultEqualizerPositions(10);
	
	private final AtomicLong currentTrackPosition;
	private final AtomicLong playbackSequence;
	private final AtomicBoolean shutdown;
	
	public MusicPlayer() {
		audioPlayerManager = new DefaultAudioPlayerManager();
		audioDataFormat = new Pcm16AudioDataFormat(2, 48000, 960, true);
		audioPlayer = audioPlayerManager.createPlayer();
		audioOutput = new AudioOutput(this);
		
		trackSearch = new TrackSearch(audioPlayerManager);
		trackManager = new TrackManager(this, audioPlayer);
		
		speed = new ObservableValue<>(1F);
		pitch = new ObservableValue<>(1F);
		currentTrackPosition = new AtomicLong();
		playbackSequence = new AtomicLong();
		shutdown = new AtomicBoolean();
		
		setup();
	}
	
	private void setup() {
		audioPlayerManager.setFrameBufferDuration(1000);
		audioPlayerManager.setPlayerCleanupThreshold(Long.MAX_VALUE);
		
		audioPlayerManager.getConfiguration().setResamplingQuality(ResamplingQuality.HIGH);
		audioPlayerManager.getConfiguration().setOpusEncodingQuality(10);
		audioPlayerManager.getConfiguration().setOutputFormat(audioDataFormat);
		
		AudioSources.registerSources(audioPlayerManager);
		
		audioPlayerManager.getConfiguration().setFilterHotSwapEnabled(true);
		
		audioPlayer.addListener(new AudioEventAdapter() {
			
			@Override
			public void onTrackStart(AudioPlayer player, AudioTrack track) {
				currentTrackPosition.set(track.getPosition());
				playbackSequence.incrementAndGet();
			}
		});
		
		audioPlayerManager.getConfiguration().setFrameBufferFactory((bufferDuration, format, stopping) -> new AllocatingAudioFrameBuffer(bufferDuration, format, stopping) {
			
			@Override
			public AudioFrame provide() {
				return updateTrackPosition(super.provide());
			}
			
			@Override
			public AudioFrame provide(long timeout, TimeUnit unit) throws TimeoutException, InterruptedException {
				return updateTrackPosition(super.provide(timeout, unit));
			}
			
			private AudioFrame updateTrackPosition(AudioFrame frame) {
				if (frame != null && !frame.isTerminator()) {
					currentTrackPosition.addAndGet((long) (frame.getFormat().frameDuration() * speed.getValue()));
				}
				return frame;
			}
		});
		
		speed.registerListener(speed -> updateFilters(speed, pitch.getValue()));
		pitch.registerListener(pitch -> updateFilters(speed.getValue(), pitch));
	}
	
	private void updateFilters(float speed, float pitch) {
		final boolean useTimescale = Math.abs(speed - 1) >= 0.01 || Math.abs(pitch - 1) >= 0.01;
		final boolean useEqualizer = equalizerEnabled || bassBoost;
		if (!useTimescale && !useEqualizer) {
			audioPlayer.setFilterFactory((track, format, output) -> Collections.emptyList());
		} else {
			audioPlayer.setFilterFactory((track, format, output) -> {
				final List<AudioFilter> filters = new ArrayList<>();
				var downstream = (com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter) output;
				if (useEqualizer) {
					final float[] gains = equalizerGains.clone();
					final float[] positions = equalizerPositions.clone();
					final Equalizer equalizer = new Equalizer(format.channelCount, downstream);
					for (int band = 0; band < Equalizer.BAND_COUNT; band++) {
						final float point = band / (Equalizer.BAND_COUNT - 1F);
						final float db = interpolateGain(gains, positions, point) + (bassBoost ? Math.max(0F, 9F - band * 1.8F) : 0F);
						final float lavaplayerGain = db < 0F ? db / 96F : db / 24F;
						equalizer.setGain(band, Math.clamp(lavaplayerGain, -0.25F, 1F));
					}
					filters.add(equalizer);
					downstream = equalizer;
				}
				if (useTimescale) {
					final TimescalePcmAudioFilter timescale = new TimescalePcmAudioFilter(downstream, format.channelCount, format.sampleRate);
					timescale.setSpeed(speed);
					timescale.setPitch(pitch);
					filters.add(timescale);
				}
				Collections.reverse(filters);
				return filters;
			});
		}
	}
	
	public AudioPlayerManager getAudioPlayerManager() {
		return audioPlayerManager;
	}
	
	public AudioDataFormat getAudioDataFormat() {
		return audioDataFormat;
	}
	
	public AudioPlayer getAudioPlayer() {
		return audioPlayer;
	}
	
	public IOutputConsumer getOutputConsumer() {
		return outputConsumer;
	}
	
	public long getCurrentTrackPosition() {
		return currentTrackPosition.get();
	}
	
	public void setCurrentTrackPosition(long currentTrackPosition) {
		this.currentTrackPosition.set(currentTrackPosition);
	}
	
	@Override
	public ITrackManager getTrackManager() {
		return trackManager;
	}
	
	@Override
	public ITrackSearch getTrackSearch() {
		return trackSearch;
	}
	
	@Override
	public void startAudioOutput() {
		audioOutput.start();
	}
	
	@Override
	public void setMixer(String name) {
		audioOutput.setMixer(name);
	}

	public long getPlaybackSequence() {
		return playbackSequence.get();
	}

	@Override
	public void setAutomaticAudioRecovery(boolean enabled) {
		audioOutput.setAutomaticRecovery(enabled);
	}
	
	@Override
	public String getMixer() {
		return audioOutput.getMixer();
	}
	
	@Override
	public Info getSpeakerInfo() {
		return audioOutput.getSpeakerInfo();
	}
	
	@Override
	public void setVolume(int volume) {
		audioPlayer.setVolume(volume);
	}
	
	@Override
	public int getVolume() {
		return audioPlayer.getVolume();
	}
	
	@Override
	public void setSpeed(float speed) {
		this.speed.setValue(Math.max(0.05F, Math.min(10, speed)));
	}
	
	@Override
	public float getSpeed() {
		return speed.getValue();
	}
	
	@Override
	public void setPitch(float pitch) {
		this.pitch.setValue(Math.max(0.05F, Math.min(10, pitch)));
	}
	
	@Override
	public float getPitch() {
		return pitch.getValue();
	}
	
	@Override
	public void setOutputConsumer(IOutputConsumer consumer) {
		outputConsumer = consumer;
	}

	@Override
	public void setEqualizer(boolean enabled, float[] gains, float[] positions, boolean bassBoost) {
		equalizerEnabled = enabled;
		this.bassBoost = bassBoost;
		equalizerGains = gains == null ? new float[10] : gains.clone();
		equalizerPositions = positions == null || positions.length != equalizerGains.length ? defaultEqualizerPositions(equalizerGains.length) : positions.clone();
		updateFilters(speed.getValue(), pitch.getValue());
	}

	private static float interpolateGain(float[] gains, float[] positions, float point) {
		if (point <= positions[0]) return gains[0];
		for (int upper = 1; upper < positions.length; upper++) {
			if (point <= positions[upper]) {
				final int lower = upper - 1;
				final float span = Math.max(0.0001F, positions[upper] - positions[lower]);
				final float fraction = (point - positions[lower]) / span;
				final float smooth = (1F - (float) Math.cos(fraction * Math.PI)) * 0.5F;
				return gains[lower] + (gains[upper] - gains[lower]) * smooth;
			}
		}
		return gains[gains.length - 1];
	}

	private static float[] defaultEqualizerPositions(int count) {
		final float[] positions = new float[count];
		for (int index = 0; index < count; index++) positions[index] = index / Math.max(1F, count - 1F);
		return positions;
	}

	@Override
	public void setChannelMix(boolean mono, float balance, boolean swapChannels) {
		audioOutput.setChannelMix(mono, balance, swapChannels);
	}

	@Override public void setDuckingGain(float gain) { audioOutput.setDuckingGain(gain); }
	@Override public void setTransitionGain(float gain) { audioOutput.setTransitionGain(gain); }

	@Override
	public void shutdown() {
		if (!shutdown.compareAndSet(false, true)) {
			return;
		}
		trackManager.stop();
		audioOutput.shutdown();
		audioPlayerManager.shutdown();
		outputConsumer = null;
	}
}
