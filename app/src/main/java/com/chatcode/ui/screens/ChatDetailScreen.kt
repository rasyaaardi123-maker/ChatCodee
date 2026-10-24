package com.chatcode.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.IntState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.chatcode.data.Message
import com.chatcode.ui.MainViewModel
import com.chatcode.ui.chat.ChatInputBar
import com.chatcode.ui.chat.ChatInputViewModel
import com.chatcode.ui.chat.InputMode
import com.chatcode.ui.chat.MessageItem
import com.chatcode.ui.components.Avatar
import com.chatcode.ui.popup.LocalPopups
import kotlinx.coroutines.flow.distinctUntilChanged

private val TopGradient = Brush.verticalGradient(
    colorStops = arrayOf(
        0f to Color.Black.copy(alpha = 0.85f),
        0.4f to Color.Black.copy(alpha = 0.65f),
        0.7f to Color.Black.copy(alpha = 0.3f),
        0.9f to Color.Black.copy(alpha = 0.08f),
        1f to Color.Black.copy(alpha = 0f)
    )
)

private val BottomGradient = Brush.verticalGradient(
    colorStops = arrayOf(
        0f to Color.Black.copy(alpha = 0f),
        0.1f to Color.Black.copy(alpha = 0.08f),
        0.3f to Color.Black.copy(alpha = 0.3f),
        0.6f to Color.Black.copy(alpha = 0.65f),
        1f to Color.Black.copy(alpha = 0.85f)
    )
)

@Composable
fun ChatDetailScreen(
    vm: MainViewModel,
    chatId: Long,
    onBack: () -> Unit,
    onWorkspace: () -> Unit
) {
    val inputVm: ChatInputViewModel = viewModel()
    val input by inputVm.state.collectAsStateWithLifecycle()
    val chats by vm.chats.collectAsStateWithLifecycle()
    val chat = chats.firstOrNull { it.id == chatId }
    val count = chat?.messages?.size ?: 0

    val context = LocalContext.current
    val density = LocalDensity.current
    val popups = LocalPopups.current
    val listState = rememberLazyListState()
    val imeInsets = WindowInsets.ime
    val latestCount = rememberUpdatedState(count)
    val maxBubble = (LocalConfiguration.current.screenWidthDp * 0.72f).dp
    val statusTop = with(density) { WindowInsets.statusBars.getTop(density).toDp() }
    val headerHeightPx = remember { mutableIntStateOf(with(density) { 64.dp.roundToPx() }) }
    val composerHeightPx = remember { mutableIntStateOf(with(density) { 68.dp.roundToPx() }) }
    val knownIds = remember(chatId) { chat?.messages?.map { it.id }?.toSet() ?: emptySet() }

    val showMessage: (String) -> Unit = { message -> popups.showToast(message) }

    val mediaPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val isVideo = context.contentResolver.getType(uri)?.startsWith("video/") == true
            vm.sendMedia(chatId, uri.toString(), isVideo)
        }
    }

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            vm.sendFile(chatId, fileNameOf(context, uri))
        }
    }

    LaunchedEffect(chatId) { vm.markRead(chatId) }

    LaunchedEffect(count) {
        if (count > 0) listState.scrollToItem(count - 1)
    }

    LaunchedEffect(Unit) {
        snapshotFlow { imeInsets.getBottom(density) > 0 }
            .distinctUntilChanged()
            .collect { open ->
                if (open && latestCount.value > 0) {
                    listState.scrollToItem(latestCount.value - 1)
                }
            }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { composerHeightPx.intValue }
            .distinctUntilChanged()
            .collect {
                if (latestCount.value > 0) {
                    listState.scrollToItem(latestCount.value - 1)
                }
            }
    }

    BackHandler(enabled = input.mode != InputMode.Idle) {
        inputVm.cancelMode()
    }

    if (chat == null) return

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .imePadding()
    ) {
        MessageList(
            messages = chat.messages,
            knownIds = knownIds,
            listState = listState,
            statusTop = statusTop,
            headerHeightPx = headerHeightPx,
            composerHeightPx = composerHeightPx,
            maxBubble = maxBubble
        )

        TopFade(statusTop, headerHeightPx, Modifier.align(Alignment.TopStart))
        BottomFade(composerHeightPx, Modifier.align(Alignment.BottomCenter))

        ChatHeader(
            name = chat.name,
            seed = chat.avatarSeed,
            onBack = onBack,
            onCall = { showMessage("Panggilan belum tersedia") },
            onVideo = { showMessage("Panggilan belum tersedia") },
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .onSizeChanged { headerHeightPx.intValue = it.height }
        )

        ChatInputBar(
            state = input,
            onTextChange = inputVm::onTextChange,
            onOpenPlus = inputVm::openPlusMenu,
            onCancel = { inputVm.cancelMode() },
            onSendText = { inputVm.takeText()?.let { vm.sendMessage(chatId, it) } },
            onStartVoice = inputVm::startVoice,
            onToggleVoicePause = inputVm::togglePause,
            onSendVoice = {
                val seconds = inputVm.finishVoice()
                if (seconds > 0) vm.sendVoiceNote(chatId, seconds)
            },
            onStartStt = inputVm::startStt,
            onStopStt = inputVm::stopStt,
            onSendStt = { inputVm.finishSttAndTakeText()?.let { vm.sendMessage(chatId, it) } },
            onCamera = {
                inputVm.cancelMode()
                showMessage("Kamera belum tersedia")
            },
            onMedia = {
                inputVm.cancelMode()
                mediaPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                )
            },
            onFile = {
                inputVm.cancelMode()
                filePicker.launch(arrayOf("*/*"))
            },
            onStream = {
                inputVm.cancelMode()
                onWorkspace()
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { composerHeightPx.intValue = it.height }
        )
    }
}

