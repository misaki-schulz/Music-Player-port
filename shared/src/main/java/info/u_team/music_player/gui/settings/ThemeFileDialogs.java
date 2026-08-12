package info.u_team.music_player.gui.settings;

import static info.u_team.music_player.init.MusicPlayerLocalization.getTranslation;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.lwjgl.util.tinyfd.TinyFileDialogs;

import info.u_team.music_player.musicplayer.MusicPlayerManager;
import net.minecraft.client.Minecraft;

public final class ThemeFileDialogs {
	private static final ExecutorService EXECUTOR=Executors.newSingleThreadExecutor(r->{final Thread t=new Thread(r,"Music Player theme dialog");t.setDaemon(true);return t;});private ThemeFileDialogs(){}
	public static void importTheme(Consumer<String> status){EXECUTOR.execute(()->{final String selected=TinyFileDialogs.tinyfd_openFileDialog(getTranslation("gui.theme.import"),"",null,"JSON",false);if(selected==null||selected.isBlank())return;try{MusicPlayerManager.getThemeManager().importTheme(Path.of(selected));publish(status,getTranslation("gui.theme.imported"));}catch(final IOException exception){publish(status,getTranslation("gui.theme.import_failed",exception.getMessage()));}});}
	public static void exportTheme(Consumer<String> status){EXECUTOR.execute(()->{String selected=TinyFileDialogs.tinyfd_saveFileDialog(getTranslation("gui.theme.export"),"music-player-theme.json",null,"JSON");if(selected==null||selected.isBlank())return;if(!selected.toLowerCase(java.util.Locale.ROOT).endsWith(".json"))selected+=".json";try{MusicPlayerManager.getThemeManager().exportTheme(Path.of(selected));publish(status,getTranslation("gui.theme.exported"));}catch(final IOException exception){publish(status,getTranslation("gui.theme.export_failed",exception.getMessage()));}});}
	private static void publish(Consumer<String> status,String value){Minecraft.getInstance().execute(()->status.accept(value));}
	public static void shutdown(){EXECUTOR.shutdownNow();}
}
