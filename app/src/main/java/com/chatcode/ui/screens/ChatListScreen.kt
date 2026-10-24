package com.chatcode.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chatcode.data.Chat
import com.chatcode.ui.MainViewModel
import com.chatcode.ui.components.Avatar
import com.chatcode.ui.components.EmptyState
import com.chatcode.ui.components.ScreenTitle
import com.chatcode.ui.components.edgeFade
import com.chatcode.ui.theme.DividerColor
import com.chatcode.ui.theme.TextSecondary

@Composable
fun ChatListScreen(vm: MainViewModel, onOpen: (Long) -> Unit) {
    val chats by vm.chats.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        ScreenTitle("Chat")
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .edgeFade(top = 24.dp, bottom = 120.dp),
            contentPadding = PaddingValues(bottom = 110.dp)
        ) {
            if (chats.isEmpty()) {
                item(key = "kosong", contentType = "kosong") {
                    EmptyState(
                        icon = Icons.Outlined.ChatBubbleOutline,
                        title = "Belum ada percakapan",
                        message = "Percakapan dengan pengguna lain akan muncul di sini."
                    )
                }
            }
            items(chats, key = { it.id }, contentType = { "chat" }) { chat ->
                Column {
                    ChatRow(chat) { onOpen(chat.id) }
                    HorizontalDivider(color = DividerColor)
                }
            }
        }
    }
}

@Composable
private fun ChatRow(chat: Chat, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(chat.name, chat.avatarSeed, 52.dp)
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = chat.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = chat.lastMessage,
                color = TextSecondary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = chat.time, color = TextSecondary, fontSize = 12.sp)
            if (chat.unread > 0) {
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1F6FEB)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chat.unread.toString(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
