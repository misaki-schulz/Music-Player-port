package info.u_team.music_player;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import info.u_team.music_player.init.NativeClientEvents;
@Mod(MusicPlayerReference.MODID)
public class MusicPlayerMod {
 public static final String MODID = MusicPlayerReference.MODID;
 public static final Logger LOGGER = LogUtils.getLogger();
 public MusicPlayerMod(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext context) {
  DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> NativeClientEvents.register(context));
 }
}
