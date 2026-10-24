package com.chatcode.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.max

private val TopStops = arrayOf(
    0f to Color.Black.copy(alpha = 0.85f),
    0.25f to Color.Black.copy(alpha = 0.62f),
    0.5f to Color.Black.copy(alpha = 0.36f),
    0.75f to Color.Black.copy(alpha = 0.14f),
    1f to Color.Black.copy(alpha = 0f)
)

private val BottomStops = arrayOf(
    0f to Color.Black.copy(alpha = 0f),
    0.25f to Color.Black.copy(alpha = 0.14f),
    0.5f to Color.Black.copy(alpha = 0.36f),
    0.75f to Color.Black.copy(alpha = 0.62f),
    1f to Color.Black.copy(alpha = 0.85f)
)

fun Modifier.edgeFade(top: Dp = 40.dp, bottom: Dp = 40.dp): Modifier = drawWithCache {
    val topPx = top.toPx()
    val bottomPx = bottom.toPx()
    val topBrush = Brush.verticalGradient(
        colorStops = TopStops,
        startY = 0f,
        endY = max(topPx, 1f)
    )
    val bottomBrush = Brush.verticalGradient(
        colorStops = BottomStops,
        startY = 0f,
        endY = max(bottomPx, 1f)
    )
    onDrawWithContent {
        drawContent()
        if (topPx > 0f) {
            drawRect(brush = topBrush, size = Size(size.width, topPx))
        }
        if (bottomPx > 0f) {
            translate(left = 0f, top = size.height - bottomPx) {
                drawRect(brush = bottomBrush, size = Size(size.width, bottomPx))
            }
        }
    }
}
