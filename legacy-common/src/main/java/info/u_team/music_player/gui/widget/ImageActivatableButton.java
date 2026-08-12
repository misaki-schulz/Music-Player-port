// Derived from U Team Core 5.6.2.384 and adapted for Minecraft 1.21.x; see NOTICE.
package info.u_team.music_player.gui.widget;

import info.u_team.music_player.util.RGBA;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class ImageActivatableButton extends ImageButton {

	protected boolean activated;
	protected RGBA activatedColor;

	public ImageActivatableButton(int x, int y, int width, int height, ResourceLocation image, boolean activated, RGBA activatedColor) {
		super(x, y, width, height, image);
		this.activated = activated;
		this.activatedColor = activatedColor;
	}

	public boolean isActivated() {
		return activated;
	}

	public void setActivated(boolean activated) {
		this.activated = activated;
	}

	@Override
	public RGBA getCurrentBackgroundColor(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		return activated ? activatedColor : buttonColor;
	}
}
