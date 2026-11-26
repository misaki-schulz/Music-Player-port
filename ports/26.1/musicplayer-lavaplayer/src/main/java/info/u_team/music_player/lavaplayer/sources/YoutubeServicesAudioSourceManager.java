package info.u_team.music_player.lavaplayer.sources;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager;

import dev.lavalink.youtube.RemotePoToken;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.YoutubeSource;
import dev.lavalink.youtube.YoutubeSourceOptions;
import dev.lavalink.youtube.cipher.RemoteCipherManager;
import dev.lavalink.youtube.clients.skeleton.Client;
import dev.lavalink.youtube.track.format.StreamFormat;
import net.hycrafthd.youtube_source_services.LocalService;
import net.hycrafthd.youtube_source_services.YoutubeSourceServices;

/**
 * Warms up the local cipher and poToken services in the background.
 * Stops them after five minutes without requests or active playback.
 */
public class YoutubeServicesAudioSourceManager extends YoutubeAudioSourceManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(YoutubeServicesAudioSourceManager.class);
	private static final String DEFAULT_CIPHER_URL = "https://cipher.kikkia.dev/";
	private static final String EXTERNAL_CIPHER_USER_AGENT = "musicplayer-minecraft";
	private static final long IDLE_TIMEOUT_NANOS = TimeUnit.MINUTES.toNanos(5);
	private static final long START_RETRY_DELAY_NANOS = TimeUnit.SECONDS.toNanos(30);

	private final YoutubeSourceServices services;
	private final BooleanSupplier playing;
	private final ScheduledExecutorService scheduler;
	private final HttpInterfaceManager fallbackHttp;

	// Guarded by this instance.
	private CompletableFuture<Void> startup;
	private long lastUsed;
	private long lastStartFailure;
	private boolean closed;

	/** Uses an explicitly configured cipher in place of the local services. */
	public static YoutubeAudioSourceManager create(BooleanSupplier playing, Client... clients) {
		final String cipherUrl = System.getProperty("musicplayer.youtube.cipher.url");
		final String cipherPassword = System.getProperty("musicplayer.youtube.cipher.password");
		if (cipherUrl != null || cipherPassword != null) {
			return externalCipher(cipherUrl, cipherPassword, clients);
		}
		try {
			final YoutubeSourceServices services = YoutubeSourceServices.builder(Paths.get(
					System.getProperty("java.io.tmpdir", "/tmp"), "musicplayer-youtube-services"))
					.log(LOGGER::debug)
					.build();
			return new YoutubeServicesAudioSourceManager(services, playing, DEFAULT_CIPHER_URL, clients);
		} catch (IOException | RuntimeException ex) {
			LOGGER.warn("Cannot configure the local YouTube services. Using the external cipher", ex);
			return externalCipher(null, null, clients);
		}
	}

	private static YoutubeAudioSourceManager externalCipher(String url, String password, Client... clients) {
		final String cipherUrl = url == null || url.isBlank() ? DEFAULT_CIPHER_URL : url;
		return new YoutubeAudioSourceManager(
				new YoutubeSourceOptions().setRemoteCipher(cipherUrl, password, EXTERNAL_CIPHER_USER_AGENT),
				clients);
	}

	YoutubeServicesAudioSourceManager(YoutubeSourceServices services, BooleanSupplier playing, String fallbackUrl, Client... clients) {
		super(createOptions(services), clients);
		this.services = services;
		this.playing = playing;

		// Keep the local cipher's Authorization header and the YouTube session out of fallback requests.
		fallbackHttp = HttpClientTools.createCookielessThreadLocalManager();
		fallbackHttp.configureBuilder(builder -> builder.setUserAgent(EXTERNAL_CIPHER_USER_AGENT)
				.setDefaultHeaders(List.of(new BasicHeader("Plugin-Version", YoutubeSource.VERSION))));
		setCipherManager(new StartupAwareCipher(services.cipher().getUrl(), fallbackUrl));

		scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
			final Thread thread = new Thread(runnable, "musicplayer-youtube-services-idle");
			thread.setDaemon(true);
			return thread;
		});
		scheduler.scheduleWithFixedDelay(this::stopIfIdle, 30, 30, TimeUnit.SECONDS);
	}

	private static YoutubeSourceOptions createOptions(YoutubeSourceServices services) {
		final LocalService cipher = services.cipher();
		final LocalService poToken = services.poToken();
		return new YoutubeSourceOptions() //
				.setRemoteCipher(cipher.getUrl(), cipher.getToken(), "musicplayer") //
				.setRemotePoToken(poToken.getUrl(), poToken.getToken());
	}

	private synchronized void use() {
		if (closed) {
			return;
		}
		lastUsed = System.nanoTime();
		if (services.isRunning() || (startup != null && !startup.isDone())
				|| (lastStartFailure != 0 && lastUsed - lastStartFailure < START_RETRY_DELAY_NANOS)) {
			return;
		}
		startup = services.start().whenComplete((ignored, error) -> {
			synchronized (this) {
				if (error != null && !closed && lastUsed != 0) {
					lastStartFailure = System.nanoTime();
					LOGGER.warn("Cannot start all local YouTube services. Using the external cipher if needed", error);
				}
			}
		});
	}

	private synchronized void stopIfIdle() {
		if (lastUsed == 0) {
			return;
		}
		if (playing.getAsBoolean()) {
			lastUsed = System.nanoTime();
		} else if (System.nanoTime() - lastUsed >= IDLE_TIMEOUT_NANOS) {
			lastUsed = 0;
			services.stop();
			LOGGER.debug("Stopped the idle YouTube services");
		}
	}

	@Override
	public synchronized void shutdown() {
		closed = true;
		scheduler.shutdownNow();
		try {
			services.stop();
		} finally {
			try {
				ExceptionTools.closeWithWarnings(fallbackHttp);
			} finally {
				super.shutdown();
			}
		}
	}

	@Override
	protected Router getRouter(HttpInterface httpInterface, String identifier) {
		final Router router = super.getRouter(httpInterface, identifier);
		if (router != null && router != Router.none) {
			use();
		}
		return router;
	}

	@Override
	public HttpInterface getInterface() {
		use();
		return super.getInterface();
	}

	@Override
	public RemotePoToken.Result generatePoToken(HttpInterface httpInterface, String contentBinding) throws IOException {
		use();
		return isPoTokenReady() ? super.generatePoToken(httpInterface, contentBinding) : null;
	}

	private synchronized boolean isPoTokenReady() {
		// The service marks itself running before its first, throwaway token request finishes.
		return !closed && startup != null && startup.isDone() && services.poToken().isRunning();
	}

	private final class StartupAwareCipher extends RemoteCipherManager {

		private final RemoteCipherManager fallback;
		private volatile long localFailureUntil;

		private StartupAwareCipher(String localUrl, String fallbackUrl) {
			super(localUrl);
			fallback = new RemoteCipherManager(fallbackUrl);
		}

		@Override
		public String getTimestamp(HttpInterface httpInterface, String sourceUrl) throws IOException {
			use();
			if (useLocalCipher()) {
				try {
					return super.getTimestamp(httpInterface, sourceUrl);
				} catch (IOException ex) {
					localCipherFailed(ex);
				}
			}
			try (final HttpInterface external = fallbackHttp.getInterface()) {
				return fallback.getTimestamp(external, sourceUrl);
			}
		}

		@Override
		public URI resolveFormatUrl(HttpInterface httpInterface, String playerScript, StreamFormat format) throws IOException {
			use();
			if (useLocalCipher()) {
				try {
					return super.resolveFormatUrl(httpInterface, playerScript, format);
				} catch (IOException ex) {
					localCipherFailed(ex);
				}
			}
			try (final HttpInterface external = fallbackHttp.getInterface()) {
				return fallback.resolveFormatUrl(external, playerScript, format);
			}
		}

		private boolean useLocalCipher() {
			final long until = localFailureUntil;
			return services.cipher().isRunning() && (until == 0 || System.nanoTime() - until >= 0);
		}

		private void localCipherFailed(IOException error) {
			// A listening process can still fail to solve a new YouTube player script.
			// Avoid repeating the same failing request for every client and track.
			localFailureUntil = System.nanoTime() + START_RETRY_DELAY_NANOS;
			LOGGER.warn("Local YouTube cipher request failed. Using the external cipher for 30 seconds", error);
		}
	}
}
