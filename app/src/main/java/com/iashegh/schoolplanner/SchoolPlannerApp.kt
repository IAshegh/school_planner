package com.iashegh.schoolplanner

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.iashegh.schoolplanner.alerts.Notifications
import com.iashegh.schoolplanner.alerts.RescheduleWorker
import java.util.concurrent.TimeUnit

class SchoolPlannerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannel(this)
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "planner_maintenance",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<RescheduleWorker>(1, TimeUnit.DAYS).build(),
        )
    }
}
