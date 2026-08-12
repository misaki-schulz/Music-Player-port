package info.u_team.music_player.gui;

import static info.u_team.music_player.init.MusicPlayerLocalization.getTranslation;

import info.u_team.music_player.artwork.ArtworkRenderer;
import info.u_team.music_player.gui.util.GuiTrackUtils;
import info.u_team.music_player.gui.history.GuiLyrics;
import info.u_team.music_player.gui.widget.UButton;
import info.u_team.music_player.gui.widget.USlider;
import info.u_team.music_player.lavaplayer.api.IMusicPlayer;
import info.u_team.music_player.lavaplayer.api.audio.IPlayingTrack;
import info.u_team.music_player.musicplayer.ABRepeatManager;
import info.u_team.music_player.musicplayer.LibraryStateManager;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.musicplayer.MusicPlayerUtils;
import info.u_team.music_player.musicplayer.settings.Settings;
import info.u_team.music_player.musicplayer.TrackRadioService;
import info.u_team.music_player.gui.playlist.TrackCardDialogs;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Full-size playback-action view shared by the two legacy compatibility targets. */
public final class GuiNowPlaying extends BetterScreen {

	private final Screen previous;
	private final Page page;
	private int artworkX, artworkY, artworkSize;
	private UButton title, metadata, position, pause, favorite, loopStatus, actionStatus;
	private final UButton[] ratings = new UButton[5];

	public GuiNowPlaying(Screen previous) { this(previous,Page.PLAYBACK); }
	private GuiNowPlaying(Screen previous,Page page) { super(Component.literal(getTranslation("gui.now.title"))); this.previous=previous;this.page=page; }

	@Override
	protected void init() {
		addRenderableWidget(new UButton(8, 8, 28, 20, Component.literal("<"), button -> minecraft.setScreen(previous)));
		title = disabled(40, 8, width - 48, 20, trackTitle());
		artworkX = 16; artworkY = 34; artworkSize = Math.max(72, Math.min(180, Math.min(width / 2 - 28, height - 48)));
		final int rightX = artworkX + artworkSize + 12, rightWidth = Math.max(80, width - rightX - 12);
		metadata = disabled(rightX,34,rightWidth,18,trackMetadata());position=disabled(rightX,56,rightWidth,18,positionLabel());
		final int actionWidth = Math.max(26, (rightWidth - 16) / 5);
		addAction(rightX,78,actionWidth,"<",MusicPlayerUtils::skipBack);pause=addAction(rightX+actionWidth+4,78,actionWidth,pauseLabel(),this::togglePause);addAction(rightX+(actionWidth+4)*2,78,actionWidth,">",MusicPlayerUtils::skipForward);favorite=addAction(rightX+(actionWidth+4)*3,78,actionWidth,favoriteLabel(),this::toggleFavorite);addAction(rightX+(actionWidth+4)*4,78,actionWidth,"URI",()->{final IPlayingTrack t=track();if(t!=null)GuiTrackUtils.openURI(t.getInfo().getURI());});
		final int half=(rightWidth-4)/2;final UButton playbackTab=addAction(rightX,100,half,getTranslation("gui.now.playback"),()->openPage(Page.PLAYBACK));final UButton toolsTab=addAction(rightX+half+4,100,half,getTranslation("gui.now.tools"),()->openPage(Page.TOOLS));playbackTab.active=page!=Page.PLAYBACK;toolsTab.active=page!=Page.TOOLS;if(page==Page.PLAYBACK)initPlaybackPage(rightX,rightWidth);else initToolsPage(rightX,rightWidth);
		refreshRating();
	}
	private void initPlaybackPage(int rightX,int rightWidth){final int third=Math.max(42,(rightWidth-8)/3);addAction(rightX,122,third,getTranslation("gui.now.set_a"),this::setPointA);addAction(rightX+third+4,122,third,getTranslation("gui.now.set_b"),this::setPointB);addAction(rightX+(third+4)*2,122,third,getTranslation("gui.now.clear_ab"),()->{MusicPlayerManager.getABRepeatManager().clear();refreshLoop();});loopStatus=disabled(rightX,144,rightWidth,18,loopLabel());final int ratingWidth=Math.max(24,(rightWidth-16)/5);for(int i=0;i<ratings.length;i++){final int rating=i+1;ratings[i]=addAction(rightX+i*(ratingWidth+4),166,ratingWidth,""+rating,()->setRating(rating));}final IMusicPlayer player=MusicPlayerManager.getPlayer();final USlider speed=addRenderableWidget(new USlider(rightX,188,rightWidth,20,Component.literal(getTranslation("gui.now.track_speed")+": "),Component.literal("x"),Settings.MIN_SPEED,Settings.MAX_SPEED,player.getSpeed(),true,true,s->saveTrackPlayback((float)s.getValue(),player.getPitch())));speed.setPrecision(2);final USlider pitch=addRenderableWidget(new USlider(rightX,210,rightWidth,20,Component.literal(getTranslation("gui.now.track_pitch")+": "),Component.literal("x"),Settings.MIN_PITCH,Settings.MAX_PITCH,player.getPitch(),true,true,s->saveTrackPlayback(player.getSpeed(),(float)s.getValue())));pitch.setPrecision(2);}
	private void initToolsPage(int rightX,int rightWidth){addAction(rightX,122,rightWidth,getTranslation("gui.now.use_global"),this::clearTrackPlayback);addAction(rightX,144,rightWidth,getTranslation("gui.now.open_lyrics"),()->minecraft.setScreen(new GuiLyrics(this)));addAction(rightX,166,rightWidth,getTranslation("gui.now.track_radio"),()->TrackRadioService.queueRecommendations(10,this::setActionStatus));final int half=(rightWidth-4)/2;addAction(rightX,188,half,getTranslation("gui.now.export_card"),()->TrackCardDialogs.exportCard(track(),this::setActionStatus));addAction(rightX+half+4,188,half,getTranslation("gui.now.export_qr"),()->TrackCardDialogs.exportQr(track(),this::setActionStatus));actionStatus=disabled(rightX,210,rightWidth,18,"");}
	private void openPage(Page target){if(target!=page)minecraft.setScreen(new GuiNowPlaying(previous,target));}

