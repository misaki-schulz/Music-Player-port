package info.u_team.music_player.gui.settings;

import static info.u_team.music_player.init.MusicPlayerLocalization.getTranslation;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import info.u_team.music_player.gui.BetterScreen;
import info.u_team.music_player.gui.widget.UButton;
import info.u_team.music_player.gui.widget.USlider;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.musicplayer.settings.EqualizerMode;
import info.u_team.music_player.musicplayer.settings.Settings;
import info.u_team.music_player.musicplayer.settings.VisualizerStyle;
import info.u_team.music_player.util.MinecraftGuiCompat;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Advanced configuration owned by Mod Menu. The normal player UI intentionally keeps only everyday playback controls.
 */
public final class GuiMusicPlayerAdvancedSettings extends BetterScreen {

	private final Screen previousScreen;
	private final Page page;

	public GuiMusicPlayerAdvancedSettings(Screen previousScreen) {
		this(previousScreen, Page.AUDIO);
	}

	private GuiMusicPlayerAdvancedSettings(Screen previousScreen, Page page) {
		super(text("gui.advanced.title"));
		this.previousScreen = previousScreen;
		this.page = page;
	}

	@Override
	protected void init() {
		final Settings settings = MusicPlayerManager.getSettingsManager().getSettings();
		addRenderableWidget(new UButton(8, 8, 24, 20, Component.literal("<"), button -> MinecraftGuiCompat.setScreen(minecraft, previousScreen)));

		final int tabWidth = Math.max(70, (width - 48) / Page.values().length);
		int tabX = 40;
		for (final Page candidate : Page.values()) {
			final UButton tab = addRenderableWidget(new UButton(tabX, 8, tabWidth - 4, 20, text(candidate.key)));
			tab.active = candidate != page;
			tab.setPressable(() -> open(candidate));
			tabX += tabWidth;
		}

		switch (page) {
		case AUDIO -> initAudio(settings);
		case INTERFACE -> initInterface(settings);
		case SERVICES -> initServices(settings);
		case PRIVACY -> initPrivacy(settings);
		}
	}

	private void initAudio(Settings settings) {
		final int left = 12;
		final int right = width / 2 + 6;
		final int column = width / 2 - 18;
		addToggle(left, 48, column, "gui.advanced.restore_session", settings::isRestoreSession, settings::setRestoreSession);
		final UButton pausedRestore = addRenderableWidget(new UButton(right, 48, column, 20, text("gui.advanced.restored_paused")));
		pausedRestore.active = false;
		addToggle(left, 74, column, "gui.advanced.recover_audio", settings::isAutomaticAudioRecovery, settings::setAutomaticAudioRecovery);
		addToggle(right, 74, column, "gui.advanced.duck_sounds", settings::isDuckingEnabled, settings::setDuckingEnabled);
		addIntSlider(left, 100, column, "gui.advanced.ducking", "%", 0, 100, settings.getDuckingPercent(), settings::setDuckingPercent);
		addFloatSlider(right, 100, column, "gui.advanced.transition", " s", 0, 15, settings.getCrossfadeSeconds(), 1, value -> settings.setCrossfadeSeconds(value));
		addIntSlider(left, 126, column, "gui.advanced.duck_attack", " ms", 20, 2000, settings.getDuckingAttackMillis(), settings::setDuckingAttackMillis);
		addIntSlider(right, 126, column, "gui.advanced.duck_release", " ms", 50, 5000, settings.getDuckingReleaseMillis(), settings::setDuckingReleaseMillis);

		final UButton equalizer = addRenderableWidget(new UButton(left, 152, column, 20, equalizerLabel(settings)));
		equalizer.setPressable(() -> {
			settings.setEqualizerMode(EqualizerMode.next(settings.getEqualizerMode()));
			equalizer.setMessage(equalizerLabel(settings));
		});
		addToggle(right, 152, column, "gui.advanced.bass_boost", settings::isBassBoost, settings::setBassBoost);
		addRenderableWidget(new UButton(left, 178, column, 20, text("gui.advanced.open_equalizer"), button -> {
			if (settings.getEqualizerMode() == EqualizerMode.OFF) settings.setEqualizerMode(EqualizerMode.PARAMETRIC);
			MinecraftGuiCompat.setScreen(minecraft, new GuiEqualizerSettings(this));
		}));
		addRenderableWidget(new UButton(right, 178, column, 20, text("gui.advanced.reset_equalizer"), button -> settings.resetEqualizer()));
		addRenderableWidget(new UButton(left, 204, width - 24, 20, text("gui.advanced.audio_routing"), button ->
				MinecraftGuiCompat.setScreen(minecraft, new GuiAudioRoutingSettings(this))));
	}

