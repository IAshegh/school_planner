package com.iashegh.schoolplanner.alerts

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.iashegh.schoolplanner.data.AppDatabase
import com.iashegh.schoolplanner.data.Exam
import com.iashegh.schoolplanner.data.Settings
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object ReminderScheduler {
    const val EXTRA_KIND = "kind"
    const val EXTRA_ID = "id"
    const val EXTRA_TITLE = "title"
    const val EXTRA_TEXT = "text"
    const val KIND_EXAM = "exam"
    const val KIND_MORNING = "morning"

    private const val MORNING_CODE = 1
    private fun examCode(examId: Long, slot: Int) = 1000 + (examId.toInt() * 2) + slot

    private fun pending(context: Context, code: Int, extras: Intent.() -> Unit = {}): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply(extras)
        return PendingIntent.getBroadcast(
            context, code, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun setAlarm(context: Context, atMillis: Long, pi: PendingIntent) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (atMillis <= System.currentTimeMillis()) return
        val exactOk = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        try {
            if (exactOk) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMillis, pi)
        }
    }

    private fun millis(dt: LocalDateTime) = dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun scheduleExam(context: Context, exam: Exam, subjectName: String) {
        val examTime = if (exam.startMinute >= 0) LocalTime.of(exam.startMinute / 60, exam.startMinute % 60) else LocalTime.of(8, 0)
        val start = LocalDateTime.of(exam.date, examTime)
        val what = if (exam.title.isBlank()) "$subjectName exam" else "$subjectName: ${exam.title}"

        // 24h before
        setAlarm(context, millis(start.minusHours(24)), pending(context, examCode(exam.id, 0)) {
            putExtra(EXTRA_KIND, KIND_EXAM); putExtra(EXTRA_ID, examCode(exam.id, 0))
            putExtra(EXTRA_TITLE, "Exam tomorrow!")
            putExtra(EXTRA_TEXT, "$what is tomorrow. Time for a last look at your notes.")
        })
        // morning of the exam
        val morning = LocalDateTime.of(exam.date, LocalTime.of(7, 0))
        setAlarm(context, millis(morning), pending(context, examCode(exam.id, 1)) {
            putExtra(EXTRA_KIND, KIND_EXAM); putExtra(EXTRA_ID, examCode(exam.id, 1))
            putExtra(EXTRA_TITLE, "Exam today - you've got this!")
            putExtra(EXTRA_TEXT, "$what is today.")
        })
    }

    fun cancelExam(context: Context, examId: Long) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        for (slot in 0..1) am.cancel(pending(context, examCode(examId, slot)))
    }

    /** Schedules the next morning-agenda alarm (the receiver re-arms itself each time it fires). */
    fun scheduleMorning(context: Context, settings: Settings) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (!settings.morningAlertOn) {
            am.cancel(pending(context, MORNING_CODE))
            return
        }
        val time = LocalTime.of(settings.morningMinute / 60, settings.morningMinute % 60)
        var next = LocalDateTime.of(java.time.LocalDate.now(), time)
        if (!next.isAfter(LocalDateTime.now().plusSeconds(5))) next = next.plusDays(1)
        setAlarm(context, millis(next), pending(context, MORNING_CODE) { putExtra(EXTRA_KIND, KIND_MORNING) })
    }

    suspend fun rescheduleAll(context: Context, settings: Settings) {
        val db = AppDatabase.get(context)
        val dao = db.dao()
        val names = dao.subjectsOnce().associate { it.id to it.name }
        dao.examsOnce().forEach { scheduleExam(context, it, names[it.subjectId] ?: "Exam") }
        scheduleMorning(context, settings)
    }
}
