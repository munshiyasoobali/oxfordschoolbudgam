package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.AttendanceStatus
import com.example.data.FeeRecord
import com.example.data.FeeStatus
import com.example.data.SchoolRepository
import com.example.data.StudentRecord
import com.example.ui.theme.HeritageGold
import com.example.ui.theme.HeritageGoldContainer
import com.example.ui.theme.HeritageGoldOnContainer
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OxfordNavy
import com.example.ui.theme.StatusAbsentBg
import com.example.ui.theme.StatusAbsentRed
import com.example.ui.theme.StatusLeaveAmber
import com.example.ui.theme.StatusLeaveBg
import com.example.ui.theme.StatusPresentBg
import com.example.ui.theme.StatusPresentGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolPortalTopBar(
    portalBadge: String,
    userName: String,
    onLogout: () -> Unit
) {
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = OxfordNavy,
            titleContentColor = Color.White,
            actionIconContentColor = Color.White
        ),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_launcher_crest),
                    contentDescription = "Oxford School Budgam Crest",
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, HeritageGold, CircleShape),
                    contentScale = ContentScale.Crop
                )
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "OXFORD SCHOOL EDUCATION BUDGAM",
                        style = MaterialTheme.typography.titleSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = HeritageGold,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = portalBadge,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = OxfordNavy,
                                    fontWeight = FontWeight.Bold
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                        Text(
                            text = userName,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White.copy(alpha = 0.85f)
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        actions = {
            IconButton(
                onClick = onLogout,
                modifier = Modifier.testTag("logout_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Sign Out"
                )
            }
        }
    )
}

@Composable
fun MetricSummaryCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(containerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun FeeStatusBadge(status: FeeStatus) {
    val (bg, fg) = when (status) {
        FeeStatus.PAID -> StatusPresentBg to StatusPresentGreen
        FeeStatus.PENDING -> StatusLeaveBg to StatusLeaveAmber
        FeeStatus.OVERDUE -> StatusAbsentBg to StatusAbsentRed
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.3f))
    ) {
        Text(
            text = status.label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                color = fg,
                fontWeight = FontWeight.Bold
            ),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun AttendanceColorSpec(status: AttendanceStatus): Pair<Color, Color> = when (status) {
    AttendanceStatus.PRESENT -> StatusPresentBg to StatusPresentGreen
    AttendanceStatus.ABSENT -> StatusAbsentBg to StatusAbsentRed
    AttendanceStatus.LEAVE -> StatusLeaveBg to StatusLeaveAmber
}

@Composable
fun StudentInitialsAvatar(name: String, modifier: Modifier = Modifier) {
    val initials = name.trim()
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
        .ifBlank { "ST" }
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(OxfordNavy)
            .border(1.5.dp, HeritageGold, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleSmall.copy(
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        )
    }
}

@Composable
fun DigitalFeeReceiptDialog(
    feeRecord: FeeRecord,
    student: StudentRecord,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Navy & Gold Receipt Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OxfordNavy)
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ReceiptLong,
                            contentDescription = null,
                            tint = HeritageGold
                        )
                        Text(
                            text = "OFFICIAL FEE RECEIPT",
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = HeritageGold,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "OXFORD SCHOOL EDUCATION BUDGAM",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Budgam, Jammu & Kashmir • Academic Session 2026",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    )
                }

                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Receipt No", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = feeRecord.receiptNumber.ifBlank { "OXB-RCP-MANUAL" },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Surface(
                            color = StatusPresentBg,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, StatusPresentGreen)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Verified,
                                    contentDescription = null,
                                    tint = StatusPresentGreen,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "PAID",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = StatusPresentGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }

                    HorizontalDivider()

                    ReceiptRow("Student Name", student.name)
                    ReceiptRow("Roll Number", student.rollNo)
                    ReceiptRow("Class / Grade", student.grade)
                    ReceiptRow("Parent / Guardian", student.parentName)
                    ReceiptRow("Billing Month", feeRecord.monthLabel)
                    ReceiptRow("Payment Date", feeRecord.paidDate.ifBlank { SchoolRepository.todayFormatted() })
                    ReceiptRow("Payment Mode", feeRecord.paymentMethod)
                    if (feeRecord.transactionRef.isNotBlank()) {
                        ReceiptRow("UTR / Txn Ref", feeRecord.transactionRef, mono = true)
                    }
                    if (feeRecord.payerAccountNote.isNotBlank()) {
                        ReceiptRow("Payer Account", feeRecord.payerAccountNote)
                    }

                    HorizontalDivider()

                    ReceiptRow("Tuition Fee", SchoolRepository.formatRs(feeRecord.tuitionFee), mono = true)
                    ReceiptRow("Transport Fee", SchoolRepository.formatRs(feeRecord.transportFee), mono = true)
                    if (feeRecord.examFee > 0) {
                        ReceiptRow("Exam Fee", SchoolRepository.formatRs(feeRecord.examFee), mono = true)
                    }

                    Surface(
                        color = HeritageGoldContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TOTAL PAID",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = HeritageGoldOnContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = SchoolRepository.formatRs(feeRecord.totalAmount),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    color = HeritageGoldOnContainer,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { shareReceiptText(context, feeRecord, student) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("share_receipt_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Print,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share / Print")
                        }
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("close_receipt_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = OxfordNavy)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Done")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptRow(label: String, value: String, mono: Boolean = false) {
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
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = if (mono) JetBrainsMonoFontFamily else MaterialTheme.typography.bodySmall.fontFamily,
                fontWeight = FontWeight.SemiBold
            )
        )
    }
}

private fun shareReceiptText(context: Context, fee: FeeRecord, student: StudentRecord) {
    val receiptText = buildString {
        appendLine("OXFORD SCHOOL EDUCATION BUDGAM")
        appendLine("OFFICIAL FEE RECEIPT")
        appendLine("--------------------------------")
        appendLine("Receipt No: ${fee.receiptNumber}")
        appendLine("Student: ${student.name} (${student.rollNo})")
        appendLine("Class: ${student.grade}")
        appendLine("Parent: ${student.parentName}")
        appendLine("Month: ${fee.monthLabel}")
        appendLine("Paid Date: ${fee.paidDate}")
        appendLine("Payment Mode: ${fee.paymentMethod}")
        if (fee.transactionRef.isNotBlank()) {
            appendLine("UTR / Txn Ref: ${fee.transactionRef}")
        }
        if (fee.payerAccountNote.isNotBlank()) {
            appendLine("Payer Account: ${fee.payerAccountNote}")
        }
        appendLine("--------------------------------")
        appendLine("Tuition Fee:   ${SchoolRepository.formatRs(fee.tuitionFee)}")
        appendLine("Transport Fee: ${SchoolRepository.formatRs(fee.transportFee)}")
        if (fee.examFee > 0) {
            appendLine("Exam Fee:      ${SchoolRepository.formatRs(fee.examFee)}")
        }
        appendLine("TOTAL PAID:    ${SchoolRepository.formatRs(fee.totalAmount)}")
        appendLine("Status: PAID")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Fee Receipt - ${student.name} (${fee.monthLabel})")
        putExtra(Intent.EXTRA_TEXT, receiptText)
    }
    context.startActivity(Intent.createChooser(intent, "Print or Share Fee Receipt"))
}
