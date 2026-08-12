package info.u_team.music_player.gui.playlist;

import static info.u_team.music_player.init.MusicPlayerLocalization.getTranslation;

import java.nio.file.Path;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import org.lwjgl.util.tinyfd.TinyFileDialogs;
import info.u_team.music_player.musicplayer.playlist.Playlist;
import info.u_team.music_player.musicplayer.playlist.PlaylistImportExport;

/** Native file dialogs kept outside the render thread. */
public final class PlaylistFileDialogs {

	private static final ExecutorService DIALOG_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
		final Thread thread = new Thread(runnable, "Music Player file dialog");
		thread.setDaemon(true);
		return thread;
	});

	private PlaylistFileDialogs() {
	}

	public static void chooseImport(Consumer<Path> callback) {
		DIALOG_EXECUTOR.execute(() -> accept(TinyFileDialogs.tinyfd_openFileDialog(
				getTranslation("gui.files.import_title"), "", null, getTranslation("gui.files.playlist_filter"), false), callback));
	}

	public static void chooseExport(String suggestedName, Consumer<Path> callback) {
		DIALOG_EXECUTOR.execute(() -> accept(TinyFileDialogs.tinyfd_saveFileDialog(
				getTranslation("gui.files.export_title"), suggestedName, null, getTranslation("gui.files.playlist_filter")), callback));
	}

	public static void importLibrary(Runnable onSuccess, Consumer<String> status) {
		chooseImport(path -> DIALOG_EXECUTOR.execute(() -> {
			try {
				final int count = PlaylistImportExport.importFile(path);
				Minecraft.getInstance().execute(() -> {
					status.accept(getTranslation("gui.files.imported", count));
					onSuccess.run();
				});
			} catch (final IOException exception) {
				Minecraft.getInstance().execute(() -> status.accept(getTranslation("gui.files.import_failed", exception.getMessage())));
			}
		}));
	}

	public static void exportLibrary(Consumer<String> status) {
		chooseExport("music-player-playlists.json", path -> DIALOG_EXECUTOR.execute(() -> {
			try {
				PlaylistImportExport.exportLibraryJson(ensureExtension(path, ".json"));
				Minecraft.getInstance().execute(() -> status.accept(getTranslation("gui.files.library_exported")));
			} catch (final IOException exception) {
				Minecraft.getInstance().execute(() -> status.accept(getTranslation("gui.files.export_failed", exception.getMessage())));
			}
		}));
	}

	public static void exportM3u(Playlist playlist, Consumer<String> status) {
		final String safeName = playlist.getName() == null ? "playlist" : playlist.getName().replaceAll("[\\\\/:*?\"<>|]", "_");
		chooseExport(safeName + ".m3u8", path -> DIALOG_EXECUTOR.execute(() -> {
			try {
				PlaylistImportExport.exportM3u(playlist, ensureExtension(path, ".m3u8"));
				Minecraft.getInstance().execute(() -> status.accept(getTranslation("gui.files.playlist_exported")));
			} catch (final IOException exception) {
				Minecraft.getInstance().execute(() -> status.accept(getTranslation("gui.files.export_failed", exception.getMessage())));
			}
		}));
	}

	private static Path ensureExtension(Path path, String extension) {
		return path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(extension) ? path
				: path.resolveSibling(path.getFileName() + extension);
	}

	private static void accept(String selected, Consumer<Path> callback) {
		if (selected != null && !selected.isBlank()) Minecraft.getInstance().execute(() -> callback.accept(Path.of(selected)));
	}

	public static void shutdown() {
		DIALOG_EXECUTOR.shutdownNow();
	}
}
