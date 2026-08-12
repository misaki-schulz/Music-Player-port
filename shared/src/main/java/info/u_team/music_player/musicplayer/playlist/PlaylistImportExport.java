package info.u_team.music_player.musicplayer.playlist;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.util.SafeFileStorage;

public final class PlaylistImportExport {

	private static final int MAX_IMPORT_BYTES = 8 * 1024 * 1024;
	private static final int MAX_PLAYLISTS = 1000;
	private static final int MAX_TRACKS = 100_000;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private PlaylistImportExport() {
	}

	public static void exportLibraryJson(Path destination) throws IOException {
		final ExportBundle bundle = new ExportBundle();
		for (final Playlist playlist : MusicPlayerManager.getPlaylistManager().getPlaylists()) {
			bundle.playlists.add(new ExportPlaylist(playlist.getName(), playlist.getUris()));
		}
		SafeFileStorage.writeAtomically(destination, output -> {
			final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
			GSON.toJson(bundle, writer);
			writer.flush();
		});
	}

	public static void exportM3u(Playlist playlist, Path destination) throws IOException {
		SafeFileStorage.writeAtomically(destination, output -> {
			final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
			writer.write("#EXTM3U");
			writer.newLine();
			for (final String uri : playlist.getUris()) {
				writer.write(uri.replace("\r", "").replace("\n", ""));
				writer.newLine();
			}
			writer.flush();
		});
	}

	public static int importFile(Path source) throws IOException {
		if (!Files.isRegularFile(source) || Files.size(source) > MAX_IMPORT_BYTES) throw new IOException("Import file is missing or exceeds 8 MiB");
		final String name = source.getFileName().toString().toLowerCase(Locale.ROOT);
		return name.endsWith(".m3u") || name.endsWith(".m3u8") ? importM3u(source) : importJson(source);
	}

	private static int importJson(Path source) throws IOException {
		final ExportBundle bundle;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(source), StandardCharsets.UTF_8))) {
			bundle = GSON.fromJson(reader, ExportBundle.class);
		} catch (final RuntimeException exception) {
			throw new IOException("Invalid Music Player JSON", exception);
		}
		if (bundle == null || bundle.playlists == null) throw new IOException("JSON contains no playlists");
		int imported = 0;
		int totalTracks = 0;
		for (final ExportPlaylist entry : bundle.playlists.stream().limit(MAX_PLAYLISTS).toList()) {
			if (entry == null || entry.uris == null) continue;
			totalTracks += entry.uris.size();
			if (totalTracks > MAX_TRACKS) throw new IOException("Import contains more than " + MAX_TRACKS + " tracks");
			final Playlist playlist = new Playlist(safeName(entry.name, "Imported playlist"));
			playlist.replaceUris(entry.uris);
			MusicPlayerManager.getPlaylistManager().getPlaylists().add(playlist);
			imported++;
		}
		return imported;
	}

	private static int importM3u(Path source) throws IOException {
		final List<String> uris = new ArrayList<>();
		try (BufferedReader reader = Files.newBufferedReader(source, StandardCharsets.UTF_8)) {
			String line;
			while ((line = reader.readLine()) != null) {
				final String value = line.strip();
				if (!value.isEmpty() && !value.startsWith("#")) {
					if (value.length() > 8192) throw new IOException("M3U entry is too long");
					uris.add(value);
					if (uris.size() > MAX_TRACKS) throw new IOException("M3U contains too many tracks");
				}
			}
		}
		final String fileName = source.getFileName().toString().replaceFirst("(?i)\\.m3u8?$", "");
		final Playlist playlist = new Playlist(safeName(fileName, "Imported M3U"));
		playlist.replaceUris(uris);
		MusicPlayerManager.getPlaylistManager().getPlaylists().add(playlist);
		return 1;
	}

	private static String safeName(String value, String fallback) {
		if (value == null || value.isBlank()) return fallback;
		return value.strip().substring(0, Math.min(200, value.strip().length()));
	}

	private static final class ExportBundle {
		private int formatVersion = 1;
		private List<ExportPlaylist> playlists = new ArrayList<>();
	}

	private static final class ExportPlaylist {
		private String name;
		private List<String> uris;

		private ExportPlaylist(String name, List<String> uris) {
			this.name = name;
			this.uris = uris;
		}
	}
}
