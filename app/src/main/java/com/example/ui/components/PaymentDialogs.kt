package com.example.ui.components

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.FeeRecord
import com.example.data.SchoolPaymentAccount
import com.example.data.SchoolRepository
import com.example.data.StudentRecord
import com.example.ui.theme.HeritageGold
import com.example.ui.theme.HeritageGoldContainer
import com.example.ui.theme.HeritageGoldOnContainer
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OxfordNavy
import com.example.ui.theme.OxfordNavyContainer
import com.example.ui.theme.StatusAbsentBg
import com.example.ui.theme.StatusAbsentRed
import com.example.ui.theme.StatusPresentGreen

@Composable
fun ConfigureBankAccountDialog(
    currentAccount: SchoolPaymentAccount,
    onDismiss: () -> Unit,
    onSave: (SchoolPaymentAccount) -> Unit
) {
    var holderName by remember { mutableStateOf(currentAccount.accountHolderName) }
    var upiId by remember { mutableStateOf(currentAccount.upiId) }
    var bankName by remember { mutableStateOf(currentAccount.bankName) }
    var accountNumber by remember { mutableStateOf(currentAccount.accountNumber) }
    var ifscCode by remember { mutableStateOf(currentAccount.ifscCode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.AccountBalance, contentDescription = null, tint = OxfordNavy)
                Text("School Settlement Account")
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Enter the real school UPI ID and Bank Account where parent fee payments are settled directly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = holderName,
                    onValueChange = { holderName = it },
                    label = { Text("Account Holder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("bank_holder_input")
                )
                OutlinedTextField(
                    value = upiId,
                    onValueChange = { upiId = it },
                    label = { Text("Official UPI ID / VPA (e.g. school@jkb)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("bank_upi_input")
                )
                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it },
                    label = { Text("Bank Name (e.g. J&K Bank Budgam)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("bank_name_input")
                )
                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { accountNumber = it },
                    label = { Text("Account Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("bank_acc_input")
                )
                OutlinedTextField(
                    value = ifscCode,
                    onValueChange = { ifscCode = it.uppercase() },
                    label = { Text("IFSC Code (e.g. JAKA0BUDGAM)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("bank_ifsc_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        SchoolPaymentAccount(
                            accountHolderName = holderName,
                            upiId = upiId,
                            bankName = bankName,
                            accountNumber = accountNumber,
                            ifscCode = ifscCode
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = OxfordNavy),
                modifier = Modifier.testTag("save_bank_account_button")
            ) {
                Text("Save Account")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun PayFeeAccountDialog(
    fee: FeeRecord,
    student: StudentRecord,
    account: SchoolPaymentAccount,
    onDismiss: () -> Unit,
    onPaymentConfirmed: (paymentMethod: String, transactionRef: String, payerAccountNote: String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var transferMode by remember { mutableStateOf("UPI App") }
    var utrReference by remember { mutableStateOf("") }
    var payerAccountNote by remember { mutableStateOf("") }
    var localFeedback by remember { mutableStateOf<String?>(null) }

    val upiLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val response = result.data?.getStringExtra("response")
        val (success, txnId) = SchoolRepository.parseUpiResponse(response)
        if (result.resultCode == Activity.RESULT_OK && success) {
            val finalRef = txnId.ifBlank { "UPI-${System.currentTimeMillis() % 1000000}" }
            onPaymentConfirmed(
                "UPI App (${account.upiId})",
                finalRef,
                payerAccountNote.ifBlank { "Direct UPI App Payment" }
            )
        } else {
            localFeedback = if (response.isNullOrBlank()) {
                "UPI payment was cancelled or returned no status. If debited, enter your 12-digit UTR below."
            } else {
                "UPI app status was not confirmed ($response). Enter UTR below if payment succeeded."
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OxfordNavy)
                        .padding(18.dp)
                ) {
                    Text(
                        text = "DIRECT TO SCHOOL ACCOUNT",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = HeritageGold,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Pay ${fee.monthLabel} Fee",
                        style = MaterialTheme.typography.titleLarge.copy(color = Color.White)
                    )
                    Text(
                        text = "${student.name} (${student.rollNo}) • ${SchoolRepository.formatRs(fee.totalAmount)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = JetBrainsMonoFontFamily,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    )
                }

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!account.isConfigured) {
                        Surface(
                            color = StatusAbsentBg,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, StatusAbsentRed.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(Icons.Filled.Info, contentDescription = null, tint = StatusAbsentRed, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "School Bank / UPI account is not configured yet. Please ask School Management to set the real UPI ID & Bank Account in Admin → Fees (Rs).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = StatusAbsentRed
                                )
                            }
                        }
                    } else {
                        // Real School Account Card
                        Surface(
                            color = HeritageGoldContainer.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, HeritageGold.copy(alpha = 0.6f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = account.accountHolderName,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        color = HeritageGoldOnContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                if (account.hasUpiConfigured) {
                                    CopyableAccountRow(
                                        label = "UPI ID",
                                        value = account.upiId,
                                        onCopy = {
                                            clipboardManager.setText(AnnotatedString(account.upiId))
                                            localFeedback = "Copied UPI ID: ${account.upiId}"
                                        }
                                    )
                                }
                                if (account.hasBankConfigured) {
                                    CopyableAccountRow(
                                        label = "Bank",
                                        value = account.bankName,
                                        onCopy = null
                                    )
                                    CopyableAccountRow(
                                        label = "A/C No",
                                        value = account.accountNumber,
                                        onCopy = {
                                            clipboardManager.setText(AnnotatedString(account.accountNumber))
                                            localFeedback = "Copied Account Number"
                                        }
                                    )
                                    CopyableAccountRow(
                                        label = "IFSC",
                                        value = account.ifscCode,
                                        onCopy = {
                                            clipboardManager.setText(AnnotatedString(account.ifscCode))
                                            localFeedback = "Copied IFSC: ${account.ifscCode}"
                                        }
                                    )
                                }
                            }
                        }

                        // Real UPI App Launcher
                        if (account.hasUpiConfigured) {
                            Button(
                                onClick = {
                                    val uri = SchoolRepository.buildUpiPaymentUri(account, fee, student)
                                    val upiIntent = Intent(Intent.ACTION_VIEW, uri)
                                    val chooser = Intent.createChooser(upiIntent, "Pay ${SchoolRepository.formatRs(fee.totalAmount)} via UPI App")
                                    try {
                                        upiLauncher.launch(chooser)
                                    } catch (_: ActivityNotFoundException) {
                                        localFeedback = "No UPI app found on this device. Copy the UPI ID or Bank details above and enter your UTR below."
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = StatusPresentGreen),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("launch_upi_app_button")
                            ) {
                                Icon(Icons.Filled.QrCode2, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Pay ${SchoolRepository.formatRs(fee.totalAmount)} via UPI App", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (!localFeedback.isNullOrBlank()) {
                        Surface(
                            color = OxfordNavyContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = localFeedback!!,
                                style = MaterialTheme.typography.bodySmall,
                                color = OxfordNavy,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    HorizontalDivider()

                    // Manual Bank / UPI Reference Submission
                    Text(
                        text = "Already transferred via UPI / Bank? Submit UTR",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("UPI App", "NEFT / IMPS", "Bank Transfer").forEach { mode ->
                            FilterChip(
                                selected = transferMode == mode,
                                onClick = { transferMode = mode },
                                label = { Text(mode, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = utrReference,
                        onValueChange = { utrReference = it },
                        label = { Text("UTR / UPI Transaction ID (min 6 chars)") },
                        leadingIcon = { Icon(Icons.Filled.Payments, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("utr_reference_input")
                    )

                    OutlinedTextField(
                        value = payerAccountNote,
                        onValueChange = { payerAccountNote = it },
                        label = { Text("Sender UPI ID / Bank A/C Last 4 (optional)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("payer_account_note_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                onPaymentConfirmed(transferMode, utrReference, payerAccountNote)
                            },
                            enabled = utrReference.trim().length >= 6,
                            colors = ButtonDefaults.buttonColors(containerColor = OxfordNavy),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("confirm_utr_button")
                        ) {
                            Text("Confirm Paid")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CopyableAccountRow(
    label: String,
    value: String,
    onCopy: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = JetBrainsMonoFontFamily,
                color = HeritageGoldOnContainer,
                fontWeight = FontWeight.Medium
            ),
            modifier = Modifier.weight(1f)
        )
        if (onCopy != null) {
            IconButton(
                onClick = onCopy,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy $label",
                    tint = HeritageGoldOnContainer,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
