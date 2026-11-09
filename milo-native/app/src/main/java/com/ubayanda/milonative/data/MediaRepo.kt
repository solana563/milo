package com.ubayanda.milonative.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.ubayanda.milonative.model.Track
import com.ubayanda.milonative.util.trackByTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads the music library straight from Android's MediaStore: every song on the phone's storage,
 * SD card and plugged-in USB drives that Android has indexed. Files are never copied or uploaded.
 */
class MediaRepo(private val context: Context) {

    suspend fun scan(): List<Track> = withContext(Dispatchers.IO) {
        val base = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DATE_ADDED,
        ).let { if (android.os.Build.VERSION.SDK_INT >= 30) it + MediaStore.Audio.Media.GENRE else it }
        // music only (not ringtones, alarms or voice notes), and skip clips shorter than 10 seconds
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 10000"
        val out = ArrayList<Track>()
        try {
            context.contentResolver.query(base, projection, selection, null, null)?.use { c ->
                val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val iAlbum = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val iAlbumId = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val iDur = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val iTrack = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val iYear = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val iAdded = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val iGenre = if (android.os.Build.VERSION.SDK_INT >= 30) c.getColumnIndex(MediaStore.Audio.Media.GENRE) else -1
                while (c.moveToNext()) {
                    val id = c.getLong(iId)
                    out.add(
                        Track(
                            id = id,
                            uri = ContentUris.withAppendedId(base, id).toString(),
                            title = clean(c.getString(iTitle), "Untitled"),
                            artist = clean(c.getString(iArtist), "Unknown artist"),
                            album = clean(c.getString(iAlbum), "Unknown album"),
                            albumId = c.getLong(iAlbumId),
                            durationMs = c.getLong(iDur),
                            // MediaStore stores disc * 1000 + track
                            trackNo = c.getInt(iTrack) % 1000,
                            year = c.getInt(iYear),
                            addedSec = c.getLong(iAdded),
                            genre = if (iGenre >= 0) clean(c.getString(iGenre), "") else "",
                        )
                    )
                }
            }
        } catch (e: SecurityException) {
            // permission was revoked while we were reading: report an empty library, the UI asks again
            return@withContext emptyList()
        }
        out.sortedWith(trackByTitle)
    }

    private fun clean(s: String?, fallback: String): String {
        val t = s?.trim().orEmpty()
        return if (t.isEmpty() || t == "<unknown>") fallback else t
    }
}
