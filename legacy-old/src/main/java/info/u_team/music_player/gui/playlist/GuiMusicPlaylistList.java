package info.u_team.music_player.gui.playlist;

import java.util.ArrayList;
import java.util.List;

import info.u_team.music_player.gui.BetterScrollableList;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.musicplayer.playlist.LoadedTracks;
import info.u_team.music_player.musicplayer.playlist.Playlist;
import info.u_team.music_player.musicplayer.playlist.Playlists;
import info.u_team.music_player.musicplayer.playlist.TrackFilter;
import info.u_team.music_player.util.WrappedObject;

public class GuiMusicPlaylistList extends BetterScrollableList<GuiMusicPlaylistListEntry> {
	
	private final Playlist playlist;
	
	private boolean tracksLoaded;
	private String filter = "";
	private WrappedObject<String> dragSource, dragTarget;
	
	private int selectIndex = -1;
	
	public GuiMusicPlaylistList(Playlist playlist) {
		super(0, 0, 0, 0, 34, 12);
		this.playlist = playlist;
		addEntry(new GuiMusicPlaylistListEntryLoading());
	}
	
	private void addLoadedTrackToGui(LoadedTracks loadedTracks) {
		final Playlists playlists = MusicPlayerManager.getPlaylistManager().getPlaylists();
		final List<GuiMusicPlaylistListEntry> list = new ArrayList<>();
		if (loadedTracks.hasError()) {// Add error gui element
			if (TrackFilter.matches(loadedTracks.getUri().get() + " " + loadedTracks.getErrorMessage(), filter)) list.add(new GuiMusicPlaylistListEntryError(this, playlists, playlist, loadedTracks, loadedTracks.getErrorMessage()));
		} else if (loadedTracks.isTrack()) { // Add track gui element
			if (TrackFilter.matches(loadedTracks.getTrack(), filter)) list.add(new GuiMusicPlaylistListEntryMusicTrack(this, playlists, playlist, loadedTracks));
		} else if (loadedTracks.isTrackList()) { // Add playlist start element and all track sub elements
			final var matchingTracks = loadedTracks.getTrackList().getTracks().stream().filter(track -> TrackFilter.matches(track, filter)).toList();
			if (matchingTracks.isEmpty()) return;
			final GuiMusicPlaylistListEntryPlaylistStart start = new GuiMusicPlaylistListEntryPlaylistStart(this, playlists, playlist, loadedTracks);
			list.add(start);
			matchingTracks.forEach(track -> {
				final GuiMusicPlaylistListEntryPlaylistTrack entry = new GuiMusicPlaylistListEntryPlaylistTrack(start, playlists, playlist, loadedTracks, track);
				start.addEntry(entry);
				list.add(entry);
			});
		}
		list.forEach(this::addEntry);
	}
	
	public void addAllEntries() {
		if (!playlist.isLoaded()) {
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

	public void setFilter(String value) {
		filter = value == null ? "" : value;
		updateAllEntries();
	}
	
	public void setSelectedEntryWhenMove(GuiMusicPlaylistListEntry entry, int indexOffset) {
		final int index = children().lastIndexOf(entry) + indexOffset;
		if (index >= 0 && index < children().size()) {
			selectIndex = index;
		}
	}
	
	@Override
	protected boolean isSelectedItem(int index) {
		return index == selectIndex;
	}
	
	@Override
	public void setSelected(GuiMusicPlaylistListEntry entry) {
		if (entry != null) {
			selectIndex = children().indexOf(entry);
		}
		super.setSelected(entry);
	}
	
	public void tick() {
		children().forEach(GuiMusicPlaylistListEntry::tick);
	}

	@Override public boolean mouseClicked(double mouseX,double mouseY,int button){if(button==0&&mouseX<=getX()+18){final WrappedObject<String> uri=sourceAt(mouseX,mouseY);if(uri!=null){dragSource=uri;dragTarget=uri;return true;}}return super.mouseClicked(mouseX,mouseY,button);}
	@Override public boolean mouseDragged(double mouseX,double mouseY,int button,double dragX,double dragY){if(dragSource!=null&&button==0){final WrappedObject<String> target=sourceAt(mouseX,mouseY);if(target!=null)dragTarget=target;return true;}return super.mouseDragged(mouseX,mouseY,button,dragX,dragY);}
	@Override public boolean mouseReleased(double mouseX,double mouseY,int button){if(dragSource!=null&&button==0){final WrappedObject<String> source=dragSource,target=dragTarget;dragSource=null;dragTarget=null;if(source!=target&&playlist.moveTo(source,target))updateAllEntries();return true;}return super.mouseReleased(mouseX,mouseY,button);}
	private WrappedObject<String> sourceAt(double x,double y){final GuiMusicPlaylistListEntry entry=getEntryAtPosition(x,y);if(entry instanceof GuiMusicPlaylistListEntryFunctions functions)return functions.getSourceUri();if(entry instanceof GuiMusicPlaylistListEntryPlaylistTrack child)return child.getStart().getSourceUri();return null;}
}
