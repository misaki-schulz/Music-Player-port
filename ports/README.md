# Сборки Music Player port.11.1

Выберите JAR для своей версии Minecraft и загрузчика. На одной установке достаточно одного JAR Music Player. Для Forge и NeoForge см. [версии загрузчиков](LOADERS.md).

## Сборка на GitHub

Откройте [Build all ports](https://github.com/misaki-schulz/Music-Player-port/actions/workflows/build.yml) и нажмите **Run workflow**. GitHub соберёт все 24 сочетания Minecraft и загрузчика в 18 группах JAR. Готовые JAR и SHA-256 доступны в артефактах запуска семь дней. Сборка выполняется на серверах GitHub; Minecraft не запускается.

## Fabric

Для сборок из этой таблицы нужен Fabric Loader 0.19.5 или новее. Установите Fabric API для своей версии игры.

| Minecraft | Java | Fabric API для профиля | Группа JAR |
|---|---|---|---|
| 1.20.1 | 17 | `0.92.12+1.20.1` | `1.20.1` |
| 1.21.1 | 21 | `0.116.17+1.21.1` | `1.21.1` |
| 1.21.11 | 21 | `0.141.6+1.21.11` | `1.21.11` |
| 26.1 | 25 | `0.145.1+26.1` | `26.1-26.1.2` |
| 26.1.1 | 25 | `0.145.4+26.1.1` | `26.1-26.1.2` |
| 26.1.2 | 25 | `0.155.3+26.1.2` | `26.1-26.1.2` |
| 26.2 | 25 | `0.156.0+26.2` | `26.2` |
| 26.3 | 25 | `0.161.0+26.3` | `26.3` |

Список профилей для скриптов хранится в [targets.json](targets.json).

## Сборка из корня репозитория

Откройте меню через `build-menu.bat` в Windows или `sh build-menu.sh` в Linux и macOS.

| Команда | Результат |
|---|---|
| `python ports/release_cli.py --list` | Показать группы Fabric |
| `python ports/release_cli.py --group 1.21.11` | Собрать один JAR Fabric для 1.21.11 |
| `python ports/release_cli.py --all` | Собрать все группы Fabric |
| `python ports/loader_cli.py --all` | Собрать Forge и NeoForge |
| `python ports/release_cli.py --everything` | Собрать все три загрузчика |

Готовые архивы имеют имена вида `music_player-fabric-1.21.11-2.7.4.384.port.11.1.jar` и лежат в `dist/port.11.1/`. В `dist/port.11.1/exact/` лежат Fabric JAR для отдельных версий Minecraft. Для 26.1, 26.1.1 и 26.1.2 также собирается один общий Fabric JAR.

Команды для проверки сборок и запуска тестов описаны в [verification/README.md](verification/README.md).
