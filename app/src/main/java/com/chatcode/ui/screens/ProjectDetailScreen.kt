package com.chatcode.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.chatcode.ui.MainViewModel
import com.chatcode.ui.components.DetailTopBar
import com.chatcode.ui.theme.TextSecondary

@Composable
fun ProjectDetailScreen(vm: MainViewModel, projectId: Long, onBack: () -> Unit) {
    val projects by vm.projects.collectAsStateWithLifecycle()
    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()
    val project = projects.firstOrNull { it.id == projectId } ?: return

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        DetailTopBar(project.name, onBack)
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(vScroll)
                .padding(horizontal = 16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(project.status)
                Spacer(Modifier.weight(1f))
                Text(
                    text = "Diperbarui " + project.updated,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                project.stack.take(5).forEach { StackChip(it) }
            }
            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { project.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = statusColor(project.status),
                trackColor = Color(0xFF2A2A2A)
            )
            Text(
                text = "Progres " + (project.progress * 100).toInt().toString() + "%",
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 6.dp)
            )

            SectionTitle("Deskripsi")
            Text(text = project.description, color = Color(0xFFDDDDDD), fontSize = 15.sp)

            SectionTitle("File")
            project.files.forEach { file ->
                Row(
                    Modifier.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = file,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    )
                }
            }

            SectionTitle("Kode")
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0D1117))
                    .horizontalScroll(hScroll)
                    .padding(14.dp)
            ) {
                Text(
                    text = project.code,
                    color = Color(0xFFE6EDF3),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    softWrap = false
                )
            }
            Spacer(Modifier.navigationBarsPadding().height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        color = Color.White,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
    )
}