	private void initInterface(Settings settings) {
		final int left = 12;
		final int right = width / 2 + 6;
		final int column = width / 2 - 18;
		addToggle(left, 48, column, "gui.advanced.track_artwork", settings::isShowTrackArtwork, settings::setShowTrackArtwork);
		addToggle(right, 48, column, "gui.advanced.track_notifications", settings::isTrackNotifications, settings::setTrackNotifications);
		addToggle(left, 74, column, "gui.advanced.draggable_player", settings::isMiniPlayerDraggable, settings::setMiniPlayerDraggable);
		addToggle(right, 74, column, "gui.advanced.lock_player", settings::isMiniPlayerLocked, settings::setMiniPlayerLocked);
		addToggle(left, 100, column, "gui.advanced.snap_edges", settings::isMiniPlayerSnapToEdges, settings::setMiniPlayerSnapToEdges);
		addToggle(right, 100, column, "gui.advanced.animations", settings::isInterfaceAnimations, settings::setInterfaceAnimations);
		addRenderableWidget(new UButton(left, 126, width - 24, 20, text("gui.advanced.hud_controls"), button ->
				MinecraftGuiCompat.setScreen(minecraft, new GuiMiniPlayerControlsSettings(this))));
		addIntSlider(left, 152, column, "gui.advanced.player_width", " px", 80, 640, settings.getMiniPlayerWidth(), settings::setMiniPlayerWidth);
		addFloatSlider(right, 152, column, "gui.advanced.player_scale", "x", 0.5F, 3F, settings.getMiniPlayerScale(), 2, settings::setMiniPlayerScale);
		addToggle(left, 178, column, "gui.advanced.dynamic_theme", settings::isDynamicTheme, settings::setDynamicTheme);
		addToggle(right, 178, column, "gui.advanced.background_blur", settings::isBackgroundBlur, settings::setBackgroundBlur);
		final UButton visualizer = addRenderableWidget(new UButton(left, 204, column, 20, visualizerLabel(settings)));
		visualizer.setPressable(() -> {
			settings.setVisualizerStyle(VisualizerStyle.next(settings.getVisualizerStyle()));
			visualizer.setMessage(visualizerLabel(settings));
		});
		addRenderableWidget(new UButton(right, 204, column, 20, text("gui.advanced.reset_player"), button -> settings.resetMiniPlayerPlacement()));
		addRenderableWidget(new UButton(left, 230, column, 20, text("gui.advanced.layout_player"), button ->
				MinecraftGuiCompat.setScreen(minecraft, new GuiMiniPlayerLayoutSettings(this))));
		addRenderableWidget(new UButton(right, 230, column, 20, text("gui.advanced.theme_io"), button ->
				MinecraftGuiCompat.setScreen(minecraft, new GuiThemeSettings(this))));
	}

	private void initServices(Settings settings) {
		final int left = 12;
		final int right = width / 2 + 6;
		final int column = width / 2 - 18;
		addToggle(left, 48, column, "gui.advanced.discord", settings::isDiscordRichPresence, settings::setDiscordRichPresence);
		addToggle(right, 48, column, "gui.advanced.lyrics", settings::isLyricsEnabled, settings::setLyricsEnabled);
		addToggle(left, 74, column, "gui.advanced.synced_lyrics", settings::isPreferSyncedLyrics, settings::setPreferSyncedLyrics);
		addIntSlider(right, 74, column, "gui.advanced.artwork_cache", " MB", 32, 4096, settings.getArtworkCacheMegabytes(), settings::setArtworkCacheMegabytes);
		addToggle(left, 100, column, "gui.advanced.online_lyrics", settings::isOnlineLyricsProvider, settings::setOnlineLyricsProvider);
		addRenderableWidget(new UButton(right, 100, column, 20, text("gui.advanced.discord_id"), button ->
				MinecraftGuiCompat.setScreen(minecraft, new GuiDiscordSettings(this))));
		addRenderableWidget(new UButton(left, 126, width - 24, 20, text("gui.advanced.clean_cache"), button ->
				MinecraftGuiCompat.setScreen(minecraft, new GuiArtworkCache(this))));
	}

