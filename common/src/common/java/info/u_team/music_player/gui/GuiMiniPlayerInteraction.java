package info.u_team.music_player.gui;
import static info.u_team.music_player.init.MusicPlayerLocalization.getTranslation;

import org.joml.Matrix3x2fStack;

import info.u_team.music_player.gui.widget.ImageButton;
import info.u_team.music_player.init.MusicPlayerResources;
import info.u_team.music_player.lavaplayer.api.audio.IPlayingTrack;
import info.u_team.music_player.lavaplayer.api.queue.ITrackManager;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.musicplayer.MusicPlayerUtils;
import info.u_team.music_player.musicplayer.settings.Settings;
import info.u_team.music_player.musicplayer.settings.MiniPlayerControl;
import info.u_team.music_player.render.OverlayPlacement;
import info.u_team.music_player.render.RenderOverlayMusicDisplay;
import info.u_team.music_player.util.MinecraftGuiCompat;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/** Temporarily releases the cursor and turns the existing HUD mini-player into a clickable control surface. */
public final class GuiMiniPlayerInteraction extends BetterScreen {

	private RenderOverlayMusicDisplay overlay;
	private int overlayX;
	private int overlayY;
	private float overlayScale;
	private ImageButton pause;

	public GuiMiniPlayerInteraction() {
		super(Component.literal(getTranslation("gui.mini.controls")));
	}

	@Override
	protected void init() {
		overlay = new RenderOverlayMusicDisplay();
		final Settings settings = MusicPlayerManager.getSettingsManager().getSettings();
		overlayScale = OverlayPlacement.scale(settings, width, height, overlay.getWidth(), overlay.getHeight());
		final int renderedWidth = Math.round(overlay.getWidth() * overlayScale);
		final int renderedHeight = Math.round(overlay.getHeight() * overlayScale);
		overlayX = OverlayPlacement.x(settings, width, renderedWidth);
		overlayY = OverlayPlacement.y(settings, height, renderedHeight);

		final int controlsY = overlayY + Math.round(overlay.getControlsY() * overlayScale);
		final var controls = settings.getMiniPlayerControls();
		for (int index = 0; index < controls.size(); index++) addControl(index, controls.size(), controlsY, controls.get(index));
	}

	private void addControl(int index, int count, int y, MiniPlayerControl control) {
		final int slot = Math.max(20, 116 / count);
		final int localX = 2 + index * slot;
		final int localRight = index == count - 1 ? 118 : Math.min(118, localX + slot - 2);
		final int x = overlayX + Math.round(localX * overlayScale);
		final int controlWidth = Math.max(12, Math.round((localRight - localX) * overlayScale));
		final int controlHeight = Math.max(10, Math.round(13 * overlayScale));
		final Runnable action = switch (control) { case PREVIOUS -> MusicPlayerUtils::skipBack; case PLAY_PAUSE -> this::togglePause; case NEXT -> MusicPlayerUtils::skipForward; case QUEUE -> () -> MinecraftGuiCompat.setScreen(minecraft, new GuiMusicPlayer()); case FAVORITE -> this::toggleFavorite; };
		final ImageButton button = addRenderableWidget(new ImageButton(x, y, controlWidth, controlHeight, icon(control)));
		button.setPressable(action);
		if (control == MiniPlayerControl.PLAY_PAUSE) pause = button;
	}

	private net.minecraft.resources.Identifier icon(MiniPlayerControl control) {
		return switch (control) { case PREVIOUS -> MusicPlayerResources.TEXTURE_SKIP_BACK; case PLAY_PAUSE -> MusicPlayerManager.getPlayer().getTrackManager().isPaused() ? MusicPlayerResources.TEXTURE_PLAY : MusicPlayerResources.TEXTURE_PAUSE; case NEXT -> MusicPlayerResources.TEXTURE_SKIP_FORWARD; case QUEUE -> MusicPlayerResources.TEXTURE_OPEN; case FAVORITE -> MusicPlayerResources.TEXTURE_ADD; };
	}

	private void togglePause() {
		final ITrackManager manager = MusicPlayerManager.getPlayer().getTrackManager();
		if (manager.getCurrentTrack() != null) manager.setPaused(!manager.isPaused());
		if (pause != null) pause.setImage(icon(MiniPlayerControl.PLAY_PAUSE));
	}

	private void toggleFavorite() {
		final IPlayingTrack track = MusicPlayerManager.getPlayer().getTrackManager().getCurrentTrack();
		if (track != null) MusicPlayerManager.getLibraryStateManager().toggleFavorite(track.getInfo());
	}

	@Override
	public void tick() {
		if (pause != null) pause.setImage(icon(MiniPlayerControl.PLAY_PAUSE));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		if (MusicPlayerManager.getPlayer().getTrackManager().getCurrentTrack() != null) {
			final Matrix3x2fStack pose = graphics.pose();
			pose.pushMatrix();
			pose.translate(overlayX, overlayY);
			pose.scale(overlayScale, overlayScale);
			overlay.extractRenderState(graphics, 0, 0, partialTick);
			pose.popMatrix();
		}
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		// Keep the world visible: this is an interaction layer, not a second player/settings screen.
	}

	@Override
	public boolean isPauseScreen() { return false; }

}
