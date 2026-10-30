package com.ubayanda.milonative.ui.screens

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.ubayanda.milonative.model.Track
import com.ubayanda.milonative.ui.MainViewModel
import com.ubayanda.milonative.ui.components.Cover
import com.ubayanda.milonative.ui.theme.Milo

private fun page(id: String) = """<!doctype html><html><head><meta name="viewport" content="width=device-width,initial-scale=1">
<style>html,body{margin:0;height:100%;background:#000}#p{position:absolute;top:0;left:0;right:0;bottom:0}</style></head><body><div id="p"></div>
<script src="https://www.youtube.com/iframe_api"></script>
<script>var pl;function onYouTubeIframeAPIReady(){pl=new YT.Player('p',{width:'100%',height:'100%',videoId:'$id',playerVars:{playsinline:1,rel:0,autoplay:1,fs:0},events:{onStateChange:function(e){if(e.data===0)Android.ended()},onError:function(e){Android.failed(e.data)}}})}
function loadVid(i){if(pl&&pl.loadVideoById)pl.loadVideoById(i)}</script></body></html>"""

/** Plays YouTube results in YouTube's own embedded player, with next/previous and the result list underneath. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeScreen(vm: MainViewModel, items: List<Track>, start: Int, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var index by remember { mutableIntStateOf(start.coerceIn(0, items.lastIndex)) }
    val holder = remember { arrayOfNulls<WebView>(1) }
    val main = remember { Handler(Looper.getMainLooper()) }
    fun vid(i: Int) = items[i].uri.removePrefix("yt:")
    fun go(i: Int) {
        if (i !in items.indices) return
        index = i
        holder[0]?.evaluateJavascript("loadVid('${vid(i)}')", null)
    }
    LaunchedEffect(Unit) { if (vm.player.state.value.isPlaying) vm.player.toggle() } // the phone's own player steps aside
    BackHandler(onBack = onClose)
    DisposableEffect(Unit) { onDispose { holder[0]?.let { it.loadUrl("about:blank"); it.destroy() }; holder[0] = null } }

    Column(Modifier.fillMaxSize().background(Milo.bg).statusBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) { Icon(Icons.Rounded.ArrowBack, "Back", tint = Milo.text) }
            Text("YouTube", color = Milo.text, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }
        AndroidView(
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(androidx.compose.ui.graphics.Color.Black),
            factory = { c ->
                WebView(c).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    setBackgroundColor(0)
                    addJavascriptInterface(object {
                        @JavascriptInterface fun ended() { main.post { if (index + 1 in items.indices) go(index + 1) } }
                        @JavascriptInterface fun failed(code: Int) {
                            main.post {
                                Toast.makeText(ctx, "YouTube won't play this one here (the owner may block embedding). Skipping.", Toast.LENGTH_SHORT).show()
                                if (index + 1 in items.indices) go(index + 1)
                            }
                        }
                    }, "Android")
                    // the player needs a real web address as its home, or YouTube refuses to play
                    loadDataWithBaseURL("https://www.youtube.com", page(vid(index)), "text/html", "utf-8", null)
                    holder[0] = this
                }
            },
        )
        val t = items[index]
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text(t.title, color = Milo.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(t.artist, color = Milo.dim, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconButton(onClick = { go(index - 1) }, enabled = index > 0) { Icon(Icons.Rounded.SkipPrevious, "Previous", tint = if (index > 0) Milo.text else Milo.dim) }
            IconButton(onClick = { go(index + 1) }, enabled = index < items.lastIndex) { Icon(Icons.Rounded.SkipNext, "Next", tint = if (index < items.lastIndex) Milo.text else Milo.dim) }
        }
        Spacer(Modifier.height(4.dp))
        LazyColumn(Modifier.fillMaxSize().navigationBarsPadding()) {
            itemsIndexed(items, key = { _, it -> it.id }) { i, it ->
                val on = i == index
                Row(
                    Modifier.fillMaxWidth().clickable { go(i) }.padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Cover(vm, it, 52.dp, 12.dp, px = 160)
                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                        Text(it.title, color = if (on) Milo.amber else Milo.text, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(it.artist, color = Milo.dim, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
