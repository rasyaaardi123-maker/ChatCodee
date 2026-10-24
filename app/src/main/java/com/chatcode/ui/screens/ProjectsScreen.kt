package com.chatcode.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.LinearProgressIndicator
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
import com.chatcode.data.Project
import com.chatcode.ui.MainViewModel
import com.chatcode.ui.components.EmptyState
import com.chatcode.ui.components.ScreenTitle
import com.chatcode.ui.components.edgeFade
import com.chatcode.ui.theme.TextSecondary

private val CardColor = Color(0xFF151515)

fun statusColor(status: String): Color = when (status) {
    "Selesai" -> Color(0xFF2CB67D)
    "Aktif" -> Color(0xFF3DA9FC)
    "Hampir selesai" -> Color(0xFFFF8906)
    "Ditunda" -> Color(0xFF9E9E9E)
    else -> Color(0xFF7F5AF0)
}

@Composable
fun StatusBadge(status: String) {
    val c = statusColor(status)
    Text(
        text = status,
        color = c,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(CircleShape)
            .background(c.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
fun StackChip(label: String) {
    Text(
        text = label,
        color = Color(0xFFDDDDDD),
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2A2A2A))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
fun ProjectsScreen(vm: MainViewModel, onOpen: (Long) -> Unit) {
    val projects by vm.projects.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        ScreenTitle("Proyek")
        if (projects.isNotEmpty()) {
            Text(
                text = projects.size.toString() + " proyek",
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .edgeFade(top = 16.dp, bottom = 120.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (projects.isEmpty()) {
                item(key = "kosong", contentType = "kosong") {
                    EmptyState(
                        icon = Icons.Outlined.Folder,
                        title = "Belum ada proyek",
                        message = "Proyek codingmu akan tampil di sini beserta progres dan statusnya."
                    )
                }
            }
            items(projects, key = { it.id }, contentType = { "proyek" }) { project ->
                ProjectCard(project) { onOpen(project.id) }
            }
        }
    }
}

@Composable
private fun ProjectCard(project: Project, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardColor)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = project.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            StatusBadge(project.status)
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            project.stack.take(4).forEach { StackChip(it) }
        }
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { project.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape),
            color = statusColor(project.status),
            trackColor = Color(0xFF2A2A2A)
        )
        Spacer(Modifier.height(8.dp))
        Row {
            Text(
                text = (project.progress * 100).toInt().toString() + "%",
                color = Color.White,
                fontSize = 12.sp
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "Diperbarui " + project.updated,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}
