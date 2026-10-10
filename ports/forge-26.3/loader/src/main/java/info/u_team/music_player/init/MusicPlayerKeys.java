// Modified for Minecraft 26.2 by misaki-schulz; see NOTICE.
package info.u_team.music_player.init;

import static info.u_team.music_player.init.MusicPlayerLocalization.KEY_OPEN;
import static info.u_team.music_player.init.MusicPlayerLocalization.KEY_PAUSE;
import static info.u_team.music_player.init.MusicPlayerLocalization.KEY_SKIP_BACK;
import static info.u_team.music_player.init.MusicPlayerLocalization.KEY_SKIP_FORWARD;

import com.mojang.blaze3d.platform.InputConstants;

import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class MusicPlayerKeys {
	
	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("musicplayer", "main"));

	public static final KeyMapping OPEN = new KeyMapping(KEY_OPEN, InputConstants.KEY_F8, CATEGORY);
	public static final KeyMapping PAUSE = new KeyMapping(KEY_PAUSE, InputConstants.Type.KEYBOARD, InputConstants.KEY_NUMPAD8, CATEGORY);
	public static final KeyMapping SKIP_FORWARD = new KeyMapping(KEY_SKIP_FORWARD, InputConstants.Type.KEYBOARD, InputConstants.KEY_NUMPAD9, CATEGORY);
	public static final KeyMapping SKIP_BACK = new KeyMapping(KEY_SKIP_BACK, InputConstants.Type.KEYBOARD, InputConstants.KEY_NUMPAD7, CATEGORY);
	
	public static void register(RegisterKeyMappingsEvent event) {
		event.register(OPEN);
		event.register(PAUSE);
		event.register(SKIP_FORWARD);
		event.register(SKIP_BACK);
	}
}
