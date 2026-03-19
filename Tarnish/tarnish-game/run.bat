@echo off
title Tarnish Server
"C:\Program Files\Java\jdk-19\bin\java.exe" -XX:-OmitStackTraceInFastThrow --enable-preview -XX:+UseZGC -Xmx8g -Xms4g --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/jdk.internal.misc=ALL-UNNAMED -jar build\libs\tarnish-server-all.jar
pause