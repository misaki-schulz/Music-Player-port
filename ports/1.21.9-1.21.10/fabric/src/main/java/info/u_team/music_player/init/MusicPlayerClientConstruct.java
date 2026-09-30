package info.u_team.music_player.init;

import info.u_team.music_player.config.ClientConfig;
import info.u_team.music_player.dependency.DependencyManager;
import info.u_team.music_player.musicplayer.MusicPlayerInitManager;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.musicplayer.YoutubeLoginCoordinator;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public class MusicPlayerClientConstruct {
	
	public static void construct() {
		System.setProperty("http.agent", "Chrome");
		
		ClientConfig.load();
		
		DependencyManager.load();
		
		MusicPlayerInitManager.register();
		YoutubeLoginCoordinator.initialize();
		MusicPlayerKeys.register();
		
		MusicPlayerEventHandler.register();
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			YoutubeLoginCoordinator.shutdown();
			MusicPlayerManager.shutdown();
		});
	}
	
}
