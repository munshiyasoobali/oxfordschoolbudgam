package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.StudentRecord
import com.example.ui.components.SchoolPortalTopBar
import com.example.ui.theme.HeritageGold
import com.example.ui.theme.OxfordNavy
import com.example.ui.viewmodel.SchoolUiState
import com.example.ui.viewmodel.SchoolViewModel

private data class PortalTabItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val adminTabs = listOf(
    PortalTabItem("Students", Icons.Filled.Groups, Icons.Outlined.Groups, "admin_tab_students"),
    PortalTabItem("Attendance", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "admin_tab_attendance"),
    PortalTabItem("Fees (Rs)", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "admin_tab_fees"),
    PortalTabItem("Diary", Icons.Filled.MenuBook, Icons.Outlined.MenuBook, "admin_tab_diary")
)

@Composable
fun AdminPortalScreen(
    adminDisplayName: String,
    uiState: SchoolUiState,
    viewModel: SchoolViewModel,
    onLogout: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showStudentDialog by remember { mutableStateOf(false) }
    var editingStudent by remember { mutableStateOf<StudentRecord?>(null) }
    var showFeeDialog by remember { mutableStateOf(false) }
    var showDiaryDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusMessage, uiState.errorMessage) {
        val msg = uiState.errorMessage ?: uiState.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearFeedback()
        }
    }

    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 680.dp

        Scaffold(
            topBar = {
                SchoolPortalTopBar(
                    portalBadge = "MANAGEMENT",
                    userName = adminDisplayName,
                    onLogout = onLogout
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        adminTabs.forEachIndexed { index, item ->
                            NavigationBarItem(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == index) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                when (selectedTab) {
                    0 -> ExtendedFloatingActionButton(
                        onClick = {
                            editingStudent = null
                            showStudentDialog = true
                        },
                        containerColor = OxfordNavy,
                        contentColor = Color.White,
                        icon = { Icon(Icons.Filled.Add, contentDescription = null, tint = HeritageGold) },
                        text = { Text("Add Student") },
                        modifier = Modifier.testTag("fab_add_student")
                    )
                    2 -> ExtendedFloatingActionButton(
                        onClick = { showFeeDialog = true },
                        containerColor = OxfordNavy,
                        contentColor = Color.White,
                        icon = { Icon(Icons.Filled.Add, contentDescription = null, tint = HeritageGold) },
                        text = { Text("New Invoice") },
                        modifier = Modifier.testTag("fab_add_fee")
                    )
                    3 -> ExtendedFloatingActionButton(
                        onClick = { showDiaryDialog = true },
                        containerColor = OxfordNavy,
                        contentColor = Color.White,
                        icon = { Icon(Icons.Filled.Add, contentDescription = null, tint = HeritageGold) },
                        text = { Text("Publish Entry") },
                        modifier = Modifier.testTag("fab_add_diary")
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail {
                        adminTabs.forEachIndexed { index, item ->
                            NavigationRailItem(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedTab == index) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (selectedTab) {
                        0 -> AdminStudentsTab(
                            students = uiState.students,
                            attendanceByStudent = uiState.attendanceByStudent,
                            totalCollectedRs = uiState.totalCollectedRs,
                            totalPendingRs = uiState.totalPendingDuesRs,
                            onEditStudent = { st ->
                                editingStudent = st
                                showStudentDialog = true
                            },
                            onDeleteStudent = viewModel::deleteStudent
                        )
                        1 -> AdminAttendanceTab(
                            students = uiState.students,
                            attendanceByStudent = uiState.attendanceByStudent,
                            onCycleAttendance = viewModel::cycleAttendance,
                            onMarkAllPresentForDay = viewModel::markAllPresentForDay
                        )
                        2 -> AdminFeesTab(
                            students = uiState.students,
                            fees = uiState.fees,
                            paymentAccount = uiState.paymentAccount,
                            totalCollectedRs = uiState.totalCollectedRs,
                            totalPendingDuesRs = uiState.totalPendingDuesRs,
                            onSavePaymentAccount = viewModel::savePaymentAccount,
                            onToggleFeeStatus = viewModel::toggleFeeStatus,
                            onDeleteFee = viewModel::deleteFeeRecord
                        )
                        3 -> AdminDiaryTab(
                            diaryEntries = uiState.diaryEntries,
                            onDeleteEntry = viewModel::deleteDiaryEntry
                        )
                    }
                }
            }
        }
    }

    if (showStudentDialog) {
        StudentEditorDialog(
            initialStudent = editingStudent,
            onDismiss = { showStudentDialog = false },
            onSave = { name, grade, rollNo, parentName, contactNumber ->
                val existing = editingStudent
                if (existing == null) {
                    viewModel.addStudent(name, grade, rollNo, parentName, contactNumber)
                } else {
                    viewModel.updateStudent(
                        existing.copy(
                            name = name.trim(),
                            grade = grade.trim(),
                            parentName = parentName.trim(),
                            contactNumber = contactNumber.trim()
                        )
                    )
                }
                showStudentDialog = false
            }
        )
    }

    if (showFeeDialog) {
        GenerateFeeInvoiceDialog(
            students = uiState.students,
            onDismiss = { showFeeDialog = false },
            onCreate = { student, month, tuition, transport, exam, status ->
                viewModel.addFeeInvoice(student, month, tuition, transport, exam, status)
                showFeeDialog = false
            }
        )
    }

    if (showDiaryDialog) {
        PublishDiaryDialog(
            availableClasses = uiState.students.map { it.grade }.distinct().sorted(),
            onDismiss = { showDiaryDialog = false },
            onPublish = { title, subject, desc, targetClass, dateLabel, remarks, isNotice ->
                viewModel.publishDiaryEntry(title, subject, desc, targetClass, dateLabel, remarks, isNotice)
                showDiaryDialog = false
            }
        )
    }
}
