package com.ubayanda.milonative.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/**
 * Where the person wants recommendations to come from: their own country, the whole world, or a mix.
 * The choice is remembered and drives Discover (radio, previews and YouTube: trending, new releases and genres).
 */
object Region {
    const val LOCAL = 0
    const val BOTH = 1
    const val WORLD = 2

    /** One search: the words to look up and the country the results should lean towards ("" = leave it to the service). */
    data class Q(val term: String, val country: String)
    data class Chip(val label: String, val qs: List<Q>)

    private val _mode = MutableStateFlow(LOCAL)
    val mode: StateFlow<Int> = _mode
    private var prefs: android.content.SharedPreferences? = null

    fun init(ctx: Context) {
        if (prefs != null) return
        prefs = ctx.applicationContext.getSharedPreferences("milo_ui", Context.MODE_PRIVATE)
        _mode.value = prefs!!.getInt("region", LOCAL).coerceIn(0, 2)
        _hideLocal.value = prefs!!.getBoolean("hideLocalOnline", false)
    }

    /** Home hides the songs on this phone while there is a connection. */
    private val _hideLocal = MutableStateFlow(false)
    val hideLocal: StateFlow<Boolean> = _hideLocal
    fun setHideLocal(v: Boolean) { _hideLocal.value = v; prefs?.edit()?.putBoolean("hideLocalOnline", v)?.apply() }

    fun set(v: Int) { _mode.value = v; prefs?.edit()?.putInt("region", v)?.apply() }

    /** Two-letter country of this phone ("ZA"), falling back to the US when the phone doesn't say. */
    val home: String get() = Locale.getDefault().country.ifEmpty { "US" }.uppercase()
    val homeName: String get() = Locale("", home).getDisplayCountry(Locale.ENGLISH).ifEmpty { home }

    private fun za() = home == "ZA"

    private fun local(): List<Chip> {
        val c = home
        return if (za()) listOf(
            Chip("Trending", listOf(Q("top South African songs this week", c))),
            Chip("New releases", listOf(Q("new South African music releases", c))),
            Chip("Amapiano", listOf(Q("amapiano hits", c))),
            Chip("Afro house", listOf(Q("south african afro house", c))),
            Chip("SA hip-hop", listOf(Q("south african hip hop", c))),
            Chip("Gqom", listOf(Q("gqom", c))),
            Chip("Gospel", listOf(Q("south african gospel", c))),
            Chip("Afropop", listOf(Q("south african afropop", c))),
        ) else listOf(
            Chip("Trending", listOf(Q("top $homeName songs this week", c))),
            Chip("New releases", listOf(Q("new $homeName music releases", c))),
            Chip("Pop", listOf(Q("$homeName pop hits", c))),
            Chip("Hip-hop", listOf(Q("$homeName hip hop", c))),
            Chip("Rock", listOf(Q("$homeName rock", c))),
            Chip("Dance", listOf(Q("$homeName dance music", c))),
        )
    }

    private fun world(): List<Chip> = listOf(
        Chip("Trending", listOf(Q("top songs this week", "US"))),
        Chip("New releases", listOf(Q("new music friday latest releases", "US"))),
        Chip("Billboard", listOf(Q("billboard hot 100", "US"))),
        Chip("Pop", listOf(Q("pop hits", "US"))),
        Chip("Hip-hop", listOf(Q("hip hop hits", "US"))),
        Chip("R&B", listOf(Q("r&b hits", "US"))),
        Chip("Rock", listOf(Q("rock hits", "US"))),
        Chip("Dance", listOf(Q("dance electronic hits", "US"))),
    )

    /** The category chips for the current choice. "Both" mixes a local and a worldwide search in each of the first two. */
    fun chips(m: Int = _mode.value): List<Chip> = when (m) {
        LOCAL -> local()
        WORLD -> world()
        else -> {
            val l = local(); val w = world()
            listOf(
                Chip("Trending", l[0].qs + w[0].qs),
                Chip("New releases", l[1].qs + w[1].qs),
            ) + l.drop(2).take(3) + w.drop(3).take(3)
        }
    }

    /** New-release albums to show for the choice. */
    fun albumQs(m: Int = _mode.value): List<Q> {
        val l = Q(if (za()) "new South African albums" else "new $homeName albums", home)
        val w = Q("new albums this week", "US")
        return when (m) { LOCAL -> listOf(l); WORLD -> listOf(w); else -> listOf(l, w) }
    }

    private val gate = Mutex()

    /** Runs one YouTube lookup leaning towards [country] ("" = the phone's own). One at a time, so two lookups never fight over the setting. */
    suspend fun <T> inCountry(country: String, block: suspend () -> T): T = gate.withLock {
        YtMusic.glOverride = country.ifEmpty { null }
        try { block() } finally { YtMusic.glOverride = null }
    }

    /** Which country charts to read: yours, the big international ones (US and UK), or both. International never includes your own. */
    fun chartCountries(m: Int = _mode.value): List<String> = when (m) {
        LOCAL -> listOf(home.lowercase())
        WORLD -> listOf("us", "gb")
        else -> listOf(home.lowercase(), "us")
    }

    /** The country to ask radio stations for: only when the choice is "Local". */
    fun radioCountry(m: Int = _mode.value): String = if (m == LOCAL) home else ""

    /** A typed search leans towards the chosen side too. */
    fun forSearch(term: String, m: Int = _mode.value): List<Q> = when (m) {
        LOCAL -> listOf(Q(term, home))
        WORLD -> listOf(Q(term, "US"))
        else -> listOf(Q(term, ""))
    }

    /** Alternates two result lists (a, b, a, b, ...) and drops repeats so "Both" really is a mix. */
    fun <T> mix(lists: List<List<T>>, key: (T) -> Any): List<T> {
        val out = ArrayList<T>(); val seen = HashSet<Any>()
        var i = 0
        while (lists.any { i < it.size }) {
            for (l in lists) if (i < l.size && seen.add(key(l[i]))) out.add(l[i])
            i++
        }
        return out
    }
}
