@echo off
title RuneCartel's HQ - Auto Update & Restart
echo ============================================
echo    RuneCartel's HQ - Full Auto Update
echo ============================================
echo.

:: Wait a few seconds for the old server process to fully exit
timeout /t 5 /nobreak >nul

:: Use the full update script from parent directory
cd /d "%~dp0.."
if exist "UPDATE-ALL.bat" (
    echo Launching full system update...
    call "UPDATE-ALL.bat"
) else (
    echo UPDATE-ALL.bat not found, doing server-only update...

    :: Rebuild server only
    echo [1/2] Compiling server...
    cd /d "%~dp0"
    call gradlew.bat shadowJar --no-daemon
    if %ERRORLEVEL% NEQ 0 (
        echo.
        echo ============================================
        echo   BUILD FAILED - Restarting old jar instead
        echo ============================================
        echo.
    )
    echo    Build complete!
    echo.

    :: Restart
    echo [2/2] Starting server...
    start "RuneCartel Server" cmd /k "C:\Program Files\Java\jdk-19\bin\java.exe" -XX:-OmitStackTraceInFastThrow --enable-preview -XX:+UseZGC -Xmx8g -Xms4g --add-opens java.base/java.lang=ALL-UNNAMED -jar build\libs\tarnish-server-all.jar

    echo.
    echo ============================================
    echo   Server updated and restarted!
    echo ============================================
)

:: Close this window after a short delay
timeout /t 3 /nobreak >nul
exit
