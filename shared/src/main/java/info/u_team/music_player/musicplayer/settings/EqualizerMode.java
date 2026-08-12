package info.u_team.music_player.musicplayer.settings;

public enum EqualizerMode {
	OFF("Off"),
	PARAMETRIC("Curve"),
	GRAPHIC("Sliders");

	private final String displayName;

	EqualizerMode(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public static EqualizerMode next(EqualizerMode value) {
		final EqualizerMode[] values = values();
		return values[(value.ordinal() + 1) % values.length];
	}
}
