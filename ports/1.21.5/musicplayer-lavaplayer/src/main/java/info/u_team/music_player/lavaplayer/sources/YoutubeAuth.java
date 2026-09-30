package info.u_team.music_player.lavaplayer.sources;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;

import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.YoutubeSourceOptions;
import dev.lavalink.youtube.clients.AndroidVr;
import dev.lavalink.youtube.clients.Music;
import dev.lavalink.youtube.clients.Tv;
import dev.lavalink.youtube.clients.TvHtml5Simply;
import dev.lavalink.youtube.clients.Web;
import dev.lavalink.youtube.clients.WebEmbedded;
import info.u_team.music_player.lavaplayer.api.IYoutubeAuth;

public final class YoutubeAuth implements IYoutubeAuth {

	private final YoutubeAudioSourceManager youtube;
	private volatile boolean enabled;

	public YoutubeAuth(AudioPlayerManager playerManager) {
		this(playerManager, createSource());
	}

	YoutubeAuth(AudioPlayerManager playerManager, YoutubeAudioSourceManager youtube) {
		this.youtube = youtube;
		youtube.setPlaylistPageCount(100);
		// Reserve YouTube's place before the generic HTTP source at player startup.
		// Saved watch URLs must never be probed as audio files after login/restore.
		playerManager.registerSourceManager(youtube);
	}

	private static YoutubeAudioSourceManager createSource() {
		// The local signature parser fails on current YouTube player scripts. The
		// cipher service receives stream URLs and player scripts, never OAuth tokens.
		final String cipherUrl = System.getProperty("musicplayer.youtube.cipher.url", "https://cipher.kikkia.dev/");
		final String cipherPassword = System.getProperty("musicplayer.youtube.cipher.password");
		return new YoutubeAudioSourceManager(
				new YoutubeSourceOptions().setRemoteCipher(cipherUrl, cipherPassword, "musicplayer-minecraft"),
				new Music(),
				new Tv(),
				new TvHtml5Simply(),
				new AndroidVr(),
				new Web(),
				new WebEmbedded());
	}

	@Override
	public DeviceCode requestDeviceCode() {
		final JsonBrowser response = youtube.getOauth2Handler().fetchDeviceCode();
		final String verificationUrl = response.get("verification_url").text();
		final String userCode = response.get("user_code").text();
		final String deviceCode = response.get("device_code").text();
		if (verificationUrl == null || userCode == null || deviceCode == null) {
			throw new IllegalStateException("YouTube did not return a usable login code");
		}
		final long interval = Math.max(5, response.get("interval").asLong(5)) * 1000;
		final long lifetime = Math.max(60, response.get("expires_in").asLong(1800)) * 1000;
		return new DeviceCode(verificationUrl, userCode, deviceCode, interval, System.currentTimeMillis() + lifetime);
	}

	@Override
	public PollResult poll(String deviceCode) {
		final JsonBrowser response;
		try {
			response = youtube.getOauth2Handler().fetchRefreshToken(deviceCode);
		} catch (Exception ex) {
			throw new IllegalStateException("Could not check YouTube login", ex);
		}
		final String error = response.get("error").text();
		if (error != null) {
			return new PollResult(null, error);
		}
		final String refreshToken = response.get("refresh_token").text();
		if (refreshToken == null || refreshToken.isBlank()) {
			throw new IllegalStateException("YouTube did not return a refresh token");
		}
		return new PollResult(refreshToken, null);
	}

	@Override
	public synchronized boolean enable(String refreshToken) {
		if (enabled) {
			return true;
		}
		youtube.useOauth2(refreshToken, true);
		if (!youtube.getOauth2Handler().hasAccessToken()) {
			return false;
		}
		enabled = true;
		return true;
	}

	@Override
	public boolean isEnabled() {
		return enabled;
	}
}
