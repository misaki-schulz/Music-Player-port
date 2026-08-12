// Shared settings model for every supported Minecraft target; see NOTICE.
package info.u_team.music_player.musicplayer.settings;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import info.u_team.music_player.musicplayer.MusicPlayerManager;

public class Settings {

	public static final float MIN_SPEED = 0.1F;
	public static final float MAX_SPEED = 4F;
	public static final float MIN_PITCH = 0.1F;
	public static final float MAX_PITCH = 3F;
	public static final float MIN_OVERLAY_SCALE = 0.5F;
	public static final float MAX_OVERLAY_SCALE = 3F;
	public static final int EQ_BAND_COUNT = 10;
	public static final float MIN_EQ_GAIN = -24F;
	public static final float MAX_EQ_GAIN = 24F;
	public static final float MIN_EQ_POINT_GAP = 0.025F;
	public static final int MAX_VOLUME = 200;

	private int volume;
	private Repeat repeat;
	private boolean shuffle;
	private ShuffleMode shuffleMode;
	private boolean showIngameOverlay;
	private boolean showIngameMenueOverlay;
	private IngameOverlayPosition ingameOverlayPosition;
	private boolean keyWorkInGui;
	private String mixer;
	private float speed;
	private float pitch;
	private MusicPlayerLanguage language;
	private float overlayScale;

	// Advanced settings are deliberately exposed through Mod Menu only.
	private boolean restoreSession;
	private boolean automaticAudioRecovery;
	private float crossfadeSeconds;
	private boolean duckingEnabled;
	private int duckingPercent;
	private int duckingAttackMillis;
	private int duckingReleaseMillis;
	private EqualizerMode equalizerMode;
	private float[] equalizerGains;
	private float[] equalizerPositions;
	private boolean bassBoost;
	private boolean monoOutput;
	private boolean swapChannels;
	private float channelBalance;
	private VisualizerStyle visualizerStyle;
	private boolean dynamicTheme;
	private boolean interfaceAnimations;
	private boolean backgroundBlur;
	private boolean trackNotifications;
	private boolean showTrackArtwork;
	private boolean miniPlayerDraggable;
	private boolean miniPlayerLocked;
	private boolean miniPlayerSnapToEdges;
	private boolean miniPlayerShowQueue;
	private boolean miniPlayerShowFavorite;
	private MiniPlayerControl[] miniPlayerControls;
	private int miniPlayerX;
	private int miniPlayerY;
	private int miniPlayerWidth;
	private float miniPlayerScale;
	private int artworkCacheMegabytes;
	private boolean discordRichPresence;
	private String discordApplicationId;
	private boolean lyricsEnabled;
	private boolean preferSyncedLyrics;
	private boolean onlineLyricsProvider;
	private boolean nearbyMusicBroadcast;
	private boolean nearbyMusicReceive;
	private int nearbyMusicDistance;
	private boolean shareTrackTitle;

	public Settings() {
		volume = 10;
		repeat = Repeat.NO;
		shuffle = false;
		shuffleMode = ShuffleMode.UNIFORM;
		showIngameOverlay = true;
		showIngameMenueOverlay = false;
		ingameOverlayPosition = IngameOverlayPosition.DOWN_RIGHT;
		keyWorkInGui = true;
		mixer = "";
		speed = 1F;
		pitch = 1F;
		language = MusicPlayerLanguage.ENGLISH;
		overlayScale = 1F;

		restoreSession = true;
		automaticAudioRecovery = true;
		crossfadeSeconds = 0F;
		duckingEnabled = true;
		duckingPercent = 50;
		duckingAttackMillis = 180;
		duckingReleaseMillis = 900;
		equalizerMode = EqualizerMode.OFF;
		equalizerGains = new float[EQ_BAND_COUNT];
		equalizerPositions = defaultEqualizerPositions();
		bassBoost = false;
		monoOutput = false;
		swapChannels = false;
		channelBalance = 0F;
		visualizerStyle = VisualizerStyle.OFF;
		dynamicTheme = false;
		interfaceAnimations = true;
		backgroundBlur = true;
		trackNotifications = true;
		showTrackArtwork = true;
		miniPlayerDraggable = true;
		miniPlayerLocked = false;
		miniPlayerSnapToEdges = true;
		miniPlayerShowQueue = true;
		miniPlayerShowFavorite = true;
		miniPlayerControls = MiniPlayerControl.values();
		miniPlayerX = -1;
		miniPlayerY = -1;
		miniPlayerWidth = 120;
		miniPlayerScale = 1F;
		artworkCacheMegabytes = 256;
		discordRichPresence = false;
		discordApplicationId = "";
		lyricsEnabled = true;
		preferSyncedLyrics = true;
		onlineLyricsProvider = true;
		nearbyMusicBroadcast = false;
		nearbyMusicReceive = false;
		nearbyMusicDistance = 32;
		shareTrackTitle = true;
	}

