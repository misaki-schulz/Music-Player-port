package info.u_team.music_player.musicplayer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;
import java.time.ZonedDateTime;
import java.net.URI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;

import info.u_team.music_player.lavaplayer.api.IMusicPlayer;
import info.u_team.music_player.lavaplayer.api.audio.IAudioTrack;
import info.u_team.music_player.lavaplayer.api.audio.IAudioTrackInfo;
import info.u_team.music_player.lavaplayer.api.audio.IPlayingTrack;
import info.u_team.music_player.lavaplayer.api.search.ISearchResult;
import info.u_team.music_player.musicplayer.settings.Settings;
import info.u_team.music_player.util.SafeFileStorage;

/** Persists favorites, listening history, ratings, statistics, and the manually resumed session. */
public final class LibraryStateManager implements IGsonLoadable {

	private static final Logger LOGGER = LogManager.getLogger();
	private static final int MAX_HISTORY = 500;
	private static final long WRITE_INTERVAL_NANOS = 5_000_000_000L;

	private final Gson gson;
	private Path path;
	private LibraryState state;
	private String currentUri;
	private long currentPosition;
	private long currentDuration;
	private long lastTickNanos;
	private long lastWriteNanos;
	private boolean dirty;

	LibraryStateManager(Gson gson) {
		this.gson = gson;
	}

	@Override
	public void setBasePath(Path basePath) {
		path = basePath.resolve("library-state.json");
	}

