package info.u_team.music_player.lavaplayer.api;

import javax.sound.sampled.DataLine;

import info.u_team.music_player.lavaplayer.api.output.IOutputConsumer;
import info.u_team.music_player.lavaplayer.api.queue.ITrackManager;
import info.u_team.music_player.lavaplayer.api.search.ITrackSearch;

public interface IMusicPlayer {
	
	ITrackManager getTrackManager();
	
	ITrackSearch getTrackSearch();
	
	void startAudioOutput();
	
	void setMixer(String name);

	void setAutomaticAudioRecovery(boolean enabled);
	
	String getMixer();
	
	DataLine.Info getSpeakerInfo();
	
	int getVolume();
	
	void setVolume(int volume);
	
	float getSpeed();
	
	void setSpeed(float speed);
	
	float getPitch();
	
	void setPitch(float pitch);

	void setEqualizer(boolean enabled, float[] gains, float[] positions, boolean bassBoost);

	/** Applies client-side stereo processing without changing Minecraft's own sound sliders. */
	void setChannelMix(boolean mono, float balance, boolean swapChannels);

	void setDuckingGain(float gain);

	void setTransitionGain(float gain);
	
	void setOutputConsumer(IOutputConsumer consumer);

	/**
	 * Stops playback and releases every thread, audio device and source-manager resource owned by the player.
	 * Implementations must make this method idempotent.
	 */
	void shutdown();
}
