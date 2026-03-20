@echo off
title RuneCartel's HQ - Full System Update
echo ============================================
echo   RuneCartel's HQ - Full System Update
echo   (Server + Client + Cache -> GitHub)
echo ============================================
echo.

:: ========================================
:: PATH setup
:: ========================================
set PATH=%PATH%;C:\Program Files\GitHub CLI
cd /d "%~dp0"

:: ========================================
:: STEP 1: Build the client
:: ========================================
echo [1/7] Building client...
cd /d "%~dp0Tarnish\tarnish-client"
call gradlew.bat shadowJar --no-daemon
if %ERRORLEVEL% NEQ 0 (
    echo    WARNING: Client build failed! Skipping client publish.
    set CLIENT_BUILD_OK=0
) else (
    echo    Client built successfully.
    set CLIENT_BUILD_OK=1
)
echo.

:: ========================================
:: STEP 2: Build the server
:: ========================================
echo [2/7] Building server...
cd /d "%~dp0Tarnish\tarnish-game"
call gradlew.bat shadowJar --no-daemon
if %ERRORLEVEL% NEQ 0 (
    echo    WARNING: Server build failed!
    set SERVER_BUILD_OK=0
) else (
    echo    Server built successfully.
    set SERVER_BUILD_OK=1
)
echo.

:: ========================================
:: STEP 3: Package update files
:: ========================================
echo [3/7] Packaging update files...
cd /d "%~dp0"

if exist "update-staging" rmdir /s /q "update-staging"
mkdir "update-staging"

:: Package client.zip (from newly built client)
if "%CLIENT_BUILD_OK%"=="1" (
    powershell -Command "Compress-Archive -Path 'Tarnish\tarnish-client\build\libs\Tarnish.jar' -DestinationPath 'update-staging\client.zip' -Force"
    echo    Packaged client.zip
)

:: Package cache.zip (from game data cache directory)
if exist "Tarnish\tarnish-game\data\cache\main_file_cache.dat" (
    powershell -Command "Compress-Archive -Path 'Tarnish\tarnish-game\data\cache\*' -DestinationPath 'update-staging\cache.zip' -Force"
    echo    Packaged cache.zip from game data
) else (
    :: Fallback to embedded cache
    if exist "Tarnish\launcher\src\main\resources\embedded\cache.zip" (
        copy "Tarnish\launcher\src\main\resources\embedded\cache.zip" "update-staging\cache.zip" >nul
        echo    Packaged cache.zip from embedded resources
    )
)

:: Also update the launcher's embedded copies
if "%CLIENT_BUILD_OK%"=="1" (
    copy "update-staging\client.zip" "Tarnish\launcher\src\main\resources\embedded\client.zip" >nul 2>&1
)
if exist "update-staging\cache.zip" (
    copy "update-staging\cache.zip" "Tarnish\launcher\src\main\resources\embedded\cache.zip" >nul 2>&1
)
echo.

:: ========================================
:: STEP 4: Determine versions
:: ========================================
echo [4/7] Determining version numbers...

set /a CLIENT_VER=2
set /a CACHE_VER=1

:: Try to read current remote version from GitHub
gh auth status >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    for /f "tokens=1,2 delims==" %%A in ('gh release download latest -R tristan9988/runecartel-updates -p version.properties -O - 2^>nul') do (
        if "%%A"=="client.version" set /a CLIENT_VER=%%B
        if "%%A"=="cache.version" set /a CACHE_VER=%%B
    )
)

:: Auto-increment both versions
if "%CLIENT_BUILD_OK%"=="1" set /a CLIENT_VER=%CLIENT_VER%+1
set /a CACHE_VER=%CACHE_VER%+1

echo    Client version: %CLIENT_VER%
echo    Cache version:  %CACHE_VER%

:: Write version.properties
(echo client.version=%CLIENT_VER%)> "update-staging\version.properties"
(echo cache.version=%CACHE_VER%)>> "update-staging\version.properties"
echo.

:: ========================================
:: STEP 5: Publish to GitHub
:: ========================================
echo [5/7] Publishing update to GitHub...

