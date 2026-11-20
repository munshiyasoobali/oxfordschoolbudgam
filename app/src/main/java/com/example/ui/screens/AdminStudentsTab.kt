package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AttendanceStatus
import com.example.data.SchoolRepository
import com.example.data.StudentRecord
import com.example.ui.components.MetricSummaryCard
import com.example.ui.components.StudentInitialsAvatar
import com.example.ui.theme.HeritageGoldContainer
import com.example.ui.theme.HeritageGoldOnContainer
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OxfordNavy
import com.example.ui.theme.OxfordNavyContainer
import com.example.ui.theme.StatusAbsentBg
import com.example.ui.theme.StatusAbsentRed
import com.example.ui.theme.StatusPresentBg
import com.example.ui.theme.StatusPresentGreen

@Composable
fun AdminStudentsTab(
    students: List<StudentRecord>,
    attendanceByStudent: Map<String, Map<Int, AttendanceStatus>>,
    totalCollectedRs: Int,
    totalPendingRs: Int,
    onEditStudent: (StudentRecord) -> Unit,
    onDeleteStudent: (StudentRecord) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedGrade by remember { mutableStateOf("All") }
    var studentPendingDelete by remember { mutableStateOf<StudentRecord?>(null) }

    val grades = remember(students) {
        listOf("All") + students.map { it.grade }.distinct().sorted()
    }

    val filteredStudents = remember(students, searchQuery, selectedGrade) {
        students.filter { student ->
            val matchesGrade = selectedGrade == "All" || student.grade == selectedGrade
            val q = searchQuery.trim().lowercase()
            val matchesQuery = q.isBlank() ||
                student.name.lowercase().contains(q) ||
                student.rollNo.lowercase().contains(q) ||
                student.parentName.lowercase().contains(q)
            matchesGrade && matchesQuery
        }
    }

    val avgAttendancePct = remember(students, attendanceByStudent) {
        if (students.isEmpty()) 0 else {
            val totalPresent = students.sumOf { st ->
                attendanceByStudent[st.id]?.values?.count { it == AttendanceStatus.PRESENT } ?: 0
            }
            ((totalPresent * 100f) / (students.size * 30f)).toInt()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // KPI Dashboard Row 1
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricSummaryCard(
                    title = "Enrolled Students",
                    value = students.size.toString(),
                    subtitle = "${grades.size - 1} Active Classes",
                    icon = Icons.Filled.Groups,
                    accentColor = OxfordNavy,
                    containerColor = OxfordNavyContainer,
                    modifier = Modifier.weight(1f)
                )
                MetricSummaryCard(
                    title = "30-Day Avg Attendance",
                    value = "$avgAttendancePct%",
                    subtitle = "Across all classes",
                    icon = Icons.Filled.VerifiedUser,
                    accentColor = StatusPresentGreen,
                    containerColor = StatusPresentBg,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // KPI Dashboard Row 2 (Rs Revenue)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricSummaryCard(
                    title = "Fees Collected",
                    value = SchoolRepository.formatRs(totalCollectedRs),
                    subtitle = "Paid invoices",
                    icon = Icons.Filled.AccountBalanceWallet,
                    accentColor = StatusPresentGreen,
                    containerColor = StatusPresentBg,
                    modifier = Modifier.weight(1f)
                )
                MetricSummaryCard(
                    title = "Pending Dues",
                    value = SchoolRepository.formatRs(totalPendingRs),
                    subtitle = "Pending & overdue",
                    icon = Icons.Filled.HourglassTop,
                    accentColor = StatusAbsentRed,
                    containerColor = StatusAbsentBg,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Search & Filter
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search student, roll no, or parent...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_student_search")
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(grades) { grade ->
                    FilterChip(
                        selected = selectedGrade == grade,
                        onClick = { selectedGrade = grade },
                        label = { Text(grade) }
                    )
                }
            }
        }

        if (filteredStudents.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No matching students found", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Tap + Add Student to enroll a new student in Oxford School Education Budgam.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredStudents, key = { it.id }) { student ->
                val presentDays = attendanceByStudent[student.id]?.values?.count {
                    it == AttendanceStatus.PRESENT
                } ?: 0
                val pct = ((presentDays / 30f) * 100).toInt()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_card_${student.rollNo}"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StudentInitialsAvatar(name = student.name)
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = student.name,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Surface(
                                        color = HeritageGoldContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = student.rollNo,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = JetBrainsMonoFontFamily,
                                                color = HeritageGoldOnContainer,
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "${student.grade} • Attendance: $presentDays/30 ($pct%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { onEditStudent(student) },
                                modifier = Modifier.testTag("edit_student_${student.rollNo}")
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit Student", tint = OxfordNavy)
                            }
                            IconButton(
                                onClick = { studentPendingDelete = student },
                                modifier = Modifier.testTag("delete_student_${student.rollNo}")
                            ) {
                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove Student", tint = StatusAbsentRed)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Parent: ${student.parentName}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Phone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = student.contactNumber,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFontFamily
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    studentPendingDelete?.let { student ->
        AlertDialog(
            onDismissRequest = { studentPendingDelete = null },
            title = { Text("Remove Student?") },
            text = { Text("Remove ${student.name} (${student.rollNo}) and their attendance/fee records?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteStudent(student)
                        studentPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusAbsentRed)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { studentPendingDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StudentEditorDialog(
    initialStudent: StudentRecord?,
    onDismiss: () -> Unit,
    onSave: (name: String, grade: String, rollNo: String, parentName: String, contactNumber: String) -> Unit
) {
    var name by remember { mutableStateOf(initialStudent?.name ?: "") }
    var grade by remember { mutableStateOf(initialStudent?.grade ?: "Class 10-A") }
    var rollNo by remember { mutableStateOf(initialStudent?.rollNo ?: "") }
    var parentName by remember { mutableStateOf(initialStudent?.parentName ?: "") }
    var contactNumber by remember { mutableStateOf(initialStudent?.contactNumber ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialStudent == null) "Enroll New Student" else "Edit Student Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Student Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_student_name")
                )
                OutlinedTextField(
                    value = grade,
                    onValueChange = { grade = it },
                    label = { Text("Class / Grade (e.g. Class 10-A)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_student_grade")
                )
                OutlinedTextField(
                    value = rollNo,
                    onValueChange = { rollNo = it },
                    label = { Text("Roll Number (e.g. OXB-108)") },
                    enabled = initialStudent == null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_student_roll")
                )
                OutlinedTextField(
                    value = parentName,
                    onValueChange = { parentName = it },
                    label = { Text("Parent / Guardian Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_student_parent")
                )
                OutlinedTextField(
                    value = contactNumber,
                    onValueChange = { contactNumber = it },
                    label = { Text("Contact Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("dialog_student_phone")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, grade, rollNo, parentName, contactNumber) },
                enabled = name.isNotBlank() && grade.isNotBlank() && rollNo.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = OxfordNavy),
                modifier = Modifier.testTag("dialog_student_save")
            ) {
                Text(if (initialStudent == null) "Add Student" else "Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
