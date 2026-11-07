# Credits

## Limusic
The YouTube Music features in milo. (`src/js/67-youtube-music-direct-streams-pages-art.js`, the `dsk:http` / `milostream`
parts of `desktop/main.js`, and `src/css/10-youtube-music-art.css`) were built after studying
[Limusic](https://github.com/SimoHypers/limusic) by SimoHypers (GPL-3.0), and it in turn credits
[Metrolist](https://github.com/mostafaalagamy/Metrolist).

What was learned from it:
- which YouTube client identity returns plain audio URLs without a cipher or PoToken (VISIONOS), and which client to use for metadata (WEB_REMIX)
- how search, artist, album and playlist answers are shaped, and how to pick the best original (non-dubbed) audio stream
- that googlevideo throttles one long download but serves bounded pieces at full speed, which is why the desktop app streams through a small local proxy
- how its pages show artwork: size-aware thumbnails with a fall-back chain, ignoring YouTube's 2x2 playlist collage, a blurred cover backdrop that fades into the page, and poster-style artist cards

The code in milo. is written separately in JavaScript, but it follows Limusic's approach closely. Limusic is licensed
under the GNU GPL v3, so if you distribute milo. builds that include this code, they should be released under a
GPL-3.0 compatible licence with source. See `LICENSE` choice notes in `milo-native/FDROID.md`.

## Licence

milo. is released under the GNU General Public License v3.0 (see `LICENSE`) because it contains YouTube Music code written from how limusic (GPL-3.0) works, on web, desktop and native Android.
The native Android app got the same engine (`data/YtMusic.kt`, `player/YtSource.kt`) and the same artist, album and playlist art display.

## Features added in this round
- Word-by-word lyrics (enhanced LRC `<mm:ss.xx>` tags) on web, desktop and native.
- ListenBrainz and Last.fm scrobbling on web, desktop, Android and native (Profile > Scrobbling / Settings).
- Listen together: host or join with a code over the realtime channel, shared between web and native.

## Motion and Bluetooth
- Song art and titles glide in the direction you skipped (web and native); shimmer skeletons replace "Loading…" text.
- Bluetooth: "Playing on" device indicator, carry on when headphones reconnect, remembered volume per device, lyrics delay for Bluetooth lag (native); output-device picker and lyrics delay (web/desktop).

## Visualiser and Discord
- Native Android: live spectrum strip on the lyrics page and a full-screen visualiser (graphic-EQ button on the player). Uses Android's Visualizer, so it needs the microphone permission; nothing is recorded.
- Windows desktop: Discord Rich Presence over Discord's local IPC (needs your own Discord Application ID, see Profile > Discord presence).

## Round 1 of the wish list
- Native Home: "Made for you" daily mixes (favourites, rediscover, more of your top artists, plus an online radio from your most played artist).
- Native player: quality badge (codec, bit depth, sample rate or bitrate).
- One-tap error report in Profile on web and native.

## Round 2: offline downloads (native)
- Download for offline from any YouTube song's menu, or Download on artist, album and playlist pages. Saved songs play with no connection; Home shows a Downloaded section (first when you're offline). Manage in Profile > Downloads.

## Round 3: audio (native)
- Gapless for streamed songs (the next song's link is looked up while the current one plays); local songs were already gapless with crossfade off.
- Loudness matching (ReplayGain tags in files, YouTube's own loudness for streams), turn-down only.
- Parametric EQ: five peaking bands (frequency, gain, Q) inside the player's audio path, with a saved curve per output device.

Round 4 (social): Listen-together invites travel as plain chat text ("Listen together · code XXXXXX"), so no database changes are needed. Recap share is plain text.

Round 5: universal search (library, playlists, friends, saved-lyrics search, YouTube Music), per-year recap, Windows offline downloads (stored in the app data folder, played through the same local file protocol as linked folders).

Round 6 (phone app): smart playlists, .m3u import/export, skip silence / mono / balance / A-B loop, long-recording resume, lyrics saved with downloads, YouTube links, song info editing (kept in the app), app shortcuts, accent colour, first-run tour.

Round 7 (phone app): ten-band graphic EQ with fader UI, visualiser rebuilt on the player's own audio (no microphone permission), crossfade fixes (skip during a fade, wait for the next song), pressable button feel.

Round 8 (phone app): glass design pass. Rim-lit translucent panels, capsule controls, glass tab bar and mini player, big-knob sliders, glowing switches, glass segmented control, larger corner radii.

## Round 9
Full-bleed art hero on album/artist/playlist pages; "Brought to you by" card (theLEAF foundation) in Profile; stronger now-playing highlight with animated bars; Now playing button only on lists that contain the song; release signing via env secrets + AAB build in CI; allowBackup off; draft privacy.html / terms.html.

Autoplay: tops up at 2 songs left, YouTube radio for streamed songs, smarter local scoring, skip learning, queue header toggle.

Liquid glass nav bubble + animated screen transitions (BottomNav.kt, MiloApp.kt).

Round 10: blurred pop-up cards (MiloPopup, BlurBehind on dialogs), raised cards, Local/Both/International region choice for Discover.

Discover is now a YouTube-first feed (trending songs, trending artists, new releases) with a prominent Live radio button; visualiser removed from the lyrics screen.

Social: Feed tab (stories, listening-now cards with reactions, shared playlists), chat search.

Round 13: typing indicator (Supabase Realtime broadcast), lyrics rescan / word highlight / video-vs-studio match + timing nudge, thread-style friend posts with lyric quotes, real Apple Music charts in Discover.

Round 14: playback notification now opens the app (media session activity); widgets redrawn (glass backgrounds, accent colour, live progress, preview images, taps open milo. when nothing is queued); Profile > Home setting to hide the phone's music on Home while online.

Round 15: iOS app started (milo-ios, SwiftUI): background audio and lock-screen controls, Music-library songs, Apple charts and search (30 s previews), synced lyrics (LRCLIB), liked songs, accent / region / hide-local settings, Brought-by card. Built by .github/workflows/ios.yml on a macOS runner.

Round 16 (desktop polish): taskbar identity (AppUserModelId), window position restored only when still on a screen, debounced window-state saving, page auto-recovers from a crash or failed load, safer app:// file serving, permission whitelist, clean exit on Windows log-off.

Round 17: Download buttons (song rows, album / playlist / liked pages, Discover, Now Playing, "Download selected" in multi-select); phone music hidden on Home by default while online; recommendations follow your listening (3 artist radios + "Picked for you", refreshed as you play); verified.html "your account is verified" page that reopens the app (milo://verified).

Round 18: a searched song plays alone and Autoplay adds similar ones; Downloads page (Library row, Home "See all", always visible); CI prints SHA-1 fingerprints for Google sign-in; GOOGLE-SIGNIN.md rewritten as a step list.

Round 19 (UX): Search has a clear button, a Search key on the keyboard, recent searches, YouTube results first when phone music is hidden (and no phone-library dump on an empty search); bigger touch target on the row download icon.

Round 20 (UX): Undo bar (Liked, playlist and download removals), "Downloading N songs left" bar, pull to refresh on Home and Discover, tapping the active tab scrolls to top.

Round 21: Now Playing control card is frosted glass (cover shows through, rim light, slides over the art); mini player and queue / sound sheets use the same frosted look; the player blurs behind open sheets on Android 12+.
