package info.u_team.music_player.gui.playlist;

import static info.u_team.music_player.init.MusicPlayerLocalization.GUI_PLAYLIST_FILTER;
import static info.u_team.music_player.init.MusicPlayerLocalization.GUI_PLAYLIST_REORDER_HINT;
import static info.u_team.music_player.init.MusicPlayerLocalization.getTranslation;

import info.u_team.music_player.gui.BetterScreen;
import info.u_team.music_player.gui.GuiMusicPlayer;
import info.u_team.music_player.gui.controls.GuiControls;
import info.u_team.music_player.gui.playlist.search.GuiMusicSearch;
import info.u_team.music_player.init.MusicPlayerResources;
import info.u_team.music_player.musicplayer.playlist.Playlist;
import info.u_team.music_player.gui.widget.ImageButton;
import info.u_team.music_player.gui.widget.UButton;
import info.u_team.music_player.gui.widget.ScrollingText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class GuiMusicPlaylist extends BetterScreen {
	
	private final Playlist playlist;
	
	private final GuiMusicPlaylistList trackList;
	
	private ImageButton addTracksButton;
	private EditBox filterField;
	private String filter = "";
	private String transferStatus = getTranslation(GUI_PLAYLIST_REORDER_HINT);
	
	private GuiControls controls;
	
	public GuiMusicPlaylist(Playlist playlist) {
		super(Component.literal("musicplaylist"));
		this.playlist = playlist;
		
		trackList = new GuiMusicPlaylistList(playlist);
		
		if (!playlist.isLoaded()) {
			playlist.load(() -> {
				if (Minecraft.getInstance().screen == this) { // Check if gui is still open
					Minecraft.getInstance().execute(() -> {
						if (Minecraft.getInstance().screen == this) { // Recheck gui because this is async on the main thread.
							trackList.addAllEntries();
							if (addTracksButton != null) {
								addTracksButton.active = true;
							}
						}
					});
				}
			});
		}
	}
	
	@Override
	protected void init() {
		final ImageButton backButton = addRenderableWidget(new ImageButton(1, 1, 15, 15, MusicPlayerResources.TEXTURE_BACK));
		backButton.setPressable(() -> minecraft.setScreen(new GuiMusicPlayer()));
		filterField = new EditBox(font, 12, 34, Math.max(20, width - 106), 18, Component.literal(getTranslation(GUI_PLAYLIST_FILTER)));
		filterField.setMaxLength(200);
		filterField.setValue(filter);
		filterField.setResponder(value -> { filter = value; trackList.setFilter(value); });
		addWidget(filterField);
		addRenderableWidget(new UButton(width - 88, 33, 38, 20, Component.literal("M3U"), button ->
				PlaylistFileDialogs.exportM3u(playlist, value -> transferStatus = value)));
		
		addTracksButton = addRenderableWidget(new ImageButton(width - 44, 33, 20, 20, MusicPlayerResources.TEXTURE_ADD));
		addTracksButton.setPressable(() -> minecraft.setScreen(new GuiMusicSearch(playlist)));
		
		if (!playlist.isLoaded()) {
			addTracksButton.active = false;
		}
		
		trackList.updateSettings(12, 67, width - 24, Math.max(1, height - 73));
		trackList.addAllEntries();
		addWidget(trackList);
		
		controls = new GuiControls(this, 5, width);
		addWidget(controls);
	}
	
	@Override
	public void tick() {
		controls.tick();
		trackList.tick();
	}
	
	@Override
	public void resize(Minecraft minecraft, int width, int height) {
		final ScrollingText titleRender = controls.getTitleRender();
		final ScrollingText authorRender = controls.getAuthorRender();
		this.init(minecraft, width, height);
		controls.copyTitleRendererState(titleRender);
		controls.copyAuthorRendererState(authorRender);
	}
	
	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		trackList.render(guiGraphics, mouseX, mouseY, partialTicks);
		filterField.render(guiGraphics, mouseX, mouseY, partialTicks);
		info.u_team.music_player.gui.util.GuiTextCompat.draw(guiGraphics, font, info.u_team.music_player.gui.util.GuiTrackUtils.trimToWith(transferStatus, Math.max(20, width - 24)), 12, 56, 0xFFB8E986, false);
		controls.render(guiGraphics, mouseX, mouseY, partialTicks);
	}
	
	@Override
	public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
	}
	
	public GuiMusicPlaylistList getTrackList() {
		return trackList;
	}
	
}
