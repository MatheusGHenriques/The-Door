package io.github.matheusghenriques.thedoor.ui.screen

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.core.graphics.createBitmap

@Composable
fun AppIcon(drawable: Drawable, modifier: Modifier) {
    val bitmap = remember(drawable) {
        try {
            if (drawable is BitmapDrawable) {
                drawable.bitmap
            } else {
                val w = drawable.intrinsicWidth.coerceAtLeast(1)
                val h = drawable.intrinsicHeight.coerceAtLeast(1)
                val bmp = createBitmap(w, h)
                val canvas = android.graphics.Canvas(bmp)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bmp
            }
        } catch (_: Exception) {
            createBitmap(1, 1)
        }
    }
    val painter = remember(bitmap) { BitmapPainter(bitmap.asImageBitmap()) }
    Image(
        painter = painter, contentDescription = null, modifier = modifier
    )
}
