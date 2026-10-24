package com.chatcode.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

private val seedPairs = listOf(
    Color(0xFF7F5AF0) to Color(0xFF2CB67D),
    Color(0xFFF25F4C) to Color(0xFFFF8906),
    Color(0xFF3DA9FC) to Color(0xFF094067),
    Color(0xFFE53170) to Color(0xFF7F5AF0),
    Color(0xFF2CB67D) to Color(0xFF094067),
    Color(0xFFFF8906) to Color(0xFFE53170),
    Color(0xFF6246EA) to Color(0xFF3DA9FC),
    Color(0xFF1F6FEB) to Color(0xFF8134AF),
    Color(0xFF16A085) to Color(0xFF2C3E50)
)

fun seedBrush(seed: Int): Brush {
    val n = seedPairs.size
    val pair = seedPairs[((seed % n) + n) % n]
    return Brush.linearGradient(listOf(pair.first, pair.second))
}

fun formatCount(n: Int): String {
    if (n < 1000) return n.toString()
    if (n >= 1_000_000) return (n / 1_000_000).toString() + "M"
    val tenths = n / 100
    val whole = tenths / 10
    val dec = tenths % 10
    return if (dec == 0) whole.toString() + "K" else whole.toString() + "," + dec.toString() + "K"
}

fun Modifier.topFade(): Modifier = this.background(
    Brush.verticalGradient(listOf(Color(0xFF3C3C3C), Color.Black))
)

@Composable
fun ScreenTitle(title: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .topFade()
            .statusBarsPadding()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun Avatar(
    name: String,
    seed: Int,
    size: Dp,
    modifier: Modifier = Modifier,
    url: String? = null
) {
    val context = LocalContext.current
    val px = with(LocalDensity.current) { size.roundToPx() }

    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(seedBrush(seed)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.firstOrNull()?.uppercase() ?: "?",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42f).sp
        )
        if (!url.isNullOrBlank()) {
            val request = remember(url, px) {
                ImageRequest.Builder(context)
                    .data(url)
                    .size(px)
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun PostImage(uri: String?, seed: Int, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(8.dp)
    val context = LocalContext.current
    if (uri != null) {
        val request = remember(uri) {
            ImageRequest.Builder(context)
                .data(uri)
                .size(900)
                .build()
        }
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier.clip(shape)
        )
    } else {
        Box(
            modifier
                .clip(shape)
                .background(seedBrush(seed)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "</>",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
