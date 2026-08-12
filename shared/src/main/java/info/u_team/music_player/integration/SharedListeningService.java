package info.u_team.music_player.integration;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.Gson;

import info.u_team.music_player.lavaplayer.api.IMusicPlayer;
import info.u_team.music_player.lavaplayer.api.audio.IAudioTrack;
import info.u_team.music_player.lavaplayer.api.audio.IPlayingTrack;
import info.u_team.music_player.lavaplayer.api.search.ISearchResult;
import info.u_team.music_player.musicplayer.settings.Settings;
import net.minecraft.client.Minecraft;

/** Explicit-consent direct/LAN or optional server-relay metadata synchronization. No audio bytes are relayed. */
public final class SharedListeningService {

	private static final int MAX_MESSAGE_CHARS = 16_384;
	private static final long SEND_INTERVAL_NANOS = 1_000_000_000L;
	private final Gson gson = new Gson();
	private final SecureRandom random = new SecureRandom();
	private final ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
		final Thread thread = new Thread(runnable, "Music Player shared listening");
		thread.setDaemon(true);
		return thread;
	});
	private final CopyOnWriteArrayList<Peer> peers = new CopyOnWriteArrayList<>();
	private final AtomicBoolean closed = new AtomicBoolean();
	private volatile ServerSocket server;
	private volatile Peer joinedPeer;
	private volatile Peer relayPeer;
	private volatile boolean relayBroadcaster;
	private volatile String sessionCode = "";
	private volatile String status = "Inactive";
	private volatile long nextSend;
	private volatile String appliedUri = "";

	public synchronized String host(int requestedPort) throws IOException {
		disconnect();
		final int port = Math.clamp(requestedPort, 1024, 65_535);
		server = new ServerSocket();
		server.setReuseAddress(true);
		server.bind(new InetSocketAddress((InetAddress) null, port), 8);
		sessionCode = randomCode();
		status = "Direct host on port " + server.getLocalPort();
		executor.execute(this::acceptLoop);
		return sessionCode;
	}

	public synchronized void join(String host, int requestedPort, String code) throws IOException {
		connect(host, requestedPort, normalizeRequiredCode(code), "hello", false, "Direct peer");
	}

	public synchronized String broadcastThroughRelay(String host, int requestedPort, String code) throws IOException {
		final String normalized = code == null || code.isBlank() ? randomCode() : normalizeRequiredCode(code);
		connect(host, requestedPort, normalized, "relay-broadcast", true, "Relay broadcaster");
		return normalized;
	}

	public synchronized void joinRelay(String host, int requestedPort, String code) throws IOException {
		connect(host, requestedPort, normalizeRequiredCode(code), "relay-listen", false, "Relay listener");
	}

	private void connect(String host, int requestedPort, String code, String helloType, boolean broadcaster, String label) throws IOException {
		disconnect();
		if (host == null || host.isBlank()) throw new IOException("Host address is empty");
		sessionCode = code;
		final Socket socket = new Socket();
		socket.connect(new InetSocketAddress(host.strip(), Math.clamp(requestedPort, 1024, 65_535)), 5000);
		socket.setSoTimeout(15_000);
		final Peer peer = new Peer(socket);
		peer.write(new WireMessage(helloType, sessionCode, null, 0L, 0L, true, false, ""));
		if (broadcaster) {
			relayPeer = peer;
			relayBroadcaster = true;
		} else {
			joinedPeer = peer;
			executor.execute(() -> readJoined(peer));
		}
		status = label + " connected to " + host.strip();
	}

	public synchronized void disconnect() {
		final ServerSocket currentServer = server;
		server = null;
		if (currentServer != null) try { currentServer.close(); } catch (final IOException ignored) { }
		for (final Peer peer : peers) peer.close();
		peers.clear();
		final Peer currentJoined = joinedPeer;
		joinedPeer = null;
		if (currentJoined != null) currentJoined.close();
		final Peer currentRelay = relayPeer;
		relayPeer = null;
		relayBroadcaster = false;
		if (currentRelay != null) currentRelay.close();
		sessionCode = "";
		appliedUri = "";
		status = "Inactive";
	}

	public void tick(IMusicPlayer player, Settings settings) {
		if (closed.get() || player == null || !settings.isNearbyMusicBroadcast()) return;
		final long now = System.nanoTime();
		if (now < nextSend) return;
		if (server == null && (!relayBroadcaster || relayPeer == null)) return;
		nextSend = now + SEND_INTERVAL_NANOS;
		final WireMessage message = createState(player, settings);
		if (server != null) {
			for (final Peer peer : peers) {
				try { peer.write(message); }
				catch (final IOException exception) { peer.close(); peers.remove(peer); }
			}
		} else {
			try { relayPeer.write(message); }
			catch (final IOException exception) { disconnect(); status = "Relay disconnected"; }
		}
	}

	private WireMessage createState(IMusicPlayer player, Settings settings) {
		final IPlayingTrack track = player.getTrackManager().getCurrentTrack();
		return track == null
				? new WireMessage("state", sessionCode, null, 0L, 0L, true, false, "")
				: new WireMessage("state", sessionCode, stableUri(track), track.getPosition(), track.getDuration(),
						player.getTrackManager().isPaused(), track.getInfo().isStream(),
						settings.isShareTrackTitle() ? track.getInfo().getFixedTitle() : "");
	}

	public String status() { return status; }
	public String sessionCode() { return sessionCode; }
	public int port() { final ServerSocket value = server; return value == null ? 0 : value.getLocalPort(); }
	public void shutdown() { if (closed.compareAndSet(false, true)) { disconnect(); executor.shutdownNow(); } }

	private void acceptLoop() {
		while (server != null && !closed.get()) {
			try {
				final Socket socket = server.accept();
				socket.setSoTimeout(15_000);
				executor.execute(() -> authenticate(socket));
			} catch (final IOException exception) {
				if (server != null) status = "Direct host error: " + exception.getMessage();
				break;
			}
		}
	}

	private void authenticate(Socket socket) {
		final Peer peer = new Peer(socket);
		try {
			final WireMessage hello = peer.read();
			if (hello == null || !"hello".equals(hello.type) || !sessionCode.equals(hello.code)) throw new IOException("Invalid session code");
			socket.setSoTimeout(0);
			peers.add(peer);
			status = "Direct host — " + peers.size() + " listener(s)";
		} catch (final IOException exception) { peer.close(); }
	}

	private void readJoined(Peer peer) {
		try {
			while (joinedPeer == peer && !closed.get()) {
				final WireMessage message = peer.read();
				if (message == null) break;
				if ("state".equals(message.type) && sessionCode.equals(message.code)
						&& info.u_team.music_player.musicplayer.MusicPlayerManager.getSettingsManager().getSettings().isNearbyMusicReceive()) applyRemote(message);
			}
		} catch (final IOException ignored) {
		} finally {
			if (joinedPeer == peer) { joinedPeer = null; status = "Disconnected"; }
			peer.close();
		}
	}

	private void applyRemote(WireMessage message) {
		if (message.uri == null || message.uri.isBlank()) {
			Minecraft.getInstance().execute(() -> {
				final IMusicPlayer player = info.u_team.music_player.musicplayer.MusicPlayerManager.getPlayer();
				if (player != null) player.getTrackManager().stop();
			});
			return;
		}
		if (message.uri.equals(appliedUri)) { Minecraft.getInstance().execute(() -> syncExisting(message)); return; }
		appliedUri = message.uri;
		final IMusicPlayer player = info.u_team.music_player.musicplayer.MusicPlayerManager.getPlayer();
		if (player == null) return;
		player.getTrackSearch().getTracks(message.uri, result -> {
			final IAudioTrack track = first(result);
			if (track == null) return;
			if (!track.getInfo().isStream()) track.setPosition(Math.clamp(message.position, 0L, Math.max(0L, track.getDuration() - 1L)));
			Minecraft.getInstance().execute(() -> {
				if (!message.uri.equals(appliedUri)) return;
				player.getTrackManager().prepare(track);
				player.getTrackManager().setPaused(message.paused);
			});
		});
	}

	private void syncExisting(WireMessage message) {
		final IMusicPlayer player = info.u_team.music_player.musicplayer.MusicPlayerManager.getPlayer();
		if (player == null) return;
		final IPlayingTrack track = player.getTrackManager().getCurrentTrack();
		if (track == null) return;
		if (!track.getInfo().isStream() && Math.abs(track.getPosition() - message.position) > 2500L) track.setPosition(message.position);
		player.getTrackManager().setPaused(message.paused);
	}

	private IAudioTrack first(ISearchResult result) {
		if (result == null || result.hasError()) return null;
		if (!result.isList()) return result.getTrack();
		if (result.getTrackList() == null) return null;
		if (result.getTrackList().getSelectedTrack() != null) return result.getTrackList().getSelectedTrack();
		return result.getTrackList().getTracks().stream().findFirst().orElse(null);
	}

	private String randomCode() {
		final char[] alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
		final StringBuilder value = new StringBuilder(8);
		for (int index = 0; index < 8; index++) value.append(alphabet[random.nextInt(alphabet.length)]);
		return value.toString();
	}

	private static String normalizeRequiredCode(String code) throws IOException {
		if (code == null || !code.strip().toUpperCase(Locale.ROOT).matches("[A-Z2-9]{8}")) throw new IOException("Session code must contain 8 characters");
		return code.strip().toUpperCase(Locale.ROOT);
	}

	private static String stableUri(IPlayingTrack track) {
		final String uri = track.getInfo().getURI();
		return uri == null || uri.isBlank() ? track.getInfo().getIdentifier() : uri;
	}

	private final class Peer {
		private final Socket socket;
		private final Reader reader;
		private final BufferedWriter writer;
		private Peer(Socket socket) {
			this.socket = socket;
			try {
				reader = new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8);
				writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
			} catch (final IOException exception) { throw new IllegalStateException(exception); }
		}
		private synchronized void write(WireMessage message) throws IOException {
			final String json = gson.toJson(message);
			if (json.length() > MAX_MESSAGE_CHARS) throw new IOException("Message too large");
			writer.write(json); writer.write('\n'); writer.flush();
		}
		private WireMessage read() throws IOException {
			final String line = readBoundedLine(reader, MAX_MESSAGE_CHARS);
			if (line == null) return null;
			try { return gson.fromJson(line, WireMessage.class); }
			catch (final RuntimeException exception) { throw new IOException("Malformed shared-listening message", exception); }
		}
		private void close() { try { socket.close(); } catch (final IOException ignored) { } }
	}

	private static String readBoundedLine(Reader reader, int maximum) throws IOException {
		final StringBuilder line = new StringBuilder(Math.min(maximum, 512));
		while (true) {
			final int value = reader.read();
			if (value < 0) return line.isEmpty() ? null : line.toString();
			if (value == '\n') return line.toString();
			if (value != '\r') line.append((char) value);
			if (line.length() > maximum) throw new IOException("Message too large");
		}
	}

	private record WireMessage(String type, String code, String uri, long position, long duration, boolean paused, boolean stream, String title) { }
}
