package info.u_team.music_player.gui.settings;

import static info.u_team.music_player.init.MusicPlayerLocalization.getTranslation;

import info.u_team.music_player.gui.BetterScreen;
import info.u_team.music_player.gui.widget.ActivatableButton;
import info.u_team.music_player.gui.widget.UButton;
import info.u_team.music_player.init.MusicPlayerColors;
import info.u_team.music_player.lavaplayer.api.IMusicPlayer;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.musicplayer.settings.Settings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class GuiMusicEqualizer extends BetterScreen {

	private static final String[] FREQUENCIES = { "32", "64", "125", "250", "500", "1k", "2k", "4k", "8k", "16k" };
	private final Screen previousScreen;

	public GuiMusicEqualizer(Screen previousScreen) {
		super(Component.literal("Equalizer"));
		this.previousScreen = previousScreen;
	}

	@Override
	protected void init() {
		final Settings settings = MusicPlayerManager.getSettingsManager().getSettings();
		final IMusicPlayer player = MusicPlayerManager.getPlayer();
		addRenderableWidget(new UButton(8, 8, 50, 20, Component.literal(getTranslation("gui.equalizer.back")), button -> minecraft.setScreen(previousScreen)));

		final int controlsWidth = Math.min(440, width - 24);
		final int left = (width - controlsWidth) / 2;
		final int toggleWidth = (controlsWidth - 8) / 2;
		final ActivatableButton equalizerButton = addRenderableWidget(new ActivatableButton(left, 39, toggleWidth, 20,
				Component.literal(getTranslation("gui.equalizer.enabled")), settings.isEqualizerEnabled(), MusicPlayerColors.LIGHT_GREEN));
		equalizerButton.setPressable(() -> {
			settings.setEqualizerEnabled(!settings.isEqualizerEnabled());
			player.setEqualizerEnabled(settings.isEqualizerEnabled());
			equalizerButton.setActivated(settings.isEqualizerEnabled());
		});
		final ActivatableButton bassButton = addRenderableWidget(new ActivatableButton(left + toggleWidth + 8, 39, toggleWidth, 20,
				Component.literal(getTranslation("gui.equalizer.bass_boost")), settings.isBassBoostEnabled(), MusicPlayerColors.LIGHT_GREEN));
		bassButton.setPressable(() -> {
			settings.setBassBoostEnabled(!settings.isBassBoostEnabled());
			player.setBassBoostEnabled(settings.isBassBoostEnabled());
			bassButton.setActivated(settings.isBassBoostEnabled());
		});

		final int bandWidth = controlsWidth / FREQUENCIES.length;
		final int railHeight = Math.max(55, height - 139);
		final float[] gains = settings.getEqualizerGains();
		for (int band = 0; band < FREQUENCIES.length; band++) {
			final int index = band;
			addRenderableWidget(new GuiEqualizerBand(left + band * bandWidth + 2, 69, bandWidth - 4, railHeight,
					FREQUENCIES[band], gains[band], gain -> {
						settings.setEqualizerGain(index, (float) gain);
						player.setEqualizerGains(settings.getEqualizerGains());
					}));
		}
		addRenderableWidget(new UButton(left, height - 30, 85, 20, Component.literal(getTranslation("gui.settings.reset")), button -> {
			settings.resetEqualizer();
			player.setEqualizerGains(settings.getEqualizerGains());
			minecraft.setScreen(new GuiMusicEqualizer(previousScreen));
		}));
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		super.render(graphics, mouseX, mouseY, partialTick);
		graphics.drawCenteredString(font, getTranslation("gui.equalizer.title"), width / 2, 14, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		minecraft.setScreen(previousScreen);
	}
}
