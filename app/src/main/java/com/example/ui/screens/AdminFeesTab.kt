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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.FeeRecord
import com.example.data.FeeStatus
import com.example.data.SchoolPaymentAccount
import com.example.data.SchoolRepository
import com.example.data.StudentRecord
import com.example.ui.components.ConfigureBankAccountDialog
import com.example.ui.components.DigitalFeeReceiptDialog
import com.example.ui.components.FeeStatusBadge
import com.example.ui.components.MetricSummaryCard
import com.example.ui.theme.HeritageGold
import com.example.ui.theme.HeritageGoldContainer
import com.example.ui.theme.HeritageGoldOnContainer
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OxfordNavy
import com.example.ui.theme.StatusAbsentBg
import com.example.ui.theme.StatusAbsentRed
import com.example.ui.theme.StatusPresentBg
import com.example.ui.theme.StatusPresentGreen

@Composable
fun AdminFeesTab(
    students: List<StudentRecord>,
    fees: List<FeeRecord>,
    paymentAccount: SchoolPaymentAccount,
    totalCollectedRs: Int,
    totalPendingDuesRs: Int,
    onSavePaymentAccount: (SchoolPaymentAccount) -> Unit,
    onToggleFeeStatus: (FeeRecord) -> Unit,
    onDeleteFee: (String) -> Unit
) {
    var statusFilter by remember { mutableStateOf("All") }
    var receiptToShow by remember { mutableStateOf<Pair<FeeRecord, StudentRecord>?>(null) }
    var showAccountConfigDialog by remember { mutableStateOf(false) }

    val studentById = remember(students) { students.associateBy { it.id } }

    val filteredFees = remember(fees, statusFilter) {
        when (statusFilter) {
            "Paid" -> fees.filter { FeeStatus.fromKey(it.status) == FeeStatus.PAID }
            "Pending" -> fees.filter { FeeStatus.fromKey(it.status) == FeeStatus.PENDING }
            "Overdue" -> fees.filter { FeeStatus.fromKey(it.status) == FeeStatus.OVERDUE }
            else -> fees
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 92.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricSummaryCard(
                    title = "Total Collected (Rs)",
                    value = SchoolRepository.formatRs(totalCollectedRs),
                    subtitle = "${fees.count { FeeStatus.fromKey(it.status) == FeeStatus.PAID }} Paid Invoices",
                    icon = Icons.Filled.AccountBalanceWallet,
                    accentColor = StatusPresentGreen,
                    containerColor = StatusPresentBg,
                    modifier = Modifier.weight(1f)
                )
                MetricSummaryCard(
                    title = "Total Pending Dues (Rs)",
                    value = SchoolRepository.formatRs(totalPendingDuesRs),
                    subtitle = "${fees.count { FeeStatus.fromKey(it.status) != FeeStatus.PAID }} Unpaid Invoices",
                    icon = Icons.Filled.HourglassTop,
                    accentColor = StatusAbsentRed,
                    containerColor = StatusAbsentBg,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // School Real Settlement Account Configuration Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = HeritageGoldContainer.copy(alpha = 0.6f)),
                border = BorderStroke(1.dp, HeritageGold.copy(alpha = 0.65f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccountBalance,
                        contentDescription = null,
                        tint = HeritageGoldOnContainer
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Real Settlement Account (UPI / Bank)",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = HeritageGoldOnContainer,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        val summary = if (paymentAccount.isConfigured) {
                            buildString {
                                if (paymentAccount.hasUpiConfigured) append("UPI: ${paymentAccount.upiId}")
                                if (paymentAccount.hasBankConfigured) {
                                    if (isNotEmpty()) append(" • ")
                                    append("${paymentAccount.bankName} (${paymentAccount.ifscCode})")
                                }
                            }
                        } else {
                            "Not configured yet — tap Configure to enable direct UPI & Bank transfers."
                        }
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = HeritageGoldOnContainer
                        )
                    }
                    OutlinedButton(
                        onClick = { showAccountConfigDialog = true },
                        border = BorderStroke(1.dp, HeritageGoldOnContainer),
                        modifier = Modifier.testTag("configure_bank_account_button")
                    ) {
                        Text(
                            text = if (paymentAccount.isConfigured) "Edit" else "Configure",
                            color = HeritageGoldOnContainer
                        )
                    }
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("All", "Paid", "Pending", "Overdue")) { filter ->
                    FilterChip(
                        selected = statusFilter == filter,
                        onClick = { statusFilter = filter },
                        label = { Text(filter) }
                    )
                }
            }
        }

        if (filteredFees.isEmpty()) {
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
                        Text("No fee invoices in this category", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Tap + Generate Invoice to bill a student in Indian Rupees (Rs).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredFees, key = { it.id }) { fee ->
                val student = studentById[fee.studentId]
                val status = FeeStatus.fromKey(fee.status)
                val isPaid = status == FeeStatus.PAID

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("fee_card_${fee.id}"),
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = student?.name ?: "Student #${fee.studentId}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "${student?.rollNo ?: ""} • ${student?.grade ?: ""} • ${fee.monthLabel}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FeeStatusBadge(status)
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Tuition ${SchoolRepository.formatRs(fee.tuitionFee)} • Transport ${SchoolRepository.formatRs(fee.transportFee)}" +
                                    if (fee.examFee > 0) " • Exam ${SchoolRepository.formatRs(fee.examFee)}" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = SchoolRepository.formatRs(fee.totalAmount),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = OxfordNavy,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        if (isPaid && fee.transactionRef.isNotBlank()) {
                            val payerSuffix = if (fee.payerAccountNote.isNotBlank()) " • From: ${fee.payerAccountNote}" else ""
                            Text(
                                text = "Paid via ${fee.paymentMethod} • Ref: ${fee.transactionRef}$payerSuffix",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = StatusPresentGreen
                                )
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onToggleFeeStatus(fee) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPaid) MaterialTheme.colorScheme.surfaceVariant else StatusPresentGreen,
                                    contentColor = if (isPaid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("toggle_fee_${fee.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isPaid) "Mark Pending" else "Mark Paid")
                            }

                            if (isPaid && student != null) {
                                OutlinedButton(
                                    onClick = { receiptToShow = fee to student },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("admin_receipt_${fee.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ReceiptLong,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Receipt")
                                }
                            }

                            IconButton(onClick = { onDeleteFee(fee.id) }) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = "Delete Invoice",
                                    tint = StatusAbsentRed
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    receiptToShow?.let { (fee, student) ->
        DigitalFeeReceiptDialog(
            feeRecord = fee,
            student = student,
            onDismiss = { receiptToShow = null }
        )
    }

    if (showAccountConfigDialog) {
        ConfigureBankAccountDialog(
            currentAccount = paymentAccount,
            onDismiss = { showAccountConfigDialog = false },
            onSave = { updated ->
                onSavePaymentAccount(updated)
                showAccountConfigDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateFeeInvoiceDialog(
    students: List<StudentRecord>,
    onDismiss: () -> Unit,
    onCreate: (student: StudentRecord, monthLabel: String, tuition: Int, transport: Int, exam: Int, status: FeeStatus) -> Unit
) {
    var selectedStudent by remember { mutableStateOf(students.firstOrNull()) }
    var studentMenuExpanded by remember { mutableStateOf(false) }
    var monthLabel by remember { mutableStateOf("December 2026") }
    var tuitionText by remember { mutableStateOf("2500") }
    var transportText by remember { mutableStateOf("800") }
    var examText by remember { mutableStateOf("0") }
    var selectedStatus by remember { mutableStateOf(FeeStatus.PENDING) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Generate Fee Invoice (Rs)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(
                    expanded = studentMenuExpanded,
                    onExpandedChange = { studentMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedStudent?.let { "${it.name} (${it.rollNo})" } ?: "Select Student",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Student") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = studentMenuExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = studentMenuExpanded,
                        onDismissRequest = { studentMenuExpanded = false }
                    ) {
                        students.forEach { st ->
                            DropdownMenuItem(
                                text = { Text("${st.name} (${st.rollNo} • ${st.grade})") },
                                onClick = {
                                    selectedStudent = st
                                    studentMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = monthLabel,
                    onValueChange = { monthLabel = it },
                    label = { Text("Billing Month (e.g. December 2026)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = tuitionText,
                        onValueChange = { tuitionText = it.filter(Char::isDigit) },
                        label = { Text("Tuition (Rs)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = transportText,
                        onValueChange = { transportText = it.filter(Char::isDigit) },
                        label = { Text("Transport (Rs)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = examText,
                    onValueChange = { examText = it.filter(Char::isDigit) },
                    label = { Text("Exam Fee (Rs)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FeeStatus.entries.forEach { st ->
                        FilterChip(
                            selected = selectedStatus == st,
                            onClick = { selectedStatus = st },
                            label = { Text(st.label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = selectedStudent ?: return@Button
                    onCreate(
                        target,
                        monthLabel,
                        tuitionText.toIntOrNull() ?: 0,
                        transportText.toIntOrNull() ?: 0,
                        examText.toIntOrNull() ?: 0,
                        selectedStatus
                    )
                },
                enabled = selectedStudent != null && monthLabel.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = OxfordNavy),
                modifier = Modifier.testTag("dialog_fee_create")
            ) {
                Text("Create Invoice")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
