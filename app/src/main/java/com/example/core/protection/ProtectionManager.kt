package com.example.core.protection

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import com.example.MainActivity
import com.example.data.BlockScheduleEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import java.util.TimeZone

data class ProtectionRuntimeState(
    val isWakeAlarmRinging: Boolean = false,
    val activeAlarmId: String = "",
    val wakeAlarmTitle: String = "منبه الاستيقاظ الخارق",
    val expectedQrCode: String = "WAKE-QR-2026",
    val placeName: String = "المكان المحدد",
    val verificationMethod: String = "LOCAL_IMAGE", // "LOCAL_IMAGE" أو "QR"
    val emergencyExitType: String = "MATH_3",
    val ringtoneUri: String = "",
    val ringtoneTitle: String = "نغمة المنبه الافتراضية",
    val isSleepModeActive: Boolean = false,
    val isStrictProtectionEnabled: Boolean = true,
    val isBlockerOverlayVisible: Boolean = false,
    val interceptedAppName: String = "",
    val interceptedReason: String = "",
    val blockedStudyPackages: Set<String> = emptySet(),
    val blockedSleepPackages: Set<String> = emptySet(),
    val enforcedBlockReasonsByPackage: Map<String, String> = emptyMap()
)

object ProtectionManager {

    val permanentExemptPackages = setOf(
        "com.android.dialer",
        "com.google.android.dialer",
        "com.samsung.android.dialer",
        "com.samsung.android.incallui",
        "com.android.incallui",
        "com.sec.android.app.clockpackage",
        "com.google.android.deskclock",
        "com.android.deskclock",
        "com.samsung.android.messaging",
        "com.google.android.apps.messaging",
        "com.android.EmergencyGesture",
        "com.android.server.telecom",
        "sa.gov.nic.tawakkalna",
        "com.moh.sehaty",
        "com.android.settings",
        "com.android.systemui"
    )

