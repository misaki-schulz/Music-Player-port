package info.u_team.music_player.gui;

import static info.u_team.music_player.init.MusicPlayerLocalization.GUI_CREATE_PLAYLIST_ADD_LIST;
import static info.u_team.music_player.init.MusicPlayerLocalization.GUI_CREATE_PLAYLIST_INSERT_NAME;
import static info.u_team.music_player.init.MusicPlayerLocalization.getTranslation;

import org.apache.commons.lang3.StringUtils;

import info.u_team.music_player.gui.controls.GuiControls;
import info.u_team.music_player.init.MusicPlayerResources;
import info.u_team.music_player.gui.widget.ImageButton;
import info.u_team.music_player.gui.widget.UButton;
import info.u_team.music_player.gui.playlist.PlaylistFileDialogs;
import info.u_team.music_player.gui.history.GuiMusicHistory;
import info.u_team.music_player.gui.settings.GuiSleepTimer;
import info.u_team.music_player.gui.widget.ScrollingText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

public class GuiMusicPlayer extends BetterScreen {
	
	private EditBox namePlaylistField;
	
	private GuiMusicPlayerList playlistsList;
	
	private GuiControls controls;
	private String transferStatus = "";
	
	public GuiMusicPlayer() {
		super(Component.literal("musicplayer"));
	}
	
	@Override
	protected void init() {
		addRenderableWidget(new ImageButton(1, 1, 15, 15, MusicPlayerResources.TEXTURE_BACK, button -> minecraft.setScreen(null)));
		
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
		
		final int actionWidth = Math.max(52, Math.min(80, (width - 36) / 4));
		addRenderableWidget(new UButton(12, 88, actionWidth, 20, Component.literal(getTranslation("gui.main.import")), button ->
				PlaylistFileDialogs.importLibrary(() -> playlistsList.refresh(), value -> transferStatus = value)));
		addRenderableWidget(new UButton(16 + actionWidth, 88, actionWidth, 20, Component.literal(getTranslation("gui.main.export")), button ->
				PlaylistFileDialogs.exportLibrary(value -> transferStatus = value)));
		addRenderableWidget(new UButton(20 + actionWidth * 2, 88, actionWidth, 20, Component.literal(getTranslation("gui.main.history")), button -> minecraft.setScreen(new GuiMusicHistory(this))));
		addRenderableWidget(new UButton(24 + actionWidth * 3, 88, actionWidth, 20, Component.literal(getTranslation("gui.main.sleep")), button -> minecraft.setScreen(new GuiSleepTimer(this))));
		playlistsList = new GuiMusicPlayerList(12, 124, width - 24, height - 134);
		addWidget(playlistsList);
		
		controls = new GuiControls(this, 5, width);
		addWidget(controls);
	}
	
	@Override
	public void resize(Minecraft minecraft, int width, int height) {
		final String text = namePlaylistField.getValue();
		final ScrollingText titleRender = controls.getTitleRender();
		final ScrollingText authorRender = controls.getAuthorRender();
		this.init(minecraft, width, height);
		namePlaylistField.setValue(text);
		controls.copyTitleRendererState(titleRender);
		controls.copyAuthorRendererState(authorRender);
	}
	
	@Override
	public void tick() {
		controls.tick();
	}
	
	@Override
	public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.render(guiGraphics, mouseX, mouseY, partialTicks);
		playlistsList.render(guiGraphics, mouseX, mouseY, partialTicks);
		info.u_team.music_player.gui.util.GuiTextCompat.draw(guiGraphics, font, getTranslation(GUI_CREATE_PLAYLIST_ADD_LIST), 20, 65, 0xFFFFFF, false);
		info.u_team.music_player.gui.util.GuiTextCompat.draw(guiGraphics, font, transferStatus, 12, 112, 0xB8E986, false);
		namePlaylistField.render(guiGraphics, mouseX, mouseY, partialTicks);
		controls.render(guiGraphics, mouseX, mouseY, partialTicks);
	}
	
	@Override
	public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
		super.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
	}
	
	public GuiMusicPlayerList getPlaylistsList() {
		return playlistsList;
	}
	
}
