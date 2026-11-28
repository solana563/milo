package com.ubayanda.milonative.ui

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Outline
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.ubayanda.milonative.data.LyricsTranslator
import com.ubayanda.milonative.model.LyricLine
import com.ubayanda.milonative.model.Lyrics
import com.ubayanda.milonative.model.PlayerUi
import com.ubayanda.milonative.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Floating lyrics: a card over other apps that shows the line being sung, with the song's cover, title and artist, while milo is not on screen.
 * You can drag it up and down, change its opacity and text size, show a translation under each line, hide the cover and title row,
 * and lock it so touches go straight through to the app underneath (a small lock button stays on screen to unlock it).
 * Tap the card to open milo. Needs "Display over other apps". Off by default.
 * Idea from Mochy Music's floating pill (GPL-3.0, github.com/realidkroo/Mochy-Music, floating/); written for milo with plain Android views.
 * Everything runs on the main thread, fed by the view-model's playback updates.
 */
object FloatingLyrics {
    private var card: LinearLayout? = null
    private var handle: TextView? = null
    private var params: WindowManager.LayoutParams? = null
    private var bg: GradientDrawable? = null
    private var metaRow: LinearLayout? = null
    private var artView: ImageView? = null
    private var titleView: TextView? = null
    private var artistView: TextView? = null
    private var lyricView: TextView? = null
    private var transView: TextView? = null
    private var lockBtn: TextView? = null

    private var forId: Long? = null
    private var lines: List<LyricLine> = emptyList()
    private var trans: List<String> = emptyList()
    private var transKey = ""
    private var shownLyric = ""
    private var shownTrans = ""
    private var started = 0
    private var hooked = false
    private var appCtx: Context? = null
    private var lastTrack: Track? = null
    private var lastScope: CoroutineScope? = null

    private fun prefs(ctx: Context) = ctx.getSharedPreferences("milo_fx", Context.MODE_PRIVATE)
    fun enabled(ctx: Context) = prefs(ctx).getBoolean("floatly", false)
    fun canDraw(ctx: Context) = Settings.canDrawOverlays(ctx)
    fun set(ctx: Context, on: Boolean) { prefs(ctx).edit().putBoolean("floatly", on).apply(); if (!on) hide() }

    // ---- the settings the card follows (the settings sheet writes these) ----
    fun opacity(ctx: Context) = prefs(ctx).getInt("floatly_op", 80).coerceIn(20, 100)
    fun textSize(ctx: Context) = prefs(ctx).getInt("floatly_sz", 18).coerceIn(12, 32)
    fun showMeta(ctx: Context) = prefs(ctx).getBoolean("floatly_meta", true)
    fun lang(ctx: Context) = prefs(ctx).getString("floatly_lang", "").orEmpty()
    fun locked(ctx: Context) = prefs(ctx).getBoolean("floatly_lock", false)
    fun setOpacity(ctx: Context, v: Int) { prefs(ctx).edit().putInt("floatly_op", v.coerceIn(20, 100)).apply(); restyle() }
    fun setTextSize(ctx: Context, v: Int) { prefs(ctx).edit().putInt("floatly_sz", v.coerceIn(12, 32)).apply(); restyle() }
    fun setShowMeta(ctx: Context, on: Boolean) { prefs(ctx).edit().putBoolean("floatly_meta", on).apply(); restyle() }
    fun setLang(ctx: Context, code: String) { prefs(ctx).edit().putString("floatly_lang", code).apply(); transKey = ""; trans = emptyList(); restyle(); refreshTranslation() }
    fun setLocked(ctx: Context, on: Boolean) { prefs(ctx).edit().putBoolean("floatly_lock", on).apply(); restyle() }

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

