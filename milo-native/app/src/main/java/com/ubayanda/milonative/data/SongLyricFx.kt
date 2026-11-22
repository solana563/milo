package com.ubayanda.milonative.data

import android.content.Context
import java.io.File

/**
 * Per-song fixes for lyrics: a timing offset (when the words come too early or late) and your own lyrics file that replaces what was found.
 * Kept inside milo; the song file is never changed. The same two fixes as Mochy Music's song edit sheet (GPL-3.0,
 * github.com/realidkroo/Mochy-Music, ui/menu/songedit); written separately for milo.
 */
object SongLyricFx {
    const val MIN_MS = -120_000L
    const val MAX_MS = 180_000L
    private const val PREFS = "milo_songfx"

    fun offset(ctx: Context, id: Long): Long = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong("off_$id", 0L)
    fun setOffset(ctx: Context, id: Long, ms: Long) {
        val e = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
        val v = ms.coerceIn(MIN_MS, MAX_MS)
        if (v == 0L) e.remove("off_$id") else e.putLong("off_$id", v)
        e.apply()
    }

    private fun file(ctx: Context, id: Long) = File(File(ctx.filesDir, "lyrics-custom").also { it.mkdirs() }, "$id.lrc")
    fun custom(ctx: Context, id: Long): String? = try { file(ctx, id).takeIf { it.exists() }?.readText() } catch (_: Exception) { null }
    fun hasCustom(ctx: Context, id: Long): Boolean = file(ctx, id).exists()
    fun setCustom(ctx: Context, id: Long, text: String?) {
        try { if (text.isNullOrBlank()) file(ctx, id).delete() else file(ctx, id).writeText(text.take(400_000)) } catch (_: Exception) {}
    }
}
