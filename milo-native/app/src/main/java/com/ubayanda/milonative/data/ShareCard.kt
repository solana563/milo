package com.ubayanda.milonative.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.graphics.ColorUtils
import java.io.File

/** Draws a lyric as a picture (song colour, cover, the lines, "milo.") and opens the share sheet for it. */
object ShareCard {
    val STYLES = listOf("Song colour", "Blurred cover", "Paper", "Black")
    val FORMATS = listOf("Portrait", "Story", "Square")
    fun style(ctx: Context) = ctx.getSharedPreferences("milo_fx", 0).getInt("cardstyle", 0).coerceIn(0, STYLES.lastIndex)
    fun format(ctx: Context) = ctx.getSharedPreferences("milo_fx", 0).getInt("cardfmt", 0).coerceIn(0, FORMATS.lastIndex)

    fun share(ctx: Context, title: String, artist: String, lines: List<String>, cover: Bitmap?, tint: Int) {
        try {
            val st = style(ctx)
            val w = 1080; val h = when (format(ctx)) { 1 -> 1920; 2 -> 1080; else -> 1350 }
            val paper = st == 2
            val fg = if (paper) 0xFF1B1916.toInt() else 0xFFFFFFFF.toInt()
            val fg2 = if (paper) 0xAA1B1916.toInt() else 0xCCFFFFFF.toInt()
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            val base = if (tint == 0) 0xFF2A2724.toInt() else tint or (0xFF shl 24)
            val top = ColorUtils.blendARGB(base, 0xFF000000.toInt(), 0.30f)
            val bottom = ColorUtils.blendARGB(base, 0xFF000000.toInt(), 0.78f)
            when (st) {
                1 -> {
                    c.drawColor(0xFF000000.toInt())
                    if (cover != null) {
                        // the cover blown up to fill the card, then darkened, so the words stay readable
                        val sc = maxOf(w.toFloat() / cover.width, h.toFloat() / cover.height)
                        val dw = cover.width * sc; val dh = cover.height * sc
                        c.drawBitmap(cover, null, RectF((w - dw) / 2f, (h - dh) / 2f, (w + dw) / 2f, (h + dh) / 2f), Paint(Paint.FILTER_BITMAP_FLAG))
                    }
                    c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply { shader = LinearGradient(0f, 0f, 0f, h.toFloat(), 0x88000000.toInt(), 0xE6000000.toInt(), Shader.TileMode.CLAMP) })
                }
                2 -> c.drawColor(ColorUtils.blendARGB(0xFFF4EFE6.toInt(), base, 0.10f))
                3 -> c.drawColor(0xFF000000.toInt())
                else -> c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply { shader = LinearGradient(0f, 0f, 0f, h.toFloat(), top, bottom, Shader.TileMode.CLAMP) })
            }
            if (cover != null) {
                val r = RectF(80f, 80f, 260f, 260f)
                c.save(); c.clipPath(Path().apply { addRoundRect(r, 36f, 36f, Path.Direction.CW) })
                c.drawBitmap(cover, null, r, Paint(Paint.FILTER_BITMAP_FLAG)); c.restore()
            }
            val tx = if (cover != null) 292f else 80f
            val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = fg; textSize = 46f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            c.drawText(ellipsize(title, tp, w - tx - 80f), tx, 150f, tp)
            val ap = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = fg2; textSize = 36f }
            c.drawText(ellipsize(artist, ap, w - tx - 80f), tx, 210f, ap)
            val text = lines.filter { it.isNotBlank() }.joinToString("\n").take(260)
            var size = 76f
            var layout: StaticLayout
            val lp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = fg; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            while (true) {
                lp.textSize = size
                layout = StaticLayout.Builder.obtain(text, 0, text.length, lp, w - 160).setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(0f, 1.15f).build()
                if (layout.height <= h - 590 || size <= 36f) break
                size -= 4f
            }
            c.save(); c.translate(80f, 360f); layout.draw(c); c.restore()
            val fp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF5B63B.toInt(); textSize = 64f
                typeface = try {
                    val base = androidx.core.content.res.ResourcesCompat.getFont(ctx, com.ubayanda.milonative.R.font.figtree_italic)
                    if (base != null && android.os.Build.VERSION.SDK_INT >= 28) Typeface.create(base, 900, true) else Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
                } catch (_: Exception) { Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC) }
                letterSpacing = -0.04f
            }
            c.drawText("milo.", 80f, h - 80f, fp)
            val dir = File(ctx.cacheDir, "cards").also { it.mkdirs() }
            val f = File(dir, "lyric-" + System.currentTimeMillis() + ".png")
            f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "$title · $artist")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            ctx.startActivity(Intent.createChooser(send, "Share lyric card").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            android.widget.Toast.makeText(ctx, "Couldn't make the card.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun ellipsize(s: String, p: TextPaint, width: Float): String =
        android.text.TextUtils.ellipsize(s, p, width, android.text.TextUtils.TruncateAt.END).toString()
}
