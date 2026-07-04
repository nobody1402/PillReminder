package com.example.pillreminder.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Pill::class, DoseLog::class, InteractionRule::class, DrugHistory::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pillDao(): PillDao
    abstract fun doseLogDao(): DoseLogDao
    abstract fun interactionRuleDao(): InteractionRuleDao
    abstract fun drugHistoryDao(): DrugHistoryDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        // نسخه ۲: فقط جدول تازه‌ی «سابقه داروها» اضافه شد؛ جدول‌های قبلی دست‌نخورده می‌مانند
        // تا داروها و لاگ‌های ثبت‌شده‌ی کاربر پاک نشود.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `drug_history` (
                        `normalizedName` TEXT NOT NULL PRIMARY KEY,
                        `displayName` TEXT NOT NULL,
                        `doseAmount` REAL NOT NULL,
                        `foodRelation` TEXT NOT NULL,
                        `waitAfterMinutes` INTEGER NOT NULL,
                        `timesOfDay` TEXT NOT NULL,
                        `treatmentDurationDays` INTEGER,
                        `lowStockThresholdDays` INTEGER NOT NULL,
                        `lastUsedEpochMillis` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pill_reminder.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
