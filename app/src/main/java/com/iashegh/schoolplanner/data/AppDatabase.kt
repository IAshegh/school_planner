package com.iashegh.schoolplanner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.withTransaction

@Database(
    entities = [Subject::class, TimetableSlot::class, BellPeriod::class, Homework::class, Exam::class, DayOverride::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): PlannerDao

    suspend fun <T> tx(block: suspend () -> T): T = withTransaction(block)

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "school_planner.db",
            ).build().also { instance = it }
        }
    }
}
