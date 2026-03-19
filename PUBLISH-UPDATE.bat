@echo off
title RuneCartel Update Publisher
echo ============================================
echo   RuneCartel Update Publisher
echo ============================================
echo.

:: Build the client
echo [1/4] Building client...
cd /d "%~dp0Tarnish\tarnish-client"
call gradlew.bat shadowJar --no-daemon
if errorlevel 1 (
    echo ERROR: Client build failed!
    pause
    exit /b 1
)

:: Create client.zip
echo [2/4] Packaging client...
cd /d "%~dp0"
if exist "update-staging" rmdir /s /q "update-staging"
mkdir "update-staging"
powershell -Command "Compress-Archive -Path 'Tarnish\tarnish-client\build\libs\Tarnish.jar' -DestinationPath 'update-staging\client.zip' -Force"

:: Copy cache.zip
echo [3/4] Packaging cache...
copy "Tarnish\launcher\src\main\resources\embedded\cache.zip" "update-staging\cache.zip" >nul 2>&1

:: Read current versions and increment client version
echo [4/4] Creating version file...
set /a CLIENT_VER=3
set /a CACHE_VER=1

:: Ask for version numbers
echo.
echo Current versions will be used for the update.
set /p CLIENT_VER="Enter CLIENT version number (default 3): "
set /p CACHE_VER="Enter CACHE version number (default 1): "

:: Write version.properties
echo client.version=%CLIENT_VER%> "update-staging\version.properties"
echo cache.version=%CACHE_VER%>> "update-staging\version.properties"

echo.
echo ============================================
echo   UPDATE FILES READY!
echo ============================================
echo.
echo Files are in: %~dp0update-staging\
echo   - version.properties (client=%CLIENT_VER%, cache=%CACHE_VER%)
echo   - client.zip
echo   - cache.zip
echo.
echo NEXT STEPS:
echo   1. Go to https://github.com/RuneCartelHQ/updates/releases
echo   2. Click "Edit" on the "latest" release (or create one tagged "latest")
echo   3. Delete the old files and upload all 3 files from update-staging\
echo   4. Save the release
echo.
echo Players will automatically get the update next time they open the launcher!
echo.

:: Also update the local installed client
echo Updating your local client too...
copy "Tarnish\tarnish-client\build\libs\Tarnish.jar" "%USERPROFILE%\.runecartel\RuneCartel.jar" >nul 2>&1
echo Done!
echo.
pause

