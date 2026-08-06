// Modified for Minecraft 26.2 by misaki-schulz; see NOTICE.
package info.u_team.music_player.gui.playlist;

import info.u_team.music_player.musicplayer.playlist.LoadedTracks;
import info.u_team.music_player.musicplayer.playlist.Playlist;
import info.u_team.music_player.musicplayer.playlist.Playlists;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class GuiMusicPlaylistListEntryError extends GuiMusicPlaylistListEntryFunctions {
	
	private final String error;
	
	public GuiMusicPlaylistListEntryError(GuiMusicPlaylistList guilist, Playlists playlists, Playlist playlist, LoadedTracks loadedTrack, String error) {
		super(guilist, playlists, playlist, loadedTrack, null);
		this.error = error;
	}
	
	@Override
	public void drawEntryExtended(GuiGraphicsExtractor guiGraphics, int entryX, int entryY, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean mouseInList, float partialTicks) {
		guiGraphics.text(minecraft.font, error, entryX + 5, entryY + 5, 0xFFFF0000, false);
		guiGraphics.text(minecraft.font, uri.get(), entryX + 5, entryY + 25, 0xFFFF0000, false);
	}
}
