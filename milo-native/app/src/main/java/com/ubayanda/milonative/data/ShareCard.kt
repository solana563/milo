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
    fun share(ctx: Context, title: String, artist: String, lines: List<String>, cover: Bitmap?, tint: Int) {
        try {
            val w = 1080; val h = 1350
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            val base = if (tint == 0) 0xFF2A2724.toInt() else tint or (0xFF shl 24)
            val top = ColorUtils.blendARGB(base, 0xFF000000.toInt(), 0.30f)
            val bottom = ColorUtils.blendARGB(base, 0xFF000000.toInt(), 0.78f)
            c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), Paint().apply { shader = LinearGradient(0f, 0f, 0f, h.toFloat(), top, bottom, Shader.TileMode.CLAMP) })
            if (cover != null) {
                val r = RectF(80f, 80f, 260f, 260f)
                c.save(); c.clipPath(Path().apply { addRoundRect(r, 36f, 36f, Path.Direction.CW) })
                c.drawBitmap(cover, null, r, Paint(Paint.FILTER_BITMAP_FLAG)); c.restore()
            }
            val tx = if (cover != null) 292f else 80f
            val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); textSize = 46f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            c.drawText(ellipsize(title, tp, w - tx - 80f), tx, 150f, tp)
            val ap = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xCCFFFFFF.toInt(); textSize = 36f }
            c.drawText(ellipsize(artist, ap, w - tx - 80f), tx, 210f, ap)
            val text = lines.filter { it.isNotBlank() }.joinToString("\n").take(260)
            var size = 76f
            var layout: StaticLayout
            val lp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt(); typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            while (true) {
                lp.textSize = size
                layout = StaticLayout.Builder.obtain(text, 0, text.length, lp, w - 160).setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(0f, 1.15f).build()
                if (layout.height <= 760 || size <= 36f) break
                size -= 4f
            }
            c.save(); c.translate(80f, 360f); layout.draw(c); c.restore()
            val fp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF5B63B.toInt(); textSize = 54f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
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
