@echo off
setlocal
set "ROOT=%~dp0..\.."
cd /d "%ROOT%"
powershell -ExecutionPolicy Bypass -File "%ROOT%\publish-release.ps1" -Mode Launcher -SkipStartServer
exit /b %ERRORLEVEL%
