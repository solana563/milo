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

## Round 68 — Classic player mode (click wheel)
- New "Classic player mode" switch in Profile. While on, MainActivity shows ClassicApp instead of MiloApp: a small screen with a plain menu and a click wheel; nothing else of the app is shown (no Home, Discover, friends, recommendations).
- Menu: Music (Playlists, Artists, Albums, Songs), Search (your songs + YouTube Music when online), Downloaded, Now Playing, Shuffle Songs, Settings. Only songs you downloaded, songs on the phone and search results.
- Wheel: drag a finger round it to scroll the list (haptic tick per step; volume on Now Playing); tap the rim for MENU (back), previous, next, play/pause; tap the middle to choose. Rows can also be tapped. Back gesture also goes back.
- Now Playing: cover, title/artist/album, progress bar (tap to seek) with elapsed/remaining.
- Settings inside: Back to milo. (leave the mode), Silver/Black body, wheel ticks on/off, Shuffle, Refresh library.
- Silver/black body and wheel are drawn in code. No Apple logos or names are used in the app.
- Not compiled or tried on a device here. The YouTube download/offline path is untouched.

## Round 69 — Crossfade rewritten as two players (like the web app's two decks)
- Why: the web app's crossfade never moves the old song: it keeps playing on its own deck while the next song, already loaded on the other deck, fades in. The native version moved the main player to the next song and rebuilt the old song's tail on a second player, which is what caused the cut/jump at the swap (the second player's position sat still for 0.3-0.65 s in the logs).
- Now: the OLD song stays in the main player, untouched, and fades out. The NEXT song is prepared on the second player (paused) well ahead, started silently about 0.7 s early (the lead is learned) and, once it is really playing, fades in. The fade follows the old song's own position, so the old song reaches silence exactly where it ends.
- Hand-over: when the old song is done the main player moves to the new song at the second player's position (a learned latency is added), stays silent until it is playing, then the two identical copies are blended over 350 ms and the second player is dropped. Nothing restarts and nothing is re-seeked while it is audible.
- Safe exits: skip, scrub back or a dead stream during any stage drops the second player at once; if the next song is not playing in time the crossfade is skipped for that song and the normal gapless change happens.
- What changes for the person: the title/cover/queue position switch to the new song at the hand-over (the end of the fade) instead of the start of it, because the main player is still on the old song while it fades.
- Log lines: "fade starts", "f=... old@... new@...", "hand-over", "main player ready, Nms behind", plus second-player underruns. Please send one Diagnostics report after a crossfade.
- Not compiled and not heard on a device here. The old version is kept as a backup at /tmp only; the previous round zip (round62) has it.

## Round 70 — crossfade fixes from the first two-player report
- Bug 1 (found in the log): the learned "start lead" measured the wrong thing (time until the fade was due, not time until the new song made sound), so it grew to its 1.5 s cap on every fade. The new song was running silently for ~1.3 s before the fade began, so its first 1-2 s were lost. Now the lead is learned from the moment the second player is really playing (position moving), stored under a new key (xflead2, default 600 ms), and the fade only starts once that has happened.
- Bug 2: the final 350 ms blend between the two identical copies of the new song used equal-power gains, which add up to +3 dB in the middle for identical audio (a small swell). It now uses a straight (linear) blend.
- The report showed the hand-over itself is accurate (the main player ends up 20/-63/-32 ms from the new song and is ready within the same second); the "main player ready" log line now also says how long that took.
- The report showed normal playback rates (old and new positions advance in step with the clock) and no underruns during the fades, so the remaining speed-up was not visible in this data: needs the moment it happens (see the chat).
- Not compiled or heard on a device here.

## Round 71 — UI refinements before publishing
- Profile: the six separate theme cards (Y2K, Spider, Sticker, Nebula, Chrome + accent) are now one "Appearance" card with a row of theme chips (Default, Y2K, Spider, Sticker, Nebula, Chrome), a one-line description of the chosen theme, and the accent colours shown only for the Default theme. Classic player mode stays its own card.
- Reduced motion: when the phone's "Remove animations" setting is on, the moving theme decorations (Nebula backdrop, hanging spider, doodle buddy, chrome planet, Y2K spinning CD) stay still.
- Touch targets: the per-song download button, the selection-bar close button and the accent colour swatches are now at least 44-48 dp.
- Nothing else about the layout changed. Not compiled or seen on a device here.

## Round 72 — bouncy, tactile feel + more room on every screen but Home
- The app is native Kotlin/Compose, so React Native / Flutter code was not touched. The requested spring (damping 12, stiffness 200, mass 1) is converted to Compose's damping ratio: 12 / (2 * sqrt(200)) = 0.424, stiffness 200. It lives in one place, `ui/components/Bounce.kt` (`Bounce.scale()`, `px()`, `dp()`, `offset()`), so it can be tuned once.
- Presses: `pressable` buttons go down with a stiff, instant spring and come back with the bouncy one. New `Modifier.tactile(radius)`: the card dips AND its corners round off on touch-down (radius x1.7), then both spring back; used on Home's mix and recent cards and tiles (tiles scale only, so their shadow is not cut). Track rows dip to 97.5% on touch.
- Card drags: swiping a song row sideways now springs back with the bounce instead of snapping.
- Sheets: Now Playing dragged down springs back if released early, or is thrown down the screen on the same spring and closes; its slide-out uses the bouncy spring. The Queue / Sound sheets spring up from the bottom, can be dragged down by the handle, and spring out when dismissed or when the dim area is tapped.
- Pull to refresh (Home and Discover): replaced the stock indicator with `BouncyRefresh`: rubber-band pull that follows the finger (harder the further), a bubble that drops in, scales up, pops with a tick when the pull is far enough, springs to a hold position while refreshing and bounces back up when done.
- The milo. logo now shows on the Home screen only; on every other screen (tabs and detail pages) it folds away on a spring, which gives about 89 dp more room. Screen titles lost 12 dp of top padding.
- Haptics: a tick on touch-down for tactile cards, a firmer one when a pull reaches the refresh line.
- Not compiled or tried on a device here. Springs this loose overshoot on purpose; if anything feels too wobbly, change DAMPING_RATIO in Bounce.kt (0.6 is calmer).

### Round 72 fix 1
CI error: `VisibilityThreshold` unresolved in Bounce.kt and MiloApp.kt. It is an extension property, so it needs `import androidx.compose.animation.core.VisibilityThreshold`. Added to both files.

### Round 73 — fluid player + downloads kept by source
- Full player: wavy progress bar (ripples while playing, flattens with a spring when paused or when "Remove animations" is on); light haptic tick every 2 % while scrubbing; slowly moving colour aura behind the player built from the cover's dominant colour (existing extractor, no Palette library); dragging the player down shrinks it toward the mini player and rounds its corners. SharedTransitionLayout was NOT used (see notes).
- Downloads remember where they came from (playlist / album / artist / single). Downloads screen shows them grouped, each with Play and Remove all. Older downloads appear under "Singles".

### Round 74 — crossfade title follows the new song; progress bar redone
- Crossfade: the title, cover, queue position and progress now switch to the incoming song at the START of the fade (`XFadeLive` in XFade.kt, set by PlaybackService, read by PlayerHolder). The phone's notification / lock screen still switch at the hand-over (the media session follows the main player).
- Progress bar redrawn after Android 16's wavy media control: 4 dp line, ~40 dp wavelength, 3 dp amplitude eased in/out (flat at the start and before the handle), travelling wave, gap around a vertical pill handle that narrows when held, stop dot at the end; flattens with a spring when paused. Themes keep their own handles.

### Round 75 — Android 16 media bar, Now Playing action row, Timer sheet, Liked banner
- Progress bar (default theme) redrawn after Android 16's media control: thick rounded bar in the cover's colour (softened), big round handle (grows when held), two soft see-through waves rolling over the played part that flatten when paused. Themed looks keep their own thin wave + handles.
- Now Playing: labelled action row under the lyrics handle: Queue, Output, Sound, Timer (Queue/Sound/Cast moved out of the top-right button column; sleep items moved out of the ⋮ menu).
- Timer sheet "When should we stop?": 5/10/15/30/45/60 min, "End of this song", countdown + Cancel. PlayerHolder.sleepInMs added.
- Library: "Liked songs" banner card (play / shuffle / open) above the segmented control.
- Not done: Library sort button and grid/list toggle; Explore mood hero; Profile personality card.

### Round 76 — Now Playing reworked around the new buttons
- Like / Download / Visualiser / More now live in one slim frosted dock hugging the right edge (idea from the phone mock-up's status-icon tab) instead of four separate round buttons.
- Control card: soft blurred colour glow from the cover bleeds through the glass (idea from the Veri glow cards); Queue / Output / Sound / Timer sit in a glass capsule; spacing tightened so the cover keeps more room.

### Round 77 — icon set restyled (soft solid glyphs)
- New `ui/theme/MiloIcons.kt`: 62 in-app icons redrawn as soft, solid, round-cornered glyphs (Phosphor Icons, MIT licence, © Phosphor Icons, https://phosphoricons.com), built from path data so no new library is needed. All `Icons.Rounded/Outlined.*` uses in the app were switched to `MiloIcons.*` (same names). Toggle pairs: liked = solid heart, not liked = outlined heart.
- Not changed: the app launcher icon, notification / widget / Android Auto icons (XML drawables), and the theme art (Y2K, Spider, Sticker, Nebula, Chrome), which stay as they are.

### Round 77 fix 1
CI error: SearchScreen.kt:119 used the full path androidx.compose.material.icons.Icons.Outlined.Search; the bulk rename turned it into a path that does not exist. Now points at com.ubayanda.milonative.ui.theme.MiloIcons.Search.

### Round 78 — Now Playing: every button in the dock, smaller control card
- The Queue / Output / Sound / Timer capsule under the playback controls is gone. All buttons now live in the right-edge dock: Like, Download, Queue, Output, Sound, Timer (shows time left), Visualiser, More. The dock scrolls if the phone is too short.
- Control card made smaller (play 72 dp, skip 58 dp, tighter padding) so the cover card is clearly the bigger of the two.

### Round 79 — progress bar: one thick solid wave
- Default theme progress bar is now a single thick (8 dp) solid round-ended wave in the cover's colour, a thick flat track after the handle and a round handle, to match the new solid icons. The two see-through layers from Round 75 are gone. Themed looks (Y2K family) keep their thinner wave.

### Round 80 — progress bar matches the Android 16 media bar
- Default progress bar: a thick (10 dp) pill, ONE soft solid wave (lighter tint of the cover colour, fully opaque) rolling out of the played part and easing flat at both ends, and a big round handle (20 dp, grows when held). Paused = a clean flat pill. Replaces Round 79's thin wavy stroke.

### Round 81 — "YouTube Music" is never an artist
- Cause: songs whose artist the YouTube parser could not read were given the stand-in artist "YouTube Music", so the Taste Profile counted that as a real artist.
- Fix: stand-in artist is now "Unknown artist"; album pages give their songs the album's artist and artist pages give theirs the page's artist; when a song starts playing, stats and taste use the real artist/channel YouTube reports for it. Stand-in words ("YouTube Music", "YouTube", "Unknown artist", blank) are ignored by Top artists, Recently discovered artists, artist boosts and mixes, and the old "YouTube Music" entries are removed from the taste data on start-up. Old plays stay counted as songs but no longer as an artist (their real artist can't be recovered).

### Round 82 — Now Playing dock icons made clearer
- Only the dock on the cover changed: Save (arrow-down circle), Queue (list), Output (speaker; Bluetooth / headphones when connected), Sound (faders), Timer (countdown clock, shows time left), Visual (waveform), Like and More as before. Each button now has a short caption under it. Every other icon in the app is untouched (5 new `Dock*` icons added to MiloIcons.kt).

### Round 83 — bouncy swipe to open the lyrics
- Swipe up anywhere on the control card (or tap the lyrics line): the lyrics page rises with your finger, then springs open on the same bouncy spring as the rest of the UI (damping ratio 0.424, stiffness 200), with a light tick as it opens. Let go early and it springs back down. Close it with the Back button, system back, or by pulling down the grab bar at the top of the lyrics page (springs back if you let go early).

## Round 84 — no blur
- Removed every live blur for performance: the liquid-glass shader + RenderEffect blur (bottom bar, mini player, Now Playing card), `Modifier.blur` behind the Queue/Sound sheets (now a plain dim), the backdrop recording layer, and the window blur behind dialogs/popups (dim only).
- Kept: the tiny pre-softened 48px cover bitmap (made once per song on the CPU, drawn as a plain image) and the animated cover-colour gradient.

## Round 85 — colours that follow the song
- Dominant colour: hue is now circular (reds no longer split across the wheel), 24 bins with neighbour smoothing, and only pixels near the winning hue are averaged, so a red cover gives red, not orange. Mostly grey covers stay grey.
- The app accent (buttons, icons, bar) now follows the song's colour unless a fixed accent was picked in Profile. Accent lightness range lowered (0.44–0.62) so reds read as deep red.
- Player background no longer shifts the hue by +40° (that turned red into orange); it uses a deeper shade of the same hue. The control-card glow uses a lighter shade of the same hue instead of pink.

## Round 86 — plain thick bar, lyrics swipe from anywhere
- Progress bar: wave removed everywhere (default and themed looks); the thick pill and big round handle stay, and the per-frame wave animation loop is gone.
- Lyrics: swipe up on the cover art, or on any free part of the Now Playing screen, now pulls the lyrics page up with the same bouncy spring (the control card and grab handle still work). Swipe down on the art still folds the player away.

## Round 87 — milo.'s official font
- Brand face: Figtree ExtraBold/Black Italic (SIL Open Font License; licence copy in docs/licenses/Figtree-OFL.txt). It is the same family as the app's UI font, so the wordmark and the text feel like one system.
- New `MiloBrand` font family and `MiloWordmark(size)` composable (heavy italic, tight spacing, the full stop in the accent colour). Used in the Connect screen title and the lyric share card. Body text is unchanged.

## Round 88 — official logo
- The supplied milo. logo (shape + heavy italic wordmark + amber full stop) replaces the old logo_wordmark.png as a crisp vector drawable (res/drawable/logo_wordmark.xml), so it stays sharp at every size. Used at the top of Home, the sign-in gate and the permission screen (same resource name, no code change).
- The blurred shadow copy, dark background and embedded metadata of the source file were left out (vector drawables cannot blur); the logo itself is unchanged. Source copy kept in docs/brand/milo-logo.svg.
- The old res/drawable-nodpi/logo_wordmark.png is deleted: if you copy files by hand, delete it, or the build reports a duplicate resource.

## Round 89 — lyrics page bounce
- The lyrics page now overshoots by a fixed ~72 dp and wobbles 2–3 times when it opens or springs back, whatever the distance (before, the bounce scaled with how far it travelled and could look flat). Closing just settles shut.
- Mini player untouched (still the softened cover art look).

## Round 90 — queue reorder crash
- Crash report (IndexOutOfBoundsException: Index 1, Size 0 in the queue drag): a drag was still running when the queue list changed or emptied, so the stale positions were applied to an empty list. The move is now bounds-checked, and a half-finished drag is dropped whenever the queue changes underneath it.

## Round 91 — milo. desktop, forked from Limusic
- New `milo-desktop-tauri/`: a fork of Limusic 1.4.0-rc.3 (Rust + Tauri 2, libmpv, Svelte), GPL-3.0, from the zip you uploaded. See `milo-desktop-tauri/MILO-NOTICE.md` for what changed.
- Stage 1: rebrand (name, wordmark, icons, Figtree, amber default), updater and release feeds switched off so milo. never installs Limusic builds, Ko-fi and issue links replaced by a credit to Limusic.
- The UI builds and passes svelte-check (0 errors) here. The Rust side was NOT compiled here (no webkit/libmpv in this sandbox): the new workflow `.github/workflows/desktop-tauri.yml` does it.
- Correction to Round 29: the whole repo is GPL-3.0, so the Windows app is not on a separate licence either.
- The old desktop apps (`desktop/` Electron, `milo-windows/` WinUI) stay until the new one matches them.

## Round 92 — remote client config (idea from Musike)
- Salvaged from Musike (MIT, Ernani Neto, github.com/Ernani1234/musike): keep YouTube client identities in a small JSON file the app fetches, so a breakage is fixed by editing the file, not by a new release.
- `config/streaming.json` holds the current WEB_REMIX / VISIONOS versions. `YtMusic.refreshRemote` fetches it at most every 6 hours on a background thread, accepts only short, plain version strings and a user-agent, and stores it in the existing "ymc" prefs. Switched OFF until `REMOTE_CONFIG` in `data/YtMusic.kt` is set to the raw https link of that file in your repository.
- No Musike code was copied; the design differs (it carries client versions, Musike's file lists client names).

## Round 93 — more lyrics sources (from Mochy Music)
- Salvaged ideas from Mochy Music (GPL-3.0, github.com/realidkroo/Mochy-Music, realidkroo): when lrclib has no synced lyrics, milo. now also asks BetterLyrics (word-by-word TTML), SimpMusic (by YouTube id) and KuGou, in that order. The code is written for milo. (`data/LyricsFallbacks.kt`); the endpoints and the way to talk to them follow Mochy's providers.
- Word-by-word lyrics from BetterLyrics go through milo.'s existing enhanced-LRC reader, so the sung-word highlight works for them.
- These are community services: song title, artist, length and (SimpMusic) the YouTube id are sent to them, as lrclib already gets. They can go down or change; a failure just means the next source is tried.
- Not ported yet (see the Mochy gap list in the chat): Crossmix transitions, song recognition, animated covers, artist bios, floating lyrics, TV, bedside clock.

## Round 94 — soft pause & skip (from Mochy Music)
- Pause, play and skips now ease the volume instead of cutting, from every route into the player: the app's buttons, the notification, the lock screen, headsets and the car. Idea from Mochy's TransportFadingPlayer (GPL-3.0, realidkroo/Mochy-Music); `player/FadingPlayer.kt` is written for milo.
- New "Soft pause & skip" slider in the Sound panel (0–1.5 s, default 0.3 s, 0 = off). A second press during a fade acts at once; pressing play mid-fade-out swells back up; a skip fades the old song out for half that time, changes song, then fades the new one in.
- It stays out of the way of the crossfade (no fade while two songs are mixing or while casting) and of the sleep timer / loudness matching (it is one more multiplier in the same volume line). When paused the volume is back at full, so a Bluetooth or widget resume is never silent.
- CastBridge (all three flavours) takes the new session player as an optional extra parameter so returning from a cast gives the fading player back to the session.

## Round 95 — Crossmix stage 1: song analysis (from Mochy Music)
- `crossmix/CrossmixAnalysis.kt`, `CrossmixAnalysisRepository.kt` and `StreamPcmDecoder.kt` are ported from Mochy Music's crossmix package (GPL-3.0, realidkroo/Mochy-Music), package renamed only. They decode a song through the player's own data source (so YouTube ids, saved songs and files all open) and find tempo, beats, bars, key, energy and quiet start/end. Results are cached on disk.
- `crossmix/CrossmixLab.kt` is milo's own glue: off by default, switched on in the Sound panel ("Song analysis"), reads the playing song whole and the next song's opening, and writes one line each to Diagnostics under "crossmix". It does not touch playback.
- Next stages: the planner (when and how to hand over), then the two-player beat-locked mix, then the transition effects. Each waits for your Diagnostics lines.

## Round 96 — more lyric sources and Wikipedia "About" (from Mochy Music)
- `data/LyricsMore.kt`: NetEase Cloud Music (word-timed when it has it), Deezer (anonymous login, word-timed or line-timed) and QQ Music (line-timed) added after BetterLyrics, SimpMusic and KuGou as fallbacks when lrclib has nothing synced. Where to ask and how to read each answer come from Mochy's providers/directlyrics (GPL-3.0, realidkroo/Mochy-Music); code written for milo. Results are checked against title, artist and length so a cover or live take isn't picked.
- `data/WikiAbout.kt` + `DetailScreen.kt`: artist and album pages show a short "About" from Wikipedia's summary service (tap to expand, "Wikipedia" credited, text is CC BY-SA). Idea from Mochy's utils/Wikipedia.kt.

## Round 97 — lyrics interlude dots and a popping heart (UI, after Mochy Music)
- `ui/components/InterludeDots.kt`: during an instrumental opening or a long gap between lyric lines, three dots count the gap down (lighting one by one, a gentle bob, a swell just before the next line) and the lines slide to make room. After Mochy's LyricsInterludeDots (GPL-3.0, realidkroo/Mochy-Music); this version is written for milo, without the shader glow. Gaps are found from the lines' own times with the sung length guessed from the line's length, since the lyrics carry no end times.
- NowPlayingScreen: the Like heart pops when a song is liked (Mochy's LikeBurst idea, much smaller).

## Round 98 — bedside clock (after Mochy Music)
- `ui/screens/BedsideScreen.kt`: a very dark clock screen (large time in the phone's 12/24-hour style, the date, what's playing with a thin progress line, previous/play/next, a one-tap 30-minute sleep timer). Tap the time to cycle bright / dim / very dim. The clock drifts a few pixels every minute to avoid burn-in. The screen stays on. Opened from the player's ⋮ menu ("Bedside clock") and Profile > Tools; Back closes it. Idea from Mochy's bedside mode (GPL-3.0, realidkroo/Mochy-Music, ui/player/bedside); written for milo and far smaller (no alarm, weather or tiles).

## Round 99 — lyric card looks and shapes (after Mochy Music's share studio)
- Long-pressing a lyric already made a picture to share. Now the lyrics ⋮ menu has "Lyric card look" (song colour, blurred cover, paper, black) and "Lyric card shape" (portrait, story 9:16, square). The text size fits the chosen shape. Mochy's share studio (GPL-3.0, realidkroo/Mochy-Music, ui/player/share) has a much larger editor with a word-by-word picker and animated cards; only the looks and shapes idea is taken here, the picker and animation are not.

## Round 100 — floating lyrics (after Mochy Music's floating pill)
- `ui/FloatingLyrics.kt`: while a song with synced lyrics plays and milo is not on screen, a small dark pill over other apps shows the line being sung. Drag it up or down to place it (remembered); tap it to open milo. It hides whenever milo is open. Off by default; switched on from the lyrics ⋮ menu ("Floating lyrics"), which sends you to the system "Display over other apps" page the first time. New permission `SYSTEM_ALERT_WINDOW` in the main manifest (all flavours).
- Fed by the view-model's playback updates, so it keeps working with the app in the background. Respects each song's lyrics timing adjustment. Mochy's pill (GPL-3.0, realidkroo/Mochy-Music, floating/) also has controls, word-by-word fill, themes and drop targets; none of that is here.

## Round 101 — lyrics follow-through, scroll fix, volume slider (UI, after Mochy Music)
- Lyrics page: when the page steps down one line, the lines below the sung one hold back by that step and catch up on a critically damped spring, the further down the later (about 55 ms more per row, up to nine rows). Same idea as Mochy's LyricsStagger (GPL-3.0, realidkroo/Mochy-Music); a per-row spring here, not Mochy's frame engine. Only on single-line steps and only with "Bouncy lyrics" on, so seeks and jumps still just go there.
- Fix to Round 97: an instrumental opening adds a dots row to the lyric list, which shifted the auto-scroll by one line. The scroll target now allows for it.
- Sound panel: a Volume slider for the phone's music volume (follows the hardware buttons too), as Mochy's player has volume on the player page.

## Round 102 — pages land as a cascade (UI, after Mochy Music)
- `ui/components/Cascade.kt`: when the Up next queue or the lyrics page opens, the rows rise into place from lower down and fade in, the further down the later and the further they travel (about 34 ms apart, ten rows). Rows that scroll into view later do not animate; a row being dragged does not either. After the cascade in Mochy's PlayerPageTransition (GPL-3.0, realidkroo/Mochy-Music); written for milo. Not taken: the pill the pages grow out of, fling speed carried into the open spring, the cover/title morph into the page header (all depend on Mochy's own card-deck layout).

## Round 103 — animated covers (canvas), person-chosen
- `ui/components/CanvasLoop.kt`: a short looping video can be chosen for any song (player ⋮ menu > "Animated cover: choose a video"; "remove" undoes it). It plays silently over the cover on the Now Playing screen, fades in after its first frame, pauses with the music, and is released when the song changes. The choice is remembered per song. No audio track, no audio focus, so it can never affect the music.
- Player idea from Mochy Music's CanvasArtworkPlayer (GPL-3.0, realidkroo/Mochy-Music); written for milo.
- Deliberately NOT ported: Mochy's SpotifyCanvasProvider. It signs into the person's Spotify account inside a hidden web view, lifts the session cookie and tokens, poses as Spotify's iOS app and calls Spotify's private endpoints. That puts the person's Spotify account at risk and breaks Spotify's terms, so milo does not do it. Animated covers here come only from videos the person picks.

## Round 104 — build fix (Round 93)
- `data/LyricsFallbacks.kt` line 143: `ifEmpty { continue }` is a `continue` inside a lambda, which Kotlin does not allow by default ("break continue in inline lambdas is experimental"). Replaced with a plain empty check. This was the only compile error in the Round 93–103 CI log.

## Round 105 — cumulative pack (rounds 94–104)
- CI said `Unresolved reference 'LyricsMore'`: LyricsMore.kt (Round 96) was not in the project the build ran on. This zip carries every file changed or added in rounds 94–104 in one go, so one unzip over the project root brings everything level. Also `MainViewModel` now looks the playing track up through the existing `trackById`.

## Round 106 — build fix: CrossmixBars
- CI: `CrossmixAnalysis.kt` refers to `CrossmixBars` (bar and section detection), a separate Mochy file I had not copied in Round 95. Added `crossmix/CrossmixBars.kt` (Mochy Music, GPL-3.0, realidkroo/Mochy-Music; package renamed only). I also listed every capitalised name used across the four crossmix files and checked each is defined here or imported; nothing else is missing.

## Round 107 — from your first error report
- Friends share: with no connection, the "now playing" push retried every 4 s forever (30 failed POSTs in two minutes in the report). It now waits longer after each failure (8 s, 16 s … up to 2 min) and goes back to normal after one success. `MainViewModel.shareLoop`.
- Queue drag: the report still held the old IndexOutOfBounds in the queue move. The move now removes the item, then inserts at an index clamped to the list, so it cannot go out of range whatever the drag reports. (The report's crash time was before the Round 90 check; this makes the move itself safe.)
- Song analysis, first real data (10 songs): it works and finishes in about one to two minutes per song. Tempo looked right for most; "Work" (92 bpm) read 122.8 with full confidence because dembow pulses in dotted eighths (4/3 of the beat). Added a 4/3 check in `CrossmixAnalysis.estimateTempo` (milo's own, not Mochy's). Expect "Work" ≈ 92 now; send the next Diagnostics to confirm, and to see whether any song that was right is now moved.

## Round 108 — automatic animated covers from the official music video (the Mochy way, without Spotify)
- Player ⋮ menu > "Animated cover from music video": Off / Wi-Fi only / Any network (off by default). For a YouTube song, milo looks up the song's official music video (YouTube Music lists it as the "Video" counterpart of the song), takes a silent video-only stream of about 360p, and plays it looping over the cover, fading in when its first frame is ready. A song with no official video just keeps its still cover; "no video" results are remembered for the session, while a network hiccup is retried. A video you chose yourself (Round 103) always wins. `YtMusic.musicVideoFor`, `AutoCanvas` and `CanvasLoop`.
- This is Mochy's "YouTube video background" idea (GPL-3.0, realidkroo/Mochy-Music, ui/player/YouTubeVideoLookup.kt), written for milo on milo's own YouTube Music calls. It uses data (a few MB per song at 360p, none on Wi-Fi-only over mobile data) and no account.
- Still not done, on purpose: Spotify's own Canvas clips (they need your Spotify sign-in and private Spotify endpoints; see Round 103).

## Round 109 — desktop test fix
- Desktop CI: 265 of 266 Rust tests passed; `macos_window_config_mirrors_the_base_window` failed because `tauri.macos.conf.json` still had the window title "Limusic" while the main config says "milo." (Round 91 renamed one and not the other). The macOS window title is now "milo.". The test guards the macOS copy staying in step with the main window config, so it was right to fail.

## Round 110 — Identify a song
- Idea from Mochy Music (GPL-3.0), which recognises songs through Shazam's private endpoints. milo does not use those; it uses AudD (audd.io), which offers an official API and a token the person supplies. Ten seconds of microphone audio is sent only when the person taps Listen.

## Round 111 — Turntable widget
- New home-screen widget: a record with the cover as its label and play/prev/next underneath. Idea from Mochy Music's turntable widget (GPL-3.0); drawn fresh with Glance, so it does not spin (Glance widgets can't animate). milo already had the player, pill and cover widgets.

## Round 112 — iPod skin
- New full-screen "iPod skin" (Profile > Tools): a grey-green screen with the song and a click wheel (turn for volume, middle button to scrub, ring buttons for close, previous, next, play/pause), white or black body. Idea from Mochy Music's iPod player style (GPL-3.0); written fresh for milo and far smaller. Mochy's cover-flow and menu pages are not ported.

## Round 113 — UI brought in line with the features (iOS-style glass)
- Detail (artist/album/playlist) header: a wide glass "Play" pill with round glass shuffle/download buttons, and a spaced-capitals line ("ALBUM · 10 SONGS · 30 MIN"), after the iOS music look the owner supplied.
- Bedside clock: Close, Sleep and the transport buttons now use milo's own glass surface and dim together with the clock.

## Round 114 — TV interface
- On Android TV / leanback devices milo now opens a big-screen home made for the remote: shuffle and liked pills, rows of albums and songs with a focus ring, and a glass now-playing bar with previous/play/next. Idea from Mochy Music's TV interface (GPL-3.0); written fresh and much smaller (Mochy's is about 5,000 lines with its own activity). milo is added to the TV launcher (leanback category and banner, touchscreen and leanback features optional). Phones are unchanged.

## Round 115 — Tools row on Home
- The extras were only in Profile > Tools. Home now has a "Tools" card of glass chips (Identify song, Bedside clock, Car mode, iPod skin, Downloads) right under the player; FEATURE-INVENTORY.md lists where every newer feature lives.

## Round 116 — build fix
- TvScreen.kt: `composed` is a Modifier extension and was called as a plain function (CI: "Unresolved reference 'composed'"). Fixed.

## Round 117 — Reverb
- New Reverb in the Sound sheet: on/off, eight rooms (Room, Studio, Bathroom, Plate, Hall, Cathedral, Stadium, Dreamy) and an Amount slider. The reverb design and presets come from Mochy Music (GPL-3.0, eq/ReverbSettings + eq/audio/ReverbAudioProcessor, itself after Jezar's freeverb); rewritten to plug into milo's own audio processor chain (16-bit and float, mono/stereo, soft-knee limiter).
- docs/MOCHY-GAP.md lists everything from Mochy that is done, refused on purpose, or not built yet.

## Round 118 — Mochy's crossmix engine brought in (not wired yet)
- Copied from Mochy Music (GPL-3.0, github.com/realidkroo/Mochy-Music) with only the package names changed: crossmix/CrossmixPlanner, CrossmixStyles (all the mix styles and their effect maths), CrossmixSettings, TransportMoves; playback/CrossmixDirector (plans each hand-over, arms the incoming player, tempo ramp and beat lock), TransportTransitions (the pause/play/skip fades and the effects over them), TransportFadingPlayer, audio/CrossmixTransitionAudioProcessor (the DSP: echo, rewind, tape stop, spin back, filter sweep, bass swap...), audio/CrossmixClockSink, SilenceDetectorAudioProcessor, SincResampler.
- Small changes: the planner reads timed lyrics with a small LRC reader of its own instead of Mochy's LyricsUtils; the clock sink and the fading player lose Mochy's Audio Lab / USB-DAC hooks; the enums (CrossmixMode, CrossmixLength, CrossmixTempoMatch, TransportEffect) sit in crossmix/CrossmixConstants.kt.
- Nothing calls these yet, so playback is unchanged. Round 119 wires them into the playback service (second player for the blend, the audio chain, settings) in place of milo's older ghost crossfade.

## Round 119 — catch-up pack
- CI: "Unresolved reference 'AutoCanvas'" in NowPlayingScreen.kt. The cover-video helper (CanvasLoop.kt, YtMusic.kt from Round 108) was missing in the tree being built while the newer NowPlayingScreen.kt was in. No code change: a single zip with the current version of every file changed since Round 106 so the tree is consistent.

## Round 120 — crossmix engine wired into playback
- Source: Mochy Music (GPL-3.0), CrossmixDirector hand-over flow, now driven by milo's PlaybackService (second ExoPlayer for the incoming song, transition processor and clock sink in the audio chain, promote/swap of the session player).
- New setting Now Playing > Sound > Transitions: Classic (milo's older crossfade, default, untouched) or Crossmix (length, tempo match, style, stunts, always blend, "Try a mix now", live status).
- Crossmix stands down for repeat-one, Bluetooth compat mode, and while casting. YouTube download/offline path unchanged.
- Not done yet: Mochy's pause/play/skip fades and effects (TransportTransitions) still use milo's own soft transport.

## Round 121 — crossmix analysis tuning from the A33 error report
- Report showed many "could not read … timed out after 40/60 s with 20–50 s of audio decoded" (streams read at under half real time) and two "Failed to instantiate extractor".
- Analysis thread now runs at nice +1 instead of Java MIN_PRIORITY (which starved it on a busy phone). The fixed 40/60 s limits became a stall watchdog (give up after 30 s with no new audio) under a 150/240 s cap, and the failure line now says how many seconds it took.
- Mochy's analysis itself is unchanged. "Failed to instantiate extractor" comes from the stream not delivering bytes (see "[stream …] timeout"): not changed, still falls back to a plain mix.

## Round 122 — Mochy's transport fades wired in
- Source: Mochy Music (GPL-3.0) TransportTransitions + TransportFadingPlayer, now the media session's player in place of milo's smaller FadingPlayer: pause, play and skips (app buttons, lock screen, notification, headset, car) fade, with an optional effect (echo, rewind, tape stop, fade to far, reverb wash, filter sweep) done in the audio renderer when the transition processor is available.
- "Soft pause & skip" slider now runs 0–3 s; effect chips appear under it. Skips fade out then in; a skip mid-crossmix or while casting acts at once.
- Not wired yet: "blend skips" (a skip played as a crossfade on a second player); milo's older FadingPlayer.kt stays in the tree unused.

## Round 123 — haptics
- Source: Mochy Music (GPL-3.0) HapticAudioProcessor (bass / vocal / vibe follower with kick detection) and its vibration-motor mapping (click / tick primitives on Android 11+, amplitude one-shots below).
- Now Playing > Sound > Haptics: switch, what to follow (Everything, Vocals, Vibe, Bass), strength. Off by default.
- Small changes: songs the chain carries as float skip haptics instead of failing; only the playing player buzzes during a crossmix, never the classic crossfade's tail player.

## Round 124 — player cover styles (first part of the player looks)
- Source: Mochy Music (GPL-3.0) DiscCover (record geometry for small / big / album / label discs, the vinyl and name plate, the tonearm) and TurntableArt (vinyl, arm and deck drawing). Pictures are milo's bitmaps instead of Coil requests.
- Now Playing > Sound > Cover style: Full screen (as before, default), Squared, Disc, Disc label, Big disc, Big disc with arm, Disc + album. Records spin while playing and grey a little when paused, the arm lifts on pause. Options: spin, realistic vinyl, text round the label.
- The cover is softened across the screen behind the stage; the dock, title and controls are unchanged.
- Not done: the Turntable style (its arm file needs more of Mochy's player), morphing cover and song info, cover reflection, like burst, Player Studio.

## Round 125 — cover reflection and like burst
- Source: Mochy Music (GPL-3.0) CoverReflection (the cover mirrored under itself, blurred once at 96 px and faded by a vertex-colour mesh, no live blur) and LikeBurst (the heart leaves the Like button, thumps red with a burst, floats back to white and dives home, the button bouncing as it lands; up to 4 in the air).
- The reflection shows under the Squared cover style. Its blur is Mochy's three-pass box blur on milo's bitmap.
- The burst runs from the Like button in the player dock (liking only; unliking stays quiet). The heart is milo's own heart glyph; Mochy's haptic cues use gentler Compose-compatible ones.
- Left out for now: double-tap on the cover to like (milo's double-tap zones already seek/play), and Mochy's "Added to Library" label.

## Round 126 — build fix for rounds 123–125
- CI: PlaybackService.kt:557 (preference key is a nullable String: safe call), DiscCover.kt (GraphicsLayerScope has no colorFilter on this Compose version: the pause tint is now the Image's colorFilter; a leftover Mochy font reference removed).

## Round 127 — lyric readings (romanisation)
- Source: Mochy Music (GPL-3.0) LyricsUtils romanisers and tables: Japanese (kuromoji tokenizer, kana to romaji with small tsu and long vowels), Chinese (TinyPinyin), Korean (hangul rules with final-consonant linking), Russian, Ukrainian, Serbian, Bulgarian, Belarusian, Kyrgyz, Macedonian, Hindi and Punjabi. Moved into data/Romanizer.kt.
- Lyrics page: a reading line under every line written in one of those scripts, above the translation line. Lyrics menu > "Readings (Latin letters)" turns it off. Latin lines get nothing.
- New dependencies: com.atilika.kuromoji:kuromoji-ipadic:0.9.0 (the Japanese dictionary adds several MB to the app) and com.github.promeg:tinypinyin:2.0.3.
- Not done: ruby (furigana) over words, per-word readings on the word-by-word highlight, per-language switches, the AI/DeepL translation providers.

## Round 128 — Mochy's lyric line (glow style), first part of the lyrics engine
- Source: Mochy Music (GPL-3.0) LyricsGlowLine (karaoke sweep along the sung letters, letter motion with rising words and swelling held notes, AGSL glow and wave on Android 13+, blur of lines not sung on Android 12+, duet alignment, ruby / sub-line / translation slots), its AGSL shader plumbing, lyric models (LyricsEntry, WordTimestamp, RubyToken) and the motion style enum. Files: ui/lyrics/LyricsGlowLine.kt, LyricsModels.kt, AgslShader.kt, LyricsGlowSupport.kt.
- Lyrics page > menu > "Lyric style: Glow (Mochy)" (off by default; Classic is milo's own line). With it on, the reading and the translation lines sit in the glow line's own slots; "Blur lines not sung" switches the blur.
- Small changes: Mochy's app-wide animation settings are replaced by one small settings object; its repaint-boundary helper is a no-op in milo. Mochy builds on Compose 1.10, milo on 1.7, so some calls may need adjusting after CI.
- Not yet: the mini-lyrics ticker (it uses this line's sweep), Mochy's ruby over words, glow style for unsynced lyrics.

## Round 129 — build fix: TinyPinyin coordinates
- CI: "Could not find com.github.promeg:tinypinyin:2.0.3". The group is spelled `promeG` (capital G) and the library was only published to JCenter, so it is read from the Aliyun public mirror (as Mochy does), limited to that one group. Maven Central stays the source for everything else.

## Round 130 — build fix: Chinese readings use pinyin4j
- TinyPinyin could not be fetched from any repository CI can reach (JCenter is gone, the mirror did not have it). Chinese readings now use pinyin4j (com.belerweb:pinyin4j:2.5.1, on Maven Central), set to toneless pinyin with v for ü, the shape Mochy's output had. The Aliyun mirror added in round 129 is removed again. Mochy's rules for Japanese, Korean, Cyrillic, Hindi and Punjabi are unchanged.

## Round 131 — build fix: glow lyric line
- CI: LyricsGlowLine.kt needed Mochy's LyricsSteadyWrap (the bold line-break helper, now ui/lyrics/LyricsSteadyWrap.kt) and an opt-in for combinedClickable on this Compose version; the "Unresolved reference 'it'" at line 429 came from the first one.

## Round 132 — build fix: duplicate packaging notes
- CI compiled Kotlin and passed unit tests, then stopped at "2 files found with path META-INF/CONTRIBUTORS.md" (kuromoji-ipadic and kuromoji-core both carry it). Those notes (CONTRIBUTORS.md, LICENSE.md, NOTICE.md under META-INF) are left out of the APK; licences stay in CREDITS.md.

## Round 133 — lyrics ticker
- `ui/lyrics/MiniLyricsTicker.kt`: port of Mochy Music's `MiniLyricsTicker` (GPL-3.0, github.com/realidkroo/Mochy-Music): slot logic, rolling line changes, sideways slide following the karaoke sweep, edge fades, interlude dots. Tall-card mode (readings + upcoming list) not ported.
- Opt-in (Sound > "Lyrics ticker", off by default); replaces the one-line preview under the controls on Now Playing. Mini player unchanged.

## Round 134 — cover + song info morph (superseded by Round 137)
- First version, drag-down only.

## Round 135 — Turntable style
- `ui/player/TurntableArm.kt`: port of Mochy Music's `TurntableArm` (GPL-3.0, github.com/realidkroo/Mochy-Music): the tonearm follows the song along the record and is its seek bar (hold and swing it; arc + time labels; haptic ticks every 10 s). Player hooks replaced with providers; stream-failure arc colouring omitted.
- `CoverStage.kt` gets the Turntable layout (slab, record running off the left edge, arm); style now offered in the cover picker.

## Round 136 — loudness + ReplayGain rules
- `player/audio/LoudnessAudioProcessor.kt`: port of Mochy Music's `LoudnessAudioProcessor` (GPL-3.0, github.com/realidkroo/Mochy-Music): stereo-linked compressor, boost, soft-knee peak limiter under -0.5 dBFS; settings in `LoudnessFx` (prefs loud_on/loud_boost/loud_comp). Last of the EQ/reverb effects in the chain.
- `PlaybackService.gainOf`: Mochy's ReplayGainTags rules (track/album gain with fallback, preamp, never past the file's peak, never boosted). YouTube loudness matching unchanged.
- Sound sheet: new "Loudness" section (switch, boost, compression, live gain-reduction readout, ReplayGain track/album + preamp).

## Round 137 — sheet morph (Mochy's MorphingCover)
- `ui/player/MorphCover.kt`: port of the core of Mochy Music's `MorphingCover` (GPL-3.0, github.com/realidkroo/Mochy-Music): one sheet progress p (0 mini player, 1 open) moves the cover (Mochy's coverGrowth / coverTravel late-grow for non-edge styles), the title and the sheet's own backdrop (grows out of the mini bar's top, sides and corners). Driven by the open spring, by dragging down, and by closing (back button/arrow/drag past the line), so the player opens out of the mini player and folds back into it.
- `NowPlayingScreen` no longer slides/scales on drag; `MiloApp` no longer slides the player in/out; `MiniPlayer` reports its bar rect. User allowed changing the mini player for this.
- Not ported yet: play/skip buttons flying between mini and full, video/canvas/liquid-warp layers, cover shrinking into the lyrics header, reflection layout.

## Round 138 — wordmark everywhere + rest of MorphingCover
- Wordmark: the app's header, sign-in/gate screens, permission screen, TV screen and "milo. Sessions" title now use the text wordmark `MiloWordmark` (Figtree Black Italic, amber full stop) that Connect and the share card already had, instead of the old `logo_wordmark` drawable.
- `ui/player/MorphCover.kt` (Mochy Music's MorphingCover, GPL-3.0): the play and next buttons now fly between the mini player and the open player; the cover flies into the lyrics page's header as the page rises (`LyricsCoverFlight`).
- `ui/player/LiquidWarpBackdrop.kt`: port of Mochy's `CoverGradientBackdrop` liquid warp (one AGSL pass on API 33+, a zoomed still picture before). Opt-in: Sound > Backdrop. Off by default.

## Round 139 — gapless albums
- Mochy Music's "gapless albums" (GPL-3.0, github.com/realidkroo/Mochy-Music, `isNextItemGapless` / `albumOf`): when the next song is on the same album, the fixed crossfade stands down so the join plays as recorded, and Crossmix gets the album names so it can play tracks that already run into each other straight through (`PlaybackService.albumAt` / `nextIsSameAlbum`). Switch: Sound > Crossfade > "Keep albums gapless" (on by default, pref `gapless_albums`).

## Round 140 — blend skips
- Mochy Music's "blend skips" (GPL-3.0, github.com/realidkroo/Mochy-Music, `blendSkip` / `blendJump`): with the switch on (Sound > Fades > "Blend skips", pref `blend_skips`, off by default) a next/previous press, or a song picked from the queue, starts the target on the second player at once and the two cross over on a clock (length = the fade slider, 0.6 to 3 s), then the main player takes over using the existing hand-over. Back more than 3.5 s into a song still restarts it. Not while casting, mid-Crossmix, mid-crossfade or on repeat-one; if the second player doesn't make sound in 4 s it falls back to a plain jump. Not ported: prefetching the neighbouring songs' first seconds, blending a freshly picked list's first song.

## Round 141 — interruptions: audio focus the Mochy way
- Problem (from the user's tests, R59-R61): after a call or another app, music comes out fast and high-pitched. Cause found by comparing with Mochy Music's engine (GPL-3.0, github.com/realidkroo/Mochy-Music, `MusicService` handleAudioFocusChange / requestAudioFocus / callWatcher): milo handed focus to Media3, which resumes the instant focus returns, while the phone is still switching Bluetooth from call mode back to music mode; the crossmix player swap also re-requested focus each time.
- `player/PlayerFocus.kt`: the service owns focus: pauses at once on a transient loss or a call, ducks to a fifth, and resumes only when the audio mode is normal and Bluetooth is off its call link for 300 ms in a row (max 6 s), after reopening the output (stop + prepare at the same place, so the audio track is made again on the route that exists now) when the pause was over 2 s or the route changed. Also reopens the output (debounced 600 ms) when Bluetooth/wired/USB output comes or goes while playing, never during a crossfade/crossmix or a call. Logs "focus lost", "resumed (...) audio mode N, bluetooth call link X", "reopening the output" to Diagnostics.
- `PlaybackService`: ExoPlayer's own focus off on the main and promoted players while this is on; call watcher goes through it; ducking multiplies the fade volume. Profile: "Handle calls and audio focus myself (restart milo)", on by default; off = Media3's handling as before.

## Round 142: Player Studio, stage 1 (player skins)
- New `ui/player/PlayerSkins.kt`: `PlayerSkin` (Flagship, Poster, Timeline, Lyrics, iPod, Bedside), the saved choice (`pskin` in milo_fx), `PlayerSkinHost` (routes the open player to the chosen skin) and `PlayerStudio` (the picker sheet with a still card per skin).
- Poster, Timeline and Lyrics are drawn for milo on its own player state; iPod and Bedside reuse milo's existing screens. Idea and names from Mochy Music's player styles and Player Studio (GPL-3.0, github.com/realidkroo/Mochy-Music, ui/player/styles, ui/player/studio). No Mochy drawing code was copied; the Studio editor (moving and restyling parts) is not ported yet.
- `MiloApp.kt` opens the player through `PlayerSkinHost`; Now Playing's ••• menu has "Player Studio (skins)".

## Round 143: build fix
- NowPlayingScreen.kt: the lyrics-flight block had landed inside `estimateWords`; moved it next to `SheetMorphLayer` in the main body. Added the missing `statusBars` import.
- MorphCover.kt (R138 version, with `fullArt`, `lyricsArt`, `LyricsCoverFlight` and the longer `CoverMorphOverlay`) resent with MiniPlayer.kt and MiloApp.kt: the CI log shows an older MorphCover.kt in the repo.

## Round 144: Player Studio, stage 2 (Customize tab)
- PlayerSkins.kt: `SkinStyle` (saved, live) and a Skins/Customize switch in the Studio sheet. Customize changes Poster, Timeline and Lyrics: text size, cover circle size (Poster), colour (cover/amber/white), gradient or black backdrop, seek bar on/off, controls on/off, centred words (Poster), Reset. The sheet's dark scrim is removed on this tab so changes show behind it.
- Idea from Mochy Music's Player Studio edit mode (GPL-3.0); this is a much smaller milo-side editor, not a port of its drag-and-arrange editor.

## Round 145: Player Studio, stage 3 (presets)
- PlayerSkins.kt: four presets (Midnight, Mono, Loud, Quiet) that set every Customize option at once, and a "Song title and artist" switch for Poster and Lyrics.

## Round 146: Gestures settings
- New `ui/player/PlayerGestures.kt` and a Gestures card in Profile: double-tap on/off, middle double-tap (play/pause or nothing), side double-tap jump (5/10/15/30 s), swipe-to-skip on/off and swipe distance. NowPlayingScreen reads these when a gesture happens. milo's own settings; Mochy has no standalone gestures screen, only adjustable gestures through its components, so this is not a port of a specific file.

## Round 147: playlist covers and the Downloads arrangement
- New `ui/components/CoverCollage.kt`: a playlist's cover made from its songs' art (1, 2 side by side, or a 2 x 2 grid; one cover per album). Used on the Library > Playlists list. Idea from Mochy Music's playlist covers (GPL-3.0); written for milo.
- New `ui/screens/DownloadsScreen.kt`: Library > Downloads now keeps saved songs the way they were downloaded: Playlists, Albums, Artists (each with a collage cover, song count and size; tap to open just those songs) and Singles (songs saved one by one). Tabs show only the kinds you have. The Download Manager already grouped the same way; Home's Downloaded card and Library's Downloads row now say what is inside ("2 playlists · 3 albums · 5 singles", from the new `OfflineStore.summary()`).
- Songs downloaded before groups existed have no origin and show under Singles.

## Round 148: importers (CSV and link)
- New `util/CsvPlaylist.kt`: reads a playlist exported as .csv (finds song/artist columns by header name, handles quotes, `,` or `;`); Library > Playlists > Import a playlist now takes .m3u or .csv and matches songs on the phone by title and artist.
- New `util/PlaylistLink.kt` + `MainViewModel.importPlaylistLink`: "Import from a link" takes a public YouTube / YouTube Music playlist (or album playlist) link and makes a milo playlist of its songs. Radio mixes, Liked music and Watch later need sign-in and are refused with a message. No sign-in or session is used.
- Idea from Mochy Music's CSV and link importers (GPL-3.0); written separately for milo. Spotify links are not handled (Spotify's playlist API needs a login; see docs/MOCHY-GAP.md).

## Round 149: fix, stuck on the Bedside skin
- PlayerSkins.kt: the Studio button was hidden on the Bedside skin, so once it was chosen there was no way back to another skin. It now shows there too (faint, top right).

## Round 150: importers find missing songs on YouTube Music
- `MainViewModel.importEntries` (+ `ImportResult`): playlist files (.m3u or .csv) now match songs on the phone first, then look up the rest on YouTube Music (up to 200, four at a time). A result is only used when its title matches closely (75% of its words) and its artist agrees when the file gave one, so a wrong song isn't slipped in. The toast says how many came from the phone, from YouTube, and how many weren't found. With no connection the look-ups fail quietly and those songs count as not found.
- Works with Spotify playlists exported to CSV by a tool such as Exportify: no Spotify login or session is used inside milo.

## Round 151: import history
- New `data/ImportLog.kt`: the last 15 imports (file or link) with where the songs came from and the songs not found (first 100). Library > Playlists > Import history shows them, with Clear. Kept on the phone only.

## Round 152: playlist header collage and descriptions
- Your own playlists now open with the collage of their songs' covers across the top (CoverCollage got `singlePx`/`tilePx` for the larger size), and carry a short description (new `data/PlaylistNotes.kt`; tap the line under the header to edit). DetailScreen gained `collage`, `note` and `onEditNote`. Ideas from Mochy Music's playlist covers and playlist descriptions (GPL-3.0); written for milo.

## Round 153: song edit, lyrics timing and your own lyrics file
- New `data/SongLyricFx.kt`; `LyricsRepo.get/rescan` apply, per song, a timing offset (-120 s to +180 s) and a lyrics file you choose (.lrc with times, or plain text), which replaces the found lyrics. New `LyricsFixDialog` (⋮ menu > "Lyrics: timing and file") on every song, YouTube or phone. Nothing in the audio file changes.
- These are two of the pieces of Mochy Music's song edit sheet (GPL-3.0, ui/menu/songedit); title/artist/album editing already existed in milo. Not ported: choosing a cover from sources or a picture, per-song speed and EQ, video picker, artist picker.

## Round 154: your own cover for a song
- New `data/SongCover.kt`; `ArtLoader.cover/soft` check it first; Cover/rememberCover/rememberSoft reload when it changes. Song ⋮ menu: "Change cover" (pick a picture; scaled to 1200 px) and "Use the original cover". The picture is kept inside milo; the song file is unchanged. The lock-screen and notification artwork and other places that don't go through milo's cover loader still show the original.
- From Mochy Music's song edit sheet cover picker (GPL-3.0); milo's version takes a picture you pick, with no cover search.

## Round 155: recap story
- New `ui/screens/RecapStory.kt`; Profile > Your listening > "Watch my recap". A full-screen story built from what milo counted on this phone: intro, time listened (with a count-up and "about N albums"), plays (and per day for a year), your number one song, top songs, top artist with bars, and a share card. Tap right for next, left for back; it also advances by itself every 6.5 s. Share sends the same plain text as before.
- Idea from Mochy Music's recap story and ID card (GPL-3.0, ui/screens/recap); the pages and visuals are milo's own. Not ported: genre/mood pages, listening-time-of-day, the card's back side, picture export (needs more data than milo records).

## Round 156: What's new
- New `data/WhatsNew.kt` (hand-written list, bundled) and `ui/screens/WhatsNewSheet.kt`. It shows once after an update (build number goes up; a fresh install doesn't show it) and any time from Profile > About > What's new. Profile also shows the version and build. No update download or check: milo has no update server, and distribution (Play, F-Droid, test builds) handles installs.
- Idea from Mochy Music's update and changelog sheets (GPL-3.0, ui/screens/update); the update-check part was not ported.

## Round 157 — Profile as a hub of categories; Tools moved off Home
- Home no longer has the Tools shelf. Identify song, Car mode, Bedside clock and iPod skin now live under Profile > Tools.
- Profile is now a list of categories, each opening its own page (back returns to the list): Your listening, Connections (connect, sessions, shared playlists, QR scan, push, together, sharing), Accounts and sources (YouTube key, scrobbling, server), Playback and sound (autoplay, classic, gestures, Bluetooth, audio info), Look and layout, Downloads and backup, Tools, Help and about, Account (signed-in only).
- No new third-party code. Downloads stay reachable from Library and Home's Downloaded card.

## Round 158 — Threads-style pull to refresh
- New pull indicator (hand-drawn, no icon): a ring of dots that appear as you pull, turn with the finger, pop when armed, then chase round while loading. Idea inspired by how the Threads app feels; the mark is milo's own and copies nothing from it.
- Pull to refresh added to the Library songs list (rescans the phone library). Home and Discover already had it and now use the new mark.
- R158b: SongCover.set changed to a block body (CI: 'Returns are prohibited for functions with an expression body').

- CI status (2026-11-23): `assembleFullDebug` is green with Rounds 148–158 and the 158b SongCover fix. Compile only; device behaviour of those rounds is still tested via docs/DEVICE-TEST-CHECKLIST.md.

## Round 159 — Mosaic Home and pixel headings
- Direction taken from a mockup of a phone home screen (wide widget, two big colour tiles, a grid of small tiles on black). Layout and colour-block idea only: all icons, colours and lettering here are milo's own, nothing copied from it.
- Home: weekday in pixel lettering with search/friends/you; a stats widget (today's minutes, day streak, last seven days as pixel columns; tap opens Profile); two big tiles (Liked, Saved); a PINNED grid of 4-up tiles (shortcuts and playlists, chosen with Edit) and a RECENT grid (last played albums). The player card and the shelves below are unchanged.
- The Tools shelf lives in Profile (R157) and is now also pinnable on Home as Identify, Car mode, Bedside, iPod.
- PixelText: a 5x7 font drawn in code (no font file). Screen titles and shelf titles use it; titles with letters it doesn't have (other scripts) stay in normal type.
- DayStats records listening time per day (starts from first run of this version); HomePins stores the pinned tiles.

## Round 160 — Mosaic Home, clean version
- Feedback on R159: no colour blocks, no pixel lettering. Removed the pixel font (PixelText.kt deleted; delete it from your repo too) and restored normal screen/shelf titles (Rows.kt back to its R158 form).
- Home keeps the mosaic layout in milo's own glass style: slim header, Now playing tile first, listening widget (minutes today, streak, 7 slim bars), Liked songs / Saved offline tiles, Quick access grid (pin with Edit), Jump back in (recent albums).
- Every Home block now sits straight on the black background; the rounded panel cards behind the shelves are gone.

## Round 161 — Black pages everywhere
- The big rounded panel card (and its border) behind every non-Home screen is gone: Library, Discover, Search, Profile and the album/artist/playlist pages now sit on black like Home, with the glass tiles on top.
- Album/artist/playlist headers fade into black instead of the song-tinted panel colour. The page background no longer takes the playing song's tint; glass tiles, cards and the player still do.

## Round 162 — Background choices, Recently played on top, accent tiles
- New Profile > Look and layout > Background: Black, Song art (the playing song's softened cover, dimmed), Song colour (a gradient from the song's own colour into black) or My picture (any image from the phone, scaled to 1600 px and kept in the app). Draws behind every screen.
- Home: Recently played now sits directly under the Now playing tile.
- Liked songs and Saved offline tiles are filled with the accent colour (text flips dark or light to stay readable) and follow accent changes.

## Round 163 — Now playing: button strip and live lyrics
- Layout follows a mockup of stacked cards: the art with the title on top; under it a horizontal strip of buttons (Like, Save, Queue, Output, Sound, Timer, Visual, More) that replaces the slim dock on the right edge; then the controls card (scrubber and transport, now a floating rounded card); and at the bottom, on black, the lyrics.
- Bottom lyrics: the line being sung in large type with the next two lines fading below it (plain lyrics show their first lines). Tap it or drag up for the full lyrics page; the Lyrics ticker option still replaces it when on.
- The like heart still flies to the Like button (its overlay moved to the screen root so the strip doesn't clip it).

## Round 164 — Fixes from feedback on R162/R163
- Now playing: the lyrics area is now a solid black panel (rounded top, running to the bottom edge) under the controls card, instead of sitting over the frosted backdrop.
- Library: the Liked songs banner and the Liked/Downloads icon tiles no longer use the fixed orange-brown gradient. They are glass on the page background with the accent only on the icon, so they follow the app's look and accent.
- Home: Liked/Saved tiles are glass again (accent icon, not accent fill); "Recently played" is replaced by "Jump back in" (recent albums) directly under Now playing.

## Round 165 — Player buttons follow the background; Output moves; Home tiles show a blurred cover
- Now playing: the button strip uses the same frosted look as the controls card (so it follows the chosen background), every button sits on its own round backdrop, and a button that is on (liked, saved, timer running) turns solid white with a dark icon instead of the accent colour.
- The Output button moved to the top right of the art, opposite Back.
- Home: the Liked songs and Saved offline tiles show a blurred cover by default (newest liked song, newest saved song), under a dark veil. Profile > Look and layout > Background has a switch for it; with it off, or when there is no song yet, the tiles are plain glass.

## Round 166 — Now playing edge to edge
- The button strip and the controls card now run the full width of the screen like the art and the black lyrics panel (the 10 dp side margins are gone).

- CI status (2026-11-23): `assembleFullDebug` green through Round 165. Round 166 (edge-to-edge player cards) not built yet.

## Round 167 — Poster lyrics page, card-free Now playing
- Lyrics page (swipe up from Now playing) is now Poster style: accent-to-black backdrop, the cover as a great circle from the top-left (idea from Mochy Music's Poster skin), big title and artist, and a button row (Like, Save, Queue, Sound, Sleep timer) above the scrubber. Queue, Sound and Timer close the lyrics page and open their sheet.
- Now playing: the cards behind the button strip and the controls are gone (buttons keep their round backdrops), both sit lower, the black lyrics area is shorter and the song art taller; the live lyric under the controls is poster-style (big, left-aligned, two lines).
