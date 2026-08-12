package info.u_team.music_player.gui.history;

import static info.u_team.music_player.init.MusicPlayerLocalization.getTranslation;

import java.util.List;

import info.u_team.music_player.gui.BetterScreen;
import info.u_team.music_player.gui.widget.UButton;
import info.u_team.music_player.musicplayer.LibraryStateManager.HistoryEntry;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.musicplayer.PlaybackActions;
import info.u_team.music_player.util.MinecraftGuiCompat;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class GuiMusicHistory extends BetterScreen {
	private static final int PAGE_SIZE = 8;
	private final Screen previous;
	private final int page;
	private String status = "";
	private UButton statusLabel;
	public GuiMusicHistory(Screen previous) { this(previous, 0); }
	private GuiMusicHistory(Screen previous, int page) { super(Component.literal(getTranslation("gui.history.title"))); this.previous=previous; this.page=Math.max(0,page); }
	@Override protected void init() {
		addRenderableWidget(new UButton(8,8,28,20,Component.literal("<"),button->MinecraftGuiCompat.setScreen(minecraft,previous)));
		addRenderableWidget(new UButton(40,8,80,20,Component.literal(getTranslation("gui.history.library")),button->MinecraftGuiCompat.setScreen(minecraft,new GuiMusicLibrary(this))));
		addRenderableWidget(new UButton(124,8,80,20,Component.literal(getTranslation("gui.history.statistics")),button->MinecraftGuiCompat.setScreen(minecraft,new GuiMusicStatistics(this))));
		final List<HistoryEntry> history=MusicPlayerManager.getLibraryStateManager().getHistory();
		final int start=Math.min(history.size(),page*PAGE_SIZE),end=Math.min(history.size(),start+PAGE_SIZE);
		for(int index=start;index<end;index++){
			final HistoryEntry entry=history.get(index);final int y=38+(index-start)*24;final int actionWidth=28;final int labelWidth=Math.max(40,width-24-actionWidth*2);
			addRenderableWidget(new UButton(8,y,labelWidth,20,Component.literal(label(entry)),button->PlaybackActions.playNow(entry.uri,this::setStatus)));
			addRenderableWidget(new UButton(12+labelWidth,y,actionWidth,20,Component.literal("+1"),button->PlaybackActions.playNext(entry.uri,this::setStatus)));
			final UButton favorite=addRenderableWidget(new UButton(16+labelWidth+actionWidth,y,actionWidth,20,favoriteLabel(entry.uri)));
			favorite.setPressable(()->{MusicPlayerManager.getLibraryStateManager().toggleFavoriteUri(entry.uri);favorite.setMessage(favoriteLabel(entry.uri));});
		}
		final int maxPage=Math.max(0,(history.size()-1)/PAGE_SIZE);
		final UButton previousPage=addRenderableWidget(new UButton(8,height-28,70,20,Component.literal(getTranslation("gui.common.previous")),button->open(page-1)));previousPage.active=page>0;
		final UButton nextPage=addRenderableWidget(new UButton(width-78,height-28,70,20,Component.literal(getTranslation("gui.common.next")),button->open(page+1)));nextPage.active=page<maxPage;
		statusLabel=addRenderableWidget(new UButton(84,height-28,Math.max(20,width-168),20,Component.literal(status)));statusLabel.active=false;
	}
	private void setStatus(String value){status=value;if(statusLabel!=null)statusLabel.setMessage(Component.literal(value));}
	private void open(int value){MinecraftGuiCompat.setScreen(minecraft,new GuiMusicHistory(previous,value));}
	private Component favoriteLabel(String uri){return Component.literal(MusicPlayerManager.getLibraryStateManager().isFavoriteUri(uri)?"★":"☆");}
	private static String label(HistoryEntry entry){final String title=entry.title==null||entry.title.isBlank()?entry.uri:entry.title;return entry.author==null||entry.author.isBlank()?title:title+" — "+entry.author;}
}
