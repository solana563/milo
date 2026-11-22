package com.ubayanda.milonative.util

/** Finds the YouTube / YouTube Music playlist or album in a pasted link. Only public playlists open; nothing is signed into. */
object PlaylistLink {
    /** the page id milo's YouTube pages use ("VL" + playlist id), or null if this isn't a playlist link */
    fun pageId(link: String): String? {
        val text = link.trim()
        if (text.isEmpty()) return null
        val list = Regex("[?&]list=([A-Za-z0-9_-]+)").find(text)?.groupValues?.get(1)
        if (list != null) {
            // radio mixes (RD...) and the "liked music" / watch-later lists are not playlists anybody can open without signing in
            if (list.startsWith("RD") || list == "LM" || list == "WL") return null
            return if (list.startsWith("VL")) list else "VL$list"
        }
        val browse = Regex("/browse/((?:VL|MPRE)[A-Za-z0-9_-]+)").find(text)?.groupValues?.get(1)
        return browse
    }
}