    private val _state = MutableStateFlow(ProtectionRuntimeState())
    val state: StateFlow<ProtectionRuntimeState> = _state.asStateFlow()

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }
    private var volumeEnforcerRunnable: Runnable? = null

    fun syncFromDatabase(
        isSleepMode: Boolean,
        isStrictProtection: Boolean,
        expectedQrCode: String,
        placeName: String,
        verificationMethod: String,
        emergencyExitType: String,
        blockedStudyPkgs: Set<String>,
        blockedSleepPkgs: Set<String>,
        enforcedReasonsMap: Map<String, String> = emptyMap()
    ) {
        _state.value = _state.value.copy(
            isSleepModeActive = isSleepMode,
            isStrictProtectionEnabled = isStrictProtection,
            expectedQrCode = expectedQrCode,
            placeName = placeName,
            verificationMethod = verificationMethod,
            emergencyExitType = emergencyExitType,
            blockedStudyPackages = blockedStudyPkgs,
            blockedSleepPackages = blockedSleepPkgs,
            enforcedBlockReasonsByPackage = enforcedReasonsMap
        )
    }

    /**
     * تشغيل المنبه الخارق فورياً مع قفل الصوت على أقصى درجة ومنع التحايل
     */
    fun triggerWakeAlarmNow(
        context: Context,
        alarmId: String = "",
        title: String = "منبه الاستيقاظ الخارق",
        overrideVerifyMethod: String? = null,
        overridePlaceName: String? = null,
        overrideQrCode: String? = null,
        overrideRingtoneUri: String? = null,
        overrideRingtoneTitle: String? = null
    ) {
        val finalMethod = overrideVerifyMethod ?: _state.value.verificationMethod
        val finalPlace = overridePlaceName ?: _state.value.placeName
        val finalQr = overrideQrCode ?: _state.value.expectedQrCode
        val finalUri = overrideRingtoneUri ?: _state.value.ringtoneUri
        val finalToneTitle = overrideRingtoneTitle ?: _state.value.ringtoneTitle

        _state.value = _state.value.copy(
            isWakeAlarmRinging = true,
            activeAlarmId = alarmId,
            wakeAlarmTitle = title,
            verificationMethod = finalMethod,
            placeName = finalPlace,
            expectedQrCode = finalQr,
            ringtoneUri = finalUri,
            ringtoneTitle = finalToneTitle
        )

        // تشغيل النغمة الحقيقية بنظام أندرويد
        AlarmSoundManager.playAlarmLooping(context, finalUri)

        // تفعيل قفل الصوت الإجباري والاهتزاز المستمر
        startVolumeLockLoop(context)
    }

    /**
     * إيقاف المنبه بعد نجاح التحقق (الصورة المطابقة للمكان أو رمز QR)
     */
    fun dismissWakeAlarmAfterVerification(context: Context? = null) {
        _state.value = _state.value.copy(
            isWakeAlarmRinging = false
        )
        AlarmSoundManager.stopAlarm()
        stopVolumeLockLoop()

        context?.let { ctx ->
            try {
                val notifManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notifManager.cancel(7700)
            } catch (_: Exception) {
            }
        }
    }

    /**
     * تثبيت وإعادة ضبط الصوت على 100% فورياً لمنع تخفيض الصوت أثناء الرنين
     */
    fun enforceMaxAlarmVolume(context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val maxAlarm = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val maxMusic = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarm, 0)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxMusic, 0)

            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(400)
            }
        } catch (_: Exception) {
        }
    }

    private fun startVolumeLockLoop(context: Context) {
        stopVolumeLockLoop()
        val appContext = context.applicationContext
        val runnable = object : Runnable {
            override fun run() {
                if (_state.value.isWakeAlarmRinging) {
                    enforceMaxAlarmVolume(appContext)
                    mainHandler.postDelayed(this, 350L)
                }
            }
        }
        volumeEnforcerRunnable = runnable
        mainHandler.post(runnable)
    }

    private fun stopVolumeLockLoop() {
        volumeEnforcerRunnable?.let { mainHandler.removeCallbacks(it) }
        volumeEnforcerRunnable = null
    }

    fun triggerBlockerOverlayPreview(
        appName: String,
        reason: String = "هذا التطبيق محظور حالياً"
    ) {
        _state.value = _state.value.copy(
            isBlockerOverlayVisible = true,
            interceptedAppName = appName,
            interceptedReason = reason
        )
    }

    fun dismissBlockerOverlay() {
        _state.value = _state.value.copy(
            isBlockerOverlayVisible = false
        )
    }

    // -------------------------------------------------------------
    // جدولة منبهات متعددة مع دعم أيام الأسبوع والتوقيت الدقيق
    // -------------------------------------------------------------

    private fun getRequestCodeForAlarm(alarmId: String): Int {
        return 7000 + ((alarmId.hashCode() and 0x7FFFFFFF) % 50000)
    }

    /**
     * تحويل يوم التقويم (Calendar.DAY_OF_WEEK) إلى ترميز النظام (1=السبت .. 7=الجمعة)
     */
    fun calendarDayToWeekIndex(calendarDay: Int): Int = when (calendarDay) {
        Calendar.SATURDAY -> 1
        Calendar.SUNDAY -> 2
        Calendar.MONDAY -> 3
        Calendar.TUESDAY -> 4
        Calendar.WEDNESDAY -> 5
        Calendar.THURSDAY -> 6
        Calendar.FRIDAY -> 7
        else -> 1
    }

    /**
     * حساب التوقيت القادم الدقيق بالمللي ثانية للمنبه وفق الوقت والأيام المحددة
     */
    fun calculateNextTriggerMillis(startTime24: String, daysOfWeekCsv: String): Long {
        val parts = startTime24.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 5
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val allowedDays: Set<Int> = if (daysOfWeekCsv.isBlank()) {
            (1..7).toSet()
        } else {
            daysOfWeekCsv.split(",")
                .mapNotNull { it.trim().toIntOrNull() }
                .filter { it in 1..7 }
                .toSet()
                .ifEmpty { (1..7).toSet() }
        }

        val tz = TimeZone.getTimeZone("GMT+03:00")
        val now = System.currentTimeMillis()

        // البحث في الأيام الـ 8 القادمة عن أول يوم ووقت يطابق الأيام المختارة
        for (dayOffset in 0..8) {
            val cal = Calendar.getInstance(tz).apply {
                timeInMillis = now
                add(Calendar.DAY_OF_MONTH, dayOffset)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (cal.timeInMillis > now) {
                val weekIdx = calendarDayToWeekIndex(cal.get(Calendar.DAY_OF_WEEK))
                if (weekIdx in allowedDays) {
                    return cal.timeInMillis
                }
            }
        }

        // في حال لم يتطابق أي يوم، نجدوله للغد بنفس التوقيت كإجراء أمان
        val fallbackCal = Calendar.getInstance(tz).apply {
            timeInMillis = now
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return fallbackCal.timeInMillis
    }

    fun scheduleDailyWakeAlarm(context: Context, time24: String = "04:30"): Boolean {
        val defaultAlarm = BlockScheduleEntity(
            id = "schedule_wake_alarm_default",
            title = "منبه الاستيقاظ",
            scheduleType = "WAKE_ALARM",
            startTime24 = time24,
            daysOfWeek = "1,2,3,4,5,6,7",
            isEnabled = true
        )
        return scheduleWakeAlarm(context, defaultAlarm)
    }

    /**
     * جدولة منبه خارق محدد في AlarmManager
     */
    fun scheduleWakeAlarm(context: Context, alarm: BlockScheduleEntity): Boolean {
        if (!alarm.isEnabled) {
            cancelWakeAlarm(context, alarm.id)
            return false
        }

        val triggerAtMillis = calculateNextTriggerMillis(alarm.startTime24, alarm.daysOfWeek)
        val requestCode = getRequestCodeForAlarm(alarm.id)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        val intent = Intent(context, WakeAlarmReceiver::class.java).apply {
            putExtra("alarm_id", alarm.id)
            putExtra("alarm_title", alarm.title)
            putExtra("alarm_verify_method", alarm.verificationMethod)
            putExtra("alarm_place_name", alarm.placeName)
            putExtra("alarm_qr_code", alarm.qrReferenceCode)
            putExtra("alarm_ringtone_uri", alarm.ringtoneUri)
            putExtra("alarm_ringtone_title", alarm.ringtoneTitle)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("trigger_wake_alarm", true)
            putExtra("custom_wake_title", alarm.title)
            putExtra("custom_verify_method", alarm.verificationMethod)
            putExtra("custom_place_name", alarm.placeName)
            putExtra("custom_qr_code", alarm.qrReferenceCode)
        }
        val showPending = PendingIntent.getActivity(
            context,
            requestCode,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return try {
            val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }
            if (canExact) {
                val clockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showPending)
                alarmManager.setAlarmClock(clockInfo, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            true
        } catch (_: SecurityException) {
            false
        }
    }

    /**
     * إلغاء منبه محدد
     */
    fun cancelWakeAlarm(context: Context, alarmId: String) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val requestCode = getRequestCodeForAlarm(alarmId)
            val intent = Intent(context, WakeAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        } catch (_: Exception) {
        }
    }

    /**
     * إعادة جدولة كل المنبهات المفعلة
     */
    fun rescheduleAllWakeAlarms(context: Context, alarms: List<BlockScheduleEntity>) {
        alarms.filter { it.scheduleType == "WAKE_ALARM" }.forEach { alarm ->
            if (alarm.isEnabled) {
                scheduleWakeAlarm(context, alarm)
            } else {
                cancelWakeAlarm(context, alarm.id)
            }
        }
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        return try {
            val expectedComponent = ComponentName(context, AppBlockerAccessibilityService::class.java)
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            enabledServices.split(':').any {
                ComponentName.unflattenFromString(it) == expectedComponent
            }
        } catch (_: Exception) {
            false
        }
    }

    fun isDeviceAdminActive(context: Context): Boolean {
        return try {
            val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            val adminComponent = ComponentName(context, AppDeviceAdminReceiver::class.java)
            dpm.isAdminActive(adminComponent)
        } catch (_: Exception) {
            false
        }
    }
}
