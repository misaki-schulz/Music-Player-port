// Modified for Minecraft 26.2 by misaki-schulz; see NOTICE.
package info.u_team.music_player.musicplayer.settings;

public enum MusicPlayerLanguage {

	ENGLISH("en_us", "English"),
	RUSSIAN("ru_ru", "Русский");

	private final String code;
	private final String displayName;

	MusicPlayerLanguage(String code, String displayName) {
		this.code = code;
		this.displayName = displayName;
	}

	public String getCode() {
		return code;
	}

	public String getDisplayName() {
		return displayName;
	}

	public static MusicPlayerLanguage forwardCycle(MusicPlayerLanguage language) {
		return language == ENGLISH ? RUSSIAN : ENGLISH;
	}
}
