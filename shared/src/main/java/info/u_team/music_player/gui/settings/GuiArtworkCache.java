package info.u_team.music_player.gui.settings;

import static info.u_team.music_player.init.MusicPlayerLocalization.getTranslation;

import info.u_team.music_player.artwork.ArtworkRepository;
import info.u_team.music_player.gui.BetterScreen;
import info.u_team.music_player.gui.widget.UButton;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.util.MinecraftGuiCompat;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class GuiArtworkCache extends BetterScreen {
	private final Screen previous;private UButton status;
	public GuiArtworkCache(Screen previous){super(Component.literal(getTranslation("gui.cache.title")));this.previous=previous;}
	@Override protected void init(){addRenderableWidget(new UButton(8,8,28,20,Component.literal("<"),b->MinecraftGuiCompat.setScreen(minecraft,previous)));status=addRenderableWidget(new UButton(40,8,width-48,20,Component.literal(getTranslation("gui.cache.calculating"))));status.active=false;refresh();addRenderableWidget(new UButton(12,46,(width-28)/2,20,Component.literal(getTranslation("gui.cache.remove_expired")),b->ArtworkRepository.cleanupExpired().thenAccept(value->minecraft.execute(()->status.setMessage(Component.literal(value.display()))))));addRenderableWidget(new UButton(width/2+2,46,(width-28)/2,20,Component.literal(getTranslation("gui.cache.clear")),b->ArtworkRepository.clearCache().thenAccept(value->minecraft.execute(()->status.setMessage(Component.literal(value.display()))))));final UButton policy=addRenderableWidget(new UButton(12,76,width-24,20,Component.literal(getTranslation("gui.cache.policy",MusicPlayerManager.getSettingsManager().getSettings().getArtworkCacheMegabytes()))));policy.active=false;}
	private void refresh(){ArtworkRepository.inspectCache().thenAccept(value->minecraft.execute(()->{if(status!=null)status.setMessage(Component.literal(value.display()));}));}
}
