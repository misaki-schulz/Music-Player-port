// Modified for Minecraft 26.2 by misaki-schulz; see NOTICE.
package info.u_team.music_player.render;

import info.u_team.music_player.gui.util.GuiTrackUtils;
import info.u_team.music_player.init.MusicPlayerColors;
import info.u_team.music_player.init.MusicPlayerResources;
import info.u_team.music_player.lavaplayer.api.audio.IAudioTrack;
import info.u_team.music_player.lavaplayer.api.queue.ITrackManager;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.gui.widget.ScalableText;
import info.u_team.music_player.gui.widget.ScrollingText;
import info.u_team.music_player.musicplayer.settings.VisualizerStyle;
import info.u_team.music_player.musicplayer.settings.MiniPlayerControl;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.resources.Identifier;

public class RenderOverlayMusicDisplay implements Renderable {
	
	private final ITrackManager manager;
	
	private final int width;
	private final int height;
	
	private final ScrollingText title;
	private final ScrollingText author;
	
	private final ScalableText position;
	private final ScalableText duration;
	
	public RenderOverlayMusicDisplay() {
		manager = MusicPlayerManager.getPlayer().getTrackManager();
		
		height = 35;
		width = 120;
		
		final Font fontRender = Minecraft.getInstance().font;
		
		title = new ScrollingText(fontRender, () -> GuiTrackUtils.getValueOfPlayingTrack(track -> track.getInfo().getFixedTitle()), 3, 2);
		title.setStepSize(0.5F);
		title.setColor(MusicPlayerColors.YELLOW);
		title.setWidth(114);
		title.setSpeedTime(35);
		
		author = new ScrollingText(fontRender, () -> GuiTrackUtils.getValueOfPlayingTrack(track -> track.getInfo().getFixedAuthor()), 3, 12);
		author.setStepSize(0.5F);
		author.setColor(MusicPlayerColors.YELLOW);
		author.setScale(0.75F);
		author.setWidth(114);
		author.setSpeedTime(35);
		
		position = new ScalableText(fontRender, () -> GuiTrackUtils.getValueOfPlayingTrack(GuiTrackUtils::getFormattedPosition), 6, 28);
		position.setColor(MusicPlayerColors.YELLOW);
		position.setScale(0.5F);
		
		duration = new ScalableText(fontRender, () -> GuiTrackUtils.getValueOfPlayingTrack(GuiTrackUtils::getFormattedDuration), width - 6, 28);
		duration.setTextChanged(renderer -> {
			duration.setX(width - 6 - renderer.getTextWidth());
		});
		duration.setColor(MusicPlayerColors.YELLOW);
		duration.setScale(0.5F);
		
	}
	
	@Override
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
		final IAudioTrack track = manager.getCurrentTrack();
		if (track == null) {
			return;
		}
		guiGraphics.fill(0, 0, width, getHeight(), 0x90212121);
		// Progressbar
		final double progress;
		if (track.getInfo().isStream()) {
			progress = 0.5;
		} else {
			progress = (double) track.getPosition() / track.getDuration();
		}
		
		guiGraphics.fill(6, 23, width - 6, 26, 0x80555555);
		guiGraphics.fill(6, 23, 6 + (int) ((width - 12) * progress), 26, 0xC03E9100);
		
		// Draw strings
		title.render(guiGraphics, mouseX, mouseY, partialTicks);
		author.render(guiGraphics, mouseX, mouseY, partialTicks);
		
