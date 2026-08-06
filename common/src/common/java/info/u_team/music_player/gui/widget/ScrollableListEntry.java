// Derived from U Team Core 5.6.2.384 and modified for Minecraft 26.2; see NOTICE.
package info.u_team.music_player.gui.widget;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;

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
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		for (final GuiEventListener child : children) {
			if (child.mouseClicked(event, doubleClick)) {
				return true;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		for (final GuiEventListener child : children) {
			if (child.mouseReleased(event)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		for (final GuiEventListener child : children) {
			if (child.mouseDragged(event, dragX, dragY)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void visitWidgets(Consumer<AbstractWidget> consumer) {
		children.stream().filter(AbstractWidget.class::isInstance).map(AbstractWidget.class::cast).forEach(consumer);
	}

	@Override
	public final void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float partialTick) {
		render(graphics, -1, getY(), getX(), getWidth(), getHeight(), mouseX, mouseY, hovered, partialTick);
	}

	public abstract void render(GuiGraphicsExtractor graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick);
}
