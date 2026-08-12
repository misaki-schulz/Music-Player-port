package info.u_team.music_player.lavaplayer.api.queue;

import info.u_team.music_player.lavaplayer.api.audio.IAudioTrack;
import info.u_team.music_player.lavaplayer.api.audio.IPlayingTrack;

public interface ITrackManager {
	
	void start();
	
	void stop();
	
	void setTrackQueue(ITrackQueue queue);
	
	void skip();

	/** Loads a track in a paused state without producing audible output. */
	void prepare(IAudioTrack track);

	/** Places a transient track before the saved playlist's next calculated entry. */
	void playNext(IAudioTrack track);

	void queue(IAudioTrack track);

	void clearTransientQueue();

	int getTransientQueueSize();

	void setStopAfterCurrent(boolean value);

	boolean isStopAfterCurrent();

	void setStopAfterQueue(boolean value);

	boolean isStopAfterQueue();
	
	void setPaused(boolean value);
	
	boolean isPaused();
	
	IPlayingTrack getCurrentTrack();
}
