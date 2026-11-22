package com.ubayanda.milonative.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * milo's own pixel lettering: a 5 x 7 font drawn square by square, so it needs no font file and looks the same on every phone.
 * Capitals, digits and a little punctuation; anything else is left blank. Lower case is shown as capitals.
 */
object PixelFont {
    const val W = 5
    const val H = 7
    private val g: Map<Char, String> = mapOf(
        'A' to "01110 10001 10001 11111 10001 10001 10001",
        'B' to "11110 10001 10001 11110 10001 10001 11110",
        'C' to "01110 10001 10000 10000 10000 10001 01110",
        'D' to "11110 10001 10001 10001 10001 10001 11110",
        'E' to "11111 10000 10000 11110 10000 10000 11111",
        'F' to "11111 10000 10000 11110 10000 10000 10000",
        'G' to "01110 10001 10000 10111 10001 10001 01111",
        'H' to "10001 10001 10001 11111 10001 10001 10001",
        'I' to "01110 00100 00100 00100 00100 00100 01110",
        'J' to "00111 00010 00010 00010 00010 10010 01100",
        'K' to "10001 10010 10100 11000 10100 10010 10001",
        'L' to "10000 10000 10000 10000 10000 10000 11111",
        'M' to "10001 11011 10101 10101 10001 10001 10001",
        'N' to "10001 11001 10101 10011 10001 10001 10001",
        'O' to "01110 10001 10001 10001 10001 10001 01110",
        'P' to "11110 10001 10001 11110 10000 10000 10000",
        'Q' to "01110 10001 10001 10001 10101 10010 01101",
        'R' to "11110 10001 10001 11110 10100 10010 10001",
        'S' to "01111 10000 10000 01110 00001 00001 11110",
        'T' to "11111 00100 00100 00100 00100 00100 00100",
        'U' to "10001 10001 10001 10001 10001 10001 01110",
        'V' to "10001 10001 10001 10001 10001 01010 00100",
        'W' to "10001 10001 10001 10101 10101 11011 10001",
        'X' to "10001 10001 01010 00100 01010 10001 10001",
        'Y' to "10001 10001 01010 00100 00100 00100 00100",
        'Z' to "11111 00001 00010 00100 01000 10000 11111",
        '0' to "01110 10001 10011 10101 11001 10001 01110",
        '1' to "00100 01100 00100 00100 00100 00100 01110",
        '2' to "01110 10001 00001 00010 00100 01000 11111",
        '3' to "11110 00001 00001 01110 00001 00001 11110",
        '4' to "00010 00110 01010 10010 11111 00010 00010",
        '5' to "11111 10000 11110 00001 00001 10001 01110",
        '6' to "00110 01000 10000 11110 10001 10001 01110",
        '7' to "11111 00001 00010 00100 01000 01000 01000",
        '8' to "01110 10001 10001 01110 10001 10001 01110",
        '9' to "01110 10001 10001 01111 00001 00010 01100",
        ':' to "00000 00100 00100 00000 00100 00100 00000",
        '.' to "00000 00000 00000 00000 00000 01100 01100",
        ',' to "00000 00000 00000 00000 01100 00100 01000",
        '-' to "00000 00000 00000 11111 00000 00000 00000",
        '+' to "00000 00100 00100 11111 00100 00100 00000",
        '/' to "00001 00001 00010 00100 01000 10000 10000",
        '!' to "00100 00100 00100 00100 00100 00000 00100",
        '?' to "01110 10001 00001 00010 00100 00000 00100",
        '\'' to "00100 00100 01000 00000 00000 00000 00000",
        '&' to "01100 10010 10100 01000 10101 10010 01101",
        '(' to "00010 00100 01000 01000 01000 00100 00010",
        ')' to "01000 00100 00010 00010 00010 00100 01000",
        '%' to "11001 11010 00010 00100 01000 01011 10011",
    )
    private val rows: Map<Char, List<String>> = g.mapValues { it.value.split(' ') }

    /** True when every letter of [text] exists in the font (spaces count), so titles in other scripts can fall back to normal text. */
    fun supports(text: String): Boolean = text.all { it == ' ' || glyph(it) != null }

    fun glyph(c: Char): List<String>? = rows[c.uppercaseChar()]

    /** Width in cells of [text] (one blank column between letters). */
    fun cells(text: String): Int = if (text.isEmpty()) 0 else text.length * (W + 1) - 1
}

/** Pixel lettering at a fixed cell size. */
@Composable
fun PixelText(text: String, color: Color, cell: Dp, modifier: Modifier = Modifier) {
    val cells = PixelFont.cells(text)
    Canvas(modifier.width(cell * cells).height(cell * PixelFont.H)) {
        val c = cell.toPx()
        text.forEachIndexed { i, ch ->
            val rows = PixelFont.glyph(ch) ?: return@forEachIndexed
            val x0 = i * (PixelFont.W + 1) * c
            for (y in 0 until PixelFont.H) for (x in 0 until PixelFont.W) {
                if (rows[y][x] == '1') drawRect(color, Offset(x0 + x * c, y * c), Size(c, c))
            }
        }
    }
}

/** Pixel lettering that shrinks (never below [min]) so the whole text fits the width it is given, up to [max] per cell. */
@Composable
fun PixelTextFit(text: String, color: Color, max: Dp, min: Dp = 2.dp, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        val cells = PixelFont.cells(text).coerceAtLeast(1)
        val fit = with(LocalDensity.current) { (constraints.maxWidth.toFloat() / cells).toDp() }
        val cell = if (fit < max) (if (fit < min) min else fit) else max
        PixelText(text, color, cell)
    }
}
