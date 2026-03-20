# Admin Scripts

This folder holds the less-frequently used maintenance scripts that were moved out of the workspace root to reduce clutter.

## Common scripts

- `PUBLISH-UPDATE.bat` — publish a client update
- `PUBLISH-CACHE-UPDATE.bat` — publish a cache update
- `PUBLISH-LAUNCHER-UPDATE.bat` — publish a launcher update
- `PUSH-LAUNCHER-GIT.bat` — push source changes to GitHub
- `SETUP-GITHUB.bat` — set up the source/update repositories
- `UPDATE-SERVER.bat` — rebuild and restart only the server
- `CLEANUP-RSPS.bat` — remove safe generated clutter

## Root entrypoints kept on purpose

These remain in the root because normal use or the auto-update flow depends on them:

- `Play RuneCartel.bat`
- `START-SERVER.bat`
- `UPDATE-ALL.bat`
- `RuneCartel-Launcher.jar`
- `publish-release.ps1`

