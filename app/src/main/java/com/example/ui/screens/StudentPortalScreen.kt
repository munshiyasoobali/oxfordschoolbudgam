package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.SchoolPortalTopBar
import com.example.ui.viewmodel.SchoolUiState
import com.example.ui.viewmodel.SchoolViewModel

private data class StudentTabItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val studentTabs = listOf(
    StudentTabItem("Profile", Icons.Filled.Badge, Icons.Outlined.Badge, "student_tab_profile"),
    StudentTabItem("Attendance", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "student_tab_attendance"),
    StudentTabItem("Fees (Rs)", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet, "student_tab_fees"),
    StudentTabItem("Diary", Icons.Filled.MenuBook, Icons.Outlined.MenuBook, "student_tab_diary")
)

@Composable
fun StudentPortalScreen(
    userDisplayName: String,
    uiState: SchoolUiState,
    viewModel: SchoolViewModel,
    onLogout: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
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

    val student = uiState.activeStudentProfile

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 680.dp

        Scaffold(
            topBar = {
                SchoolPortalTopBar(
                    portalBadge = "STUDENT / PARENT",
                    userName = student?.name ?: userDisplayName,
                    onLogout = onLogout
                )
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        studentTabs.forEachIndexed { index, item ->
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
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail {
                        studentTabs.forEachIndexed { index, item ->
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
                    if (student == null) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else {
                        val attendanceMap = uiState.attendanceByStudent[student.id].orEmpty()
                        val studentFees = remember(uiState.fees, student.id) {
                            uiState.fees.filter { it.studentId == student.id }
                        }

                        when (selectedTab) {
                            0 -> StudentProfileOverviewTab(
                                student = student,
                                attendanceMap = attendanceMap,
                                studentFees = studentFees,
                                diaryEntries = uiState.diaryEntries,
                                completedHomeworkIds = uiState.completedHomeworkIds,
                                onNavigateTab = { selectedTab = it }
                            )
                            1 -> StudentAttendanceTab(
                                student = student,
                                attendanceMap = attendanceMap
                            )
                            2 -> StudentFeesTab(
                                student = student,
                                studentFees = studentFees,
                                paymentAccount = uiState.paymentAccount,
                                onRecordPayment = viewModel::recordStudentFeePayment
                            )
                            3 -> StudentDiaryTab(
                                student = student,
                                diaryEntries = uiState.diaryEntries,
                                completedHomeworkIds = uiState.completedHomeworkIds,
                                onToggleCompleted = viewModel::toggleHomeworkCompleted
                            )
                        }
                    }
                }
            }
        }
    }
}
