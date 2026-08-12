package info.u_team.music_player.gui;
import static info.u_team.music_player.init.MusicPlayerLocalization.getTranslation;

import info.u_team.music_player.gui.util.LegacyGuiTransform;
import info.u_team.music_player.gui.widget.ImageButton;
import info.u_team.music_player.init.MusicPlayerResources;
import info.u_team.music_player.lavaplayer.api.audio.IPlayingTrack;
import info.u_team.music_player.lavaplayer.api.queue.ITrackManager;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.musicplayer.MusicPlayerUtils;
import info.u_team.music_player.musicplayer.settings.Settings;
import info.u_team.music_player.musicplayer.settings.MiniPlayerControl;
import info.u_team.music_player.render.OverlayPlacement;
import info.u_team.music_player.render.RenderOverlayMusicDisplay;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class GuiMiniPlayerInteraction extends BetterScreen {
	private RenderOverlayMusicDisplay overlay; private int overlayX,overlayY; private float overlayScale; private ImageButton pause;
	public GuiMiniPlayerInteraction(){super(Component.literal(getTranslation("gui.mini.controls")));}
	@Override protected void init(){overlay=new RenderOverlayMusicDisplay();final Settings s=MusicPlayerManager.getSettingsManager().getSettings();overlayScale=OverlayPlacement.scale(s,width,height,overlay.getWidth(),overlay.getHeight());final int rw=Math.round(overlay.getWidth()*overlayScale),rh=Math.round(overlay.getHeight()*overlayScale);overlayX=OverlayPlacement.x(s,width,rw);overlayY=OverlayPlacement.y(s,height,rh);final int cy=overlayY+Math.round(overlay.getControlsY()*overlayScale);final var controls=s.getMiniPlayerControls();for(int i=0;i<controls.size();i++)addControl(i,controls.size(),cy,controls.get(i));}
	private void addControl(int i,int count,int y,MiniPlayerControl control){final int slot=Math.max(20,116/count),localX=2+i*slot,localRight=i==count-1?118:Math.min(118,localX+slot-2),x=overlayX+Math.round(localX*overlayScale);final Runnable action=switch(control){case PREVIOUS->MusicPlayerUtils::skipBack;case PLAY_PAUSE->this::togglePause;case NEXT->MusicPlayerUtils::skipForward;case QUEUE->()->minecraft.setScreen(new GuiMusicPlayer());case FAVORITE->this::toggleFavorite;};final ImageButton b=addRenderableWidget(new ImageButton(x,y,Math.max(12,Math.round((localRight-localX)*overlayScale)),Math.max(10,Math.round(13*overlayScale)),icon(control)));b.setPressable(action);if(control==MiniPlayerControl.PLAY_PAUSE)pause=b;}
	private net.minecraft.resources.ResourceLocation icon(MiniPlayerControl control){return switch(control){case PREVIOUS->MusicPlayerResources.TEXTURE_SKIP_BACK;case PLAY_PAUSE->MusicPlayerManager.getPlayer().getTrackManager().isPaused()?MusicPlayerResources.TEXTURE_PLAY:MusicPlayerResources.TEXTURE_PAUSE;case NEXT->MusicPlayerResources.TEXTURE_SKIP_FORWARD;case QUEUE->MusicPlayerResources.TEXTURE_OPEN;case FAVORITE->MusicPlayerResources.TEXTURE_ADD;};}
	private void togglePause(){final ITrackManager m=MusicPlayerManager.getPlayer().getTrackManager();if(m.getCurrentTrack()!=null)m.setPaused(!m.isPaused());if(pause!=null)pause.setImage(icon(MiniPlayerControl.PLAY_PAUSE));}
	private void toggleFavorite(){final IPlayingTrack t=MusicPlayerManager.getPlayer().getTrackManager().getCurrentTrack();if(t!=null)MusicPlayerManager.getLibraryStateManager().toggleFavorite(t.getInfo());}
	@Override public void tick(){if(pause!=null)pause.setImage(icon(MiniPlayerControl.PLAY_PAUSE));}
	@Override public void render(GuiGraphics g,int mx,int my,float pt){if(MusicPlayerManager.getPlayer().getTrackManager().getCurrentTrack()!=null)LegacyGuiTransform.transformed(g,overlayX,overlayY,overlayScale,()->overlay.render(g,0,0,pt));super.render(g,mx,my,pt);}
	@Override public void renderBackground(GuiGraphics g,int mx,int my,float pt){}
	@Override public boolean isPauseScreen(){return false;}
}
