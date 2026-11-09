package com.ubayanda.milonative.player

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.MediaCodecList
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri

/** What this phone can really play and output. Everything here is read from Android, nothing is assumed. */
object AudioCaps {
    class Fmt(val name: String, val mime: String, val ok: Boolean, val how: String)
    class Out(val name: String, val type: String, val maxRate: Int, val hiRes: Boolean, val note: String)
    class Info(val mime: String, val rate: Int, val channels: Int, val kbps: Int, val seconds: Int)

    private val wanted = listOf(
        Triple("MP3", "audio/mpeg", false), Triple("AAC / M4A", "audio/mp4a-latm", false), Triple("FLAC", "audio/flac", false),
        Triple("WAV", "audio/raw", true), Triple("OGG Vorbis", "audio/vorbis", false), Triple("Opus", "audio/opus", false), Triple("ALAC", "audio/alac", false),
    )

    /** PCM (WAV) is read by the player itself; the rest need a decoder on the phone. */
    fun formats(): List<Fmt> {
        val decoders = try { MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.filter { !it.isEncoder }.flatMap { it.supportedTypes.toList() }.map { it.lowercase() }.toSet() } catch (_: Throwable) { emptySet() }
        return wanted.map { (n, m, builtin) ->
            val ok = builtin || m in decoders
            Fmt(n, m, ok, if (builtin) "built into milo." else if (ok) "decoder on this phone" else "no decoder on this phone")
        }
    }

    fun isHiRes(rate: Int, bits24Plus: Boolean) = rate >= 88200 && bits24Plus

    fun outputs(c: Context): List<Out> {
        val am = c.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return am.getDevices(AudioManager.GET_DEVICES_OUTPUTS).mapNotNull { d ->
            val type = when (d.type) {
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Phone speaker"; AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> return@mapNotNull null
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired headphones"
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth"; AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> return@mapNotNull null
                AudioDeviceInfo.TYPE_USB_DEVICE, AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_USB_ACCESSORY -> "USB audio"
                AudioDeviceInfo.TYPE_HDMI -> "HDMI"; else -> return@mapNotNull null
            }
            val rates = d.sampleRates.toList(); val enc = d.encodings.toList()
            val max = rates.maxOrNull() ?: 0   // an empty list means the device accepts whatever the system gives it
            val deep = enc.any { it == AudioFormat.ENCODING_PCM_24BIT_PACKED || it == AudioFormat.ENCODING_PCM_32BIT || it == AudioFormat.ENCODING_PCM_FLOAT }
            val hi = isHiRes(max, deep)
            Out(d.productName?.toString().orEmpty().ifEmpty { type }, type, max, hi,
                if (rates.isEmpty()) "The phone doesn't report this output's limits." else "up to ${max / 1000.0} kHz" + if (deep) ", 24-bit or better" else ", 16-bit")
        }.distinctBy { it.name + it.type }
    }

    /** Format of a song stored on the phone. Streamed songs pick their format when played, so they return null. */
    fun trackInfo(c: Context, uri: String): Info? {
        if (uri.startsWith("yt:") || uri.startsWith("http")) return null
        val ex = MediaExtractor()
        return try {
            ex.setDataSource(c, Uri.parse(uri), null)
            val f = (0 until ex.trackCount).map { ex.getTrackFormat(it) }.firstOrNull { it.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true } ?: return null
            fun i(k: String) = if (f.containsKey(k)) f.getInteger(k) else 0
            val us = if (f.containsKey(MediaFormat.KEY_DURATION)) f.getLong(MediaFormat.KEY_DURATION) else 0L
            Info(f.getString(MediaFormat.KEY_MIME).orEmpty(), i(MediaFormat.KEY_SAMPLE_RATE), i(MediaFormat.KEY_CHANNEL_COUNT), i(MediaFormat.KEY_BIT_RATE) / 1000, (us / 1_000_000).toInt())
        } catch (_: Throwable) { null } finally { try { ex.release() } catch (_: Throwable) {} }
    }

    fun mimeLabel(m: String) = when (m.lowercase()) {
        "audio/mpeg" -> "MP3"; "audio/mp4a-latm" -> "AAC"; "audio/flac" -> "FLAC"; "audio/vorbis" -> "OGG Vorbis"; "audio/opus" -> "Opus"
        "audio/raw" -> "WAV (PCM)"; "audio/alac" -> "ALAC"; else -> m.removePrefix("audio/")
    }
}
