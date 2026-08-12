package info.u_team.music_player.gui.playlist;

import static info.u_team.music_player.init.MusicPlayerLocalization.getTranslation;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.lwjgl.util.tinyfd.TinyFileDialogs;

import info.u_team.music_player.artwork.ArtworkRepository;
import net.minecraft.client.Minecraft;

public final class ArtworkOverrideDialogs {
	private static final ExecutorService EXECUTOR=Executors.newSingleThreadExecutor(r->{final Thread t=new Thread(r,"Music Player artwork dialog");t.setDaemon(true);return t;});
	private ArtworkOverrideDialogs(){}
	public static void choose(String uri,Consumer<String> status){EXECUTOR.execute(()->{final String selected=TinyFileDialogs.tinyfd_openFileDialog(getTranslation("gui.artwork.choose_title"),"",null,"PNG or JPEG",false);if(selected==null||selected.isBlank())return;try{ArtworkRepository.setOverride(uri,Path.of(selected));publish(status,getTranslation("gui.artwork.saved"));}catch(final IOException exception){publish(status,getTranslation("gui.artwork.failed",exception.getMessage()));}});}
	public static void reset(String uri,Consumer<String> status){EXECUTOR.execute(()->{try{ArtworkRepository.clearOverride(uri);publish(status,getTranslation("gui.artwork.restored"));}catch(final IOException exception){publish(status,getTranslation("gui.artwork.reset_failed",exception.getMessage()));}});}
	private static void publish(Consumer<String> status,String value){Minecraft.getInstance().execute(()->status.accept(value));}
	public static void shutdown(){EXECUTOR.shutdownNow();}
}
