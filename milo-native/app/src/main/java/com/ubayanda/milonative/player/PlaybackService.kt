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

    /** Fades the volume out at the end of a song and in at the start of the next, as long as crossfade is on. */
    private val fader = object : Runnable {
        override fun run() {
            val p = exo ?: return
            val x = fxPrefs.getInt("xfade", 0) * 1000L
            var v = 1f
            if (x > 0 && p.isPlaying) {
                val dur = p.duration
                if (dur > 0 && dur != C.TIME_UNSET) {
                    val pos = p.currentPosition
                    val toEnd = dur - pos
                    val canNext = p.hasNextMediaItem() || p.repeatMode != Player.REPEAT_MODE_OFF
                    if (canNext && toEnd < x) v = (toEnd.toFloat() / x).coerceIn(0f, 1f)
                    if (pos < x) v = minOf(v, (pos.toFloat() / x).coerceIn(0f, 1f))
                }
            }
            if (p.volume != v) p.volume = v
            handler.postDelayed(this, 100)
        }
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
                player.stop(); player.clearMediaItems()
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
