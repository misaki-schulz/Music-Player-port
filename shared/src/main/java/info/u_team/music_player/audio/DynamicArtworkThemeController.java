package info.u_team.music_player.audio;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import javax.imageio.ImageIO;

import info.u_team.music_player.artwork.ArtworkRepository;
import info.u_team.music_player.init.MusicPlayerColors;
import info.u_team.music_player.lavaplayer.api.IMusicPlayer;
import info.u_team.music_player.lavaplayer.api.audio.IPlayingTrack;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.musicplayer.settings.Settings;
import net.minecraft.client.Minecraft;

/** Derives a bounded high-contrast accent from artwork once per concrete playback instance. */
public final class DynamicArtworkThemeController{
	private long playbackId=-1L;private boolean dynamicApplied;
	public void tick(IMusicPlayer player,Settings settings){if(!settings.isDynamicTheme()){if(dynamicApplied){dynamicApplied=false;MusicPlayerManager.getThemeManager().applyCurrentColors();}return;}final IPlayingTrack track=player==null?null:player.getTrackManager().getCurrentTrack();if(track==null||track.getPlaybackId()==playbackId)return;playbackId=track.getPlaybackId();ArtworkRepository.request(track.getInfo()).thenAccept(bytes->{try{final BufferedImage image=ImageIO.read(new ByteArrayInputStream(bytes));if(image==null)return;long red=0,green=0,blue=0,count=0;final int stepX=Math.max(1,image.getWidth()/32),stepY=Math.max(1,image.getHeight()/32);for(int y=0;y<image.getHeight();y+=stepY)for(int x=0;x<image.getWidth();x+=stepX){final int rgb=image.getRGB(x,y);final int r=rgb>>>16&255,g=rgb>>>8&255,b=rgb&255;final int max=Math.max(r,Math.max(g,b)),min=Math.min(r,Math.min(g,b));if(max-min<20)continue;red+=r;green+=g;blue+=b;count++;}if(count==0)return;final int r=boost((int)(red/count)),g=boost((int)(green/count)),b=boost((int)(blue/count));Minecraft.getInstance().execute(()->{MusicPlayerColors.apply(0x555555FF,r<<24|g<<16|b<<8|0xFF,0xFFFFFFFF,Math.min(255,r+40)<<24|Math.min(255,g+40)<<16|Math.min(255,b+40)<<8|0xFF);dynamicApplied=true;});}catch(final Exception ignored){}}).exceptionally(error->null);}
	private static int boost(int value){return Math.clamp((int)(value*1.25F)+20,48,240);}
}
