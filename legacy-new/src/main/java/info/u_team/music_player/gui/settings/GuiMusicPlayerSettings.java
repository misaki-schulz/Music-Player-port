// Settings UI from the 26.2 port, adapted for legacy Minecraft GUI APIs; see NOTICE.
package info.u_team.music_player.gui.settings;

import static info.u_team.music_player.init.MusicPlayerLocalization.GUI_SETTINGS_LANGUAGE;
import static info.u_team.music_player.init.MusicPlayerLocalization.GUI_SETTINGS_MIXER_DEVICE_SELECTION;
import static info.u_team.music_player.init.MusicPlayerLocalization.GUI_SETTINGS_OVERLAY_SCALE;
import static info.u_team.music_player.init.MusicPlayerLocalization.GUI_SETTINGS_PITCH;
import static info.u_team.music_player.init.MusicPlayerLocalization.GUI_SETTINGS_POSITION_OVERLAY;
import static info.u_team.music_player.init.MusicPlayerLocalization.GUI_SETTINGS_SPEED;
import static info.u_team.music_player.init.MusicPlayerLocalization.GUI_SETTINGS_STATE_OFF;
import static info.u_team.music_player.init.MusicPlayerLocalization.GUI_SETTINGS_STATE_ON;
import static info.u_team.music_player.init.MusicPlayerLocalization.GUI_SETTINGS_TOGGLE_INGAME_OVERLAY;
import static info.u_team.music_player.init.MusicPlayerLocalization.GUI_SETTINGS_TOGGLE_KEY_IN_GUI;
import static info.u_team.music_player.init.MusicPlayerLocalization.GUI_SETTINGS_TOGGLE_MENUE_OVERLAY;
import static info.u_team.music_player.init.MusicPlayerLocalization.getTranslation;

