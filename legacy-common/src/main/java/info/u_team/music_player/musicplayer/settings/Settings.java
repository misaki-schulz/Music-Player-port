// Modified for Minecraft 26.2 by misaki-schulz; see NOTICE.
package info.u_team.music_player.musicplayer.settings;

import info.u_team.music_player.musicplayer.MusicPlayerManager;

public class Settings {

	public static final float MIN_SPEED = 0.1F;
	public static final float MAX_SPEED = 4F;
	public static final float MIN_PITCH = 0.1F;
	public static final float MAX_PITCH = 3F;
	public static final float MIN_OVERLAY_SCALE = 0.5F;
	public static final float MAX_OVERLAY_SCALE = 2F;
	
	private int volume;
	
	private Repeat repeat;
	
	private boolean shuffle;
	
	private boolean showIngameOverlay;
	private boolean showIngameMenueOverlay;
	
	private IngameOverlayPosition ingameOverlayPosition;
	
	private boolean keyWorkInGui;

	private String mixer;
	private float speed;
	private float pitch;
	private MusicPlayerLanguage language;
	private float overlayScale;
	
	public Settings() {
		volume = 10;
		repeat = Repeat.NO;
		shuffle = false;
		showIngameOverlay = true;
		showIngameMenueOverlay = false;
		ingameOverlayPosition = IngameOverlayPosition.DOWN_RIGHT;
		keyWorkInGui = true;
		mixer = "";
		speed = 1F;
		pitch = 1F;
		language = MusicPlayerLanguage.ENGLISH;
		overlayScale = 1F;
	}
	
	public int getVolume() {
		return volume;
	}
	
	public void setVolume(int volume) {
		this.volume = volume;
		save();
	}
	
	public Repeat getRepeat() {
		return repeat;
	}
	
	public void setRepeat(Repeat repeat) {
		this.repeat = repeat;
		save();
	}
	
	public boolean isShuffle() {
		return shuffle;
	}
	
	public void setShuffle(boolean shuffle) {
		this.shuffle = shuffle;
		save();
	}
	
	public boolean isShowIngameOverlay() {
		return showIngameOverlay;
	}
	
	public void setShowIngameOverlay(boolean showIngameOverlay) {
		this.showIngameOverlay = showIngameOverlay;
		save();
	}
	
	public boolean isShowIngameMenueOverlay() {
		return showIngameMenueOverlay;
	}
	
	public void setShowIngameMenueOverlay(boolean showIngameMenueOverlay) {
		this.showIngameMenueOverlay = showIngameMenueOverlay;
		save();
	}
	
	public IngameOverlayPosition getIngameOverlayPosition() {
		return ingameOverlayPosition;
	}
	
	public void setIngameOverlayPosition(IngameOverlayPosition ingameOverlayPosition) {
		this.ingameOverlayPosition = ingameOverlayPosition;
		save();
	}
	
	public boolean isKeyWorkInGui() {
		return keyWorkInGui;
	}
	
	public void setKeyWorkInGui(boolean keyWorkInGui) {
		this.keyWorkInGui = keyWorkInGui;
		save();
	}

	public String getMixer() {
		return mixer;
	}

	public void setMixer(String mixer) {
		this.mixer = mixer == null ? "" : mixer;
		save();
	}

	public float getSpeed() {
		return speed;
	}

	public void setSpeed(float speed) {
		this.speed = Math.clamp(speed, MIN_SPEED, MAX_SPEED);
		save();
	}

	public float getPitch() {
		return pitch;
	}

	public void setPitch(float pitch) {
		this.pitch = Math.clamp(pitch, MIN_PITCH, MAX_PITCH);
		save();
	}

	public MusicPlayerLanguage getLanguage() {
		return language;
	}

	public void setLanguage(MusicPlayerLanguage language) {
		this.language = language == null ? MusicPlayerLanguage.ENGLISH : language;
		save();
	}

	public float getOverlayScale() {
		return overlayScale;
	}

	public void setOverlayScale(float overlayScale) {
		this.overlayScale = Math.clamp(overlayScale, MIN_OVERLAY_SCALE, MAX_OVERLAY_SCALE);
		save();
	}

	public boolean normalize() {
		boolean changed = false;
		final int normalizedVolume = Math.clamp(volume, 0, 100);
		if (volume != normalizedVolume) {
			volume = normalizedVolume;
			changed = true;
		}
		if (repeat == null) {
			repeat = Repeat.NO;
			changed = true;
		}
		if (ingameOverlayPosition == null) {
			ingameOverlayPosition = IngameOverlayPosition.DOWN_RIGHT;
			changed = true;
		}
		if (mixer == null) {
			mixer = "";
			changed = true;
		}
		if (!Float.isFinite(speed) || speed < MIN_SPEED || speed > MAX_SPEED) {
			speed = 1F;
			changed = true;
		}
		if (!Float.isFinite(pitch) || pitch < MIN_PITCH || pitch > MAX_PITCH) {
			pitch = 1F;
			changed = true;
		}
		if (language == null) {
			language = MusicPlayerLanguage.ENGLISH;
			changed = true;
		}
		if (!Float.isFinite(overlayScale) || overlayScale < MIN_OVERLAY_SCALE || overlayScale > MAX_OVERLAY_SCALE) {
			overlayScale = 1F;
			changed = true;
		}
		return changed;
	}
	
	public boolean isFinite() {
		return repeat == Repeat.NO;
	}
	
	public boolean isSingleRepeat() {
		return repeat == Repeat.SINGLE;
	}
	
	private void save() {
		MusicPlayerManager.getSettingsManager().writeToFile();
	}
}
