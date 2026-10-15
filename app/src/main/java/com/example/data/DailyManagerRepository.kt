package com.example.data

import com.example.core.fullday.IcsCalendarParser
import com.example.core.insights.DailyInsightsEngine
import com.example.core.prayer.PrayerType
import com.example.core.prayer.UmmAlQuraCalculator
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import kotlin.math.roundToInt

class DailyManagerRepository(
    private val db: AppDatabase
) {
    private val habitDao = db.habitDao()
    private val taskDao = db.taskDao()
    private val prayerLogDao = db.prayerLogDao()
    private val studyDao = db.studyDao()
    private val protectionDao = db.protectionDao()
    private val fullDayDao = db.fullDayDao()
    private val insightsDao = db.insightsDao()
    private val settingDao = db.settingDao()
    private val categoryDao = db.categoryDao()
    private val eventDao = db.eventDao()

    val categoriesFlow: Flow<List<CategoryEntity>> = categoryDao.observeCategories()
    val activeHabitsFlow: Flow<List<HabitEntity>> = habitDao.observeActiveHabits()
    val appEventsFlow: Flow<List<AppEventEntity>> = eventDao.observeEvents()
    val allHabitLogsFlow: Flow<List<HabitLogEntity>> = habitDao.observeAllHabitLogs()
    val activeTasksFlow: Flow<List<TaskEntity>> = taskDao.observeActiveTasks()
    val activeSubjectsFlow: Flow<List<SubjectEntity>> = studyDao.observeActiveSubjects()
    val studySessionsFlow: Flow<List<StudySessionEntity>> = studyDao.observeStudySessions()
    val blockedAppsFlow: Flow<List<BlockedAppEntity>> = protectionDao.observeBlockedApps()
    val blockSchedulesFlow: Flow<List<BlockScheduleEntity>> = protectionDao.observeBlockSchedules()
    val lecturesFlow: Flow<List<LectureEntity>> = fullDayDao.observeLectures()
    val wirdProgressFlow: Flow<WirdProgressEntity?> = fullDayDao.observeWirdProgress()
    val quranBookmarksFlow: Flow<List<QuranBookmarkEntity>> = fullDayDao.observeQuranBookmarks()
    val currentWeeklyReviewFlow: Flow<WeeklyReviewEntity?> = insightsDao.observeCurrentWeeklyReview()
    val yearWeekGoalsFlow: Flow<List<YearWeekGoalEntity>> = insightsDao.observeYearWeekGoals()
    val settingsFlow: Flow<List<SettingEntity>> = settingDao.observeSettings()
    val mosqueLocationsFlow: Flow<List<MosqueLocationEntity>> = prayerLogDao.observeMosqueLocations()

    fun observePrayerLogsForDate(dateKey: String): Flow<List<PrayerLogEntity>> {
        return prayerLogDao.observePrayerLogsForDate(dateKey)
    }

    fun observeAzkarLogsForDate(dateKey: String): Flow<List<AzkarLogEntity>> {
        return fullDayDao.observeAzkarLogsForDate(dateKey)
    }

    fun observeHealthSnapshotForDate(dateKey: String): Flow<HealthSnapshotEntity?> {
        return fullDayDao.observeHealthSnapshotForDate(dateKey)
    }

    fun observeDailySummaryForDate(dateKey: String): Flow<DailySummaryEntity?> {
        return insightsDao.observeDailySummaryForDate(dateKey)
    }

    suspend fun ensureSeeded() {
        val now = System.currentTimeMillis()
        val todayKey = UmmAlQuraCalculator.todayDateKey(now)

        val cleanInitDone = settingDao.getSettingValue("clean_init_v7")
        if (cleanInitDone != "true") {
            settingDao.setSetting(SettingEntity("city_id", "riyadh", now))
            settingDao.setSetting(SettingEntity("dark_mode", "false", now))
            settingDao.setSetting(SettingEntity("auto_schedule_adhan", "true", now))
            settingDao.setSetting(SettingEntity("auto_open_standby", "false", now))
            settingDao.setSetting(SettingEntity("standby_on_charging", "true", now))
            settingDao.setSetting(SettingEntity("sleep_mode_active", "false", now))
            settingDao.setSetting(SettingEntity("strict_protection_enabled", "true", now))

            val defaultSchedules = listOf(
                BlockScheduleEntity(
                    id = "schedule_wake_alarm",
                    title = "منبه الاستيقاظ",
                    scheduleType = "WAKE_ALARM",
                    startTime24 = "04:30",
                    endTime24 = "04:45",
                    verificationMethod = "LOCAL_IMAGE",
                    qrReferenceCode = "WAKE-QR-2026",
                    placeName = "مغسلة الحمام / المكتب",
                    emergencyExitType = "MATH_3",
                    isEnabled = true,
                    strictAppProtection = true,
                    createdAt = now
                ),
                BlockScheduleEntity(
                    id = "schedule_sleep_mode",
                    title = "وضع النوم الليلي",
                    scheduleType = "SLEEP_MODE",
                    startTime24 = "23:00",
                    endTime24 = "04:30",
                    verificationMethod = "LOCAL_IMAGE",
                    qrReferenceCode = "WAKE-QR-2026",
                    placeName = "مغسلة الحمام / المكتب",
                    emergencyExitType = "MATH_3",
                    isEnabled = true,
                    strictAppProtection = true,
                    createdAt = now
                )
            )
            protectionDao.upsertBlockSchedules(defaultSchedules)

            fullDayDao.upsertWirdProgress(
                WirdProgressEntity(
                    id = "khatmah_main",
                    currentPage = 1,
                    totalPages = 604,
                    dailyTargetPages = 2,
                    currentJuz = 1,
                    bookmarkSurahName = "سورة الفاتحة",
                    bookmarkAyahNumber = 1,
                    bookmarkNote = "",
                    completedKhatmahCount = 0,
                    lastReadDateKey = todayKey,
                    todayPagesRead = 0,
                    createdAt = now,
                    updatedAt = now
                )
            )

            settingDao.setSetting(SettingEntity("clean_init_v7", "true", now))
        }

        ensureTodayAzkarSeeded(todayKey)
        ensureTodayHealthSeeded(todayKey)

        if (categoryDao.getCategoriesOnce().isEmpty()) {
            categoryDao.upsertCategories(com.example.core.habits.HabitHelper.DEFAULT_CATEGORIES)
        }
    }

    private suspend fun ensureTodayAzkarSeeded(todayKey: String) {
        val existing = fullDayDao.getAzkarLogsForDateOnce(todayKey)
        if (existing.isEmpty()) {
            val now = System.currentTimeMillis()
            val logs = listOf(
                AzkarLogEntity(
                    id = "${todayKey}_MORNING",
                    dateKey = todayKey,
                    categoryCode = "MORNING",
                    categoryTitle = "أذكار الصباح",
                    currentCount = 0,
                    targetCount = 24,
                    isCompleted = false,
                    createdAt = now
                ),
                AzkarLogEntity(
                    id = "${todayKey}_AFTER_PRAYER",
                    dateKey = todayKey,
                    categoryCode = "AFTER_PRAYER",
                    categoryTitle = "أذكار بعد الصلاة",
                    currentCount = 0,
                    targetCount = 33,
                    isCompleted = false,
                    createdAt = now + 1
                ),
                AzkarLogEntity(
                    id = "${todayKey}_EVENING",
                    dateKey = todayKey,
                    categoryCode = "EVENING",
                    categoryTitle = "أذكار المساء",
                    currentCount = 0,
                    targetCount = 24,
                    isCompleted = false,
                    createdAt = now + 2
                ),
                AzkarLogEntity(
                    id = "${todayKey}_SLEEP",
                    dateKey = todayKey,
                    categoryCode = "SLEEP",
                    categoryTitle = "أذكار النوم",
                    currentCount = 0,
                    targetCount = 15,
                    isCompleted = false,
                    createdAt = now + 3
                )
            )
            fullDayDao.upsertAzkarLogs(logs)
        }
    }

    private suspend fun ensureTodayHealthSeeded(todayKey: String) {
        val existing = fullDayDao.getHealthSnapshotForDateOnce(todayKey)
        if (existing == null) {
            val now = System.currentTimeMillis()
            fullDayDao.upsertHealthSnapshot(
                HealthSnapshotEntity(
                    id = "health_$todayKey",
                    dateKey = todayKey,
                    stepsCount = 0,
                    stepsGoal = 8000,
                    activeCalories = 0,
                    burnedCalories = 0,
                    consumedCalories = 0,
                    dailyCalorieLimit = 2100,
                    sleepHours = 0f,
                    distanceMeters = 0,
                    exerciseMinutes = 0,
                    waterMl = 0,
                    avgHeartRateBpm = 0,
                    mealsLogJson = "",
                    syncSource = "Health Connect",
                    createdAt = now,
                    updatedAt = now
                )
            )
        }
    }

    // --- المساجد المعتمدة والتحقق المكاني بالـ GPS ---

    suspend fun addMosqueLocation(
        name: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Int
    ): MosqueLocationEntity {
        val now = System.currentTimeMillis()
        val entity = MosqueLocationEntity(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifBlank { "مسجدي" },
            latitude = latitude,
            longitude = longitude,
            radiusMeters = radiusMeters.coerceIn(30, 500),
            createdAt = now,
            updatedAt = now
        )
        prayerLogDao.upsertMosqueLocation(entity)
        return entity
    }

    suspend fun deleteMosqueLocation(id: String) {
        prayerLogDao.softDeleteMosqueLocation(id)
    }

    suspend fun getApprovedMosquesOnce(): List<MosqueLocationEntity> {
        return prayerLogDao.getMosqueLocationsOnce()
    }

    /**
     * تسجيل الصلاة بعد التحقق المكاني الفعلي (GPS) من التواجد داخل نطاق المسجد
     */
    suspend fun logPrayerVerifiedAtMosque(
        dateKey: String,
        prayerType: PrayerType,
        mosqueName: String,
        distanceMeters: Int
    ) {
        if (!prayerType.isObligatory) return
        val now = System.currentTimeMillis()
        val logId = "${dateKey}_${prayerType.id}"
        prayerLogDao.upsertPrayerLog(
            PrayerLogEntity(
                id = logId,
                dateKey = dateKey,
                prayerName = prayerType.id,
                status = "ON_TIME",
                mosqueName = mosqueName,
                distanceMeters = distanceMeters,
                verifiedByLocation = true,
                loggedAt = now,
                updatedAt = now
            )
        )
        syncPrayerHabitsForDate(dateKey, now, "تحقق مكاني في «$mosqueName» (${distanceMeters}م)")
    }

    /**
     * تسجيل عدم أداء الصلاة / إلغاء حالتها
     */
    suspend fun logPrayerStatus(
        dateKey: String,
        prayerType: PrayerType,
        status: String = "NOT_PRAYED"
    ) {
        if (!prayerType.isObligatory) return
        val now = System.currentTimeMillis()
        val logId = "${dateKey}_${prayerType.id}"
        prayerLogDao.upsertPrayerLog(
            PrayerLogEntity(
                id = logId,
                dateKey = dateKey,
                prayerName = prayerType.id,
                status = status,
                verifiedByLocation = status == "ON_TIME",
                loggedAt = now,
                updatedAt = now
            )
        )
        syncPrayerHabitsForDate(dateKey, now, "سجل الصلاة")
    }

    /**
     * تسجيل الصلوات التي مضى وقتها دون تحقق مكاني كـ «متأخر» تلقائياً
     */
    suspend fun markMissedPrayersAsLate(
        dateKey: String,
        missedTypes: List<PrayerType>
    ) {
        if (missedTypes.isEmpty()) return
        val now = System.currentTimeMillis()
        val existingLogs = prayerLogDao.getPrayerLogsForDateOnce(dateKey).associateBy { it.prayerName }
        for (prayerType in missedTypes) {
            if (!prayerType.isObligatory) continue
            val existing = existingLogs[prayerType.id]
            if (existing == null || existing.status == "PENDING") {
                prayerLogDao.upsertPrayerLog(
                    PrayerLogEntity(
                        id = "${dateKey}_${prayerType.id}",
                        dateKey = dateKey,
                        prayerName = prayerType.id,
                        status = "LATE",
                        mosqueName = "",
                        distanceMeters = 0,
                        verifiedByLocation = false,
                        loggedAt = now,
                        updatedAt = now
                    )
                )
            }
        }
    }

    private suspend fun syncPrayerHabitsForDate(dateKey: String, now: Long, notePrefix: String) {
        val prayerHabits = habitDao.findHabitsByAutoSource("MOSQUE_PRAYER") +
            habitDao.findHabitsByAutoSource("PRAYER")
        val todayLogs = prayerLogDao.getPrayerLogsForDateOnce(dateKey)
        val completedPrayersCount = todayLogs.count {
            it.status == "ON_TIME"
        }

        for (prayerHabit in prayerHabits.distinctBy { it.id }) {
            val habitLogId = "${prayerHabit.id}_$dateKey"
            val isAllDone = completedPrayersCount >= prayerHabit.targetCount
            val existingHabitLog = habitDao.getHabitLogById(habitLogId)
            val wasCompletedBefore = existingHabitLog?.isCompleted == true

            habitDao.upsertHabitLog(
                HabitLogEntity(
                    id = habitLogId,
                    habitId = prayerHabit.id,
                    dateKey = dateKey,
                    completedCount = completedPrayersCount,
                    targetCount = prayerHabit.targetCount,
                    isCompleted = isAllDone,
                    sourceNote = "$notePrefix ($completedPrayersCount/${prayerHabit.targetCount})",
                    createdAt = existingHabitLog?.createdAt ?: now,
                    updatedAt = now
                )
            )

            if (isAllDone && !wasCompletedBefore) {
                rewardHabitCompletion(prayerHabit, now)
            }
        }
    }

    // --- الورد القرآني والأذكار ---

    suspend fun logWirdReadingAndSyncHabit(
        pagesReadNow: Int,
        bookmarkSurah: String,
        bookmarkAyah: Int,
        bookmarkNote: String
    ): WirdProgressEntity {
        val now = System.currentTimeMillis()
        val todayKey = UmmAlQuraCalculator.todayDateKey(now)
        val current = fullDayDao.getWirdProgressOnce() ?: WirdProgressEntity()

        val rawNextPage = current.currentPage + pagesReadNow.coerceAtLeast(1)
        val completedNewKhatmah = rawNextPage > 604
        val nextPage = if (completedNewKhatmah) ((rawNextPage - 1) % 604) + 1 else rawNextPage
        val nextKhatmahCount = if (completedNewKhatmah) {
            current.completedKhatmahCount + 1
        } else {
            current.completedKhatmahCount
        }
        val nextJuz = IcsCalendarParser.calculateJuzForMadinahPage(nextPage)
        val todayPages = if (current.lastReadDateKey == todayKey) {
            current.todayPagesRead + pagesReadNow
        } else {
            pagesReadNow
        }

        val updated = current.copy(
            currentPage = nextPage,
            currentJuz = nextJuz,
            bookmarkSurahName = bookmarkSurah.trim().ifBlank { current.bookmarkSurahName },
            bookmarkAyahNumber = bookmarkAyah.coerceAtLeast(1),
            bookmarkNote = bookmarkNote.trim(),
            completedKhatmahCount = nextKhatmahCount,
            lastReadDateKey = todayKey,
            todayPagesRead = todayPages,
            updatedAt = now
        )
        fullDayDao.upsertWirdProgress(updated)

        val wirdHabits = (habitDao.findHabitsByAutoSource("WIRD") + habitDao.findHabitsByAutoSource("QURAN_WIRD")).distinctBy { it.id }
        for (wirdHabit in wirdHabits) {
            val logId = "${wirdHabit.id}_$todayKey"
            val existingLog = habitDao.getHabitLogById(logId)
            val isDone = todayPages >= current.dailyTargetPages || todayPages >= wirdHabit.targetCount
            val wasDone = existingLog?.isCompleted == true

            habitDao.upsertHabitLog(
                HabitLogEntity(
                    id = logId,
                    habitId = wirdHabit.id,
                    dateKey = todayKey,
                    completedCount = if (isDone) wirdHabit.targetCount else todayPages,
                    targetCount = wirdHabit.targetCount,
                    isCompleted = isDone,
                    sourceNote = "الورد القرآني (ص $nextPage)",
                    createdAt = existingLog?.createdAt ?: now,
                    updatedAt = now
                )
            )
            if (isDone && !wasDone) {
                rewardHabitCompletion(wirdHabit, now)
            }
        }
        return updated
    }

    suspend fun saveCurrentQuranPage(page: Int, surahName: String = "", ayahNumber: Int = 1) {
        val now = System.currentTimeMillis()
        val current = fullDayDao.getWirdProgressOnce() ?: WirdProgressEntity()
        val nextJuz = IcsCalendarParser.calculateJuzForMadinahPage(page)
        val updated = current.copy(
            currentPage = page.coerceIn(1, 604),
            currentJuz = nextJuz,
            bookmarkSurahName = surahName.ifBlank { current.bookmarkSurahName },
            bookmarkAyahNumber = ayahNumber.coerceAtLeast(1),
            updatedAt = now
        )
        fullDayDao.upsertWirdProgress(updated)
    }

    suspend fun configureKhatmahPlan(
        dailyTargetPages: Int,
        planType: String = "PAGES",
        khatmahTitle: String = "الختمة الحالية",
        startPage: Int? = null
    ) {
        val now = System.currentTimeMillis()
        val current = fullDayDao.getWirdProgressOnce() ?: WirdProgressEntity()
        val newPage = startPage?.coerceIn(1, 604) ?: current.currentPage
        val updated = current.copy(
            currentPage = newPage,
            dailyTargetPages = dailyTargetPages.coerceIn(1, 100),
            planType = planType,
            khatmahTitle = khatmahTitle,
            updatedAt = now
        )
        fullDayDao.upsertWirdProgress(updated)
    }

    suspend fun addQuranBookmark(
        pageNumber: Int,
        surahName: String,
        ayahNumber: Int = 1,
        title: String = "علامة توقف",
        note: String = "",
        colorHex: String = "#059669",
        tag: String = "ورد اليوم"
    ): QuranBookmarkEntity {
        val bookmark = QuranBookmarkEntity(
            pageNumber = pageNumber.coerceIn(1, 604),
            surahName = surahName,
            ayahNumber = ayahNumber.coerceAtLeast(1),
            title = title.ifBlank { "علامة توقف" },
            note = note.trim(),
            colorHex = colorHex,
            tag = tag,
            createdAt = System.currentTimeMillis()
        )
        fullDayDao.insertQuranBookmark(bookmark)
        return bookmark
    }

    suspend fun deleteQuranBookmark(id: String) {
        fullDayDao.deleteQuranBookmark(id)
    }

    suspend fun progressAzkarAndSyncHabit(
        dateKey: String,
        azkarLog: AzkarLogEntity,
        completeImmediately: Boolean = false
    ) {
        val now = System.currentTimeMillis()
        val nextCount = if (completeImmediately) {
            azkarLog.targetCount
        } else {
            (azkarLog.currentCount + 1).coerceAtMost(azkarLog.targetCount)
        }
        val isNowCompleted = nextCount >= azkarLog.targetCount

        fullDayDao.upsertAzkarLog(
            azkarLog.copy(
                currentCount = nextCount,
                isCompleted = isNowCompleted,
                completedAt = if (isNowCompleted) now else null,
                updatedAt = now
            )
        )

        val allTodayAzkar = fullDayDao.getAzkarLogsForDateOnce(dateKey)
        val completedSections = allTodayAzkar.count { it.isCompleted }

        val specificSource = if (azkarLog.categoryCode == "MORNING") "AZKAR_MORNING" else if (azkarLog.categoryCode == "EVENING") "AZKAR_EVENING" else ""
        val azkarHabits = (habitDao.findHabitsByAutoSource("AZKAR") +
            (if (specificSource.isNotEmpty()) habitDao.findHabitsByAutoSource(specificSource) else emptyList()))
            .distinctBy { it.id }

        for (azkarHabit in azkarHabits) {
            val logId = "${azkarHabit.id}_$dateKey"
            val existingLog = habitDao.getHabitLogById(logId)
            val isHabitDone = if (azkarHabit.autoSource == specificSource) isNowCompleted else completedSections >= azkarHabit.targetCount
            val wasDone = existingLog?.isCompleted == true

            habitDao.upsertHabitLog(
                HabitLogEntity(
                    id = logId,
                    habitId = azkarHabit.id,
                    dateKey = dateKey,
                    completedCount = if (azkarHabit.autoSource == specificSource) (if (isNowCompleted) 1 else 0) else completedSections,
                    targetCount = azkarHabit.targetCount,
                    isCompleted = isHabitDone,
                    sourceNote = "الأذكار (${azkarLog.categoryTitle})",
                    createdAt = existingLog?.createdAt ?: now,
                    updatedAt = now
                )
            )
            if (isHabitDone && !wasDone) {
                rewardHabitCompletion(azkarHabit, now)
            }
        }
    }

    suspend fun completeAthkarCategory(dateKey: String, categoryCode: String) {
        val now = System.currentTimeMillis()
        val todayAzkar = fullDayDao.getAzkarLogsForDateOnce(dateKey)
        val targetLog = todayAzkar.firstOrNull { it.categoryCode == categoryCode }
        if (targetLog != null) {
            fullDayDao.upsertAzkarLog(
                targetLog.copy(
                    currentCount = targetLog.targetCount,
                    isCompleted = true,
                    completedAt = now,
                    updatedAt = now
                )
            )
        }

        val specificSource = if (categoryCode == "MORNING") "AZKAR_MORNING" else if (categoryCode == "EVENING") "AZKAR_EVENING" else ""
        val targetHabits = (habitDao.findHabitsByAutoSource("AZKAR") +
            (if (specificSource.isNotEmpty()) habitDao.findHabitsByAutoSource(specificSource) else emptyList()))
            .distinctBy { it.id }

        for (habit in targetHabits) {
            val logId = "${habit.id}_$dateKey"
            val existingLog = habitDao.getHabitLogById(logId)
            val wasDone = existingLog?.isCompleted == true

            habitDao.upsertHabitLog(
                HabitLogEntity(
                    id = logId,
                    habitId = habit.id,
                    dateKey = dateKey,
                    completedCount = habit.targetCount,
                    targetCount = habit.targetCount,
                    isCompleted = true,
                    sourceNote = "قراءة كاملة في القسم الديني (${if (categoryCode == "MORNING") "أذكار الصباح" else "أذكار المساء"})",
                    createdAt = existingLog?.createdAt ?: now,
                    updatedAt = now
                )
            )
            if (!wasDone) {
                rewardHabitCompletion(habit, now)
            }
        }
    }

    suspend fun toggleAthkarHabit(categoryCode: String, enabled: Boolean) {
        val now = System.currentTimeMillis()
        val autoSource = if (categoryCode == "MORNING") "AZKAR_MORNING" else "AZKAR_EVENING"
        val habitId = if (categoryCode == "MORNING") "habit_auto_azkar_morning" else "habit_auto_azkar_evening"
        val habitTitle = if (categoryCode == "MORNING") "أذكار الصباح" else "أذكار المساء"
        val habitDesc = if (categoryCode == "MORNING") {
            "قراءة أذكار الصباح كاملة في القسم الديني لحفظ اليوم بالذكر والبركة"
        } else {
            "قراءة أذكار المساء كاملة في القسم الديني وتحصين النفس قبل الغروب"
        }

        val existing = habitDao.findHabitById(habitId)
        if (enabled) {
            val categories = categoryDao.getCategoriesOnce()
            val deenCategory = categories.firstOrNull { it.name.contains("دين") || it.iconKey == "DEEN" } ?: categories.firstOrNull()
            if (existing != null) {
                habitDao.upsertHabit(
                    existing.copy(
                        title = habitTitle,
                        description = habitDesc,
                        autoSource = autoSource,
                        deletedAt = null,
                        updatedAt = now
                    )
                )
            } else {
                habitDao.upsertHabit(
                    HabitEntity(
                        id = habitId,
                        title = habitTitle,
                        description = habitDesc,
                        categoryId = deenCategory?.id ?: "",
                        goalMode = "ACHIEVE",
                        comparisonOperator = "EQ",
                        frequencyType = "DAILY",
                        targetCount = 1,
                        unitLabel = "قراءة كاملة",
                        autoSource = autoSource,
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
        } else {
            if (existing != null) {
                habitDao.upsertHabit(
                    existing.copy(
                        deletedAt = now,
                        updatedAt = now
                    )
                )
            }
        }
    }

    suspend fun toggleQuranWirdHabit(enabled: Boolean) {
        val now = System.currentTimeMillis()
        val habitId = "habit_auto_quran_wird"
        val habitTitle = "الورد القرآني اليومي"
        val habitDesc = "قراءة الورد القرآني اليومي في المصحف الشريف ووضع علامة التوقف عند الآية المطلوبة لإتمام الختمة المباركة"
        val existing = habitDao.findHabitById(habitId)
        if (enabled) {
            val categories = categoryDao.getCategoriesOnce()
            val deenCategory = categories.firstOrNull { it.name.contains("دين") || it.iconKey == "DEEN" } ?: categories.firstOrNull()
            if (existing != null) {
                habitDao.upsertHabit(
                    existing.copy(
                        title = habitTitle,
                        description = habitDesc,
                        autoSource = "QURAN_WIRD",
                        deletedAt = null,
                        updatedAt = now
                    )
                )
            } else {
                habitDao.upsertHabit(
                    HabitEntity(
                        id = habitId,
                        title = habitTitle,
                        description = habitDesc,
                        categoryId = deenCategory?.id ?: "",
                        goalMode = "ACHIEVE",
                        comparisonOperator = "EQ",
                        frequencyType = "DAILY",
                        targetCount = 1,
                        unitLabel = "ورد اليوم",
                        autoSource = "QURAN_WIRD",
                        createdAt = now,
                        updatedAt = now
                    )
                )
            }
        } else {
            if (existing != null) {
                habitDao.upsertHabit(existing.copy(deletedAt = now, updatedAt = now))
            }
        }
    }

    suspend fun completeQuranWirdAtAyah(surahName: String, ayahNumber: Int, pageNumber: Int) {
        val now = System.currentTimeMillis()
        val todayKey = UmmAlQuraCalculator.todayDateKey(now)
        val current = fullDayDao.getWirdProgressOnce() ?: WirdProgressEntity()
        val nextJuz = IcsCalendarParser.calculateJuzForMadinahPage(pageNumber)

        val updated = current.copy(
            currentPage = pageNumber.coerceIn(1, 604),
            currentJuz = nextJuz,
            bookmarkSurahName = surahName.ifBlank { current.bookmarkSurahName },
            bookmarkAyahNumber = ayahNumber.coerceAtLeast(1),
            lastReadDateKey = todayKey,
            todayPagesRead = (current.todayPagesRead + 1).coerceAtLeast(current.dailyTargetPages),
            updatedAt = now
        )
        fullDayDao.upsertWirdProgress(updated)

        // مزامنة إتمام عادة الورد القرآني تلقائياً
        val wirdHabits = (habitDao.findHabitsByAutoSource("QURAN_WIRD") + habitDao.findHabitsByAutoSource("WIRD")).distinctBy { it.id }
        for (wirdHabit in wirdHabits) {
            val logId = "${wirdHabit.id}_$todayKey"
            val existingLog = habitDao.getHabitLogById(logId)
            val wasDone = existingLog?.isCompleted == true

            habitDao.upsertHabitLog(
                HabitLogEntity(
                    id = logId,
                    habitId = wirdHabit.id,
                    dateKey = todayKey,
                    completedCount = wirdHabit.targetCount,
                    targetCount = wirdHabit.targetCount,
                    isCompleted = true,
                    sourceNote = "توقف الورد عند سورة $surahName آية $ayahNumber (ص $pageNumber)",
                    createdAt = existingLog?.createdAt ?: now,
                    updatedAt = now
                )
            )
            if (!wasDone) {
                rewardHabitCompletion(wirdHabit, now)
            }
        }
    }

    // --- أحداث ومواعيد التقويم ---

    suspend fun upsertAppEvent(event: AppEventEntity) {
        eventDao.upsertEvent(event)
    }

    suspend fun deleteAppEvent(id: String) {
        eventDao.deleteEvent(id)
    }

    suspend fun getAppEventsOnce(): List<AppEventEntity> {
        return eventDao.getEventsOnce()
    }

    suspend fun resetAzkarLog(azkarLog: AzkarLogEntity) {
        val now = System.currentTimeMillis()
        fullDayDao.upsertAzkarLog(
            azkarLog.copy(
                currentCount = 0,
                isCompleted = false,
                completedAt = null,
                updatedAt = now
            )
        )
    }

    // --- المزامنة الصارمة مع جميع مؤشرات Health Connect بدون تعبئة يدوية ---

    suspend fun updateAllHealthConnectMetricsAndSyncHabits(
        dateKey: String,
        stepsCount: Int,
        sleepHours: Float,
        activeCalories: Int,
        totalCalories: Int,
        distanceMeters: Int,
        exerciseMinutes: Int,
        waterMl: Int,
        avgHeartRateBpm: Int,
        syncSource: String = "Health Connect"
    ) {
        val now = System.currentTimeMillis()
        val existing = fullDayDao.getHealthSnapshotForDateOnce(dateKey) ?: HealthSnapshotEntity(
            id = "health_$dateKey",
            dateKey = dateKey
        )
        val updated = existing.copy(
            stepsCount = stepsCount.coerceAtLeast(0),
            sleepHours = sleepHours.coerceIn(0f, 24f),
            activeCalories = activeCalories.coerceAtLeast(0),
            burnedCalories = totalCalories.coerceAtLeast(activeCalories),
            distanceMeters = distanceMeters.coerceAtLeast(0),
            exerciseMinutes = exerciseMinutes.coerceAtLeast(0),
            waterMl = waterMl.coerceAtLeast(0),
            avgHeartRateBpm = avgHeartRateBpm.coerceAtLeast(0),
            syncSource = syncSource,
            updatedAt = now
        )
        fullDayDao.upsertHealthSnapshot(updated)

        val allHabits = habitDao.getActiveHabitsOnce()
        for (habit in allHabits) {
            val metricValAndLabel: Pair<Int, String>? = when (habit.autoSource) {
                "HEALTH_STEPS", "STEPS" -> stepsCount to "Health Connect ($stepsCount خطوة)"
                "HEALTH_SLEEP" -> sleepHours.roundToInt() to "Health Connect (${String.format("%.1f", sleepHours)} س نوم)"
                "HEALTH_ACTIVE_CALORIES" -> activeCalories to "Health Connect ($activeCalories سعرة نشطة)"
                "HEALTH_TOTAL_CALORIES" -> totalCalories to "Health Connect ($totalCalories سعرة محروقة)"
                "HEALTH_DISTANCE" -> distanceMeters to "Health Connect ($distanceMeters متر)"
                "HEALTH_EXERCISE" -> exerciseMinutes to "Health Connect ($exerciseMinutes د تمرين)"
                "HEALTH_WATER" -> waterMl to "Health Connect ($waterMl مل ماء)"
                "HEALTH_HEART_RATE" -> avgHeartRateBpm to "Health Connect ($avgHeartRateBpm نبضة/د)"
                else -> null
            }

            if (metricValAndLabel != null) {
                val (actualValue, note) = metricValAndLabel
                val logId = "${habit.id}_$dateKey"
                val existingLog = habitDao.getHabitLogById(logId)
                val isDone = actualValue >= habit.targetCount
                val wasDone = existingLog?.isCompleted == true

                habitDao.upsertHabitLog(
                    HabitLogEntity(
                        id = logId,
                        habitId = habit.id,
                        dateKey = dateKey,
                        completedCount = actualValue,
                        targetCount = habit.targetCount,
                        isCompleted = isDone,
                        sourceNote = note,
                        createdAt = existingLog?.createdAt ?: now,
                        updatedAt = now
                    )
                )
                if (isDone && !wasDone) {
                    rewardHabitCompletion(habit, now)
                }
            }
        }
    }

    suspend fun updateHealthAndSyncHabit(
        dateKey: String,
        stepsCount: Int,
        burnedCalories: Int,
        sleepHours: Float
    ) {
        val existing = fullDayDao.getHealthSnapshotForDateOnce(dateKey)
        updateAllHealthConnectMetricsAndSyncHabits(
            dateKey = dateKey,
            stepsCount = stepsCount,
            sleepHours = sleepHours,
            activeCalories = burnedCalories,
            totalCalories = maxOf(burnedCalories, existing?.burnedCalories ?: 0),
            distanceMeters = existing?.distanceMeters ?: (stepsCount * 0.72).roundToInt(),
            exerciseMinutes = existing?.exerciseMinutes ?: 0,
            waterMl = existing?.waterMl ?: 0,
            avgHeartRateBpm = existing?.avgHeartRateBpm ?: 0,
            syncSource = "Health Connect / حساس الحركة"
        )
    }

    suspend fun addMealToHealthLog(
        dateKey: String,
        mealDescription: String,
        calories: Int
    ) {
        val now = System.currentTimeMillis()
        val existing = fullDayDao.getHealthSnapshotForDateOnce(dateKey) ?: HealthSnapshotEntity(
            id = "health_$dateKey",
            dateKey = dateKey
        )
        val newConsumed = existing.consumedCalories + calories.coerceAtLeast(0)
        val arabicCal = UmmAlQuraCalculator.toArabicDigits(calories.toString())
        val newMeals = if (existing.mealsLogJson.isBlank()) {
            "$mealDescription ($arabicCal سعرة)"
        } else {
            "${existing.mealsLogJson}\n$mealDescription ($arabicCal سعرة)"
        }
        fullDayDao.upsertHealthSnapshot(
            existing.copy(
                consumedCalories = newConsumed,
                mealsLogJson = newMeals,
                updatedAt = now
            )
        )
    }

    private suspend fun rewardHabitCompletion(habit: HabitEntity, now: Long) {
        val newStreak = habit.currentStreak + 1
        val newBest = maxOf(habit.bestStreak, newStreak)
        val newXp = habit.xp + 25
        val newLevel = 1 + (newXp / 100)
        habitDao.upsertHabit(
            habit.copy(
                currentStreak = newStreak,
                bestStreak = newBest,
                xp = newXp,
                level = newLevel,
                updatedAt = now
            )
        )
    }

    // --- العداد (المؤقت التصاعدي والبومودورو المخصص) وربطه بالعادات والمهام ---

    suspend fun saveStudySessionAndSync(
        subjectName: String,
        subjectColorHex: String,
        mode: String,
        durationSeconds: Int,
        startedAt: Long,
        endedAt: Long,
        pausedAt: Long? = null,
        completedAt: Long? = null,
        linkedType: String = "NONE", // HABIT, TASK, NONE
        linkedId: String? = null,
        notes: String = ""
    ): StudySessionEntity {
        val now = System.currentTimeMillis()
        val todayKey = UmmAlQuraCalculator.todayDateKey(endedAt)
        val durationMinutes = (durationSeconds / 60).coerceAtLeast(1)

        val session = StudySessionEntity(
            id = UUID.randomUUID().toString(),
            subjectId = linkedId ?: "general_timer",
            subjectName = subjectName,
            subjectColorHex = subjectColorHex,
            mode = mode,
            durationMinutes = durationMinutes,
            durationSeconds = durationSeconds,
            dateKey = todayKey,
            startedAt = startedAt,
            endedAt = endedAt,
            pausedAt = pausedAt,
            completedAt = completedAt,
            isCompleted = durationSeconds >= 30,
            linkedType = linkedType,
            linkedId = linkedId ?: "",
            notes = notes,
            createdAt = now,
            updatedAt = now
        )
        studyDao.insertStudySession(session)

        // مزامنة التقدم مع العادة إذا كان العداد مرتبطاً بعادة
        if (linkedType == "HABIT" && !linkedId.isNullOrBlank()) {
            habitDao.findHabitById(linkedId)?.let { habit ->
                val logId = "${habit.id}_$todayKey"
                val existingLog = habitDao.getHabitLogById(logId)
                val newCompletedMinutes = (existingLog?.completedCount ?: 0) + durationMinutes
                val isTargetMet = newCompletedMinutes >= habit.targetCount
                val wasCompleted = existingLog?.isCompleted == true
                val extraMins = (newCompletedMinutes - habit.targetCount).coerceAtLeast(0)

                val noteStr = when {
                    isTargetMet && extraMins > 0 -> "العداد ($newCompletedMinutes/${habit.targetCount} د • +$extraMins د إضافية 🔥)"
                    isTargetMet -> "العداد ($newCompletedMinutes/${habit.targetCount} د • تحقق الهدف ✅)"
                    else -> "العداد ($newCompletedMinutes/${habit.targetCount} د)"
                }

                habitDao.upsertHabitLog(
                    HabitLogEntity(
                        id = logId,
                        habitId = habit.id,
                        dateKey = todayKey,
                        completedCount = newCompletedMinutes,
                        targetCount = habit.targetCount,
                        isCompleted = isTargetMet,
                        sourceNote = noteStr,
                        createdAt = existingLog?.createdAt ?: now,
                        updatedAt = now
                    )
                )
                if (isTargetMet && !wasCompleted) {
                    rewardHabitCompletion(habit, now)
                }
            }
        } else if (linkedType == "TASK" && !linkedId.isNullOrBlank()) {
            // مزامنة مع المهمة: توثيق مدة التركيز في ملاحظات المهمة
            taskDao.getTaskById(linkedId)?.let { task ->
                val focusTag = "⏱️ جلسة تركيز: $durationMinutes د"
                val updatedNotes = if (task.notes.isBlank()) {
                    focusTag
                } else {
                    "${task.notes}\n$focusTag"
                }
                taskDao.upsertTask(task.copy(notes = updatedNotes, updatedAt = now))
            }
        }
        return session
    }

    suspend fun saveStudySessionAndSyncHabit(
        subject: SubjectEntity,
        mode: String,
        durationSeconds: Int,
        notes: String = "",
        linkedHabitId: String? = null
    ): StudySessionEntity {
        val now = System.currentTimeMillis()
        val started = now - (durationSeconds * 1000L)
        return saveStudySessionAndSync(
            subjectName = subject.name,
            subjectColorHex = subject.colorHex,
            mode = mode,
            durationSeconds = durationSeconds,
            startedAt = started,
            endedAt = now,
            pausedAt = null,
            completedAt = now,
            linkedType = if (linkedHabitId != null) "HABIT" else "NONE",
            linkedId = linkedHabitId,
            notes = notes
        )
    }

    suspend fun addSubject(
        name: String,
        colorHex: String,
        dailyGoalMinutes: Int,
        weeklyGoalMinutes: Int,
        defaultTimerType: String = "COUNT_UP",
        pomodoroFocusMin: Int = 25,
        pomodoroBreakMin: Int = 5,
        pomodoroRounds: Int = 2
    ): SubjectEntity {
        val now = System.currentTimeMillis()
        val entity = SubjectEntity(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            colorHex = colorHex,
            dailyGoalMinutes = dailyGoalMinutes.coerceAtLeast(5),
            weeklyGoalMinutes = weeklyGoalMinutes.coerceAtLeast(15),
            defaultTimerType = defaultTimerType,
            pomodoroFocusMin = pomodoroFocusMin.coerceAtLeast(5),
            pomodoroBreakMin = pomodoroBreakMin.coerceAtLeast(1),
            pomodoroRounds = pomodoroRounds.coerceAtLeast(1),
            createdAt = now,
            updatedAt = now
        )
        studyDao.upsertSubject(entity)
        return entity
    }

    suspend fun deleteSubject(subjectId: String) {
        studyDao.softDeleteSubject(subjectId)
    }

    suspend fun addLecture(
        title: String,
        courseCode: String,
        location: String,
        dayOfWeek: Int,
        startTime24: String,
        endTime24: String,
        reminderMinutesBefore: Int
    ) {
        val now = System.currentTimeMillis()
        fullDayDao.upsertLecture(
            LectureEntity(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                courseCode = courseCode.trim(),
                location = location.trim(),
                dayOfWeek = dayOfWeek.coerceIn(1, 7),
                startTime24 = startTime24.trim(),
                endTime24 = endTime24.trim(),
                reminderMinutesBefore = reminderMinutesBefore,
                isImportedFromIcs = false,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun importLecturesFromIcs(icsContent: String): Int {
        val parsed = IcsCalendarParser.parseIcsToLectures(icsContent)
        if (parsed.isNotEmpty()) {
            fullDayDao.upsertLectures(parsed)
        }
        return parsed.size
    }

    suspend fun deleteLecture(id: String) {
        fullDayDao.softDeleteLecture(id)
    }

    // --- الحظر والمنبه ---

    suspend fun toggleBlockedAppStudy(app: BlockedAppEntity) {
        protectionDao.upsertBlockedApp(
            app.copy(
                blockInStudyMode = !app.blockInStudyMode,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun toggleBlockedAppSleep(app: BlockedAppEntity) {
        protectionDao.upsertBlockedApp(
            app.copy(
                blockInSleepMode = !app.blockInSleepMode,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun addOrUpdateBlockedApp(
        appName: String,
        packageName: String,
        category: String = "تطبيق",
        dailyLimitMinutes: Int = 0,
        isAlwaysBlocked: Boolean = false,
        blockInStudyMode: Boolean = true,
        blockInSleepMode: Boolean = true
    ) {
        val now = System.currentTimeMillis()
        val existing = protectionDao.getBlockedAppsOnce().find { it.packageName == packageName }
        protectionDao.upsertBlockedApp(
            BlockedAppEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                packageName = packageName.trim().ifBlank { "com.example.customapp" },
                appName = appName.trim(),
                category = category.trim().ifBlank { "تطبيق" },
                blockInStudyMode = blockInStudyMode,
                blockInSleepMode = blockInSleepMode,
                isAlwaysBlocked = isAlwaysBlocked,
                dailyLimitMinutes = dailyLimitMinutes.coerceAtLeast(0),
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
        )
    }

    suspend fun addBlockedApp(appName: String, packageName: String, category: String) {
        addOrUpdateBlockedApp(appName = appName, packageName = packageName, category = category)
    }

    suspend fun deleteBlockedApp(id: String) {
        protectionDao.softDeleteBlockedApp(id)
    }

    suspend fun updateBlockSchedule(schedule: BlockScheduleEntity) {
        protectionDao.upsertBlockSchedule(
            schedule.copy(updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun saveCustomBlockSchedule(
        id: String? = null,
        title: String,
        startTime24: String,
        endTime24: String,
        blockedPackagesCsv: String,
        isEnabled: Boolean = true
    ): BlockScheduleEntity {
        val now = System.currentTimeMillis()
        val schedule = BlockScheduleEntity(
            id = id ?: UUID.randomUUID().toString(),
            title = title.trim().ifBlank { "جدول حظر" },
            scheduleType = "CUSTOM_BLOCK",
            startTime24 = startTime24.trim(),
            endTime24 = endTime24.trim(),
            blockedPackagesCsv = blockedPackagesCsv.trim(),
            isEnabled = isEnabled,
            createdAt = now,
            updatedAt = now
        )
        protectionDao.upsertBlockSchedule(schedule)
        return schedule
    }

    suspend fun deleteBlockSchedule(id: String) {
        protectionDao.softDeleteBlockSchedule(id)
    }

    // --- الفئات (يونيفرسل) ---

    suspend fun addCategory(name: String, colorHex: String, iconKey: String): CategoryEntity {
        val now = System.currentTimeMillis()
        val category = CategoryEntity(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            colorHex = colorHex.trim().ifBlank { "#10B981" },
            iconKey = iconKey.trim().ifBlank { "TAG" },
            createdAt = now,
            updatedAt = now
        )
        categoryDao.upsertCategory(category)
        return category
    }

    suspend fun updateCategory(id: String, name: String, colorHex: String, iconKey: String) {
        val cat = categoryDao.getCategoryById(id) ?: return
        categoryDao.upsertCategory(
            cat.copy(
                name = name.trim(),
                colorHex = colorHex.trim().ifBlank { cat.colorHex },
                iconKey = iconKey.trim().ifBlank { cat.iconKey },
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteCategory(id: String) {
        categoryDao.softDeleteCategory(id)
    }

    // --- العادات ---

    suspend fun addHabit(
        title: String,
        description: String,
        frequencyType: String,
        targetCount: Int,
        unitLabel: String,
        autoSource: String,
        alarmTime24: String = "",
        alarmEnabled: Boolean = false,
        goalMode: String = "ACHIEVE",
        timerType: String = "COUNT_UP",
        pomodoroFocusMin: Int = 25,
        pomodoroBreakMin: Int = 5,
        pomodoroRounds: Int = 2,
        linkedSubjectId: String = "",
        penaltyLockApps: Boolean = false,
        penaltyScheduleId: String = "",
        penaltyBlockedPackages: String = "",
        alarmType: String = "NORMAL",
        alarmVerifyMethod: String = "LOCAL_IMAGE",
        alarmPlaceName: String = "المكان المحدد",
        alarmQrCode: String = "WAKE-QR-2026",
        categoryId: String = "",
        comparisonOperator: String = "GTE",
        frequencyDaysOfWeek: String = "1,2,3,4,5,6,7",
        frequencyDaysOfMonth: String = "",
        frequencyDaysOfYear: String = "",
        frequencyPeriodType: String = "WEEK",
        frequencyPeriodTimes: Int = 3,
        frequencyIntervalUnit: String = "DAYS",
        frequencyIntervalValue: Int = 1,
        cycleWorkDays: Int = 3,
        cycleRestDays: Int = 1,
        cycleStartDateKey: String = "",
        allowedSkipDaysPerWeek: Int = 1,
        startDateKey: String = "",
        endDateKey: String = "",
        checklistItemsJson: String = "[]",
        checklistRequiredCount: Int = 1
    ): HabitEntity {
        val now = System.currentTimeMillis()
        val effectiveTarget = when {
            autoSource == "CHECKBOX" -> 1
            autoSource == "CHECKLIST" -> checklistRequiredCount.coerceAtLeast(1)
            autoSource == "STUDY_TIMER" && timerType == "POMODORO" ->
                pomodoroFocusMin.coerceAtLeast(5) * pomodoroRounds.coerceAtLeast(1)
            else -> targetCount.coerceAtLeast(1)
        }
        val habit = HabitEntity(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            description = description.trim(),
            categoryId = categoryId,
            goalMode = goalMode,
            comparisonOperator = comparisonOperator,
            frequencyType = frequencyType,
            frequencyDaysOfWeek = frequencyDaysOfWeek,
            frequencyDaysOfMonth = frequencyDaysOfMonth,
            frequencyDaysOfYear = frequencyDaysOfYear,
            frequencyPeriodType = frequencyPeriodType,
            frequencyPeriodTimes = frequencyPeriodTimes,
            frequencyIntervalUnit = frequencyIntervalUnit,
            frequencyIntervalValue = frequencyIntervalValue,
            cycleWorkDays = cycleWorkDays,
            cycleRestDays = cycleRestDays,
            cycleStartDateKey = cycleStartDateKey,
            allowedSkipDaysPerWeek = allowedSkipDaysPerWeek,
            startDateKey = startDateKey,
            endDateKey = endDateKey,
            targetCount = effectiveTarget,
            unitLabel = when (autoSource) {
                "CHECKBOX" -> "إنجاز"
                "CHECKLIST" -> "عنصر"
                else -> unitLabel.ifBlank { "مرة" }
            },
            autoSource = autoSource,
            checklistItemsJson = checklistItemsJson,
            checklistRequiredCount = checklistRequiredCount.coerceAtLeast(1),
            timerType = timerType,
            pomodoroFocusMin = pomodoroFocusMin.coerceAtLeast(5),
            pomodoroBreakMin = pomodoroBreakMin.coerceAtLeast(1),
            pomodoroRounds = pomodoroRounds.coerceAtLeast(1),
            linkedSubjectId = linkedSubjectId,
            penaltyLockApps = penaltyLockApps,
            penaltyScheduleId = penaltyScheduleId,
            penaltyBlockedPackages = penaltyBlockedPackages,
            alarmTime24 = alarmTime24.trim(),
            alarmEnabled = alarmEnabled && alarmTime24.isNotBlank(),
            alarmType = alarmType,
            alarmVerifyMethod = alarmVerifyMethod,
            alarmPlaceName = alarmPlaceName.trim().ifBlank { "المكان المحدد" },
            alarmQrCode = alarmQrCode.trim().ifBlank { "WAKE-QR-2026" },
            currentStreak = 0,
            bestStreak = 0,
            level = 1,
            xp = 0,
            createdAt = now,
            updatedAt = now
        )
        habitDao.upsertHabit(habit)
        return habit
    }

    suspend fun updateHabitAlarm(
        habitId: String,
        alarmTime24: String,
        alarmEnabled: Boolean,
        alarmType: String = "NORMAL",
        alarmVerifyMethod: String = "LOCAL_IMAGE",
        alarmPlaceName: String = "المكان المحدد",
        alarmQrCode: String = "WAKE-QR-2026"
    ): HabitEntity? {
        val habit = habitDao.findHabitById(habitId) ?: return null
        val updated = habit.copy(
            alarmTime24 = alarmTime24.trim(),
            alarmEnabled = alarmEnabled && alarmTime24.isNotBlank(),
            alarmType = alarmType,
            alarmVerifyMethod = alarmVerifyMethod,
            alarmPlaceName = alarmPlaceName.trim().ifBlank { "المكان المحدد" },
            alarmQrCode = alarmQrCode.trim().ifBlank { "WAKE-QR-2026" },
            updatedAt = System.currentTimeMillis()
        )
        habitDao.upsertHabit(updated)
        return updated
    }

    suspend fun updateHabitPenaltyConfig(
        habitId: String,
        penaltyLockApps: Boolean,
        penaltyScheduleId: String,
        penaltyBlockedPackages: String
    ): HabitEntity? {
        val habit = habitDao.findHabitById(habitId) ?: return null
        val updated = habit.copy(
            penaltyLockApps = penaltyLockApps,
            penaltyScheduleId = penaltyScheduleId,
            penaltyBlockedPackages = penaltyBlockedPackages,
            updatedAt = System.currentTimeMillis()
        )
        habitDao.upsertHabit(updated)
        return updated
    }

    suspend fun toggleHabitPenaltyLock(habitId: String): HabitEntity? {
        val habit = habitDao.findHabitById(habitId) ?: return null
        val updated = habit.copy(
            penaltyLockApps = !habit.penaltyLockApps,
            updatedAt = System.currentTimeMillis()
        )
        habitDao.upsertHabit(updated)
        return updated
    }

    suspend fun setCheckboxHabitState(
        habitId: String,
        dateKey: String,
        completed: Boolean
    ) {
        val now = System.currentTimeMillis()
        val habit = habitDao.findHabitById(habitId) ?: return
        val logId = "${habit.id}_$dateKey"
        val existing = habitDao.getHabitLogById(logId)
        val wasCompleted = existing?.isCompleted == true

        habitDao.upsertHabitLog(
            HabitLogEntity(
                id = logId,
                habitId = habit.id,
                dateKey = dateKey,
                completedCount = if (completed) 1 else 0,
                targetCount = 1,
                isCompleted = completed,
                status = if (completed) "COMPLETED" else "NONE",
                completionPercent = if (completed) 100 else 0,
                sourceNote = if (completed) "تم الإنجاز" else "غير منجزة",
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
        )

        if (completed && !wasCompleted) {
            rewardHabitCompletion(habit, now)
        }
    }

    suspend fun updateValueHabitProgress(
        habitId: String,
        dateKey: String,
        newValue: Int
    ) {
        val now = System.currentTimeMillis()
        val habit = habitDao.findHabitById(habitId) ?: return
        val logId = "${habit.id}_$dateKey"
        val existing = habitDao.getHabitLogById(logId)
        val clamped = newValue.coerceAtLeast(0)
        val nowCompleted = com.example.core.habits.HabitHelper.isGoalAchieved(habit.comparisonOperator, clamped, habit.targetCount)
        val percent = com.example.core.habits.HabitHelper.calculatePercent(habit.comparisonOperator, clamped, habit.targetCount)
        val wasCompleted = existing?.isCompleted == true

        habitDao.upsertHabitLog(
            HabitLogEntity(
                id = logId,
                habitId = habit.id,
                dateKey = dateKey,
                completedCount = clamped,
                targetCount = habit.targetCount,
                isCompleted = nowCompleted,
                status = if (nowCompleted) "COMPLETED" else if (clamped > 0) "PARTIAL" else "NONE",
                completionPercent = percent,
                sourceNote = "$clamped / ${habit.targetCount} ${habit.unitLabel}",
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
        )

        if (nowCompleted && !wasCompleted) {
            rewardHabitCompletion(habit, now)
        }
    }

    suspend fun recordHabitRestSkip(habitId: String, dateKey: String) {
        val now = System.currentTimeMillis()
        val habit = habitDao.findHabitById(habitId) ?: return
        val logId = "${habit.id}_$dateKey"
        val existing = habitDao.getHabitLogById(logId)
        val isAlreadyRest = existing?.status == "REST_SKIP"

        if (isAlreadyRest) {
            habitDao.upsertHabitLog(
                HabitLogEntity(
                    id = logId,
                    habitId = habit.id,
                    dateKey = dateKey,
                    completedCount = 0,
                    targetCount = habit.targetCount,
                    isCompleted = false,
                    status = "NONE",
                    completionPercent = 0,
                    sourceNote = "تم إلغاء الراحة",
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now
                )
            )
        } else {
            habitDao.upsertHabitLog(
                HabitLogEntity(
                    id = logId,
                    habitId = habit.id,
                    dateKey = dateKey,
                    completedCount = 0,
                    targetCount = habit.targetCount,
                    isCompleted = false,
                    status = "REST_SKIP",
                    completionPercent = 0,
                    sourceNote = "راحة / تخطي مسموح",
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now
                )
            )
        }
    }

    suspend fun updateHabitChecklistItem(
        habitId: String,
        dateKey: String,
        itemId: String,
        checked: Boolean? = null,
        deltaValue: Int? = null,
        setValue: Int? = null
    ) {
        val now = System.currentTimeMillis()
        val habit = habitDao.findHabitById(habitId) ?: return
        val logId = "${habit.id}_$dateKey"
        val existing = habitDao.getHabitLogById(logId)
        val items = com.example.core.habits.HabitHelper.parseChecklistItems(habit.checklistItemsJson)
        val currentProgressMap = com.example.core.habits.HabitHelper.parseChecklistProgress(existing?.checklistProgressJson).toMutableMap()

        val targetItem = items.find { it.id == itemId } ?: return
        val currentP = currentProgressMap[itemId] ?: com.example.core.habits.HabitChecklistProgressItem(itemId = itemId)

        val nextP = when (targetItem.type) {
            "CHECKBOX" -> {
                val isChk = checked ?: !currentP.isChecked
                currentP.copy(isChecked = isChk, isCompleted = isChk)
            }
            "VALUE" -> {
                val nextVal = when {
                    setValue != null -> setValue.coerceAtLeast(0)
                    deltaValue != null -> (currentP.currentValue + deltaValue).coerceAtLeast(0)
                    else -> currentP.currentValue
                }
                val done = nextVal >= targetItem.targetValue
                currentP.copy(currentValue = nextVal, isCompleted = done)
            }
            "TIMER" -> {
                val nextVal = when {
                    setValue != null -> setValue.coerceAtLeast(0)
                    deltaValue != null -> (currentP.currentValue + deltaValue).coerceAtLeast(0)
                    checked != null -> if (checked) targetItem.targetValue else 0
                    else -> currentP.currentValue
                }
                val done = nextVal >= targetItem.targetValue
                currentP.copy(currentValue = nextVal, isCompleted = done)
            }
            else -> currentP
        }

        currentProgressMap[itemId] = nextP
        val completedItemsCount = currentProgressMap.values.count { it.isCompleted }
        val requiredCount = habit.checklistRequiredCount.coerceIn(1, maxOf(1, items.size))
        val isHabitDone = completedItemsCount >= requiredCount
        val wasCompleted = existing?.isCompleted == true
        val percent = ((completedItemsCount.toFloat() / requiredCount.toFloat()) * 100).toInt().coerceIn(0, 100)

        val updatedLog = HabitLogEntity(
            id = logId,
            habitId = habit.id,
            dateKey = dateKey,
            completedCount = completedItemsCount,
            targetCount = requiredCount,
            isCompleted = isHabitDone,
            status = if (isHabitDone) "COMPLETED" else if (completedItemsCount > 0) "PARTIAL" else "NONE",
            completionPercent = percent,
            checklistProgressJson = com.example.core.habits.HabitHelper.serializeChecklistProgress(currentProgressMap),
            sourceNote = "أنجز $completedItemsCount من $requiredCount عناصر",
            createdAt = existing?.createdAt ?: now,
            updatedAt = now
        )
        habitDao.upsertHabitLog(updatedLog)

        if (isHabitDone && !wasCompleted) {
            rewardHabitCompletion(habit, now)
        }
    }

    suspend fun deleteHabit(habitId: String) {
        habitDao.softDeleteHabit(habitId)
    }

    suspend fun incrementHabitProgress(
        habitId: String,
        dateKey: String,
        stepAmount: Int = 1,
        sourceNote: String = ""
    ) {
        val now = System.currentTimeMillis()
        val habit = habitDao.findHabitById(habitId) ?: return
        val logId = "${habit.id}_$dateKey"
        val existing = habitDao.getHabitLogById(logId)

        val currentVal = existing?.completedCount ?: 0
        val nextVal = (currentVal + stepAmount).coerceAtLeast(0)
        val nowCompleted = nextVal >= habit.targetCount
        val wasCompleted = existing?.isCompleted == true

        habitDao.upsertHabitLog(
            HabitLogEntity(
                id = logId,
                habitId = habit.id,
                dateKey = dateKey,
                completedCount = nextVal,
                targetCount = habit.targetCount,
                isCompleted = nowCompleted,
                sourceNote = sourceNote,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now
            )
        )

        if (nowCompleted && !wasCompleted) {
            rewardHabitCompletion(habit, now)
        }
    }

    suspend fun acceptHabitUpgrade(habitId: String) {
        val habit = habitDao.findHabitById(habitId) ?: return
        val newTarget = habit.suggestedTargetCount ?: return
        val now = System.currentTimeMillis()
        habitDao.upsertHabit(
            habit.copy(
                targetCount = newTarget,
                suggestedTargetCount = null,
                xp = habit.xp + 30,
                level = 1 + ((habit.xp + 30) / 100),
                updatedAt = now
            )
        )
    }

    suspend fun dismissHabitUpgrade(habitId: String) {
        val habit = habitDao.findHabitById(habitId) ?: return
        habitDao.upsertHabit(
            habit.copy(
                suggestedTargetCount = null,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    // --- المهام ---

    suspend fun addTask(
        title: String,
        notes: String,
        listCategory: String,
        priority: String,
        dueDateText: String,
        reminderTimeText: String,
        recurrence: String,
        parentTaskId: String? = null,
        alarmTime24: String = "",
        alarmEnabled: Boolean = false,
        alarmType: String = "NORMAL",
        alarmVerifyMethod: String = "LOCAL_IMAGE",
        alarmPlaceName: String = "المكان المحدد",
        alarmQrCode: String = "WAKE-QR-2026",
        categoryId: String = ""
    ): TaskEntity {
        val now = System.currentTimeMillis()
        val task = TaskEntity(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            notes = notes.trim(),
            listCategory = listCategory,
            priority = priority,
            dueDateText = dueDateText,
            reminderTimeText = if (alarmEnabled && alarmTime24.isNotBlank()) alarmTime24 else reminderTimeText,
            alarmTime24 = alarmTime24.trim(),
            alarmEnabled = alarmEnabled && alarmTime24.isNotBlank(),
            alarmType = alarmType,
            alarmVerifyMethod = alarmVerifyMethod,
            alarmPlaceName = alarmPlaceName.trim().ifBlank { "المكان المحدد" },
            alarmQrCode = alarmQrCode.trim().ifBlank { "WAKE-QR-2026" },
            recurrence = recurrence,
            parentTaskId = parentTaskId,
            categoryId = categoryId,
            isCompleted = false,
            createdAt = now,
            updatedAt = now
        )
        taskDao.upsertTask(task)
        return task
    }

    suspend fun updateTaskAlarm(
        taskId: String,
        alarmTime24: String,
        alarmEnabled: Boolean,
        alarmType: String = "NORMAL",
        alarmVerifyMethod: String = "LOCAL_IMAGE",
        alarmPlaceName: String = "المكان المحدد",
        alarmQrCode: String = "WAKE-QR-2026"
    ): TaskEntity? {
        val task = taskDao.getTaskById(taskId) ?: return null
        val updated = task.copy(
            alarmTime24 = alarmTime24.trim(),
            alarmEnabled = alarmEnabled && alarmTime24.isNotBlank(),
            alarmType = alarmType,
            alarmVerifyMethod = alarmVerifyMethod,
            alarmPlaceName = alarmPlaceName.trim().ifBlank { "المكان المحدد" },
            alarmQrCode = alarmQrCode.trim().ifBlank { "WAKE-QR-2026" },
            reminderTimeText = if (alarmEnabled && alarmTime24.isNotBlank()) alarmTime24.trim() else "بدون تذكير",
            updatedAt = System.currentTimeMillis()
        )
        taskDao.upsertTask(updated)
        return updated
    }

    suspend fun toggleTaskCompletion(taskId: String) {
        val task = taskDao.getTaskById(taskId) ?: return
        val now = System.currentTimeMillis()
        val nextCompleted = !task.isCompleted
        taskDao.upsertTask(
            task.copy(
                isCompleted = nextCompleted,
                completedAt = if (nextCompleted) now else null,
                updatedAt = now
            )
        )
    }

    suspend fun deleteTask(taskId: String) {
        taskDao.softDeleteTaskWithSubtasks(taskId)
    }

    suspend fun updateSetting(key: String, value: String) {
        settingDao.setSetting(SettingEntity(key, value, System.currentTimeMillis()))
    }

    suspend fun getSelectedCityId(): String {
        return settingDao.getSettingValue("city_id") ?: "riyadh"
    }

    suspend fun regenerateSmartDailySummary(
        dateKey: String,
        customFocusNote: String = ""
    ): DailySummaryEntity {
        val prayers = prayerLogDao.getPrayerLogsForDateOnce(dateKey)
        val prayerMap = prayers.associate { PrayerType.fromId(it.prayerName) to it.status }
        val azkarLogs = fullDayDao.getAzkarLogsForDateOnce(dateKey)
        val wird = fullDayDao.getWirdProgressOnce() ?: WirdProgressEntity()
        val sessions = studyDao.getStudySessionsOnce()
        val subjects = studyDao.getActiveSubjectsOnce()
        val tasks = taskDao.getActiveTasksOnce().filter { it.parentTaskId == null }
        val habits = habitDao.getActiveHabitsOnce()
        val habitLogsToday = habits.mapNotNull { h ->
            habitDao.getHabitLogById("${h.id}_$dateKey")?.let { h.id to it }
        }.toMap()
        val health = fullDayDao.getHealthSnapshotForDateOnce(dateKey)
            ?: HealthSnapshotEntity(id = "health_$dateKey", dateKey = dateKey)
        val lectures = fullDayDao.getLecturesOnce()

        val summary = DailyInsightsEngine.generateSmartDailySummary(
            dateKey = dateKey,
            prayerStatusMap = prayerMap,
            azkarLogs = azkarLogs,
            wird = wird,
            studySessions = sessions,
            subjects = subjects,
            tasks = tasks,
            habits = habits,
            habitLogsToday = habitLogsToday,
            health = health,
            lectures = lectures,
            customFocusNote = customFocusNote
        )
        insightsDao.upsertDailySummary(summary)
        return summary
    }

    suspend fun applyTomorrowSuggestedTasks(dateKey: String) {
        val now = System.currentTimeMillis()
        val existingSummary = insightsDao.getDailySummaryForDateOnce(dateKey)
            ?: regenerateSmartDailySummary(dateKey)
        insightsDao.upsertDailySummary(
            existingSummary.copy(
                isTomorrowTasksApplied = true,
                updatedAt = now
            )
        )
    }

    suspend fun updateWeeklyReview(
        topGoal1: String,
        topGoal2: String,
        topGoal3: String,
        reflectionNote: String
    ) {
        val now = System.currentTimeMillis()
        val existing = insightsDao.getCurrentWeeklyReviewOnce() ?: WeeklyReviewEntity()
        insightsDao.upsertWeeklyReview(
            existing.copy(
                topGoal1 = topGoal1.trim(),
                topGoal2 = topGoal2.trim(),
                topGoal3 = topGoal3.trim(),
                reflectionNote = reflectionNote.trim(),
                updatedAt = now
            )
        )
    }

    suspend fun updateYearWeekGoal(
        weekNumber: Int,
        focusTitle: String,
        status: String,
        completionPercent: Int
    ) {
        val now = System.currentTimeMillis()
        insightsDao.upsertYearWeekGoal(
            YearWeekGoalEntity(
                weekNumber = weekNumber.coerceIn(1, 52),
                focusTitle = focusTitle.trim(),
                status = status,
                completionPercent = completionPercent.coerceIn(0, 100),
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun exportLocalBackupSummary(): String {
        val habits = habitDao.getActiveHabitsOnce()
        val tasks = taskDao.getActiveTasksOnce()
        val subjects = studyDao.getActiveSubjectsOnce()
        val sessions = studyDao.getStudySessionsOnce()
        return buildString {
            appendLine("{")
            appendLine("  \"habits\": ${habits.size},")
            appendLine("  \"tasks\": ${tasks.size},")
            appendLine("  \"subjects\": ${subjects.size},")
            appendLine("  \"study_sessions\": ${sessions.size}")
            append("}")
        }
    }
}
