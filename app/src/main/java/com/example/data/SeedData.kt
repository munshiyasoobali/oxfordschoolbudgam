package com.example.data

object SeedData {
    val defaultStudents = listOf(
        StudentRecord(
            id = "OXB-101",
            name = "Aarav Bhat",
            grade = "Class 10-A",
            rollNo = "OXB-101",
            parentName = "Sanjay Bhat",
            contactNumber = "+91 94190 11201",
            admissionYear = "2022",
            parentUid = ""
        ),
        StudentRecord(
            id = "OXB-102",
            name = "Zoya Mir",
            grade = "Class 10-A",
            rollNo = "OXB-102",
            parentName = "Farooq Ahmad Mir",
            contactNumber = "+91 94190 11202",
            admissionYear = "2022",
            parentUid = ""
        ),
        StudentRecord(
            id = "OXB-103",
            name = "Hamza Lone",
            grade = "Class 9-B",
            rollNo = "OXB-103",
            parentName = "Shabir Ahmad Lone",
            contactNumber = "+91 94190 11203",
            admissionYear = "2023",
            parentUid = ""
        ),
        StudentRecord(
            id = "OXB-104",
            name = "Mehreen Dar",
            grade = "Class 9-B",
            rollNo = "OXB-104",
            parentName = "Gulzar Ahmad Dar",
            contactNumber = "+91 94190 11204",
            admissionYear = "2023",
            parentUid = ""
        ),
        StudentRecord(
            id = "OXB-105",
            name = "Kabir Raina",
            grade = "Class 8-A",
            rollNo = "OXB-105",
            parentName = "Rajesh Raina",
            contactNumber = "+91 94190 11205",
            admissionYear = "2024",
            parentUid = ""
        ),
        StudentRecord(
            id = "OXB-106",
            name = "Sana Wani",
            grade = "Class 8-A",
            rollNo = "OXB-106",
            parentName = "Manzoor Ahmad Wani",
            contactNumber = "+91 94190 11206",
            admissionYear = "2024",
            parentUid = ""
        ),
        StudentRecord(
            id = "OXB-107",
            name = "Ayaan Shah",
            grade = "Class 7-A",
            rollNo = "OXB-107",
            parentName = "Bilal Ahmad Shah",
            contactNumber = "+91 94190 11207",
            admissionYear = "2025",
            parentUid = ""
        )
    )

    fun build30DayAttendanceMap(studentIndex: Int): Map<String, String> {
        val patterns = listOf(
            setOf(6, 14, 22) to setOf(15),
            setOf(9) to setOf(18, 19),
            setOf(3, 11, 17, 25) to setOf(20),
            setOf(12) to setOf(24),
            setOf(4, 5, 16, 23, 28) to setOf(10),
            setOf(7, 21) to setOf(2, 26),
            setOf(8, 19) to setOf(13)
        )
        val (absentDays, leaveDays) = patterns[studentIndex % patterns.size]
        val map = LinkedHashMap<String, String>(30)
        for (day in 1..30) {
            map[day.toString()] = when {
                day in absentDays -> AttendanceStatus.ABSENT.code
                day in leaveDays -> AttendanceStatus.LEAVE.code
                else -> AttendanceStatus.PRESENT.code
            }
        }
        return map
    }

    fun defaultPresentMap(): Map<String, String> =
        (1..30).associate { it.toString() to AttendanceStatus.PRESENT.code }

