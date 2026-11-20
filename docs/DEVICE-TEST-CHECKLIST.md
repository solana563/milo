# Physical-device test checklist (tick only what you actually ran)

Devices: ☐ Android 8–9 (Lite) ☐ Android 12 ☐ Android 14+ ☐ low-RAM phone

## Playback
- ☐ Play local song, lock screen, controls work from notification and lock screen
- ☐ Plug/unplug headphones: pauses on unplug
- ☐ Incoming call: pauses, resumes after
- ☐ Bluetooth connect/disconnect; car stereo controls
- ☐ Swipe app away from recents: music keeps playing (or stops, per setting)
- ☐ Reboot phone, open app: last queue and position restored
- ☐ Airplane mode mid-YouTube song: clear error, no crash; recovers when back online
- ☐ Crossfade on/off, EQ on/off during playback

## Library
- ☐ Library with 5,000+ songs: scroll smoothness, search speed, cold start time
- ☐ Scroll position kept when opening/closing an album and artist

## Account & sync
- ☐ Sign up, log in, log out, Google sign-in
- ☐ Use app with no account
- ☐ Phone A: edit playlist offline, phone B: edit online, reconnect A: both edits present (Round 30)
- ☐ Fresh phone signs in: library arrives; `sync_backup.json` created when replacing
- ☐ Delete account: data gone, app returns to signed-out state
- ☐ Export backup, import on another phone

## Release
- ☐ Install release build over previous: data kept
- ☐ Unit tests green in CI (`testFullDebugUnitTest`)

