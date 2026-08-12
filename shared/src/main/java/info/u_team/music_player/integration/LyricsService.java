package info.u_team.music_player.integration;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import info.u_team.music_player.artwork.ArtworkRepository;
import info.u_team.music_player.lavaplayer.api.audio.IAudioTrack;
import info.u_team.music_player.musicplayer.MusicPlayerManager;

/** Local LRC/TXT overrides with an optional LRCLIB fallback. */
public final class LyricsService {
	private static final Pattern LRC=Pattern.compile("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?].*?");
	private static final ExecutorService EXECUTOR=Executors.newFixedThreadPool(2,r->{final Thread t=new Thread(r,"Music Player lyrics");t.setDaemon(true);return t;});
	private static final HttpClient HTTP=HttpClient.newBuilder().executor(EXECUTOR).connectTimeout(Duration.ofSeconds(8)).followRedirects(HttpClient.Redirect.NORMAL).build();
	private static final ConcurrentHashMap<String,CompletableFuture<Lyrics>> REQUESTS=new ConcurrentHashMap<>();
	private LyricsService(){}

	public static CompletableFuture<Lyrics> request(IAudioTrack track){if(track==null||track.getInfo()==null)return CompletableFuture.completedFuture(Lyrics.empty("No track"));final String key=stableKey(track);return REQUESTS.computeIfAbsent(key,ignored->load(track,key));}
	private static CompletableFuture<Lyrics> load(IAudioTrack track,String key){return CompletableFuture.supplyAsync(()->readLocal(key),EXECUTOR).thenCompose(local->{if(local!=null)return CompletableFuture.completedFuture(local);if(!MusicPlayerManager.getSettingsManager().getSettings().isOnlineLyricsProvider())return CompletableFuture.completedFuture(Lyrics.empty("No local lyrics"));final String title=encode(track.getInfo().getFixedTitle()),artist=encode(track.getInfo().getFixedAuthor());final long duration=Math.max(0L,track.getDuration()/1000L);final URI uri=URI.create("https://lrclib.net/api/get?track_name="+title+"&artist_name="+artist+"&duration="+duration);final HttpRequest request=HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(12)).header("User-Agent","MusicPlayer-Minecraft-Mod/2.7 (lyrics client)").GET().build();return HTTP.sendAsync(request,HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).thenApplyAsync(response->{if(response.statusCode()==404)return Lyrics.empty("Lyrics not found");if(response.statusCode()<200||response.statusCode()>=300)return Lyrics.empty("Lyrics provider HTTP "+response.statusCode());try{final JsonObject json=JsonParser.parseString(response.body()).getAsJsonObject();final String synced=getString(json,"syncedLyrics"),plain=getString(json,"plainLyrics");final String selected=MusicPlayerManager.getSettingsManager().getSettings().isPreferSyncedLyrics()&&synced!=null?synced:plain!=null?plain:synced;if(selected==null||selected.isBlank())return Lyrics.empty("Lyrics not found");return parse(selected,synced!=null&&selected.equals(synced),"LRCLIB");}catch(final RuntimeException exception){return Lyrics.empty("Invalid lyrics response");}},EXECUTOR).exceptionally(error->Lyrics.empty("Lyrics provider unavailable"));});}
	private static Lyrics readLocal(String key){final Path directory=lyricsDirectory();for(final String extension:new String[]{".lrc",".txt"}){final Path path=directory.resolve(key+extension);if(!Files.isRegularFile(path))continue;try{final String content=Files.readString(path,StandardCharsets.UTF_8);return parse(content,extension.equals(".lrc"),"Local override");}catch(final IOException ignored){}}return null;}
	private static Lyrics parse(String text,boolean synchronizedLyrics,String source){final List<Line> lines=new ArrayList<>();if(synchronizedLyrics){for(final String raw:text.split("\\R")){final Matcher matcher=LRC.matcher(raw);if(!matcher.find())continue;final long minutes=Long.parseLong(matcher.group(1)),seconds=Long.parseLong(matcher.group(2));final String fraction=matcher.group(3);final long millis=fraction==null?0L:fraction.length()==1?Long.parseLong(fraction)*100L:fraction.length()==2?Long.parseLong(fraction)*10L:Long.parseLong(fraction.substring(0,Math.min(3,fraction.length())));final String value=raw.substring(matcher.end()).strip();if(!value.isBlank())lines.add(new Line((minutes*60L+seconds)*1000L+millis,value));}lines.sort(Comparator.comparingLong(Line::millis));}else{long order=0L;for(final String raw:text.split("\\R")){final String value=raw.strip();if(!value.isBlank())lines.add(new Line(order++,value));}}return lines.isEmpty()?Lyrics.empty("Lyrics are empty"):new Lyrics(List.copyOf(lines),synchronizedLyrics,source,"");}
	private static String stableKey(IAudioTrack track){final String uri=track.getInfo().getURI()==null?track.getInfo().getIdentifier():track.getInfo().getURI();return ArtworkRepository.cacheKey(uri==null?track.getInfo().getFixedTitle()+"|"+track.getInfo().getFixedAuthor():uri);}
	private static Path lyricsDirectory(){final Path base=MusicPlayerManager.getFiles().getDirectory();final Path path=(base==null?Path.of("config","musicplayer"):base).resolve("lyrics");try{Files.createDirectories(path);}catch(final IOException ignored){}return path;}
	private static String encode(String value){return URLEncoder.encode(value==null?"":value,StandardCharsets.UTF_8);}
	private static String getString(JsonObject object,String name){return object.has(name)&&!object.get(name).isJsonNull()?object.get(name).getAsString():null;}
	public static void shutdown(){REQUESTS.clear();EXECUTOR.shutdownNow();}
	public record Line(long millis,String text){}
	public record Lyrics(List<Line> lines,boolean synchronizedLyrics,String source,String message){public static Lyrics empty(String message){return new Lyrics(List.of(),false,"",message);}public int activeLine(long position){if(lines.isEmpty())return-1;if(!synchronizedLyrics)return 0;int selected=0;for(int i=0;i<lines.size();i++){if(lines.get(i).millis()>position)break;selected=i;}return selected;}}
}