	public int getVolume() { return volume; }
	public void setVolume(int value) { volume = Math.clamp(value, 0, MAX_VOLUME); save(); }
	public Repeat getRepeat() { return repeat; }
	public void setRepeat(Repeat value) { repeat = value == null ? Repeat.NO : value; save(); }
	public boolean isShuffle() { return shuffle; }
	public void setShuffle(boolean value) { shuffle = value; if (value && (shuffleMode == null || shuffleMode == ShuffleMode.OFF)) shuffleMode = ShuffleMode.UNIFORM; save(); }
	public ShuffleMode getShuffleMode() { return shuffle ? (shuffleMode == null || shuffleMode == ShuffleMode.OFF ? ShuffleMode.UNIFORM : shuffleMode) : ShuffleMode.OFF; }
	public void setShuffleMode(ShuffleMode value) { shuffleMode = value == null ? ShuffleMode.UNIFORM : value; shuffle = shuffleMode != ShuffleMode.OFF; save(); }
	public boolean isShowIngameOverlay() { return showIngameOverlay; }
	public void setShowIngameOverlay(boolean value) { showIngameOverlay = value; save(); }
	public boolean isShowIngameMenueOverlay() { return showIngameMenueOverlay; }
	public void setShowIngameMenueOverlay(boolean value) { showIngameMenueOverlay = value; save(); }
	public IngameOverlayPosition getIngameOverlayPosition() { return ingameOverlayPosition; }
	public void setIngameOverlayPosition(IngameOverlayPosition value) { ingameOverlayPosition = value == null ? IngameOverlayPosition.DOWN_RIGHT : value; save(); }
	public boolean isKeyWorkInGui() { return keyWorkInGui; }
	public void setKeyWorkInGui(boolean value) { keyWorkInGui = value; save(); }
	public String getMixer() { return mixer; }
	public void setMixer(String value) { mixer = value == null ? "" : value; save(); }
	public float getSpeed() { return speed; }
	public void setSpeed(float value) { speed = Math.clamp(value, MIN_SPEED, MAX_SPEED); save(); }
	public float getPitch() { return pitch; }
	public void setPitch(float value) { pitch = Math.clamp(value, MIN_PITCH, MAX_PITCH); save(); }
	public MusicPlayerLanguage getLanguage() { return language; }
	public void setLanguage(MusicPlayerLanguage value) { language = value == null ? MusicPlayerLanguage.ENGLISH : value; save(); }
	public float getOverlayScale() { return overlayScale; }
	public void setOverlayScale(float value) { overlayScale = Math.clamp(value, MIN_OVERLAY_SCALE, MAX_OVERLAY_SCALE); save(); }

