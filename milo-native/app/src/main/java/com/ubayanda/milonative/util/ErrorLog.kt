package com.ubayanda.milonative.util

import android.content.Context
import android.content.Intent
import android.os.Build
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
        // a crash from last time, saved by the handler below
        val p = app.getSharedPreferences("milo_err", 0)
        p.getString("crash", null)?.let { add("previous crash", it); p.edit().remove("crash").apply() }
        val old = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try { p.edit().putString("crash", (e.javaClass.name + ": " + e.message + "\n" + e.stackTrace.take(12).joinToString("\n")).take(3000)).commit() } catch (_: Throwable) {}
            old?.uncaughtException(t, e)
        }
    }

    @Synchronized fun add(tag: String, msg: String?) {
        items.addLast(fmt.format(Date()) + "  [" + tag + "] " + (msg ?: "").take(600))
        while (items.size > 40) items.removeFirst()
    }

    @Synchronized fun text(c: Context): String {
        val v = try { c.packageManager.getPackageInfo(c.packageName, 0).versionName } catch (_: Exception) { "?" }
        return buildString {
            append("milo. error report\n")
            append("App ").append(v).append(" · Android ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(") · ")
            append(Build.MANUFACTURER).append(' ').append(Build.MODEL).append('\n')
            append("-----\n")
            if (items.isEmpty()) append("Nothing has gone wrong since the app opened.\n") else items.forEach { append(it).append('\n') }
        }
    }

    fun share(c: Context) {
        val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_SUBJECT, "milo. error report"); putExtra(Intent.EXTRA_TEXT, text(c)) }
        c.startActivity(Intent.createChooser(send, "Send error report").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
