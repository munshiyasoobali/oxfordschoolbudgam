package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.AttendanceStatus
import com.example.data.StudentRecord
import com.example.ui.components.AttendanceColorSpec
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OxfordNavy
import com.example.ui.theme.StatusAbsentBg
import com.example.ui.theme.StatusAbsentRed
import com.example.ui.theme.StatusLeaveAmber
import com.example.ui.theme.StatusLeaveBg
import com.example.ui.theme.StatusPresentBg
import com.example.ui.theme.StatusPresentGreen

@Composable
fun AdminAttendanceTab(
    students: List<StudentRecord>,
    attendanceByStudent: Map<String, Map<Int, AttendanceStatus>>,
    onCycleAttendance: (studentId: String, dayOfMonth: Int) -> Unit,
    onMarkAllPresentForDay: (dayOfMonth: Int) -> Unit
) {
    val sharedHorizontalScroll = rememberScrollState()
    var selectedQuickDay by remember { mutableIntStateOf(20) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header & Legend Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "30-Day Attendance Register",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Tap any day cell to cycle: P → A → L",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    FilledTonalButton(
                        onClick = { onMarkAllPresentForDay(selectedQuickDay) },
                        modifier = Modifier.testTag("mark_all_present_button")
                    ) {
                        Icon(Icons.Filled.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("All P (Day $selectedQuickDay)")
                    }
                }

                // Color Legend
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    LegendChip("P • Present", StatusPresentBg, StatusPresentGreen)
                    LegendChip("A • Absent", StatusAbsentBg, StatusAbsentRed)
                    LegendChip("L • Leave", StatusLeaveBg, StatusLeaveAmber)
                }
            }
        }

        // Grid Header Row (Student column pinned left + Days 1..30 scrollable right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(OxfordNavy)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(142.dp)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = "Student / Total",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(sharedHorizontalScroll),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (day in 1..30) {
                    val isSelected = day == selectedQuickDay
                    Box(
                        modifier = Modifier
                            .size(width = 40.dp, height = 32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) Color.White.copy(alpha = 0.25f) else Color.Transparent)
                            .clickable { selectedQuickDay = day },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "D$day",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
            }
        }

        // Student Attendance Rows
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(students, key = { it.id }) { student ->
                val dayMap = attendanceByStudent[student.id].orEmpty()
                val presentCount = (1..30).count { day ->
                    (dayMap[day] ?: AttendanceStatus.PRESENT) == AttendanceStatus.PRESENT
                }
                val pct = ((presentCount / 30f) * 100).toInt()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pinned Student Column
                        Column(
                            modifier = Modifier
                                .width(134.dp)
                                .padding(horizontal = 10.dp)
                        ) {
                            Text(
                                text = student.name,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${student.rollNo} • $presentCount/30 ($pct%)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = if (pct >= 75) StatusPresentGreen else StatusAbsentRed,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Synchronized 30-Day Grid
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(sharedHorizontalScroll),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (day in 1..30) {
                                val status = dayMap[day] ?: AttendanceStatus.PRESENT
                                val (bg, fg) = AttendanceColorSpec(status)
                                Box(
                                    modifier = Modifier
                                        .size(width = 40.dp, height = 44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(bg)
                                        .border(1.dp, fg.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                        .clickable { onCycleAttendance(student.id, day) }
                                        .testTag("att_cell_${student.rollNo}_$day"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = status.code,
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            color = fg,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                        }
                    }
                }
                HorizontalDivider(color = Color.Transparent, modifier = Modifier.height(1.dp))
            }
        }
    }
}

@Composable
private fun LegendChip(label: String, bg: Color, fg: Color) {
    Surface(
        color = bg,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.35f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = fg,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