	public boolean isRestoreSession() { return restoreSession; }
	public void setRestoreSession(boolean value) { restoreSession = value; save(); }
	public boolean isAutomaticAudioRecovery() { return automaticAudioRecovery; }
	public void setAutomaticAudioRecovery(boolean value) {
		automaticAudioRecovery = value;
		if (MusicPlayerManager.getPlayer() != null) MusicPlayerManager.getPlayer().setAutomaticAudioRecovery(value);
		save();
	}
	public float getCrossfadeSeconds() { return crossfadeSeconds; }
	public void setCrossfadeSeconds(float value) { crossfadeSeconds = Math.clamp(value, 0F, 15F); save(); }
	public boolean isDuckingEnabled() { return duckingEnabled; }
	public void setDuckingEnabled(boolean value) { duckingEnabled = value; save(); }
	public int getDuckingPercent() { return duckingPercent; }
	public void setDuckingPercent(int value) { duckingPercent = Math.clamp(value, 0, 100); save(); }
	public int getDuckingAttackMillis() { return duckingAttackMillis; }
	public void setDuckingAttackMillis(int value) { duckingAttackMillis = Math.clamp(value, 20, 2000); save(); }
	public int getDuckingReleaseMillis() { return duckingReleaseMillis; }
	public void setDuckingReleaseMillis(int value) { duckingReleaseMillis = Math.clamp(value, 50, 5000); save(); }
	public EqualizerMode getEqualizerMode() { return equalizerMode; }
	public void setEqualizerMode(EqualizerMode value) { equalizerMode = value == null ? EqualizerMode.OFF : value; applyEqualizer(); save(); }
	public float[] getEqualizerGains() { return Arrays.copyOf(equalizerGains, equalizerGains.length); }
	public float[] getEqualizerPositions() { return Arrays.copyOf(equalizerPositions, equalizerPositions.length); }
	public void setEqualizerGains(float[] values) {
		if (values == null || values.length != EQ_BAND_COUNT) return;
		for (int band = 0; band < EQ_BAND_COUNT; band++) equalizerGains[band] = Math.clamp(values[band], MIN_EQ_GAIN, MAX_EQ_GAIN);
		applyEqualizer();
		save();
	}
	public void setEqualizerGain(int band, float value) {
		previewEqualizerGain(band, value);
		save();
	}
	public void previewEqualizerGain(int band, float value) {
		if (band < 0 || band >= EQ_BAND_COUNT) return;
		equalizerGains[band] = Math.clamp(value, MIN_EQ_GAIN, MAX_EQ_GAIN);
		applyEqualizer();
	}
	public void setEqualizerPoint(int band, float position, float gain) {
		previewEqualizerPoint(band, position, gain);
		save();
	}
	public void previewEqualizerPoint(int band, float position, float gain) {
		if (band < 0 || band >= EQ_BAND_COUNT) return;
		final float minimum = band == 0 ? 0F : equalizerPositions[band - 1] + MIN_EQ_POINT_GAP;
		final float maximum = band == EQ_BAND_COUNT - 1 ? 1F : equalizerPositions[band + 1] - MIN_EQ_POINT_GAP;
		equalizerPositions[band] = Math.clamp(position, minimum, maximum);
		equalizerGains[band] = Math.clamp(gain, MIN_EQ_GAIN, MAX_EQ_GAIN);
		applyEqualizer();
	}
	public void commitEqualizerEdit() { save(); }
	public void resetEqualizer() { Arrays.fill(equalizerGains, 0F); equalizerPositions = defaultEqualizerPositions(); applyEqualizer(); save(); }
	public boolean isBassBoost() { return bassBoost; }
	public void setBassBoost(boolean value) { bassBoost = value; applyEqualizer(); save(); }
	public boolean isMonoOutput() { return monoOutput; }
	public void setMonoOutput(boolean value) { monoOutput = value; applyChannelMix(); save(); }
	public boolean isSwapChannels() { return swapChannels; }
	public void setSwapChannels(boolean value) { swapChannels = value; applyChannelMix(); save(); }
	public float getChannelBalance() { return channelBalance; }
	public void setChannelBalance(float value) { channelBalance = Math.clamp(value, -1F, 1F); applyChannelMix(); save(); }
	public VisualizerStyle getVisualizerStyle() { return visualizerStyle; }
	public void setVisualizerStyle(VisualizerStyle value) { visualizerStyle = value == null ? VisualizerStyle.OFF : value; MusicPlayerManager.updateVisualizer(this); save(); }
	public boolean isDynamicTheme() { return dynamicTheme; }
	public void setDynamicTheme(boolean value) { dynamicTheme = value; save(); }
	public boolean isInterfaceAnimations() { return interfaceAnimations; }
	public void setInterfaceAnimations(boolean value) { interfaceAnimations = value; save(); }
	public boolean isBackgroundBlur() { return backgroundBlur; }
	public void setBackgroundBlur(boolean value) { backgroundBlur = value; save(); }
	public boolean isTrackNotifications() { return trackNotifications; }
	public void setTrackNotifications(boolean value) { trackNotifications = value; save(); }
	public boolean isShowTrackArtwork() { return showTrackArtwork; }
	public void setShowTrackArtwork(boolean value) { showTrackArtwork = value; save(); }
	public boolean isMiniPlayerDraggable() { return miniPlayerDraggable; }
	public void setMiniPlayerDraggable(boolean value) { miniPlayerDraggable = value; save(); }
	public boolean isMiniPlayerLocked() { return miniPlayerLocked; }
	public void setMiniPlayerLocked(boolean value) { miniPlayerLocked = value; save(); }
	public boolean isMiniPlayerSnapToEdges() { return miniPlayerSnapToEdges; }
	public void setMiniPlayerSnapToEdges(boolean value) { miniPlayerSnapToEdges = value; save(); }
	public boolean isMiniPlayerShowQueue() { return getMiniPlayerControls().contains(MiniPlayerControl.QUEUE); }
	public void setMiniPlayerShowQueue(boolean value) { setMiniPlayerControlVisible(MiniPlayerControl.QUEUE, value); }
	public boolean isMiniPlayerShowFavorite() { return getMiniPlayerControls().contains(MiniPlayerControl.FAVORITE); }
	public void setMiniPlayerShowFavorite(boolean value) { setMiniPlayerControlVisible(MiniPlayerControl.FAVORITE, value); }
	public List<MiniPlayerControl> getMiniPlayerControls() {
		ensureMiniPlayerControls();
		return List.copyOf(Arrays.asList(miniPlayerControls));
	}
	public void setMiniPlayerControlVisible(MiniPlayerControl control, boolean visible) {
		if (control == null) return;
		final ArrayList<MiniPlayerControl> controls = new ArrayList<>(getMiniPlayerControls());
		if (visible && !controls.contains(control)) controls.add(control);
		else if (!visible && controls.size() > 1) controls.remove(control);
		miniPlayerControls = controls.toArray(MiniPlayerControl[]::new);
		syncLegacyControlFlags();
		save();
	}
	public void moveMiniPlayerControl(MiniPlayerControl control, int delta) {
		final ArrayList<MiniPlayerControl> controls = new ArrayList<>(getMiniPlayerControls());
		final int oldIndex = controls.indexOf(control), newIndex = Math.clamp(oldIndex + delta, 0, controls.size() - 1);
		if (oldIndex < 0 || oldIndex == newIndex) return;
		controls.add(newIndex, controls.remove(oldIndex));
		miniPlayerControls = controls.toArray(MiniPlayerControl[]::new);
		save();
	}
	public void resetMiniPlayerControls() { miniPlayerControls = MiniPlayerControl.values(); syncLegacyControlFlags(); save(); }
	public int getMiniPlayerX() { return miniPlayerX; }
	public int getMiniPlayerY() { return miniPlayerY; }
	public void setMiniPlayerPosition(int x, int y) { miniPlayerX = x; miniPlayerY = y; save(); }
	public void setMiniPlayerLayout(int x, int y, int width) { miniPlayerX = x; miniPlayerY = y; miniPlayerWidth = Math.clamp(width, 80, 640); save(); }
	public int getMiniPlayerWidth() { return miniPlayerWidth; }
	public void setMiniPlayerWidth(int value) { miniPlayerWidth = Math.clamp(value, 80, 640); save(); }
	public float getMiniPlayerScale() { return miniPlayerScale; }
	public void setMiniPlayerScale(float value) { miniPlayerScale = Math.clamp(value, MIN_OVERLAY_SCALE, MAX_OVERLAY_SCALE); save(); }
	public void resetMiniPlayerPlacement() { miniPlayerX = -1; miniPlayerY = -1; miniPlayerWidth = 120; miniPlayerScale = 1F; save(); }
	public int getArtworkCacheMegabytes() { return artworkCacheMegabytes; }
	public void setArtworkCacheMegabytes(int value) { artworkCacheMegabytes = Math.clamp(value, 32, 4096); save(); }
	public boolean isDiscordRichPresence() { return discordRichPresence; }
	public void setDiscordRichPresence(boolean value) { discordRichPresence = value; MusicPlayerManager.getDiscordRichPresence().settingsChanged(); save(); }
	public String getDiscordApplicationId() { return discordApplicationId == null ? "" : discordApplicationId; }
	public void setDiscordApplicationId(String value) { discordApplicationId = value == null ? "" : value.strip(); MusicPlayerManager.getDiscordRichPresence().settingsChanged(); save(); }
	public boolean isLyricsEnabled() { return lyricsEnabled; }
	public void setLyricsEnabled(boolean value) { lyricsEnabled = value; save(); }
	public boolean isPreferSyncedLyrics() { return preferSyncedLyrics; }
	public void setPreferSyncedLyrics(boolean value) { preferSyncedLyrics = value; save(); }
	public boolean isOnlineLyricsProvider() { return onlineLyricsProvider; }
	public void setOnlineLyricsProvider(boolean value) { onlineLyricsProvider = value; save(); }
	public boolean isNearbyMusicBroadcast() { return nearbyMusicBroadcast; }
	public void setNearbyMusicBroadcast(boolean value) { nearbyMusicBroadcast = value; save(); }
	public boolean isNearbyMusicReceive() { return nearbyMusicReceive; }
	public void setNearbyMusicReceive(boolean value) { nearbyMusicReceive = value; save(); }
	public int getNearbyMusicDistance() { return nearbyMusicDistance; }
	public void setNearbyMusicDistance(int value) { nearbyMusicDistance = Math.clamp(value, 4, 128); save(); }
	public boolean isShareTrackTitle() { return shareTrackTitle; }
	public void setShareTrackTitle(boolean value) { shareTrackTitle = value; save(); }

