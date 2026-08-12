package info.u_team.music_player.gui.playlist;

import info.u_team.music_player.gui.util.GuiTrackUtils;
import info.u_team.music_player.init.MusicPlayerResources;
import info.u_team.music_player.lavaplayer.api.audio.IAudioTrack;
import info.u_team.music_player.lavaplayer.api.audio.IPlayingTrack;
import info.u_team.music_player.lavaplayer.api.queue.ITrackManager;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.musicplayer.playlist.LoadedTracks;
import info.u_team.music_player.musicplayer.playlist.Playlist;
import info.u_team.music_player.musicplayer.playlist.Playlists;
import info.u_team.music_player.gui.widget.ImageToggleButton;
import info.u_team.music_player.gui.widget.UButton;
import info.u_team.music_player.gui.history.GuiTrackContextMenu;
import info.u_team.music_player.musicplayer.LibraryStateManager.LibraryEntry;
import info.u_team.music_player.util.WrappedObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public abstract class GuiMusicPlaylistListEntryPlayable extends GuiMusicPlaylistListEntry {
	
	private final ITrackManager manager;
	private final IAudioTrack track;
	
	private final LoadedTracks loadedTrack;
	private final Playlist playlist;
	
	protected final ImageToggleButton playTrackButton;
	private final UButton playNextButton;
	private final UButton favoriteButton;
	
	GuiMusicPlaylistListEntryPlayable(Playlists playlists, Playlist playlist, LoadedTracks loadedTrack, IAudioTrack track) {
		this.track = track;
		this.loadedTrack = loadedTrack;
		this.playlist = playlist;
		manager = MusicPlayerManager.getPlayer().getTrackManager();
		
		playTrackButton = addChildren(new ImageToggleButton(0, 0, 16, 16, MusicPlayerResources.TEXTURE_PLAY, MusicPlayerResources.TEXTURE_PAUSE, false));
		playNextButton = addChildren(new UButton(0, 0, 16, 16, Component.literal("+1")));
		favoriteButton = addChildren(new UButton(0, 0, 16, 16, favoriteLabel()));
		
		if (loadedTrack.hasError() || track == null) {
			playTrackButton.visible = false;
			playNextButton.visible = false;
			favoriteButton.visible = false;
		} else {
			playNextButton.setPressable(() -> manager.playNext(track));
			favoriteButton.setPressable(() -> {
				MusicPlayerManager.getLibraryStateManager().toggleFavorite(track.getInfo());
				favoriteButton.setMessage(favoriteLabel());
			});
			playTrackButton.setToggled(track == getCurrentlyPlaying());
			playTrackButton.setPressable(() -> {
				final boolean play = playTrackButton.isToggled();
				if (play) {
					if (manager.isPaused() && getCurrentlyPlaying() == track) {
						manager.setPaused(false);
					} else {
						playlists.setPlaying(playlist);
						playlist.setPlayable(loadedTrack, track);
						manager.setTrackQueue(playlist);
						manager.start();
					}
				} else {
					manager.setPaused(true);
				}
			});
		}
	}
	
	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 1 && track != null) {
			openContextMenu();
			return true;
		}
		if (event.button() == 2) {
			final String uri = this instanceof GuiMusicPlaylistListEntryPlaylistStart || this instanceof GuiMusicPlaylistListEntryError ? loadedTrack.getUri().get() : track.getInfo().getURI();
			if (GuiTrackUtils.openURI(uri)) {
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}
	
	@Override
	public void render(GuiGraphics guiGraphics, int slotIndex, int entryY, int entryX, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float partialTicks) {
		playTrackButton.setX(entryWidth - 60);
		playTrackButton.setY(entryY + 9);
		playTrackButton.render(guiGraphics, mouseX, mouseY, partialTicks);
		playNextButton.setX(entryWidth - 80);
		playNextButton.setY(entryY + 9);
		playNextButton.render(guiGraphics, mouseX, mouseY, partialTicks);
		favoriteButton.setX(entryWidth - 100);
		favoriteButton.setY(entryY + 9);
		favoriteButton.render(guiGraphics, mouseX, mouseY, partialTicks);
	}
	
	@Override
	protected void tick() {
		if (isPlaying()) {
			playTrackButton.setToggled(!manager.isPaused());
		} else {
			playTrackButton.setToggled(false);
		}
	}
	
	protected IAudioTrack getCurrentlyPlaying() {
		final IPlayingTrack track = manager.getCurrentTrack();
		return track == null ? null : track.getOriginalTrack();
	}
	
	protected boolean isPlaying() {
		return getCurrentlyPlaying() == track;
	}
	
	public IAudioTrack getTrack() {
		return track;
	}
	
	public ImageToggleButton getPlayTrackButton() {
		return playTrackButton;
	}

	private void openContextMenu() {
		final WrappedObject<String> sourceUri = this instanceof GuiMusicPlaylistListEntryFunctions functions ? functions.getSourceUri() : null;
		final String uri = sourceUri == null ? track.getInfo().getURI() : sourceUri.get();
		final var library = MusicPlayerManager.getLibraryStateManager();
		final LibraryEntry entry = new LibraryEntry(uri, track.getInfo().getFixedTitle(), track.getInfo().getFixedAuthor(), track.getInfo().getArtworkURL(), 0L, 0L, 0L, library.isFavoriteUri(uri), library.getRatingUri(uri), track.getDuration());
		minecraft.setScreen(new GuiTrackContextMenu(minecraft.screen, entry, sourceUri == null ? null : playlist, sourceUri));
	}

	private Component favoriteLabel() {
		return Component.literal(track != null && MusicPlayerManager.getLibraryStateManager().isFavorite(track.getInfo()) ? "★" : "☆");
	}
}
