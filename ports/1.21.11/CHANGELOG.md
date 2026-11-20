# Changelog

## port.10.2 — 2026-10-10

- Added Forge profiles for the selected Minecraft versions.
- Updated the bundled YouTube cipher and poToken services, including the Windows and IPv6 fixes.
- Updated Lavaplayer and LavaSrc for SoundCloud AAC playback. SoundCloud authorization, saved-track refresh, and format fallback remain in place.
- Stopped the audio thread cleanly when the client closes.
- Added an offline SoundCloud regression using a local HTTP fixture.

## port.10 — 2026-10-01

- Added search by playlist name and by title of saved tracks.
- Search ignores case and surrounding spaces.
- Clearing a search restores duplicate and unavailable entries.
- Filtering leaves playlist order, track order, and the playback queue unchanged.
- Text fields no longer trigger music hotkeys while typing.
- Added search labels and empty-result messages in all nine bundled languages.

Build, packaging, and offline regression checks passed. Minecraft runtime behavior and screen appearance were not tested manually.

Release: [port.10](https://github.com/misaki-schulz/Music-Player-port/releases/tag/port.10)

## port.9 — 2026-10-01

- Added playlist sorting by title, artist, and file name or URI in both directions.
- Sorting is case-insensitive and compares numbers naturally, so `Track 2` comes before `Track 10`.
- Saved order, duplicate entries, unavailable entries, and nested online playlists are preserved.
- Folder imports keep file-name order while metadata loads asynchronously.
- Fixed saved YouTube links after restarting Minecraft.
- Added a local mirror for the lavadsp and native-loader versions used by this release.

Release: [port.9](https://github.com/misaki-schulz/Music-Player-port/releases/tag/port.9)

## port.8

- Registered YouTube before the generic HTTP source so saved links work after a restart.
- Reauthorization updates the existing source.
- Saved playlist and token formats remain compatible.

## 2.7.1.351+mc26.2.port.1

- Ported the Fabric client to Minecraft 26.2 and Java 25.
- Updated the GUI, rendering, input, audio, localization, and resource APIs.
- Added the required adapted U Team Core classes.

## 2.7.1.351+mc26.2.port.2

- Added YouTube sign-in through the browser with a one-time code.
- Saved the refresh token between launches.
- Added a sign-in button to the settings screen.

## 2.7.1.351+mc26.2.port.3

- Updated the YouTube library with the TV OAuth User-Agent fix.

## 2.7.1.351+mc26.2.port.4

- Added a configurable external service for YouTube signature decoding.

## 2.7.1.351+mc26.2.port.5

- Fixed saved YouTube tracks after reopening Minecraft.
- Extended the volume range to 200.

## 2.7.1.351+mc26.2.port.6

- Added a ten-band equalizer and bass boost.
- Added selection for all bundled languages.

## 2.7.1.351+mc26.2.port.7

- Stopped audio and background workers when Minecraft closes.
