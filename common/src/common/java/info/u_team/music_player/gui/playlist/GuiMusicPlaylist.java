// Modified for Minecraft 26.2 by misaki-schulz; see NOTICE.
package info.u_team.music_player.gui.playlist;

import info.u_team.music_player.gui.BetterScreen;
import info.u_team.music_player.gui.GuiMusicPlayer;
import info.u_team.music_player.gui.controls.GuiControls;
import info.u_team.music_player.gui.playlist.search.GuiMusicSearch;
import info.u_team.music_player.init.MusicPlayerResources;
import info.u_team.music_player.musicplayer.playlist.Playlist;
import info.u_team.music_player.gui.widget.ImageButton;
import info.u_team.music_player.gui.widget.ScrollingText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class GuiMusicPlaylist extends BetterScreen {
	
	private final Playlist playlist;
	
	private final GuiMusicPlaylistList trackList;
	
	private ImageButton addTracksButton;
	
	private GuiControls controls;
	
	public GuiMusicPlaylist(Playlist playlist) {
		super(Component.literal("musicplaylist"));
		this.playlist = playlist;
		
		trackList = new GuiMusicPlaylistList(playlist);
		
		if (!playlist.isLoaded()) {
			playlist.load(() -> {
				if (Minecraft.getInstance().gui.screen() == this) { // Check if gui is still open
					Minecraft.getInstance().execute(() -> {
						if (Minecraft.getInstance().gui.screen() == this) { // Recheck gui because this is async on the main thread.
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
		backButton.setPressable(() -> minecraft.gui.setScreen(new GuiMusicPlayer()));
		
		addTracksButton = addRenderableWidget(new ImageButton(width - 35, 20, 22, 22, MusicPlayerResources.TEXTURE_ADD));
		addTracksButton.setPressable(() -> minecraft.gui.setScreen(new GuiMusicSearch(playlist)));
		
		if (!playlist.isLoaded()) {
			addTracksButton.active = false;
		}
		
		trackList.updateSettings(12, 50, width - 24, height - 60);
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
	public void resize(int width, int height) {
		final ScrollingText titleRender = controls.getTitleRender();
		final ScrollingText authorRender = controls.getAuthorRender();
		this.init(width, height);
		controls.copyTitleRendererState(titleRender);
		controls.copyAuthorRendererState(authorRender);
	}
	
	@Override
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
		trackList.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
		controls.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
	}
	
	@Override
	public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.extractBackground(guiGraphics, mouseX, mouseY, partialTicks);
	}
	
	public GuiMusicPlaylistList getTrackList() {
		return trackList;
	}
	
}