		position.render(guiGraphics, mouseX, mouseY, partialTicks);
		duration.render(guiGraphics, mouseX, mouseY, partialTicks);
		renderVisualizer(guiGraphics);
		renderControls(guiGraphics);
	}

	private void renderControls(GuiGraphicsExtractor graphics) {
		final int y = getControlsY();
		final var settings = MusicPlayerManager.getSettingsManager().getSettings();
		final var controls = settings.getMiniPlayerControls();
		final int slot = Math.max(20, 116 / controls.size());
		for (int index = 0; index < controls.size(); index++) {
			final MiniPlayerControl control = controls.get(index);
			final int x = 2 + index * slot, right = index == controls.size() - 1 ? 118 : Math.min(118, x + slot - 2);
			graphics.fill(x, y, right, y + 13, 0x60333333);
			final int iconSize = Math.min(9, right - x - 4);
			final int iconX = x + (right - x - iconSize) / 2;
			graphics.blit(controlIcon(control), iconX, y + 2, iconX + iconSize, y + 2 + iconSize, 0, 1, 0, 1);
		}
	}

	private Identifier controlIcon(MiniPlayerControl control) {
		return switch (control) {
		case PREVIOUS -> MusicPlayerResources.TEXTURE_SKIP_BACK;
		case PLAY_PAUSE -> manager.isPaused() ? MusicPlayerResources.TEXTURE_PLAY : MusicPlayerResources.TEXTURE_PAUSE;
		case NEXT -> MusicPlayerResources.TEXTURE_SKIP_FORWARD;
		case QUEUE -> MusicPlayerResources.TEXTURE_OPEN;
		case FAVORITE -> MusicPlayerResources.TEXTURE_ADD;
		};
	}

	private void renderVisualizer(GuiGraphicsExtractor graphics) {
		final VisualizerStyle style = MusicPlayerManager.getSettingsManager().getSettings().getVisualizerStyle();
		if (style == VisualizerStyle.OFF) return;
		if (style == VisualizerStyle.WAVEFORM || style == VisualizerStyle.MINIMAL_LINE) {
			final float[] points = MusicPlayerManager.getAudioVisualizer().waveform();
			for (int index = 0; index < points.length; index++) {
				final int x = index * width / points.length;
				final int y = (style == VisualizerStyle.MINIMAL_LINE ? 43 : 44) - Math.round(points[index] * (style == VisualizerStyle.MINIMAL_LINE ? 3F : 9F));
				graphics.fill(x, y, x + 2, y + (style == VisualizerStyle.MINIMAL_LINE ? 1 : 2), 0xFF75E0B5);
			}
		} else if (style == VisualizerStyle.PARTICLES) {
			final float[] bands = MusicPlayerManager.getAudioVisualizer().spectrum();
			final long tick = System.nanoTime() / 30_000_000L;
			for (int index = 0; index < bands.length; index += 2) {
				final int x = Math.floorMod(index * 37 + (int) tick, width - 2);
				final int y = 52 - Math.floorMod(index * 13 + (int) (tick / 2), 17) - Math.round(bands[index] * 10F);
				graphics.fill(x, y, x + 2, y + 2, 0xFF75E0B5);
			}
		} else if (style == VisualizerStyle.CIRCLE) {
			final float[] bands = MusicPlayerManager.getAudioVisualizer().spectrum();
			final int centerX = width / 2;
			final int centerY = 52;
			for (int index = 0; index < bands.length; index++) {
				final double angle = Math.PI * 2D * index / bands.length - Math.PI / 2D;
				final int radius = 10 + Math.round(bands[index] * 13F);
				final int x = centerX + (int) Math.round(Math.cos(angle) * radius);
				final int y = centerY + (int) Math.round(Math.sin(angle) * radius);
				graphics.fill(x - 1, y - 1, x + 2, y + 2, 0xFFFFCC66);
			}
		} else {
			final float[] bands = MusicPlayerManager.getAudioVisualizer().spectrum();
			for (int index = 0; index < bands.length; index++) {
				final int left = index * width / bands.length;
				final int right = Math.max(left + 1, (index + 1) * width / bands.length - 1);
				final int bar = Math.max(1, Math.round(bands[index] * 17F));
				graphics.fill(left, 52 - bar, right, 52, 0xFF75E0B5);
			}
		}
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return getControlsY() + 15;
	}

	public int getControlsY() {
		final VisualizerStyle style = MusicPlayerManager.getSettingsManager().getSettings().getVisualizerStyle();
		return style == VisualizerStyle.OFF ? height : style == VisualizerStyle.CIRCLE ? 68 : style == VisualizerStyle.PARTICLES ? 62 : 54;
	}
	
}
