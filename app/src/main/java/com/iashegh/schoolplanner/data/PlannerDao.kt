package com.iashegh.schoolplanner.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannerDao {
    // Subjects
    @Query("SELECT * FROM subject ORDER BY name COLLATE NOCASE")
    fun subjects(): Flow<List<Subject>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubject(subject: Subject): Long

    @Delete
    suspend fun deleteSubject(subject: Subject)

    // Timetable
    @Query("SELECT * FROM timetable_slot")
    fun slots(): Flow<List<TimetableSlot>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSlot(slot: TimetableSlot): Long

    @Query("DELETE FROM timetable_slot WHERE dayKey = :dayKey AND period = :period")
    suspend fun clearSlot(dayKey: Int, period: Int)

    @Query("DELETE FROM timetable_slot")
    suspend fun clearSlots()

    // Bells
    @Query("SELECT * FROM bell_period ORDER BY period")
    fun bells(): Flow<List<BellPeriod>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBell(bell: BellPeriod)

    @Query("DELETE FROM bell_period WHERE period = :period")
    suspend fun deleteBell(period: Int)

    // Homework
    @Query("SELECT * FROM homework ORDER BY done, dueDate, priority DESC")
    fun homework(): Flow<List<Homework>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHomework(hw: Homework): Long

    @Delete
    suspend fun deleteHomework(hw: Homework)

    // Exams
    @Query("SELECT * FROM exam ORDER BY date, startMinute")
    fun exams(): Flow<List<Exam>>

    @Query("SELECT * FROM exam")
    suspend fun examsOnce(): List<Exam>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExam(exam: Exam): Long

    @Delete
    suspend fun deleteExam(exam: Exam)

    // Overrides
    @Query("SELECT * FROM day_override ORDER BY date")
    fun overrides(): Flow<List<DayOverride>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOverride(o: DayOverride)

    @Delete
    suspend fun deleteOverride(o: DayOverride)

    // One-shot reads used by backup + alarms
    @Query("SELECT * FROM subject")
    suspend fun subjectsOnce(): List<Subject>

    @Query("SELECT * FROM timetable_slot")
    suspend fun slotsOnce(): List<TimetableSlot>

    @Query("SELECT * FROM bell_period")
    suspend fun bellsOnce(): List<BellPeriod>

    @Query("SELECT * FROM homework")
    suspend fun homeworkOnce(): List<Homework>

    @Query("SELECT * FROM day_override")
    suspend fun overridesOnce(): List<DayOverride>

    // Wipe (children first is not required because of CASCADE, but be explicit)
    @Query("DELETE FROM homework")
    suspend fun clearHomework()

    @Query("DELETE FROM exam")
    suspend fun clearExams()

    @Query("DELETE FROM day_override")
    suspend fun clearOverrides()

    @Query("DELETE FROM bell_period")
    suspend fun clearBells()

    @Query("DELETE FROM subject")
    suspend fun clearSubjects()

    @Update
    suspend fun updateHomework(hw: Homework)
}
