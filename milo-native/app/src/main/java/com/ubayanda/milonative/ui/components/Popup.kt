package com.ubayanda.milonative.ui.components

import android.os.Build
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.ubayanda.milonative.ui.theme.Milo
import com.ubayanda.milonative.ui.theme.Rad
import com.ubayanda.milonative.ui.theme.Ty

/**
 * Call inside a dialog: blurs and dims everything behind it. Real blur needs Android 12+; older phones get the dim only.
 */
@Composable
fun BlurBehind(radius: Int = 42, dim: Float = 0.42f) {
    val view = LocalView.current
    SideEffect {
        val w = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        w.setDimAmount(if (Build.VERSION.SDK_INT >= 31) dim else dim + 0.2f)
        if (Build.VERSION.SDK_INT >= 31) {
            w.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            val lp = w.attributes
            lp.blurBehindRadius = radius
            w.attributes = lp
        }
    }
}

/** A raised glass card that springs up over a blurred app. Tap outside to close. Same shape and weight as the rest of the UI. */
@Composable
fun MiloPopup(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        BlurBehind()
        var shown by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { shown = true }
        val sc by animateFloatAsState(if (shown) 1f else 0.86f, spring(0.72f, 380f), label = "pop")
        val al by animateFloatAsState(if (shown) 1f else 0f, tween(160), label = "popA")
        val shape = RoundedCornerShape(Rad.xl)
        Box(
            Modifier.fillMaxSize().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier.padding(horizontal = 22.dp, vertical = 40.dp).widthIn(max = 420.dp).fillMaxWidth()
                    .graphicsLayer { scaleX = sc; scaleY = sc; alpha = al }
                    .lift(shape, 28f).glass(shape, 1.4f, Milo.card.copy(alpha = 0.97f)).border(1.dp, glassStroke(1f), shape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                    .verticalScroll(rememberScrollState()).padding(16.dp),
                content = content,
            )
        }
    }
}

/** One full-width action row inside a popup. */
@Composable
fun PopupAction(icon: ImageVector?, label: String, tint: Color = Milo.text, onClick: () -> Unit) {
    val shape = RoundedCornerShape(Rad.md)
    Row(
        Modifier.fillMaxWidth().clip(shape).pressable(true, onClick).padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (tint == Milo.text) Milo.dim else tint, modifier = Modifier.size(22.dp))
            Spacer(Modifier.size(14.dp))
        }
        Text(label, color = tint, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

/** A square quick-action tile (4 of these sit in a row at the top of a song's popup). */
@Composable
fun RowScope.PopupQuick(icon: ImageVector, label: String, on: Boolean = false, onClick: () -> Unit) {
    val shape = RoundedCornerShape(Rad.md)
    Column(
        Modifier.weight(1f).clip(shape).glass(shape, 1.3f, Milo.card2).border(1.dp, glassStroke(0.8f), shape)
            .pressable(true, onClick).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, label, tint = if (on) Milo.amber else Milo.text, modifier = Modifier.size(24.dp))
        Text(label, color = if (on) Milo.amber else Milo.dim, style = Ty.micro, textAlign = TextAlign.Center, maxLines = 1)
    }
}
