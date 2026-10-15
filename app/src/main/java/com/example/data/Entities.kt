package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorHex: String = "#10B981",
    val iconKey: String = "TAG", // FITNESS, BOOK, DEEN, WORK, STAR, CODE, HEART, ART, LEAF
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val categoryId: String = "", // الفئة المرتبطة (يونيفرسل)
    val goalMode: String = "ACHIEVE",
    val comparisonOperator: String = "GTE", // GTE (>= مثل أو أكثر), EQ (== مثله تماماً), LTE (<= أقل منه / حد أقصى)
    val frequencyType: String = "DAILY", // DAILY, SPECIFIC_DAYS_WEEK, SPECIFIC_DAYS_MONTH, SPECIFIC_DAYS_YEAR, TIMES_PER_PERIOD, INTERVAL_EVERY_X, WORKOUT_CYCLE
    val frequencyDaysOfWeek: String = "1,2,3,4,5,6,7", // 1=Sat, 2=Sun, etc.
    val frequencyDaysOfMonth: String = "", // "1,15,30"
    val frequencyDaysOfYear: String = "", // "01-01,12-25"
    val frequencyPeriodType: String = "WEEK", // WEEK, MONTH, YEAR
    val frequencyPeriodTimes: Int = 3, // عدد X من المرات في الأسبوع/الشهر/السنة
    val frequencyIntervalUnit: String = "DAYS", // DAYS, WEEKS, MONTHS
    val frequencyIntervalValue: Int = 1, // كل X أيام/أسابيع/شهور
    val cycleWorkDays: Int = 3, // أيام التمرين/العمل
    val cycleRestDays: Int = 1, // أيام الراحة
    val cycleStartDateKey: String = "",
    val allowedSkipDaysPerWeek: Int = 1, // كم يوم راحة (تخطي أزرق) مسموح
    val startDateKey: String = "", // تاريخ البدء YYYY-MM-DD
    val endDateKey: String = "", // تاريخ الانتهاء YYYY-MM-DD
    val targetCount: Int = 1,
    val unitLabel: String = "مرة",
    /**
     * نوع العادة:
     * CHECKBOX (تأشير يومي صح / خطأ)
     * TARGET_VALUE (تحقيق قيمة رقمية)
     * CHECKLIST (قائمة تحقق من عدة مهام داخل العادة)
     * STUDY_TIMER (مؤقت مذاكرة وتركيز: بومودورو مخصص أو مؤقت تصاعدي)
     * HEALTH_STEPS, HEALTH_SLEEP, HEALTH_ACTIVE_CALORIES, HEALTH_TOTAL_CALORIES,
     * HEALTH_DISTANCE, HEALTH_EXERCISE, HEALTH_WATER, HEALTH_HEART_RATE
     */
    val autoSource: String = "CHECKBOX",
    val checklistItemsJson: String = "[]", // قائمة مهام التحقق: JSON
    val checklistRequiredCount: Int = 1, // كم عنصر مطلوب تنفيذه لاعتبار العادة مكتملة
    val timerType: String = "COUNT_UP", // COUNT_UP أو POMODORO
    val pomodoroFocusMin: Int = 25,
    val pomodoroBreakMin: Int = 5,
    val pomodoroRounds: Int = 2,
    val linkedSubjectId: String = "",
    // نظام قفل العقاب المتقدم: ربط بجدولة حظر أو اختيار تطبيقات محددة يتوقف حظرها فور إنهاء العادة
    val penaltyLockApps: Boolean = false,
    val penaltyScheduleId: String = "",
    val penaltyBlockedPackages: String = "",
    // نظام المنبه: عادي (NORMAL) أو المنبه الذكي الخارق (SUPER_WAKE)
    val alarmTime24: String = "",
    val alarmEnabled: Boolean = false,
    val alarmType: String = "NORMAL", // NORMAL أو SUPER_WAKE
    val alarmVerifyMethod: String = "LOCAL_IMAGE", // LOCAL_IMAGE أو QR
    val alarmPlaceName: String = "المكان المحدد",
    val alarmQrCode: String = "WAKE-QR-2026",
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val suggestedTargetCount: Int? = null,
    val level: Int = 1,
    val xp: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

