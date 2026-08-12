# Music Player server companion

This optional Fabric server mod relays synchronized track URI, position, pause state, and an optional title. It never uploads or relays audio or artwork. The normal client player does not require this artifact.

On its first launch it creates `config/musicplayer-server.json` with `enabled: false`. An administrator must review the bind address, port, room/client limits, and permitted URI schemes, then explicitly enable it and restart the server. Binding to `0.0.0.0` exposes the selected TCP port and may require a firewall or router rule.

Players then use **Mod Menu → Music Player → Services → Shared listening**, enter the server address and configured port, and choose either **Broadcast via server relay** or **Join server relay** with the same eight-character room code.
