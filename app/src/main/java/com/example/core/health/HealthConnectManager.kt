package com.example.core.health

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

enum class HealthGoalMetric(
    val sourceCode: String,
    val arabicTitle: String,
    val unitLabel: String,
    val defaultTarget: Int,
    val description: String
) {
    STEPS(
        sourceCode = "HEALTH_STEPS",
        arabicTitle = "عدد الخطوات (Health Connect)",
        unitLabel = "خطوة",
        defaultTarget = 8000,
        description = "قراءة تلقائية صارمة لعدد خطوات اليوم من Health Connect وحساس المشي"
    ),
    SLEEP(
        sourceCode = "HEALTH_SLEEP",
        arabicTitle = "ساعات النوم (Health Connect)",
        unitLabel = "ساعة",
        defaultTarget = 7,
        description = "قراءة تلقائية لمدة النوم المسجلة في Health Connect / ساعة سامسونج"
    ),
    ACTIVE_CALORIES(
        sourceCode = "HEALTH_ACTIVE_CALORIES",
        arabicTitle = "السعرات النشطة المحروقة (Health Connect)",
        unitLabel = "سعرة",
        defaultTarget = 450,
        description = "السعرات المحروقة من الحركة والنشاط البدني عبر Health Connect"
    ),
    TOTAL_CALORIES(
        sourceCode = "HEALTH_TOTAL_CALORIES",
        arabicTitle = "إجمالي السعرات المحروقة (Health Connect)",
        unitLabel = "سعرة",
        defaultTarget = 2100,
        description = "مجموع السعرات اليومية المحروقة من Health Connect"
    ),
    DISTANCE(
        sourceCode = "HEALTH_DISTANCE",
        arabicTitle = "المسافة المقطوعة بالمتر (Health Connect)",
        unitLabel = "متر",
        defaultTarget = 5000,
        description = "إجمالي مسافة المشي والجري بالمتر من Health Connect"
    ),
    EXERCISE(
        sourceCode = "HEALTH_EXERCISE",
        arabicTitle = "دقائق التمارين الرياضية (Health Connect)",
        unitLabel = "دقيقة",
        defaultTarget = 30,
        description = "مدة جلسات التمارين الرياضية المسجلة في Health Connect"
    ),
    WATER(
        sourceCode = "HEALTH_WATER",
        arabicTitle = "شرب الماء بالملي لتر (Health Connect)",
        unitLabel = "مل",
        defaultTarget = 2500,
        description = "كمية السوائل والماء المسجلة في Health Connect"
    ),
    HEART_RATE(
        sourceCode = "HEALTH_HEART_RATE",
        arabicTitle = "متوسط نبضات القلب (Health Connect)",
        unitLabel = "نبضة/د",
        defaultTarget = 70,
        description = "قراءة متوسط نبض القلب اليومي من Health Connect"
    );

    companion object {
        fun fromSourceCode(code: String): HealthGoalMetric? =
            entries.find { it.sourceCode == code }

        fun isHealthSource(code: String): Boolean =
            code == "STEPS" || entries.any { it.sourceCode == code }
    }
}

data class HealthConnectDailyData(
    val isSdkAvailable: Boolean = false,
    val hasAllPermissions: Boolean = false,
    val grantedPermissionsCount: Int = 0,
    val steps: Int = 0,
    val sleepHours: Float = 0f,
    val activeCalories: Int = 0,
    val totalCalories: Int = 0,
    val distanceMeters: Int = 0,
    val exerciseMinutes: Int = 0,
    val waterMl: Int = 0,
    val avgHeartRateBpm: Int = 0,
    val statusMessage: String = ""
)

/**
 * مدير الربط الرسمي مع Jetpack Health Connect SDK بدون أي إدخال يدوي
 */
object HealthConnectManager {

