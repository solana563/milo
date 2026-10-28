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
        session = MediaSession.Builder(this, player).build()
        AudioFx.attach(this, player.audioSessionId)
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
        AudioFx.release()
        session?.let {
            it.player.release()
            it.release()
        }
        session = null
        super.onDestroy()
    }
}
