package com.ubayanda.milonative.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.ubayanda.milonative.data.ChatMedia
import com.ubayanda.milonative.ui.theme.Milo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val avatarCache = android.util.LruCache<String, Bitmap>(48)
private val palette = listOf(
    0xFFF99C00 to 0xFFB36F00, 0xFF4F8EF7 to 0xFF2B5BB8, 0xFFE0568A to 0xFF9C2B5C, 0xFF3DBE8B to 0xFF1F7F5A,
    0xFF9B6BE8 to 0xFF5E3AA8, 0xFFE8704A to 0xFFA8401F,
)

/** A round profile picture: their photo when they have one, otherwise a coloured circle with their initial. [ring] draws the amber "listening now" ring. */
@Composable
fun UserAvatar(name: String, photo: String, size: Dp, modifier: Modifier = Modifier, ring: Boolean = false) {
    val bmp by produceState<Bitmap?>(null, photo) {
        value = if (photo.isEmpty()) null else {
            val key = photo.length.toString() + ":" + photo.hashCode()
            avatarCache.get(key) ?: withContext(Dispatchers.Default) { ChatMedia.photoBitmap(photo) }?.also { avatarCache.put(key, it) }
        }
    }
    val (c1, c2) = palette[(name.hashCode() and 0x7fffffff) % palette.size]
    val inner = if (ring) size - 7.dp else size
    Box(
        modifier.size(size).then(if (ring) Modifier.border(2.5.dp, Milo.amber, CircleShape).padding(3.dp) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        val b = bmp
        if (b != null) {
            Image(remember(b) { b.asImageBitmap() }, null, Modifier.size(inner).clip(CircleShape), contentScale = ContentScale.Crop)
        } else {
            Box(Modifier.size(inner).clip(CircleShape).background(Brush.verticalGradient(listOf(Color(c1), Color(c2)))), contentAlignment = Alignment.Center) {
                Text((name.firstOrNull() ?: '?').uppercaseChar().toString(), color = Color.White, fontSize = (inner.value * 0.42f).sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
