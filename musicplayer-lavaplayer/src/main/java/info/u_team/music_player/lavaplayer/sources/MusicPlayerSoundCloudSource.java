package info.u_team.music_player.lavaplayer.sources;

import java.io.DataInput;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.DefaultSoundCloudDataLoader;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.DefaultSoundCloudDataReader;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.DefaultSoundCloudFormatHandler;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.DefaultSoundCloudPlaylistLoader;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.DefaultSoundCloudTrackFormat;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioTrack;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudTrackFormat;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools;
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.BasicAudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;

/** Keeps SoundCloud authorization and resolves current transcodings when playback starts. */
public class MusicPlayerSoundCloudSource extends SoundCloudAudioSourceManager {
    private final AuthorizedDataReader reader;

    public MusicPlayerSoundCloudSource() {
        this(new AuthorizedDataReader(), new DefaultSoundCloudDataLoader(), new DefaultSoundCloudFormatHandler());
    }

    private MusicPlayerSoundCloudSource(AuthorizedDataReader reader, DefaultSoundCloudDataLoader loader,
            DefaultSoundCloudFormatHandler formats) {
        super(true, reader, loader, formats, new DefaultSoundCloudPlaylistLoader(loader, reader, formats), false);
        this.reader = reader;
    }

    @Override
    protected AudioTrack loadFromTrackData(JsonBrowser data) {
        if (reader.isTrackBlocked(data)) {
            throw new FriendlyException("This SoundCloud track is unavailable in your region.",
                    FriendlyException.Severity.COMMON, null);
        }
        final List<SoundCloudTrackFormat> remaining = new ArrayList<>(reader.readTrackFormats(data));
        final List<AudioTrackInfo> candidates = new ArrayList<>();
        while (!remaining.isEmpty()) {
            final SoundCloudTrackFormat format;
            try {
                format = getFormatHandler().chooseBestFormat(remaining);
            } catch (RuntimeException unsupported) {
                break;
            }
            candidates.add(reader.readTrackInfo(data, getFormatHandler().buildFormatIdentifier(format)));
            remaining.remove(format);
        }
        if (candidates.isEmpty()) {
            throw new FriendlyException("SoundCloud has no supported public audio format for this track.",
                    FriendlyException.Severity.COMMON, null);
        }
        return new RefreshableTrack(candidates.get(0), this, candidates);
    }

    @Override
    public AudioItem loadItem(AudioPlayerManager manager, AudioReference reference) {
        final AudioItem item = super.loadItem(manager, reference);
        if (item instanceof AudioTrack track) return refreshable(track);
        if (item instanceof AudioPlaylist playlist) {
            final List<AudioTrack> tracks = new ArrayList<>();
            AudioTrack selected = null;
            for (AudioTrack track : playlist.getTracks()) {
                final AudioTrack wrapped = refreshable(track);
                tracks.add(wrapped);
                if (track == playlist.getSelectedTrack()) selected = wrapped;
            }
            return new BasicAudioPlaylist(playlist.getName(), tracks, selected, playlist.isSearchResult());
        }
        return item;
    }

    private AudioTrack refreshable(AudioTrack track) {
        return track instanceof RefreshableTrack ? track : new RefreshableTrack(track.getInfo(), this, List.of());
    }

    @Override
    public AudioTrack loadTrack(String pageUrl, boolean honorPreviewFilter) {
        final AudioTrack loaded = super.loadTrack(pageUrl, honorPreviewFilter);
        if (!(loaded instanceof RefreshableTrack fresh)) return loaded;
        // Keep the supplied secret path/query for unlisted tracks; permalink_url can omit it.
        final List<AudioTrackInfo> candidates = new ArrayList<>();
        for (AudioTrackInfo info : fresh.candidates) candidates.add(withUri(info, pageUrl));
        return new RefreshableTrack(withUri(fresh.getInfo(), pageUrl), this, candidates);
    }

    private static AudioTrackInfo withUri(AudioTrackInfo info, String uri) {
        return new AudioTrackInfo(info.title, info.author, info.length, info.identifier, info.isStream,
                uri, info.artworkUrl, info.isrc);
    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo info, DataInput input) {
        // Preserve Lavaplayer's existing soundcloud source name and encoding format.
        return new RefreshableTrack(info, this, List.of());
    }

