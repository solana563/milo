package com.ubayanda.milonative.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File

/** Photos and voice notes travel inside the message as a data address, the same way the web milo. does it. */
object ChatMedia {
    private const val MAX_CHARS = 380_000

    /** Shrinks the picked photo to fit one message. Null when it can't be read. */
    fun photoToData(ctx: Context, uri: Uri): String? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2048) sample *= 2
        val raw = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
        if (raw == null) null else {
            var side = 1024
            var out: String? = null
            while (out == null && side >= 320) {
                val s = minOf(1f, side.toFloat() / maxOf(raw.width, raw.height))
                val bmp = if (s < 1f) Bitmap.createScaledBitmap(raw, (raw.width * s).toInt().coerceAtLeast(1), (raw.height * s).toInt().coerceAtLeast(1), true) else raw
                val bos = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, 65, bos)
                val d = "data:image/jpeg;base64," + Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
                if (d.length <= MAX_CHARS) out = d else side = (side * 0.75f).toInt()
            }
            out
        }
    } catch (e: Exception) {
        null
    }

    /** Centre-crops the picked photo to [w] x [h] and shrinks it until the data address fits in [maxChars]. */
    fun cropToData(ctx: Context, uri: Uri, w: Int, h: Int, maxChars: Int): String? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxOf(w, h)) sample *= 2
        val raw = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
        if (raw == null) null else {
            var scale = 1f
            var out: String? = null
            while (out == null && scale > 0.3f) {
                val tw = (w * scale).toInt().coerceAtLeast(32); val th = (h * scale).toInt().coerceAtLeast(32)
                // crop the largest centred area with the target's shape, then scale it
                val ratio = tw.toFloat() / th
                var cw = raw.width; var ch = (cw / ratio).toInt()
                if (ch > raw.height) { ch = raw.height; cw = (ch * ratio).toInt() }
                val crop = Bitmap.createBitmap(raw, (raw.width - cw) / 2, (raw.height - ch) / 2, cw.coerceAtLeast(1), ch.coerceAtLeast(1))
                val bmp = Bitmap.createScaledBitmap(crop, tw, th, true)
                val bos = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, 70, bos)
                val d = "data:image/jpeg;base64," + Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
                if (d.length <= maxChars) out = d else scale *= 0.8f
            }
            out
        }
    } catch (e: Exception) {
        null
    }

    fun bytesOf(data: String): ByteArray? = try {
        Base64.decode(data.substringAfter("base64,"), Base64.DEFAULT)
    } catch (e: Exception) {
        null
    }

    fun photoBitmap(data: String): Bitmap? = bytesOf(data)?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
}

/** Records a short voice note (AAC in an .m4a file). */
class VoiceRecorder(private val ctx: Context) {
    private var rec: MediaRecorder? = null
    private var file: File? = null
    private var startedAt = 0L

    fun start(): Boolean = try {
        val f = File(ctx.cacheDir, "voice_out.m4a").also { it.delete() }
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(ctx) else @Suppress("DEPRECATION") MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioSamplingRate(22050)
        r.setAudioEncodingBitRate(32000)
        r.setOutputFile(f.absolutePath)
        r.prepare(); r.start()
        rec = r; file = f; startedAt = System.currentTimeMillis()
        true
    } catch (e: Exception) {
        cancel(); false
    }

    /** Stops and returns the note as a data address, or null when it was too short or too big. */
    fun finish(): String? {
        val r = rec ?: return null
        val long = System.currentTimeMillis() - startedAt
        try { r.stop() } catch (e: Exception) { cancel(); return null }
        r.release(); rec = null
        val f = file ?: return null
        if (long < 600) { f.delete(); return null }
        val d = "data:audio/mp4;base64," + Base64.encodeToString(f.readBytes(), Base64.NO_WRAP)
        f.delete()
        return if (d.length <= 380_000) d else null
    }

    fun cancel() {
        try { rec?.release() } catch (e: Exception) {}
        rec = null; file?.delete()
    }
}

/** Plays one voice note at a time. */
object VoicePlayer {
    private var mp: MediaPlayer? = null
    var playingId: Long = -1L
        private set

    fun toggle(ctx: Context, id: Long, data: String, onChange: () -> Unit) {
        if (playingId == id) { stop(); onChange(); return }
        stop()
        val bytes = ChatMedia.bytesOf(data) ?: return
        try {
            val f = File(ctx.cacheDir, "voice_in_$id.m4a").also { it.writeBytes(bytes) }
            val p = MediaPlayer()
            p.setDataSource(f.absolutePath)
            p.setOnCompletionListener { stop(); onChange() }
            p.prepare(); p.start()
            mp = p; playingId = id
        } catch (e: Exception) {
            stop()
        }
        onChange()
    }

    fun stop() {
        try { mp?.release() } catch (e: Exception) {}
        mp = null; playingId = -1L
    }
}
