package info.u_team.music_player.gui.playlist;

import static info.u_team.music_player.init.MusicPlayerLocalization.getTranslation;

import info.u_team.music_player.gui.BetterScreen;
import info.u_team.music_player.gui.widget.UButton;
import info.u_team.music_player.musicplayer.playlist.Playlist;
import info.u_team.music_player.musicplayer.playlist.PlaylistSort;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class GuiMusicPlaylistSort extends BetterScreen {

	private final GuiMusicPlaylist previousScreen;
	private final Playlist playlist;

	public GuiMusicPlaylistSort(GuiMusicPlaylist previousScreen, Playlist playlist) {
		super(Component.literal(getTranslation("gui.playlist.sort")));
		this.previousScreen = previousScreen;
		this.playlist = playlist;
	}

	@Override
	protected void init() {
		final int panelWidth = Math.min(360, width - 24);
		final int buttonWidth = (panelWidth - 8) / 2;
		final int left = (width - panelWidth) / 2;
		final int top = Math.max(65, height / 2 - 40);
		final PlaylistSort[] orders = PlaylistSort.values();
		for (int index = 0; index < orders.length; index++) {
			final PlaylistSort order = orders[index];
			final UButton button = addRenderableWidget(new UButton(left + index % 2 * (buttonWidth + 8), top + index / 2 * 26,
					buttonWidth, 22, Component.literal(getTranslation(order.getTranslationKey()))));
			button.active = playlist.isLoaded();
			button.setPressable(() -> {
				if (playlist.sort(order)) {
					previousScreen.getTrackList().updateAllEntries();
				}
				onClose();
			});
		}
		addRenderableWidget(new UButton(left, top + 86, panelWidth, 22, Component.literal(getTranslation("gui.equalizer.back")), button -> onClose()));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		graphics.centeredText(font, getTranslation("gui.playlist.sort"), width / 2, 14, 0xFFFFFFFF);
		graphics.centeredText(font, getTranslation("gui.playlist.sort.hint"), width / 2, 40, 0xFFFFFFFF);
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(previousScreen);
	}
}
