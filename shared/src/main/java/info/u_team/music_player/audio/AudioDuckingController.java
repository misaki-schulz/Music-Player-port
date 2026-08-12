package info.u_team.music_player.audio;

import java.util.concurrent.TimeUnit;

import info.u_team.music_player.lavaplayer.api.IMusicPlayer;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.musicplayer.settings.Settings;
import net.minecraft.sounds.SoundSource;

/**
 * Smoothly lowers only this mod's output for important game sounds. Minecraft's own volume options are never changed.
 */
public final class AudioDuckingController {

	private static final long HOLD_NANOS = TimeUnit.MILLISECONDS.toNanos(1400);
	private static volatile long duckUntilNanos;
	private static float appliedGain = 1F;
	private static long lastTickNanos;

	private AudioDuckingController() {
	}

	public static void onSound(SoundSource source) {
		if (!isImportant(source)) return;
		duckUntilNanos = Math.max(duckUntilNanos, System.nanoTime() + HOLD_NANOS);
	}

	public static void tick() {
		final IMusicPlayer player = MusicPlayerManager.getPlayer();
		if (player == null) return;
		final Settings settings = MusicPlayerManager.getSettingsManager().getSettings();
		final boolean active = settings.isDuckingEnabled() && System.nanoTime() < duckUntilNanos;
		final float target = active ? 1F - settings.getDuckingPercent() / 100F : 1F;
		final long now=System.nanoTime();final float elapsedMillis=lastTickNanos==0L?50F:(now-lastTickNanos)/1_000_000F;lastTickNanos=now;
		final float duration=target<appliedGain?settings.getDuckingAttackMillis():settings.getDuckingReleaseMillis();
		final float step=Math.clamp(elapsedMillis/Math.max(1F,duration),0F,1F);
		appliedGain += (target-appliedGain)*step;
		if(Math.abs(target-appliedGain)<0.002F)appliedGain=target;
		player.setDuckingGain(appliedGain);
	}

	private static boolean isImportant(SoundSource source) {
		return source == SoundSource.HOSTILE || source == SoundSource.PLAYERS || source == SoundSource.WEATHER
				|| source == SoundSource.VOICE;
	}
}
