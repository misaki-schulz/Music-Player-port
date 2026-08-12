package info.u_team.music_player.lavaplayer.api.queue;

import info.u_team.music_player.lavaplayer.api.audio.IAudioTrack;

public interface ITrackQueue {
	
	/**
	 * This calculates the next {@link IAudioTrack} for this queue. This method has a side effect.
	 *
	 * @return true if a next track could be calculated false otherwise
	 */
	boolean calculateNext();

	/** Calculates a next track without wrapping/repeating when a finite queue boundary is requested. */
	default boolean calculateNext(boolean forceFinite) {
		return calculateNext();
	}
	
	/**
	 * Get the calculated {@link IAudioTrack}. This method is a getter and can only return an {@link IAudioTrack} if
	 * {@link ITrackQueue#calculateNext()} has been called before with <strong>true<strong> as result.
	 *
	 * @return The calculated {@link IAudioTrack}
	 */
	IAudioTrack getNext();
	
}
