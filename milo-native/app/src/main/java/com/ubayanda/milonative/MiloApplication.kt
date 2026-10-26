package com.ubayanda.milonative

import android.app.Application
import java.io.File

/** Saves the stack trace of any crash so the next launch can show it. */
class MiloApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val old = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            try {
                File(filesDir, "crash.txt").writeText(
                    "milo native crash (${android.os.Build.MODEL}, Android ${android.os.Build.VERSION.SDK_INT})\n" +
                        "thread: ${t.name}\n\n" + android.util.Log.getStackTraceString(e)
                )
            } catch (_: Throwable) {}
            old?.uncaughtException(t, e)
        }
    }
}