	public boolean normalize() {
		boolean changed = false;
		final int normalizedVolume = Math.clamp(volume, 0, MAX_VOLUME);
		if (volume != normalizedVolume) { volume = normalizedVolume; changed = true; }
		if (repeat == null) { repeat = Repeat.NO; changed = true; }
		if (shuffleMode == null) { shuffleMode = ShuffleMode.UNIFORM; changed = true; }
		if (ingameOverlayPosition == null) { ingameOverlayPosition = IngameOverlayPosition.DOWN_RIGHT; changed = true; }
		if (mixer == null) { mixer = ""; changed = true; }
		if (!Float.isFinite(speed) || speed < MIN_SPEED || speed > MAX_SPEED) { speed = 1F; changed = true; }
		if (!Float.isFinite(pitch) || pitch < MIN_PITCH || pitch > MAX_PITCH) { pitch = 1F; changed = true; }
		if (language == null) { language = MusicPlayerLanguage.ENGLISH; changed = true; }
		if (!Float.isFinite(overlayScale) || overlayScale < MIN_OVERLAY_SCALE || overlayScale > MAX_OVERLAY_SCALE) { overlayScale = 1F; changed = true; }
		if (!Float.isFinite(crossfadeSeconds) || crossfadeSeconds < 0F || crossfadeSeconds > 15F) { crossfadeSeconds = 0F; changed = true; }
		final int normalizedDucking = Math.clamp(duckingPercent, 0, 100);
		if (duckingPercent != normalizedDucking) { duckingPercent = normalizedDucking; changed = true; }
		final int normalizedAttack = Math.clamp(duckingAttackMillis, 20, 2000); if (duckingAttackMillis != normalizedAttack) { duckingAttackMillis = normalizedAttack; changed = true; }
		final int normalizedRelease = Math.clamp(duckingReleaseMillis, 50, 5000); if (duckingReleaseMillis != normalizedRelease) { duckingReleaseMillis = normalizedRelease; changed = true; }
		if (equalizerMode == null) { equalizerMode = EqualizerMode.OFF; changed = true; }
		if (equalizerGains == null || equalizerGains.length != EQ_BAND_COUNT) { equalizerGains = new float[EQ_BAND_COUNT]; changed = true; }
		for (int index = 0; index < equalizerGains.length; index++) {
			final float gain = equalizerGains[index];
			if (!Float.isFinite(gain) || gain < MIN_EQ_GAIN || gain > MAX_EQ_GAIN) { equalizerGains[index] = 0F; changed = true; }
		}
		if (!validEqualizerPositions(equalizerPositions)) { equalizerPositions = defaultEqualizerPositions(); changed = true; }
		if (visualizerStyle == null) { visualizerStyle = VisualizerStyle.OFF; changed = true; }
		if (normalizeMiniPlayerControls()) changed = true;
		if (discordApplicationId == null) { discordApplicationId = ""; changed = true; }
		else if (!discordApplicationId.isBlank() && !discordApplicationId.matches("[0-9]{15,24}")) { discordApplicationId = ""; discordRichPresence = false; changed = true; }
		if (!Float.isFinite(channelBalance) || channelBalance < -1F || channelBalance > 1F) { channelBalance = 0F; changed = true; }
		final int normalizedWidth = Math.clamp(miniPlayerWidth, 80, 640);
		if (miniPlayerWidth != normalizedWidth) { miniPlayerWidth = normalizedWidth; changed = true; }
		if (!Float.isFinite(miniPlayerScale) || miniPlayerScale < MIN_OVERLAY_SCALE || miniPlayerScale > MAX_OVERLAY_SCALE) { miniPlayerScale = 1F; changed = true; }
		final int normalizedCache = Math.clamp(artworkCacheMegabytes, 32, 4096);
		if (artworkCacheMegabytes != normalizedCache) { artworkCacheMegabytes = normalizedCache; changed = true; }
		final int normalizedDistance = Math.clamp(nearbyMusicDistance, 4, 128);
		if (nearbyMusicDistance != normalizedDistance) { nearbyMusicDistance = normalizedDistance; changed = true; }
		return changed;
	}

