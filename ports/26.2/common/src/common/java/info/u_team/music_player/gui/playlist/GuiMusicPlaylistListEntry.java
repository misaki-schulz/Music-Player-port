// Modified for Minecraft 26.2 by misaki-schulz; see NOTICE.
package info.u_team.music_player.gui.playlist;

import info.u_team.music_player.gui.BetterScrollableListEntry;
import info.u_team.music_player.gui.util.GuiTrackUtils;
import info.u_team.music_player.lavaplayer.api.audio.IAudioTrack;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public abstract class GuiMusicPlaylistListEntry extends BetterScrollableListEntry<GuiMusicPlaylistListEntry> {
	
	protected void addTrackInfo(GuiGraphicsExtractor guiGraphics, IAudioTrack track, int entryX, int entryY, int entryWidth, int leftMargin, int titleColor) {
		GuiTrackUtils.addTrackInfo(guiGraphics, track, entryX, entryY, entryWidth, leftMargin, titleColor);
	}
	
	protected void tick() {
	}
	
	@Override
	public Component getNarration() {
		return CommonComponents.EMPTY;
	}
}