import info.u_team.music_player.gui.BetterScreen;
import info.u_team.music_player.gui.controls.GuiControls;
import info.u_team.music_player.gui.util.LegacyScreenCompat;
import info.u_team.music_player.gui.widget.ActivatableButton;
import info.u_team.music_player.gui.widget.ImageButton;
import info.u_team.music_player.gui.widget.ScrollingText;
import info.u_team.music_player.gui.widget.UButton;
import info.u_team.music_player.gui.widget.USlider;
import info.u_team.music_player.init.MusicPlayerColors;
import info.u_team.music_player.init.MusicPlayerResources;
import info.u_team.music_player.lavaplayer.api.IMusicPlayer;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.musicplayer.settings.IngameOverlayPosition;
import info.u_team.music_player.musicplayer.settings.MusicPlayerLanguage;
import info.u_team.music_player.musicplayer.settings.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class GuiMusicPlayerSettings extends BetterScreen {

	private final Screen previousGui;
	private GuiMusicPlayerSettingsMixerDeviceList mixerDeviceList;
	private GuiControls controls;

	public GuiMusicPlayerSettings(Screen previousGui) {
		super(Component.literal("musicplayersettings"));
		this.previousGui = previousGui;
	}

	@Override
	protected void init() {
		addRenderableWidget(new ImageButton(1, 1, 15, 15, MusicPlayerResources.TEXTURE_BACK,
				button -> minecraft.setScreen(previousGui)));

		final Settings settings = MusicPlayerManager.getSettingsManager().getSettings();
		final IMusicPlayer player = MusicPlayerManager.getPlayer();
		final int columnWidth = width / 2 - 24;
		final int rightX = width / 2 + 14;

		final ActivatableButton keyButton = addRenderableWidget(new ActivatableButton(12, 60, columnWidth, 20,
				toggleMessage(GUI_SETTINGS_TOGGLE_KEY_IN_GUI, settings.isKeyWorkInGui()),
				settings.isKeyWorkInGui(), MusicPlayerColors.LIGHT_GREEN));
		keyButton.setPressable(() -> {
			settings.setKeyWorkInGui(!settings.isKeyWorkInGui());
			keyButton.setActivated(settings.isKeyWorkInGui());
			keyButton.setMessage(toggleMessage(GUI_SETTINGS_TOGGLE_KEY_IN_GUI, settings.isKeyWorkInGui()));
		});

		final ActivatableButton menuOverlayButton = addRenderableWidget(new ActivatableButton(rightX, 60, columnWidth, 20,
				toggleMessage(GUI_SETTINGS_TOGGLE_MENUE_OVERLAY, settings.isShowIngameMenueOverlay()),
				settings.isShowIngameMenueOverlay(), MusicPlayerColors.LIGHT_GREEN));
		menuOverlayButton.setPressable(() -> {
			settings.setShowIngameMenueOverlay(!settings.isShowIngameMenueOverlay());
			menuOverlayButton.setActivated(settings.isShowIngameMenueOverlay());
			menuOverlayButton.setMessage(toggleMessage(GUI_SETTINGS_TOGGLE_MENUE_OVERLAY, settings.isShowIngameMenueOverlay()));
		});

		final ActivatableButton overlayButton = addRenderableWidget(new ActivatableButton(12, 90, columnWidth, 20,
				toggleMessage(GUI_SETTINGS_TOGGLE_INGAME_OVERLAY, settings.isShowIngameOverlay()),
				settings.isShowIngameOverlay(), MusicPlayerColors.LIGHT_GREEN));
		overlayButton.setPressable(() -> {
			settings.setShowIngameOverlay(!settings.isShowIngameOverlay());
			overlayButton.setActivated(settings.isShowIngameOverlay());
			overlayButton.setMessage(toggleMessage(GUI_SETTINGS_TOGGLE_INGAME_OVERLAY, settings.isShowIngameOverlay()));
		});

		final UButton positionButton = addRenderableWidget(new UButton(rightX, 90, columnWidth, 20,
				Component.literal(getTranslation(GUI_SETTINGS_POSITION_OVERLAY) + ": " + getTranslation(settings.getIngameOverlayPosition().getLocalization()))));
		positionButton.setPressable(() -> {
			settings.setIngameOverlayPosition(IngameOverlayPosition.forwardCycle(settings.getIngameOverlayPosition()));
			positionButton.setMessage(Component.literal(getTranslation(GUI_SETTINGS_POSITION_OVERLAY) + ": " + getTranslation(settings.getIngameOverlayPosition().getLocalization())));
		});

		final int resetWidth = 34;
		final int sliderWidth = columnWidth - resetWidth - 4;
		final USlider speedSlider = addRenderableWidget(new USlider(12, 120, sliderWidth, 20,
				Component.literal(getTranslation(GUI_SETTINGS_SPEED) + ": "), Component.literal("x"),
				Settings.MIN_SPEED, Settings.MAX_SPEED, settings.getSpeed(), true, true, slider -> {
					settings.setSpeed((float) slider.getValue());
					player.setSpeed(settings.getSpeed());
				}));
		speedSlider.setPrecision(2);
		addRenderableWidget(new UButton(12 + sliderWidth + 4, 120, resetWidth, 20, Component.literal("1x"),
				button -> speedSlider.setCurrentValue(1)));

		final USlider pitchSlider = addRenderableWidget(new USlider(rightX, 120, sliderWidth, 20,
				Component.literal(getTranslation(GUI_SETTINGS_PITCH) + ": "), Component.literal("x"),
				Settings.MIN_PITCH, Settings.MAX_PITCH, settings.getPitch(), true, true, slider -> {
					settings.setPitch((float) slider.getValue());
					player.setPitch(settings.getPitch());
				}));
		pitchSlider.setPrecision(2);
		addRenderableWidget(new UButton(rightX + sliderWidth + 4, 120, resetWidth, 20, Component.literal("1x"),
				button -> pitchSlider.setCurrentValue(1)));

		final UButton languageButton = addRenderableWidget(new UButton(12, 150, columnWidth, 20,
				Component.literal(getTranslation(GUI_SETTINGS_LANGUAGE) + ": " + settings.getLanguage().getDisplayName())));
		languageButton.setPressable(() -> {
			settings.setLanguage(MusicPlayerLanguage.forwardCycle(settings.getLanguage()));
			minecraft.setScreen(new GuiMusicPlayerSettings(previousGui));
		});

		final USlider overlayScaleSlider = addRenderableWidget(new USlider(rightX, 150, columnWidth, 20,
				Component.literal(getTranslation(GUI_SETTINGS_OVERLAY_SCALE) + ": "), Component.literal("%"),
				Settings.MIN_OVERLAY_SCALE * 100, Settings.MAX_OVERLAY_SCALE * 100, settings.getOverlayScale() * 100,
				false, true, slider -> settings.setOverlayScale((float) slider.getValue() / 100F)));
		overlayScaleSlider.setPrecision(0);

		mixerDeviceList = new GuiMusicPlayerSettingsMixerDeviceList(12, 193, width - 24, Math.max(20, height - 203));
		addWidget(mixerDeviceList);
		controls = new GuiControls(this, 5, width);
		addWidget(controls);
	}

	@Override
	public void tick() {
		controls.tick();
	}

	@Override
	public void resize(Minecraft minecraft, int width, int height) {
		resizeContents(width, height);
	}

	public void resize(int width, int height) {
		resizeContents(width, height);
	}

	private void resizeContents(int width, int height) {
		final ScrollingText titleRender = controls.getTitleRender();
		final ScrollingText authorRender = controls.getAuthorRender();
		LegacyScreenCompat.reinitialize(this, width, height);
		controls.copyTitleRendererState(titleRender);
		controls.copyAuthorRendererState(authorRender);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		mixerDeviceList.render(graphics, mouseX, mouseY, partialTick);
		info.u_team.music_player.gui.util.GuiTextCompat.draw(graphics, minecraft.font, getTranslation(GUI_SETTINGS_MIXER_DEVICE_SELECTION), 13, 177, 0xFFFFFF);
		controls.render(graphics, mouseX, mouseY, partialTick);
	}

	private static Component toggleMessage(String key, boolean enabled) {
		return Component.literal(getTranslation(key) + ": " + getTranslation(enabled ? GUI_SETTINGS_STATE_ON : GUI_SETTINGS_STATE_OFF));
	}
}
