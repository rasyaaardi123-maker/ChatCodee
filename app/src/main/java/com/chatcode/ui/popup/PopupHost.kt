package com.chatcode.ui.popup

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.chatcode.ui.components.edgeFade
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

private val PanelColor = Color(0xFF1A1A1A).copy(alpha = 0.94f)
private val PanelBorder = Color.White.copy(alpha = 0.12f)
private val ScrimColor = Color.Black.copy(alpha = 0.6f)
private val AccentBlue = Color(0xFF1E6FE8)
private val DangerRed = Color(0xFFFF453A)

@Composable
fun PopupHost(controller: PopupController) {
    BackHandler(enabled = controller.hasModal) { controller.dismissTop() }

    Box(Modifier.fillMaxSize()) {
        SheetLayer(controller.sheet) { controller.dismissSheet() }
        SheetLayer(controller.picker) { controller.dismissPicker() }
        MenuLayer(controller)
        CenterCardLayer(controller.dialog, { controller.dismissDialog() }) { spec ->
            DialogContent(spec) { controller.dismissDialog() }
        }
        CenterCardLayer(controller.datePicker, { controller.dismissDatePicker() }) { spec ->
            CustomDatePickerContent(
                initial = spec.initial,
                onCancel = { controller.dismissDatePicker() },
                onPick = { date ->
                    controller.dismissDatePicker()
                    spec.onPicked(date)
                }
            )
        }
        ToastLayer(controller)
    }
}

@Composable
private fun <T : Any> rememberLast(value: T?): T? {
    val holder = remember { arrayOfNulls<Any>(1) }
    if (value != null) holder[0] = value
    @Suppress("UNCHECKED_CAST")
    return (value ?: holder[0]) as T?
}

@Composable
private fun Scrim(onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(ScrimColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
    )
}

@Composable
private fun SheetLayer(spec: SheetSpec?, onDismiss: () -> Unit) {
    val visible = spec != null
    val shown = rememberLast(spec)

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(220, easing = FastOutSlowInEasing)),
            exit = fadeOut(tween(200, easing = FastOutSlowInEasing))
        ) {
            Scrim(onDismiss)
        }
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(tween(260, easing = FastOutSlowInEasing)) { it } +
                fadeIn(tween(220, easing = FastOutSlowInEasing)),
            exit = slideOutVertically(tween(220, easing = FastOutSlowInEasing)) { it } +
                fadeOut(tween(200, easing = FastOutSlowInEasing))
        ) {
            val current = shown
            if (current != null) {
                SheetPanel(current, onDismiss)
            }
        }
    }
}

@Composable
private fun SheetPanel(spec: SheetSpec, onDismiss: () -> Unit) {
    val shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    val sink = remember { MutableInteractionSource() }

    Column(
        Modifier
            .fillMaxWidth()
            .then(
                if (spec.fullScreen) Modifier.fillMaxHeight()
                else Modifier.heightIn(max = 560.dp)
            )
            .clip(shape)
            .background(PanelColor)
            .border(1.dp, PanelBorder, shape)
            .clickable(interactionSource = sink, indication = null) { }
            .then(if (spec.fullScreen) Modifier.statusBarsPadding() else Modifier)
            .imePadding()
            .navigationBarsPadding()
            .padding(top = 10.dp)
    ) {
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .size(width = 36.dp, height = 4.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.25f))
        )
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .weight(1f, fill = spec.fullScreen)
                .fillMaxWidth()
                .edgeFade(top = 0.dp, bottom = 20.dp)
        ) {
            spec.content(onDismiss)
        }
    }
}

@Composable
private fun <T : Any> CenterCardLayer(
    spec: T?,
    onDismiss: () -> Unit,
    content: @Composable (T) -> Unit
) {
    val visible = spec != null
    val shown = rememberLast(spec)
    val sink = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(28.dp)

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(220, easing = FastOutSlowInEasing)),
            exit = fadeOut(tween(180, easing = FastOutSlowInEasing))
        ) {
            Scrim(onDismiss)
        }
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 28.dp),
            enter = fadeIn(tween(220, easing = FastOutSlowInEasing)) +
                scaleIn(tween(220, easing = FastOutSlowInEasing), initialScale = 0.92f),
            exit = fadeOut(tween(180, easing = FastOutSlowInEasing)) +
                scaleOut(tween(180, easing = FastOutSlowInEasing), targetScale = 0.92f)
        ) {
            val current = shown
            if (current != null) {
                Column(
                    Modifier
                        .widthIn(min = 280.dp, max = 340.dp)
                        .clip(shape)
                        .background(PanelColor)
                        .border(1.dp, PanelBorder, shape)
                        .clickable(interactionSource = sink, indication = null) { }
                        .padding(24.dp)
                ) {
                    content(current)
                }
            }
        }
    }
}

