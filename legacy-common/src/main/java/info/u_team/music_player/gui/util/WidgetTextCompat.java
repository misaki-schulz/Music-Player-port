// Compact widget-label renderer used where Minecraft's 1.21.x text APIs diverge.
package info.u_team.music_player.gui.util;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class WidgetTextCompat {

	private WidgetTextCompat() {
	}

	public static void drawFitted(GuiGraphics graphics, Font font, Component message,
			int x, int y, int width, int height, int color, float maximumScale) {
		final String text = message == null ? "" : message.getString();
		if (text.isEmpty()) {
			return;
		}

		final int textWidth = font.width(text);
		if (textWidth <= 0) {
			return;
		}
		final float availableWidth = Math.max(1, width - 4);
		final float scale = Math.min(Math.max(0.01F, maximumScale), Math.min(1F, availableWidth / textWidth));
		final float drawX = (x + (width - textWidth * scale) / 2F) / scale;
		final float drawY = (y + (height - font.lineHeight * scale) / 2F) / scale;

		LegacyGuiTransform.transformed(graphics, 0, 0, scale,
				() -> GuiTextCompat.draw(graphics, font, text, Math.round(drawX), Math.round(drawY), color, true));
	}
}
