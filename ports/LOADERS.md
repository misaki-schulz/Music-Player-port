# Forge и NeoForge для Music Player port.11.1

Выберите JAR Music Player для указанной версии Minecraft и своего загрузчика. Эти сборки не требуют Fabric API.

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

Суффикс `beta` обозначает предварительный выпуск NeoForge. Профили хранятся в [loader-targets.json](loader-targets.json).

## Сборка из корня репозитория

```bash
python ports/loader_cli.py --list
python ports/loader_cli.py --loader forge --version 1.20.1
python ports/loader_cli.py --loader neoforge --version 26.2
python ports/loader_cli.py --all
```

Скрипт собирает профили по очереди и проверяет JAR перед упаковкой. Архивы, `loader-manifest.json` и контрольные суммы `LOADER-SHA256SUMS` появляются в `dist/port.11.1/`.

Для сборки всей матрицы нужны JDK 17, 21 и 25. В Windows скрипт ищет их в `JAVA_HOME`, `%ProgramFiles%/Java` и `%USERPROFILE%/.jdks`.
