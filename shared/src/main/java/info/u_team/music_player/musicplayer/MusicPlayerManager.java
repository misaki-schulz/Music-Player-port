// Shared lifecycle manager for every supported Minecraft target; see NOTICE.
package info.u_team.music_player.musicplayer;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import info.u_team.music_player.artwork.ArtworkRepository;
import info.u_team.music_player.gui.playlist.PlaylistFileDialogs;
import info.u_team.music_player.gui.playlist.ArtworkOverrideDialogs;
import info.u_team.music_player.gui.settings.ThemeFileDialogs;
import info.u_team.music_player.gui.playlist.TrackCardDialogs;
import info.u_team.music_player.audio.AudioVisualizer;
import info.u_team.music_player.lavaplayer.api.IMusicPlayer;
import info.u_team.music_player.musicplayer.playlist.Playlist;
import info.u_team.music_player.musicplayer.settings.Settings;
import info.u_team.music_player.integration.DiscordRichPresence;
import info.u_team.music_player.integration.LyricsService;
import info.u_team.music_player.audio.TrackTransitionController;
import info.u_team.music_player.integration.SharedListeningService;
import info.u_team.music_player.audio.DynamicArtworkThemeController;
import info.u_team.music_player.audio.TrackNotificationController;

public final class MusicPlayerManager {

	private static final Logger LOGGER = LogManager.getLogger();
	private static IMusicPlayer player;
	private static boolean shutDown;
	private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	private static final MusicPlayerFiles files = new MusicPlayerFiles();
	private static final PlaylistManager playlistManager = new PlaylistManager(gson);
	private static final SettingsManager settingsManager = new SettingsManager(gson);
	private static final LibraryStateManager libraryStateManager = new LibraryStateManager(gson);
	private static final EqualizerPresetManager equalizerPresetManager = new EqualizerPresetManager(gson);
	private static final AudioVisualizer audioVisualizer = new AudioVisualizer();
	private static final SleepTimerManager sleepTimerManager = new SleepTimerManager();
	private static final ABRepeatManager abRepeatManager = new ABRepeatManager();
	private static final DiscordRichPresence discordRichPresence = new DiscordRichPresence();
	private static final ThemeManager themeManager = new ThemeManager(gson);
	private static final TrackTransitionController trackTransitionController = new TrackTransitionController();
	private static final SharedListeningService sharedListeningService = new SharedListeningService();
	private static final DynamicArtworkThemeController dynamicArtworkThemeController = new DynamicArtworkThemeController();
	private static final TrackNotificationController trackNotificationController = new TrackNotificationController();

	private MusicPlayerManager() {
	}

	static void setup(ClassLoader classLoader, boolean internalPlaylists) {
		shutDown = false;
		fixLogLevel(classLoader);
		generatePlayer(classLoader);
		files.load(internalPlaylists);
		playlistManager.setBasePath(files.getDirectory());
		settingsManager.setBasePath(files.getDirectory());
		libraryStateManager.setBasePath(files.getDirectory());
		equalizerPresetManager.setBasePath(files.getDirectory());
		themeManager.setBasePath(files.getDirectory());
		playlistManager.loadFromFile();
		settingsManager.loadFromFile();
		libraryStateManager.loadFromFile();
		equalizerPresetManager.loadFromFile();
		themeManager.load();

		final Settings settings = settingsManager.getSettings();
		player.setVolume(settings.getVolume());
		player.setMixer(settings.getMixer());
		player.setAutomaticAudioRecovery(settings.isAutomaticAudioRecovery());
		player.setSpeed(settings.getSpeed());
		player.setPitch(settings.getPitch());
		player.setEqualizer(settings.getEqualizerMode() != info.u_team.music_player.musicplayer.settings.EqualizerMode.OFF,
				settings.getEqualizerGains(), settings.getEqualizerPositions(), settings.isBassBoost());
		player.setChannelMix(settings.isMonoOutput(), settings.getChannelBalance(), settings.isSwapChannels());
		player.startAudioOutput();
		updateVisualizer(settings);
		libraryStateManager.restoreSession(player, settings);
	}

	private static void fixLogLevel(ClassLoader classLoader) {
		final String loggerName = "org.apache.http";
		final LoggerContext context = (LoggerContext) LogManager.getContext(classLoader, false);
		context.getConfiguration().addLogger(loggerName, new LoggerConfig(loggerName, Level.INFO, true));
	}

	private static void generatePlayer(ClassLoader classLoader) {
		try {
			final Class<?> clazz = Class.forName("info.u_team.music_player.lavaplayer.MusicPlayer", true, classLoader);
			if (!IMusicPlayer.class.isAssignableFrom(clazz)) throw new IllegalAccessError("The player implementation does not implement IMusicPlayer");
			player = (IMusicPlayer) clazz.getDeclaredConstructor().newInstance();
			LOGGER.info("Successfully created music player instance");
		} catch (final Exception exception) {
			throw new IllegalStateException("Cannot create music player instance. Report this to the mod authors", exception);
		}
	}

	public static void tick() {
		if (!shutDown && player != null) {
			libraryStateManager.tick(player, settingsManager.getSettings());
			sleepTimerManager.tick(player);
			abRepeatManager.tick(player);
			discordRichPresence.tick(player, settingsManager.getSettings());
			trackTransitionController.tick(player, settingsManager.getSettings());
			sharedListeningService.tick(player, settingsManager.getSettings());
			dynamicArtworkThemeController.tick(player, settingsManager.getSettings());
			trackNotificationController.tick(player, settingsManager.getSettings());
		}
	}

	public static void updateVisualizer(Settings settings) {
		if (player != null) player.setOutputConsumer(settings.getVisualizerStyle() == info.u_team.music_player.musicplayer.settings.VisualizerStyle.OFF ? null : audioVisualizer);
	}

	public static synchronized void shutdown() {
		if (shutDown) return;
		shutDown = true;
		libraryStateManager.shutdown(player, settingsManager.getSettings());
		Playlist.shutdownExecutor();
		ArtworkRepository.shutdown();
		PlaylistFileDialogs.shutdown();
		ArtworkOverrideDialogs.shutdown();
		ThemeFileDialogs.shutdown();
		TrackCardDialogs.shutdown();
		discordRichPresence.shutdown();
		LyricsService.shutdown();
		sharedListeningService.shutdown();
		if (player != null) {
			try {
				player.shutdown();
			} catch (final RuntimeException exception) {
				LOGGER.warn("Cannot cleanly shut down music player", exception);
			} finally {
				player = null;
			}
		}
	}

	public static IMusicPlayer getPlayer() { return player; }
	public static MusicPlayerFiles getFiles() { return files; }
	public static PlaylistManager getPlaylistManager() { return playlistManager; }
	public static SettingsManager getSettingsManager() { return settingsManager; }
	public static LibraryStateManager getLibraryStateManager() { return libraryStateManager; }
	public static EqualizerPresetManager getEqualizerPresetManager() { return equalizerPresetManager; }
	public static AudioVisualizer getAudioVisualizer() { return audioVisualizer; }
	public static SleepTimerManager getSleepTimerManager() { return sleepTimerManager; }
	public static ABRepeatManager getABRepeatManager() { return abRepeatManager; }
	public static DiscordRichPresence getDiscordRichPresence() { return discordRichPresence; }
	public static ThemeManager getThemeManager() { return themeManager; }
	public static SharedListeningService getSharedListeningService() { return sharedListeningService; }
}
