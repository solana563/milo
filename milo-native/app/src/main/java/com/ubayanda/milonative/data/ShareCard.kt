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
import com.ubayanda.milonative.BuildConfig
import java.io.File

/**
 * Draws a lyric as a picture and opens the share sheet for it.
 * Layout (Spotify-style): song row on top, the lyric big and bold with an accent bar, and a footer that
 * sends people to milo: the wordmark, the address, and a QR code that opens the download page.
 */
object ShareCard {
    val STYLES = listOf("Song colour", "Blurred cover", "Paper", "Black", "Sticker")
    val FORMATS = listOf("Portrait", "Story", "Square")
    private const val AMBER = 0xFFF5B63B.toInt()
    fun style(ctx: Context) = ctx.getSharedPreferences("milo_fx", 0).getInt("cardstyle", 0).coerceIn(0, STYLES.lastIndex)
    fun format(ctx: Context) = ctx.getSharedPreferences("milo_fx", 0).getInt("cardfmt", 0).coerceIn(0, FORMATS.lastIndex)

    /** Where the QR and the share text send people: the newest milo release page of the repo this build came from. */
    fun link(): String {
        val r = BuildConfig.UPDATE_REPO.ifBlank { "solana563/milo" }
        return "https://github.com/$r/releases/latest"
    }
    private fun shortLink() = link().removePrefix("https://").removeSuffix("/releases/latest")