	@Override
	public synchronized void loadFromFile() {
		state = null;
		for (final Path candidate : SafeFileStorage.readCandidates(path)) {
			if (!Files.isRegularFile(candidate)) continue;
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(candidate), StandardCharsets.UTF_8))) {
				state = gson.fromJson(reader, LibraryState.class);
				if (state != null) {
					state.normalize();
					if (!candidate.equals(path)) LOGGER.warn("Recovered music library state from backup {}", candidate);
					break;
				}
			} catch (final IOException | RuntimeException exception) {
				LOGGER.warn("Cannot load music library state candidate {}", candidate, exception);
			}
		}
		if (state == null) state = new LibraryState();
		dirty = true;
		writeToFile();
	}

	@Override
	public synchronized void writeToFile() {
		if (state == null || path == null || !dirty) return;
		try {
			SafeFileStorage.writeAtomically(path, output -> {
				final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
				gson.toJson(state, writer);
				writer.flush();
			});
			dirty = false;
			lastWriteNanos = System.nanoTime();
		} catch (final IOException exception) {
			LOGGER.error("Could not write music library state at {}", path, exception);
		}
	}

	public synchronized void tick(IMusicPlayer player, Settings settings) {
		if (state == null || player == null) return;
		final long now = System.nanoTime();
		final IPlayingTrack track = player.getTrackManager().getCurrentTrack();
		if (track == null || track.getInfo() == null) {
			if (currentUri != null) {
				recordPossibleSkip();
				currentUri = null;
				currentPosition = 0L;
				currentDuration = 0L;
				state.session = null;
				dirty = true;
			}
			lastTickNanos = now;
			writeIfDue(now);
			return;
		}

		final IAudioTrackInfo info = track.getInfo();
		final String uri = stableUri(info);
		if (uri != null && !uri.equals(currentUri)) {
			if (currentUri != null) recordPossibleSkip();
			currentUri = uri;
			recordTrackStart(uri, info, track.getDuration());
			applyTrackPreferences(player, settings, uri);
		}

		if (lastTickNanos > 0 && !player.getTrackManager().isPaused() && uri != null) {
			final long elapsedMillis = Math.min(1000L, Math.max(0L, (now - lastTickNanos) / 1_000_000L));
			state.totalListeningMillis += elapsedMillis;
			state.statistics.computeIfAbsent(uri, ignored -> new TrackStatistics()).listeningMillis += elapsedMillis;
			final ZonedDateTime time = ZonedDateTime.now();
			state.listeningByMonth.merge(monthKey(time), elapsedMillis, Long::sum);
			state.listeningByYear.merge(Integer.toString(time.getYear()), elapsedMillis, Long::sum);
			state.listeningByHour.merge(Integer.toString(time.getHour()), elapsedMillis, Long::sum);
			dirty = true;
		}
		currentPosition = track.getPosition();
		currentDuration = track.getDuration();
		lastTickNanos = now;

		if (settings.isRestoreSession() && uri != null) {
			state.session = new SessionState(uri, track.getPosition(), true);
			dirty = true;
		} else if (!settings.isRestoreSession() && state.session != null) {
			state.session = null;
			dirty = true;
		}
		writeIfDue(now);
	}

	public synchronized void restoreSession(IMusicPlayer player, Settings settings) {
		if (state == null || !settings.isRestoreSession() || state.session == null || state.session.uri == null) return;
		final SessionState session = state.session.copy();
		player.getTrackSearch().getTracks(session.uri, result -> restoreResult(player, session, result));
	}

	private void restoreResult(IMusicPlayer player, SessionState session, ISearchResult result) {
		if (result == null || result.hasError()) return;
		final IAudioTrack track;
		if (!result.isList()) {
			track = result.getTrack();
		} else if (result.getTrackList() == null) {
			track = null;
		} else if (result.getTrackList().getSelectedTrack() != null) {
			track = result.getTrackList().getSelectedTrack();
		} else {
			track = result.getTrackList().getTracks().stream().findFirst().orElse(null);
		}
		if (track == null) return;
		if (!track.getInfo().isStream()) track.setPosition(Math.clamp(session.position, 0L, Math.max(0L, track.getDuration() - 1L)));
		// Intentionally always paused: restoring state is not permission to start making sound.
		player.getTrackManager().prepare(track);
	}

	public synchronized boolean toggleFavorite(IAudioTrackInfo info) {
		final String uri = stableUri(info);
		if (uri == null) return false;
		final boolean favorite;
		if (state.favorites.remove(uri)) favorite = false;
		else { state.favorites.add(uri); favorite = true; }
		dirty = true;
		writeToFile();
		return favorite;
	}

	public synchronized boolean isFavorite(IAudioTrackInfo info) {
		final String uri = stableUri(info);
		return uri != null && state.favorites.contains(uri);
	}

	public synchronized boolean toggleFavoriteUri(String uri) {
		if (uri == null || uri.isBlank()) return false;
		final boolean favorite;
		if (state.favorites.remove(uri)) favorite = false;
		else { state.favorites.add(uri); favorite = true; }
		dirty = true; writeToFile(); return favorite;
	}

	public synchronized boolean isFavoriteUri(String uri) { return uri != null && state.favorites.contains(uri); }

	public synchronized Set<String> getFavorites() {
		return Collections.unmodifiableSet(new LinkedHashSet<>(state.favorites));
	}

	public synchronized List<HistoryEntry> getHistory() {
		return Collections.unmodifiableList(new ArrayList<>(state.history));
	}

	public synchronized Set<String> getRecentUris(int limit) {
		return state.history.stream().limit(Math.max(0, limit)).map(entry -> entry.uri).filter(java.util.Objects::nonNull)
				.collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
	}

	public synchronized void setRating(IAudioTrackInfo info, int rating) {
		final String uri = stableUri(info);
		if (uri == null) return;
		if (rating == 0) state.ratings.remove(uri);
		else state.ratings.put(uri, Math.clamp(rating, -1, 5));
		dirty = true;
		writeToFile();
	}

	public synchronized int getRating(IAudioTrackInfo info) {
		final String uri = stableUri(info);
		return uri == null ? 0 : state.ratings.getOrDefault(uri, 0);
	}

	public synchronized void setRatingUri(String uri, int rating) {
		if (uri == null || uri.isBlank()) return;
		if (rating == 0) state.ratings.remove(uri);
		else state.ratings.put(uri, Math.clamp(rating, -1, 5));
		dirty = true;
		writeToFile();
	}

	public synchronized int getRatingUri(String uri) {
		return uri == null ? 0 : state.ratings.getOrDefault(uri, 0);
	}

	public synchronized TrackPlaybackPreferences getTrackPlaybackPreferences(IAudioTrackInfo info) {
		final String uri = stableUri(info);
		final TrackPlaybackPreferences preferences = uri == null ? null : state.playbackPreferences.get(uri);
		return preferences == null ? null : preferences.copy();
	}

	public synchronized void setTrackPlaybackPreferences(IAudioTrackInfo info, float speed, float pitch) {
		final String uri = stableUri(info);
		if (uri == null) return;
		state.playbackPreferences.put(uri, new TrackPlaybackPreferences(
				Math.clamp(speed, Settings.MIN_SPEED, Settings.MAX_SPEED),
				Math.clamp(pitch, Settings.MIN_PITCH, Settings.MAX_PITCH)));
		dirty = true;
		writeToFile();
	}

	public synchronized void clearTrackPlaybackPreferences(IAudioTrackInfo info, IMusicPlayer player, Settings settings) {
		final String uri = stableUri(info);
		if (uri == null) return;
		state.playbackPreferences.remove(uri);
		if (player != null) {
			player.setSpeed(settings.getSpeed());
			player.setPitch(settings.getPitch());
		}
		dirty = true;
		writeToFile();
	}

	public synchronized long getTotalListeningMillis() {
		return state.totalListeningMillis;
	}

	public synchronized List<LibraryEntry> getUnifiedLibrary() {
		final Map<String, MutableLibraryEntry> entries = new LinkedHashMap<>();
		MusicPlayerManager.getPlaylistManager().getPlaylists().asList().forEach(playlist -> playlist.getUris().forEach(uri -> {
			if (uri != null && !uri.isBlank()) entries.computeIfAbsent(uri, MutableLibraryEntry::new);
		}));
		for (final HistoryEntry history : state.history) {
			if (history.uri == null || history.uri.isBlank()) continue;
			final MutableLibraryEntry entry = entries.computeIfAbsent(history.uri, MutableLibraryEntry::new);
			entry.title = history.title;
			entry.author = history.author;
			entry.artworkUrl = history.artworkUrl;
			entry.duration = Math.max(entry.duration, history.duration);
			entry.lastPlayedEpochMillis = Math.max(entry.lastPlayedEpochMillis, history.playedAtEpochMillis);
		}
		state.statistics.forEach((uri, statistics) -> {
			final MutableLibraryEntry entry = entries.computeIfAbsent(uri, MutableLibraryEntry::new);
			entry.playCount = statistics.playCount;
			entry.listeningMillis = statistics.listeningMillis;
			entry.lastPlayedEpochMillis = Math.max(entry.lastPlayedEpochMillis, statistics.lastPlayedEpochMillis);
			entry.duration = Math.max(entry.duration, statistics.duration);
		});
		entries.forEach((uri, entry) -> { entry.favorite = state.favorites.contains(uri); entry.rating = state.ratings.getOrDefault(uri, 0); });
		return entries.values().stream().map(MutableLibraryEntry::snapshot).toList();
	}

	public synchronized List<LibraryEntry> getMostPlayed(int limit) {
		return getUnifiedLibrary().stream().sorted(Comparator.comparingLong(LibraryEntry::playCount).reversed()
				.thenComparing(Comparator.comparingLong(LibraryEntry::listeningMillis).reversed())).limit(Math.max(0, limit)).toList();
	}

	public synchronized WrappedStatistics getWrappedStatistics() {
		final ZonedDateTime now = ZonedDateTime.now();
		return new WrappedStatistics(state.totalListeningMillis,
				state.listeningByMonth.getOrDefault(monthKey(now), 0L),
				state.listeningByYear.getOrDefault(Integer.toString(now.getYear()), 0L),
				state.statistics.values().stream().mapToLong(value -> value.playCount).sum(), state.skips,
				state.statistics.size(), topKey(state.playsByArtist, "Unknown artist"),
				topKey(state.playsBySource, "Unknown source"), topHour(state.listeningByHour));
	}

	public synchronized List<String> getAchievements() {
		final List<String> achievements = new ArrayList<>();
		final long plays = state.statistics.values().stream().mapToLong(value -> value.playCount).sum();
		if (plays >= 1) achievements.add("First play");
		if (state.totalListeningMillis >= 60L * 60_000L) achievements.add("One hour listener");
		if (plays >= 100) achievements.add("Century of tracks");
		if (state.totalListeningMillis >= 24L * 60L * 60_000L) achievements.add("A day of music");
		if (state.favorites.size() >= 10) achievements.add("Collector: 10 favorites");
		if (state.ratings.values().stream().filter(value -> value > 0).count() >= 25) achievements.add("Music critic");
		return List.copyOf(achievements);
	}

	public synchronized void shutdown(IMusicPlayer player, Settings settings) {
		if (player != null) tick(player, settings);
		writeToFile();
	}

	private void recordTrackStart(String uri, IAudioTrackInfo info, long duration) {
		state.history.removeIf(entry -> uri.equals(entry.uri));
		state.history.add(0, new HistoryEntry(uri, info.getFixedTitle(), info.getFixedAuthor(), info.getArtworkURL(), System.currentTimeMillis(), Math.max(0L, duration)));
		while (state.history.size() > MAX_HISTORY) state.history.remove(state.history.size() - 1);
		final TrackStatistics statistics = state.statistics.computeIfAbsent(uri, ignored -> new TrackStatistics());
		statistics.playCount++;
		statistics.lastPlayedEpochMillis = System.currentTimeMillis();
		statistics.duration = Math.max(statistics.duration, duration);
		final ZonedDateTime time = ZonedDateTime.now();
		state.playsByArtist.merge(nonBlank(info.getFixedAuthor(), "Unknown artist"), 1L, Long::sum);
		state.playsBySource.merge(sourceOf(uri), 1L, Long::sum);
		state.playsByMonth.merge(monthKey(time), 1L, Long::sum);
		state.playsByYear.merge(Integer.toString(time.getYear()), 1L, Long::sum);
		dirty = true;
	}

	private void recordPossibleSkip() {
		if (currentUri == null || currentDuration < 30_000L) return;
		final long completedThreshold = Math.min(currentDuration - 5000L, Math.round(currentDuration * 0.80D));
		if (currentPosition >= Math.max(0L, completedThreshold)) return;
		state.skips++;
		state.statistics.computeIfAbsent(currentUri, ignored -> new TrackStatistics()).skips++;
		dirty = true;
	}

	private void applyTrackPreferences(IMusicPlayer player, Settings settings, String uri) {
		final TrackPlaybackPreferences preferences = state.playbackPreferences.get(uri);
		player.setSpeed(preferences == null ? settings.getSpeed() : preferences.speed);
		player.setPitch(preferences == null ? settings.getPitch() : preferences.pitch);
	}

	private void writeIfDue(long now) {
		if (dirty && now - lastWriteNanos >= WRITE_INTERVAL_NANOS) writeToFile();
	}

	private static String stableUri(IAudioTrackInfo info) {
		if (info == null) return null;
		if (info.getURI() != null && !info.getURI().isBlank()) return info.getURI();
		return info.getIdentifier() == null || info.getIdentifier().isBlank() ? null : info.getIdentifier();
	}

	private static String monthKey(ZonedDateTime time) { return "%04d-%02d".formatted(time.getYear(), time.getMonthValue()); }
	private static String nonBlank(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
	private static String sourceOf(String uri) {
		if (uri == null || uri.isBlank()) return "Unknown source";
		try { final URI parsed = URI.create(uri); if (parsed.getHost() != null) return parsed.getHost().toLowerCase(java.util.Locale.ROOT); if (parsed.getScheme() != null) return parsed.getScheme().toLowerCase(java.util.Locale.ROOT); }
		catch (final IllegalArgumentException ignored) { }
		return "Local / identifier";
	}
	private static String topKey(Map<String, Long> values, String fallback) { return values.entrySet().stream().max(Map.Entry.comparingByValue()).map(Map.Entry::getKey).orElse(fallback); }
	private static int topHour(Map<String, Long> values) { return values.entrySet().stream().max(Map.Entry.comparingByValue()).map(entry -> { try { return Integer.parseInt(entry.getKey()); } catch (final NumberFormatException ignored) { return -1; } }).orElse(-1); }

	private static final class LibraryState {
		private List<HistoryEntry> history = new ArrayList<>();
		private Set<String> favorites = new LinkedHashSet<>();
		private Map<String, Integer> ratings = new LinkedHashMap<>();
		private Map<String, TrackStatistics> statistics = new LinkedHashMap<>();
		private Map<String, TrackPlaybackPreferences> playbackPreferences = new LinkedHashMap<>();
		private Map<String, Long> listeningByMonth = new LinkedHashMap<>();
		private Map<String, Long> listeningByYear = new LinkedHashMap<>();
		private Map<String, Long> listeningByHour = new LinkedHashMap<>();
		private Map<String, Long> playsByMonth = new LinkedHashMap<>();
		private Map<String, Long> playsByYear = new LinkedHashMap<>();
		private Map<String, Long> playsByArtist = new LinkedHashMap<>();
		private Map<String, Long> playsBySource = new LinkedHashMap<>();
		private SessionState session;
		private long totalListeningMillis;
		private long skips;

		private void normalize() {
			if (history == null) history = new ArrayList<>();
			if (favorites == null) favorites = new LinkedHashSet<>();
			if (ratings == null) ratings = new LinkedHashMap<>();
			if (statistics == null) statistics = new LinkedHashMap<>();
			if (playbackPreferences == null) playbackPreferences = new LinkedHashMap<>();
			if (listeningByMonth == null) listeningByMonth = new LinkedHashMap<>();
			if (listeningByYear == null) listeningByYear = new LinkedHashMap<>();
			if (listeningByHour == null) listeningByHour = new LinkedHashMap<>();
			if (playsByMonth == null) playsByMonth = new LinkedHashMap<>();
			if (playsByYear == null) playsByYear = new LinkedHashMap<>();
			if (playsByArtist == null) playsByArtist = new LinkedHashMap<>();
			if (playsBySource == null) playsBySource = new LinkedHashMap<>();
			playbackPreferences.entrySet().removeIf(entry -> entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null);
			playbackPreferences.values().forEach(TrackPlaybackPreferences::normalize);
			while (history.size() > MAX_HISTORY) history.remove(history.size() - 1);
		}
	}

	public static final class HistoryEntry {
		public String uri;
		public String title;
		public String author;
		public String artworkUrl;
		public long playedAtEpochMillis;
		public long duration;

		private HistoryEntry(String uri, String title, String author, String artworkUrl, long playedAtEpochMillis, long duration) {
			this.uri = uri;
			this.title = title;
			this.author = author;
			this.artworkUrl = artworkUrl;
			this.playedAtEpochMillis = playedAtEpochMillis;
			this.duration = duration;
		}
	}

	public record LibraryEntry(String uri, String title, String author, String artworkUrl, long playCount,
			long listeningMillis, long lastPlayedEpochMillis, boolean favorite, int rating, long duration) {
		public String displayName() {
			final String fixedTitle = title == null || title.isBlank() ? uri : title;
			return author == null || author.isBlank() ? fixedTitle : fixedTitle + " — " + author;
		}
		public boolean isLocal() {
			if (uri == null) return false;
			final String lower = uri.toLowerCase(java.util.Locale.ROOT);
			return lower.startsWith("file:") || (!lower.startsWith("http://") && !lower.startsWith("https://"));
		}
		public String sourceLabel() { return sourceOf(uri); }
	}

	private static final class MutableLibraryEntry {
		private final String uri;
		private String title, author, artworkUrl;
		private long playCount, listeningMillis, lastPlayedEpochMillis, duration;
		private boolean favorite;
		private int rating;
		private MutableLibraryEntry(String uri) { this.uri = uri; }
		private LibraryEntry snapshot() { return new LibraryEntry(uri,title,author,artworkUrl,playCount,listeningMillis,lastPlayedEpochMillis,favorite,rating,duration); }
	}

	private static final class TrackStatistics {
		private long playCount;
		private long listeningMillis;
		private long lastPlayedEpochMillis;
		private long skips;
		private long duration;
	}

	public record WrappedStatistics(long totalListeningMillis, long currentMonthListeningMillis,
			long currentYearListeningMillis, long totalPlays, long skips, int uniqueTracks,
			String topArtist, String topSource, int mostActiveHour) { }

	public static final class TrackPlaybackPreferences {
		public float speed;
		public float pitch;

		private TrackPlaybackPreferences(float speed, float pitch) { this.speed = speed; this.pitch = pitch; }
		private void normalize() {
			if (!Float.isFinite(speed) || speed < Settings.MIN_SPEED || speed > Settings.MAX_SPEED) speed = 1F;
			if (!Float.isFinite(pitch) || pitch < Settings.MIN_PITCH || pitch > Settings.MAX_PITCH) pitch = 1F;
		}
		private TrackPlaybackPreferences copy() { return new TrackPlaybackPreferences(speed, pitch); }
	}

	private static final class SessionState {
		private String uri;
		private long position;
		@SuppressWarnings("unused")
		private boolean paused;

		private SessionState(String uri, long position, boolean paused) {
			this.uri = uri;
			this.position = position;
			this.paused = paused;
		}

		private SessionState copy() { return new SessionState(uri, position, true); }
	}
}
