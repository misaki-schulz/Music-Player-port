// Logic from the 26.2 port, adapted to the direct legacy renderer; see NOTICE.
package info.u_team.music_player.gui.util;

import static info.u_team.music_player.init.MusicPlayerLocalization.GUI_TRACK_DURATION_UNDEFINED;
import static info.u_team.music_player.init.MusicPlayerLocalization.getTranslation;

import java.io.File;
import java.net.URI;
import java.util.function.Function;

import info.u_team.music_player.lavaplayer.api.audio.IAudioTrack;
import info.u_team.music_player.lavaplayer.api.audio.IAudioTrackInfo;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.util.TimeUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.Util;

public final class GuiTrackUtils {

	private static final Minecraft MINECRAFT = Minecraft.getInstance();

	private GuiTrackUtils() {
	}

	public static String trimToWith(String string, int width) {
		String value = MINECRAFT.font.plainSubstrByWidth(string, width);
		if (!value.equals(string)) {
			value += "...";
		}
		return value;
	}

	public static void addTrackInfo(GuiGraphics graphics, IAudioTrack track, int x, int y, int entryWidth, int leftMargin, int titleColor) {
		final int textSize = entryWidth - 150 - leftMargin;
		final IAudioTrackInfo trackInfo = track.getInfo();
		info.u_team.music_player.gui.util.GuiTextCompat.draw(graphics, MINECRAFT.font, trimToWith(trackInfo.getFixedTitle(), textSize), x + leftMargin, y + 5, titleColor, false);
		info.u_team.music_player.gui.util.GuiTextCompat.draw(graphics, MINECRAFT.font, trimToWith(trackInfo.getFixedAuthor(), textSize), x + leftMargin + 4, y + 25, 0xFFD86D1C, false);
		info.u_team.music_player.gui.util.GuiTextCompat.draw(graphics, MINECRAFT.font, getFormattedDuration(track), x + entryWidth - 140, y + 15, 0xFFFFFF00, false);
	}

	public static boolean openURI(String uri) {
		try {
			Util.getPlatform().openUri(new URI(uri));
		} catch (final Exception ex) {
			Util.getPlatform().openFile(new File(uri));
		}
		return true;
	}

	public static String getFormattedDuration(IAudioTrack track) {
		if (track != null && track.getInfo().isStream()) {
			return getTranslation(GUI_TRACK_DURATION_UNDEFINED);
		}
		return TimeUtil.timeConversion(track == null ? 0 : track.getDuration() / 1000);
	}

	public static String getFormattedPosition(IAudioTrack track) {
		return TimeUtil.timeConversion(track == null ? 0 : track.getPosition() / 1000);
	}

	public static <T> T getValueOfPlayingTrack(Function<IAudioTrack, T> function) {
		return getValueOfNullableTrack(MusicPlayerManager.getPlayer().getTrackManager().getCurrentTrack(), function);
	}

	public static <T> T getValueOfNullableTrack(IAudioTrack track, Function<IAudioTrack, T> function) {
		return track == null ? null : function.apply(track);
	}
}
