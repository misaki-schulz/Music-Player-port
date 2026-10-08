package info.u_team.music_player.platform;
import java.nio.file.Path;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import info.u_team.music_player.MusicPlayerReference;
public final class LoaderPlatform {
 private LoaderPlatform() {}
 public static Path configDirectory() { return FMLPaths.CONFIGDIR.get(); }
 public static Path modResource(String path) {
  return ModList.get().getModFileById(MusicPlayerReference.MODID).getFile().findResource(path);
 }
}
