package com.ubayanda.milonative.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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

/** The web milo. pill nav: tinted rounded bar, the active tab sits in a raised lighter pill. Solid colours, no blur effect. */
@Composable
fun BottomNav(current: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier, avatar: String = "", name: String = "") {
    val bar = RoundedCornerShape(40.dp)
    Row(
        modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)
            .clip(bar).background(Milo.card.copy(alpha = 0.96f), bar).border(1.dp, Milo.line, bar).padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically,
    ) {
        Tab.values().forEach { tab ->
            val on = tab == current
            val pill = RoundedCornerShape(32.dp)
            Box(
                Modifier.width(if (on) 84.dp else 60.dp).height(56.dp).clip(pill)
                    .then(
                        if (on) Modifier.background(Brush.verticalGradient(listOf(lerp(Milo.card2, Color.White, 0.10f), Milo.card2)), pill)
                            .border(1.dp, Milo.line, pill) else Modifier
                    )
                    .clickable { onSelect(tab) },
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (tab == Tab.Profile && avatar.isNotEmpty()) UserAvatar(name, avatar, if (on) 26.dp else 32.dp, ring = on)
                    else Icon(tab.icon, tab.label, tint = if (on) Milo.amber else Milo.text, modifier = Modifier.size(if (on) 24.dp else 28.dp))
                    if (on) androidx.compose.material3.Text(tab.label, color = Milo.amber, style = com.ubayanda.milonative.ui.theme.Ty.micro)
                }
            }
        }
    }
}
