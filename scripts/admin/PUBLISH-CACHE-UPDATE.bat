@echo off
setlocal
set "ROOT=%~dp0..\.."
cd /d "%ROOT%"
powershell -ExecutionPolicy Bypass -File "%ROOT%\publish-release.ps1" -Mode Cache -SkipStartServer
exit /b %ERRORLEVEL%
