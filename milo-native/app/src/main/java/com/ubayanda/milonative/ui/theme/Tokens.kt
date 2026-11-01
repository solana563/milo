package com.ubayanda.milonative.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Design tokens: one 4dp spacing grid, one corner scale, one type scale. Screens use these instead of one-off numbers. */
object Sp {
    val x1 = 4.dp
    val x2 = 8.dp
    val x3 = 12.dp
    val x4 = 16.dp
    val x5 = 20.dp
    val x6 = 24.dp
    val x8 = 32.dp
    /** Side margin of every screen. */
    val gutter = 20.dp
}

object Rad {
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
}

object Ty {
    val display = TextStyle(fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.ExtraBold)
    val title = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.ExtraBold)
    val heading = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
    val body = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium)
    val label = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
    val caption = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal)
    val micro = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.SemiBold)
}
