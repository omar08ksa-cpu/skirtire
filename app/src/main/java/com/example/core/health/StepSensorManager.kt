package com.example.core.health

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import com.example.core.prayer.UmmAlQuraCalculator
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

data class HealthConnectDailyMetrics(
    val stepsCount: Int = 0,
    val burnedCalories: Int = 0,
    val sleepMinutes: Int = 0,
    val sleepHours: Float = 0f,
    val distanceMeters: Int = 0,
    val activeMinutes: Int = 0,
    val waterMl: Int = 0,
    val avgHeartRateBpm: Int = 0,
    val grantedPermissionsCount: Int = 0,
    val isConnectedToHealthConnect: Boolean = false,
    val syncSourceLabel: String = "Health Connect"
)

/**
 * مدير الربط الحقيقي الكامل مع Health Connect وحساس الخطوات بدون أي إدخال يدوي
 * يقرأ: الخطوات، السعرات المحروقة، النوم، المسافة، التمارين (دقائق النشاط)، شرب الماء، ومعدل النبض
 */
object StepSensorManager {

    private const val PREFS_NAME = "daily_manager_steps_prefs"
    const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"
    const val SAMSUNG_HEALTH_PACKAGE = "com.sec.android.app.shealth"

    val requiredHealthPermissions: Set<String> by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setOf(
                HealthPermission.getReadPermission(StepsRecord::class),
                HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
                HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
                HealthPermission.getReadPermission(SleepSessionRecord::class),
                HealthPermission.getReadPermission(DistanceRecord::class),
                HealthPermission.getReadPermission(ExerciseSessionRecord::class),
                HealthPermission.getReadPermission(HydrationRecord::class),
                HealthPermission.getReadPermission(HeartRateRecord::class)
            )
        } else {
            emptySet()
        }
    }

    fun hasStepHardwareSensor(context: Context): Boolean {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return false
        return sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null ||
            sm.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null
    }

    fun getHealthConnectSdkStatus(context: Context): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return HealthConnectClient.SDK_UNAVAILABLE
        }
        return try {
            HealthConnectClient.getSdkStatus(context, HEALTH_CONNECT_PACKAGE)
        } catch (_: Exception) {
            HealthConnectClient.SDK_UNAVAILABLE
        }
    }

    fun isHealthConnectAvailable(context: Context): Boolean {
        val status = getHealthConnectSdkStatus(context)
        if (status == HealthConnectClient.SDK_AVAILABLE) return true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        return isPackageInstalled(context, HEALTH_CONNECT_PACKAGE)
    }

    private fun isPackageInstalled(context: Context, pkg: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(pkg, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    suspend fun getGrantedHealthPermissionsCount(context: Context): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return 0
        return try {
            val status = getHealthConnectSdkStatus(context)
            if (status != HealthConnectClient.SDK_AVAILABLE) return 0
            val client = HealthConnectClient.getOrCreate(context)
            val granted = client.permissionController.getGrantedPermissions()
            granted.intersect(requiredHealthPermissions).size
        } catch (_: Exception) {
            0
        }
    }

    /**
     * قراءة جميع البيانات الصحية الفعلية لليوم من Health Connect API مباشرة بدون أي تعبئة يدوية
     */
    suspend fun readTodayHealthConnectMetrics(
        context: Context,
        hardwareStepsFallback: Int = 0
    ): HealthConnectDailyMetrics {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            val cal = calculateBurnedCaloriesFromSteps(hardwareStepsFallback)
            return HealthConnectDailyMetrics(
                stepsCount = hardwareStepsFallback,
                burnedCalories = cal,
                syncSourceLabel = "حساس الخطوات"
            )
        }

        val status = getHealthConnectSdkStatus(context)
        if (status != HealthConnectClient.SDK_AVAILABLE) {
            val cal = calculateBurnedCaloriesFromSteps(hardwareStepsFallback)
            return HealthConnectDailyMetrics(
                stepsCount = hardwareStepsFallback,
                burnedCalories = cal,
                isConnectedToHealthConnect = false,
                syncSourceLabel = if (hardwareStepsFallback > 0) "حساس الخطوات بالجهاز" else "بانتظار ربط Health Connect"
            )
        }

        return try {
            val client = HealthConnectClient.getOrCreate(context)
            val granted = client.permissionController.getGrantedPermissions()
            val grantedCount = granted.intersect(requiredHealthPermissions).size

            val zoneId = ZoneId.systemDefault()
            val startOfToday = LocalDate.now(zoneId).atStartOfDay(zoneId).toInstant()
            val sleepLookbackStart = LocalDate.now(zoneId).minusDays(1).atTime(18, 0).atZone(zoneId).toInstant()
            val nowInstant = Instant.now()

            val todayAggregate = client.aggregate(
                AggregateRequest(
                    metrics = setOf(
                        StepsRecord.COUNT_TOTAL,
                        TotalCaloriesBurnedRecord.ENERGY_TOTAL,
                        ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL,
                        DistanceRecord.DISTANCE_TOTAL,
                        ExerciseSessionRecord.EXERCISE_DURATION_TOTAL,
                        HydrationRecord.VOLUME_TOTAL,
                        HeartRateRecord.BPM_AVG
                    ),
                    timeRangeFilter = TimeRangeFilter.between(startOfToday, nowInstant)
                )
            )

            val sleepAggregate = client.aggregate(
                AggregateRequest(
                    metrics = setOf(SleepSessionRecord.SLEEP_DURATION_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(sleepLookbackStart, nowInstant)
                )
            )

            val hcSteps = (todayAggregate[StepsRecord.COUNT_TOTAL] ?: 0L).toInt()
            val finalSteps = maxOf(hcSteps, hardwareStepsFallback)

            val totalKcal = todayAggregate[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories?.roundToInt() ?: 0
            val activeKcal = todayAggregate[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories?.roundToInt() ?: 0
            val finalCalories = when {
                totalKcal > 0 -> totalKcal
                activeKcal > 0 -> activeKcal
                else -> calculateBurnedCaloriesFromSteps(finalSteps)
            }

            val distanceMeters = todayAggregate[DistanceRecord.DISTANCE_TOTAL]?.inMeters?.roundToInt()
                ?: ((finalSteps * 0.72f).roundToInt())

            val activeMins = todayAggregate[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL]?.toMinutes()?.toInt()
                ?: (finalSteps / 110)

            val sleepDuration = sleepAggregate[SleepSessionRecord.SLEEP_DURATION_TOTAL]
            val sleepMins = sleepDuration?.toMinutes()?.toInt() ?: 0
            val sleepHours = ((sleepMins / 60f) * 10f).roundToInt() / 10f

            val waterMl = todayAggregate[HydrationRecord.VOLUME_TOTAL]?.inMilliliters?.roundToInt() ?: 0
            val avgBpm = (todayAggregate[HeartRateRecord.BPM_AVG] ?: 0L).toInt()

            HealthConnectDailyMetrics(
                stepsCount = finalSteps,
                burnedCalories = finalCalories,
                sleepMinutes = sleepMins,
                sleepHours = sleepHours,
                distanceMeters = distanceMeters,
                activeMinutes = activeMins,
                waterMl = waterMl,
                avgHeartRateBpm = avgBpm,
                grantedPermissionsCount = grantedCount,
                isConnectedToHealthConnect = grantedCount > 0,
                syncSourceLabel = if (grantedCount > 0) {
                    "متصل بـ Health Connect ✓"
                } else {
                    "بانتظار منح أذونات Health Connect"
                }
            )
        } catch (_: Exception) {
            val cal = calculateBurnedCaloriesFromSteps(hardwareStepsFallback)
            HealthConnectDailyMetrics(
                stepsCount = hardwareStepsFallback,
                burnedCalories = cal,
                distanceMeters = (hardwareStepsFallback * 0.72f).roundToInt(),
                activeMinutes = hardwareStepsFallback / 110,
                isConnectedToHealthConnect = false,
                syncSourceLabel = "حساس الخطوات المباشر"
            )
        }
    }

    /**
     * فتح إعدادات أو صلاحيات Health Connect الرسمية في النظام
     */
    fun openHealthConnectAppOrSettings(context: Context): Boolean {
        val intentsToTry = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                add(
                    Intent("android.health.connect.action.MANAGE_HEALTH_PERMISSIONS").apply {
                        putExtra(Intent.EXTRA_PACKAGE_NAME, context.packageName)
                    }
                )
            }
            add(
                Intent("androidx.health.ACTION_MANAGE_HEALTH_PERMISSIONS").apply {
                    putExtra(Intent.EXTRA_PACKAGE_NAME, context.packageName)
                }
            )
            add(Intent("android.health.connect.action.HEALTH_HOME_SETTINGS"))
            context.packageManager.getLaunchIntentForPackage(HEALTH_CONNECT_PACKAGE)?.let { add(it) }
            context.packageManager.getLaunchIntentForPackage(SAMSUNG_HEALTH_PACKAGE)?.let { add(it) }
        }

        for (intent in intentsToTry) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return true
            } catch (_: Exception) {
            }
        }

        return try {
            val marketIntent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("market://details?id=$HEALTH_CONNECT_PACKAGE")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(marketIntent)
            true
        } catch (_: Exception) {
            try {
                val appSettings = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(appSettings)
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    /**
     * قراءة خطوات اليوم الفعلية من حساس الجوال (TYPE_STEP_COUNTER) بدون أي تعديل يدوي
     */
    fun processHardwareStepCounterValue(context: Context, rawTotalSensorSteps: Int): Int {
        val todayKey = UmmAlQuraCalculator.todayDateKey()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val baselineKey = "baseline_$todayKey"

        val savedBaseline = prefs.getInt(baselineKey, -1)
        val baseline = if (savedBaseline < 0 || rawTotalSensorSteps < savedBaseline) {
            prefs.edit().putInt(baselineKey, rawTotalSensorSteps).apply()
            rawTotalSensorSteps
        } else {
            savedBaseline
        }

        return (rawTotalSensorSteps - baseline).coerceAtLeast(0)
    }

    fun calculateBurnedCaloriesFromSteps(steps: Int): Int {
        return (steps * 0.042f).toInt()
    }
}
