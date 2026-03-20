param(
    [ValidateSet('All', 'Client', 'Cache', 'Launcher', 'PushSource', 'Cleanup')]
    [string]$Mode = 'All',
    [switch]$SkipPublish,
    [switch]$SkipPush,
    [switch]$SkipStartServer,
    [switch]$SkipServerBuild
)

$ErrorActionPreference = 'Stop'

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$TarnishRoot = Join-Path $Root 'Tarnish'
$ClientProject = Join-Path $TarnishRoot 'tarnish-client'
$ServerProject = Join-Path $TarnishRoot 'tarnish-game'
$LauncherProject = Join-Path $TarnishRoot 'launcher'
$LauncherResources = Join-Path $LauncherProject 'src\main\resources'
$EmbeddedDir = Join-Path $LauncherResources 'embedded'
$StagingDir = Join-Path $Root 'update-staging'
$LocalGameDir = Join-Path $HOME '.runecartel'
$UpdatesRepo = 'tristan9988/runecartel-updates'
$SourceRepo = 'tristan9988/runecartel-rsps'
$ReleaseTag = 'latest'

function Write-Section([string]$message) {
    Write-Host ''
    Write-Host ('=' * 60)
    Write-Host $message
    Write-Host ('=' * 60)
}

function Ensure-Directory([string]$path) {
    if (-not (Test-Path -LiteralPath $path)) {
        New-Item -ItemType Directory -Path $path | Out-Null
    }
}

function Remove-PathIfExists([string]$path) {
    if (Test-Path -LiteralPath $path) {
        Remove-Item -LiteralPath $path -Recurse -Force
    }
}

function Invoke-External {
    param(
        [Parameter(Mandatory = $true)][string]$FilePath,
        [Parameter(Mandatory = $true)][string[]]$Arguments,
        [string]$WorkingDirectory = $Root,
        [switch]$AllowFailure,
        [switch]$Quiet
    )

    Push-Location $WorkingDirectory
    try {
        if ($Quiet) {
            & $FilePath @Arguments *> $null
        } else {
            & $FilePath @Arguments
        }
        $exitCode = $LASTEXITCODE
        if (-not $AllowFailure -and $exitCode -ne 0) {
            throw "Command failed ($exitCode): $FilePath $($Arguments -join ' ')"
        }
        return $exitCode
    } finally {
        Pop-Location
    }
}

function Test-GitHubReady {
    $gh = Get-Command gh -ErrorAction SilentlyContinue
    if ($null -eq $gh) {
        return $false
    }

    Invoke-External -FilePath $gh.Source -Arguments @('auth', 'status') -AllowFailure -Quiet | Out-Null
    return $LASTEXITCODE -eq 0
}

function Get-RemoteVersions {
    $versions = [ordered]@{
        'client.version' = 0
        'cache.version' = 0
        'launcher.version' = 0
    }

    if (-not (Test-GitHubReady)) {
        return $versions
    }

    $tempDir = Join-Path $env:TEMP ("runecartel-versions-" + [guid]::NewGuid().ToString())
    Ensure-Directory $tempDir

    try {
        Invoke-External -FilePath 'gh' -Arguments @('release', 'download', $ReleaseTag, '--repo', $UpdatesRepo, '-p', 'version.properties', '-D', $tempDir) -AllowFailure -Quiet | Out-Null
        $versionFile = Join-Path $tempDir 'version.properties'
        if (Test-Path -LiteralPath $versionFile) {
            foreach ($line in Get-Content -LiteralPath $versionFile) {
                if ($line -match '^\s*([^#][^=]*)=(.*)$') {
                    $key = $matches[1].Trim()
                    $value = $matches[2].Trim()
                    if ($versions.Contains($key)) {
                        $versions[$key] = [int]$value
                    }
                }
            }
        }
    } finally {
        Remove-PathIfExists $tempDir
    }

    return $versions
}

function Write-PropertiesFile([string]$path, [hashtable]$properties) {
    $lines = foreach ($entry in $properties.GetEnumerator()) {
        "{0}={1}" -f $entry.Key, $entry.Value
    }
    Set-Content -LiteralPath $path -Value $lines -Encoding ASCII
}

