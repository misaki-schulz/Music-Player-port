package info.u_team.music_player.lavaplayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.sound.sampled.DataLine.Info;

import com.github.natanbc.lavadsp.timescale.TimescalePcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.format.Pcm16AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.filter.AudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter;
import com.sedmelluq.discord.lavaplayer.filter.equalizer.Equalizer;
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration.ResamplingQuality;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.AllocatingAudioFrameBuffer;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;

import info.u_team.music_player.lavaplayer.api.IMusicPlayer;
import info.u_team.music_player.lavaplayer.api.IYoutubeAuth;
import info.u_team.music_player.lavaplayer.api.output.IOutputConsumer;
import info.u_team.music_player.lavaplayer.api.queue.ITrackManager;
import info.u_team.music_player.lavaplayer.api.search.ITrackSearch;
import info.u_team.music_player.lavaplayer.output.AudioOutput;
import info.u_team.music_player.lavaplayer.queue.TrackManager;
import info.u_team.music_player.lavaplayer.search.TrackSearch;
import info.u_team.music_player.lavaplayer.sources.AudioSources;
import info.u_team.music_player.lavaplayer.sources.YoutubeAuth;
import info.u_team.music_player.lavaplayer.util.ObservableValue;

public class MusicPlayer implements IMusicPlayer {

	private static final int[] CONTROL_FREQUENCIES = { 32, 64, 125, 250, 500, 1000, 2000, 4000, 8000, 16000 };
	private static final int[] FILTER_FREQUENCIES = { 25, 40, 63, 100, 160, 250, 400, 630, 1000, 1600, 2500, 4000, 6300, 10000, 16000 };
	
	private final AudioPlayerManager audioPlayerManager;
	private final AudioDataFormat audioDataFormat;
	private final AudioPlayer audioPlayer;
	private final AudioOutput audioOutput;
	
	private final TrackSearch trackSearch;
	private final TrackManager trackManager;
	private final YoutubeAuth youtubeAuth;
	
	private IOutputConsumer outputConsumer;
	
	private final ObservableValue<Float> speed;
	private final ObservableValue<Float> pitch;
	private volatile float[] equalizerGains = new float[EQUALIZER_BAND_COUNT];
	private final float[] activeFilterGains = new float[Equalizer.BAND_COUNT];
	private volatile boolean equalizerEnabled;
	private volatile boolean bassBoostEnabled;
	
	private long currentTrackPosition;
	
	public MusicPlayer() {
		audioPlayerManager = new DefaultAudioPlayerManager();
		audioDataFormat = new Pcm16AudioDataFormat(2, 48000, 960, true);
		audioPlayer = audioPlayerManager.createPlayer();
		audioOutput = new AudioOutput(this);
		
		trackSearch = new TrackSearch(audioPlayerManager);
		trackManager = new TrackManager(this, audioPlayer);
		youtubeAuth = new YoutubeAuth(audioPlayerManager);
		
		speed = new ObservableValue<>(1F);
		pitch = new ObservableValue<>(1F);
		
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
				currentTrackPosition = track.getPosition();
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
					currentTrackPosition += frame.getFormat().frameDuration() * speed.getValue();
				}
				return frame;
			}
		});
		
		speed.registerListener(speed -> updateFilters(speed, pitch.getValue()));
		pitch.registerListener(pitch -> updateFilters(speed.getValue(), pitch));
	}
	
	private void updateFilters(float speed, float pitch) {
		updateFilterGains();
		final boolean useTimescale = Math.abs(speed - 1) >= 0.01 || Math.abs(pitch - 1) >= 0.01;
		final boolean useEqualizer = equalizerEnabled || bassBoostEnabled;
		if (!useTimescale && !useEqualizer) {
			audioPlayer.setFilterFactory((track, format, output) -> Collections.emptyList());
		} else {
			audioPlayer.setFilterFactory((track, format, output) -> {
				final List<AudioFilter> filters = new ArrayList<>(2);
				FloatPcmAudioFilter next = output;
				if (useEqualizer && Equalizer.isCompatible(format)) {
					final Equalizer equalizer = new Equalizer(format.channelCount, next, activeFilterGains);
					filters.add(equalizer);
					next = equalizer;
				}
				if (useTimescale) {
					final TimescalePcmAudioFilter timescale = new TimescalePcmAudioFilter(next, format.channelCount, format.sampleRate);
					timescale.setSpeed(speed);
					timescale.setPitch(pitch);
					filters.add(0, timescale);
				}
				return filters;
			});
		}
	}

	private void updateFilterGains() {
		final float[] calculated = filterGains(equalizerEnabled ? equalizerGains : new float[EQUALIZER_BAND_COUNT], bassBoostEnabled);
		System.arraycopy(calculated, 0, activeFilterGains, 0, calculated.length);
	}

	private static float[] filterGains(float[] controls, boolean bassBoost) {
		final float[] adjusted = controls.clone();
		if (bassBoost) {
			adjusted[0] = Math.min(1F, adjusted[0] + 0.35F);
			adjusted[1] = Math.min(1F, adjusted[1] + 0.30F);
			adjusted[2] = Math.min(1F, adjusted[2] + 0.20F);
			adjusted[3] = Math.min(1F, adjusted[3] + 0.07F);
		}
		final float[] result = new float[Equalizer.BAND_COUNT];
		for (int band = 0; band < result.length; band++) {
			final int frequency = FILTER_FREQUENCIES[band];
			int left = 0;
			while (left < CONTROL_FREQUENCIES.length - 2 && frequency > CONTROL_FREQUENCIES[left + 1]) {
				left++;
			}
			final int right = left + 1;
			final float fraction = Math.clamp((float) (Math.log((double) frequency / CONTROL_FREQUENCIES[left])
					/ Math.log((double) CONTROL_FREQUENCIES[right] / CONTROL_FREQUENCIES[left])), 0F, 1F);
			final float control = adjusted[left] + (adjusted[right] - adjusted[left]) * fraction;
			result[band] = control < 0 ? control * 0.25F : control * 0.7F;
		}
		return result;
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
		return currentTrackPosition;
	}
	
	public void setCurrentTrackPosition(long currentTrackPosition) {
		this.currentTrackPosition = currentTrackPosition;
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
	public IYoutubeAuth getYoutubeAuth() {
		return youtubeAuth;
	}
	
	@Override
	public void startAudioOutput() {
		audioOutput.start();
	}

	@Override
	public void shutdown() {
		audioOutput.shutdown();
		audioPlayer.destroy();
		audioPlayerManager.shutdown();
	}
	
	@Override
	public void setMixer(String name) {
		audioOutput.setMixer(name);
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
	public void setEqualizerGains(float[] gains) {
		if (gains == null || gains.length != EQUALIZER_BAND_COUNT) {
			throw new IllegalArgumentException("Expected " + EQUALIZER_BAND_COUNT + " equalizer bands");
		}
		final float[] normalized = gains.clone();
		for (int band = 0; band < normalized.length; band++) {
			normalized[band] = Float.isFinite(normalized[band]) ? Math.clamp(normalized[band], -1F, 1F) : 0F;
		}
		equalizerGains = normalized;
		updateFilterGains();
	}

	@Override
	public void setEqualizerEnabled(boolean enabled) {
		equalizerEnabled = enabled;
		updateFilters(speed.getValue(), pitch.getValue());
	}

	@Override
	public void setBassBoostEnabled(boolean enabled) {
		bassBoostEnabled = enabled;
		updateFilters(speed.getValue(), pitch.getValue());
	}
	
	@Override
	public void setOutputConsumer(IOutputConsumer consumer) {
		outputConsumer = consumer;
	}
}
