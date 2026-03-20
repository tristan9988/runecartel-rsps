@echo off
setlocal
cd /d "%~dp0"
set PATH=%PATH%;C:\Program Files\GitHub CLI

echo ============================================
echo   RuneCartel GitHub Repository Setup
echo ============================================
echo.

gh auth status >nul 2>&1
if errorlevel 1 (
    echo Logging into GitHub...
    gh auth login -p https -h github.com -w
    if errorlevel 1 (
        echo ERROR: GitHub login failed.
        exit /b 1
    )
)

echo.
echo Ensuring source repository exists...
gh repo view tristan9988/runecartel-rsps >nul 2>&1
if errorlevel 1 (
    gh repo create tristan9988/runecartel-rsps --private --source=. --remote=origin --push
    if errorlevel 1 (
        echo ERROR: Failed to create or push the source repository.
        exit /b 1
    )
) else (
    git remote get-url origin >nul 2>&1
    if errorlevel 1 git remote add origin https://github.com/tristan9988/runecartel-rsps.git
)

echo.
echo Ensuring updates repository exists...
gh repo view tristan9988/runecartel-updates >nul 2>&1
if errorlevel 1 (
    gh repo create tristan9988/runecartel-updates --public --description "RuneCartel game update files"
    if errorlevel 1 (
        echo ERROR: Failed to create the updates repository.
        exit /b 1
    )
)

echo.
echo Publishing the initial full release assets...
powershell -ExecutionPolicy Bypass -File "%~dp0publish-release.ps1" -Mode All -SkipStartServer -SkipPush
exit /b %ERRORLEVEL%
