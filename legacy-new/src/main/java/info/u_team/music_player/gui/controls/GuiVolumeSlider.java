package info.u_team.music_player.gui.controls;

import info.u_team.music_player.gui.widget.USlider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

public class GuiVolumeSlider extends USlider {
	
	private boolean clicked;
	
	public GuiVolumeSlider(int x, int y, int width, int height, Component prefix, Component suffix, double minValue, double maxValue, double value, boolean decimalPrecision, boolean drawDescription, OnSliderChange slider) {
		super(x, y, width, height, prefix, suffix, minValue, maxValue, value, decimalPrecision, drawDescription, slider);
	}
	
	@Override
	public void onClick(MouseButtonEvent event, boolean doubleClick) {
		super.onClick(event, doubleClick);
		clicked = true;
	}
	
	@Override
	public void onRelease(MouseButtonEvent event) {
		if (isHoveredOrFocused() && clicked) {
			Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1));
		}
		clicked = false;
	}
}
