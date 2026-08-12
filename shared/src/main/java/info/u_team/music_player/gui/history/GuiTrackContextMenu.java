package info.u_team.music_player.gui.history;

import static info.u_team.music_player.init.MusicPlayerLocalization.getTranslation;

import info.u_team.music_player.gui.BetterScreen;
import info.u_team.music_player.gui.playlist.ArtworkOverrideDialogs;
import info.u_team.music_player.gui.util.GuiTrackUtils;
import info.u_team.music_player.gui.widget.UButton;
import info.u_team.music_player.musicplayer.LibraryStateManager.LibraryEntry;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.musicplayer.PlaybackActions;
import info.u_team.music_player.musicplayer.playlist.Playlist;
import info.u_team.music_player.util.MinecraftGuiCompat;
import info.u_team.music_player.util.WrappedObject;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class GuiTrackContextMenu extends BetterScreen {

	private final Screen previous;
	private final LibraryEntry entry;
	private final Playlist source;
	private final WrappedObject<String> sourceUri;
	private final Page page;
	private UButton status;
	private UButton favorite;

	public GuiTrackContextMenu(Screen previous, LibraryEntry entry) { this(previous, entry, null, null, Page.ACTIONS); }
	public GuiTrackContextMenu(Screen previous, LibraryEntry entry, Playlist source, WrappedObject<String> sourceUri) { this(previous, entry, source, sourceUri, Page.ACTIONS); }
	private GuiTrackContextMenu(Screen previous, LibraryEntry entry, Playlist source, WrappedObject<String> sourceUri, Page page) {
		super(Component.literal(getTranslation("gui.track_actions.title")));
		this.previous = previous; this.entry = entry; this.source = source; this.sourceUri = sourceUri; this.page = page;
	}

	@Override
	protected void init() {
		addRenderableWidget(new UButton(8, 8, 28, 20, Component.literal("<"), button -> MinecraftGuiCompat.setScreen(minecraft, previous)));
		final UButton title = addRenderableWidget(new UButton(40, 8, width - 48, 20, Component.literal(entry.displayName())));
		title.active = false;
		final int half = (width - 28) / 2;
		final UButton actions = addRenderableWidget(new UButton(12, 34, half, 20, Component.literal(getTranslation("gui.track_actions.playback")), button -> open(Page.ACTIONS)));
		final UButton manage = addRenderableWidget(new UButton(width / 2 + 2, 34, half, 20, Component.literal(getTranslation("gui.track_actions.manage")), button -> open(Page.MANAGE)));
		actions.active = page != Page.ACTIONS; manage.active = page != Page.MANAGE;
		if (page == Page.ACTIONS) initActions(half); else initManage(half);
	}

	private void initActions(int half) {
		add(new UButton(12, 58, half, 20, Component.literal(getTranslation("gui.track_actions.play_now")), button -> PlaybackActions.playNow(entry.uri(), this::setStatus)));
		add(new UButton(width / 2 + 2, 58, half, 20, Component.literal(getTranslation("gui.track_actions.play_next")), button -> PlaybackActions.playNext(entry.uri(), this::setStatus)));
		add(new UButton(12, 82, half, 20, Component.literal(getTranslation("gui.track_actions.queue")), button -> PlaybackActions.queue(entry.uri(), this::setStatus)));
		favorite = add(new UButton(width / 2 + 2, 82, half, 20, favoriteLabel()));
		favorite.setPressable(() -> { MusicPlayerManager.getLibraryStateManager().toggleFavoriteUri(entry.uri()); favorite.setMessage(favoriteLabel()); });
		final int ratingWidth = Math.max(30, (width - 32) / 5);
		for (int index = 0; index < 5; index++) { final int rating = index + 1; add(new UButton(12 + index * ratingWidth, 108, ratingWidth - 3, 20, Component.literal(getTranslation("gui.track_actions.rate", rating)), button -> { MusicPlayerManager.getLibraryStateManager().setRatingUri(entry.uri(), rating); setStatus(getTranslation("gui.track_actions.rating_saved")); })); }
		add(new UButton(12, 134, half, 20, Component.literal(getTranslation("gui.track_actions.copy_uri")), button -> { minecraft.keyboardHandler.setClipboard(entry.uri()); setStatus(getTranslation("gui.track_actions.uri_copied")); }));
		add(new UButton(width / 2 + 2, 134, half, 20, Component.literal(getTranslation("gui.track_actions.open_source")), button -> setStatus(getTranslation(GuiTrackUtils.openURI(entry.uri()) ? "gui.track_actions.opened" : "gui.track_actions.unsupported"))));
		status = disabled(12, 160, width - 24, "");
	}

	private void initManage(int half) {
		add(new UButton(12, 58, half, 20, Component.literal(getTranslation("gui.track_actions.add_playlist")), button -> openSelector(false)));
		final UButton move = add(new UButton(width / 2 + 2, 58, half, 20, Component.literal(getTranslation("gui.track_actions.move_playlist")), button -> openSelector(true)));
		move.active = source != null && sourceUri != null;
		final UButton remove = add(new UButton(12, 84, width - 24, 20, Component.literal(getTranslation("gui.track_actions.remove_playlist")), button -> removeFromSource()));
		remove.active = source != null && sourceUri != null;
		add(new UButton(12, 110, half, 20, Component.literal(getTranslation("gui.track_actions.choose_artwork")), button -> ArtworkOverrideDialogs.choose(entry.uri(), this::setStatus)));
		add(new UButton(width / 2 + 2, 110, half, 20, Component.literal(getTranslation("gui.track_actions.reset_artwork")), button -> ArtworkOverrideDialogs.reset(entry.uri(), this::setStatus)));
		status = disabled(12, 138, width - 24, getTranslation("gui.track_actions.manage_hint"));
	}

	private void openSelector(boolean move) { MinecraftGuiCompat.setScreen(minecraft, new GuiPlaylistTargetSelector(this, entry, source, sourceUri, move)); }
	private void removeFromSource() {
		if (source == null || sourceUri == null) return;
		if (source.remove(sourceUri)) { setStatus(getTranslation("gui.track_actions.removed", source.getName())); MinecraftGuiCompat.setScreen(minecraft, previous); }
		else setStatus(getTranslation("gui.track_actions.remove_failed"));
	}
	private void open(Page target) { if (target != page) MinecraftGuiCompat.setScreen(minecraft, new GuiTrackContextMenu(previous, entry, source, sourceUri, target)); }
	private UButton add(UButton button) { return addRenderableWidget(button); }
	private UButton disabled(int x, int y, int buttonWidth, String value) { final UButton button = add(new UButton(x, y, buttonWidth, 20, Component.literal(value))); button.active = false; return button; }
	private void setStatus(String value) { if (status != null) status.setMessage(Component.literal(value)); }
	private Component favoriteLabel() { return Component.literal(getTranslation(MusicPlayerManager.getLibraryStateManager().isFavoriteUri(entry.uri()) ? "gui.track_actions.remove_favorite" : "gui.track_actions.add_favorite")); }
	private enum Page { ACTIONS, MANAGE }
}
