package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SchoolRepository(
    private val dao: SchoolDao,
    private val sessionPrefs: SessionPreferences
) {
    private val seedMutex = Mutex()

    val studentsFlow: Flow<List<StudentEntity>> = dao.observeStudents()
    val attendanceFlow: Flow<List<AttendanceEntity>> = dao.observeAllAttendance()
    val feesFlow: Flow<List<FeeRecordEntity>> = dao.observeAllFees()
    val diaryFlow: Flow<List<DiaryEntryEntity>> = dao.observeDiaryEntries()
    val loggedInUserIdFlow: Flow<Long?> = sessionPrefs.loggedInUserId
    val paymentAccountFlow: Flow<SchoolPaymentAccount> = sessionPrefs.paymentAccount

    suspend fun savePaymentAccount(account: SchoolPaymentAccount): Result<Unit> {
        if (account.upiId.isNotBlank() && !account.upiId.contains("@")) {
            return Result.failure(IllegalArgumentException("Enter a valid UPI ID containing '@' (e.g. school@jkb)."))
        }
        if (account.accountNumber.isNotBlank() && account.ifscCode.isBlank()) {
            return Result.failure(IllegalArgumentException("IFSC Code is required when entering a Bank Account Number."))
        }
        sessionPrefs.savePaymentAccount(account)
        return Result.success(Unit)
    }

    suspend fun recordStudentFeePayment(
        fee: FeeRecordEntity,
        paymentMethod: String,
        transactionRef: String
    ): Result<Unit> {
        val cleanRef = transactionRef.trim()
        if (cleanRef.length < 6) {
            return Result.failure(IllegalArgumentException("Please enter a valid UPI Txn ID or Bank UTR reference (6+ characters)."))
        }
        dao.updateFeeRecord(
            fee.copy(
                status = FeeStatus.PAID.key,
                receiptNumber = fee.receiptNumber.ifBlank { generateReceiptNumber() },
                paidDate = todayFormatted(),
                paymentMethod = paymentMethod.trim().ifBlank { "UPI / Bank Transfer" },
                transactionRef = cleanRef
            )
        )
        return Result.success(Unit)
    }

    fun observeHomeworkCompletions(studentId: Long): Flow<List<HomeworkCompletionEntity>> =
        dao.observeHomeworkCompletions(studentId)

    suspend fun ensureSeeded() {
        seedMutex.withLock {
            if (dao.countStudents() == 0) {
                SeedData.defaultUsers.forEach { user ->
                    if (dao.findUserByIdentifier(user.contactIdentifier) == null) {
                        dao.insertUser(user)
                    }
                }
                val studentIds = dao.insertStudents(SeedData.defaultStudents)
                dao.insertAttendanceBatch(SeedData.generate30DayAttendance(studentIds))
                dao.insertFeeRecords(SeedData.generateFeeRecords(studentIds))
                dao.insertDiaryEntries(SeedData.defaultDiaryEntries)
            }
        }
    }

    suspend fun getSavedSessionUser(): UserAccountEntity? {
        ensureSeeded()
        val id = sessionPrefs.loggedInUserId.first() ?: return null
        return dao.getUserById(id)
    }

    suspend fun login(identifier: String, password: String): Result<UserAccountEntity> {
        ensureSeeded()
        val trimmedId = identifier.trim()
        val user = dao.authenticateUser(trimmedId, password)
            ?: return Result.failure(IllegalArgumentException("Invalid email/phone or password."))
        sessionPrefs.setLoggedInUserId(user.id)
        return Result.success(user)
    }

    suspend fun demoLogin(role: UserRole): Result<UserAccountEntity> {
        ensureSeeded()
        val id = if (role == UserRole.ADMIN) SeedData.DEMO_ADMIN_EMAIL else SeedData.DEMO_STUDENT_EMAIL
        val pass = if (role == UserRole.ADMIN) SeedData.DEMO_ADMIN_PASS else SeedData.DEMO_STUDENT_PASS
        return login(id, pass)
    }

    suspend fun signUp(
        fullName: String,
        role: UserRole,
        rollNumber: String,
        identifier: String,
        password: String
    ): Result<UserAccountEntity> {
        ensureSeeded()
        val cleanName = fullName.trim()
        val cleanId = identifier.trim()
        val cleanRoll = rollNumber.trim().uppercase(Locale.ROOT)
        if (cleanName.isBlank() || cleanId.isBlank() || password.length < 4) {
            return Result.failure(IllegalArgumentException("Please enter name, email/phone, and password (4+ chars)."))
        }
        if (role == UserRole.STUDENT_PARENT && cleanRoll.isBlank()) {
            return Result.failure(IllegalArgumentException("Roll Number / Admission ID is required for students."))
        }
        if (dao.findUserByIdentifier(cleanId) != null) {
            return Result.failure(IllegalArgumentException("An account with this email/phone already exists."))
        }

        // If student/parent signs up with a brand-new Roll No, auto-provision a student profile so their portal is rich & functional immediately
        if (role == UserRole.STUDENT_PARENT && dao.findStudentByRollNo(cleanRoll) == null) {
            val newStudentId = dao.insertStudent(
                StudentEntity(
                    name = cleanName,
                    grade = "Class 10-A",
                    rollNo = cleanRoll,
                    parentName = "Guardian of $cleanName",
                    contactNumber = cleanId,
                    admissionYear = "2026"
                )
            )
            val defaultAttendance = (1..30).map { day ->
                AttendanceEntity(studentId = newStudentId, dayOfMonth = day, status = AttendanceStatus.PRESENT.code)
            }
            dao.insertAttendanceBatch(defaultAttendance)
            dao.insertFeeRecord(
                FeeRecordEntity(
                    studentId = newStudentId,
                    monthLabel = "November 2026",
                    tuitionFee = 2500,
                    transportFee = 750,
                    examFee = 0,
                    status = FeeStatus.PENDING.key
                )
            )
        }

        val newUserId = dao.insertUser(
            UserAccountEntity(
                fullName = cleanName,
                role = role.storageKey,
                rollNumber = if (role == UserRole.STUDENT_PARENT) cleanRoll else "",
                contactIdentifier = cleanId,
                password = password
            )
        )
        val created = dao.getUserById(newUserId)!!
        sessionPrefs.setLoggedInUserId(created.id)
        return Result.success(created)
    }

    suspend fun logout() {
        sessionPrefs.setLoggedInUserId(null)
    }

    suspend fun findStudentByRollNo(rollNo: String): StudentEntity? =
        dao.findStudentByRollNo(rollNo)

    suspend fun addStudent(
        name: String,
        grade: String,
        rollNo: String,
        parentName: String,
        contactNumber: String
    ): Result<Unit> {
        val cleanRoll = rollNo.trim().uppercase(Locale.ROOT)
        if (name.isBlank() || grade.isBlank() || cleanRoll.isBlank()) {
            return Result.failure(IllegalArgumentException("Name, Class, and Roll No are required."))
        }
        if (dao.findStudentByRollNo(cleanRoll) != null) {
            return Result.failure(IllegalArgumentException("Roll No $cleanRoll already exists."))
        }
        val id = dao.insertStudent(
            StudentEntity(
                name = name.trim(),
                grade = grade.trim(),
                rollNo = cleanRoll,
                parentName = parentName.trim().ifBlank { "Parent / Guardian" },
                contactNumber = contactNumber.trim().ifBlank { "Not provided" }
            )
        )
        val initialAttendance = (1..30).map { day ->
            AttendanceEntity(studentId = id, dayOfMonth = day, status = AttendanceStatus.PRESENT.code)
        }
        dao.insertAttendanceBatch(initialAttendance)
        return Result.success(Unit)
    }

    suspend fun updateStudent(student: StudentEntity) {
        dao.updateStudent(student)
    }

    suspend fun deleteStudent(studentId: Long) {
        dao.deleteStudentById(studentId)
    }

    suspend fun setAttendanceStatus(studentId: Long, dayOfMonth: Int, status: AttendanceStatus) {
        dao.upsertAttendance(
            AttendanceEntity(studentId = studentId, dayOfMonth = dayOfMonth, status = status.code)
        )
    }

    suspend fun markAllStudentsForDay(studentIds: List<Long>, dayOfMonth: Int, status: AttendanceStatus) {
        val records = studentIds.map { studentId ->
            AttendanceEntity(studentId = studentId, dayOfMonth = dayOfMonth, status = status.code)
        }
        dao.insertAttendanceBatch(records)
    }

    suspend fun addFeeInvoice(
        studentId: Long,
        monthLabel: String,
        tuitionFee: Int,
        transportFee: Int,
        examFee: Int,
        status: FeeStatus
    ) {
        val isPaid = status == FeeStatus.PAID
        val receiptNo = if (isPaid) generateReceiptNumber() else ""
        val paidDate = if (isPaid) todayFormatted() else ""
        dao.insertFeeRecord(
            FeeRecordEntity(
                studentId = studentId,
                monthLabel = monthLabel.trim(),
                tuitionFee = tuitionFee,
                transportFee = transportFee,
                examFee = examFee,
                status = status.key,
                receiptNumber = receiptNo,
                paidDate = paidDate,
                paymentMethod = "School Counter"
            )
        )
    }

    suspend fun toggleFeePaymentStatus(record: FeeRecordEntity) {
        val current = FeeStatus.fromKey(record.status)
        val updated = if (current == FeeStatus.PAID) {
            record.copy(
                status = FeeStatus.PENDING.key,
                receiptNumber = "",
                paidDate = ""
            )
        } else {
            record.copy(
                status = FeeStatus.PAID.key,
                receiptNumber = record.receiptNumber.ifBlank { generateReceiptNumber() },
                paidDate = record.paidDate.ifBlank { todayFormatted() },
                paymentMethod = record.paymentMethod.ifBlank { "School Counter" }
            )
        }
        dao.updateFeeRecord(updated)
    }

    suspend fun deleteFeeRecord(feeId: Long) {
        dao.deleteFeeById(feeId)
    }

    suspend fun publishDiaryEntry(
        title: String,
        subject: String,
        description: String,
        targetClass: String,
        dateLabel: String,
        teacherRemarks: String,
        isNotice: Boolean
    ) {
        dao.insertDiaryEntry(
            DiaryEntryEntity(
                title = title.trim(),
                subject = subject.trim().ifBlank { if (isNotice) "School Notice" else "General" },
                description = description.trim(),
                targetClass = targetClass.trim().ifBlank { "All Classes" },
                dateLabel = dateLabel.trim().ifBlank { todayFormatted() },
                teacherRemarks = teacherRemarks.trim(),
                isNotice = isNotice
            )
        )
    }

    suspend fun deleteDiaryEntry(diaryId: Long) {
        dao.deleteDiaryById(diaryId)
    }

    suspend fun setHomeworkCompleted(diaryId: Long, studentId: Long, completed: Boolean) {
        dao.upsertHomeworkCompletion(
            HomeworkCompletionEntity(
                diaryId = diaryId,
                studentId = studentId,
                completed = completed
            )
        )
    }

    companion object {
        fun formatRs(amount: Int): String {
            val formatter = NumberFormat.getNumberInstance(Locale("en", "IN"))
            return "Rs ${formatter.format(amount)}"
        }

        fun todayFormatted(): String =
            SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date())

        fun generateReceiptNumber(): String {
            val suffix = (1000..9999).random()
            val yymm = SimpleDateFormat("yyMM", Locale.ENGLISH).format(Date())
            return "OXB-RCP-$yymm-$suffix"
        }

        fun buildUpiPaymentUri(
            account: SchoolPaymentAccount,
            fee: FeeRecordEntity,
            student: StudentEntity
        ): android.net.Uri {
            val txRef = "OXB${fee.id}${System.currentTimeMillis() % 100000}"
            return android.net.Uri.Builder()
                .scheme("upi")
                .authority("pay")
                .appendQueryParameter("pa", account.upiId.trim())
                .appendQueryParameter("pn", account.accountHolderName.trim())
                .appendQueryParameter("tr", txRef)
                .appendQueryParameter("tn", "Fee ${fee.monthLabel} - ${student.rollNo} ${student.name}")
                .appendQueryParameter("am", "${fee.totalAmount}.00")
                .appendQueryParameter("cu", "INR")
                .build()
        }

        /**
         * Parses standard NPCI UPI intent response string, e.g.
         * "txnId=UPI123&responseCode=00&Status=SUCCESS&txnRef=OXB123"
         */
        fun parseUpiResponse(response: String?): Pair<Boolean, String> {
            if (response.isNullOrBlank()) return false to ""
            val params = response.split("&")
                .mapNotNull { part ->
                    val eqIdx = part.indexOf('=')
                    if (eqIdx > 0) {
                        part.substring(0, eqIdx).lowercase(Locale.ROOT) to part.substring(eqIdx + 1)
                    } else null
                }
                .toMap()
            val status = params["status"]?.lowercase(Locale.ROOT) ?: ""
            val txnId = params["txnid"]
                ?: params["approvalrefno"]
                ?: params["txnref"]
                ?: ""
            val isSuccess = status == "success"
            return isSuccess to txnId
        }
    }
}
