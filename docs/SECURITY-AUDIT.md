# milo. security review (native Android app and Supabase files)

Scope and honesty: this is a read-through of the files in this repository (Android manifest, Kotlin sources, the Supabase SQL files). It is not a penetration test, nothing was run against your live Supabase project, and the web, TV, Windows and iOS code was not reviewed in this pass. Secrets found are described, never copied here.

## Fixed in Round 40
| Finding | Risk | Fix |
|---|---|---|
| Sign-in tokens and the music-server password were stored as plain text in app storage | Readable on a rooted phone or by malware with root | `SecureStore`: encrypted with an Android Keystore key (AES-256-GCM). Old plain values are upgraded on first read. If a phone's Keystore fails, it falls back to the old behaviour rather than locking the person out. If a stored value can't be decrypted the person is simply signed out |
| Older Supabase functions (`find_user`, `delete_me`, push functions, `are_friends`, trigger functions) were runnable by signed-out visitors, because Postgres allows everyone unless revoked | User lookup by exact username without signing in; needless exposure | `supabase_security_hardening.sql` revokes public/anon access and keeps only what the app needs (`username_free` stays open for sign-up) |
| The text cleaner in the Sessions and Shared playlists SQL used a regex form that some Postgres versions reject | Functions could fail to create or run | Switched to `[[:cntrl:]]` in both files and in the hardening file |

## Already sound
- Every Supabase table in the repo has Row Level Security on. Sessions, Connect and Shared playlists use "no direct access" tables plus functions that check who is calling.
- The only key in the Android source for Supabase is the public "anon" key, which is designed to be public; protection is the RLS above. No service-role key, keystore password or database credential was found in source, workflows or SQL.
- No logging of tokens or passwords was found. Backups are disabled (`allowBackup=false`). Subsonic passwords are never sent, only a salted hash.
- No custom TLS trust or certificate-bypass code.
- Messages: senders can't edit another person's message; reads limited to the two people; messages expire after a day.

## Needs your action (I can't do these from here)
1. **YouTube Data API key.** A Google API key is written in `YouTubeRepo.kt`. Client keys can't be secret, so in Google Cloud Console restrict it to the YouTube Data API v3 only and to your Android package names and signing SHA-1 fingerprints (`com.ubayanda.milonative`, `.lite`). Without this anyone can copy it and use your quota.
2. **Run `supabase_security_hardening.sql`**, and re-run `supabase_native_sessions.sql` and `supabase_native_playlists.sql` if they failed earlier because of the text cleaner.
3. **Signing key.** You told me earlier the signing key was exposed. Generate a new one, and keep the keystore and passwords only in GitHub Secrets. A new key means a new app identity for Play (use Play App Signing if you publish).
4. **Supabase Auth settings:** turn on email confirmation, leaked-password protection, and set a sensible rate limit. Set the Site URL for password-reset links.

## Known, accepted for now (documented, not fixed)
- `usesCleartextTraffic=true`: needed because internet radio stations and self-hosted music servers are often plain http. Cost: those streams can be read or altered on the network. A later option is to allow http only for those features.
- All signed-in users can read every profile (username, display name, avatar). That is how friend search works. A later option is to limit it to exact-username lookup via `find_user`.
- The media service is exported, as Android Auto requires. It accepts any media controller; a later option is an allow-list of known car and system packages.
- The YouTube embed page runs JavaScript with a small bridge that only skips to the next song. It loads only the YouTube player; navigation away from it is not blocked.
- Display names in rooms and playlists are typed by the client, so impersonation inside a room is possible.
- Reports from Sessions land in `milo_reports`; nobody reads them unless you do. Before a public launch you need a way to review them.
- Account deletion removes sync data, messages, friendships and the login; rooms and playlists are removed through database cascades. Data export exists as a backup file; there is no full export of server-side data.

## Added in Round 44 (pairing)
- Codes are 8 characters from a 31-letter set (about 850 billion combinations), last 5 minutes, work once, and wrong guesses are rate limited per account.
- A controller can only send play, pause, next, previous and a seek of up to 60 seconds; the server refuses anything else, including loading songs. Access can be limited to 1 hour, 1 day or until removed, and the owner can remove it at any time.
- The controller sees only the device name, whether it is online, and the current song and artist. The queue and everything else stay private.
- Risk to know: anyone who has a valid, unexpired code can link. Treat the code like a key and don't post a screenshot.
