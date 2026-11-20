# Music Player — матрица port.11

В репозитории оставлены только Minecraft 1.20.1, 1.21.1, 1.21.11 и все версии 26.x.
Fabric, Forge и NeoForge используют точные профили из соответствующих матриц.
После сборки JAR размещаются в `dist/port.11/`, а точные Fabric-сборки — в
`dist/port.11/exact/`.

## Fabric

| Основной JAR | Поддерживаемые версии |
|---|---|
| `music_player-fabric-1.20.1-2.7.1.351.port.11.jar` | 1.20.1 |
| `music_player-fabric-1.21.1-2.7.1.351.port.11.jar` | 1.21.1 |
| `music_player-fabric-1.21.11-2.7.1.351.port.11.jar` | 1.21.11 |
| `music_player-fabric-26.1-26.1.2-2.7.1.351.port.11.jar` | 26.1, 26.1.1, 26.1.2 |
| `music_player-fabric-26.2-2.7.1.351.port.11.jar` | 26.2 |
| `music_player-fabric-26.3-2.7.1.351.port.11.jar` | 26.3 |

Матрица точных версий и Fabric API находится в [`targets.json`](targets.json).

## Сборка

Из корня проекта используйте `build-menu.bat` в Windows или `sh build-menu.sh` в Linux/macOS.
Для командной строки доступны следующие варианты:

```bash
python ports/release_cli.py --list
python ports/release_cli.py --all
python ports/release_cli.py --everything
python ports/release_cli.py --group 1.21.11
python ports/loader_cli.py --list
python ports/loader_cli.py --all
```

Для отдельного профиля Fabric можно запустить `./gradlew build` внутри каталога профиля.
Для Forge и NeoForge используются `Build-Target.ps1` и `loader_cli.py`.
Сборки идут последовательно, Minecraft не запускается.

## Проверки

```bash
python ports/verify_release.py
python ports/test_sorting.py 1.20.1
python ports/test_audio_routing.py 1.21.1
python ports/test_soundcloud.py 26.2
```

Скрипты упаковки проверяют точную зависимость от Minecraft, метаданные загрузчика,
ресурсы, лицензии, байткод и вложенные аудиобиблиотеки.
