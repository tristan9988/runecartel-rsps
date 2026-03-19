@echo off
title RuneCartel Server
echo ============================================
echo   RuneCartel's HQ - Game Server
echo ============================================
echo.

:: Add firewall rule if not exists
netsh advfirewall firewall show rule name="RuneCartel Server" >nul 2>&1
if errorlevel 1 (
    echo Adding firewall rule for port 43594...
    netsh advfirewall firewall add rule name="RuneCartel Server" dir=in action=allow protocol=TCP localport=43594 >nul 2>&1
    netsh advfirewall firewall add rule name="RuneCartel Server" dir=out action=allow protocol=TCP localport=43594 >nul 2>&1
    echo Firewall rule added!
    echo.
)

cd /d "%~dp0Tarnish\tarnish-game"

:: Build if JAR doesn't exist
if not exist "build\libs\tarnish-server-all.jar" (
    echo Server not built yet, building...
    call gradlew.bat shadowJar --no-daemon
    echo.
)

echo Starting server on 0.0.0.0:43594 (accessible from your IP: 90.217.81.162)
echo Friends can connect using the launcher!
echo.
echo Press Ctrl+C to stop the server.
echo ============================================
echo.
"C:\Program Files\Java\jdk-19\bin\java.exe" -XX:-OmitStackTraceInFastThrow --enable-preview -XX:+UseZGC -Xmx8g -Xms4g --add-opens java.base/java.lang=ALL-UNNAMED -jar build\libs\tarnish-server-all.jar
pause

