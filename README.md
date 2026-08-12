# Music Player — Multi-version Fabric Port

Unofficial Fabric port of [Music Player](https://github.com/MC-U-Team/Music-Player) 2.7.1.351 for Minecraft 1.21.2 through 1.21.11 and Minecraft 26.x. This repository is based on the working 26.2 port.

This port is maintained by [misaki-schulz](https://github.com/misaki-schulz). It is not an official U-Team release.

## Supported JARs

| JAR | Minecraft | Runtime Java | Source set |
| --- | --- | --- | --- |
| `music_player-fabric-1.21.2-1.21.8-*.jar` | 1.21.2–1.21.8 | 21 | `legacy-common` + `legacy-old` |
| `music_player-fabric-1.21.9-1.21.11-*.jar` | 1.21.9–1.21.11 | 21 | `legacy-common` + `legacy-new` |
| `music_player-fabric-26.x-*.jar` | 26.1–26.2 | 25 | `common` + `fabric` |

Install the Fabric API build matching the selected Minecraft version. The endpoint versions of both 1.21.x ranges were tested with the exact same JAR: 1.21.2/1.21.8 and 1.21.9/1.21.11. The 26.x JAR was tested on 26.1, 26.1.1, 26.1.2, and 26.2.

## Build

JDK 25 is required to build all targets. One command builds all three release JARs:

```powershell
.\gradlew.bat clean build
```

On Windows, double-click `build-version.bat` to choose one compatible Minecraft range or build all targets. Each 1.21.x choice builds one range-compatible JAR, not a separate copy for every patch version.

For a faster incremental rebuild, omit `clean`. Outputs are written to:

- `legacy-old/build/libs/`
- `legacy-new/build/libs/`
- `build/libs/`

Use the JAR without `-sources`, `-dev`, or `-thin` in its name. No separate PowerShell build script is needed.

## Why there are three source sets

There is no source copy for every Minecraft patch. The 1.21.x ports share 55 Java files in `legacy-common`; only the incompatible API adapters live in `legacy-old` and `legacy-new`.

The split at 1.21.9 is required by the input-event and list-rendering API changes. The separate 26.x source is required by the newer rendering API, unobfuscated development environment, and Java 25. Compatibility adapters handle smaller changes inside each range, including the 1.21.11 screen initialization change.

## Credits

- Music Player: HyCraftHD / U-Team and contributors
- Selected adapted classes from [U Team Core](https://github.com/MC-U-Team/U-Team-Core) 5.6.2.384
- Multi-version port: misaki-schulz

Port-specific bugs belong in this repository's [issue tracker](https://github.com/misaki-schulz/Music-Player-port/issues), not in the upstream issue tracker.

## License

Licensed under Apache-2.0. See [LICENSE](LICENSE), [NOTICE](NOTICE), and [THIRD_PARTY_LICENSES](THIRD_PARTY_LICENSES).
