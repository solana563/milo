package com.ubayanda.milonative

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CrashScreen(trace: String, onContinue: () -> Unit) {
    val ctx = LocalContext.current
    Column(
        Modifier.fillMaxSize().background(Color(0xFF0B0A09)).statusBarsPadding().navigationBarsPadding().padding(16.dp),
    ) {
        Text("milo. crashed last time", color = Color(0xFFF99C00), fontSize = 20.sp)
        Spacer(Modifier.height(6.dp))
        Text("Tap Share and send this to the developer.", color = Color(0xFFF4EFE4), fontSize = 14.sp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = {
                val i = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, trace)
                ctx.startActivity(Intent.createChooser(i, "Share crash report"))
            }) { Text("Share") }
            Button(onClick = onContinue) { Text("Try again") }
        }
        Spacer(Modifier.height(10.dp))
        SelectionContainer(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Text(trace, color = Color(0xFFCCC5B8), fontSize = 11.sp)
        }
    }
}
