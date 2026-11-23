package com.ubayanda.milonative.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.ubayanda.milonative.ui.theme.Milo
import java.io.File

/** What sits behind every screen: plain black, the playing song's softened cover, the song's colour, or a picture the person chose. */
object AppBackground {
    const val BLACK = 0
    const val SONG_ART = 1
    const val SONG_COLOUR = 2
    const val CUSTOM = 3

    var mode by mutableIntStateOf(BLACK)
        private set
    /** Home's Liked and Saved tiles show a blurred cover (default on). */
    var tileArt by mutableStateOf(true)
        private set
    /** Changes whenever the custom picture is replaced, so the screen reloads it. */
    var rev by mutableIntStateOf(0)
        private set
    private var loaded = false

    private fun p(c: Context) = c.applicationContext.getSharedPreferences("milo_bg", Context.MODE_PRIVATE)
    fun file(c: Context) = File(c.applicationContext.filesDir, "bg-custom.jpg")
    fun hasCustom(c: Context) = file(c).exists()

    fun init(c: Context) {
        if (loaded) return
        loaded = true
        mode = p(c).getInt("mode", BLACK)
        tileArt = p(c).getBoolean("tileArt", true)
    }

    fun setTileArt(c: Context, on: Boolean) { tileArt = on; p(c).edit().putBoolean("tileArt", on).apply() }

    fun set(c: Context, m: Int) { mode = m; p(c).edit().putInt("mode", m).apply() }

    /** Saves the picture at [uri] (scaled to at most 1600 px on its long side) and switches to it. False if it can't be read as a picture. */
    fun setCustom(c: Context, uri: Uri): Boolean {
        return try {
            val bytes = c.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return false
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, o)
            if (o.outWidth <= 0 || o.outHeight <= 0) return false
            var sample = 1
            while (o.outWidth / (sample * 2) >= 1600 || o.outHeight / (sample * 2) >= 1600) sample *= 2
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return false
            file(c).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            rev++
            set(c, CUSTOM)
            true
        } catch (_: Exception) { false }
    }
}

/** Draws the chosen background behind a screen. [soft] is the playing song's small, already blurred cover. */
@Composable
fun BackgroundLayer(soft: Bitmap?) {
    val ctx = LocalContext.current
    androidx.compose.runtime.LaunchedEffect(Unit) { AppBackground.init(ctx) }
    val mode = AppBackground.mode
    when (mode) {
        AppBackground.SONG_ART -> if (soft != null) {
            val img = remember(soft) { soft.asImageBitmap() }
            Box(Modifier.fillMaxSize()) {
                Image(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.6f)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.78f)))))
            }
        }
        AppBackground.SONG_COLOUR -> {
            val glow: Color = remember(soft) { soft?.let { glowOf(it) } } ?: Milo.amber
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(lerp(Color.Black, glow, 0.42f), lerp(Color.Black, glow, 0.14f), Color.Black))))
        }
        AppBackground.CUSTOM -> {
            val rev = AppBackground.rev
            val bmp by produceState<Bitmap?>(null, rev) {
                value = try { BitmapFactory.decodeFile(AppBackground.file(ctx).path) } catch (_: Exception) { null }
            }
            val b = bmp
            if (b != null) {
                val img = remember(b) { b.asImageBitmap() }
                Box(Modifier.fillMaxSize()) {
                    Image(img, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))
                }
            }
        }
    }
}
