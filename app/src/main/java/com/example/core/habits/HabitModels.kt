package com.example.core.habits

import com.example.data.CategoryEntity
import com.example.data.HabitEntity
import com.example.data.HabitLogEntity
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * بنود قائمة التحقق داخل العادة الواحدة
 */
data class HabitChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: String = "CHECKBOX", // CHECKBOX, VALUE, TIMER
    val targetValue: Int = 1,
    val unitLabel: String = ""
)

/**
 * حالة تقدم بند من بنود قائمة التحقق ليوم معين
 */
data class HabitChecklistProgressItem(
    val itemId: String,
    val isChecked: Boolean = false,
    val currentValue: Int = 0,
    val isCompleted: Boolean = false
)

enum class HabitGoalComparison(val code: String, val title: String, val symbol: String) {
    GTE("GTE", "مثل أو أكثر من الرقم", "≥"),
    EQ("EQ", "مثله تماماً", "="),
    LTE("LTE", "أقل منه أو يساويه (حد أقصى)", "≤");

    companion object {
        fun fromCode(code: String): HabitGoalComparison {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: GTE
        }
    }
}

enum class HabitFrequencyType(val code: String, val title: String) {
    DAILY("DAILY", "كل يوم"),
    SPECIFIC_DAYS_WEEK("SPECIFIC_DAYS_WEEK", "أيام محددة بالأسبوع"),
    SPECIFIC_DAYS_MONTH("SPECIFIC_DAYS_MONTH", "أيام محددة بالشهر"),
    SPECIFIC_DAYS_YEAR("SPECIFIC_DAYS_YEAR", "أيام محددة بالسنة"),
    TIMES_PER_PERIOD("TIMES_PER_PERIOD", "عدد مرات محددة بفترة"),
    INTERVAL_EVERY_X("INTERVAL_EVERY_X", "تكرار كل X (أيام/أسابيع/شهور)"),
    WORKOUT_CYCLE("WORKOUT_CYCLE", "أيام تمرين وأيام راحة");

    companion object {
        fun fromCode(code: String): HabitFrequencyType {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: DAILY
        }
    }
}

enum class HabitDayStatus(val code: String, val title: String) {
    COMPLETED("COMPLETED", "مكتمل"),
    PARTIAL("PARTIAL", "إنجاز جزئي"),
    FAILED("FAILED", "لم يكتمل / فائت"),
    REST_SKIP("REST_SKIP", "راحة / تخطي مسموح"),
    UNRECORDED("UNRECORDED", "غير مسجل");
}

object HabitHelper {

    val DEFAULT_CATEGORIES: List<CategoryEntity> = emptyList()

    fun serializeChecklistItems(items: List<HabitChecklistItem>): String {
        val array = JSONArray()
        items.forEach { item ->
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("type", item.type)
            obj.put("targetValue", item.targetValue)
            obj.put("unitLabel", item.unitLabel)
            array.put(obj)
        }
        return array.toString()
    }

    fun parseChecklistItems(json: String?): List<HabitChecklistItem> {
        if (json.isNullOrBlank() || json.trim() == "[]") return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<HabitChecklistItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    HabitChecklistItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        title = obj.optString("title", ""),
                        type = obj.optString("type", "CHECKBOX"),
                        targetValue = obj.optInt("targetValue", 1).coerceAtLeast(1),
                        unitLabel = obj.optString("unitLabel", "")
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun serializeChecklistProgress(map: Map<String, HabitChecklistProgressItem>): String {
        val obj = JSONObject()
        map.forEach { (id, progress) ->
            val pObj = JSONObject()
            pObj.put("itemId", progress.itemId)
            pObj.put("isChecked", progress.isChecked)
            pObj.put("currentValue", progress.currentValue)
            pObj.put("isCompleted", progress.isCompleted)
            obj.put(id, pObj)
        }
        return obj.toString()
    }

    fun parseChecklistProgress(json: String?): Map<String, HabitChecklistProgressItem> {
        if (json.isNullOrBlank() || json.trim() == "{}") return emptyMap()
        return try {
            val obj = JSONObject(json)
            val map = mutableMapOf<String, HabitChecklistProgressItem>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val pObj = obj.getJSONObject(key)
                map[key] = HabitChecklistProgressItem(
                    itemId = pObj.optString("itemId", key),
                    isChecked = pObj.optBoolean("isChecked", false),
                    currentValue = pObj.optInt("currentValue", 0),
                    isCompleted = pObj.optBoolean("isCompleted", false)
                )
            }
            map
        } catch (_: Exception) {
            emptyMap()
        }
    }

    /**
     * فحص هل تحقق الهدف بناءً على المقارنة المطلوبة
     */
    fun isGoalAchieved(operator: String, current: Int, target: Int): Boolean {
        return when (operator) {
            "LTE" -> current <= target
            "EQ" -> current == target
            else -> current >= target
        }
    }

    /**
     * حساب نسبة الإنجاز 0..100
     */
    fun calculatePercent(operator: String, current: Int, target: Int): Int {
        if (target <= 0) return 0
        return when (operator) {
            "LTE" -> {
                if (current <= target) 100
                else {
                    val excess = current - target
                    (100 - (excess * 20)).coerceAtLeast(0)
                }
            }
            "EQ" -> {
                if (current == target) 100
                else ((current.toFloat() / target.toFloat()) * 100).toInt().coerceIn(0, 99)
            }
            else -> {
                ((current.toFloat() / target.toFloat()) * 100).toInt().coerceIn(0, 100)
            }
        }
    }

