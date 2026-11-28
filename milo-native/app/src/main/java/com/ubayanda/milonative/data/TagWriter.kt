package com.ubayanda.milonative.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.ubayanda.milonative.util.ErrorLog
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File

/**
 * Writes title, artist and album into the audio file itself (MP3, M4A, FLAC, Ogg/Opus, WAV, WMA).
 * The file is copied to the cache, edited there, and only then copied back, with a spare copy of the original kept until the write has worked.
 * The caller must already have write access to [uri] (a MediaStore write request on Android 11+).
 */
object TagWriter {
    private val formats = setOf("mp3", "m4a", "mp4", "flac", "ogg", "oga", "opus", "wav", "wma")

    fun supports(name: String) = name.substringAfterLast('.', "").lowercase() in formats

    /** true when the tags were written; false when the file type can't be edited or something failed (the file is left as it was). SecurityException is passed on so the caller can ask for access. */
    fun write(ctx: Context, uri: Uri, title: String, artist: String, album: String): Boolean {
        val cr = ctx.contentResolver
        var name = ""
        var path = ""
        cr.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.DATA), null, null, null)?.use {
            if (it.moveToFirst()) { name = it.getString(0).orEmpty(); path = runCatching { it.getString(1).orEmpty() }.getOrDefault("") }
        }
        if (!supports(name)) return false
        val ext = name.substringAfterLast('.').lowercase()
        val dir = File(ctx.cacheDir, "tagedit").also { it.mkdirs() }
        val stamp = System.nanoTime()
        val work = File(dir, "$stamp.$ext")
        val orig = File(dir, "$stamp.orig.$ext")
        try {
            (cr.openInputStream(uri) ?: return false).use { i -> work.outputStream().use { o -> i.copyTo(o) } }
            work.copyTo(orig, overwrite = true)
            java.util.logging.Logger.getLogger("org.jaudiotagger").level = java.util.logging.Level.OFF
            val f = AudioFileIO.read(work)
            val tag = f.tagOrCreateAndSetDefault
            fun put(key: FieldKey, v: String) { if (v.isBlank()) tag.deleteField(key) else tag.setField(key, v.trim()) }
            put(FieldKey.TITLE, title); put(FieldKey.ARTIST, artist); put(FieldKey.ALBUM, album)
            f.commit()
            if (work.length() <= 0L) return false
            try {
                (cr.openOutputStream(uri, "wt") ?: return false).use { o -> work.inputStream().use { it.copyTo(o) } }
            } catch (e: SecurityException) {
                throw e
            } catch (e: Throwable) {
                // the copy back failed half way: put the original bytes back
                ErrorLog.swallowed(e)
                ErrorLog.catching { cr.openOutputStream(uri, "wt")?.use { o -> orig.inputStream().use { it.copyTo(o) } } }
                return false
            }
            // tell the media database now, so lists update without waiting for a rescan
            ErrorLog.catching {
                val cv = ContentValues().apply { put(MediaStore.Audio.Media.TITLE, title.trim()); put(MediaStore.Audio.Media.ARTIST, artist.trim()); put(MediaStore.Audio.Media.ALBUM, album.trim()) }
                cr.update(uri, cv, null, null)
            }
            if (path.isNotBlank()) ErrorLog.catching { android.media.MediaScannerConnection.scanFile(ctx, arrayOf(path), null, null) }
            return true
        } catch (e: SecurityException) {
            throw e
        } catch (e: Throwable) {
            ErrorLog.swallowed(e)
            return false
        } finally {
            work.delete(); orig.delete()
        }
    }
}
