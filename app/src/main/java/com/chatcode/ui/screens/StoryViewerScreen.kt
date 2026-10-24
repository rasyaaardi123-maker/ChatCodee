package com.chatcode.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.chatcode.data.StoryGroup
import com.chatcode.data.relativeTime
import com.chatcode.ui.MainViewModel
import com.chatcode.ui.components.Avatar
import com.chatcode.ui.components.edgeFade

@Composable
fun StoryViewerScreen(vm: MainViewModel, authorId: String, onClose: () -> Unit) {
    val groups by vm.stories.collectAsStateWithLifecycle()
    val group = groups.firstOrNull { it.authorId == authorId }

    if (group == null) {
        LaunchedEffect(Unit) { onClose() }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
        )
    } else {
        StoryPlayer(group, onClose)
    }
}

@Composable
private fun StoryPlayer(group: StoryGroup, onClose: () -> Unit) {
    val context = LocalContext.current
    val count = group.items.size
    var index by remember { mutableIntStateOf(0) }
    var restart by remember { mutableIntStateOf(0) }
    val progress = remember { Animatable(0f) }
    val safeIndex = index.coerceIn(0, count - 1)
    val item = group.items[safeIndex]
    val sink = remember { MutableInteractionSource() }

    LaunchedEffect(index, restart) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(5000, easing = LinearEasing))
        if (index < count - 1) index += 1 else onClose()
    }

    val request = remember(item.imageUrl) {
        ImageRequest.Builder(context)
            .data(item.imageUrl)
            .size(1080, 1920)
            .build()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AsyncImage(
            model = request,
            contentDescription = "Story " + group.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .edgeFade(top = 140.dp, bottom = 0.dp)
        )

        Row(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .weight(0.35f)
                    .fillMaxHeight()
                    .clickable(interactionSource = sink, indication = null) {
                        if (index > 0) index -= 1 else restart += 1
                    }
            )
            Box(
                Modifier
                    .weight(0.65f)
                    .fillMaxHeight()
                    .clickable(interactionSource = sink, indication = null) {
                        if (index < count - 1) index += 1 else onClose()
                    }
            )
        }

        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(Modifier.fillMaxWidth()) {
                for (i in 0 until count) {
                    Box(
                        Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.3f))
                            .drawBehind {
                                val fraction = when {
                                    i < safeIndex -> 1f
                                    i == safeIndex -> progress.value
                                    else -> 0f
                                }
                                drawRect(
                                    color = Color.White,
                                    size = Size(size.width * fraction, size.height)
                                )
                            }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(
                    name = group.name,
                    seed = group.avatarSeed,
                    size = 36.dp,
                    url = group.avatarUrl
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (group.mine) "Story kamu" else group.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = relativeTime(item.createdAt),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Tutup",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}