    fun share(ctx: Context, title: String, artist: String, lines: List<String>, cover: Bitmap?, tint: Int) {
        try {
            val st = style(ctx)
            val w = 1080; val h = when (format(ctx)) { 1 -> 1920; 2 -> 1080; else -> 1350 }
            val light = st == 2
            val sticker = st == 4
            val fg = if (light) 0xFF1B1916.toInt() else 0xFFFFFFFF.toInt()
            val fg2 = if (light) 0xAA1B1916.toInt() else 0xCCFFFFFF.toInt()
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            val base = if (tint == 0) 0xFF2A2724.toInt() else tint or (0xFF shl 24)
            val top = ColorUtils.blendARGB(base, 0xFF000000.toInt(), 0.22f)
            val bottom = ColorUtils.blendARGB(base, 0xFF000000.toInt(), 0.72f)
            val fw = w.toFloat(); val fh = h.toFloat()
            when (st) {
                1, 4 -> {
                    c.drawColor(0xFF000000.toInt())
                    val soft = cover?.let { blurred(it, w, h) }
                    if (soft != null) c.drawBitmap(soft, 0f, 0f, null)
                    else c.drawRect(0f, 0f, fw, fh, Paint().apply { shader = LinearGradient(0f, 0f, 0f, fh, top, bottom, Shader.TileMode.CLAMP) })
                    c.drawRect(0f, 0f, fw, fh, Paint().apply { shader = LinearGradient(0f, 0f, 0f, fh, if (sticker) 0x55000000 else 0x88000000.toInt(), 0xE6000000.toInt(), Shader.TileMode.CLAMP) })
                }
                2 -> c.drawColor(ColorUtils.blendARGB(0xFFF4EFE6.toInt(), base, 0.10f))
                3 -> c.drawColor(0xFF000000.toInt())
                else -> c.drawRect(0f, 0f, fw, fh, Paint().apply { shader = LinearGradient(0f, 0f, 0f, fh, top, bottom, Shader.TileMode.CLAMP) })
            }

            // ── header: cover + song ──
            val m = 80f
            if (cover != null) {
                val r = RectF(m, m, m + 150f, m + 150f)
                c.save(); c.clipPath(Path().apply { addRoundRect(r, 28f, 28f, Path.Direction.CW) })
                c.drawBitmap(cover, null, r, Paint(Paint.FILTER_BITMAP_FLAG)); c.restore()
            }
            val tx = if (cover != null) m + 150f + 32f else m
            val tp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = fg; textSize = 46f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            c.drawText(ellipsize(title, tp, fw - tx - m), tx, m + 66f, tp)
            val ap = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = fg2; textSize = 36f }
            c.drawText(ellipsize(artist, ap, fw - tx - m), tx, m + 118f, ap)

            // ── footer geometry ──
            val footH = 250f
            val footTop = fh - m - footH

            // ── the lyric ──
            val text = lines.filter { it.isNotBlank() }.joinToString("\n").take(260)
            val areaTop = m + 150f + 70f
            val areaBot = footTop - 50f
            val areaH = areaBot - areaTop
            val inset = if (sticker) 60f else 38f
            val boxL = if (sticker) 60f else m
            val boxW = if (sticker) fw - 120f else fw - 2 * m
            val textW = (boxW - inset * (if (sticker) 2 else 1) - (if (sticker) 0f else 0f)).toInt()
            val lp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = if (sticker) 0xFF1B1916.toInt() else fg; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            var size = 88f
            var layout: StaticLayout
            val pad = if (sticker) 56f else 0f
            while (true) {
                lp.textSize = size
                layout = StaticLayout.Builder.obtain(text, 0, text.length, lp, textW).setAlignment(Layout.Alignment.ALIGN_NORMAL).setLineSpacing(0f, 1.12f).build()
                if (layout.height <= areaH - pad * 2 || size <= 34f) break
                size -= 4f
            }
            val lh = layout.height.toFloat()
            if (sticker) {
                // a cream card holding the words, like the stickers other apps use
                val cardH = lh + pad * 2
                val cy = areaTop + (areaH - cardH) / 2f
                val card = RectF(boxL, cy, boxL + boxW, cy + cardH)
                c.drawRoundRect(card, 48f, 48f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF8F3EA.toInt() })
                c.drawRoundRect(RectF(card.left + 28f, card.top + 22f, card.left + 28f + 10f, card.top + 22f + 56f), 5f, 5f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AMBER })
                c.save(); c.translate(card.left + inset, card.top + pad); layout.draw(c); c.restore()
            } else {
                val ly = areaTop + (areaH - lh).coerceAtLeast(0f) / 2f
                c.drawRoundRect(RectF(m, ly + 6f, m + 10f, ly + lh - 6f), 5f, 5f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AMBER })
                c.save(); c.translate(m + inset, ly); layout.draw(c); c.restore()
            }

            // ── footer: milo. + where to get it + QR ──
            c.drawRect(m, footTop - 20f, fw - m, footTop - 18f, Paint().apply { color = if (light) 0x221B1916 else 0x33FFFFFF })
            val fp = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = AMBER; textSize = 76f
                typeface = try {
                    val b = androidx.core.content.res.ResourcesCompat.getFont(ctx, com.ubayanda.milonative.R.font.figtree_italic)
                    if (b != null && android.os.Build.VERSION.SDK_INT >= 28) Typeface.create(b, 900, true) else Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
                } catch (swallowed: Exception) { com.ubayanda.milonative.util.ErrorLog.swallowed(swallowed); Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC) }
                letterSpacing = -0.04f
            }
            val qr = 200f
            val qx = fw - m - qr
            val textMax = qx - 40f - m
            c.drawText("milo.", m, footTop + 84f, fp)
            val s1 = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = fg; textSize = 34f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            c.drawText(ellipsize("Synced lyrics. Free music player.", s1, textMax), m, footTop + 142f, s1)
            val s2 = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = fg2; textSize = 30f }
            c.drawText(ellipsize("Scan or visit " + shortLink(), s2, textMax), m, footTop + 190f, s2)
            drawQr(c, link(), qx, footTop + 25f, qr, if (light) 0xFF1B1916.toInt() else 0xFF14120F.toInt())

            val dir = File(ctx.cacheDir, "cards").also { it.mkdirs() }
            dir.listFiles()?.filter { System.currentTimeMillis() - it.lastModified() > 3600_000L }?.forEach { it.delete() }
            val f = File(dir, "lyric-" + System.currentTimeMillis() + ".png")
            f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "$title · $artist\nListen with synced lyrics on milo: ${link()}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            ctx.startActivity(Intent.createChooser(send, "Share lyric card").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (swallowed: Exception) { com.ubayanda.milonative.util.ErrorLog.swallowed(swallowed);
            android.widget.Toast.makeText(ctx, "Couldn't make the card.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    /** A real blur without RenderEffect: shrink the cover to a few pixels and stretch it back with smoothing. */
    private fun blurred(src: Bitmap, w: Int, h: Int): Bitmap? = try {
        val s = if (src.config == Bitmap.Config.HARDWARE) src.copy(Bitmap.Config.ARGB_8888, false) else src
        val small = Bitmap.createScaledBitmap(s, 18, 18, true)
        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also {
            Canvas(it).drawBitmap(small, null, RectF(0f, 0f, w.toFloat(), h.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG))
        }
    } catch (swallowed: Exception) { com.ubayanda.milonative.util.ErrorLog.swallowed(swallowed); null }

    /** QR on a white rounded plate (white so every phone camera reads it, whatever the card colour). */
    private fun drawQr(c: Canvas, text: String, x: Float, y: Float, size: Float, dark: Int) {
        val plate = 18f
        c.drawRoundRect(RectF(x - plate, y - plate, x + size + plate, y + size + plate), 28f, 28f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() })
        try {
            val hints = mapOf<com.google.zxing.EncodeHintType, Any>(
                com.google.zxing.EncodeHintType.MARGIN to 0,
                com.google.zxing.EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.M)
            val bm = com.google.zxing.qrcode.QRCodeWriter().encode(text, com.google.zxing.BarcodeFormat.QR_CODE, 0, 0, hints)
            val cell = size / bm.width
            val p = Paint().apply { color = dark; isAntiAlias = false }
            for (yy in 0 until bm.height) for (xx in 0 until bm.width) if (bm.get(xx, yy)) {
                c.drawRect(x + xx * cell, y + yy * cell, x + (xx + 1) * cell + 0.6f, y + (yy + 1) * cell + 0.6f, p)
            }
        } catch (swallowed: Exception) { com.ubayanda.milonative.util.ErrorLog.swallowed(swallowed) }
    }

    private fun ellipsize(s: String, p: TextPaint, width: Float): String =
        android.text.TextUtils.ellipsize(s, p, width, android.text.TextUtils.TruncateAt.END).toString()
}
