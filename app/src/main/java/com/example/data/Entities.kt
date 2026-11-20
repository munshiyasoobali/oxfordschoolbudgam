package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class UserRole(val storageKey: String, val displayName: String) {
    ADMIN("ADMIN", "School Management"),
    STUDENT_PARENT("STUDENT_PARENT", "Student / Parent");

    companion object {
        fun fromKey(key: String): UserRole =
            entries.firstOrNull { it.storageKey == key } ?: STUDENT_PARENT
    }
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
        fun fromCode(code: String): AttendanceStatus =
            entries.firstOrNull { it.code == code } ?: PRESENT
    }
}

enum class FeeStatus(val key: String, val label: String) {
    PAID("PAID", "Paid"),
    PENDING("PENDING", "Pending"),
    OVERDUE("OVERDUE", "Overdue");

    companion object {
        fun fromKey(key: String): FeeStatus =
            entries.firstOrNull { it.key == key } ?: PENDING
    }
}

@Entity(
    tableName = "user_accounts",
    indices = [Index(value = ["contactIdentifier"], unique = true)]
)
data class UserAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val fullName: String,
    val role: String,
    val rollNumber: String = "",
    val contactIdentifier: String,
    val password: String
)

@Entity(
    tableName = "students",
    indices = [Index(value = ["rollNo"], unique = true)]
)
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val grade: String,
    val rollNo: String,
    val parentName: String,
    val contactNumber: String,
    val admissionYear: String = "2026"
)

@Entity(
    tableName = "attendance",
    primaryKeys = ["studentId", "dayOfMonth"],
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["studentId"])]
)
data class AttendanceEntity(
    val studentId: Long,
    val dayOfMonth: Int,
    val status: String
)

@Entity(
    tableName = "fee_records",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["studentId"])]
)
data class FeeRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val studentId: Long,
    val monthLabel: String,
    val tuitionFee: Int,
    val transportFee: Int,
    val examFee: Int,
    val status: String,
    val receiptNumber: String = "",
    val paidDate: String = "",
    val paymentMethod: String = "School Counter",
    val transactionRef: String = ""
) {
    val totalAmount: Int get() = tuitionFee + transportFee + examFee
}

@Entity(tableName = "diary_entries")
data class DiaryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subject: String,
    val description: String,
    val targetClass: String,
    val dateLabel: String,
    val teacherRemarks: String = "",
    val isNotice: Boolean = false
)

@Entity(
    tableName = "homework_completions",
    primaryKeys = ["diaryId", "studentId"],
    foreignKeys = [
        ForeignKey(
            entity = DiaryEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["diaryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["studentId"])]
)
data class HomeworkCompletionEntity(
    val diaryId: Long,
    val studentId: Long,
    val completed: Boolean
)
