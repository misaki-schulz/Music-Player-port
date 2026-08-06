// Modified for Minecraft 26.2 by misaki-schulz; see NOTICE.
package info.u_team.music_player.init;

import info.u_team.music_player.MusicPlayerReference;
import net.minecraft.resources.Identifier;

public class MusicPlayerResources {
	
	public static final Identifier TEXTURE_CREATE = createResource("create");
	public static final Identifier TEXTURE_OPEN = createResource("open");
	public static final Identifier TEXTURE_CLEAR = createResource("clear");
	public static final Identifier TEXTURE_PLAY = createResource("play");
	public static final Identifier TEXTURE_STOP = createResource("stop");
	public static final Identifier TEXTURE_PAUSE = createResource("pause");
	public static final Identifier TEXTURE_ADD = createResource("add");
	public static final Identifier TEXTURE_UP = createResource("up");
	public static final Identifier TEXTURE_DOWN = createResource("down");
	public static final Identifier TEXTURE_BACK = createResource("back");
	public static final Identifier TEXTURE_SKIP_FORWARD = createResource("skip-forward");
	public static final Identifier TEXTURE_SKIP_BACK = createResource("skip-back");
	public static final Identifier TEXTURE_SHUFFLE = createResource("shuffle");
	public static final Identifier TEXTURE_REPEAT = createResource("repeat");
	public static final Identifier TEXTURE_REPEAT_SINGLE = createResource("repeat-single");
	public static final Identifier TEXTURE_SETTINGS = createResource("settings");
	
	public static final Identifier TEXTURE_SOCIAL_YOUTUBE = createResource("socialmedia/youtube-logo");
	public static final Identifier TEXTURE_SOCIAL_SOUNDCLOUD = createResource("socialmedia/soundcloud-logo");
	
	private static Identifier createResource(String name) {
		return Identifier.fromNamespaceAndPath(MusicPlayerReference.MODID, "textures/gui/" + name + ".png");
	}
	
}
