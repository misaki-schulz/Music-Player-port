# Forge и NeoForge — port.11

Оставлены варианты для Minecraft 1.20.1, 1.21.1, 1.21.11 и всех версий 26.x.
В одну установку Minecraft ставьте один JAR Music Player, соответствующий загрузчику
и версии игры. Forge и NeoForge не требуют Fabric API.

| Minecraft | Forge | NeoForge | Java |
|---|---|---|---|
| 1.20.1 | 47.4.26 | 47.1.106 | 17 |
| 1.21.1 | 52.1.16 | 21.1.256 | 21 |
| 1.21.11 | 61.2.1 | 21.11.45 | 21 |
| 26.1 | 62.0.9 | 26.1.0.19-beta | 25 |
| 26.1.1 | 63.0.2 | 26.1.1.15-beta | 25 |
| 26.1.2 | 64.1.3 | 26.1.2.114 | 25 |
| 26.2 | 65.1.3 | 26.2.0.88 | 25 |
| 26.3 | 66.0.10 | 26.3.0.57-beta | 25 |

Версии загрузчиков и точные группы находятся в [`loader-targets.json`](loader-targets.json).
Суффикс `beta` обозначает предварительный выпуск NeoForge.

## Сборка

```bash
python ports/loader_cli.py --list
python ports/loader_cli.py --loader forge --version 1.20.1
python ports/loader_cli.py --loader neoforge --version 26.2
python ports/loader_cli.py --all
```

Сборки выполняются последовательно и после каждой проверяется JAR. Результаты и
контрольные суммы находятся в `dist/port.11/loader-manifest.json` и
`dist/port.11/LOADER-SHA256SUMS`.
Для компиляции нужны JDK 17, 21 и 25. Windows-скрипт ищет их в `JAVA_HOME`,
`%ProgramFiles%/Java` и `%USERPROFILE%/.jdks`.

`package_loaders.py` проверяет метаданные, точную версию Minecraft, отсутствие Fabric,
байткод Java, вложенные аудиобиблиотеки, лицензии и маршрутизацию YouTube.
