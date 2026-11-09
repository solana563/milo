# milo. native Android: feature inventory (Phase 1 audit)

Status key: **A** implemented, user-confirmed on a device · **B** implemented, not yet device-tested · **C** partial · **D** UI only · **E** broken · **F** not implemented.
Nothing here is marked "verified" unless a person ran it on a phone. The build sandbox can't compile or run the app, so only CI and the owner's phone count as evidence.

| Area | Feature | Status | Notes |
|---|---|---|---|
| Playback | Local files via Media3 + MediaSessionService | A | Background play, notification, lock screen |
| Playback | YouTube streaming | B | Depends on stream client availability; can break when YouTube changes |
| Playback | Crossfade (ghost player), EQ, bass, spatial | B | Custom code; test on low-end devices |
| Playback | Resume last queue and position | B | `PlayerHolder.restore` |
| Playback | Audio focus / headphones unplugged | B | Standard Media3 handling |
| Playback | Android Auto / car browsing | B | Round 31: `MediaLibraryService` + browse tree. Never run on a DHU or car yet; see docs/ANDROID-AUTO.md |
| Library | Scan, albums, artists, playlists, favourites | A | |
| Library | M3U import/export | B | Unit-tested parser (this round) |
| Library | Very large libraries (10k+) | C | Lazy lists, windowed play queue; scan post-processing moved off main thread (Round 30c); sort speed unit-tested at 20k; real-phone benchmark still to do |
| UI | Liquid glass, scroll/search memory | A | Confirmed Rounds 26, 28 |
| UI | milo. Lite flavour (Android 8+) | A | Confirmed on Samsung S7 edge |
| Account | Email sign-up / log-in | A | |
| Account | Google sign-in | B | Needs OAuth client per package |
| Account | Delete account (RPC) | B | Needs `delete_my_account` in Supabase |
| Account | Password reset | B | "Forgot password?" sends Supabase recovery email (Round 30b). The link opens Supabase's hosted page, where the user sets the new password; set Site URL / redirect in Supabase |
| Account | Sign out other devices | B | Round 30c. Supabase gives no session list to apps, so a per-device list isn't possible without our own device table |
| Account | Use without an account | A | Local library works signed out |
| Sync | Whole-library cloud backup | B | Hardened in Round 30 (see below) |
| Sync | Offline edit queue | C | Edits mark the library dirty and upload on next sync; no per-change queue |
| Social | Friends, chat, EQ sharing | A (Lite) / B (full) | |
| Social | Listen Together | B | Full flavour only |
| Data | Backup export / import | B | |
| Quality | Automated tests | C | Unit tests added Round 30 (pure logic only); no instrumented or UI tests |
| Quality | Crash reporting | C | Local `ErrorLog`, user-initiated share; nothing sent automatically |
| Downloads | Offline downloads of YouTube audio | A | Existing feature, kept unchanged by owner's decision. Note: YouTube's terms generally don't permit downloading audio from YouTube; that compliance risk is the owner's call |
| Downloads | Download Manager (queue, pause/resume, retry, auto-retry x3, Wi-Fi only, data saver quality, storage limit + auto-remove oldest, free space, unfinished-file cleanup) | B | Round 32, untested on device |
| Downloads | Queue survives app restart | C | Unfinished queue returns paused after restart; downloads only run while the app process is alive (no background service yet) |
| Downloads | Streaming cache | F | No streaming cache exists, so "cached" content isn't a separate category yet |

## Round 30 sync changes
Before: if the cloud copy was newer, it replaced the local library, even when the phone had unsynced offline edits.
Now:
- A `sync_base` marker records the last state both sides agreed on; `sync_ts > sync_base` means local edits are unsynced.
- Cloud newer + unsynced local edits: merge both (union), then upload. Nothing is dropped.
- Cloud newer + no local edits: replace, but only after saving `sync_backup.json` in app storage.
- An empty cloud copy never wipes a non-empty local library; the phone's copy is uploaded instead.
- Sign-out / account deletion resets both markers.
Limit: merging is additive, so a deletion made on one device can reappear after a merge. Per-item tombstones are the proper fix (future round).

## Known gaps to schedule
1. Password reset email flow. 2. Session/device list. 3. Large-library benchmark. 4. Instrumented tests for playback service. 5. Tombstones for sync deletes.

