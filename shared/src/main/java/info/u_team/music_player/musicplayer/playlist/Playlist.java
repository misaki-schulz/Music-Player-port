package info.u_team.music_player.musicplayer.playlist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Function;
import java.util.function.IntPredicate;

import org.apache.commons.lang3.tuple.Pair;

import info.u_team.music_player.lavaplayer.api.audio.IAudioTrack;
import info.u_team.music_player.lavaplayer.api.audio.IAudioTrackList;
import info.u_team.music_player.lavaplayer.api.queue.ITrackQueue;
import info.u_team.music_player.lavaplayer.api.search.ITrackSearch;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.musicplayer.settings.Settings;
import info.u_team.music_player.musicplayer.settings.ShuffleMode;
import info.u_team.music_player.util.WrappedObject;

/**
 * This class represents a playlist. This list can be serialized or deserialized. After a serialization the tracks must
 * be loaded, because only the uris are saved. {@link IAudioTrack} and {@link IAudioTrackList} can be added. Tracks can
 * be removed. Tracks can be moved in the order. Any changes to the serializable fields are saved
 *
 * @author HyCraftHD
 */
public class Playlist implements ITrackQueue {

	private static final ExecutorService LOAD_EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
		final Thread thread = new Thread(runnable, "Music Player playlist loader");
		thread.setDaemon(true);
		return thread;
	});
	private static final ScheduledExecutorService LOAD_TIMEOUT_EXECUTOR = Executors.newSingleThreadScheduledExecutor(runnable -> {
		final Thread thread = new Thread(runnable, "Music Player playlist retry timer");
		thread.setDaemon(true);
		return thread;
	});
	private static final int MAX_LOAD_ATTEMPTS = 3;
	private static final long LOAD_TIMEOUT_SECONDS = 15;
	
	// Used in gson serialization and deserialization
	public String name;
	public final ArrayList<WrappedObject<String>> uris;
	
	// Should not be serialized or deserialized
	private transient volatile boolean loaded;
	private transient boolean loading;
	private transient long loadGeneration;
	private transient ArrayList<LoadedTracks> loadedTracks;
	private transient ArrayList<Runnable> loadCallbacks;
	
	/**
	 * Only used for gson deserialization
	 */
	@SuppressWarnings("unused")
	private Playlist() {
		uris = new ArrayList<>();
		initializeTransientState();
	}
	
	/**
	 * Create a new playlist object with a name
	 *
	 * @param name The playlist's name
	 */
	public Playlist(String name) {
		this.name = name;
		uris = new ArrayList<>();
		initializeTransientState();
	}

	private void initializeTransientState() {
		loadedTracks = new ArrayList<>();
		loadCallbacks = new ArrayList<>();
	}
	
	/**
	 * Loads this playlist. This will go through all uris and search with {@link ITrackSearch} for the {@link IAudioTrack}
	 * and {@link IAudioTrackList} for {@link LoadedTracks}. This method is async.
	 */
	public void load() {
		load(() -> {
		});
	}
	
	/**
	 * Loads this playlist. This will go through all uris and search with {@link ITrackSearch} for the {@link IAudioTrack}
	 * and {@link IAudioTrackList} for {@link LoadedTracks}. This method is async. This method calls the
	 * {@link Runnable#run()} method when everything is loaded and the playlist was not loaded before.
	 *
	 * @param runnable A runnable that should be executed when the playlist is loaded
	 */
	public void load(Runnable runnable) {
		final List<WrappedObject<String>> uriSnapshot;
		final long generation;
		boolean runImmediately = false;
		synchronized (this) {
			ensureTransientState();
			if (loaded) {
				runImmediately = true;
				uriSnapshot = null;
				generation = 0;
			} else if (loading) {
				loadCallbacks.add(runnable);
				return;
			} else {
				loading = true;
				generation = ++loadGeneration;
				loadCallbacks.add(runnable);
				uriSnapshot = new ArrayList<>(uris);
			}
		}
		if (runImmediately) {
			runnable.run();
			return;
		}
		LOAD_EXECUTOR.execute(() -> loadSnapshot(uriSnapshot, generation));
	}

	private void loadSnapshot(List<WrappedObject<String>> uriSnapshot, long generation) {
		if (uriSnapshot.isEmpty()) {
			completeLoad(uriSnapshot, new AtomicReferenceArray<>(0), generation);
			return;
		}
		final ITrackSearch search = MusicPlayerManager.getPlayer().getTrackSearch();
		final AtomicReferenceArray<LoadedTracks> results = new AtomicReferenceArray<>(uriSnapshot.size());
		final AtomicInteger remaining = new AtomicInteger(uriSnapshot.size());
		for (int index = 0; index < uriSnapshot.size(); index++) {
			loadSlot(search, uriSnapshot, results, remaining, generation, new LoadSlot(index, uriSnapshot.get(index)));
		}
	}

	private void loadSlot(ITrackSearch search, List<WrappedObject<String>> uriSnapshot, AtomicReferenceArray<LoadedTracks> results,
			AtomicInteger remaining, long generation, LoadSlot slot) {
		final int token;
		synchronized (slot) {
			if (slot.finished) return;
			slot.attempt++;
			token = ++slot.token;
		}
		LOAD_TIMEOUT_EXECUTOR.schedule(() -> failAttempt(search, uriSnapshot, results, remaining, generation, slot, token,
				"Track loading timed out after " + LOAD_TIMEOUT_SECONDS + " seconds"), LOAD_TIMEOUT_SECONDS, TimeUnit.SECONDS);
		try {
			search.getTracks(slot.uri.get(), result -> {
				if (result != null && !result.hasError()) finishAttempt(uriSnapshot, results, remaining, generation, slot, token, new LoadedTracks(slot.uri, result));
				else failAttempt(search, uriSnapshot, results, remaining, generation, slot, token,
						result == null ? "Track source returned no result" : result.getErrorMessage());
			});
		} catch (final RuntimeException exception) {
			failAttempt(search, uriSnapshot, results, remaining, generation, slot, token, exception.getMessage());
		}
	}

	private void failAttempt(ITrackSearch search, List<WrappedObject<String>> uriSnapshot, AtomicReferenceArray<LoadedTracks> results,
			AtomicInteger remaining, long generation, LoadSlot slot, int token, String error) {
		final boolean retry;
		synchronized (slot) {
			if (slot.finished || slot.token != token) return;
			slot.token++;
			retry = slot.attempt < MAX_LOAD_ATTEMPTS;
			if (!retry) slot.finished = true;
		}
		if (retry) {
			LOAD_TIMEOUT_EXECUTOR.schedule(() -> loadSlot(search, uriSnapshot, results, remaining, generation, slot), slot.attempt, TimeUnit.SECONDS);
		} else {
			acceptLoadedTrack(uriSnapshot, results, remaining, generation, slot.index,
					new LoadedTracks(slot.uri, (error == null || error.isBlank()) ? "Track could not be loaded after 3 attempts" : error));
		}
	}

	private void finishAttempt(List<WrappedObject<String>> uriSnapshot, AtomicReferenceArray<LoadedTracks> results,
			AtomicInteger remaining, long generation, LoadSlot slot, int token, LoadedTracks loadedTracks) {
		synchronized (slot) {
			if (slot.finished || slot.token != token) return;
			slot.finished = true;
			slot.token++;
		}
		acceptLoadedTrack(uriSnapshot, results, remaining, generation, slot.index, loadedTracks);
	}

	private void acceptLoadedTrack(List<WrappedObject<String>> uriSnapshot, AtomicReferenceArray<LoadedTracks> results, AtomicInteger remaining, long generation, int index, LoadedTracks result) {
		if (results.compareAndSet(index, null, result) && remaining.decrementAndGet() == 0) {
			completeLoad(uriSnapshot, results, generation);
		}
	}

	private void completeLoad(List<WrappedObject<String>> uriSnapshot, AtomicReferenceArray<LoadedTracks> results, long generation) {
		final List<Runnable> callbacks;
		synchronized (this) {
			ensureTransientState();
			if (!loading || generation != loadGeneration || !uris.equals(uriSnapshot)) {
				return;
			}
			loadedTracks.clear();
			for (int index = 0; index < results.length(); index++) {
				loadedTracks.add(results.get(index));
			}
			loaded = true;
			loading = false;
			callbacks = new ArrayList<>(loadCallbacks);
			loadCallbacks.clear();
		}
		callbacks.forEach(Runnable::run);
	}

	private void ensureTransientState() {
		if (loadedTracks == null) {
			initializeTransientState();
		}
	}
	
	/**
	 * Unloads this playlist and removes all loaded tracks.
	 */
	public synchronized void unload() {
		ensureTransientState();
		loadGeneration++;
		loadedTracks.clear();
		loaded = false;
		loading = false;
		loadCallbacks.clear();
	}
	
	/**
	 * Is this playlist loaded
	 *
	 * @return Playlist loaded
	 */
	public boolean isLoaded() {
		return loaded;
	}
	
	/**
	 * Adds an {@link IAudioTrack} to the uri list and the loaded tracks. This playlist must be loaded.
	 *
	 * @param track The track that should be added
	 * @return The {@link WrappedObject} with the uri as a string
	 */
	public synchronized WrappedObject<String> add(IAudioTrack track) {
		ensureTransientState();
		if (!loaded) {
			return null;
		}
		final WrappedObject<String> uri = new WrappedObject<>(track.getInfo().getURI());
		final int index = uris.size();
		uris.add(index, uri);
		loadedTracks.add(index, new LoadedTracks(uri, track));
		save();
		return uri;
	}
	
	/**
	 * Adds an {@link IAudioTrackList} to the uri list and the loaded tracks if it has a valid uri and is not a search
	 * result. This playlist must be loaded.
	 *
	 * @param trackList The tracklist that should be added
	 * @return The {@link WrappedObject} with the uri as a string
	 */
	public synchronized WrappedObject<String> add(IAudioTrackList trackList) {
		ensureTransientState();
		if (!loaded) {
			return null;
		}
		if (!trackList.isSearch() && trackList.hasUri()) {
			final WrappedObject<String> uri = new WrappedObject<>(trackList.getUri());
			final int index = uris.size();
			uris.add(index, uri);
			loadedTracks.add(index, new LoadedTracks(uri, trackList));
			save();
			return uri;
		}
		return null;
	}

	/** Adds a raw source URI without blocking on metadata resolution. The next open/load resolves it normally. */
	public synchronized WrappedObject<String> addUri(String value) {
		if (value == null || value.isBlank()) return null;
		final WrappedObject<String> uri = new WrappedObject<>(value.strip());
		uris.add(uri);
		unload();
		save();
		return uri;
	}
	
	/**
	 * Removes an uri from the uri list and the loaded tracks. This playlist must be loaded.
	 *
	 * @param uri The {@link WrappedObject} with the uri as a string
	 * @return If the uri was removed
	 */
	public synchronized boolean remove(WrappedObject<String> uri) {
		ensureTransientState();
		if (!loaded) {
			return false;
		}
		final int index = uris.indexOf(uri);
		if (index >= 0) {
			uris.remove(index);
			loadedTracks.remove(index);
			save();
			return true;
		}
		return false;
	}
	
	/**
	 * Move the uri and loaded track in the list up or down. This playlist must be loaded.
	 *
	 * @param uri The {@link WrappedObject} with the uri as a string
	 * @param value Positive value to move the uri up the value, and the other way around for a negative value
	 * @return If move was successful
	 */
	public synchronized boolean move(WrappedObject<String> uri, int value) {
		ensureTransientState();
		if (!loaded) {
			return false;
		}
		final int oldIndex = uris.indexOf(uri);
		final int newIndex = oldIndex - value;
		if (newIndex >= 0 && newIndex < uris.size()) {
			uris.add(newIndex, uris.remove(oldIndex));
			loadedTracks.add(newIndex, loadedTracks.remove(oldIndex));
			save();
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Sets the name of this playlist
	 *
	 * @param name Name
	 */
	public void setName(String name) {
		this.name = name;
		save();
	}
	
	/**
	 * Gets the name of this playlist
	 *
	 * @return Name of this playlist
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Gets the size of uri entries
	 *
	 * @return Size of uri entries
	 */
	public int getEntrySize() {
		return uris.size();
	}

	/** Reorders one serialized source entry by dropping it on another source entry. */
	public synchronized boolean moveTo(WrappedObject<String> source, WrappedObject<String> target) {
		ensureTransientState();
		if (!loaded || source == null || target == null || source == target) return false;
		final int oldIndex = uris.indexOf(source);
		final int targetIndex = uris.indexOf(target);
		if (oldIndex < 0 || targetIndex < 0) return false;
		final WrappedObject<String> movedUri = uris.remove(oldIndex);
		final LoadedTracks movedTracks = loadedTracks.remove(oldIndex);
		final int insertion = Math.min(targetIndex, uris.size());
		uris.add(insertion, movedUri);
		loadedTracks.add(insertion, movedTracks);
		save();
		return true;
	}

	public synchronized List<String> getUris() {
		return uris.stream().map(WrappedObject::get).toList();
	}

	public synchronized void replaceUris(Collection<String> values) {
		uris.clear();
		if (values != null) {
			values.stream().filter(value -> value != null && !value.isBlank()).limit(100_000)
					.forEach(value -> uris.add(new WrappedObject<>(value.strip())));
		}
		unload();
		save();
	}
	
	/**
	 * Gets a {@link Collection} of {@link LoadedTracks}. Should only be used if this playlist is already loaded. This
	 * collection is immutable
	 *
	 * @return Collection with all loaded tracks
	 */
	public synchronized Collection<LoadedTracks> getLoadedTracks() {
		ensureTransientState();
		return Collections.unmodifiableList(new ArrayList<>(loadedTracks));
	}
	
	/**
	 * Returns true if the playlist is empty and don't contain any uris.
	 *
	 * @return true if empty
	 */
	public boolean isEmpty() {
		return uris.isEmpty();
	}
	
	private void save() {
		MusicPlayerManager.getPlaylistManager().writeToFile();
	}
	
	// -------------------------------------------------------------------------------------------------
	// Start of implementation for playing this playlist. Nothing here is serializable.
	// -------------------------------------------------------------------------------------------------
	
	private transient LoadedTracks nextLoadedTrack;
	private transient IAudioTrack next;
	
	private transient boolean first;
	
	private transient Random random;
	private transient Set<String> shuffleCycle;
	
	@Override
	public synchronized boolean calculateNext() {
		return calculateNext(false);
	}

	@Override
	public synchronized boolean calculateNext(boolean forceFinite) {
		ensureTransientState();
		final Settings settings = MusicPlayerManager.getSettingsManager().getSettings();
		if (nextLoadedTrack == null || next == null) {
			return false;
		} else if (first) {
			first = false;
			return true;
		} else if (!forceFinite && settings.isSingleRepeat()) {
			return true;
		} else if (!forceFinite && settings.isShuffle()) {
			return selectRandomTrack();
		} else {
			return findNextSong(settings, Skip.FORWARD, forceFinite);
		}
	}
	
	@Override
	public synchronized IAudioTrack getNext() {
		return next;
	}
	
	/**
	 * Skip the current song in the {@link Skip} direction
	 *
	 * @param skip Should be skipped forward or backward
	 * @return If skip was executed
	 */
	public synchronized boolean skip(Skip skip) {
		ensureTransientState();
		final Settings settings = MusicPlayerManager.getSettingsManager().getSettings();
		return first = settings.isShuffle() ? selectRandomTrack() : findNextSong(settings, skip);
	}
	
	/**
	 * Gets the first track {@link Pair} with {@link LoadedTracks} and {@link IAudioTrack} in this playlist. Values in the
	 * pair might be null if there are no tracks.
	 *
	 * @return Pair of {@link LoadedTracks} and {@link IAudioTrack}. Can't be null, but elements can be null.
	 */
	public synchronized Pair<LoadedTracks, IAudioTrack> getFirstTrack() {
		ensureTransientState();
		return getTrackAtIndex(0, LoadedTracks::getFirstTrack);
	}
	
	/**
	 * Gets the last track {@link Pair} with {@link LoadedTracks} and {@link IAudioTrack} in this playlist. Values in the
	 * pair might be null if there are no tracks.
	 *
	 * @return Pair of {@link LoadedTracks} and {@link IAudioTrack}. Can't be null, but elements can be null.
	 */
	public synchronized Pair<LoadedTracks, IAudioTrack> getLastTrack() {
		ensureTransientState();
		return getTrackAtIndex(loadedTracks.size() - 1, LoadedTracks::getLastTrack);
	}
	
	/**
	 * Gets a {@link LoadedTracks} at the index of the loaded tracks list in this playlist. The supplied function must then
	 * select the right {@link IAudioTrack}
	 *
	 * @param index The index of the {@link LoadedTracks} entry. Must be in bound
	 * @param function A function that returns an {@link IAudioTrack} based on the passed {@link LoadedTracks}
	 * @return Pair of {@link LoadedTracks} and {@link IAudioTrack}. Can't be null, but elements can be null.
	 */
	private Pair<LoadedTracks, IAudioTrack> getTrackAtIndex(int index, Function<LoadedTracks, IAudioTrack> function) {
		if (loadedTracks.isEmpty()) {
			return Pair.of(null, null);
		}
		final LoadedTracks loadedTrack = loadedTracks.get(index);
		if (loadedTrack == null) {
			return Pair.of(null, null);
		} else {
			return Pair.of(loadedTrack, function.apply(loadedTrack));
		}
	}
	
	/**
	 * Sets the start {@link LoadedTracks} with the contained {@link IAudioTrack}
	 *
	 * @param loadedTrack {@link LoadedTracks} which must be in this playlist
	 * @param track {@link IAudioTrack} which must be in the passed loadedTrack
	 */
	public synchronized void setPlayable(LoadedTracks loadedTrack, IAudioTrack track) {
		setTracks(loadedTrack, track);
		first = true;
	}
	
	/**
	 * Sets the next track to null. So the queue if playing will then be stopped.
	 */
	public synchronized void setStopable() {
		nextLoadedTrack = null;
		next = null;
	}
	
	/**
	 * Returns a pair of calculated songs. This pair is either one after the current song if {@link Skip} is
	 * {@link Skip#FORWARD} or one behind. If the next song is invalid which is tested with
	 * {@link #getTrackAndValidate(int)} then the next valid song is chosen. If the end or start of the playlist is reached
	 * the pair contains null values.
	 *
	 * @param loadedTrack The currently loaded track {@link LoadedTracks}
	 * @param track The currently playing {@link IAudioTrack}
	 * @param skip In which direction we want to skip
	 * @return Pair of {@link LoadedTracks} and {@link IAudioTrack}. Can't be null, but elements can be null.
	 */
	private Pair<LoadedTracks, IAudioTrack> getOtherTrack(LoadedTracks loadedTrack, IAudioTrack track, Skip skip) {
		if (loadedTrack == null) {
			return Pair.of(null, null);
		}
		final IAudioTrack nextTrack = loadedTrack.getOtherTrack(track, skip);
		if (nextTrack != null) {
			return Pair.of(loadedTrack, nextTrack);
		} else {
			final int index = loadedTracks.indexOf(loadedTrack);
			if (index == -1) {
				return Pair.of(null, null);
			}
			
			final IntPredicate testIndex = newIndex -> skip == Skip.FORWARD ? newIndex < loadedTracks.size() : newIndex >= 0;
			
			for (int newIndex = index + skip.getValue(); testIndex.test(newIndex); newIndex += skip.getValue()) {
				final LoadedTracks nextValidLoadedTrack = getTrackAndValidate(newIndex);
				if (nextValidLoadedTrack != null) {
					return Pair.of(nextValidLoadedTrack, skip == Skip.FORWARD ? nextValidLoadedTrack.getFirstTrack() : nextValidLoadedTrack.getLastTrack());
				}
			}
			return Pair.of(null, null);
			
		}
	}
	
	/**
	 * Get the index of a {@link LoadedTracks} in the playlist and tests if the index is valid and the {@link LoadedTracks}
	 * has no errors and contains a valid {@link IAudioTrack} or a valid {@link IAudioTrackList}. Returns null if the test
	 * above failed.
	 *
	 * @param index The index to search for in the playlist
	 * @return The loaded track or null if the index is out of bounds or the {@link LoadedTracks} has an error
	 */
	private LoadedTracks getTrackAndValidate(int index) {
		if (index < 0 || index >= loadedTracks.size()) {
			return null;
		}
		final LoadedTracks loadedTrack = loadedTracks.get(index);
		if (loadedTrack.hasError() || (!loadedTrack.isTrack() && !loadedTrack.isTrackList())) {
			return null;
		}
		return loadedTrack;
	}
	
	/**
	 * Sets the {@link #loadedTracks} and {@link #next} variable to the passed arguments
	 *
	 * @param loadedTrack {@link LoadedTracks} which must be in this playlist
	 * @param track {@link IAudioTrack} which must be in the passed loadedTrack
	 */
	private void setTracks(LoadedTracks loadedTrack, IAudioTrack track) {
		nextLoadedTrack = loadedTrack;
		next = track;
	}
	
	/**
	 * Find a next song and set it to the {@link #nextLoadedTrack} and {@link #next} track variable
	 *
	 * @param settings The current settings
	 * @param skip In which direction we want to find the song
	 * @return Return true if a valid next song could be found. Otherwise return false
	 */
	private boolean findNextSong(Settings settings, Skip skip) {
		return findNextSong(settings, skip, false);
	}

	private boolean findNextSong(Settings settings, Skip skip, boolean forceFinite) {
		final Pair<LoadedTracks, IAudioTrack> pair = getOtherTrack(nextLoadedTrack, next, skip);
		final LoadedTracks loadedTrack = pair.getLeft();
		final IAudioTrack track = pair.getRight();
		
		if (loadedTrack == null || track == null) {
			if (forceFinite || settings.isFinite()) {
				return false;
			} else if (loadedTracks.size() > 0) {
				final Pair<LoadedTracks, IAudioTrack> sidePair = skip == Skip.FORWARD ? getFirstTrack() : getLastTrack();
				final LoadedTracks sideLoadedTrack = sidePair.getLeft();
				final IAudioTrack sideTrack = sidePair.getRight();
				if (sideLoadedTrack != null && sideTrack != null) {
					setTracks(sideLoadedTrack, sideTrack);
					return true;
				} else if (sideLoadedTrack != null) {
					final Pair<LoadedTracks, IAudioTrack> nextValidPair = getOtherTrack(sideLoadedTrack, null, skip);
					final LoadedTracks nextValidLoadedTrack = nextValidPair.getLeft();
					final IAudioTrack nextValidTrack = nextValidPair.getRight();
					if (nextValidLoadedTrack != null && nextValidTrack != null) {
						setTracks(nextValidLoadedTrack, nextValidTrack);
						return true;
					}
				}
			}
		} else if (loadedTrack != null && track != null) {
			setTracks(loadedTrack, track);
			return true;
		}
		return false;
	}
	
	/**
	 * Select a random track
	 *
	 * @return If a new random track was found
	 */
	private boolean selectRandomTrack() {
		final List<Pair<LoadedTracks, IAudioTrack>> shuffleEntries = new ArrayList<>();
		loadedTracks.forEach(loadedTrack -> {
			if (loadedTrack.isTrack()) {
				shuffleEntries.add(Pair.of(loadedTrack, loadedTrack.getTrack()));
			} else if (loadedTrack.isTrackList()) {
				loadedTrack.getTrackList().getTracks().forEach(track -> {
					shuffleEntries.add(Pair.of(loadedTrack, track));
				});
			}
		});
		if (shuffleEntries.isEmpty()) {
			return false;
		}
		if (random == null) {
			random = new Random();
		}
		final ShuffleMode mode = MusicPlayerManager.getSettingsManager().getSettings().getShuffleMode();
		if (shuffleCycle == null) shuffleCycle = new HashSet<>();
		final String currentAuthor = next == null ? "" : next.getInfo().getFixedAuthor();
		List<Pair<LoadedTracks, IAudioTrack>> candidates = new ArrayList<>(shuffleEntries);
		if (mode == ShuffleMode.NO_REPEAT) {
			candidates.removeIf(pair -> shuffleCycle.contains(stableUri(pair.getRight())));
			if (candidates.isEmpty()) { shuffleCycle.clear(); candidates = new ArrayList<>(shuffleEntries); }
		} else if (mode == ShuffleMode.HISTORY_AWARE) {
			final Set<String> recent = MusicPlayerManager.getLibraryStateManager().getRecentUris(Math.min(20, Math.max(1, shuffleEntries.size() / 2)));
			candidates.removeIf(pair -> recent.contains(stableUri(pair.getRight())));
			if (candidates.isEmpty()) candidates = new ArrayList<>(shuffleEntries);
		} else if (mode == ShuffleMode.ARTIST_SPACING && currentAuthor != null && !currentAuthor.isBlank()) {
			candidates.removeIf(pair -> currentAuthor.equalsIgnoreCase(pair.getRight().getInfo().getFixedAuthor()));
			if (candidates.isEmpty()) candidates = new ArrayList<>(shuffleEntries);
		}
		final Pair<LoadedTracks, IAudioTrack> pair;
		if (mode == ShuffleMode.RATING_WEIGHTED) {
			int totalWeight = 0;
			for (final Pair<LoadedTracks, IAudioTrack> candidate : candidates) totalWeight += Math.max(1, MusicPlayerManager.getLibraryStateManager().getRating(candidate.getRight().getInfo()) + 2);
			int selected = random.nextInt(Math.max(1, totalWeight));
			Pair<LoadedTracks, IAudioTrack> weighted = candidates.get(0);
			for (final Pair<LoadedTracks, IAudioTrack> candidate : candidates) { selected -= Math.max(1, MusicPlayerManager.getLibraryStateManager().getRating(candidate.getRight().getInfo()) + 2); weighted = candidate; if (selected < 0) break; }
			pair = weighted;
		} else {
			pair = candidates.get(random.nextInt(candidates.size()));
		}
		nextLoadedTrack = pair.getLeft();
		next = pair.getRight();
		if (mode == ShuffleMode.NO_REPEAT) shuffleCycle.add(stableUri(next));
		return true;
	}

	private static String stableUri(IAudioTrack track) {
		if (track == null || track.getInfo() == null) return "";
		final String uri = track.getInfo().getURI();
		return uri == null || uri.isBlank() ? track.getInfo().getIdentifier() : uri;
	}

	public static void shutdownExecutor() {
		LOAD_EXECUTOR.shutdownNow();
		LOAD_TIMEOUT_EXECUTOR.shutdownNow();
	}

	private static final class LoadSlot {
		private final int index;
		private final WrappedObject<String> uri;
		private int attempt;
		private int token;
		private boolean finished;
		private LoadSlot(int index, WrappedObject<String> uri) { this.index = index; this.uri = uri; }
	}
}