    /** called with every playback update; [loader] finds the lyrics of a song and [artLoader] its cover (both off the main thread inside) */
    fun onState(app: Application, scope: CoroutineScope, u: PlayerUi, track: Track?, artLoader: suspend (Track) -> Bitmap?, loader: suspend (Track) -> Lyrics?) {
        hook(app)
        try {
            if (!enabled(app) || !canDraw(app) || track == null || started > 0) { hide(); return }
            lastScope = scope
            if (forId != track.id) {
                forId = track.id; lastTrack = track; lines = emptyList(); trans = emptyList(); transKey = ""; shownLyric = ""; shownTrans = ""
                artView?.setImageDrawable(null)
                scope.launch {
                    val l = loader(track)
                    if (forId == track.id) { lines = l?.synced.orEmpty(); refreshTranslation() }
                }
                scope.launch { val b = artLoader(track); if (forId == track.id) artView?.setImageBitmap(b) }
            }
            if (!u.isPlaying) { card?.visibility = View.GONE; handle?.visibility = View.GONE; return }
            val off = prefs(app).getLong("lyoff_" + track.id, 0L)
            val pos = u.positionMs - off
            val i = lines.indexOfLast { it.timeMs <= pos + 250 }
            val text = if (i >= 0) lines[i].text.trim() else ""
            val tr = if (i >= 0) trans.getOrNull(i)?.trim().orEmpty() else ""
            render(track, text, tr)
        } catch (swallowed: Throwable) { com.ubayanda.milonative.util.ErrorLog.swallowed(swallowed); hide() }
    }

    /** translates the song's lines into the chosen language, once per song and language */
    private fun refreshTranslation() {
        val ctx = appCtx ?: return
        val code = lang(ctx)
        val id = forId ?: return
        if (code.isBlank() || lines.isEmpty()) { trans = emptyList(); return }
        val key = "$id:$code"
        if (key == transKey) return
        transKey = key
        lastScope?.launch {
            val r = LyricsTranslator(ctx).translate(lines.map { it.text }, code)
            if (transKey == key) trans = r.orEmpty()
        }
    }

    private fun render(track: Track, lyric: String, translation: String) {
        val ctx = appCtx ?: return
        val meta = showMeta(ctx)
        // nothing to show: no lyric line yet and the cover/title row is off
        if (lyric.isEmpty() && !meta) { card?.visibility = View.GONE; handle?.visibility = View.GONE; return }
        val c = card ?: make(ctx) ?: return
        c.visibility = View.VISIBLE
        if (titleView?.text?.toString() != track.title) titleView?.text = track.title
        if (artistView?.text?.toString() != track.artist) artistView?.text = track.artist
        if (lyric != shownLyric) { shownLyric = lyric; lyricView?.text = lyric; lyricView?.visibility = if (lyric.isEmpty()) View.GONE else View.VISIBLE }
        val showTr = translation.isNotEmpty() && translation.trim().lowercase() != lyric.trim().lowercase()
        if (translation != shownTrans) { shownTrans = translation; transView?.text = if (showTr) translation else ""; transView?.visibility = if (showTr) View.VISIBLE else View.GONE }
        applyLock()
    }

    private fun dp(ctx: Context, v: Float) = (v * ctx.resources.displayMetrics.density).toInt()

