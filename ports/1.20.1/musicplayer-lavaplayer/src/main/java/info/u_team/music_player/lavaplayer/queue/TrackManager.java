package info.u_team.music_player.lavaplayer.queue;

import java.util.Objects;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import info.u_team.music_player.lavaplayer.MusicPlayer;
import info.u_team.music_player.lavaplayer.api.audio.IPlayingTrack;
import info.u_team.music_player.lavaplayer.api.queue.ITrackManager;
import info.u_team.music_player.lavaplayer.api.queue.ITrackQueue;
import info.u_team.music_player.lavaplayer.impl.PlayingTrackImpl;

public class TrackManager extends AudioEventAdapter implements ITrackManager {
	private static final long PREMATURE_END_TOLERANCE = 10_000;
	
	private final MusicPlayer musicPlayer;
	
	private final AudioPlayer audioPlayer;
	private final Object queueLock = new Object();
	
	private TrackQueueWrapper queueWrapper;
	private String retriedTrackIdentifier;
	private boolean retriedPrematureEnd;
	
	public TrackManager(MusicPlayer musicPlayer, AudioPlayer audioPlayer) {
		this.musicPlayer = musicPlayer;
		this.audioPlayer = audioPlayer;
		audioPlayer.addListener(this);
	}
	
	@Override
	public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
		if (endReason == AudioTrackEndReason.FINISHED || endReason == AudioTrackEndReason.LOAD_FAILED) {
			synchronized (queueLock) {
				final AudioTrack playingTrack = audioPlayer.getPlayingTrack();
				if (playingTrack != null && playingTrack != track) {
					return;
				}
				if (isPrematureEnd(track) && retryPrematureEnd(track)) {
					return;
				}
				clearRetryState();
				skipLocked();
			}
		}
	}
	
	@Override
	public void start() {
		synchronized (queueLock) {
			setPaused(false);
			skipLocked();
		}
	}
	
	@Override
	public void stop() {
		synchronized (queueLock) {
			setPaused(false);
			audioPlayer.stopTrack();
			queueWrapper = null;
			clearRetryState();
		}
	}
	
	@Override
	public void setTrackQueue(ITrackQueue queue) {
		synchronized (queueLock) {
			queueWrapper = queue == null ? null : new TrackQueueWrapper(queue);
			clearRetryState();
		}
	}
	
	@Override
	public void skip() {
		synchronized (queueLock) {
			clearRetryState();
			skipLocked();
		}
	}

	private void skipLocked() {
		if (queueWrapper == null) {
			setPaused(false);
			audioPlayer.stopTrack();
			queueWrapper = null;
			return;
		}
		if (queueWrapper.calculateNext() && queueWrapper.getNext() != null) {
			audioPlayer.startTrack(queueWrapper.getNext(), false);
		} else {
			setPaused(false);
			audioPlayer.stopTrack();
			queueWrapper = null;
		}
	}

	private boolean isPrematureEnd(AudioTrack track) {
		final long duration = track.getDuration();
		final long position = track.getPosition();
		return duration > 0 && duration < Long.MAX_VALUE
				&& position + PREMATURE_END_TOLERANCE < duration;
	}

	private boolean retryPrematureEnd(AudioTrack track) {
		final String identifier = track.getIdentifier();
		if (!Objects.equals(identifier, retriedTrackIdentifier)) {
			retriedTrackIdentifier = identifier;
			retriedPrematureEnd = false;
		}
		if (retriedPrematureEnd) {
			return false;
		}
		retriedPrematureEnd = true;
		final AudioTrack retry = track.makeClone();
		if (retry.isSeekable()) {
			retry.setPosition(track.getPosition());
		}
		audioPlayer.startTrack(retry, false);
		return true;
	}

	private void clearRetryState() {
		retriedTrackIdentifier = null;
		retriedPrematureEnd = false;
	}
	
	@Override
	public void setPaused(boolean value) {
		synchronized (queueLock) {
			audioPlayer.setPaused(value);
		}
	}
	
	@Override
	public boolean isPaused() {
		return audioPlayer.isPaused();
	}
	
	@Override
	public IPlayingTrack getCurrentTrack() {
		synchronized (queueLock) {
			return audioPlayer.getPlayingTrack() == null ? null : new PlayingTrackImpl(musicPlayer, audioPlayer.getPlayingTrack());
		}
	}
}
