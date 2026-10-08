package info.u_team.music_player.util;

import java.util.List;
import java.util.Locale;

import info.u_team.music_player.lavaplayer.api.audio.IAudioTrack;
import info.u_team.music_player.musicplayer.playlist.LoadedTracks;

/** A view of saved tracks; never changes the playlist or playback queue. */
public final class TitleFilter {

	private final String query;

	public TitleFilter(String query) {
		this.query = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
	}

	public boolean isActive() {
		return !query.isEmpty();
	}

	public boolean matches(String title) {
		return !isActive() || title != null && title.toLowerCase(Locale.ROOT).contains(query);
	}

	public boolean matches(IAudioTrack track) {
		return track != null && matches(track.getInfo().getFixedTitle());
	}

	public List<IAudioTrack> tracks(LoadedTracks loaded) {
		if (loaded.isTrack()) {
			return matches(loaded.getTrack()) ? List.of(loaded.getTrack()) : List.of();
		}
		if (loaded.isTrackList()) {
			return loaded.getTrackList().getTracks().stream().filter(this::matches).toList();
		}
		return List.of();
	}
}
