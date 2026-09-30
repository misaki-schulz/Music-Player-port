// Derived from U Team Core 5.6.2.384 and modified for Minecraft 26.2; see NOTICE.
package info.u_team.music_player.gui.widget;

import java.util.function.Consumer;
import java.util.function.Supplier;

import info.u_team.music_player.util.RGBA;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;

public class ProgressBar implements GuiEventListener, Renderable {

	protected Supplier<Double> progress;
	protected Consumer<Double> click;
	protected int x;
	protected int y;
	protected int width;
	protected int height;
	protected RGBA backgroundColor;
	protected RGBA progressColor;
	protected boolean enabled = true;
	protected boolean visible = true;
	protected boolean hovered;
	protected boolean focused;

	public ProgressBar(int x, int y, int width, int height, RGBA backgroundColor, RGBA progressColor, Supplier<Double> progress, Consumer<Double> click) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.backgroundColor = backgroundColor;
		this.progressColor = progressColor;
		this.progress = progress;
		this.click = click;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		if (!visible) {
			return;
		}
		hovered = isMouseOver(mouseX, mouseY);
		graphics.fill(x, y, x + width, y + height, backgroundColor.getColorARGB());
		graphics.fill(x, y, x + (int) (Math.clamp(progress.get(), 0, 1) * width), y + height, progressColor.getColorARGB());
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() != 0 || !enabled || !visible || !isMouseOver(event.x(), event.y())) {
			return false;
		}
		AbstractWidget.playButtonClickSound(Minecraft.getInstance().getSoundManager());
		if (click != null) {
			click.accept((event.x() - x) / width);
		}
		return true;
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return mouseX >= x && mouseY >= y && mouseX < x + width && mouseY < y + height;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	@Override
	public void setFocused(boolean focused) {
		this.focused = focused;
	}

	@Override
	public boolean isFocused() {
		return focused;
	}
}
