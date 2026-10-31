package com.ubayanda.milonative.player

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * The music player itself. It lives in a foreground service, so playback carries on with the screen off,
 * the lock-screen / notification controls and headset buttons all work, and it never depends on the UI being open.
 */
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null
    private var exo: ExoPlayer? = null
    private var castBridge: CastBridge? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val fxPrefs by lazy { getSharedPreferences("milo_fx", MODE_PRIVATE) }

    // ---- true crossfade: a second, short-lived player carries the tail of the old song while the main player starts the next one ----
    private var ghost: ExoPlayer? = null
    private var primedKey: String? = null
    private var xfT0 = 0L
    private var xfLen = 0L
    private var lastTick = 0L

    private fun ghostPlayer(): ExoPlayer = ghost ?: ExoPlayer.Builder(this)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), false)
        .setHandleAudioBecomingNoisy(false)
        .build().also { ghost = it }

    private fun endGhost() {
        xfLen = 0L
        primedKey = null
        ghost?.let { try { it.stop(); it.clearMediaItems() } catch (_: Throwable) {} }
    }

    private val fader = object : Runnable {
        override fun run() {
            val p = exo ?: return
            val now = android.os.SystemClock.elapsedRealtime()
            val dt = now - lastTick
            lastTick = now
            try {
                tick(p, now, dt)
            } catch (_: Throwable) { endGhost(); if (p.volume != 1f) p.volume = 1f }
            handler.postDelayed(this, 50)
        }
    }

    private fun tick(p: ExoPlayer, now: Long, dt: Long) {
        val x = fxPrefs.getInt("xfade", 0) * 1000L
        var v = 1f
        if (xfLen > 0L) {
            // a crossfade is running: old song (ghost) goes down, new song goes up, equal power
            if (!p.playWhenReady) xfT0 += dt // paused: hold the fade where it is
            val f = ((now - xfT0).toFloat() / xfLen).coerceIn(0f, 1f)
            v = Math.sin(f * Math.PI / 2).toFloat()
            ghost?.let { g ->
                g.volume = Math.cos(f * Math.PI / 2).toFloat()
                g.playWhenReady = p.playWhenReady
            }
            if (f >= 1f) endGhost()
        } else if (x > 0L && p.isPlaying && session?.player === p && p.repeatMode != Player.REPEAT_MODE_ONE && p.hasNextMediaItem()) {
            val dur = p.duration
            val item = p.currentMediaItem
            if (item != null && dur != C.TIME_UNSET && dur > x * 3) {
                val toEnd = dur - p.currentPosition
                val key = item.mediaId + "#" + p.currentMediaItemIndex
                if (toEnd <= x + 4000L && primedKey != key) {
                    val g = ghostPlayer()
                    g.stop()
                    g.setMediaItem(item, (dur - x).coerceAtLeast(0L))
                    g.volume = 0f
                    g.playWhenReady = false
                    g.setPlaybackSpeed(p.playbackParameters.speed)
                    g.prepare()
                    primedKey = key
                }
                if (toEnd <= x && primedKey == key) {
                    val g = ghostPlayer()
                    g.seekTo(p.currentPosition)
                    g.volume = 1f
                    g.playWhenReady = true
                    xfLen = toEnd.coerceIn(500L, x)
                    xfT0 = now
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
        if (p.volume != v) p.volume = v
    }

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this)
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
        session = MediaSession.Builder(this, player).build()
        AudioFx.attach(this, player.audioSessionId)
        castBridge = CastBridge(this, { session }, player).also { it.init() }
        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                // dead stream: drop it right here so the session never carries a broken timeline
                endGhost(); player.stop(); player.clearMediaItems()
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
        handler.removeCallbacksAndMessages(null)
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
