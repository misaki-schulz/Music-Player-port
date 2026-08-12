package info.u_team.music_player.musicplayer;

import static info.u_team.music_player.init.MusicPlayerLocalization.getTranslation;

import java.util.function.Consumer;

import info.u_team.music_player.lavaplayer.api.audio.IAudioTrack;
import info.u_team.music_player.lavaplayer.api.search.ISearchResult;
import net.minecraft.client.Minecraft;

public final class PlaybackActions {
	private PlaybackActions() { }
	public static void playNow(String uri, Consumer<String> status) { resolve(uri, track -> { MusicPlayerManager.getPlayer().getTrackManager().prepare(track); MusicPlayerManager.getPlayer().getTrackManager().setPaused(false); publish(status,getTranslation("gui.playback.playing")); }, status); }
	public static void playNext(String uri, Consumer<String> status) { resolve(uri, track -> { MusicPlayerManager.getPlayer().getTrackManager().playNext(track); publish(status,getTranslation("gui.playback.queued_next")); }, status); }
	public static void queue(String uri, Consumer<String> status) { resolve(uri, track -> { MusicPlayerManager.getPlayer().getTrackManager().queue(track); publish(status,getTranslation("gui.playback.queued")); }, status); }
	private static void resolve(String uri, Consumer<IAudioTrack> action, Consumer<String> status) {
		publish(status,getTranslation("gui.playback.loading"));
		MusicPlayerManager.getPlayer().getTrackSearch().getTracks(uri, result -> {
			final IAudioTrack track = first(result);
			if (track == null) publish(status,getTranslation("gui.playback.failed")); else action.accept(track);
		});
	}
	private static IAudioTrack first(ISearchResult result) {
		if (result == null || result.hasError()) return null;
		if (!result.isList()) return result.getTrack();
		if (result.getTrackList() == null) return null;
		if (result.getTrackList().getSelectedTrack() != null) return result.getTrackList().getSelectedTrack();
		return result.getTrackList().getTracks().stream().findFirst().orElse(null);
	}
	private static void publish(Consumer<String> status,String value){Minecraft.getInstance().execute(()->status.accept(value));}
}
