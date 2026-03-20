@echo off
title RuneCartel Cache Update Publisher
echo ============================================
echo   RuneCartel CACHE-ONLY Update Publisher
echo ============================================
echo.

:: Refresh PATH for gh CLI
set PATH=%PATH%;C:\Program Files\GitHub CLI

:: Check gh is available and authenticated
gh auth status >nul 2>&1
if errorlevel 1 (
    echo You need to log into GitHub first. Run SETUP-GITHUB.bat first.
    pause
    exit /b 1
)

cd /d "%~dp0"

:: ============================================
:: STEP 1: Package the cache
:: ============================================
echo [1/3] Packaging cache from game data...

:: Re-create cache.zip from the game's cache directory
if exist "update-staging\cache.zip" del "update-staging\cache.zip"
if not exist "update-staging" mkdir "update-staging"

powershell -Command "Compress-Archive -Path 'Tarnish\tarnish-game\data\cache\*' -DestinationPath 'update-staging\cache.zip' -Force"
if errorlevel 1 (
    echo ERROR: Failed to package cache!
    pause
    exit /b 1
)

:: Also update the launcher's embedded cache
echo    Updating embedded cache in launcher...
copy "update-staging\cache.zip" "Tarnish\launcher\src\main\resources\embedded\cache.zip" >nul 2>&1

:: ============================================
:: STEP 2: Read and increment cache version
:: ============================================
echo [2/3] Determining version numbers...

set /a CLIENT_VER=2
set /a CACHE_VER=1

:: Read current remote versions
for /f "tokens=1,2 delims==" %%A in ('gh release download latest -R tristan9988/runecartel-updates -p version.properties -O - 2^>nul') do (
    if "%%A"=="client.version" set /a CLIENT_VER=%%B
    if "%%A"=="cache.version" set /a CACHE_VER=%%B
)

:: Auto-increment cache version
set /a CACHE_VER=%CACHE_VER%+1

echo    Client version (unchanged): %CLIENT_VER%
echo    New cache version: %CACHE_VER%
echo.

:: Write version.properties
echo client.version=%CLIENT_VER%> "update-staging\version.properties"
echo cache.version=%CACHE_VER%>> "update-staging\version.properties"

:: ============================================
:: STEP 3: Upload to GitHub
:: ============================================
echo [3/3] Publishing cache update to GitHub...

:: Download the existing client.zip BEFORE deleting the release
if exist "update-staging\client.zip" del "update-staging\client.zip"
gh release download latest -R tristan9988/runecartel-updates -p client.zip -D update-staging >nul 2>&1
if not exist "update-staging\client.zip" (
    echo WARNING: Could not download existing client.zip, using embedded...
    copy "Tarnish\launcher\src\main\resources\embedded\client.zip" "update-staging\client.zip" >nul 2>&1
)

:: Now delete and recreate release
gh release delete latest -R tristan9988/runecartel-updates --yes >nul 2>&1
gh api -X DELETE repos/tristan9988/runecartel-updates/git/refs/tags/latest >nul 2>&1

gh release create latest ^
    "update-staging\client.zip" ^
    "update-staging\cache.zip" ^
    "update-staging\version.properties" ^
    -R tristan9988/runecartel-updates ^
    --title "Latest Update - Client v%CLIENT_VER% / Cache v%CACHE_VER%" ^
    --notes "Cache update. Client v%CLIENT_VER%, Cache v%CACHE_VER%." ^
    --latest

if errorlevel 1 (
    echo ERROR: Failed to publish release!
    pause
    exit /b 1
)

:: Update local cache too
echo Updating your local cache...
powershell -Command "Expand-Archive -Path 'update-staging\cache.zip' -DestinationPath '%USERPROFILE%\.runecartel\cache' -Force"

echo.
echo ============================================
echo   CACHE UPDATE PUBLISHED!
echo ============================================
echo.
echo   Cache version: %CACHE_VER%
echo   Players will get this next launcher start.
echo.
pause

