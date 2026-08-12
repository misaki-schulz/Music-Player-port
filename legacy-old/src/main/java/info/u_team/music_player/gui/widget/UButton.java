// Derived from U Team Core 5.6.2.384 and adapted for Minecraft 1.21.2-1.21.8; see NOTICE.
package info.u_team.music_player.gui.widget;

import info.u_team.music_player.gui.util.WidgetTextCompat;
import info.u_team.music_player.util.RGBA;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class UButton extends AbstractButton {

	protected static final RGBA WHITE = RGBA.WHITE;
	protected static final RGBA LIGHT_GRAY = new RGBA(0xA0A0A0FF);

	protected OnPress pressable;
	protected RGBA buttonColor = WHITE;
	protected RGBA textColor = WHITE;
	protected RGBA disabledTextColor = LIGHT_GRAY;

	public UButton(int x, int y, int width, int height, Component text) {
		this(x, y, width, height, text, button -> {
		});
	}

	public UButton(int x, int y, int width, int height, Component text, OnPress pressable) {
		super(x, y, width, height, text);
		this.pressable = pressable;
	}

	public void setPressable(OnPress pressable) {
		this.pressable = pressable;
	}

	public void setPressable(Runnable runnable) {
		pressable = button -> runnable.run();
	}

	@Override
	public void onPress() {
		pressable.onPress(this);
	}

	@Override
	public void renderString(GuiGraphics graphics, Font font, int color) {
		renderButtonLabel(graphics, font);
	}

	protected void renderButtonContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		renderButtonBackground(graphics, mouseX, mouseY, partialTick);
		renderButtonLabel(graphics, net.minecraft.client.Minecraft.getInstance().font);
	}

	protected void renderButtonBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		// Minecraft 1.21.2-1.21.8 renders the button sprite before renderString.
	}

	protected void renderButtonLabel(GuiGraphics graphics, Font font) {
		final RGBA foreground = active ? textColor : disabledTextColor;
		WidgetTextCompat.drawFitted(graphics, font, getMessage(), getX(), getY(), getWidth(), getHeight(),
				foreground.getColorARGB(alpha), 1F);
	}

	public RGBA getCurrentBackgroundColor(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		return buttonColor;
	}

	public void setButtonColor(RGBA buttonColor) {
		this.buttonColor = buttonColor;
	}

	public void setTextColor(RGBA textColor) {
		this.textColor = textColor;
	}

	public void setDisabledTextColor(RGBA disabledTextColor) {
		this.disabledTextColor = disabledTextColor;
	}

	@Override
	public void updateWidgetNarration(NarrationElementOutput output) {
		defaultButtonNarrationText(output);
	}

	@FunctionalInterface
	public interface OnPress {
		void onPress(UButton button);
	}
}
