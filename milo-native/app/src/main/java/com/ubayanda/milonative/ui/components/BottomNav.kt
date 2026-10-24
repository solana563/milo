package com.ubayanda.milonative.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.ubayanda.milonative.ui.theme.Milo

enum class Tab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Outlined.Home),
    Search("Search", Icons.Outlined.Search),
    Library("Library", Icons.Outlined.BookmarkBorder),
}

/** The web milo. pill nav: a dark rounded bar with hairline border, the active tab sits in a raised pill. Solid, no blur. */
@Composable
fun BottomNav(current: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val bar = RoundedCornerShape(36.dp)
    Row(
        modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp)
            .clip(bar).background(Milo.card, bar).border(1.dp, Milo.line, bar).padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically,
    ) {
        Tab.values().forEach { tab ->
            val on = tab == current
            val pill = RoundedCornerShape(28.dp)
            Box(
                Modifier.width(76.dp).clip(pill)
                    .then(if (on) Modifier.background(Brush.verticalGradient(listOf(Color(0xFF3A3530), Color(0xFF26221E))), pill).border(1.dp, Milo.line, pill) else Modifier)
                    .clickable { onSelect(tab) }.padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(tab.icon, tab.label, tint = if (on) Milo.amber else Milo.text, modifier = Modifier.size(26.dp))
            }
        }
    }
}