	private UButton disabled(int x,int y,int w,int h,String label){final UButton b=addRenderableWidget(new UButton(x,y,w,h,Component.literal(label)));b.active=false;return b;}
	private UButton addAction(int x,int y,int w,String label,Runnable action){final UButton b=addRenderableWidget(new UButton(x,y,w,20,Component.literal(label)));b.setPressable(action);return b;}

	@Override public void tick(){if(track()==null)return;title.setMessage(Component.literal(trackTitle()));metadata.setMessage(Component.literal(trackMetadata()));position.setMessage(Component.literal(positionLabel()));pause.setMessage(Component.literal(pauseLabel()));favorite.setMessage(Component.literal(favoriteLabel()));refreshLoop();refreshRating();}
	@Override public void render(GuiGraphics g,int mx,int my,float pt){final IPlayingTrack t=track();if(t!=null){ArtworkRenderer.render(g,t,artworkX,artworkY,artworkSize);final float[] spectrum=MusicPlayerManager.getAudioVisualizer().spectrum();for(int i=0;i<spectrum.length;i++){final int left=artworkX+i*artworkSize/spectrum.length,bar=Math.max(1,Math.round(spectrum[i]*28F));g.fill(left,artworkY+artworkSize-bar,left+Math.max(1,artworkSize/spectrum.length-1),artworkY+artworkSize,0xB075E0B5);}}super.render(g,mx,my,pt);}
	@Override public void renderBackground(GuiGraphics g,int mx,int my,float pt){super.renderBackground(g,mx,my,pt);if(MusicPlayerManager.getSettingsManager().getSettings().isDynamicTheme()){float energy=0F;for(final float value:MusicPlayerManager.getAudioVisualizer().spectrum())energy+=value;final int alpha=Math.clamp(35+Math.round(energy*3F),35,105);g.fill(0,0,width,height,alpha<<24|0x00305042);}}
	private void togglePause(){final var m=MusicPlayerManager.getPlayer().getTrackManager();if(m.getCurrentTrack()!=null)m.setPaused(!m.isPaused());}
	private void toggleFavorite(){final IPlayingTrack t=track();if(t!=null)MusicPlayerManager.getLibraryStateManager().toggleFavorite(t.getInfo());}
	private void setPointA(){MusicPlayerManager.getABRepeatManager().setA(MusicPlayerManager.getPlayer());refreshLoop();}
	private void setPointB(){MusicPlayerManager.getABRepeatManager().setB(MusicPlayerManager.getPlayer());refreshLoop();}
	private void refreshLoop(){if(loopStatus!=null)loopStatus.setMessage(Component.literal(loopLabel()));}
	private void setRating(int rating){final IPlayingTrack t=track();if(t==null)return;final LibraryStateManager l=MusicPlayerManager.getLibraryStateManager();l.setRating(t.getInfo(),l.getRating(t.getInfo())==rating?0:rating);refreshRating();}
	private void refreshRating(){final IPlayingTrack t=track();final int selected=t==null?0:MusicPlayerManager.getLibraryStateManager().getRating(t.getInfo());for(int i=0;i<ratings.length;i++)if(ratings[i]!=null)ratings[i].setMessage(Component.literal((i+1)+(selected==i+1?"*":"")));}
	private void saveTrackPlayback(float speed,float pitch){final IPlayingTrack t=track();if(t==null)return;final IMusicPlayer p=MusicPlayerManager.getPlayer();p.setSpeed(speed);p.setPitch(pitch);MusicPlayerManager.getLibraryStateManager().setTrackPlaybackPreferences(t.getInfo(),speed,pitch);}
	private void clearTrackPlayback(){final IPlayingTrack t=track();if(t!=null)MusicPlayerManager.getLibraryStateManager().clearTrackPlaybackPreferences(t.getInfo(),MusicPlayerManager.getPlayer(),MusicPlayerManager.getSettingsManager().getSettings());}
	private void setActionStatus(String value){if(actionStatus!=null)actionStatus.setMessage(Component.literal(value));}
	private IPlayingTrack track(){return MusicPlayerManager.getPlayer().getTrackManager().getCurrentTrack();}
	private String trackTitle(){final IPlayingTrack t=track();return t==null?getTranslation("gui.now.nothing"):t.getInfo().getFixedTitle();}
	private String trackMetadata(){final IPlayingTrack t=track();return t==null?"":t.getInfo().getFixedAuthor()+"  |  "+sourceLabel(t.getInfo().getURI());}
	private String positionLabel(){final IPlayingTrack t=track();return t==null?"":GuiTrackUtils.getFormattedPosition(t)+" / "+GuiTrackUtils.getFormattedDuration(t);}
	private String pauseLabel(){return getTranslation(MusicPlayerManager.getPlayer().getTrackManager().isPaused()?"gui.now.play":"gui.now.pause");}
	private String favoriteLabel(){final IPlayingTrack t=track();return getTranslation(t!=null&&MusicPlayerManager.getLibraryStateManager().isFavorite(t.getInfo())?"gui.now.favorite_marked":"gui.now.favorite");}
	private String loopLabel(){final ABRepeatManager l=MusicPlayerManager.getABRepeatManager();return "A: "+format(l.getPointA())+"   B: "+format(l.getPointB())+(l.isActive()?"   "+getTranslation("gui.now.looping"):"");}
	private static String format(long millis){return millis<0?"--:--":info.u_team.music_player.util.TimeUtil.timeConversion(millis/1000L);}
	private static String sourceLabel(String uri){if(uri==null||uri.isBlank())return getTranslation("gui.now.unknown_source");try{final String host=java.net.URI.create(uri).getHost();return host==null?getTranslation("gui.now.local_source"):host;}catch(final IllegalArgumentException ignored){return getTranslation("gui.now.local_source");}}
	private enum Page{PLAYBACK,TOOLS}
}