@Composable
private fun DialogContent(spec: DialogSpec, onDismiss: () -> Unit) {
    Text(
        text = spec.title,
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(10.dp))
    Text(
        text = spec.message,
        color = Color.White.copy(alpha = 0.75f),
        fontSize = 15.sp,
        lineHeight = 21.sp
    )
    Spacer(Modifier.height(22.dp))
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (spec.dismissLabel != null) {
            PopupTextButton(
                label = spec.dismissLabel,
                color = Color.White.copy(alpha = 0.7f),
                onClick = onDismiss
            )
            Spacer(Modifier.width(8.dp))
        }
        PopupTextButton(
            label = spec.confirmLabel,
            color = if (spec.destructive) DangerRed else AccentBlue,
            onClick = {
                onDismiss()
                spec.onConfirm()
            }
        )
    }
}

@Composable
internal fun PopupTextButton(
    label: String,
    color: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Text(
        text = label,
        color = if (enabled) color else color.copy(alpha = 0.35f),
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

@Composable
private fun MenuLayer(controller: PopupController) {
    val spec = controller.menu
    val visible = spec != null
    val shown = rememberLast(spec)
    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val shape = RoundedCornerShape(18.dp)

    val menuWidthPx = with(density) { 200.dp.toPx() }
    val screenW = with(density) { config.screenWidthDp.dp.toPx() }
    val screenH = with(density) { config.screenHeightDp.dp.toPx() }
    val margin = with(density) { 12.dp.toPx() }
    val count = shown?.items?.size ?: 0
    val heightPx = with(density) { (count * 48 + 16).dp.toPx() }
    val x = (shown?.anchor?.x ?: 0f).coerceIn(margin, max(margin, screenW - menuWidthPx - margin))
    val y = (shown?.anchor?.y ?: 0f).coerceIn(margin, max(margin, screenH - heightPx - margin))

    Box(Modifier.fillMaxSize()) {
        if (visible) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { controller.dismissMenu() }
            )
        }
        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.offset { IntOffset(x.roundToInt(), y.roundToInt()) },
            enter = fadeIn(tween(160, easing = FastOutSlowInEasing)) +
                scaleIn(
                    tween(160, easing = FastOutSlowInEasing),
                    initialScale = 0.9f,
                    transformOrigin = TransformOrigin(0f, 0f)
                ),
            exit = fadeOut(tween(120, easing = FastOutSlowInEasing))
        ) {
            val current = shown
            if (current != null) {
                Column(
                    Modifier
                        .width(200.dp)
                        .clip(shape)
                        .background(PanelColor)
                        .border(1.dp, PanelBorder, shape)
                        .padding(vertical = 8.dp)
                ) {
                    current.items.forEach { item ->
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .clickable {
                                    controller.dismissMenu()
                                    item.onClick()
                                }
                                .padding(horizontal = 18.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = item.label,
                                color = if (item.destructive) DangerRed else Color.White,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToastLayer(controller: PopupController) {
    val spec = controller.toast
    val shown = rememberLast(spec)

    LaunchedEffect(spec?.id) {
        val id = spec?.id
        if (id != null) {
            delay(2400)
            controller.clearToast(id)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 12.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = spec != null,
            enter = fadeIn(tween(200, easing = FastOutSlowInEasing)) +
                slideInVertically(tween(220, easing = FastOutSlowInEasing)) { -it / 2 },
            exit = fadeOut(tween(180, easing = FastOutSlowInEasing)) +
                slideOutVertically(tween(200, easing = FastOutSlowInEasing)) { -it / 2 }
        ) {
            val current = shown
            if (current != null) {
                Text(
                    text = current.message,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .clip(CircleShape)
                        .background(PanelColor)
                        .border(1.dp, PanelBorder, CircleShape)
                        .padding(horizontal = 18.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
internal fun OptionList(
    title: String,
    options: List<String>,
    selected: String?,
    onPick: (String) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .edgeFade(top = 0.dp, bottom = 28.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(options, key = { it }) { option ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onPick(option) }
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = option,
                        color = Color.White,
                        fontSize = 17.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (option == selected) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Terpilih",
                            tint = AccentBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
