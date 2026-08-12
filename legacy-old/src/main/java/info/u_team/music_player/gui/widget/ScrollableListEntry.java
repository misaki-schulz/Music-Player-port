// Derived from U Team Core 5.6.2.384 and adapted for Minecraft 1.21.2-1.21.8; see NOTICE.
package info.u_team.music_player.gui.widget;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;

public abstract class ScrollableListEntry<T extends ScrollableListEntry<T>> extends ObjectSelectionList.Entry<T> {

	protected final Minecraft minecraft = Minecraft.getInstance();
	private final List<GuiEventListener> children = new ArrayList<>();
	private ScrollableList<?> list;

	protected <B extends GuiEventListener> B addChildren(B listener) {
		children.add(listener);
		return listener;
	}

	void attachList(ScrollableList<?> list) {
		this.list = list;
	}

	protected ScrollableList<?> getList() {
		return list;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		for (final GuiEventListener child : children) {
			if (child.mouseClicked(mouseX, mouseY, button)) {
				return true;
			}
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		for (final GuiEventListener child : children) {
			if (child.mouseReleased(mouseX, mouseY, button)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		for (final GuiEventListener child : children) {
			if (child.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public abstract void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
			int mouseX, int mouseY, boolean hovered, float partialTick);
}
