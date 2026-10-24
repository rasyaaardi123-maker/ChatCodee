package com.chatcode.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val TextSecondary = Color(0xFF9E9E9E)
val DividerColor = Color(0xFF2A2A2A)
val GlassFill = Color(0xCC3A3A3C)
val GlassBorder = Color(0x33FFFFFF)
val GlassActive = Color(0x55FFFFFF)
val FieldFill = Color(0x66555559)

private val DarkColors = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    secondary = Color(0xFF8AB4F8),
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF121212),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1E1E1E),
    onSurfaceVariant = TextSecondary,
    outline = Color(0xFF3A3A3C)
)

@Composable
fun ChatCodeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
