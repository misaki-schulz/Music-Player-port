// Modified for Minecraft 26.2 by misaki-schulz; see NOTICE.
package info.u_team.music_player.gui.controls;

import info.u_team.music_player.gui.util.GuiTrackUtils;
import info.u_team.music_player.init.MusicPlayerColors;
import info.u_team.music_player.lavaplayer.api.audio.IAudioTrack;
import info.u_team.music_player.lavaplayer.api.queue.ITrackManager;
import info.u_team.music_player.gui.widget.ProgressBar;
import info.u_team.music_player.gui.widget.ScalableText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class GuiMusicProgressBar extends ProgressBar {
	
	private final ScalableText positionRender;
	private final ScalableText durationRender;
	
	public GuiMusicProgressBar(ITrackManager manager, int x, int y, int width, int height, float scale) {
		super(x, y, width, height, MusicPlayerColors.GREY, MusicPlayerColors.GREEN, () -> getProgress(manager), (value) -> updateProgress(manager, value));
		final Font fontRender = Minecraft.getInstance().font;
		positionRender = new ScalableText(fontRender, () -> GuiTrackUtils.getFormattedPosition(manager.getCurrentTrack()), x, y);
		positionRender.setScale(scale);
		positionRender.setColor(MusicPlayerColors.YELLOW);
		positionRender.setTextChanged(renderer -> {
			renderer.setX(getX() - renderer.getTextWidth() - (renderer.getScale() < 1 ? 3 : 5));
			renderer.setY(getY() - (renderer.getScale() < 1 ? 1 : 2));
		});
		durationRender = new ScalableText(fontRender, () -> GuiTrackUtils.getFormattedDuration(manager.getCurrentTrack()), x, y);
		durationRender.setScale(scale);
		durationRender.setColor(MusicPlayerColors.YELLOW);
		durationRender.setTextChanged(renderer -> {
			renderer.setX(getX() + getWidth() + (renderer.getScale() < 1 ? 3 : 5));
			renderer.setY(getY() - (renderer.getScale() < 1 ? 1 : 2));
		});
	}
	
	@Override
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
		
		positionRender.render(guiGraphics, mouseX, mouseY, partialTicks);
		durationRender.render(guiGraphics, mouseX, mouseY, partialTicks);
	}
	
	private static double getProgress(ITrackManager manager) {
		final IAudioTrack track = manager.getCurrentTrack();
		if (track == null) {
			return 0;
		}
		if (track.getInfo().isStream()) {
			return 0.5;
		}
		return (double) track.getPosition() / track.getDuration();
	}
	
	private static void updateProgress(ITrackManager manager, double value) {
		final IAudioTrack track = manager.getCurrentTrack();
		if (track != null) {
			track.setPosition((long) (track.getDuration() * value));
		}
	}
}
