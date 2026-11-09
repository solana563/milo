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
