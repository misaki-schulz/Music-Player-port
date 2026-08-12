package info.u_team.music_player.init;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

import com.mojang.brigadier.arguments.StringArgumentType;

import info.u_team.music_player.gui.GuiMusicPlayer;
import info.u_team.music_player.musicplayer.MusicPlayerManager;
import info.u_team.music_player.musicplayer.MusicPlayerUtils;
import info.u_team.music_player.musicplayer.PlaybackActions;
import info.u_team.music_player.musicplayer.TrackRadioService;
import info.u_team.music_player.util.MinecraftGuiCompat;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;

public final class MusicPlayerClientCommands{
	private MusicPlayerClientCommands(){}
	public static void register(){ClientCommandRegistrationCallback.EVENT.register((dispatcher,access)->dispatcher.register(literal("music")
		.then(literal("open").executes(context->{MinecraftGuiCompat.setScreen(Minecraft.getInstance(),new GuiMusicPlayer());return 1;}))
		.then(literal("play").executes(context->{final var manager=MusicPlayerManager.getPlayer().getTrackManager();if(manager.getCurrentTrack()!=null)manager.setPaused(false);return 1;}))
		.then(literal("pause").executes(context->{final var manager=MusicPlayerManager.getPlayer().getTrackManager();if(manager.getCurrentTrack()!=null)manager.setPaused(true);return 1;}))
		.then(literal("next").executes(context->{MusicPlayerUtils.skipForward();return 1;}))
		.then(literal("previous").executes(context->{MusicPlayerUtils.skipBack();return 1;}))
		.then(literal("stop").executes(context->{MusicPlayerManager.getPlayer().getTrackManager().stop();return 1;}))
		.then(literal("radio").executes(context->{TrackRadioService.queueRecommendations(10,value->{});return 1;}))
		.then(literal("load").then(argument("uri",StringArgumentType.greedyString()).executes(context->{PlaybackActions.playNow(StringArgumentType.getString(context,"uri"),value->{});return 1;})))
		.then(literal("queue").then(argument("uri",StringArgumentType.greedyString()).executes(context->{PlaybackActions.queue(StringArgumentType.getString(context,"uri"),value->{});return 1;})))));}
}
