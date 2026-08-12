// Derived from U Team Core 5.6.2.384 and adapted for binary-compatible legacy Minecraft 1.21.x rendering; see NOTICE.
package info.u_team.music_player.gui.widget;

import info.u_team.music_player.gui.util.GuiTextureCompat;
import info.u_team.music_player.util.RGBA;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ImageButton extends UButton {

	protected ResourceLocation image;
	protected RGBA imageColor = WHITE;

	public ImageButton(int x, int y, int width, int height, ResourceLocation image) {
		this(x, y, width, height, image, button -> { });
	}

	public ImageButton(int x, int y, int width, int height, ResourceLocation image, OnPress pressable) {
		super(x, y, width, height, Component.empty(), pressable);
		this.image = image;
	}

	public ResourceLocation getImage() { return image; }

	public void setImage(ResourceLocation image) {
		this.image = image;
	}

	@Override
	public void renderString(GuiGraphics graphics, Font font, int color) {
		renderButtonContents(graphics, 0, 0, 0);
	}

	@Override
	protected void renderButtonContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		renderButtonBackground(graphics, mouseX, mouseY, partialTick);
		GuiTextureCompat.blit(graphics, getCurrentImage(graphics, mouseX, mouseY, partialTick),
				getX() + 2, getY() + 2, getX() + getWidth() - 2, getY() + getHeight() - 2);
	}

	public ResourceLocation getCurrentImage(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		return image;
	}

	public void setImageColor(RGBA imageColor) { this.imageColor = imageColor; }
}
