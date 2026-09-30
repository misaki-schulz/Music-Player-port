// Modified for Minecraft 26.2 by misaki-schulz; see NOTICE.
package info.u_team.music_player.gui;

import info.u_team.music_player.gui.widget.ScrollableList;
import net.minecraft.client.gui.components.ObjectSelectionList;

public class BetterScrollableList<T extends ObjectSelectionList.Entry<T>> extends ScrollableList<T> {
	
	public BetterScrollableList(int x, int y, int width, int height, int slotHeight, int sideDistance) {
		super(x, y, width <= 0 ? 1 : width, height <= 0 ? 1 : height, slotHeight, sideDistance);
		setRenderTransparentBorder(true);
	}
	
	public void updateSettings(int x, int y, int width, int height) {
		setX(x);
		setY(y);
		setWidth(width <= 0 ? 1 : width);
		setHeight(height <= 0 ? 1 : height);
	}
	
}
