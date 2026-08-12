package info.u_team.music_player.gui;

import info.u_team.music_player.gui.widget.ScrollableListEntry;

public abstract class BetterScrollableListEntry<T extends ScrollableListEntry<T>> extends ScrollableListEntry<T> {
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		getList().selectEntry(this);
		return super.mouseClicked(mouseX, mouseY, button);
	}
	
}
