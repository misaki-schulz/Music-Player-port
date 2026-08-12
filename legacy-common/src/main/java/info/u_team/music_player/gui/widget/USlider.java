// Derived from U Team Core 5.6.2.384 and adapted for legacy Minecraft 1.21.x; see NOTICE.
package info.u_team.music_player.gui.widget;

import info.u_team.music_player.gui.util.WidgetTextCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

public class USlider extends AbstractSliderButton {

	protected final Component prefix;
	protected final Component suffix;
	protected final double minValue;
	protected final double maxValue;
	protected final boolean decimalPrecision;
	protected final boolean drawDescription;
	protected int precision = 1;
	protected OnSliderChange slider;
	protected float scale = 1;

	public USlider(int x, int y, int width, int height, Component prefix, Component suffix, double minValue,
			double maxValue, double value, boolean decimalPrecision, boolean drawDescription, OnSliderChange slider) {
		super(x, y, width, height, Component.empty(), Math.clamp((value - minValue) / (maxValue - minValue), 0, 1));
		this.prefix = prefix;
		this.suffix = suffix;
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.decimalPrecision = decimalPrecision;
		this.drawDescription = drawDescription;
		this.slider = slider;
		updateMessage();
	}

	@Override
	protected void updateMessage() {
		if (!drawDescription || prefix == null || suffix == null) {
			setMessage(Component.empty());
			return;
		}
		final String displayValue = decimalPrecision
				? String.format(java.util.Locale.ROOT, "%." + precision + "f", getValue())
				: Long.toString(Math.round(getValue()));
		setMessage(Component.empty().append(prefix).append(displayValue).append(suffix));
	}

	@Override
	protected void applyValue() {
		if (slider != null) {
			slider.onChange(this);
		}
	}

	@Override
	public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		final Component message = getMessage();
		setMessage(Component.empty());
		try {
			super.renderWidget(graphics, mouseX, mouseY, partialTick);
		} finally {
			setMessage(message);
		}

		final int alphaValue = Math.clamp(Math.round(alpha * 255), 0, 255);
		final int color = alphaValue << 24 | (active ? 0xFFFFFF : 0xA0A0A0);
		WidgetTextCompat.drawFitted(graphics, Minecraft.getInstance().font, message,
				getX(), getY(), getWidth(), getHeight(), color, scale);
	}

	public double getValue() { return value * (maxValue - minValue) + minValue; }
	public int getValueInt() { return (int) Math.round(getValue()); }

	public void setCurrentValue(double currentValue) {
		value = Math.clamp((currentValue - minValue) / (maxValue - minValue), 0, 1);
		updateMessage();
		applyValue();
	}

	public void setPrecision(int precision) { this.precision = Math.max(0, precision); updateMessage(); }
	public void setSlider(OnSliderChange slider) { this.slider = slider; }
	public void setScale(float scale) { this.scale = scale; }
	public float getScale() { return scale; }

	@FunctionalInterface
	public interface OnSliderChange { void onChange(USlider slider); }
}
