package com.ubayanda.milonative.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ubayanda.milonative.ui.theme.Milo

enum class Tab(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Rounded.Home),
    Library("Library", Icons.Rounded.LibraryMusic),
    Search("Search", Icons.Rounded.Search),
}

/** Solid (no blur effect) floating bar: cheap to draw, so screens stay smooth. */
@Composable
fun BottomNav(current: Tab, accent: Color, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(26.dp)).background(Color(0xFF111111)).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        Tab.values().forEach { tab ->
            val on = tab == current
            Column(
                Modifier.clip(RoundedCornerShape(16.dp)).clickable { onSelect(tab) }.padding(horizontal = 18.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(tab.icon, tab.label, tint = if (on) accent else Milo.dim, modifier = Modifier.size(24.dp))
                Text(tab.label, color = if (on) accent else Milo.dim, fontSize = 11.sp)
            }
        }
    }
}
