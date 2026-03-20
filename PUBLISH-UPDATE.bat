@echo off
title RuneCartel Update Publisher
echo ============================================
echo   RuneCartel Update Publisher (Auto-Deploy)
echo ============================================
echo.

:: Refresh PATH for gh CLI
set PATH=%PATH%;C:\Program Files\GitHub CLI

:: Check gh is available
gh --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: GitHub CLI (gh) not found!
    echo Please install it from https://cli.github.com/
    pause
    exit /b 1
)

:: Check gh is authenticated
gh auth status >nul 2>&1
if errorlevel 1 (
    echo You need to log into GitHub first.
    gh auth login -p https -h github.com -w
    if errorlevel 1 (
        echo ERROR: GitHub login failed!
        pause
        exit /b 1
    )
)

:: ============================================
:: STEP 1: Build the client
:: ============================================
echo [1/6] Building client...
cd /d "%~dp0Tarnish\tarnish-client"
call gradlew.bat shadowJar --no-daemon
if errorlevel 1 (
    echo ERROR: Client build failed!
    pause
    exit /b 1
)

:: ============================================
:: STEP 2: Package client.zip
:: ============================================
echo [2/6] Packaging client...
cd /d "%~dp0"
if exist "update-staging" rmdir /s /q "update-staging"
mkdir "update-staging"
powershell -Command "Compress-Archive -Path 'Tarnish\tarnish-client\build\libs\Tarnish.jar' -DestinationPath 'update-staging\client.zip' -Force"

:: ============================================
:: STEP 3: Package cache.zip
:: ============================================
echo [3/6] Packaging cache...
copy "Tarnish\launcher\src\main\resources\embedded\cache.zip" "update-staging\cache.zip" >nul 2>&1

:: ============================================
:: STEP 4: Read previous versions and auto-increment
:: ============================================
echo [4/6] Determining version numbers...

:: Read current version from staging (if exists) or use defaults
set /a CLIENT_VER=2
set /a CACHE_VER=1

:: Try to read current remote version
for /f "tokens=1,2 delims==" %%A in ('gh release download latest -R tristan9988/runecartel-updates -p version.properties -O - 2^>nul') do (
    if "%%A"=="client.version" set /a CLIENT_VER=%%B
    if "%%A"=="cache.version" set /a CACHE_VER=%%B
)

:: Auto-increment client version (cache only increments when cache actually changes)
set /a CLIENT_VER=%CLIENT_VER%+1

echo    Previous remote client version detected.
echo    New client version: %CLIENT_VER%
echo    Cache version: %CACHE_VER%
echo.
set /p CONFIRM="Publish client v%CLIENT_VER% and cache v%CACHE_VER%? (Y/n): "
if /i "%CONFIRM%"=="n" (
    set /p CLIENT_VER="Enter CLIENT version number: "
    set /p CACHE_VER="Enter CACHE version number: "
)

:: Write version.properties
echo client.version=%CLIENT_VER%> "update-staging\version.properties"
echo cache.version=%CACHE_VER%>> "update-staging\version.properties"

:: ============================================
:: STEP 5: Ensure updates repo exists
:: ============================================
echo [5/6] Preparing GitHub updates repository...

:: Check if the updates repo exists
gh repo view tristan9988/runecartel-updates >nul 2>&1
if errorlevel 1 (
    echo    Creating tristan9988/runecartel-updates repository...
    gh repo create tristan9988/runecartel-updates --public --description "RuneCartel game update files (auto-managed)" --confirm 2>nul
    if errorlevel 1 (
        echo    WARNING: Could not create repo. It may need manual creation.
        echo    Go to: https://github.com/organizations/RuneCartelHQ/repositories/new
        echo    Name: updates, Public, then re-run this script.
        pause
        exit /b 1
    )
)

:: ============================================
:: STEP 6: Upload to GitHub Release
:: ============================================
echo [6/6] Publishing update to GitHub...

:: Delete existing "latest" release (so we can recreate it cleanly)
gh release delete latest -R tristan9988/runecartel-updates --yes >nul 2>&1

:: Delete the tag too so it can be recreated at HEAD
gh api -X DELETE repos/tristan9988/runecartel-updates/git/refs/tags/latest >nul 2>&1

:: Create new release with all 3 files
gh release create latest ^
    "update-staging\client.zip" ^
    "update-staging\cache.zip" ^
    "update-staging\version.properties" ^
    -R tristan9988/runecartel-updates ^
    --title "Latest Update - Client v%CLIENT_VER% / Cache v%CACHE_VER%" ^
    --notes "Auto-published update. Client v%CLIENT_VER%, Cache v%CACHE_VER%." ^
    --latest

if errorlevel 1 (
    echo.
    echo ERROR: Failed to publish release!
    echo Make sure you have write access to tristan9988/runecartel-updates
    pause
    exit /b 1
)

:: Also update the local installed client
echo.
echo Updating your local client too...
copy "Tarnish\tarnish-client\build\libs\Tarnish.jar" "%USERPROFILE%\.runecartel\RuneCartel.jar" >nul 2>&1

echo.
echo ============================================
echo   UPDATE PUBLISHED SUCCESSFULLY!
echo ============================================
echo.
echo   Client version: %CLIENT_VER%
echo   Cache version:  %CACHE_VER%
echo.
echo   Release: https://github.com/tristan9988/runecartel-updates/releases/tag/latest
echo.
echo   Players will automatically get this update
echo   next time they open the launcher!
echo.
pause

