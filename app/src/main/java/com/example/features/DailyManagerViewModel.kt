package com.example.features

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.calendar.CalendarEventItem
import com.example.core.calendar.DeviceCalendarManager
import com.example.data.AppEventEntity
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.core.health.HealthConnectDailyData
import com.example.core.health.HealthConnectManager
import com.example.core.health.HealthGoalMetric
import com.example.core.health.StepSensorManager
import com.example.core.insights.DailyInsightsEngine
import com.example.core.insights.TimelineEventItem
import com.example.core.notifications.AdhanNotificationHelper
import com.example.core.prayer.CityLocation
import com.example.core.prayer.DailyPrayerSchedule
import com.example.core.prayer.MosqueLocationVerifier
import com.example.core.prayer.PrayerType
import com.example.core.prayer.UmmAlQuraCalculator
import com.example.core.prayer.UserGpsPoint
import com.example.core.protection.AppUsageMonitor
import com.example.core.protection.DeviceUsageOverview
import com.example.core.protection.ProtectionManager
import com.example.core.protection.ProtectionRuntimeState
import com.example.core.study.StudyTimerManager
import com.example.core.study.StudyTimerState
import com.example.core.widget.DailyManagerAppWidgetProvider
import com.example.data.AppDatabase
import com.example.data.AzkarLogEntity
import com.example.data.BlockScheduleEntity
import com.example.data.BlockedAppEntity
import com.example.data.CategoryEntity
import com.example.data.DailyManagerRepository
import com.example.data.DailySummaryEntity
import com.example.data.HabitEntity
import com.example.data.HabitLogEntity
import com.example.data.HealthSnapshotEntity
import com.example.data.LectureEntity
import com.example.data.MosqueLocationEntity
import com.example.data.PrayerLogEntity
import com.example.data.QuranBookmarkEntity
import com.example.data.StudySessionEntity
import com.example.data.SubjectEntity
import com.example.data.TaskEntity
import com.example.data.WeeklyReviewEntity
import com.example.data.WirdProgressEntity
import com.example.data.YearWeekGoalEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DailyManagerUiState(
    val isDarkMode: Boolean = false,
    val themePreset: com.example.ui.theme.ThemePreset = com.example.ui.theme.ThemePreset.EMERALD,
    val fontScale: Float = 1.0f,
    val selectedCity: CityLocation = UmmAlQuraCalculator.saudiCities.first(),
    val prayerSchedule: DailyPrayerSchedule = UmmAlQuraCalculator.calculateScheduleForNow(
        UmmAlQuraCalculator.saudiCities.first()
    ),
    val todayPrayerStatusMap: Map<PrayerType, String> = emptyMap(),
    val todayPrayerLogsMap: Map<PrayerType, PrayerLogEntity> = emptyMap(),
    val mosqueLocations: List<MosqueLocationEntity> = emptyList(),
    val userGpsPoint: UserGpsPoint? = null,
    val nearestMosqueDistanceMeters: Int? = null,
    val nearestMosqueName: String? = null,
    val isInsideMosqueNow: Boolean = false,
    val habits: List<HabitEntity> = emptyList(),
    val habitLogsByHabitAndDate: Map<String, Map<String, HabitLogEntity>> = emptyMap(),
    val activePenaltyHabitsCount: Int = 0,
    val mainTasks: List<TaskEntity> = emptyList(),
    val subtasksByParentId: Map<String, List<TaskEntity>> = emptyMap(),
    val selectedTaskCategory: String = "الكل",
    val subjects: List<SubjectEntity> = emptyList(),
    val studySessions: List<StudySessionEntity> = emptyList(),
    val autoOpenStandby: Boolean = false,
    val standbyOnCharging: Boolean = true,
    val isStandbyVisible: Boolean = false,
    val blockedApps: List<BlockedAppEntity> = emptyList(),
    val blockSchedules: List<BlockScheduleEntity> = emptyList(),
    val usageOverview: DeviceUsageOverview = DeviceUsageOverview(),
    val enforcedBlockReasonsByPackage: Map<String, String> = emptyMap(),
    val isSleepModeActive: Boolean = false,
    val isStrictProtectionEnabled: Boolean = true,
    val lectures: List<LectureEntity> = emptyList(),
    val wirdProgress: WirdProgressEntity = WirdProgressEntity(),
    val todayAzkarLogs: List<AzkarLogEntity> = emptyList(),
    val todayHealth: HealthSnapshotEntity = HealthSnapshotEntity(
        id = "health_default",
        dateKey = UmmAlQuraCalculator.todayDateKey()
    ),
    val healthConnectData: HealthConnectDailyData = HealthConnectDailyData(),
    val dailySummary: DailySummaryEntity = DailySummaryEntity(
        id = "summary_default",
        dateKey = UmmAlQuraCalculator.todayDateKey()
    ),
    val weeklyReview: WeeklyReviewEntity = WeeklyReviewEntity(),
    val yearWeekGoals: List<YearWeekGoalEntity> = emptyList(),
    val timelineEvents: List<TimelineEventItem> = emptyList(),
    val prayerBellModesMap: Map<PrayerType, com.example.core.prayer.PrayerBellMode> = emptyMap(),
    val prayerOverrideSilentMap: Map<PrayerType, Boolean> = emptyMap(),
    val prayerMuazzinMap: Map<PrayerType, String> = emptyMap(),
    val customPrayerAlarms: List<com.example.core.prayer.CustomPrayerAlarm> = emptyList(),
    val prayerEndTimeAlarmEnabled: Boolean = false,
    val midnightAlarmEnabled: Boolean = false,
    val lastThirdAlarmEnabled: Boolean = false,
    val fridaySilentEnabled: Boolean = true,
    val fridayLastHourReminderEnabled: Boolean = true,
    val fridaySurahKahfReminderEnabled: Boolean = true,
    val iqamaSilentEnabled: Boolean = true,
    val iqamaSilentDurationMinutes: Int = 20,
    val flipToSnoozeAzanEnabled: Boolean = true,
    val persistentPrayerNotificationEnabled: Boolean = true,
    val categories: List<CategoryEntity> = emptyList(),
    val selectedCategoryId: String = "",
    val statusBannerMessage: String? = null,
    val backupJsonPreview: String? = null,
    val isGoogleConnected: Boolean = false,
    val googleUserEmail: String? = null,
    val googleCalendarEvents: List<com.example.core.google.GoogleCalendarEvent> = emptyList(),
    val gmailMessages: List<com.example.core.google.GmailEmailItem> = emptyList(),
    val appEvents: List<AppEventEntity> = emptyList(),
    val deviceCalendarEvents: List<CalendarEventItem> = emptyList(),
    val isDeviceCalendarSyncing: Boolean = false
)

class DailyManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = DailyManagerRepository(AppDatabase.getInstance(application))
    private val nowMillisFlow = MutableStateFlow(System.currentTimeMillis())
    private val selectedTaskCategoryFlow = MutableStateFlow("الكل")
    private val isStandbyVisibleFlow = MutableStateFlow(false)
    private val statusBannerFlow = MutableStateFlow<String?>(null)
    private val backupPreviewFlow = MutableStateFlow<String?>(null)
    private val userGpsFlow = MutableStateFlow<UserGpsPoint?>(null)
    private val selectedCategoryIdFlow = MutableStateFlow("")
    private val healthConnectDataFlow = MutableStateFlow(HealthConnectDailyData())
    private val usageOverviewFlow = MutableStateFlow(DeviceUsageOverview())
    private val googleCalendarEventsFlow = MutableStateFlow<List<com.example.core.google.GoogleCalendarEvent>>(emptyList())
    private val gmailMessagesFlow = MutableStateFlow<List<com.example.core.google.GmailEmailItem>>(emptyList())
    private val deviceCalendarEventsFlow = MutableStateFlow<List<CalendarEventItem>>(emptyList())
    private val isDeviceCalendarSyncingFlow = MutableStateFlow(false)
    private var bannerDismissJob: Job? = null

    val timerState: StateFlow<StudyTimerState> = StudyTimerManager.timerState
    val protectionState: StateFlow<ProtectionRuntimeState> = ProtectionManager.state

    val deviceCalendarEvents: StateFlow<List<CalendarEventItem>> = deviceCalendarEventsFlow.asStateFlow()
    val appEvents: StateFlow<List<AppEventEntity>> = repo.appEventsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )
    val isDeviceCalendarSyncing: StateFlow<Boolean> = isDeviceCalendarSyncingFlow.asStateFlow()

    private val todayKey: String
        get() = UmmAlQuraCalculator.todayDateKey()

    private val coreDbFlow = combine(
        repo.settingsFlow,
        repo.activeHabitsFlow,
        repo.allHabitLogsFlow,
        repo.activeTasksFlow,
        repo.observePrayerLogsForDate(todayKey)
    ) { settings, habits, habitLogs, tasks, prayerLogs ->
        Quint(settings, habits, habitLogs, tasks, prayerLogs)
    }

    private val studyAndProtectionFlow = combine(
        combine(repo.activeSubjectsFlow, repo.studySessionsFlow, repo.blockedAppsFlow) { subjects, sessions, blockedApps ->
            Triple(subjects, sessions, blockedApps)
        },
        combine(repo.blockSchedulesFlow, repo.mosqueLocationsFlow, repo.categoriesFlow) { schedules, mosques, categories ->
            Triple(schedules, mosques, categories)
        }
    ) { group1, group2 ->
        Sext(
            first = group1.first,
            second = group1.second,
            third = group1.third,
            fourth = group2.first,
            fifth = group2.second,
            sixth = group2.third
        )
    }

    private val fullDayFlow = combine(
        repo.lecturesFlow,
        repo.wirdProgressFlow,
        repo.observeAzkarLogsForDate(todayKey),
        repo.observeHealthSnapshotForDate(todayKey),
        usageOverviewFlow
    ) { lectures, wird, azkarLogs, health, usageOverview ->
        Quint(
            lectures,
            wird ?: WirdProgressEntity(),
            azkarLogs,
            health ?: HealthSnapshotEntity(id = "health_$todayKey", dateKey = todayKey),
            usageOverview
        )
    }

    private val insightsFlow = combine(
        repo.observeDailySummaryForDate(todayKey),
        repo.currentWeeklyReviewFlow,
        repo.yearWeekGoalsFlow,
        userGpsFlow,
        healthConnectDataFlow
    ) { summary, weekly, yearWeeks, gpsPoint, hcData ->
        Quint(
            summary ?: DailySummaryEntity(id = "summary_$todayKey", dateKey = todayKey),
            weekly ?: WeeklyReviewEntity(),
            yearWeeks,
            gpsPoint,
            hcData
        )
    }

    private val localUiFlow = combine(
        combine(nowMillisFlow, selectedTaskCategoryFlow, statusBannerFlow) { now, cat, banner ->
            Triple(now, cat, banner)
        },
        combine(backupPreviewFlow, isStandbyVisibleFlow, selectedCategoryIdFlow) { backup, standbyVisible, selectedCatId ->
            Triple(backup, standbyVisible, selectedCatId)
        },
        combine(googleCalendarEventsFlow, gmailMessagesFlow) { calEvents, emails ->
            Pair(calEvents, emails)
        }
    ) { group1, group2, group3 ->
        Oct(
            first = group1.first,
            second = group1.second,
            third = group1.third,
            fourth = group2.first,
            fifth = group2.second,
            sixth = group2.third,
            seventh = group3.first,
            eighth = group3.second
        )
    }

    val uiState: StateFlow<DailyManagerUiState> = combine(
        coreDbFlow,
        studyAndProtectionFlow,
        fullDayFlow,
        insightsFlow,
        localUiFlow
    ) { coreQuint, spQuint, fullDayQuint, insightsQuint, localQuint ->
        val settingsMap = coreQuint.first.associate { it.key to it.value }
        val isDark = settingsMap["dark_mode"] == "true"
        val savedPresetId = settingsMap["theme_preset"] ?: "emerald"
        val themePreset = com.example.ui.theme.ThemePreset.fromId(savedPresetId)
        val fontScale = settingsMap["font_scale"]?.toFloatOrNull() ?: 1.0f
        val autoStandby = settingsMap["auto_open_standby"] == "true"
        val chargingStandby = settingsMap["standby_on_charging"] != "false"
        val isSleepMode = settingsMap["sleep_mode_active"] == "true"
        val isStrictProtection = settingsMap["strict_protection_enabled"] != "false"

        val cityId = settingsMap["city_id"] ?: "riyadh"
        val customLat = settingsMap["custom_lat"]?.toDoubleOrNull()
        val customLng = settingsMap["custom_lng"]?.toDoubleOrNull()
        val customCityName = settingsMap["custom_city_name"]

        val city = if (cityId == "custom" && customLat != null && customLng != null) {
            CityLocation(
                id = "custom",
                arabicName = customCityName ?: "موقعي",
                regionName = "GPS",
                latitude = customLat,
                longitude = customLng
            )
        } else {
            UmmAlQuraCalculator.findCityById(cityId)
        }

        val schedule = UmmAlQuraCalculator.calculateScheduleForNow(city, localQuint.first)
        val nowMillis = localQuint.first

        val rawPrayerMap = coreQuint.fifth.associate {
            PrayerType.fromId(it.prayerName) to it.status
        }
        val obligatorySlots = schedule.prayers.filter { it.type.isObligatory }
        val prayerMap = buildMap {
            putAll(rawPrayerMap)
            obligatorySlots.forEachIndexed { idx, slot ->
                val currentStatus = rawPrayerMap[slot.type]
                if (currentStatus != "ON_TIME" && currentStatus != "LATE") {
                    val nextObligatoryMillis = obligatorySlots.getOrNull(idx + 1)?.timeMillisToday ?: Long.MAX_VALUE
                    val windowEndMillis = minOf(slot.timeMillisToday + 45 * 60_000L, nextObligatoryMillis)
                    if (nowMillis > windowEndMillis) {
                        put(slot.type, "LATE")
                    }
                }
            }
        }
        val prayerLogsMap = coreQuint.fifth.associateBy {
            PrayerType.fromId(it.prayerName)
        }

        val logsGrouped = coreQuint.third
            .groupBy { it.habitId }
            .mapValues { (_, list) -> list.associateBy { it.dateKey } }

        val habits = coreQuint.second
        val blockedApps = spQuint.third
        val blockSchedules = spQuint.fourth
        val mosques = spQuint.fifth
        val userGps = insightsQuint.fourth
        val usageOverview = fullDayQuint.fifth

        val incompletePenaltyHabits = habits.filter { h ->
            h.penaltyLockApps && (logsGrouped[h.id]?.get(todayKey)?.isCompleted != true)
        }

        val allMain = coreQuint.fourth.filter { it.parentTaskId == null }
        val filteredMain = if (localQuint.second == "الكل") {
            allMain
        } else {
            allMain.filter { it.listCategory == localQuint.second }
        }
        val subtasksMap = coreQuint.fourth
            .filter { it.parentTaskId != null }
            .groupBy { it.parentTaskId!! }

        var nearestDist: Int? = null
        var nearestName: String? = null
        var insideMosque = false
        if (userGps != null && mosques.isNotEmpty()) {
            val sorted = mosques.map { m ->
                val d = MosqueLocationVerifier.calculateDistanceMeters(
                    userGps.latitude,
                    userGps.longitude,
                    m.latitude,
                    m.longitude
                )
                m to d
            }.sortedBy { it.second }
            val (closest, dist) = sorted.first()
            nearestDist = dist
            nearestName = closest.name
            insideMosque = dist <= closest.radiusMeters.coerceAtLeast(40)
        }

        // حساب الحظر الفعلي الشامل (StayFree: الحد اليومي + الجداول الزمنية + قفل العقاب للعادات)
        val enforcedReasons = mutableMapOf<String, String>()

        // 1. التطبيقات المحظورة دائماً أو التي تجاوزت الحد اليومي
        blockedApps.forEach { app ->
            val usedMins = usageOverview.usageByPackageMinutes[app.packageName] ?: 0
            if (app.isAlwaysBlocked) {
                enforcedReasons[app.packageName] = "حظر دائم مفعل"
            } else if (app.dailyLimitMinutes > 0 && usedMins >= app.dailyLimitMinutes) {
                enforcedReasons[app.packageName] = "انتهى الحد اليومي (${app.dailyLimitMinutes} د)"
            }
        }

        // 2. جداول الحظر الزمنية النشطة حالياً
        blockSchedules.forEach { sched ->
            if (AppUsageMonitor.isScheduleCurrentlyInTimeWindow(sched, localQuint.first)) {
                val pkgs = sched.blockedPackagesCsv.split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                val targetPkgs = if (pkgs.isNotEmpty()) {
                    pkgs
                } else {
                    blockedApps.filter { it.blockInSleepMode || it.blockInStudyMode }.map { it.packageName }
                }
                targetPkgs.forEach { pkg ->
                    enforcedReasons[pkg] = "جدول الحظر: ${sched.title}"
                }
            }
        }

        // 3. قفل العقاب المرتبط بالعادات غير المنجزة اليوم (يتوقف الحظر فوراً عند إنجاز العادة)
        incompletePenaltyHabits.forEach { habit ->
            val directPkgs = habit.penaltyBlockedPackages.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
            val linkedSchedule = blockSchedules.find { it.id == habit.penaltyScheduleId }
            val schedulePkgs = linkedSchedule?.blockedPackagesCsv
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                .orEmpty()

            val effectivePenaltyPkgs = when {
                directPkgs.isNotEmpty() || schedulePkgs.isNotEmpty() -> (directPkgs + schedulePkgs).toSet()
                else -> blockedApps.map { it.packageName }.toSet()
            }
            effectivePenaltyPkgs.forEach { pkg ->
                enforcedReasons[pkg] = "قفل العقاب حتى إتمام عادة: ${habit.title}"
            }
        }

        val wakeSchedule = blockSchedules.firstOrNull { it.scheduleType == "WAKE_ALARM" }
        val effectiveLockActive = isSleepMode || incompletePenaltyHabits.isNotEmpty() || enforcedReasons.isNotEmpty()

        ProtectionManager.syncFromDatabase(
            isSleepMode = effectiveLockActive,
            isStrictProtection = isStrictProtection,
            expectedQrCode = wakeSchedule?.qrReferenceCode ?: "WAKE-QR-2026",
            placeName = wakeSchedule?.placeName ?: "المكان المحدد",
            verificationMethod = wakeSchedule?.verificationMethod ?: "LOCAL_IMAGE",
            emergencyExitType = wakeSchedule?.emergencyExitType ?: "MATH_3",
            blockedStudyPkgs = blockedApps.filter { it.blockInStudyMode }.map { it.packageName }.toSet(),
            blockedSleepPkgs = blockedApps.filter { it.blockInSleepMode }.map { it.packageName }.toSet(),
            enforcedReasonsMap = enforcedReasons
        )

        val timeline = DailyInsightsEngine.buildChronologicalTimeline(
            schedule = schedule,
            prayerStatusMap = prayerMap,
            azkarLogs = fullDayQuint.third,
            wird = fullDayQuint.second,
            lectures = fullDayQuint.first,
            studySessions = spQuint.second.filter { it.dateKey == todayKey },
            tasks = allMain,
            health = fullDayQuint.fourth,
            isSleepModeActive = effectiveLockActive
        )

        val bellModes = PrayerType.entries.associateWith { p ->
            val saved = settingsMap["prayer_bell_${p.id}"]
            if (saved != null) com.example.core.prayer.PrayerBellMode.fromId(saved)
            else if (p.isObligatory) com.example.core.prayer.PrayerBellMode.AZAN
            else if (p == PrayerType.LAST_THIRD) com.example.core.prayer.PrayerBellMode.VIBRATE
            else com.example.core.prayer.PrayerBellMode.SILENT
        }
        val overrideSilent = PrayerType.entries.associateWith { p ->
            settingsMap["prayer_override_silent_${p.id}"] == "true"
        }
        val muazzinMap = PrayerType.entries.associateWith { p ->
            settingsMap["prayer_muazzin_${p.id}"] ?: "makkah"
        }
        val customAlarmsJson = settingsMap["custom_prayer_alarms_json"].orEmpty()
        val customAlarmsList = parseCustomPrayerAlarms(customAlarmsJson)

        val prayerEndAlarm = settingsMap["prayer_end_time_alarm"] == "true"
        val midnightAlarm = settingsMap["midnight_alarm"] == "true"
        val lastThirdAlarm = settingsMap["last_third_alarm"] == "true"
        val fridaySilent = settingsMap["friday_silent_enabled"] != "false"
        val fridayLastHour = settingsMap["friday_last_hour_reminder"] != "false"
        val fridaySurahKahf = settingsMap["friday_surah_kahf_reminder"] != "false"
        val iqamaSilent = settingsMap["iqama_silent_enabled"] != "false"
        val iqamaDuration = settingsMap["iqama_silent_duration"]?.toIntOrNull() ?: 20
        val flipSnooze = settingsMap["flip_to_snooze_azan"] != "false"
        val persistentPrayerNotification = settingsMap["persistent_prayer_notification"] != "false"

        DailyManagerUiState(
            isDarkMode = isDark,
            themePreset = themePreset,
            fontScale = fontScale,
            selectedCity = city,
            prayerSchedule = schedule,
            todayPrayerStatusMap = prayerMap,
            todayPrayerLogsMap = prayerLogsMap,
            mosqueLocations = mosques,
            userGpsPoint = userGps,
            nearestMosqueDistanceMeters = nearestDist,
            nearestMosqueName = nearestName,
            isInsideMosqueNow = insideMosque,
            habits = habits,
            habitLogsByHabitAndDate = logsGrouped,
            activePenaltyHabitsCount = incompletePenaltyHabits.size,
            mainTasks = filteredMain,
            subtasksByParentId = subtasksMap,
            selectedTaskCategory = localQuint.second,
            subjects = spQuint.first,
            studySessions = spQuint.second,
            autoOpenStandby = autoStandby,
            standbyOnCharging = chargingStandby,
            isStandbyVisible = localQuint.fifth,
            blockedApps = blockedApps,
            blockSchedules = blockSchedules,
            usageOverview = usageOverview,
            enforcedBlockReasonsByPackage = enforcedReasons,
            isSleepModeActive = isSleepMode,
            isStrictProtectionEnabled = isStrictProtection,
            lectures = fullDayQuint.first,
            wirdProgress = fullDayQuint.second,
            todayAzkarLogs = fullDayQuint.third,
            todayHealth = fullDayQuint.fourth,
            healthConnectData = insightsQuint.fifth,
            dailySummary = insightsQuint.first,
            weeklyReview = insightsQuint.second,
            yearWeekGoals = insightsQuint.third,
            timelineEvents = timeline,
            prayerBellModesMap = bellModes,
            prayerOverrideSilentMap = overrideSilent,
            prayerMuazzinMap = muazzinMap,
            customPrayerAlarms = customAlarmsList,
            prayerEndTimeAlarmEnabled = prayerEndAlarm,
            midnightAlarmEnabled = midnightAlarm,
            lastThirdAlarmEnabled = lastThirdAlarm,
            fridaySilentEnabled = fridaySilent,
            fridayLastHourReminderEnabled = fridayLastHour,
            fridaySurahKahfReminderEnabled = fridaySurahKahf,
            iqamaSilentEnabled = iqamaSilent,
            iqamaSilentDurationMinutes = iqamaDuration,
            flipToSnoozeAzanEnabled = flipSnooze,
            persistentPrayerNotificationEnabled = persistentPrayerNotification,
            categories = spQuint.sixth,
            selectedCategoryId = localQuint.sixth,
            statusBannerMessage = localQuint.third,
            backupJsonPreview = localQuint.fourth,
            isGoogleConnected = com.example.core.google.GoogleWorkspaceManager.isConnected(getApplication()),
            googleUserEmail = com.example.core.google.GoogleWorkspaceManager.getUserEmail(getApplication()),
            googleCalendarEvents = localQuint.seventh,
            gmailMessages = localQuint.eighth
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DailyManagerUiState()
    )

    val quranBookmarks: StateFlow<List<QuranBookmarkEntity>> = repo.quranBookmarksFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        AdhanNotificationHelper.ensureChannelsCreated(application)
        viewModelScope.launch {
            repo.ensureSeeded()
            val cityId = repo.getSelectedCityId()
            val schedule = UmmAlQuraCalculator.calculateScheduleForNow(
                UmmAlQuraCalculator.findCityById(cityId)
            )
            AdhanNotificationHelper.scheduleDailyPrayerAlarms(getApplication(), schedule)
            ProtectionManager.scheduleDailyWakeAlarm(getApplication(), "04:30")
            refreshHealthConnectNow(silent = true)
            refreshAppUsageOverview()
            autoVerifyMosqueInBackground()
            pushWidgetSnapshot(schedule)
        }
        viewModelScope.launch {
            var tickCounter = 0
            while (true) {
                delay(1000L)
                nowMillisFlow.value = System.currentTimeMillis()
                val completedMsg = StudyTimerManager.timerState.value.lastCompletedMessage
                if (completedMsg != null) {
                    showBanner(completedMsg)
                    StudyTimerManager.internalUpdate { it.copy(lastCompletedMessage = null) }
                }
                tickCounter++
                if (tickCounter % 5 == 0) {
                    refreshAppUsageOverview()
                    if (uiState.value.persistentPrayerNotificationEnabled) {
                        AdhanNotificationHelper.showOrUpdatePersistentPrayerCountdownNotification(
                            getApplication(),
                            uiState.value.prayerSchedule
                        )
                    }
                }
                if (tickCounter % 25 == 0) {
                    refreshHealthConnectNow(silent = true)
                    autoVerifyMosqueInBackground()
                }
            }
        }
    }

    private fun pushWidgetSnapshot(schedule: DailyPrayerSchedule = uiState.value.prayerSchedule) {
        val current = uiState.value
        val completedHabits = current.habits.count { h ->
            current.habitLogsByHabitAndDate[h.id]?.get(todayKey)?.isCompleted == true
        }
        val todayStudyMins = current.studySessions.filter { it.dateKey == todayKey }.sumOf { it.durationMinutes }
        DailyManagerAppWidgetProvider.pushLiveUpdateToHomeWidget(
            context = getApplication(),
            cityName = schedule.city.arabicName,
            nextPrayerName = schedule.nextPrayer.type.arabicName,
            nextPrayerTime12 = schedule.nextPrayer.formatted12h,
            completedHabits = completedHabits,
            totalHabits = current.habits.size,
            wirdPage = current.wirdProgress.currentPage,
            studyMinutesToday = todayStudyMins
        )
    }

    // --- التحقق المكاني التلقائي في الخلفية عند دخول وقت الصلاة ---

    fun autoVerifyMosqueInBackground() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val schedule = uiState.value.prayerSchedule
            val obligatorySlots = schedule.prayers.filter { it.type.isObligatory }

            // 1. تسجيل الصلوات التي مضى وقتها (٤٥ دقيقة بعد الأذان أو دخول الصلاة التالية) دون تسجيل كـ «متأخر»
            val missedPrayers = obligatorySlots.mapIndexedNotNull { idx, slot ->
                val nextMillis = obligatorySlots.getOrNull(idx + 1)?.timeMillisToday ?: Long.MAX_VALUE
                val windowEnd = minOf(slot.timeMillisToday + 45 * 60_000L, nextMillis)
                val status = uiState.value.todayPrayerLogsMap[slot.type]?.status
                if (now > windowEnd && status != "ON_TIME" && status != "LATE") {
                    slot.type
                } else {
                    null
                }
            }
            if (missedPrayers.isNotEmpty()) {
                repo.markMissedPrayersAsLate(todayKey, missedPrayers)
            }

            // 2. التحقق المكاني التلقائي للصلاة الحالية إذا كنا داخل وقتها المسموح
            val mosques = repo.getApprovedMosquesOnce()
            val point = MosqueLocationVerifier.fetchCurrentUserLocation(getApplication())
            if (point != null) {
                userGpsFlow.value = point
            }
            if (mosques.isEmpty() || point == null) return@launch

            val result = MosqueLocationVerifier.verifyUserAtMosque(
                context = getApplication(),
                approvedMosques = mosques,
                overrideUserLocation = point
            )
            if (result.isVerifiedInsideMosque && result.matchedMosque != null) {
                val activePrayerSlot = obligatorySlots.mapIndexedNotNull { idx, slot ->
                    val nextMillis = obligatorySlots.getOrNull(idx + 1)?.timeMillisToday ?: Long.MAX_VALUE
                    val windowEnd = minOf(slot.timeMillisToday + 45 * 60_000L, nextMillis)
                    if (now in slot.timeMillisToday..windowEnd) slot else null
                }.lastOrNull()

                if (activePrayerSlot != null) {
                    val currentStatus = uiState.value.todayPrayerLogsMap[activePrayerSlot.type]?.status
                    if (currentStatus != "ON_TIME") {
                        repo.logPrayerVerifiedAtMosque(
                            dateKey = todayKey,
                            prayerType = activePrayerSlot.type,
                            mosqueName = result.matchedMosque.name,
                            distanceMeters = result.distanceMeters ?: 0
                        )
                        pushWidgetSnapshot()
                    }
                }
            }
        }
    }

    fun refreshUserGpsLocation() {
        autoVerifyMosqueInBackground()
    }

    fun addMosqueWithCoordinates(
        name: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Int = 100
    ) {
        viewModelScope.launch {
            val added = repo.addMosqueLocation(
                name = name.ifBlank { "مسجد معتمد" },
                latitude = latitude,
                longitude = longitude,
                radiusMeters = radiusMeters
            )
            autoVerifyMosqueInBackground()
            showBanner("تم حفظ موقع «${added.name}»")
        }
    }

    fun deleteMosqueLocation(id: String) {
        viewModelScope.launch {
            repo.deleteMosqueLocation(id)
        }
    }

    // --- الورد والأذكار ---

    fun logWirdReading(
        pagesReadNow: Int,
        bookmarkSurah: String,
        bookmarkAyah: Int,
        bookmarkNote: String
    ) {
        viewModelScope.launch {
            val updated = repo.logWirdReadingAndSyncHabit(
                pagesReadNow = pagesReadNow,
                bookmarkSurah = bookmarkSurah,
                bookmarkAyah = bookmarkAyah,
                bookmarkNote = bookmarkNote
            )
            pushWidgetSnapshot()
            showBanner("تم تحديث الورد إلى صفحة ${updated.currentPage}")
        }
    }

    fun saveCurrentQuranPage(page: Int, surahName: String = "", ayahNumber: Int = 1) {
        viewModelScope.launch {
            repo.saveCurrentQuranPage(page, surahName, ayahNumber)
            pushWidgetSnapshot()
        }
    }

    fun configureKhatmahPlan(
        dailyTargetPages: Int,
        planType: String = "PAGES",
        khatmahTitle: String = "الختمة الحالية",
        startPage: Int? = null
    ) {
        viewModelScope.launch {
            repo.configureKhatmahPlan(dailyTargetPages, planType, khatmahTitle, startPage)
            showBanner("تم حفظ خطة الختمة بنجاح 📖")
        }
    }

    fun addQuranBookmark(
        pageNumber: Int,
        surahName: String,
        ayahNumber: Int = 1,
        title: String = "علامة توقف",
        note: String = "",
        colorHex: String = "#059669",
        tag: String = "ورد اليوم"
    ) {
        viewModelScope.launch {
            repo.addQuranBookmark(pageNumber, surahName, ayahNumber, title, note, colorHex, tag)
            showBanner("تم حفظ علامة التوقف لصفحة $pageNumber")
        }
    }

    fun deleteQuranBookmark(id: String) {
        viewModelScope.launch {
            repo.deleteQuranBookmark(id)
            showBanner("تم حذف العلامة")
        }
    }

    fun progressAzkar(azkarLog: AzkarLogEntity, completeImmediately: Boolean = false) {
        viewModelScope.launch {
            repo.progressAzkarAndSyncHabit(todayKey, azkarLog, completeImmediately)
        }
    }

    fun resetAzkar(azkarLog: AzkarLogEntity) {
        viewModelScope.launch {
            repo.resetAzkarLog(azkarLog)
        }
    }

    fun completeAthkarCategory(categoryCode: String) {
        viewModelScope.launch {
            repo.completeAthkarCategory(todayKey, categoryCode)
            val name = if (categoryCode == "MORNING") "أذكار الصباح" else "أذكار المساء"
            showBanner("تقبّل الله منك! تم إكمال $name بنجاح وتحديث العادة اليومية 🌟")
            pushWidgetSnapshot()
        }
    }

    fun toggleAthkarHabit(categoryCode: String, enabled: Boolean) {
        viewModelScope.launch {
            repo.toggleAthkarHabit(categoryCode, enabled)
            val name = if (categoryCode == "MORNING") "أذكار الصباح" else "أذكار المساء"
            if (enabled) {
                showBanner("تم تفعيل $name كعادة يومية في صفحة العادات 🌟")
            } else {
                showBanner("تم إزالة $name من صفحة العادات")
            }
        }
    }

    fun toggleQuranWirdHabit(enabled: Boolean) {
        viewModelScope.launch {
            repo.toggleQuranWirdHabit(enabled)
            val msg = if (enabled) "تم تفعيل عادة الورد القرآني في قائمة العادات 📖" else "تم إزالة عادة الورد القرآني من العادات"
            showBanner(msg)
        }
    }

    fun completeQuranWirdAtAyah(surahName: String, ayahNumber: Int, pageNumber: Int) {
        viewModelScope.launch {
            repo.completeQuranWirdAtAyah(surahName, ayahNumber, pageNumber)
            showBanner("تم حفظ موضع الورد وإتمام عادة الورد القرآني بنجاح 📖✨")
        }
    }

    // --- العادات (تأشير صح/خطأ، قيمة رقمية، مؤقت، Health Connect، عقاب، ومنبه ذكي) ---

    fun setCheckboxHabitState(habit: HabitEntity, completed: Boolean) {
        if (habit.autoSource in setOf("AZKAR_MORNING", "AZKAR_EVENING", "AZKAR")) {
            showBanner("أذكار الصباح والمساء لا تُكتمل يدوياً! يجب قراءتها كاملة في القسم الديني 🕌")
            return
        }
        if (habit.autoSource in setOf("QURAN_WIRD", "WIRD")) {
            showBanner("الورد القرآني لا يُكتمل يدوياً! يجب قراءة الورد في المصحف ووضع علامة التوقف عند الآية المطلوبة 📖")
            return
        }
        viewModelScope.launch {
            repo.setCheckboxHabitState(habit.id, todayKey, completed)
            pushWidgetSnapshot()
        }
    }

    // --- التقويم والأحداث ومزامنة تقويم الهاتف (سامسونج) ---

    fun refreshDeviceCalendarEvents(context: Context) {
        viewModelScope.launch {
            isDeviceCalendarSyncingFlow.value = true
            try {
                val events = DeviceCalendarManager.fetchDeviceCalendarEvents(context)
                deviceCalendarEventsFlow.value = events
                if (events.isNotEmpty()) {
                    showBanner("تمت مزامنة ${events.size} حدث من تقويم الهاتف 📱")
                } else if (!DeviceCalendarManager.hasCalendarPermission(context)) {
                    showBanner("يرجى منح إذن قراءة التقويم لجلب أحداث تقويم الهاتف (سامسونج)")
                } else {
                    showBanner("لا توجد أحداث مجدولة في تقويم الهاتف حالياً")
                }
            } catch (e: Exception) {
                showBanner("تعذر جلب أحداث تقويم الهاتف: ${e.localizedMessage}")
            } finally {
                isDeviceCalendarSyncingFlow.value = false
            }
        }
    }

    fun addAppEvent(
        title: String,
        description: String = "",
        location: String = "",
        dateKey: String,
        startTime: String = "",
        endTime: String = "",
        colorHex: String = "#059669",
        isAllDay: Boolean = false,
        category: String = "عام",
        reminderMinutes: Int = 15,
        syncToDevice: Boolean = false,
        context: Context? = null
    ) {
        viewModelScope.launch {
            val event = AppEventEntity(
                title = title.trim(),
                description = description.trim(),
                location = location.trim(),
                dateKey = dateKey,
                startTime = startTime,
                endTime = endTime,
                colorHex = colorHex,
                isAllDay = isAllDay,
                category = category,
                reminderMinutesBefore = reminderMinutes
            )
            repo.upsertAppEvent(event)

            if (syncToDevice && context != null) {
                try {
                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    val startMillis = if (startTime.isNotBlank()) {
                        sdf.parse("$dateKey $startTime")?.time ?: System.currentTimeMillis()
                    } else {
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateKey)?.time ?: System.currentTimeMillis()
                    }
                    val endMillis = if (endTime.isNotBlank()) {
                        sdf.parse("$dateKey $endTime")?.time ?: (startMillis + 3600_000L)
                    } else {
                        startMillis + 3600_000L
                    }
                    DeviceCalendarManager.addEventToDeviceCalendar(
                        context = context,
                        title = title,
                        description = description,
                        location = location,
                        startMillis = startMillis,
                        endMillis = endMillis,
                        isAllDay = isAllDay
                    )
                } catch (_: Exception) {}
            }

            showBanner("تمت إضافة الحدث بنجاح إلى التقويم 📅")
        }
    }

    fun deleteAppEvent(id: String) {
        viewModelScope.launch {
            repo.deleteAppEvent(id)
            showBanner("تم حذف الحدث من التقويم")
        }
    }

    fun incrementValueHabit(habit: HabitEntity, stepAmount: Int = 1) {
        viewModelScope.launch {
            repo.incrementHabitProgress(
                habitId = habit.id,
                dateKey = todayKey,
                stepAmount = stepAmount,
                sourceNote = "إدخال رقمي"
            )
            pushWidgetSnapshot()
        }
    }

    fun updateValueHabitExact(habit: HabitEntity, newValue: Int) {
        viewModelScope.launch {
            repo.updateValueHabitProgress(habit.id, todayKey, newValue)
            pushWidgetSnapshot()
        }
    }

    fun startHabitTimer(habit: HabitEntity) {
        val mins = habit.targetCount.coerceAtLeast(1)
        val mode = if (habit.timerType == "POMODORO") "POMODORO" else "COUNT_UP"
        val linkedSubj = uiState.value.subjects.find { it.id == habit.linkedSubjectId }
        val targetSubject = linkedSubj ?: SubjectEntity(
            id = "habit_${habit.id}",
            name = habit.title,
            colorHex = "#145946",
            dailyGoalMinutes = mins,
            weeklyGoalMinutes = mins * 7,
            defaultTimerType = mode,
            pomodoroFocusMin = habit.pomodoroFocusMin,
            pomodoroBreakMin = habit.pomodoroBreakMin,
            pomodoroRounds = habit.pomodoroRounds
        )
        StudyTimerManager.startTimer(
            context = getApplication(),
            subject = targetSubject,
            mode = mode,
            targetMinutes = mins,
            linkedHabitId = habit.id,
            pomodoroFocusMin = habit.pomodoroFocusMin,
            pomodoroBreakMin = habit.pomodoroBreakMin,
            pomodoroRounds = habit.pomodoroRounds
        )
    }

    // --- الفئات (يونيفرسل) ---

    fun selectCategoryFilter(id: String) {
        selectedCategoryIdFlow.value = id
    }

    fun addCategory(name: String, colorHex: String, iconKey: String) {
        viewModelScope.launch {
            repo.addCategory(name, colorHex, iconKey)
            showBanner("تمت إضافة الفئة «$name»")
        }
    }

    fun updateCategory(id: String, name: String, colorHex: String, iconKey: String) {
        viewModelScope.launch {
            repo.updateCategory(id, name, colorHex, iconKey)
            showBanner("تم تعديل الفئة بنجاح")
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            repo.deleteCategory(id)
            if (selectedCategoryIdFlow.value == id) {
                selectedCategoryIdFlow.value = ""
            }
            showBanner("تم حذف الفئة")
        }
    }

    // --- العادات ---

    fun recordHabitRestSkip(habit: HabitEntity, dateKey: String) {
        viewModelScope.launch {
            repo.recordHabitRestSkip(habit.id, dateKey)
            pushWidgetSnapshot()
            showBanner("تم تسجيل يوم راحة / تخطي 🏖️")
        }
    }

    fun updateHabitChecklistItem(
        habit: HabitEntity,
        dateKey: String,
        itemId: String,
        checked: Boolean? = null,
        deltaValue: Int? = null,
        setValue: Int? = null
    ) {
        viewModelScope.launch {
            repo.updateHabitChecklistItem(habit.id, dateKey, itemId, checked, deltaValue, setValue)
            pushWidgetSnapshot()
        }
    }

    fun addHabit(
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
    ) {
        viewModelScope.launch {
            val created = repo.addHabit(
                title = title,
                description = description,
                frequencyType = frequencyType,
                targetCount = targetCount,
                unitLabel = unitLabel,
                autoSource = autoSource,
                alarmTime24 = alarmTime24,
                alarmEnabled = alarmEnabled,
                goalMode = goalMode,
                timerType = timerType,
                pomodoroFocusMin = pomodoroFocusMin,
                pomodoroBreakMin = pomodoroBreakMin,
                pomodoroRounds = pomodoroRounds,
                linkedSubjectId = linkedSubjectId,
                penaltyLockApps = penaltyLockApps,
                penaltyScheduleId = penaltyScheduleId,
                penaltyBlockedPackages = penaltyBlockedPackages,
                alarmType = alarmType,
                alarmVerifyMethod = alarmVerifyMethod,
                alarmPlaceName = alarmPlaceName,
                alarmQrCode = alarmQrCode,
                categoryId = categoryId,
                comparisonOperator = comparisonOperator,
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
                checklistItemsJson = checklistItemsJson,
                checklistRequiredCount = checklistRequiredCount
            )
            if (created.alarmEnabled && created.alarmTime24.isNotBlank()) {
                AdhanNotificationHelper.scheduleHabitOrTaskDailyAlarm(
                    context = getApplication(),
                    itemId = created.id,
                    title = created.title,
                    isHabit = true,
                    time24 = created.alarmTime24,
                    alarmType = created.alarmType,
                    verifyMethod = created.alarmVerifyMethod,
                    placeName = created.alarmPlaceName,
                    qrCode = created.alarmQrCode
                )
            }
            if (HealthGoalMetric.isHealthSource(autoSource)) {
                refreshHealthConnectNow(silent = true)
            }
            pushWidgetSnapshot()
            showBanner("تمت إضافة العادة «$title»")
        }
    }

    fun updateHabitAlarm(
        habitId: String,
        alarmTime24: String,
        alarmEnabled: Boolean,
        alarmType: String = "NORMAL",
        alarmVerifyMethod: String = "LOCAL_IMAGE",
        alarmPlaceName: String = "المكان المحدد",
        alarmQrCode: String = "WAKE-QR-2026"
    ) {
        viewModelScope.launch {
            val updated = repo.updateHabitAlarm(
                habitId = habitId,
                alarmTime24 = alarmTime24,
                alarmEnabled = alarmEnabled,
                alarmType = alarmType,
                alarmVerifyMethod = alarmVerifyMethod,
                alarmPlaceName = alarmPlaceName,
                alarmQrCode = alarmQrCode
            ) ?: return@launch
            if (updated.alarmEnabled && updated.alarmTime24.isNotBlank()) {
                AdhanNotificationHelper.scheduleHabitOrTaskDailyAlarm(
                    context = getApplication(),
                    itemId = updated.id,
                    title = updated.title,
                    isHabit = true,
                    time24 = updated.alarmTime24,
                    alarmType = updated.alarmType,
                    verifyMethod = updated.alarmVerifyMethod,
                    placeName = updated.alarmPlaceName,
                    qrCode = updated.alarmQrCode
                )
                showBanner("تم ضبط المنبه على ${updated.alarmTime24}")
            } else {
                AdhanNotificationHelper.cancelHabitOrTaskAlarm(getApplication(), updated.id)
                showBanner("تم إيقاف المنبه")
            }
        }
    }

    fun updateHabitPenaltyConfig(
        habitId: String,
        penaltyLockApps: Boolean,
        penaltyScheduleId: String,
        penaltyBlockedPackages: String
    ) {
        viewModelScope.launch {
            repo.updateHabitPenaltyConfig(
                habitId = habitId,
                penaltyLockApps = penaltyLockApps,
                penaltyScheduleId = penaltyScheduleId,
                penaltyBlockedPackages = penaltyBlockedPackages
            )
            showBanner("تم تحديث إعدادات قفل العقاب")
        }
    }

    fun toggleHabitPenaltyLock(habitId: String) {
        viewModelScope.launch {
            repo.toggleHabitPenaltyLock(habitId)
        }
    }

    fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            AdhanNotificationHelper.cancelHabitOrTaskAlarm(getApplication(), habitId)
            repo.deleteHabit(habitId)
            pushWidgetSnapshot()
        }
    }

    // --- Health Connect وحساس الخطوات (في الخلفية ومن الإعدادات فقط) ---

    fun refreshHealthConnectNow(silent: Boolean = false) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val currentSteps = uiState.value.todayHealth.stepsCount
            val hcData = HealthConnectManager.readTodayData(
                context = context,
                hardwareSensorStepsFallback = currentSteps
            )
            healthConnectDataFlow.value = hcData

            repo.updateAllHealthConnectMetricsAndSyncHabits(
                dateKey = todayKey,
                stepsCount = hcData.steps,
                sleepHours = hcData.sleepHours,
                activeCalories = hcData.activeCalories,
                totalCalories = hcData.totalCalories,
                distanceMeters = hcData.distanceMeters,
                exerciseMinutes = hcData.exerciseMinutes,
                waterMl = hcData.waterMl,
                avgHeartRateBpm = hcData.avgHeartRateBpm,
                syncSource = hcData.statusMessage
            )
            pushWidgetSnapshot()
            if (!silent) {
                showBanner(hcData.statusMessage)
            }
        }
    }

    fun onHardwareSensorStepsDetected(rawSensorTotalSteps: Int) {
        val context = getApplication<Application>()
        val todaySteps = StepSensorManager.processHardwareStepCounterValue(context, rawSensorTotalSteps)
        val burned = StepSensorManager.calculateBurnedCaloriesFromSteps(todaySteps)
        viewModelScope.launch {
            repo.updateHealthAndSyncHabit(
                dateKey = todayKey,
                stepsCount = todaySteps,
                burnedCalories = burned,
                sleepHours = uiState.value.todayHealth.sleepHours
            )
        }
    }

    // --- المهام ومنبهاتها (منبه عادي أو منبه ذكي خارق) ---

    fun selectTaskCategory(category: String) {
        selectedTaskCategoryFlow.value = category
    }

    fun addTask(
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
    ) {
        viewModelScope.launch {
            val created = repo.addTask(
                title = title,
                notes = notes,
                listCategory = listCategory,
                priority = priority,
                dueDateText = dueDateText,
                reminderTimeText = reminderTimeText,
                recurrence = recurrence,
                parentTaskId = parentTaskId,
                alarmTime24 = alarmTime24,
                alarmEnabled = alarmEnabled,
                alarmType = alarmType,
                alarmVerifyMethod = alarmVerifyMethod,
                alarmPlaceName = alarmPlaceName,
                alarmQrCode = alarmQrCode,
                categoryId = categoryId
            )
            if (created.alarmEnabled && created.alarmTime24.isNotBlank()) {
                AdhanNotificationHelper.scheduleHabitOrTaskDailyAlarm(
                    context = getApplication(),
                    itemId = created.id,
                    title = created.title,
                    isHabit = false,
                    time24 = created.alarmTime24,
                    alarmType = created.alarmType,
                    verifyMethod = created.alarmVerifyMethod,
                    placeName = created.alarmPlaceName,
                    qrCode = created.alarmQrCode
                )
            }
            showBanner("تمت إضافة المهمة «$title»")
        }
    }

    fun updateTaskAlarm(
        taskId: String,
        alarmTime24: String,
        alarmEnabled: Boolean,
        alarmType: String = "NORMAL",
        alarmVerifyMethod: String = "LOCAL_IMAGE",
        alarmPlaceName: String = "المكان المحدد",
        alarmQrCode: String = "WAKE-QR-2026"
    ) {
        viewModelScope.launch {
            val updated = repo.updateTaskAlarm(
                taskId = taskId,
                alarmTime24 = alarmTime24,
                alarmEnabled = alarmEnabled,
                alarmType = alarmType,
                alarmVerifyMethod = alarmVerifyMethod,
                alarmPlaceName = alarmPlaceName,
                alarmQrCode = alarmQrCode
            ) ?: return@launch
            if (updated.alarmEnabled && updated.alarmTime24.isNotBlank()) {
                AdhanNotificationHelper.scheduleHabitOrTaskDailyAlarm(
                    context = getApplication(),
                    itemId = updated.id,
                    title = updated.title,
                    isHabit = false,
                    time24 = updated.alarmTime24,
                    alarmType = updated.alarmType,
                    verifyMethod = updated.alarmVerifyMethod,
                    placeName = updated.alarmPlaceName,
                    qrCode = updated.alarmQrCode
                )
                showBanner("تم ضبط المنبه على ${updated.alarmTime24}")
            } else {
                AdhanNotificationHelper.cancelHabitOrTaskAlarm(getApplication(), updated.id)
                showBanner("تم إيقاف المنبه")
            }
        }
    }

    fun toggleTask(taskId: String) {
        viewModelScope.launch {
            repo.toggleTaskCompletion(taskId)
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            AdhanNotificationHelper.cancelHabitOrTaskAlarm(getApplication(), taskId)
            repo.deleteTask(taskId)
        }
    }

    // --- العداد (المؤقت التصاعدي والبومودورو المخصص) ---

    fun selectSubject(subject: SubjectEntity) {
        StudyTimerManager.updateSelection(subject)
    }

    fun addSubject(
        name: String,
        colorHex: String,
        dailyGoal: Int,
        weeklyGoal: Int,
        defaultTimerType: String = "COUNT_UP",
        pomodoroFocusMin: Int = 25,
        pomodoroBreakMin: Int = 5,
        pomodoroRounds: Int = 2
    ) {
        viewModelScope.launch {
            repo.addSubject(
                name = name,
                colorHex = colorHex,
                dailyGoalMinutes = dailyGoal,
                weeklyGoalMinutes = weeklyGoal,
                defaultTimerType = defaultTimerType,
                pomodoroFocusMin = pomodoroFocusMin,
                pomodoroBreakMin = pomodoroBreakMin,
                pomodoroRounds = pomodoroRounds
            )
            showBanner("تمت إضافة مادة «$name»")
        }
    }

    fun deleteSubject(subjectId: String) {
        viewModelScope.launch {
            repo.deleteSubject(subjectId)
        }
    }

    // --- نظام الحظر المتكامل (StayFree) ومنبه الاستيقاظ الذكي ---

    fun refreshAppUsageOverview() {
        viewModelScope.launch {
            val overview = AppUsageMonitor.readTodayUsageOverview(getApplication())
            usageOverviewFlow.value = overview
        }
    }

    fun triggerWakeAlarmNow(alarm: BlockScheduleEntity? = null) {
        val target = alarm ?: uiState.value.blockSchedules.firstOrNull { it.scheduleType == "WAKE_ALARM" }
        val method = target?.verificationMethod ?: "LOCAL_IMAGE"
        val title = target?.title ?: if (method == "LOCAL_IMAGE") {
            "منبه الاستيقاظ • ${target?.placeName ?: "المكان المحدد"}"
        } else {
            "منبه الاستيقاظ • رمز QR"
        }
        ProtectionManager.triggerWakeAlarmNow(
            context = getApplication(),
            alarmId = target?.id.orEmpty(),
            title = title,
            overrideVerifyMethod = method,
            overridePlaceName = target?.placeName ?: "المكان المحدد",
            overrideQrCode = target?.qrReferenceCode ?: "WAKE-QR-2026",
            overrideRingtoneUri = target?.ringtoneUri,
            overrideRingtoneTitle = target?.ringtoneTitle
        )
    }

    fun dismissWakeAlarmVerified(message: String) {
        ProtectionManager.dismissWakeAlarmAfterVerification(getApplication())
        showBanner(message)
    }

    fun saveWakeAlarm(alarm: BlockScheduleEntity) {
        viewModelScope.launch {
            repo.updateBlockSchedule(alarm)
            if (alarm.isEnabled) {
                ProtectionManager.scheduleWakeAlarm(getApplication(), alarm)
            } else {
                ProtectionManager.cancelWakeAlarm(getApplication(), alarm.id)
            }
            showBanner("تم حفظ المنبه «${alarm.title}» (${alarm.startTime24})")
        }
    }

    fun toggleWakeAlarm(alarm: BlockScheduleEntity) {
        viewModelScope.launch {
            val updated = alarm.copy(isEnabled = !alarm.isEnabled)
            repo.updateBlockSchedule(updated)
            if (updated.isEnabled) {
                ProtectionManager.scheduleWakeAlarm(getApplication(), updated)
                showBanner("تم تفعيل المنبه «${updated.title}» (${updated.startTime24})")
            } else {
                ProtectionManager.cancelWakeAlarm(getApplication(), updated.id)
                showBanner("تم إيقاف المنبه «${updated.title}»")
            }
        }
    }

    fun deleteWakeAlarm(id: String) {
        viewModelScope.launch {
            ProtectionManager.cancelWakeAlarm(getApplication(), id)
            repo.deleteBlockSchedule(id)
            showBanner("تم حذف المنبه")
        }
    }

    fun updateWakeSettings(
        time24: String,
        verifyMethod: String,
        qrCode: String,
        placeName: String = "المكان المحدد"
    ) {
        viewModelScope.launch {
            val existing = uiState.value.blockSchedules.firstOrNull { it.scheduleType == "WAKE_ALARM" }
                ?: BlockScheduleEntity(
                    id = "schedule_wake_alarm",
                    title = "منبه الاستيقاظ",
                    scheduleType = "WAKE_ALARM"
                )
            val updated = existing.copy(
                startTime24 = time24,
                verificationMethod = verifyMethod,
                qrReferenceCode = qrCode,
                placeName = placeName
            )
            repo.updateBlockSchedule(updated)
            ProtectionManager.scheduleWakeAlarm(getApplication(), updated)
            showBanner("تم حفظ إعدادات المنبه ($time24)")
        }
    }

    fun saveCustomBlockSchedule(
        id: String? = null,
        title: String,
        startTime24: String,
        endTime24: String,
        blockedPackagesCsv: String,
        isEnabled: Boolean = true
    ) {
        viewModelScope.launch {
            repo.saveCustomBlockSchedule(
                id = id,
                title = title,
                startTime24 = startTime24,
                endTime24 = endTime24,
                blockedPackagesCsv = blockedPackagesCsv,
                isEnabled = isEnabled
            )
            showBanner("تم حفظ جدول الحظر «$title»")
        }
    }

    fun toggleScheduleEnabled(schedule: BlockScheduleEntity) {
        viewModelScope.launch {
            repo.updateBlockSchedule(schedule.copy(isEnabled = !schedule.isEnabled))
        }
    }

    fun deleteBlockSchedule(id: String) {
        viewModelScope.launch {
            repo.deleteBlockSchedule(id)
        }
    }

    fun toggleSleepMode(active: Boolean) {
        viewModelScope.launch {
            repo.updateSetting("sleep_mode_active", active.toString())
        }
    }

    fun dismissBlockerOverlay() {
        ProtectionManager.dismissBlockerOverlay()
    }

    fun emergencyUnlockAllProtection() {
        viewModelScope.launch {
            repo.updateSetting("sleep_mode_active", "false")
            StudyTimerManager.cancelTimer(getApplication())
            ProtectionManager.dismissBlockerOverlay()
        }
    }

    fun addOrUpdateBlockedApp(
        appName: String,
        packageName: String,
        category: String = "تطبيق",
        dailyLimitMinutes: Int = 0,
        isAlwaysBlocked: Boolean = false,
        blockInStudyMode: Boolean = true,
        blockInSleepMode: Boolean = true
    ) {
        viewModelScope.launch {
            repo.addOrUpdateBlockedApp(
                appName = appName,
                packageName = packageName,
                category = category,
                dailyLimitMinutes = dailyLimitMinutes,
                isAlwaysBlocked = isAlwaysBlocked,
                blockInStudyMode = blockInStudyMode,
                blockInSleepMode = blockInSleepMode
            )
        }
    }

    fun toggleBlockedAppStudy(app: BlockedAppEntity) {
        viewModelScope.launch {
            repo.toggleBlockedAppStudy(app)
        }
    }

    fun toggleBlockedAppSleep(app: BlockedAppEntity) {
        viewModelScope.launch {
            repo.toggleBlockedAppSleep(app)
        }
    }

    fun addBlockedApp(appName: String, packageName: String, category: String) {
        addOrUpdateBlockedApp(appName = appName, packageName = packageName, category = category)
    }

    fun deleteBlockedApp(id: String) {
        viewModelScope.launch {
            repo.deleteBlockedApp(id)
        }
    }

    // --- الإعدادات العامة ---

    fun toggleDarkMode() {
        viewModelScope.launch {
            val next = !uiState.value.isDarkMode
            repo.updateSetting("dark_mode", next.toString())
        }
    }

    fun setThemePreset(preset: com.example.ui.theme.ThemePreset) {
        viewModelScope.launch {
            repo.updateSetting("theme_preset", preset.id)
            showBanner("تم تطبيق طابع «${preset.arabicName}»")
        }
    }

    fun setFontScale(scale: Float) {
        viewModelScope.launch {
            repo.updateSetting("font_scale", scale.toString())
        }
    }

    // --- تكامل Google Workspace (Calendar & Gmail) ---

    fun saveGoogleAccessToken(token: String, email: String = "") {
        viewModelScope.launch {
            com.example.core.google.GoogleWorkspaceManager.saveAccessToken(getApplication(), token, email)
            showBanner("تم ربط حساب Google بنجاح ✅")
            refreshGoogleCalendarEvents()
            refreshGmailMessages()
        }
    }

    fun disconnectGoogleAccount() {
        viewModelScope.launch {
            com.example.core.google.GoogleWorkspaceManager.disconnect(getApplication())
            googleCalendarEventsFlow.value = emptyList()
            gmailMessagesFlow.value = emptyList()
            showBanner("تم فصل حساب Google")
        }
    }

    fun refreshGoogleCalendarEvents() {
        viewModelScope.launch {
            val res = com.example.core.google.GoogleWorkspaceManager.fetchTodayCalendarEvents(getApplication())
            res.onSuccess { events ->
                googleCalendarEventsFlow.value = events
            }.onFailure { e ->
                Log.w("DailyManagerViewModel", "Failed to fetch calendar events: ${e.message}")
            }
        }
    }

    fun refreshGmailMessages() {
        viewModelScope.launch {
            val res = com.example.core.google.GoogleWorkspaceManager.fetchRecentEmailsAndExtractTasks(getApplication())
            res.onSuccess { msgs ->
                gmailMessagesFlow.value = msgs
            }.onFailure { e ->
                Log.w("DailyManagerViewModel", "Failed to fetch Gmail messages: ${e.message}")
            }
        }
    }

    fun exportTaskToGoogleCalendar(task: TaskEntity) {
        viewModelScope.launch {
            if (!com.example.core.google.GoogleWorkspaceManager.isConnected(getApplication())) {
                showBanner("يرجى ربط حساب Google أولاً من الإعدادات")
                return@launch
            }
            showBanner("جاري تصدير «${task.title}» إلى تقويم Google...")
            val res = com.example.core.google.GoogleWorkspaceManager.exportTaskToCalendar(getApplication(), task)
            res.onSuccess {
                showBanner("تم تصدير المهمة إلى تقويم Google بنجاح 📅")
                refreshGoogleCalendarEvents()
            }.onFailure { err ->
                showBanner("فشل تصدير المهمة: ${err.message}")
            }
        }
    }

    fun addEmailTaskDirectly(emailItem: com.example.core.google.GmailEmailItem) {
        val title = emailItem.extractedTaskTitle ?: "متابعة إيميل: ${emailItem.subject}"
        addTask(
            title = title,
            notes = "من: ${emailItem.sender}\n${emailItem.snippet}",
            listCategory = "اليوم",
            priority = "MEDIUM",
            dueDateText = "اليوم",
            reminderTimeText = "بدون تذكير",
            recurrence = "NONE",
            parentTaskId = null,
            alarmTime24 = "",
            alarmEnabled = false,
            alarmType = "SOUND",
            alarmVerifyMethod = "NORMAL",
            alarmPlaceName = "",
            alarmQrCode = "",
            categoryId = ""
        )
        showBanner("تمت إضافة المهمة من البريد الإلكتروني ✅")
    }

    fun openStandbyMode() {
        isStandbyVisibleFlow.value = true
    }

    fun closeStandbyMode() {
        isStandbyVisibleFlow.value = false
    }

    fun toggleStandbyOnCharging() {
        viewModelScope.launch {
            val next = !uiState.value.standbyOnCharging
            repo.updateSetting("standby_on_charging", next.toString())
        }
    }

    fun selectCity(cityId: String) {
        viewModelScope.launch {
            repo.updateSetting("city_id", cityId)
            val city = UmmAlQuraCalculator.findCityById(cityId)
            val schedule = UmmAlQuraCalculator.calculateScheduleForNow(city)
            AdhanNotificationHelper.scheduleDailyPrayerAlarms(getApplication(), schedule)
            pushWidgetSnapshot(schedule)
            showBanner("تم اختيار مدينة ${city.arabicName}")
        }
    }

    fun saveCustomCoordinates(name: String, lat: Double, lng: Double) {
        viewModelScope.launch {
            repo.updateSetting("custom_city_name", name.ifBlank { "موقعي" })
            repo.updateSetting("custom_lat", lat.toString())
            repo.updateSetting("custom_lng", lng.toString())
            repo.updateSetting("city_id", "custom")
            showBanner("تم تحديث موقع المدينة")
        }
    }

    fun addLecture(
        title: String,
        courseCode: String,
        location: String,
        dayOfWeek: Int,
        startTime24: String,
        endTime24: String,
        reminderMinutesBefore: Int
    ) {
        viewModelScope.launch {
            repo.addLecture(title, courseCode, location, dayOfWeek, startTime24, endTime24, reminderMinutesBefore)
        }
    }

    fun deleteLecture(id: String) {
        viewModelScope.launch {
            repo.deleteLecture(id)
        }
    }

    fun clearBanner() {
        bannerDismissJob?.cancel()
        statusBannerFlow.value = null
    }

    fun showBanner(message: String) {
        bannerDismissJob?.cancel()
        statusBannerFlow.value = message
        bannerDismissJob = viewModelScope.launch {
            delay(3000L)
            if (statusBannerFlow.value == message) {
                statusBannerFlow.value = null
            }
        }
    }

    // --- إعدادات وضبط الصلوات والمنبهات ---

    fun setPrayerBellMode(prayerType: PrayerType, mode: com.example.core.prayer.PrayerBellMode) {
        viewModelScope.launch {
            repo.updateSetting("prayer_bell_${prayerType.id}", mode.id)
            showBanner("تم تعيين تنبيه ${prayerType.arabicName} إلى: ${mode.arabicTitle}")
        }
    }

    fun setPrayerOverrideSilent(prayerType: PrayerType, override: Boolean) {
        viewModelScope.launch {
            repo.updateSetting("prayer_override_silent_${prayerType.id}", override.toString())
            showBanner(if (override) "تم تفعيل التنبيه حتى لو كان الجوال صامت" else "تم إيقاف تجاوز وضع الصامت")
        }
    }

    fun setPrayerMuazzin(prayerType: PrayerType, voiceId: String) {
        viewModelScope.launch {
            repo.updateSetting("prayer_muazzin_${prayerType.id}", voiceId)
            val voice = com.example.core.prayer.MuazzinCatalog.findVoiceById(voiceId)
            showBanner("تم اختيار صوت: ${voice.name}")
        }
    }

    fun previewMuazzinVoice(voiceId: String) {
        AdhanNotificationHelper.playMuazzinPreviewSound(getApplication(), voiceId)
    }

    fun addCustomPrayerAlarm(
        name: String,
        prayerIds: List<String>,
        isBeforeAzan: Boolean,
        offsetMinutes: Int,
        daysOfWeek: List<Int>,
        soundType: String
    ) {
        viewModelScope.launch {
            val current = uiState.value.customPrayerAlarms
            val newAlarm = com.example.core.prayer.CustomPrayerAlarm(
                name = name.ifBlank { "منبه مخصص" },
                prayerIds = if (prayerIds.isEmpty()) listOf("FAJR", "DHUHR", "ASR", "MAGHRIB", "ISHA") else prayerIds,
                isBeforeAzan = isBeforeAzan,
                offsetMinutes = offsetMinutes,
                daysOfWeek = if (daysOfWeek.isEmpty()) listOf(1, 2, 3, 4, 5, 6, 7) else daysOfWeek,
                soundType = soundType,
                isEnabled = true
            )
            val updated = current + newAlarm
            val json = serializeCustomPrayerAlarms(updated)
            repo.updateSetting("custom_prayer_alarms_json", json)
            showBanner("تمت إضافة منبه «${newAlarm.name}»")
        }
    }

    fun toggleCustomPrayerAlarm(alarmId: String) {
        viewModelScope.launch {
            val current = uiState.value.customPrayerAlarms
            val updated = current.map {
                if (it.id == alarmId) it.copy(isEnabled = !it.isEnabled) else it
            }
            val json = serializeCustomPrayerAlarms(updated)
            repo.updateSetting("custom_prayer_alarms_json", json)
        }
    }

    fun deleteCustomPrayerAlarm(alarmId: String) {
        viewModelScope.launch {
            val current = uiState.value.customPrayerAlarms
            val updated = current.filter { it.id != alarmId }
            val json = serializeCustomPrayerAlarms(updated)
            repo.updateSetting("custom_prayer_alarms_json", json)
            showBanner("تم حذف المنبه المخصص")
        }
    }

    fun togglePrayerEndTimeAlarm() {
        viewModelScope.launch {
            val next = !uiState.value.prayerEndTimeAlarmEnabled
            repo.updateSetting("prayer_end_time_alarm", next.toString())
            showBanner(if (next) "تم تفعيل منبه خروج وقت الصلاة" else "تم إيقاف منبه خروج وقت الصلاة")
        }
    }

    fun toggleMidnightAlarm() {
        viewModelScope.launch {
            val next = !uiState.value.midnightAlarmEnabled
            repo.updateSetting("midnight_alarm", next.toString())
            showBanner(if (next) "تم تفعيل منبه منتصف الليل" else "تم إيقاف منبه منتصف الليل")
        }
    }

    fun toggleLastThirdAlarm() {
        viewModelScope.launch {
            val next = !uiState.value.lastThirdAlarmEnabled
            repo.updateSetting("last_third_alarm", next.toString())
            showBanner(if (next) "تم تفعيل منبه الثلث الأخير من الليل (قيام الليل)" else "تم إيقاف منبه الثلث الأخير")
        }
    }

    fun toggleFridaySilent() {
        viewModelScope.launch {
            val next = !uiState.value.fridaySilentEnabled
            repo.updateSetting("friday_silent_enabled", next.toString())
            showBanner(if (next) "تم تفعيل الصامت التلقائي في صلاة الجمعة" else "تم إيقاف الصامت التلقائي في صلاة الجمعة")
        }
    }

    fun toggleFridayLastHourReminder() {
        viewModelScope.launch {
            val next = !uiState.value.fridayLastHourReminderEnabled
            repo.updateSetting("friday_last_hour_reminder", next.toString())
            showBanner(if (next) "تم تفعيل تذكير آخر ساعة من يوم الجمعة" else "تم إيقاف تذكير آخر ساعة")
        }
    }

    fun toggleFridaySurahKahfReminder() {
        viewModelScope.launch {
            val next = !uiState.value.fridaySurahKahfReminderEnabled
            repo.updateSetting("friday_surah_kahf_reminder", next.toString())
            showBanner(if (next) "تم تفعيل تذكير قراءة سورة الكهف" else "تم إيقاف تذكير سورة الكهف")
        }
    }

    fun toggleIqamaSilent() {
        viewModelScope.launch {
            val next = !uiState.value.iqamaSilentEnabled
            repo.updateSetting("iqama_silent_enabled", next.toString())
            showBanner(if (next) "تم تفعيل صامت الإقامة التلقائي" else "تم إيقاف صامت الإقامة")
        }
    }

    fun setIqamaSilentDuration(minutes: Int) {
        viewModelScope.launch {
            repo.updateSetting("iqama_silent_duration", minutes.toString())
            showBanner("تم تحديد مدة صامت الإقامة: $minutes دقيقة")
        }
    }

    fun toggleFlipToSnoozeAzan() {
        viewModelScope.launch {
            val next = !uiState.value.flipToSnoozeAzanEnabled
            repo.updateSetting("flip_to_snooze_azan", next.toString())
            showBanner(if (next) "تم تفعيل إيقاف الأذان بقلب الجهاز" else "تم إيقاف ميزة قلب الجهاز")
        }
    }

    fun togglePersistentPrayerNotification() {
        viewModelScope.launch {
            val next = !uiState.value.persistentPrayerNotificationEnabled
            repo.updateSetting("persistent_prayer_notification", next.toString())
            if (next) {
                AdhanNotificationHelper.showOrUpdatePersistentPrayerCountdownNotification(
                    getApplication(),
                    uiState.value.prayerSchedule
                )
                showBanner("تم تفعيل عداد الصلاة الدائم في لوحة الإشعارات")
            } else {
                AdhanNotificationHelper.cancelPersistentPrayerCountdownNotification(getApplication())
                showBanner("تم إيقاف عداد الصلاة الدائم")
            }
        }
    }

    private fun parseCustomPrayerAlarms(json: String): List<com.example.core.prayer.CustomPrayerAlarm> {
        if (json.isBlank()) return emptyList()
        val list = mutableListOf<com.example.core.prayer.CustomPrayerAlarm>()
        try {
            val arr = org.json.JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val prayerIdsArr = obj.optJSONArray("prayerIds")
                val prayerIds = mutableListOf<String>()
                if (prayerIdsArr != null) {
                    for (j in 0 until prayerIdsArr.length()) {
                        prayerIds.add(prayerIdsArr.getString(j))
                    }
                }
                val daysArr = obj.optJSONArray("daysOfWeek")
                val daysOfWeek = mutableListOf<Int>()
                if (daysArr != null) {
                    for (j in 0 until daysArr.length()) {
                        daysOfWeek.add(daysArr.getInt(j))
                    }
                }
                list.add(
                    com.example.core.prayer.CustomPrayerAlarm(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        name = obj.optString("name", "منبه صلاة"),
                        prayerIds = if (prayerIds.isEmpty()) listOf("FAJR", "DHUHR", "ASR", "MAGHRIB", "ISHA") else prayerIds,
                        isBeforeAzan = obj.optBoolean("isBeforeAzan", true),
                        offsetMinutes = obj.optInt("offsetMinutes", 10),
                        daysOfWeek = if (daysOfWeek.isEmpty()) listOf(1, 2, 3, 4, 5, 6, 7) else daysOfWeek,
                        soundType = obj.optString("soundType", "تكبيرات الأذان"),
                        isEnabled = obj.optBoolean("isEnabled", true)
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    private fun serializeCustomPrayerAlarms(list: List<com.example.core.prayer.CustomPrayerAlarm>): String {
        val arr = org.json.JSONArray()
        list.forEach { alarm ->
            val obj = org.json.JSONObject()
            obj.put("id", alarm.id)
            obj.put("name", alarm.name)
            val pArr = org.json.JSONArray()
            alarm.prayerIds.forEach { pArr.put(it) }
            obj.put("prayerIds", pArr)
            obj.put("isBeforeAzan", alarm.isBeforeAzan)
            obj.put("offsetMinutes", alarm.offsetMinutes)
            val dArr = org.json.JSONArray()
            alarm.daysOfWeek.forEach { dArr.put(it) }
            obj.put("daysOfWeek", dArr)
            obj.put("soundType", alarm.soundType)
            obj.put("isEnabled", alarm.isEnabled)
            arr.put(obj)
        }
        return arr.toString()
    }

    private data class Quint<A, B, C, D, E>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
        val fifth: E
    )

    private data class Sext<A, B, C, D, E, F>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
        val fifth: E,
        val sixth: F
    )

    private data class Oct<A, B, C, D, E, F, G, H>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
        val fifth: E,
        val sixth: F,
        val seventh: G,
        val eighth: H
    )
}
