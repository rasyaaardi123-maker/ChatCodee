package com.chatcode.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chatcode.data.Post
import com.chatcode.ui.theme.TextSecondary

@Composable
fun PostItem(
    post: Post,
    actions: PostActions,
    modifier: Modifier = Modifier
) {
    val anchor = remember { FloatArray(2) }

    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(
            Modifier.width(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Avatar(
                name = post.name,
                seed = post.avatarSeed,
                size = 48.dp,
                modifier = Modifier.clickable { actions.author(post) },
                url = post.avatarUrl
            )
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier.clickable { actions.like(post) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (post.liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Suka",
                    tint = if (post.liked) Color(0xFFFF3B5C) else Color.White,
                    modifier = Modifier.size(30.dp)
                )
                Text(formatCount(post.likes), color = Color.White, fontSize = 12.sp)
            }
            Spacer(Modifier.height(10.dp))
            Column(
                Modifier.clickable { actions.comments(post) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "Komentar",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Text(formatCount(post.comments), color = Color.White, fontSize = 12.sp)
            }
            Spacer(Modifier.height(2.dp))
            Box(
                Modifier
                    .size(34.dp)
                    .onGloballyPositioned { coords ->
                        val p = coords.positionInRoot()
                        anchor[0] = p.x
                        anchor[1] = p.y
                    }
                    .clip(CircleShape)
                    .clickable { actions.menu(post, Offset(anchor[0], anchor[1])) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Menu",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        Column(
            Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Row(
                Modifier.clickable { actions.author(post) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = post.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "@" + post.username,
                    color = TextSecondary,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = post.time,
                    color = Color(0xFFDDDDDD),
                    fontSize = 12.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
            if (post.text.isNotBlank()) {
                Text(
                    text = post.text,
                    color = Color.White,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (post.imageUri != null || post.imageSeed >= 0) {
                PostImage(
                    uri = post.imageUri,
                    seed = post.imageSeed,
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth(0.55f)
                        .aspectRatio(0.5f)
                )
            }
        }
    }
}
