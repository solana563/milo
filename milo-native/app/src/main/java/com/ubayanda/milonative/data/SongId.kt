package com.ubayanda.milonative.data

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * "What song is this?": listens through the microphone for about ten seconds and asks AudD (audd.io), a music-recognition service
 * with a public API, what it heard. AudD needs a token the person gets from audd.io (a free allowance); it is kept on the phone only.
 * Mochy Music has the same feature through Shazam's private endpoints; milo uses the service that offers an official API instead.
 * Blocking: call off the main thread. The microphone permission must already be granted.
 */
object SongId {
    data class Heard(val title: String, val artist: String, val album: String, val timecode: String)

    private const val RATE = 16_000
    private val http = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).writeTimeout(30, TimeUnit.SECONDS).build()

    fun token(ctx: Context): String = ctx.getSharedPreferences("milo_fx", Context.MODE_PRIVATE).getString("audd_token", "").orEmpty()
    fun setToken(ctx: Context, t: String) { ctx.getSharedPreferences("milo_fx", Context.MODE_PRIVATE).edit().putString("audd_token", t.trim()).apply() }

    /** records mono 16-bit audio and returns it as a WAV file's bytes, or null if the microphone can't be opened */
    @SuppressLint("MissingPermission")
    fun listen(seconds: Int = 10): ByteArray? {
        val min = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (min <= 0) return null
        val rec = try { AudioRecord(MediaRecorder.AudioSource.MIC, RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, min * 4) } catch (_: Throwable) { return null }
        if (rec.state != AudioRecord.STATE_INITIALIZED) { rec.release(); return null }
        val pcm = ByteArrayOutputStream()
        val buf = ByteArray(min)
        try {
            rec.startRecording()
            val end = System.currentTimeMillis() + seconds * 1000L
            while (System.currentTimeMillis() < end) {
                val n = rec.read(buf, 0, buf.size)
                if (n > 0) pcm.write(buf, 0, n) else if (n < 0) break
            }
        } finally { try { rec.stop() } catch (_: Throwable) {}; rec.release() }
        val data = pcm.toByteArray()
        return if (data.size < RATE * 2) null else wav(data)
    }

    private fun wav(pcm: ByteArray): ByteArray {
        val out = ByteArrayOutputStream(pcm.size + 44)
        fun le32(v: Int) { out.write(v and 255); out.write((v shr 8) and 255); out.write((v shr 16) and 255); out.write((v shr 24) and 255) }
        fun le16(v: Int) { out.write(v and 255); out.write((v shr 8) and 255) }
        out.write("RIFF".toByteArray()); le32(36 + pcm.size); out.write("WAVE".toByteArray())
        out.write("fmt ".toByteArray()); le32(16); le16(1); le16(1); le32(RATE); le32(RATE * 2); le16(2); le16(16)
        out.write("data".toByteArray()); le32(pcm.size); out.write(pcm)
        return out.toByteArray()
    }

    /** null = nothing recognised; throws on no connection or a bad token (message says which) */
    fun identify(wav: ByteArray, token: String): Heard? {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("api_token", token)
            .addFormDataPart("file", "heard.wav", wav.toRequestBody("audio/wav".toMediaType()))
            .build()
        val resp = http.newCall(Request.Builder().url("https://api.audd.io/").post(body).build()).execute().use { it.body?.string().orEmpty() }
        val j = try { JSONObject(resp) } catch (_: Exception) { throw java.io.IOException("AudD gave an unreadable answer") }
        if (j.optString("status") != "success") {
            val msg = j.optJSONObject("error")?.optString("error_message").orEmpty()
            throw java.io.IOException(msg.ifEmpty { "AudD refused the request" })
        }
        val r = j.optJSONObject("result") ?: return null
        val title = r.optString("title")
        if (title.isBlank()) return null
        return Heard(title, r.optString("artist"), r.optString("album"), r.optString("timecode"))
    }
}
