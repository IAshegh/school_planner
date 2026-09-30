package com.iashegh.schoolplanner.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "subject")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** ARGB colour as Int */
    val color: Int,
    /** key into [SubjectIcons] */
    val icon: String = "star",
)

/**
 * One lesson in the recurring template.
 * [dayKey] meaning depends on the cycle mode, see [ScheduleResolver.columns].
 */
@Entity(
    tableName = "timetable_slot",
    foreignKeys = [ForeignKey(
        entity = Subject::class,
        parentColumns = ["id"],
        childColumns = ["subjectId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("subjectId")],
)
data class TimetableSlot(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val dayKey: Int,
    val period: Int,
    val room: String = "",
    val teacher: String = "",
)

/** Bell schedule: start / end of each numbered period, in minutes after midnight. */
@Entity(tableName = "bell_period")
data class BellPeriod(
    @PrimaryKey val period: Int,
    val startMinute: Int,
    val endMinute: Int,
)

@Entity(
    tableName = "homework",
    foreignKeys = [ForeignKey(
        entity = Subject::class,
        parentColumns = ["id"],
        childColumns = ["subjectId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("subjectId")],
)
data class Homework(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val title: String,
    val notes: String = "",
    val dueDate: LocalDate,
    /** 0 = normal, 1 = important, 2 = urgent */
    val priority: Int = 0,
    val done: Boolean = false,
    val doneDate: LocalDate? = null,
    val photoUri: String? = null,
)

@Entity(
    tableName = "exam",
    foreignKeys = [ForeignKey(
        entity = Subject::class,
        parentColumns = ["id"],
        childColumns = ["subjectId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("subjectId")],
)
data class Exam(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val title: String = "",
    val date: LocalDate,
    /** minutes after midnight, or -1 when no time was given */
    val startMinute: Int = -1,
    val room: String = "",
    val topics: String = "",
    val notes: String = "",
)

/**
 * A one-off change for a single date. Either the day is off (holiday / teacher workday),
 * or [replacementJson] holds a replacement lesson list: [{"p":1,"s":subjectId,"r":"room"}, ...]
 */
@Entity(tableName = "day_override")
data class DayOverride(
    @PrimaryKey @ColumnInfo(name = "date") val date: LocalDate,
    val isHoliday: Boolean = true,
    val note: String = "",
    val replacementJson: String? = null,
)
