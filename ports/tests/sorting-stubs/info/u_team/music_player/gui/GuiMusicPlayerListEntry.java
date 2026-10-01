package info.u_team.music_player.gui;

import info.u_team.music_player.musicplayer.playlist.Playlist;
import info.u_team.music_player.musicplayer.playlist.Playlists;

public class GuiMusicPlayerListEntry {
	private final Playlist playlist;
	public GuiMusicPlayerListEntry(GuiMusicPlayerList list, Playlists playlists, Playlist playlist) { this.playlist = playlist; }
	public Playlist getPlaylist() { return playlist; }
}
