# Pinned audio dependency mirror

This directory preserves the exact `dev.arbjerg:lavadsp:0.7.8` and
`dev.arbjerg:native-loader:0.0.1` JARs, POMs, and Gradle module metadata used
by port.9. The upstream Maven URLs currently return HTTP 404 for these
versions. The JARs match their published module hashes and the dependencies
already packed in the port.9 release; no audio implementation or version changes.

Every source profile uses this local Maven mirror only for these two modules.
All other dependencies continue to resolve from their existing repositories.
`SHA256SUMS.txt` covers the preserved artifacts and licenses.

Upstream sources and licenses:
- https://github.com/lavalink-devs/lavadsp
- https://github.com/lavalink-devs/native-loader

Keep `vendor/` at the repository root when building a profile under `ports/`.
