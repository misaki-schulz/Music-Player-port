package info.u_team.music_player.lavaplayer.sources;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;

import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.http.YoutubeOauth2Handler;
import info.u_team.music_player.lavaplayer.api.search.ISearchResult;
import info.u_team.music_player.lavaplayer.search.TrackSearch;

/** Offline regression: real Lavaplayer dispatch and TrackSearch, fake network sources. */
public final class SourceRoutingRegression {

	private static final List<String> SAVED_URLS = List.of(
			"https://www.youtube.com/watch?v=UL_Wnh5kPLI",
			"https://www.youtube.com/watch?v=PCDX2qw35T0",
			"https://youtu.be/UL_Wnh5kPLI",
			"https://www.youtube.com/playlist?list=PL_saved_playlist");

	public static void main(String[] args) throws Exception {
		// Control: reproduce port.7's late YouTube registration and exact error.
		DefaultAudioPlayerManager manager = new DefaultAudioPlayerManager();
		try {
			FakeYoutube youtube = new FakeYoutube();
			manager.registerSourceManager(new FakeHttp(youtube));
			manager.registerSourceManager(youtube);
			youtube.useOauth2("test-token", true);
			ISearchResult result = load(manager, SAVED_URLS.getFirst());
			check(result.hasError() && "Unknown file format.".equals(result.getErrorMessage()),
					"Old source order must reproduce Unknown file format");
		} finally {
			manager.shutdown();
		}

		// New player instances model a restart/version switch using the same saved URLs.
		for (int restart = 0; restart < 3; restart++) {
			manager = new DefaultAudioPlayerManager();
			try {
				FakeYoutube youtube = new FakeYoutube();
				YoutubeAuth auth = new YoutubeAuth(manager, youtube);
				FakeHttp http = new FakeHttp(youtube);
				manager.registerSourceManager(http);
				check(manager.getSourceManagers().getFirst() == youtube, "YouTube must precede HTTP");
				check(!auth.isEnabled(), "Registration must not pretend OAuth succeeded");
				ISearchResult pending = load(manager, SAVED_URLS.getFirst());
				check(pending.hasError() && pending.getErrorMessage().equals("YouTube sign-in required"),
						"YouTube failures during restore must not fall through to HTTP");
				youtube.acceptToken = false;
				check(!auth.enable("test-token"), "Rejected OAuth token must remain disabled");
				check(manager.getSourceManagers().size() == 2, "Failed auth must not change registration");
				youtube.acceptToken = true;
				check(auth.enable("test-token") && auth.isEnabled(), "Saved OAuth token must enable the source");
				check(auth.enable("test-token"), "Repeated enable must be safe");
				check(manager.getSourceManagers().size() == 2, "Login must not duplicate source managers");
				for (String uri : SAVED_URLS) {
					ISearchResult result = load(manager, uri);
					check(!result.hasError(), "Saved URL failed after restart: " + uri);
					check(uri.equals(result.getUri()), "Saved URL must be retained");
				}
				check(!load(manager, "ytsearch:test").hasError(), "YouTube search must still work");
				check(!load(manager, "ytmsearch:test").hasError(), "YouTube Music search must still work");
				check(http.youtubeProbes == 0, "HTTP must never probe a YouTube page");
				check(!load(manager, "https://example.test/music.mp3").hasError(), "Direct audio must reach HTTP");
				check(http.directLoads == 1, "Direct HTTP fallback must remain available");
			} finally {
				manager.shutdown();
			}
		}
		System.out.println("PASS: old-order reproduction; 3 fresh players; saved links/playlists; OAuth restore/retry; source ownership; HTTP fallback");
	}

	private static ISearchResult load(AudioPlayerManager manager, String uri) throws Exception {
		CompletableFuture<ISearchResult> result = new CompletableFuture<>();
		new TrackSearch(manager).getTracks(uri, result::complete);
		return result.get(5, TimeUnit.SECONDS);
	}

	private static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}

	private static boolean isYoutube(String uri) {
		return uri.startsWith("https://www.youtube.com/") || uri.startsWith("https://youtu.be/")
				|| uri.startsWith("ytsearch:") || uri.startsWith("ytmsearch:");
	}

	private static final class FakeYoutube extends YoutubeAudioSourceManager {
		private boolean ready;
		private boolean acceptToken = true;
		private final YoutubeOauth2Handler oauth = new YoutubeOauth2Handler(null) {
			@Override public boolean hasAccessToken() { return ready; }
		};

		@Override public void useOauth2(String token, boolean skipInitialization) { ready = acceptToken; }
		@Override public YoutubeOauth2Handler getOauth2Handler() { return oauth; }

		@Override
		public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
			if (!isYoutube(reference.identifier)) return null;
			if (!ready) throw new FriendlyException("YouTube sign-in required", FriendlyException.Severity.COMMON, null);
			var track = buildAudioTrack(new AudioTrackInfo("Saved song", "Artist", 1000,
					"UL_Wnh5kPLI", false, reference.identifier));
			if (reference.identifier.contains("playlist?") || reference.identifier.startsWith("yt")) {
				return new BasicAudioPlaylist("Saved playlist", List.of(track), null, reference.identifier.startsWith("yt"));
			}
			return track;
		}
	}

	private static final class FakeHttp extends HttpAudioSourceManager {
		private final FakeYoutube youtube;
		private int youtubeProbes;
		private int directLoads;
		FakeHttp(FakeYoutube youtube) { this.youtube = youtube; }

		@Override
		public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
			if (isYoutube(reference.identifier)) {
				youtubeProbes++;
				throw new FriendlyException("Unknown file format.", FriendlyException.Severity.COMMON, null);
			}
			if (reference.identifier.equals("https://example.test/music.mp3")) {
				directLoads++;
				return youtube.buildAudioTrack(new AudioTrackInfo("Direct audio", "Artist", 1000,
						"direct", false, reference.identifier));
			}
			return null;
		}
	}
}
