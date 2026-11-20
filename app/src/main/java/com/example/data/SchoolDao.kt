package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SchoolDao {
    // User Accounts
    @Query("SELECT * FROM user_accounts WHERE LOWER(contactIdentifier) = LOWER(:identifier) AND password = :password LIMIT 1")
    suspend fun authenticateUser(identifier: String, password: String): UserAccountEntity?

    @Query("SELECT * FROM user_accounts WHERE LOWER(contactIdentifier) = LOWER(:identifier) LIMIT 1")
    suspend fun findUserByIdentifier(identifier: String): UserAccountEntity?

    @Query("SELECT * FROM user_accounts WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: Long): UserAccountEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserAccountEntity): Long

    // Students
    @Query("SELECT * FROM students ORDER BY grade ASC, rollNo ASC")
    fun observeStudents(): Flow<List<StudentEntity>>

    @Query("SELECT COUNT(*) FROM students")
    suspend fun countStudents(): Int

    @Query("SELECT * FROM students WHERE UPPER(rollNo) = UPPER(:rollNo) LIMIT 1")
    suspend fun findStudentByRollNo(rollNo: String): StudentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<StudentEntity>): List<Long>

    @Update
    suspend fun updateStudent(student: StudentEntity)

    @Query("DELETE FROM students WHERE id = :studentId")
    suspend fun deleteStudentById(studentId: Long)

    // 30-Day Attendance
    @Query("SELECT * FROM attendance")
    fun observeAllAttendance(): Flow<List<AttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAttendance(attendance: AttendanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendanceBatch(records: List<AttendanceEntity>)

    // Monthly Fees (Rs)
    @Query("SELECT * FROM fee_records ORDER BY id DESC")
    fun observeAllFees(): Flow<List<FeeRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeeRecord(record: FeeRecordEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeeRecords(records: List<FeeRecordEntity>)

    @Update
    suspend fun updateFeeRecord(record: FeeRecordEntity)

    @Query("DELETE FROM fee_records WHERE id = :feeId")
    suspend fun deleteFeeById(feeId: Long)

    // School Diary & Notices
    @Query("SELECT * FROM diary_entries ORDER BY id DESC")
    fun observeDiaryEntries(): Flow<List<DiaryEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiaryEntry(entry: DiaryEntryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDiaryEntries(entries: List<DiaryEntryEntity>): List<Long>

    @Query("DELETE FROM diary_entries WHERE id = :diaryId")
    suspend fun deleteDiaryById(diaryId: Long)

    // Homework Completion
    @Query("SELECT * FROM homework_completions WHERE studentId = :studentId")
    fun observeHomeworkCompletions(studentId: Long): Flow<List<HomeworkCompletionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHomeworkCompletion(completion: HomeworkCompletionEntity)
}
