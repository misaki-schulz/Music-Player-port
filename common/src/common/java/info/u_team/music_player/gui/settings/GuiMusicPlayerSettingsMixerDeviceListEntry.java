// Modified for Minecraft 26.2 by misaki-schulz; see NOTICE.
package info.u_team.music_player.gui.settings;

import info.u_team.music_player.gui.BetterScrollableListEntry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

class GuiMusicPlayerSettingsMixerDeviceListEntry extends BetterScrollableListEntry<GuiMusicPlayerSettingsMixerDeviceListEntry> {
	
	private final String mixerName;
	
	public GuiMusicPlayerSettingsMixerDeviceListEntry(String mixerName) {
		this.mixerName = mixerName;
	}
	
	@Override
	public void render(GuiGraphicsExtractor guiGraphics, int slotIndex, int entryY, int entryX, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float partialTicks) {
		guiGraphics.text(minecraft.font, mixerName, entryX + 5, entryY + 5, 0xFF0083FF, false);
	}
	
	public String getMixerName() {
		return mixerName;
	}
	
	@Override
	public Component getNarration() {
		return CommonComponents.EMPTY;
	}
	
}
