@echo off
setlocal
chcp 65001 >nul
where py >nul 2>nul
if %errorlevel%==0 (
    py -3 "%~dp0ports\release_cli.py" %*
) else (
    python "%~dp0ports\release_cli.py" %*
)
exit /b %errorlevel%
