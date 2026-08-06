// Derived from U Team Core 5.6.2.384 and modified for Minecraft 26.2; see NOTICE.
package info.u_team.music_player.gui.widget;

import info.u_team.music_player.util.RGBA;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

public class ImageActivatableButton extends ImageButton {

	protected boolean activated;
	protected RGBA activatedColor;

	public ImageActivatableButton(int x, int y, int width, int height, Identifier image, boolean activated, RGBA activatedColor) {
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
	public RGBA getCurrentBackgroundColor(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		return activated ? activatedColor : buttonColor;
	}
}
