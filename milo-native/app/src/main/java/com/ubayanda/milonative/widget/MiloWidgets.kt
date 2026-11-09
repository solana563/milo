package com.ubayanda.milonative.widget

import android.content.ComponentName
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
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

/** Tap a widget button: connects to the player and sends the command, even if the app itself is closed. */
class MediaAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val c = parameters[CMD] ?: return
        withContext(Dispatchers.Main) {
            try {
                val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
                val ctrl = suspendCancellableCoroutine<MediaController> { k ->
                    val f = MediaController.Builder(context, token).buildAsync()
                    f.addListener({ try { k.resume(f.get()) } catch (e: Exception) { k.cancel(e) } }, ContextCompat.getMainExecutor(context))
                }
                when (c) {
                    "toggle" -> if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
                    "next" -> ctrl.seekToNextMediaItem()
                    "prev" -> ctrl.seekToPreviousMediaItem()
                    "shuffle" -> { ctrl.shuffleModeEnabled = !ctrl.shuffleModeEnabled; WidgetUpdater.push(context, ctrl) }
                    "like" -> {
                        val id = ctrl.currentMediaItem?.mediaId?.toLongOrNull()
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
                delay(600)
                ctrl.release()
            } catch (_: Throwable) {}
        }
    }
}

@Composable
private fun Cover(s: WidgetState, size: androidx.compose.ui.unit.Dp, radius: androidx.compose.ui.unit.Dp) {
    val art = s.art
    if (art != null) {
        Image(ImageProvider(art), null, GlanceModifier.size(size).cornerRadius(radius), contentScale = ContentScale.Crop)
    } else {
        Box(GlanceModifier.size(size).cornerRadius(radius).background(Color(0xFF2A2623)), contentAlignment = Alignment.Center) {
            Image(ImageProvider(R.drawable.ic_w_play), null, GlanceModifier.size(size / 2), colorFilter = ColorFilter.tint(ColorProvider(DIM)))
        }
    }
}

@Composable
private fun Btn(icon: Int, desc: String, command: String, size: androidx.compose.ui.unit.Dp, tint: Color = TEXT, bg: Color? = null, empty: Boolean = false, ctx: Context? = null) {
    // nothing queued yet: the buttons just open milo. so a tap is never dead
    val act = if (empty && ctx != null) actionStartActivity(android.content.Intent(ctx, MainActivity::class.java)) else cmd(command)
    var m = GlanceModifier.size(size).clickable(act)
    if (bg != null) m = m.background(bg).cornerRadius(size / 2)
    Box(m, contentAlignment = Alignment.Center) {
        Image(ImageProvider(icon), desc, GlanceModifier.size(size * 0.5f), colorFilter = ColorFilter.tint(ColorProvider(tint)))
    }
}

private fun ttl(s: WidgetState) = if (s.empty) "Nothing playing" else s.title
private fun sub(s: WidgetState) = if (s.empty) "Tap to open milo." else s.artist