## Download manager (Round 32)
- [ ] Queue 10 songs: 2 run at a time, rest show Queued; Pause all / Resume all work
- [ ] Pause a song mid-way, resume: continues (progress doesn't restart from 0)
- [ ] Switch off data / airplane mode mid-download: shows "Waiting for a connection", resumes by itself
- [ ] Wi-Fi only ON with mobile data: shows "Waiting for Wi-Fi", starts when on Wi-Fi
- [ ] Data saver ON: new downloads are visibly smaller
- [ ] Limit 500 MB: stops with a clear message; with "remove oldest" ON it frees space and continues
- [ ] Force-close the app with songs queued: reopen, they are Paused, Resume works
- [ ] Airplane mode, force-close, reopen: Downloads list and playback of saved songs work
- [ ] Failed song shows reason; Retry works; auto-retry happened up to 3 times

## Smart mixes and taste (Round 33)
- [ ] Home shows Smart mix / Daily rotation after some listening; "Refresh" gives a different set
- [ ] Skip 3 songs quickly: they drop in the next refresh; Skips noticed count rises in Taste profile
- [ ] "Don't recommend this artist" removes them from mixes; Undo restores; Allow in Taste profile restores
- [ ] Mood cards appear only when genre tags exist; none shown on a library without tags
- [ ] Switch personalisation off: counters stop; Reset empties history but keeps blocked list
- [ ] Taste profile on a 5k-song library opens quickly

## milo. Connect (Round 34) - needs two devices on the same account
- [ ] Ran supabase_native_connect.sql; Connect switch on both devices: each sees the other as online
- [ ] Play on A: B shows title/artist, Play/Pause on B controls A within ~5 s
- [ ] Prev / Next / +15s / -15s work
- [ ] "Play here" on B moves a YouTube song from A with the right position, A pauses
- [ ] "Send my music there" from B: A starts the song, B pauses
- [ ] Local-only song: both transfer buttons refuse with a clear message
- [ ] Switch Connect off: device disappears from the other's list; sign out does the same
- [ ] A second account cannot see or control these devices (check in Supabase table editor / with a test user)

## milo. Sessions (Round 35) - needs two accounts on two phones
- [ ] Ran supabase_native_sessions.sql; Profile > Sessions opens
- [ ] A starts a private session, B joins by code; B joins by pasting the link; QR shown on A, scanned by B's camera opens milo.
- [ ] A plays a YouTube song: B starts the same song within ~5 s and stays within ~3 s of A; pause/seek on A follow
- [ ] A plays a local file: B sees the message that it can't be heard, nothing breaks
- [ ] B suggests a song: A sees Approve/Remove; approved song plays after the current one ends
- [ ] Open queue ON: B's songs go straight in
- [ ] Reactions and chat appear on both; spamming is refused ("slow down")
- [ ] A mutes B: B can't react/suggest; A removes B: B is told; banned B can't rejoin with the code
- [ ] Report and Block work; a third account never sees a private room
- [ ] A ends the session: B is told; A's app closed for a minute: B sees "host away"
- [ ] Public session shows in the list for C; stops showing when A ends it

## Shared playlists (Round 36) - needs two accounts on two phones
- [ ] Ran supabase_native_playlists.sql; Profile > Shared playlists opens
- [ ] Phone A creates a playlist, adds songs by search and with "Add current song"
- [ ] Phone B joins by code, by pasted link and by scanning the QR; role matches the "new members can edit" switch
- [ ] B as editor adds a song; A sees it within ~6 s; History lists who did it
- [ ] B can remove only B's songs; A can remove any; B as view-only sees no add/move/remove buttons
- [ ] Reordering with the arrows keeps the new order on both phones
- [ ] A switches B to view-only / removes B; B's buttons update, B can no longer edit
- [ ] Public toggle lists the playlist on a third account; private hides it
- [ ] Play and Copy to my library work; copy is independent afterwards
- [ ] Local-storage songs cannot be added (clear message)

## Audio info and crossfade curves (Round 37)
- [ ] Profile > Audio info lists formats; compare with a real MP3, FLAC, OGG, Opus and WAV file on this phone
- [ ] Now playing line shows format, kHz and kbps for a local song; streamed song says it is streamed
- [ ] Plug in wired or Bluetooth output: it appears in the outputs list
- [ ] Crossfade 6 s with each curve: no dip or jump in volume; equal power sounds steady, linear dips slightly in the middle

## Widgets (Round 38)
- [ ] Add the 4x2 widget: shuffle, previous, play, next, heart all fit and respond
- [ ] Heart on the widget fills and the song appears in Liked songs; liking in the app updates the widget heart
- [ ] With the app swiped away: widget heart works for a phone song; for a YouTube song it opens milo.
- [ ] Shuffle button lights in the accent colour when on; matches the Now Playing shuffle
- [ ] 4x1 and 2x2 widgets still show artwork and respond
- [ ] Lock screen: artwork, progress, previous/next on at least two phone brands

## Design and accessibility (Round 39)
- [ ] TalkBack on: pills, chips and mini-player buttons are announced as buttons with sensible names
- [ ] Shared playlist ▲ ▼ ✕ are announced as Move up / Move down / Remove and are easy to hit with a thumb
- [ ] Settings > Display > Font size at largest: Profile, Home and Now Playing don't clip or overlap
- [ ] Shared playlists screen shows "Loading…" first, then the list or "None yet"
- [ ] Pills look the same as before (only slightly taller where they were short)

## Security (Round 40)
- [ ] Update over an existing install: you stay signed in (old tokens upgrade silently)
- [ ] Fresh install: sign in, close the app, reopen: still signed in; restart the phone: still signed in
- [ ] Music server (Subsonic) still connects after the update
- [ ] After running supabase_security_hardening.sql: sign up, sign in, add a friend by username, send a message, Sessions and Shared playlists all still work

## Diagnostics and sync deletions (Round 42) - two phones, same account
- [ ] Phone A: unlike a song, sync; phone B syncs: the song is gone from B and does not come back after B syncs again
- [ ] Delete a playlist on A, sync both: it stays deleted on both
- [ ] Remove one song from a playlist on A: stays removed on B after syncing
- [ ] Like the song again on B later: it returns on A and stays
- [ ] Profile > Something not working?: switch history on, cause a failure (airplane mode, then Back up now), close and reopen, Send error report: the earlier line is there; no email or token appears
- [ ] Switch history off, or Clear the list: the saved history is gone

## Background downloads (Round 43)
- [ ] Queue 10 songs, swipe milo. away: the "Saving 10 songs" notification stays and the count goes down
- [ ] Screen off for 5 minutes: downloads keep going
- [ ] Pause all from the notification: everything pauses, notification disappears; reopen the app and resume works
- [ ] Wi-Fi only on, mobile data only: notification says Waiting for Wi-Fi; switch to Wi-Fi and it continues
- [ ] All done: notification goes away by itself
- [ ] Test on at least two brands with aggressive battery saving (e.g. Samsung, Xiaomi) and note which ones stop it
- [ ] Notifications denied in Android settings: downloads still run

## Pairing and QR scanner (Round 44) - two phones, two different accounts
- [ ] Ran supabase_native_pairing.sql; both phones signed in to different accounts, Connect on
- [ ] Phone A: Connect > Create a code (1 hour); phone B: Scan QR opens the camera, scans, fills the code; Link works
- [ ] Phone B sees A's now-playing and Prev/Play/Next move A's music; A shows a short "name: command" message
- [ ] Wrong code shows a clear message; 8 wrong codes in a row are blocked for a while
- [ ] A code works only once and not after 5 minutes
- [ ] A removes B: B's controls disappear within a few seconds and commands stop working
- [ ] Camera permission refused: message shown, typing the code still works
- [ ] Scan a room QR and a shared playlist QR from the same scanner: each opens its own screen
- [ ] The phone's own camera app scanning the QR opens milo. and fills the code

## Push notifications (Round 45) - needs google-services.json and the milo-push function
- [ ] Build with the Firebase file: Profile shows "Instant message notifications"; without it, the switch is absent
- [ ] Sign in, then check the server has an `fcm` row for the account in `push_subs`
- [ ] Close the app, send a message from another account: notification arrives within seconds
- [ ] No double notification (the 15-minute check stays quiet while push is active)
- [ ] Switch off: row disappears from `push_subs` and messages still arrive within about 15 minutes
- [ ] Sign out: the phone's row is removed; no notifications for that account afterwards
- [ ] Android 13+: notification permission prompt appears and refusing it doesn't crash anything
- [ ] F-Droid and Lite builds still compile and show no push switch
- [ ] R48: play over Bluetooth; take a call, switch apps, pull the notification shade, lock and wake the phone. No stutter or speed-up; check Diagnostics for "audio underrun" lines.
- [ ] R50: Profile → Bluetooth compatibility mode on, restart milo, repeat the call / app switch / lock-wake test over Bluetooth. Stutter gone? Report either way.
- [ ] R51: over Bluetooth, play music and have someone call you. Music pauses when it rings; if you reject or end the call, music resumes by itself. No fast/high-pitched audio. Repeat with a WhatsApp call. Pausing yourself before the call must not auto-resume.
- [ ] R52: crossfade 6 s on, play 6+ songs in a row (downloaded and streamed). Old song only ever fades down once; no jump back up. If it happens, send Diagnostics ("xfade" lines).
- [ ] R53: crossfade on, let 3 songs change by themselves, then open Profile → Diagnostics → Share and send the "xfade" lines.
- [ ] R54: crossfade on, let 4 songs change by themselves. At the swap the old song should carry on without a dip or restart and then fade out under the new song. Also try: pause during the last 10 s then resume, scrub back from the last 5 s, press Next near the end. Send the "swap:" Diagnostics lines.
- [ ] R55: first crossfade after opening milo is as smooth as later ones; crossfading into a 20+ minute mix does not jump to a saved position mid-fade.
- [ ] R56: open synced lyrics. The sung line pops up with a small bounce, the page glides after it, no stutter on long songs. ⋯ → Bouncy lyrics: off returns to the plain look. Tapping a line still seeks; seeking far jumps without a long scroll.
- [ ] R57: Now Playing, double-tap the left / middle / right of the cover: -10 s / pause-play / +10 s with a bubble. Swipes and the side buttons still work. ⋯ → Double-tap gestures: off disables them.
- [ ] R58 widgets: add all three; resize each (tiny → big). Layout changes with the size and nothing is cut off. Play music: progress bar creeps along, pause/play icon follows within a few seconds. Buttons work: play/pause, next, prev, shuffle, like. Swipe milo away while paused, then press Play on the widget: milo opens, last queue returns and plays. Restart phone: widgets show the last song as paused, not playing. Any failure shows in Profile → Diagnostics ("widget").
- [ ] R61: play over Bluetooth, switch apps / pull the shade / lock and wake. No fast-forwarded or high-pitched catch-up; at most one short restart. Switch off in Profile if it clicks. Then send Diagnostics from a crossfade: "old-song player" lines and the "playing new=/old=" part of the xfade trace show what stalls at the swap.
- [ ] R62: crossfade swap: is the small jump gone or shorter? In Diagnostics the old-song position at the start of the fade should now advance on every line (it sat still for 3 lines before). Does stall recovery still feel right with the default buffer?
- [ ] Y2K mode (Profile): switch on/off; panels, sliders, play button change; Now Playing shows spinning CD + LCD strip; CD stops when paused; off restores the old look exactly.
- [ ] Spider mode (Profile): on/off; webs in screen corners and panels; Now Playing spider hangs/rocks only while playing; scrubber and sliders show spider thumb; turning on Y2K turns Spider off.
- [ ] Sticker mode (Profile): on/off; white outlines + corner sparkles; scattered sparkles/rings; Now Playing art is B/W with buddy bobbing only while playing; sparkle thumb on scrubber/sliders; switching between Y2K/Spider/Sticker leaves only one on.
- [ ] Nebula mode (Profile): on/off; marble bands move only while playing; glitter twinkles; neon panel outlines; glowing orb thumb on sliders/scrubber; astronaut helmet on Now Playing; only one of the four themes on at a time; check battery/CPU while playing with it on.
- [ ] Chrome mode (Profile): on/off; panel rims look like polished metal; chrome ball thumb on sliders/scrubber; scatter shapes visible but faint; Now Playing art silver, planet bobs only while playing; only one theme on at a time.
- [ ] Classic player mode (Profile): turn on switches the whole app; wheel drag scrolls with ticks; rim taps = menu/prev/next/play; centre selects; Search finds phone + online songs; Downloaded plays offline; Now Playing wheel changes volume, progress tap seeks; Settings > Back to milo. returns; app restarts into the chosen mode; back gesture pops screens.
- [ ] Crossfade (Round 69, two players): fade starts with the old song continuing without a jump; new song fades in from silence; no cut/jump or speed-up at the swap; title changes at the end of the fade; skip, scrub back and pause during the fade behave; Diagnostics shows 'fade starts' / 'hand-over' / 'main player ready'; local, downloaded and streamed songs; shuffle and repeat.
- [ ] Profile > Appearance: theme chips switch themes (only one on), accent colours only for Default; with the phone's Remove animations on, theme decorations do not move; download/close buttons are easy to hit.
- [ ] Round 72 feel: buttons/cards dip on touch and wobble back; Home cards round their corners while pressed; swipe a song row and let go: it bounces back; drag Now Playing down a little: springs back, a lot: closes; Queue/Sound sheets spring up, drag by the handle, tap outside; pull to refresh on Home and Discover: bubble drops, ticks at the line, bounces; milo. logo only on Home (folds away on other screens and detail pages, content starts higher).

- [ ] R73: wave moves while playing and flattens on pause; ticks while dragging the bar; aura colour changes per song; drag player down = it shrinks and rounds; download a playlist, an album and one song, then check Downloads shows 3 groups

- [ ] R74: start a crossfade, check the title/cover change as the fade begins, scrubber follows the new song; check wave looks like the Android 16 media bar, flat when paused, handle narrows when held

- [ ] R75: progress bar looks like the Android 16 media bar (waves move, flat when paused); Queue/Output/Sound/Timer row works; Timer sheet incl. End of this song; Library Liked banner play/shuffle

- [ ] R76: Now Playing: dock on the right edge works (like, download, visualiser, more); card glow changes with the cover; capsule row; cover area not cramped on a small phone

- [ ] R77: icons look soft and solid everywhere (nav bar, player controls, rows, search, Classic mode); heart outline vs solid; nothing blank or oversized

- [ ] R78: all 8 dock buttons reachable (scroll on a small phone); timer badge shows time left; control card clearly smaller than the cover

- [ ] R79: progress bar is one thick solid wave, flat when paused, handle grows when held

- [ ] R80: progress bar = thick pill + one solid wave on the played part + big round thumb; clean flat pill when paused

- [ ] R81: play a YouTube song from search, an album and an artist page for a while; Taste Profile top artists show real names and no "YouTube Music"; old "YouTube Music" entry gone

- [ ] R82: dock icons + captions readable at a glance; all 8 reachable by scrolling on a short phone

- [ ] R83: swipe up on the control card opens lyrics with a bounce; short swipe springs back; pull the lyrics grab bar down to close; system back closes lyrics first
- [ ] R84: no blur anywhere — bottom bar, mini player, Now Playing card are flat translucent; Queue/Sound sheets dim the player; dialogs dim only. Check scrolling/animation feels smoother and text stays readable.
- [ ] R85: play a red, a blue, a green, a purple and a mostly grey song: accent, bar, buttons and player background match the cover's hue (red = deep red, never orange). If an accent was picked in Profile it stays fixed.
- [ ] R86: progress bar is a plain thick pill (no wave) in every theme; swipe up on the cover and on empty parts of the player opens lyrics with the bounce; swipe down on cover still closes the player; sideways swipe on the cover still skips.
- [ ] R87: 'milo.' on the Connect title and on shared lyric cards is heavy italic (Figtree Black Italic) with the dot in the accent; the home header logo is still the old image.
- [ ] R88: new milo. logo shows at the top of Home, on the sign-in gate and the permission screen, sharp, cream on dark with the amber dot; no old PNG logo left.
- [ ] R89: swipe up (or tap the lyrics handle): the page rises, overshoots the top a little and wobbles into place; let go early: bounces back down; pulling it closed settles without a bounce.
- [ ] R90: open the queue, start dragging a song and let the queue change under the finger (song ends, clear queue, autoplay adds): no crash, the drag just stops.
- [ ] R91 (desktop): run Actions → 'Desktop (Tauri) checks' and send me the log if it fails. Then, on a PC: build milo-desktop-tauri (pnpm + cargo tauri dev), check name/logo/amber look, search + play a YouTube Music song, About page credits Limusic, Settings → check for updates shows an error, not an update.
- [ ] R92: after REMOTE_CONFIG is set: publish config/streaming.json in the repo, install, play a song as normal (nothing visibly changes). To test: edit v in the file, wait/clear app data, search still works. A bad or missing file must change nothing.
- [ ] R93: open a song lrclib has no synced lyrics for (try a lesser-known or non-English song): lyrics now appear synced; BetterLyrics songs highlight word by word; a song nobody has still says no lyrics. Rescan (⋯ menu) re-tries all sources.
- [ ] R94: pause/play with the app button, the notification and the lock screen: volume eases out/in (about 0.3 s). Skip next/previous eases. Tap pause twice quickly: stops at once. Pause then play mid-fade: swells back. Slider at 0 = old behaviour. Crossfade still works; casting still hands back to the phone; a Bluetooth headset play button is never silent.
- [ ] R95: Sound panel > Song analysis ON, play 3-4 songs (one local, one saved, one YouTube). Diagnostics shows 'crossmix' lines: bpm, beats, key, energy. Send them. Check a known song's bpm is right (or double/half). Playback sounds identical; battery/data not unusual. OFF = no lines.
- [ ] R96: play a song lrclib has no synced lyrics for (a non-English or older one): lyrics now appear and follow the beat (NetEase/Deezer/QQ). Open an artist and an album page: a short Wikipedia About shows under the header, tap expands; nothing shows (and no gap) for obscure names.
- [ ] R97: open lyrics of a song with an instrumental intro or break: three dots appear and light one by one, lines make room, dots close as the next line starts; a quick pause between lines shows no dots. Like a song: the heart pops once.
- [ ] R98: Player ⋮ > Bedside clock (and Profile > Tools): black screen, correct time/date, stays awake; tap time cycles brightness; clock drifts slightly each minute; play/pause/skip work; Sleep in 30 min starts and cancels; Back closes. Check the 4 Tools pills in Profile still fit on a small phone.
- [ ] R99: Lyrics ⋮ > Lyric card look / shape: tap cycles. Long-press a lyric: the shared picture uses the chosen look (blurred cover shows the cover large and dark; paper is light with dark text) and shape (story is tall, square is square); text fits in every shape.
- [ ] R100: Lyrics ⋮ > Floating lyrics: first time opens the system 'Display over other apps' page; allow, come back, turn on again. Play a synced song, go to the home screen or another app: a dark pill shows the current line and follows the song; drag it up/down (stays there next time); tap opens milo; it disappears when milo is open and when paused; pause/skip updates; off removes it. No crash on a phone that hasn't granted the permission.
- [ ] R101: lyrics (bouncy on): as the line changes, lines below trail the scroll slightly then settle, no shaking after a seek or tap on a line; song with an instrumental intro: current line is still the 3rd row from the top, not one off. Sound panel > Volume slider moves the phone volume and follows the volume keys.
- [ ] R102: open Up next: the first ~10 rows rise in one after another; scroll the queue and drag-reorder: no re-animation, no jumpiness. Open lyrics: the first lines rise in as a cascade; scrolling and the sung-line follow-through still smooth. Reduced-animation phones: nothing odd.
- [ ] R103: player ⋮ > Animated cover: choose a video (pick a short mp4): after a moment it fades in over the cover, loops silently, pauses when music pauses, is gone on the next song, comes back when you return to this song (also after restarting the app). Remove clears it. Music/volume/Bluetooth unaffected while it plays.
- [ ] R104: CI build of the full flavour passes again (LyricsFallbacks KuGou fix). If a new compile error appears, send the log.
- [ ] R105: if CI says 'Unresolved reference LyricsMore' (or any new file), a file from rounds 94-103 was not copied: unzip round94-104-cumulative.zip over the project root to bring everything in line.
- [ ] R106: CI full-debug compile passes with CrossmixBars added. Send the log if another error shows.
- [ ] R107: with airplane mode on and 'share what I'm playing' on, Diagnostics shows far fewer 'POST /rest/v#/shared' lines (spaced out). Drag songs in Up next, including to the very end/start: no crash. Song analysis on: play 'Work' again, expect bpm near 92; check 2-3 other songs didn't change tempo.
- [ ] R108: player ⋮ > Animated cover from music video: set Wi-Fi only. Play a YouTube song that has an official video (a current hit): after a few seconds the cover turns into a silent looping video and fades in; pause stops it; next song switches. A song with no video keeps its picture. Mobile data + Wi-Fi only: nothing downloads. A video you chose by hand still wins. Music, Bluetooth and the lock-screen are unaffected; battery/data not unusual over a few songs.
- [ ] R109: desktop CI: all Rust tests pass (macOS window title 'milo.'); next stage in the workflow (frontend build / bundles) runs.
- [ ] R110: Profile > Tools > "Identify song": paste an AudD token, allow the mic, play music nearby; expect a title/artist and "Search in milo" opening Search with it. Try a bad token and airplane mode for the error text.
- [ ] R111: add the "Turntable" widget (2x2 up to 4x4); play something and check the cover shows inside the record, buttons work, it follows resize.
- [ ] R112: Profile > Tools > iPod skin: circle a finger round the wheel (volume bar shows), press the middle (scrub, seeks in the song), tap ring top/left/right/bottom, tap the title bar to flip white/black, system Back closes.
- [ ] R113: open an album/artist: wide glass Play pill + round shuffle/download, caps info line with minutes; Bedside clock buttons are glass and dim with brightness taps.
- [ ] R114: on an Android TV (or emulator): milo shows in the launcher, opens the big-screen home, arrow keys move a white focus ring, OK plays an album/song, bottom bar buttons work. On a phone nothing changes.
- [ ] R115: Home shows a Tools card under the player; each glass chip opens its screen (identify dialog, bedside, car mode, iPod, downloads).
- [ ] R116: CI build green again after the TvScreen fix (compile only).
- [ ] R117: Now Playing > Sound > Reverb: switch on, pick Hall/Cathedral, move Amount (no clicks, no clipping on loud songs); off returns the dry sound; try with crossfade, headphones and speaker.
- [ ] R118: build only: CI compiles the new crossmix/engine files (no behaviour change yet).
- [ ] R119: CI compiles after the catch-up pack (replace every file in it, none skipped).
