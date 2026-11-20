package com.example.ui.viewmodel

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AttendanceStatus
import com.example.data.DiaryEntry
import com.example.data.FeeRecord
import com.example.data.FeeStatus
import com.example.data.ManagementConfig
import com.example.data.PortalIntent
import com.example.data.SchoolPaymentAccount
import com.example.data.SchoolRepository
import com.example.data.StudentRecord
import com.example.data.UserLinkProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PortalDestination {
    data object Loading : PortalDestination
    data class Management(val config: ManagementConfig) : PortalDestination
    data object NeedsStudentLink : PortalDestination
    data class Student(val userProfile: UserLinkProfile) : PortalDestination
    data class AccessDenied(val message: String) : PortalDestination
    data class Error(val message: String) : PortalDestination
}

private sealed interface LoadResult<out T> {
    data object Loading : LoadResult<Nothing>
    data class Success<T>(val data: T) : LoadResult<T>
    data class Failure(val message: String) : LoadResult<Nothing>
}

data class SchoolUiState(
    val destination: PortalDestination = PortalDestination.Loading,
    val students: List<StudentRecord> = emptyList(),
    val attendanceByStudent: Map<String, Map<Int, AttendanceStatus>> = emptyMap(),
    val fees: List<FeeRecord> = emptyList(),
    val diaryEntries: List<DiaryEntry> = emptyList(),
    val completedHomeworkIds: Set<String> = emptySet(),
    val activeStudentProfile: StudentRecord? = null,
    val paymentAccount: SchoolPaymentAccount = SchoolPaymentAccount(),
    val totalCollectedRs: Int = 0,
    val totalPendingDuesRs: Int = 0,
    val isBusy: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class SchoolViewModel(
    private val repository: SchoolRepository,
    private val currentUserId: String,
    private val portalIntent: PortalIntent
) : ViewModel() {

    private val isBusyState = MutableStateFlow(false)
    private val errorState = MutableStateFlow<String?>(null)
    private val messageState = MutableStateFlow<String?>(null)
    private val deniedState = MutableStateFlow<String?>(null)

    private fun <T> Flow<T>.asLoadResult(fallbackMsg: String): Flow<LoadResult<T>> =
        map<T, LoadResult<T>> { LoadResult.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                emit(LoadResult.Failure(rootError.message ?: fallbackMsg))
            }

    private val managementConfigFlow: Flow<LoadResult<ManagementConfig?>> =
        repository.observeManagementConfig().asLoadResult("Failed to load management lock")

    private val userProfileFlow: Flow<LoadResult<UserLinkProfile?>> =
        repository.observeUserLinkProfile(currentUserId).asLoadResult("Failed to load user profile")

    private val destinationFlow: Flow<PortalDestination> = combine(
        managementConfigFlow,
        userProfileFlow,
        deniedState
    ) { mgmtRes, userRes, denied ->
        if (!denied.isNullOrBlank()) {
            return@combine PortalDestination.AccessDenied(denied)
        }
        when {
            mgmtRes is LoadResult.Loading || userRes is LoadResult.Loading -> PortalDestination.Loading
            mgmtRes is LoadResult.Failure -> PortalDestination.Error(mgmtRes.message)
            userRes is LoadResult.Failure -> PortalDestination.Error(userRes.message)
            mgmtRes is LoadResult.Success && userRes is LoadResult.Success -> {
                val mgmt = mgmtRes.data
                val profile = userRes.data
                when {
                    mgmt != null && mgmt.adminUid == currentUserId -> PortalDestination.Management(mgmt)
                    portalIntent == PortalIntent.MANAGEMENT -> PortalDestination.Loading
                    profile != null -> PortalDestination.Student(profile)
                    else -> PortalDestination.NeedsStudentLink
                }
            }
            else -> PortalDestination.Loading
        }
    }

    private val adminDataFlow: Flow<Triple<List<StudentRecord>, Map<String, Map<Int, AttendanceStatus>>, List<FeeRecord>>> =
        destinationFlow.flatMapLatest { dest ->
            if (dest is PortalDestination.Management) {
                combine(
                    repository.observeAllStudents().asLoadResult("Failed to load students"),
                    repository.observeAllAttendance().asLoadResult("Failed to load attendance"),
                    repository.observeAllFees().asLoadResult("Failed to load fees")
                ) { stRes, attRes, feeRes ->
                    Triple(
                        (stRes as? LoadResult.Success)?.data.orEmpty(),
                        (attRes as? LoadResult.Success)?.data.orEmpty(),
                        (feeRes as? LoadResult.Success)?.data.orEmpty()
                    )
                }
            } else {
                flowOf(Triple(emptyList(), emptyMap(), emptyList()))
            }
        }

    private val studentDataFlow: Flow<Triple<StudentRecord?, Map<String, Map<Int, AttendanceStatus>>, List<FeeRecord>>> =
        destinationFlow.flatMapLatest { dest ->
            if (dest is PortalDestination.Student) {
                val sid = dest.userProfile.studentId
                combine(
                    repository.observeLinkedStudent(sid).asLoadResult("Failed to load student profile"),
                    repository.observeStudentAttendance(sid).asLoadResult("Failed to load attendance"),
                    repository.observeStudentFees(sid).asLoadResult("Failed to load fees")
                ) { stRes, attRes, feeRes ->
                    val st = (stRes as? LoadResult.Success)?.data
                    val att = (attRes as? LoadResult.Success)?.data.orEmpty()
                    val fees = (feeRes as? LoadResult.Success)?.data.orEmpty()
                    Triple(st, mapOf(sid to att), fees)
                }
            } else {
                flowOf(Triple(null, emptyMap(), emptyList()))
            }
        }

    private val diaryAndAccountFlow = destinationFlow.flatMapLatest { dest ->
        if (dest is PortalDestination.Management || dest is PortalDestination.Student) {
            combine(
                repository.observeDiaryEntries().asLoadResult("Failed to load diary"),
                repository.observePaymentAccount().asLoadResult("Failed to load payment account"),
                repository.observeHomeworkCompletions(currentUserId).asLoadResult("Failed to load completions")
            ) { diaryRes, accRes, compRes ->
                Triple(
                    (diaryRes as? LoadResult.Success)?.data.orEmpty(),
                    (accRes as? LoadResult.Success)?.data ?: SchoolPaymentAccount(),
                    (compRes as? LoadResult.Success)?.data.orEmpty()
                )
            }
        } else {
            flowOf(Triple(emptyList(), SchoolPaymentAccount(), emptySet()))
        }
    }

    private val feedbackFlow = combine(
        isBusyState,
        errorState,
        messageState
    ) { busy, err, msg -> Triple(busy, err, msg) }

    val uiState: StateFlow<SchoolUiState> = combine(
        destinationFlow,
        adminDataFlow,
        studentDataFlow,
        diaryAndAccountFlow,
        feedbackFlow
    ) { dest, adminData, studentData, diaryAndAcc, feedback ->
        val students = if (dest is PortalDestination.Management) adminData.first else listOfNotNull(studentData.first)
        val attendance = if (dest is PortalDestination.Management) adminData.second else studentData.second
        val fees = if (dest is PortalDestination.Management) adminData.third else studentData.third

        val collected = fees
            .filter { FeeStatus.fromKey(it.status) == FeeStatus.PAID }
            .sumOf { it.totalAmount }
        val pending = fees
            .filter { FeeStatus.fromKey(it.status) != FeeStatus.PAID }
            .sumOf { it.totalAmount }

        SchoolUiState(
            destination = dest,
            students = students,
            attendanceByStudent = attendance,
            fees = fees,
            diaryEntries = diaryAndAcc.first,
            completedHomeworkIds = diaryAndAcc.third,
            activeStudentProfile = studentData.first,
            paymentAccount = diaryAndAcc.second,
            totalCollectedRs = collected,
            totalPendingDuesRs = pending,
            isBusy = feedback.first,
            errorMessage = feedback.second,
            statusMessage = feedback.third
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = SchoolUiState()
    )

    init {
        if (portalIntent == PortalIntent.MANAGEMENT) {
            verifyOrClaimManagementAccess()
        }
    }

    fun verifyOrClaimManagementAccess() {
        viewModelScope.launch {
            isBusyState.value = true
            val result = repository.claimOrVerifyManagementRole()
            result.onFailure { err ->
                deniedState.value = err.message
                    ?: "Access Denied: Management portal is restricted to the single authorized Management Google account."
            }
            isBusyState.value = false
        }
    }

    fun linkParentToStudent(
        rollNo: String,
        studentNameIfNew: String,
        gradeIfNew: String,
        parentName: String,
        contactNumber: String
    ) {
        viewModelScope.launch {
            isBusyState.value = true
            errorState.value = null
            val result = repository.linkParentToStudent(
                rollNoInput = rollNo,
                studentNameIfNew = studentNameIfNew,
                gradeIfNew = gradeIfNew,
                parentNameInput = parentName,
                contactNumberInput = contactNumber
            )
            result.onSuccess {
                messageState.value = "Student profile linked successfully"
            }.onFailure { err ->
                errorState.value = err.message ?: "Could not link student profile"
            }
            isBusyState.value = false
        }
    }

    fun signOut(credentialManager: CredentialManager, onSignedOut: (String?) -> Unit = {}) {
        val reason = deniedState.value
        viewModelScope.launch {
            repository.signOut(credentialManager)
            onSignedOut(reason)
        }
    }

    fun clearFeedback() {
        errorState.value = null
        messageState.value = null
    }

    fun addStudent(
        name: String,
        grade: String,
        rollNo: String,
        parentName: String,
        contactNumber: String
    ) {
        viewModelScope.launch {
            val result = repository.addStudent(name, grade, rollNo, parentName, contactNumber)
            result.onSuccess {
                messageState.value = "Student $name added to directory"
            }.onFailure { err ->
                errorState.value = err.message ?: "Could not add student"
            }
        }
    }

    fun updateStudent(student: StudentRecord) {
        viewModelScope.launch {
            val result = repository.updateStudent(student)
            result.onSuccess {
                messageState.value = "Updated ${student.name}'s profile"
            }.onFailure { err ->
                errorState.value = err.message ?: "Could not update student"
            }
        }
    }

    fun deleteStudent(student: StudentRecord) {
        viewModelScope.launch {
            val result = repository.deleteStudent(student.id)
            result.onSuccess {
                messageState.value = "Removed ${student.name} from directory"
            }.onFailure { err ->
                errorState.value = err.message ?: "Could not remove student"
            }
        }
    }

    fun cycleAttendance(studentId: String, dayOfMonth: Int) {
        val student = uiState.value.students.firstOrNull { it.id == studentId } ?: return
        viewModelScope.launch {
            val current = uiState.value.attendanceByStudent[studentId]?.get(dayOfMonth)
                ?: AttendanceStatus.PRESENT
            val next = current.next()
            val result = repository.setAttendanceStatus(student, dayOfMonth, next)
            result.onFailure { err ->
                errorState.value = err.message ?: "Could not update attendance"
            }
        }
    }

    fun markAllPresentForDay(dayOfMonth: Int) {
        val students = uiState.value.students
        if (students.isEmpty()) return
        viewModelScope.launch {
            val result = repository.markAllStudentsForDay(students, dayOfMonth, AttendanceStatus.PRESENT)
            result.onSuccess {
                messageState.value = "Marked all ${students.size} students Present for Day $dayOfMonth"
            }.onFailure { err ->
                errorState.value = err.message ?: "Could not mark all present"
            }
        }
    }

    fun addFeeInvoice(
        student: StudentRecord,
        monthLabel: String,
        tuitionFee: Int,
        transportFee: Int,
        examFee: Int,
        status: FeeStatus
    ) {
        viewModelScope.launch {
            val result = repository.addFeeInvoice(student, monthLabel, tuitionFee, transportFee, examFee, status)
            result.onSuccess {
                messageState.value = "Fee invoice added for $monthLabel"
            }.onFailure { err ->
                errorState.value = err.message ?: "Could not create fee invoice"
            }
        }
    }

    fun toggleFeeStatus(record: FeeRecord) {
        viewModelScope.launch {
            val result = repository.toggleFeePaymentStatus(record)
            result.onSuccess {
                val newStatus = if (FeeStatus.fromKey(record.status) == FeeStatus.PAID) "Pending" else "Paid"
                messageState.value = "Marked ${record.monthLabel} fee as $newStatus"
            }.onFailure { err ->
                errorState.value = err.message ?: "Could not update fee status"
            }
        }
    }

    fun deleteFeeRecord(feeId: String) {
        viewModelScope.launch {
            val result = repository.deleteFeeRecord(feeId)
            result.onSuccess {
                messageState.value = "Fee invoice deleted"
            }.onFailure { err ->
                errorState.value = err.message ?: "Could not delete fee invoice"
            }
        }
    }

    fun publishDiaryEntry(
        title: String,
        subject: String,
        description: String,
        targetClass: String,
        dateLabel: String,
        teacherRemarks: String,
        isNotice: Boolean
    ) {
        viewModelScope.launch {
            if (title.isBlank() || description.isBlank()) {
                errorState.value = "Title and description are required."
                return@launch
            }
            val result = repository.publishDiaryEntry(
                title = title,
                subject = subject,
                description = description,
                targetClass = targetClass,
                dateLabel = dateLabel,
                teacherRemarks = teacherRemarks,
                isNotice = isNotice
            )
            result.onSuccess {
                messageState.value = if (isNotice) "Notice published" else "Diary entry published"
            }.onFailure { err ->
                errorState.value = err.message ?: "Could not publish entry"
            }
        }
    }

    fun deleteDiaryEntry(diaryId: String) {
        viewModelScope.launch {
            val result = repository.deleteDiaryEntry(diaryId)
            result.onSuccess {
                messageState.value = "Entry removed"
            }.onFailure { err ->
                errorState.value = err.message ?: "Could not delete entry"
            }
        }
    }

    fun toggleHomeworkCompleted(diaryId: String, completed: Boolean) {
        viewModelScope.launch {
            val result = repository.setHomeworkCompleted(diaryId, completed)
            result.onFailure { err ->
                errorState.value = err.message ?: "Could not update homework status"
            }
        }
    }

    fun savePaymentAccount(account: SchoolPaymentAccount) {
        viewModelScope.launch {
            val result = repository.savePaymentAccount(account)
            result.onSuccess {
                messageState.value = "School bank & UPI settlement account saved"
            }.onFailure { err ->
                errorState.value = err.message ?: "Could not save settlement account"
            }
        }
    }

    fun recordStudentFeePayment(
        fee: FeeRecord,
        paymentMethod: String,
        transactionRef: String,
        payerAccountNote: String
    ) {
        viewModelScope.launch {
            val result = repository.recordStudentFeePayment(fee, paymentMethod, transactionRef, payerAccountNote)
            result.onSuccess {
                messageState.value = "Payment recorded for ${fee.monthLabel} (Ref: ${transactionRef.trim()})"
            }.onFailure { err ->
                errorState.value = err.message ?: "Could not record payment"
            }
        }
    }

    class Factory(
        private val appContext: Context,
        private val currentUserId: String,
        private val portalIntent: PortalIntent
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val dbId = appContext.getString(com.example.R.string.firestore_database_id)
            val db = FirebaseFirestore.getInstance(dbId)
            return SchoolViewModel(
                repository = SchoolRepository(db),
                currentUserId = currentUserId,
                portalIntent = portalIntent
            ) as T
        }
    }
}
