package info.u_team.music_player;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.google.gson.Gson;
import info.u_team.music_player.lavaplayer.api.audio.IAudioTrack;
import info.u_team.music_player.lavaplayer.api.audio.IAudioTrackInfo;
import info.u_team.music_player.lavaplayer.api.audio.IAudioTrackList;
import info.u_team.music_player.lavaplayer.api.search.ISearchResult;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.musicplayer.playlist.LoadedTracks;
import info.u_team.music_player.musicplayer.playlist.Playlist;
import info.u_team.music_player.musicplayer.playlist.PlaylistSort;
import info.u_team.music_player.musicplayer.playlist.Skip;
import info.u_team.music_player.util.NaturalOrder;
import info.u_team.music_player.util.OrderedTrackLoader;
import info.u_team.music_player.util.WrappedObject;

public final class SortingRegression {

	private static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}

	public static void main(String[] args) throws Exception {
		naturalOrder();
		orderedLoading();
		playlistSorting();
		System.out.println("PASS: natural folder order, async completion/error/cancellation, all six sorts, stable ties, duplicate URIs, serialization/reload, playback and grouped playlists");
	}

	private static void naturalOrder() throws Exception {
		check(NaturalOrder.compare("Track 2", "track 10") < 0, "numeric order");
		check(NaturalOrder.compare("track 02", "TRACK 2") == 0, "stable equivalent names");
		check(NaturalOrder.compare("9".repeat(40), "1" + "0".repeat(40)) < 0, "large numbers without overflow");
		check(NaturalOrder.compare("Песня 2", "песня 10") < 0, "Unicode case");
		final LoadedTracks unknown = new LoadedTracks(new WrappedObject<>("file:///C:/music/2%20song.mp3"), track("file:///C:/music/2%20song.mp3", null, null));
		final LoadedTracks known = new LoadedTracks(new WrappedObject<>("C:\\music\\10 song.mp3"), track("C:\\music\\10 song.mp3", "10 song.mp3", "Artist"));
		check(PlaylistSort.TITLE_ASCENDING.getComparator().compare(unknown, known) < 0, "missing title falls back to decoded file name");
		check(PlaylistSort.AUTHOR_ASCENDING.getComparator().compare(unknown, known) < 0, "null author is sortable");
		final Random random = new Random(9);
		final List<String> samples = new ArrayList<>(Arrays.asList("", "0", "00", "a0", "a00", "A", "a", "a1", "a01", "a01b"));
		for (int i = 0; i < 100; i++) samples.add((i % 2 == 0 ? "a" : "B") + random.nextInt(50) + "x" + random.nextInt(50));
		for (String a : samples) for (String b : samples) {
			check(Integer.signum(NaturalOrder.compare(a, b)) == -Integer.signum(NaturalOrder.compare(b, a)), "antisymmetry");
			for (String c : samples) if (NaturalOrder.compare(a, b) <= 0 && NaturalOrder.compare(b, c) <= 0)
				check(NaturalOrder.compare(a, c) <= 0, "transitivity");
		}
		final Path folder = Files.createTempDirectory("musicplayer-sort-");
		try {
			for (String name : List.of("Track 10.mp3", "track 2.mp3", "Track 1.mp3")) Files.createFile(folder.resolve(name));
			Files.createDirectory(folder.resolve("Track 0.mp3"));
			try (var stream = Files.list(folder)) {
				final List<String> names = stream.filter(Files::isRegularFile)
						.sorted(Comparator.<Path, String>comparing(path -> path.getFileName().toString(), NaturalOrder::compare).thenComparing(Path::toString))
						.map(path -> path.getFileName().toString()).toList();
				check(names.equals(List.of("Track 1.mp3", "track 2.mp3", "Track 10.mp3")), "folder order and directory filtering");
			}
		} finally {
			try (var stream = Files.list(folder)) { for (Path path : stream.toList()) Files.delete(path); }
			Files.delete(folder);
		}
	}

	private static void orderedLoading() {
		final Map<String, Consumer<ISearchResult>> callbacks = new HashMap<>();
		final ArrayDeque<Runnable> deliveries = new ArrayDeque<>();
		final List<String> seen = new ArrayList<>();
		final int[] finished = { 0 };
		final OrderedTrackLoader loader = new OrderedTrackLoader(callbacks::put, deliveries::add);
		final Consumer<ISearchResult> output = result -> seen.add(result.getUri());
		loader.load(List.of("1", "2", "10"), output, () -> finished[0]++);
		callbacks.get("10").accept(result("10", null, null, false));
		drain(deliveries);
		check(seen.isEmpty() && loader.isLoading(), "must wait for earlier files");
		callbacks.get("1").accept(result("1", null, null, true));
		drain(deliveries);
		check(seen.equals(List.of("1")), "failed file keeps its position without blocking following files");
		callbacks.get("2").accept(result("2", null, null, false));
		check(seen.equals(List.of("1")), "delivery thread boundary");
		drain(deliveries);
		check(seen.equals(List.of("1", "2", "10")) && finished[0] == 1 && !loader.isLoading(), "async result order");
		callbacks.get("2").accept(result("2", null, null, false));
		drain(deliveries);
		check(finished[0] == 1 && seen.size() == 3, "duplicate callback ignored");
		loader.load(List.of("old"), output, () -> finished[0]++);
		callbacks.get("old").accept(result("old", null, null, false));
		loader.load(List.of("new"), output, () -> finished[0]++);
		drain(deliveries);
		check(seen.size() == 3 && loader.isLoading(), "queued stale callback ignored");
		callbacks.get("new").accept(result("new", null, null, false));
		drain(deliveries);
		check(seen.get(3).equals("new") && finished[0] == 2, "replacement search");
		loader.load(List.of("closed"), output, () -> finished[0]++);
		loader.cancel();
		callbacks.get("closed").accept(result("closed", null, null, false));
		drain(deliveries);
		check(seen.size() == 4 && !loader.isLoading(), "closed screen ignores results");
		loader.load(List.of(), output, () -> finished[0]++);
		check(finished[0] == 3 && !loader.isLoading(), "empty folder completes");
	}

	private static void drain(ArrayDeque<Runnable> queue) {
		while (!queue.isEmpty()) queue.remove().run();
	}

	private static void load(Playlist playlist) throws Exception {
		final CountDownLatch ready = new CountDownLatch(1);
		playlist.load(ready::countDown);
		check(ready.await(3, TimeUnit.SECONDS), "playlist load timeout");
	}

	private static void playlistSorting() throws Exception {
		final IAudioTrack one = track("C:\\music\\10.mp3", "Song 1", "Artist 10");
		final IAudioTrack two = track("file:///C:/music/2%20song.mp3", "song 2", "Artist 2");
		final IAudioTrack ten = track("C:\\music\\1.mp3", "Song 10", "Artist 1");
		final Playlist playlist = new Playlist("Test");
		MusicPlayerManager.saving = playlist;
		check(!playlist.sort(PlaylistSort.TITLE_ASCENDING), "cannot sort unloaded entries");
		load(playlist);
		playlist.add(ten);
		playlist.add(two);
		playlist.add(one);
		final Map<String, ISearchResult> lookup = new HashMap<>();
		for (IAudioTrack track : List.of(one, two, ten)) lookup.put(track.getInfo().getURI(), result(track.getInfo().getURI(), track, null, false));
		MusicPlayerManager.search = (uri, consumer) -> consumer.accept(lookup.getOrDefault(uri, result(uri, null, null, true)));
		final Map<PlaylistSort, List<IAudioTrack>> expected = Map.of(
				PlaylistSort.TITLE_ASCENDING, List.of(one, two, ten), PlaylistSort.TITLE_DESCENDING, List.of(ten, two, one),
				PlaylistSort.AUTHOR_ASCENDING, List.of(ten, two, one), PlaylistSort.AUTHOR_DESCENDING, List.of(one, two, ten),
				PlaylistSort.FILE_ASCENDING, List.of(ten, two, one), PlaylistSort.FILE_DESCENDING, List.of(one, two, ten));
		for (PlaylistSort order : PlaylistSort.values()) {
			final int writes = MusicPlayerManager.writes;
			check(playlist.sort(order), "sort " + order);
			check(playlist.getLoadedTracks().stream().map(LoadedTracks::getTrack).toList().equals(expected.get(order)), "order " + order);
			check(MusicPlayerManager.writes == writes + 1, "saved once per sort");
			checkPairing(playlist);
			final Playlist restored = new Gson().fromJson(MusicPlayerManager.saved, Playlist.class);
			try {
				load(restored);
				check(restored.getLoadedTracks().stream().map(LoadedTracks::getTrack).toList().equals(expected.get(order)), "saved order after reload " + order);
			} finally { restored.shutdown(); }
		}
		playlist.sort(PlaylistSort.TITLE_ASCENDING);
		final LoadedTracks playing = playlist.getLoadedTracks().stream().filter(entry -> entry.getTrack() == two).findFirst().orElseThrow();
		playlist.setPlayable(playing, two);
		playlist.sort(PlaylistSort.TITLE_DESCENDING);
		check(playlist.getNext() == two, "sorting keeps current playback object");
		check(playlist.skip(Skip.FORWARD) && playlist.getNext() == one, "next song follows sorted order");
		final IAudioTrack duplicate = track(one.getInfo().getURI(), "song 1", "Artist 10");
		final WrappedObject<String> originalUri = playlist.uris.stream().filter(uri -> uri.get().equals(one.getInfo().getURI())).findFirst().orElseThrow();
		final WrappedObject<String> duplicateUri = playlist.add(duplicate);
		playlist.sort(PlaylistSort.TITLE_ASCENDING);
		check(playlist.uris.indexOf(originalUri) < playlist.uris.indexOf(duplicateUri), "stable ties and distinct duplicate URIs");
		playlist.sort(PlaylistSort.TITLE_DESCENDING);
		check(playlist.uris.indexOf(originalUri) < playlist.uris.indexOf(duplicateUri), "descending remains stable");
		checkPairing(playlist);
		final List<IAudioTrack> nestedTracks = new ArrayList<>(List.of(ten, one, two));
		final IAudioTrackList nested = new IAudioTrackList() {
			public String getName() { return "Album"; }
			public List<IAudioTrack> getTracks() { return nestedTracks; }
			public IAudioTrack getSelectedTrack() { return null; }
			public boolean isSearch() { return false; }
			public boolean hasUri() { return true; }
			public String getUri() { return "https://example.test/playlist"; }
		};
        playlist.add(nested);
        lookup.put(nested.getUri(), result(nested.getUri(), null, nested, false));
		playlist.sort(PlaylistSort.AUTHOR_ASCENDING);
		check(nested.getTracks().equals(List.of(ten, one, two)), "nested playlist stays grouped and unchanged");
		checkPairing(playlist);
		playlist.uris.add(new WrappedObject<>("C:\\missing\\0.mp3"));
		playlist.unload();
		load(playlist);
		for (PlaylistSort order : PlaylistSort.values()) {
			check(playlist.sort(order), "failed/nested entries sort " + order);
			checkPairing(playlist);
		}
		check(playlist.getLoadedTracks().stream().anyMatch(LoadedTracks::hasError), "missing files retained");
		check(playlist.getLoadedTracks().stream().anyMatch(entry -> entry.isTrackList() && entry.getTrackList() == nested), "nested list remains grouped after reload");
		playlist.shutdown();
	}

	private static void checkPairing(Playlist playlist) {
		final List<LoadedTracks> entries = new ArrayList<>(playlist.getLoadedTracks());
		check(playlist.uris.size() == entries.size(), "same entry count");
		for (int i = 0; i < entries.size(); i++) check(playlist.uris.get(i) == entries.get(i).getUri(), "URI/loaded object identity");
	}

	private static IAudioTrack track(String uri, String title, String author) {
		final IAudioTrackInfo info = new IAudioTrackInfo() {
			public String getTitle() { return title; }
			public String getAuthor() { return author; }
			public String getIdentifier() { return uri; }
			public String getURI() { return uri; }
			public boolean isStream() { return false; }
			public String getFixedTitle() { return title; }
			public String getFixedAuthor() { return author; }
		};
		return new IAudioTrack() {
			public IAudioTrackInfo getInfo() { return info; }
			public long getPosition() { return 0; }
			public void setPosition(long value) {}
			public long getDuration() { return 1000; }
		};
	}

	private static ISearchResult result(String uri, IAudioTrack track, IAudioTrackList list, boolean error) {
		return new ISearchResult() {
			public String getUri() { return uri; }
			public boolean isList() { return list != null; }
			public IAudioTrackList getTrackList() { return list; }
			public IAudioTrack getTrack() { return track; }
			public boolean hasError() { return error; }
			public String getErrorMessage() { return "Unreadable file"; }
			public StackTraceElement[] getStackTrace() { return new StackTraceElement[0]; }
		};
	}
}
