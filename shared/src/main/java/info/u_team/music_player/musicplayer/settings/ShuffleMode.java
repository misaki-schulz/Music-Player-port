package info.u_team.music_player.musicplayer.settings;

import static info.u_team.music_player.init.MusicPlayerLocalization.getTranslation;

public enum ShuffleMode {
	OFF("gui.shuffle.off"), UNIFORM("gui.shuffle.uniform"), NO_REPEAT("gui.shuffle.no_repeat"), RATING_WEIGHTED("gui.shuffle.rating_weighted"),
	HISTORY_AWARE("gui.shuffle.history_aware"), ARTIST_SPACING("gui.shuffle.artist_spacing");
	private final String translationKey;
	ShuffleMode(String translationKey){this.translationKey=translationKey;}
	public String getDisplayName(){return getTranslation(translationKey);}
	public static ShuffleMode next(ShuffleMode value){final ShuffleMode[] values=values();return values[(value.ordinal()+1)%values.length];}
}
