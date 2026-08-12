package info.u_team.music_player.musicplayer.playlist;

import java.util.Locale;

import info.u_team.music_player.lavaplayer.api.audio.IAudioTrack;
import info.u_team.music_player.lavaplayer.api.audio.IAudioTrackInfo;

public final class TrackFilter {

	private TrackFilter() {
	}

	public static boolean matches(IAudioTrack track, String query) {
		if (query == null || query.isBlank()) return true;
		if (track == null || track.getInfo() == null) return false;
		final String needle = query.strip().toLowerCase(Locale.ROOT);
		final IAudioTrackInfo info = track.getInfo();
		return contains(info.getFixedTitle(), needle) || contains(info.getFixedAuthor(), needle)
				|| contains(info.getURI(), needle) || contains(info.getIdentifier(), needle);
	}

	public static boolean matches(String value, String query) {
		return query == null || query.isBlank() || contains(value, query.strip().toLowerCase(Locale.ROOT));
	}

	private static boolean contains(String value, String needle) {
		return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
	}
}