    /**
     * هل العادة مجدولة ومطلوبة في هذا التاريخ؟
     */
    fun isHabitScheduledForDate(habit: HabitEntity, dateKey: String): Boolean {
        val targetDate = try {
            LocalDate.parse(dateKey)
        } catch (_: Exception) {
            return true
        }

        // تاريخ البدء
        if (habit.startDateKey.isNotBlank()) {
            try {
                val start = LocalDate.parse(habit.startDateKey)
                if (targetDate.isBefore(start)) return false
            } catch (_: Exception) {}
        }

        // تاريخ الانتهاء
        if (habit.endDateKey.isNotBlank()) {
            try {
                val end = LocalDate.parse(habit.endDateKey)
                if (targetDate.isAfter(end)) return false
            } catch (_: Exception) {}
        }

        when (habit.frequencyType) {
            "DAILY" -> return true
            "SPECIFIC_DAYS_WEEK" -> {
                // 1=Monday... 7=Sunday in java.time DayOfWeek.
                // في التطبيق، نعتمد DayOfWeek: 1..7
                val dayOfWeek = targetDate.dayOfWeek.value // 1..7 (1=Monday, 6=Saturday, 7=Sunday)
                val csv = habit.frequencyDaysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
                return csv.isEmpty() || csv.contains(dayOfWeek)
            }
            "SPECIFIC_DAYS_MONTH" -> {
                val dayOfMonth = targetDate.dayOfMonth
                val csv = habit.frequencyDaysOfMonth.split(",").mapNotNull { it.trim().toIntOrNull() }
                return csv.isEmpty() || csv.contains(dayOfMonth)
            }
            "SPECIFIC_DAYS_YEAR" -> {
                val monthDay = String.format("%02d-%02d", targetDate.monthValue, targetDate.dayOfMonth)
                val csv = habit.frequencyDaysOfYear.split(",").map { it.trim() }
                return csv.isEmpty() || csv.contains(monthDay)
            }
            "INTERVAL_EVERY_X" -> {
                val interval = habit.frequencyIntervalValue.coerceAtLeast(1)
                val baseDate = try {
                    if (habit.startDateKey.isNotBlank()) LocalDate.parse(habit.startDateKey) else LocalDate.of(2026, 1, 1)
                } catch (_: Exception) {
                    LocalDate.of(2026, 1, 1)
                }
                when (habit.frequencyIntervalUnit) {
                    "WEEKS" -> {
                        val weeksDiff = ChronoUnit.WEEKS.between(baseDate, targetDate)
                        return weeksDiff % interval == 0L
                    }
                    "MONTHS" -> {
                        val monthsDiff = ChronoUnit.MONTHS.between(baseDate, targetDate)
                        return monthsDiff % interval == 0L
                    }
                    else -> {
                        val daysDiff = ChronoUnit.DAYS.between(baseDate, targetDate)
                        return daysDiff % interval == 0L
                    }
                }
            }
            "WORKOUT_CYCLE" -> {
                val workDays = habit.cycleWorkDays.coerceAtLeast(1)
                val restDays = habit.cycleRestDays.coerceAtLeast(1)
                val totalCycle = workDays + restDays
                val baseDate = try {
                    if (habit.cycleStartDateKey.isNotBlank()) LocalDate.parse(habit.cycleStartDateKey)
                    else if (habit.startDateKey.isNotBlank()) LocalDate.parse(habit.startDateKey)
                    else LocalDate.of(2026, 1, 1)
                } catch (_: Exception) {
                    LocalDate.of(2026, 1, 1)
                }
                val daysDiff = ChronoUnit.DAYS.between(baseDate, targetDate)
                if (daysDiff < 0) return true
                val dayInCycle = (daysDiff % totalCycle).toInt()
                // أول workDays أيام عمل وتمرين، الباقي أيام راحة
                return dayInCycle < workDays
            }
            else -> return true
        }
    }

    /**
     * استخراج حالة يوم معين في التقويم
     */
    fun resolveDayStatus(
        habit: HabitEntity,
        log: HabitLogEntity?,
        dateKey: String,
        todayKey: String
    ): HabitDayStatus {
        if (log != null && log.status == "REST_SKIP") {
            return HabitDayStatus.REST_SKIP
        }

        if (log != null && log.isCompleted) {
            return HabitDayStatus.COMPLETED
        }

        val isScheduled = isHabitScheduledForDate(habit, dateKey)

        if (log != null && log.completedCount > 0) {
            val percent = if (log.completionPercent > 0) log.completionPercent
            else calculatePercent(habit.comparisonOperator, log.completedCount, habit.targetCount)
            return if (percent >= 100) HabitDayStatus.COMPLETED else HabitDayStatus.PARTIAL
        }

        if (!isScheduled) {
            return HabitDayStatus.UNRECORDED
        }

        // إذا كان التاريخ يسبق تاريخ بدء العادة أو تاريخ إنشائها، لا يعتبر فاشلاً بل غير مسجل
        if (habit.startDateKey.isNotBlank()) {
            try {
                val start = LocalDate.parse(habit.startDateKey)
                if (LocalDate.parse(dateKey).isBefore(start)) return HabitDayStatus.UNRECORDED
            } catch (_: Exception) {}
        }
        if (habit.createdAt > 0) {
            try {
                val created = java.time.Instant.ofEpochMilli(habit.createdAt)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
                if (LocalDate.parse(dateKey).isBefore(created)) return HabitDayStatus.UNRECORDED
            } catch (_: Exception) {}
        }

        // إذا كان التاريخ في الماضي ولم يُنجز
        val isPast = try {
            LocalDate.parse(dateKey).isBefore(LocalDate.parse(todayKey))
        } catch (_: Exception) {
            false
        }

        return if (isPast) HabitDayStatus.FAILED else HabitDayStatus.UNRECORDED
    }
}
