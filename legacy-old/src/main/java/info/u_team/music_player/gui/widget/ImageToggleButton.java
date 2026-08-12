// Derived from U Team Core 5.6.2.384 and adapted for Minecraft 1.21.2-1.21.8; see NOTICE.
package info.u_team.music_player.gui.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class ImageToggleButton extends ImageButton {

	protected ResourceLocation toggleImage;
	protected boolean toggled;

	public ImageToggleButton(int x, int y, int width, int height, ResourceLocation image, ResourceLocation toggleImage, boolean toggled) {
		this(x, y, width, height, image, toggleImage, toggled, button -> {
		});
	}

	public ImageToggleButton(int x, int y, int width, int height, ResourceLocation image, ResourceLocation toggleImage, boolean toggled, OnPress pressable) {
		super(x, y, width, height, image, pressable);
		this.toggleImage = toggleImage;
		this.toggled = toggled;
	}

	public boolean isToggled() {
		return toggled;
	}

	public void setToggled(boolean toggled) {
		this.toggled = toggled;
	}

	@Override
	public void onPress() {
		toggled = !toggled;
		super.onPress();
	}

	@Override
	public ResourceLocation getCurrentImage(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		return toggled ? toggleImage : image;
	}
}
