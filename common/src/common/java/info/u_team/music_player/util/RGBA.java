// Derived from U Team Core 5.6.2.384 and modified for Minecraft 26.2; see NOTICE.
package info.u_team.music_player.util;

/** Lightweight RGBA color used by Music Player's built-in widgets. */
public final class RGBA {

	public static final RGBA WHITE = new RGBA(0xFFFFFFFF);

	private final int rgba;
	private final int argb;

	public RGBA(int rgba) {
		this.rgba = rgba;
		final int red = rgba >>> 24 & 0xFF;
		final int green = rgba >>> 16 & 0xFF;
		final int blue = rgba >>> 8 & 0xFF;
		final int alpha = rgba & 0xFF;
		argb = alpha << 24 | red << 16 | green << 8 | blue;
	}

	public int getColor() {
		return rgba;
	}

	public int getColorARGB() {
		return argb;
	}

	public int getColorARGB(float alphaMultiplier) {
		final int alpha = Math.clamp(Math.round((argb >>> 24) * alphaMultiplier), 0, 255);
		return alpha << 24 | argb & 0x00FFFFFF;
	}
}
