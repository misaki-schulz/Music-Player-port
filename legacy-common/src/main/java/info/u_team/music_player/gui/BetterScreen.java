package info.u_team.music_player.gui;

import info.u_team.music_player.musicplayer.MusicPlayerManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Minecraft 1.21.x screen bridge with a configurable no-blur fallback. */
public abstract class BetterScreen extends Screen implements BetterNestedGui {

	protected BetterScreen(Component title) {
		super(title);
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if (MusicPlayerManager.getSettingsManager().getSettings().isBackgroundBlur()) {
			super.renderBackground(graphics, mouseX, mouseY, partialTick);
		} else {
			graphics.fill(0, 0, width, height, 0xE0101418);
		}
	}
}
