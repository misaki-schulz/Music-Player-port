# Проверка GUI в настоящем Minecraft-клиенте

Сценарий запускает выбранные профили Fabric, Forge и NeoForge из текущих матриц. Результаты последнего сохранённого прогона указаны в [отчёте port.11](../verification/README.md). Файл `last-results.json` относится к предыдущему релизу.

Агент открывает экраны Music Player в Minecraft, выполняет действия мыши и клавиатуры на render thread и сохраняет скриншоты. Проверяются плейлисты, импорт WAV, поиск, управление воспроизведением, сортировка и настройки.

## Ограничения

- Ответ системного диалога выбора файла подменяется тестом. Сам диалог Windows не проверяется.
- Авторизация и доступность сторонних музыкальных сервисов не проверяются.
- Сценарий не оценивает качество звука и поведение игрового мира.
- Production JAR проверяется упаковщиками `package_ports.py` и `package_loaders.py`.

## Запуск в Windows

Нужны Python 3, PowerShell 7, JDK 17, 21 и 25, а также рабочий OpenGL-драйвер. Сначала соберите профиль через `Build-Target.ps1`.

```powershell
python ports/client-gui/build_agent.py
python ports/client-gui/run_matrix.py --loader forge --version 1.20.1
python ports/client-gui/run_matrix.py --loader neoforge --version 1.21.1
python ports/client-gui/run_matrix.py --resume
```

Клиенты запускаются последовательно. `--no-build` использует уже собранный профиль, а `--resume` пропускает записи, совместимые с тем же агентом и исходниками.

Результаты сохраняются в `.cache/client-tests/`. Отчёт матрицы находится в `.cache/client-tests/matrix-results.json`, отдельные логи — в каталогах профилей, скриншоты — в `screenshots/`.
