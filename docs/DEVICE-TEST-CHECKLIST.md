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
- [ ] R120: Sound > Transitions > Crossmix on a local playlist: a song ends into the next on the beat, no double audio/click; Try a mix now; skip, seek, pause mid-mix; headphones plug/unplug; cast starts (no mix); switch back to Classic. Send Diagnostics lines tagged crossmix.
- [ ] R121: with Crossmix on, play a streamed playlist: fewer 'could not read' lines in Diagnostics, more varied styles than Filter sweep; playback and UI stay smooth while it analyses.
- [ ] R122: Sound > Soft pause & skip: set 1 s, pause/play and skip from the app, lock screen and notification: smooth fade, no click, play resumes at full volume; try Echo and Tape stop; press pause twice (second = now); skip during a crossmix; cast.
- [ ] R123: Sound > Haptics on: phone buzzes on kicks, Bass vs Vibe feel different, Low/High strength; off stops it; no stutter in the audio; during a crossmix it doesn't double-buzz.
- [ ] R124: Sound > Cover style: try Squared, Disc, Big disc with arm, Disc + album; spin on/off; pause greys the record and lifts the arm; swipe next/previous still glides; back to Full screen restores the old look; no jank on a long listen.
- [ ] R125: Squared cover shows a soft mirror under it that fades out; like a song from the dock: heart flies up, thumps, bursts, dives back and the button bounces; unlike is quiet; like twice quickly (2 hearts).
- [ ] R126: CI compiles (haptics, cover styles, reflection, like burst); a paused disc still greys.
- [ ] R127: open lyrics of a Japanese / Korean / Chinese / Russian song: reading line under each line; Latin songs show none; Lyrics menu > Readings off hides it; translation line still shows below; CI resolves kuromoji + tinypinyin (APK grows several MB).
- [ ] R128: CI compiles ui/lyrics/*; Lyrics menu > Lyric style: Glow: active line sweeps along the sung letters, words rise, lines not sung blur (Android 12+), glow on Android 13+; tapping a line seeks; readings and translation still show; switch back to Classic.
- [ ] R129: CI resolves tinypinyin; Chinese lyrics get pinyin readings.
- [ ] R130: CI resolves pinyin4j and unit tests run; Chinese lyrics show pinyin readings (e.g. 'ni hao').
- [ ] R131: CI compiles the glow lyric line.
- [ ] R132: CI builds the full debug APK (kuromoji packaging); Japanese lyrics get romaji readings.
- R133: turn on Sound > Lyrics ticker; play a word-synced song: line should roll up per lyric, long lines slide with the sweep, gaps show dots; off = old preview line. Uncompiled, untested.
- R134: drag the player down slowly: the cover should fly into the mini player's art slot (rounded), title/artist slide toward the mini title and fade; release <220px springs back. Check it lands on the mini art (assumes edge-to-edge window). Uncompiled, untested.
- R135: Cover style > Turntable: record runs off the left, arm top-right moves in as the song plays; arc peeks 3 s on play; hold + swing the arm to scrub, release seeks; swiping the art elsewhere still skips. Uncompiled, untested.
- R136: Sound > Loudness on: +4 dB should sound louder without crackle on loud tracks, reduction readout moves; ReplayGain Album vs Track changes level on tagged local files; YouTube songs unchanged; downloads/offline still play. Uncompiled, untested.
- R137: tap the mini player: the sheet should grow out of the bar with the cover flying from the mini art; drag down slowly: it shrinks back into the bar (not slide); back button/arrow fold into the bar; try with Squared/disc styles (cover stays small, grows late); open when no mini bar is showing (from the Now playing pill): grows from the bottom. Uncompiled, untested.
- R138: wordmark should read heavy italic 'milo.' with amber dot on: header, sign-in, permission, TV, Sessions title. Tap mini player: play + next buttons fly into place. Open lyrics: the cover flies into the 86dp header slot (stays there); close: flies back. Sound > Backdrop > Liquid backdrop: slow colour drift behind the player (API 33+; older = still zoom). Uncompiled, untested.
- R139: crossfade on (e.g. 6 s), play an album with continuous tracks: no fade between its songs; different albums/shuffled across albums still crossfade; switch off 'Keep albums gapless' = fades everywhere. Uncompiled, untested.
- R140: Sound > Fades > Blend skips on: press next while playing: the next song starts immediately under the old one and they cross over (no silent gap); previous within 3.5 s of the start blends back, later restarts the song; tap a queue song blends; with it off, skips fade as before; Crossmix on / casting: plain behaviour; YouTube songs: check the cross-over starts within ~2 s. Uncompiled, untested.
- R141: Profile > 'Handle calls and audio focus myself' on. (1) Play over Bluetooth, take/ring a call: music pauses at once; after the call it resumes only after a moment, at normal pitch. Diagnostics: 'focus lost...', 'resumed (...) audio mode 0, bluetooth call link false'. (2) Open another music/video app then come back: pauses, then resumes. (3) A navigation voice prompt ducks the music to a fifth and restores it. (4) Connect/disconnect Bluetooth headphones while playing: a brief gap, then plays on the new output at normal speed. (5) Crossmix hand-over then take a call: still pauses/resumes. Tell me if it ever fails to resume or resumes into a call. Uncompiled, untested.
- [ ] R142 Player Studio skins: Now Playing ••• > Player Studio (skins); pick Poster, Timeline, Lyrics, iPod, Bedside; back button closes the picker first, then the player; choice is kept after restart; Studio button (top right) on every skin except Bedside; seek bar and prev/play/next work; Timeline scrolls to the sung line and tapping a line seeks; songs without synced lyrics show the title
- [ ] R143 build fix: Now Playing opens/folds with the cover morph; swiping lyrics up flies the cover into the lyrics header
- [ ] R144 Studio Customize: Player Studio > Customize on Poster/Timeline/Lyrics; sliders and chips change the player live behind the sheet; settings survive restart; Reset restores defaults; iPod/Bedside/Flagship show an explanation only
- [ ] R145 Studio presets: Customize > Presets (Midnight, Mono, Loud, Quiet) change the look at once; 'Song title and artist' hides the artist/title on Poster and Lyrics; Reset still restores defaults
- [ ] R146 Gestures: Profile > Gestures; change side double-tap to 30 s and it jumps 30 s; middle 'Nothing' stops play/pause on double-tap; swipe off stops skipping; 'long' swipe distance needs a longer swipe; Reset restores defaults
- [ ] R147 Downloads + collages: Library > Downloads shows tabs All/Playlists/Albums/Artists/Singles; download a whole album and a whole playlist and one song alone and each lands in its own section; tapping a group opens only its songs and Back returns to the groups; Library > Playlists shows 2x2 collage covers for playlists of phone songs; Home Downloaded card shows the breakdown
- [ ] R148 importers: Library > Playlists > Import a playlist with a .csv (Exportify-style: Track Name, Artist Name(s)) makes a playlist of the songs found on the phone and says 'Imported X of Y'; Import from a link with a public music.youtube.com/playlist?list=PL... link makes a playlist of those songs; a radio/mix link shows the 'isn't a public playlist' message; a private playlist shows 'No songs found'
- [ ] R149 Bedside escape: choose Bedside in Player Studio; the faint Studio button (top right) opens the picker and you can pick another skin
- [ ] R150 import matching: import a .csv whose songs are not on the phone; the toast says 'Matching N songs…' then 'Imported X of N (A on this phone, B from YouTube Music)'; the new playlist plays the YouTube songs; a song with a wrong/unclear match is counted as not found, not added; airplane mode: finds only phone songs and does not crash
- [ ] R151 import history: after an import, Library > Playlists > Import history lists it with counts and the 'Not found' songs; Clear empties it
- [ ] R152 playlist header: open one of your playlists; the top shows a collage of its songs' covers (one cover if all one album); tap 'Add a description', save text, it shows under the header and survives restart
- [ ] R153 lyrics fix: song ⋮ > Lyrics: timing and file; +/-0.25 s steps move the lyric highlight on reopening the lyrics; choose an .lrc and it replaces the found lyrics; 'Use the found lyrics again' removes it; Reset timing
- [ ] R154 song cover: song ⋮ > Change cover, pick a picture; it shows in rows, mini player and player right away; Use the original cover restores; notification art may stay original
- [ ] R155 recap: Profile > Your listening > Watch my recap; pages advance by tap and by themselves; left third goes back; the last card's Share opens the share sheet; All time and a year both work; Back closes it
- [ ] R156 What's new: install over an older build -> the What's new dialog shows once; fresh install doesn't show it; Profile > About shows version/build and reopens it
- [ ] R157: Home has no Tools shelf; Profile shows category rows; each opens a page, back returns to the list; Tools page launches Identify/Car/Bedside/iPod; Connections page has shared playlists + QR; Account row only when signed in.
- [ ] R158: pull down on Home, Discover and Library songs: dots draw in, tick when armed, spin while loading, bounce back. Library pull rescans songs.
- CI: Rounds 148–158 compile green (2026-11-23). Device checks above still open unless ticked.
- [ ] R159: Home shows weekday in pixels, stats widget (minutes today / streak / 7 bars; tap = Profile), Liked + Saved big tiles, PINNED grid (Edit adds/removes shortcuts and playlists, Reset restores), RECENT albums grid; screen and shelf titles are pixel lettering; a title in another script (Japanese, Arabic) stays normal text; player card + shelves still work.
- [ ] R160: Home is on plain black (no panel cards); Now playing tile is first under the header; glass tiles in amber/white only; titles are normal type again; Quick access Edit/Reset still work.
- [ ] R161: Library, Discover, Search, Profile and album/artist/playlist pages are on plain black (no card behind); header photo fades cleanly into black; screen slide animations unchanged; Y2K/Spider/Nebula styles still look right.
- [ ] R162: Profile > Look > Background switches Black / Song art / Song colour / My picture live; picture survives restart; Song art and colour change with the song; Recently played is under Now playing; Liked and Saved tiles follow the accent colour (try white and dark accents for readable text).
- [ ] R163: Now playing shows art+title, then the button strip (all 7-8 buttons work: Like heart flies, Save, Queue, Output, Sound, Timer, Visual, More menu), then the controls card, then live lyrics on black; lyrics follow the song, tap or swipe up opens the full page; morph from/to the mini player still lines up; try a short phone and Y2K/Spider themes.
