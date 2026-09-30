// Derived from U Team Core 5.6.2.384 and modified for Minecraft 26.2; see NOTICE.
package info.u_team.music_player.gui.widget;

import info.u_team.music_player.util.RGBA;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class ImageButton extends UButton {

	protected ResourceLocation image;
	protected RGBA imageColor = WHITE;

	public ImageButton(int x, int y, int width, int height, ResourceLocation image) {
		this(x, y, width, height, image, button -> {
		});
	}

	public ImageButton(int x, int y, int width, int height, ResourceLocation image, OnPress pressable) {
		super(x, y, width, height, Component.empty(), pressable);
		this.image = image;
	}

	public ResourceLocation getImage() {
		return image;
	}

	public void setImage(ResourceLocation image) {
		this.image = image;
	}

	@Override
	protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.renderWidget(graphics, mouseX, mouseY, partialTick);
		final ResourceLocation currentImage = getCurrentImage(graphics, mouseX, mouseY, partialTick);
		graphics.blit(currentImage,
				getX() + 2, getY() + 2,
				getX() + getWidth() - 2, getY() + getHeight() - 2,
				0, 1, 0, 1);
	}

	public ResourceLocation getCurrentImage(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		return image;
	}

	public void setImageColor(RGBA imageColor) {
		this.imageColor = imageColor;
	}
}
