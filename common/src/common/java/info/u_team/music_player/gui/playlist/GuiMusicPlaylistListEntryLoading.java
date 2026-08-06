// Modified for Minecraft 26.2 by misaki-schulz; see NOTICE.
package info.u_team.music_player.gui.playlist;

import static info.u_team.music_player.init.MusicPlayerLocalization.GUI_PLAYLIST_LOADING;
import static info.u_team.music_player.init.MusicPlayerLocalization.getTranslation;

import net.minecraft.client.gui.GuiGraphicsExtractor;

public class GuiMusicPlaylistListEntryLoading extends GuiMusicPlaylistListEntry {
	
	@Override
	public void render(GuiGraphicsExtractor guiGraphics, int slotIndex, int entryY, int entryX, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float partialTicks) {
		final String text = getTranslation(GUI_PLAYLIST_LOADING);
		guiGraphics.text(minecraft.font, text, entryX + (entryWidth / 2) - (minecraft.font.width(text) / 2), entryY + 20, 0xFFFF0000, false);
	}
	
}
