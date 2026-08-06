// Modified for Minecraft 26.2 by misaki-schulz; see NOTICE.
package info.u_team.music_player.gui.playlist;

import info.u_team.music_player.lavaplayer.api.audio.IAudioTrack;
import info.u_team.music_player.musicplayer.playlist.LoadedTracks;
import info.u_team.music_player.musicplayer.playlist.Playlist;
import info.u_team.music_player.musicplayer.playlist.Playlists;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class GuiMusicPlaylistListEntryMusicTrack extends GuiMusicPlaylistListEntryFunctions {
	
	private final IAudioTrack track;
	
	public GuiMusicPlaylistListEntryMusicTrack(GuiMusicPlaylistList guilist, Playlists playlists, Playlist playlist, LoadedTracks loadedTrack) {
		super(guilist, playlists, playlist, loadedTrack, loadedTrack.getTrack());
		track = loadedTrack.getTrack();
	}
	
	@Override
	public void drawEntryExtended(GuiGraphicsExtractor guiGraphics, int entryX, int entryY, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean mouseInList, float partialTicks) {
		addTrackInfo(guiGraphics, track, entryX, entryY, entryWidth, 5, isPlaying() ? 0xFFE02626 : 0xFF419BF4);
	}
}
