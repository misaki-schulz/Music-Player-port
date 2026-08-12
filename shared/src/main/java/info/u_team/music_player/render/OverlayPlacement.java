package info.u_team.music_player.render;

import info.u_team.music_player.musicplayer.settings.IngameOverlayPosition;
import info.u_team.music_player.musicplayer.settings.Settings;

/** Pure placement math shared by every renderer and the layout editor. */
public final class OverlayPlacement {

	private static final int MARGIN = 3;

	private OverlayPlacement() {
	}

	public static float scale(Settings settings, int screenWidth, int screenHeight, int baseWidth, int baseHeight) {
		final float widthScale = settings.getMiniPlayerWidth() / (float) baseWidth;
		final float requested = settings.getOverlayScale() * settings.getMiniPlayerScale() * widthScale;
		final float fit = Math.min((screenWidth - MARGIN * 2F) / baseWidth, (screenHeight - MARGIN * 2F) / baseHeight);
		return Math.max(0.1F, Math.min(requested, fit));
	}

	public static int x(Settings settings, int screenWidth, int renderedWidth) {
		if (settings.getMiniPlayerX() >= 0) {
			return Math.clamp(settings.getMiniPlayerX(), MARGIN, Math.max(MARGIN, screenWidth - MARGIN - renderedWidth));
		}
		final IngameOverlayPosition position = settings.getIngameOverlayPosition();
		return position.isLeft() ? MARGIN : screenWidth - MARGIN - renderedWidth;
	}

	public static int y(Settings settings, int screenHeight, int renderedHeight) {
		if (settings.getMiniPlayerY() >= 0) {
			return Math.clamp(settings.getMiniPlayerY(), MARGIN, Math.max(MARGIN, screenHeight - MARGIN - renderedHeight));
		}
		final IngameOverlayPosition position = settings.getIngameOverlayPosition();
		return position.isUp() ? MARGIN : screenHeight - MARGIN - renderedHeight;
	}
}
