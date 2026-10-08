package info.u_team.music_player.lavaplayer.api;

import javax.sound.sampled.DataLine;

import info.u_team.music_player.lavaplayer.api.output.IOutputConsumer;
import info.u_team.music_player.lavaplayer.api.queue.ITrackManager;
import info.u_team.music_player.lavaplayer.api.search.ITrackSearch;

public interface IMusicPlayer {

	int EQUALIZER_BAND_COUNT = 10;
	
	ITrackManager getTrackManager();
	
	ITrackSearch getTrackSearch();

	IYoutubeAuth getYoutubeAuth();
	
	void startAudioOutput();
	void shutdown();
	
	void setMixer(String name);
	
	String getMixer();
	
	DataLine.Info getSpeakerInfo();
	
	int getVolume();
	
	void setVolume(int volume);
	
	float getSpeed();
	
	void setSpeed(float speed);
	
	float getPitch();
	
	void setPitch(float pitch);

	void setEqualizerGains(float[] gains);

	void setEqualizerEnabled(boolean enabled);

	void setBassBoostEnabled(boolean enabled);
	
	void setOutputConsumer(IOutputConsumer consumer);
}
