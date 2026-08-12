package info.u_team.music_player.musicplayer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;

import info.u_team.music_player.init.MusicPlayerColors;
import info.u_team.music_player.musicplayer.settings.Settings;
import info.u_team.music_player.util.SafeFileStorage;

/** Portable color/layout theme. Font and texture packs remain resource-pack concerns. */
public final class ThemeManager {
	private static final int MAX_THEME_BYTES=128*1024;
	private final Gson gson;private Path activePath;private ThemeProfile active=new ThemeProfile();
	ThemeManager(Gson gson){this.gson=gson;}
	void setBasePath(Path base){activePath=base.resolve("themes").resolve("active-theme.json");}
	void load(){active=new ThemeProfile();if(activePath!=null){for(final Path candidate:SafeFileStorage.readCandidates(activePath)){if(!Files.isRegularFile(candidate))continue;try{final ThemeProfile value=gson.fromJson(Files.readString(candidate,StandardCharsets.UTF_8),ThemeProfile.class);if(value!=null){active=value;break;}}catch(final IOException|RuntimeException ignored){}}}active.normalize();apply(active,false);}
	public synchronized ThemeProfile current(){return active.copy();}
	public synchronized void applyCurrentColors(){apply(active,false);}
	public synchronized void reset(){active=new ThemeProfile();apply(active,true);write();}
	public synchronized void importTheme(Path source)throws IOException{if(source==null||!Files.isRegularFile(source)||Files.size(source)>MAX_THEME_BYTES)throw new IOException("Theme file is invalid or larger than 128 KiB");final ThemeProfile value;try{value=gson.fromJson(Files.readString(source,StandardCharsets.UTF_8),ThemeProfile.class);}catch(final RuntimeException exception){throw new IOException("Theme JSON is malformed",exception);}if(value==null)throw new IOException("Theme JSON is empty");value.normalize();active=value;apply(value,true);write();}
	public synchronized void exportTheme(Path destination)throws IOException{if(destination==null)throw new IOException("No destination selected");final Settings settings=MusicPlayerManager.getSettingsManager().getSettings();final ThemeProfile value=active.copy();value.miniPlayerWidth=settings.getMiniPlayerWidth();value.miniPlayerScale=settings.getMiniPlayerScale();Files.createDirectories(destination.toAbsolutePath().getParent());SafeFileStorage.writeAtomically(destination,output->output.write(gson.toJson(value).getBytes(StandardCharsets.UTF_8)));}
	private void apply(ThemeProfile value,boolean layout){MusicPlayerColors.apply(value.grey,value.green,value.yellow,value.lightGreen);if(layout){final Settings settings=MusicPlayerManager.getSettingsManager().getSettings();settings.setMiniPlayerWidth(value.miniPlayerWidth);settings.setMiniPlayerScale(value.miniPlayerScale);}}
	private void write(){if(activePath==null)return;try{SafeFileStorage.writeAtomically(activePath,output->output.write(gson.toJson(active).getBytes(StandardCharsets.UTF_8)));}catch(final IOException ignored){}}
	public static final class ThemeProfile{public String name="Music Player Green";public int grey=0x555555FF;public int green=0x3E9100FF;public int yellow=0xFFFF00FF;public int lightGreen=0x80FF00FF;public int miniPlayerWidth=120;public float miniPlayerScale=1F;private void normalize(){if(name==null||name.isBlank())name="Imported theme";miniPlayerWidth=Math.clamp(miniPlayerWidth,80,640);if(!Float.isFinite(miniPlayerScale)||miniPlayerScale<Settings.MIN_OVERLAY_SCALE||miniPlayerScale>Settings.MAX_OVERLAY_SCALE)miniPlayerScale=1F;}private ThemeProfile copy(){final ThemeProfile value=new ThemeProfile();value.name=name;value.grey=grey;value.green=green;value.yellow=yellow;value.lightGreen=lightGreen;value.miniPlayerWidth=miniPlayerWidth;value.miniPlayerScale=miniPlayerScale;return value;}}
}
