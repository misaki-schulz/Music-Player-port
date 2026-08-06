// Derived from U Team Core 5.6.2.384 and modified for Minecraft 26.2; see NOTICE.
package info.u_team.music_player.gui.widget;

import info.u_team.music_player.util.RGBA;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class UButton extends AbstractButton {

	private static final WidgetSprites SPRITES = new WidgetSprites(
			Identifier.withDefaultNamespace("widget/button"),
			Identifier.withDefaultNamespace("widget/button_disabled"),
			Identifier.withDefaultNamespace("widget/button_highlighted"));

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
	public void onPress(InputWithModifiers input) {
		pressable.onPress(this);
	}

	@Override
	protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		final RGBA background = getCurrentBackgroundColor(graphics, mouseX, mouseY, partialTick);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SPRITES.get(active, isHoveredOrFocused()), getX(), getY(), getWidth(), getHeight(), background.getColorARGB(getAlpha()));

		final RGBA foreground = active ? textColor : disabledTextColor;
		final Component label = getMessage().copy().withColor(foreground.getColorARGB() & 0x00FFFFFF);
		extractScrollingStringOverContents(
				graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE),
				label,
				2);
	}

	public RGBA getCurrentBackgroundColor(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
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
