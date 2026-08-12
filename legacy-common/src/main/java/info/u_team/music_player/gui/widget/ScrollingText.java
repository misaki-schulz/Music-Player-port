// Derived from U Team Core 5.6.2.384 and adapted for legacy Minecraft 1.21.x; see NOTICE.
package info.u_team.music_player.gui.widget;

import java.util.function.Supplier;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public class ScrollingText extends ScalableText {

	protected int width = 100;
	protected float stepSize = 1;
	protected int speedTime = 20;
	protected int waitTime = 4000;
	protected float moveDifference;
	protected long lastTime;
	protected State state = State.WAITING;

	public ScrollingText(Font font, Supplier<String> textSupplier, float x, float y) {
		super(font, textSupplier, x, y);
	}

	public int getWidth() { return width; }
	public void setWidth(int width) { this.width = width; }
	public void setStepSize(float stepSize) { this.stepSize = stepSize; }
	public void setSpeedTime(int speedTime) { this.speedTime = speedTime; }
	public void setWaitTime(int waitTime) { this.waitTime = waitTime; }

	public void copyState(ScrollingText renderer) {
		setText(textSupplier.get());
		state = renderer.state;
		moveDifference = renderer.moveDifference;
		lastTime = renderer.lastTime;
	}

	@Override
	protected void updatedText() {
		state = State.WAITING;
		moveDifference = 0;
		lastTime = 0;
		super.updatedText();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		// GuiGraphics applies its current pose to the scissor rectangle itself.
		// Pre-transforming it here applied the HUD scale twice and clipped the title
		// and artist completely when the mini-player was enlarged or moved.
		graphics.enableScissor(Math.round(x), Math.round(y), Math.round(x + width), Math.round(y + (font.lineHeight + 1) * scale));
		setText(textSupplier.get());
		renderFont(graphics, movingX(), y + 2 * scale);
		graphics.disableScissor();
	}

	private float movingX() {
		final float scaledTextWidth = getTextWidth();
		if (width >= scaledTextWidth) {
			return x;
		}
		final float maxMove = width - scaledTextWidth;
		if (lastTime == 0) {
			lastTime = System.currentTimeMillis();
		}
		final long now = System.currentTimeMillis();
		if (state == State.WAITING) {
			if (now - lastTime >= waitTime) {
				state = moveDifference >= 0 ? State.LEFT : State.RIGHT;
				lastTime = now;
			}
		} else if (now - lastTime >= speedTime) {
			if (state == State.LEFT ? moveDifference >= maxMove : moveDifference <= 0) {
				moveDifference += state == State.LEFT ? -stepSize : stepSize;
			} else {
				state = State.WAITING;
			}
			lastTime = now;
		}
		return x + moveDifference;
	}

	private enum State { WAITING, LEFT, RIGHT }
}
