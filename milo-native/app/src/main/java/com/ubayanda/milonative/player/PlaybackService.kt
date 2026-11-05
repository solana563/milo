package com.ubayanda.milonative.player

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * The music player itself. It lives in a foreground service, so playback carries on with the screen off,
 * the lock-screen / notification controls and headset buttons all work, and it never depends on the UI being open.
 */
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
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
    private var lastTick = 0L

    /** Renderers whose audio goes through the parametric equaliser first. */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun eqRenderers() = object : androidx.media3.exoplayer.DefaultRenderersFactory(this) {
        override fun buildAudioSink(context: android.content.Context, enableFloatOutput: Boolean, enableAudioTrackPlaybackParams: Boolean): androidx.media3.exoplayer.audio.AudioSink =
            androidx.media3.exoplayer.audio.DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .setAudioProcessorChain(androidx.media3.exoplayer.audio.DefaultAudioSink.DefaultAudioProcessorChain(ParamEq.Processor(), GraphicEq.Processor(), Extras.Processor(), Spectrum.Tap()))
                .build()
    }

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
                if (p.playbackState == Player.STATE_READY && p.playWhenReady) {
                    xfWait = false
                    xfT0 = now
                    val rem = if (g != null && g.duration != C.TIME_UNSET) g.duration - g.currentPosition else xfLen
                    xfLen = rem.coerceIn(400L, xfLen)
                } else if (now - xfStart > 8000L) {
                    endGhost()   // the next song didn't start: stop waiting
                }
                v = 0f
                g?.let { it.volume = rgPrev; it.playWhenReady = p.playWhenReady }
            } else {
                if (!p.playWhenReady) xfT0 += dt // paused: hold the fade where it is
                val f = ((now - xfT0).toFloat() / xfLen).coerceIn(0f, 1f)
                v = Math.sin(f * Math.PI / 2).toFloat()
                g?.let {
                    it.volume = Math.cos(f * Math.PI / 2).toFloat() * rgPrev
                    it.playWhenReady = p.playWhenReady
                }
                if (f >= 1f) endGhost()
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
                    if (Math.abs(g.currentPosition - p.currentPosition) > 300L) g.seekTo(p.currentPosition)
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
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        exo = player
        handler.post(fader)
        handler.postDelayed(resumeSaver, 10_000)
        session = MediaSession.Builder(this, player).build()
        AudioFx.attach(this, player.audioSessionId)
        castBridge = CastBridge(this, { session }, player).also { it.init() }
        BtOutput.start(this, { player.isPlaying }, { player.play() })
        svcScope.launch { Extras.state.collect { player.skipSilenceEnabled = it.skipSilence } }
        svcScope.launch { BtOutput.now.collect { ParamEq.useDevice(if (it.kind == "speaker") "default" else it.name) } }
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
                if (xfOwn) xfOwn = false else if (xfLen > 0L || ghost != null) endGhost()
                resumedFor = null
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

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

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