    private fun make(ctx: Context): LinearLayout? {
        val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val dm = ctx.resources.displayMetrics
        fun text(sizeSp: Float, bold: Boolean, color: Long) = TextView(ctx).apply {
            setTextColor(color.toInt()); setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
            if (bold) typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        val art = ImageView(ctx).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            setBackgroundColor(0xFF2A2A2A.toInt())
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() { override fun getOutline(v: View, o: Outline) { o.setRoundRect(0, 0, v.width, v.height, 10f * dm.density) } }
        }
        val title = text(13f, true, 0xFFFFFFFF).apply { maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END }
        val artist = text(11f, false, 0xB3FFFFFF).apply { maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END }
        val names = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; addView(title); addView(artist) }
        val lock = text(15f, false, 0xFFFFFFFF).apply {
            text = "🔒"; gravity = Gravity.CENTER; setPadding(dp(ctx, 8f), dp(ctx, 4f), dp(ctx, 8f), dp(ctx, 4f))
            setOnClickListener { setLocked(ctx, true) }
        }
        val close = text(15f, false, 0xFFFFFFFF).apply {
            text = "✕"; gravity = Gravity.CENTER; setPadding(dp(ctx, 8f), dp(ctx, 4f), dp(ctx, 8f), dp(ctx, 4f))
            setOnClickListener { set(ctx, false) }
        }
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
            addView(art, LinearLayout.LayoutParams(dp(ctx, 44f), dp(ctx, 44f)))
            addView(names, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(ctx, 10f) })
            addView(lock); addView(close)
        }
        val lyric = text(textSize(ctx).toFloat(), true, 0xFFFFFFFF).apply { gravity = Gravity.CENTER; maxLines = 3; visibility = View.GONE }
        val tr = text(textSize(ctx) * 0.78f, false, 0xCCFFFFFF).apply { gravity = Gravity.CENTER; maxLines = 3; visibility = View.GONE }
        val fill = GradientDrawable().apply { cornerRadius = 22f * dm.density }
        val c = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val pad = dp(ctx, 12f)
            setPadding(pad, pad * 3 / 4, pad, pad)
            background = fill
            addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(lyric, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(ctx, 8f) })
            addView(tr, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(ctx, 4f) })
        }
        val p = WindowManager.LayoutParams(
            (dm.widthPixels * 0.92f).toInt(), WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            y = prefs(ctx).getInt("floatly_y", (dm.heightPixels * 0.12f).toInt())
        }
        var downY = 0f; var startY = 0; var moved = false
        c.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { downY = e.rawY; startY = p.y; moved = false; true }
                MotionEvent.ACTION_MOVE -> {
                    val d = e.rawY - downY
                    if (Math.abs(d) > 12 * dm.density) moved = true
                    if (moved) { p.y = (startY + d).toInt().coerceIn(0, dm.heightPixels - 160); try { wm.updateViewLayout(c, p) } catch (swallowed: Throwable) { com.ubayanda.milonative.util.ErrorLog.swallowed(swallowed) } }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (moved) prefs(ctx).edit().putInt("floatly_y", p.y).apply()
                    else ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)?.let { ctx.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    true
                }
                else -> false
            }
        }
        return try {
            wm.addView(c, p)
            card = c; params = p; bg = fill; metaRow = row; artView = art; titleView = title; artistView = artist; lyricView = lyric; transView = tr; lockBtn = lock
            restyle()
            c
        } catch (swallowed: Throwable) { com.ubayanda.milonative.util.ErrorLog.swallowed(swallowed); null }
    }

    /** applies the settings (opacity, text size, cover row, lock) to the card that is on screen */
    fun restyle() {
        val ctx = appCtx ?: return
        val c = card ?: return
        try {
            bg?.setColor((0xFF101010.toInt() and 0x00FFFFFF) or ((opacity(ctx) * 255 / 100) shl 24))
            lyricView?.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize(ctx).toFloat())
            transView?.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize(ctx) * 0.78f)
            metaRow?.visibility = if (showMeta(ctx)) View.VISIBLE else View.GONE
            applyLock()
            c.requestLayout()
        } catch (swallowed: Throwable) { com.ubayanda.milonative.util.ErrorLog.swallowed(swallowed) }
    }

    /** locked: touches pass straight through the card; a small unlock button stays on screen instead */
    private fun applyLock() {
        val ctx = appCtx ?: return
        val c = card ?: return
        val p = params ?: return
        val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val lock = locked(ctx)
        val base = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        val want = if (lock) base or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else base
        try {
            if (p.flags != want) { p.flags = want; wm.updateViewLayout(c, p) }
            lockBtn?.visibility = if (lock) View.GONE else View.VISIBLE
            val visible = c.visibility == View.VISIBLE
            if (lock && visible) {
                val h = handle ?: TextView(ctx).apply {
                    text = "🔓"; setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f); gravity = Gravity.CENTER
                    background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(0xCC101010.toInt()) }
                    setOnClickListener { setLocked(ctx, false) }
                }.also { v ->
                    val s = dp(ctx, 40f)
                    val hp = WindowManager.LayoutParams(s, s, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT)
                        .apply { gravity = Gravity.TOP or Gravity.END; x = dp(ctx, 8f); y = p.y }
                    wm.addView(v, hp); handle = v
                }
                h.visibility = View.VISIBLE
                (h.layoutParams as? WindowManager.LayoutParams)?.let { if (it.y != p.y) { it.y = p.y; wm.updateViewLayout(h, it) } }
            } else handle?.visibility = View.GONE
        } catch (swallowed: Throwable) { com.ubayanda.milonative.util.ErrorLog.swallowed(swallowed) }
    }

    fun init(ctx: Context) { appCtx = ctx.applicationContext }

    fun hide() {
        val wm = (card?.context ?: handle?.context)?.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        card?.let { try { wm?.removeView(it) } catch (swallowed: Throwable) { com.ubayanda.milonative.util.ErrorLog.swallowed(swallowed) } }
        handle?.let { try { wm?.removeView(it) } catch (swallowed: Throwable) { com.ubayanda.milonative.util.ErrorLog.swallowed(swallowed) } }
        card = null; handle = null; params = null; bg = null; metaRow = null; artView = null; titleView = null; artistView = null; lyricView = null; transView = null; lockBtn = null
        shownLyric = ""; shownTrans = ""
    }
}