## Phase 4 (Round 33): smart playlists and taste
| Feature | Status | Notes |
|---|---|---|
| Smart mix (plays, likes, skips, artist affinity) | B | Local only. Tempo and energy are NOT analysed (needs audio analysis); not claimed |
| Daily rotation (favourites / new finds / preferred artists / rested favourites, 12h no-repeat, artist cap) | B | Changes daily, or on refresh |
| Forgotten favourites | B | Only after 45 days of recorded listening; appears empty before then by design |
| Mood mixes (Chill, Focus, Workout, Energy, Sleep, Romantic, Party, Nostalgia) | C | From genre tags (Android 11+), year and title words only. Hidden when fewer than 8 songs qualify. YouTube songs have no genre, so no moods |
| Discovery (local unplayed songs from liked artists) | B | Online discovery still the existing YouTube artist radios, now filtered by blocks |
| Feedback: More like this / Don't recommend song / artist, undo, unblock list | B | Skip is recorded automatically (left early without counting as a play). A Like already exists |
| Taste profile screen (top artists/songs, genres, active hours, new artists, weekly trend, playlist variety) | B | Reached from Profile > Your listening |
| Personalisation switch + reset | B | Off = nothing new learned and mixes see no history; explicit blocks still apply |
| Similar artists / related genres from an outside source | F | Not added; would need a permitted metadata source |

## Phase 5 (Round 34): milo. Connect (own devices)
| Feature | Status | Notes |
|---|---|---|
| Device list with online status and what each is playing | B | Needs `supabase_native_connect.sql`; same account only; off until switched on |
| Remote control (play/pause, prev, next, seek +/-15s) | B | Commands pass through an account-private table; polled every 3 s, so expect a short delay; commands older than 30 s are dropped |
| Transfer: "Play here" / "Send my music there" | B | Online (YouTube) songs only. Songs stored on one phone are refused with a clear message, never faked |
| Secure pairing | C | Same-account devices need no pairing: Row Level Security is the gate and nothing secret is in a QR. QR / code pairing for other accounts or signed-out devices (TV, desktop) is NOT built: it needs a server-side function to issue short-lived links, which should be designed and tested on its own |
| Connect on Windows / TV / web | F | Android only so far |
| Works with app closed | F | Needs the milo. process alive on both devices; no push-wake |
| Synchronized listening (Listen Together) | B | Existing (Round earlier); not changed this round |
| milo. Sessions: create/join by code, invite link and QR, private/public rooms | B | Round 35. Needs `supabase_native_sessions.sql`. QR is drawn on the phone; scanning with the camera app opens `milo://room/CODE`. Custom links aren't clickable in every chat app, so the code is always shown too. No in-app QR scanner |
| Sessions: host/listener roles, shared queue, suggestions + approval, open-queue mode | B | Server-enforced limits (3 s between adds, 10 waiting suggestions each, 100 in queue, 50 people) |
| Sessions: synchronized playback with the host | B | Polls every 2 s, corrects drift above 3 s. Only online (YouTube) songs are shared; a host song from local storage is shown but never promised to listeners |
| Sessions: reactions, activity feed, chat line | B | 6 fixed reactions + 140-char messages, rate-limited server-side |
| Sessions: moderation and safety (mute, remove, ban, report, block, end) | B | Reports land in `milo_reports` for you to review in Supabase; the app cannot read them. Block hides a person on that phone only. Display names are typed by the client, so impersonation inside a room is possible; host can remove anyone |
| Sessions: real-time over websockets | C | Uses 2 s polling through RLS-locked functions instead (simpler, auditable); a little delay is normal |
| Sessions: room outlives the app / push invites | F | Host's app must stay running; a room whose host is silent for 60 s shows "host away" |
| Shared playlists: owner/editor/viewer roles, invite code/link/QR, public or private, add/remove/reorder, change history (last 50), copy to my library | B | Round 36. Needs `supabase_native_playlists.sql`. Server-enforced: editors remove only their own songs, 500 songs, 50 people, 1 add per second each. Only online (YouTube) songs. Updates by polling every 6 s, no live push. Copying makes a private copy that stops following. Display names are typed by the client |
| Collaborative playlists | F | Round 36 |