    SoundCloudAudioTrack refreshForPlayback(AudioTrackInfo saved) throws IOException {
        if (saved.uri == null || saved.uri.isBlank()) {
            throw new FriendlyException("The saved SoundCloud track has no page URL. Add it again.",
                    FriendlyException.Severity.COMMON, null);
        }
        // Even newly queued tracks may wait long enough for their authorization/lookup URL to expire.
        final AudioTrack loaded = loadTrack(saved.uri, false);
        if (!(loaded instanceof RefreshableTrack fresh)) {
            throw new FriendlyException("This SoundCloud track is no longer available.",
                    FriendlyException.Severity.COMMON, null);
        }
        try (HttpInterface http = getHttpInterface()) {
            for (AudioTrackInfo candidate : fresh.candidates) {
                try (CloseableHttpResponse response = http.execute(new HttpGet(candidate.identifier.substring(2)))) {
                    final int status = response.getStatusLine().getStatusCode();
                    // A transcoding may be unavailable while another public format still works.
                    if (status == 401 || status == 403 || status == 404) continue;
                    HttpClientTools.assertSuccessWithContent(response, "SoundCloud stream response");
                    final String url = JsonBrowser.parse(response.getEntity().getContent()).get("url").text();
                    if (url == null || url.isBlank()) continue;
                    return new SoundCloudAudioTrack(candidate, this);
                }
            }
        }
        throw new FriendlyException("SoundCloud did not provide an accessible audio stream for this track.",
                FriendlyException.Severity.COMMON, null);
    }

    static final class AuthorizedDataReader extends DefaultSoundCloudDataReader {
        private static String supportedMimeType(String mime) {
            try {
                final ContentType type = ContentType.parse(mime);
                final String base = type.getMimeType();
                final String codec = type.getParameter("codecs");
                // SoundCloud's Ogg decoder supports Opus; do not relabel other codecs.
                if ("audio/ogg".equals(base) && (codec == null || "opus".equalsIgnoreCase(codec.trim()))) return base;
                if ("audio/mpeg".equals(base)) return base;
            } catch (RuntimeException invalidMime) {
                // Preserve unsupported/malformed formats so the selector skips them.
            }
            return mime;
        }

        @Override
        public List<SoundCloudTrackFormat> readTrackFormats(JsonBrowser data) {
            final String authorization = data.get("track_authorization").text();
            final List<SoundCloudTrackFormat> result = new ArrayList<>();
            for (SoundCloudTrackFormat format : super.readTrackFormats(data)) {
                String lookup = format.getLookupUrl();
                if (authorization != null && !authorization.isBlank()) {
                    try {
                        final URIBuilder uri = new URIBuilder(lookup);
                        // Never attach the track token to a CDN or unrelated third-party URL.
                        if ("https".equalsIgnoreCase(uri.getScheme()) && "api-v2.soundcloud.com".equalsIgnoreCase(uri.getHost())) {
                            lookup = uri.setParameter("track_authorization", authorization).build().toString();
                        }
                    } catch (URISyntaxException invalid) {
                        throw new IllegalArgumentException("Invalid SoundCloud transcoding URL", invalid);
                    }
                }
                result.add(new DefaultSoundCloudTrackFormat(format.getTrackId(), format.getProtocol(), supportedMimeType(format.getMimeType()), lookup));
            }
            return result;
        }
    }

    private static final class RefreshableTrack extends DelegatedAudioTrack {
        private final MusicPlayerSoundCloudSource source;
        private final List<AudioTrackInfo> candidates;

        RefreshableTrack(AudioTrackInfo info, MusicPlayerSoundCloudSource source, List<AudioTrackInfo> candidates) {
            super(info);
            this.source = source;
            this.candidates = List.copyOf(candidates);
        }

        @Override
        public void process(LocalAudioTrackExecutor executor) throws Exception {
            processDelegate(source.refreshForPlayback(trackInfo), executor);
        }

        @Override
        protected AudioTrack makeShallowClone() {
            return new RefreshableTrack(trackInfo, source, candidates);
        }

        @Override
        public AudioSourceManager getSourceManager() { return source; }
    }
}
