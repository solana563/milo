package com.ubayanda.milonative.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.ubayanda.milonative.ui.components.LeavesBackground
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.R
import com.ubayanda.milonative.ui.theme.Milo

/** First screen, in the same look as the web milo. welcome: drifting leaves, the logo, then one clear button. */
@Composable
fun PermissionScreen(blockedForever: Boolean, onAllow: () -> Unit, onOpenSettings: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color(0xFF050505))) {
        LeavesBackground(Modifier.fillMaxSize())
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 28.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(painterResource(R.drawable.logo_wordmark), null, Modifier.height(54.dp))
            Spacer(Modifier.height(22.dp))
            Text(
                "Your music.\nYour phone.", color = Milo.text, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center, lineHeight = 42.sp,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "milo. reads your songs straight from your phone, SD card and USB drives. Nothing is uploaded or copied.",
                color = Milo.dim, fontSize = 16.sp, textAlign = TextAlign.Center,
            )
            if (blockedForever) {
                Spacer(Modifier.height(14.dp))
                Text(
                    "Android is no longer showing the permission box. Open Settings, tap Permissions, then allow Music and audio.",
                    color = Milo.dim, fontSize = 14.sp, textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(26.dp))
            Box(
                Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(27.dp)).background(Milo.amber)
                    .clickable { if (blockedForever) onOpenSettings() else onAllow() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (blockedForever) "OPEN SETTINGS" else "ALLOW ACCESS TO MY MUSIC",
                    color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp,
                )
            }
        }
    }
}