function Get-BuiltJarPath([string]$projectPath, [string[]]$preferredNames) {
    $libsDir = Join-Path $projectPath 'build\libs'
    foreach ($name in $preferredNames) {
        $candidate = Join-Path $libsDir $name
        if (Test-Path -LiteralPath $candidate) {
            return $candidate
        }
    }

    $latest = Get-ChildItem -LiteralPath $libsDir -Filter *.jar -File | Sort-Object LastWriteTimeUtc -Descending | Select-Object -First 1
    if ($null -eq $latest) {
        throw "No JAR found in $libsDir"
    }
    return $latest.FullName
}

function Compress-SingleFile([string]$sourceFile, [string]$destinationZip) {
    if (Test-Path -LiteralPath $destinationZip) {
        Remove-Item -LiteralPath $destinationZip -Force
    }
    Compress-Archive -Path $sourceFile -DestinationPath $destinationZip -CompressionLevel Optimal -Force
}

function Compress-DirectoryContents([string]$sourceDirectory, [string]$destinationZip) {
    if (Test-Path -LiteralPath $destinationZip) {
        Remove-Item -LiteralPath $destinationZip -Force
    }
    Compress-Archive -Path (Join-Path $sourceDirectory '*') -DestinationPath $destinationZip -CompressionLevel Optimal -Force
}

function Download-RemoteAsset([string]$assetName, [string]$destinationPath) {
    if (-not (Test-GitHubReady)) {
        return $false
    }

    $tempDir = Join-Path $env:TEMP ("runecartel-asset-" + [guid]::NewGuid().ToString())
    Ensure-Directory $tempDir

    try {
        Invoke-External -FilePath 'gh' -Arguments @('release', 'download', $ReleaseTag, '--repo', $UpdatesRepo, '-p', $assetName, '-D', $tempDir) -AllowFailure -Quiet | Out-Null
        $downloaded = Join-Path $tempDir $assetName
        if (Test-Path -LiteralPath $downloaded) {
            Copy-Item -LiteralPath $downloaded -Destination $destinationPath -Force
            return $true
        }
        return $false
    } finally {
        Remove-PathIfExists $tempDir
    }
}

function Ensure-StagedAsset([string]$assetName, [string]$localFallbackPath) {
    $destinationPath = Join-Path $StagingDir $assetName
    if (Test-Path -LiteralPath $destinationPath) {
        return $destinationPath
    }

    if (Download-RemoteAsset -assetName $assetName -destinationPath $destinationPath) {
        return $destinationPath
    }

    if ($localFallbackPath -and (Test-Path -LiteralPath $localFallbackPath)) {
        Copy-Item -LiteralPath $localFallbackPath -Destination $destinationPath -Force
        return $destinationPath
    }

    throw "Missing required asset '$assetName' and no fallback was available."
}

function Invoke-GradleBuild([string]$projectPath, [string[]]$arguments) {
    Invoke-External -FilePath (Join-Path $projectPath 'gradlew.bat') -Arguments $arguments -WorkingDirectory $projectPath | Out-Null
}

function Build-ClientAsset {
    Write-Host '[client] Building client...'
    Invoke-GradleBuild -projectPath $ClientProject -arguments @('shadowJar', '--no-daemon')

    $clientJar = Get-BuiltJarPath -projectPath $ClientProject -preferredNames @('Tarnish.jar', 'tarnish-client-1.0.jar')
    $clientZip = Join-Path $StagingDir 'client.zip'
    Compress-SingleFile -sourceFile $clientJar -destinationZip $clientZip

    Ensure-Directory $LocalGameDir
    Copy-Item -LiteralPath $clientJar -Destination (Join-Path $LocalGameDir 'RuneCartel.jar') -Force
    return $clientZip
}

function Build-CacheAsset {
    Write-Host '[cache] Packaging cache...'
    $cacheDir = Join-Path $ServerProject 'data\cache'
    if (-not (Test-Path -LiteralPath (Join-Path $cacheDir 'main_file_cache.dat'))) {
        throw "Cache directory not found at $cacheDir"
    }

    $cacheZip = Join-Path $StagingDir 'cache.zip'
    Compress-DirectoryContents -sourceDirectory $cacheDir -destinationZip $cacheZip
    return $cacheZip
}

function Sync-EmbeddedAssets([string]$versionFile) {
    Ensure-Directory $EmbeddedDir

    foreach ($asset in @('client.zip', 'cache.zip')) {
        $source = Join-Path $StagingDir $asset
        if (Test-Path -LiteralPath $source) {
            Copy-Item -LiteralPath $source -Destination (Join-Path $EmbeddedDir $asset) -Force
        }
    }

    Copy-Item -LiteralPath $versionFile -Destination (Join-Path $EmbeddedDir 'version.properties') -Force
}

