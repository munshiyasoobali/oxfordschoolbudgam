package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AttendanceStatus
import com.example.data.DiaryEntry
import com.example.data.FeeRecord
import com.example.data.FeeStatus
import com.example.data.SchoolPaymentAccount
import com.example.data.SchoolRepository
import com.example.data.StudentRecord
import com.example.ui.components.AttendanceColorSpec
import com.example.ui.components.DigitalFeeReceiptDialog
import com.example.ui.components.FeeStatusBadge
import com.example.ui.components.MetricSummaryCard
import com.example.ui.components.PayFeeAccountDialog
import com.example.ui.components.StudentInitialsAvatar
import com.example.ui.theme.HeritageGold
import com.example.ui.theme.HeritageGoldContainer
import com.example.ui.theme.HeritageGoldOnContainer
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OxfordNavy
import com.example.ui.theme.OxfordNavyContainer
import com.example.ui.theme.StatusAbsentBg
import com.example.ui.theme.StatusAbsentRed
import com.example.ui.theme.StatusLeaveAmber
import com.example.ui.theme.StatusLeaveBg
import com.example.ui.theme.StatusPresentBg
import com.example.ui.theme.StatusPresentGreen

@Composable
fun StudentProfileOverviewTab(
    student: StudentRecord,
    attendanceMap: Map<Int, AttendanceStatus>,
    studentFees: List<FeeRecord>,
    diaryEntries: List<DiaryEntry>,
    completedHomeworkIds: Set<String>,
    onNavigateTab: (Int) -> Unit
) {
    val presentDays = (1..30).count { (attendanceMap[it] ?: AttendanceStatus.PRESENT) == AttendanceStatus.PRESENT }
    val attendancePct = ((presentDays / 30f) * 100).toInt()
    val pendingDues = studentFees.filter { FeeStatus.fromKey(it.status) != FeeStatus.PAID }.sumOf { it.totalAmount }
    val relevantDiary = diaryEntries.filter {
        it.targetClass == "All Classes" || it.targetClass.equals(student.grade, ignoreCase = true)
    }
    val homeworkCount = relevantDiary.count { !it.isNoticeEntry }
    val completedCount = relevantDiary.count { !it.isNoticeEntry && it.id in completedHomeworkIds }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Prestigious Student ID / Personal Profile Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("student_profile_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                border = BorderStroke(1.dp, HeritageGold.copy(alpha = 0.55f))
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(108.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_school_banner),
                            contentDescription = "Oxford School Budgam",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            OxfordNavy.copy(alpha = 0.92f),
                                            OxfordNavy.copy(alpha = 0.65f)
                                        )
                                    )
                                )
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_launcher_crest),
                                contentDescription = "Crest",
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, HeritageGold, CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Column {
                                Text(
                                    text = "OXFORD SCHOOL EDUCATION BUDGAM",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = HeritageGold,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "OFFICIAL STUDENT IDENTITY CARD",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StudentInitialsAvatar(name = student.name, modifier = Modifier.size(54.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = student.name,
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Text(
                                    text = "${student.grade} • Session ${student.admissionYear}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = HeritageGoldContainer,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, HeritageGold)
                            ) {
                                Text(
                                    text = student.rollNo,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        color = HeritageGoldOnContainer,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(16.dp), tint = OxfordNavy)
                                Text(
                                    text = "Parent: ${student.parentName}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.Phone, contentDescription = null, modifier = Modifier.size(16.dp), tint = OxfordNavy)
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

        // Quick Summary Metrics
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricSummaryCard(
                    title = "30-Day Attendance",
                    value = "$attendancePct%",
                    subtitle = "$presentDays of 30 days present",
                    icon = Icons.Filled.CalendarMonth,
                    accentColor = if (attendancePct >= 75) StatusPresentGreen else StatusAbsentRed,
                    containerColor = if (attendancePct >= 75) StatusPresentBg else StatusAbsentBg,
                    modifier = Modifier.weight(1f)
                )
                MetricSummaryCard(
                    title = "Pending Fee Dues",
                    value = SchoolRepository.formatRs(pendingDues),
                    subtitle = if (pendingDues == 0) "All dues cleared" else "Tap Fees tab to view",
                    icon = Icons.Filled.AccountBalanceWallet,
                    accentColor = if (pendingDues == 0) StatusPresentGreen else StatusAbsentRed,
                    containerColor = if (pendingDues == 0) StatusPresentBg else StatusAbsentBg,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Homework Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.MenuBook, contentDescription = null, tint = OxfordNavy)
                            Text("Daily Diary & Notices", style = MaterialTheme.typography.titleMedium)
                        }
                        Text(
                            text = "$completedCount / $homeworkCount Done",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                color = OxfordNavy
                            )
                        )
                    }
                    LinearProgressIndicator(
                        progress = {
                            if (homeworkCount == 0) 1f else completedCount.toFloat() / homeworkCount.toFloat()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = HeritageGold,
                        trackColor = OxfordNavyContainer
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { onNavigateTab(1) },
                            colors = ButtonDefaults.buttonColors(containerColor = OxfordNavy),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("30-Day Grid")
                        }
                        Button(
                            onClick = { onNavigateTab(3) },
                            colors = ButtonDefaults.buttonColors(containerColor = HeritageGold, contentColor = OxfordNavy),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Open Diary", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StudentAttendanceTab(
    student: StudentRecord,
    attendanceMap: Map<Int, AttendanceStatus>
) {
    val presentCount = (1..30).count { (attendanceMap[it] ?: AttendanceStatus.PRESENT) == AttendanceStatus.PRESENT }
    val absentCount = (1..30).count { attendanceMap[it] == AttendanceStatus.ABSENT }
    val leaveCount = (1..30).count { attendanceMap[it] == AttendanceStatus.LEAVE }
    val pct = ((presentCount / 30f) * 100).toInt()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = OxfordNavy)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "30-Day Attendance Summary • ${student.grade}",
                        style = MaterialTheme.typography.titleSmall.copy(color = HeritageGold)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "$pct%",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontFamily = JetBrainsMonoFontFamily,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "$presentCount Present • $absentCount Absent • $leaveCount Leave",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.88f)
                            )
                        )
                    }
                    LinearProgressIndicator(
                        progress = { presentCount / 30f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = HeritageGold,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }
            }
        }

        // Read-only 30-day calendar grid (5 rows x 6 columns)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("student_attendance_grid"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Monthly Calendar (Days 1–30)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    for (rowIndex in 0 until 5) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (colIndex in 0 until 6) {
                                val day = rowIndex * 6 + colIndex + 1
                                val status = attendanceMap[day] ?: AttendanceStatus.PRESENT
                                val (bg, fg) = AttendanceColorSpec(status)
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(bg)
                                        .border(1.dp, fg.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                        .padding(vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "$day",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = fg.copy(alpha = 0.85f)
                                        )
                                    )
                                    Text(
                                        text = status.code,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            color = fg,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Text("P = Present", style = MaterialTheme.typography.labelSmall, color = StatusPresentGreen, fontWeight = FontWeight.Bold)
                        Text("A = Absent", style = MaterialTheme.typography.labelSmall, color = StatusAbsentRed, fontWeight = FontWeight.Bold)
                        Text("L = Leave", style = MaterialTheme.typography.labelSmall, color = StatusLeaveAmber, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StudentFeesTab(
    student: StudentRecord,
    studentFees: List<FeeRecord>,
    paymentAccount: SchoolPaymentAccount,
    onRecordPayment: (fee: FeeRecord, paymentMethod: String, transactionRef: String, payerAccountNote: String) -> Unit
) {
    var receiptToPrint by remember { mutableStateOf<FeeRecord?>(null) }
    var feeToPay by remember { mutableStateOf<FeeRecord?>(null) }

    val totalPaid = studentFees.filter { FeeStatus.fromKey(it.status) == FeeStatus.PAID }.sumOf { it.totalAmount }
    val totalPending = studentFees.filter { FeeStatus.fromKey(it.status) != FeeStatus.PAID }.sumOf { it.totalAmount }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricSummaryCard(
                    title = "Total Paid (Rs)",
                    value = SchoolRepository.formatRs(totalPaid),
                    subtitle = "${studentFees.count { FeeStatus.fromKey(it.status) == FeeStatus.PAID }} Receipts",
                    icon = Icons.Filled.CheckCircle,
                    accentColor = StatusPresentGreen,
                    containerColor = StatusPresentBg,
                    modifier = Modifier.weight(1f)
                )
                MetricSummaryCard(
                    title = "Pending Dues (Rs)",
                    value = SchoolRepository.formatRs(totalPending),
                    subtitle = "${studentFees.count { FeeStatus.fromKey(it.status) != FeeStatus.PAID }} Due",
                    icon = Icons.Filled.HourglassTop,
                    accentColor = StatusAbsentRed,
                    containerColor = StatusAbsentBg,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (studentFees.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No fee records yet", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        } else {
            items(studentFees, key = { it.id }) { fee ->
                val status = FeeStatus.fromKey(fee.status)
                val isPaid = status == FeeStatus.PAID
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(fee.monthLabel, style = MaterialTheme.typography.titleMedium)
                                if (isPaid && fee.paidDate.isNotBlank()) {
                                    Text(
                                        text = "Paid on ${fee.paidDate} • ${fee.receiptNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = StatusPresentGreen
                                    )
                                }
                            }
                            FeeStatusBadge(status)
                        }

                        HorizontalDivider()

                        FeeBreakdownRow("Tuition Fee", fee.tuitionFee)
                        FeeBreakdownRow("Transport Fee", fee.transportFee)
                        if (fee.examFee > 0) {
                            FeeBreakdownRow("Exam Fee", fee.examFee)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Monthly Total", style = MaterialTheme.typography.labelLarge)
                            Text(
                                text = SchoolRepository.formatRs(fee.totalAmount),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = OxfordNavy,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        if (isPaid) {
                            if (fee.transactionRef.isNotBlank()) {
                                Text(
                                    text = "UTR / Txn Ref: ${fee.transactionRef}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                            Button(
                                onClick = { receiptToPrint = fee },
                                colors = ButtonDefaults.buttonColors(containerColor = OxfordNavy),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("student_view_receipt_${fee.id}")
                            ) {
                                Icon(Icons.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("View / Print Digital Receipt")
                            }
                        } else {
                            Button(
                                onClick = { feeToPay = fee },
                                colors = ButtonDefaults.buttonColors(containerColor = StatusPresentGreen),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("student_pay_fee_${fee.id}")
                            ) {
                                Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pay Now (${SchoolRepository.formatRs(fee.totalAmount)}) • UPI / Bank")
                            }
                        }
                    }
                }
            }
        }
    }

    receiptToPrint?.let { fee ->
        DigitalFeeReceiptDialog(
            feeRecord = fee,
            student = student,
            onDismiss = { receiptToPrint = null }
        )
    }

    feeToPay?.let { fee ->
        PayFeeAccountDialog(
            fee = fee,
            student = student,
            account = paymentAccount,
            onDismiss = { feeToPay = null },
            onPaymentConfirmed = { method, ref, payerNote ->
                onRecordPayment(fee, method, ref, payerNote)
                feeToPay = null
            }
        )
    }
}

@Composable
private fun FeeBreakdownRow(label: String, amount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = SchoolRepository.formatRs(amount),
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
fun StudentDiaryTab(
    student: StudentRecord,
    diaryEntries: List<DiaryEntry>,
    completedHomeworkIds: Set<String>,
    onToggleCompleted: (diaryId: String, completed: Boolean) -> Unit
) {
    val relevantEntries = remember(diaryEntries, student.grade) {
        diaryEntries.filter {
            it.targetClass == "All Classes" || it.targetClass.equals(student.grade, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (relevantEntries.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.Badge, contentDescription = null, tint = OxfordNavy, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No diary entries for ${student.grade}", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        } else {
            items(relevantEntries, key = { it.id }) { entry ->
                val isCompleted = entry.id in completedHomeworkIds
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(
                        1.dp,
                        if (isCompleted) StatusPresentGreen.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = if (entry.isNoticeEntry) HeritageGoldContainer else OxfordNavyContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (entry.isNoticeEntry) Icons.Filled.Campaign else Icons.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = if (entry.isNoticeEntry) HeritageGoldOnContainer else OxfordNavy,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "${entry.subject} • ${entry.targetClass}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (entry.isNoticeEntry) HeritageGoldOnContainer else OxfordNavy,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                            Text(
                                text = entry.dateLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(entry.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = entry.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (entry.teacherRemarks.isNotBlank()) {
                            Surface(
                                color = HeritageGoldContainer.copy(alpha = 0.45f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Teacher Remark: ${entry.teacherRemarks}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = HeritageGoldOnContainer,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp)
                                )
                            }
                        }

                        if (!entry.isNoticeEntry) {
                            HorizontalDivider()
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isCompleted) "Homework Completed" else "Mark homework as completed",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = if (isCompleted) StatusPresentGreen else MaterialTheme.colorScheme.onSurface
                                    )
                                )
                                Checkbox(
                                    checked = isCompleted,
                                    onCheckedChange = { checked -> onToggleCompleted(entry.id, checked) },
                                    colors = CheckboxDefaults.colors(checkedColor = StatusPresentGreen),
                                    modifier = Modifier.testTag("homework_checkbox_${entry.id}")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
