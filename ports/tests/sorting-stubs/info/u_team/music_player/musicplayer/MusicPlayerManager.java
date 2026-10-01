package info.u_team.music_player.musicplayer;

import com.google.gson.Gson;
import info.u_team.music_player.lavaplayer.api.search.ITrackSearch;
import info.u_team.music_player.musicplayer.playlist.Playlist;
import info.u_team.music_player.musicplayer.playlist.Playlists;
import info.u_team.music_player.musicplayer.settings.Settings;

/** Minecraft-free boundary for testing the production playlist and serialization. */
public final class MusicPlayerManager {
	public static ITrackSearch search;
	public static Playlist saving;
	public static String saved;
	public static int writes;
	public static final Playlists lists = new Playlists();
	private static final Player PLAYER = new Player();
	private static final PlaylistManager PLAYLISTS = new PlaylistManager();
	private static final SettingsManager SETTINGS = new SettingsManager();
	public static Player getPlayer() { return PLAYER; }
	public static PlaylistManager getPlaylistManager() { return PLAYLISTS; }
	public static SettingsManager getSettingsManager() { return SETTINGS; }
	public static final class Player {
		public Auth getYoutubeAuth() { return new Auth(); }
		public ITrackSearch getTrackSearch() { return search; }
	}
	public static final class Auth {
		public boolean isEnabled() { return false; }
	}
	public static final class PlaylistManager {
		public Playlists getPlaylists() { return lists; }
		public void writeToFile() {
			writes++;
			if (saving != null) saved = new Gson().toJson(saving);
		}
	}
	public static final class SettingsManager {
		public Settings getSettings() { return new Settings(); }
	}
}
