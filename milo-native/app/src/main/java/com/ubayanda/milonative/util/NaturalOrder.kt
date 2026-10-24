package com.ubayanda.milonative.util

import com.ubayanda.milonative.model.Track
import java.text.Collator
import java.util.Locale

/**
 * A to Z the way people expect: case and accents ignored, leading symbols skipped,
 * and numbers compare as numbers ("2 two" comes before "10 ten").
 */
object NaturalOrder : Comparator<String> {
    private val collator: Collator = Collator.getInstance(Locale.getDefault()).apply { strength = Collator.PRIMARY }

    fun key(s: String): String = s.trimStart { !it.isLetterOrDigit() }

    override fun compare(a: String, b: String): Int {
        val x = key(a)
        val y = key(b)
        var i = 0
        var j = 0
        while (i < x.length && j < y.length) {
            val dx = x[i].isDigit()
            val dy = y[j].isDigit()
            if (dx && dy) {
                var ei = i
                while (ei < x.length && x[ei].isDigit()) ei++
                var ej = j
                while (ej < y.length && y[ej].isDigit()) ej++
                val nx = x.substring(i, ei).trimStart('0')
                val ny = y.substring(j, ej).trimStart('0')
                if (nx.length != ny.length) return nx.length - ny.length
                val c = nx.compareTo(ny)
                if (c != 0) return c
                i = ei
                j = ej
            } else {
                var ei = i
                while (ei < x.length && !x[ei].isDigit()) ei++
                var ej = j
                while (ej < y.length && !y[ej].isDigit()) ej++
                val c = collator.compare(x.substring(i, ei), y.substring(j, ej))
                if (c != 0) return c
                i = ei
                j = ej
            }
        }
        return (x.length - i) - (y.length - j)
    }

    /** First letter for the A..Z section headers; everything else goes under "#". */
    fun letter(s: String): String {
        val c = key(s).firstOrNull() ?: return "#"
        val u = java.text.Normalizer.normalize(c.toString(), java.text.Normalizer.Form.NFD).firstOrNull()?.uppercaseChar() ?: return "#"
        return if (u in 'A'..'Z') u.toString() else "#"
    }
}

val trackByTitle: Comparator<Track> = Comparator { a, b ->
    val c = NaturalOrder.compare(a.title, b.title)
    if (c != 0) c else NaturalOrder.compare(a.artist, b.artist)
}
