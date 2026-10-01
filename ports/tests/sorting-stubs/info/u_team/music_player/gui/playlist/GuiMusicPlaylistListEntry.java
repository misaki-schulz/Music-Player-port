package info.u_team.music_player.gui.playlist;

import info.u_team.music_player.lavaplayer.api.audio.IAudioTrack;
import info.u_team.music_player.musicplayer.playlist.LoadedTracks;
import info.u_team.music_player.musicplayer.playlist.Playlist;
import info.u_team.music_player.musicplayer.playlist.Playlists;

public class GuiMusicPlaylistListEntry {
	public final IAudioTrack track;
	public GuiMusicPlaylistListEntry() { this(null); }
	public GuiMusicPlaylistListEntry(IAudioTrack track) { this.track = track; }
	public void tick() {}
}

class GuiMusicPlaylistListEntryLoading extends GuiMusicPlaylistListEntry {}

class GuiMusicPlaylistListEntryError extends GuiMusicPlaylistListEntry {
	GuiMusicPlaylistListEntryError(GuiMusicPlaylistList list, Playlists lists, Playlist playlist, LoadedTracks tracks, String error) {}
}

class GuiMusicPlaylistListEntryMusicTrack extends GuiMusicPlaylistListEntry {
	GuiMusicPlaylistListEntryMusicTrack(GuiMusicPlaylistList list, Playlists lists, Playlist playlist, LoadedTracks tracks) { super(tracks.getTrack()); }
}

class GuiMusicPlaylistListEntryPlaylistStart extends GuiMusicPlaylistListEntry {
	GuiMusicPlaylistListEntryPlaylistStart(GuiMusicPlaylistList list, Playlists lists, Playlist playlist, LoadedTracks tracks) {}
	void addEntry(GuiMusicPlaylistListEntryPlaylistTrack entry) {}
}

class GuiMusicPlaylistListEntryPlaylistTrack extends GuiMusicPlaylistListEntry {
	GuiMusicPlaylistListEntryPlaylistTrack(GuiMusicPlaylistListEntryPlaylistStart start, Playlists lists, Playlist playlist, LoadedTracks tracks, IAudioTrack track) { super(track); }
}