    val requiredPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(HydrationRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class)
    )

    fun getSdkStatus(context: Context): Int {
        return try {
            HealthConnectClient.getSdkStatus(context)
        } catch (_: Exception) {
            HealthConnectClient.SDK_UNAVAILABLE
        }
    }

    fun isAvailable(context: Context): Boolean {
        return getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    suspend fun hasGrantedPermissions(context: Context): Boolean {
        if (!isAvailable(context)) return false
        return try {
            val client = HealthConnectClient.getOrCreate(context)
            val granted = client.permissionController.getGrantedPermissions()
            granted.isNotEmpty()
        } catch (_: Exception) {
            false
        }
    }

    suspend fun readTodayData(
        context: Context,
        hardwareSensorStepsFallback: Int = 0
    ): HealthConnectDailyData {
        val sdkStatus = getSdkStatus(context)
        if (sdkStatus != HealthConnectClient.SDK_AVAILABLE) {
            val fallbackBurned = StepSensorManager.calculateBurnedCaloriesFromSteps(hardwareSensorStepsFallback)
            return HealthConnectDailyData(
                isSdkAvailable = false,
                hasAllPermissions = false,
                steps = hardwareSensorStepsFallback,
                activeCalories = fallbackBurned,
                totalCalories = fallbackBurned,
                statusMessage = if (sdkStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
                    "يتطلب تحديث أو تثبيت تطبيق Health Connect على الجهاز"
                } else {
                    "متصل بحساس الحركة الداخلي للجهاز (Health Connect غير مثبت في هذه البيئة)"
                }
            )
        }

        return try {
            val client = HealthConnectClient.getOrCreate(context)
            val granted = client.permissionController.getGrantedPermissions()
            if (granted.isEmpty()) {
                return HealthConnectDailyData(
                    isSdkAvailable = true,
                    hasAllPermissions = false,
                    grantedPermissionsCount = 0,
                    steps = hardwareSensorStepsFallback,
                    statusMessage = "Health Connect متوفر — اضغط (منح صلاحيات Health Connect) لقراءة بياناتك تلقائياً"
                )
            }

            val zoneId = ZoneId.systemDefault()
            val startOfDay = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant()
            val now = Instant.now()
            // للنوم نقرأ آخر 24 ساعة حتى تشمل نومة الليل إلى الصباح
            val sleepStart = now.minusSeconds(24 * 3600)

            val aggResponse = client.aggregate(
                AggregateRequest(
                    metrics = setOf(
                        StepsRecord.COUNT_TOTAL,
                        ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
                        TotalCaloriesBurnedRecord.ENERGY_TOTAL,
                        DistanceRecord.DISTANCE_TOTAL,
                        ExerciseSessionRecord.EXERCISE_DURATION_TOTAL,
                        HydrationRecord.VOLUME_TOTAL,
                        HeartRateRecord.BPM_AVG
                    ),
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
                )
            )

            val sleepAgg = client.aggregate(
                AggregateRequest(
                    metrics = setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(sleepStart, now)
                )
            )

            val hcSteps = (aggResponse[StepsRecord.COUNT_TOTAL] ?: 0L).toInt()
            val finalSteps = maxOf(hcSteps, hardwareSensorStepsFallback)

            val activeKcal = aggResponse[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]
                ?.inKilocalories?.roundToInt()
                ?: StepSensorManager.calculateBurnedCaloriesFromSteps(finalSteps)

            val totalKcal = aggResponse[TotalCaloriesBurnedRecord.ENERGY_TOTAL]
                ?.inKilocalories?.roundToInt()
                ?: activeKcal

            val distanceMeters = aggResponse[DistanceRecord.DISTANCE_TOTAL]
                ?.inMeters?.roundToInt()
                ?: ((finalSteps * 0.72).roundToInt())

            val exerciseMins = (aggResponse[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]
                ?.toMinutes() ?: 0L).toInt()

            val waterMl = aggResponse[HydrationRecord.VOLUME_TOTAL]
                ?.inMilliliters?.roundToInt() ?: 0

            val avgBpm = (aggResponse[HeartRateRecord.BPM_AVG] ?: 0L).toInt()

            val sleepDurationMillis = sleepAgg[SleepSessionRecord.SLEEP_DURATION_TOTAL]?.toMillis() ?: 0L
            val sleepHours = (sleepDurationMillis.toFloat() / (1000f * 60f * 60f))

            HealthConnectDailyData(
                isSdkAvailable = true,
                hasAllPermissions = granted.containsAll(requiredPermissions),
                grantedPermissionsCount = granted.size,
                steps = finalSteps,
                sleepHours = sleepHours,
                activeCalories = activeKcal,
                totalCalories = totalKcal,
                distanceMeters = distanceMeters,
                exerciseMinutes = exerciseMins,
                waterMl = waterMl,
                avgHeartRateBpm = avgBpm,
                statusMessage = "✅ متصل بـ Health Connect تلقائياً (${granted.size} صلاحيات مفعلة)"
            )
        } catch (e: Exception) {
            HealthConnectDailyData(
                isSdkAvailable = true,
                hasAllPermissions = false,
                steps = hardwareSensorStepsFallback,
                statusMessage = "تعذر قراءة بعض السجلات: تأكد من منح صلاحيات القراءة في Health Connect"
            )
        }
    }

    fun openHealthConnectSettings(context: Context) {
        try {
            val intent = Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val marketIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=com.google.android.apps.healthdata")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(marketIntent)
            } catch (_: Exception) {
            }
        }
    }
}
