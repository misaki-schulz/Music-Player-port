package info.u_team.music_player.lavaplayer;

import java.util.List;

public final class LifecycleSmokeTest {

	private LifecycleSmokeTest() {
	}

	public static void main(String[] arguments) throws Exception {
		final MusicPlayer player = new MusicPlayer();
		player.startAudioOutput();
		Thread.sleep(300L);
		player.shutdown();
		Thread.sleep(300L);

		final List<String> leaked = Thread.getAllStackTraces().keySet().stream()
				.filter(Thread::isAlive)
				.filter(thread -> !thread.isDaemon())
				.map(Thread::getName)
				.filter(name -> name.contains("Music Player") || name.toLowerCase(java.util.Locale.ROOT).contains("lavaplayer"))
				.toList();
		if (!leaked.isEmpty()) {
			throw new IllegalStateException("Non-daemon audio threads survived shutdown: " + leaked);
		}
	}
}
