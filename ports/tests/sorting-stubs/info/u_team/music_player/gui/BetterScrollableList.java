package info.u_team.music_player.gui;

import java.util.ArrayList;
import java.util.List;

/** Minecraft-free rendering boundary; production list projection is tested directly. */
public class BetterScrollableList<T> {
	private final List<T> entries = new ArrayList<>();
	public BetterScrollableList(int x, int y, int w, int h, int slot, int margin) {}
	public List<T> children() { return entries; }
	protected int addEntry(T entry) { entries.add(entry); return entries.size() - 1; }
	protected void removeEntry(T entry) { entries.remove(entry); }
	protected void clearEntries() { entries.clear(); }
	public void setSelected(T entry) {}
	public void setScrollAmount(double amount) {}
}
