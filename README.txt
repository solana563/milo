milo. - installable app
=======================

What's in this folder
  index.html            the app
  manifest.webmanifest  app name, icons, shortcuts, "open audio files with milo." on desktop
  sw.js                 service worker (makes the app open offline)
  icons/                app icons made from your logo (any, maskable, Apple touch, favicon, SVG)
  splash/               splash screen made from your image (splash.jpg + iPhone/iPad startup images)

How to get it on a phone or computer
  An installable web app has to be served from https (or from localhost).
  1. Put this whole folder on any static host. Free options: Netlify Drop (drag the folder onto app.netlify.com/drop),
     GitHub Pages, Cloudflare Pages, Vercel.
  2. Open the https address:
       Android / Chrome, Edge:  menu > Install app  (or the install button in Profile > Install milo.)
       iPhone / iPad (Safari):  Share > Add to Home Screen
       Desktop Chrome / Edge:   install icon at the right of the address bar
  3. To try it on your computer first, open a terminal in this folder and run:
       python3 -m http.server 8080
     then visit http://localhost:8080

Notes
  - Songs you add are stored inside the installed app's own browser storage and stay between launches.
  - Android shows its own black splash with your icon for a moment before the milo. splash image appears;
    iPhone/iPad show your splash image straight away.
  - If you change index.html later, change the version name in sw.js (milo-v1 -> milo-v2) so installed copies update.
  - To wrap this as a Play Store / Microsoft Store / App Store package, upload the hosted address to pwabuilder.com.

Cloud sync (Supabase) and YouTube
  - Run supabase-setup.sql once in your Supabase project (SQL editor > paste > Run). It creates the milo_sync table where
    each signed-in person can only read and write their own row. Then Profile > Cloud sync > Sign up.
  - In Supabase > Authentication, email confirmation is on by default: new accounts get a confirmation email first.
  - The Supabase anon key in index.html is meant to be public; row level security is what protects the data.
  - The YouTube API key in index.html is visible to anyone who opens the app. In Google Cloud Console, restrict it to the
    YouTube Data API v3 and to your site's address (HTTP referrers) so nobody else can spend your quota.
  - Hosted previews (like the shared claude.ai page) block outside connections, so cloud sync and YouTube work in the
    installed or self-hosted app.

If installing doesn't work
  - "Install" only appears when the app is opened from an https address (or http://localhost). Opening index.html straight from your
    files app, or from a chat/preview link, can't be installed. Profile > Install milo. tells you which case you are in.
  - Keep the whole folder together. The service worker (sw.js), manifest.webmanifest, icons/ and splash/ must sit next to index.html.
  - Android Chrome: if the Install option is missing, reload once after the first visit.
  - iPhone/iPad: use Safari (not Chrome) and Share > Add to Home Screen.

Smart TVs
  See ../milo-tv/README-TV.txt (Samsung Tizen and LG webOS packages are in milo-tv/). TV mode turns on by itself on TV browsers,
  or use Profile > Insights & tools > TV mode, or add ?tv=1 to the address.

Music intelligence: autoplay now queues songs like the one playing (artist, genre, tempo, energy, what you like and skip). Tempo and energy of your own songs are worked out on the device. While online it also uses similar artists from iTunes, Deezer and YouTube (Sound settings > Discover from the internet). "Start radio" is in each song's menu.

Sources inspired by SimpMusic (github.com/maxrave-dev/SimpMusic), used through their public web APIs only, no SimpMusic code is bundled:
- Lyrics for YouTube songs: SimpMusic Lyrics, then LRCLIB, then BetterLyrics (timed lyrics).
- Discover > Free music > YouTube: SimpMusic weekly charts (chart.simpmusic.org).
- SponsorBlock: skips non-music parts of YouTube songs (Sound settings > Skip non-music parts).
- Return YouTube Dislike: YouTube likes and views on the Now Playing screen.
- Song menu: "Open in SimpMusic" for YouTube songs.
YouTube Music's own search can't be called from a web page (browsers block it), so YouTube search still uses the YouTube Data API.

Sources: Audius, Creative Commons (Openverse) and Internet Archive were removed. Streaming now comes from the YouTube Music catalogue (the source SimpMusic streams from), plus 30-second iTunes previews and live radio. Songs you saved earlier from the removed sources are dropped from Saved songs.
