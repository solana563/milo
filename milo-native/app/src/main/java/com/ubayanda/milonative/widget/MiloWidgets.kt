package com.ubayanda.milonative.widget

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.appwidget.SizeMode
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.ubayanda.milonative.MainActivity
import com.ubayanda.milonative.R
import com.ubayanda.milonative.player.PlaybackService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

private val CARD = Color(0xFF141210)
private val CREAM = Color(0xFFF4EFE4)
private val TEXT = Color(0xFFF3EDE0)
private val DIM = Color(0xFFA8A194)
private val AMBER = Color(0xFFF99C00)
private val CMD = ActionParameters.Key<String>("cmd")

/** Set by the app screen's brain while it is alive, so a widget heart goes through the same code as the in-app heart. */
object FavBridge { @Volatile var toggle: ((Long) -> Unit)? = null }

private fun cmd(c: String) = actionRunCallback<MediaAction>(actionParametersOf(CMD to c))

const val RESUME_ACTION = "com.ubayanda.milonative.RESUME"

/** Tap a widget button: connects to the player and sends the command, even if the app itself is closed. */
class MediaAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val c = parameters[CMD] ?: return
        withContext(Dispatchers.Main) {
            var ctrl: MediaController? = null
            try {
                val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
                val ctl = withTimeout(6_000) {
                    suspendCancellableCoroutine<MediaController> { k ->
                        val f = MediaController.Builder(context, token).buildAsync()
                        f.addListener({ try { k.resume(f.get()) } catch (e: Exception) { k.cancel(e) } }, ContextCompat.getMainExecutor(context))
                    }
                }
                ctrl = ctl
                if (ctl.mediaItemCount == 0 && (c == "toggle" || c == "next" || c == "prev")) {
                    // nothing queued in the player: open milo and let it bring the last queue back
                    com.ubayanda.milonative.util.ErrorLog.add("widget", "$c with an empty player: opening milo")
                    context.startActivity(android.content.Intent(context, MainActivity::class.java).setAction(RESUME_ACTION).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                    return@withContext
                }
                when (c) {
                    "toggle" -> if (ctl.isPlaying) ctl.pause() else ctl.play()
                    "next" -> ctl.seekToNextMediaItem()
                    "prev" -> ctl.seekToPreviousMediaItem()
                    "shuffle" -> { ctl.shuffleModeEnabled = !ctl.shuffleModeEnabled; WidgetUpdater.push(context, ctl) }
                    "like" -> {
                        val id = ctl.currentMediaItem?.mediaId?.toLongOrNull()
                        val bridge = FavBridge.toggle
                        if (id != null && bridge != null) bridge(id)
                        else if (id != null && id > 0) {
                            // the app isn't open: edit the saved list directly (songs from the phone only)
                            val p = context.getSharedPreferences("milo", Context.MODE_PRIVATE)
                            val set = (p.getStringSet("favs", emptySet()) ?: emptySet()).toMutableSet()
                            if (!set.remove(id.toString())) set.add(id.toString())
                            p.edit().putStringSet("favs", set).apply()
                            WidgetUpdater.pokeLiked(context)
                        } else {
                            context.startActivity(android.content.Intent(context, MainActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                        }
                    }
                }
                delay(1500)
            } catch (e: Throwable) {
                // used to vanish silently: now it shows up in Profile > Diagnostics
                com.ubayanda.milonative.util.ErrorLog.add("widget", "$c failed: ${e.javaClass.simpleName}: ${e.message}")
            } finally {
                try { ctrl?.release() } catch (_: Throwable) {}
            }
        }
    }
}

@Composable
private fun Cover(s: WidgetState, size: Dp, radius: Dp) {
    val art = s.art
    if (art != null) {
        Image(ImageProvider(art), null, GlanceModifier.size(size).cornerRadius(radius), contentScale = ContentScale.Crop)
    } else {
        Box(GlanceModifier.size(size).cornerRadius(radius).background(Color(0xFF2A2623)), contentAlignment = Alignment.Center) {
            Image(ImageProvider(R.drawable.ic_w_play), null, GlanceModifier.size(size / 2), colorFilter = ColorFilter.tint(ColorProvider(DIM)))
        }
    }
}

private fun openApp(ctx: Context, resume: Boolean = false): Action =
    actionStartActivity(android.content.Intent(ctx, MainActivity::class.java).apply { if (resume) action = RESUME_ACTION })

/** What a button does: talk to the running player, or (player not running) open milo and bring the last queue back. */
private fun act(ctx: Context, s: WidgetState, command: String): Action =
    if (s.alive) cmd(command) else openApp(ctx, resume = command == "toggle" || command == "next" || command == "prev")

@Composable
private fun Btn(ctx: Context, s: WidgetState, icon: Int, desc: String, command: String, size: Dp, tint: Color = TEXT, bg: Color? = null) {
    var m = GlanceModifier.size(size).clickable(act(ctx, s, command))
    if (bg != null) m = m.background(bg).cornerRadius(size / 2)
    Box(m, contentAlignment = Alignment.Center) {
        Image(ImageProvider(icon), desc, GlanceModifier.size(size * 0.5f), colorFilter = ColorFilter.tint(ColorProvider(tint)))
    }
}

private fun ttl(s: WidgetState) = if (s.empty) "milo." else s.title
private fun sub(s: WidgetState) = when {
    s.empty -> "Tap to open"
    !s.alive -> "Tap play to continue"
    else -> s.artist
}

/** A row of controls. [level] 1 = play only, 2 = play + next, 3 = prev/play/next, 4 = everything. */
@Composable
private fun Controls(ctx: Context, s: WidgetState, level: Int, play: Dp, side: Dp, small: Dp) {
    val gap = 6.dp
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (level >= 4) { Btn(ctx, s, R.drawable.ic_w_shuffle, "Shuffle", "shuffle", small, if (s.shuffle) s.accent else DIM); Spacer(GlanceModifier.width(gap)) }
        if (level >= 3) { Btn(ctx, s, R.drawable.ic_w_prev, "Previous", "prev", side); Spacer(GlanceModifier.width(gap)) }
        Btn(ctx, s, if (s.playing) R.drawable.ic_w_pause else R.drawable.ic_w_play, if (s.playing) "Pause" else "Play", "toggle", play, Color.Black, s.accent)
        if (level >= 2) { Spacer(GlanceModifier.width(gap)); Btn(ctx, s, R.drawable.ic_w_next, "Next", "next", side) }
        if (level >= 4) { Spacer(GlanceModifier.width(gap)); Btn(ctx, s, if (s.liked) R.drawable.ic_w_heart_fill else R.drawable.ic_w_heart, if (s.liked) "Remove from liked songs" else "Add to liked songs", "like", small, if (s.liked) s.accent else DIM) }
    }
}

