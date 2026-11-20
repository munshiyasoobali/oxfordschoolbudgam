package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AttendanceStatus
import com.example.data.DiaryEntryEntity
import com.example.data.FeeRecordEntity
import com.example.data.FeeStatus
import com.example.data.SchoolDatabase
import com.example.data.SchoolPaymentAccount
import com.example.data.SchoolRepository
import com.example.data.SessionPreferences
import com.example.data.StudentEntity
import com.example.data.UserAccountEntity
import com.example.data.UserRole
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SessionUiState {
    data object Loading : SessionUiState
    data object Unauthenticated : SessionUiState
    data class Authenticated(val user: UserAccountEntity) : SessionUiState
}

data class SchoolUiState(
    val session: SessionUiState = SessionUiState.Loading,
    val students: List<StudentEntity> = emptyList(),
    val attendanceByStudent: Map<Long, Map<Int, AttendanceStatus>> = emptyMap(),
    val fees: List<FeeRecordEntity> = emptyList(),
    val diaryEntries: List<DiaryEntryEntity> = emptyList(),
    val completedHomeworkIds: Set<Long> = emptySet(),
    val activeStudentProfile: StudentEntity? = null,
    val paymentAccount: SchoolPaymentAccount = SchoolPaymentAccount(),
    val totalCollectedRs: Int = 0,
    val totalPendingDuesRs: Int = 0,
    val isBusy: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class SchoolViewModel(application: Application) : AndroidViewModel(application) {
    private val database = SchoolDatabase.getInstance(application)
    private val sessionPrefs = SessionPreferences(application)
    private val repository = SchoolRepository(database.schoolDao(), sessionPrefs)

    private val currentUserState = MutableStateFlow<SessionUiState>(SessionUiState.Loading)
    private val isBusyState = MutableStateFlow(false)
    private val errorState = MutableStateFlow<String?>(null)
    private val messageState = MutableStateFlow<String?>(null)

    private val activeStudentFlow = combine(
        currentUserState,
        repository.studentsFlow
    ) { session, students ->
        if (session is SessionUiState.Authenticated &&
            UserRole.fromKey(session.user.role) == UserRole.STUDENT_PARENT
        ) {
            students.firstOrNull {
                it.rollNo.equals(session.user.rollNumber, ignoreCase = true)
            } ?: students.firstOrNull()
        } else {
            null
        }
    }

    private val homeworkCompletionsFlow = activeStudentFlow.flatMapLatest { student ->
        if (student != null) {
            repository.observeHomeworkCompletions(student.id)
        } else {
            flowOf(emptyList())
        }
    }

    private val accountAndFeedbackFlow = combine(
        repository.paymentAccountFlow,
        isBusyState,
        errorState,
        messageState
    ) { account, busy, err, msg ->
        listOf(account, busy, err, msg)
    }

    val uiState: StateFlow<SchoolUiState> = combine(
        currentUserState,
        repository.studentsFlow,
        repository.attendanceFlow,
        repository.feesFlow,
        repository.diaryFlow,
        activeStudentFlow,
        homeworkCompletionsFlow,
        accountAndFeedbackFlow
    ) { values ->
        val session = values[0] as SessionUiState
        @Suppress("UNCHECKED_CAST")
        val students = values[1] as List<StudentEntity>
        @Suppress("UNCHECKED_CAST")
        val attendanceList = values[2] as List<com.example.data.AttendanceEntity>
        @Suppress("UNCHECKED_CAST")
        val fees = values[3] as List<FeeRecordEntity>
        @Suppress("UNCHECKED_CAST")
        val diary = values[4] as List<DiaryEntryEntity>
        val activeStudent = values[5] as StudentEntity?
        @Suppress("UNCHECKED_CAST")
        val completions = values[6] as List<com.example.data.HomeworkCompletionEntity>
        @Suppress("UNCHECKED_CAST")
        val extra = values[7] as List<Any?>
        val paymentAccount = extra[0] as SchoolPaymentAccount
        val isBusy = extra[1] as Boolean
        val error = extra[2] as String?
        val status = extra[3] as String?

        val attendanceMap = attendanceList
            .groupBy { it.studentId }
            .mapValues { (_, list) ->
                list.associate { it.dayOfMonth to AttendanceStatus.fromCode(it.status) }
            }

        val collected = fees
            .filter { FeeStatus.fromKey(it.status) == FeeStatus.PAID }
            .sumOf { it.totalAmount }
        val pending = fees
            .filter { FeeStatus.fromKey(it.status) != FeeStatus.PAID }
            .sumOf { it.totalAmount }

        val completedIds = completions.filter { it.completed }.map { it.diaryId }.toSet()

        SchoolUiState(
            session = session,
            students = students,
            attendanceByStudent = attendanceMap,
            fees = fees,
            diaryEntries = diary,
            completedHomeworkIds = completedIds,
            activeStudentProfile = activeStudent,
            paymentAccount = paymentAccount,
            totalCollectedRs = collected,
            totalPendingDuesRs = pending,
            isBusy = isBusy,
            errorMessage = error,
            statusMessage = status
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SchoolUiState()
    )

    init {
        viewModelScope.launch {
            val user = repository.getSavedSessionUser()
            currentUserState.value = if (user != null) {
                SessionUiState.Authenticated(user)
            } else {
                SessionUiState.Unauthenticated
            }
        }
    }

    fun clearFeedback() {
        errorState.value = null
        messageState.value = null
    }

    fun login(identifier: String, password: String) {
        viewModelScope.launch {
            isBusyState.value = true
            errorState.value = null
            val result = repository.login(identifier, password)
            result.onSuccess { user ->
                currentUserState.value = SessionUiState.Authenticated(user)
                messageState.value = "Welcome back, ${user.fullName}"
            }.onFailure { err ->
                errorState.value = err.message ?: "Login failed"
            }
            isBusyState.value = false
        }
    }

    fun demoLogin(role: UserRole) {
        viewModelScope.launch {
            isBusyState.value = true
            errorState.value = null
            val result = repository.demoLogin(role)
            result.onSuccess { user ->
                currentUserState.value = SessionUiState.Authenticated(user)
                messageState.value = "Signed in to ${role.displayName} Demo"
            }.onFailure { err ->
                errorState.value = err.message ?: "Demo login failed"
            }
            isBusyState.value = false
        }
    }

    fun signUp(
        fullName: String,
        role: UserRole,
        rollNumber: String,
        identifier: String,
        password: String
    ) {
        viewModelScope.launch {
            isBusyState.value = true
            errorState.value = null
            val result = repository.signUp(fullName, role, rollNumber, identifier, password)
            result.onSuccess { user ->
                currentUserState.value = SessionUiState.Authenticated(user)
                messageState.value = "Account created for ${user.fullName}"
            }.onFailure { err ->
                errorState.value = err.message ?: "Sign-up failed"
            }
            isBusyState.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            currentUserState.value = SessionUiState.Unauthenticated
            messageState.value = "Signed out safely"
        }
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

    fun updateStudent(student: StudentEntity) {
        viewModelScope.launch {
            repository.updateStudent(student)
            messageState.value = "Updated ${student.name}'s profile"
        }
    }

    fun deleteStudent(student: StudentEntity) {
        viewModelScope.launch {
            repository.deleteStudent(student.id)
            messageState.value = "Removed ${student.name} from directory"
        }
    }

    fun cycleAttendance(studentId: Long, dayOfMonth: Int) {
        viewModelScope.launch {
            val current = uiState.value.attendanceByStudent[studentId]?.get(dayOfMonth)
                ?: AttendanceStatus.PRESENT
            val next = current.next()
            repository.setAttendanceStatus(studentId, dayOfMonth, next)
        }
    }

    fun markAllPresentForDay(dayOfMonth: Int) {
        viewModelScope.launch {
            val ids = uiState.value.students.map { it.id }
            if (ids.isNotEmpty()) {
                repository.markAllStudentsForDay(ids, dayOfMonth, AttendanceStatus.PRESENT)
                messageState.value = "Marked all ${ids.size} students Present for Day $dayOfMonth"
            }
        }
    }

    fun addFeeInvoice(
        studentId: Long,
        monthLabel: String,
        tuitionFee: Int,
        transportFee: Int,
        examFee: Int,
        status: FeeStatus
    ) {
        viewModelScope.launch {
            repository.addFeeInvoice(studentId, monthLabel, tuitionFee, transportFee, examFee, status)
            messageState.value = "Fee invoice added for $monthLabel"
        }
    }

    fun toggleFeeStatus(record: FeeRecordEntity) {
        viewModelScope.launch {
            repository.toggleFeePaymentStatus(record)
            val newStatus = if (FeeStatus.fromKey(record.status) == FeeStatus.PAID) "Pending" else "Paid"
            messageState.value = "Marked ${record.monthLabel} fee as $newStatus"
        }
    }

    fun deleteFeeRecord(feeId: Long) {
        viewModelScope.launch {
            repository.deleteFeeRecord(feeId)
            messageState.value = "Fee invoice deleted"
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
            repository.publishDiaryEntry(
                title = title,
                subject = subject,
                description = description,
                targetClass = targetClass,
                dateLabel = dateLabel,
                teacherRemarks = teacherRemarks,
                isNotice = isNotice
            )
            messageState.value = if (isNotice) "Notice published" else "Diary entry published"
        }
    }

    fun deleteDiaryEntry(diaryId: Long) {
        viewModelScope.launch {
            repository.deleteDiaryEntry(diaryId)
            messageState.value = "Entry removed"
        }
    }

    fun toggleHomeworkCompleted(diaryId: Long, completed: Boolean) {
        val studentId = uiState.value.activeStudentProfile?.id ?: return
        viewModelScope.launch {
            repository.setHomeworkCompleted(diaryId, studentId, completed)
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
        fee: FeeRecordEntity,
        paymentMethod: String,
        transactionRef: String
    ) {
        viewModelScope.launch {
            val result = repository.recordStudentFeePayment(fee, paymentMethod, transactionRef)
            result.onSuccess {
                messageState.value = "Payment recorded for ${fee.monthLabel} (Ref: ${transactionRef.trim()})"
            }.onFailure { err ->
                errorState.value = err.message ?: "Could not record payment"
            }
        }
    }
}
