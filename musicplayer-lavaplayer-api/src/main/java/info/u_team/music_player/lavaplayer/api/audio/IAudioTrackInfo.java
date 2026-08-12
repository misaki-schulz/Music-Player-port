package info.u_team.music_player.lavaplayer.api.audio;

public interface IAudioTrackInfo {
	
	String getTitle();
	
	String getAuthor();
	
	String getIdentifier();
	
	String getURI();

	String getArtworkURL();
	
	boolean isStream();
	
	String getFixedTitle();
	
	String getFixedAuthor();
	
}
