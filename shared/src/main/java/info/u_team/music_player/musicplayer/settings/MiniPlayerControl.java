package info.u_team.music_player.musicplayer.settings;

import static info.u_team.music_player.init.MusicPlayerLocalization.getTranslation;

public enum MiniPlayerControl {
	PREVIOUS("gui.hud.control.previous", "<"),
	PLAY_PAUSE("gui.hud.control.play_pause", ">"),
	NEXT("gui.hud.control.next", ">"),
	QUEUE("gui.hud.control.queue", "Q"),
	FAVORITE("gui.hud.control.favorite", "+");

	private final String translationKey;
	private final String glyph;
	MiniPlayerControl(String translationKey, String glyph) { this.translationKey = translationKey; this.glyph = glyph; }
	public String getDisplayName() { return getTranslation(translationKey); }
	public String getGlyph() { return glyph; }
}
