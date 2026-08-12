package info.u_team.music_player.gui.history;

import static info.u_team.music_player.init.MusicPlayerLocalization.getTranslation;

import java.util.Comparator;
import java.util.List;

import info.u_team.music_player.gui.BetterScreen;
import info.u_team.music_player.gui.widget.UButton;
import info.u_team.music_player.musicplayer.LibraryStateManager.LibraryEntry;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.musicplayer.PlaybackActions;
import info.u_team.music_player.util.MinecraftGuiCompat;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Unified client-side library with generated smart views. */
public final class GuiMusicLibrary extends BetterScreen {

	private static final int PAGE_SIZE = 7;
	private static final Comparator<LibraryEntry> NAME = Comparator.comparing(LibraryEntry::displayName, String.CASE_INSENSITIVE_ORDER);
	private final Screen previous;
	private final View view;
	private final int page;
	private UButton status;

	public GuiMusicLibrary(Screen previous) { this(previous, View.ALL, 0); }
	private GuiMusicLibrary(Screen previous, View view, int page) { super(Component.literal(getTranslation("gui.library.title"))); this.previous = previous; this.view = view; this.page = Math.max(0, page); }

	@Override protected void init() {
		addRenderableWidget(new UButton(8, 8, 28, 20, Component.literal("<"), button -> MinecraftGuiCompat.setScreen(minecraft, previous)));
		addRenderableWidget(new UButton(40, 8, width - 48, 20, Component.literal(getTranslation("gui.library.smart", getTranslation(view.key))), button -> open(view.next(), 0)));
		final List<LibraryEntry> entries = entries();
		final int start = Math.min(entries.size(), page * PAGE_SIZE), end = Math.min(entries.size(), start + PAGE_SIZE);
		for (int index = start; index < end; index++) {
			final LibraryEntry entry = entries.get(index); final int y = 38 + (index - start) * 25;
			final int contextWidth = 28, queueWidth = 30, labelWidth = Math.max(50, width - 28 - contextWidth - queueWidth);
			addRenderableWidget(new UButton(8, y, labelWidth, 20, Component.literal(entry.displayName()), button -> PlaybackActions.playNow(entry.uri(), this::setStatus)));
			addRenderableWidget(new UButton(12 + labelWidth, y, queueWidth, 20, Component.literal("+1"), button -> PlaybackActions.playNext(entry.uri(), this::setStatus)));
			addRenderableWidget(new UButton(16 + labelWidth + queueWidth, y, contextWidth, 20, Component.literal("..."), button -> MinecraftGuiCompat.setScreen(minecraft, new GuiTrackContextMenu(this, entry))));
		}
		final int maxPage = Math.max(0, (entries.size() - 1) / PAGE_SIZE);
		final UButton back = addRenderableWidget(new UButton(8, height - 28, 66, 20, Component.literal(getTranslation("gui.common.previous")), button -> open(view, page - 1))); back.active = page > 0;
		final UButton next = addRenderableWidget(new UButton(width - 74, height - 28, 66, 20, Component.literal(getTranslation("gui.common.next")), button -> open(view, page + 1))); next.active = page < maxPage;
		status = addRenderableWidget(new UButton(80, height - 28, Math.max(30, width - 160), 20, Component.literal(getTranslation("gui.library.tracks", entries.size())))); status.active = false;
	}

	private List<LibraryEntry> entries() {
		final var stream = MusicPlayerManager.getLibraryStateManager().getUnifiedLibrary().stream();
		return switch (view) {
		case ALL -> stream.sorted(NAME).toList();
		case RECENT -> stream.filter(entry -> entry.lastPlayedEpochMillis() > 0).sorted(Comparator.comparingLong(LibraryEntry::lastPlayedEpochMillis).reversed()).toList();
		case FAVORITES -> stream.filter(LibraryEntry::favorite).sorted(NAME).toList();
		case FREQUENT -> stream.filter(entry -> entry.playCount() > 0).sorted(Comparator.comparingLong(LibraryEntry::playCount).reversed()).toList();
		case FORGOTTEN -> stream.filter(entry -> entry.lastPlayedEpochMillis() > 0).sorted(Comparator.comparingLong(LibraryEntry::lastPlayedEpochMillis)).toList();
		case ARTISTS -> stream.sorted(Comparator.comparing((LibraryEntry entry) -> entry.author() == null ? "" : entry.author(), String.CASE_INSENSITIVE_ORDER).thenComparing(NAME)).toList();
		case SOURCES -> stream.sorted(Comparator.comparing(LibraryEntry::sourceLabel, String.CASE_INSENSITIVE_ORDER).thenComparing(NAME)).toList();
		case LOCAL -> stream.filter(LibraryEntry::isLocal).sorted(NAME).toList();
		case SHORT -> stream.filter(entry -> entry.duration() > 0 && entry.duration() <= 5 * 60_000L).sorted(NAME).toList();
		case LONG -> stream.filter(entry -> entry.duration() >= 20 * 60_000L).sorted(NAME).toList();
		};
	}

	private void setStatus(String value) { if (status != null) status.setMessage(Component.literal(value)); }
	private void open(View target, int targetPage) { MinecraftGuiCompat.setScreen(minecraft, new GuiMusicLibrary(previous, target, targetPage)); }
	private enum View {
		ALL("gui.library.view.all"), RECENT("gui.library.view.recent"), FAVORITES("gui.library.view.favorites"), FREQUENT("gui.library.view.frequent"), FORGOTTEN("gui.library.view.forgotten"),
		ARTISTS("gui.library.view.artists"), SOURCES("gui.library.view.sources"), LOCAL("gui.library.view.local"), SHORT("gui.library.view.short"), LONG("gui.library.view.long");
		private final String key;
		View(String key) { this.key = key; }
		View next() { final View[] values = values(); return values[(ordinal() + 1) % values.length]; }
	}
}
