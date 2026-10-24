package com.ubayanda.milonative.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.LruCache
import android.util.Size
import com.ubayanda.milonative.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loads cover art from MediaStore and keeps a few in memory. Also makes the tiny 36px copy that the cards stretch up into a soft backdrop. */
class ArtLoader(private val context: Context) {
    private val big = object : LruCache<Long, Bitmap>(24) {}
    private val small = object : LruCache<Long, Bitmap>(64) {}
    private val none = HashSet<Long>()

    private fun key(t: Track): Long = if (t.albumId > 0) t.albumId else -t.id

    /** Cover at roughly [px] pixels, or null when the song has no art. */
    suspend fun cover(t: Track, px: Int = 640): Bitmap? = withContext(Dispatchers.IO) {
        val k = key(t)
        big.get(k)?.let { return@withContext it }
        if (none.contains(k)) return@withContext null
        val bmp = try {
            context.contentResolver.loadThumbnail(Uri.parse(t.uri), Size(px, px), null)
        } catch (e: Exception) {
            null
        }
        if (bmp == null) none.add(k) else big.put(k, bmp)
        bmp
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
