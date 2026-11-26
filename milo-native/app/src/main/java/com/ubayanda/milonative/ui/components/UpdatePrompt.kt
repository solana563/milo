package com.ubayanda.milonative.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.BuildConfig
import com.ubayanda.milonative.data.UpdateChecker
import com.ubayanda.milonative.ui.theme.Milo
import com.ubayanda.milonative.util.ErrorLog

/** A few seconds after launch, asks GitHub whether a newer milo. is out and offers to download it. */
@Composable
fun UpdatePrompt() {
    val ctx = LocalContext.current
    var up by remember { mutableStateOf<UpdateChecker.Update?>(null) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(4000)
        up = UpdateChecker.check(ctx)
    }
    up?.let { u ->
        AlertDialog(
            onDismissRequest = { up = null },
            containerColor = Milo.card,
            title = { Text("Update available", color = Milo.text) },
            text = {
                Column {
                    Text("milo. ${u.version} is out. You have ${BuildConfig.VERSION_NAME}.", color = Milo.text)
                    if (u.notes.isNotBlank()) Text(u.notes.take(500), color = Milo.dim, fontSize = 13.sp, modifier = androidx.compose.ui.Modifier.padding(top = 10.dp))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    ErrorLog.catching {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u.apkUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                    up = null
                }) { Text("Download", color = Milo.amber) }
            },
            dismissButton = { TextButton(onClick = { UpdateChecker.dismiss(ctx, u.version); up = null }) { Text("Not now", color = Milo.dim) } },
        )
    }
}
