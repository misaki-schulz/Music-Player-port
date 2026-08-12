package info.u_team.music_player.musicplayer.settings;

public enum VisualizerStyle {
	OFF("Off"),
	SPECTRUM("Spectrum"),
	WAVEFORM("Waveform"),
	CIRCLE("Circle"),
	PARTICLES("Particles"),
	MINIMAL_LINE("Minimal line");

	private final String displayName;

	VisualizerStyle(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}

	public static VisualizerStyle next(VisualizerStyle value) {
		final VisualizerStyle[] values = values();
		return values[(value.ordinal() + 1) % values.length];
	}
}