@Composable
private fun MessageList(
    messages: List<Message>,
    knownIds: Set<Long>,
    listState: LazyListState,
    statusTop: Dp,
    headerHeightPx: IntState,
    composerHeightPx: IntState,
    maxBubble: Dp
) {
    val density = LocalDensity.current
    val topPad = statusTop + with(density) { headerHeightPx.intValue.toDp() }
    val bottomPad = with(density) { composerHeightPx.intValue.toDp() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = topPad + 4.dp,
            bottom = bottomPad + 8.dp
        )
    ) {
        itemsIndexed(
            messages,
            key = { _, message -> message.id },
            contentType = { _, message -> message.kind }
        ) { index, message ->
            val previous = messages.getOrNull(index - 1)
            val top = when {
                previous == null -> 0.dp
                previous.fromMe == message.fromMe -> 2.dp
                else -> 8.dp
            }
            MessageItem(
                message = message,
                topPadding = top,
                maxBubbleWidth = maxBubble,
                animateIn = index == messages.lastIndex && message.id !in knownIds
            )
        }
    }
}

@Composable
private fun TopFade(statusTop: Dp, headerHeightPx: IntState, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val height = statusTop + with(density) { headerHeightPx.intValue.toDp() } + 24.dp
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(TopGradient)
    )
}

@Composable
private fun BottomFade(composerHeightPx: IntState, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val height = with(density) { composerHeightPx.intValue.toDp() } + 24.dp
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .background(BottomGradient)
    )
}

@Composable
private fun ChatHeader(
    name: String,
    seed: Int,
    onBack: () -> Unit,
    onCall: () -> Unit,
    onVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Kembali",
                tint = Color.White
            )
        }
        Avatar(name, seed, 44.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            text = name,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onCall) {
            Icon(
                imageVector = Icons.Outlined.Call,
                contentDescription = "Telepon",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        IconButton(onClick = onVideo) {
            Icon(
                imageVector = Icons.Outlined.Videocam,
                contentDescription = "Panggilan video",
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

private fun fileNameOf(context: Context, uri: Uri): String {
    try {
        context.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) {
                    val name = cursor.getString(index)
                    if (!name.isNullOrBlank()) return name
                }
            }
        }
    } catch (e: Exception) {
        return uri.lastPathSegment ?: "File"
    }
    return uri.lastPathSegment ?: "File"
}
