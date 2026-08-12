// Modified for Minecraft 26.2 by misaki-schulz; see NOTICE.
package info.u_team.music_player.init;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.musicplayer.settings.MusicPlayerLanguage;
import info.u_team.music_player.musicplayer.settings.Settings;
import net.minecraft.client.resources.language.I18n;

public class MusicPlayerLocalization {

	private static final Logger LOGGER = LogManager.getLogger();
	private static final Map<MusicPlayerLanguage, Map<String, String>> TRANSLATIONS = loadTranslations();
	
	// Keys
	public static final String KEY_CATEGORY = "key.musicplayer.category";
	public static final String KEY_OPEN = "key.musicplayer.open";
	public static final String KEY_PAUSE = "key.musicplayer.pause";
	public static final String KEY_SKIP_FORWARD = "key.musicplayer.skip.forward";
	public static final String KEY_SKIP_BACK = "key.musicplayer.skip.back";
	public static final String KEY_HUD_CONTROLS = "key.musicplayer.hud_controls";
	
	// Guis
	public static final String GUI_CREATE_PLAYLIST_INSERT_NAME = "gui.musicplayer.create_playlist.insert_name";
	public static final String GUI_CREATE_PLAYLIST_ADD_LIST = "gui.musicplayer.create_playlist.add_list";
	
	public static final String GUI_PLAYLISTS_NO_NAME = "gui.musicplayer.playlists.no_name";
	public static final String GUI_PLAYLISTS_ENTRY = "gui.musicplayer.playlists.entry";
	public static final String GUI_PLAYLISTS_ENTRIES = "gui.musicplayer.playlists.entries";
	
	public static final String GUI_CONTROLS_VOLUME = "gui.controls.volume";
	
	public static final String GUI_PLAYLIST_LOADING = "gui.playlist.loading";
	public static final String GUI_PLAYLIST_FILTER = "gui.playlist.filter";
	public static final String GUI_PLAYLIST_REORDER_HINT = "gui.playlist.reorder_hint";
	
	public static final String GUI_TRACK_DURATION_UNDEFINED = "gui.track.duration_undefined";
	
	public static final String GUI_SEARCH_HEADER = "gui.search.header";
	public static final String GUI_SEARCH_LOAD_FILE = "gui.search.load.file";
	public static final String GUI_SEARCH_LOAD_FOLDER = "gui.search.load.folder";
	public static final String GUI_SEARCH_MUSIC_FILES = "gui.search.music_files";
	public static final String GUI_SEARCH_ADD_ALL = "gui.search.add_all";
	public static final String GUI_SEARCH_ADDED = "gui.search.added";
	public static final String GUI_SEARCH_ADDED_LIST = "gui.search.added_list";
	public static final String GUI_SEARCH_ADDED_ALL = "gui.search.added_all";
	public static final String GUI_SEARCH_SEARCH_URI = "gui.search.search.uri";
	public static final String GUI_SEARCH_SEARCH_FILE = "gui.search.search.file";
	public static final String GUI_SEARCH_SEARCH_SEARCH = "gui.search.search.search";
	
	public static final String GUI_SETTINGS_TOGGLE_INGAME_OVERLAY = "gui.settings.toggle.ingame_overlay";
	public static final String GUI_SETTINGS_TOGGLE_MENUE_OVERLAY = "gui.settings.toggle.menue_overlay";
	public static final String GUI_SETTINGS_STATE_ON = "gui.settings.state.on";
	public static final String GUI_SETTINGS_STATE_OFF = "gui.settings.state.off";
	public static final String GUI_SETTINGS_TOGGLE_KEY_IN_GUI = "gui.settings.toggle.key_in_gui";
	public static final String GUI_SETTINGS_POSITION_OVERLAY = "gui.settings.position.overlay";
	public static final String GUI_SETTINGS_POSITION_UP_LEFT = "gui.settings.position.up_left";
	public static final String GUI_SETTINGS_POSITION_UP_RIGHT = "gui.settings.position.up_right";
	public static final String GUI_SETTINGS_POSITION_DOWN_RIGHT = "gui.settings.position.down_right";
	public static final String GUI_SETTINGS_POSITION_DOWN_LEFT = "gui.settings.position.down_left";
	public static final String GUI_SETTINGS_SPEED = "gui.settings.speed";
	public static final String GUI_SETTINGS_PITCH = "gui.settings.pitch";
	public static final String GUI_SETTINGS_RESET = "gui.settings.reset";
	public static final String GUI_SETTINGS_LANGUAGE = "gui.settings.language";
	public static final String GUI_SETTINGS_OVERLAY_SCALE = "gui.settings.overlay_scale";
	public static final String GUI_SETTINGS_MIXER_DEVICE_SELECTION = "gui.settings.mixer_device_selection";
	
	public static String getTranslation(String key, Object... parameters) {
		final MusicPlayerLanguage language = getSelectedLanguage();
		final String translation = TRANSLATIONS.getOrDefault(language, Collections.emptyMap()).get(key);
		final String fallback = TRANSLATIONS.getOrDefault(MusicPlayerLanguage.ENGLISH, Collections.emptyMap()).get(key);
		final String value = translation != null ? translation : fallback != null ? fallback : I18n.get(key);
		if (parameters.length == 0) {
			return value;
		}
		try {
			return String.format(Locale.ROOT, value, parameters);
		} catch (final RuntimeException ex) {
			LOGGER.warn("Cannot format music player translation {}", key, ex);
			return value;
		}
	}

	private static MusicPlayerLanguage getSelectedLanguage() {
		try {
			final Settings settings = MusicPlayerManager.getSettingsManager().getSettings();
			if (settings != null && settings.getLanguage() != null) {
				return settings.getLanguage();
			}
		} catch (final RuntimeException ignored) {
		}
		return MusicPlayerLanguage.ENGLISH;
	}

	private static Map<MusicPlayerLanguage, Map<String, String>> loadTranslations() {
		final Map<MusicPlayerLanguage, Map<String, String>> translations = new EnumMap<>(MusicPlayerLanguage.class);
		for (final MusicPlayerLanguage language : MusicPlayerLanguage.values()) {
			translations.put(language, loadTranslation(language));
		}
		return Collections.unmodifiableMap(translations);
	}

	private static Map<String, String> loadTranslation(MusicPlayerLanguage language) {
		final String path = "assets/musicplayer/lang/" + language.getCode() + ".json";
		try (final InputStream stream = MusicPlayerLocalization.class.getClassLoader().getResourceAsStream(path)) {
			if (stream == null) {
				throw new IllegalStateException("Missing language resource " + path);
			}
			try (final InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
				final JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
				final Map<String, String> values = new HashMap<>();
				for (final Map.Entry<String, JsonElement> entry : json.entrySet()) {
					values.put(entry.getKey(), entry.getValue().getAsString());
				}
				return Collections.unmodifiableMap(values);
			}
		} catch (final Exception ex) {
			LOGGER.error("Cannot load music player language {}", language.getCode(), ex);
			return Collections.emptyMap();
		}
	}
	
}
