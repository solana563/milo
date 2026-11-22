# Mochy Music vs milo.: what is and isn't ported

Source: github.com/realidkroo/Mochy-Music (GPL-3.0). Updated Round 117. "Not built" means nobody has written it for milo yet, not that it was refused.

## Done (ideas or code, credited in CREDITS.md)
Soft transport fades, haptics (bass-following vibration), Crossmix analysis (tempo, beats, bars, key), extra lyric sources, interlude dots, lyrics follow-through, Wikipedia About, bedside clock, lyric share cards, floating lyrics, animated covers (YouTube official video, not Spotify), song identify (AudD), widgets (+ turntable), iPod skin (small), TV home (small), reverb with 8 rooms.

## Refused on purpose
- **Spotify canvas** (hidden WebView, harvested session tokens, private endpoints, impersonation).
- **Shazam recognition** (private endpoint + reverse-engineered signature). milo uses AudD's official API instead.
- **Discord presence by logging in with a user token** (self-botting is against Discord's terms). A proper Rich Presence through Discord's own SDK/app is possible later.
- **Spotify playlist import by harvesting a logged-in session.** Offered instead: import from an exported file or a public playlist link.

## Not built yet (rough size, Mochy side)
- Crossmix planner + dual-deck beat lock + transitions (needs Diagnostics from a device first).
- Player looks (done R124-125: cover styles, reflection, like burst): morphing cover/song info, cover reflection, disc/turntable cover styles, ring, player background styles, seam blur, flagship layouts, landscape, Player Studio (edit layout), module/skin system.
- Lyrics (readings done R127): glow line (R128, style switch in the lyrics menu), ruby over words, stagger, mini-lyrics ticker in the mini player, source footer, lyric video sync, full lyric share editor (word editor, backdrops, choreography).
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

- Lyrics ticker (MiniLyricsTicker): ported single-row card, opt-in on Now Playing. Tall mode (readings/upcoming list) not ported; mini-player placement intentionally not done.
- Morphing cover / song info / sheet: core ported (R137: sheet progress, cover + title + backdrop morph, open/close/drag). Play/next flying and the lyrics-header flight: ported (R138); liquid warp backdrop: ported (R138, opt-in). Not ported: Mochy's video-mode layer, canvas layer (declined: Spotify canvas scraping), reflection layout during the morph.
- Turntable style + arm: ported (R135). Not ported: arm's stream-failure red arc, wide-screen (tablet) turntable stage, record carousel sliding.
- Loudness processor + ReplayGain album/preamp/peak: ported (R136). Gapless albums: ported (R139). Still not ported: AutoEQ / Audio Lab pages, PrecisionSink.
- Gapless albums: ported (R139).
- Blend skips: ported (R140) for skip/previous/queue pick. Not ported: neighbour prefetch, blend into a freshly picked list.
- Mochy's audio-focus / call handling: ported as PlayerFocus (R141). Not ported: its PrecisionSink / PassSink output layer (USB/exclusive output, resampling, dither), mixing with other apps.


## Update R142
Player skins: Poster, Timeline, Lyrics (milo-drawn), iPod and Bedside (existing) now switch through a Player Studio picker. Still not ported: the Studio editor (arrange/restyle parts, cover/middle/bar editors), Mochy's exact iPod (click-wheel menus, cover flow), poster variants, Video and Module styles, per-skin settings, reflection during morph.


## Update R144
Studio now has a Customize tab (text size, circle size, colour, backdrop, show/hide seek and controls, centred words) for Poster, Timeline and Lyrics. Still missing: Mochy's drag-to-arrange editor, per-part editors (cover/middle/bar), and per-skin presets.


## Update R145-R146
Studio presets shipped in R145 (the R144 note above that lists presets as missing is out of date). R146: Gestures settings in Profile (milo's own; Mochy has no standalone gestures screen). Next in line: playlist covers, importers.


## Update R147
Playlist covers (collage): ported for phone-song playlists in Library > Playlists. Not yet: collage on the playlist detail header, playlist descriptions/transition. Downloads arranged by origin (milo's own feature).


## Update R148
Importers: CSV (to songs on the phone) and public YouTube playlist link: ported. Not built: import history, local-file tag/artwork import, importing CSV songs that aren't on the phone by searching YouTube, Spotify links (declined: needs a harvested session).


## Update R150
Importers now find songs missing from the phone on YouTube Music (so a Spotify playlist exported to CSV imports in full). Still missing: import history, a review step for uncertain matches, local-file tag/artwork import.


## Update R151
Import history shipped. Still missing: a review step to fix uncertain matches by hand, local-file tag and artwork import.


## Update R152
Playlist header collage and descriptions shipped for your own playlists. Still missing: custom playlist cover from a picture, collage style choices.


## Update R153
Song edit: lyrics offset and own lyrics file shipped. Still missing: cover pick (sources/picture), per-song speed/EQ, video picker, artist picker.


## Update R154
Song cover from a picture shipped (no cover search). Still missing: per-song speed/EQ, video picker, artist picker, notification artwork using the custom cover.
