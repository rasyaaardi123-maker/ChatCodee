package com.chatcode.ui.popup

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle as MonthTextStyle
import java.util.Locale

private val DayLabels = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
private val PickerBlue = Color(0xFF1E6FE8)

@Composable
fun CustomDatePickerContent(
    initial: LocalDate?,
    onCancel: () -> Unit,
    onPick: (LocalDate) -> Unit
) {
    val today = remember { LocalDate.now() }
    val locale = remember { Locale("id", "ID") }
    var month by remember { mutableStateOf(YearMonth.from(initial ?: today)) }
    var selected by remember { mutableStateOf(initial) }

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MonthArrow(Icons.Filled.ChevronLeft, "Bulan sebelumnya") {
                month = month.minusMonths(1)
            }
            Text(
                text = month.month.getDisplayName(MonthTextStyle.FULL, locale)
                    .replaceFirstChar { it.uppercase() } + " " + month.year,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            MonthArrow(Icons.Filled.ChevronRight, "Bulan berikutnya") {
                month = month.plusMonths(1)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            DayLabels.forEach { label ->
                Text(
                    text = label,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Spacer(Modifier.height(6.dp))

        val firstOffset = month.atDay(1).dayOfWeek.value - 1
        val days = month.lengthOfMonth()
        val rows = (firstOffset + days + 6) / 7

        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (c in 0 until 7) {
                    val day = r * 7 + c - firstOffset + 1
                    Box(
                        Modifier
                            .weight(1f)
                            .height(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day in 1..days) {
                            val date = month.atDay(day)
                            val isSelected = date == selected
                            val isToday = date == today
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) PickerBlue else Color.Transparent)
                                    .then(
                                        if (isToday && !isSelected) {
                                            Modifier.border(
                                                1.dp,
                                                Color.White.copy(alpha = 0.4f),
                                                CircleShape
                                            )
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable { selected = date },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.toString(),
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PopupTextButton(
                label = "Batal",
                color = Color.White.copy(alpha = 0.7f),
                onClick = onCancel
            )
            Spacer(Modifier.width(8.dp))
            PopupTextButton(
                label = "Pilih",
                color = PickerBlue,
                enabled = selected != null,
                onClick = {
                    val date = selected
                    if (date != null) onPick(date)
                }
            )
        }
    }
}

@Composable
private fun MonthArrow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
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