function Build-LauncherAsset {
    Write-Host '[launcher] Building launcher...'
    Invoke-GradleBuild -projectPath $LauncherProject -arguments @('shadowJar', '--no-daemon')

    $launcherJar = Get-BuiltJarPath -projectPath $LauncherProject -preferredNames @('RuneCartel-Launcher.jar')
    $stagedLauncher = Join-Path $StagingDir 'RuneCartel-Launcher.jar'
    Copy-Item -LiteralPath $launcherJar -Destination $stagedLauncher -Force
    Copy-Item -LiteralPath $launcherJar -Destination (Join-Path $Root 'RuneCartel-Launcher.jar') -Force
    return $stagedLauncher
}

function Build-ServerArtifact {
    Write-Host '[server] Building server...'
    Invoke-GradleBuild -projectPath $ServerProject -arguments @('shadowJar', '--no-daemon')
}

function Publish-Release([hashtable]$versions) {
    if ($SkipPublish) {
        Write-Host '[publish] Skipped by flag.'
        return
    }

    if (-not (Test-GitHubReady)) {
        throw 'GitHub CLI is not installed or not authenticated.'
    }

    Write-Host '[publish] Uploading release assets...'
    $assets = @(
        (Join-Path $StagingDir 'client.zip'),
        (Join-Path $StagingDir 'cache.zip'),
        (Join-Path $StagingDir 'RuneCartel-Launcher.jar'),
        (Join-Path $StagingDir 'version.properties')
    )

    Invoke-External -FilePath 'gh' -Arguments @('release', 'delete', $ReleaseTag, '--repo', $UpdatesRepo, '--yes') -AllowFailure -Quiet | Out-Null
    Invoke-External -FilePath 'gh' -Arguments @('api', '-X', 'DELETE', "repos/$UpdatesRepo/git/refs/tags/$ReleaseTag") -AllowFailure -Quiet | Out-Null

    $title = "Latest Update - Client v$($versions['client.version']) / Cache v$($versions['cache.version']) / Launcher v$($versions['launcher.version'])"
    $notes = "Auto-published update. Client v$($versions['client.version']), Cache v$($versions['cache.version']), Launcher v$($versions['launcher.version'])."

    $args = @('release', 'create', $ReleaseTag) + $assets + @('--repo', $UpdatesRepo, '--title', $title, '--notes', $notes, '--latest')
    Invoke-External -FilePath 'gh' -Arguments $args | Out-Null
}

function Push-SourceChanges([string]$commitMessage) {
    if ($SkipPush) {
        Write-Host '[git] Source push skipped by flag.'
        return
    }

    if (-not (Test-Path -LiteralPath (Join-Path $Root '.git'))) {
        Write-Host '[git] No git repository found; skipping source push.'
        return
    }

    Write-Host '[git] Staging and pushing source changes...'
    Invoke-External -FilePath 'git' -Arguments @('add', '-A') -WorkingDirectory $Root | Out-Null

    $status = (& git -C $Root status --porcelain) | Out-String
    if ([string]::IsNullOrWhiteSpace($status)) {
        Write-Host '[git] Working tree already clean.'
        return
    }

    $commitExit = Invoke-External -FilePath 'git' -Arguments @('commit', '-m', $commitMessage) -WorkingDirectory $Root -AllowFailure
    if ($commitExit -ne 0) {
        $statusAfter = (& git -C $Root status --porcelain) | Out-String
        if ([string]::IsNullOrWhiteSpace($statusAfter)) {
            Write-Host '[git] Nothing new to commit.'
            return
        }
        throw 'git commit failed.'
    }

    Invoke-External -FilePath 'git' -Arguments @('push', 'origin', 'HEAD') -WorkingDirectory $Root | Out-Null
}

function Start-ServerWindow {
    if ($SkipStartServer) {
        Write-Host '[server] Restart skipped by flag.'
        return
    }

    $startServerScript = Join-Path $Root 'START-SERVER.bat'
    if (-not (Test-Path -LiteralPath $startServerScript)) {
        throw "START-SERVER.bat not found at $startServerScript"
    }

    Start-Process -FilePath $startServerScript -WorkingDirectory $Root | Out-Null
}

