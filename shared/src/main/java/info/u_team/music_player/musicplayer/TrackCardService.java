package info.u_team.music_player.musicplayer;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumMap;

import javax.imageio.ImageIO;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import info.u_team.music_player.artwork.ArtworkRepository;
import info.u_team.music_player.lavaplayer.api.audio.IPlayingTrack;
import info.u_team.music_player.util.TimeUtil;

public final class TrackCardService {
	private TrackCardService(){}
	public static void exportCard(IPlayingTrack track,Path destination,boolean includeQr)throws IOException{if(track==null)throw new IOException("Nothing playing");final BufferedImage image=new BufferedImage(960,540,BufferedImage.TYPE_INT_ARGB);final Graphics2D graphics=image.createGraphics();try{graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);graphics.setPaint(new GradientPaint(0,0,new Color(20,28,36),960,540,new Color(30,89,65)));graphics.fillRect(0,0,960,540);final BufferedImage artwork=artwork(track);if(artwork!=null)graphics.drawImage(artwork,42,42,456,456,null);else{graphics.setColor(new Color(42,48,56));graphics.fillRoundRect(42,42,456,456,28,28);graphics.setColor(new Color(62,145,0));graphics.fillOval(174,174,192,192);}graphics.setColor(Color.WHITE);graphics.setFont(new Font(Font.SANS_SERIF,Font.BOLD,34));drawFitted(graphics,track.getInfo().getFixedTitle(),530,96,390);graphics.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,23));graphics.setColor(new Color(190,224,211));drawFitted(graphics,track.getInfo().getFixedAuthor(),530,142,390);graphics.setColor(new Color(220,230,234));graphics.setFont(new Font(Font.MONOSPACED,Font.PLAIN,18));graphics.drawString(TimeUtil.timeConversion(track.getPosition()/1000L)+" / "+TimeUtil.timeConversion(track.getDuration()/1000L),530,190);final String uri=track.getInfo().getURI();graphics.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,16));graphics.setColor(new Color(150,180,170));drawFitted(graphics,safeHost(uri),530,224,390);if(includeQr&&isSafeHttp(uri)){final BufferedImage qr=qr(uri,190);graphics.drawImage(qr,530,272,null);graphics.setColor(Color.WHITE);graphics.drawString("Open track source",730,372);}graphics.setFont(new Font(Font.SANS_SERIF,Font.BOLD,16));graphics.setColor(new Color(117,224,181));graphics.drawString("Music Player for Minecraft",530,485);}finally{graphics.dispose();}Files.createDirectories(destination.toAbsolutePath().getParent());if(!ImageIO.write(image,"PNG",destination.toFile()))throw new IOException("PNG writer unavailable");}
	public static void exportQr(IPlayingTrack track,Path destination)throws IOException{if(track==null||!isSafeHttp(track.getInfo().getURI()))throw new IOException("Current track has no safe HTTP source");Files.createDirectories(destination.toAbsolutePath().getParent());if(!ImageIO.write(qr(track.getInfo().getURI(),512),"PNG",destination.toFile()))throw new IOException("PNG writer unavailable");}
	private static BufferedImage artwork(IPlayingTrack track){try{final byte[] bytes=ArtworkRepository.request(track.getInfo()).get(Duration.ofSeconds(18).toMillis(),java.util.concurrent.TimeUnit.MILLISECONDS);return ImageIO.read(new ByteArrayInputStream(bytes));}catch(final Exception ignored){return null;}}
	private static BufferedImage qr(String value,int size)throws IOException{try{final EnumMap<EncodeHintType,Object> hints=new EnumMap<>(EncodeHintType.class);hints.put(EncodeHintType.ERROR_CORRECTION,ErrorCorrectionLevel.M);hints.put(EncodeHintType.MARGIN,2);final BitMatrix matrix=new QRCodeWriter().encode(value,BarcodeFormat.QR_CODE,size,size,hints);final BufferedImage image=new BufferedImage(size,size,BufferedImage.TYPE_INT_RGB);for(int y=0;y<size;y++)for(int x=0;x<size;x++)image.setRGB(x,y,matrix.get(x,y)?0xFF000000:0xFFFFFFFF);return image;}catch(final WriterException exception){throw new IOException("Cannot create QR code",exception);}}
	private static void drawFitted(Graphics2D graphics,String value,int x,int y,int width){String text=value==null?"":value;while(text.length()>1&&graphics.getFontMetrics().stringWidth(text+"…")>width)text=text.substring(0,text.length()-1);graphics.drawString(text.equals(value)?text:text+"…",x,y);}
	private static boolean isSafeHttp(String value){if(value==null||value.isBlank())return false;try{final String scheme=URI.create(value).getScheme();return"http".equalsIgnoreCase(scheme)||"https".equalsIgnoreCase(scheme);}catch(final IllegalArgumentException ignored){return false;}}
	private static String safeHost(String value){if(!isSafeHttp(value))return"Local / unsupported source";final String host=URI.create(value).getHost();return host==null?"Web source":host;}
}
