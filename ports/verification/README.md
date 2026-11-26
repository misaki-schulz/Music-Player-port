# Проверка port.11.1

Из корня репозитория запустите `python ports/verify_release.py`. Скрипт собирает профили из [матрицы Fabric](../targets.json) и [матрицы Forge и NeoForge](../loader-targets.json), запускает тесты без Minecraft и проверяет упаковку JAR.

Логи и `results.json` появляются в `.cache/port.11.1-upstream-2.7.4.384-verification/`. Чтобы продолжить прерванную проверку с сохранённых результатов, добавьте `--resume`.

Отдельные тесты можно запустить после сборки указанного профиля.

```bash
python ports/test_sorting.py 1.20.1
python ports/test_audio_routing.py 1.21.1
python ports/test_soundcloud.py 26.2
```

Для проверки в запущенной игре используйте [сценарий для клиента Minecraft](../client-gui/README.md).

Проверки локальных сервисов YouTube и конкретной ошибки EJS без Minecraft
описаны в [youtube-services/README.md](../youtube-services/README.md).
Они не запускают сборку мода.
