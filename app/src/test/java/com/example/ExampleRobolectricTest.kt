package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AttendanceStatus
import com.example.data.FeeRecord
import com.example.data.FeeStatus
import com.example.data.SchoolPaymentAccount
import com.example.data.SchoolRepository
import com.example.data.SeedData
import com.example.data.StudentRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `app branding and firebase database config are present`() {
        assertEquals("Oxford School Budgam", context.getString(R.string.app_name))
        assertEquals("OXFORD SCHOOL EDUCATION BUDGAM", context.getString(R.string.school_full_name))
        assertTrue(context.getString(R.string.firestore_database_id).isNotBlank())
    }

    @Test
    fun `school seed data has 30-day attendance and rs fees without demo accounts`() {
        val students = SeedData.defaultStudents
        assertEquals(7, students.size)
        assertTrue(students.all { it.parentUid.isEmpty() })

        val attMap = SeedData.build30DayAttendanceMap(0)
        assertEquals(30, attMap.size)
        assertTrue(attMap.values.all { it in setOf(AttendanceStatus.PRESENT.code, AttendanceStatus.ABSENT.code, AttendanceStatus.LEAVE.code) })

        val fees = SeedData.generateFeeRecords(students)
        assertTrue(fees.size >= 21)
        assertEquals("Rs 2,500", SchoolRepository.formatRs(2500))
    }

    @Test
    fun `real bank upi settlement uri parsing and id sanitization work`() {
        assertEquals("OXB-101", SchoolRepository.sanitizeId("  oxb 101  "))
        assertEquals("pa***@oxfordbudgam.edu.in", SchoolRepository.maskEmail("parent@oxfordbudgam.edu.in"))

        val account = SchoolPaymentAccount(
            accountHolderName = "OXFORD SCHOOL EDUCATION BUDGAM",
            upiId = "oxfordbudgam@jkb",
            bankName = "J&K Bank Budgam",
            accountNumber = "0123040100004567",
            ifscCode = "JAKA0BUDGAM"
        )
        assertTrue(account.isConfigured)
        assertTrue(account.hasUpiConfigured)
        assertTrue(account.hasBankConfigured)

        val student = StudentRecord(
            id = "OXB-101",
            name = "Aarav Bhat",
            grade = "Class 10-A",
            rollNo = "OXB-101"
        )
        val pendingFee = FeeRecord(
            id = "fee_OXB-101_nov26",
            studentId = "OXB-101",
            studentName = "Aarav Bhat",
            rollNo = "OXB-101",
            grade = "Class 10-A",
            monthLabel = "November 2026",
            tuitionFee = 2500,
            transportFee = 800,
            examFee = 0,
            totalAmount = 3300,
            status = FeeStatus.PENDING.key
        )

        val upiUri = SchoolRepository.buildUpiPaymentUri(account, pendingFee, student)
        assertEquals("upi", upiUri.scheme)
        assertEquals("pay", upiUri.host)
        assertEquals("oxfordbudgam@jkb", upiUri.getQueryParameter("pa"))
        assertEquals("3300.00", upiUri.getQueryParameter("am"))
        assertEquals("INR", upiUri.getQueryParameter("cu"))

        val (ok, txnId) = SchoolRepository.parseUpiResponse("txnId=UPI99887766&responseCode=00&Status=SUCCESS")
        assertTrue(ok)
        assertEquals("UPI99887766", txnId)

        val (failed, _) = SchoolRepository.parseUpiResponse("responseCode=ZM&Status=FAILURE")
        assertFalse(failed)
    }
}