// ---- player: 2x1 up to 5x3, the layout follows the size the person gave it ----
class PlayerWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(110.dp, 56.dp), DpSize(110.dp, 110.dp), DpSize(180.dp, 56.dp), DpSize(250.dp, 56.dp), DpSize(250.dp, 110.dp), DpSize(250.dp, 180.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val s = WidgetState.read(context)
        provideContent {
            val sz = LocalSize.current
            val wide = sz.width >= 180.dp
            val tall = sz.height >= 100.dp
            val roomy = sz.height >= 170.dp
            val root = GlanceModifier.fillMaxSize().background(ImageProvider(R.drawable.widget_bg)).clickable(openApp(context))
            when {
                // tiny: cover and one button
                !wide && !tall -> Row(root.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalAlignment = Alignment.CenterHorizontally) {
                    Cover(s, 40.dp, 10.dp)
                    Spacer(GlanceModifier.width(8.dp))
                    Controls(context, s, 1, 40.dp, 30.dp, 26.dp)
                }
                // narrow and tall: cover, title, play + next
                !wide -> Column(root.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalAlignment = Alignment.CenterHorizontally) {
                    Cover(s, if (roomy) 84.dp else 52.dp, 12.dp)
                    Spacer(GlanceModifier.height(4.dp))
                    Text(ttl(s), maxLines = 1, style = TextStyle(color = ColorProvider(TEXT), fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    Spacer(GlanceModifier.height(4.dp))
                    Controls(context, s, 2, 34.dp, 28.dp, 24.dp)
                }
                // wide and short: one slim row
                !tall -> Row(root.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Cover(s, 40.dp, 10.dp)
                    Spacer(GlanceModifier.width(10.dp))
                    Column(GlanceModifier.defaultWeight()) {
                        Text(ttl(s), maxLines = 1, style = TextStyle(color = ColorProvider(TEXT), fontSize = 14.sp, fontWeight = FontWeight.Bold))
                        Text(sub(s), maxLines = 1, style = TextStyle(color = ColorProvider(DIM), fontSize = 12.sp))
                    }
                    Controls(context, s, if (sz.width >= 250.dp) 3 else 2, 40.dp, 34.dp, 28.dp)
                }
                // wide and tall: the full player
                else -> Column(root.padding(if (roomy) 18.dp else 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Cover(s, if (roomy) 84.dp else 60.dp, 14.dp)
                        Spacer(GlanceModifier.width(12.dp))
                        Column(GlanceModifier.defaultWeight()) {
                            Text(ttl(s), maxLines = if (roomy) 2 else 1, style = TextStyle(color = ColorProvider(TEXT), fontSize = if (roomy) 18.sp else 16.sp, fontWeight = FontWeight.Bold))
                            Text(sub(s), maxLines = 1, style = TextStyle(color = ColorProvider(DIM), fontSize = 13.sp))
                        }
                    }
                    Spacer(GlanceModifier.height(if (roomy) 14.dp else 10.dp))
                    LinearProgressIndicator(s.progress, GlanceModifier.fillMaxWidth().height(4.dp), color = ColorProvider(s.accent), backgroundColor = ColorProvider(Color(0xFF3A3632)))
                    Spacer(GlanceModifier.height(if (roomy) 12.dp else 8.dp))
                    Row(GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Controls(context, s, 4, if (roomy) 54.dp else 48.dp, 40.dp, 32.dp)
                    }
                }
            }
        }
    }
}
class PlayerWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = PlayerWidget() }

