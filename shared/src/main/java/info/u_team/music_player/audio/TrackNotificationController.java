package info.u_team.music_player.audio;

import info.u_team.music_player.lavaplayer.api.IMusicPlayer;
import info.u_team.music_player.lavaplayer.api.audio.IPlayingTrack;
import info.u_team.music_player.musicplayer.settings.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import info.u_team.music_player.util.MinecraftGuiCompat;

public final class TrackNotificationController{
	private long playbackId=-1L;
	public void tick(IMusicPlayer player,Settings settings){final IPlayingTrack track=player==null?null:player.getTrackManager().getCurrentTrack();if(track==null)return;if(track.getPlaybackId()==playbackId)return;playbackId=track.getPlaybackId();if(!settings.isTrackNotifications())return;MinecraftGuiCompat.showActionBar(Minecraft.getInstance(),Component.literal("♫ "+track.getInfo().getFixedTitle()+" — "+track.getInfo().getFixedAuthor()));}
}
