package info.u_team.music_player.lavaplayer.sources;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.http.entity.ContentType;

import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sun.net.httpserver.HttpServer;

import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.clients.AndroidVr;
import dev.lavalink.youtube.track.format.StreamFormat;
import net.hycrafthd.youtube_source_services.YoutubeSourceServices;

public class YoutubeStartupCheck {
	private static final String SCRIPT = "https://test.invalid/player.js";
	private static final String ID = "UL_Wnh5kPLI";
	private static final StreamFormat FORMAT = new StreamFormat(ContentType.parse("audio/webm; codecs=opus"), 251, 100,
			100, 2, "https://test.invalid/stream", "n", "s", "sig", true, false);

	public static void main(String[] args) throws Exception {
		final Path fakeDeno = Path.of(args[0]);
		final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", exchange -> {
			exchange.getRequestBody().readAllBytes();
			final String response = switch (exchange.getRequestURI().getPath()) {
				case "/get_sts" -> "{\"sts\":\"fallback\"}";
				case "/resolve_url" -> "{\"resolved_url\":\"https://test.invalid/fallback\"}";
				default -> null;
			};
			if (response == null || exchange.getRequestHeaders().getFirst("Authorization") != null
					|| exchange.getRequestHeaders().getFirst("Cookie") != null
					|| !Objects.equals(exchange.getRequestHeaders().getFirst("User-Agent"), "musicplayer-minecraft")) {
				exchange.sendResponseHeaders(400, -1);
			} else {
				final byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
				exchange.sendResponseHeaders(200, bytes.length);
				exchange.getResponseBody().write(bytes);
			}
			exchange.close();
		});
		server.start();
		final String fallback = "http://127.0.0.1:" + server.getAddress().getPort();
		final ExecutorService worker = Executors.newSingleThreadExecutor();
		try {
			for (String scenario : new String[] {"resolve", "timestamp", "start-failed"}) {
                final boolean fail = scenario.equals("start-failed");
				final Path base = prepare(fakeDeno);
				final YoutubeSourceServices services = YoutubeSourceServices.builder(base).build();
				final var source = new YoutubeServicesAudioSourceManager(services, () -> false, fallback, new AndroidVr() {
					@Override public AudioTrack loadVideo(YoutubeAudioSourceManager manager, HttpInterface http, String id) {
						if (!ID.equals(id)) throw new AssertionError(id);
						return manager.buildAudioTrack(new AudioTrackInfo("title", "author", 60_000, id, false, id));
					}
				});
				try {
					worker.submit(() -> {
						final var item = source.loadItem(null, new AudioReference("https://www.youtube.com/watch?v=" + ID, null));
						check(item instanceof AudioTrack, "Cold metadata did not return a track");
						try (HttpInterface http = source.getInterface()) {
							http.getContext().setAttribute("oauthToken", "fixture-context-marker");
							check("fallback".equals(source.getCipherManager().getTimestamp(http, SCRIPT)), "Cold STS");
							check(URI.create("https://test.invalid/fallback").equals(source.getCipherManager().resolveFormatUrl(http, SCRIPT, FORMAT)), "Cold resolve");
							check(source.generatePoToken(http, ID) == null, "Cold PoToken should be omitted");
						}
						return null;
					}).get(2, TimeUnit.SECONDS);
					Files.writeString(base.resolve("cipher-go"), fail ? "fail" : "go");
					Files.writeString(base.resolve("pot-go"), "go");
					await(() -> Files.exists(base.resolve("warmup-started")));
					try (HttpInterface http = source.getInterface()) {
						check(source.generatePoToken(http, ID) == null, "Waited on or used PoToken during warm-up");
						if (!fail) {
							await(services.cipher()::isRunning);
							check("local".equals(source.getCipherManager().getTimestamp(http, SCRIPT)), "Ready cipher should work during independent PoToken warm-up");
							check(URI.create("https://test.invalid/local").equals(source.getCipherManager().resolveFormatUrl(http, SCRIPT, FORMAT)), "Ready local resolve");
						}
						Files.createFile(base.resolve("warmup-go"));
						final var start = services.start();
						try {
							start.get(3, TimeUnit.SECONDS);
							check(!fail, "Setup unexpectedly succeeded");
						} catch (java.util.concurrent.ExecutionException ex) {
							if (!fail) throw ex;
						}
						await(() -> {
							final var result = source.generatePoToken(http, ID);
							return result != null && result.getPoToken().equals("local-pot");
						});
						if (fail) {
							check("fallback".equals(source.getCipherManager().getTimestamp(http, SCRIPT)), "Failed cipher must use fallback");
						} else {
                            Files.createFile(base.resolve("cipher-broken"));
                            if (scenario.equals("timestamp")) {
                                check("fallback".equals(source.getCipherManager().getTimestamp(http, SCRIPT)), "HTTP 500 STS must use fallback");
                            } else {
                                check(URI.create("https://test.invalid/fallback").equals(source.getCipherManager().resolveFormatUrl(http, SCRIPT, FORMAT)), "HTTP 500 resolve must use fallback");
                            }
                            Files.delete(base.resolve("cipher-broken"));
                            check("fallback".equals(source.getCipherManager().getTimestamp(http, SCRIPT)), "Cooldown must avoid failing local requests");
                        }
					}
				} finally {
					source.shutdown();
				}
			}
			System.setProperty("musicplayer.youtube.cipher.url", fallback);
			try {
				final var explicit = YoutubeServicesAudioSourceManager.create(() -> false, new AndroidVr());
				try {
					check(!(explicit instanceof YoutubeServicesAudioSourceManager), "Configured cipher must bypass local services");
					check(fallback.equals(explicit.getRemoteCipherManager().getRemoteUrl()), "Configured cipher URL lost");
				} finally {
					explicit.shutdown();
				}
			} finally {
				System.clearProperty("musicplayer.youtube.cipher.url");
			}
			System.out.println("PASS: HTTP 500 fallback and cooldown, metadata and cipher while cold, local cipher during PoToken warm-up, local PoToken, startup failure, isolated fallback headers, explicit override");
		} finally {
			worker.shutdownNow();
			server.stop(0);
		}
	}

	private static Path prepare(Path executable) throws Exception {
		final Path base = Files.createTempDirectory(Path.of(System.getProperty("musicplayer.test.work", "work")), "youtube-fixture").toAbsolutePath();
        final Properties config = new Properties();
        try (final var input = YoutubeSourceServices.class.getResourceAsStream("services.properties")) {
            config.load(input);
        }
		final Path deno = Files.createDirectories(base.resolve("deno-" + config.getProperty("deno.version"))).resolve("deno");
		Files.copy(executable, deno, StandardCopyOption.COPY_ATTRIBUTES);
		Files.createFile(Files.createDirectories(base.resolve("yt-cipher/" + config.getProperty("yt-cipher.commit").substring(0, 7))).resolve(".installed"));
		Files.createFile(Files.createDirectories(base.resolve("webpo-generator/" + config.getProperty("webpo-generator.commit").substring(0, 7))).resolve(".installed"));
		return base;
	}

	private static void await(CheckedBoolean ready) throws Exception {
		final long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
		while (!ready.get()) {
			if (System.nanoTime() >= end) throw new AssertionError("Fixture condition timed out");
			Thread.sleep(15);
		}
	}

	private static void check(boolean condition, String message) {
		if (!condition) throw new AssertionError(message);
	}

	private interface CheckedBoolean {
		boolean get() throws Exception;
	}
}
