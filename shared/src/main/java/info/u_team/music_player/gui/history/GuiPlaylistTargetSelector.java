package info.u_team.music_player.gui.history;

import static info.u_team.music_player.init.MusicPlayerLocalization.getTranslation;

import java.util.List;

import info.u_team.music_player.gui.BetterScreen;
import info.u_team.music_player.gui.widget.UButton;
import info.u_team.music_player.musicplayer.LibraryStateManager.LibraryEntry;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.musicplayer.playlist.Playlist;
import info.u_team.music_player.util.MinecraftGuiCompat;
import info.u_team.music_player.util.WrappedObject;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Selects a saved playlist for a copy/add or move operation. */
public final class GuiPlaylistTargetSelector extends BetterScreen {

	private static final int PAGE_SIZE = 7;
	private final Screen previous;
	private final LibraryEntry entry;
	private final Playlist source;
	private final WrappedObject<String> sourceUri;
	private final boolean move;
	private final int page;
	private UButton status;

	public GuiPlaylistTargetSelector(Screen previous, LibraryEntry entry, Playlist source, WrappedObject<String> sourceUri, boolean move) {
		this(previous, entry, source, sourceUri, move, 0);
	}

	private GuiPlaylistTargetSelector(Screen previous, LibraryEntry entry, Playlist source, WrappedObject<String> sourceUri, boolean move, int page) {
		super(Component.literal(getTranslation(move ? "gui.selector.move_title" : "gui.selector.add_title")));
		this.previous = previous; this.entry = entry; this.source = source; this.sourceUri = sourceUri; this.move = move; this.page = Math.max(0, page);
	}

	@Override
	protected void init() {
		addRenderableWidget(new UButton(8, 8, 28, 20, Component.literal("<"), button -> MinecraftGuiCompat.setScreen(minecraft, previous)));
		final UButton title = addRenderableWidget(new UButton(40, 8, width - 48, 20, Component.literal(getTranslation(move ? "gui.selector.move" : "gui.selector.add", entry.displayName()))));
		title.active = false;
		final List<Playlist> playlists = MusicPlayerManager.getPlaylistManager().getPlaylists().asList();
		final int start = Math.min(playlists.size(), page * PAGE_SIZE), end = Math.min(playlists.size(), start + PAGE_SIZE);
		for (int index = start; index < end; index++) {
			final Playlist target = playlists.get(index);
			addRenderableWidget(new UButton(12, 40 + (index - start) * 24, width - 24, 20,
					Component.literal(target.getName() + " (" + target.getEntrySize() + ")"), button -> choose(target)));
		}
		final int maxPage = Math.max(0, (playlists.size() - 1) / PAGE_SIZE);
		final UButton back = addRenderableWidget(new UButton(12, height - 50, 72, 20, Component.literal(getTranslation("gui.common.previous")), button -> open(page - 1)));
		back.active = page > 0;
		final UButton next = addRenderableWidget(new UButton(width - 84, height - 50, 72, 20, Component.literal(getTranslation("gui.common.next")), button -> open(page + 1)));
		next.active = page < maxPage;
		status = addRenderableWidget(new UButton(12, height - 26, width - 24, 20, Component.literal(getTranslation(playlists.isEmpty() ? "gui.selector.create_first" : "gui.selector.choose"))));
		status.active = false;
	}

	private void choose(Playlist target) {
		if (move && target == source) { setStatus(getTranslation("gui.selector.already", target.getName())); return; }
		if (target.addUri(entry.uri()) == null) { setStatus(getTranslation("gui.selector.empty_uri")); return; }
		if (move && source != null && sourceUri != null && !source.remove(sourceUri)) { setStatus(getTranslation("gui.selector.source_changed")); return; }
		setStatus(getTranslation(move ? "gui.selector.moved" : "gui.selector.added", target.getName()));
	}

	private void open(int targetPage) { MinecraftGuiCompat.setScreen(minecraft, new GuiPlaylistTargetSelector(previous, entry, source, sourceUri, move, targetPage)); }
	private void setStatus(String value) { if (status != null) status.setMessage(Component.literal(value)); }
}
