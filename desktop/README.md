# milo. desktop (Windows)

Electron wrapper around the milo. web app. Same code as the web/PWA, so features stay in sync.

- **Get the installer:** GitHub → Actions → "Build Desktop (Windows)" → Run workflow → download `milo-desktop-windows`. It holds an installer (`...-win-x64.exe`) and a portable exe.
- **Run locally:** `cd desktop && npm install && npm start`
- Closing the window hides milo. to the system tray so music keeps playing. Quit from the tray icon.
- Media keys, single-instance, remembered window size, links open in your browser.
- Windows SmartScreen may warn on first run because the exe is unsigned. Choose "More info → Run anyway".
