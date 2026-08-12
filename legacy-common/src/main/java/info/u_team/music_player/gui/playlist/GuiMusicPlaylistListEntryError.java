package info.u_team.music_player.gui.playlist;

import info.u_team.music_player.musicplayer.playlist.LoadedTracks;
import info.u_team.music_player.musicplayer.playlist.Playlist;
import info.u_team.music_player.musicplayer.playlist.Playlists;
import net.minecraft.client.gui.GuiGraphics;

public class GuiMusicPlaylistListEntryError extends GuiMusicPlaylistListEntryFunctions {
	
	private final String error;
	
	public GuiMusicPlaylistListEntryError(GuiMusicPlaylistList guilist, Playlists playlists, Playlist playlist, LoadedTracks loadedTrack, String error) {
		super(guilist, playlists, playlist, loadedTrack, null);
		this.error = error;
	}
	
	@Override
	public void drawEntryExtended(GuiGraphics guiGraphics, int entryX, int entryY, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean mouseInList, float partialTicks) {
		info.u_team.music_player.gui.util.GuiTextCompat.draw(guiGraphics, minecraft.font, info.u_team.music_player.gui.util.GuiTrackUtils.trimToWith(error, Math.max(20, entryWidth - 30)), entryX + 5, entryY + 4, 0xFFFF5555, false);
		info.u_team.music_player.gui.util.GuiTextCompat.draw(guiGraphics, minecraft.font, info.u_team.music_player.gui.util.GuiTrackUtils.trimToWith(uri.get(), Math.max(20, entryWidth - 30)), entryX + 5, entryY + 19, 0xFFFF7777, false);
	}
}
