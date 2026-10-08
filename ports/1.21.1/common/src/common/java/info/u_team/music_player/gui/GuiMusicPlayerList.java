package info.u_team.music_player.gui;

import info.u_team.music_player.util.TitleFilter;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.musicplayer.playlist.Playlist;
import info.u_team.music_player.musicplayer.playlist.Playlists;

public class GuiMusicPlayerList extends BetterScrollableList<GuiMusicPlayerListEntry> {
	
	private final Playlists playlists;
	private TitleFilter filter = new TitleFilter("");
	
	public GuiMusicPlayerList(int x, int y, int width, int height) {
		super(x, y, width, height, 50, 20);
		
		playlists = MusicPlayerManager.getPlaylistManager().getPlaylists();
		updateEntries();
	}
	
	public void addPlaylist(String name) {
		final Playlist playlist = new Playlist(name);
		playlists.add(playlist);
		updateEntries();
	}
	
	public void removePlaylist(GuiMusicPlayerListEntry entry) {
		playlists.remove(entry.getPlaylist());
		removeEntry(entry);
	}
	
	public void setFilter(String query) {
		filter = new TitleFilter(query);
		updateEntries();
		setScrollAmount(0);
	}

	private void updateEntries() {
		clearEntries();
		setSelected(null);
		playlists.forEach(playlist -> {
			if (filter.matches(playlist.getName())) {
				addEntry(new GuiMusicPlayerListEntry(this, playlists, playlist));
			}
		});
	}

	public boolean hasNoMatches() {
		return filter.isActive() && children().isEmpty();
	}

	public Playlists getPlaylists() {
		return playlists;
	}
}
