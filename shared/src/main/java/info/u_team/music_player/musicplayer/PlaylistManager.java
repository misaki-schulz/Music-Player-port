package info.u_team.music_player.musicplayer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

import info.u_team.music_player.musicplayer.playlist.Playlists;
import info.u_team.music_player.util.SafeFileStorage;

public class PlaylistManager implements IGsonLoadable {

	private static final Logger LOGGER = LogManager.getLogger();
	private final Gson gson;
	private Path path;
	private Playlists playlists;

	PlaylistManager(Gson gson) {
		this.gson = gson;
	}

	@Override
	public void setBasePath(Path basePath) {
		path = basePath.resolve("playlist.json.gz");
	}

	@Override
	public synchronized void loadFromFile() {
		playlists = null;
		for (final Path candidate : SafeFileStorage.readCandidates(path)) {
			if (!Files.exists(candidate)) {
				continue;
			}
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(Files.newInputStream(candidate)), StandardCharsets.UTF_8))) {
				final Playlists loaded = gson.fromJson(reader, Playlists.class);
				if (loaded != null) {
					playlists = loaded;
					if (!candidate.equals(path)) {
						LOGGER.warn("Recovered music player playlists from backup {}", candidate);
					}
					break;
				}
			} catch (final IOException | RuntimeException exception) {
				LOGGER.warn("Cannot load music player playlist candidate {}", candidate, exception);
			}
		}
		if (playlists == null) {
			playlists = new Playlists();
		}
		writeToFile();
	}

	@Override
	public synchronized void writeToFile() {
		if (playlists == null || path == null) {
			return;
		}
		try {
			SafeFileStorage.writeAtomically(path, output -> {
				try (GZIPOutputStream gzip = new GZIPOutputStream(output)) {
					final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(gzip, StandardCharsets.UTF_8));
					gson.toJson(playlists, writer);
					writer.flush();
				}
			});
		} catch (final IOException exception) {
			LOGGER.error("Could not write music player playlists at {}", path, exception);
		}
	}

	public Playlists getPlaylists() {
		return playlists;
	}
}
