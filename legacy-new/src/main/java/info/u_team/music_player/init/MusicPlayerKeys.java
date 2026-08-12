package info.u_team.music_player.init;

import static info.u_team.music_player.init.MusicPlayerLocalization.KEY_OPEN;
import static info.u_team.music_player.init.MusicPlayerLocalization.KEY_PAUSE;
import static info.u_team.music_player.init.MusicPlayerLocalization.KEY_SKIP_BACK;
import static info.u_team.music_player.init.MusicPlayerLocalization.KEY_SKIP_FORWARD;

import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;

public class MusicPlayerKeys {
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(ResourceLocation.fromNamespaceAndPath("musicplayer", "main"));

	public static final KeyMapping OPEN = new KeyMapping(KEY_OPEN, GLFW.GLFW_KEY_F8, CATEGORY);
	public static final KeyMapping PAUSE = new KeyMapping(KEY_PAUSE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_8, CATEGORY);
	public static final KeyMapping SKIP_FORWARD = new KeyMapping(KEY_SKIP_FORWARD, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_9, CATEGORY);
	public static final KeyMapping SKIP_BACK = new KeyMapping(KEY_SKIP_BACK, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_7, CATEGORY);

	public static void register() {
		KeyBindingHelper.registerKeyBinding(OPEN);
		KeyBindingHelper.registerKeyBinding(PAUSE);
		KeyBindingHelper.registerKeyBinding(SKIP_FORWARD);
		KeyBindingHelper.registerKeyBinding(SKIP_BACK);
	}
}
