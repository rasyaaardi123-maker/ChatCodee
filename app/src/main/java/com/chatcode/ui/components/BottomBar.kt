package com.chatcode.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.chatcode.ui.theme.GlassActive
import com.chatcode.ui.theme.GlassBorder
import com.chatcode.ui.theme.GlassFill

private class NavItem(val icon: ImageVector, val label: String)

private val navItems = listOf(
    NavItem(Icons.Outlined.Home, "Home"),
    NavItem(Icons.Outlined.ChatBubbleOutline, "Chat"),
    NavItem(Icons.Outlined.Folder, "Proyek"),
    NavItem(Icons.Outlined.Person, "Akun")
)

@Composable
fun FloatingBottomBar(
    pagePosition: () -> Float,
    onSelect: (Int) -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .weight(1f)
                .height(66.dp)
                .clip(CircleShape)
                .background(GlassFill)
                .border(1.dp, GlassBorder, CircleShape)
                .padding(6.dp)
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(1f / navItems.size)
                    .graphicsLayer { translationX = pagePosition() * size.width }
                    .clip(CircleShape)
                    .background(GlassActive)
            )
            Row(Modifier.fillMaxSize()) {
                navItems.forEachIndexed { index, item ->
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .clickable { onSelect(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.label,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
        Box(
            Modifier
                .size(66.dp)
                .clip(CircleShape)
                .background(GlassFill)
                .border(1.dp, GlassBorder, CircleShape)
                .clickable(onClick = onPlus),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.AddCircleOutline,
                contentDescription = "Buat",
                tint = Color.White,
                modifier = Modifier.size(44.dp)
            )
        }
    }
}
