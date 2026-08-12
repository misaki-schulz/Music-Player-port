package info.u_team.music_player.server;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/** Optional dedicated-server relay. It deliberately has no Minecraft-version-specific references. */
public final class MusicPlayerServerCompanion implements ModInitializer {

	private static final Logger LOGGER = Logger.getLogger("musicplayer-server");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static volatile RelayServer relay;

	@Override
	public void onInitialize() {
		final Path path = FabricLoader.getInstance().getConfigDir().resolve("musicplayer-server.json");
		final ServerConfig config = loadConfig(path);
		if (!config.enabled) {
			LOGGER.info("Music Player relay is disabled. Review " + path + " and set enabled=true to opt in.");
			return;
		}
		try {
			relay = new RelayServer(config);
			relay.start();
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				final RelayServer current = relay;
				if (current != null) current.close();
			}, "Music Player relay shutdown"));
		} catch (final IOException exception) {
			throw new IllegalStateException("Cannot start Music Player relay on " + config.bindAddress + ":" + config.port, exception);
		}
	}

	private static ServerConfig loadConfig(Path path) {
		try {
			Files.createDirectories(path.toAbsolutePath().getParent());
			if (!Files.exists(path)) {
				final ServerConfig defaults = new ServerConfig();
				Files.writeString(path, GSON.toJson(defaults), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
				return defaults;
			}
			final String json = Files.readString(path, StandardCharsets.UTF_8);
			if (json.length() > 65_536) throw new IOException("Config is larger than 64 KiB");
			final ServerConfig config = GSON.fromJson(json, ServerConfig.class);
			if (config == null) throw new IOException("Config is empty");
			config.normalize();
			return config;
		} catch (final IOException | RuntimeException exception) {
			throw new IllegalStateException("Cannot load Music Player server config " + path, exception);
		}
	}

	private static final class ServerConfig {
		boolean enabled = false;
		String bindAddress = "127.0.0.1";
		int port = 27_842;
		int maximumClients = 64;
		int maximumRooms = 16;
		int maximumListenersPerRoom = 16;
		int minimumStateIntervalMillis = 500;
		List<String> allowedUriSchemes = List.of("http", "https");

		void normalize() {
			if (bindAddress == null || bindAddress.isBlank()) bindAddress = "127.0.0.1";
			port = Math.clamp(port, 1024, 65_535);
			maximumClients = Math.clamp(maximumClients, 2, 1024);
			maximumRooms = Math.clamp(maximumRooms, 1, 256);
			maximumListenersPerRoom = Math.clamp(maximumListenersPerRoom, 1, 128);
			minimumStateIntervalMillis = Math.clamp(minimumStateIntervalMillis, 250, 5000);
			if (allowedUriSchemes == null || allowedUriSchemes.isEmpty()) allowedUriSchemes = List.of("https");
			allowedUriSchemes = allowedUriSchemes.stream().filter(value -> value != null && value.matches("[a-zA-Z][a-zA-Z0-9+.-]{0,31}"))
					.map(value -> value.toLowerCase(Locale.ROOT)).distinct().limit(16).toList();
		}
	}

	private static final class RelayServer implements AutoCloseable {
		private static final int MAX_MESSAGE_CHARS = 16_384;
		private final ServerConfig config;
		private final Set<String> allowedSchemes;
		private final ServerSocket server = new ServerSocket();
		private final AtomicBoolean closed = new AtomicBoolean();
		private final AtomicInteger clientCount = new AtomicInteger();
		private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();
		private final CopyOnWriteArrayList<Peer> peers = new CopyOnWriteArrayList<>();
		private final ExecutorService executor = Executors.newCachedThreadPool(runnable -> {
			final Thread thread = new Thread(runnable, "Music Player server relay");
			thread.setDaemon(true);
			return thread;
		});

		RelayServer(ServerConfig config) throws IOException {
			this.config = config;
			this.allowedSchemes = Set.copyOf(config.allowedUriSchemes);
			server.setReuseAddress(true);
			server.bind(new InetSocketAddress(config.bindAddress, config.port), config.maximumClients);
		}

		void start() {
			LOGGER.warning("Music Player metadata relay explicitly enabled on " + config.bindAddress + ":" + config.port + ". No audio is relayed.");
			executor.execute(this::acceptLoop);
		}

		private void acceptLoop() {
			while (!closed.get()) {
				try {
					final Socket socket = server.accept();
					if (clientCount.incrementAndGet() > config.maximumClients) {
						clientCount.decrementAndGet(); socket.close(); continue;
					}
					socket.setSoTimeout(15_000);
					executor.execute(() -> handle(socket));
				} catch (final IOException exception) {
					if (!closed.get()) LOGGER.log(Level.WARNING, "Music Player relay accept failed", exception);
				}
			}
		}

		private void handle(Socket socket) {
			final Peer peer;
			try { peer = new Peer(socket); }
			catch (final IOException exception) { clientCount.decrementAndGet(); closeSocket(socket); return; }
			peers.add(peer);
			Room room = null;
			try {
				final WireMessage hello = peer.read();
				if (hello == null || !validCode(hello.code)) throw new IOException("Invalid session code");
				final boolean broadcaster = "relay-broadcast".equals(hello.type);
				final boolean listener = "relay-listen".equals(hello.type);
				if (!broadcaster && !listener) throw new IOException("Unsupported relay role");
				room = getOrCreateRoom(hello.code);
				socket.setSoTimeout(0);
				if (broadcaster) handleBroadcaster(room, peer);
				else handleListener(room, peer);
			} catch (final IOException exception) {
				LOGGER.fine("Music Player relay client disconnected: " + exception.getMessage());
			} finally {
				if (room != null) { room.broadcaster.compareAndSet(peer, null); room.listeners.remove(peer); prune(room); }
				peers.remove(peer); peer.close(); clientCount.decrementAndGet();
			}
		}

		private Room getOrCreateRoom(String code) throws IOException {
			Room room = rooms.get(code);
			if (room != null) return room;
			if (rooms.size() >= config.maximumRooms) throw new IOException("Room limit reached");
			room = new Room(code);
			final Room existing = rooms.putIfAbsent(code, room);
			return existing == null ? room : existing;
		}

		private void handleBroadcaster(Room room, Peer peer) throws IOException {
			if (!room.broadcaster.compareAndSet(null, peer)) throw new IOException("Room already has a broadcaster");
			long nextAllowed = 0L;
			while (!closed.get()) {
				final WireMessage message = peer.read();
				if (message == null) return;
				if (!"state".equals(message.type) || !room.code.equals(message.code)) throw new IOException("Invalid state message");
				final long now = System.nanoTime();
				if (now < nextAllowed) throw new IOException("State rate limit exceeded");
				nextAllowed = now + config.minimumStateIntervalMillis * 1_000_000L;
				validateState(message);
				for (final Peer listener : room.listeners) {
					try { listener.write(message); }
					catch (final IOException exception) { room.listeners.remove(listener); listener.close(); }
				}
			}
		}

		private void handleListener(Room room, Peer peer) throws IOException {
			if (room.listeners.size() >= config.maximumListenersPerRoom) throw new IOException("Room listener limit reached");
			room.listeners.add(peer);
			while (!closed.get() && peer.read() != null) throw new IOException("Listeners may not send state");
		}

		private void validateState(WireMessage message) throws IOException {
			if (message.title != null && message.title.length() > 256) throw new IOException("Title too large");
			if (message.uri == null || message.uri.isBlank()) return;
			if (message.uri.length() > 4096) throw new IOException("URI too large");
			try {
				final String scheme = URI.create(message.uri).getScheme();
				if (scheme == null || !allowedSchemes.contains(scheme.toLowerCase(Locale.ROOT))) throw new IOException("URI scheme is not allowed");
			} catch (final IllegalArgumentException exception) { throw new IOException("Malformed URI", exception); }
			if (message.position < 0L || message.duration < 0L) throw new IOException("Negative timing value");
		}

		private void prune(Room room) { if (room.broadcaster.get() == null && room.listeners.isEmpty()) rooms.remove(room.code, room); }

		@Override
		public void close() {
			if (!closed.compareAndSet(false, true)) return;
			try { server.close(); } catch (final IOException ignored) { }
			for (final Peer peer : peers) peer.close();
			peers.clear(); rooms.clear(); executor.shutdownNow();
		}

		private static boolean validCode(String code) { return code != null && code.matches("[A-Z2-9]{8}"); }
		private static void closeSocket(Socket socket) { try { socket.close(); } catch (final IOException ignored) { } }

		private static final class Room {
			final String code;
			final AtomicReference<Peer> broadcaster = new AtomicReference<>();
			final CopyOnWriteArrayList<Peer> listeners = new CopyOnWriteArrayList<>();
			Room(String code) { this.code = code; }
		}

		private static final class Peer {
			private final Socket socket;
			private final Reader reader;
			private final BufferedWriter writer;
			Peer(Socket socket) throws IOException {
				this.socket = socket;
				reader = new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8);
				writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
			}
			WireMessage read() throws IOException {
				final String line = readBoundedLine(reader, MAX_MESSAGE_CHARS);
				if (line == null) return null;
				try { return GSON.fromJson(line, WireMessage.class); }
				catch (final RuntimeException exception) { throw new IOException("Malformed message", exception); }
			}
			synchronized void write(WireMessage message) throws IOException {
				final String json = GSON.toJson(message);
				if (json.length() > MAX_MESSAGE_CHARS) throw new IOException("Message too large");
				writer.write(json); writer.write('\n'); writer.flush();
			}
			void close() { closeSocket(socket); }
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
	}

	private record WireMessage(String type, String code, String uri, long position, long duration, boolean paused, boolean stream, String title) { }
}
