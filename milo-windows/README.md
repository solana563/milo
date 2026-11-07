# milo. for Windows (WinUI 3 / C#)

A native Windows app (not Electron). Round 25 = **core**: sign-in (Google + email), Home, Discover, Library from folders,
Now Playing with synced lyrics, downloads, media keys, Smart play. Social (feed, chats, friends) comes next.

## Build (GitHub Actions)
Push, then run **Build milo. for Windows**. Artifacts: `milo-windows-portable` (unzip, run `milo.exe`) and
`milo-windows-installer` (`milo-setup.exe`). Both are unsigned, so Windows SmartScreen will say "More info → Run anyway".

## Google sign-in on Windows
The app opens your browser and listens on `http://127.0.0.1:53682/cb`.
Add that exact URL in Supabase → Authentication → URL Configuration → **Redirect URLs**. Nothing else changes
(the Google provider and Web client ID you already set are reused).

## Where things live
- `%APPDATA%\milo\prefs.json` settings, likes, history · `session.bin` login (encrypted with Windows DPAPI) · `log.txt` errors
- Downloads: `Music\milo` (change in Profile). Downloaded songs show up in Library automatically.

## Files
- `Data/` Core (settings, log), Supabase (account), YtMusic, Lyrics, Library, Downloads, Player (MediaPlayer + loopback stream proxy)
- `Views/` code-built screens: Gate, Shell, Home, Discover, Library, Downloads, Profile, NowPlaying, PlayerBar

## Not yet
System tray, taskbar thumbnail buttons, auto-update, social features.