@Entity(
    tableName = "habit_logs",
    indices = [Index(value = ["habitId", "dateKey"])]
)
data class HabitLogEntity(
    @PrimaryKey val id: String, // "${habitId}_${dateKey}"
    val habitId: String,
    val dateKey: String, // YYYY-MM-DD
    val completedCount: Int = 0,
    val targetCount: Int = 1,
    val isCompleted: Boolean = false,
    val status: String = "NONE", // "COMPLETED", "PARTIAL", "FAILED", "REST_SKIP", "NONE"
    val completionPercent: Int = 0, // 0..100
    val checklistProgressJson: String = "{}", // تقدم بنود قائمة التحقق
    val sourceNote: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

@Entity(tableName = "mosque_locations")
data class MosqueLocationEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int = 100,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

@Entity(
    tableName = "tasks",
    indices = [Index(value = ["parentTaskId"])]
)
data class TaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val notes: String = "",
    val listCategory: String = "اليوم",
    val categoryId: String = "", // الفئة المرتبطة (يونيفرسل)
    val priority: String = "MEDIUM", // HIGH, MEDIUM, LOW
    val dueDateText: String = "اليوم",
    val reminderTimeText: String = "بدون تذكير",
    val alarmTime24: String = "",
    val alarmEnabled: Boolean = false,
    val alarmType: String = "NORMAL", // NORMAL أو SUPER_WAKE
    val alarmVerifyMethod: String = "LOCAL_IMAGE",
    val alarmPlaceName: String = "المكان المحدد",
    val alarmQrCode: String = "WAKE-QR-2026",
    val recurrence: String = "NONE", // NONE, DAILY, WEEKLY
    val parentTaskId: String? = null,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

@Entity(
    tableName = "prayer_logs",
    indices = [Index(value = ["dateKey"])]
)
data class PrayerLogEntity(
    @PrimaryKey val id: String, // "${dateKey}_${prayerName}"
    val dateKey: String,
    val prayerName: String, // FAJR, DHUHR, ASR, MAGHRIB, ISHA
    val status: String = "PENDING", // ON_TIME, LATE, NOT_PRAYED, PENDING
    val mosqueName: String = "",
    val distanceMeters: Int = 0,
    val verifiedByLocation: Boolean = false,
    val loggedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorHex: String = "#145946",
    val dailyGoalMinutes: Int = 60,
    val weeklyGoalMinutes: Int = 300,
    val defaultTimerType: String = "COUNT_UP", // COUNT_UP أو POMODORO
    val pomodoroFocusMin: Int = 25,
    val pomodoroBreakMin: Int = 5,
    val pomodoroRounds: Int = 2,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

@Entity(
    tableName = "study_sessions",
    indices = [Index(value = ["subjectId"]), Index(value = ["dateKey"])]
)
data class StudySessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val subjectId: String,
    val subjectName: String,
    val subjectColorHex: String,
    val mode: String = "COUNT_UP",
    val durationMinutes: Int,
    val durationSeconds: Int,
    val dateKey: String,
    val startedAt: Long,
    val endedAt: Long = System.currentTimeMillis(),
    val pausedAt: Long? = null,
    val completedAt: Long? = null,
    val isCompleted: Boolean = true,
    val linkedType: String = "NONE", // HABIT, TASK, NONE
    val linkedId: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

@Entity(
    tableName = "blocked_apps",
    indices = [Index(value = ["packageName"])]
)
data class BlockedAppEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val packageName: String,
    val appName: String,
    val category: String = "OTHER", // "SOCIAL", "ENTERTAINMENT", "PRODUCTIVITY", "TOOLS", "OTHER"
    val blockInStudyMode: Boolean = true,
    val blockInSleepMode: Boolean = true,
    val isAlwaysBlocked: Boolean = false,
    val dailyLimitMinutes: Int = 0, // 0 = بدون حد زمني (حظر حسب الجدول)، >0 = حد استخدام يومي بالدقائق مثل StayFree
    val dailyLaunchLimit: Int = 0, // 0 = بدون حد لمرات الفتح، >0 = أقصى عدد لمرات فتح التطبيق اليوم
    val usageImpact: String = "NEUTRAL", // "POSITIVE", "NEGATIVE", "NEUTRAL", "IGNORED" (عدم شمل التطبيق في مدة الاستخدام)
    val specificBlockStartTime24: String = "", // جدول حظر خاص بالتطبيق
    val specificBlockEndTime24: String = "",
    val specificBlockDays: String = "1,2,3,4,5,6,7",
    val specificBlockEnabled: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

@Entity(tableName = "block_schedules")
data class BlockScheduleEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val scheduleType: String, // "WAKE_ALARM", "SLEEP_MODE", "STUDY_BLOCK", "CUSTOM_BLOCK", "GROUP_USAGE_LIMIT", "PERMANENT", "SESSION_LIMIT", "FOCUS_TIMER"
    val startTime24: String = "04:30",
    val endTime24: String = "05:00",
    val blockedPackagesCsv: String = "", // حزم التطبيقات المشمولة في هذه الجدولة
    val targetCategory: String = "", // في حال تم اختيار فئة كاملة (مثل SOCIAL أو ENTERTAINMENT)
    val groupLimitMinutes: Int = 0, // حد الاستخدام الجماعي المشترك لنوع GROUP_USAGE_LIMIT
    val sessionDurationMinutes: Int = 15, // مدة جلسة الاستخدام بالدقائق لنوع SESSION_LIMIT
    val sessionCooldownMinutes: Int = 30, // فترة الإغلاق بعد انتهاء الجلسة لنوع SESSION_LIMIT
    val verificationMethod: String = "LOCAL_IMAGE", // "LOCAL_IMAGE" أو "QR"
    val qrReferenceCode: String = "WAKE-QR-2026",
    val placeName: String = "المكان المحدد",
    val emergencyExitType: String = "MATH_3",
    val isEnabled: Boolean = true,
    val strictAppProtection: Boolean = true,
    val daysOfWeek: String = "1,2,3,4,5,6,7", // الأيام: 1=السبت، 2=الأحد، 3=الاثنين، 4=الثلاثاء، 5=الأربعاء، 6=الخميس، 7=الجمعة
    val ringtoneUri: String = "",
    val ringtoneTitle: String = "نغمة المنبه الافتراضية",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

@Entity(tableName = "lectures")
data class LectureEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val courseCode: String = "",
    val location: String = "",
    val dayOfWeek: Int = 1,
    val startTime24: String = "08:00",
    val endTime24: String = "09:30",
    val reminderMinutesBefore: Int = 15,
    val isImportedFromIcs: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

@Entity(tableName = "wird_progress")
data class WirdProgressEntity(
    @PrimaryKey val id: String = "khatmah_main",
    val currentPage: Int = 1,
    val totalPages: Int = 604,
    val dailyTargetPages: Int = 2,
    val currentJuz: Int = 1,
    val bookmarkSurahName: String = "سورة الفاتحة",
    val bookmarkAyahNumber: Int = 1,
    val bookmarkNote: String = "",
    val completedKhatmahCount: Int = 0,
    val lastReadDateKey: String = "",
    val todayPagesRead: Int = 0,
    val planType: String = "PAGES", // "PAGES" or "JUZ"
    val khatmahTitle: String = "الختمة الحالية",
    val khatmahStartDate: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

@Entity(tableName = "quran_bookmarks")
data class QuranBookmarkEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val pageNumber: Int,
    val surahName: String,
    val ayahNumber: Int = 1,
    val title: String = "علامة توقف",
    val note: String = "",
    val colorHex: String = "#059669",
    val tag: String = "ورد اليوم",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "azkar_logs",
    indices = [Index(value = ["dateKey", "categoryCode"])]
)
data class AzkarLogEntity(
    @PrimaryKey val id: String,
    val dateKey: String,
    val categoryCode: String, // "MORNING", "EVENING", "AFTER_PRAYER", "SLEEP"
    val categoryTitle: String,
    val currentCount: Int = 0,
    val targetCount: Int = 33,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

@Entity(tableName = "health_snapshots")
data class HealthSnapshotEntity(
    @PrimaryKey val id: String,
    val dateKey: String,
    val stepsCount: Int = 0,
    val stepsGoal: Int = 8000,
    val activeCalories: Int = 0,
    val burnedCalories: Int = 0,
    val consumedCalories: Int = 0,
    val dailyCalorieLimit: Int = 2100,
    val sleepHours: Float = 0f,
    val distanceMeters: Int = 0,
    val exerciseMinutes: Int = 0,
    val waterMl: Int = 0,
    val avgHeartRateBpm: Int = 0,
    val mealsLogJson: String = "",
    val syncSource: String = "Health Connect",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

@Entity(tableName = "daily_summaries")
data class DailySummaryEntity(
    @PrimaryKey val id: String,
    val dateKey: String,
    val overallScore: Int = 0,
    val coachHeadline: String = "",
    val strengthsText: String = "",
    val improvementsText: String = "",
    val suggestedTomorrowSchedule: String = "",
    val engineSource: String = "محلي",
    val isTomorrowTasksApplied: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

@Entity(tableName = "weekly_reviews")
data class WeeklyReviewEntity(
    @PrimaryKey val id: String = "weekly_current",
    val weekNumber: Int = 1,
    val topGoal1: String = "",
    val topGoal2: String = "",
    val topGoal3: String = "",
    val reflectionNote: String = "",
    val weeklyScore: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

@Entity(tableName = "year_week_goals")
data class YearWeekGoalEntity(
    @PrimaryKey val weekNumber: Int,
    val focusTitle: String,
    val status: String = "UPCOMING",
    val completionPercent: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

@Entity(tableName = "settings")
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_events")
data class AppEventEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val location: String = "",
    val dateKey: String, // YYYY-MM-DD
    val startTime: String = "", // HH:mm
    val endTime: String = "", // HH:mm
    val colorHex: String = "#059669",
    val isAllDay: Boolean = false,
    val category: String = "عام",
    val reminderMinutesBefore: Int = 15,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)

