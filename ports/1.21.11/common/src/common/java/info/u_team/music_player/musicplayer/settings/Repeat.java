// Modified for Minecraft 26.2 by misaki-schulz; see NOTICE.
package info.u_team.music_player.musicplayer.settings;

import info.u_team.music_player.init.MusicPlayerResources;
import net.minecraft.resources.Identifier;

public enum Repeat {
	
	NO(false, MusicPlayerResources.TEXTURE_REPEAT),
	PLAYLIST(true, MusicPlayerResources.TEXTURE_REPEAT),
	SINGLE(true, MusicPlayerResources.TEXTURE_REPEAT_SINGLE);
	
	private final boolean active;
	private final Identifier resource;
	
	private Repeat(boolean active, Identifier resource) {
		this.active = active;
		this.resource = resource;
	}
	
	public Identifier getResource() {
		return resource;
	}
	
	public boolean isActive() {
		return active;
	}
	
	public static Repeat forwardCycle(Repeat repeat) {
		if (repeat == NO) {
			repeat = PLAYLIST;
		} else if (repeat == PLAYLIST) {
			repeat = SINGLE;
		} else {
			repeat = NO;
		}
		return repeat;
	}
	
}
