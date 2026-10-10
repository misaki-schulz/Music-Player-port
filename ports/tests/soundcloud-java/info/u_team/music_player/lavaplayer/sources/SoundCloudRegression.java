package info.u_team.music_player.lavaplayer.sources;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import com.sun.net.httpserver.HttpServer;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.HttpClients;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.*;
import com.sedmelluq.discord.lavaplayer.tools.*;
import com.sedmelluq.discord.lavaplayer.tools.http.AbstractHttpContextFilter;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.track.*;

public final class SoundCloudRegression {
    static String token = "old-token";
    static int streamStatus = 200;
    static final AtomicInteger deadCalls = new AtomicInteger(), liveCalls = new AtomicInteger(), resolves = new AtomicInteger();
    static String format(String path, String protocol, String mime) {
        return "{\"url\":\"https://api-v2.soundcloud.com/" + path + "?existing=keep\",\"format\":{\"protocol\":\"" + protocol + "\",\"mime_type\":\"" + mime + "\"}}";
    }
    static String metadata() {
        return "{\"kind\":\"track\",\"id\":123,\"title\":\"Fixture\",\"user\":{\"username\":\"Fixture\",\"avatar_url\":\"https://cdn.example/avatar-large.jpg\"},\"full_duration\":1000,\"policy\":\"ALLOW\",\"permalink_url\":\"https://soundcloud.com/fixture/track\",\"track_authorization\":\"" + token + "\",\"media\":{\"transcodings\":[" + format("dead", "hls", "audio/ogg") + "," + format("live", "progressive", "audio/mpeg") + "]}}";
    }
    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    static final class FixtureSource extends MusicPlayerSoundCloudSource {
        final int port;
        FixtureSource(int port) { this.port = port; }
        @Override public HttpInterface getHttpInterface() {
            return new HttpInterface(HttpClients.createDefault(), HttpClientContext.create(), true, new AbstractHttpContextFilter(null) {}) {
                @Override public CloseableHttpResponse execute(HttpUriRequest request) throws IOException {
                    URI uri = request.getURI();
                    check("api-v2.soundcloud.com".equals(uri.getHost()), "unexpected external HTTP request " + uri);
                    ((HttpRequestBase) request).setURI(URI.create("http://127.0.0.1:" + port + uri.getRawPath() + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery())));
                    return super.execute(request);
                }
            };
        }
    }
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getRawQuery();
            int status; String body;
            if (path.equals("/resolve")) { resolves.incrementAndGet(); status = 200; body = metadata(); }
            else if (path.equals("/dead")) { deadCalls.incrementAndGet(); status = 404; body = "{}"; }
            else { liveCalls.incrementAndGet(); status = query != null && query.contains("track_authorization=" + token) ? streamStatus : 404; body = "{\"url\":\"https://cdn.example/fixture.mp3\"}"; }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes); exchange.close();
        });
        server.start();
        try {
            FixtureSource source = new FixtureSource(server.getAddress().getPort());
            JsonBrowser data = JsonBrowser.parse(metadata());
            List<SoundCloudTrackFormat> original = new DefaultSoundCloudDataReader().readTrackFormats(data);
            check(!original.get(1).getLookupUrl().contains("track_authorization"), "baseline changed");
            try (HttpInterface http = source.getHttpInterface()) {
                try { SoundCloudHelper.loadPlaybackUrl(http, original.get(1).getLookupUrl()); throw new AssertionError("baseline must reproduce 404"); }
                catch (IOException expected) { check(expected.getMessage().contains("404"), "baseline failure is not 404"); }
            }
            var reader = new MusicPlayerSoundCloudSource.AuthorizedDataReader();
            var qualified = JsonBrowser.parse(metadata().replace("\"audio/ogg\"", "\"audio/ogg; codecs=\\\"opus\\\"\""));
            var formats = new DefaultSoundCloudFormatHandler();
            check(formats.buildFormatIdentifier(formats.chooseBestFormat(new DefaultSoundCloudDataReader().readTrackFormats(qualified))).startsWith("M:"), "baseline must ignore parameterized Opus MIME");
            check(formats.buildFormatIdentifier(formats.chooseBestFormat(reader.readTrackFormats(qualified))).startsWith("O:"), "parameterized Opus MIME was not recognized");
            check(source.loadFromTrackData(qualified).getIdentifier().startsWith("O:"), "Opus format priority changed");
            var vorbis = JsonBrowser.parse(metadata().replace("\"audio/ogg\"", "\"audio/ogg; codecs=\\\"vorbis\\\"\""));
            check(formats.buildFormatIdentifier(formats.chooseBestFormat(reader.readTrackFormats(vorbis))).startsWith("M:"), "unsupported Vorbis selected as Opus");
            String lookup = reader.readTrackFormats(data).get(1).getLookupUrl();
            check(lookup.contains("existing=keep") && lookup.contains("track_authorization=old-token"), "token/query lost");
            var foreign = JsonBrowser.parse(metadata().replace("https://api-v2.soundcloud.com/live", "https://foreign.example/live"));
            check(!reader.readTrackFormats(foreign).get(1).getLookupUrl().contains("track_authorization"), "token leaked to foreign host");
            AudioTrack queued = (AudioTrack) source.loadItem(null, new AudioReference("https://soundcloud.com/fixture/track", null));
            String secretPage = "https://soundcloud.com/fixture/track/s-secret?secret_token=s-secret";
            AudioTrack unlisted = source.loadTrack(secretPage, false);
            check(unlisted.getInfo().uri.equals(secretPage), "unlisted page secret was lost");
            check(source.refreshForPlayback(unlisted.getInfo()).getInfo().uri.equals(secretPage), "unlisted refresh lost its secret URL");
            token = "fresh-token"; deadCalls.set(0); liveCalls.set(0);
            AudioTrack playable = source.refreshForPlayback(queued.getInfo());
            check(playable.getIdentifier().startsWith("M:") && playable.getIdentifier().contains("fresh-token"), "metadata/token not refreshed or fallback failed");
            check(deadCalls.get() == 1 && liveCalls.get() == 1, "fallback retries are not bounded");
            check(queued.makeClone().getSourceManager() == source, "clone lost refresh source");
            ByteArrayOutputStream encoded = new ByteArrayOutputStream();
            source.encodeTrack(queued, new DataOutputStream(encoded));
            check(encoded.size() == 0 && source.getSourceName().equals("soundcloud"), "old playlist encoding changed");
            AudioTrack restored = source.decodeTrack(queued.getInfo(), new DataInputStream(new ByteArrayInputStream(encoded.toByteArray())));
            check(source.refreshForPlayback(restored.getInfo()).getIdentifier().contains("fresh-token"), "saved track not refreshed");
            streamStatus = 429; liveCalls.set(0);
            try { source.refreshForPlayback(restored.getInfo()); throw new AssertionError("429 must propagate"); }
            catch (IOException expected) { check(liveCalls.get() == 1, "rate limit retried"); }
            streamStatus = 403;
            try { source.refreshForPlayback(restored.getInfo()); throw new AssertionError("unavailable formats must fail"); }
            catch (FriendlyException expected) { check(expected.severity == FriendlyException.Severity.COMMON, "unclear unavailable error"); }
            try { source.loadFromTrackData(JsonBrowser.parse(metadata().replace("ALLOW", "BLOCK"))); throw new AssertionError("blocked track accepted"); }
            catch (FriendlyException expected) { /* preserve access restrictions */ }
            System.out.println("PASS SoundCloud: baseline 404, authorization, query/host safety, MIME parameters and codec safety, refresh, unlisted URL, format fallback, clone/save, bounded errors, blocked track");
        } finally { server.stop(0); }
    }
}
