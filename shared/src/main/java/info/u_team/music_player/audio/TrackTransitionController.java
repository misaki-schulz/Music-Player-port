package info.u_team.music_player.audio;

import info.u_team.music_player.lavaplayer.api.IMusicPlayer;
import info.u_team.music_player.lavaplayer.api.audio.IPlayingTrack;
import info.u_team.music_player.musicplayer.settings.Settings;

/**
 * Safe single-decoder transition fade. It avoids pretending to overlap two decoders: unsupported streams remain at
 * unity gain, while finite tracks fade to silence and the following track fades in.
 */
public final class TrackTransitionController {
	private long playbackId=-1L;private long fadeInStartNanos;private boolean expectFadeIn;
	public void tick(IMusicPlayer player,Settings settings){if(player==null)return;final IPlayingTrack track=player.getTrackManager().getCurrentTrack();final long durationMillis=Math.round(settings.getCrossfadeSeconds()*1000F);if(track==null||durationMillis<=0L||track.getInfo().isStream()||track.getDuration()<=durationMillis){player.setTransitionGain(1F);playbackId=track==null?-1L:track.getPlaybackId();expectFadeIn=false;return;}if(track.getPlaybackId()!=playbackId){playbackId=track.getPlaybackId();if(expectFadeIn){fadeInStartNanos=System.nanoTime();expectFadeIn=false;}else fadeInStartNanos=0L;}if(fadeInStartNanos>0L){final float progress=Math.clamp((System.nanoTime()-fadeInStartNanos)/(durationMillis*1_000_000F),0F,1F);player.setTransitionGain(progress);if(progress>=1F)fadeInStartNanos=0L;return;}final long remaining=track.getDuration()-track.getPosition();if(remaining<=durationMillis){expectFadeIn=true;player.setTransitionGain(Math.clamp(remaining/(float)durationMillis,0F,1F));}else player.setTransitionGain(1F);}
}
