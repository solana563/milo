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

    // ---- true crossfade: a second, short-lived player carries the tail of the old song while the main player starts the next one ----
    private var ghost: ExoPlayer? = null
    private var primedKey: String? = null
    private var xfT0 = 0L
    private var xfLen = 0L
    /** The new song hasn't started yet: the old one stays at full volume until it has, then the fade begins. */
    private var xfWait = false
    private var xfStart = 0L
    /** Set just before the crossfade itself moves to the next song, so a skip by the person can be told apart. */
    private var xfOwn = false
    private var xfF = 0f   // how far the fade has got; it only ever moves forward
    private var xfG0 = 0L  // where the old song's tail was when the fade began
    private var xfTrace = 0L
    private var lastTick = 0L

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
            // a roomier audio buffer rides out short stalls (phone call, app switch, screen wake) that Bluetooth makes audible
            .setAudioTrackBufferSizeProvider(androidx.media3.exoplayer.audio.DefaultAudioTrackBufferSizeProvider.Builder()
                .setMinPcmBufferDurationUs(600_000).setMaxPcmBufferDurationUs(1_500_000).setPcmBufferMultiplicationFactor(6).build())
            .setAudioProcessorChain(androidx.media3.exoplayer.audio.DefaultAudioSink.DefaultAudioProcessorChain(ParamEq.Processor(), GraphicEq.Processor(), Extras.Processor(), Spectrum.Tap()))
            .build()

    private fun ghostPlayer(): ExoPlayer = ghost ?: ExoPlayer.Builder(this, eqRenderers())
        .setMediaSourceFactory(YtSource.mediaSourceFactory(this))
        .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), false)
        .setHandleAudioBecomingNoisy(false)
        .build().also { ghost = it }

    private fun endGhost() {
        xfLen = 0L
        xfWait = false
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

    private fun tick(p: ExoPlayer, now: Long, dt: Long) {
        val x = fxPrefs.getInt("xfade", 0) * 1000L
        var v = 1f
        if (xfLen > 0L) {
            // a crossfade is running: old song (ghost) goes down, new song goes up, equal power
            val g = ghost
            if (xfWait) {
                // hold the old song at full volume until the new one is really playing (a slow link would otherwise fade into silence)
                // wait for the new song AND the old song's tail to be playable, or the old song drops out and comes back mid-fade
                val ghostOk = g == null || g.playbackState == Player.STATE_READY || g.playbackState == Player.STATE_ENDED
                if (p.playbackState == Player.STATE_READY && p.playWhenReady && ghostOk) {
                    xfWait = false
                    xfF = 0f
                    xfG0 = g?.currentPosition ?: 0L
                    xfTrace = 0L
                    xfT0 = now
                    val rem = if (g != null && g.duration != C.TIME_UNSET) g.duration - g.currentPosition else xfLen
                    xfLen = rem.coerceIn(400L, xfLen)
                    com.ubayanda.milonative.util.ErrorLog.add("xfade", "fade starts, ${xfLen}ms, waited ${now - xfStart}ms")
                } else if (now - xfStart > 8000L) {
                    com.ubayanda.milonative.util.ErrorLog.add("xfade", "gave up waiting: new=${p.playbackState} old=${g?.playbackState}")
                    endGhost()   // the next song didn't start: stop waiting
                }
                v = 0f
                g?.let { it.volume = rgPrev; it.playWhenReady = p.playWhenReady }
            } else {
                if (!p.playWhenReady) xfT0 += dt.coerceAtMost(100L) // paused: hold the fade where it is
                // progress follows the old song's own playback position, so it reaches silence exactly where that song ends;
                // the clock is only the fallback while the old song's length is unknown
                val gd = g?.duration ?: C.TIME_UNSET
                val byClock = ((now - xfT0).toFloat() / xfLen).coerceIn(0f, 1f)
                val byPos = if (g != null && gd != C.TIME_UNSET && gd - xfG0 > 300L) ((g.currentPosition - xfG0).toFloat() / (gd - xfG0)).coerceIn(0f, 1f) else byClock
                val f = maxOf(xfF, byPos)
                xfF = f
                if (now - xfTrace >= 300L) {
                    xfTrace = now
                    com.ubayanda.milonative.util.ErrorLog.add("xfade", "f=${"%.2f".format(f)} clock=${"%.2f".format(byClock)} new=${p.currentMediaItem?.mediaId?.takeLast(6)}@${p.currentPosition}/${p.playbackState} vol=${"%.2f".format(p.volume)} old@${g?.currentPosition}/${g?.playbackState} vol=${"%.2f".format(g?.volume ?: 0f)} idx=${p.currentMediaItemIndex}")
                }
                val gn = XFade.gains(f, fxPrefs.getInt("xfcurve", 0))
                v = gn.second
                g?.let {
                    it.volume = gn.first * rgPrev
                    it.playWhenReady = p.playWhenReady
                }
                if (f >= 1f || g?.playbackState == Player.STATE_ENDED) endGhost()
            }
        } else if (x > 0L && p.isPlaying && session?.player === p && p.repeatMode != Player.REPEAT_MODE_ONE && p.hasNextMediaItem()) {
            val dur = p.duration
            val item = p.currentMediaItem
            if (item != null && dur != C.TIME_UNSET && dur > x * 3) {
                val toEnd = dur - p.currentPosition
                val key = item.mediaId + "#" + p.currentMediaItemIndex
                if (toEnd <= x + 4000L && primedKey != key) {
                    val g = ghostPlayer()
                    g.stop()
                    g.setSeekParameters(androidx.media3.exoplayer.SeekParameters.EXACT)
                    g.setMediaItem(item, (dur - x).coerceAtLeast(0L))
                    g.volume = 0f
                    g.playWhenReady = false
                    g.setPlaybackSpeed(p.playbackParameters.speed)
                    g.prepare()
                    primedKey = key
                }
                if (toEnd <= x && primedKey == key) {
                    val g = ghostPlayer()
                    // already waiting at the right spot; only nudge it if the two clocks disagree
                    if (Math.abs(g.currentPosition - p.currentPosition) > 1500L) { com.ubayanda.milonative.util.ErrorLog.add("xfade", "old song moved ${g.currentPosition - p.currentPosition}ms"); g.seekTo(p.currentPosition) }
                    g.volume = rgPrev
                    g.playWhenReady = true
                    xfLen = toEnd.coerceIn(500L, x)
                    xfT0 = now
                    xfStart = now
                    xfWait = true
                    xfOwn = true
                    v = 0f
                    p.volume = 0f
                    p.seekToNextMediaItem()
                }
            }
        }
        // sleep timer: ease the volume down over the last 30 seconds
        val se = fxPrefs.getLong("sleep_end", 0L)
        if (se > 0L) {
            val left = se - System.currentTimeMillis()
            if (left in 1..30_000L) {
                v = minOf(v, left / 30_000f)
                ghost?.takeIf { xfLen > 0L }?.let { it.volume = minOf(it.volume, left / 30_000f) }
            }
        }
        val tv = v * rgScale
        if (p.volume != tv) p.volume = tv
    }

    /** Loudness matching: songs that are louder than average are turned down to match (never boosted, so nothing clips). */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun updateGain(p: Player) {
        if (!fxPrefs.getBoolean("rg", true)) { rgScale = 1f; return }
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
        rgScale = if (db.isNaN()) 1f else Math.pow(10.0, minOf(db, 0.0) / 20.0).toFloat().coerceIn(0.05f, 1f)
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
                        if (d >= Resume.LONG_MS) Resume.get(this@PlaybackService, id)?.let { (pos, sp) ->
                            if (pos > 10_000 && pos < d - 30_000) { player.seekTo(pos); if (sp > 0f && sp != player.playbackParameters.speed) player.setPlaybackSpeed(sp) }
                        }
                    }
                } else if (state == Player.STATE_ENDED) {
                    player.currentMediaItem?.mediaId?.let { Resume.drop(this@PlaybackService, it) }
                }
            }
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                // the person skipped, or the queue changed, in the middle of a crossfade: drop the old song's tail at once
                if (xfOwn) xfOwn = false else if (xfLen > 0L || ghost != null) { com.ubayanda.milonative.util.ErrorLog.add("xfade", "cut short by a song change (reason $reason)"); endGhost() }
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
