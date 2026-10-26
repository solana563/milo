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
        if (b >= 1000) bmp = embedded(t, b)
        if (bmp == null) bmp = thumb(Uri.parse(t.uri), b)
        // the album's own cover (folder art such as cover.jpg is registered on the album, not the song)
        if (bmp == null && t.albumId > 0) {
            bmp = thumb(android.content.ContentUris.withAppendedId(android.provider.MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, t.albumId), b)
        }
        // last try: the picture inside the file, whatever the size
        if (bmp == null && b < 1000) bmp = embedded(t, b)
        if (bmp == null) none[k] = System.currentTimeMillis() else {
            none.remove(k)
            cache.put(k, bmp)
        }
        bmp
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

    /** 36px copy: stretched back up it is already soft, so no blur effect is needed (cheap on the GPU). */
    suspend fun soft(t: Track): Bitmap? = withContext(Dispatchers.IO) {
        val k = key(t)
        small.get(k)?.let { return@withContext it }
        val src = cover(t, 160) ?: return@withContext null
        val s = Bitmap.createScaledBitmap(src, 36, 36, true)
        small.put(k, s)
        s
    }
}
