package info.u_team.music_player;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import info.u_team.music_player.init.MusicPlayerClientConstruct;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class MusicPlayerMod implements ClientModInitializer {
	
	public static final String MODID = MusicPlayerReference.MODID;
	public static Logger LOGGER = LogUtils.getLogger();
	
	@Override
	public void onInitializeClient() {
		verifyFabricApiVersion();
		MusicPlayerClientConstruct.construct();
	}

	private static void verifyFabricApiVersion() {
		final FabricLoader loader = FabricLoader.getInstance();
		final String minecraft = loader.getModContainer("minecraft").orElseThrow().getMetadata().getVersion().getFriendlyString();
		final String minimum = switch (minecraft) {
			case "26.1" -> "0.145.1";
			case "26.1.1" -> "0.145.4";
			case "26.1.2" -> "0.155.3";
			default -> throw new IllegalStateException("Unsupported Minecraft version for Music Player port.8: " + minecraft);
		};
		final String installed = loader.getModContainer("fabric-api").orElseThrow().getMetadata().getVersion().getFriendlyString();
		final String[] actual = installed.split("[.+-]", 4);
		final String[] required = minimum.split("\\.");
		if (actual.length < 3) {
			throw new IllegalStateException("Invalid Fabric API version: " + installed);
		}
		try {
			for (int index = 0; index < 3; index++) {
				final int current = Integer.parseInt(actual[index]);
				final int needed = Integer.parseInt(required[index]);
				if (current > needed) return;
				if (current < needed) {
					throw new IllegalStateException("Music Player port.8 on Minecraft " + minecraft + " requires Fabric API " + minimum + " or newer; found " + installed);
				}
			}
		} catch (NumberFormatException exception) {
			throw new IllegalStateException("Invalid Fabric API version: " + installed, exception);
		}
	}
}
