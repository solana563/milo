# Android Auto / car browsing (Phase 2)

## What was built (Round 31)
- `PlaybackService` is now a Media3 `MediaLibraryService`, the officially supported way to expose a browsable library. The same player, equaliser, crossfade and notification are kept; the in-app player connects exactly as before.
- Browse tree shown in the car: Recently played, Liked songs, Playlists, Albums, Artists. Songs on the phone plus online songs the user saved.
- Tapping a song queues the folder it was tapped in, starting at that song (capped at 200 songs).
- Search from the car and voice requests ("play <song or artist> on milo."), searched in the phone library only.
- Lists are limited to 100 items per page, as the driving rules ask.
- `res/xml/automotive_app_desc.xml` + manifest entry declare milo. as a media app.
- The existing in-app Driving screen (`CarScreen`) is unchanged.

## Not done / limits (honest)
- **Not tested in a car or on the Desktop Head Unit.** A normal build passing does NOT prove Android Auto works.
- Radio stations and YouTube browsing are not in the car tree yet.
- Android Auto only lists apps installed from Google Play, unless "Unknown sources" is on in Android Auto's developer settings (see below). The `fdroid`/`lite` builds work the same way for sideloading.
- Automatic playback on Bluetooth connect already follows the existing in-app "resume" setting; steering-wheel buttons go through the media session.

## How to test on a phone with the Desktop Head Unit (DHU)
1. Phone: install the Android Auto app, open it, tap the version number 10 times, open the menu, "Developer settings", turn on "Unknown sources", then start "Head unit server".
2. Computer: install the DHU via Android Studio SDK Manager (Android Auto Desktop Head Unit Emulator), run `adb forward tcp:5277 tcp:5277`, then start `desktop-head-unit`.
3. In the DHU open the apps launcher, choose milo.
Checklist (tick only what you ran):
- ☐ milo. appears in the launcher ☐ five folders show ☐ album, artist, playlist open
- ☐ tapping a song plays and Next/Previous move through that folder
- ☐ search works ☐ voice "play <artist>" works
- ☐ play/pause, skip from the DHU and from phone notification stay in sync
- ☐ phone call during playback pauses then resumes
- ☐ real car or Bluetooth head unit: connect, disconnect, reconnect behaviour
