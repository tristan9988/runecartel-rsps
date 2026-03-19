@echo off
title Tarnish Client
echo Starting Tarnish Client (connecting to 127.0.0.1:43594)...
echo.
"C:\Program Files\Eclipse Adoptium\jdk-11.0.22.7-hotspot\bin\java.exe" -XX:-OmitStackTraceInFastThrow -Xmx2g -Xms1g -jar build\libs\Tarnish.jar
pause
