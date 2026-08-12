package info.u_team.music_player.gui.playlist;

import static info.u_team.music_player.init.MusicPlayerLocalization.getTranslation;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.lwjgl.util.tinyfd.TinyFileDialogs;

import info.u_team.music_player.lavaplayer.api.audio.IPlayingTrack;
import info.u_team.music_player.musicplayer.TrackCardService;
import net.minecraft.client.Minecraft;

public final class TrackCardDialogs{
	private static final ExecutorService EXECUTOR=Executors.newSingleThreadExecutor(r->{final Thread t=new Thread(r,"Music Player track card");t.setDaemon(true);return t;});private TrackCardDialogs(){}
	public static void exportCard(IPlayingTrack track,Consumer<String> status){save(getTranslation("gui.card.export_title"),"music-player-track-card.png",path->TrackCardService.exportCard(track,path,true),status);}
	public static void exportQr(IPlayingTrack track,Consumer<String> status){save(getTranslation("gui.card.qr_title"),"music-player-track-qr.png",path->TrackCardService.exportQr(track,path),status);}
	private static void save(String title,String suggested,Writer writer,Consumer<String> status){EXECUTOR.execute(()->{String selected=TinyFileDialogs.tinyfd_saveFileDialog(title,suggested,null,"PNG");if(selected==null||selected.isBlank())return;if(!selected.toLowerCase(java.util.Locale.ROOT).endsWith(".png"))selected+=".png";try{writer.write(Path.of(selected));publish(status,getTranslation("gui.card.exported",Path.of(selected).getFileName()));}catch(final IOException exception){publish(status,getTranslation("gui.files.export_failed",exception.getMessage()));}});}
	private static void publish(Consumer<String> status,String value){Minecraft.getInstance().execute(()->status.accept(value));}
	public static void shutdown(){EXECUTOR.shutdownNow();}
	@FunctionalInterface private interface Writer{void write(Path path)throws IOException;}
}
