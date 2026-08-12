package info.u_team.music_player.gui.history;

import static info.u_team.music_player.init.MusicPlayerLocalization.getTranslation;

import java.util.List;

import info.u_team.music_player.gui.BetterScreen;
import info.u_team.music_player.gui.widget.UButton;
import info.u_team.music_player.musicplayer.LibraryStateManager.LibraryEntry;
import info.u_team.music_player.musicplayer.LibraryStateManager.WrappedStatistics;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.util.MinecraftGuiCompat;
import info.u_team.music_player.util.TimeUtil;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class GuiMusicStatistics extends BetterScreen {

	private final Screen previous;
	private final Page page;
	public GuiMusicStatistics(Screen previous) { this(previous, Page.OVERVIEW); }
	private GuiMusicStatistics(Screen previous, Page page) { super(Component.literal(getTranslation("gui.statistics.title"))); this.previous = previous; this.page = page; }

	@Override protected void init() {
		addRenderableWidget(new UButton(8, 8, 28, 20, Component.literal("<"), button -> MinecraftGuiCompat.setScreen(minecraft, previous)));
		final int tabWidth = Math.max(64, (width - 48) / Page.values().length); int x = 40;
		for (final Page candidate : Page.values()) { final UButton tab = addRenderableWidget(new UButton(x, 8, tabWidth - 4, 20, Component.literal(getTranslation(candidate.key)), button -> open(candidate))); tab.active = candidate != page; x += tabWidth; }
		switch (page) { case OVERVIEW -> overview(); case TOP_TRACKS -> topTracks(); case ACHIEVEMENTS -> achievements(); }
	}

	private void overview() {
		final WrappedStatistics stats = MusicPlayerManager.getLibraryStateManager().getWrappedStatistics();
		row(40, getTranslation("gui.statistics.all_time", duration(stats.totalListeningMillis())));
		row(62, getTranslation("gui.statistics.month", duration(stats.currentMonthListeningMillis())));
		row(84, getTranslation("gui.statistics.year", duration(stats.currentYearListeningMillis())));
		row(106, getTranslation("gui.statistics.counts", stats.totalPlays(), stats.skips(), stats.uniqueTracks()));
		row(128, getTranslation("gui.statistics.top_artist", stats.topArtist()));
		row(150, getTranslation("gui.statistics.top_source", stats.topSource()));
		row(172, getTranslation("gui.statistics.active_time", stats.mostActiveHour() < 0 ? getTranslation("gui.statistics.not_enough") : "%02d:00–%02d:00".formatted(stats.mostActiveHour(), (stats.mostActiveHour() + 1) % 24)));
		row(194, getTranslation("gui.statistics.private"));
	}

	private void topTracks() {
		final List<LibraryEntry> top = MusicPlayerManager.getLibraryStateManager().getMostPlayed(7);
		for (int index = 0; index < top.size(); index++) {
			final LibraryEntry entry = top.get(index);
			row(40 + index * 24, (index + 1) + ". " + entry.displayName() + " — " + getTranslation("gui.statistics.plays", entry.playCount(), duration(entry.listeningMillis())));
		}
		if (top.isEmpty()) row(40, getTranslation("gui.statistics.empty"));
	}

	private void achievements() {
		final List<String> values = MusicPlayerManager.getLibraryStateManager().getAchievements();
		row(40, getTranslation("gui.statistics.unlocked_count", values.size()));
		for (int index = 0; index < Math.min(7, values.size()); index++) row(66 + index * 22, getTranslation("gui.statistics.unlocked", values.get(index)));
	}

	private void row(int y, String value) { final UButton button = addRenderableWidget(new UButton(12, y, width - 24, 20, Component.literal(value))); button.active = false; }
	private void open(Page target) { if (target != page) MinecraftGuiCompat.setScreen(minecraft, new GuiMusicStatistics(previous, target)); }
	private static String duration(long millis) { return TimeUtil.timeConversion(Math.max(0L, millis) / 1000L); }
	private enum Page { OVERVIEW("gui.statistics.overview"), TOP_TRACKS("gui.statistics.top_tracks"), ACHIEVEMENTS("gui.statistics.achievements"); private final String key; Page(String key) { this.key = key; } }
}
