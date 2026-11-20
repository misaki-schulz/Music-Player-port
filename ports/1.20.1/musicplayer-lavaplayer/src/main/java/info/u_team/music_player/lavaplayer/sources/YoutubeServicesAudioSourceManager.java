package info.u_team.music_player.lavaplayer.sources;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;

import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.YoutubeSourceOptions;
import dev.lavalink.youtube.clients.skeleton.Client;
import net.hycrafthd.youtube_source_services.LocalService;
import net.hycrafthd.youtube_source_services.YoutubeSourceServices;

/**
 * Starts the local cipher and poToken services on the first YouTube request.
 * Stops them after five minutes without requests or active playback.
 */
public class YoutubeServicesAudioSourceManager extends YoutubeAudioSourceManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(YoutubeServicesAudioSourceManager.class);

	private static final long IDLE_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(5);

	private final YoutubeSourceServices services;
	private final BooleanSupplier playing;
	private final ScheduledExecutorService scheduler;
	private int pendingRequests;
	private boolean closed;

	/** Last use in nanoseconds, or zero before startup or after an idle stop. Guarded by this instance. */
	private long lastUsed;

	/** Uses an external cipher if configured. Otherwise uses the local services. */
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
			return new YoutubeServicesAudioSourceManager(services, playing, clients);
		} catch (IOException | RuntimeException ex) {
			LOGGER.warn("Cannot configure the local YouTube services. Using the external cipher", ex);
			return externalCipher(null, null, clients);
		}
	}

	private static YoutubeAudioSourceManager externalCipher(String url, String password, Client... clients) {
		final String cipherUrl = url == null || url.isBlank() ? "https://cipher.kikkia.dev/" : url;
		return new YoutubeAudioSourceManager(
				new YoutubeSourceOptions().setRemoteCipher(cipherUrl, password, "musicplayer-minecraft"),
				clients);
	}

	private YoutubeServicesAudioSourceManager(YoutubeSourceServices services, BooleanSupplier playing, Client... clients) {
		super(createOptions(services), clients);
		this.services = services;
		this.playing = playing;

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

	/** Waits up to two minutes for service startup, including any downloads. */
	private void use() {
		final CompletableFuture<Void> start;
		synchronized (this) {
			if (closed) {
				return;
			}
			start = services.start();
			pendingRequests++;
		}
		try {
			start.get(2, TimeUnit.MINUTES);
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
		} catch (ExecutionException | TimeoutException ex) {
			LOGGER.warn("Cannot start the local YouTube services. YouTube tracks might not be playable", ex);
		} finally {
			synchronized (this) {
				pendingRequests--;
				lastUsed = System.nanoTime();
			}
		}
	}

	private synchronized void stopIfIdle() {
		if (pendingRequests != 0 || lastUsed == 0) {
			return;
		}
		if (playing.getAsBoolean()) {
			lastUsed = System.nanoTime();
		} else if (System.nanoTime() - lastUsed >= TimeUnit.MILLISECONDS.toNanos(IDLE_TIMEOUT_MS)) {
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
			super.shutdown();
		}
	}

	/** Starts the services only for identifiers handled by YouTube. */
	@Override
	protected Router getRouter(HttpInterface httpInterface, String identifier) {
		final Router router = super.getRouter(httpInterface, identifier);
		if (router != null && router != Router.none) {
			use();
		}
		return router;
	}

	/** Starts the services before opening a YouTube playback connection. */
	@Override
	public HttpInterface getInterface() {
		use();
		return super.getInterface();
	}
}
