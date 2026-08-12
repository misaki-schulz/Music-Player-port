package info.u_team.music_player.gui.history;

import static info.u_team.music_player.init.MusicPlayerLocalization.getTranslation;

import info.u_team.music_player.gui.BetterScreen;
import info.u_team.music_player.gui.widget.UButton;
import info.u_team.music_player.integration.LyricsService;
import info.u_team.music_player.integration.LyricsService.Lyrics;
import info.u_team.music_player.lavaplayer.api.audio.IPlayingTrack;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.util.MinecraftGuiCompat;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class GuiLyrics extends BetterScreen {
	private static final int VISIBLE_LINES=10;private final Screen previous;private Lyrics lyrics=Lyrics.empty(getTranslation("gui.lyrics.loading"));private UButton source;private final UButton[] rows=new UButton[VISIBLE_LINES];
	public GuiLyrics(Screen previous){super(Component.literal(getTranslation("gui.lyrics.title")));this.previous=previous;}
	@Override protected void init(){addRenderableWidget(new UButton(8,8,28,20,Component.literal("<"),b->MinecraftGuiCompat.setScreen(minecraft,previous)));source=addRenderableWidget(new UButton(40,8,width-48,20,Component.literal(getTranslation("gui.lyrics.loading"))));source.active=false;for(int i=0;i<rows.length;i++){rows[i]=addRenderableWidget(new UButton(12,40+i*22,width-24,20,Component.empty()));rows[i].active=false;}final IPlayingTrack track=track();if(track==null){lyrics=Lyrics.empty(getTranslation("gui.lyrics.nothing"));refresh();}else LyricsService.request(track).thenAccept(result->minecraft.execute(()->{lyrics=result;refresh();}));}
	@Override public void tick(){refresh();}
	private void refresh(){if(source==null)return;source.setMessage(Component.literal(lyrics.lines().isEmpty()?lyrics.message():lyrics.source()+" — "+getTranslation(lyrics.synchronizedLyrics()?"gui.lyrics.synchronized":"gui.lyrics.plain")));if(lyrics.lines().isEmpty()){for(int i=0;i<rows.length;i++)rows[i].setMessage(Component.literal(i==0?lyrics.message():""));return;}final IPlayingTrack track=track();final int active=track==null?0:lyrics.activeLine(track.getPosition());final int start=Math.max(0,Math.min(Math.max(0,lyrics.lines().size()-rows.length),active-rows.length/2));for(int i=0;i<rows.length;i++){final int index=start+i;rows[i].setMessage(Component.literal(index<lyrics.lines().size()?(index==active?"> ":"  ")+lyrics.lines().get(index).text():""));}}
	private IPlayingTrack track(){return MusicPlayerManager.getPlayer().getTrackManager().getCurrentTrack();}
}