	private void initPrivacy(Settings settings) {
		final int left = 12;
		final int right = width / 2 + 6;
		final int column = width / 2 - 18;
		addToggle(left, 48, column, "gui.advanced.broadcast", settings::isNearbyMusicBroadcast, settings::setNearbyMusicBroadcast);
		addToggle(right, 48, column, "gui.advanced.receive", settings::isNearbyMusicReceive, settings::setNearbyMusicReceive);
		addToggle(left, 74, column, "gui.advanced.share_title", settings::isShareTrackTitle, settings::setShareTrackTitle);
		addIntSlider(right, 74, column, "gui.advanced.distance", " blocks", 4, 128, settings.getNearbyMusicDistance(), settings::setNearbyMusicDistance);
		addRenderableWidget(new UButton(12, 100, width - 24, 20, text("gui.advanced.shared_listening"), button ->
				MinecraftGuiCompat.setScreen(minecraft, new GuiSharedListening(this))));
		final UButton notice = addRenderableWidget(new UButton(12, 110, width - 24, 20,
				text("gui.advanced.network_notice")));
		notice.setY(128);
		notice.active = false;
	}

	private void addToggle(int x, int y, int width, String key, BooleanSupplier getter, Consumer<Boolean> setter) {
		final UButton button = addRenderableWidget(new UButton(x, y, width, 20, toggleLabel(key, getter.getAsBoolean())));
		button.setPressable(() -> {
			setter.accept(!getter.getAsBoolean());
			button.setMessage(toggleLabel(key, getter.getAsBoolean()));
		});
	}

	private void addIntSlider(int x, int y, int width, String key, String suffix, int min, int max, int value, Consumer<Integer> setter) {
		addRenderableWidget(new USlider(x, y, width, 20, Component.literal(getTranslation(key) + ": "), Component.literal(suffix), min, max, value,
				false, true, slider -> setter.accept(slider.getValueInt())));
	}

	private void addFloatSlider(int x, int y, int width, String key, String suffix, float min, float max, float value,
			int precision, Consumer<Float> setter) {
		final USlider slider = addRenderableWidget(new USlider(x, y, width, 20, Component.literal(getTranslation(key) + ": "), Component.literal(suffix), min, max, value,
				true, true, changed -> setter.accept((float) changed.getValue())));
		slider.setPrecision(precision);
	}

	private void open(Page target) {
		MinecraftGuiCompat.setScreen(minecraft, new GuiMusicPlayerAdvancedSettings(previousScreen, target));
	}

	private static Component toggleLabel(String key, boolean enabled) {
		return Component.literal(getTranslation(key) + ": " + getTranslation(enabled ? "gui.settings.state.on" : "gui.settings.state.off"));
	}

	private static Component equalizerLabel(Settings settings) {
		return Component.literal(getTranslation("gui.advanced.equalizer") + ": " + getTranslation("gui.equalizer.mode." + settings.getEqualizerMode().name().toLowerCase(java.util.Locale.ROOT)));
	}

	private static Component visualizerLabel(Settings settings) {
		return Component.literal(getTranslation("gui.advanced.visualizer") + ": " + getTranslation("gui.visualizer." + settings.getVisualizerStyle().name().toLowerCase(java.util.Locale.ROOT)));
	}

	private static Component text(String key) { return Component.literal(getTranslation(key)); }


	private enum Page {
		AUDIO("gui.advanced.tab.audio"),
		INTERFACE("gui.advanced.tab.interface"),
		SERVICES("gui.advanced.tab.services"),
		PRIVACY("gui.advanced.tab.privacy");

		private final String key;

		Page(String key) {
			this.key = key;
		}
	}
}
