package info.u_team.music_player.musicplayer.playlist;

import java.net.URI;
import java.util.Comparator;
import java.util.function.Function;

import info.u_team.music_player.util.NaturalOrder;

public enum PlaylistSort {
	TITLE_ASCENDING("title_ascending", PlaylistSort::title, false),
	TITLE_DESCENDING("title_descending", PlaylistSort::title, true),
	AUTHOR_ASCENDING("author_ascending", PlaylistSort::author, false),
	AUTHOR_DESCENDING("author_descending", PlaylistSort::author, true),
	FILE_ASCENDING("file_ascending", PlaylistSort::file, false),
	FILE_DESCENDING("file_descending", PlaylistSort::file, true);

	private final String translationKey;
	private final Comparator<LoadedTracks> comparator;

	PlaylistSort(String key, Function<LoadedTracks, String> value, boolean descending) {
		translationKey = "gui.playlist.sort." + key;
		final Comparator<LoadedTracks> order = (left, right) -> NaturalOrder.compare(value.apply(left), value.apply(right));
		comparator = descending ? order.reversed() : order;
	}

	public String getTranslationKey() {
		return translationKey;
	}

	public Comparator<LoadedTracks> getComparator() {
		return comparator;
	}

	private static String title(LoadedTracks entry) {
		final String title = entry.getTitle();
		return title == null || title.isBlank() ? file(entry) : title;
	}

	private static String author(LoadedTracks entry) {
		return entry.isTrack() ? entry.getTrack().getInfo().getAuthor() : "";
	}

	private static String file(LoadedTracks entry) {
		final String uri = entry.getUri().get();
		if (uri == null) return "";
		String path = uri;
		if (uri.regionMatches(true, 0, "file:", 0, 5)) {
			try {
				path = URI.create(uri).getPath();
				if (path == null) return uri;
			} catch (final IllegalArgumentException ignored) {
				// Also accept raw local paths containing spaces or backslashes.
			}
		} else if (uri.contains("://")) {
			return uri;
		}
		path = path.replace('\\', '/');
		return path.substring(path.lastIndexOf('/') + 1);
	}
}