// ---- slim bar: 4x1, shrinks to cover + play when narrow ----
class PillWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(110.dp, 56.dp), DpSize(200.dp, 56.dp), DpSize(250.dp, 56.dp), DpSize(250.dp, 110.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val s = WidgetState.read(context)
        provideContent {
            val sz = LocalSize.current
            val level = if (sz.width >= 250.dp) 3 else if (sz.width >= 200.dp) 2 else 1
            val tall = sz.height >= 100.dp
            Row(
                GlanceModifier.fillMaxSize().background(ImageProvider(R.drawable.widget_bg_pill)).padding(horizontal = 10.dp, vertical = 6.dp).clickable(openApp(context)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Cover(s, if (tall) 64.dp else 44.dp, if (tall) 16.dp else 22.dp)
                Spacer(GlanceModifier.width(10.dp))
                if (level >= 2) {
                    Column(GlanceModifier.defaultWeight()) {
                        Text(ttl(s), maxLines = 1, style = TextStyle(color = ColorProvider(TEXT), fontSize = 14.sp, fontWeight = FontWeight.Bold))
                        Text(sub(s), maxLines = 1, style = TextStyle(color = ColorProvider(DIM), fontSize = 12.sp))
                    }
                } else Spacer(GlanceModifier.defaultWeight())
                Controls(context, s, level, 42.dp, 36.dp, 28.dp)
            }
        }
    }
}
class PillWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = PillWidget() }

// ---- cover: 2x2 and bigger ----
class ArtWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(110.dp, 56.dp), DpSize(110.dp, 110.dp), DpSize(180.dp, 110.dp), DpSize(250.dp, 180.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val s = WidgetState.read(context)
        provideContent {
            val sz = LocalSize.current
            val wide = sz.width >= 150.dp
            val big = sz.height >= 160.dp
            Box(GlanceModifier.fillMaxSize().cornerRadius(24.dp).background(Color(0xFF2A2623)).clickable(openApp(context))) {
                val art = s.art
                if (art != null) Image(ImageProvider(art), null, GlanceModifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Box(GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Image(ImageProvider(R.drawable.ic_w_play), null, GlanceModifier.size(48.dp), colorFilter = ColorFilter.tint(ColorProvider(DIM)))
                }
                Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.Bottom) {
                    Row(
                        GlanceModifier.fillMaxWidth().background(ImageProvider(R.drawable.widget_scrim)).padding(start = 12.dp, end = 8.dp, top = if (big) 36.dp else 22.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (sz.height >= 80.dp) Column(GlanceModifier.defaultWeight()) {
                            Text(ttl(s), maxLines = 1, style = TextStyle(color = ColorProvider(Color.White), fontSize = if (big) 15.sp else 13.sp, fontWeight = FontWeight.Bold))
                            Text(sub(s), maxLines = 1, style = TextStyle(color = ColorProvider(Color(0xFFD6D0C4)), fontSize = 11.sp))
                        } else Spacer(GlanceModifier.defaultWeight())
                        Controls(context, s, if (wide) 2 else 1, 40.dp, 36.dp, 28.dp)
                    }
                }
            }
        }
    }
}
class ArtWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = ArtWidget() }
