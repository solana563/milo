# Credits

## Limusic
The YouTube Music features in milo. (`src/js/67-youtube-music-direct-streams-pages-art.js`, the `dsk:http` / `milostream`
parts of `desktop/main.js`, and `src/css/10-youtube-music-art.css`) were built after studying
[Limusic](https://github.com/SimoHypers/limusic) by SimoHypers (GPL-3.0), and it in turn credits
[Metrolist](https://github.com/mostafaalagamy/Metrolist).

What was learned from it:
- which YouTube client identity returns plain audio URLs without a cipher or PoToken (VISIONOS), and which client to use for metadata (WEB_REMIX)
- how search, artist, album and playlist answers are shaped, and how to pick the best original (non-dubbed) audio stream
- that googlevideo throttles one long download but serves bounded pieces at full speed, which is why the desktop app streams through a small local proxy
- how its pages show artwork: size-aware thumbnails with a fall-back chain, ignoring YouTube's 2x2 playlist collage, a blurred cover backdrop that fades into the page, and poster-style artist cards

The code in milo. is written separately in JavaScript, but it follows Limusic's approach closely. Limusic is licensed
under the GNU GPL v3, so if you distribute milo. builds that include this code, they should be released under a
GPL-3.0 compatible licence with source. See `LICENSE` choice notes in `milo-native/FDROID.md`.
