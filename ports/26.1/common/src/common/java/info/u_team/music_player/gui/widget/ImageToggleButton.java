// Derived from U Team Core 5.6.2.384 and modified for Minecraft 26.2; see NOTICE.
package info.u_team.music_player.gui.widget;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.resources.Identifier;

public class ImageToggleButton extends ImageButton {

	protected Identifier toggleImage;
	protected boolean toggled;

	public ImageToggleButton(int x, int y, int width, int height, Identifier image, Identifier toggleImage, boolean toggled) {
		this(x, y, width, height, image, toggleImage, toggled, button -> {
		});
	}

	public ImageToggleButton(int x, int y, int width, int height, Identifier image, Identifier toggleImage, boolean toggled, OnPress pressable) {
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
	public void onPress(InputWithModifiers input) {
		toggled = !toggled;
		super.onPress(input);
	}

	@Override
	public Identifier getCurrentImage(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		return toggled ? toggleImage : image;
	}
}
