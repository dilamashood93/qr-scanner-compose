// qrscanner/src/main/java/nfm/qrscanner/ui/ScanOverlay.kt
package nfm.qrscanner.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp

@Composable
fun ScanOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize().background(Color(0x88000000))) {
        val boxWidth = size.width * 0.76f
        val boxHeight = boxWidth
        val left = (size.width - boxWidth) / 2f
        val top = (size.height - boxHeight) / 2.6f
        val rect = Rect(left, top, left + boxWidth, top + boxHeight)
        val rr = RoundRect(rect, CornerRadius(24f, 24f))

        val cutout = Path().apply { addRoundRect(rr) }

        clipPath(cutout, clipOp = androidx.compose.ui.graphics.ClipOp.Difference) { /* mask */ }

        drawRoundRect(
            color = Color.White,
            topLeft = rect.topLeft,
            size = rect.size,
            cornerRadius = CornerRadius(24f, 24f),
            style = Stroke(width = 3.dp.toPx())
        )

        val g = 28.dp.toPx()
        val s = 4.dp.toPx()
        // corner guides
        drawLine(Color.White, rect.topLeft, rect.topLeft.copy(x = rect.left + g), strokeWidth = s)
        drawLine(Color.White, rect.topLeft, rect.topLeft.copy(y = rect.top + g), strokeWidth = s)
        drawLine(Color.White, rect.topRight, rect.topRight.copy(x = rect.right - g), strokeWidth = s)
        drawLine(Color.White, rect.topRight, rect.topRight.copy(y = rect.top + g), strokeWidth = s)
        drawLine(Color.White, rect.bottomLeft, rect.bottomLeft.copy(x = rect.left + g), strokeWidth = s)
        drawLine(Color.White, rect.bottomLeft, rect.bottomLeft.copy(y = rect.bottom - g), strokeWidth = s)
        drawLine(Color.White, rect.bottomRight, rect.bottomRight.copy(x = rect.right - g), strokeWidth = s)
        drawLine(Color.White, rect.bottomRight, rect.bottomRight.copy(y = rect.bottom - g), strokeWidth = s)
    }
}
