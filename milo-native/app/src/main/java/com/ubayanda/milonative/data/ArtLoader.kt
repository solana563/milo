package com.ubayanda.milonative.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import android.util.Size
import com.ubayanda.milonative.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads cover art and keeps a few in memory. Sizes are cached separately (a tiny row thumbnail must never be
 * reused for the full-screen cover, that is what made it look pixelated). Big covers come from the picture embedded
 * in the file when there is one, because Android's own thumbnails are small.
 */
class ArtLoader(private val context: Context) {
    private val cache = object : LruCache<String, Bitmap>(40) {}
    private val small = object : LruCache<Long, Bitmap>(64) {}
    /** keys that had no art, with the time we gave up; retried later so one hiccup never hides a cover for good */
    private val none = java.util.concurrent.ConcurrentHashMap<String, Long>()

    private fun key(t: Track): Long = if (t.albumId > 0) t.albumId else -t.id

    private fun bucket(px: Int) = when {
        px <= 160 -> 160
        px <= 400 -> 400
        else -> 1000
    }

    /** Cover at roughly [px] pixels, or null when the song has no art. */
    suspend fun cover(t: Track, px: Int = 640): Bitmap? = withContext(Dispatchers.IO) {
        val b = bucket(px)
        val k = "${key(t)}:$b"
        cache.get(k)?.let { return@withContext it }
        val missed = none[k]
        if (missed != null && System.currentTimeMillis() - missed < 30_000) return@withContext null
        var bmp: Bitmap? = null
        if (t.artUrl.isNotEmpty()) {
            // online songs carry a 544 px picture; ask for the size this slot really shows (full screen = 1080), else it is stretched and soft
            val u = bigger(t.artUrl, b)
            bmp = remote(u, b)
            if (bmp == null && u != t.artUrl) bmp = remote(t.artUrl, b)
        }
        if (bmp == null && t.uri.startsWith("content:") && b >= 1000) bmp = embedded(t, b)
        if (bmp == null && t.uri.startsWith("content:")) bmp = thumb(Uri.parse(t.uri), b)
        // the album's own cover (folder art such as cover.jpg is registered on the album, not the song)
        if (bmp == null && t.uri.startsWith("content:") && t.albumId > 0) {
            bmp = thumb(android.content.ContentUris.withAppendedId(android.provider.MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, t.albumId), b)
        }
        // last try: the picture inside the file, whatever the size
        if (bmp == null && t.uri.startsWith("content:") && b < 1000) bmp = embedded(t, b)
        if (bmp == null) none[k] = System.currentTimeMillis() else {
            none.remove(k)
            cache.put(k, bmp)
        }
        bmp
    }

    /** The same picture at a bigger size, for YouTube Music, Google and iTunes addresses (others stay as they are). */
    private fun bigger(url: String, b: Int): String {
        if (!url.startsWith("http")) return url
        val want = when { b <= 160 -> 226; b <= 400 -> 544; else -> 1080 }
        if (url.contains("mzstatic.com")) return if (b >= 1000) url.replace(Regex("\\d+x\\d+bb"), "1200x1200bb") else url
        return com.ubayanda.milonative.data.YtMusic.art(url, want)
    }

    private val http = okhttp3.OkHttpClient.Builder().connectTimeout(8, java.util.concurrent.TimeUnit.SECONDS).readTimeout(10, java.util.concurrent.TimeUnit.SECONDS).build()

    /** A cover picture from the web (radio logos, iTunes artwork), decoded down to about [target] pixels. */
    private fun remote(url: String, target: Int): Bitmap? = try {
        if (url.startsWith("/")) {
            // a cover saved with an offline song
            val f = java.io.File(url)
            if (!f.exists()) null else {
                val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(url, o)
                var sample = 1
                while (o.outWidth / (sample * 2) >= target && o.outHeight / (sample * 2) >= target) sample *= 2
                BitmapFactory.decodeFile(url, BitmapFactory.Options().apply { inSampleSize = sample })
            }
        } else {
        val req = okhttp3.Request.Builder().url(url).header("User-Agent", "milo-native/1.0").build()
        http.newCall(req).execute().use { r ->
            val bytes = r.body?.bytes()
            if (!r.isSuccessful || bytes == null) null else {
                val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
                var sample = 1
                while (o.outWidth / (sample * 2) >= target && o.outHeight / (sample * 2) >= target) sample *= 2
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            }
        }
        }
    } catch (e: Exception) {
        null
    }

    private fun thumb(uri: Uri, b: Int): Bitmap? = try {
        context.contentResolver.loadThumbnail(uri, Size(b, b), null)
    } catch (e: Exception) {
        null
    }

    /** The picture stored inside the audio file, decoded down to about [target] pixels. */
    private fun embedded(t: Track, target: Int): Bitmap? {
        val r = MediaMetadataRetriever()
        return try {
            r.setDataSource(context, Uri.parse(t.uri))
            val bytes = r.embeddedPicture ?: return null
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
            var sample = 1
            while (o.outWidth / (sample * 2) >= target && o.outHeight / (sample * 2) >= target) sample *= 2
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
        } catch (e: Exception) {
            null
        } finally {
            try { r.release() } catch (_: Exception) {}
        }
    }

    /** 48px copy, blurred by hand: stretched back up it is smooth (no blocks) and needs no blur effect on the GPU. */
    suspend fun soft(t: Track): Bitmap? = withContext(Dispatchers.IO) {
        val k = key(t)
        small.get(k)?.let { return@withContext it }
        val src = cover(t, 160) ?: return@withContext null
        val n = 48
        val s = Bitmap.createScaledBitmap(src, n, n, true).copy(Bitmap.Config.ARGB_8888, true)
        val px = IntArray(n * n)
        s.getPixels(px, 0, n, 0, 0, n, n)
        var cur = px
        repeat(3) { cur = boxBlur(cur, n, 2) }
        s.setPixels(cur, 0, n, 0, 0, n, n)
        small.put(k, s)
        s
    }

    private fun boxBlur(src: IntArray, n: Int, r: Int): IntArray {
        val out = IntArray(src.size)
        for (y in 0 until n) for (x in 0 until n) {
            var a = 0; var rr = 0; var g = 0; var b = 0; var c = 0
            for (dy in -r..r) for (dx in -r..r) {
                val yy = y + dy; val xx = x + dx
                if (yy < 0 || xx < 0 || yy >= n || xx >= n) continue
                val p = src[yy * n + xx]
                a += p ushr 24; rr += (p shr 16) and 255; g += (p shr 8) and 255; b += p and 255; c++
            }
            out[y * n + x] = ((a / c) shl 24) or ((rr / c) shl 16) or ((g / c) shl 8) or (b / c)
        }
        return out
    }
}
