package com.ubayanda.milonative.player

import android.os.Handler
import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * What the media session drives (the app's buttons, the notification, the lock screen, headsets, the car): pause, play and skips ease the
 * volume instead of cutting. Idea from Mochy Music's TransportFadingPlayer (GPL-3.0, github.com/realidkroo/Mochy-Music); written for milo.
 *
 * The fade is only a number ([gain], 0..1) that the service multiplies into the player's volume; [apply] is called after every step.
 * While the player is paused the gain is back at 1, so anything that plays the ExoPlayer directly (a Bluetooth resume, the widget) is heard at full volume.
 */
class FadingPlayer(
    private val exo: ExoPlayer,
    private val handler: Handler,
    /** length of the fade in ms; 0 = off */
    private val lengthMs: () -> Long,
    /** false while a crossfade is running or while casting: the buttons then act at once, as they always did */
    private val canFade: () -> Boolean,
    private val apply: () -> Unit,
) : ForwardingPlayer(exo) {

    @Volatile var gain = 1f
        private set

    private var ramp: Runnable? = null
    private var fadingOut = false
    private var songKey: String? = null

    private fun key() = exo.currentMediaItem?.mediaId + "#" + exo.currentMediaItemIndex

    private fun smooth(k: Float) = k * k * (3f - 2f * k)

    private fun stopRamp() {
        ramp?.let { handler.removeCallbacks(it) }
        ramp = null
    }

    /** Back to normal at once (a seek, a stop, a new queue). */
    fun reset() {
        stopRamp(); fadingOut = false; gain = 1f; apply()
    }

    private fun go(to: Float, ms: Long, done: (() -> Unit)? = null) {
        stopRamp()
        val from = gain
        val t0 = SystemClock.elapsedRealtime()
        val r = object : Runnable {
            override fun run() {
                val k = ((SystemClock.elapsedRealtime() - t0).toFloat() / ms.coerceAtLeast(1L)).coerceIn(0f, 1f)
                gain = from + (to - from) * smooth(k)
                apply()
                if (k < 1f) handler.postDelayed(this, 16) else { ramp = null; done?.invoke() }
            }
        }
        ramp = r
        r.run()
    }

    private fun fadeable(): Boolean =
        lengthMs() > 0L && canFade() && exo.playWhenReady && exo.playbackState == Player.STATE_READY && exo.mediaItemCount > 0

    // ---- pause / play ----
    override fun pause() = setPlayWhenReady(false)

    override fun play() = setPlayWhenReady(true)

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        if (!playWhenReady) {
            if (fadingOut) { reset(); super.setPlayWhenReady(false); return }   // a second press means now
            if (!fadeable()) { reset(); super.setPlayWhenReady(false); return }
            fadingOut = true
            val len = lengthMs()
            val k = key()
            go(0f, len) {
                fadingOut = false
                if (key() == k) super.setPlayWhenReady(false)
                gain = 1f; apply()                  // paused now: nothing to hear, and the next play starts from the fade-in below
            }
        } else {
            if (fadingOut) {
                // changed my mind in the middle of a fade out: come back up from where it had got to
                fadingOut = false
                go(1f, (lengthMs() * (1f - gain)).toLong().coerceAtLeast(150L))
                return
            }
            val coming = !exo.playWhenReady && lengthMs() > 0L && canFade() && exo.mediaItemCount > 0
            if (coming) {
                gain = 0f; apply()
                super.setPlayWhenReady(true)
                go(1f, lengthMs())
            } else {
                super.setPlayWhenReady(true)
            }
        }
    }

    // ---- skips ----
    private fun skip(action: () -> Unit) {
        if (fadingOut || !fadeable()) { reset(); action(); return }
        fadingOut = true
        songKey = key()
        val out = (lengthMs() / 2).coerceIn(80L, 1_500L)
        go(0f, out) {
            fadingOut = false
            if (key() == songKey) {
                action()
                gain = 0f; apply()
                go(1f, lengthMs())
            } else { gain = 1f; apply() }           // the queue changed under the fade: leave the new song alone
        }
    }

    override fun seekToNext() = skip { super.seekToNext() }
    override fun seekToNextMediaItem() = skip { super.seekToNextMediaItem() }
    override fun seekToPrevious() = skip { super.seekToPrevious() }
    override fun seekToPreviousMediaItem() = skip { super.seekToPreviousMediaItem() }

    // a song picked from the queue (or the lock screen) eases in like a skip
    override fun seekToDefaultPosition(mediaItemIndex: Int) {
        if (mediaItemIndex == exo.currentMediaItemIndex) { reset(); super.seekToDefaultPosition(mediaItemIndex) }
        else skip { super.seekToDefaultPosition(mediaItemIndex) }
    }

    override fun seekTo(positionMs: Long) { reset(); super.seekTo(positionMs) }

    override fun seekTo(mediaItemIndex: Int, positionMs: Long) { reset(); super.seekTo(mediaItemIndex, positionMs) }

    override fun stop() { reset(); super.stop() }

    override fun setMediaItems(mediaItems: MutableList<androidx.media3.common.MediaItem>, resetPosition: Boolean) { reset(); super.setMediaItems(mediaItems, resetPosition) }

    override fun setMediaItems(mediaItems: MutableList<androidx.media3.common.MediaItem>, startIndex: Int, startPositionMs: Long) { reset(); super.setMediaItems(mediaItems, startIndex, startPositionMs) }
}
