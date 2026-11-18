package com.ubayanda.milonative.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView
import com.ubayanda.milonative.model.LyricLine
import com.ubayanda.milonative.model.Lyrics
import com.ubayanda.milonative.model.PlayerUi
import com.ubayanda.milonative.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Floating lyrics: a small pill over other apps showing the line being sung, while milo is not on screen.
 * Drag it up or down to place it; tap it to open milo. Needs "Display over other apps". Off by default.
 * Idea from Mochy Music's floating pill (GPL-3.0, github.com/realidkroo/Mochy-Music, floating/); this is a much smaller version
 * written for milo (one line of text, no controls). Everything runs on the main thread, fed by the view-model's playback updates.
 */
object FloatingLyrics {
    private var view: TextView? = null
    private var lp: WindowManager.LayoutParams? = null
    private var shownText = ""
    private var forId: Long? = null
    private var lines: List<LyricLine> = emptyList()
    private var started = 0
    private var hooked = false

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("milo_fx", Context.MODE_PRIVATE)
    fun enabled(ctx: Context) = prefs(ctx).getBoolean("floatly", false)
    fun canDraw(ctx: Context) = Settings.canDrawOverlays(ctx)
    fun set(ctx: Context, on: Boolean) { prefs(ctx).edit().putBoolean("floatly", on).apply(); if (!on) hide() }

    /** sends the person to the system page where "Display over other apps" is granted */
    fun askPermission(ctx: Context) {
        ctx.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + ctx.packageName)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun hook(app: Application) {
        if (hooked) return
        hooked = true
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(a: Activity) { started++; hide() }
            override fun onActivityStopped(a: Activity) { started = (started - 1).coerceAtLeast(0) }
            override fun onActivityCreated(a: Activity, b: Bundle?) {}
            override fun onActivityResumed(a: Activity) {}
            override fun onActivityPaused(a: Activity) {}
            override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}
            override fun onActivityDestroyed(a: Activity) {}
        })
    }

    /** called with every playback update; [loader] finds the lyrics of a song (off the main thread inside) */
    fun onState(app: Application, scope: CoroutineScope, u: PlayerUi, track: Track?, loader: suspend (Track) -> Lyrics?) {
        hook(app)
        try {
            if (!enabled(app) || !canDraw(app) || track == null || started > 0) { hide(); return }
            if (forId != track.id) {
                forId = track.id; lines = emptyList(); setText("")
                scope.launch { val l = loader(track); if (forId == track.id) lines = l?.synced.orEmpty() }
            }
            if (!u.isPlaying || lines.isEmpty()) { setText(""); return }
            val off = prefs(app).getLong("lyoff_" + track.id, 0L)
            val pos = u.positionMs - off
            val text = lines.lastOrNull { it.timeMs <= pos + 250 }?.text?.trim().orEmpty()
            setText(text)
        } catch (_: Throwable) { hide() }
    }

    private fun setText(t: String) {
        if (t == shownText) return
        shownText = t
        if (t.isEmpty()) { view?.visibility = android.view.View.GONE; return }
        val ctx = appCtx ?: return
        val v = view ?: make(ctx) ?: return
        v.text = t
        v.visibility = android.view.View.VISIBLE
    }

    private var appCtx: Context? = null

    private fun make(ctx: Context): TextView? {
        val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = ctx.resources.displayMetrics
        val tv = TextView(ctx).apply {
            setTextColor(0xFFFFFFFF.toInt())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER
            maxWidth = (dm.widthPixels * 0.88f).toInt()
            maxLines = 2
            val pad = (14 * dm.density).toInt()
            setPadding(pad * 3 / 2, pad * 2 / 3, pad * 3 / 2, pad * 2 / 3)
            background = GradientDrawable().apply { setColor(0xCC101010.toInt()); cornerRadius = 28f * dm.density }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = prefs(ctx).getInt("floatly_y", (dm.heightPixels * 0.12f).toInt())
        }
        var downY = 0f; var startY = 0; var moved = false
        tv.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { downY = e.rawY; startY = params.y; moved = false; true }
                MotionEvent.ACTION_MOVE -> {
                    val d = e.rawY - downY
                    if (Math.abs(d) > 12 * dm.density) moved = true
                    if (moved) { params.y = (startY + d).toInt().coerceIn(0, dm.heightPixels - 120); try { wm.updateViewLayout(tv, params) } catch (_: Throwable) {} }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (moved) prefs(ctx).edit().putInt("floatly_y", params.y).apply()
                    else ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.let { ctx.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    true
                }
                else -> false
            }
        }
        return try { wm.addView(tv, params); view = tv; lp = params; tv } catch (_: Throwable) { null }
    }

    fun init(ctx: Context) { appCtx = ctx.applicationContext }

    fun hide() {
        val v = view ?: return
        try { (v.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(v) } catch (_: Throwable) {}
        view = null; lp = null; shownText = ""
    }
}
