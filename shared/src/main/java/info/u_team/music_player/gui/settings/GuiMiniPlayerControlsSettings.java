package info.u_team.music_player.gui.settings;

import static info.u_team.music_player.init.MusicPlayerLocalization.getTranslation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import info.u_team.music_player.gui.BetterScreen;
import info.u_team.music_player.gui.widget.UButton;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.musicplayer.settings.MiniPlayerControl;
import info.u_team.music_player.musicplayer.settings.Settings;
import info.u_team.music_player.util.MinecraftGuiCompat;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Mod Menu-only editor for HUD control visibility and order. */
public final class GuiMiniPlayerControlsSettings extends BetterScreen {
	private final Screen previous;
	public GuiMiniPlayerControlsSettings(Screen previous) { super(Component.literal(getTranslation("gui.hud.title"))); this.previous = previous; }
	@Override protected void init() {
		final Settings settings = MusicPlayerManager.getSettingsManager().getSettings();
		addRenderableWidget(new UButton(8,8,28,20,Component.literal("<"),button->MinecraftGuiCompat.setScreen(minecraft,previous)));
		final UButton title=addRenderableWidget(new UButton(40,8,width-48,20,Component.literal(getTranslation("gui.hud.hint"))));title.active=false;
		final List<MiniPlayerControl> enabled=settings.getMiniPlayerControls();
		final ArrayList<MiniPlayerControl> rows=new ArrayList<>(List.of(MiniPlayerControl.values()));
		rows.sort(Comparator.comparingInt(control->{final int index=enabled.indexOf(control);return index<0?100+control.ordinal():index;}));
		for(int row=0;row<rows.size();row++){
			final MiniPlayerControl control=rows.get(row);final boolean visible=enabled.contains(control);final int y=40+row*26;
			final UButton label=addRenderableWidget(new UButton(12,y,Math.max(80,width-210),20,Component.literal((visible?(enabled.indexOf(control)+1)+". ":"— ")+control.getDisplayName())));label.active=false;
			final UButton up=addRenderableWidget(new UButton(width-194,y,42,20,Component.literal(getTranslation("gui.hud.up")),button->{settings.moveMiniPlayerControl(control,-1);reopen();}));up.active=visible&&enabled.indexOf(control)>0;
			final UButton down=addRenderableWidget(new UButton(width-148,y,48,20,Component.literal(getTranslation("gui.hud.down")),button->{settings.moveMiniPlayerControl(control,1);reopen();}));down.active=visible&&enabled.indexOf(control)<enabled.size()-1;
			addRenderableWidget(new UButton(width-96,y,84,20,Component.literal(getTranslation(visible?"gui.hud.disable":"gui.hud.enable")),button->{settings.setMiniPlayerControlVisible(control,!visible);reopen();}));
		}
		addRenderableWidget(new UButton(12,178,width-24,20,Component.literal(getTranslation("gui.hud.reset")),button->{settings.resetMiniPlayerControls();reopen();}));
	}
	private void reopen(){MinecraftGuiCompat.setScreen(minecraft,new GuiMiniPlayerControlsSettings(previous));}
}
