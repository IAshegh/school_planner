package com.iashegh.schoolplanner.alerts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.iashegh.schoolplanner.data.AppDatabase
import com.iashegh.schoolplanner.data.ScheduleResolver
import com.iashegh.schoolplanner.data.SettingsRepository
import com.iashegh.schoolplanner.data.fmtMinute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra(ReminderScheduler.EXTRA_KIND) ?: return
        if (kind == ReminderScheduler.KIND_EXAM) {
            Notifications.show(
                context,
                intent.getIntExtra(ReminderScheduler.EXTRA_ID, 1),
                intent.getStringExtra(ReminderScheduler.EXTRA_TITLE).orEmpty(),
                intent.getStringExtra(ReminderScheduler.EXTRA_TEXT).orEmpty(),
            )
            return
        }
        // Morning agenda: needs the database, so finish asynchronously.
        val result = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settingsRepo = SettingsRepository(app)
                val settings = settingsRepo.flow.first()
                val dao = AppDatabase.get(app).dao()
                val plan = ScheduleResolver.resolve(
                    LocalDate.now(), settings, dao.subjectsOnce(), dao.slotsOnce(), dao.bellsOnce(), dao.overridesOnce(),
                )
                if (plan.isSchoolDay && plan.lessons.isNotEmpty()) {
                    val text = plan.lessons.joinToString("\n") {
                        val t = it.startMinute?.let { m -> fmtMinute(m) + "  " }.orEmpty()
                        t + it.subject.name + if (it.room.isNotBlank()) " (${it.room})" else ""
                    }
                    Notifications.show(app, 1, "Today's classes", text)
                }
                ReminderScheduler.scheduleMorning(app, settings)
            } finally {
                result.finish()
            }
        }
    }
}
