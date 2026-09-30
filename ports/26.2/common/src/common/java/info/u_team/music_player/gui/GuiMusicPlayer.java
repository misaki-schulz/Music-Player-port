// Modified for Minecraft 26.2 by misaki-schulz; see NOTICE.
package info.u_team.music_player.gui;

import static info.u_team.music_player.init.MusicPlayerLocalization.GUI_CREATE_PLAYLIST_ADD_LIST;
import static info.u_team.music_player.init.MusicPlayerLocalization.GUI_CREATE_PLAYLIST_INSERT_NAME;
import static info.u_team.music_player.init.MusicPlayerLocalization.getTranslation;

import org.apache.commons.lang3.StringUtils;

import info.u_team.music_player.gui.controls.GuiControls;
import info.u_team.music_player.init.MusicPlayerResources;
import info.u_team.music_player.gui.widget.ImageButton;
import info.u_team.music_player.gui.widget.ScrollingText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class GuiMusicPlayer extends BetterScreen {
	
	private EditBox namePlaylistField;
	
	private GuiMusicPlayerList playlistsList;
	
	private GuiControls controls;
	
	public GuiMusicPlayer() {
		super(Component.literal("musicplayer"));
	}
	
	@Override
	protected void init() {
		addRenderableWidget(new ImageButton(1, 1, 15, 15, MusicPlayerResources.TEXTURE_BACK, button -> minecraft.gui.setScreen(null)));
		
		namePlaylistField = new EditBox(font, 100, 60, width - 150, 20, Component.nullToEmpty(null));
		namePlaylistField.setMaxLength(500);
		addWidget(namePlaylistField);
		
		final ImageButton addPlaylistButton = addRenderableWidget(new ImageButton(width - 41, 59, 22, 22, MusicPlayerResources.TEXTURE_CREATE));
		addPlaylistButton.setPressable(() -> {
			final String name = namePlaylistField.getValue();
			if (StringUtils.isBlank(name) || name.equals(getTranslation(GUI_CREATE_PLAYLIST_INSERT_NAME))) {
				namePlaylistField.setValue(getTranslation(GUI_CREATE_PLAYLIST_INSERT_NAME));
				return;
			}
			playlistsList.addPlaylist(name);
			namePlaylistField.setValue("");
		});
		
		playlistsList = new GuiMusicPlayerList(12, 90, width - 24, height - 100);
		addWidget(playlistsList);
		
		controls = new GuiControls(this, 5, width);
		addWidget(controls);
	}
	
	@Override
	public void resize(int width, int height) {
		final String text = namePlaylistField.getValue();
		final ScrollingText titleRender = controls.getTitleRender();
		final ScrollingText authorRender = controls.getAuthorRender();
		this.init(width, height);
		namePlaylistField.setValue(text);
		controls.copyTitleRendererState(titleRender);
		controls.copyAuthorRendererState(authorRender);
	}
	
	@Override
	public void tick() {
		controls.tick();
	}
	
	@Override
	public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
		playlistsList.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
		guiGraphics.text(font, getTranslation(GUI_CREATE_PLAYLIST_ADD_LIST), 20, 65, 0xFFFFFFFF, false);
		namePlaylistField.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
		controls.extractRenderState(guiGraphics, mouseX, mouseY, partialTicks);
	}
	
	@Override
	public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.extractBackground(guiGraphics, mouseX, mouseY, partialTicks);
	}
	
	public GuiMusicPlayerList getPlaylistsList() {
		return playlistsList;
	}
	
}
