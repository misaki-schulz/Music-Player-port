package info.u_team.music_player.lavaplayer.api.audio;

public interface IPlayingTrack extends IAudioTrack {
	
	IAudioTrack getOriginalTrack();

	/** Monotonically changing identifier for the concrete playback instance. */
	long getPlaybackId();
	
}
