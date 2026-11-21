// Audio focus and interruptions, handled by the service the way Mochy Music's MusicService does it (GPL-3.0,
// github.com/realidkroo/Mochy-Music: handleAudioFocusChange / requestAudioFocus / callWatcher), instead of by each ExoPlayer.
//
// Why: milo's main player used to be handed to Media3's own focus handling, which resumes the instant focus comes back. After a call that
// is while the phone is still switching Bluetooth from call mode back to music mode, and music started across that switch plays fast
// and high-pitched. Here the service owns focus (so it also survives the crossmix player swap), pauses the moment focus goes, and
// only resumes once the audio mode is back to normal and has stayed there, on a freshly opened output.
package com.ubayanda.milonative.player

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.ubayanda.milonative.util.ErrorLog

class PlayerFocus(
    private val ctx: Context,
    private val handler: Handler,
    private val player: () -> ExoPlayer?,
    /** false while a crossfade / crossmix is moving the volumes or players about: no reopening the output then */
    private val canRebuild: () -> Boolean,
    private val onDuckChanged: () -> Unit,
) {
    private val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var request: AudioFocusRequest? = null
    private var hasFocus = false

    /** it was playing when focus (or a call) took it away, so it carries on afterwards */
    private var wasPlaying = false
    /** pauses and plays made here, which must not be taken for the person's own */
    private var selfChange = false
    private var resuming = false
    private var pausedAt = 0L
    /** the headphones / speaker changed while it was paused or playing: reopen the output before the next sound */
    private var routeDirty = false

    /** the volume scale while another app asks us to duck (a fifth, like Mochy) */
    @Volatile var duck = 1f
        private set

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change -> handler.post { onFocusChange(change) } }

    private fun focusRequest(): AudioFocusRequest = request ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
        .setOnAudioFocusChangeListener(focusListener, handler)
        .setAcceptsDelayedFocusGain(true)
        .build().also { request = it }

    // ---- what the player tells us ----

    /** from the main player's listener: play was asked for (true) or paused/stopped by someone (false) */
    fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        if (selfChange) return
        if (playWhenReady) {
            if (!requestFocus()) {
                // focus denied or delayed: don't play over a call; it starts when focus arrives
                val p = player() ?: return
                wasPlaying = true
                pausedAt = android.os.SystemClock.elapsedRealtime()
                selfPause(p)
            } else wasPlaying = false
        } else if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST || reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) {
            // the person paused: let go of focus so other apps can have it, and don't resume by ourselves
            wasPlaying = false
            abandon()
        }
    }

    private fun requestFocus(): Boolean {
        if (hasFocus) return true
        val r = try { am.requestAudioFocus(focusRequest()) } catch (_: Throwable) { AudioManager.AUDIOFOCUS_REQUEST_FAILED }
        hasFocus = r == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        if (hasFocus) setDuck(1f)
        return hasFocus
    }

    fun abandon() {
        try { request?.let { am.abandonAudioFocusRequest(it) } } catch (_: Throwable) {}
        hasFocus = false
        setDuck(1f)
    }

    private fun setDuck(d: Float) {
        if (duck == d) return
        duck = d
        onDuckChanged()
    }

    private fun selfPause(p: ExoPlayer) {
        selfChange = true
        try { p.pause() } finally { selfChange = false }
    }

    private fun onFocusChange(change: Int) {
        val p = player() ?: return
        when (change) {
            AudioManager.AUDIOFOCUS_GAIN, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK -> {
                hasFocus = true
                setDuck(1f)
                if (wasPlaying && !p.playWhenReady) resumeWhenSettled("focus back")
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                // another player took over for good: stop, and don't start by ourselves later
                hasFocus = false
                wasPlaying = false
                if (p.playWhenReady) { ErrorLog.add("audio", "focus lost for good: paused"); selfPause(p) }
                abandon()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                hasFocus = false
                if (p.playWhenReady) {
                    wasPlaying = true
                    pausedAt = android.os.SystemClock.elapsedRealtime()
                    ErrorLog.add("audio", "focus lost for a moment (call or another app): paused")
                    selfPause(p)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                hasFocus = false
                setDuck(0.2f)
            }
        }
    }

    // ---- calls (the audio mode), for phones that don't send a focus change on a ring ----

    /** the call watcher paused the music; it resumes the same way after the call */
    fun pausedForCall() {
        val p = player() ?: return
        wasPlaying = true
        pausedAt = android.os.SystemClock.elapsedRealtime()
        selfPause(p)
    }

    fun callEnded() {
        val p = player() ?: return
        if (!p.playWhenReady) resumeWhenSettled("call ended")
    }

    private fun inCallMode(): Boolean =
        am.mode == AudioManager.MODE_RINGTONE || am.mode == AudioManager.MODE_IN_CALL || am.mode == AudioManager.MODE_IN_COMMUNICATION

    @Suppress("DEPRECATION")
    private fun scoOn(): Boolean = try { am.isBluetoothScoOn } catch (_: Throwable) { false }

    /**
     * Carry on after an interruption, but not until the phone has really switched back: the audio mode is normal and Bluetooth is off its
     * call link, for 300 ms in a row (Mochy waits the same 300 ms), at most 6 s. The output is reopened first when it was paused a while or
     * the route changed, so no audio queued for the old route plays in a rush.
     */
    private fun resumeWhenSettled(why: String) {
        if (resuming) return
        resuming = true
        val startedAt = android.os.SystemClock.elapsedRealtime()
        var calmSince = 0L
        val step = object : Runnable {
            override fun run() {
                val p = player()
                if (p == null || !wasPlaying || p.playWhenReady) { resuming = false; return }
                val now = android.os.SystemClock.elapsedRealtime()
                val busy = inCallMode() || scoOn()
                if (busy) calmSince = 0L else if (calmSince == 0L) calmSince = now
                val settled = calmSince != 0L && now - calmSince >= 300L
                if (!settled && now - startedAt < 6_000L) { handler.postDelayed(this, 100L); return }
                resuming = false
                wasPlaying = false
                // a long pause or a route change: new output, so nothing stale is left in it
                if (routeDirty || now - pausedAt > 2_000L) rebuildOutput("resume after $why")
                if (!hasFocus && !requestFocus()) { ErrorLog.add("audio", "wanted to resume ($why) but focus wasn't granted"); return }
                ErrorLog.add("audio", "resumed ($why) after ${now - pausedAt} ms, audio mode ${am.mode}, bluetooth call link ${scoOn()}")
                selfChange = true
                try { p.play() } finally { selfChange = false }
            }
        }
        handler.postDelayed(step, 300L)
    }

    // ---- the output ----

    /**
     * Reopens the audio output on the current route: a stop and prepare keep the queue and the place in the song, but the audio track is
     * thrown away and made again, so it matches the headphones or speaker that are really there now.
     */
    fun rebuildOutput(why: String) {
        val p = player() ?: return
        routeDirty = false
        if (p.mediaItemCount == 0 || p.isCurrentMediaItemLive || p.playbackState == Player.STATE_IDLE) return
        try {
            val idx = p.currentMediaItemIndex
            val pos = p.currentPosition
            ErrorLog.add("audio", "reopening the output ($why) at $pos ms")
            selfChange = true
            try {
                p.stop()
                p.seekTo(idx, pos)
                p.prepare()
            } finally { selfChange = false }
        } catch (t: Throwable) { ErrorLog.add("audio", "could not reopen the output: " + t.message) }
    }

    // ---- headphones and Bluetooth coming and going ----

    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) = routeChanged(addedDevices, "added")
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) = routeChanged(removedDevices, "removed")
    }

    private fun routeChanged(devices: Array<out AudioDeviceInfo>?, what: String) {
        val relevant = devices?.any {
            it.isSink && (it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                it.type == AudioDeviceInfo.TYPE_BLE_HEADSET || it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || it.type == AudioDeviceInfo.TYPE_USB_HEADSET)
        } == true
        if (!relevant) return
        routeDirty = true
        val p = player() ?: return
        // paused: the dirty flag reopens the output when it resumes. Playing: reopen once the route has stopped changing.
        if (p.isPlaying) {
            handler.removeCallbacks(reopenPlaying)
            handler.postDelayed(reopenPlaying, 600L)
        }
        ErrorLog.add("audio", "output $what: " + devices?.joinToString { it.type.toString() })
    }

    private val reopenPlaying = Runnable {
        val p = player() ?: return@Runnable
        // not for a SCO link opened by a call (that pauses first), and not while two songs are being mixed
        if (routeDirty && p.isPlaying && canRebuild() && !inCallMode()) rebuildOutput("output changed while playing")
    }

    fun start() {
        try { am.registerAudioDeviceCallback(deviceCallback, handler) } catch (_: Throwable) {}
    }

    fun stop() {
        try { am.unregisterAudioDeviceCallback(deviceCallback) } catch (_: Throwable) {}
        handler.removeCallbacks(reopenPlaying)
        abandon()
    }
}
