package com.chatcode.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chatcode.data.Comment
import com.chatcode.ui.MainViewModel
import com.chatcode.ui.theme.TextSecondary
import kotlinx.coroutines.launch

private val InputColor = Color(0xFF2B2B2B).copy(alpha = 0.85f)
private val SendColor = Color(0xFF1E6FE8).copy(alpha = 0.90f)

@Composable
fun CommentsSheetContent(vm: MainViewModel, postId: Long) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var comments by remember { mutableStateOf<List<Comment>?>(null) }
    var text by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

    LaunchedEffect(postId) {
        comments = vm.comments(postId).getOrDefault(emptyList())
    }

    val size = comments?.size ?: 0
    LaunchedEffect(size) {
        if (size > 0) listState.scrollToItem(size - 1)
    }

    Column(Modifier.fillMaxWidth()) {
        Text(
            text = "Komentar",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        val loaded = comments
        if (loaded == null) {
            Text(
                text = "Memuat komentar...",
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
            )
        } else if (loaded.isEmpty()) {
            Text(
                text = "Belum ada komentar. Jadilah yang pertama.",
                color = TextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .edgeFade(top = 0.dp, bottom = 20.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
            ) {
                items(loaded, key = { it.id }, contentType = { "komentar" }) { comment ->
                    CommentRowItem(comment)
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = text,
                onValueChange = { if (it.length <= 1000) text = it },
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                cursorBrush = SolidColor(Color.White),
                maxLines = 4,
                modifier = Modifier.weight(1f),
                decorationBox = { inner ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(CircleShape)
                            .background(InputColor)
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (text.isEmpty()) {
                            Text("Tulis komentar...", color = TextSecondary, fontSize = 16.sp)
                        }
                        inner()
                    }
                }
            )
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(SendColor)
                    .clickable(enabled = text.isNotBlank() && !sending) {
                        val body = text.trim()
                        scope.launch {
                            sending = true
                            if (vm.addComment(postId, body)) {
                                text = ""
                                comments = vm.comments(postId).getOrDefault(emptyList())
                            }
                            sending = false
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowUpward,
                    contentDescription = "Kirim komentar",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}

@Composable
private fun CommentRowItem(comment: Comment) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Avatar(
            name = comment.name,
            seed = comment.avatarSeed,
            size = 36.dp,
            url = comment.avatarUrl
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = comment.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(Modifier.width(8.dp))
                Text(text = comment.time, color = TextSecondary, fontSize = 12.sp)
            }
            Text(
                text = comment.text,
                color = Color(0xFFDDDDDD),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}
