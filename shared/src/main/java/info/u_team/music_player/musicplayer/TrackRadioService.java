package info.u_team.music_player.musicplayer;

import static info.u_team.music_player.init.MusicPlayerLocalization.getTranslation;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import info.u_team.music_player.lavaplayer.api.audio.IAudioTrack;
import info.u_team.music_player.lavaplayer.api.audio.IPlayingTrack;
import info.u_team.music_player.lavaplayer.api.search.ISearchResult;
import net.minecraft.client.Minecraft;

/** Creates a temporary recommendation queue. Results are never written to a playlist until the user does so manually. */
public final class TrackRadioService {
	private TrackRadioService(){}
	public static void queueRecommendations(int maximum,Consumer<String> status){final IPlayingTrack current=MusicPlayerManager.getPlayer().getTrackManager().getCurrentTrack();if(current==null){publish(status,getTranslation("gui.now.nothing"));return;}publish(status,getTranslation("gui.radio.loading"));final String query="ytsearch:"+current.getInfo().getFixedAuthor()+" "+current.getInfo().getFixedTitle()+" mix";MusicPlayerManager.getPlayer().getTrackSearch().getTracks(query,result->{if(result==null||result.hasError()){publish(status,getTranslation("gui.radio.unavailable"));return;}final Set<String> recent=new HashSet<>(MusicPlayerManager.getLibraryStateManager().getRecentUris(20));recent.add(stable(current));int count=0;if(result.isList()&&result.getTrackList()!=null){for(final IAudioTrack track:result.getTrackList().getTracks()){if(count>=Math.clamp(maximum,1,50))break;if(track==null||recent.contains(stable(track)))continue;MusicPlayerManager.getPlayer().getTrackManager().queue(track);recent.add(stable(track));count++;}}else if(result.getTrack()!=null&&!recent.contains(stable(result.getTrack()))){MusicPlayerManager.getPlayer().getTrackManager().queue(result.getTrack());count=1;}publish(status,count==0?getTranslation("gui.radio.empty"):getTranslation("gui.radio.queued",count));});}
	private static String stable(IAudioTrack track){if(track==null||track.getInfo()==null)return"";final String uri=track.getInfo().getURI();return uri==null||uri.isBlank()?track.getInfo().getIdentifier():uri;}
	private static void publish(Consumer<String> status,String value){Minecraft.getInstance().execute(()->status.accept(value));}
}
