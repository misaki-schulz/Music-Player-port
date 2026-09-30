// Derived from U Team Core 5.6.2.384 and modified for Minecraft 26.2; see NOTICE.
package info.u_team.music_player.gui.widget;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.joml.Matrix3x2fStack;

import info.u_team.music_player.util.RGBA;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;

public class ScalableText implements Renderable {

	protected final Font font;
	protected Supplier<String> textSupplier;
	protected float x;
	protected float y;
	protected String text = "";
	protected int textWidth = -1;
	protected RGBA color = RGBA.WHITE;
	protected boolean shadow;
	protected float scale = 1;
	protected Consumer<ScalableText> textChanged = renderer -> {
	};

	public ScalableText(Font font, Supplier<String> textSupplier, float x, float y) {
		this.font = font;
		this.textSupplier = Objects.requireNonNull(textSupplier);
		this.x = x;
		this.y = y;
	}

	public float getX() {
		return x;
	}

	public void setX(float x) {
		this.x = x;
	}

	public float getY() {
		return y;
	}

	public void setY(float y) {
		this.y = y;
	}

	public void setColor(RGBA color) {
		this.color = color;
	}

	public float getScale() {
		return scale;
	}

	public void setScale(float scale) {
		this.scale = scale;
	}

	public void setTextChanged(Consumer<ScalableText> textChanged) {
		this.textChanged = textChanged;
	}

	public float getTextWidth() {
		if (textWidth < 0) {
			setText(textSupplier.get());
		}
		return textWidth * scale;
	}

	protected void setText(String newText) {
		newText = newText == null ? "" : newText;
		if (!newText.equals(text) || textWidth < 0) {
			text = newText;
			textWidth = font.width(newText);
			updatedText();
		}
	}

	protected void updatedText() {
		textChanged.accept(this);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		setText(textSupplier.get());
		renderFont(graphics, x, y);
	}

	public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	protected void renderFont(GuiGraphicsExtractor graphics, float drawX, float drawY) {
		final Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.scale(scale, scale);
		graphics.text(font, text, Math.round(drawX / scale), Math.round(drawY / scale), color.getColorARGB(), shadow);
		pose.popMatrix();
	}
}
