package com.ubayanda.milonative.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

/** A QR code drawn on the phone. Nothing is sent anywhere to make it. */
@Composable
fun QrImage(text: String, size: Dp, modifier: Modifier = Modifier) {
    val bmp = remember(text) {
        try {
            val n = 512
            val m = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, n, n, mapOf(EncodeHintType.MARGIN to 2))
            val px = IntArray(n * n) { if (m[it % n, it / n]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt() }
            Bitmap.createBitmap(px, n, n, Bitmap.Config.ARGB_8888)
        } catch (_: Exception) { null }
    }
    bmp?.let { Image(it.asImageBitmap(), "QR code", modifier.size(size)) }
}
