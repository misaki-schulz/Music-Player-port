# Music Player — port.11 / Minecraft 1.20.1

Unofficial Fabric port of [Music Player](https://github.com/MC-U-Team/Music-Player) 2.7.4.384.

## Requirements

- Minecraft 1.20.1
- Fabric Loader 0.19.5 or newer
- Fabric API from the exact entry in [../targets.json](../targets.json)
- Java 17

## Features

Playlists can be sorted by title, artist, or file name or URI. Folder imports keep natural file-name order. Search filters playlists and saved tracks without changing their order or the playback queue.

YouTube supports browser sign-in and a saved refresh token. SoundCloud keeps `track_authorization`, refreshes saved tracks, and tries the next format after a 401, 403, or 404 response.

The equalizer has ten bands from 32 Hz to 16 kHz and a separate bass boost. All nine bundled languages are available from the language button.

## Build

```bash
./gradlew build
python ports/package_ports.py --group 1.20.1
```

The exact JAR is written to `build/<Minecraft version>/libs/`, and the packaged release is written to `dist/port.11/`. JARs ending in `-thin` do not include the bundled audio libraries.

## More information

- Shared matrix and build commands — [ports/README.md](../README.md)
- Forge and NeoForge — [ports/LOADERS.md](../LOADERS.md)
- Changelog — [CHANGELOG.md](CHANGELOG.md)
- License — [LICENSE](LICENSE), [NOTICE](NOTICE), [THIRD_PARTY_LICENSES](THIRD_PARTY_LICENSES)
