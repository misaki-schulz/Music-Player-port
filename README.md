# Music Player — port.10.2 for Minecraft 1.20.1–26.3

The multi-version port.10.2 sources and build matrix are in [ports/](ports/README.md).
Fabric supports 17 exact Minecraft versions, packaged into ten release JARs.
Forge supports 16 exact versions from 1.20.1 to 26.3; NeoForge supports all 17.
Minecraft 1.21.2 has no official Forge release.
See [native loader support](ports/LOADERS.md) for requirements and build commands.
Use `python ports/verify_release.py` to build all 50 loader/version targets,
run offline regressions, and validate/collect release JARs under `dist/port.10.2/`.
`--resume` continues a verification run after a failed target is fixed.
The 17 exact-version Fabric builds are written to `dist/port.10.2/exact/` when built.
The [client GUI scenario](ports/client-gui/README.md) launches real Minecraft clients
and exercises screens, playlists, WAV imports, playback controls, sorting and settings.
The [port.10.2 verification report](ports/verification/README.md) records all 50 builds,
33 offline regression suites and the completed GUI runs for all 16 Forge versions.
Further client runs were stopped at the maintainer's request. Account authorization, audio quality, game-world behavior
and the native Windows file chooser are outside that scenario's coverage.

On Windows, run `build-menu.bat`; on Linux/macOS, run `sh build-menu.sh`.
The menu lists ten compatible Minecraft ranges, one entry per release JAR.
Choose a range, build all ten Fabric JARs, use N for Forge/NeoForge, or clean generated project files.
For example, the single 1.21.6–1.21.8 entry builds its three versions and produces
one shared JAR. Use `--group 1.21.6-1.21.8` to select that range from the command line.
The clean option removes
`dist/port.10.2`, project-local Gradle caches, build folders, and generated logs.
It preserves the original source and does not touch the shared `~/.gradle` cache.
The old port.7 binaries are not included in this source tree: they cannot be
reproduced exactly from the current port.10.2 sources and must be archived separately.

The root source tree targets Minecraft 26.2 and preserves the port.8 source-order fix.
YouTube is registered before generic HTTP at startup, so saved video links are no
longer probed as audio files after restarting or switching Minecraft versions.
Existing playlist files remain compatible. Previous port.7 release JARs are archived separately.

## SoundCloud playback in port.10.2

All loader variants preserve `track_authorization` in SoundCloud transcoding requests
and refresh track metadata when playback starts, including restored playlists.
If a public format returns 401, 403 or 404, the player tries the next supported format.
Rate limits and unavailable tracks remain errors; retries are bounded.
The SoundCloud source name and playlist serialization remain compatible.
Supported MIME types with parameters, such as `audio/ogg; codecs="opus"`, are
recognized without selecting incompatible Ogg codecs.
YouTube source code is unchanged in this release.

`python ports/test_soundcloud.py <source-profile>` exercises the compiled implementation
against a local HTTP fixture: the original missing-token 404, renewed authorization,
format fallback, MIME parameters, cloned/restored tracks and unavailable/rate-limited responses.
The supplied original-mod log contained YouTube errors and did not establish the cause
of the users' SoundCloud failures; this fix addresses defects identified in the audio library.

Pinned audio dependencies are preserved in [vendor/](vendor/README.md) so fresh builds resolve the same release binaries even when the original Maven artifacts are unavailable.

## Original 26.2 baseline

Unofficial Fabric port of [Music Player](https://github.com/MC-U-Team/Music-Player) 2.7.1.351 for Minecraft 26.2.

This port is maintained by [misaki-schulz](https://github.com/misaki-schulz). It is not an official U-Team release.

## Requirements

- Minecraft 26.2
- Fabric Loader 0.19.3 or newer
- Fabric API 0.156.0+26.2 or newer
- Java 25

## Playlist sorting and folder import

Open a playlist and click **Sort playlist** below the playback controls. Choose title,
artist, or file name / URI, in ascending or descending order. Sorting ignores case
and compares numbers naturally (`Track 2` comes before `Track 10`). Equal keys
keep their existing relative order. The new order is saved immediately and the
currently playing track keeps playing; subsequent tracks follow the new order.
Nested online playlists move as a group and keep their provider's internal order.
Unavailable files stay in the playlist, using their file name / URI as a title fallback.

**Load folder** reads regular files in natural file-name order. Results retain that
order even when audio metadata loads asynchronously. **Add all** becomes available
when the batch finishes. Starting another search or leaving the search screen
ignores late results from the previous request. Existing playlist files remain compatible.

## YouTube sign-in

On the title screen, the mod asks whether to sign in to YouTube. Choosing **Skip** leaves YouTube unavailable for that game session; local files and other audio sources remain available. Sign-in opens the system browser and shows a one-time code in Minecraft. The refresh token is saved in the Music Player configuration directory with owner-only file permissions so subsequent launches do not require another sign-in. If the saved token stops working, the prompt appears again. The Music Player settings screen also has a sign-in button.

The YouTube source library warns that using OAuth may put the Google account at risk. Use a separate account. OAuth does not guarantee that YouTube will provide an audio stream or avoid every verification challenge.

This port pins an upstream `youtube-source` snapshot containing the TV client User-Agent fix for [issue #226](https://github.com/lavalink-devs/youtube-source/issues/226). The released `1.18.2` library predates that fix.

YouTube changes its player script frequently. When the built-in signature parser cannot decode it, this port uses the public [yt-cipher](https://github.com/kikkia/yt-cipher) service at `https://cipher.kikkia.dev/`. The service receives the stream URL and player script URL for decoding, but not the Google OAuth refresh token. It may be temporarily unavailable. To use your own compatible server, set JVM properties `-Dmusicplayer.youtube.cipher.url=https://your-server/` and, if needed, `-Dmusicplayer.youtube.cipher.password=your-password` in the launcher.

## Equalizer and languages

Open **Music Player Settings → Equalizer** for ten frequency controls (32 Hz to 16 kHz), an on/off switch, and a separate bass boost switch. Equalizer and bass boost settings are saved between launches. The reset button centers the ten controls. High boosts can distort loud audio, especially with player volume above 100%.

The language button cycles through every bundled translation: English, German, Japanese, Korean, Brazilian Portuguese, Russian, Turkish, Simplified Chinese, and Traditional Chinese. Missing strings fall back to English.

## Build

```bash
./gradlew build
```

The release JAR is written to `build/libs/`. Do not distribute the `-thin.jar`; it does not contain the bundled audio dependencies.

## Credits

- Music Player: HyCraftHD / U-Team and contributors
- Selected adapted classes from [U Team Core](https://github.com/MC-U-Team/U-Team-Core) 5.6.2.384
- Minecraft 26.2 port: misaki-schulz

Port-specific bugs belong in this repository's [issue tracker](https://github.com/misaki-schulz/Music-Player-port/issues), not in the upstream issue tracker.

## License

Licensed under Apache-2.0. See [LICENSE](LICENSE), [NOTICE](NOTICE), and [THIRD_PARTY_LICENSES](THIRD_PARTY_LICENSES).

## Search saved music

Filter the main playlist list by name. Inside a playlist, filter already added songs by title, including tracks inside provider playlists. Matching ignores case and surrounding spaces. Clearing the field restores the complete list; filtering leaves saved order and playback unchanged.
