# Changelog

## 2.7.1.351+multi.port.6

- Fixed all legacy GUI text using RGB colors (including track titles, playlist names, counters, search labels, and errors) rendering fully transparent because the colors were missing an alpha channel.
- Added safe fallbacks for blank track titles, artist names, and playlist names.
- Removed YouTube's service suffix ` - Topic` from displayed artist names.
- Added `build-version.bat` with an interactive choice for either 1.21.x compatibility range, 26.x, or all targets.

## 2.7.1.351+multi.port.5

- Fixed the Minecraft 1.21.11 F8 menu crash caused by the new `AbstractButton.renderContents` binary contract.
- Restored the original PNG icons on legacy music-player buttons instead of text placeholders.
- Made long button and slider labels fit their widgets, including the Russian volume label.
- Runtime-tested F8 menu rendering on Minecraft 1.21.2, 1.21.8, and 1.21.11.

## 2.7.1.351+multi.port.4

- Fixed the F8 music player hotkey on title, pause, and other GUI screens for Minecraft 1.21.2 through 1.21.11.
- Routed screen key presses through the Fabric screen keyboard event while keeping normal gameplay input on the client tick.

## 2.7.1.351+multi.port.3

- Added two range-compatible JARs covering Minecraft 1.21.2 through 1.21.11 on Java 21.
- Kept the existing universal Minecraft 26.x JAR on Java 25.
- Shared the legacy implementation through `legacy-common`; only incompatible API overlays are split at Minecraft 1.21.9.
- Added compatibility adapters for rendering, input, list widgets, and the Minecraft 1.21.11 screen initialization change.
- Replaced the per-version script and CI matrix with one standard `gradlew build` command producing all three JARs.

## 2.7.1.351+mc26.x.port.2

- Backported the existing Minecraft 26.2 port to 26.1, 26.1.1, and 26.1.2.
- Added a compatibility adapter for the screen API moved in Minecraft 26.2.
- Added universal and exact-version Gradle build targets for Minecraft 26.1 through 26.2.
- Added a GitHub Actions build matrix and a Windows multi-version build script.

## 2.7.1.351+mc26.2.port.1

- Ported the Fabric client to Minecraft 26.2 and Java 25.
- Updated GUI, rendering, input, audio, localization, and resource APIs.
- Replaced the external U Team Core runtime dependency with the required adapted classes.
- Kept Fabric API as a required dependency.
