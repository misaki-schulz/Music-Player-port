package info.u_team.music_player.init;

import info.u_team.music_player.config.ClientConfig;
import info.u_team.music_player.dependency.DependencyManager;
import info.u_team.music_player.musicplayer.MusicPlayerInitManager;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;

public class MusicPlayerClientConstruct {
	
	public static void construct() {
		ClientConfig.load();
		
		DependencyManager.load();
		
		MusicPlayerInitManager.register();
		MusicPlayerKeys.register();
		MusicPlayerClientCommands.register();
		
		MusicPlayerEventHandler.register();
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			MusicPlayerManager.shutdown();
			DependencyManager.shutdown();
		});
	}
	
}
