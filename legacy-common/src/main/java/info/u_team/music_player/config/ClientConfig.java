package info.u_team.music_player.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import info.u_team.music_player.MusicPlayerMod;
import info.u_team.music_player.util.SafeFileStorage;
import net.fabricmc.loader.api.FabricLoader;

public class ClientConfig {

	private static volatile ClientConfig INSTANCE = new ClientConfig();

	public static ClientConfig getInstance() {
		return INSTANCE;
	}

	public boolean internalPlaylists = false;

	public static void load() {
		final Path path = FabricLoader.getInstance().getConfigDir().resolve("musicplayer.json");
		final Gson gson = new GsonBuilder().setPrettyPrinting().create();
		ClientConfig config = null;
		for (final Path candidate : SafeFileStorage.readCandidates(path)) {
			if (!Files.exists(candidate)) {
				continue;
			}
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(candidate), StandardCharsets.UTF_8))) {
				config = gson.fromJson(reader, ClientConfig.class);
				if (config != null) {
					if (!candidate.equals(path)) {
						MusicPlayerMod.LOGGER.warn("Recovered music player client config from {}", candidate);
					}
					break;
				}
			} catch (final IOException | RuntimeException exception) {
				MusicPlayerMod.LOGGER.warn("Cannot load music player config candidate {}", candidate, exception);
			}
		}
		if (config == null) {
			config = new ClientConfig();
		}
		INSTANCE = config;
		write(path, gson, config);
	}

	private static void write(Path path, Gson gson, ClientConfig config) {
		try {
			SafeFileStorage.writeAtomically(path, output -> {
				final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
				gson.toJson(config, writer);
				writer.flush();
			});
		} catch (final IOException exception) {
			MusicPlayerMod.LOGGER.error("Could not write music player config at {}", path, exception);
		}
	}
}
