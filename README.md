# Music Player — port.8 for Minecraft 1.21.2–26.3

The multi-version port.8 sources and build matrix are in [ports/](ports/README.md).
There are 15 exact-version build targets, including Minecraft 26.1.1 and 26.1.2,
packaged into eight multi-version release JARs.
Use `ports/Build-All.ps1` to build the matrix and `python ports/package_ports.py`
to validate and collect the release JARs under `dist/port.8/`.
The 15 exact-version builds are written to `dist/port.8/exact/` when built.
The user provided a successful startup log on Minecraft 26.2 with port.8.
Gameplay, audio, and startup on the other versions still require manual testing.

On Windows, run `build-menu.bat`; on Linux/macOS, run `sh build-menu.sh`.
The menu can build one selected Minecraft version's shared JAR, build all eight JARs,
or clean generated project files. For example, choosing 1.21.7 builds 1.21.6,
1.21.7, and 1.21.8 because they share one release JAR. The clean option removes
`dist/port.8`, project-local Gradle caches, build folders, and generated logs.
It preserves the original source and does not touch the shared `~/.gradle` cache.
The old port.7 binaries are not included in this source tree: they cannot be
reproduced exactly from the current port.8 sources and must be archived separately.

The root source tree targets Minecraft 26.2 and includes the port.8 source-order fix.
YouTube is registered before generic HTTP at startup, so saved video links are no
longer probed as audio files after restarting or switching Minecraft versions.
Existing playlist files remain compatible. Previous port.7 release JARs are archived separately.

## Original 26.2 baseline

Unofficial Fabric port of [Music Player](https://github.com/MC-U-Team/Music-Player) 2.7.1.351 for Minecraft 26.2.

This port is maintained by [misaki-schulz](https://github.com/misaki-schulz). It is not an official U-Team release.

## Requirements

- Minecraft 26.2
- Fabric Loader 0.19.3 or newer
- Fabric API 0.156.0+26.2 or newer
- Java 25

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
