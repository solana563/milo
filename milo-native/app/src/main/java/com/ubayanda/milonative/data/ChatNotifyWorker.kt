package com.ubayanda.milonative.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ubayanda.milonative.MainActivity
import org.json.JSONArray
import java.net.URLEncoder
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Every ~15 minutes (Android's shortest repeat), looks for new messages and friend requests and posts a notification.
 * It is not instant push; that would need Firebase. Opening the app always shows messages live.
 */
class ChatNotifyWorker(private val ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        try {
            val cloud = CloudApi(ctx)
            val me = cloud.session?.userId ?: return Result.success()
            if (!cloud.fresh()) return Result.success()
            if (!NotificationManagerCompat.from(ctx).areNotificationsEnabled()) return Result.success()
            val prefs = ctx.getSharedPreferences("milo_notify", Context.MODE_PRIVATE)
            val since = URLEncoder.encode(Instant.now().minus(24, ChronoUnit.HOURS).toString(), "UTF-8")

            val msgs = JSONArray(cloud.request("/rest/v1/messages?select=id,sender,kind,pv&rcpt=eq.$me&read_at=is.null&created_at=gt.$since&order=id.desc&limit=30"))
            if (msgs.length() > 0) {
                val newest = msgs.getJSONObject(0).getLong("id")
                if (newest > prefs.getLong("last_msg", 0L)) {
                    val top = msgs.getJSONObject(0)
                    val text = if (msgs.length() == 1) when (top.optString("kind")) {
                        "text" -> top.optString("pv", "New message")
                        "img" -> "Sent you a photo"
                        "voice" -> "Sent you a voice message"
                        else -> "Shared a song with you"
                    } else "${msgs.length()} new messages"
                    post(1, "New message on milo.", text)
                    prefs.edit().putLong("last_msg", newest).apply()
                }
            }

            val reqs = JSONArray(cloud.request("/rest/v1/friendships?select=a&b=eq.$me&status=eq.pending"))
            if (reqs.length() > prefs.getInt("last_req", 0)) post(2, "Friend request", "Someone wants to be your friend on milo.")
            prefs.edit().putInt("last_req", reqs.length()).apply()
        } catch (e: Exception) {
            return Result.success() // a missed check is fine, the next one retries
        }
        return Result.success()
    }

    private fun post(id: Int, title: String, text: String) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(NotificationChannel("milo_chat", "Messages", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(ctx, "milo_chat")
            .setSmallIcon(android.R.drawable.stat_notify_chat).setContentTitle(title).setContentText(text)
            .setContentIntent(open).setAutoCancel(true).build()
        try { NotificationManagerCompat.from(ctx).notify(id, n) } catch (_: SecurityException) {}
    }
}
