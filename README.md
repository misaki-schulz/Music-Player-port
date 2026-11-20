# Music Player — port.11

An unofficial port of [Music Player](https://github.com/MC-U-Team/Music-Player) 2.7.4.384 for selected Minecraft versions. The repository contains Fabric, Forge, and NeoForge profiles. See the [build matrix](ports/README.md) for the supported versions.

The current matrix covers Minecraft 1.20.1, 1.21.1, 1.21.11, and the 26.x releases that have a profile. Forge and NeoForge requirements are listed in [ports/LOADERS.md](ports/LOADERS.md).

## Build

From the repository root, run `build-menu.bat` on Windows or `sh build-menu.sh` on Linux and macOS.

```bash
python ports/release_cli.py --list
python ports/release_cli.py --all
python ports/loader_cli.py --all
python ports/verify_release.py
```

Builds run one profile at a time. Verified JARs are written to `dist/port.11/`, with exact Fabric builds under `dist/port.11/exact/`. To build one profile, run `./gradlew build` in its directory. JARs ending in `-thin` do not include the bundled audio libraries.

## Playback

The player supports local files, HTTP, YouTube, and SoundCloud. YouTube sign-in opens the system browser and stores the refresh token in the configuration directory. An external signature service can be selected with `musicplayer.youtube.cipher.url` and `musicplayer.youtube.cipher.password`.

SoundCloud requests retain `track_authorization`, and saved tracks refresh before playback. When a format returns 401, 403, or 404, the player tries the next supported format. Run `python ports/test_soundcloud.py <source-profile>` for the local HTTP regression.

## Playlists and search

Playlists can be sorted by title, artist, or file name or URI. Folder imports keep the natural file-name order. Search filters playlists and saved tracks without changing their order or the playback queue.

## Settings

The equalizer has ten bands from 32 Hz to 16 kHz and a separate bass boost. Settings persist between launches. The language button cycles through all nine bundled translations.

## Upstream and license

The port is maintained by [misaki-schulz](https://github.com/misaki-schulz) and is not an official U-Team release.

The root profile targets Minecraft 26.2 with Fabric Loader 0.19.3 or newer, Fabric API 0.156.0+26.2 or newer, and Java 25. Other profiles list their own requirements.

The project is licensed under Apache-2.0. See [LICENSE](LICENSE), [NOTICE](NOTICE), and [THIRD_PARTY_LICENSES](THIRD_PARTY_LICENSES).
