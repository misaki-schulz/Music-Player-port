// Derived from U Team Core 5.6.2.384 and modified for Minecraft 26.2; see NOTICE.
package info.u_team.music_player.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;

public abstract class ScrollableList<T extends ObjectSelectionList.Entry<T>> extends ObjectSelectionList<T> {

	protected int sideDistance;
	protected boolean renderTransparentBorder;
	protected int transparentBorderSize = 4;

	public ScrollableList(int x, int y, int width, int height, int slotHeight, int sideDistance) {
		super(Minecraft.getInstance(), width, height, y, y + height, slotHeight);
		setRenderBackground(false);
		setRenderTopAndBottom(false);
		setX(x);
		this.sideDistance = sideDistance;
	}

	public int getX() { return x0; }
	public int getY() { return y0; }
	public int getWidth() { return width; }
	public int getHeight() { return y1 - y0; }
	public void setX(int x) { setLeftPos(x); }
	public void setY(int y) { int visibleHeight = getHeight(); y0 = y; y1 = y + visibleHeight; }
	public void setWidth(int width) { this.width = width; x1 = x0 + width; }
	public void setHeight(int height) {
		int left = x0;
		updateSize(width, y0 + height, y0, y0 + height);
		setLeftPos(left);
	}

	public void setRenderTransparentBorder(boolean renderTransparentBorder) {
		this.renderTransparentBorder = renderTransparentBorder;
	}

	@Override
	protected int addEntry(T entry) {
		if (entry instanceof ScrollableListEntry<?> localEntry) {
			localEntry.attachList(this);
		}
		return super.addEntry(entry);
	}

	@SuppressWarnings("unchecked")
	public void selectEntry(ObjectSelectionList.Entry<?> entry) {
		setSelected((T) entry);
	}

	@Override
	public int getRowWidth() {
		return getWidth() - sideDistance;
	}

	@Override
	protected int getScrollbarPosition() {
		return (getX() + getWidth()) - 5;
	}

	@Override
	protected void renderList(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.renderList(graphics, mouseX, mouseY, partialTick);
		if (renderTransparentBorder) {
			graphics.fillGradient(getX(), getY(), (getX() + getWidth()), getY() + transparentBorderSize, 0xFF000000, 0);
			graphics.fillGradient(getX(), (getY() + getHeight()) - transparentBorderSize, (getX() + getWidth()), (getY() + getHeight()), 0, 0xFF000000);
		}
	}
}