// ---- 4 x 2 player ----
class PlayerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val s = WidgetState.read(context)
        val open = actionStartActivity(android.content.Intent(context, MainActivity::class.java))
        provideContent {
            Column(
                GlanceModifier.fillMaxSize().background(ImageProvider(R.drawable.widget_bg)).padding(14.dp).clickable(open),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Cover(s, 60.dp, 14.dp)
                    Spacer(GlanceModifier.width(12.dp))
                    Column(GlanceModifier.defaultWeight()) {
                        Text(ttl(s), maxLines = 1, style = TextStyle(color = ColorProvider(TEXT), fontSize = 16.sp, fontWeight = FontWeight.Bold))
                        Text(sub(s), maxLines = 1, style = TextStyle(color = ColorProvider(DIM), fontSize = 13.sp))
                    }
                }
                Spacer(GlanceModifier.height(10.dp))
                LinearProgressIndicator(s.progress, GlanceModifier.fillMaxWidth().height(4.dp), color = ColorProvider(s.accent), backgroundColor = ColorProvider(Color(0xFF3A3632)))
                Spacer(GlanceModifier.height(8.dp))
                Row(GlanceModifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalAlignment = Alignment.CenterVertically) {
                    Btn(R.drawable.ic_w_shuffle, "Shuffle", "shuffle", 32.dp, if (s.shuffle) s.accent else DIM, empty = s.empty, ctx = context)
                    Spacer(GlanceModifier.width(6.dp))
                    Btn(R.drawable.ic_w_prev, "Previous", "prev", 40.dp, empty = s.empty, ctx = context)
                    Spacer(GlanceModifier.width(6.dp))
                    Btn(if (s.playing) R.drawable.ic_w_pause else R.drawable.ic_w_play, "Play or pause", "toggle", 48.dp, Color.Black, s.accent, s.empty, context)
                    Spacer(GlanceModifier.width(6.dp))
                    Btn(R.drawable.ic_w_next, "Next", "next", 40.dp, empty = s.empty, ctx = context)
                    Spacer(GlanceModifier.width(6.dp))
                    Btn(if (s.liked) R.drawable.ic_w_heart_fill else R.drawable.ic_w_heart, if (s.liked) "Remove from liked songs" else "Add to liked songs", "like", 32.dp, if (s.liked) s.accent else DIM, empty = s.empty, ctx = context)
                }
            }
        }
    }
}
class PlayerWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = PlayerWidget() }

// ---- 4 x 1 slim bar ----
class PillWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val s = WidgetState.read(context)
        val open = actionStartActivity(android.content.Intent(context, MainActivity::class.java))
        provideContent {
            Row(
                GlanceModifier.fillMaxSize().background(ImageProvider(R.drawable.widget_bg_pill)).padding(horizontal = 10.dp, vertical = 6.dp).clickable(open),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Cover(s, 44.dp, 22.dp)
                Spacer(GlanceModifier.width(10.dp))
                Column(GlanceModifier.defaultWeight()) {
                    Text(ttl(s), maxLines = 1, style = TextStyle(color = ColorProvider(TEXT), fontSize = 14.sp, fontWeight = FontWeight.Bold))
                    Text(sub(s), maxLines = 1, style = TextStyle(color = ColorProvider(DIM), fontSize = 12.sp))
                }
                Btn(R.drawable.ic_w_prev, "Previous", "prev", 36.dp, empty = s.empty, ctx = context)
                Btn(if (s.playing) R.drawable.ic_w_pause else R.drawable.ic_w_play, "Play or pause", "toggle", 42.dp, Color.Black, s.accent, s.empty, context)
                Btn(R.drawable.ic_w_next, "Next", "next", 36.dp, empty = s.empty, ctx = context)
            }
        }
    }
}
class PillWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = PillWidget() }

// ---- 2 x 2 cover ----
class ArtWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val s = WidgetState.read(context)
        val open = actionStartActivity(android.content.Intent(context, MainActivity::class.java))
        provideContent {
            Box(GlanceModifier.fillMaxSize().cornerRadius(24.dp).background(Color(0xFF2A2623)).clickable(open)) {
                val art = s.art
                if (art != null) Image(ImageProvider(art), null, GlanceModifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Column(GlanceModifier.fillMaxSize(), verticalAlignment = Alignment.Bottom) {
                    Row(
                        GlanceModifier.fillMaxWidth().background(ImageProvider(R.drawable.widget_scrim)).padding(start = 12.dp, end = 8.dp, top = 22.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(GlanceModifier.defaultWeight()) {
                            Text(ttl(s), maxLines = 1, style = TextStyle(color = ColorProvider(Color.White), fontSize = 13.sp, fontWeight = FontWeight.Bold))
                            Text(sub(s), maxLines = 1, style = TextStyle(color = ColorProvider(Color(0xFFD6D0C4)), fontSize = 11.sp))
                        }
                        Btn(if (s.playing) R.drawable.ic_w_pause else R.drawable.ic_w_play, "Play or pause", "toggle", 40.dp, Color.Black, s.accent, s.empty, context)
                        Btn(R.drawable.ic_w_next, "Next", "next", 36.dp, Color.White, empty = s.empty, ctx = context)
                    }
                }
            }
        }
    }
}
class ArtWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = ArtWidget() }
