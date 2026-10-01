package com.iashegh.schoolplanner.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.withTransaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE homework ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE exam ADD COLUMN syncId TEXT NOT NULL DEFAULT ''")
        db.execSQL("UPDATE homework SET syncId = lower(hex(randomblob(16)))")
        db.execSQL("UPDATE exam SET syncId = lower(hex(randomblob(16)))")
    }
}

@Database(
    entities = [Subject::class, TimetableSlot::class, BellPeriod::class, Homework::class, Exam::class, DayOverride::class],
    version = 2,
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
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
        }
    }
}
