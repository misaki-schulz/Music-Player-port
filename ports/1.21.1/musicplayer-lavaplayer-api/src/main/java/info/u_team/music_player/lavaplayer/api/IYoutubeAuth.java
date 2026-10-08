package info.u_team.music_player.lavaplayer.api;

public interface IYoutubeAuth {

	record DeviceCode(String verificationUrl, String userCode, String deviceCode, long intervalMillis, long expiresAtMillis) {
	}

	record PollResult(String refreshToken, String error) {
		public boolean isPending() {
			return "authorization_pending".equals(error) || "slow_down".equals(error);
		}
	}

	DeviceCode requestDeviceCode();

	PollResult poll(String deviceCode);

	boolean enable(String refreshToken);

	boolean isEnabled();
}
