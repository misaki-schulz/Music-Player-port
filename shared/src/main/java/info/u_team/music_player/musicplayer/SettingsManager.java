// Modified for Minecraft 26.2 by misaki-schulz; see NOTICE.
package info.u_team.music_player.musicplayer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

import info.u_team.music_player.musicplayer.settings.Settings;
import info.u_team.music_player.util.SafeFileStorage;

public class SettingsManager implements IGsonLoadable {

	private static final Logger LOGGER = LogManager.getLogger();
	private final Gson gson;
	private Path path;
	private Settings settings;

	SettingsManager(Gson gson) {
		this.gson = gson;
	}

	@Override
	public void setBasePath(Path basePath) {
		path = basePath.resolve("settings.json");
	}

	@Override
	public synchronized void loadFromFile() {
		settings = null;
		for (final Path candidate : SafeFileStorage.readCandidates(path)) {
			if (!Files.exists(candidate)) {
				continue;
			}
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(candidate), StandardCharsets.UTF_8))) {
				final Settings loaded = gson.fromJson(reader, Settings.class);
				if (loaded != null) {
					settings = loaded;
					if (!candidate.equals(path)) {
						LOGGER.warn("Recovered music player settings from backup {}", candidate);
					}
					break;
				}
			} catch (final IOException | RuntimeException exception) {
				LOGGER.warn("Cannot load music player settings candidate {}", candidate, exception);
			}
		}
		if (settings == null) {
			settings = new Settings();
		}
		settings.normalize();
		writeToFile();
	}

	@Override
	public synchronized void writeToFile() {
		if (settings == null || path == null) {
			return;
		}
		try {
			SafeFileStorage.writeAtomically(path, output -> {
				final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
				gson.toJson(settings, writer);
				writer.flush();
			});
		} catch (final IOException exception) {
			LOGGER.error("Could not write music player settings at {}", path, exception);
		}
	}

	public Settings getSettings() {
		return settings;
	}
}
