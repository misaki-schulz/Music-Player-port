package info.u_team.music_player.artwork;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.atomic.AtomicLong;

import info.u_team.music_player.lavaplayer.api.audio.IAudioTrackInfo;
import info.u_team.music_player.musicplayer.MusicPlayerManager;

/** Bounded, daemon-backed artwork downloader with a persistent local cache. */
public final class ArtworkRepository {

	private static final int MAX_IMAGE_BYTES = 8 * 1024 * 1024;
	private static final String LOCAL_OVERRIDE_PREFIX = "musicplayer-local-artwork:";
	private static final Pattern YOUTUBE_ID = Pattern.compile("(?:youtu\\.be/|youtube(?:-nocookie)?\\.com/(?:watch\\?v=|embed/|shorts/))([A-Za-z0-9_-]{6,})");
	private static final Map<String, CompletableFuture<byte[]>> REQUESTS = new ConcurrentHashMap<>();
	private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(2, runnable -> {
		final Thread thread = new Thread(runnable, "Music Player artwork cache");
		thread.setDaemon(true);
		return thread;
	});
	private static final HttpClient HTTP = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(8))
			.followRedirects(HttpClient.Redirect.NORMAL)
			.executor(EXECUTOR)
			.build();
	private static final long CACHE_EXPIRY_MILLIS = Duration.ofDays(90).toMillis();

	private ArtworkRepository() {
	}

	public static CompletableFuture<byte[]> request(IAudioTrackInfo info) {
		final String url = resolveURL(info);
		if (url == null) return CompletableFuture.failedFuture(new IllegalArgumentException("Track has no artwork URL"));
		if (url.startsWith(LOCAL_OVERRIDE_PREFIX)) return REQUESTS.computeIfAbsent(url, key -> CompletableFuture.supplyAsync(() -> readOverride(key.substring(LOCAL_OVERRIDE_PREFIX.length())), EXECUTOR));
		return REQUESTS.computeIfAbsent(url, ArtworkRepository::load);
	}

	public static String resolveURL(IAudioTrackInfo info) {
		if (info == null) return null;
		final String overrideKey = trackKey(info);
		if (findOverride(overrideKey) != null) return LOCAL_OVERRIDE_PREFIX + overrideKey;
		final String artwork = info.getArtworkURL();
		if (isHttpURL(artwork)) return artwork;
		final String uri = info.getURI();
		if (!isHttpURL(uri)) return null;
		final Matcher matcher = YOUTUBE_ID.matcher(uri);
		return matcher.find() ? "https://i.ytimg.com/vi/" + matcher.group(1) + "/mqdefault.jpg" : null;
	}

	public static void setOverride(String uri, Path source) throws IOException {
		if (uri == null || uri.isBlank() || source == null || !Files.isRegularFile(source)) throw new IOException("Invalid artwork file");
		final long size = Files.size(source);
		if (size <= 0 || size > MAX_IMAGE_BYTES) throw new IOException("Artwork must be between 1 byte and 8 MiB");
		final String lower = source.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
		final String extension = lower.endsWith(".png") ? ".png" : lower.endsWith(".jpeg") ? ".jpeg" : lower.endsWith(".jpg") ? ".jpg" : null;
		if (extension == null) throw new IOException("Choose a PNG or JPEG image");
		final String key = cacheKey(uri);
		final Path directory = overrideDirectory();
		Files.createDirectories(directory);
		clearOverride(uri);
		final Path destination = directory.resolve(key + extension);
		final Path temporary = directory.resolve(key + ".tmp");
		Files.copy(source, temporary, StandardCopyOption.REPLACE_EXISTING);
		try { Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
		catch (final IOException unsupportedAtomicMove) { Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING); }
		REQUESTS.remove(LOCAL_OVERRIDE_PREFIX + key);
	}

	public static void clearOverride(String uri) throws IOException {
		if (uri == null || uri.isBlank()) return;
		final String key = cacheKey(uri);
		for (final String extension : new String[] { ".png", ".jpg", ".jpeg" }) Files.deleteIfExists(overrideDirectory().resolve(key + extension));
		REQUESTS.remove(LOCAL_OVERRIDE_PREFIX + key);
	}

	private static String trackKey(IAudioTrackInfo info) {
		final String uri = info.getURI() == null || info.getURI().isBlank() ? info.getIdentifier() : info.getURI();
		return cacheKey(uri == null ? info.getFixedTitle() + "|" + info.getFixedAuthor() : uri);
	}

	private static byte[] readOverride(String key) {
		final Path path = findOverride(key);
		if (path == null) throw new IllegalStateException("Artwork override disappeared");
		try { return Files.readAllBytes(path); } catch (final IOException exception) { throw new IllegalStateException("Cannot read artwork override", exception); }
	}

	private static Path findOverride(String key) {
		for (final String extension : new String[] { ".png", ".jpg", ".jpeg" }) {
			final Path path = overrideDirectory().resolve(key + extension);
			if (Files.isRegularFile(path)) return path;
		}
		return null;
	}

	public static String cacheKey(String value) {
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
		} catch (final NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable", exception);
		}
	}

	private static CompletableFuture<byte[]> load(String url) {
		return CompletableFuture.supplyAsync(() -> readCached(url), EXECUTOR).thenCompose(cached -> {
			if (cached != null) return CompletableFuture.completedFuture(cached);
			final HttpRequest request = HttpRequest.newBuilder(URI.create(url))
					.timeout(Duration.ofSeconds(15))
					.header("User-Agent", "MusicPlayer-Minecraft-Mod/2.7")
					.header("Accept", "image/avif,image/webp,image/png,image/jpeg,image/*;q=0.8")
					.GET().build();
			return HTTP.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream()).thenApplyAsync(response -> {
				if (response.statusCode() < 200 || response.statusCode() >= 300) {
					closeQuietly(response.body());
					throw new IllegalStateException("Artwork server returned HTTP " + response.statusCode());
				}
				final long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
				if (contentLength > MAX_IMAGE_BYTES) {
					closeQuietly(response.body());
					throw new IllegalStateException("Artwork exceeds the 8 MiB limit");
				}
				try (InputStream input = response.body()) {
					final byte[] bytes = input.readNBytes(MAX_IMAGE_BYTES + 1);
					if (bytes.length == 0 || bytes.length > MAX_IMAGE_BYTES) throw new IllegalStateException("Invalid artwork size");
					writeCached(url, bytes);
					return bytes;
				} catch (final IOException exception) {
					throw new IllegalStateException("Cannot read artwork", exception);
				}
			}, EXECUTOR);
		});
	}

	private static byte[] readCached(String url) {
		final Path file = cacheDirectory().resolve(cacheKey(url) + ".img");
		try {
			if (!Files.isRegularFile(file)) return null;
			final long size = Files.size(file);
			if (size <= 0 || size > MAX_IMAGE_BYTES) {
				Files.deleteIfExists(file);
				return null;
			}
			if (System.currentTimeMillis() - Files.getLastModifiedTime(file).toMillis() > CACHE_EXPIRY_MILLIS) {
				Files.deleteIfExists(file);
				return null;
			}
			Files.setLastModifiedTime(file, java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis()));
			return Files.readAllBytes(file);
		} catch (final IOException exception) {
			return null;
		}
	}

	private static synchronized void writeCached(String url, byte[] bytes) {
		final Path directory = cacheDirectory();
		final Path destination = directory.resolve(cacheKey(url) + ".img");
		final Path temporary = directory.resolve(cacheKey(url) + ".tmp");
		try {
			Files.createDirectories(directory);
			Files.write(temporary, bytes);
			try {
				Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (final IOException unsupportedAtomicMove) {
				Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
			}
			trimCache(directory);
		} catch (final IOException ignored) {
			try { Files.deleteIfExists(temporary); } catch (final IOException ignoredCleanup) { }
		}
	}

	private static void trimCache(Path directory) throws IOException {
		final long limit = MusicPlayerManager.getSettingsManager().getSettings().getArtworkCacheMegabytes() * 1024L * 1024L;
		try (var files = Files.list(directory)) {
			final var entries = files.filter(path -> path.getFileName().toString().endsWith(".img"))
					.sorted(Comparator.comparingLong(ArtworkRepository::lastModified)).toList();
			long total = 0L;
			for (final Path entry : entries) total += size(entry);
			for (final Path entry : entries) {
				if (total <= limit) break;
				final long size = size(entry);
				Files.deleteIfExists(entry);
				total -= size;
			}
		}
	}

	private static Path cacheDirectory() {
		final Path base = MusicPlayerManager.getFiles().getDirectory();
		return (base == null ? Path.of("config", "musicplayer") : base).resolve("artwork-cache");
	}

	private static Path overrideDirectory() {
		final Path base = MusicPlayerManager.getFiles().getDirectory();
		return (base == null ? Path.of("config", "musicplayer") : base).resolve("artwork-overrides");
	}

	private static long lastModified(Path path) {
		try { return Files.getLastModifiedTime(path).toMillis(); } catch (final IOException exception) { return Long.MIN_VALUE; }
	}

	private static long size(Path path) {
		try { return Files.size(path); } catch (final IOException exception) { return 0L; }
	}

	private static boolean isHttpURL(String value) {
		if (value == null || value.isBlank()) return false;
		try {
			final String scheme = URI.create(value).getScheme();
			return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
		} catch (final IllegalArgumentException exception) {
			return false;
		}
	}

	private static void closeQuietly(InputStream input) {
		try { input.close(); } catch (final IOException ignored) { }
	}

	public static CompletableFuture<CacheStatus> inspectCache() {
		return CompletableFuture.supplyAsync(() -> {
			final Path directory = cacheDirectory();
			if (!Files.isDirectory(directory)) return new CacheStatus(0L, 0L);
			final AtomicLong bytes = new AtomicLong();
			final AtomicLong files = new AtomicLong();
			try (var paths = Files.list(directory)) {
				paths.filter(path -> path.getFileName().toString().endsWith(".img")).forEach(path -> {
					bytes.addAndGet(size(path)); files.incrementAndGet();
				});
			} catch (final IOException ignored) { }
			return new CacheStatus(files.get(), bytes.get());
		}, EXECUTOR);
	}

	public static CompletableFuture<CacheStatus> cleanupExpired() {
		return CompletableFuture.supplyAsync(() -> {
			final Path directory = cacheDirectory();
			if (Files.isDirectory(directory)) {
				try (var paths = Files.list(directory)) {
					paths.filter(path -> path.getFileName().toString().endsWith(".img")).forEach(path -> {
						try { if (System.currentTimeMillis() - Files.getLastModifiedTime(path).toMillis() > CACHE_EXPIRY_MILLIS) Files.deleteIfExists(path); }
						catch (final IOException ignored) { }
					});
				} catch (final IOException ignored) { }
			}
			return inspectCacheSync();
		}, EXECUTOR);
	}

	public static CompletableFuture<CacheStatus> clearCache() {
		return CompletableFuture.supplyAsync(() -> {
			final Path directory = cacheDirectory();
			if (Files.isDirectory(directory)) {
				try (var paths = Files.list(directory)) {
					paths.filter(path -> path.getFileName().toString().endsWith(".img")).forEach(path -> { try { Files.deleteIfExists(path); } catch (final IOException ignored) { } });
				} catch (final IOException ignored) { }
			}
			REQUESTS.clear();
			return new CacheStatus(0L, 0L);
		}, EXECUTOR);
	}

	private static CacheStatus inspectCacheSync() {
		final Path directory = cacheDirectory();
		long count = 0L, bytes = 0L;
		if (Files.isDirectory(directory)) {
			try (var paths = Files.list(directory)) {
				for (final Path path : paths.filter(value -> value.getFileName().toString().endsWith(".img")).toList()) { count++; bytes += size(path); }
			} catch (final IOException ignored) { }
		}
		return new CacheStatus(count, bytes);
	}

	public record CacheStatus(long files, long bytes) {
		public String display() { return files + " images / " + String.format(java.util.Locale.ROOT, "%.1f", bytes / 1_048_576D) + " MB"; }
	}

	public static void shutdown() {
		REQUESTS.clear();
		EXECUTOR.shutdownNow();
	}
}
