package com.ubayanda.milonative.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Size
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** What the home-screen widgets show: written by the playback service, read by the widgets. */
data class WidgetState(val title: String, val artist: String, val playing: Boolean, val pos: Long, val dur: Long, val art: Bitmap?, val accent: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color(0xFFF99C00), val liked: Boolean = false, val shuffle: Boolean = false) {
    val progress: Float get() = if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f
    val empty: Boolean get() = title.isEmpty()

    companion object {
        private fun prefs(ctx: Context) = ctx.getSharedPreferences("milo_widget", Context.MODE_PRIVATE)
        private fun artFile(ctx: Context) = File(ctx.filesDir, "widget_art.jpg")

        fun read(ctx: Context): WidgetState {
            val p = prefs(ctx)
            val f = artFile(ctx)
            val art = try { if (p.getBoolean("art", false) && f.exists()) BitmapFactory.decodeFile(f.path) else null } catch (_: Throwable) { null }
            // the colour the person picked in Profile
            val a = ctx.getSharedPreferences("milo_ui", Context.MODE_PRIVATE).getLong("accent", 0L)
            val accent = if (a != 0L) androidx.compose.ui.graphics.Color(a.toULong()) else androidx.compose.ui.graphics.Color(0xFFF99C00)
            return WidgetState(p.getString("title", "").orEmpty(), p.getString("artist", "").orEmpty(), p.getBoolean("playing", false), p.getLong("pos", 0L), p.getLong("dur", 0L), art, accent, p.getBoolean("liked", false), p.getBoolean("shuffle", false))
        }

        fun write(ctx: Context, title: String, artist: String, playing: Boolean, pos: Long, dur: Long, id: String = "", shuffle: Boolean = false) {
            prefs(ctx).edit().putString("title", title).putString("artist", artist).putBoolean("playing", playing).putLong("pos", pos).putLong("dur", dur)
                .putString("cur", id).putBoolean("shuffle", shuffle).putBoolean("liked", isLiked(ctx, id)).apply()
        }

        /** Is this song in Liked songs? Reads the same list the app saves. */
        fun isLiked(ctx: Context, id: String): Boolean =
            id.isNotEmpty() && (ctx.getSharedPreferences("milo", Context.MODE_PRIVATE).getStringSet("favs", emptySet()) ?: emptySet()).contains(id)

        fun currentId(ctx: Context): String = prefs(ctx).getString("cur", "").orEmpty()

        fun clear(ctx: Context) { prefs(ctx).edit().clear().apply(); artFile(ctx).delete() }

        /** Saves a small cover for the widgets: from the web address if there is one, else the phone's album art. */
        fun saveArt(ctx: Context, media: Uri?, artwork: Uri?) {
            var bmp: Bitmap? = null
            try {
                if (artwork != null && artwork.scheme?.startsWith("http") == true) {
                    val c = URL(artwork.toString()).openConnection() as HttpURLConnection
                    c.connectTimeout = 8000; c.readTimeout = 8000
                    bmp = c.inputStream.use { BitmapFactory.decodeStream(it) }
                } else if (media != null && media.scheme == "content") {
                    bmp = ctx.contentResolver.loadThumbnail(media, Size(360, 360), null)
                }
            } catch (_: Throwable) {}
            if (bmp == null && media != null) try {
                val r = MediaMetadataRetriever()
                r.setDataSource(ctx, media)
                r.embeddedPicture?.let { bmp = BitmapFactory.decodeByteArray(it, 0, it.size) }
                r.release()
            } catch (_: Throwable) {}
            val f = artFile(ctx)
            val b = bmp
            if (b == null) { prefs(ctx).edit().putBoolean("art", false).apply(); f.delete(); return }
            try {
                val s = 360f / maxOf(b.width, b.height)
                val small = if (s < 1f) Bitmap.createScaledBitmap(b, (b.width * s).toInt().coerceAtLeast(1), (b.height * s).toInt().coerceAtLeast(1), true) else b
                f.outputStream().use { small.compress(Bitmap.CompressFormat.JPEG, 88, it) }
                prefs(ctx).edit().putBoolean("art", true).apply()
            } catch (_: Throwable) { prefs(ctx).edit().putBoolean("art", false).apply() }
        }
    }
}