function Cleanup-Workspace {
    Write-Section 'Cleaning safe generated workspace clutter'

    $safeRemovals = @(
        (Join-Path $Root 'GitHub)'),
        (Join-Path $Root 'logo.png'),
        (Join-Path $ClientProject 'update-staging')
    )

    foreach ($path in $safeRemovals) {
        if (Test-Path -LiteralPath $path) {
            Remove-PathIfExists $path
            Write-Host "Removed: $path"
        }
    }

    $staleStagingFile = Join-Path $StagingDir 'client.jar'
    if (Test-Path -LiteralPath $staleStagingFile) {
        Remove-Item -LiteralPath $staleStagingFile -Force
        Write-Host "Removed: $staleStagingFile"
    }
}

try {
    Ensure-Directory $StagingDir
    Ensure-Directory $EmbeddedDir

    switch ($Mode) {
        'Cleanup' {
            Cleanup-Workspace
            exit 0
        }
        'PushSource' {
            Push-SourceChanges -commitMessage 'chore: sync launcher and update pipeline'
            exit 0
        }
    }

    Write-Section "RuneCartel publish pipeline ($Mode)"

    Remove-PathIfExists $StagingDir
    Ensure-Directory $StagingDir

    $remoteVersions = Get-RemoteVersions
    $versions = [ordered]@{
        'client.version' = [int]$remoteVersions['client.version']
        'cache.version' = [int]$remoteVersions['cache.version']
        'launcher.version' = [int]$remoteVersions['launcher.version']
    }

    $rebuildClient = $Mode -in @('All', 'Client')
    $rebuildCache = $Mode -in @('All', 'Cache')
    $rebuildLauncher = $Mode -in @('All', 'Client', 'Cache', 'Launcher')
    $rebuildServer = $Mode -eq 'All' -and -not $SkipServerBuild

    if ($rebuildClient) {
        Build-ClientAsset | Out-Null
        $versions['client.version']++
    } else {
        Ensure-StagedAsset -assetName 'client.zip' -localFallbackPath (Join-Path $EmbeddedDir 'client.zip') | Out-Null
    }

    if ($rebuildCache) {
        try {
            Build-CacheAsset | Out-Null
            $versions['cache.version']++
        } catch {
            Write-Host "[cache] $($_.Exception.Message)" -ForegroundColor Yellow
            Write-Host '[cache] Falling back to the last published/embedded cache package for this run.' -ForegroundColor Yellow
            Ensure-StagedAsset -assetName 'cache.zip' -localFallbackPath (Join-Path $EmbeddedDir 'cache.zip') | Out-Null
        }
    } else {
        Ensure-StagedAsset -assetName 'cache.zip' -localFallbackPath (Join-Path $EmbeddedDir 'cache.zip') | Out-Null
    }

    if ($rebuildServer) {
        Build-ServerArtifact
    }

    if ($rebuildLauncher) {
        $versions['launcher.version']++
    } else {
        Ensure-StagedAsset -assetName 'RuneCartel-Launcher.jar' -localFallbackPath (Join-Path $Root 'RuneCartel-Launcher.jar') | Out-Null
    }

    $versionFile = Join-Path $StagingDir 'version.properties'
    Write-PropertiesFile -path $versionFile -properties $versions
    Sync-EmbeddedAssets -versionFile $versionFile

    if ($rebuildLauncher) {
        Build-LauncherAsset | Out-Null
    }

    $commitMessage = "chore: publish update client v$($versions['client.version']) cache v$($versions['cache.version']) launcher v$($versions['launcher.version'])"
    Push-SourceChanges -commitMessage $commitMessage
    Publish-Release -versions $versions

    if ($Mode -eq 'All') {
        Start-ServerWindow
    }

    Write-Section 'Done'
    Write-Host "Client version:   $($versions['client.version'])"
    Write-Host "Cache version:    $($versions['cache.version'])"
    Write-Host "Launcher version: $($versions['launcher.version'])"
    exit 0
} catch {
    Write-Host ''
    Write-Host "ERROR: $($_.Exception.Message)" -ForegroundColor Red
    if ($Mode -eq 'All' -and -not $SkipStartServer) {
        Write-Host 'Attempting to bring the server back up with the existing startup script...' -ForegroundColor Yellow
        try {
            Start-ServerWindow
        } catch {
            Write-Host "Failed to restart server automatically: $($_.Exception.Message)" -ForegroundColor Red
        }
    }
    exit 1
}

