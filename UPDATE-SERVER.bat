@echo off
title RuneCartel's HQ - Update Server
echo ============================================
echo       RuneCartel's HQ Server Updater
echo ============================================
echo.

:: Kill the running server
echo [1/3] Stopping running server...
taskkill /FI "WINDOWTITLE eq RuneCartel Server" /F >nul 2>&1
for /f "tokens=1" %%p in ('wmic process where "commandline like '%%tarnish-server-all%%'" get processid 2^>nul ^| findstr /r "[0-9]"') do taskkill /PID %%p /F >nul 2>&1
timeout /t 2 /nobreak >nul
echo    Done.
echo.

:: Rebuild
echo [2/3] Compiling server...
cd /d "%~dp0Tarnish\tarnish-game"
call gradlew.bat shadowJar
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo ============================================
    echo   BUILD FAILED - fix errors and try again
    echo ============================================
    pause
    exit /b 1
)
echo.
echo    Build successful!
echo.

:: Restart
echo [3/3] Starting server...
start "RuneCartel Server" cmd /k "C:\Program Files\Java\jdk-19\bin\java.exe" -XX:-OmitStackTraceInFastThrow --enable-preview -XX:+UseZGC -Xmx8g -Xms4g --add-opens java.base/java.lang=ALL-UNNAMED -jar build\libs\tarnish-server-all.jar

echo.
echo ============================================
echo   Server updated and restarted!
echo   Players can log out and back in to get
echo   the latest changes.
echo ============================================
pause
