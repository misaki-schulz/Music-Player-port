package info.u_team.music_player.gui;

import java.util.Collection;
import java.util.stream.Collectors;

import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;

public interface BetterNestedGui extends ContainerEventHandler {
	
	default Collection<GuiEventListener> getEventListenersForPos(double mouseX, double mouseY) {
		return children().stream().filter(listener -> listener.isMouseOver(mouseX, mouseY)).collect(Collectors.toList());
	}
	
	@Override
	public default boolean mouseReleased(MouseButtonEvent event) {
		setDragging(false);
		final Collection<GuiEventListener> list = getEventListenersForPos(event.x(), event.y());
		list.forEach(listener -> listener.mouseReleased(event));
		return !list.isEmpty();
	}
	
	@Override
	public default boolean mouseScrolled(double mouseX, double mouseY, double button, double value) {
		final Collection<GuiEventListener> list = getEventListenersForPos(mouseX, mouseY);
		getEventListenersForPos(mouseX, mouseY).forEach(listener -> listener.mouseScrolled(mouseX, mouseY, button, value));
		return !list.isEmpty();
	}
	
}
