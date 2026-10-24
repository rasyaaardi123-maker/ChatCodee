package com.chatcode.ui.chat

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

private val FieldGray = Color(0xFF2B2B2B).copy(alpha = 0.85f)
private val ActionBlue = Color(0xFF1E6FE8).copy(alpha = 0.90f)
private val LightGray = Color(0xFFBDBDBD)

private enum class InputLayout { Typing, Voice, Stt }

private fun layoutOf(mode: InputMode): InputLayout = when (mode) {
    InputMode.RecordingVoice -> InputLayout.Voice
    InputMode.RecordingStt -> InputLayout.Stt
    else -> InputLayout.Typing
}

@Composable
fun ChatInputBar(
    state: ChatInputState,
    onTextChange: (String) -> Unit,
    onOpenPlus: () -> Unit,
    onCancel: () -> Unit,
    onSendText: () -> Unit,
    onStartVoice: () -> Unit,
    onToggleVoicePause: () -> Unit,
    onSendVoice: () -> Unit,
    onStartStt: () -> Unit,
    onStopStt: () -> Unit,
    onSendStt: () -> Unit,
    onCamera: () -> Unit,
    onMedia: () -> Unit,
    onFile: () -> Unit,
    onStream: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .animateContentSize(animationSpec = tween(220, easing = FastOutSlowInEasing))
    ) {
        Crossfade(
            targetState = layoutOf(state.mode),
            animationSpec = tween(200, easing = FastOutSlowInEasing),
            label = "inputLayout"
        ) { layout ->
            when (layout) {
                InputLayout.Voice -> VoiceRecorderBar(
                    paused = state.paused,
                    onTogglePause = onToggleVoicePause,
                    onSend = onSendVoice
                )
                InputLayout.Stt -> SttBar(
                    text = state.text,
                    onCancel = onCancel,
                    onStop = onStopStt,
                    onSend = onSendStt
                )
                InputLayout.Typing -> TypingBar(
                    state = state,
                    onTextChange = onTextChange,
                    onOpenPlus = onOpenPlus,
                    onCancel = onCancel,
                    onSendText = onSendText,
                    onStartVoice = onStartVoice,
                    onStartStt = onStartStt,
                    onCamera = onCamera,
                    onMedia = onMedia,
                    onFile = onFile,
                    onStream = onStream
                )
            }
        }
    }
}

@Composable
private fun TypingBar(
    state: ChatInputState,
    onTextChange: (String) -> Unit,
    onOpenPlus: () -> Unit,
    onCancel: () -> Unit,
    onSendText: () -> Unit,
    onStartVoice: () -> Unit,
    onStartStt: () -> Unit,
    onCamera: () -> Unit,
    onMedia: () -> Unit,
    onFile: () -> Unit,
    onStream: () -> Unit
) {
    val menuOpen = state.mode == InputMode.PlusMenu
    val hasText = state.text.isNotEmpty()
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(hasText) {
        if (!hasText) expanded = false
    }

    Box(Modifier.fillMaxWidth()) {
        if (menuOpen) {
            Spacer(Modifier.height(48.dp))
        } else {
            val sidePad = if (expanded) 0.dp else 56.dp
            val bottomPad = if (expanded) 60.dp else 0.dp
            val fieldShape = RoundedCornerShape(if (expanded) 28.dp else 24.dp)

            BasicTextField(
                value = state.text,
                onValueChange = onTextChange,
                textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
                cursorBrush = SolidColor(Color.White),
                maxLines = 7,
                onTextLayout = { result ->
                    if (result.lineCount > 1 && !expanded) expanded = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = sidePad, end = sidePad, bottom = bottomPad),
                decorationBox = { inner ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .clip(fieldShape)
                            .background(FieldGray)
                            .padding(
                                start = 18.dp,
                                end = if (expanded) 18.dp else 52.dp,
                                top = 12.dp,
                                bottom = 12.dp
                            ),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        inner()
                    }
                }
            )

            RoundButton(
                icon = Icons.Filled.Add,
                description = "Tambah",
                onClick = onOpenPlus,
                modifier = Modifier.align(Alignment.BottomStart)
            )

            if (expanded) {
                RoundButton(
                    icon = Icons.Outlined.Mic,
                    description = "Suara ke teks",
                    onClick = onStartStt,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            } else {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 60.dp, bottom = 4.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onStartStt),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Mic,
                        contentDescription = "Suara ke teks",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .size(48.dp)
                .clip(CircleShape)
                .background(ActionBlue)
                .clickable(enabled = !menuOpen) {
                    if (hasText) onSendText() else onStartVoice()
                },
            contentAlignment = Alignment.Center
        ) {
            Crossfade(
                targetState = hasText,
                animationSpec = tween(200, easing = FastOutSlowInEasing),
                label = "actionIcon"
            ) { has ->
                Icon(
                    imageVector = if (has) Icons.Filled.ArrowUpward else Icons.Filled.GraphicEq,
                    contentDescription = if (has) "Kirim" else "Pesan suara",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        if (menuOpen) {
            Popup(
                alignment = Alignment.BottomStart,
                onDismissRequest = onCancel,
                properties = PopupProperties(focusable = true)
            ) {
                AnimatedPlusMenu(
                    onCamera = onCamera,
                    onMedia = onMedia,
                    onFile = onFile,
                    onStream = onStream,
                    onClose = onCancel
                )
            }
        }
    }
}

@Composable
private fun AnimatedPlusMenu(
    onCamera: () -> Unit,
    onMedia: () -> Unit,
    onFile: () -> Unit,
    onStream: () -> Unit,
    onClose: () -> Unit
) {
    val enter = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        enter.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
    }

    PlusMenuCard(
        onCamera = onCamera,
        onMedia = onMedia,
        onFile = onFile,
        onStream = onStream,
        onClose = onClose,
        modifier = Modifier.graphicsLayer {
            val s = 0.9f + 0.1f * enter.value
            scaleX = s
            scaleY = s
            transformOrigin = TransformOrigin(0f, 1f)
            alpha = 0.92f * enter.value
        }
    )
}

@Composable
private fun VoiceRecorderBar(
    paused: Boolean,
    onTogglePause: () -> Unit,
    onSend: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(CircleShape)
            .background(ActionBlue)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onTogglePause),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                contentDescription = if (paused) "Lanjutkan" else "Jeda",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        VoiceWaveform(
            running = !paused,
            barColor = Color.White.copy(alpha = 0.5f),
            modifier = Modifier
                .weight(1f)
                .height(32.dp)
                .padding(horizontal = 8.dp)
        )
        Box(
            Modifier
                .size(40.dp)
                .clip(CircleShape)
                .clickable(onClick = onSend),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowUpward,
                contentDescription = "Kirim",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun SttBar(
    text: String,
    onCancel: () -> Unit,
    onStop: () -> Unit,
    onSend: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        if (text.isNotEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(FieldGray)
            ) {
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundButton(
                icon = Icons.Filled.Close,
                description = "Batal",
                onClick = onCancel
            )
            Spacer(Modifier.width(8.dp))
            Row(
                Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(FieldGray)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VoiceWaveform(
                    running = true,
                    barColor = LightGray,
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(LightGray)
                        .clickable(onClick = onStop)
                )
            }
            Spacer(Modifier.width(8.dp))
            RoundButton(
                icon = Icons.Filled.ArrowUpward,
                description = "Kirim",
                onClick = onSend,
                container = ActionBlue
            )
        }
    }
}

@Composable
private fun RoundButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    container: Color = FieldGray
) {
    Box(
        modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(container)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier.size(26.dp)
        )
    }
}
