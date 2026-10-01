# Changelog

## port.10 — 2026-10-01 — search saved songs and playlists

Supports Minecraft 1.21.2 through 26.3: 15 exact build targets, packaged into eight release JARs.

### Added

- Live search by playlist name on the main player screen.
- Live search by title for songs already added to a playlist, including individual songs inside provider playlists.
- Case-insensitive substring matching that ignores surrounding spaces in the query.
- Search hints and empty-result messages in all nine bundled languages.

### Behavior and fixes

- Clearing a search restores the complete list, including duplicate and unavailable entries.
- Filtering leaves saved playlists, track order, and the playback queue unchanged.
- Search queries remain after resizing the window or returning from the sorting screen.
- Focused text fields no longer trigger the music menu or playback hotkeys while typing.

### Validation

- All 15 Minecraft targets compiled successfully; the eight grouped JARs passed bytecode, dependency, and SHA-256 checks, with matching code and assets within each shared version group.
- Offline regressions passed for the production song and playlist lists, nested songs, duplicates, loading, empty results, clearing, serialization, playback, sorting, callback ordering, and YouTube source routing.
- The GitHub build passed. Minecraft runtime behavior and screen appearance have not been manually tested.

Release: [port.10](https://github.com/misaki-schulz/Music-Player-port/releases/tag/port.10)

## port.9 — 2026-10-01 — playlist sorting and ordered folder imports

Supports all Minecraft targets from 1.21.2 through 26.3.

- Added six playlist sort options: title, artist, and file name / URI, each ascending or descending.
- Used stable, case-insensitive natural ordering so Track 2 precedes Track 10.
- Saved the sorted order without changing the playlist file format or interrupting the current track.
- Retained duplicate and unavailable entries; nested provider playlists remain grouped.
- Sorted folder imports by file name and preserved that order across asynchronous metadata callbacks.
- Disabled Add all until loading completes; ignored results from replaced searches and closed screens.
- Added sorting labels in all nine languages and offline regressions for saved reloads, playback, sorting, and callback ordering.
- Included the port.8 fix for saved YouTube links after restarting Minecraft.
- Fixed clean builds after the upstream Maven server stopped serving lavadsp 0.7.8 and native-loader 0.0.1: preserved the exact release artifacts, metadata, hashes, and licenses in the local vendor repository used by every source profile.
- Verified the preserved dependency JARs against the published port.9 JARs and tested dependency resolution with an empty Gradle cache. Audio implementation and dependency versions did not change.

Release: [port.9](https://github.com/misaki-schulz/Music-Player-port/releases/tag/port.9)

## port.8 — all Minecraft targets 1.21.2 through 26.3

- Fixed saved YouTube URLs failing with `Unknown file format.` after restarting or changing Minecraft versions: register YouTube before generic HTTP at startup.
- OAuth restoration now updates the existing source instead of appending a late source manager.
- Preserved playlist and token storage formats; existing playlists do not need to be recreated.
- Added an offline source-routing regression for restart, OAuth retry, saved links/playlists, search, and HTTP fallback. Minecraft runtime testing is left to the user.

## 2.7.1.351+mc26.2.port.1

- Ported the Fabric client to Minecraft 26.2 and Java 25.
- Updated GUI, rendering, input, audio, localization, and resource APIs.
- Replaced the external U Team Core runtime dependency with the required adapted classes.
- Kept Fabric API as a required dependency.

## 2.7.1.351+mc26.2.port.2

- Added YouTube sign-in through the system browser with a device code shown in Minecraft.
- Added a startup choice to sign in or skip YouTube for the current session.
- Saved the YouTube refresh token for reuse after restarting Minecraft.
- Added a YouTube sign-in control to Music Player settings and localized the new prompts.

## 2.7.1.351+mc26.2.port.3

- Updated the bundled YouTube source library to a snapshot with the TV OAuth User-Agent fix, addressing stream requests that returned "The page needs to be reloaded".

## 2.7.1.351+mc26.2.port.4

- Added a configurable remote cipher fallback for YouTube player signatures when the local parser cannot decode the current player script.

## 2.7.1.351+mc26.2.port.5

- Fixed saved YouTube tracks becoming "Unknown file format" after reopening Minecraft.
- Expanded the volume control from 0–100 to 0–200, with levels above 100 increasing output gain.

## 2.7.1.351+mc26.2.port.6

- Added a ten-band equalizer and a separate bass boost switch, with settings preserved between launches.
- Made every bundled language selectable.
- Changed the initial Google account sign-in prompt to English.

## 2.7.1.351+mc26.2.port.7

- Fixed Minecraft staying open after its window closed by stopping the mod's audio and playlist workers during client shutdown.
