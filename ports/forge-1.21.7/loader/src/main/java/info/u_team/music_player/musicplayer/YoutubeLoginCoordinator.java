package info.u_team.music_player.musicplayer;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import info.u_team.music_player.MusicPlayerMod;
import info.u_team.music_player.gui.GuiYoutubeLogin;
import info.u_team.music_player.lavaplayer.api.IYoutubeAuth;
import info.u_team.music_player.lavaplayer.api.IYoutubeAuth.DeviceCode;
import info.u_team.music_player.lavaplayer.api.IYoutubeAuth.PollResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;

public final class YoutubeLoginCoordinator {

	public enum State {
		RESTORING, NEEDS_LOGIN, REQUESTING, WAITING, AUTHENTICATED, ERROR
	}

	private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(task -> {
		final Thread thread = new Thread(task, "musicplayer-youtube-login");
		thread.setDaemon(true);
		return thread;
	});

	private static IYoutubeAuth auth;
	private static YoutubeTokenStore store;
	private static volatile State state = State.RESTORING;
	private static volatile DeviceCode code;
	private static volatile String error;
	private static volatile boolean promptShown;
	private static volatile int generation;

	private YoutubeLoginCoordinator() {
	}

	public static void shutdown() {
		WORKER.shutdownNow();
	}

	public static void initialize() {
		auth = MusicPlayerManager.getPlayer().getYoutubeAuth();
		store = new YoutubeTokenStore(MusicPlayerManager.getFiles().getDirectory());
		final String savedToken;
		try {
			savedToken = store.load();
		} catch (IOException ex) {
			MusicPlayerMod.LOGGER.warn("Could not read saved YouTube login", ex);
			state = State.ERROR;
			error = "Could not read the saved sign-in.";
			return;
		}
		if (savedToken == null) {
			state = State.NEEDS_LOGIN;
			return;
		}
		WORKER.execute(() -> {
			try {
				if (auth.enable(savedToken)) {
					state = State.AUTHENTICATED;
				} else {
					error = "The saved sign-in no longer works.";
					state = State.ERROR;
				}
			} catch (RuntimeException ex) {
				MusicPlayerMod.LOGGER.warn("Could not restore YouTube login", ex);
				error = "Could not restore the YouTube sign-in.";
				state = State.ERROR;
			}
		});
	}

	public static void showStartupPrompt(Minecraft minecraft) {
		if (!promptShown && (state == State.NEEDS_LOGIN || state == State.ERROR) && minecraft.screen instanceof TitleScreen title) {
			promptShown = true;
			minecraft.setScreen(new GuiYoutubeLogin(title));
		}
	}

	public static void showLogin(Minecraft minecraft) {
		promptShown = true;
		minecraft.setScreen(new GuiYoutubeLogin(minecraft.screen));
	}

	public static State getState() {
		return state;
	}

	public static DeviceCode getCode() {
		return code;
	}

	public static String getError() {
		return error;
	}

	public static void beginLogin() {
		if (state == State.REQUESTING || state == State.WAITING || state == State.AUTHENTICATED) {
			return;
		}
		final int current = ++generation;
		code = null;
		error = null;
		state = State.REQUESTING;
		WORKER.execute(() -> runLogin(current));
	}

	public static void skip() {
		generation++;
		code = null;
		state = State.NEEDS_LOGIN;
		promptShown = true;
	}

	private static void runLogin(int current) {
		try {
			final DeviceCode requested = auth.requestDeviceCode();
			if (current != generation) {
				return;
			}
			code = requested;
			state = State.WAITING;
			long interval = requested.intervalMillis();
			while (current == generation && System.currentTimeMillis() < requested.expiresAtMillis()) {
				TimeUnit.MILLISECONDS.sleep(interval);
				if (current != generation) {
					return;
				}
				final PollResult result = auth.poll(requested.deviceCode());
				if (result.isPending()) {
					if ("slow_down".equals(result.error())) {
						interval += 5000;
					}
					continue;
				}
				if (result.error() != null) {
					fail(current, "Google rejected sign-in: " + result.error());
					return;
				}
				store.save(result.refreshToken());
				if (auth.enable(result.refreshToken())) {
					if (current == generation) {
						state = State.AUTHENTICATED;
					}
				} else {
					fail(current, "Google issued a token, but YouTube did not accept it.");
				}
				return;
			}
			fail(current, "Sign-in timed out.");
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			fail(current, "Sign-in was interrupted.");
		} catch (IOException ex) {
			MusicPlayerMod.LOGGER.warn("Could not save YouTube login", ex);
			fail(current, "Could not save the sign-in for the next launch.");
		} catch (RuntimeException ex) {
			MusicPlayerMod.LOGGER.warn("YouTube login failed", ex);
			fail(current, "Could not sign in to YouTube. Check the network and retry.");
		}
	}

	private static void fail(int current, String message) {
		if (current == generation) {
			error = message;
			state = State.ERROR;
		}
	}
}
