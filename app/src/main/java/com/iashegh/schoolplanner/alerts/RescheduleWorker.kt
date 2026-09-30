package com.iashegh.schoolplanner.alerts

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.iashegh.schoolplanner.data.AppDatabase
import com.iashegh.schoolplanner.data.SettingsRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/** Re-arms exam + morning alarms and deletes exams that ended more than 30 days ago. */
class RescheduleWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val dao = AppDatabase.get(applicationContext).dao()
        val cutoff = LocalDate.now().minusDays(30)
        dao.examsOnce().filter { it.date < cutoff }.forEach { dao.deleteExam(it) }
        val settings = SettingsRepository(applicationContext).flow.first()
        ReminderScheduler.rescheduleAll(applicationContext, settings)
        return Result.success()
    }
}
