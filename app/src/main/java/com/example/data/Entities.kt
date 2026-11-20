package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class PortalIntent(val displayName: String) {
    PARENT_STUDENT("Parent / Student Portal"),
    MANAGEMENT("School Management Portal")
}

enum class AttendanceStatus(val code: String, val label: String) {
    PRESENT("P", "Present"),
    ABSENT("A", "Absent"),
    LEAVE("L", "Leave");

    fun next(): AttendanceStatus = when (this) {
        PRESENT -> ABSENT
        ABSENT -> LEAVE
        LEAVE -> PRESENT
    }

    companion object {
        fun fromCode(code: String?): AttendanceStatus =
            entries.firstOrNull { it.code == code } ?: PRESENT
    }
}

enum class FeeStatus(val key: String, val label: String) {
    PAID("PAID", "Paid"),
    PENDING("PENDING", "Pending"),
    OVERDUE("OVERDUE", "Overdue");

    companion object {
        fun fromKey(key: String?): FeeStatus =
            entries.firstOrNull { it.key == key } ?: PENDING
    }
}

@Keep
@IgnoreExtraProperties
data class ManagementConfig(
    @DocumentId val id: String = "",
    val adminUid: String = "",
    val maskedEmail: String = "",
    val adminName: String = "",
    @ServerTimestamp val claimedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SchoolPaymentAccount(
    @DocumentId val id: String = "",
    val accountHolderName: String = "OXFORD SCHOOL EDUCATION BUDGAM",
    val upiId: String = "",
    val bankName: String = "",
    val accountNumber: String = "",
    val ifscCode: String = "",
    val updatedByUid: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val hasUpiConfigured: Boolean
        get() = upiId.isNotBlank() && upiId.contains("@")

    @get:Exclude
    val hasBankConfigured: Boolean
        get() = bankName.isNotBlank() && accountNumber.isNotBlank() && ifscCode.isNotBlank()

    @get:Exclude
    val isConfigured: Boolean
        get() = hasUpiConfigured || hasBankConfigured
}

@Keep
@IgnoreExtraProperties
data class UserLinkProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val studentId: String = "",
    val rollNo: String = "",
    val displayName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class StudentRecord(
    @DocumentId val id: String = "",
    val name: String = "",
    val grade: String = "",
    val rollNo: String = "",
    val parentName: String = "",
    val contactNumber: String = "",
    val admissionYear: String = "2026",
    val parentUid: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AttendanceRecord(
    @DocumentId val id: String = "",
    val studentId: String = "",
    val parentUid: String = "",
    val days: Map<String, String> = emptyMap(),
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class FeeRecord(
    @DocumentId val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val rollNo: String = "",
    val grade: String = "",
    val parentUid: String = "",
    val monthLabel: String = "",
    val tuitionFee: Int = 0,
    val transportFee: Int = 0,
    val examFee: Int = 0,
    val totalAmount: Int = 0,
    val status: String = FeeStatus.PENDING.key,
    val receiptNumber: String = "",
    val paidDate: String = "",
    val paymentMethod: String = "School Counter",
    val transactionRef: String = "",
    val payerAccountNote: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DiaryEntry(
    @DocumentId val id: String = "",
    val title: String = "",
    val subject: String = "",
    val description: String = "",
    val targetClass: String = "",
    val dateLabel: String = "",
    val teacherRemarks: String = "",
    val entryType: String = "HOMEWORK",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val isNoticeEntry: Boolean
        get() = entryType.equals("NOTICE", ignoreCase = true)
}

@Keep
@IgnoreExtraProperties
data class HomeworkCompletion(
    @DocumentId val id: String = "",
    val diaryId: String = "",
    val completed: Boolean = false,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
