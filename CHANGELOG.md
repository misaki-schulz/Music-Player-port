# Changelog

## 2.7.1.351+multi.port.7

- Fixed a Minecraft 26.x crash when audio ducking observed a sound event before Minecraft had resolved its nullable concrete sound.
- Fixed Minecraft 1.21.8 failing before the main menu when its changed `SoundManager.play` signature did not match the optional legacy ducking hook.
- Reworked the equalizer with draggable frequency/gain curve points, clearer zero-based graphic sliders, smooth deferred persistence, real positive bass gain, and localized Russian controls.
- Fixed shared advanced screens crashing on Minecraft 1.21.8 because runtime intermediary names hid the screen setter from the compatibility bridge.
- Removed the settings volume slider's direct dependency on the unstable `SoundManager.play` binary signature across the complete Minecraft 1.21.2–1.21.11 legacy line.
- Reworked the playlist layout on every target with compact controls, non-overlapping search/status/list regions, denser rows, properly bounded actions, and a neutral missing-artwork placeholder.
- Localized the newly added player, playlist, library, statistics, track-action, lyrics, timer, theme, audio, HUD, cache, file-dialog, and shared-listening UI through the selected Music Player language.
- Fixed the in-world legacy HUD crashing Minecraft 1.21.8 when rendering mini-player control labels through the changed `GuiGraphics.drawString` binary signature.
- Fixed scaled mini-player title/artist clipping, restored texture icons for HUD controls, and made the complete HUD panel consistently translucent on every target.
- Extended the player volume range from 0–100% to 0–200%.
- Fixed player, audio-output, dependency-loader, playlist-executor, and shutdown lifecycle handling so Minecraft can exit cleanly while music is active.
- Fixed queue transitions, null queues, cross-thread visibility, bounded source retries, audio-device recovery, and playlist edge cases.
- Added atomic settings, playlist, library, theme, and preset persistence with rotating recovery backups.
- Added listening history, favorites, ratings, statistics, smart library views, transient play-next queues, playlist filtering, drag ordering, and JSON/M3U import/export.
- Added artwork previews and overrides, a full Now Playing screen, draggable/resizable mini-player controls, sleep timer, local client commands, track radio, and track-card/QR export.
- Added parametric and graphic equalizers, channel routing, audio ducking, transition fades, A/B repeat, per-track speed/pitch, and bounded visualizer styles.
- Added local/LRCLIB lyrics, optional Discord Rich Presence, importable themes, dynamic artwork colors, cache controls, and track notifications.
- Made Mod Menu a required client dependency and kept persistent advanced configuration out of the ordinary playback UI.
- Added explicit-consent direct/LAN listening plus an optional disabled-by-default dedicated-server metadata relay.
- Consolidated duplicated common domain sources and exposed all bundled translations with corrected Japanese and Chinese locale codes.

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