gh auth status >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo    WARNING: Not logged into GitHub - skipping publish.
    echo    Run SETUP-GITHUB.bat to configure GitHub access.
    goto skip_publish
)

gh repo view tristan9988/runecartel-updates >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo    WARNING: tristan9988/runecartel-updates repo not found - skipping publish.
    echo    Run SETUP-GITHUB.bat to create the repository.
    goto skip_publish
)

:: Delete existing release and recreate
gh release delete latest -R tristan9988/runecartel-updates --yes >nul 2>&1
gh api -X DELETE repos/tristan9988/runecartel-updates/git/refs/tags/latest >nul 2>&1

:: Build the file list for upload
set UPLOAD_FILES=
if exist "update-staging\client.zip" set UPLOAD_FILES=%UPLOAD_FILES% "update-staging\client.zip"
if exist "update-staging\cache.zip" set UPLOAD_FILES=%UPLOAD_FILES% "update-staging\cache.zip"
set UPLOAD_FILES=%UPLOAD_FILES% "update-staging\version.properties"

gh release create latest %UPLOAD_FILES% -R tristan9988/runecartel-updates --title "Latest Update - Client v%CLIENT_VER% / Cache v%CACHE_VER%" --notes "Auto-published by ::update command. Client v%CLIENT_VER%, Cache v%CACHE_VER%." --latest

if %ERRORLEVEL% NEQ 0 (
    echo    WARNING: Failed to publish to GitHub!
) else (
    echo    Published to GitHub successfully!
    echo    Release: https://github.com/tristan9988/runecartel-updates/releases/tag/latest
)

:skip_publish
echo.

:: ========================================
:: STEP 6: Update local installed client
:: ========================================
echo [6/7] Updating local files...
if "%CLIENT_BUILD_OK%"=="1" (
    copy "Tarnish\tarnish-client\build\libs\Tarnish.jar" "%USERPROFILE%\.runecartel\RuneCartel.jar" >nul 2>&1
    echo    Local client updated.
)

:: Rebuild the launcher JAR so the distributable copy has fresh embedded files
echo    Rebuilding launcher...
cd /d "%~dp0Tarnish\launcher"
call gradlew.bat shadowJar --no-daemon >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    cd /d "%~dp0"
    copy "Tarnish\launcher\build\libs\RuneCartel-Launcher.jar" "RuneCartel-Launcher.jar" >nul 2>&1
    echo    Launcher JAR updated (RuneCartel-Launcher.jar).
) else (
    cd /d "%~dp0"
    echo    WARNING: Launcher rebuild failed, distributable JAR not updated.
)
echo.

:: ========================================
:: STEP 7: Start the server
:: ========================================
echo [7/7] Starting server...
cd /d "%~dp0Tarnish\tarnish-game"

if "%SERVER_BUILD_OK%"=="1" (
    start "RuneCartel Server" cmd /k "C:\Program Files\Java\jdk-19\bin\java.exe" -XX:-OmitStackTraceInFastThrow --enable-preview -XX:+UseZGC -Xmx8g -Xms4g --add-opens java.base/java.lang=ALL-UNNAMED -jar build\libs\tarnish-server-all.jar
    echo    Server started!
) else (
    echo    Server build failed - attempting to start old jar...
    start "RuneCartel Server" cmd /k "C:\Program Files\Java\jdk-19\bin\java.exe" -XX:-OmitStackTraceInFastThrow --enable-preview -XX:+UseZGC -Xmx8g -Xms4g --add-opens java.base/java.lang=ALL-UNNAMED -jar build\libs\tarnish-server-all.jar
    echo    Started with old server jar.
)

echo.
echo ============================================
echo   FULL UPDATE COMPLETE!
echo ============================================
echo   Client v%CLIENT_VER% / Cache v%CACHE_VER%
echo   Server: %SERVER_BUILD_OK% (1=OK, 0=FAILED)
echo   Client: %CLIENT_BUILD_OK% (1=OK, 0=FAILED)
echo ============================================
echo.
echo Players will get the new client/cache
echo automatically when they reopen the launcher.
echo.

