package com.ubayanda.milonative.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import java.io.File

/**
 * A cover picture you choose for one song, kept inside milo (the song file isn't changed). The cover loader checks here first.
 * Same idea as the cover picker in Mochy Music's song edit sheet (GPL-3.0, ui/menu/songedit); written separately for milo, and
 * the picture comes from a file you pick, not from a search.
 */
object SongCover {
    /** bumps whenever a cover is set or removed, so covers on screen load again */
    var rev by mutableIntStateOf(0)
        private set

    fun file(ctx: Context, id: Long) = File(File(ctx.filesDir, "covers-custom").also { it.mkdirs() }, "$id.jpg")
    fun has(ctx: Context, id: Long) = file(ctx, id).exists()

    /** Saves the picture at [uri] (scaled down to at most 1200 px) as this song's cover. False if it can't be read as a picture. */
    fun set(ctx: Context, id: Long, uri: Uri): Boolean {
    return try {
        val bytes = ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return false
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
        if (o.outWidth <= 0 || o.outHeight <= 0) return false
        var sample = 1
        while (o.outWidth / (sample * 2) >= 1200 && o.outHeight / (sample * 2) >= 1200) sample *= 2
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return false
        file(ctx, id).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        rev++
        true
    } catch (_: Exception) { false }
    }

    fun remove(ctx: Context, id: Long) { try { file(ctx, id).delete() } catch (_: Exception) {}; rev++ }
}
