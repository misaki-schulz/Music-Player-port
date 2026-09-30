# Music Player — port.8 / Minecraft 26.2

Unofficial Fabric port of [Music Player](https://github.com/MC-U-Team/Music-Player) 2.7.1.351 for Minecraft 26.2.

This port is maintained by [misaki-schulz](https://github.com/misaki-schulz). It is not an official U-Team release.

## Requirements

- Minecraft: 26.2
- Fabric Loader 0.19.5 or newer
- Fabric API: use the exact target entry in [../targets.json](../targets.json)
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

This profile produces an exact-version JAR under `build/<Minecraft version>/libs/`. Run `python ports/package_ports.py` from the repository root to validate all versions and create the grouped release JARs in `dist/port.8/`. Do not distribute `-thin.jar`; it lacks the bundled audio dependencies.

## Credits

- Music Player: HyCraftHD / U-Team and contributors
- Selected adapted classes from [U Team Core](https://github.com/MC-U-Team/U-Team-Core) 5.6.2.384
- Minecraft 26.2 port: misaki-schulz

Port-specific bugs belong in this repository's [issue tracker](https://github.com/misaki-schulz/Music-Player-port/issues), not in the upstream issue tracker.

## License

Licensed under Apache-2.0. See [LICENSE](LICENSE), [NOTICE](NOTICE), and [THIRD_PARTY_LICENSES](THIRD_PARTY_LICENSES).

## port.8 source-order fix

Saved YouTube URLs are routed before generic HTTP after restarting. Existing playlists remain compatible. See [the shared matrix and verification instructions](../README.md).
