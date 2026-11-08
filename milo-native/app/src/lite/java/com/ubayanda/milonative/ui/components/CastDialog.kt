package com.ubayanda.milonative.ui.components

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext

/** F-Droid build: no Google Cast (it relies on proprietary Google libraries). Tells the person and closes. */
@Composable
fun CastDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    LaunchedEffect(Unit) {
        Toast.makeText(ctx, "Casting isn't included in the F-Droid build.", Toast.LENGTH_LONG).show()
        onDismiss()
    }
}
