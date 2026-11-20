package com.ubayanda.milonative.player

import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer

// from Mochy Music (GPL-3.0); its USB-DAC volume handling is left out
// what the media session drives (notification, lock screen, headset, car): its pause, play and skips get the same fades as the app's buttons
@UnstableApi
class TransportFadingPlayer(
    val exoPlayer: ExoPlayer,
    private val transport: TransportTransitions,
) : ForwardingPlayer(exoPlayer) {

    override fun play() = transport.play()

    override fun pause() = transport.pause()

    override fun setPlayWhenReady(playWhenReady: Boolean) = if (playWhenReady) transport.play() else transport.pause()

    override fun seekToNext() = transport.skipNext()

    override fun seekToNextMediaItem() = transport.skipNext()

    override fun seekToPrevious() = transport.skipPrevious()

    override fun seekToPreviousMediaItem() = transport.skipPrevious()

    override fun seekTo(positionMs: Long) {
        transport.cancel()
        super.seekTo(positionMs)
    }

    // a song picked from the queue on the lock screen or in the car fades or blends in like one picked in the app
    override fun seekToDefaultPosition(mediaItemIndex: Int) {
        if (mediaItemIndex == exoPlayer.currentMediaItemIndex) {
            transport.cancel()
            super.seekToDefaultPosition(mediaItemIndex)
        } else {
            transport.pickFromQueue { mediaItemIndex }
        }
    }

    override fun stop() {
        transport.cancel()
        super.stop()
    }

}
