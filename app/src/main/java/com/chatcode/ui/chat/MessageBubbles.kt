package com.chatcode.ui.chat

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.chatcode.data.Message
import com.chatcode.data.MessageKind

private val IncomingColor = Color(0xFF2A2A2A).copy(alpha = 0.80f)
private val OutgoingColor = Color(0xFF1E6FE8).copy(alpha = 0.85f)
private val BubbleShape = RoundedCornerShape(18.dp)

private val StaticBars = floatArrayOf(
    0.30f, 0.50f, 0.80f, 0.50f, 0.30f, 0.60f, 1.00f, 0.70f,
    0.40f, 0.30f, 0.50f, 0.90f, 0.60f, 0.35f, 0.45f, 0.80f,
    0.55f, 0.30f, 0.60f, 0.95f, 0.50f, 0.30f, 0.40f, 0.70f
)

fun formatDuration(totalSec: Int): String {
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return minutes.toString() + ":" + seconds.toString().padStart(2, '0')
}

@Composable
fun MessageItem(
    message: Message,
    topPadding: Dp,
    maxBubbleWidth: Dp,
    animateIn: Boolean
) {
    val enter = remember { Animatable(if (animateIn) 0f else 1f) }

    LaunchedEffect(Unit) {
        if (animateIn && enter.value < 1f) {
            enter.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = topPadding)
            .graphicsLayer {
                alpha = enter.value
                translationY = (1f - enter.value) * 14.dp.toPx()
            },
        contentAlignment = if (message.fromMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        when (message.kind) {
            MessageKind.TEXT -> TextBubble(message, maxBubbleWidth)
            MessageKind.VOICE -> VoiceNoteBubble(message.durationSec)
            MessageKind.MEDIA -> MediaBubble(message)
            MessageKind.FILE -> FileBubble(message.text, maxBubbleWidth)
        }
    }
}

@Composable
private fun TextBubble(message: Message, maxWidth: Dp) {
    Text(
        text = message.text,
        color = Color.White,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .widthIn(max = maxWidth)
            .clip(BubbleShape)
            .background(if (message.fromMe) OutgoingColor else IncomingColor)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
fun VoiceNoteBubble(durationSec: Int, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(BubbleShape)
            .background(OutgoingColor)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.PlayArrow,
            contentDescription = "Putar",
            tint = Color.White,
            modifier = Modifier.size(26.dp)
        )
        Spacer(Modifier.width(6.dp))
        Canvas(
            Modifier
                .width(108.dp)
                .height(24.dp)
        ) {
            val n = StaticBars.size
            val slot = size.width / n
            val barWidth = slot * 0.45f
            val minHeight = 3.dp.toPx()
            for (i in 0 until n) {
                val h = maxOf(minHeight, size.height * StaticBars[i])
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.85f),
                    topLeft = Offset(slot * i + (slot - barWidth) / 2f, (size.height - h) / 2f),
                    size = Size(barWidth, h),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
                )
            }
        }
        Spacer(Modifier.width(6.dp))
        Text(
            text = formatDuration(durationSec),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MediaBubble(message: Message) {
    val context = LocalContext.current
    Box(
        Modifier
            .size(width = 180.dp, height = 230.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(OutgoingColor),
        contentAlignment = Alignment.Center
    ) {
        if (message.isVideo || message.uri == null) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Video",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        } else {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(message.uri)
                    .size(400)
                    .build(),
                contentDescription = "Foto",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun FileBubble(fileName: String, maxWidth: Dp) {
    Row(
        Modifier
            .widthIn(max = maxWidth)
            .clip(BubbleShape)
            .background(OutgoingColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.Description,
            contentDescription = "File",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = fileName,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}
