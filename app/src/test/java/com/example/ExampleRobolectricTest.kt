package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AttendanceStatus
import com.example.data.FeeStatus
import com.example.data.SchoolDatabase
import com.example.data.SchoolRepository
import com.example.data.SessionPreferences
import com.example.data.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: SchoolDatabase
    private lateinit var repository: SchoolRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, SchoolDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = SchoolRepository(db.schoolDao(), SessionPreferences(context))
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `app branding and seed data are populated`() = runBlocking {
        assertEquals("Oxford School Budgam", context.getString(R.string.app_name))
        assertEquals("OXFORD SCHOOL EDUCATION BUDGAM", context.getString(R.string.school_full_name))

        repository.ensureSeeded()
        val students = repository.studentsFlow.first()
        val attendance = repository.attendanceFlow.first()
        val fees = repository.feesFlow.first()
        val diary = repository.diaryFlow.first()

        assertEquals(7, students.size)
        assertEquals(7 * 30, attendance.size)
        assertTrue(fees.size >= 21)
        assertTrue(diary.size >= 3)
    }

    @Test
    fun `demo logins and sign up create valid sessions`() = runBlocking {
        val adminResult = repository.demoLogin(UserRole.ADMIN)
        assertTrue(adminResult.isSuccess)
        assertEquals(UserRole.ADMIN.storageKey, adminResult.getOrNull()?.role)

        val studentResult = repository.demoLogin(UserRole.STUDENT_PARENT)
        assertTrue(studentResult.isSuccess)
        assertEquals("OXB-101", studentResult.getOrNull()?.rollNumber)

        val signUpResult = repository.signUp(
            fullName = "Zainab Khan",
            role = UserRole.STUDENT_PARENT,
            rollNumber = "OXB-209",
            identifier = "zainab@budgam.in",
            password = "pass123"
        )
        assertTrue(signUpResult.isSuccess)
        val createdStudent = repository.findStudentByRollNo("OXB-209")
        assertNotNull(createdStudent)
    }

    @Test
    fun `attendance toggle fee payment and rs formatting work`() = runBlocking {
        repository.ensureSeeded()
        val firstStudent = repository.studentsFlow.first().first()

        repository.setAttendanceStatus(firstStudent.id, 1, AttendanceStatus.LEAVE)
        val updatedAtt = repository.attendanceFlow.first()
            .first { it.studentId == firstStudent.id && it.dayOfMonth == 1 }
        assertEquals(AttendanceStatus.LEAVE.code, updatedAtt.status)

        val pendingFee = repository.feesFlow.first()
            .first { FeeStatus.fromKey(it.status) != FeeStatus.PAID }
        repository.toggleFeePaymentStatus(pendingFee)
        val toggledFee = repository.feesFlow.first().first { it.id == pendingFee.id }
        assertEquals(FeeStatus.PAID.key, toggledFee.status)
        assertTrue(toggledFee.receiptNumber.startsWith("OXB-RCP-"))
        assertEquals("Rs 2,500", SchoolRepository.formatRs(2500))
    }

    @Test
    fun `real bank upi settlement account uri and utr payment recording work`() = runBlocking {
        repository.ensureSeeded()
        val account = com.example.data.SchoolPaymentAccount(
            accountHolderName = "OXFORD SCHOOL EDUCATION BUDGAM",
            upiId = "oxfordbudgam@jkb",
            bankName = "J&K Bank Budgam",
            accountNumber = "0123040100004567",
            ifscCode = "JAKA0BUDGAM"
        )
        assertTrue(repository.savePaymentAccount(account).isSuccess)
        val savedAccount = repository.paymentAccountFlow.first()
        assertEquals("oxfordbudgam@jkb", savedAccount.upiId)
        assertTrue(savedAccount.isConfigured)

        val student = repository.studentsFlow.first().first()
        val pendingFee = repository.feesFlow.first()
            .first { it.studentId == student.id && FeeStatus.fromKey(it.status) != FeeStatus.PAID }

        val upiUri = SchoolRepository.buildUpiPaymentUri(savedAccount, pendingFee, student)
        assertEquals("upi", upiUri.scheme)
        assertEquals("pay", upiUri.host)
        assertEquals("oxfordbudgam@jkb", upiUri.getQueryParameter("pa"))
        assertEquals("${pendingFee.totalAmount}.00", upiUri.getQueryParameter("am"))
        assertEquals("INR", upiUri.getQueryParameter("cu"))

        val (ok, txnId) = SchoolRepository.parseUpiResponse("txnId=UPI99887766&responseCode=00&Status=SUCCESS")
        assertTrue(ok)
        assertEquals("UPI99887766", txnId)

        val payResult = repository.recordStudentFeePayment(pendingFee, "UPI App", txnId)
        assertTrue(payResult.isSuccess)
        val paidRecord = repository.feesFlow.first().first { it.id == pendingFee.id }
        assertEquals(FeeStatus.PAID.key, paidRecord.status)
        assertEquals("UPI99887766", paidRecord.transactionRef)
    }
}
