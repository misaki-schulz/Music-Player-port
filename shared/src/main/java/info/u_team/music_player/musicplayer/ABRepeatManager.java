package info.u_team.music_player.musicplayer;

import info.u_team.music_player.lavaplayer.api.IMusicPlayer;
import info.u_team.music_player.lavaplayer.api.audio.IPlayingTrack;

/** Runtime-only A/B loop. Points are discarded whenever the concrete playback instance changes. */
public final class ABRepeatManager {

	private long playbackId = -1L;
	private long pointA = -1L;
	private long pointB = -1L;

	public synchronized boolean setA(IMusicPlayer player) {
		final IPlayingTrack track = current(player);
		if (track == null || track.getInfo().isStream()) return false;
		playbackId = track.getPlaybackId();
		pointA = track.getPosition();
		if (pointB <= pointA + 250L) pointB = -1L;
		return true;
	}

	public synchronized boolean setB(IMusicPlayer player) {
		final IPlayingTrack track = current(player);
		if (track == null || track.getInfo().isStream() || track.getPlaybackId() != playbackId || pointA < 0L
				|| track.getPosition() <= pointA + 250L) return false;
		pointB = track.getPosition();
		return true;
	}

	public synchronized void clear() {
		playbackId = -1L;
		pointA = -1L;
		pointB = -1L;
	}

	public synchronized void tick(IMusicPlayer player) {
		final IPlayingTrack track = current(player);
		if (track == null || (playbackId >= 0L && track.getPlaybackId() != playbackId)) {
			clear();
			return;
		}
		if (pointA >= 0L && pointB > pointA && !player.getTrackManager().isPaused() && track.getPosition() >= pointB) {
			track.setPosition(pointA);
		}
	}

	public synchronized long getPointA() { return pointA; }
	public synchronized long getPointB() { return pointB; }
	public synchronized boolean isActive() { return pointA >= 0L && pointB > pointA; }

	private static IPlayingTrack current(IMusicPlayer player) {
		return player == null ? null : player.getTrackManager().getCurrentTrack();
	}
}
