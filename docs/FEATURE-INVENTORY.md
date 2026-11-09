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
| Playback | Android Auto | F | Needs `MediaLibraryService` (Phase 2) |
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
| Downloads | Offline downloads of YouTube audio | F | Conflicts with YouTube's terms; to be rethought, not built as-is |

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
