package com.ubayanda.milonative.player

import com.ubayanda.milonative.crossmix.TransportEffect
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
    private var fading: Player? = null
    private var lastV = 1f
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
    private var xfAudibleAt = 0L   // when the second player was first really making sound after it was set running
    private var xfHStart = 0L
    private var xfH2 = 0L
    private var xfTrace = 0L
    /** Set just before the crossfade itself moves the main player to another song, so a skip by the person can be told apart. */
    private var xfOwn = false
    // blend skips: a skip started the next song on the second player and the two cross over on a clock, not on the old song's own end
    private var xfSkip = false
    private var xfSkipAt = 0L
    private var xfSkipLen = 0L
    private var xfClockOnly = false
    private var xfWarm = false     // the next song is already running silently on the second player
    private var lastTick = 0L

    // After a stall (app switch, notification shade, screen wake) the audio left in the output's queue plays back in a rush to catch up.
    // Throw that stale audio away instead: one clean restart at the current spot, so the interrupted moment is simply lost, not replayed fast.
    // Never during a crossfade, never for live radio, and at most once per 8 seconds.
    private var lastResync = 0L
    private fun resyncAfterStall(p: Player) {
        try {
            if (!fxPrefs.getBoolean("resync", true)) return
            if (xfPhase != 0 || xfWarm || directorOrNull?.isTransitioning == true) return
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

    // Audio focus and interruptions are handled here, not by each ExoPlayer (Mochy's way, see PlayerFocus.kt); "own_focus" in Profile turns it back to Media3's
    private fun ownFocus() = fxPrefs.getBoolean("own_focus", true)
    private val focus: PlayerFocus by lazy {
        PlayerFocus(this, handler, { exo }, { xfPhase == 0 && !xfWarm && directorOrNull?.isTransitioning != true }, { applyVolumeNow() })
    }
    private fun duckScale(): Float = if (ownFocus()) focus.duck else 1f

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
                            if (ownFocus()) focus.pausedForCall() else p.pause()
                            com.ubayanda.milonative.util.ErrorLog.add("audio", "paused for a call")
                        }
                    } else if (pausedByCall != 0L) {
                        // wait a moment after the call so the phone has switched the headphones back
                        if (++callClear >= 3) {
                            val fresh = System.currentTimeMillis() - pausedByCall < 30 * 60_000L
                            pausedByCall = 0L; callClear = 0
                            if (fresh && !p.playWhenReady) { if (ownFocus()) focus.callEnded() else p.play() }
                        }
                    }
                }
            } catch (_: Throwable) {}
            handler.postDelayed(this, 500)
        }
    }

    /** Renderers whose audio goes through the parametric equaliser first. */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun eqRenderers(xp: com.ubayanda.milonative.player.audio.CrossmixTransitionAudioProcessor? = null) = object : androidx.media3.exoplayer.DefaultRenderersFactory(this) {
        override fun buildAudioSink(context: android.content.Context, enableFloatOutput: Boolean, enableAudioTrackPlaybackParams: Boolean): androidx.media3.exoplayer.audio.AudioSink =
            if (btCompat()) androidx.media3.exoplayer.audio.DefaultAudioSink.Builder(context).setEnableFloatOutput(false).setEnableAudioTrackPlaybackParams(false).build()
            else buildEqSink(context, enableFloatOutput, enableAudioTrackPlaybackParams, xp)
    }

    private val hapticProcs: MutableSet<com.ubayanda.milonative.player.audio.HapticAudioProcessor> =
        java.util.Collections.synchronizedSet(java.util.Collections.newSetFromMap(java.util.WeakHashMap()))

    private val hapticVibrator: android.os.Vibrator? by lazy {
        try {
            if (android.os.Build.VERSION.SDK_INT >= 31) (getSystemService(VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager).defaultVibrator
            else @Suppress("DEPRECATION") (getSystemService(VIBRATOR_SERVICE) as android.os.Vibrator)
        } catch (_: Throwable) { null }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun makeHaptic(xp: com.ubayanda.milonative.player.audio.CrossmixTransitionAudioProcessor): com.ubayanda.milonative.player.audio.HapticAudioProcessor {
        val hp = com.ubayanda.milonative.player.audio.HapticAudioProcessor { amp, kick ->
            if (exo?.let { crossmixProcessors[it] } === xp) {
                // the buzz is timed to what is heard, which is a little after what the processor has just read
                svcScope.launch(kotlinx.coroutines.Dispatchers.Default) { kotlinx.coroutines.delay(150); hapticBuzz(amp, kick) }
            }
        }
        applyHaptic(hp)
        hapticProcs.add(hp)
        return hp
    }

    private fun applyHaptic(hp: com.ubayanda.milonative.player.audio.HapticAudioProcessor) {
        val sens = com.ubayanda.milonative.player.audio.HapticSensitivity.values()
        val focus = com.ubayanda.milonative.player.audio.HapticFocus.values()
        hp.enabled = fxPrefs.getBoolean("haptic_on", false)
        hp.sensitivity = sens.getOrElse(fxPrefs.getInt("haptic_sens", 1)) { sens[1] }
        hp.focus = focus.getOrElse(fxPrefs.getInt("haptic_focus", 2)) { focus[2] }
    }

    private fun hapticBuzz(amplitude: Int, kick: Boolean) {
        val v = hapticVibrator ?: return
        try {
            if (!v.hasVibrator()) return
            val scale = (amplitude / 255f).coerceIn(0.01f, 1f)
            val sdk = android.os.Build.VERSION.SDK_INT
            if (sdk >= 30) {
                val prim = when {
                    kick && v.areAllPrimitivesSupported(android.os.VibrationEffect.Composition.PRIMITIVE_CLICK) -> android.os.VibrationEffect.Composition.PRIMITIVE_CLICK
                    v.areAllPrimitivesSupported(android.os.VibrationEffect.Composition.PRIMITIVE_LOW_TICK) -> android.os.VibrationEffect.Composition.PRIMITIVE_LOW_TICK
                    v.areAllPrimitivesSupported(android.os.VibrationEffect.Composition.PRIMITIVE_TICK) -> android.os.VibrationEffect.Composition.PRIMITIVE_TICK
                    else -> -1
                }
                if (prim != -1) v.vibrate(android.os.VibrationEffect.startComposition().addPrimitive(prim, scale).compose())
                else if (v.hasAmplitudeControl()) v.vibrate(android.os.VibrationEffect.createOneShot(20, amplitude.coerceIn(1, 255)))
            } else if (sdk >= 26 && v.hasAmplitudeControl()) {
                v.vibrate(android.os.VibrationEffect.createOneShot(20, amplitude.coerceIn(1, 255)))
            }
        } catch (_: Throwable) {}
    }

    private fun btCompat() = getSharedPreferences("milo_fx", MODE_PRIVATE).getBoolean("btcompat", false)

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun buildEqSink(context: android.content.Context, enableFloatOutput: Boolean, enableAudioTrackPlaybackParams: Boolean, xp: com.ubayanda.milonative.player.audio.CrossmixTransitionAudioProcessor? = null): androidx.media3.exoplayer.audio.AudioSink {
        // vibration that follows the music; only the player that is playing may buzz, and the classic crossfade's tail player never does
        val haptic = if (xp != null) makeHaptic(xp) else null
        val sink = androidx.media3.exoplayer.audio.DefaultAudioSink.Builder(context)
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
            // Round 62: back to Media3's default audio buffer. The Round 48 bigger buffer (600 ms) did not help and matches the ~0.6 s freeze seen at every crossfade swap.
            .setAudioProcessorChain(androidx.media3.exoplayer.audio.DefaultAudioSink.DefaultAudioProcessorChain(*listOfNotNull(ParamEq.Processor(), GraphicEq.Processor(), Reverb.Processor(), Extras.Processor(), com.ubayanda.milonative.player.audio.LoudnessAudioProcessor(), Spectrum.Tap(), xp, haptic).toTypedArray()))
            .build()
        // Crossmix needs to know where in its song each buffer sits: the clock sink tells the processor
        return if (xp != null) com.ubayanda.milonative.player.audio.CrossmixClockSink(sink, xp) else sink
    }

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
        XFadeLive.info.value = null
        xfLen = 0L
        xfWarm = false
        xfAudibleAt = 0L
        xfSkip = false
        xfClockOnly = false
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

    /** the album of the song at [index], when "Keep albums gapless" is on (Sound > Crossfade, on by default) */
    private fun albumAt(p: Player, index: Int): String? {
        if (!fxPrefs.getBoolean("gapless_albums", true) || index < 0 || index >= p.mediaItemCount) return null
        return p.getMediaItemAt(index).mediaMetadata.albumTitle?.toString()?.trim()?.takeIf { it.isNotEmpty() }
    }

    /** the next song is on the same album as this one, so a fixed crossfade would only break the join */
    private fun nextIsSameAlbum(p: Player): Boolean {
        val ni = p.nextMediaItemIndex
        if (ni == C.INDEX_UNSET) return false
        val a = albumAt(p, p.currentMediaItemIndex) ?: return false
        return a == albumAt(p, ni)
    }

    private fun tick(p: ExoPlayer, now: Long, dt: Long) {
        val x = if (crossmixOn() || nextIsSameAlbum(p)) 0L else fxPrefs.getInt("xfade", 0) * 1000L
        var v = 1f
        val g = ghost
        if (xfPhase == 0) {
            if (xfWarm && g != null && g.playWhenReady != p.playWhenReady) g.playWhenReady = p.playWhenReady
            if (xfSkip) {
                // the song asked for is starting on the second player: cross over the moment it makes sound
                if (g == null) { endGhost() } else {
                    g.playWhenReady = p.playWhenReady
                    if (xfAudibleAt == 0L && g.isPlaying && g.currentPosition > 60L) xfAudibleAt = now
                    if (xfAudibleAt != 0L) {
                        xfSkip = false
                        xfPhase = 1; xfT0 = now; xfP0 = p.currentPosition; xfLen = xfSkipLen; xfF = 0f; xfTrace = 0L
                        xfClockOnly = true
                        xfRgNew = gainOf(g)
                        com.ubayanda.milonative.util.ErrorLog.add("xfade", "blend skip: ${xfLen}ms, new song took ${now - xfSkipAt}ms to start, next=${g.currentMediaItem?.mediaId?.takeLast(6)}")
                    } else if (now - xfSkipAt > 4000L) {
                        val idx = xfNextIdx
                        com.ubayanda.milonative.util.ErrorLog.add("xfade", "blend skip gave up: the song wasn't playing in time, plain jump")
                        endGhost()
                        if (idx in 0 until p.mediaItemCount) p.seekTo(idx, 0L)
                    }
                }
            } else if (x > 0L && p.isPlaying && (session?.player === p || session?.player === fading) && p.repeatMode != Player.REPEAT_MODE_ONE && p.hasNextMediaItem()) {
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
                        xfAudibleAt = 0L
                    }
                    // The second player needs a few hundred ms before it makes sound, so it is set running (silent) a little before the fade.
                    // The lead is learned: each fade measures how long it really took and adjusts the next one.
                    val lead = fxPrefs.getInt("xflead2", 600).toLong()
                    if (primedKey == key && !xfWarm && toEnd <= x + lead && toEnd > 0L) {
                        val gp = ghostPlayer()
                        if (gp.playbackState == Player.STATE_READY) { gp.volume = 0f; gp.playWhenReady = true; xfWarm = true; xfWarmAt = now; xfAudibleAt = 0L }
                    }
                    if (xfWarm && xfAudibleAt == 0L) {
                        // measure how long the second player really takes to make sound, and learn the lead from that
                        val gp = ghostPlayer()
                        if (gp.isPlaying && gp.currentPosition > 60L) {
                            xfAudibleAt = now
                            val lat = now - xfWarmAt
                            fxPrefs.edit().putInt("xflead2", ((lead + lat + 150L) / 2).toInt().coerceIn(250, 1500)).apply()
                        }
                    }
                    if (xfWarm && toEnd > x + lead + 1500L) {
                        // the person scrubbed back: stop and set the next song up again later
                        endGhost()
                    }
                    if (primedKey == key && xfWarm && toEnd <= x) {
                        val gp = ghostPlayer()
                        if (xfAudibleAt != 0L && gp.isPlaying) {
                            val took = xfAudibleAt - xfWarmAt
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
                val f = maxOf(xfF, if (xfClockOnly) byClock else byPos)
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
                    com.ubayanda.milonative.util.ErrorLog.add("xfade", "main player ready after ${now - xfHStart}ms, ${d}ms behind the new song")
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
                // the two copies of the new song are the same sound, so they add up: a straight blend (not equal power) keeps the volume level
                val gn = XFade.gains(f, XFade.LINEAR)
                v = gn.second
                g.volume = (gn.first * xfRgNew).coerceIn(0f, 1f)
                if (f >= 1f) endGhost()
            }
        }
        // tell the screens which song is coming in, from the first moment of the fade
        if (xfPhase != 0 && g != null && xfNextIdx >= 0) XFadeLive.info.value = XFadeLive.Info(xfNextIdx, g.currentPosition.coerceAtLeast(0L), if (g.duration > 0L) g.duration else 0L)
        else if (XFadeLive.info.value != null) XFadeLive.info.value = null
        // sleep timer: ease the volume down over the last 30 seconds
        val se = fxPrefs.getLong("sleep_end", 0L)
        if (se > 0L) {
            val left = se - System.currentTimeMillis()
            if (left in 1..30_000L) {
                v = minOf(v, left / 30_000f)
                ghost?.takeIf { xfPhase != 0 }?.let { it.volume = minOf(it.volume, left / 30_000f) }
            }
        }
        lastV = v
        val tv = v * rgScale * transport.gain * duckScale()
        if (p.volume != tv) p.volume = tv
    }

    private fun applyVolumeNow() {
        val p = exo ?: return
        // while two songs are crossfading the tick owns the volumes
        if (xfPhase != 0) return
        val tv = lastV * rgScale * transport.gain * duckScale()
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
            // Mochy's ReplayGainTags rules: track or album gain (the other as fallback) + preamp, never boosted, never past the file's peak
            var tg = Double.NaN; var tp = Double.NaN; var ag = Double.NaN; var ap = Double.NaN
            for (g in p.currentTracks.groups) {
                if (g.type != C.TRACK_TYPE_AUDIO || !g.isSelected) continue
                val md = g.getTrackFormat(0).metadata ?: continue
                for (i in 0 until md.length()) {
                    val e = md[i]
                    var key = ""; var value = ""
                    if (e is androidx.media3.extractor.metadata.id3.TextInformationFrame) { key = e.description.orEmpty(); value = e.values.firstOrNull().orEmpty() }
                    else if (e is androidx.media3.extractor.metadata.vorbis.VorbisComment) { key = e.key; value = e.value }
                    val num = Regex("[-+]?\\d+(\\.\\d+)?").find(value)?.value?.toDoubleOrNull() ?: continue
                    when (key.uppercase()) {
                        "REPLAYGAIN_TRACK_GAIN" -> tg = num
                        "REPLAYGAIN_TRACK_PEAK" -> tp = num
                        "REPLAYGAIN_ALBUM_GAIN" -> ag = num
                        "REPLAYGAIN_ALBUM_PEAK" -> ap = num
                        "R128_TRACK_GAIN" -> if (tg.isNaN()) tg = num / 256.0 + 5.0
                    }
                }
            }
            val album = fxPrefs.getInt("rg_mode", 0) == 1
            val gain = if (album) (if (!ag.isNaN()) ag else tg) else (if (!tg.isNaN()) tg else ag)
            val peak = if (album) (if (!ap.isNaN()) ap else tp) else (if (!tp.isNaN()) tp else ap)
            if (!gain.isNaN()) {
                var f = Math.pow(10.0, (gain + fxPrefs.getInt("rg_pre", 0)) / 20.0)
                if (!peak.isNaN() && peak > 0.0) f = minOf(f, 1.0 / peak)
                return f.toFloat().coerceIn(0.03f, 1f)
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

    private val mainAnalytics = object : androidx.media3.exoplayer.analytics.AnalyticsListener {
            override fun onAudioUnderrun(eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime, bufferSize: Int, bufferSizeMs: Long, elapsedSinceLastFeedMs: Long) {
                com.ubayanda.milonative.util.ErrorLog.add("audio", "underrun buffer=${bufferSizeMs}ms gap=${elapsedSinceLastFeedMs}ms speed=${cur().playbackParameters.speed} skipSilence=${cur().skipSilenceEnabled}")
                resyncAfterStall(cur())
            }
        }

    private val mainListener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                com.ubayanda.milonative.util.ErrorLog.add("player", error.errorCodeName + ": " + error.message + (error.cause?.let { " / " + it.javaClass.simpleName + ": " + it.message } ?: "") + " | " + cur().currentMediaItem?.localConfiguration?.uri)
                // a YouTube link that expired or was refused: look up a fresh one and carry on from the same place, once per song
                val u = cur().currentMediaItem?.localConfiguration?.uri
                val vid = if (u?.scheme == "yt") u.schemeSpecificPart.orEmpty() else ""
                if (vid.isNotEmpty() && ytRetried.add(vid)) {
                    com.ubayanda.milonative.data.YtMusic.forget(vid)
                    cur().prepare()
                    return
                }
                // dead stream: drop it right here so the session never carries a broken timeline
                endGhost(); cur().stop(); cur().clearMediaItems()
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) xdir()?.schedule()
                if (state == Player.STATE_READY) {
                    val id = cur().currentMediaItem?.mediaId
                    if (id != null && resumedFor != id) {
                        resumedFor = id
                        val d = cur().duration
                        // arriving through a crossfade: start the song from its beginning; jumping to a saved spot would cut the fade
                        if (d >= Resume.LONG_MS && xfPhase == 0) Resume.get(this@PlaybackService, id)?.let { (pos, sp) ->
                            if (pos > 10_000 && pos < d - 30_000) { cur().seekTo(pos); if (sp > 0f && sp != cur().playbackParameters.speed) cur().setPlaybackSpeed(sp) }
                        }
                    }
                } else if (state == Player.STATE_ENDED) {
                    cur().currentMediaItem?.mediaId?.let { Resume.drop(this@PlaybackService, it) }
                }
            }
            override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, reason: Int) {
                if (!promotingCrossmix) xdir()?.onItemChanged()
                if (!crossmixOn()) try { com.ubayanda.milonative.crossmix.CrossmixLab.look(this@PlaybackService, mediaItem, if (cur().hasNextMediaItem()) cur().getMediaItemAt(cur().nextMediaItemIndex) else null) } catch (_: Throwable) {}
                // the main cur() reached the end of the old song by itself during the fade: that is the hand-over
                if (xfOwn) xfOwn = false
                else if (xfPhase == 1 && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO && ghost != null) beginHandoff(cur(), ghost!!, android.os.SystemClock.elapsedRealtime())
                else if (xfPhase != 0 || xfWarm || primedKey != null) {
                    // the person skipped, or the queue changed, while the next song was being got ready or faded in: drop the second cur() at once
                    if (xfPhase != 0) com.ubayanda.milonative.util.ErrorLog.add("xfade", "cut short by a song change (reason $reason)")
                    endGhost()
                }
                resumedFor = null
                mediaItem?.mediaId?.let { RecentStore.add(this@PlaybackService, it) }
                Extras.clearLoop()
                rgPrev = rgScale
                updateGain(cur())
                warmNext(cur())
                xdir()?.schedule()
            }
            override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
                if (reason == Player.DISCONTINUITY_REASON_SEEK) xdir()?.onUserSeek()
            }
            override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
                if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) xdir()?.schedule()
            }
            override fun onTracksChanged(tracks: androidx.media3.common.Tracks) { updateGain(cur()) }
            override fun onIsPlayingChanged(isPlaying: Boolean) { if (isPlaying) ytRetried.clear() else BtOutput.saveVolume(this@PlaybackService) }
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) { if (ownFocus()) focus.onPlayWhenReadyChanged(playWhenReady, reason) }
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
        }


    // ======== Crossmix (Round 120): Mochy Music's engine plans and plays the hand-over on two real players ========
    // The next song gets its own player (own audio chain, own crossmix processor); the director arms both sides, locks the beats and fades,
    // and at the hand-over the new player becomes the session's player while the old one plays out and is released.
    private val musicAttrs = AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build()
    private val crossmixProcessors = HashMap<Player, com.ubayanda.milonative.player.audio.CrossmixTransitionAudioProcessor>()
    private var outgoingExo: ExoPlayer? = null
    private var promotingCrossmix = false
    private var directorOrNull: CrossmixDirector? = null
    private val director: CrossmixDirector
        get() = directorOrNull ?: CrossmixDirector(
            svcScope, CrossmixHost(),
            com.ubayanda.milonative.crossmix.CrossmixAnalysisRepository(applicationContext) { YtSource.dataSourceFactory(applicationContext) },
        ).also { directorOrNull = it }

    private fun cur(): ExoPlayer = exo!!
    private fun crossmixOn() = CrossmixLive.on(fxPrefs)
    private fun xdir(): CrossmixDirector? = if (crossmixOn()) director else null

    // a changed Crossmix choice (engine, length, tempo, style...) re-plans the next hand-over
    private val xPrefsWatch = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key in CrossmixLive.keys) handler.post { try { director.onSettingsChanged() } catch (_: Throwable) {} }
        if (key?.startsWith("loud_") == true) com.ubayanda.milonative.player.audio.LoudnessFx.load(fxPrefs)
        if (key == "rg_mode" || key == "rg_pre" || key == "rg") handler.post { updateGain(cur()); applyVolumeNow() }
        if (key?.startsWith("haptic_") == true) handler.post { synchronized(hapticProcs) { hapticProcs.toList() }.forEach { applyHaptic(it) } }
        if (key == "tfade" || key == "tfx") handler.post { try { syncTransport() } catch (_: Throwable) {} }
    }

    /** Mochy's pause / play / skip fades and effects (TransportTransitions); length from the "Soft pause & skip" slider, effect from the chips under it */
    private val transport: TransportTransitions by lazy { TransportTransitions({ svcScope }, TransportHost()).also { syncTransport(it) } }

    private fun syncTransport(t: TransportTransitions = transport) {
        val ms = fxPrefs.getInt("tfade", 300)
        t.lengthMs = ms.toFloat()
        t.enabled = ms >= 50
        t.effect = TransportEffect.values().getOrElse(fxPrefs.getInt("tfx", 0)) { TransportEffect.OFF }
    }

    private inner class TransportHost : TransportTransitions.Host {
        override val player: ExoPlayer get() = cur()
        override fun processorFor(player: ExoPlayer) = crossmixProcessors[player]
        override fun targetVolume(): Float = lastV * rgScale
        // not while a crossfade or a crossmix runs, nor while casting: those drive the volume themselves
        override fun canFade(): Boolean = xfPhase == 0 && session?.player === fading && directorOrNull?.isTransitioning != true
        // blend skips (Mochy): the song asked for starts on the second player and the two cross over on the press
        override fun blendsSkips(): Boolean = canBlend()
        override fun blendSkip(previous: Boolean): Boolean {
            val p = cur()
            val target = when {
                // back a few seconds in starts the song over, which stays a plain jump
                previous && (p.currentPosition > 3_500L || !p.hasPreviousMediaItem()) -> return false
                previous -> p.previousMediaItemIndex
                !p.hasNextMediaItem() -> return false
                else -> p.nextMediaItemIndex
            }
            return startBlend(target)
        }
        override fun blendJump(index: Int): Boolean = startBlend(index)
        override fun beatMsFor(mediaId: String?): Double? =
            mediaId?.let { directorOrNull?.cachedBpm(it) }?.takeIf { it in 40f..240f }?.let { 60_000.0 / it }
    }

    /** blend skips can cross over right now: it's on, a song is really playing, nothing else is mixing and we're not casting */
    private fun canBlend(): Boolean {
        val p = exo ?: return false
        return fxPrefs.getBoolean("blend_skips", false) && xfPhase == 0 && !xfWarm && !xfSkip &&
            session?.player === fading && directorOrNull?.isTransitioning != true &&
            p.isPlaying && !p.isCurrentMediaItemLive && p.repeatMode != Player.REPEAT_MODE_ONE
    }

    private fun startBlend(target: Int): Boolean {
        val p = cur()
        if (!canBlend() || target == C.INDEX_UNSET || target < 0 || target >= p.mediaItemCount || target == p.currentMediaItemIndex) return false
        return try {
            val gp = ghostPlayer()
            gp.stop()
            gp.setMediaItem(p.getMediaItemAt(target), 0L)
            gp.volume = 0f
            gp.playWhenReady = p.playWhenReady
            gp.setPlaybackSpeed(p.playbackParameters.speed)
            gp.prepare()
            val now = android.os.SystemClock.elapsedRealtime()
            xfNextIdx = target
            xfSkip = true
            xfSkipAt = now
            xfSkipLen = transport.lengthMs.toLong().coerceIn(600L, 3000L)
            xfWarm = true
            xfWarmAt = now
            xfAudibleAt = 0L
            primedKey = "skip#" + target
            true
        } catch (_: Throwable) { endGhost(); false }
    }

    private fun makeFading(p: ExoPlayer): Player = TransportFadingPlayer(p, transport)

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private fun newCrossmixPlayer(): ExoPlayer {
        val xp = com.ubayanda.milonative.player.audio.CrossmixTransitionAudioProcessor()
        val p = ExoPlayer.Builder(this, eqRenderers(xp))
            .setMediaSourceFactory(YtSource.mediaSourceFactory(this))
            .setAudioAttributes(musicAttrs, /* handleAudioFocus = */ false)
            .setHandleAudioBecomingNoisy(false)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        crossmixProcessors[p] = xp
        // the player still waiting to take over only logs its dropouts: a stall here must not make the playing song restart
        p.addAnalyticsListener(object : androidx.media3.exoplayer.analytics.AnalyticsListener {
            override fun onAudioUnderrun(eventTime: androidx.media3.exoplayer.analytics.AnalyticsListener.EventTime, bufferSize: Int, bufferSizeMs: Long, elapsedSinceLastFeedMs: Long) {
                com.ubayanda.milonative.util.ErrorLog.add("crossmix", "second player underrun buffer=${bufferSizeMs}ms gap=${elapsedSinceLastFeedMs}ms")
            }
        })
        return p
    }

    private fun copyPlayOrder(from: Player, to: ExoPlayer) {
        val timeline = from.currentTimeline
        if (timeline.isEmpty || timeline.windowCount != to.mediaItemCount) return
        val order = ArrayList<Int>(timeline.windowCount)
        var index = timeline.getFirstWindowIndex(true)
        while (index != C.INDEX_UNSET && order.size < timeline.windowCount) {
            order += index
            index = timeline.getNextWindowIndex(index, Player.REPEAT_MODE_OFF, true)
        }
        if (order.size != timeline.windowCount) return
        to.setShuffleOrder(androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder(order.toIntArray(), System.currentTimeMillis()))
    }

    /** what the Crossmix director needs from the service: players, the session, the queue */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    private inner class CrossmixHost : CrossmixDirector.Host {
        override val currentPlayer: ExoPlayer get() = cur()

        override fun processorFor(player: Player): com.ubayanda.milonative.player.audio.CrossmixTransitionAudioProcessor? = crossmixProcessors[player]

        override fun createIncoming(index: Int, positionMs: Long, parameters: androidx.media3.common.PlaybackParameters): ExoPlayer {
            val from = cur()
            val incoming = newCrossmixPlayer()
            incoming.setMediaItems((0 until from.mediaItemCount).map { from.getMediaItemAt(it) })
            copyPlayOrder(from, incoming)
            incoming.repeatMode = from.repeatMode
            incoming.shuffleModeEnabled = from.shuffleModeEnabled
            // the tempo is set before anything plays: changing it later drains and resets the audio chain, which would cut into the transition
            incoming.playbackParameters = parameters
            incoming.volume = from.volume
            incoming.seekTo(index, positionMs)
            incoming.playWhenReady = false
            // not prepared here: the director arms its processor first
            return incoming
        }

        override fun promote(incoming: ExoPlayer, outgoing: ExoPlayer) {
            outgoingExo = outgoing
            outgoing.removeListener(mainListener)
            outgoing.removeAnalyticsListener(mainAnalytics)
            // the outgoing player must never roll on into the next song itself, and lets go of audio focus and the headphones-unplugged pause
            outgoing.pauseAtEndOfMediaItems = true
            outgoing.setHandleAudioBecomingNoisy(false)
            outgoing.setAudioAttributes(musicAttrs, false)

            exo = incoming
            incoming.setAudioAttributes(musicAttrs, !ownFocus())
            incoming.setHandleAudioBecomingNoisy(true)
            incoming.skipSilenceEnabled = Extras.state.value.skipSilence
            incoming.addListener(mainListener)
            incoming.addAnalyticsListener(mainAnalytics)
            // pause and resume both sides together; an outgoing side that has played out stays put
            incoming.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    val out = outgoingExo
                    if (!director.isTransitioning || out == null) { incoming.removeListener(this); return }
                    if (isPlaying) {
                        val finished = out.playbackState == Player.STATE_ENDED || (out.duration > 0 && out.currentPosition >= out.duration - 250)
                        if (!finished) out.play()
                    } else out.pause()
                }
            })
            val fp = makeFading(incoming)
            fading = fp
            try { session?.player = fp } catch (e: Throwable) { com.ubayanda.milonative.util.ErrorLog.add("crossmix", "could not swap the session's player: " + e.message) }
            AudioFx.attach(this@PlaybackService, incoming.audioSessionId)
            // a cast bridge built on the old player would restore the queue onto a released one: build a new one on this player
            try { castBridge?.release() } catch (_: Throwable) {}
            castBridge = CastBridge(this@PlaybackService, { session }, incoming, fp).also { it.init() }

            // the hooks a song change runs (loudness, recents, loop reset) for the song now playing, which this player reached by a seek before it was ours
            promotingCrossmix = true
            try { mainListener.onMediaItemTransition(incoming.currentMediaItem, Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) } finally { promotingCrossmix = false }
            resumedFor = incoming.currentMediaItem?.mediaId
            applyVolumeNow()
            com.ubayanda.milonative.widget.WidgetUpdater.push(this@PlaybackService, incoming)
        }

        override fun release(outgoing: ExoPlayer) {
            runCatching { outgoing.stop(); outgoing.clearMediaItems(); outgoing.release() }
            crossmixProcessors.remove(outgoing)
            if (outgoingExo === outgoing) outgoingExo = null
        }

        override fun discard(incoming: ExoPlayer) {
            runCatching { incoming.stop(); incoming.release() }
            crossmixProcessors.remove(incoming)
        }

        override fun crossmixBlocker(): String? {
            val p = cur()
            return when {
                !crossmixOn() -> "Transitions are set to Classic"
                btCompat() -> "Bluetooth compatibility mode is on"
                xfPhase != 0 || xfWarm -> "A classic crossfade is running"
                p.repeatMode == Player.REPEAT_MODE_ONE -> "Repeat one plays the song straight round"
                session?.player !== fading -> "Casting: Crossmix waits until you're back"
                else -> null
            }
        }

        override fun nextIndex(): Int = cur().let { if (it.repeatMode == Player.REPEAT_MODE_ONE) it.currentMediaItemIndex else it.nextMediaItemIndex }

        // the next song's lyrics, when they've been fetched before or can be now: synced lines say exactly where its voice comes in
        override suspend fun lyrics(item: androidx.media3.common.MediaItem): String? {
            val id = item.mediaId.toLongOrNull() ?: return null
            val md = item.mediaMetadata
            val t = com.ubayanda.milonative.model.Track(id, item.localConfiguration?.uri?.toString().orEmpty(), md.title?.toString().orEmpty(), md.artist?.toString().orEmpty(), md.albumTitle?.toString().orEmpty(), 0L, 0L, 0, 0, 0L)
            val l = kotlinx.coroutines.withTimeoutOrNull(8_000L) { com.ubayanda.milonative.data.LyricsRepo(applicationContext).get(t) } ?: return null
            if (l.synced.isEmpty()) return null
            return l.synced.joinToString("\n") { "[%02d:%02d.%02d]%s".format(it.timeMs / 60_000, (it.timeMs / 1000) % 60, (it.timeMs % 1000) / 10, it.text) }
        }

        override fun baseParameters(): androidx.media3.common.PlaybackParameters = cur().playbackParameters

        override fun onMixingChanged(mixing: Boolean) { CrossmixLive.mixing.value = mixing }

        override fun onStatus(status: String) { CrossmixLive.status.value = status }

        // gapless albums (Mochy): songs of one album are checked for running into each other, and the director plays them straight through when their audio really does
        override fun albumOf(index: Int): String? = albumAt(cur(), index)

        override fun settings() = CrossmixLive.settings(fxPrefs)

        override fun learnedStartLatencyMs(): Double? = if (fxPrefs.contains(CrossmixLive.LATENCY)) fxPrefs.getFloat(CrossmixLive.LATENCY, 0f).toDouble() else null

        override fun onStartLatencyLearned(ms: Double) { fxPrefs.edit().putFloat(CrossmixLive.LATENCY, ms.toFloat()).apply() }
    }

    override fun onCreate() {
        super.onCreate()
        com.ubayanda.milonative.util.ErrorLog.init(this)
        ParamEq.init(this)
        Extras.init(this)
        GraphicEq.init(this)
        Reverb.init(this)
        com.ubayanda.milonative.player.audio.LoudnessFx.load(fxPrefs)
        val mainXp = com.ubayanda.milonative.player.audio.CrossmixTransitionAudioProcessor()
        val player = ExoPlayer.Builder(this, eqRenderers(mainXp))
            .setMediaSourceFactory(YtSource.mediaSourceFactory(this))
            .setAudioAttributes(musicAttrs, /* handleAudioFocus = */ !ownFocus())
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        crossmixProcessors[player] = mainXp
        exo = player
        getSharedPreferences("milo_fx", MODE_PRIVATE).registerOnSharedPreferenceChangeListener(xPrefsWatch)
        svcScope.launch { com.ubayanda.milonative.player.CrossmixLive.test.collect { xdir()?.testMix() } }
        svcScope.launch { com.ubayanda.milonative.player.CrossmixLive.status.collect { if (it.isNotBlank() && crossmixOn()) com.ubayanda.milonative.util.ErrorLog.add("crossmix", it) } }
        if (btCompat()) {
            // let the phone's audio chip play the music: it keeps going while the app is busy or in the background
            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                .setAudioOffloadPreferences(androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences.Builder()
                    .setAudioOffloadMode(androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED)
                    .build())
                .build()
        }
        if (ownFocus()) focus.start()
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
        // pause / play / skips ease the volume ("Soft pause & skip" in the Sound panel); 0 = off
        val fp = makeFading(player)
        fading = fp
        session = MediaLibrarySession.Builder(this, fp, CarLibrary(this)).setSessionActivity(open).build()
        AudioFx.attach(this, player.audioSessionId)
        castBridge = CastBridge(this, { session }, player, fp).also { it.init() }
        BtOutput.start(this, { exo?.isPlaying == true }, { exo?.play() })
        svcScope.launch { Extras.state.collect { exo?.skipSilenceEnabled = it.skipSilence } }
        svcScope.launch { BtOutput.now.collect { ParamEq.useDevice(if (it.kind == "speaker") "default" else it.name) } }
        player.addAnalyticsListener(mainAnalytics)
        player.addListener(mainListener)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = session?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        try { castBridge?.release() } catch (_: Throwable) {}
        castBridge = null
        if (ownFocus()) try { focus.stop() } catch (_: Throwable) {}
        BtOutput.stop(this)
        svcScope.cancel()
        handler.removeCallbacksAndMessages(null)
        com.ubayanda.milonative.widget.WidgetUpdater.clear(this)
        try { ghost?.release() } catch (_: Throwable) {}
        ghost = null
        try { getSharedPreferences("milo_fx", MODE_PRIVATE).unregisterOnSharedPreferenceChangeListener(xPrefsWatch) } catch (_: Throwable) {}
        try { transport.cancel() } catch (_: Throwable) {}
        try { directorOrNull?.release() } catch (_: Throwable) {}
        try { outgoingExo?.release() } catch (_: Throwable) {}
        outgoingExo = null
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
