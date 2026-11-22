package com.ubayanda.milonative.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.data.WhatsNew
import com.ubayanda.milonative.ui.theme.Milo

/** The What's new list as a dialog. [onDone] runs when it is closed. */
@Composable
fun WhatsNewSheet(onDone: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDone,
        containerColor = Milo.card, shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
        title = { com.ubayanda.milonative.ui.components.BlurBehind(); Text("What's new", color = Milo.text) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                WhatsNew.entries.forEach { e ->
                    Text(e.title, color = Milo.amber, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                    Text(e.body, color = Milo.text, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDone) { Text("Got it", color = Milo.amber) } },
    )
}
