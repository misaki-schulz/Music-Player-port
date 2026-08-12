package info.u_team.music_player.gui.settings;

import static info.u_team.music_player.init.MusicPlayerLocalization.getTranslation;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import info.u_team.music_player.gui.BetterScreen;
import info.u_team.music_player.gui.widget.UButton;
import info.u_team.music_player.gui.widget.USlider;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.musicplayer.settings.Settings;
import info.u_team.music_player.musicplayer.settings.ShuffleMode;
import info.u_team.music_player.util.MinecraftGuiCompat;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class GuiAudioRoutingSettings extends BetterScreen {
	private final Screen previous;
	public GuiAudioRoutingSettings(Screen previous){super(Component.literal(getTranslation("gui.audio.title")));this.previous=previous;}
	@Override protected void init(){final Settings settings=MusicPlayerManager.getSettingsManager().getSettings();addRenderableWidget(new UButton(8,8,28,20,Component.literal("<"),b->MinecraftGuiCompat.setScreen(minecraft,previous)));toggle(12,46,(width-28)/2,"gui.audio.mono",settings::isMonoOutput,settings::setMonoOutput);toggle(width/2+2,46,(width-28)/2,"gui.audio.swap",settings::isSwapChannels,settings::setSwapChannels);final USlider balance=addRenderableWidget(new USlider(12,74,width-24,20,Component.literal(getTranslation("gui.audio.balance")+": "),Component.empty(),-1F,1F,settings.getChannelBalance(),true,true,s->settings.setChannelBalance((float)s.getValue())));balance.setPrecision(2);final UButton shuffle=addRenderableWidget(new UButton(12,102,width-24,20,shuffleLabel(settings)));shuffle.setPressable(()->{settings.setShuffleMode(ShuffleMode.next(settings.getShuffleMode()));shuffle.setMessage(shuffleLabel(settings));});}
	private void toggle(int x,int y,int width,String key,BooleanSupplier getter,Consumer<Boolean> setter){final UButton button=addRenderableWidget(new UButton(x,y,width,20,toggleLabel(key,getter.getAsBoolean())));button.setPressable(()->{setter.accept(!getter.getAsBoolean());button.setMessage(toggleLabel(key,getter.getAsBoolean()));});}
	private static Component toggleLabel(String key,boolean enabled){return Component.literal(getTranslation(key)+": "+getTranslation(enabled?"gui.settings.state.on":"gui.settings.state.off"));}
	private static Component shuffleLabel(Settings settings){return Component.literal(getTranslation("gui.audio.shuffle",settings.getShuffleMode().getDisplayName()));}
}
