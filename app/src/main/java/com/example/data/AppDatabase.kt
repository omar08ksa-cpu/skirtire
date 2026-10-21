package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        HabitEntity::class,
        HabitLogEntity::class,
        MosqueLocationEntity::class,
        TaskEntity::class,
        PrayerLogEntity::class,
        SubjectEntity::class,
        StudySessionEntity::class,
        BlockedAppEntity::class,
        BlockScheduleEntity::class,
        LectureEntity::class,
        WirdProgressEntity::class,
        AzkarLogEntity::class,
        HealthSnapshotEntity::class,
        DailySummaryEntity::class,
        WeeklyReviewEntity::class,
        YearWeekGoalEntity::class,
        SettingEntity::class,
        CategoryEntity::class,
        QuranBookmarkEntity::class,
        AppEventEntity::class,
        LifeMilestoneEntity::class,
        DailyAppUsageArchiveEntity::class
    ],
    version = 16,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun categoryDao(): CategoryDao
    abstract fun taskDao(): TaskDao
    abstract fun prayerLogDao(): PrayerLogDao
    abstract fun studyDao(): StudyDao
    abstract fun protectionDao(): ProtectionDao
    abstract fun fullDayDao(): FullDayDao
    abstract fun insightsDao(): InsightsDao
    abstract fun settingDao(): SettingDao
    abstract fun eventDao(): EventDao
    abstract fun milestoneDao(): MilestoneDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "daily_manager_local.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .fallbackToDestructiveMigrationOnDowngrade(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
