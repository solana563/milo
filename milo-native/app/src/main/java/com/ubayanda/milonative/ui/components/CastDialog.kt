package com.ubayanda.milonative.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.mediarouter.media.MediaRouteSelector
import androidx.mediarouter.media.MediaRouter
import com.google.android.gms.cast.CastMediaControlIntent
import com.ubayanda.milonative.ui.theme.Milo

/** Lists Cast devices on the Wi-Fi; picking one sends the music there. */
@Composable
fun CastDialog(onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    val router = remember { try { MediaRouter.getInstance(ctx) } catch (_: Throwable) { null } }
    val selector = remember {
        MediaRouteSelector.Builder().addControlCategory(CastMediaControlIntent.categoryForCast(CastMediaControlIntent.DEFAULT_MEDIA_RECEIVER_APPLICATION_ID)).build()
    }
    DisposableEffect(router) {
        try { com.google.android.gms.cast.framework.CastContext.getSharedInstance(ctx, java.util.concurrent.Executors.newSingleThreadExecutor()) } catch (_: Throwable) {}
        val cb = object : MediaRouter.Callback() {
            override fun onRouteAdded(r: MediaRouter, route: MediaRouter.RouteInfo) { tick++ }
            override fun onRouteRemoved(r: MediaRouter, route: MediaRouter.RouteInfo) { tick++ }
            override fun onRouteChanged(r: MediaRouter, route: MediaRouter.RouteInfo) { tick++ }
            override fun onRouteSelected(r: MediaRouter, route: MediaRouter.RouteInfo, reason: Int) { tick++ }
            override fun onRouteUnselected(r: MediaRouter, route: MediaRouter.RouteInfo, reason: Int) { tick++ }
        }
        try { router?.addCallback(selector, cb, MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY) } catch (_: Throwable) {}
        onDispose { try { router?.removeCallback(cb) } catch (_: Throwable) {} }
    }
    val routes = if (tick >= 0 && router != null) router.routes.filter { it.matchesSelector(selector) && !it.isDefault } else emptyList()
    val selectedId = if (tick >= 0) router?.selectedRoute?.id else null
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Milo.card,
        title = { Text("Cast to a device", color = Milo.text) },
        text = {
            Column {
                if (routes.isEmpty()) Text(
                    "Looking for devices on your Wi-Fi... Make sure the TV or speaker is on and on the same network.",
                    color = Milo.dim, fontSize = 15.sp,
                )
                routes.forEach { r ->
                    val on = r.id == selectedId
                    Text(
                        r.name + if (on) "  ·  connected" else "", color = if (on) Milo.amber else Milo.text, fontSize = 17.sp,
                        modifier = Modifier.fillMaxWidth().clickable {
                            try { router?.selectRoute(r) } catch (_: Throwable) {}
                            onDismiss()
                        }.padding(vertical = 12.dp),
                    )
                }
            }
        },
        confirmButton = {
            val connected = routes.any { it.id == selectedId }
            if (connected) TextButton(onClick = { try { router?.selectRoute(router.defaultRoute) } catch (_: Throwable) {}; onDismiss() }) { Text("Stop casting", color = Milo.amber) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close", color = Milo.dim) } },
    )
}
