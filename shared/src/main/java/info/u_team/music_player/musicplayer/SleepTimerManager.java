package info.u_team.music_player.musicplayer;

import static info.u_team.music_player.init.MusicPlayerLocalization.getTranslation;

import java.time.Duration;
import java.time.ZonedDateTime;

import info.u_team.music_player.lavaplayer.api.IMusicPlayer;
import info.u_team.music_player.lavaplayer.api.queue.ITrackManager;

/** Runtime-only sleep timer. Arming it is an explicit action and is never restored on startup. */
public final class SleepTimerManager {

	private volatile long deadlineEpochMillis;

	public void afterMinutes(int minutes) {
		cancel();
		deadlineEpochMillis = System.currentTimeMillis() + Math.max(1, minutes) * 60_000L;
	}

	public void atClockTime(int hour, int minute) {
		cancel();
		final ZonedDateTime now = ZonedDateTime.now();
		ZonedDateTime target = now.withHour(Math.clamp(hour, 0, 23)).withMinute(Math.clamp(minute, 0, 59)).withSecond(0).withNano(0);
		if (!target.isAfter(now)) target = target.plusDays(1);
		deadlineEpochMillis = target.toInstant().toEpochMilli();
	}

	public void afterCurrentTrack(IMusicPlayer player) {
		cancel();
		if (player != null && player.getTrackManager().getCurrentTrack() != null) player.getTrackManager().setStopAfterCurrent(true);
	}

	public void afterCurrentQueue(IMusicPlayer player) {
		cancel();
		if (player != null && player.getTrackManager().getCurrentTrack() != null) player.getTrackManager().setStopAfterQueue(true);
	}

	public void cancel() {
		deadlineEpochMillis = 0L;
		final IMusicPlayer player = MusicPlayerManager.getPlayer();
		if (player != null) {
			player.getTrackManager().setStopAfterCurrent(false);
			player.getTrackManager().setStopAfterQueue(false);
		}
	}

	public void tick(IMusicPlayer player) {
		final long deadline = deadlineEpochMillis;
		if (deadline > 0L && System.currentTimeMillis() >= deadline) {
			deadlineEpochMillis = 0L;
			if (player != null) player.getTrackManager().stop();
		}
	}

	public String status(IMusicPlayer player) {
		final long deadline = deadlineEpochMillis;
		if (deadline > 0L) {
			final long remaining = Math.max(0L, deadline - System.currentTimeMillis());
			final Duration duration = Duration.ofMillis(remaining);
			return getTranslation("gui.sleep.stops_in", duration.toHours(), String.format("%02d", duration.toMinutesPart()));
		}
		if (player != null) {
			final ITrackManager manager = player.getTrackManager();
			if (manager.isStopAfterCurrent()) return getTranslation("gui.sleep.stops_track");
			if (manager.isStopAfterQueue()) return getTranslation("gui.sleep.stops_playlist");
		}
		return getTranslation("gui.sleep.inactive");
	}
}
