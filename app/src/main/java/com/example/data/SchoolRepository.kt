package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SchoolRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Please sign in with Google before accessing school records.")
    }

    private fun <T> observeDocSafe(
        path: String,
        opType: OperationType = OperationType.GET,
        transform: (DocumentSnapshot) -> T
    ): Flow<T> = flow {
        val uid = requireUserId()
        emitAll(
            db.document(path)
                .snapshots()
                .map(transform)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, opType, path)
                    }
                    throw error
                }
        )
    }

    private fun <T> observeQuerySafe(
        path: String,
        queryBuilder: () -> Query,
        transform: (com.google.firebase.firestore.QuerySnapshot) -> T
    ): Flow<T> = flow {
        val uid = requireUserId()
        emitAll(
            queryBuilder()
                .snapshots()
                .map(transform)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    // --- ROLE & AUTH LOCK OBSERVATION ---

    fun observeManagementConfig(): Flow<ManagementConfig?> =
        observeDocSafe("school_config/management") { snap ->
            if (!snap.exists()) null
            else snap.toObject(ManagementConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }

    fun observeUserLinkProfile(uid: String): Flow<UserLinkProfile?> =
        observeDocSafe("users/$uid") { snap ->
            if (!snap.exists()) null
            else snap.toObject(UserLinkProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }

    fun observePaymentAccount(): Flow<SchoolPaymentAccount> =
        observeDocSafe("school_config/payment_account") { snap ->
            if (!snap.exists()) SchoolPaymentAccount()
            else snap.toObject(SchoolPaymentAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                ?: SchoolPaymentAccount()
        }

    /**
     * Single-Admin School Management Claim/Verify.
     * Permanently binds /school_config/management to the first Management Google account.
     * Any other Google account attempting Management entry is denied.
     */
    suspend fun claimOrVerifyManagementRole(): Result<ManagementConfig> {
        val path = "school_config/management"
        return try {
            val uid = requireUserId()
            val user = auth.currentUser!!
            val docRef = db.document(path)
            val snap = docRef.get().await()
            if (snap.exists()) {
                val config = snap.toObject(
                    ManagementConfig::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: return Result.failure(IllegalStateException("Invalid management configuration."))
                if (config.adminUid == uid) {
                    seedInitialSchoolRecordsIfEmpty()
                    Result.success(config)
                } else {
                    Result.failure(
                        SecurityException(
                            "Access Denied: School Management is exclusively locked to ${config.maskedEmail}. Use Parent / Student sign-in instead."
                        )
                    )
                }
            } else {
                val email = user.email.orEmpty()
                val masked = maskEmail(email)
                val adminName = user.displayName?.takeIf { it.isNotBlank() } ?: "School Management"
                val payload = mapOf(
                    "adminUid" to uid,
                    "maskedEmail" to masked,
                    "adminName" to adminName,
                    "claimedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
                seedInitialSchoolRecordsIfEmpty()
                Result.success(ManagementConfig(id = "management", adminUid = uid, maskedEmail = masked, adminName = adminName))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    /**
     * Links the signed-in Parent/Student Google account to an existing unclaimed Roll Number
     * OR registers a new student profile for that Roll Number.
     */
    suspend fun linkParentToStudent(
        rollNoInput: String,
        studentNameIfNew: String,
        gradeIfNew: String,
        parentNameInput: String,
        contactNumberInput: String
    ): Result<Unit> {
        var activePath = "users"
        return try {
            val uid = requireUserId()
            val user = auth.currentUser!!
            val cleanId = sanitizeId(rollNoInput)
            if (cleanId.isBlank()) {
                return Result.failure(IllegalArgumentException("Please enter a valid Roll Number / Admission ID (e.g. OXB-101)."))
            }

            val studentPath = "students/$cleanId"
            activePath = studentPath
            val studentDoc = db.document(studentPath).get().await()
            if (studentDoc.exists()) {
                val existingStudent = studentDoc.toObject(
                    StudentRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
                val existingParentUid = existingStudent?.parentUid.orEmpty()
                if (existingParentUid.isNotBlank() && existingParentUid != uid) {
                    return Result.failure(
                        IllegalStateException("Roll No $cleanId is already linked to another parent Google account.")
                    )
                }
                if (existingParentUid.isBlank()) {
                    db.document(studentPath).update(
                        mapOf(
                            "parentUid" to uid,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            } else {
                val cleanName = studentNameIfNew.trim()
                val cleanGrade = gradeIfNew.trim()
                if (cleanName.isBlank() || cleanGrade.isBlank()) {
                    return Result.failure(
                        IllegalArgumentException("Student Roll No $cleanId was not found. Enter Student Full Name and Class to register.")
                    )
                }
                val cleanParent = parentNameInput.trim()
                    .ifBlank { user.displayName?.takeIf { it.isNotBlank() } ?: "Parent / Guardian" }
                val cleanContact = contactNumberInput.trim()
                    .ifBlank { user.email ?: "Registered via Google" }

                val studentPayload = mapOf(
                    "name" to cleanName,
                    "grade" to cleanGrade,
                    "rollNo" to cleanId,
                    "parentName" to cleanParent,
                    "contactNumber" to cleanContact,
                    "admissionYear" to "2026",
                    "parentUid" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.document(studentPath).set(studentPayload).await()

                val attPath = "attendance/$cleanId"
                activePath = attPath
                db.document(attPath).set(
                    mapOf(
                        "studentId" to cleanId,
                        "parentUid" to uid,
                        "days" to SeedData.defaultPresentMap(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            val userPath = "users/$uid"
            activePath = userPath
            val displayName = parentNameInput.trim()
                .ifBlank { user.displayName?.takeIf { it.isNotBlank() } ?: "Parent / Student" }
            db.document(userPath).set(
                mapOf(
                    "userId" to uid,
                    "studentId" to cleanId,
                    "rollNo" to cleanId,
                    "displayName" to displayName,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, activePath)
            Result.failure(e)
        }
    }

    suspend fun signOut(credentialManager: CredentialManager) {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                auth.signOut()
            }
        }
    }

    // --- ADMIN OBSERVABLES ---

    fun observeAllStudents(): Flow<List<StudentRecord>> =
        observeQuerySafe("students", { db.collection("students") }) { snapshot ->
            snapshot.toObjects(StudentRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedBy { it.rollNo }
        }

    fun observeAllAttendance(): Flow<Map<String, Map<Int, AttendanceStatus>>> =
        observeQuerySafe("attendance", { db.collection("attendance") }) { snapshot ->
            snapshot.documents.associate { doc ->
                val rawDays = doc.get("days") as? Map<*, *> ?: emptyMap<Any, Any>()
                val parsed = rawDays.entries.mapNotNull { (k, v) ->
                    val dayInt = k?.toString()?.toIntOrNull() ?: return@mapNotNull null
                    dayInt to AttendanceStatus.fromCode(v?.toString())
                }.toMap()
                doc.id to parsed
            }
        }

    fun observeAllFees(): Flow<List<FeeRecord>> =
        observeQuerySafe("fees", { db.collection("fees") }) { snapshot ->
            snapshot.documents.map { doc -> doc.toFeeRecordSafe() }
                .sortedWith(compareBy({ it.status == FeeStatus.PAID.key }, { it.rollNo }, { it.monthLabel }))
        }

    // --- STUDENT / PARENT SCOPED OBSERVABLES (MATCHES FIRESTORE RULES) ---

    fun observeLinkedStudent(studentId: String): Flow<StudentRecord?> =
        observeDocSafe("students/$studentId") { snap ->
            if (!snap.exists()) null
            else snap.toObject(StudentRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }

    fun observeStudentAttendance(studentId: String): Flow<Map<Int, AttendanceStatus>> =
        observeDocSafe("attendance/$studentId") { doc ->
            val rawDays = doc.get("days") as? Map<*, *> ?: emptyMap<Any, Any>()
            rawDays.entries.mapNotNull { (k, v) ->
                val dayInt = k?.toString()?.toIntOrNull() ?: return@mapNotNull null
                dayInt to AttendanceStatus.fromCode(v?.toString())
            }.toMap()
        }

    fun observeStudentFees(studentId: String): Flow<List<FeeRecord>> =
        observeQuerySafe("fees", { db.collection("fees").whereEqualTo("studentId", studentId) }) { snapshot ->
            snapshot.documents.map { doc -> doc.toFeeRecordSafe() }
                .sortedBy { it.status == FeeStatus.PAID.key }
        }

    fun observeDiaryEntries(): Flow<List<DiaryEntry>> =
        observeQuerySafe("diary", { db.collection("diary") }) { snapshot ->
            snapshot.toObjects(DiaryEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedByDescending { it.createdAt?.seconds ?: 0L }
        }

    fun observeHomeworkCompletions(uid: String): Flow<Set<String>> =
        observeQuerySafe("users/$uid/homework_completions", {
            db.collection("users").document(uid).collection("homework_completions")
        }) { snapshot ->
            snapshot.toObjects(HomeworkCompletion::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .filter { it.completed }
                .map { it.diaryId }
                .toSet()
        }

    // --- INITIAL SAMPLE SCHOOL POPULATION (ADMIN ONLY) ---

    suspend fun seedInitialSchoolRecordsIfEmpty(): Result<Unit> {
        var path = "students"
        return try {
            requireUserId()
            val existing = db.collection("students").limit(1).get().await()
            if (!existing.isEmpty) return Result.success(Unit)

            SeedData.defaultStudents.forEachIndexed { idx, st ->
                path = "students/${st.id}"
                db.document(path).set(
                    mapOf(
                        "name" to st.name,
                        "grade" to st.grade,
                        "rollNo" to st.rollNo,
                        "parentName" to st.parentName,
                        "contactNumber" to st.contactNumber,
                        "admissionYear" to st.admissionYear,
                        "parentUid" to "",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

                path = "attendance/${st.id}"
                db.document(path).set(
                    mapOf(
                        "studentId" to st.id,
                        "parentUid" to "",
                        "days" to SeedData.build30DayAttendanceMap(idx),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            SeedData.generateFeeRecords(SeedData.defaultStudents).forEach { fee ->
                path = "fees/${fee.id}"
                db.document(path).set(
                    mapOf(
                        "studentId" to fee.studentId,
                        "studentName" to fee.studentName,
                        "rollNo" to fee.rollNo,
                        "grade" to fee.grade,
                        "parentUid" to "",
                        "monthLabel" to fee.monthLabel,
                        "tuitionFee" to fee.tuitionFee,
                        "transportFee" to fee.transportFee,
                        "examFee" to fee.examFee,
                        "totalAmount" to fee.totalAmount,
                        "status" to fee.status,
                        "receiptNumber" to fee.receiptNumber,
                        "paidDate" to fee.paidDate,
                        "paymentMethod" to fee.paymentMethod,
                        "transactionRef" to fee.transactionRef,
                        "payerAccountNote" to fee.payerAccountNote,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            SeedData.defaultDiaryEntries.forEach { entry ->
                path = "diary/${entry.id}"
                db.document(path).set(
                    mapOf(
                        "title" to entry.title,
                        "subject" to entry.subject,
                        "description" to entry.description,
                        "targetClass" to entry.targetClass,
                        "dateLabel" to entry.dateLabel,
                        "teacherRemarks" to entry.teacherRemarks,
                        "entryType" to entry.entryType,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- ADMIN WRITE OPERATIONS ---

    suspend fun savePaymentAccount(account: SchoolPaymentAccount): Result<Unit> {
        val path = "school_config/payment_account"
        return try {
            val uid = requireUserId()
            val cleanHolder = account.accountHolderName.trim().ifBlank { "OXFORD SCHOOL EDUCATION BUDGAM" }
            val cleanUpi = account.upiId.trim()
            val cleanBank = account.bankName.trim()
            val cleanAcc = account.accountNumber.trim()
            val cleanIfsc = account.ifscCode.trim().uppercase(Locale.ROOT)

            if (cleanUpi.isNotBlank() && !cleanUpi.contains("@")) {
                return Result.failure(IllegalArgumentException("Enter a valid UPI ID containing '@' (e.g. school@jkb)."))
            }
            if (cleanAcc.isNotBlank() && cleanIfsc.isBlank()) {
                return Result.failure(IllegalArgumentException("IFSC Code is required when entering a Bank Account Number."))
            }
            if (cleanUpi.isBlank() && cleanAcc.isBlank()) {
                return Result.failure(IllegalArgumentException("Enter either an Official UPI ID or Bank Account Number."))
            }

            db.document(path).set(
                mapOf(
                    "accountHolderName" to cleanHolder,
                    "upiId" to cleanUpi,
                    "bankName" to cleanBank,
                    "accountNumber" to cleanAcc,
                    "ifscCode" to cleanIfsc,
                    "updatedByUid" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun addStudent(
        name: String,
        grade: String,
        rollNo: String,
        parentName: String,
        contactNumber: String
    ): Result<Unit> {
        val cleanRoll = sanitizeId(rollNo)
        val path = "students/$cleanRoll"
        return try {
            requireUserId()
            if (name.isBlank() || grade.isBlank() || cleanRoll.isBlank()) {
                return Result.failure(IllegalArgumentException("Name, Class, and Roll No are required."))
            }
            val existing = db.document(path).get().await()
            if (existing.exists()) {
                return Result.failure(IllegalArgumentException("Roll No $cleanRoll already exists."))
            }
            db.document(path).set(
                mapOf(
                    "name" to name.trim(),
                    "grade" to grade.trim(),
                    "rollNo" to cleanRoll,
                    "parentName" to parentName.trim().ifBlank { "Parent / Guardian" },
                    "contactNumber" to contactNumber.trim().ifBlank { "Not provided" },
                    "admissionYear" to "2026",
                    "parentUid" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.document("attendance/$cleanRoll").set(
                mapOf(
                    "studentId" to cleanRoll,
                    "parentUid" to "",
                    "days" to SeedData.defaultPresentMap(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateStudent(student: StudentRecord): Result<Unit> {
        val path = "students/${student.id}"
        return try {
            requireUserId()
            require(student.id.isNotBlank()) { "Student ID cannot be blank" }
            db.document(path).update(
                mapOf(
                    "name" to student.name.trim(),
                    "grade" to student.grade.trim(),
                    "parentName" to student.parentName.trim(),
                    "contactNumber" to student.contactNumber.trim(),
                    "admissionYear" to student.admissionYear.trim(),
                    "parentUid" to student.parentUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteStudent(studentId: String): Result<Unit> {
        var path = "students/$studentId"
        return try {
            requireUserId()
            require(studentId.isNotBlank()) { "Student ID cannot be blank" }
            db.document(path).delete().await()
            path = "attendance/$studentId"
            db.document(path).delete().await()
            val relatedFees = db.collection("fees").whereEqualTo("studentId", studentId).get().await()
            relatedFees.documents.forEach { feeDoc ->
                feeDoc.reference.delete().await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun setAttendanceStatus(
        student: StudentRecord,
        dayOfMonth: Int,
        status: AttendanceStatus
    ): Result<Unit> {
        val path = "attendance/${student.id}"
        return try {
            requireUserId()
            val docRef = db.document(path)
            val snap = docRef.get().await()
            if (snap.exists()) {
                docRef.update(
                    mapOf(
                        "days.$dayOfMonth" to status.code,
                        "parentUid" to student.parentUid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                val baseMap = SeedData.defaultPresentMap().toMutableMap()
                baseMap[dayOfMonth.toString()] = status.code
                docRef.set(
                    mapOf(
                        "studentId" to student.id,
                        "parentUid" to student.parentUid,
                        "days" to baseMap,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun markAllStudentsForDay(
        students: List<StudentRecord>,
        dayOfMonth: Int,
        status: AttendanceStatus
    ): Result<Unit> {
        var path = "attendance"
        return try {
            requireUserId()
            students.forEach { st ->
                path = "attendance/${st.id}"
                val docRef = db.document(path)
                val snap = docRef.get().await()
                if (snap.exists()) {
                    docRef.update(
                        mapOf(
                            "days.$dayOfMonth" to status.code,
                            "parentUid" to st.parentUid,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                } else {
                    val baseMap = SeedData.defaultPresentMap().toMutableMap()
                    baseMap[dayOfMonth.toString()] = status.code
                    docRef.set(
                        mapOf(
                            "studentId" to st.id,
                            "parentUid" to st.parentUid,
                            "days" to baseMap,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun addFeeInvoice(
        student: StudentRecord,
        monthLabel: String,
        tuitionFee: Int,
        transportFee: Int,
        examFee: Int,
        status: FeeStatus
    ): Result<Unit> {
        val docRef = db.collection("fees").document()
        val path = docRef.path
        return try {
            requireUserId()
            val isPaid = status == FeeStatus.PAID
            val total = tuitionFee + transportFee + examFee
            docRef.set(
                mapOf(
                    "studentId" to student.id,
                    "studentName" to student.name,
                    "rollNo" to student.rollNo,
                    "grade" to student.grade,
                    "parentUid" to student.parentUid,
                    "monthLabel" to monthLabel.trim(),
                    "tuitionFee" to tuitionFee,
                    "transportFee" to transportFee,
                    "examFee" to examFee,
                    "totalAmount" to total,
                    "status" to status.key,
                    "receiptNumber" to if (isPaid) generateReceiptNumber() else "",
                    "paidDate" to if (isPaid) todayFormatted() else "",
                    "paymentMethod" to "School Counter",
                    "transactionRef" to if (isPaid) "COUNTER-${(100000..999999).random()}" else "",
                    "payerAccountNote" to if (isPaid) "Received at School Office" else "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleFeePaymentStatus(record: FeeRecord): Result<Unit> {
        val path = "fees/${record.id}"
        return try {
            requireUserId()
            require(record.id.isNotBlank()) { "Fee ID cannot be blank" }
            val current = FeeStatus.fromKey(record.status)
            val updates = if (current == FeeStatus.PAID) {
                mapOf(
                    "status" to FeeStatus.PENDING.key,
                    "receiptNumber" to "",
                    "paidDate" to "",
                    "paymentMethod" to "School Counter",
                    "transactionRef" to "",
                    "payerAccountNote" to "",
                    "parentUid" to record.parentUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            } else {
                mapOf(
                    "status" to FeeStatus.PAID.key,
                    "receiptNumber" to record.receiptNumber.ifBlank { generateReceiptNumber() },
                    "paidDate" to record.paidDate.ifBlank { todayFormatted() },
                    "paymentMethod" to record.paymentMethod.ifBlank { "School Counter" },
                    "transactionRef" to record.transactionRef.ifBlank { "COUNTER-${(100000..999999).random()}" },
                    "payerAccountNote" to record.payerAccountNote.ifBlank { "Verified by School Management" },
                    "parentUid" to record.parentUid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            }
            db.document(path).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteFeeRecord(feeId: String): Result<Unit> {
        val path = "fees/$feeId"
        return try {
            requireUserId()
            require(feeId.isNotBlank()) { "Fee ID cannot be blank" }
            db.document(path).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun recordStudentFeePayment(
        fee: FeeRecord,
        paymentMethod: String,
        transactionRef: String,
        payerAccountNote: String
    ): Result<Unit> {
        val path = "fees/${fee.id}"
        return try {
            val uid = requireUserId()
            val cleanRef = transactionRef.trim()
            if (cleanRef.length < 6) {
                return Result.failure(IllegalArgumentException("Please enter a valid UPI Txn ID or Bank UTR reference (6+ characters)."))
            }
            val cleanPayerNote = payerAccountNote.trim().ifBlank { auth.currentUser?.email ?: "Verified Parent Account" }
            db.document(path).update(
                mapOf(
                    "status" to FeeStatus.PAID.key,
                    "receiptNumber" to fee.receiptNumber.ifBlank { generateReceiptNumber() },
                    "paidDate" to todayFormatted(),
                    "paymentMethod" to paymentMethod.trim().ifBlank { "UPI / Bank Transfer" },
                    "transactionRef" to cleanRef,
                    "payerAccountNote" to cleanPayerNote,
                    "parentUid" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun publishDiaryEntry(
        title: String,
        subject: String,
        description: String,
        targetClass: String,
        dateLabel: String,
        teacherRemarks: String,
        isNotice: Boolean
    ): Result<Unit> {
        val docRef = db.collection("diary").document()
        val path = docRef.path
        return try {
            requireUserId()
            docRef.set(
                mapOf(
                    "title" to title.trim(),
                    "subject" to subject.trim().ifBlank { if (isNotice) "School Notice" else "General" },
                    "description" to description.trim(),
                    "targetClass" to targetClass.trim().ifBlank { "All Classes" },
                    "dateLabel" to dateLabel.trim().ifBlank { todayFormatted() },
                    "teacherRemarks" to teacherRemarks.trim(),
                    "entryType" to if (isNotice) "NOTICE" else "HOMEWORK",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteDiaryEntry(diaryId: String): Result<Unit> {
        val path = "diary/$diaryId"
        return try {
            requireUserId()
            require(diaryId.isNotBlank()) { "Diary ID cannot be blank" }
            db.document(path).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun setHomeworkCompleted(diaryId: String, completed: Boolean): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            val cleanDiaryId = sanitizeId(diaryId)
            path = "users/$uid/homework_completions/$cleanDiaryId"
            db.document(path).set(
                mapOf(
                    "diaryId" to cleanDiaryId,
                    "completed" to completed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private fun DocumentSnapshot.toFeeRecordSafe(): FeeRecord {
        return FeeRecord(
            id = id,
            studentId = getString("studentId") ?: "",
            studentName = getString("studentName") ?: "",
            rollNo = getString("rollNo") ?: "",
            grade = getString("grade") ?: "",
            parentUid = getString("parentUid") ?: "",
            monthLabel = getString("monthLabel") ?: "",
            tuitionFee = (get("tuitionFee") as? Number)?.toInt() ?: 0,
            transportFee = (get("transportFee") as? Number)?.toInt() ?: 0,
            examFee = (get("examFee") as? Number)?.toInt() ?: 0,
            totalAmount = (get("totalAmount") as? Number)?.toInt() ?: 0,
            status = getString("status") ?: FeeStatus.PENDING.key,
            receiptNumber = getString("receiptNumber") ?: "",
            paidDate = getString("paidDate") ?: "",
            paymentMethod = getString("paymentMethod") ?: "School Counter",
            transactionRef = getString("transactionRef") ?: "",
            payerAccountNote = getString("payerAccountNote") ?: "",
            createdAt = getTimestamp("createdAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE),
            updatedAt = getTimestamp("updatedAt", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        )
    }

    companion object {
        fun sanitizeId(raw: String): String =
            raw.trim()
                .uppercase(Locale.ROOT)
                .replace(Regex("[^A-Z0-9_-]"), "-")
                .replace(Regex("-+"), "-")
                .trim('-')
                .take(64)

        fun maskEmail(email: String): String {
            val parts = email.split("@")
            if (parts.size != 2) return "Authorized Management Google Account"
            val local = parts[0]
            val maskedLocal = if (local.length <= 2) "${local.firstOrNull() ?: '*'}***" else "${local.take(2)}***"
            return "$maskedLocal@${parts[1]}"
        }

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
            fee: FeeRecord,
            student: StudentRecord
        ): Uri {
            val cleanFeeSuffix = fee.id.takeLast(6).filter { it.isLetterOrDigit() }.ifBlank { "FEE" }
            val txRef = "OXB$cleanFeeSuffix${System.currentTimeMillis() % 100000}"
            return Uri.Builder()
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
