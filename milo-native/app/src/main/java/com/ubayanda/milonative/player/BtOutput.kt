package com.ubayanda.milonative.player

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Where the sound is going: speaker, wired headphones, USB DAC or Bluetooth (with the device's name).
 * Also the Bluetooth comforts: carry on when your headphones come back, remember the volume you like on each device,
 * and a lyric delay because Bluetooth audio arrives a little after it is played.
 */
object BtOutput {
    data class Out(val kind: String = "speaker", val name: String = "Phone speaker", val bt: Boolean = false)

    private val _now = MutableStateFlow(Out())
    val now: StateFlow<Out> = _now.asStateFlow()

    // ---- settings ----
    private fun p(c: Context) = c.getSharedPreferences("milo_bt", 0)
    fun autoResume(c: Context) = p(c).getBoolean("resume", true)
    fun setAutoResume(c: Context, v: Boolean) = p(c).edit().putBoolean("resume", v).apply()
    fun rememberVolume(c: Context) = p(c).getBoolean("vol", true)
    fun setRememberVolume(c: Context, v: Boolean) = p(c).edit().putBoolean("vol", v).apply()
    /** Lyrics are shown this many milliseconds later while a Bluetooth device is playing. */
    fun lyricDelay(c: Context) = p(c).getInt("delay", 0)
    fun setLyricDelay(c: Context, ms: Int) = p(c).edit().putInt("delay", ms.coerceIn(0, 800)).apply()

    fun canNameDevices(c: Context) = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        ContextCompat.checkSelfPermission(c, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    private fun rank(t: Int): Int = when (t) {
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_HEARING_AID -> 5
        AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_DEVICE -> 4
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_LINE_ANALOG, AudioDeviceInfo.TYPE_LINE_DIGITAL -> 3
        else -> if (Build.VERSION.SDK_INT >= 31 && (t == AudioDeviceInfo.TYPE_BLE_HEADSET || t == AudioDeviceInfo.TYPE_BLE_SPEAKER)) 5
        else if (t == AudioDeviceInfo.TYPE_HDMI) 2 else if (t == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) 1 else 0
    }

    fun read(c: Context): Out {
        val am = c.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val best = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).filter { rank(it.type) >= 1 }.maxByOrNull { rank(it.type) } ?: return Out()
        val r = rank(best.type)
        val named = if (canNameDevices(c)) best.productName?.toString().orEmpty() else ""
        return when {
            r == 5 -> Out("bluetooth", named.ifBlank { "Bluetooth device" }, true)
            best.type == AudioDeviceInfo.TYPE_USB_HEADSET || best.type == AudioDeviceInfo.TYPE_USB_DEVICE -> Out("usb", named.ifBlank { "USB audio" })
            r == 3 -> Out("wired", "Wired headphones")
            best.type == AudioDeviceInfo.TYPE_HDMI -> Out("hdmi", "HDMI")
            else -> Out()
        }
    }

    private var cb: AudioDeviceCallback? = null
    private var pausedByBt = 0L

    /** Called by the playback service. [isPlaying] and [play] let it carry on or note a pause. */
    fun start(c: Context, isPlaying: () -> Boolean, play: () -> Unit) {
        if (cb != null) return
        val app = c.applicationContext
        val am = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        _now.value = read(app)
        var lastPlaying = 0L
        val tick = object : Runnable { override fun run() { if (isPlaying()) lastPlaying = System.currentTimeMillis(); Handler(Looper.getMainLooper()).postDelayed(this, 1000) } }
        Handler(Looper.getMainLooper()).post(tick)
        cb = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>) {
                val before = _now.value
                val after = read(app); _now.value = after
                if (after.bt && !before.bt) {
                    if (rememberVolume(app)) restoreVolume(app, after.name)
                    if (autoResume(app) && pausedByBt > 0 && System.currentTimeMillis() - pausedByBt < 10 * 60_000L && !isPlaying()) play()
                    pausedByBt = 0
                }
            }
            override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>) {
                val before = _now.value
                val after = read(app); _now.value = after
                if (before.bt && !after.bt && System.currentTimeMillis() - lastPlaying < 5000) pausedByBt = System.currentTimeMillis()
            }
        }
        am.registerAudioDeviceCallback(cb, Handler(Looper.getMainLooper()))
    }

    fun stop(c: Context) {
        val am = c.applicationContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        cb?.let { am.unregisterAudioDeviceCallback(it) }; cb = null
    }

    /** Remember the music volume you chose while this device was playing. Call when playback pauses or stops. */
    fun saveVolume(c: Context) {
        val o = _now.value
        if (!o.bt || !rememberVolume(c)) return
        val am = c.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        p(c).edit().putInt("v:" + o.name, am.getStreamVolume(AudioManager.STREAM_MUSIC)).apply()
    }

    private fun restoreVolume(c: Context, name: String) {
        val v = p(c).getInt("v:$name", -1)
        if (v < 0) return
        val am = c.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        try { am.setStreamVolume(AudioManager.STREAM_MUSIC, v.coerceIn(0, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)), 0) } catch (_: Exception) {}
    }

    fun refresh(c: Context) { _now.value = read(c.applicationContext) }
}
