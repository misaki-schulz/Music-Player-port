# Music Player — port.11

Music Player is a client mod for playing local files, HTTP audio links, YouTube and SoundCloud in Minecraft. This unofficial port is based on Music Player 2.7.4.384. [Upstream project](https://github.com/MC-U-Team/Music-Player) · [Maintainer](https://github.com/misaki-schulz)

## Install

Fabric, Forge and NeoForge builds target Minecraft **1.20.1, 1.21.1, 1.21.11, 26.1, 26.1.1, 26.1.2, 26.2 and 26.3**. Put one Music Player JAR for your game version and loader in the `mods` folder. With Fabric, also install the matching Fabric API.

See [Fabric requirements and build commands](ports/README.md) or [Forge and NeoForge requirements](ports/LOADERS.md).

## Use the player

- Search for playlists by name or for tracks you’ve already added. Sort a playlist by title, artist, file name or URL. Imports from a folder keep files in natural order, so `Track 2` comes before `Track 10`.
- Sign in to YouTube from the player settings. Sign-in opens in your browser, and the mod saves its sign-in token in the Minecraft configuration folder.
- Use the equalizer and bass boost from the player settings. You can also select one of the nine bundled languages.

## Build

Run `build-menu.bat` on Windows or `sh build-menu.sh` on Linux or macOS. The build scripts put release JARs in `dist/port.11/` and exact Fabric builds in `dist/port.11/exact/`. JARs ending in `-thin` omit the bundled audio libraries.

The Gradle project at the repository root targets Minecraft 26.2 with Java 25, Fabric Loader 0.19.3 or newer, and Fabric API 0.156.0+26.2 or newer. The release scripts use the profiles under `ports/`.

See the [changelog](CHANGELOG.md) for release notes.

## License

[Apache-2.0](LICENSE). See [NOTICE](NOTICE) and [third-party licenses](THIRD_PARTY_LICENSES).
