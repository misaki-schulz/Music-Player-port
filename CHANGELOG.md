# Changelog

## port.10 — search saved songs and playlists for all supported targets

- Added a title filter for songs already in a playlist, including individual tracks inside provider playlists.
- Added a name filter to the main playlist list with translated hints and empty-result messages in all nine languages.
- Kept filters when resizing or returning from sorting; clearing a filter restores the complete list.
- Protected focused text fields from music menu and playback hotkeys while typing.
- Filtering only changes the visible list; saved tracks, their order, and the playback queue stay unchanged.
- Preserved port.9 sorting and ordered folder imports, and the port.8 YouTube routing fix.

## port.9 — playlist sorting for all Minecraft targets 1.21.2 through 26.3

- Added a playlist sorting screen with title, artist, and file name / URI in both directions.
- Used stable, case-insensitive natural ordering so Track 2 precedes Track 10.
- Saved the sorted URI order without changing the playlist file format or interrupting the current track.
- Kept duplicate and unavailable entries; nested provider playlists remain grouped.
- Sorted folder imports by file name and preserved that order across asynchronous metadata callbacks.
- Disabled Add all until loading completes and ignored results from replaced searches or closed screens.
- Added labels for all nine languages and offline regressions for sorting, saved reloads, playback and callback ordering.
- Preserved the port.8 YouTube routing fix. Minecraft runtime and screen appearance need manual testing.

- Build repair: preserved the exact lavadsp and native-loader release artifacts locally after upstream Maven started returning 404; audio code and versions are unchanged.

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
