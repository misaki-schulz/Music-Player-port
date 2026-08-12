@echo off
setlocal
cd /d "%~dp0"

:menu
cls
echo Music Player multi-version builder
echo.
echo   1. Minecraft 1.21.2 - 1.21.8
echo   2. Minecraft 1.21.9 - 1.21.11
echo   3. Minecraft 26.1 - 26.2 (26.x)
echo   4. All supported versions
echo   5. Exit
echo.
choice /c 12345 /n /m "Select a target [1-5]: "

if errorlevel 5 exit /b 0
if errorlevel 4 goto build_all
if errorlevel 3 goto build_26
if errorlevel 2 goto build_legacy_new
if errorlevel 1 goto build_legacy_old

:build_legacy_old
set "target=Minecraft 1.21.2 - 1.21.8"
set "tasks=:legacy-old:build"
set "output=legacy-old\build\libs"
goto run_build

:build_legacy_new
set "target=Minecraft 1.21.9 - 1.21.11"
set "tasks=:legacy-new:build"
set "output=legacy-new\build\libs"
goto run_build

:build_26
set "target=Minecraft 26.1 - 26.2 (26.x)"
set "tasks=:build"
set "output=build\libs"
goto run_build

:build_all
set "target=all supported Minecraft versions"
set "tasks=build"
set "output=legacy-old\build\libs, legacy-new\build\libs, build\libs"

:run_build
echo.
echo Building %target%...
call "%~dp0gradlew.bat" %tasks% --console=plain
set "result=%errorlevel%"
echo.

if not "%result%"=="0" (
	echo BUILD FAILED with exit code %result%.
) else (
	echo BUILD SUCCESSFUL.
	echo Output: %~dp0%output%
)

echo.
pause
goto menu
