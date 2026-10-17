# milo. as a native app

milo. detects where it is running (`NV.mode`: `capacitor`, `webview`, `pwa` or `web`) and switches on what each one supports.

## What already works in every build
- **Storage:** Profile > Storage shows usage, asks the system to protect your data, backs up / restores, adds a folder (desktop Chrome / Edge) and removes uploaded songs. Settings are also mirrored into IndexedDB, so they come back if localStorage is cleared.
- **Back button:** closes pop-ups, lyrics, queue and Now Playing before leaving the app.
- **Screen stays on** while Now Playing or lyrics is open and a song is playing.
- **Offline notice**, system / status bar colour follows the theme, no text-selection or long-press menus on controls.
- **Share target (installed PWA):** share songs from another app to milo. (`manifest.webmanifest` > `share_target`, handled in `sw.js`).

## Option A: Web2APK (what you used)
Loads your hosted site, so every update to the hosted files shows up in the app. Back button, storage, wake lock and offline notice work. The hardware back button leaves the app from the home screen (no "press again to exit"), and there is no lock-screen player.

## Option B: Capacitor (full native, background audio, native splash)
Everything is already set up in the `native/` folder. The hosted site (GitHub Pages / Vercel) stays the main app; this is the installable Android build.

**Easiest: let GitHub build it.** Push the repo, open Actions > "Build Android APK" > Run workflow, then download `milo-debug-apk` from the run. (`.github/workflows/android.yml` is included.)

**Or build it yourself** (Node 22, Java 21, Android SDK):
```bash
cd native
npm install
npm run prepare:web      # copies the web app into native/www
npx cap add android      # first time only
npm run assets           # icons and your eye photo as the native splash
npm run sync             # also adds the audio / notification permissions
npm run build:debug      # APK: android/app/build/outputs/apk/debug/
```
What you get over Web2APK:
- **Music keeps playing with the screen off** (a foreground service with a "Playing" notification; Android 13+ asks for notification permission once).
- **Native splash** shows your eye photo straight away, then hands over to the in-app splash. No default Android splash.
- Back button with "press back again to exit", haptics, share sheet, keep-awake, status bar colours.

The app id is `com.ubayanda.milo` (change it in `native/capacitor.config.json` before you publish anywhere). `androidScheme` stays `https` so YouTube accepts the origin.

This was generated and synced here, but **not compiled or run on a phone**, so treat the first APK as a test build. On some phones (Xiaomi, Huawei, Samsung power saving) you may need to set milo. to "Unrestricted" battery use for background playback.

## Not done yet (needs native code)
- A lock-screen player with album art and buttons inside the native build (the web Media Session already provides it in Chrome).
- Reading the phone's music library directly (MediaStore) instead of picking files.
- Keeping song files in the app's own folder instead of the browser database.


## Music already on the phone
`native/plugin/MiloMediaPlugin.java` reads the phone's music (MediaStore) and `scripts/patch-android.mjs` installs it and registers it in MainActivity every time you run `npm run android:add` or `npm run sync`.
The first launch offers "Play the music on your phone"; the permission asked is Music and audio (Android 13+) or Storage (older).
Songs play from their file path through Capacitor's local file route; cover art is written to the app cache. Please test on a real phone: seeking inside long files and Android 14+ partial media access are the two things to check.
