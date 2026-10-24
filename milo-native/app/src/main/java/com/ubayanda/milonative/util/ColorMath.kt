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
    /** Returns packed 0xRRGGBB, or -1 when there are no usable pixels. */
    fun dominant(argb: IntArray): Int {
        class Bin { var w = 0.0; var r = 0.0; var g = 0.0; var b = 0.0 }
        val bins = HashMap<Int, Bin>()
        var ar = 0.0; var ag = 0.0; var ab = 0.0; var an = 0
        for (p in argb) {
            if ((p ushr 24) < 200) continue
            val r = (p shr 16) and 255
            val g = (p shr 8) and 255
            val b = p and 255
            val hsl = rgbToHsl(r, g, b)
            ar += r; ag += g; ab += b; an++
            val h = hsl[0]; val s = hsl[1]; val l = hsl[2]
            if (l < 0.1 || l > 0.94) continue
            val key = if (s < 0.12) 100 + (l * 4).toInt() else ((h * 12).toInt()) % 12
            val w = (0.25 + s * s * 1.6) * (1 - abs(l - 0.5) * 0.9)
            val bin = bins.getOrPut(key) { Bin() }
            bin.w += w; bin.r += r * w; bin.g += g * w; bin.b += b * w
        }
        val best = bins.values.maxByOrNull { it.w }
        return when {
            best != null && best.w > 0 -> pack((best.r / best.w).toInt(), (best.g / best.w).toInt(), (best.b / best.w).toInt())
            an > 0 -> pack((ar / an).toInt(), (ag / an).toInt(), (ab / an).toInt())
            else -> -1
        }
    }

    /** UI accent from the song's colour: same hue and saturation, only the lightness is nudged so it stays readable. */
    fun accent(rgb: Int): Int {
        val r = (rgb shr 16) and 255
        val g = (rgb shr 8) and 255
        val b = rgb and 255
        val hsl = rgbToHsl(r, g, b)
        if (hsl[1] < 0.1) return pack(226, 226, 226)
        val l = min(0.7, max(0.56, hsl[2]))
        val out = hslToRgb(hsl[0], min(1.0, hsl[1] * 1.04), l)
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
