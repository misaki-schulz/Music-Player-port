package info.u_team.music_player.gui;

import info.u_team.music_player.gui.widget.ScrollableListEntry;
import net.minecraft.client.input.MouseButtonEvent;

public abstract class BetterScrollableListEntry<T extends ScrollableListEntry<T>> extends ScrollableListEntry<T> {
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		getList().selectEntry(this);
		return super.mouseClicked(event, doubleClick);
	}
	
}
