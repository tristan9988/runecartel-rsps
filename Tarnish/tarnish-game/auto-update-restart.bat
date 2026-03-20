@echo off
title RuneCartel's HQ - Auto Update & Restart
echo ============================================
echo    RuneCartel's HQ - Full Auto Update
echo    Triggered by ::update in-game command
echo ============================================
echo.

:: Wait for the old server process to fully exit
echo Waiting for server to shut down...
timeout /t 5 /nobreak >nul
echo.

:: Navigate to project root (two levels up from tarnish-game)
:: tarnish-game is at: Tarnish\tarnish-game
:: Project root is at: ..\..\
cd /d "%~dp0..\.."

echo Working directory: %CD%
echo.

:: Look for UPDATE-ALL.bat in the project root
if exist "UPDATE-ALL.bat" (
    echo Found UPDATE-ALL.bat - launching full system update...
    echo This will: Build client + cache, publish to GitHub, rebuild server, restart.
    echo.
    call "UPDATE-ALL.bat"
) else (
    echo UPDATE-ALL.bat not found at %CD%
    echo Falling back to server-only rebuild...
    echo.

    :: Rebuild server only
    echo [1/2] Compiling server...
    cd /d "%~dp0"
    call gradlew.bat shadowJar --no-daemon
    if %ERRORLEVEL% NEQ 0 (
        echo.
        echo BUILD FAILED - Restarting old jar instead
        echo.
    ) else (
        echo    Build complete!
        echo.
    )

    :: Restart
    echo [2/2] Starting server...
    start "RuneCartel Server" cmd /k "C:\Program Files\Java\jdk-19\bin\java.exe" -XX:-OmitStackTraceInFastThrow --enable-preview -XX:+UseZGC -Xmx8g -Xms4g --add-opens java.base/java.lang=ALL-UNNAMED -jar build\libs\tarnish-server-all.jar

    echo.
    echo Server restarted! (server-only, no client/cache publish)
)

:: Close this window after a short delay
echo.
echo Auto-update process complete. This window will close in 5 seconds.
timeout /t 5 /nobreak >nul
exit
