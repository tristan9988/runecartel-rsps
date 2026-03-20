@echo off
title RuneCartel - GitHub Setup
echo ============================================
echo   RuneCartel GitHub Repository Setup
echo ============================================
echo.

:: Refresh PATH to pick up gh
set PATH=%PATH%;C:\Program Files\GitHub CLI

:: Check if already authenticated
gh auth status >nul 2>&1
if errorlevel 1 (
    echo You need to log into GitHub first.
    echo A browser window will open - log in with your RuneCartelHQ account.
    echo.
    gh auth login -p https -h github.com -w
    if errorlevel 1 (
        echo.
        echo ERROR: GitHub login failed!
        pause
        exit /b 1
    )
)

echo.
echo Logged in! Setting up repositories...
echo.

cd /d "%~dp0"

:: ============================================
:: REPO 1: Source code (private)
:: ============================================
echo [1/2] Creating source code repository (private)...
gh repo create tristan9988/runecartel-rsps --private --source=. --remote=origin --push 2>nul
if errorlevel 1 (
    echo    Repository may already exist, trying to set remote and push...
    git remote remove origin 2>nul
    git remote add origin https://github.com/tristan9988/runecartel-rsps.git
    git branch -M main
    git push -u origin main
)
echo    Source repo ready!
echo.

:: ============================================
:: REPO 2: Updates/releases (public - so launcher can download)
:: ============================================
echo [2/2] Creating updates repository (public)...
gh repo view tristan9988/runecartel-updates >nul 2>&1
if errorlevel 1 (
    :: Create a temp directory to initialize the repo
    mkdir "%TEMP%\runecartel-updates" 2>nul
    cd /d "%TEMP%\runecartel-updates"
    git init >nul 2>&1
    echo # RuneCartel Updates> README.md
    echo Auto-managed update files for the RuneCartel launcher.>> README.md
    git add . >nul 2>&1
    git commit -m "Initial commit" >nul 2>&1
    gh repo create tristan9988/runecartel-updates --public --source=. --remote=origin --push --description "RuneCartel game update files" 2>nul
    cd /d "%~dp0"
    rmdir /s /q "%TEMP%\runecartel-updates" 2>nul
    echo    Updates repo created!
) else (
    echo    Updates repo already exists!
)
echo.

echo ============================================
echo   DONE! Both repositories are set up!
echo ============================================
echo.
echo   Source code:  https://github.com/tristan9988/runecartel-rsps (private)
echo   Game updates: https://github.com/tristan9988/runecartel-updates (public)
echo.

:: ============================================
:: STEP 3: Publish first update release
:: ============================================
echo Now publishing the initial update release so the launcher works...
echo.

cd /d "%~dp0"
if not exist "update-staging" mkdir "update-staging"

:: Package client.zip if the built client exists
if exist "Tarnish\tarnish-client\build\libs\Tarnish.jar" (
    powershell -Command "Compress-Archive -Path 'Tarnish\tarnish-client\build\libs\Tarnish.jar' -DestinationPath 'update-staging\client.zip' -Force"
    echo    Packaged client.zip
) else if exist "Tarnish\launcher\src\main\resources\embedded\client.zip" (
    copy "Tarnish\launcher\src\main\resources\embedded\client.zip" "update-staging\client.zip" >nul
    echo    Using embedded client.zip
)

:: Package cache.zip
if exist "Tarnish\tarnish-game\data\cache\main_file_cache.dat" (
    powershell -Command "Compress-Archive -Path 'Tarnish\tarnish-game\data\cache\*' -DestinationPath 'update-staging\cache.zip' -Force"
    echo    Packaged cache.zip
) else if exist "Tarnish\launcher\src\main\resources\embedded\cache.zip" (
    copy "Tarnish\launcher\src\main\resources\embedded\cache.zip" "update-staging\cache.zip" >nul
    echo    Using embedded cache.zip
)

:: Write initial version
echo client.version=2> "update-staging\version.properties"
echo cache.version=1>> "update-staging\version.properties"

:: Check if a release already exists
gh release view latest -R tristan9988/runecartel-updates >nul 2>&1
if errorlevel 1 (
    echo    Creating initial release on GitHub...
    gh release create latest "update-staging\client.zip" "update-staging\cache.zip" "update-staging\version.properties" -R tristan9988/runecartel-updates --title "Latest Update - Client v2 / Cache v1" --notes "Initial release." --latest
    if errorlevel 1 (
        echo    WARNING: Could not create initial release. Run PUBLISH-UPDATE.bat to do it manually.
    ) else (
        echo    Initial release published!
    )
) else (
    echo    Release already exists, skipping.
)

echo.
echo ============================================
echo   ALL DONE! Everything is connected.
echo ============================================
echo.
echo   HOW IT WORKS:
echo     1. Give players the RuneCartel-Launcher.jar file
echo     2. They double-click it, it auto-downloads client ^& cache
echo     3. When you type ::update in-game, it rebuilds everything
echo        and pushes to GitHub so players get updates automatically
echo.
echo   MANUAL SCRIPTS (optional):
echo     - PUBLISH-UPDATE.bat     = build client ^& push to GitHub
echo     - PUBLISH-CACHE-UPDATE.bat = push cache-only to GitHub
echo     - UPDATE-ALL.bat         = full rebuild (::update calls this)
echo.
pause

