package info.u_team.music_player.gui.settings;

import static info.u_team.music_player.init.MusicPlayerLocalization.getTranslation;

import info.u_team.music_player.gui.BetterScreen;
import info.u_team.music_player.gui.playlist.PlaylistFileDialogs;
import info.u_team.music_player.gui.util.GuiTextCompat;
import info.u_team.music_player.gui.widget.UButton;
import info.u_team.music_player.musicplayer.EqualizerPresetManager;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.musicplayer.settings.EqualizerMode;
import info.u_team.music_player.musicplayer.settings.Settings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class GuiEqualizerSettings extends BetterScreen {
	private final Screen previous; private EqualizerEditorWidget editor; private String status = "";
	public GuiEqualizerSettings(Screen previous) { super(text("gui.equalizer.title")); this.previous = previous; }
	@Override protected void init() {
		final Settings settings = MusicPlayerManager.getSettingsManager().getSettings();
		addRenderableWidget(new UButton(8, 8, 24, 20, Component.literal("<"), button -> minecraft.setScreen(previous)));
		final UButton mode = addRenderableWidget(new UButton(40, 8, 110, 20, modeLabel(settings)));
		mode.setPressable(() -> { settings.setEqualizerMode(EqualizerMode.next(settings.getEqualizerMode())); if (settings.getEqualizerMode() == EqualizerMode.OFF) settings.setEqualizerMode(EqualizerMode.PARAMETRIC); mode.setMessage(modeLabel(settings)); });
		addRenderableWidget(new UButton(158, 8, 70, 20, text("gui.equalizer.reset"), button -> settings.resetEqualizer()));
		final UButton bass = addRenderableWidget(new UButton(236, 8, 100, 20, bassLabel(settings)));
		bass.setPressable(() -> { settings.setBassBoost(!settings.isBassBoost()); bass.setMessage(bassLabel(settings)); });
		addPresetButtons(settings);
		editor = new EqualizerEditorWidget(12, 66, width - 24, Math.max(60, height - 78)); addWidget(editor);
	}
	private void addPresetButtons(Settings settings) {
		final EqualizerPresetManager presets = MusicPlayerManager.getEqualizerPresetManager(); final int w = Math.max(45, (width - 16) / 5);
		final UButton preset = addRenderableWidget(new UButton(8,36,w-3,20,Component.literal(presets.getSelected()))); preset.setPressable(() -> preset.setMessage(Component.literal(presets.selectNext(settings))));
		addRenderableWidget(new UButton(8+w,36,w-3,20,text("gui.equalizer.save"),button->preset.setMessage(Component.literal(presets.saveCurrent(settings)))));
		addRenderableWidget(new UButton(8+w*2,36,w-3,20,text("gui.equalizer.delete"),button->{if(presets.deleteSelected()){settings.resetEqualizer();preset.setMessage(Component.literal(presets.getSelected()));}}));
		addRenderableWidget(new UButton(8+w*3,36,w-3,20,text("gui.equalizer.import"),button->PlaylistFileDialogs.chooseImport(path->{try{status=getTranslation("gui.equalizer.imported",presets.importFile(path));}catch(java.io.IOException exception){status=getTranslation("gui.equalizer.import_failed");}})));
		addRenderableWidget(new UButton(8+w*4,36,w-3,20,text("gui.equalizer.export"),button->PlaylistFileDialogs.chooseExport("music-player-eq-presets.json",path->{try{presets.exportFile(path);status=getTranslation("gui.equalizer.exported");}catch(java.io.IOException exception){status=getTranslation("gui.equalizer.export_failed");}})));
	}
	@Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float partialTick){super.render(graphics,mouseX,mouseY,partialTick);editor.render(graphics,mouseX,mouseY,partialTick);GuiTextCompat.draw(graphics,font,status,12,58,0xB8E986,false);}
	private static Component modeLabel(Settings settings){return Component.literal(getTranslation("gui.equalizer.mode")+": "+getTranslation("gui.equalizer.mode."+settings.getEqualizerMode().name().toLowerCase(java.util.Locale.ROOT)));}
	private static Component bassLabel(Settings settings){return Component.literal(getTranslation("gui.equalizer.bass")+": "+getTranslation(settings.isBassBoost()?"gui.settings.state.on":"gui.settings.state.off"));}
	private static Component text(String key){return Component.literal(getTranslation(key));}
}
