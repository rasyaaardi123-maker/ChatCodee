package com.chatcode.ui.chat

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlinx.coroutines.delay

private const val MinBar = 0.06f

@Composable
fun VoiceWaveform(
    running: Boolean,
    barColor: Color,
    modifier: Modifier = Modifier,
    barCount: Int = 40
) {
    val meter = rememberMicMeter(running)
    var amps by remember { mutableStateOf(FloatArray(barCount) { MinBar }) }

    LaunchedEffect(running) {
        while (running) {
            delay(120)
            val peak = meter.takePeak()
            val prev = amps
            amps = FloatArray(barCount) { i ->
                if (i < barCount - 1) prev[i + 1] else max(MinBar, peak)
            }
        }
    }

    Canvas(modifier = modifier) {
        val n = amps.size
        val slot = size.width / n
        val barWidth = slot * 0.45f
        val minHeight = 4.dp.toPx()
        for (i in 0 until n) {
            val h = max(minHeight, size.height * amps[i])
            val x = slot * i + (slot - barWidth) / 2f
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, (size.height - h) / 2f),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
