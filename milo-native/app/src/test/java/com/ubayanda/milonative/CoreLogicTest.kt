package com.ubayanda.milonative

import com.ubayanda.milonative.data.EqShare
import com.ubayanda.milonative.data.LibrarySync
import com.ubayanda.milonative.util.M3u
import com.ubayanda.milonative.util.NaturalOrder
import com.ubayanda.milonative.data.OfflineStore
import com.ubayanda.milonative.model.Track
import com.ubayanda.milonative.data.SmartMix
import com.ubayanda.milonative.data.SongStat
import com.ubayanda.milonative.data.Taste
import org.junit.Assert.*
import org.junit.Test

class CoreLogicTest {
    @Test fun syncKeyIgnoresCaseAndSpacing() {
        assertEquals(LibrarySync.key("  Hello   World ", "ARTIST"), LibrarySync.key("hello world", "artist"))
    }
    @Test fun syncKeyDistinguishesArtists() {
        assertNotEquals(LibrarySync.key("Intro", "A"), LibrarySync.key("Intro", "B"))
    }
    @Test fun serverStreamsAreNotSynced() {
        assertFalse(LibrarySync.syncable("https://x/rest/stream?id=1"))
        assertTrue(LibrarySync.syncable("content://media/external/audio/media/5"))
    }
    @Test fun eqShareRoundTrips() {
        val msg = EqShare.make("Warm", 40, 25, listOf(1.5f, 0f, -2.0f, 3.0f, 0.5f))
        val p = EqShare.parse(msg)!!
        assertEquals("Warm", p.name); assertEquals(40, p.bass); assertEquals(25, p.spatial)
        assertEquals(listOf(1.5f, 0f, -2.0f, 3.0f, 0.5f), p.levels)
    }
    @Test fun eqShareRejectsPlainText() { assertNull(EqShare.parse("hey, what's up")) }
    @Test fun m3uParsesExtinf() {
        val r = M3u.parse("#EXTM3U\n#EXTINF:200,Daft Punk - One More Time\n/music/a.mp3\n")
        assertEquals(listOf("One More Time" to "Daft Punk"), r)
    }
    @Test fun m3uFallsBackToFileName() {
        assertEquals(listOf("song" to ""), M3u.parse("/music/song.mp3\n"))
    }
    @Test fun m3uEmptyIsEmpty() { assertTrue(M3u.parse("").isEmpty()) }
    @Test fun naturalOrderComparesNumbers() {
        assertEquals(listOf("2 two", "10 ten"), listOf("10 ten", "2 two").sortedWith(NaturalOrder))
    }
    @Test fun naturalOrderSkipsLeadingSymbolsAndCase() {
        assertEquals(listOf("(a) one", "Beta"), listOf("Beta", "(a) one").sortedWith(NaturalOrder))
    }

    @Test fun sortingTwentyThousandTitlesIsFast() {
        val rnd = java.util.Random(7)
        val titles = List(20_000) { "Song ${rnd.nextInt(100000)} " + ('a' + rnd.nextInt(26)) }
        val t0 = System.nanoTime()
        val sorted = titles.sortedWith(NaturalOrder)
        val ms = (System.nanoTime() - t0) / 1_000_000
        assertEquals(20_000, sorted.size)
        assertTrue("sort took $ms ms", ms < 5_000)
    }

    private fun track(durMs: Long) = Track(1, "yt:abc", "T", "A", "Al", 1, durMs, 0, 0, 0)
    @Test fun downloadEstimateScalesWithLengthAndSaver() {
        val j = OfflineStore.Job("abc", track(200_000), OfflineStore.State.QUEUED)
        assertEquals(4_000_000L, j.estimate(false))
        assertEquals(1_200_000L, j.estimate(true))
        assertEquals(5_000_000L, j.copy(totalBytes = 5_000_000L).estimate(false))
    }
    @Test fun downloadDefaultsKeepOldBehaviour() {
        val s = OfflineStore.Settings()
        assertFalse(s.wifiOnly); assertEquals(0, s.limitMb); assertFalse(s.saver)
    }

