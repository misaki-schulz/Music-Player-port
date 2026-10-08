// Modified for Minecraft 26.2 by misaki-schulz; see NOTICE.
package info.u_team.music_player.gui.playlist;

import java.util.ArrayList;
import java.util.List;

import info.u_team.music_player.util.TitleFilter;
import info.u_team.music_player.gui.BetterScrollableList;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.musicplayer.playlist.LoadedTracks;
import info.u_team.music_player.musicplayer.playlist.Playlist;
import info.u_team.music_player.musicplayer.playlist.Playlists;

public class GuiMusicPlaylistList extends BetterScrollableList<GuiMusicPlaylistListEntry> {
	
	private final Playlist playlist;
	
	private boolean tracksLoaded;
	private TitleFilter filter = new TitleFilter("");
	
	public GuiMusicPlaylistList(Playlist playlist) {
		super(0, 0, 0, 0, 40, 20);
		this.playlist = playlist;
		addEntry(new GuiMusicPlaylistListEntryLoading());
	}
	
	private void addLoadedTrackToGui(LoadedTracks loadedTracks) {
		final Playlists playlists = MusicPlayerManager.getPlaylistManager().getPlaylists();
		final List<GuiMusicPlaylistListEntry> list = new ArrayList<>();
		if (loadedTracks.hasError()) {// Add error gui element
			if (filter.matches(loadedTracks.getUri().get())) {
				list.add(new GuiMusicPlaylistListEntryError(this, playlists, playlist, loadedTracks, loadedTracks.getErrorMessage()));
			}
		} else if (loadedTracks.isTrack()) { // Add track gui element
			if (filter.matches(loadedTracks.getTrack())) {
				list.add(new GuiMusicPlaylistListEntryMusicTrack(this, playlists, playlist, loadedTracks));
			}
		} else if (loadedTracks.isTrackList()) { // Add playlist start element and all track sub elements
			final var matchingTracks = filter.tracks(loadedTracks);
			if (filter.isActive() && matchingTracks.isEmpty()) {
				return;
			}
			final GuiMusicPlaylistListEntryPlaylistStart start = new GuiMusicPlaylistListEntryPlaylistStart(this, playlists, playlist, loadedTracks);
			list.add(start);
			loadedTracks.getTrackList().getTracks().forEach(track -> {
				final GuiMusicPlaylistListEntryPlaylistTrack entry = new GuiMusicPlaylistListEntryPlaylistTrack(start, playlists, playlist, loadedTracks, track);
				start.addEntry(entry);
				if (filter.matches(track)) {
					list.add(entry);
				}
			});
		}
		list.forEach(this::addEntry);
	}
	
	public void addAllEntries() {
		if (!playlist.isLoaded()) {
			if (children().isEmpty()) {
				addEntry(new GuiMusicPlaylistListEntryLoading());
			}
			return;
		}
		if (!tracksLoaded) {
			clearEntries();
			playlist.getLoadedTracks().forEach(this::addLoadedTrackToGui);
			tracksLoaded = true;
		}
	}
	
	public void removeAllEntries() {
		clearEntries();
		tracksLoaded = false;
	}
	
	public void updateAllEntries() {
		removeAllEntries();
		addAllEntries();
	}
	
	public void setSelectedEntryWhenMove(GuiMusicPlaylistListEntry entry, int indexOffset) {
		final int index = children().lastIndexOf(entry) + indexOffset;
		if (index >= 0 && index < children().size()) {
			setSelected(children().get(index));
		}
	}
	
	public void setFilter(String query) {
		filter = new TitleFilter(query);
		setSelected(null);
		updateAllEntries();
		setScrollAmount(0);
	}

	public boolean hasNoMatches() {
		return playlist.isLoaded() && filter.isActive() && children().isEmpty();
	}

	public void tick() {
		children().forEach(GuiMusicPlaylistListEntry::tick);
	}
}
