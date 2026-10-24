# milo. native (Kotlin + Jetpack Compose) — release 1

A real Android app (not a web wrapper). It reads music **directly** from the phone, SD card and USB drives through
Android's MediaStore, so there is no uploading, no "link a folder" and no file picker.

## Build the APK (no Android Studio needed)
1. Upload this whole repo (including `milo-native/` and `.github/workflows/native.yml`) to GitHub.
2. GitHub > **Actions** > **Build milo. native APK** > **Run workflow**.
3. When it finishes, open the run and download **milo-native-debug-apk** (a zip holding `app-debug.apk`).
4. Install it. It is a separate app (`com.ubayanda.milonative`), so it sits next to your current milo.

> **Heads-up:** this code was written without being able to compile it (the build tools are blocked in the
> authoring environment). The first run may fail on a small mistake. If it does, open the failed run, copy the red
> error lines and send them over; fixes are quick.

## What's in release 1
- First screen explains and asks for audio access (and notifications on Android 13+). If Android stops asking, a button opens Settings.
- Whole library scanned in place: songs on the phone, SD card and any USB drive Android has mounted; refreshes itself when you plug one in.
- Songs A to Z with letter headers and a letter rail; Albums; Artists; Search.
- Now Playing laid out like your mockup: edge-to-edge art, back chevron, four round buttons (Queue, Like, Speed, More/Sleep timer), big title/artist/album, progress bar, shuffle / previous / play / next / repeat, "Up next" handle. Swipe down or press Back to close.
- Background playback with notification and lock-screen controls, headset buttons, pauses when headphones unplug.
- Colour that follows the song's art; cards use a tiny stretched copy of the cover (no blur effect), so it stays smooth.
- Likes, speed, sleep timer.

## Not yet (later releases)
Accounts and miloCHAT, lyrics (the handle opens the queue for now), equaliser / sound, casting, sharing, playlists.

The web/PWA and TV apps are untouched and keep working.