    // ---- smart mixes ----
    private val NOW = 1_800_000_000_000L
    private fun song(id: Long, title: String, artist: String, genre: String = "", year: Int = 2020, dur: Long = 200_000L, added: Long = 0L) =
        Track(id, "content://$id", title, artist, "Al", 1, dur, 0, year, added, genre = genre)
    private fun taste(vararg s: Pair<Track, SongStat>, favs: Set<Long> = emptySet(), banA: Set<String> = emptySet(), banS: Set<String> = emptySet(), since: Long = 0L) =
        Taste(songs = s.associate { SmartMix.key(it.first) to it.second }, favs = favs, bannedArtists = banA, bannedSongs = banS, since = since, now = NOW)

    @Test fun blockedArtistAndSongNeverAppear() {
        val a = song(1, "A1", "Bad"); val b = song(2, "B1", "Good"); val c = song(3, "B2", "Good")
        val t = taste(a to SongStat(5), b to SongStat(5), c to SongStat(5), banA = setOf("bad"), banS = setOf(SmartMix.key(c)))
        val all = listOf(a, b, c)
        listOf(SmartMix.smart(all, t, 1), SmartMix.daily(all, t, 1), SmartMix.discovery(all, t, 1)).forEach { r ->
            assertTrue(r.none { it.id == 1L || it.id == 3L })
        }
    }
    @Test fun skipsLowerAffinityAndLikesRaiseIt() {
        val x = song(1, "X", "A"); val y = song(2, "Y", "B")
        val t = taste(x to SongStat(plays = 3, skips = 6), y to SongStat(plays = 3), favs = setOf(2L))
        assertTrue(SmartMix.affinity(y, t) > SmartMix.affinity(x, t))
    }
    @Test fun forgottenNeedsRealHistory() {
        val old = song(1, "Old", "A"); val recent = song(2, "Recent", "B")
        val t = taste(old to SongStat(plays = 5, last = NOW - 90 * SmartMix.DAY), recent to SongStat(plays = 5, last = NOW - 2 * SmartMix.DAY))
        assertEquals(listOf(1L), SmartMix.forgotten(listOf(old, recent), t).map { it.id })
    }
    @Test fun likedButNeverTrackedIsNotCalledForgottenTooEarly() {
        val s = song(1, "S", "A")
        assertTrue(SmartMix.forgotten(listOf(s), taste(favs = setOf(1L), since = NOW - 5 * SmartMix.DAY)).isEmpty())
        assertEquals(1, SmartMix.forgotten(listOf(s), taste(favs = setOf(1L), since = NOW - 100 * SmartMix.DAY)).size)
    }
    @Test fun moodNeedsEnoughInformation() {
        val few = (1L..5L).map { song(it, "T$it", "A$it", genre = "Ambient") }
        assertNull(SmartMix.mood(few, taste(), SmartMix.Mood.CHILL, 1, 2026))
        val untagged = (1L..20L).map { song(it, "T$it", "A$it") }
        assertNull(SmartMix.mood(untagged, taste(), SmartMix.Mood.CHILL, 1, 2026))
        val many = (1L..20L).map { song(it, "T$it", "A$it", genre = "Ambient") }
        assertNotNull(SmartMix.mood(many, taste(), SmartMix.Mood.CHILL, 1, 2026))
    }
    @Test fun nostalgiaUsesYearOnly() {
        val old = (1L..10L).map { song(it, "T$it", "A$it", year = 1999) }
        assertEquals(10, SmartMix.mood(old, taste(), SmartMix.Mood.NOSTALGIA, 1, 2026)!!.size)
    }
    @Test fun dailyRotationAvoidsRecentlyPlayedAndRepeatArtists() {
        val all = (1L..30L).map { song(it, "T$it", "Artist${it % 3}") }
        val just = all[0]
        val t = taste(just to SongStat(plays = 9, last = NOW - 3_600_000L), favs = all.map { it.id }.toSet())
        val r = SmartMix.daily(all, t, 5, 12)
        assertFalse(r.any { it.id == just.id })
        assertTrue(r.groupBy { it.artist }.values.all { it.size <= 3 })
    }
    @Test fun sameSeedSameMixDifferentSeedDifferent() {
        val all = (1L..60L).map { song(it, "T$it", "A$it") }
        val t = taste(*all.take(30).map { it to SongStat(plays = 2) }.toTypedArray())
        assertEquals(SmartMix.smart(all, t, 7).map { it.id }, SmartMix.smart(all, t, 7).map { it.id })
        assertNotEquals(SmartMix.smart(all, t, 7).map { it.id }, SmartMix.smart(all, t, 8).map { it.id })
    }
}
