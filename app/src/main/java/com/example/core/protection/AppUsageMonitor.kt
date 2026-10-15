package com.example.core.protection

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Process
import android.provider.Settings
import com.example.core.prayer.UmmAlQuraCalculator
import com.example.data.BlockScheduleEntity
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap

data class DayUsageItem(
    val dayNameArabic: String,
    val dateKey: String,
    val dayOfMonth: Int,
    val totalMinutes: Int,
    val formattedDuration: String,
    val isToday: Boolean
)

data class WeeklyUsageData(
    val weekOffset: Int = 0,
    val weekRangeLabel: String = "",
    val days: List<DayUsageItem> = emptyList(),
    val totalWeekMinutes: Int = 0,
    val averageDailyMinutes: Int = 0
)

data class AppUsageStatItem(
    val packageName: String,
    val appName: String,
    val usedMinutesToday: Int,
    val usedSecondsToday: Int = usedMinutesToday * 60,
    val usedTimeMillisToday: Long = usedSecondsToday * 1000L,
    val formattedDuration: String = AppUsageMonitor.formatDurationArabic(usedSecondsToday * 1000L),
    val launchCountToday: Int = 0,
    val isSystemApp: Boolean = false
) {
    fun formattedUsageArabic(): String = formattedDuration
}

data class DeviceUsageOverview(
    val hasUsagePermission: Boolean = false,
    val totalScreenMinutesToday: Int = 0,
    val totalScreenSecondsToday: Int = 0,
    val totalScreenTimeMillisToday: Long = totalScreenSecondsToday * 1000L,
    val usageByPackageMinutes: Map<String, Int> = emptyMap(),
    val usageByPackageSeconds: Map<String, Int> = emptyMap(),
    val usageByPackageMillis: Map<String, Long> = usageByPackageSeconds.mapValues { it.value * 1000L },
    val launchesByPackageToday: Map<String, Int> = emptyMap(),
    val topUsedApps: List<AppUsageStatItem> = emptyList(),
    val installedUserApps: List<AppUsageStatItem> = emptyList(),
    val negativeScreenMinutesToday: Int = 0,
    val positiveScreenMinutesToday: Int = 0
)

object AppUsageMonitor {

    fun formatDurationArabic(millis: Long): String {
        val totalSeconds = (millis / 1000L).toInt().coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val mins = (totalSeconds % 3600) / 60
        val secs = totalSeconds % 60
        return UmmAlQuraCalculator.toArabicDigits(
            when {
                hours > 0 && mins > 0 -> "$hours س و $mins د"
                hours > 0 -> "$hours ساعة"
                mins > 0 -> "$mins دقيقة"
                secs > 0 -> "$secs ثانية"
                else -> "0 دقيقة"
            }
        )
    }

    private var currentLivePackage: String? = null
    private var currentLiveStartMillis: Long = 0L
    private var liveDayKey: Int = -1
    private val liveAccumulatedMillis = ConcurrentHashMap<String, Long>()

    fun recordLiveForegroundSwitch(newPackage: String, selfPackage: String) {
        val now = System.currentTimeMillis()
        ensureSameDay(now)
        flushCurrentLivePackage(now)
        if (newPackage.isNotBlank() &&
            newPackage != selfPackage &&
            newPackage !in ProtectionManager.permanentExemptPackages &&
            !newPackage.startsWith("com.android.systemui") &&
            !newPackage.contains("launcher", ignoreCase = true)
        ) {
            currentLivePackage = newPackage
            currentLiveStartMillis = now
        } else {
            currentLivePackage = null
            currentLiveStartMillis = 0L
        }
    }

    private fun flushCurrentLivePackage(now: Long) {
        val pkg = currentLivePackage
        val start = currentLiveStartMillis
        if (pkg != null && start > 0L && now > start) {
            val delta = (now - start).coerceAtMost(30 * 60_000L)
            liveAccumulatedMillis[pkg] = (liveAccumulatedMillis[pkg] ?: 0L) + delta
            currentLiveStartMillis = now
        }
    }

