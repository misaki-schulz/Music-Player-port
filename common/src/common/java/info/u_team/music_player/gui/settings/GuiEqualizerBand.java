package info.u_team.music_player.gui.settings;

import java.util.function.DoubleConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

final class GuiEqualizerBand extends AbstractWidget {

	private final DoubleConsumer onChange;
	private double gain;

	GuiEqualizerBand(int x, int y, int width, int height, String frequency, double gain, DoubleConsumer onChange) {
		super(x, y, width, height, Component.literal(frequency));
		this.gain = Math.clamp(gain, -1, 1);
		this.onChange = onChange;
	}

	@Override
	protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		final int middleX = getX() + getWidth() / 2;
		final int top = getY() + 5;
		final int bottom = getY() + getHeight() - 5;
		final int zero = (top + bottom) / 2;
		final int handle = (int) Math.round(zero - gain * (bottom - top) / 2D);
		graphics.fill(middleX - 2, top, middleX + 2, bottom + 1, 0xFF425564);
		graphics.fill(middleX - 1, top, middleX + 1, bottom + 1, 0xFF77CDEA);
		graphics.fill(getX() + 2, zero, getRight() - 2, zero + 1, 0xFF8A969D);
		graphics.fill(getX() + 2, handle - 3, getRight() - 2, handle + 4, isHoveredOrFocused() ? 0xFFFFFFFF : 0xFFD9E2E8);
		graphics.centeredText(Minecraft.getInstance().font, getMessage(), middleX, bottom + 7, 0xFFFFFFFF);
	}

	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		setFromMouse(event.y());
	}

	@Override
	protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
		setFromMouse(event.y());
	}

	private void setFromMouse(double mouseY) {
		final int top = getY() + 5;
		final int bottom = getY() + getHeight() - 5;
		final int zero = (top + bottom) / 2;
		setGain((zero - mouseY) * 2D / (bottom - top));
	}

	void setGain(double gain) {
		final double normalized = Math.clamp(gain, -1, 1);
		if (Math.abs(this.gain - normalized) >= 0.001) {
			this.gain = normalized;
			onChange.accept(normalized);
		}
	}

	@Override
	public void updateWidgetNarration(NarrationElementOutput output) {
		output.add(NarratedElementType.TITLE, getMessage().getString() + " " + Math.round(gain * 100) + "%");
	}
}
