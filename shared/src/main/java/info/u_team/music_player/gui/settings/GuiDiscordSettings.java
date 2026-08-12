package info.u_team.music_player.gui.settings;

import static info.u_team.music_player.init.MusicPlayerLocalization.getTranslation;

import info.u_team.music_player.gui.BetterScreen;
import info.u_team.music_player.gui.widget.UButton;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.musicplayer.settings.Settings;
import info.u_team.music_player.util.MinecraftGuiCompat;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class GuiDiscordSettings extends BetterScreen {
	private final Screen previous;private EditBox applicationId;
	public GuiDiscordSettings(Screen previous){super(Component.literal(getTranslation("gui.discord.title")));this.previous=previous;}
	@Override protected void init(){final Settings settings=MusicPlayerManager.getSettingsManager().getSettings();addRenderableWidget(new UButton(8,8,28,20,Component.literal("<"),b->saveAndClose()));applicationId=new EditBox(font,12,48,width-24,20,Component.literal(getTranslation("gui.advanced.discord_id")));applicationId.setMaxLength(24);applicationId.setValue(settings.getDiscordApplicationId());addRenderableWidget(applicationId);final UButton help=addRenderableWidget(new UButton(12,76,width-24,20,Component.literal(getTranslation("gui.discord.help"))));help.active=false;final UButton status=addRenderableWidget(new UButton(12,102,width-24,20,Component.literal(getTranslation("gui.discord.status",MusicPlayerManager.getDiscordRichPresence().status()))));status.active=false;addRenderableWidget(new UButton(12,132,width-24,20,Component.literal(getTranslation("gui.common.save")),b->saveAndClose()));}
	private void saveAndClose(){final String value=applicationId==null?"":applicationId.getValue().strip();if(value.isEmpty()||value.matches("[0-9]{15,24}")){MusicPlayerManager.getSettingsManager().getSettings().setDiscordApplicationId(value);MinecraftGuiCompat.setScreen(minecraft,previous);}else applicationId.setValue("");}
}
