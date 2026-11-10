# milo. on iPhone: feasibility assessment

Status today: `milo-ios/` is a small SwiftUI app (about 850 lines) that CI builds into an **unsigned** .ipa. It has never been run on an iPhone by me, and I have not seen a signed build. **iOS is not supported yet and must not be advertised.** This document says what a proper version needs.

## Verdict
A real native iPhone app is feasible and the platform suits a music player well. The cost is mostly (1) an Apple Developer account and a Mac-based signing setup, (2) a rewrite of the Android-only features in Swift, and (3) the fact that YouTube-based features and downloads are handled very differently on iOS. Estimate: a basic, reliable player is weeks of work; feature parity with the Android app is months.

## Audio architecture needed
- **Playback:** `AVPlayer` (already used) for files and streams; `AVQueuePlayer` or a manual queue for gapless. Crossfade needs two players or `AVAudioEngine`.
- **Session:** `AVAudioSession` category `.playback`, with interruption and route-change handling (calls, Siri, headphones unplugged, Bluetooth). The current code handles interruptions only.
- **Lock screen and Control Center:** `MPNowPlayingInfoCenter` (title, artwork, position) and `MPRemoteCommandCenter` (play, pause, next, previous, seek). Not present yet. Required.
- **Equaliser and effects:** `AVAudioEngine` with `AVAudioUnitEQ`. Loudness/ReplayGain is done by reading tags and adjusting volume.
- **Car:** CarPlay is a separate Apple entitlement (apply to Apple); without it there is no car screen. Bluetooth audio works without it.

## Background audio restrictions
- Needs `UIBackgroundModes: audio` (already in `project.yml`) and an active audio session, or playback stops when the screen locks.
- iOS suspends apps that are not playing. So **downloads, Connect polling and Sessions polling do not run in the background** the way the Android foreground service does. Downloads would use `URLSession` background sessions (the system continues them), which is more reliable than Android but has its own rules.
- Push notifications go through Apple's service (APNs), not Firebase alone. Firebase can relay, but an APNs key from the Apple Developer account is required.

## Local media permissions and limits
- The Music library (`MPMediaLibrary`) needs `NSAppleMusicUsageDescription` (present). It exposes songs the person synced or bought; **DRM-protected Apple Music tracks cannot be played by a third-party app**, and songs without a local file (`assetURL` is nil) cannot be played.
- Files from the Files app need the document picker or security-scoped bookmarks; there is no free access to arbitrary folders like on Android.
- Format support is decided by the system: MP3, AAC, ALAC, WAV, AIFF are fine; **FLAC is supported by iOS 11+, Ogg Vorbis and Opus are not natively** (would need a bundled decoder).

## Playback limitations to plan around
- **YouTube:** no supported, App-Store-safe way to extract audio. A YouTube-based feature on iOS would have to use the official embedded player (video, no background audio guarantee, and Apple and YouTube both restrict that). Treat YouTube features, and YouTube downloads, as **not available** on iOS unless the owner accepts that risk. Apple also reviews apps for terms compliance.
- Shared playlists, Sessions, Connect, taste profile and sync are server-based and could be ported, since they only need HTTPS calls to the same Supabase functions.
- Widgets are WidgetKit (SwiftUI), a separate target. Live Activities are possible for Now Playing.

## Platform UI
SwiftUI with the same visual language (dark, amber, glass) but iOS navigation: tab bar, large titles, swipe back, haptics, Dynamic Type, VoiceOver labels, safe areas and the Dynamic Island. Do not copy Android layouts.

## Build and signing requirements
- Apple Developer Program, USD 99 per year, for TestFlight and the App Store. A free account only gives 7-day sideload builds.
- A macOS runner (the existing `ios.yml` uses `macos-14`). Needs a distribution certificate, provisioning profile and App Store Connect API key stored as GitHub secrets, and a signed archive step. The current workflow builds unsigned only.
- App Store review: privacy nutrition labels, account deletion in the app (required), and content/terms compliance for any streaming feature.
- Test on real devices, at least one older and one current iPhone. Simulator audio is not enough for background and Bluetooth behaviour.

## Suggested order if you go ahead
1. Join the Apple Developer Program; make a signed TestFlight build of the existing app.
2. Lock screen and Control Center controls, audio session handling, queue persistence.
3. Local library polish, playlists, favourites, search.
4. Supabase sign-in and sync (reuse the same tables and functions).
5. Shared playlists, then Sessions and Connect (foreground only).
6. Widgets, CarPlay application, push through APNs.
Do not claim iOS support until a signed build has been tested on a physical iPhone.
