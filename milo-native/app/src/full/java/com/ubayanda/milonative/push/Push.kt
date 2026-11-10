package com.ubayanda.milonative.push

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.ubayanda.milonative.data.CloudApi
import com.ubayanda.milonative.util.ErrorLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Instant notifications for new messages and friend requests, through Firebase Cloud Messaging.
 * The server side is the existing `milo-push` function (see PUSH.md): this phone only tells it where to send.
 * Switched on only when google-services.json was added at build time; otherwise [available] is false and the app keeps checking every 15 minutes.
 */
object Push {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private const val CHANNEL = "milo_messages"
    private fun prefs(c: Context) = c.applicationContext.getSharedPreferences("milo_push", Context.MODE_PRIVATE)

    private fun ready(c: Context): Boolean = try { FirebaseApp.getApps(c.applicationContext).isNotEmpty() } catch (_: Throwable) { false }

    /** True when this build was made with a Firebase config. */
    fun available(c: Context): Boolean = ready(c)

    fun enabled(c: Context): Boolean = prefs(c).getBoolean("on", true)
    fun setEnabled(c: Context, on: Boolean) {
        prefs(c).edit().putBoolean("on", on).apply()
        if (on) sync(c) else unregister(c)
    }

    /** Push is working for this phone: set up, signed in, and the server has our address. The 15-minute check then stays quiet to avoid doubles. */
    fun active(c: Context): Boolean = ready(c) && enabled(c) && prefs(c).getString("token", "").orEmpty().isNotEmpty()

    fun init(c: Context) {
        if (!ready(c)) return
        if (Build.VERSION.SDK_INT >= 26) {
            (c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(NotificationChannel(CHANNEL, "Messages", NotificationManager.IMPORTANCE_DEFAULT))
        }
        sync(c)
    }

    /** Make sure the server has this phone's current address for the signed-in account. Cheap to call often. */
    fun sync(c: Context) {
        if (!ready(c) || !enabled(c)) return
        val app = c.applicationContext
        try {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token -> if (token != null) save(app, token) }
                .addOnFailureListener { ErrorLog.add("push", "token: " + it.javaClass.simpleName) }
        } catch (_: Throwable) {}
    }

    fun save(c: Context, token: String) {
        val app = c.applicationContext
        scope.launch {
            try {
                val cloud = CloudApi(app)
                val uid = cloud.session?.userId ?: return@launch
                val p = prefs(app)
                if (p.getString("token", "") == token && p.getString("uid", "") == uid) return@launch
                cloud.request("/rest/v1/rpc/push_save", "POST", JSONObject().put("k", "fcm").put("e", token).put("x", JSONObject()))
                p.edit().putString("token", token).putString("uid", uid).apply()
            } catch (e: Exception) { ErrorLog.add("push", "register: " + e.javaClass.simpleName) }
        }
    }

    /** Stop sending to this phone (switched off). */
    fun unregister(c: Context) { scope.launch { unregisterNow(c) } }

    /** Same, but waits for the server, so signing out can do it while the login still works. */
    suspend fun unregisterNow(c: Context) {
        val app = c.applicationContext
        val p = prefs(app)
        val token = p.getString("token", "").orEmpty()
        p.edit().remove("token").remove("uid").apply()
        if (token.isEmpty()) return
        withContext(Dispatchers.IO) {
            try { CloudApi(app).request("/rest/v1/rpc/push_drop", "POST", JSONObject().put("e", token)) } catch (_: Exception) {}
        }
    }
}
