// Derived from U Team Core 5.6.2.384 and adapted for legacy Minecraft 1.21.x; see NOTICE.
package info.u_team.music_player.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;

public abstract class ScrollableList<T extends ObjectSelectionList.Entry<T>> extends ObjectSelectionList<T> {

	protected int sideDistance;
	protected boolean renderTransparentBorder;
	protected int transparentBorderSize = 4;

	public ScrollableList(int x, int y, int width, int height, int slotHeight, int sideDistance) {
		super(Minecraft.getInstance(), width, height, y, slotHeight);
		setX(x);
		this.sideDistance = sideDistance;
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
	protected void renderListItems(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.renderListItems(graphics, mouseX, mouseY, partialTick);
		if (renderTransparentBorder) {
			graphics.fillGradient(getX(), getY(), getRight(), getY() + transparentBorderSize, 0xFF000000, 0);
			graphics.fillGradient(getX(), getBottom() - transparentBorderSize, getRight(), getBottom(), 0, 0xFF000000);
		}
	}
}
