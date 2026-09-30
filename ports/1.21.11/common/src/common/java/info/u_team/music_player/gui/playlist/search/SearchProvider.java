// Modified for Minecraft 26.2 by misaki-schulz; see NOTICE.
package info.u_team.music_player.gui.playlist.search;

import info.u_team.music_player.init.MusicPlayerResources;
import net.minecraft.resources.Identifier;

public enum SearchProvider {
	
	YOUTUBE("ytsearch:", MusicPlayerResources.TEXTURE_SOCIAL_YOUTUBE),
	SOUNDCLOUD("scsearch:", MusicPlayerResources.TEXTURE_SOCIAL_SOUNDCLOUD);
	
	private final String prefix;
	private final Identifier logo;
	
	private SearchProvider(String prefix, Identifier logo) {
		this.prefix = prefix;
		this.logo = logo;
	}
	
	public String getPrefix() {
		return prefix;
	}
	
	public Identifier getLogo() {
		return logo;
	}
	
	public static SearchProvider toggle(SearchProvider provider) {
		if (provider == YOUTUBE) {
			return SOUNDCLOUD;
		}
		return YOUTUBE;
	}
	
}
