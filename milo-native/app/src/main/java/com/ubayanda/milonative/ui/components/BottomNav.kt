package com.ubayanda.milonative.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import com.ubayanda.milonative.ui.components.UserAvatar
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ubayanda.milonative.ui.theme.Milo

enum class Tab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Outlined.Home),
    Search("Search", Icons.Outlined.Search),
    Discover("Discover", Icons.Outlined.Explore),
    Library("Library", Icons.Outlined.BookmarkBorder),
    Profile("Profile", Icons.Outlined.PersonOutline),
}

/**
 * Liquid glass nav: one glass bubble slides under the chosen tab. Its leading edge races ahead and the trailing edge
 * follows a beat later, so it stretches like a drop of liquid and then settles with a little bounce.
 */
@Composable
fun BottomNav(current: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier, avatar: String = "", name: String = "", backdrop: BackdropState? = null) {
    val bar = RoundedCornerShape(44.dp)
    val tabs = Tab.values()
    val idx = current.ordinal
    var prev by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(idx) }
    val right = idx >= prev
    androidx.compose.runtime.SideEffect { prev = idx }
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)
            .lift(bar, 18f).liquidGlass(bar, 44.dp, backdrop, Milo.card, 1.2f, tintAlpha = 0.5f).padding(8.dp),
    ) {
        val slot = maxWidth / tabs.size
        val fast = androidx.compose.animation.core.spring<androidx.compose.ui.unit.Dp>(0.78f, 520f)
        val slow = androidx.compose.animation.core.spring<androidx.compose.ui.unit.Dp>(0.80f, 150f)
        val left by androidx.compose.animation.core.animateDpAsState(slot * idx, if (right) slow else fast, label = "navL")
        val rightEdge by androidx.compose.animation.core.animateDpAsState(slot * (idx + 1), if (right) fast else slow, label = "navR")
        val pill = RoundedCornerShape(32.dp)
        val w = (rightEdge - left + 14.dp).coerceAtLeast(44.dp)
        Box(
            Modifier.offset(x = left - 7.dp).width(w).height(56.dp).clip(pill)
                .glass(pill, 1.7f, Milo.card2)
                .background(Brush.radialGradient(listOf(Milo.amber.copy(alpha = 0.16f), Color.Transparent))),
        )
        Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
            tabs.forEach { tab ->
                val on = tab == current
                val lift by androidx.compose.animation.core.animateFloatAsState(if (on) 1f else 0f, androidx.compose.animation.core.spring(0.6f, 380f), label = "lift")
                Box(
                    Modifier.width(slot).height(56.dp).pressable(true) { onSelect(tab) },
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.foundation.layout.Column(
                        Modifier.graphicsLayer { translationY = -2.dp.toPx() * lift; scaleX = 1f + 0.08f * lift; scaleY = 1f + 0.08f * lift },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (tab == Tab.Profile && avatar.isNotEmpty()) UserAvatar(name, avatar, if (on) 26.dp else 32.dp, ring = on)
                        else Icon(tab.icon, tab.label, tint = if (on) Milo.amber else Milo.text, modifier = Modifier.size(if (on) 24.dp else 28.dp))
                        if (on) androidx.compose.material3.Text(tab.label, color = Milo.amber, style = com.ubayanda.milonative.ui.theme.Ty.micro, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}
