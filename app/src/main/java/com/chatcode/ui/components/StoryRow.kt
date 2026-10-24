package com.chatcode.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chatcode.data.StoryGroup

private val ringBrush = Brush.sweepGradient(
    listOf(
        Color(0xFFF58529),
        Color(0xFFDD2A7B),
        Color(0xFF8134AF),
        Color(0xFF515BD4),
        Color(0xFFF58529)
    )
)

@Composable
fun StoryRow(
    groups: List<StoryGroup>,
    onOpen: (StoryGroup) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        items(groups, key = { it.authorId }, contentType = { "story" }) { group ->
            StoryItem(group) { onOpen(group) }
        }
    }
}

@Composable
private fun StoryItem(group: StoryGroup, onClick: () -> Unit) {
    Column(
        Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(66.dp)
                .border(2.5.dp, ringBrush, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Avatar(
                name = group.name,
                seed = group.avatarSeed,
                size = 54.dp,
                url = group.avatarUrl
            )
        }
        Text(
            text = if (group.mine) "Story kamu" else group.name,
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 4.dp)
                .width(66.dp)
        )
    }
}
