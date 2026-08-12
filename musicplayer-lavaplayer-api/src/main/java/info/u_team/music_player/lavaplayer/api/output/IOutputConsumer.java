package info.u_team.music_player.lavaplayer.api.output;

@FunctionalInterface
public interface IOutputConsumer {
	/**
	 * Receives a borrowed PCM buffer. Consumers must finish reading it before returning and must not retain it.
	 */
	void accept(byte[] buffer, int chunkSize);
}
