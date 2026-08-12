package info.u_team.music_player.lavaplayer.queue;

import java.util.ArrayDeque;
import java.util.Deque;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import info.u_team.music_player.lavaplayer.MusicPlayer;
import info.u_team.music_player.lavaplayer.api.audio.IPlayingTrack;
import info.u_team.music_player.lavaplayer.api.audio.IAudioTrack;
import info.u_team.music_player.lavaplayer.api.queue.ITrackManager;
import info.u_team.music_player.lavaplayer.api.queue.ITrackQueue;
import info.u_team.music_player.lavaplayer.impl.PlayingTrackImpl;
import info.u_team.music_player.lavaplayer.impl.AudioTrackImpl;

public class TrackManager extends AudioEventAdapter implements ITrackManager {
	
	private final MusicPlayer musicPlayer;
	
	private final AudioPlayer audioPlayer;
	private final Object queueLock = new Object();
	
	private TrackQueueWrapper queueWrapper;
	private final Deque<IAudioTrack> transientQueue = new ArrayDeque<>();
	private boolean stopAfterCurrent;
	private boolean stopAfterQueue;
	
	public TrackManager(MusicPlayer musicPlayer, AudioPlayer audioPlayer) {
		this.musicPlayer = musicPlayer;
		this.audioPlayer = audioPlayer;
		audioPlayer.addListener(this);
	}
	
	@Override
	public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
		if (endReason == AudioTrackEndReason.FINISHED || endReason == AudioTrackEndReason.LOAD_FAILED) {
			 synchronized (queueLock) {
				// A manual skip may already have replaced this track before Lavaplayer delivers the end event.
				// In that case advancing again would visibly skip two entries.
				if (audioPlayer.getPlayingTrack() == null || audioPlayer.getPlayingTrack() == track) {
					if (stopAfterCurrent) {
						stopAfterCurrent = false;
						stopAfterQueue = false;
						stopLocked();
					} else {
						skipLocked();
					}
				}
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
			stopLocked();
		}
	}
	
	@Override
	public void setTrackQueue(ITrackQueue queue) {
		synchronized (queueLock) {
			queueWrapper = queue == null ? null : new TrackQueueWrapper(queue);
		}
	}
	
	@Override
	public void skip() {
		synchronized (queueLock) {
			if (stopAfterCurrent) {
				stopAfterCurrent = false;
				stopAfterQueue = false;
				stopLocked();
			} else {
				skipLocked();
			}
		}
	}

	@Override
	public void prepare(IAudioTrack track) {
		if (track == null) return;
		synchronized (queueLock) {
			queueWrapper = null;
			audioPlayer.setPaused(true);
			audioPlayer.startTrack(cloneTrack(track), false);
			audioPlayer.setPaused(true);
		}
	}

	@Override
	public void playNext(IAudioTrack track) {
		if (track == null) return;
		synchronized (queueLock) {
			transientQueue.addFirst(track);
		}
	}

	@Override
	public void queue(IAudioTrack track) {
		if (track == null) return;
		synchronized (queueLock) {
			transientQueue.addLast(track);
		}
	}

	@Override
	public void clearTransientQueue() {
		synchronized (queueLock) {
			transientQueue.clear();
		}
	}

	@Override
	public int getTransientQueueSize() {
		synchronized (queueLock) {
			return transientQueue.size();
		}
	}

	@Override
	public void setStopAfterCurrent(boolean value) {
		synchronized (queueLock) {
			stopAfterCurrent = value;
			if (value) stopAfterQueue = false;
		}
	}

	@Override
	public boolean isStopAfterCurrent() {
		synchronized (queueLock) { return stopAfterCurrent; }
	}

	@Override
	public void setStopAfterQueue(boolean value) {
		synchronized (queueLock) {
			stopAfterQueue = value;
			if (value) stopAfterCurrent = false;
		}
	}

	@Override
	public boolean isStopAfterQueue() {
		synchronized (queueLock) { return stopAfterQueue; }
	}

	private void skipLocked() {
		final IAudioTrack queued = transientQueue.pollFirst();
		if (queued != null) {
			audioPlayer.startTrack(cloneTrack(queued), false);
			return;
		}
		if (queueWrapper == null) {
			stopAfterQueue = false;
			stopLocked();
			return;
		}
		if (queueWrapper.calculateNext(stopAfterQueue) && queueWrapper.getNext() != null) {
			audioPlayer.startTrack(queueWrapper.getNext(), false);
		} else {
			stopAfterQueue = false;
			stopLocked();
		}
	}

	private AudioTrack cloneTrack(IAudioTrack track) {
		final AudioTrack clone = ((AudioTrackImpl) track).getImplTrack().makeClone();
		clone.setUserData(track);
		return clone;
	}

	private void stopLocked() {
		stopAfterCurrent = false;
		stopAfterQueue = false;
		setPaused(false);
		audioPlayer.stopTrack();
		queueWrapper = null;
	}
	
	@Override
	public void setPaused(boolean value) {
		audioPlayer.setPaused(value);
	}
	
	@Override
	public boolean isPaused() {
		return audioPlayer.isPaused();
	}
	
	@Override
	public IPlayingTrack getCurrentTrack() {
		return audioPlayer.getPlayingTrack() == null ? null : new PlayingTrackImpl(musicPlayer, audioPlayer.getPlayingTrack());
	}
}
