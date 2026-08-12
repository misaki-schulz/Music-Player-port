package info.u_team.music_player.gui.settings;

import static info.u_team.music_player.init.MusicPlayerLocalization.getTranslation;

import java.time.LocalTime;

import info.u_team.music_player.gui.BetterScreen;
import info.u_team.music_player.gui.widget.UButton;
import info.u_team.music_player.gui.widget.USlider;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.musicplayer.SleepTimerManager;
import info.u_team.music_player.util.MinecraftGuiCompat;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Action screen for the transient sleep timer; no persistent advanced configuration is duplicated here. */
public final class GuiSleepTimer extends BetterScreen {

	private final Screen previous;
	private int hour = LocalTime.now().getHour();
	private int minute = LocalTime.now().getMinute();
	private UButton status;

	public GuiSleepTimer(Screen previous) {
		super(Component.literal(getTranslation("gui.sleep.title")));
		this.previous = previous;
	}

	@Override
	protected void init() {
		final SleepTimerManager timer = MusicPlayerManager.getSleepTimerManager();
		addRenderableWidget(new UButton(8, 8, 24, 20, Component.literal("<"), button -> MinecraftGuiCompat.setScreen(minecraft, previous)));
		status = addRenderableWidget(new UButton(40, 8, width - 52, 20, Component.literal(timer.status(MusicPlayerManager.getPlayer()))));
		status.active = false;
		final int buttonWidth = Math.max(48, (width - 36) / 4);
		int x = 12;
		for (final int minutes : new int[] { 15, 30, 60, 120 }) {
			addRenderableWidget(new UButton(x, 48, buttonWidth, 20, Component.literal(getTranslation("gui.sleep.minutes", minutes)), button -> {
				timer.afterMinutes(minutes);
				refreshStatus();
			}));
			x += buttonWidth + 4;
		}
		addRenderableWidget(new UButton(12, 74, (width - 28) / 2, 20, Component.literal(getTranslation("gui.sleep.track")), button -> {
			timer.afterCurrentTrack(MusicPlayerManager.getPlayer());
			refreshStatus();
		}));
		addRenderableWidget(new UButton(width / 2 + 2, 74, (width - 28) / 2, 20, Component.literal(getTranslation("gui.sleep.playlist")), button -> {
			timer.afterCurrentQueue(MusicPlayerManager.getPlayer());
			refreshStatus();
		}));
		addRenderableWidget(new USlider(12, 106, (width - 30) / 2, 20, Component.literal(getTranslation("gui.sleep.hour") + ": "), Component.empty(), 0, 23, hour,
				false, true, slider -> hour = slider.getValueInt()));
		addRenderableWidget(new USlider(width / 2 + 3, 106, (width - 30) / 2, 20, Component.literal(getTranslation("gui.sleep.minute") + ": "), Component.empty(), 0, 59, minute,
				false, true, slider -> minute = slider.getValueInt()));
		addRenderableWidget(new UButton(12, 132, (width - 28) / 2, 20, Component.literal(getTranslation("gui.sleep.stop_time")), button -> {
			timer.atClockTime(hour, minute);
			refreshStatus();
		}));
		addRenderableWidget(new UButton(width / 2 + 2, 132, (width - 28) / 2, 20, Component.literal(getTranslation("gui.sleep.cancel")), button -> {
			timer.cancel();
			refreshStatus();
		}));
	}

	@Override
	public void tick() {
		refreshStatus();
	}

	private void refreshStatus() {
		if (status != null) status.setMessage(Component.literal(MusicPlayerManager.getSleepTimerManager().status(MusicPlayerManager.getPlayer())));
	}
}