	public boolean isFinite() { return repeat == Repeat.NO; }
	public boolean isSingleRepeat() { return repeat == Repeat.SINGLE; }

	private void save() {
		MusicPlayerManager.getSettingsManager().writeToFile();
	}

	private void applyEqualizer() {
		if (MusicPlayerManager.getPlayer() != null) MusicPlayerManager.getPlayer().setEqualizer(equalizerMode != EqualizerMode.OFF, equalizerGains, equalizerPositions, bassBoost);
	}

	private static float[] defaultEqualizerPositions() {
		final float[] positions = new float[EQ_BAND_COUNT];
		for (int band = 0; band < EQ_BAND_COUNT; band++) positions[band] = band / (EQ_BAND_COUNT - 1F);
		return positions;
	}

	private static boolean validEqualizerPositions(float[] positions) {
		if (positions == null || positions.length != EQ_BAND_COUNT) return false;
		for (int band = 0; band < positions.length; band++) {
			if (!Float.isFinite(positions[band]) || positions[band] < 0F || positions[band] > 1F) return false;
			if (band > 0 && positions[band] - positions[band - 1] < MIN_EQ_POINT_GAP) return false;
		}
		return true;
	}

	private void applyChannelMix() {
		if (MusicPlayerManager.getPlayer() != null) MusicPlayerManager.getPlayer().setChannelMix(monoOutput, channelBalance, swapChannels);
	}

