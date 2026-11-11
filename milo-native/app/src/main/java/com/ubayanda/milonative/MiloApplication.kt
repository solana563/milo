package com.ubayanda.milonative

import android.app.Application
import java.io.File

/** Saves the stack trace of any crash so the next launch can show it. */
class MiloApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // a new process has no player service yet: whatever the widgets remember about one is out of date
        try { com.ubayanda.milonative.widget.WidgetState.setAlive(this, false) } catch (_: Throwable) {}
        try {
            androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "milo_chat_check", androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                androidx.work.PeriodicWorkRequestBuilder<com.ubayanda.milonative.data.ChatNotifyWorker>(15, java.util.concurrent.TimeUnit.MINUTES)
                    .setConstraints(androidx.work.Constraints.Builder().setRequiredNetworkType(androidx.work.NetworkType.CONNECTED).build()).build(),
            )
        } catch (_: Throwable) {}
        try { com.ubayanda.milonative.push.Push.init(this) } catch (_: Throwable) {}
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
