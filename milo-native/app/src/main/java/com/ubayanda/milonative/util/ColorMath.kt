package com.ubayanda.milonative.util

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * The song's true colour: group the cover's pixels into hue families and take the biggest family,
 * instead of averaging everything (an average of red and blue art is a muddy purple that is neither).
 * Ported 1:1 from the web app so both apps pick the same colour.
 */
object ColorMath {
    /** Returns packed 0xRRGGBB, or -1 when there are no usable pixels. Hue is circular (reds don't split) and only pixels near the winning hue are averaged, so red stays red. */
    fun dominant(argb: IntArray): Int {
        val n = 24
        val score = DoubleArray(n)
        val greyScore = DoubleArray(4)
        var ar = 0.0; var ag = 0.0; var ab = 0.0; var an = 0
        val keep = ArrayList<DoubleArray>()   // r, g, b, hue(0..1), weight
        for (p in argb) {
            if ((p ushr 24) < 200) continue
            val r = (p shr 16) and 255
            val g = (p shr 8) and 255
            val b = p and 255
            ar += r; ag += g; ab += b; an++
            val hsl = rgbToHsl(r, g, b)
            val h = hsl[0]; val sat = hsl[1]; val l = hsl[2]
            if (l < 0.08 || l > 0.95) continue
            val w = (0.2 + sat * sat * 2.0) * (1 - abs(l - 0.5) * 0.8)
            if (sat < 0.12) { greyScore[min(3, (l * 4).toInt())] += w; continue }
            score[(h * n).toInt() % n] += w
            keep.add(doubleArrayOf(r.toDouble(), g.toDouble(), b.toDouble(), h, w))
        }
        if (keep.isEmpty()) {
            val gi = greyScore.indices.maxByOrNull { greyScore[it] } ?: 0
            return if (an > 0 && greyScore[gi] > 0) pack((ar / an).toInt(), (ag / an).toInt(), (ab / an).toInt()) else if (an > 0) pack((ar / an).toInt(), (ag / an).toInt(), (ab / an).toInt()) else -1
        }
        // the colourful part has to matter; mostly grey art stays grey
        val colourful = score.sum()
        if (colourful < greyScore.sum() * 0.5) {
            return if (an > 0) pack((ar / an).toInt(), (ag / an).toInt(), (ab / an).toInt()) else -1
        }
        // smooth neighbours (circular) so one hue family wins, then take the peak
        var peak = 0; var best = -1.0
        for (i in 0 until n) {
            val v = score[i] + 0.5 * (score[(i + 1) % n] + score[(i + n - 1) % n])
            if (v > best) { best = v; peak = i }
        }
        val centre = (peak + 0.5) / n
        var wr = 0.0; var wg = 0.0; var wb = 0.0; var wt = 0.0
        for (k in keep) {
            var d = abs(k[3] - centre); if (d > 0.5) d = 1 - d
            if (d > 1.5 / n) continue
            wr += k[0] * k[4]; wg += k[1] * k[4]; wb += k[2] * k[4]; wt += k[4]
        }
        if (wt <= 0) return pack((ar / an).toInt(), (ag / an).toInt(), (ab / an).toInt())
        return pack((wr / wt).toInt(), (wg / wt).toInt(), (wb / wt).toInt())
    }

    /** UI accent from the song's colour: same hue and saturation, only the lightness is nudged so it stays readable. */
    fun accent(rgb: Int): Int {
        val r = (rgb shr 16) and 255
        val g = (rgb shr 8) and 255
        val b = rgb and 255
        val hsl = rgbToHsl(r, g, b)
        if (hsl[1] < 0.1) return pack(226, 226, 226)
        val l = min(0.62, max(0.44, hsl[2]))
        val out = hslToRgb(hsl[0], min(1.0, max(hsl[1], 0.55) * 1.04), l)
        return pack(out[0], out[1], out[2])
    }

    /** Relative brightness 0..1, used to pick light or dark text over art. */
    fun luminance(rgb: Int): Double {
        val r = (rgb shr 16) and 255
        val g = (rgb shr 8) and 255
        val b = rgb and 255
        return (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0
    }

    fun pack(r: Int, g: Int, b: Int): Int = (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)

    fun rgbToHsl(r: Int, g: Int, b: Int): DoubleArray {
        val rf = r / 255.0; val gf = g / 255.0; val bf = b / 255.0
        val mx = max(rf, max(gf, bf)); val mn = min(rf, min(gf, bf))
        val l = (mx + mn) / 2
        val d = mx - mn
        if (d == 0.0) return doubleArrayOf(0.0, 0.0, l)
        val s = if (l > 0.5) d / (2 - mx - mn) else d / (mx + mn)
        var h = when (mx) {
            rf -> (gf - bf) / d + (if (gf < bf) 6 else 0)
            gf -> (bf - rf) / d + 2
            else -> (rf - gf) / d + 4
        }
        h /= 6
        return doubleArrayOf(h, s, l)
    }

    fun hslToRgb(h: Double, s: Double, l: Double): IntArray {
        if (s == 0.0) { val v = Math.round(l * 255).toInt(); return intArrayOf(v, v, v) }
        val q = if (l < 0.5) l * (1 + s) else l + s - l * s
        val p = 2 * l - q
        fun f(t0: Double): Int {
            var t = t0
            if (t < 0) t += 1.0
            if (t > 1) t -= 1.0
            val v = when {
                t < 1.0 / 6 -> p + (q - p) * 6 * t
                t < 0.5 -> q
                t < 2.0 / 3 -> p + (q - p) * (2.0 / 3 - t) * 6
                else -> p
            }
            return Math.round(v * 255).toInt()
        }
        return intArrayOf(f(h + 1.0 / 3), f(h), f(h - 1.0 / 3))
    }
}
