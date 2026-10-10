package info.u_team.music_player.platform;
import java.nio.file.Path;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;
import info.u_team.music_player.MusicPlayerReference;
public final class LoaderPlatform {
 private LoaderPlatform() {}
 public static Path configDirectory() { return FMLPaths.CONFIGDIR.get(); }
 public static Path modResource(String path) {
  return ModList.get().getModFileById(MusicPlayerReference.MODID).getFile().findResource(path);
 }
}
