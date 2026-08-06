// Modified for Minecraft 26.2 by misaki-schulz; see NOTICE.
package info.u_team.music_player.gui.playlist.search;

import info.u_team.music_player.gui.BetterScrollableListEntry;
import info.u_team.music_player.gui.util.GuiTrackUtils;
import info.u_team.music_player.init.MusicPlayerResources;
import info.u_team.music_player.lavaplayer.api.audio.IAudioTrack;
import info.u_team.music_player.gui.widget.ImageButton;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

abstract class GuiMusicSearchListEntry extends BetterScrollableListEntry<GuiMusicSearchListEntry> {
	
	protected final ImageButton addTrackButton;
	
	GuiMusicSearchListEntry() {
		addTrackButton = addChildren(new ImageButton(0, 0, 20, 20, MusicPlayerResources.TEXTURE_ADD));
	}
	
	@Override
	public void render(GuiGraphicsExtractor guiGraphics, int slotIndex, int entryY, int entryX, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float partialTicks) {
		addTrackButton.setX(entryWidth - 20);
		addTrackButton.setY(entryY + 8);
		addTrackButton.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
	}
	
	protected void addTrackInfo(GuiGraphicsExtractor guiGraphics, IAudioTrack track, int entryX, int entryY, int entryWidth, int leftMargin, int titleColor) {
		GuiTrackUtils.addTrackInfo(guiGraphics, track, entryX, entryY, entryWidth, leftMargin, titleColor);
	}
	
	@Override
	public Component getNarration() {
		return CommonComponents.EMPTY;
	}
	
}
