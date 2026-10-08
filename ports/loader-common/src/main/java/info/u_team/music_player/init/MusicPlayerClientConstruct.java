package info.u_team.music_player.init;

import info.u_team.music_player.config.ClientConfig;
import info.u_team.music_player.dependency.DependencyManager;
import info.u_team.music_player.musicplayer.MusicPlayerInitManager;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.musicplayer.YoutubeLoginCoordinator;


public class MusicPlayerClientConstruct {
	
	private static boolean ready;
	public static boolean isReady() { return ready; }
	public static void construct() {
		System.setProperty("http.agent", "Chrome");
		
		ClientConfig.load();
		
		DependencyManager.load();
		
		MusicPlayerInitManager.register();
		YoutubeLoginCoordinator.initialize();
		ready = true;
	}
	public static void shutdown() {
		if (!ready) return;
		ready = false;
		YoutubeLoginCoordinator.shutdown();
		MusicPlayerManager.shutdown();
	}
}