    private fun ensureSameDay(now: Long) {
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val dayOfYear = cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
        if (liveDayKey != dayOfYear) {
            liveDayKey = dayOfYear
            liveAccumulatedMillis.clear()
            currentLivePackage = null
            currentLiveStartMillis = 0L
        }
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            )
            if (mode == AppOpsManager.MODE_DEFAULT) {
                context.checkCallingOrSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) ==
                    PackageManager.PERMISSION_GRANTED
            } else {
                mode == AppOpsManager.MODE_ALLOWED
            }
        } catch (_: Exception) {
            false
        }
    }

    fun openUsageAccessSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(fallback)
            } catch (_: Exception) {
            }
        }
    }

    fun readTodayUsageOverview(context: Context): DeviceUsageOverview {
        val now = System.currentTimeMillis()
        ensureSameDay(now)
        flushCurrentLivePackage(now)

        val pm = context.packageManager
        val hasPerm = hasUsageStatsPermission(context)

        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = cal.timeInMillis

        val usageSecondsMap = mutableMapOf<String, Int>()
        val launchesMap = mutableMapOf<String, Int>()

        // 1. دمج التتبع اللحظي المباشر
        for ((pkg, millis) in liveAccumulatedMillis) {
            val secs = (millis / 1000L).toInt()
            if (secs > 0) {
                usageSecondsMap[pkg] = secs
            }
        }

        // 2. قراءة UsageEvents الدقيقة + UsageStats اليومية من النظام
        if (hasPerm) {
            try {
                val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                if (usm != null) {
                    val eventMillisByPkg = mutableMapOf<String, Long>()
                    val activeResumeByPkg = mutableMapOf<String, Long>()
                    val usageEvents = usm.queryEvents(startOfDay, now)
                    val event = UsageEvents.Event()

                    while (usageEvents != null && usageEvents.hasNextEvent()) {
                        usageEvents.getNextEvent(event)
                        val pkg = event.packageName ?: continue
                        if (pkg == context.packageName ||
                            pkg in ProtectionManager.permanentExemptPackages ||
                            pkg.startsWith("com.android.systemui") ||
                            pkg.contains("launcher", ignoreCase = true)
                        ) {
                            continue
                        }
                        when (event.eventType) {
                            UsageEvents.Event.ACTIVITY_RESUMED -> {
                                activeResumeByPkg[pkg] = event.timeStamp
                                launchesMap[pkg] = (launchesMap[pkg] ?: 0) + 1
                            }
                            UsageEvents.Event.ACTIVITY_PAUSED,
                            UsageEvents.Event.ACTIVITY_STOPPED -> {
                                val startTs = activeResumeByPkg.remove(pkg)
                                if (startTs != null && event.timeStamp > startTs) {
                                    val delta = (event.timeStamp - startTs).coerceAtMost(6 * 3600_000L)
                                    eventMillisByPkg[pkg] = (eventMillisByPkg[pkg] ?: 0L) + delta
                                }
                            }
                        }
                    }

                    // إذا كان التطبيق ما يزال مفتوحاً في المقدمة الآن
                    for ((pkg, startTs) in activeResumeByPkg) {
                        if (now > startTs && (now - startTs) < 3 * 3600_000L) {
                            eventMillisByPkg[pkg] = (eventMillisByPkg[pkg] ?: 0L) + (now - startTs)
                        }
                    }

                    for ((pkg, millis) in eventMillisByPkg) {
                        val secs = (millis / 1000L).toInt()
                        if (secs > 0) {
                            usageSecondsMap[pkg] = maxOf(usageSecondsMap[pkg] ?: 0, secs)
                        }
                    }

                    // دمج queryUsageStats اليومية لضمان عدم ضياع أي بيانات سابقة في اليوم
                    val dailyStats = usm.queryUsageStats(
                        UsageStatsManager.INTERVAL_DAILY,
                        startOfDay,
                        now
                    ).orEmpty()
                    for (stat in dailyStats) {
                        val pkg = stat.packageName ?: continue
                        if (pkg == context.packageName ||
                            pkg in ProtectionManager.permanentExemptPackages ||
                            pkg.startsWith("com.android.systemui") ||
                            pkg.contains("launcher", ignoreCase = true)
                        ) {
                            continue
                        }
                        if (stat.lastTimeUsed >= startOfDay && stat.totalTimeInForeground > 0L) {
                            val secs = (stat.totalTimeInForeground / 1000L).toInt()
                            if (secs > 0) {
                                usageSecondsMap[pkg] = maxOf(usageSecondsMap[pkg] ?: 0, secs)
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            }
        }

        val usageMinutesMap = usageSecondsMap.mapValues { (_, secs) ->
            if (secs <= 0) 0 else ((secs + 59) / 60).coerceAtLeast(1)
        }

        // 3. جمع قائمة التطبيقات المثبتة مع أسمائها العربية/الرسمية
        val labelByPkg = mutableMapOf<String, Pair<String, Boolean>>()
        try {
            val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(launcherIntent, 0)
            for (ri in resolveInfos) {
                val ai = ri.activityInfo?.applicationInfo ?: continue
                val pkg = ai.packageName
                if (pkg == context.packageName || pkg in ProtectionManager.permanentExemptPackages) continue
                val label = try {
                    ri.loadLabel(pm).toString().ifBlank { pm.getApplicationLabel(ai).toString() }
                } catch (_: Exception) {
                    pkg.substringAfterLast('.')
                }
                val isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                    (ai.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
                labelByPkg[pkg] = label to isSystem
            }
        } catch (_: Exception) {
        }

        try {
            val allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (ai in allApps) {
                val pkg = ai.packageName
                if (pkg == context.packageName ||
                    pkg in ProtectionManager.permanentExemptPackages ||
                    pkg in labelByPkg
                ) {
                    continue
                }
                val hasLaunch = pm.getLaunchIntentForPackage(pkg) != null
                if (!hasLaunch && (usageSecondsMap[pkg] ?: 0) <= 0) continue
                val label = try {
                    pm.getApplicationLabel(ai).toString()
                } catch (_: Exception) {
                    pkg.substringAfterLast('.')
                }
                val isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                    (ai.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
                labelByPkg[pkg] = label to isSystem
            }
        } catch (_: Exception) {
        }

        // التأكد من وجود أي تطبيق له سجل استخدام اليوم أو من التطبيقات الشهيرة
        val popularDefaults = listOf(
            "com.zhiliaoapp.musically" to "تيك توك",
            "com.instagram.android" to "إنستغرام",
            "com.google.android.youtube" to "يوتيوب",
            "com.twitter.android" to "منصة إكس",
            "com.snapchat.android" to "سناب شات",
            "com.whatsapp" to "واتساب",
            "org.telegram.messenger" to "تيليجرام"
        )
        for ((pkg, arName) in popularDefaults) {
            if (pkg !in labelByPkg) {
                labelByPkg[pkg] = arName to false
            }
        }
        for (pkg in usageSecondsMap.keys) {
            if (pkg !in labelByPkg) {
                labelByPkg[pkg] = pkg.substringAfterLast('.') to false
            }
        }

        val installedList = labelByPkg.map { (pkg, pair) ->
            val secs = usageSecondsMap[pkg] ?: 0
            val mins = usageMinutesMap[pkg] ?: 0
            val launches = launchesMap[pkg] ?: 0
            AppUsageStatItem(
                packageName = pkg,
                appName = pair.first,
                usedMinutesToday = mins,
                usedSecondsToday = secs,
                launchCountToday = launches,
                isSystemApp = pair.second
            )
        }.sortedWith(
            compareByDescending<AppUsageStatItem> { it.usedSecondsToday }
                .thenByDescending { it.usedMinutesToday }
                .thenBy { it.isSystemApp }
                .thenBy { it.appName }
        )

        val totalSeconds = usageSecondsMap.values.sum()
        val totalMinutes = if (totalSeconds <= 0) 0 else ((totalSeconds + 59) / 60)
        val topUsed = installedList.filter { it.usedSecondsToday > 0 }.take(10)

        return DeviceUsageOverview(
            hasUsagePermission = hasPerm,
            totalScreenMinutesToday = totalMinutes,
            totalScreenSecondsToday = totalSeconds,
            usageByPackageMinutes = usageMinutesMap,
            usageByPackageSeconds = usageSecondsMap,
            launchesByPackageToday = launchesMap,
            topUsedApps = topUsed,
            installedUserApps = installedList
        )
    }

    /**
     * استعلام إحصائيات الاستخدام الأسبوعية (رسم بياني) سواء للهاتف كاملاً أو لتطبيق محدد
     * مع دعم التراجع في الأسابيع السابقة (weekOffset: 0 = هذا الأسبوع، -1 = الأسبوع الماضي، ...)
     */
    fun queryWeeklyUsage(
        context: Context,
        targetPackage: String? = null,
        weekOffset: Int = 0
    ): WeeklyUsageData {
        val now = System.currentTimeMillis()
        val hasPerm = hasUsageStatsPermission(context)
        val usm = if (hasPerm) context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager else null

        // بداية الأسبوع (السبت) حسب إزاحة الأسبوع
        val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+03:00")).apply {
            timeInMillis = now
            add(Calendar.WEEK_OF_YEAR, weekOffset)
            firstDayOfWeek = Calendar.SATURDAY
            set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val dayNames = listOf("السبت", "الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة")
        val daysList = mutableListOf<DayUsageItem>()

        for (i in 0..6) {
            val dayStart = cal.timeInMillis
            val dayEnd = dayStart + 24 * 3600_000L - 1000L
            val dayName = dayNames[i]
            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
            val dateKey = UmmAlQuraCalculator.todayDateKey(dayStart)
            val isToday = (dayStart <= now && now <= dayEnd)

            var dayMinutes = 0
            if (usm != null && dayStart <= now) {
                try {
                    val endQuery = minOf(now, dayEnd)
                    val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, dayStart, endQuery).orEmpty()
                    if (targetPackage != null) {
                        val stat = stats.find { it.packageName == targetPackage }
                        dayMinutes = ((stat?.totalTimeInForeground ?: 0L) / 60_000L).toInt()
                        if (isToday) {
                            val liveSecs = readTodayUsageOverview(context).usageByPackageSeconds[targetPackage] ?: 0
                            dayMinutes = maxOf(dayMinutes, (liveSecs + 59) / 60)
                        }
                    } else {
                        val validStats = stats.filter {
                            it.packageName != context.packageName &&
                                it.packageName !in ProtectionManager.permanentExemptPackages &&
                                !it.packageName.startsWith("com.android.systemui") &&
                                !it.packageName.contains("launcher", ignoreCase = true)
                        }
                        dayMinutes = (validStats.sumOf { it.totalTimeInForeground } / 60_000L).toInt()
                        if (isToday) {
                            dayMinutes = maxOf(dayMinutes, readTodayUsageOverview(context).totalScreenMinutesToday)
                        }
                    }
                } catch (_: Exception) {
                }
            }

            daysList.add(
                DayUsageItem(
                    dayNameArabic = dayName,
                    dateKey = dateKey,
                    dayOfMonth = dayOfMonth,
                    totalMinutes = dayMinutes,
                    formattedDuration = formatDurationArabic(dayMinutes * 60_000L),
                    isToday = isToday
                )
            )

            cal.add(Calendar.DAY_OF_MONTH, 1)
        }

        val firstDay = daysList.first()
        val lastDay = daysList.last()
        val rangeLabel = "${firstDay.dayOfMonth} إلى ${lastDay.dayOfMonth} ${
            when {
                weekOffset == 0 -> "(هذا الأسبوع)"
                weekOffset == -1 -> "(الأسبوع الماضي)"
                else -> "(قبل ${-weekOffset} أسابيع)"
            }
        }"

        val totalMins = daysList.sumOf { it.totalMinutes }
        val activeDaysCount = daysList.count { it.totalMinutes > 0 }.coerceAtLeast(1)
        val avgMins = totalMins / activeDaysCount

        return WeeklyUsageData(
            weekOffset = weekOffset,
            weekRangeLabel = rangeLabel,
            days = daysList,
            totalWeekMinutes = totalMins,
            averageDailyMinutes = avgMins
        )
    }

    /**
     * التحقق مما إذا كانت جدولة الحظر نشطة في الوقت الحالي
     */
    fun isScheduleCurrentlyInTimeWindow(
        schedule: BlockScheduleEntity,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (!schedule.isEnabled || schedule.scheduleType == "WAKE_ALARM") return false
        val startMins = parseTime24ToMinutes(schedule.startTime24) ?: return false
        val endMins = parseTime24ToMinutes(schedule.endTime24) ?: return false

        val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+03:00")).apply {
            timeInMillis = nowMillis
        }
        val currentMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

        return if (startMins <= endMins) {
            currentMins in startMins..endMins
        } else {
            currentMins >= startMins || currentMins <= endMins
        }
    }

    private fun parseTime24ToMinutes(time24: String): Int? {
        val parts = time24.trim().split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val m = parts.getOrNull(1)?.toIntOrNull() ?: return null
        return (h.coerceIn(0, 23) * 60) + m.coerceIn(0, 59)
    }
}
