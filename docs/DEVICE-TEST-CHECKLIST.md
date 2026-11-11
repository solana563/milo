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
