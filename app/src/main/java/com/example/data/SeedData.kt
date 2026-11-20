package com.example.data

object SeedData {
    const val DEMO_ADMIN_EMAIL = "admin@oxfordbudgam.edu.in"
    const val DEMO_ADMIN_PASS = "admin123"
    const val DEMO_STUDENT_EMAIL = "parent@oxfordbudgam.edu.in"
    const val DEMO_STUDENT_PASS = "student123"

    val defaultUsers = listOf(
        UserAccountEntity(
            fullName = "Principal Tariq Ahmad",
            role = UserRole.ADMIN.storageKey,
            rollNumber = "",
            contactIdentifier = DEMO_ADMIN_EMAIL,
            password = DEMO_ADMIN_PASS
        ),
        UserAccountEntity(
            fullName = "Aarav Bhat (Parent: Sanjay Bhat)",
            role = UserRole.STUDENT_PARENT.storageKey,
            rollNumber = "OXB-101",
            contactIdentifier = DEMO_STUDENT_EMAIL,
            password = DEMO_STUDENT_PASS
        )
    )

    val defaultStudents = listOf(
        StudentEntity(
            name = "Aarav Bhat",
            grade = "Class 10-A",
            rollNo = "OXB-101",
            parentName = "Sanjay Bhat",
            contactNumber = "+91 94190 11201",
            admissionYear = "2022"
        ),
        StudentEntity(
            name = "Zoya Mir",
            grade = "Class 10-A",
            rollNo = "OXB-102",
            parentName = "Farooq Ahmad Mir",
            contactNumber = "+91 94190 11202",
            admissionYear = "2022"
        ),
        StudentEntity(
            name = "Hamza Lone",
            grade = "Class 9-B",
            rollNo = "OXB-103",
            parentName = "Shabir Ahmad Lone",
            contactNumber = "+91 94190 11203",
            admissionYear = "2023"
        ),
        StudentEntity(
            name = "Mehreen Dar",
            grade = "Class 9-B",
            rollNo = "OXB-104",
            parentName = "Gulzar Ahmad Dar",
            contactNumber = "+91 94190 11204",
            admissionYear = "2023"
        ),
        StudentEntity(
            name = "Kabir Raina",
            grade = "Class 8-A",
            rollNo = "OXB-105",
            parentName = "Rajesh Raina",
            contactNumber = "+91 94190 11205",
            admissionYear = "2024"
        ),
        StudentEntity(
            name = "Sana Wani",
            grade = "Class 8-A",
            rollNo = "OXB-106",
            parentName = "Manzoor Ahmad Wani",
            contactNumber = "+91 94190 11206",
            admissionYear = "2024"
        ),
        StudentEntity(
            name = "Ayaan Shah",
            grade = "Class 7-A",
            rollNo = "OXB-107",
            parentName = "Bilal Ahmad Shah",
            contactNumber = "+91 94190 11207",
            admissionYear = "2025"
        )
    )

    fun generate30DayAttendance(studentIds: List<Long>): List<AttendanceEntity> {
        val patterns = listOf(
            // Pattern per student index for realistic 30-day presence (75%-97%)
            setOf(6, 14, 22) to setOf(15),          // Aarav: 26P, 3A, 1L
            setOf(9) to setOf(18, 19),              // Zoya: 27P, 1A, 2L
            setOf(3, 11, 17, 25) to setOf(20),      // Hamza: 25P, 4A, 1L
            setOf(12) to setOf(24),                 // Mehreen: 28P, 1A, 1L
            setOf(4, 5, 16, 23, 28) to setOf(10),   // Kabir: 24P, 5A, 1L
            setOf(7, 21) to setOf(2, 26),           // Sana: 26P, 2A, 2L
            setOf(8, 19) to setOf(13)               // Ayaan: 27P, 2A, 1L
        )
        val records = mutableListOf<AttendanceEntity>()
        studentIds.forEachIndexed { idx, studentId ->
            val (absentDays, leaveDays) = patterns[idx % patterns.size]
            for (day in 1..30) {
                val status = when {
                    day in absentDays -> AttendanceStatus.ABSENT.code
                    day in leaveDays -> AttendanceStatus.LEAVE.code
                    else -> AttendanceStatus.PRESENT.code
                }
                records += AttendanceEntity(studentId = studentId, dayOfMonth = day, status = status)
            }
        }
        return records
    }

