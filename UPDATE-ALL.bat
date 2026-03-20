@echo off
setlocal
cd /d "%~dp0"
powershell -ExecutionPolicy Bypass -File "%~dp0publish-release.ps1" -Mode All
exit /b %ERRORLEVEL%
