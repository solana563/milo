# milo. native (Kotlin + Jetpack Compose) — release 1

A real Android app (not a web wrapper). It reads music **directly** from the phone, SD card and USB drives through
Android's MediaStore, so there is no uploading, no "link a folder" and no file picker.

## Build the APK (no Android Studio needed)
1. Upload this whole repo (including `milo-native/` and `.github/workflows/native.yml`) to GitHub.
2. GitHub > **Actions** > **Build milo. native APK** > **Run workflow**.
3. When it finishes, open the run and download **milo-native-debug-apk** (a zip holding `app-debug.apk`).
4. Install it. It is a separate app (`com.ubayanda.milonative`), so it sits next to your current milo.

> The workflow also builds a minified **release** APK (`milo-native-release-apk`), which scrolls much more smoothly than debug.

## What's in this version
- Looks like the web milo.: black header with the logo, rounded panel, amber accent, Figtree font, pill nav, bordered cards.
- First screen explains and asks for audio access (and notifications on Android 13+). If Android stops asking, a button opens Settings.
- Whole library read in place from the phone, SD card and USB drives; refreshes itself when you plug one in.
- Songs A to Z with letter headers and a letter rail; Albums; Artists; Playlists; Search.
- **Playlists**: create, rename, delete, add any song from its "⋮" menu. **Liked songs** page. Play next / Add to queue / Share on every song.
- **Now Playing** like your mockup: edge-to-edge art, four round buttons (Queue, Like, Sound, More), progress bar, shuffle / prev / play / next / repeat.
- **Lyrics**: tap or drag up the handle. Synced lines follow the song and tap-to-seek (from lrclib.net, same as the web app; needs internet the first time per song).
- **Sound**: speed, 8 equaliser presets, per-band sliders, bass boost and spatial (uses the phone's own audio effects).
- Sleep timer (More menu), background playback with notification and lock-screen controls.

- **Accounts**: welcome screen (drifting leaves), sign up, log in, pick a username, terms, Profile tab and sign out. Same Supabase account as the web milo., so one login works in both. "Continue without an account" works too.

## Also in current builds
Friends and chat, cloud sync, casting, YouTube, Navidrome/Subsonic, crossfade, listening insights, backup/restore, car mode, lyric translation and share cards.

The web/PWA and TV apps are untouched and keep working.
