// Modified for Minecraft 26.2 by misaki-schulz; see NOTICE.
package info.u_team.music_player.musicplayer.settings;

public enum MusicPlayerLanguage {

	ENGLISH("en_us", "English"),
	GERMAN("de_de", "Deutsch"),
	JAPANESE("ja_jp", "日本語"),
	KOREAN("ko_kr", "한국어"),
	PORTUGUESE_BRAZIL("pt_br", "Português (Brasil)"),
	RUSSIAN("ru_ru", "Русский"),
	TURKISH("tr_tr", "Türkçe"),
	CHINESE_SIMPLIFIED("zh_cn", "简体中文"),
	CHINESE_TRADITIONAL("zh_tw", "繁體中文");

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
		final MusicPlayerLanguage[] values = values();
		if (language == null) {
			return ENGLISH;
		}
		return values[(language.ordinal() + 1) % values.length];
	}
}
