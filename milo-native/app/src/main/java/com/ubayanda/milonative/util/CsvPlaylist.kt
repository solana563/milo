package com.ubayanda.milonative.util

/**
 * Reads a playlist exported as a spreadsheet (.csv) by the usual export tools: it finds the song and artist columns by their header names
 * ("Track Name", "Title", "Song", "Artist Name(s)", "Artist" ...) and ignores the rest. Quoted cells and commas inside quotes are handled.
 * With no recognisable header it takes the first column as the title and the second as the artist.
 * Same job as the CSV importer in Mochy Music (GPL-3.0, github.com/realidkroo/Mochy-Music); written separately for milo.
 */
object CsvPlaylist {
    private val TITLE = listOf("track name", "track", "title", "song", "song name", "name")
    private val ARTIST = listOf("artist name(s)", "artist names", "artist name", "artist(s)", "artists", "artist", "album artist name(s)")

    /** (title, artist) pairs in file order. The artist is empty when the file has none. */
    fun parse(text: String): List<Pair<String, String>> {
        val rows = rows(text.removePrefix("﻿"))
        if (rows.isEmpty()) return emptyList()
        val head = rows[0].map { it.trim().lowercase() }
        var ti = TITLE.firstNotNullOfOrNull { n -> head.indexOf(n).takeIf { it >= 0 } } ?: -1
        var ai = ARTIST.firstNotNullOfOrNull { n -> head.indexOf(n).takeIf { it >= 0 } } ?: -1
        val hasHeader = ti >= 0 || ai >= 0
        if (ti < 0) ti = 0
        if (ai < 0 && !hasHeader && rows[0].size > 1) ai = 1
        val body = if (hasHeader) rows.drop(1) else rows
        return body.mapNotNull { r ->
            val title = r.getOrNull(ti)?.trim().orEmpty()
            if (title.isEmpty()) return@mapNotNull null
            // several artists come as "A;B" or "A, B": the first one is the one songs are filed under
            val artist = r.getOrNull(ai)?.trim().orEmpty().split(';', '、').first().trim()
            title to artist
        }
    }

    /** the cells of every row; a newline inside quotes stays in its cell */
    private fun rows(text: String): List<List<String>> {
        val delim = if (text.lineSequence().firstOrNull().orEmpty().count { it == ';' } > text.lineSequence().firstOrNull().orEmpty().count { it == ',' }) ';' else ','
        val out = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val cell = StringBuilder()
        var quoted = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                quoted && c == '"' && text.getOrNull(i + 1) == '"' -> { cell.append('"'); i++ }
                c == '"' -> quoted = !quoted
                !quoted && c == delim -> { row.add(cell.toString()); cell.setLength(0) }
                !quoted && (c == '\n' || c == '\r') -> {
                    if (c == '\r' && text.getOrNull(i + 1) == '\n') i++
                    row.add(cell.toString()); cell.setLength(0)
                    if (row.any { it.isNotBlank() }) out.add(row)
                    row = ArrayList()
                }
                else -> cell.append(c)
            }
            i++
        }
        row.add(cell.toString())
        if (row.any { it.isNotBlank() }) out.add(row)
        return out
    }
}
