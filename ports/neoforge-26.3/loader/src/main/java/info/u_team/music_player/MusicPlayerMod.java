package info.u_team.music_player;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import net.neoforged.fml.common.Mod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import info.u_team.music_player.init.NativeClientEvents;
@Mod(value = MusicPlayerReference.MODID, dist = Dist.CLIENT)
public class MusicPlayerMod {
 public static final String MODID = MusicPlayerReference.MODID;
 public static final Logger LOGGER = LogUtils.getLogger();
 public MusicPlayerMod(IEventBus modBus) { NativeClientEvents.register(modBus); }
}