    fun generateFeeRecords(studentIds: List<Long>): List<FeeRecordEntity> {
        val records = mutableListOf<FeeRecordEntity>()
        studentIds.forEachIndexed { idx, studentId ->
            val tuition = 2200 + (idx % 3) * 300
            val transport = if (idx % 2 == 0) 800 else 650
            // September 2026 - All Paid
            records += FeeRecordEntity(
                studentId = studentId,
                monthLabel = "September 2026",
                tuitionFee = tuition,
                transportFee = transport,
                examFee = 0,
                status = FeeStatus.PAID.key,
                receiptNumber = "OXB-RCP-2609-${101 + idx}",
                paidDate = "08 Sep 2026",
                paymentMethod = if (idx % 2 == 0) "UPI / J&K Bank" else "School Counter"
            )
            // October 2026 - Mostly Paid, one Overdue
            val octOverdue = idx == 4
            records += FeeRecordEntity(
                studentId = studentId,
                monthLabel = "October 2026",
                tuitionFee = tuition,
                transportFee = transport,
                examFee = 450,
                status = if (octOverdue) FeeStatus.OVERDUE.key else FeeStatus.PAID.key,
                receiptNumber = if (octOverdue) "" else "OXB-RCP-2610-${201 + idx}",
                paidDate = if (octOverdue) "" else "10 Oct 2026",
                paymentMethod = if (idx % 3 == 0) "School Counter" else "UPI / J&K Bank"
            )
            // November 2026 - Mix of Paid, Pending, Overdue
            val novStatus = when (idx) {
                1, 3 -> FeeStatus.PAID.key
                2, 5 -> FeeStatus.OVERDUE.key
                else -> FeeStatus.PENDING.key
            }
            records += FeeRecordEntity(
                studentId = studentId,
                monthLabel = "November 2026",
                tuitionFee = tuition,
                transportFee = transport,
                examFee = 0,
                status = novStatus,
                receiptNumber = if (novStatus == FeeStatus.PAID.key) "OXB-RCP-2611-${301 + idx}" else "",
                paidDate = if (novStatus == FeeStatus.PAID.key) "09 Nov 2026" else "",
                paymentMethod = "UPI / J&K Bank"
            )
        }
        return records
    }

    val defaultDiaryEntries = listOf(
        DiaryEntryEntity(
            title = "Winter Session Timing & Parent-Teacher Meeting",
            subject = "School Notice",
            description = "Dear Parents, effective from 23 November 2026, winter school hours will be 9:30 AM to 2:30 PM. Parent-Teacher Conference for Term-II progress review is scheduled this Saturday from 10:00 AM to 1:00 PM at the Budgam Main Campus.",
            targetClass = "All Classes",
            dateLabel = "20 Nov 2026",
            teacherRemarks = "Attendance of at least one parent is mandatory. Please carry the student diary.",
            isNotice = true
        ),
        DiaryEntryEntity(
            title = "Quadratic Equations & Trigonometry Worksheet",
            subject = "Mathematics",
            description = "Solve Exercise 4.3 (Questions 1 to 8) in the fair Mathematics notebook. Focus on discriminant proofs and word problems on height and distance.",
            targetClass = "Class 10-A",
            dateLabel = "20 Nov 2026",
            teacherRemarks = "Show complete step-by-step derivations; neat graphs carry 2 bonus marks.",
            isNotice = false
        ),
        DiaryEntryEntity(
            title = "Light Reflection Lab Diagram & Notes",
            subject = "Science",
            description = "Draw labeled ray diagrams for concave and convex mirrors in your Practical Lab Manual and write five everyday applications of spherical mirrors.",
            targetClass = "Class 9-B",
            dateLabel = "19 Nov 2026",
            teacherRemarks = "Use pencil and ruler for optical ray diagrams.",
            isNotice = false
        ),
        DiaryEntryEntity(
            title = "Essay: Environmental Conservation in Kashmir Valley",
            subject = "English",
            description = "Compose a structured 250-word essay highlighting wetland preservation and sustainable winter practices in Budgam and surrounding areas.",
            targetClass = "Class 10-A",
            dateLabel = "18 Nov 2026",
            teacherRemarks = "Check grammar, paragraph transitions, and underline five new vocabulary words.",
            isNotice = false
        )
    )
}
