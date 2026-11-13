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

Round 22: Google sign-in errors are now shown on the welcome screen (they were invisible) with clear hints for the common Supabase settings.

Round 23: Now Playing glass card no longer overlaps the art (it sits below it again, still frosted).

Round 24: sharper YouTube Music covers (cover requested at the size the screen shows: up to 1080 px instead of 544 stretched); downloaded songs save the big cover too.

Round 25 (Windows): new native Windows app in milo-windows/ (WinUI 3 / C#, not Electron): Google + email sign-in, Home, Discover, Library from folders, Now Playing with synced lyrics (LRCLIB), downloads, media keys, Smart play, installer. YouTube Music approach credited to Limusic / Metrolist as above; tags via TagLibSharp.

Round 25b: app starts even when Windows has no media components (N editions): shows how to add the Media Feature Pack instead of failing at launch.

Round 26: liquid glass on Android, inspired by quickLiquid (MIT, github.com/amarnath3003/quickLiquid; the web library, re-created natively, no code copied): bottom nav, mini player and Now Playing card blur the page behind them and bend the edges like a lens with colour fringing (Android 13+; blur only on 12; the old glass look below that), a bright liquid rim, and a squish-and-wobble press on every pressable.

Round 26b: fix build (missing drawOutline import in Glass.kt).

Round 27 (Lite): new milo. Lite flavor (separate app, Android 8+): chat kept, Listen together replaced by sharing your EQ preset, Cast / widgets / visualiser / liquid glass left out, ARM-only; thumbnails fall back safely below Android 10.

Round 28: milo. remembers where you were: scroll positions (Home, Library songs/albums/artists/playlists, album and artist pages, Discover, Search, Profile) and the Discover and Search results survive opening an album or artist and coming back; opened YouTube pages are kept so going back is instant.

Round 28b: fix crash "Key mxPicked for you was already used" on Home (overlapping mix builds stacked duplicate mixes; mix list is now built from its own run and keyed by position).

Round 29 (Windows): system tray with close-to-tray, start with Windows, ListenBrainz scrobbling. Feature ideas come from Limusic (GPL-3.0, github.com/SimoHypers/limusic); no Limusic code is used, so milo. for Windows keeps its own licence.

Round 30 (Phase 1 hardening): safer cloud sync (merge instead of overwrite when both sides changed, local backup before replacing, empty cloud copy never wipes a library), first unit tests plus a CI test step, feature inventory and device test checklist in docs/.

Round 30b: "Forgot password?" on the log-in screen sends a password-reset email.

Round 30c: large-library scan work moved off the main thread plus a 20,000-title sort test; "Sign out of my other devices" in Profile.

Round 31 (Phase 2): Android Auto support. The playback service is now a Media3 MediaLibraryService with a browsable library (recent, liked, playlists, albums, artists), car search and voice play. Untested on a head unit.

Round 32 (Phase 3): Download Manager screen over the existing YouTube offline downloads (queue states, pause/resume, retry with automatic retries, Wi-Fi only, data-saver quality, storage limit with optional remove-oldest, free space, unfinished-file cleanup). The download format and playback path are unchanged.

Round 33 (Phase 4): on-device smart mixes (Smart mix, Daily rotation, Forgotten favourites, Discovery, mood mixes from genre/year/title), skip tracking, More like this / Don't recommend controls, Taste profile dashboard with switch and reset. No outside AI service.

Round 33b: build fix for Round 33 (missing icon imports; sync helper made suspend).

Round 34 (Phase 5, first slice): milo. Connect for your own devices: device list, remote control, transfer of online songs, Row Level Security tables (supabase_native_connect.sql). Rooms and collaborative playlists follow.

Round 34b: crash fix. Smart mixes sorted with random scores inside the comparison, which Java rejects on libraries of 32+ songs ("Comparison method violates its general contract"). Each song is now scored once before sorting; a 800-song regression test added.

Round 34c: build fix (duplicate `signedIn` property removed from MainViewModel).

Round 35 (Phase 5): milo. Sessions, listening rooms with invite code/link/QR, roles, shared queue with suggestions, synced playback with the host, reactions, moderation and reporting. All server access goes through permission-checking database functions (supabase_native_sessions.sql). QR drawn with ZXing core (Apache-2.0).

Round 36 (Phase 5): shared (collaborative) playlists with owner/editor/viewer roles, invite code/link/QR, public or private, add/remove/reorder, change history, copy to library. Permission-checking database functions only (supabase_native_playlists.sql).

Round 36b: "Recently played" row on Home (last 20 songs played, newest first, local and online), from the same history Android Auto uses.

Round 37 (Phase 6): crossfade curves and an Audio info screen (decoder support, output capabilities, track format). Most of Phase 6 (EQ, per-device EQ, ReplayGain, loudness matching, visualiser styles, full-screen visualiser) already existed.

Round 38 (Phase 7): shuffle and favourite buttons on the 4x2 widget, kept in step with the in-app Liked songs. Other widgets and lock-screen controls already existed.

Round 39 (Phase 8): focused accessibility and state polish: buttons announce as buttons, 44 dp pill minimum, labelled 48 dp symbol buttons, loading state for shared playlists. No visual redesign.

Round 40: security review (docs/SECURITY-AUDIT.md), encrypted storage of sign-in tokens and server password (Android Keystore), Supabase permission hardening SQL.

Round 41: CI release gate in native.yml. Unit tests now fail the run (reports uploaded), and a final step checks that the debug APK exists, has the right package name and a valid version code and signature, reports AAB/release/F-Droid outcomes and whether a release key is configured. Firebase distribution only runs if the gate passed.

Round 42: diagnostics (scrubbed, optional on-phone history, API/sync/perf entries) and sync deletion tracking (SyncOps) so removals no longer reappear.

Round 43: downloads keep running with the app closed (foreground service with a notification and Pause all, stops when the queue is empty).

Round 43b: fix for the Release gate. Its file paths were written from the repository root, but the step runs inside milo-native (the job's working directory), so it looked in milo-native/milo-native/... and reported the APK and AAB as missing even though every build step succeeded.

Round 44: Connect pairing across accounts (short-lived code or QR, restricted commands, revocable) and an in-app QR scanner for rooms, shared playlists and pairing.

Round 45: Firebase-ready native app: push notification registration (full flavour only, built only when google-services.json is present), existing milo-push server reused, duplicate-free with the 15-minute check, CI secret step, docs/FIREBASE-NATIVE.md.

Round 46: web-check.yml workflow (drift, syntax, manifest checks, no deployment) and docs/IOS-FEASIBILITY.md.

Round 47: milo-push now requires a WEBHOOK_SECRET header, so only your own database webhooks can trigger notifications (before, anyone with the public anon key could send fake ones). PUSH.md updated.

## Round 48 — background / Bluetooth playback stutter
- Audio buffer raised (min 600 ms, max 1.5 s) so short stalls (calls, app switches, notification shade, screen wake) are absorbed instead of heard over Bluetooth.
- Spectrum tap no longer copies samples unless a visualiser screen asked within the last 1.5 s (no background work).
- Underruns are logged to Diagnostics (tag "audio") with buffer size, gap, speed and skip-silence state.
- Not verified on a device from the sandbox.

## Round 49 — background playback, second pass
- Wake mode changed from CPU-only to CPU + Wi-Fi lock (`WAKE_MODE_NETWORK`, adds `ACCESS_WIFI_STATE`) so streamed songs keep their connection when the screen is off or the app is in the background.
- Honest note: nothing in the app speeds playback up, so the catch-up effect is the audio output (Bluetooth stack/phone power management) recovering from a stall. Needs a Diagnostics report ("audio underrun" lines) to tell which.

## Round 50 — Bluetooth compatibility mode
- First real Diagnostics report (Samsung SM-A336E, Android 16) showed audio underruns in clusters right at the disruptions, with the 600 ms buffer in place and feed gaps of only a few ms. That points at the output path (Bluetooth/phone audio stack), not the app being too slow.
- New switch in Profile: "Bluetooth compatibility mode" (restart milo). It plays through a plain audio sink with hardware audio offload, no EQ/balance/mono/spectrum/speed/crossfade. Meant as an A/B test: if it fixes the stutter, the cause is confirmed; if not, it is outside the app's audio path.
- Not verified on a device from the sandbox.

## Round 51 — pause for phone calls
- Finding from the user's tests: milo kept playing while the phone rang, and over Bluetooth the music came out fast and high-pitched (Poweramp pauses on calls and doesn't do this). Bluetooth switches from music mode to call mode during a call; playing across that switch plays audio at the wrong rate.
- Fix: the service watches the phone's audio mode every 500 ms (no permission needed). Ringing, in a call, or in an app call (WhatsApp etc.) pauses; when the call ends it resumes after ~1.5 s, only if milo paused itself and within 30 minutes. A manual pause/play during the call is respected. Logged to Diagnostics as "paused for a call".
- Not verified on a device from the sandbox.

## Round 52 — crossfade: old song "restarting" its fade
- Reported: during a crossfade the fading-out song suddenly starts fading out again. Could not be reproduced from the sandbox; three plausible causes fixed and the rest logged.
- The fade now waits for the old song's tail to be playable as well as the new song (previously the old song could still be loading, drop out, and come in mid-fade).
- Fade progress can never go backwards (a long main-thread stall while paused could previously push the fade start later and make the volume jump back up).
- The old song is only re-positioned if it is more than 1.5 s off (was 0.3 s); re-positioning forces a reload that sounds like a restart.
- Diagnostics now records "xfade" lines: fade start and wait time, the old song moving, giving up, or a song change cutting the fade short. If it still happens, that report shows which.
- Not verified on a device.

## Round 53 — crossfade: position-driven fade + trace
- User description: the fade starts, the new song then comes in "as if Next was pressed", and the old song returns to finish the fade. Not found by reading the code (curves return old/new in the right order; no other code path touches the fade).
- The fade now follows the old song's own playback position (it reaches silence exactly where that song ends); the wall clock is only a fallback. The fade also ends if the old song reaches its end.
- Diagnostics gets one "xfade" trace line every 300 ms during a fade: progress, both songs' positions, states and volumes. A single report from a bad transition will show whether the new song is loud at the wrong time, the old song restarts, or a song-change event cuts in.
- Full dual-deck spec (settings for fade-in/out/skip fade, short-track protection, source capability matrix, PCM tests) is a larger project: see the plan given in chat; nothing in it is claimed done.

## Round 54 — crossfade gap found from the trace and fixed
- The Round 53 trace (Samsung SM-A336E) showed volumes and fade progress were exactly right, but the old song's tail player sat at the same position for ~0.3-0.4 s after the swap ("old@242641" twice, the "waited 408ms" hold). So at the swap the main player was silenced instantly, the tail player took ~0.4 s to start making sound, and the old song came back afterwards: the "plays as if I clicked next, then the old song comes back" effect.
- Fix: the tail player is now started silently a little before the swap (default 450 ms) so it is already running when it takes over. The lead is learned: every swap logs "old song Nms behind, lead Mms" and adjusts the lead for the next one (saved as `xflead`, 150-1500 ms). Pausing, scrubbing back, and skipping while it is warming are handled (it follows pause/resume, is set up again after a scrub, and a skip drops it).
- Not verified by ear; the next report's "swap:" line shows how well aligned it was.

## Round 55 — crossfade follow-up
- R54 verified from the user's report: the second swap logged "old song -6ms behind" (lead 501 ms) and its old-song position moved straight away: the start-up gap is gone. Fade curves and progress were correct throughout.
- The first fade in that report was still cold (old song's position frozen ~0.5 s): the tail player was probably not ready when its first warm-up was due. It is now prepared 9 s before the swap (was 4 s).
- Another fade showed the new song (a 20+ minute track) jump to 2:05 in the middle of the fade and go to buffering: the "resume where you left off" feature for long tracks. It is now skipped when the song arrives through a crossfade.
- Not verified by ear.

## Round 56 — bouncy lyrics
- Synced lyrics: lines that aren't being sung sit slightly smaller and lower; the line being sung springs up to full size with a small overshoot (spring damping 0.38). The page follows with a spring scroll (glides to the next line and settles with a little give) instead of a fixed-speed scroll. A seek or other far jump moves straight there.
- "Bouncy lyrics" switch in the lyrics ⋯ menu (on by default, saved as `lybounce`). It is forced off when the phone's animations are turned off.
- Word highlight, translation, tap-to-seek and long-press share are unchanged. Not run on a device from the sandbox.

## Round 57 — double-tap gestures on Now Playing
- Double-tap the cover: left third = back 10 s, right third = forward 10 s, middle = play / pause. A round bubble confirms it (back/forward icon, or play/pause); repeated double-taps on the same side show the running total (20 s, 30 s...).
- Single taps, swipe sideways (next/previous) and swipe down (close) are unchanged, as are the buttons on the art.
- "Double-tap gestures" switch in the ⋯ menu (on by default, saved as `dtap`) in case of accidental taps.
- Not run on a device from the sandbox.

## Round 58 — widgets: alive, responsive, and buttons that recover
- Why they felt dead: widget state was only rewritten on song/play events, so a killed service or a reboot left "playing" stuck on screen, the progress bar never moved between events, and every button failed silently when the player service wasn't running (the queue lives inside it) or a command threw (errors were swallowed).
- Alive: a 5 s heartbeat from the player service while music plays keeps progress/"playing" fresh. A "playing" claim not refreshed for 25 s, or from before the app process started, shows as paused. The service tells the widgets when it is up/down (`alive`), reset on every app process start so it can never outlive the service.
- Buttons: with the player running they talk to it as before (connection time-out 6 s, kept open 1.5 s). With the player not running (app swiped away, phone restarted) Play/Next/Previous now open milo with a new `RESUME` action that brings the last queue back and plays it; other buttons just open milo. A command with an empty player does the same. Every failure is now logged to Diagnostics as "widget".
- Responsive: all three widgets use Glance responsive sizes (SizeMode.Responsive). Player: tiny (cover + play), narrow-tall (cover, title, play/next), slim row, full player, roomy full player (bigger cover/title). Pill: cover+play, +text and next, +previous, tall variant. Cover: play only when narrow, play+next when wider, text hidden when short. The launcher picks the layout from the size the person drags it to / their home-screen grid.
- Widget cover picture capped at 256 px (several size variants each carry a copy; the home screen refuses oversized pictures).
- Not run on a device from the sandbox (Glance layouts can only be judged on a real launcher).

## Round 59 — Firebase App Distribution, CI/CD
- Audit: `native.yml` uploaded the DEBUG APK to a hard-coded group "testers" on every push to any branch via an unpinned third-party action; `android.yml` (web wrapper) used the same `FIREBASE_APP_ID` (wrong app); Lite had no distribution and no tests; no workflow permissions, concurrency, release notes or summary.
- New: `.github/scripts/fb-release-notes.sh`, `.github/scripts/fb-distribute.sh` (APK validation, pinned firebase-tools 13.29.1, temp credentials removed on exit, failure diagnosis, summary with only real links).
- `native.yml` and `lite.yml`: triggers (PR build/test only; main = internal; `v*` = beta; `v*-rc*` = rc; manual input), `permissions: contents: read`, concurrency, version name from tag, separate `distribute` job gated on tests + signed release APK, track-specific group secrets, Lite uses its own `FIREBASE_LITE_*` secrets, Lite gets a unit-test step and a release-APK artifact. `android.yml`: now reads `FIREBASE_WEB_APP_ID`.
- Docs: `docs/FIREBASE_APP_DISTRIBUTION.md`.
- Not verified: no real Firebase upload, no invitations sent, runner tool checks untested. Release signing and the build steps themselves are unchanged.

## Round 60 — Profile spacing fix
- The Bluetooth compatibility card (Round 50) sat directly against the Home card with no gap, and its switch label ran right up to the switch. Added the 16 dp gap every other card has, and a 12 dp gap between a switch label and its switch (all switch rows in Profile).

## Round 61 — stall recovery + swap diagnostics
- From the user's logs (R54/55 build): at each swap the old song is aligned (11-12 ms) and the fade is smooth, but the old-song player's position barely moves for ~0.3-0.65 s right as the fade starts (old@138921 on three lines; old@207021 to 207034), i.e. the old song stalls just as the new one starts: the "minimal but noticeable cut". Cause not yet known (the trace cannot tell a stall of the old player from the output stalling). The fade-start trace now also logs both players' isPlaying/playWhenReady, and the old-song player logs its own underruns and isPlaying changes ("xfade" lines).
- The speed-up after a stall: the audio queued in the output while the app/phone was busy plays back in a rush. New "Skip interrupted moments, don't rush" switch (Profile, on by default, saved as `resync`): after an underrun on the main player, milo waits 150 ms and restarts the output at the current position, throwing away the stale queued audio. Not during a crossfade, not for live radio, at most once per 8 s; logged as "stall: restarting the output...".
- Experimental: could not be tried on a device; if it causes a click on every app switch, turn the switch off and tell me.

## Round 62 — back to the default audio buffer
- Report from R61: at the swap the old song's player says it is playing (isPlaying true, playWhenReady true, READY) yet its position stays put for ~0.6 s right as the new song's output starts; the old player logs no underrun, so it is the output not consuming it. 0.6 s equals the 600 ms audio buffer introduced in Round 48 (which did not fix the original complaint). Buffer sizing is back to Media3's default so the next trace shows whether the freeze shrinks or goes. An experiment, not a proven fix.
- Stall recovery from R61 fired as designed (3 underruns, a restart ~150 ms later, logged "stall: restarting the output"). Whether the rush is gone is for the listener to judge.
- The "cut short by a song change" line also appeared for ordinary gapless changes (reason 1) when no crossfade was running; it now only logs when a crossfade was actually cut.

## Round 63 — Y2K mode, part 1 (Aqua chrome)
- New switch in Profile ("Y2K mode"); off by default, so the normal look is untouched.
- On: aqua gel panels (glossy top half, bright rim), deep-blue panel tint (song colour still shows through), aqua accent and ice-white text, chrome slider thumb and play button.
- Now Playing: the cover becomes a spinning CD (iridescent sheen, hub, spins up/down with playback, no frames drawn while paused) over a darkened copy of the art; an LCD strip (monospace, scan lines) shows time, quality, title and artist.
- Not compiled or seen on a device here: judge the look on the phone and send a screenshot to tune it.
- Later ideas: sound pack/chime, buddy list with now-listening, mixtape/CD-R playlist look, plastic widget skin.

## Round 64 — Spider mode
- New switch in Profile ("Spider mode"); off by default; turning it on turns Y2K off (and the reverse).
- Pitch-black panels, bone-white text, blood-red accent; red-edged glass rims.
- Webs: drawn in the top-left and bottom-right corners of every screen, and tucked into the top-right corner of every panel.
- Now Playing: art sinks into a black/red vignette with a web across it; a spider hangs on a thread from the top and rocks while music plays (idle when paused).
- Sliders and the Now Playing scrubber: red thread for the played part, a little spider as the thumb (black widow hourglass).
- Everything is original drawing code (no character artwork). Not compiled or seen on a device here.

## Round 65 — Sticker mode
- New "Sticker mode" switch in Profile (off by default). Y2K, Spider and Sticker are mutually exclusive (Themes.apply).
- Black-and-white sticker-sheet look: white text/accent, near-black panels with 2 dp white outlines and a sparkle in each panel corner, faint sparkles, orbit rings and a chequered strip scattered over every screen.
- Now Playing: cover art printed black-and-white, sparkles, and an original doodle buddy that bobs and twitches its ears while music plays; scrubber/sliders use a white sparkle thumb.
- All artwork is drawn in code and original; the reference mascots/characters were not copied.
- Not compiled or seen on a device here. Possible later preset: light "paper" version (black ink on white).

## Round 66 — Nebula mode
- New "Nebula mode" switch in Profile (off by default); Y2K, Spider, Sticker and Nebula are mutually exclusive (Themes.apply).
- Deep violet backdrop with a purple-and-teal marble made of drifting wave bands (they move only while music plays, ~12 updates/s), magenta/indigo glows, twinkling blue/pink glitter at the top and curly white-outlined clouds.
- Panels: neon cyan-to-magenta outline over a violet wash; accent cyan; sliders and scrubber get a glowing orb thumb and a cyan-to-magenta fill.
- Now Playing: art washed in violet/magenta; an original astronaut helmet with a glowing cyan visor and glittering magenta glass floats in the corner while music plays.
- All artwork is original drawing code; the reference skeleton, cloud-pattern and astronaut artwork were not copied. Not compiled or seen on a device here.

## Round 67 — Chrome mode
- New "Chrome mode" switch in Profile (off by default); Y2K, Spider, Sticker, Nebula and Chrome are mutually exclusive (Themes.apply).
- Liquid-silver look: black base, silver text/accent; panels get a polished-metal rim (banded light/dark gradient) with two chrome sparkles in the corner; sliders/scrubber are chrome tubes with a glossy chrome ball thumb.
- Whole-screen scatter of chrome sparkles, tilted orbit rings, a heart and a planet (static, faint).
- Now Playing: cover art goes silver and dark; a chrome planet bobs and sparkles pulse while music plays.
- All shapes are original drawing code in the generic Y2K-chrome style; no brand logos or marks (the requested "chrome hearts" name is a real brand, so the theme is called Chrome). Not compiled or seen on a device here.
