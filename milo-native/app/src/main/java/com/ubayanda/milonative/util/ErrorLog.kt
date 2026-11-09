package com.ubayanda.milonative.util

import android.content.Context
import android.content.Intent
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale

/**
 * Keeps the last few things that went wrong (a song that wouldn't stream, a crash) so one tap in Profile can send them.
 * Nothing is sent anywhere by milo.; the person chooses where the report goes. No account details are included.
 */
object ErrorLog {
    private val items = ArrayDeque<String>()
    private var installed = false
    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.US)

    fun init(c: Context) {
        if (installed) return
        installed = true
        val app = c.applicationContext
        this.app = app
        // a crash from last time, saved by the handler below
        val p = app.getSharedPreferences("milo_err", 0)
        p.getString("crash", null)?.let { add("previous crash", it); p.edit().remove("crash").apply() }
        val old = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try { p.edit().putString("crash", (e.javaClass.name + ": " + e.message + "\n" + e.stackTrace.take(12).joinToString("\n")).take(3000)).commit() } catch (_: Throwable) {}
            old?.uncaughtException(t, e)
        }
    }

    private var app: Context? = null
    private const val KEEP = "keep_history"
    private val email = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
    private val query = Regex("\\?[^\\s|]*")
    private val uuid = Regex("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    private val path = Regex("/(storage|sdcard|data)/[^\\s|]*/")
    private val bearer = Regex("(?i)(bearer|token|apikey|key)[=: ]+[A-Za-z0-9._-]{12,}")

    /** Removes anything personal before it is written: emails, tokens, ids, web-address queries, folder names. */
    fun scrub(x: String): String = x.replace(email, "<email>").replace(bearer, "$1=<hidden>").replace(uuid, "<id>").replace(query, "").replace(path, "<folder>/")

    private fun keepHistory(): Boolean = app?.getSharedPreferences("milo_err", 0)?.getBoolean(KEEP, false) == true
    fun keepOn(c: Context) = c.applicationContext.getSharedPreferences("milo_err", 0).getBoolean(KEEP, false)
    fun setKeep(c: Context, on: Boolean) {
        c.applicationContext.getSharedPreferences("milo_err", 0).edit().putBoolean(KEEP, on).apply()
        if (!on) File(c.applicationContext.filesDir, "diag.log").delete()
    }
    fun clear(c: Context) { synchronized(this) { items.clear() }; File(c.applicationContext.filesDir, "diag.log").delete() }

    @Synchronized fun add(tag: String, msg: String?) {
        val line = fmt.format(Date()) + "  [" + scrub(tag) + "] " + scrub(msg ?: "").take(600)
        items.addLast(line)
        while (items.size > 40) items.removeFirst()
        if (keepHistory()) try {
            val f = File(app!!.filesDir, "diag.log")
            if (f.length() > 64_000) f.writeText(f.readLines().takeLast(100).joinToString("\n") + "\n")
            f.appendText(line + "\n")
        } catch (_: Throwable) {}
    }

    /** A failed call to the server: the kind of call and the status only, never the body, ids or the person's details. */
    fun api(method: String, path: String, status: Int) = add("api", "$method ${path.substringBefore('?').replace(Regex("[0-9]+"), "#")} -> ${if (status == 0) "no connection" else status}")

    /** How long something took, so slow spots show up in a report. */
    inline fun <T> timed(what: String, block: () -> T): T {
        val t0 = System.currentTimeMillis()
        try { return block() } finally { add("perf", "$what took ${System.currentTimeMillis() - t0} ms") }
    }

    @Synchronized fun text(c: Context): String {
        val v = try { c.packageManager.getPackageInfo(c.packageName, 0).versionName } catch (_: Exception) { "?" }
        return buildString {
            append("milo. error report\n")
            append("App ").append(v).append(" · Android ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(") · ")
            append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n')
            append("-----\n")
            val old = try { if (keepHistory()) File(c.applicationContext.filesDir, "diag.log").readLines().takeLast(60) else emptyList() } catch (_: Throwable) { emptyList() }
            if (old.isNotEmpty()) { append("Kept history (this phone only):\n"); old.forEach { append(it).append('\n') }; append("-----\nThis session:\n") }
            if (items.isEmpty()) append("Nothing has gone wrong since the app opened.\n") else items.forEach { append(it).append('\n') }
        }
    }

    fun share(c: Context) {
        val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, "milo. error report"); putExtra(Intent.EXTRA_TEXT, text(c)) }
        c.startActivity(Intent.createChooser(send, "Send error report").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
