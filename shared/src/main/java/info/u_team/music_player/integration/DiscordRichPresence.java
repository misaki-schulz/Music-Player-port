package info.u_team.music_player.integration;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.gson.Gson;

import info.u_team.music_player.lavaplayer.api.IMusicPlayer;
import info.u_team.music_player.lavaplayer.api.audio.IPlayingTrack;
import info.u_team.music_player.musicplayer.settings.Settings;

/** Optional, rate-limited local Discord IPC. It never participates in player startup or playback. */
public final class DiscordRichPresence {
	private static final long UPDATE_INTERVAL_MILLIS=15_000L;
	private final Gson gson=new Gson();
	private final ExecutorService executor=Executors.newSingleThreadExecutor(r->{final Thread t=new Thread(r,"Music Player Discord presence");t.setDaemon(true);return t;});
	private final AtomicBoolean updateRunning=new AtomicBoolean();
	private volatile Transport transport;private volatile String connectedApplicationId="";private volatile long nextUpdate;private volatile String status="Disabled";

	public void tick(IMusicPlayer player,Settings settings){if(!settings.isDiscordRichPresence()||settings.getDiscordApplicationId().isBlank()){disconnect("Disabled");return;}final long now=System.currentTimeMillis();if(now<nextUpdate||!updateRunning.compareAndSet(false,true))return;nextUpdate=now+UPDATE_INTERVAL_MILLIS;executor.execute(()->{try{ensureConnected(settings.getDiscordApplicationId());sendActivity(player,settings);status="Connected";}catch(final Exception exception){disconnect("Discord unavailable");nextUpdate=System.currentTimeMillis()+30_000L;}finally{updateRunning.set(false);}});}
	public void settingsChanged(){nextUpdate=0L;}
	public String status(){return status;}
	public void shutdown(){disconnect("Disabled");executor.shutdownNow();}

	private void ensureConnected(String applicationId)throws IOException{if(transport!=null&&applicationId.equals(connectedApplicationId))return;disconnect("Connecting");transport=openTransport();connectedApplicationId=applicationId;sendFrame(0,Map.of("v",1,"client_id",applicationId));readFrame();}
	private void sendActivity(IMusicPlayer player,Settings settings)throws IOException{final Map<String,Object> activity=new LinkedHashMap<>();activity.put("instance",false);final IPlayingTrack track=player==null?null:player.getTrackManager().getCurrentTrack();if(track==null){activity.put("details","Browsing the music library");activity.put("state","Idle");}else{final boolean reveal=settings.isShareTrackTitle();activity.put("details",reveal?trim(track.getInfo().getFixedTitle(),128):"Listening to music");activity.put("state",player.getTrackManager().isPaused()?"Paused":reveal?trim(track.getInfo().getFixedAuthor(),128):"Playing");if(!player.getTrackManager().isPaused()&&!track.getInfo().isStream()){final long start=System.currentTimeMillis()/1000L-track.getPosition()/1000L;activity.put("timestamps",Map.of("start",start,"end",start+track.getDuration()/1000L));}}final Map<String,Object> command=new LinkedHashMap<>();command.put("cmd","SET_ACTIVITY");command.put("args",Map.of("pid",ProcessHandle.current().pid(),"activity",activity));command.put("nonce",UUID.randomUUID().toString());sendFrame(1,command);readFrame();}
	private void sendFrame(int opcode,Object payload)throws IOException{final byte[] data=gson.toJson(payload).getBytes(StandardCharsets.UTF_8);final ByteBuffer frame=ByteBuffer.allocate(8+data.length).order(ByteOrder.LITTLE_ENDIAN).putInt(opcode).putInt(data.length).put(data);transport.write(frame.array());}
	private void readFrame()throws IOException{final byte[] header=transport.readExact(8);final ByteBuffer values=ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN);values.getInt();final int length=values.getInt();if(length<0||length>1_048_576)throw new IOException("Invalid Discord IPC frame length");transport.readExact(length);}
	private void disconnect(String reason){final Transport current=transport;transport=null;connectedApplicationId="";status=reason;if(current!=null)try{current.close();}catch(final IOException ignored){}}
	private static String trim(String value,int maximum){if(value==null)return "";return value.length()<=maximum?value:value.substring(0,maximum);}
	private static Transport openTransport()throws IOException{final boolean windows=System.getProperty("os.name","").toLowerCase(java.util.Locale.ROOT).contains("win");IOException failure=null;for(int i=0;i<10;i++){try{if(windows)return new FileTransport(new RandomAccessFile("\\\\?\\pipe\\discord-ipc-"+i,"rw"));final Path base=runtimeDirectory();final Path path=base.resolve("discord-ipc-"+i);if(Files.exists(path)){final SocketChannel channel=SocketChannel.open(StandardProtocolFamily.UNIX);channel.connect(UnixDomainSocketAddress.of(path));return new ChannelTransport(channel);}}catch(final IOException exception){failure=exception;}}throw failure==null?new IOException("Discord IPC endpoint was not found"):failure;}
	private static Path runtimeDirectory(){for(final String value:new String[]{System.getenv("XDG_RUNTIME_DIR"),System.getenv("TMPDIR"),System.getenv("TMP"),"/tmp"})if(value!=null&&!value.isBlank())return Path.of(value);return Path.of("/tmp");}
	private interface Transport extends Closeable{void write(byte[] data)throws IOException;byte[] readExact(int length)throws IOException;}
	private static final class FileTransport implements Transport{private final RandomAccessFile file;private FileTransport(RandomAccessFile file){this.file=file;}public void write(byte[] data)throws IOException{file.write(data);}public byte[] readExact(int length)throws IOException{final byte[] data=new byte[length];file.readFully(data);return data;}public void close()throws IOException{file.close();}}
	private static final class ChannelTransport implements Transport{private final SocketChannel channel;private ChannelTransport(SocketChannel channel){this.channel=channel;}public void write(byte[] data)throws IOException{final ByteBuffer buffer=ByteBuffer.wrap(data);while(buffer.hasRemaining())channel.write(buffer);}public byte[] readExact(int length)throws IOException{final ByteBuffer buffer=ByteBuffer.allocate(length);while(buffer.hasRemaining())if(channel.read(buffer)<0)throw new EOFException();return buffer.array();}public void close()throws IOException{channel.close();}}
}
