package info.u_team.music_player.gui.settings;

import static info.u_team.music_player.init.MusicPlayerLocalization.getTranslation;

import info.u_team.music_player.gui.BetterScreen;
import info.u_team.music_player.gui.widget.UButton;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.util.MinecraftGuiCompat;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class GuiThemeSettings extends BetterScreen{
	private final Screen previous;private UButton status;public GuiThemeSettings(Screen previous){super(Component.literal(getTranslation("gui.theme.title")));this.previous=previous;}
	@Override protected void init(){addRenderableWidget(new UButton(8,8,28,20,Component.literal("<"),b->MinecraftGuiCompat.setScreen(minecraft,previous)));final UButton name=addRenderableWidget(new UButton(40,8,width-48,20,Component.literal(getTranslation("gui.theme.active",MusicPlayerManager.getThemeManager().current().name))));name.active=false;final int w=(width-28)/2;addRenderableWidget(new UButton(12,46,w,20,Component.literal(getTranslation("gui.theme.import")),b->ThemeFileDialogs.importTheme(this::setStatus)));addRenderableWidget(new UButton(16+w,46,w,20,Component.literal(getTranslation("gui.theme.export")),b->ThemeFileDialogs.exportTheme(this::setStatus)));addRenderableWidget(new UButton(12,74,width-24,20,Component.literal(getTranslation("gui.theme.reset")),b->{MusicPlayerManager.getThemeManager().reset();setStatus(getTranslation("gui.theme.reset_done"));}));status=addRenderableWidget(new UButton(12,104,width-24,20,Component.empty()));status.active=false;final UButton note=addRenderableWidget(new UButton(12,132,width-24,20,Component.literal(getTranslation("gui.theme.note"))));note.active=false;}
	private void setStatus(String value){if(status!=null)status.setMessage(Component.literal(value));}
}
