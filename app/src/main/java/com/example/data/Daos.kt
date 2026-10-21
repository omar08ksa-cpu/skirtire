package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE deletedAt IS NULL ORDER BY createdAt ASC")
    fun observeActiveHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE deletedAt IS NULL ORDER BY createdAt ASC")
    suspend fun getActiveHabitsOnce(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE autoSource = :source AND deletedAt IS NULL LIMIT 1")
    suspend fun findHabitByAutoSource(source: String): HabitEntity?

    @Query("SELECT * FROM habits WHERE autoSource = :source AND deletedAt IS NULL")
    suspend fun findHabitsByAutoSource(source: String): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    suspend fun findHabitById(id: String): HabitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabit(habit: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabits(habits: List<HabitEntity>)

    @Query("UPDATE habits SET deletedAt = :now, updatedAt = :now WHERE id = :habitId")
    suspend fun softDeleteHabit(habitId: String, now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM habit_logs WHERE deletedAt IS NULL")
    fun observeAllHabitLogs(): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE id = :id LIMIT 1")
    suspend fun getHabitLogById(id: String): HabitLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabitLog(log: HabitLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabitLogs(logs: List<HabitLogEntity>)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL ORDER BY isCompleted ASC, CASE priority WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 ELSE 3 END ASC, createdAt DESC")
    fun observeActiveTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL")
    suspend fun getActiveTasksOnce(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTasks(tasks: List<TaskEntity>)

    @Query("UPDATE tasks SET deletedAt = :now, updatedAt = :now WHERE id = :taskId OR parentTaskId = :taskId")
    suspend fun softDeleteTaskWithSubtasks(taskId: String, now: Long = System.currentTimeMillis())
}

@Dao
interface PrayerLogDao {
    @Query("SELECT * FROM prayer_logs WHERE dateKey = :dateKey AND deletedAt IS NULL")
    fun observePrayerLogsForDate(dateKey: String): Flow<List<PrayerLogEntity>>

    @Query("SELECT * FROM prayer_logs WHERE dateKey = :dateKey AND deletedAt IS NULL")
    suspend fun getPrayerLogsForDateOnce(dateKey: String): List<PrayerLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPrayerLog(log: PrayerLogEntity)

    @Query("SELECT * FROM mosque_locations WHERE deletedAt IS NULL ORDER BY createdAt ASC")
    fun observeMosqueLocations(): Flow<List<MosqueLocationEntity>>

    @Query("SELECT * FROM mosque_locations WHERE deletedAt IS NULL ORDER BY createdAt ASC")
    suspend fun getMosqueLocationsOnce(): List<MosqueLocationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMosqueLocation(mosque: MosqueLocationEntity)

    @Query("UPDATE mosque_locations SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDeleteMosqueLocation(id: String, now: Long = System.currentTimeMillis())
}

@Dao
interface StudyDao {
    @Query("SELECT * FROM subjects WHERE deletedAt IS NULL ORDER BY createdAt ASC")
    fun observeActiveSubjects(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE deletedAt IS NULL ORDER BY createdAt ASC")
    suspend fun getActiveSubjectsOnce(): List<SubjectEntity>

    @Query("SELECT * FROM subjects WHERE id = :id LIMIT 1")
    suspend fun getSubjectById(id: String): SubjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubject(subject: SubjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSubjects(subjects: List<SubjectEntity>)

    @Query("UPDATE subjects SET deletedAt = :now, updatedAt = :now WHERE id = :subjectId")
    suspend fun softDeleteSubject(subjectId: String, now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM study_sessions WHERE deletedAt IS NULL ORDER BY endedAt DESC")
    fun observeStudySessions(): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions WHERE deletedAt IS NULL ORDER BY endedAt DESC")
    suspend fun getStudySessionsOnce(): List<StudySessionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudySession(session: StudySessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudySessions(sessions: List<StudySessionEntity>)
}

@Dao
interface ProtectionDao {
    @Query("SELECT * FROM blocked_apps WHERE deletedAt IS NULL ORDER BY createdAt ASC")
    fun observeBlockedApps(): Flow<List<BlockedAppEntity>>

    @Query("SELECT * FROM blocked_apps WHERE deletedAt IS NULL ORDER BY createdAt ASC")
    suspend fun getBlockedAppsOnce(): List<BlockedAppEntity>

    @Query("SELECT * FROM blocked_apps WHERE id = :id LIMIT 1")
    suspend fun getBlockedAppById(id: String): BlockedAppEntity?

    @Query("SELECT * FROM blocked_apps WHERE packageName = :packageName AND deletedAt IS NULL LIMIT 1")
    suspend fun getBlockedAppByPackage(packageName: String): BlockedAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBlockedApp(app: BlockedAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBlockedApps(apps: List<BlockedAppEntity>)

    @Query("UPDATE blocked_apps SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDeleteBlockedApp(id: String, now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM block_schedules WHERE deletedAt IS NULL ORDER BY createdAt ASC")
    fun observeBlockSchedules(): Flow<List<BlockScheduleEntity>>

    @Query("SELECT * FROM block_schedules WHERE deletedAt IS NULL ORDER BY createdAt ASC")
    suspend fun getBlockSchedulesOnce(): List<BlockScheduleEntity>

    @Query("SELECT * FROM block_schedules WHERE id = :id LIMIT 1")
    suspend fun getBlockScheduleById(id: String): BlockScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBlockSchedule(schedule: BlockScheduleEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBlockSchedules(schedules: List<BlockScheduleEntity>)

    @Query("UPDATE block_schedules SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDeleteBlockSchedule(id: String, now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM daily_app_usage_archive WHERE dateKey = :dateKey ORDER BY usedSeconds DESC")
    fun observeDailyUsageArchive(dateKey: String): Flow<List<DailyAppUsageArchiveEntity>>

    @Query("SELECT * FROM daily_app_usage_archive WHERE dateKey = :dateKey ORDER BY usedSeconds DESC")
    suspend fun getDailyUsageArchiveOnce(dateKey: String): List<DailyAppUsageArchiveEntity>

    @Query("SELECT * FROM daily_app_usage_archive WHERE packageName = :packageName ORDER BY dateKey DESC")
    suspend fun getPackageUsageHistory(packageName: String): List<DailyAppUsageArchiveEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyUsageArchives(items: List<DailyAppUsageArchiveEntity>)

    @Query("UPDATE blocked_apps SET isUninstalled = :uninstalled, updatedAt = :now WHERE packageName = :packageName")
    suspend fun setAppUninstalledStatus(packageName: String, uninstalled: Boolean, now: Long = System.currentTimeMillis())
}

@Dao
interface FullDayDao {
    @Query("SELECT * FROM lectures WHERE deletedAt IS NULL ORDER BY dayOfWeek ASC, startTime24 ASC")
    fun observeLectures(): Flow<List<LectureEntity>>

    @Query("SELECT * FROM lectures WHERE deletedAt IS NULL ORDER BY dayOfWeek ASC, startTime24 ASC")
    suspend fun getLecturesOnce(): List<LectureEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLecture(lecture: LectureEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLectures(lectures: List<LectureEntity>)

    @Query("UPDATE lectures SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDeleteLecture(id: String, now: Long = System.currentTimeMillis())

    @Query("SELECT * FROM wird_progress WHERE deletedAt IS NULL LIMIT 1")
    fun observeWirdProgress(): Flow<WirdProgressEntity?>

    @Query("SELECT * FROM wird_progress WHERE deletedAt IS NULL LIMIT 1")
    suspend fun getWirdProgressOnce(): WirdProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWirdProgress(wird: WirdProgressEntity)

    @Query("SELECT * FROM quran_bookmarks ORDER BY createdAt DESC")
    fun observeQuranBookmarks(): Flow<List<QuranBookmarkEntity>>

    @Query("SELECT * FROM quran_bookmarks ORDER BY createdAt DESC")
    suspend fun getQuranBookmarksOnce(): List<QuranBookmarkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuranBookmark(bookmark: QuranBookmarkEntity)

    @Query("DELETE FROM quran_bookmarks WHERE id = :id")
    suspend fun deleteQuranBookmark(id: String)

    @Query("SELECT * FROM quran_bookmarks WHERE pageNumber = :page LIMIT 1")
    suspend fun getBookmarkForPage(page: Int): QuranBookmarkEntity?

    @Query("SELECT * FROM azkar_logs WHERE dateKey = :dateKey AND deletedAt IS NULL ORDER BY createdAt ASC")
    fun observeAzkarLogsForDate(dateKey: String): Flow<List<AzkarLogEntity>>

    @Query("SELECT * FROM azkar_logs WHERE dateKey = :dateKey AND deletedAt IS NULL ORDER BY createdAt ASC")
    suspend fun getAzkarLogsForDateOnce(dateKey: String): List<AzkarLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAzkarLog(log: AzkarLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAzkarLogs(logs: List<AzkarLogEntity>)

    @Query("SELECT * FROM health_snapshots WHERE dateKey = :dateKey AND deletedAt IS NULL LIMIT 1")
    fun observeHealthSnapshotForDate(dateKey: String): Flow<HealthSnapshotEntity?>

    @Query("SELECT * FROM health_snapshots WHERE dateKey = :dateKey AND deletedAt IS NULL LIMIT 1")
    suspend fun getHealthSnapshotForDateOnce(dateKey: String): HealthSnapshotEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHealthSnapshot(snapshot: HealthSnapshotEntity)
}

@Dao
interface InsightsDao {
    @Query("SELECT * FROM daily_summaries WHERE dateKey = :dateKey AND deletedAt IS NULL LIMIT 1")
    fun observeDailySummaryForDate(dateKey: String): Flow<DailySummaryEntity?>

    @Query("SELECT * FROM daily_summaries WHERE dateKey = :dateKey AND deletedAt IS NULL LIMIT 1")
    suspend fun getDailySummaryForDateOnce(dateKey: String): DailySummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailySummary(summary: DailySummaryEntity)

    @Query("SELECT * FROM weekly_reviews WHERE deletedAt IS NULL LIMIT 1")
    fun observeCurrentWeeklyReview(): Flow<WeeklyReviewEntity?>

    @Query("SELECT * FROM weekly_reviews WHERE deletedAt IS NULL LIMIT 1")
    suspend fun getCurrentWeeklyReviewOnce(): WeeklyReviewEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWeeklyReview(review: WeeklyReviewEntity)

    @Query("SELECT * FROM year_week_goals WHERE deletedAt IS NULL ORDER BY weekNumber ASC")
    fun observeYearWeekGoals(): Flow<List<YearWeekGoalEntity>>

    @Query("SELECT * FROM year_week_goals WHERE deletedAt IS NULL ORDER BY weekNumber ASC")
    suspend fun getYearWeekGoalsOnce(): List<YearWeekGoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertYearWeekGoal(goal: YearWeekGoalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertYearWeekGoals(goals: List<YearWeekGoalEntity>)
}

@Dao
interface SettingDao {
    @Query("SELECT * FROM settings")
    fun observeSettings(): Flow<List<SettingEntity>>

    @Query("SELECT value FROM settings WHERE `key` = :key LIMIT 1")
    suspend fun getSettingValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSetting(setting: SettingEntity)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories WHERE deletedAt IS NULL ORDER BY createdAt ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE deletedAt IS NULL ORDER BY createdAt ASC")
    suspend fun getCategoriesOnce(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :id LIMIT 1")
    suspend fun getCategoryById(id: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategory(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCategories(categories: List<CategoryEntity>)

    @Query("UPDATE categories SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun softDeleteCategory(id: String, now: Long = System.currentTimeMillis())
}

@Dao
interface EventDao {
    @Query("SELECT * FROM app_events WHERE deletedAt IS NULL ORDER BY dateKey ASC, startTime ASC")
    fun observeEvents(): Flow<List<AppEventEntity>>

    @Query("SELECT * FROM app_events WHERE deletedAt IS NULL ORDER BY dateKey ASC, startTime ASC")
    suspend fun getEventsOnce(): List<AppEventEntity>

    @Query("SELECT * FROM app_events WHERE dateKey = :dateKey AND deletedAt IS NULL ORDER BY startTime ASC")
    fun observeEventsForDate(dateKey: String): Flow<List<AppEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvent(event: AppEventEntity)

    @Query("UPDATE app_events SET deletedAt = :now, updatedAt = :now WHERE id = :id")
    suspend fun deleteEvent(id: String, now: Long = System.currentTimeMillis())
}

@Dao
interface MilestoneDao {
    @Query("SELECT * FROM life_milestones ORDER BY dateKey ASC")
    fun observeMilestones(): Flow<List<LifeMilestoneEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMilestone(milestone: LifeMilestoneEntity)

    @Query("DELETE FROM life_milestones WHERE id = :id")
    suspend fun deleteMilestone(id: String)
}

