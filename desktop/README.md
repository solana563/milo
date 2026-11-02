# milo. desktop (Windows)

Electron wrapper around the milo. web app. Same code as the web/PWA, so features stay in sync.

- **Get the installer:** GitHub → Actions → "Build Desktop (Windows)" → Run workflow → download `milo-desktop-windows`. It holds an installer (`...-win-x64.exe`) and a portable exe.
- **Run locally:** `cd desktop && npm install && npm start`
- Closing the window hides milo. to the system tray so music keeps playing. Quit from the tray icon.
- Media keys, single-instance, remembered window size, links open in your browser.
- Windows SmartScreen may warn on first run because the exe is unsigned. Choose "More info → Run anyway".

## Real folders and data (v1.1)
- **Music folders:** "Link a music folder" uses the Windows folder picker. Folders are remembered, scanned straight from disk, watched for new songs, and never copied. Manage them under Music folders. Songs are streamed from disk (seeking works).
- **Data folder:** your settings, playlists and likes are mirrored to `Documents\milo\milo-data.json`, with the last 7 daily backups in `backups\`. Open or move it from Storage. If the app ever starts empty (new PC, cleared storage), it restores from that file.
- Songs you *upload* (copied into the app) still live in the app's internal storage, as before.
- Folders linked in the earlier version (browser style) need linking once more.

## YouTube Music (v1.2)
- Discover → YouTube now searches YouTube Music (songs, artists, albums) and plays direct audio streams in milo.'s own player.
- Artist, album and playlist pages open inside milo. with a big cover and a blurred cover backdrop.
- If a direct stream can't be fetched, milo. falls back to YouTube's embedded player and tells you why.
