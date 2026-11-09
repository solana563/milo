package com.ubayanda.milonative

import com.ubayanda.milonative.data.EqShare
import com.ubayanda.milonative.data.LibrarySync
import com.ubayanda.milonative.util.M3u
import com.ubayanda.milonative.util.NaturalOrder
import com.ubayanda.milonative.data.OfflineStore
import com.ubayanda.milonative.model.Track
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
}