    fun generateFeeRecords(students: List<StudentRecord>): List<FeeRecord> {
        val records = mutableListOf<FeeRecord>()
        students.forEachIndexed { idx, st ->
            val tuition = 2200 + (idx % 3) * 300
            val transport = if (idx % 2 == 0) 800 else 650

            records += FeeRecord(
                id = "fee_${st.id}_sep26",
                studentId = st.id,
                studentName = st.name,
                rollNo = st.rollNo,
                grade = st.grade,
                parentUid = "",
                monthLabel = "September 2026",
                tuitionFee = tuition,
                transportFee = transport,
                examFee = 0,
                totalAmount = tuition + transport,
                status = FeeStatus.PAID.key,
                receiptNumber = "OXB-RCP-2609-${101 + idx}",
                paidDate = "08 Sep 2026",
                paymentMethod = "Bank Transfer",
                transactionRef = "JKB260908${1001 + idx}",
                payerAccountNote = "Parent Bank Account"
            )

            val octOverdue = idx == 4
            records += FeeRecord(
                id = "fee_${st.id}_oct26",
                studentId = st.id,
                studentName = st.name,
                rollNo = st.rollNo,
                grade = st.grade,
                parentUid = "",
                monthLabel = "October 2026",
                tuitionFee = tuition,
                transportFee = transport,
                examFee = 450,
                totalAmount = tuition + transport + 450,
                status = if (octOverdue) FeeStatus.OVERDUE.key else FeeStatus.PAID.key,
                receiptNumber = if (octOverdue) "" else "OXB-RCP-2610-${201 + idx}",
                paidDate = if (octOverdue) "" else "10 Oct 2026",
                paymentMethod = if (octOverdue) "School Counter" else "UPI App",
                transactionRef = if (octOverdue) "" else "UPI261010${2001 + idx}",
                payerAccountNote = if (octOverdue) "" else "Parent UPI"
            )

            val novStatus = when (idx) {
                1, 3 -> FeeStatus.PAID.key
                2, 5 -> FeeStatus.OVERDUE.key
                else -> FeeStatus.PENDING.key
            }
            records += FeeRecord(
                id = "fee_${st.id}_nov26",
                studentId = st.id,
                studentName = st.name,
                rollNo = st.rollNo,
                grade = st.grade,
                parentUid = "",
                monthLabel = "November 2026",
                tuitionFee = tuition,
                transportFee = transport,
                examFee = 0,
                totalAmount = tuition + transport,
                status = novStatus,
                receiptNumber = if (novStatus == FeeStatus.PAID.key) "OXB-RCP-2611-${301 + idx}" else "",
                paidDate = if (novStatus == FeeStatus.PAID.key) "09 Nov 2026" else "",
                paymentMethod = if (novStatus == FeeStatus.PAID.key) "UPI App" else "School Counter",
                transactionRef = if (novStatus == FeeStatus.PAID.key) "UPI261109${3001 + idx}" else "",
                payerAccountNote = if (novStatus == FeeStatus.PAID.key) "Parent UPI" else ""
            )
        }
        return records
    }

    val defaultDiaryEntries = listOf(
        DiaryEntry(
            id = "diary_winter_ptm",
            title = "Winter Session Timing & Parent-Teacher Meeting",
            subject = "School Notice",
            description = "Dear Parents, effective from 23 November 2026, winter school hours will be 9:30 AM to 2:30 PM. Parent-Teacher Conference for Term-II progress review is scheduled this Saturday from 10:00 AM to 1:00 PM at the Budgam Main Campus.",
            targetClass = "All Classes",
            dateLabel = "20 Nov 2026",
            teacherRemarks = "Attendance of at least one parent is mandatory. Please carry the student diary.",
            entryType = "NOTICE"
        ),
        DiaryEntry(
            id = "diary_math_quadratics",
            title = "Quadratic Equations & Trigonometry Worksheet",
            subject = "Mathematics",
            description = "Solve Exercise 4.3 (Questions 1 to 8) in the fair Mathematics notebook. Focus on discriminant proofs and word problems on height and distance.",
            targetClass = "Class 10-A",
            dateLabel = "20 Nov 2026",
            teacherRemarks = "Show complete step-by-step derivations; neat graphs carry 2 bonus marks.",
            entryType = "HOMEWORK"
        ),
        DiaryEntry(
            id = "diary_science_optics",
            title = "Light Reflection Lab Diagram & Notes",
            subject = "Science",
            description = "Draw labeled ray diagrams for concave and convex mirrors in your Practical Lab Manual and write five everyday applications of spherical mirrors.",
            targetClass = "Class 9-B",
            dateLabel = "19 Nov 2026",
            teacherRemarks = "Use pencil and ruler for optical ray diagrams.",
            entryType = "HOMEWORK"
        ),
        DiaryEntry(
            id = "diary_english_essay",
            title = "Essay: Environmental Conservation in Kashmir Valley",
            subject = "English",
            description = "Compose a structured 250-word essay highlighting wetland preservation and sustainable winter practices in Budgam and surrounding areas.",
            targetClass = "Class 10-A",
            dateLabel = "18 Nov 2026",
            teacherRemarks = "Check grammar, paragraph transitions, and underline five new vocabulary words.",
            entryType = "HOMEWORK"
        )
    )
}
