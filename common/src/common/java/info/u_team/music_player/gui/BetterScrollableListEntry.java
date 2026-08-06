// Modified for Minecraft 26.2 by misaki-schulz; see NOTICE.
package info.u_team.music_player.gui;

import info.u_team.music_player.gui.widget.ScrollableListEntry;
import net.minecraft.client.input.MouseButtonEvent;

public abstract class BetterScrollableListEntry<T extends ScrollableListEntry<T>> extends ScrollableListEntry<T> {

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (getList() != null) {
			getList().selectEntry(this);
		}
		return super.mouseClicked(event, doubleClick);
	}
}
