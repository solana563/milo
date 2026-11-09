package com.ubayanda.milonative.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ubayanda.milonative.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * Keeps the app's process alive while songs are being saved, so downloads carry on after milo. is closed or the screen is off.
 * It only runs while something is queued or downloading, shows a notification with a Pause button, and stops itself when the queue is empty.
 * The downloading itself is still done by [OfflineStore]; this service only keeps it running.
 */
class DownloadService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    companion object {
        private const val CH = "milo_downloads"
        private const val ID = 4107
        private const val PAUSE = "com.ubayanda.milonative.PAUSE_DOWNLOADS"
        private const val GIVE_UP_MS = 30 * 60_000L
        @Volatile private var running = false

        /** Notification wording: pure, so it can be tested. [pct] is 0..100 for songs downloading right now. */
        fun summary(left: Int, waiting: String, pct: Int): Pair<String, String> {
            val title = if (left == 1) "Saving 1 song" else "Saving $left songs"
            val text = if (waiting.isNotEmpty()) waiting else if (pct > 0) "$pct% of the current songs" else "Starting…"
            return title to text
        }

        fun start(ctx: Context) {
            if (running) return
            try {
                val i = Intent(ctx, DownloadService::class.java)
                if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(i) else ctx.startService(i)
                running = true
            } catch (_: Throwable) { /* started from the background where Android refuses: downloads still run while the app is alive */ }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        running = true
        if (intent?.action == PAUSE) OfflineStore.pauseAll()
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(NotificationChannel(CH, "Downloads", NotificationManager.IMPORTANCE_LOW))
        val first = build(OfflineStore.pending.value.coerceAtLeast(1), "", 0)
        if (Build.VERSION.SDK_INT >= 29) startForeground(ID, first, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) else startForeground(ID, first)
        scope.coroutineContext[kotlinx.coroutines.Job]?.children?.forEach { it.cancel() }
        scope.launch {
            var stuckSince = 0L
            combine(OfflineStore.pending, OfflineStore.waiting, OfflineStore.progress) { n, w, p -> Triple(n, w, p) }.collect { (n, w, p) ->
                if (n <= 0) { stop(); return@collect }
                val active = OfflineStore.jobs.value.filter { it.state == OfflineStore.State.ACTIVE }
                val pct = if (active.isEmpty()) 0 else (active.map { (p[it.id] ?: 0f).coerceIn(0f, 1f) }.average() * 100).toInt()
                nm.notify(ID, build(n, w, pct))
                // waiting for Wi-Fi with nothing moving for a long time: stop holding the phone awake; the queue stays (paused) in the app
                if (w.isNotEmpty() && active.isEmpty()) { if (stuckSince == 0L) stuckSince = System.currentTimeMillis(); if (System.currentTimeMillis() - stuckSince > GIVE_UP_MS) { OfflineStore.pauseAll(); stop() } } else stuckSince = 0L
                delay(800)
            }
        }
        return START_NOT_STICKY
    }

    private fun stop() {
        running = false
        try { stopForeground(STOP_FOREGROUND_REMOVE) } catch (_: Throwable) {}
        stopSelf()
    }

    private fun build(left: Int, waiting: String, pct: Int): Notification {
        val (title, text) = summary(left, waiting, pct)
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val pause = PendingIntent.getService(this, 1, Intent(this, DownloadService::class.java).setAction(PAUSE), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CH)
            .setSmallIcon(android.R.drawable.stat_sys_download).setContentTitle(title).setContentText(text)
            .setOngoing(true).setOnlyAlertOnce(true).setContentIntent(open)
            .setProgress(100, pct, pct == 0 && waiting.isEmpty())
            .addAction(0, "Pause all", pause).build()
    }

    override fun onDestroy() { running = false; scope.cancel(); super.onDestroy() }
}
