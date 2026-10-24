package com.ubayanda.milonative.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.ui.theme.Milo

/** First screen: says plainly why milo. needs to see audio files, then asks. If Android won't ask again, it opens Settings. */
@Composable
fun PermissionScreen(blockedForever: Boolean, onAllow: () -> Unit, onOpenSettings: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Milo.bg).statusBarsPadding().navigationBarsPadding().padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("milo.", color = Color.White, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(18.dp))
        Text(
            "Play the music that's already on your phone",
            color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "milo. reads your songs straight from your phone, SD card and USB drives. " +
                "Nothing is uploaded or copied, so there is nothing to import.",
            color = Milo.dim, fontSize = 15.sp, textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        if (blockedForever) {
            Text(
                "Android is no longer showing the permission box. Open Settings, tap Permissions, then allow Music and audio.",
                color = Milo.dim, fontSize = 14.sp, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
        }
        Button(
            onClick = if (blockedForever) onOpenSettings else onAllow,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
        ) {
            Text(if (blockedForever) "Open Settings" else "Allow access to my music", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}
