# YouTube cipher revisions

The official youtube-source-services 1.0.4 artifact still installs
yt-cipher `1e1fd8e` and EJS `cd4e87f`. That EJS fails on recent
YouTube players with `a.addOnDisposeCallback is not a function`.
Updating only the Java library version does not fix the player parser.

`../youtube-services.gradle` replaces the library's own
`net/hycrafthd/youtube_source_services/services.properties` inside its packed
dependency JAR. All seven audio profiles apply the same script. The release
packagers verify the configuration bytes and the compiled service manager.
This avoids relying on resource lookup order in DependencyClassLoader.

The pinned pair in this directory is:

- [yt-cipher 91d55a24](https://github.com/kikkia/yt-cipher/commit/91d55a24fafd83fa85f72dfe6914626b05f799f6)
- [EJS c47fb1b5](https://github.com/yt-dlp/ejs/commit/c47fb1b54215afa4f6a3d557ad65bb724022f6ba),
  matching that cipher revision's Dockerfile.

EJS includes [40b7a7d4](https://github.com/yt-dlp/ejs/commit/40b7a7d4aee5),
which preserves the prototype helper calls required by player 92eef845.
The cipher revision also supports the new `tcl` player variants.
The service uses the cipher commit's first seven characters as its installation
directory, so the new configuration installs into `yt-cipher/91d55a2`;
an existing `yt-cipher/1e1fd8e/.installed` marker cannot suppress the update.
The Java service version 1.0.4 also repairs damaged installations.

The service manager starts services in the background. Until they are ready,
or for 30 seconds after a local cipher HTTP failure, it uses the external
cipher with a separate HTTP session. Explicit JVM cipher settings continue to
select the configured service. The fallback is still subject to external
service availability.

## Checks without Minecraft

`python ports/test_youtube_services.py --artifact /absolute/path/to/mod.jar --slf4j /absolute/path/to/slf4j-api.jar --group 1.21.11`
compiles only the source manager and test harness with the selected JDK.
It uses local HTTP fixtures and an executable Python fixture in place of Deno.
It checks startup, warm-up, local HTTP 500 fallback, retry cooldown, isolated
fallback headers and explicit overrides. It requires Linux/macOS and Python 3
on PATH. It never invokes Gradle or launches Minecraft.

`ports/tests/youtube-cipher-check.ts` checks the actual EJS parser with
the known n-signature vector for YouTube player 92eef845 (IAS).
Pass an EJS directory as a file URL ending in a slash and a saved copy of
`https://www.youtube.com/s/player/92eef845/player_ias.vflset/en_US/base.js`.
Use a Deno import map with the versions of astring and meriyah from that EJS
revision's package.json. Pass `old` as the third argument to verify the failure
with the previous EJS revision. This test parses JavaScript and requests no
audio.
