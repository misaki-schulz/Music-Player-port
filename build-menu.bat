@echo off
setlocal
chcp 65001 >nul
where py >nul 2>nul
if %errorlevel%==0 (
    py -3 -B "%~dp0ports\release_cli.py" %*
) else (
    python -B "%~dp0ports\release_cli.py" %*
)
set "build_result=%errorlevel%"
if "%~1"=="" if not "%build_result%"=="0" pause
exit /b %build_result%
