# Mochy Music vs milo.: what is and isn't ported

Source: github.com/realidkroo/Mochy-Music (GPL-3.0). Updated Round 117. "Not built" means nobody has written it for milo yet, not that it was refused.

## Done (ideas or code, credited in CREDITS.md)
Soft transport fades, Crossmix analysis (tempo, beats, bars, key), extra lyric sources, interlude dots, lyrics follow-through, Wikipedia About, bedside clock, lyric share cards, floating lyrics, animated covers (YouTube official video, not Spotify), song identify (AudD), widgets (+ turntable), iPod skin (small), TV home (small), reverb with 8 rooms.

## Refused on purpose
- **Spotify canvas** (hidden WebView, harvested session tokens, private endpoints, impersonation).
- **Shazam recognition** (private endpoint + reverse-engineered signature). milo uses AudD's official API instead.
- **Discord presence by logging in with a user token** (self-botting is against Discord's terms). A proper Rich Presence through Discord's own SDK/app is possible later.
- **Spotify playlist import by harvesting a logged-in session.** Offered instead: import from an exported file or a public playlist link.

## Not built yet (rough size, Mochy side)
- Crossmix planner + dual-deck beat lock + transitions (needs Diagnostics from a device first).
- Player looks: morphing cover/song info, cover reflection, disc/turntable cover styles, ring, player background styles, seam blur, flagship layouts, landscape, Player Studio (edit layout), module/skin system.
- Lyrics: glow line, readings (romanisation), stagger, mini-lyrics ticker in the mini player, source footer, lyric video sync, full lyric share editor (word editor, backdrops, choreography).
- Video mode (YouTube video with lyrics).
- Audio Lab: AutoEQ profile search/fit, loudness, peak limiter, ReplayGain, spatial audio processor, output-device tuning, quality screen. (milo has graphic + parametric EQ, bass, spatial, reverb now.)
- Gestures customisation screen.
- Recap story, stats charts, ID card.
- Import: CSV, link importer, import history, local-file tag/artwork import.
- Song edit: cover from sources, lyrics file read/write.
- Playlist covers (collage), playlist descriptions/transition.
- i18n (app languages), update/changelog sheets, donate/support, setup wizard.
- Android Auto app screens (milo has a media-browser library, not Mochy's full Car App UI), phone ↔ TV link/remote.
- Last.fm queue/worker polish (milo already scrobbles to Last.fm and ListenBrainz).
