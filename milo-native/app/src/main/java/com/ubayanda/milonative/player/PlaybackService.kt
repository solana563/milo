package com.ubayanda.milonative.player

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * The music player itself. It lives in a foreground service, so playback carries on with the screen off,
 * the lock-screen / notification controls and headset buttons all work, and it never depends on the UI being open.
 */
class PlaybackService : MediaLibraryService() {
    private var session: MediaLibrarySession? = null
    private var exo: ExoPlayer? = null
    private var castBridge: CastBridge? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private var resumedFor: String? = null
    private val resumeSaver = object : Runnable {
        override fun run() {
            val p = exo
            try {
                val id = p?.currentMediaItem?.mediaId
                if (p != null && id != null && resumedFor == id && p.duration >= Resume.LONG_MS) {
                    if (p.currentPosition > p.duration - 30_000) Resume.drop(this@PlaybackService, id)
                    else if (p.isPlaying) Resume.put(this@PlaybackService, id, p.currentPosition, p.playbackParameters.speed)
                }
                // keep the widgets' progress bar moving while a song plays
                if (p != null && p.isPlaying) com.ubayanda.milonative.widget.WidgetUpdater.push(this@PlaybackService, p)
            } catch (_: Throwable) {}
            handler.postDelayed(this, 10_000)
        }
    }
    private val ytRetried = HashSet<String>()
    private val svcScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main)
    private var rgScale = 1f
    private var rgPrev = 1f
    private val fxPrefs by lazy { getSharedPreferences("milo_fx", MODE_PRIVATE) }

    // ---- true crossfade (two players, like the web app's two decks) ----
    // The OLD song keeps playing in the main player, untouched, and fades out. The NEXT song is prepared in a second player ("ghost"),
    // started silently a little early so it is already making sound, and fades in. When the old song is done the main player takes the new
    // song over at the same spot and the two copies of it are blended for a third of a second, so there is no restart and no gap.
    private var ghost: ExoPlayer? = null
    private var primedKey: String? = null
    private var xfDeadKey: String? = null   // a song where the fade was given up, so it isn't tried again
    /** 0 = no crossfade; 1 = fading (old out in the main player, new in on the second); 2 = main player is taking the new song over; 3 = blending the two copies. */
    private var xfPhase = 0
    private var xfT0 = 0L
    private var xfLen = 0L
    private var xfP0 = 0L          // where the old song was when the fade began
    private var xfF = 0f           // how far the fade has got; it only ever moves forward
    private var xfNextIdx = -1     // queue index of the song being faded in
    private var xfRgNew = 1f       // loudness scale of the song being faded in
    private var xfWarmAt = 0L
    private var xfHStart = 0L
    private var xfH2 = 0L
    private var xfTrace = 0L
    /** Set just before the crossfade itself moves the main player to another song, so a skip by the person can be told apart. */
    private var xfOwn = false
    private var xfWarm = false     // the next song is already running silently on the second player
    private var lastTick = 0L

    // After a stall (app switch, notification shade, screen wake) the audio left in the output's queue plays back in a rush to catch up.
    // Throw that stale audio away instead: one clean restart at the current spot, so the interrupted moment is simply lost, not replayed fast.
    // Never during a crossfade, never for live radio, and at most once per 8 seconds.
    private var lastResync = 0L
    private fun resyncAfterStall(p: Player) {
        try {
            if (!fxPrefs.getBoolean("resync", true)) return
            if (xfPhase != 0 || xfWarm) return
            val now = android.os.SystemClock.elapsedRealtime()
            if (now - lastResync < 8_000L) return
            if (!p.isPlaying || p.duration == C.TIME_UNSET || p.isCurrentMediaItemLive) return
            lastResync = now
            handler.postDelayed({
                try {
                    if (p.isPlaying && p.playbackState == Player.STATE_READY && xfPhase == 0 && !xfWarm) {
                        com.ubayanda.milonative.util.ErrorLog.add("audio", "stall: restarting the output at ${p.currentPosition} ms to drop queued audio")
                        p.seekTo(p.currentPosition)
                    }
                } catch (_: Throwable) {}
            }, 150L)
        } catch (_: Throwable) {}
    }

    // keeps the home-screen widgets alive: the progress bar moves and "playing" stays fresh while music plays
    private val widgetBeat = object : Runnable {
        override fun run() {
            try { exo?.let { if (it.isPlaying) com.ubayanda.milonative.widget.WidgetUpdater.push(this@PlaybackService, it) } } catch (_: Throwable) {}
            handler.postDelayed(this, 5_000)
        }
    }

    // Phone calls: Bluetooth swaps from music mode to call mode, and music still playing across that swap comes out fast and high-pitched.
    // So like other players, pause when a call rings or starts (cell or app calls) and carry on when it ends.
    private var pausedByCall = 0L
    private var callClear = 0
    private val callWatch = object : Runnable {
        override fun run() {
            try {
                val p = exo
                val am = getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
                val inCall = am.mode == android.media.AudioManager.MODE_RINGTONE || am.mode == android.media.AudioManager.MODE_IN_CALL || am.mode == android.media.AudioManager.MODE_IN_COMMUNICATION
                if (p != null) {
                    if (inCall) {
                        callClear = 0
                        if (p.playWhenReady && pausedByCall == 0L) {
                            pausedByCall = System.currentTimeMillis()
                            p.pause()
                            com.ubayanda.milonative.util.ErrorLog.add("audio", "paused for a call")
                        }
                    } else if (pausedByCall != 0L) {
                        // wait a moment after the call so the phone has switched the headphones back
                        if (++callClear >= 3) {
                            val fresh = System.currentTimeMillis() - pausedByCall < 30 * 60_000L
                            pausedByCall = 0L; callClear = 0
                            if (fresh && !p.playWhenReady) p.play()
                        }
                    }
                }
            } catch (_: Throwable) {}
            handler.postDelayed(this, 500)
        }
    }

    /** Renderers whose audio goes through the parametric equaliser first. */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun eqRenderers() = object : androidx.media3.exoplayer.DefaultRenderersFactory(this) {
        override fun buildAudioSink(context: android.content.Context, enableFloatOutput: Boolean, enableAudioTrackPlaybackParams: Boolean): androidx.media3.exoplayer.audio.AudioSink =
            if (btCompat()) androidx.media3.exoplayer.audio.DefaultAudioSink.Builder(context).setEnableFloatOutput(false).setEnableAudioTrackPlaybackParams(false).build()
            else buildEqSink(context, enableFloatOutput, enableAudioTrackPlaybackParams)
    }

    private fun btCompat() = getSharedPreferences("milo_fx", MODE_PRIVATE).getBoolean("btcompat", false)

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun buildEqSink(context: android.content.Context, enableFloatOutput: Boolean, enableAudioTrackPlaybackParams: Boolean): androidx.media3.exoplayer.audio.AudioSink =
        androidx.media3.exoplayer.audio.DefaultAudioSink.Builder(context)
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
            // Round 62: back to Media3's default audio buffer. The Round 48 bigger buffer (600 ms) did not help and matches the ~0.6 s freeze seen at every crossfade swap.
            .setAudioProcessorChain(androidx.media3.exoplayer.audio.DefaultAudioSink.DefaultAudioProcessorChain(ParamEq.Processor(), GraphicEq.Processor(), Extras.Processor(), Spectrum.Tap()))
            .build()

    private fun ghostPlayer(): ExoPlayer = ghost ?: ExoPlayer.Builder(this, eqRenderers())
        .setMediaSourceFactory(YtSource.mediaSourceFactory(this))
        .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), false)
        .setHandleAudioBecomingNoisy(false)
        .build().also { g ->
            ghost = g
            // the old song's tail player: log its dropouts too, so a cut at the swap can be told apart from a stall of the main player
            g.addAnalyticsListener(object : androidx.media3.exoplayer.analytics.AnalyticsListener {
                override fun onAudioUnderrun(eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime, bufferSize: Int, bufferSizeMs: Long, elapsedSinceLastFeedMs: Long) {
                    com.ubayanda.milonative.util.ErrorLog.add("xfade", "new-song player underrun buffer=${bufferSizeMs}ms gap=${elapsedSinceLastFeedMs}ms")
                }
            })
            g.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (xfPhase != 0 || xfWarm) com.ubayanda.milonative.util.ErrorLog.add("xfade", "new-song player isPlaying=$isPlaying")
                }
            })
        }

    private fun endGhost() {
        xfPhase = 0
        xfLen = 0L
        xfWarm = false
        primedKey = null
        ghost?.let { try { it.stop(); it.clearMediaItems() } catch (_: Throwable) {} }
    }

    private val fader = object : Runnable {
        override fun run() {
            val p = exo ?: return
            Extras.loopCheck(p)
            val now = android.os.SystemClock.elapsedRealtime()
            val dt = now - lastTick
            lastTick = now
            try {
                tick(p, now, dt)
            } catch (_: Throwable) { endGhost(); if (p.volume != 1f) p.volume = 1f }
            handler.postDelayed(this, 25)
        }
    }

    /** The old song is done: the main player moves to the new song at the spot where the second player has got to. */
    private fun beginHandoff(p: ExoPlayer, g: ExoPlayer, now: Long) {
        xfPhase = 2
        xfHStart = now
        val lat = fxPrefs.getInt("xflat", 300)
        val target = g.currentPosition + lat
        if (p.currentMediaItemIndex == xfNextIdx) p.seekTo(target)
        else { xfOwn = true; p.seekTo(xfNextIdx, target) }
        com.ubayanda.milonative.util.ErrorLog.add("xfade", "hand-over: new song at ${g.currentPosition}ms, main player asked for ${target}ms")
    }

    private fun tick(p: ExoPlayer, now: Long, dt: Long) {
        val x = fxPrefs.getInt("xfade", 0) * 1000L
        var v = 1f
        val g = ghost
        if (xfPhase == 0) {
            if (xfWarm && g != null && g.playWhenReady != p.playWhenReady) g.playWhenReady = p.playWhenReady
            if (x > 0L && p.isPlaying && session?.player === p && p.repeatMode != Player.REPEAT_MODE_ONE && p.hasNextMediaItem()) {
                val dur = p.duration
                val item = p.currentMediaItem
                if (item != null && dur != C.TIME_UNSET && dur > x * 3) {
                    val toEnd = dur - p.currentPosition
                    val key = item.mediaId + "#" + p.currentMediaItemIndex
                    val ni = p.nextMediaItemIndex
                    if (toEnd <= x + 9000L && primedKey != key && xfDeadKey != key && ni != C.INDEX_UNSET) {
                        // get the next song ready on the second player well ahead (paused, silent)
                        val gp = ghostPlayer()
                        gp.stop()
                        gp.setMediaItem(p.getMediaItemAt(ni), 0L)
                        gp.volume = 0f
                        gp.playWhenReady = false
                        gp.setPlaybackSpeed(p.playbackParameters.speed)
                        gp.prepare()
                        primedKey = key
                        xfNextIdx = ni
                        xfWarm = false
                    }
                    // The second player needs a few hundred ms before it makes sound, so it is set running (silent) a little before the fade.
                    // The lead is learned: each fade measures how long it really took and adjusts the next one.
                    val lead = fxPrefs.getInt("xflead", 700).toLong()
                    if (primedKey == key && !xfWarm && toEnd <= x + lead && toEnd > 0L) {
                        val gp = ghostPlayer()
                        if (gp.playbackState == Player.STATE_READY) { gp.volume = 0f; gp.playWhenReady = true; xfWarm = true; xfWarmAt = now }
                    }
                    if (xfWarm && toEnd > x + lead + 1500L) {
                        // the person scrubbed back: stop and set the next song up again later
                        endGhost()
                    }
                    if (primedKey == key && xfWarm && toEnd <= x) {
                        val gp = ghostPlayer()
                        if (gp.isPlaying && gp.currentPosition > 60L) {
                            val took = now - xfWarmAt
                            fxPrefs.edit().putInt("xflead", ((lead + (took + 150L)) / 2).toInt().coerceIn(300, 1500)).apply()
                            xfPhase = 1
                            xfT0 = now
                            xfP0 = p.currentPosition
                            xfLen = toEnd.coerceIn(500L, x)
                            xfF = 0f
                            xfTrace = 0L
                            xfRgNew = gainOf(gp)
                            com.ubayanda.milonative.util.ErrorLog.add("xfade", "fade starts, ${xfLen}ms, new song took ${took}ms to start, next=${gp.currentMediaItem?.mediaId?.takeLast(6)}")
                        } else if (toEnd <= 600L) {
                            com.ubayanda.milonative.util.ErrorLog.add("xfade", "gave up: the next song wasn't playing in time (state ${gp.playbackState})")
                            xfDeadKey = key
                            endGhost()
                        }
                    } else if (primedKey == key && !xfWarm && toEnd <= 600L) {
                        com.ubayanda.milonative.util.ErrorLog.add("xfade", "gave up: the next song wasn't ready in time")
                        xfDeadKey = key
                        endGhost()
                    }
                }
            }
        } else if (xfPhase == 1) {
            // old song (main player) goes down, new song (second player) comes up
            if (g == null) { endGhost() } else {
                g.playWhenReady = p.playWhenReady
                if (!p.playWhenReady) xfT0 += dt.coerceAtMost(100L) // paused: hold the fade where it is
                val dur = p.duration
                val byClock = ((now - xfT0).toFloat() / xfLen).coerceIn(0f, 1f)
                // progress follows the old song's own position, so it reaches silence exactly where that song ends
                val byPos = if (dur != C.TIME_UNSET && dur - xfP0 > 300L) ((p.currentPosition - xfP0).toFloat() / (dur - xfP0)).coerceIn(0f, 1f) else byClock
                val f = maxOf(xfF, byPos)
                xfF = f
                if (now - xfTrace >= 300L) {
                    xfTrace = now
                    com.ubayanda.milonative.util.ErrorLog.add("xfade", "f=${"%.2f".format(f)} old@${p.currentPosition}/${p.playbackState} vol=${"%.2f".format(p.volume)} new@${g.currentPosition}/${g.playbackState} vol=${"%.2f".format(g.volume)} playing old=${p.isPlaying} new=${g.isPlaying}")
                }
                if (p.currentPosition < xfP0 - 1500L) {
                    com.ubayanda.milonative.util.ErrorLog.add("xfade", "cut short: the old song was moved back")
                    endGhost()
                } else {
                    val gn = XFade.gains(f, fxPrefs.getInt("xfcurve", 0))
                    v = gn.first
                    g.volume = (gn.second * xfRgNew).coerceIn(0f, 1f)
                    if (f >= 0.99f && xfPhase == 1) beginHandoff(p, g, now)
                }
            }
        } else if (xfPhase == 2) {
            // the new song plays on the second player at full volume while the main player moves over to it (silent)
            v = 0f
            if (g == null) { endGhost() } else {
                g.volume = xfRgNew.coerceIn(0f, 1f)
                g.playWhenReady = p.playWhenReady
                if (p.playbackState == Player.STATE_READY && p.isPlaying && now - xfHStart > 120L) {
                    val d = g.currentPosition - p.currentPosition   // positive: the main player is behind
                    val lat = fxPrefs.getInt("xflat", 300)
                    if (Math.abs(d) < 1500L) fxPrefs.edit().putInt("xflat", (lat + d * 0.7).toInt().coerceIn(50, 1200)).apply()
                    com.ubayanda.milonative.util.ErrorLog.add("xfade", "main player ready, ${d}ms behind the new song")
                    xfPhase = 3
                    xfH2 = now
                } else if (now - xfHStart > 7000L) {
                    com.ubayanda.milonative.util.ErrorLog.add("xfade", "gave up: the main player didn't take over (state ${p.playbackState})")
                    endGhost()
                }
            }
        } else {
            // blend the two copies of the new song, then drop the second player
            if (g == null) { endGhost() } else {
                val f = ((now - xfH2) / 350f).coerceIn(0f, 1f)
                g.playWhenReady = p.playWhenReady
                val gn = XFade.gains(f, XFade.EQUAL_POWER)
                v = gn.second
                g.volume = (gn.first * xfRgNew).coerceIn(0f, 1f)
                if (f >= 1f) endGhost()
            }
        }
        // sleep timer: ease the volume down over the last 30 seconds
        val se = fxPrefs.getLong("sleep_end", 0L)
        if (se > 0L) {
            val left = se - System.currentTimeMillis()
            if (left in 1..30_000L) {
                v = minOf(v, left / 30_000f)
                ghost?.takeIf { xfPhase != 0 }?.let { it.volume = minOf(it.volume, left / 30_000f) }
            }
        }
        val tv = v * rgScale
        if (p.volume != tv) p.volume = tv
    }

    /** Loudness matching: songs that are louder than average are turned down to match (never boosted, so nothing clips). */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun updateGain(p: Player) { rgScale = gainOf(p) }

    /** The loudness scale (0.05..1) for the song [p] is on. */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun gainOf(p: Player): Float {
        if (!fxPrefs.getBoolean("rg", true)) return 1f
        var db = Double.NaN
        val u = p.currentMediaItem?.localConfiguration?.uri
        if (u?.scheme == "yt") {
            val l = com.ubayanda.milonative.data.YtMusic.loudness(u.schemeSpecificPart.orEmpty())
            if (l != 0.0) db = -l
        } else {
            for (g in p.currentTracks.groups) {
                if (g.type != C.TRACK_TYPE_AUDIO || !g.isSelected) continue
                val md = g.getTrackFormat(0).metadata ?: continue
                for (i in 0 until md.length()) {
                    val e = md[i]
                    var key = ""; var value = ""
                    if (e is androidx.media3.extractor.metadata.id3.TextInformationFrame) { key = e.description.orEmpty(); value = e.values.firstOrNull().orEmpty() }
                    else if (e is androidx.media3.extractor.metadata.vorbis.VorbisComment) { key = e.key; value = e.value }
                    val num = Regex("-?\\d+(\\.\\d+)?").find(value)?.value?.toDoubleOrNull() ?: continue
                    if (key.equals("REPLAYGAIN_TRACK_GAIN", true)) db = num
                    else if (key.equals("R128_TRACK_GAIN", true) && db.isNaN()) db = num / 256.0 + 5.0
                }
            }
        }
        return if (db.isNaN()) 1f else Math.pow(10.0, minOf(db, 0.0) / 20.0).toFloat().coerceIn(0.05f, 1f)
    }

    /** Gapless for streamed songs: look up the next song's link while the current one plays. */
    private fun warmNext(p: Player) {
        val n = p.currentMediaItemIndex + 1
        if (n >= p.mediaItemCount) return
        val u = p.getMediaItemAt(n).localConfiguration?.uri ?: return
        if (u.scheme != "yt") return
        val vid = u.schemeSpecificPart.orEmpty()
        if (vid.isEmpty() || com.ubayanda.milonative.data.OfflineStore.has(vid)) return
        Thread { com.ubayanda.milonative.data.YtMusic.warm(vid) }.start()
    }

    override fun onCreate() {
        super.onCreate()
        com.ubayanda.milonative.util.ErrorLog.init(this)
        ParamEq.init(this)
        Extras.init(this)
        GraphicEq.init(this)
        val player = ExoPlayer.Builder(this, eqRenderers())
            .setMediaSourceFactory(YtSource.mediaSourceFactory(this))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        exo = player
        if (btCompat()) {
            // let the phone's audio chip play the music: it keeps going while the app is busy or in the background
            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                .setAudioOffloadPreferences(androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences.Builder()
                    .setAudioOffloadMode(androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED)
                    .build())
                .build()
        }
        handler.post(fader)
        handler.post(callWatch)
        handler.post(widgetBeat)
        com.ubayanda.milonative.widget.WidgetUpdater.serviceUp(this)
        handler.postDelayed(resumeSaver, 10_000)
        // tapping the playback notification (or the lock-screen player) opens milo.
        val open = android.app.PendingIntent.getActivity(
            this, 0, Intent(this, com.ubayanda.milonative.MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaLibrarySession.Builder(this, player, CarLibrary(this)).setSessionActivity(open).build()
        AudioFx.attach(this, player.audioSessionId)
        castBridge = CastBridge(this, { session }, player).also { it.init() }
        BtOutput.start(this, { player.isPlaying }, { player.play() })
        svcScope.launch { Extras.state.collect { player.skipSilenceEnabled = it.skipSilence } }
        svcScope.launch { BtOutput.now.collect { ParamEq.useDevice(if (it.kind == "speaker") "default" else it.name) } }
        player.addAnalyticsListener(object : androidx.media3.exoplayer.analytics.AnalyticsListener {
            override fun onAudioUnderrun(eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime, bufferSize: Int, bufferSizeMs: Long, elapsedSinceLastFeedMs: Long) {
                com.ubayanda.milonative.util.ErrorLog.add("audio", "underrun buffer=${bufferSizeMs}ms gap=${elapsedSinceLastFeedMs}ms speed=${player.playbackParameters.speed} skipSilence=${player.skipSilenceEnabled}")
                resyncAfterStall(player)
            }
        })
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                com.ubayanda.milonative.util.ErrorLog.add("player", error.errorCodeName + ": " + error.message + (error.cause?.let { " / " + it.javaClass.simpleName + ": " + it.message } ?: "") + " | " + player.currentMediaItem?.localConfiguration?.uri)
                // a YouTube link that expired or was refused: look up a fresh one and carry on from the same place, once per song
                val u = player.currentMediaItem?.localConfiguration?.uri
                val vid = if (u?.scheme == "yt") u.schemeSpecificPart.orEmpty() else ""
                if (vid.isNotEmpty() && ytRetried.add(vid)) {
                    com.ubayanda.milonative.data.YtMusic.forget(vid)
                    player.prepare()
                    return
                }
                // dead stream: drop it right here so the session never carries a broken timeline
                endGhost(); player.stop(); player.clearMediaItems()
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    val id = player.currentMediaItem?.mediaId
                    if (id != null && resumedFor != id) {
                        resumedFor = id
                        val d = player.duration
                        // arriving through a crossfade: start the song from its beginning; jumping to a saved spot would cut the fade
                        if (d >= Resume.LONG_MS && xfPhase == 0) Resume.get(this@PlaybackService, id)?.let { (pos, sp) ->
                            if (pos > 10_000 && pos < d - 30_000) { player.seekTo(pos); if (sp > 0f && sp != player.playbackParameters.speed) player.setPlaybackSpeed(sp) }
                        }
                    }
                } else if (state == Player.STATE_ENDED) {
                    player.currentMediaItem?.mediaId?.let { Resume.drop(this@PlaybackService, it) }
                }
            }
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                // the main player reached the end of the old song by itself during the fade: that is the hand-over
                if (xfOwn) xfOwn = false
                else if (xfPhase == 1 && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO && ghost != null) beginHandoff(player, ghost!!, android.os.SystemClock.elapsedRealtime())
                else if (xfPhase != 0 || xfWarm || primedKey != null) {
                    // the person skipped, or the queue changed, while the next song was being got ready or faded in: drop the second player at once
                    if (xfPhase != 0) com.ubayanda.milonative.util.ErrorLog.add("xfade", "cut short by a song change (reason $reason)")
                    endGhost()
                }
                resumedFor = null
                mediaItem?.mediaId?.let { RecentStore.add(this@PlaybackService, it) }
                Extras.clearLoop()
                rgPrev = rgScale
                updateGain(player)
                warmNext(player)
            }
            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) { updateGain(player) }
            override fun onIsPlayingChanged(isPlaying: Boolean) { if (isPlaying) ytRetried.clear() else BtOutput.saveVolume(this@PlaybackService) }
            override fun onEvents(p: Player, events: Player.Events) {
                if (events.containsAny(
                        Player.EVENT_MEDIA_ITEM_TRANSITION, Player.EVENT_IS_PLAYING_CHANGED, Player.EVENT_PLAY_WHEN_READY_CHANGED,
                        Player.EVENT_POSITION_DISCONTINUITY, Player.EVENT_PLAYBACK_STATE_CHANGED, Player.EVENT_MEDIA_METADATA_CHANGED,
                    )
                ) com.ubayanda.milonative.widget.WidgetUpdater.push(this@PlaybackService, p)
            }
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                AudioFx.attach(this@PlaybackService, audioSessionId)
            }
        })
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = session?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        try { castBridge?.release() } catch (_: Throwable) {}
        castBridge = null
        BtOutput.stop(this)
        svcScope.cancel()
        handler.removeCallbacksAndMessages(null)
        com.ubayanda.milonative.widget.WidgetUpdater.clear(this)
        try { ghost?.release() } catch (_: Throwable) {}
        ghost = null
        exo = null
        AudioFx.release()
        session?.let {
            it.player.release()
            it.release()
        }
        session = null
        super.onDestroy()
    }
}