	private void ensureMiniPlayerControls() { if (miniPlayerControls == null || miniPlayerControls.length == 0) normalizeMiniPlayerControls(); }
	private boolean normalizeMiniPlayerControls() {
		final LinkedHashSet<MiniPlayerControl> controls = new LinkedHashSet<>();
		if (miniPlayerControls != null) for (final MiniPlayerControl control : miniPlayerControls) if (control != null) controls.add(control);
		if (controls.isEmpty()) {
			controls.add(MiniPlayerControl.PREVIOUS); controls.add(MiniPlayerControl.PLAY_PAUSE); controls.add(MiniPlayerControl.NEXT);
			if (miniPlayerShowQueue) controls.add(MiniPlayerControl.QUEUE);
			if (miniPlayerShowFavorite) controls.add(MiniPlayerControl.FAVORITE);
		}
		final MiniPlayerControl[] normalized = controls.toArray(MiniPlayerControl[]::new);
		final boolean changed = !Arrays.equals(miniPlayerControls, normalized);
		miniPlayerControls = normalized;
		syncLegacyControlFlags();
		return changed;
	}
	private void syncLegacyControlFlags() {
		miniPlayerShowQueue = Arrays.asList(miniPlayerControls).contains(MiniPlayerControl.QUEUE);
		miniPlayerShowFavorite = Arrays.asList(miniPlayerControls).contains(MiniPlayerControl.FAVORITE);
	}
}
