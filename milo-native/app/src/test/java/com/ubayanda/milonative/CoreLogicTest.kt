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
import com.ubayanda.milonative.data.Connect
import com.ubayanda.milonative.data.Sessions
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

    // ---- Connect ----
    @Test fun deviceOnlineWindow() {
        assertTrue(Connect.online(100_000L, 120_000L))
        assertFalse(Connect.online(100_000L, 150_000L))
        assertFalse(Connect.online(0L, 10L))
    }
    @Test fun staleCommandsAreIgnored() {
        assertTrue(Connect.fresh(100_000L, 110_000L))
        assertFalse(Connect.fresh(100_000L, 200_000L))
        assertFalse(Connect.fresh(500_000L, 100_000L))     // from the far future: clock nonsense
    }
    @Test fun livePositionAdvancesOnlyWhilePlaying() {
        assertEquals(15_000L, Connect.livePos(10_000L, 1_000L, true, 6_000L, 200_000L))
        assertEquals(10_000L, Connect.livePos(10_000L, 1_000L, false, 6_000L, 200_000L))
        assertEquals(200_000L, Connect.livePos(199_000L, 0L, true, 50_000L, 200_000L))
    }

    /** The Round 33 crash: sorting with random scores inside the comparison breaks only on lists of 32+ songs. */
    @Test fun bigLibrariesNeverBreakTheSort() {
        val all = (1L..800L).map { song(it, "T$it", "Artist${it % 40}", genre = listOf("Ambient", "Rock", "Pop", "")[(it % 4).toInt()], year = 1990 + (it % 30).toInt()) }
        val t = taste(*all.filter { it.id % 3 == 0L }.map { it to SongStat(plays = (it.id % 7).toInt(), skips = (it.id % 3).toInt(), last = NOW - (it.id % 90) * SmartMix.DAY) }.toTypedArray(), favs = all.filter { it.id % 5 == 0L }.map { it.id }.toSet(), since = NOW - 200 * SmartMix.DAY)
        for (seed in 1L..25L) {
            assertTrue(SmartMix.smart(all, t, seed).isNotEmpty())
            assertTrue(SmartMix.daily(all, t, seed).isNotEmpty())
            assertTrue(SmartMix.discovery(all, t, seed).isNotEmpty())
            SmartMix.forgotten(all, t)
            SmartMix.Mood.values().forEach { SmartMix.mood(all, t, it, seed, 2026) }
        }
    }

    // ---- Sessions ----
    @Test fun inviteCodesParseFromLinksAndTypedText() {
        assertEquals("ABC234", Sessions.parseCode("milo://room/ABC234"))
        assertEquals("ABC234", Sessions.parseCode(" abc 234 "))
        assertNull(Sessions.parseCode("hello"))
        assertNull(Sessions.parseCode("ABC12"))              // too short
        assertEquals("ABC234", Sessions.parseCode("ABCO234")) // O is never in a code, so it is dropped
    }
    @Test fun inviteLinkRoundTrips() { assertEquals("XK7P2M", Sessions.parseCode(Sessions.link("XK7P2M"))) }
    @Test fun onlyOnlineSongsAreShared() {
        assertTrue(Sessions.playable(Track(1, "yt:abc", "T", "A", "Al", 1, 1000, 0, 0, 0)))
        assertFalse(Sessions.playable(Track(2, "content://x", "T", "A", "Al", 1, 1000, 0, 0, 0)))
        assertFalse(Sessions.playable(null))
    }
    @Test fun listenersOnlySeekWhenDriftIsLarge() {
        assertEquals(-1L, Sessions.seekTarget(10_000L, 1_000L, true, 14_200L, 6_000L))   // host ~15 000, me 14 200: close enough
        assertEquals(15_000L, Sessions.seekTarget(10_000L, 1_000L, true, 30_000L, 6_000L))
        assertEquals(10_000L, Sessions.seekTarget(10_000L, 1_000L, false, 20_000L, 6_000L)) // paused host: no extrapolation
    }
    @Test fun hostAwayAfterAMinute() {
        assertFalse(Sessions.hostAway(100_000L, 150_000L))
        assertTrue(Sessions.hostAway(100_000L, 170_000L))
        assertFalse(Sessions.hostAway(0L, 170_000L))
    }

    @Test
    fun sharedPlaylistRulesAndLinks() {
        assertEquals("ABC234", com.ubayanda.milonative.data.CPlaylists.parseCode("milo://playlist/abc234"))
        assertEquals("ABC234", com.ubayanda.milonative.data.CPlaylists.parseCode(" abc 234 "))
        assertNull(com.ubayanda.milonative.data.CPlaylists.parseCode("hi"))
        val P = com.ubayanda.milonative.data.CPlaylists
        assertTrue(P.canAdd("owner")); assertTrue(P.canAdd("editor")); assertFalse(P.canAdd("viewer"))
        assertTrue(P.canRemove("owner", "x", "me"))
        assertTrue(P.canRemove("editor", "me", "me")); assertFalse(P.canRemove("editor", "x", "me"))
        assertFalse(P.canRemove("viewer", "me", "me"))
        assertEquals("milo://playlist/ABC234", P.link("ABC234"))
    }

    @Test
    fun sharedPlaylistMoveTargets() {
        val P = com.ubayanda.milonative.data.CPlaylists
        val ids = listOf(10L, 11L, 12L, 13L)
        assertNull(P.afterIdFor(ids, 2, 0))        // to the top
        assertEquals(12L, P.afterIdFor(ids, 1, 2)) // 11 moves down: goes after 12
        assertEquals(10L, P.afterIdFor(ids, 2, 1)) // 12 moves up: goes after 10
        assertEquals(13L, P.afterIdFor(ids, 0, 3))
    }

    @Test
    fun crossfadeCurves() {
        for (c in 0..2) {
            val (a0, b0) = com.ubayanda.milonative.player.XFade.gains(0f, c)
            val (a1, b1) = com.ubayanda.milonative.player.XFade.gains(1f, c)
            assertEquals(1f, a0, 0.001f); assertEquals(0f, b0, 0.001f)
            assertEquals(0f, a1, 0.001f); assertEquals(1f, b1, 0.001f)
        }
        val (a, b) = com.ubayanda.milonative.player.XFade.gains(0.5f, 0)
        assertEquals(1f, a * a + b * b, 0.001f)   // equal power
        val (la, lb) = com.ubayanda.milonative.player.XFade.gains(0.5f, 1)
        assertEquals(1f, la + lb, 0.001f)         // linear
    }

    @Test
    fun hiResRule() {
        assertTrue(com.ubayanda.milonative.player.AudioCaps.isHiRes(96000, true))
        assertFalse(com.ubayanda.milonative.player.AudioCaps.isHiRes(48000, true))
        assertFalse(com.ubayanda.milonative.player.AudioCaps.isHiRes(96000, false))
        assertEquals("FLAC", com.ubayanda.milonative.player.AudioCaps.mimeLabel("audio/flac"))
    }
}
