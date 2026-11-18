# milo. desktop (Tauri)

This folder is a fork of **Limusic** by SimoHypers (https://github.com/SimoHypers/limusic, version 1.4.0-rc.3), licensed under the
GNU GPL v3 (see `LICENSE`). milo. is GPL-3.0 as well, so the fork stays GPL-3.0 and keeps Limusic's copyright and credits.
Everything under `crates/`, `src-tauri/src/` and most of `ui/` is Limusic's work. The YouTube Music engine, player (libmpv), lyrics and
the rest of the app are theirs; please support them.

## What milo. changed (stage 1)
- Name, window title and identifier: `milo.` / `com.ubayanda.milo.desktop`, version 0.1.0.
- Look: milo. wordmark and icons, Figtree font, amber accent and warm neutrals as the default theme.
- About page: credits Limusic; the Ko-fi and "report an issue" links (which belong to the original author) are gone.
- Updates: the auto-updater and release-notes feed pointed at Limusic's GitHub releases, which would have installed Limusic builds over milo.
  They are switched off (empty endpoints) until milo. has its own signed releases.
- English texts say "milo." instead of "Limusic" (other languages still say Limusic until they are reworked).
- `ui/package.json` adds `@fontsource-variable/figtree`.

## Still Limusic's (on purpose, for now)
- Discord presence badge URL, lyrics service names ("Limusic Lyrics"), the user-agent strings sent to lyrics and ListenBrainz servers.
- Other languages' texts. Rust crate names (`limusic-app`).

## Stage 2 (not done)
Own updater key and releases, Windows/Linux/macOS installers in CI, milo. sign-in / Listen together / chat, Now Playing look, own user agent.
