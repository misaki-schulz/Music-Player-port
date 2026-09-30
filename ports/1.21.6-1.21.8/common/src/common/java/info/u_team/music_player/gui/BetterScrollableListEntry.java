// Modified for Minecraft 26.2 by misaki-schulz; see NOTICE.
package info.u_team.music_player.gui;

import info.u_team.music_player.gui.widget.ScrollableListEntry;

public abstract class BetterScrollableListEntry<T extends ScrollableListEntry<T>> extends ScrollableListEntry<T> {

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (getList() != null) {
			getList().selectEntry(this);
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}
}
