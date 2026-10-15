package com.example.core.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.core.prayer.DailyPrayerSchedule
import com.example.core.prayer.PrayerType

object AdhanNotificationHelper {

    const val ADHAN_CHANNEL_ID = "adhan_exact_channel_v1"
    const val COUNTDOWN_CHANNEL_ID = "prayer_countdown_channel_v1"

    const val EXTRA_PRAYER_ID = "extra_prayer_id"
    const val EXTRA_CITY_NAME = "extra_city_name"
    const val EXTRA_IS_TEST = "extra_is_test"
    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_REMINDER_TITLE = "extra_reminder_title"
    const val EXTRA_REMINDER_IS_HABIT = "extra_reminder_is_habit"
    const val EXTRA_ALARM_TYPE = "extra_alarm_type"
    const val EXTRA_ALARM_VERIFY_METHOD = "extra_alarm_verify_method"
    const val EXTRA_ALARM_PLACE_NAME = "extra_alarm_place_name"
    const val EXTRA_ALARM_QR_CODE = "extra_alarm_qr_code"

    const val PERSISTENT_COUNTDOWN_NOTIFICATION_ID = 1099
    private var activeFlipDetector: com.example.core.prayer.PrayerFlipDetector? = null

    fun ensureChannelsCreated(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val adhanChannel = NotificationChannel(
                ADHAN_CHANNEL_ID,
                "نداء الأذان والتنبيهات الدقيقة",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "تنبيهات دخول وقت الصلاة حسب تقويم أم القرى مع زر (صليت)"
                enableVibration(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }

            val countdownChannel = NotificationChannel(
                COUNTDOWN_CHANNEL_ID,
                "العدّاد التنازلي للصلاة القادمة",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعار دائم أعلى لوحة الإشعارات يعرض الوقت المتبقي للصلاة القادمة"
                setShowBadge(false)
            }

            manager.createNotificationChannel(adhanChannel)
            manager.createNotificationChannel(countdownChannel)
        }
    }

    /**
     * إظهار أو تحديث إشعار عداد الصلاة الدائم في لوحة الإشعارات
     */
    fun showOrUpdatePersistentPrayerCountdownNotification(
        context: Context,
        schedule: DailyPrayerSchedule
    ) {
        ensureChannelsCreated(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPending = PendingIntent.getActivity(
            context,
            1099,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "باقي على صلاة ${schedule.nextPrayer.type.arabicName}: ${schedule.countdownFormatted}"
        val content = "الموعد: ${schedule.nextPrayer.formatted12h} • ${schedule.city.arabicName} (${schedule.hijriFormatted})"

        val notification = NotificationCompat.Builder(context, COUNTDOWN_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openAppPending)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        NotificationManagerCompat.from(context).notify(PERSISTENT_COUNTDOWN_NOTIFICATION_ID, notification)
    }

    fun cancelPersistentPrayerCountdownNotification(context: Context) {
        NotificationManagerCompat.from(context).cancel(PERSISTENT_COUNTDOWN_NOTIFICATION_ID)
    }

    /**
     * تفعيل مستشعر قلب الجهاز لإسكات الأذان
     */
    fun activateFlipToMuteDetector(context: Context) {
        try {
            activeFlipDetector?.stopListening()
            activeFlipDetector = com.example.core.prayer.PrayerFlipDetector(context) {
                // عند قلب الجهاز للأسفل: إسكات التنبيه فوراً
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                audioManager?.setStreamMute(AudioManager.STREAM_ALARM, true)
                Handler(Looper.getMainLooper()).postDelayed({
                    audioManager?.setStreamMute(AudioManager.STREAM_ALARM, false)
                }, 2000L)
            }
            activeFlipDetector?.startListening()
            // إيقاف الاستماع تلقائياً بعد دقيقة
            Handler(Looper.getMainLooper()).postDelayed({
                activeFlipDetector?.stopListening()
                activeFlipDetector = null
            }, 60_000L)
        } catch (_: Exception) {
        }
    }

    /**
     * إظهار إشعار الأذان بشاشة كاملة/أولوية قصوى مع زر "صليت ✅" لتسجيلها كعادة مباشرة
     */
    fun showAdhanNotification(
        context: Context,
        prayerType: PrayerType,
        cityName: String,
        isTest: Boolean = false
    ) {
        ensureChannelsCreated(context)

        // تشغيل نغمة تنبيه مسموعة بدون إنترنت
        playOfflineAdhanChime(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_PRAYER_ID, prayerType.id)
        }
        val openAppPending = PendingIntent.getActivity(
            context,
            prayerType.ordinal + 100,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // زر "صليت ✅" داخل الإشعار نفسه
        val prayedActionIntent = Intent(context, PrayerActionReceiver::class.java).apply {
            putExtra(EXTRA_PRAYER_ID, prayerType.id)
            putExtra(EXTRA_CITY_NAME, cityName)
        }
        val prayedActionPending = PendingIntent.getBroadcast(
            context,
            prayerType.ordinal + 200,
            prayedActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val titleText = if (isTest) {
            "🕌 تجربة أذان ${prayerType.arabicName} (${cityName})"
        } else {
            "🕌 حان الآن وقت صلاة ${prayerType.arabicName} (${cityName})"
        }

        val bodyText = "حيّ على الصلاة، حيّ على الفلاح • اضغط «صليت ✅» لتسجيلها في عاداتك اليومية تلقائيًا."

        val notification = NotificationCompat.Builder(context, ADHAN_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(titleText)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(openAppPending)
            .setFullScreenIntent(openAppPending, true)
            .addAction(
                android.R.drawable.checkbox_on_background,
                "صليت ✅ (تسجيل كعادة)",
                prayedActionPending
            )
            .build()

        NotificationManagerCompat.from(context).notify(1000 + prayerType.ordinal, notification)
    }

    /**
     * تحديث الإشعار بعد ضغط المستخدم على زر "صليت" لتأكيد التسجيل التلقائي
     */
    fun showPrayerConfirmedNotification(context: Context, prayerType: PrayerType) {
        ensureChannelsCreated(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val notification = NotificationCompat.Builder(context, ADHAN_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("✅ تقبّل الله! تم تسجيل صلاة ${prayerType.arabicName}")
            .setContentText("تم تحديث سجل صلوات اليوم وإضافتها لعادة «المحافظة على الصلوات الخمس» تلقائيًا.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(1000 + prayerType.ordinal, notification)
    }

    /**
     * إظهار إشعار التنبيه المسبق للمحاضرة الجامعية (المرحلة ٥)
     */
    fun showLectureReminderNotification(
        context: Context,
        lectureTitle: String,
        courseCode: String,
        location: String,
        startTime24: String,
        reminderMinutesBefore: Int
    ) {
        ensureChannelsCreated(context)
        playOfflineAdhanChime(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPending = PendingIntent.getActivity(
            context,
            770,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val titleText = "🎓 تذكير محاضرة بعد $reminderMinutesBefore د: $lectureTitle ($courseCode)"
        val bodyText = "الموعد: $startTime24 • القاعة: $location • جهّز ملاحظاتك الآن."

        val notification = NotificationCompat.Builder(context, ADHAN_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(titleText)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(openAppPending)
            .build()

        NotificationManagerCompat.from(context).notify(1770, notification)
    }

    /**
     * إظهار إشعار منبه العادة أو المهمة في الوقت المحدد
     */
    fun showHabitOrTaskAlarmNotification(
        context: Context,
        itemId: String,
        title: String,
        isHabit: Boolean
    ) {
        ensureChannelsCreated(context)
        playOfflineAdhanChime(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val reqCode = (itemId.hashCode() and 0x7FFFFFFF) % 50000 + 2000
        val openAppPending = PendingIntent.getActivity(
            context,
            reqCode,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prefix = if (isHabit) "🔥 منبه عادة" else "✅ منبه مهمة"
        val bodyText = if (isHabit) {
            "حان موعد إنجاز عادتك: «$title»"
        } else {
            "تذكير بإنجاز مهمتك: «$title»"
        }

        val notification = NotificationCompat.Builder(context, ADHAN_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("$prefix: $title")
            .setContentText(bodyText)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(openAppPending)
            .build()

        NotificationManagerCompat.from(context).notify(reqCode, notification)
    }

    /**
     * إظهار إشعار إتمام هدف المذاكرة أو البومودورو
     */
    fun showStudyPomodoroCompletedNotification(
        context: Context,
        subjectName: String,
        durationMinutes: Int
    ) {
        ensureChannelsCreated(context)
        playOfflineAdhanChime(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPending = PendingIntent.getActivity(
            context,
            880,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, ADHAN_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🎉 اكتمل الهدف المطلوب: $subjectName ($durationMinutes د)")
            .setContentText("أحسنت! تم تحقيق المطلوب، ويمكنك الاستمرار لاحتساب وقت إضافي أو حفظ الجلسة.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setContentIntent(openAppPending)
            .build()

        NotificationManagerCompat.from(context).notify(1880, notification)
    }

    /**
     * جدولة منبه حقيقي لعادة أو مهمة في وقت محدد (HH:mm) عبر AlarmManager
     */
    fun scheduleHabitOrTaskDailyAlarm(
        context: Context,
        itemId: String,
        title: String,
        isHabit: Boolean,
        time24: String,
        alarmType: String = "NORMAL",
        verifyMethod: String = "LOCAL_IMAGE",
        placeName: String = "المكان المحدد",
        qrCode: String = "WAKE-QR-2026"
    ): Boolean {
        val parts = time24.trim().split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return false
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: return false

        val now = System.currentTimeMillis()
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("GMT+03:00")).apply {
            timeInMillis = now
            set(java.util.Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(java.util.Calendar.MINUTE, minute.coerceIn(0, 59))
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) {
                add(java.util.Calendar.DAY_OF_MONTH, 1)
            }
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val reqCode = (itemId.hashCode() and 0x7FFFFFFF) % 50000 + 2000
        val intent = Intent(context, HabitTaskAlarmReceiver::class.java).apply {
            putExtra(EXTRA_REMINDER_ID, itemId)
            putExtra(EXTRA_REMINDER_TITLE, title)
            putExtra(EXTRA_REMINDER_IS_HABIT, isHabit)
            putExtra(EXTRA_ALARM_TYPE, alarmType)
            putExtra(EXTRA_ALARM_VERIFY_METHOD, verifyMethod)
            putExtra(EXTRA_ALARM_PLACE_NAME, placeName)
            putExtra(EXTRA_ALARM_QR_CODE, qrCode)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val showIntent = Intent(context, MainActivity::class.java)
        val showPending = PendingIntent.getActivity(
            context,
            reqCode,
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
                val info = AlarmManager.AlarmClockInfo(cal.timeInMillis, showPending)
                alarmManager.setAlarmClock(info, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    cal.timeInMillis,
                    pendingIntent
                )
            }
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun cancelHabitOrTaskAlarm(context: Context, itemId: String) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val reqCode = (itemId.hashCode() and 0x7FFFFFFF) % 50000 + 2000
            val intent = Intent(context, HabitTaskAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reqCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        } catch (_: Exception) {
        }
    }

    /**
     * جدولة منبهات الأذان الدقيقة لليوم الحالي عبر AlarmManager.setAlarmClock
     * لضمان عملها حتى مع قفل الشاشة ووضع توفير الطاقة في سامسونج One UI
     */
    fun scheduleDailyPrayerAlarms(context: Context, schedule: DailyPrayerSchedule): Int {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()
        var scheduledCount = 0

        schedule.prayers
            .filter { it.type.isObligatory && it.timeMillisToday > now }
            .forEach { slot ->
                scheduleSingleExactAlarm(
                    context = context,
                    alarmManager = alarmManager,
                    triggerAtMillis = slot.timeMillisToday,
                    prayerType = slot.type,
                    cityName = schedule.city.arabicName,
                    requestCode = 300 + slot.type.ordinal,
                    isTest = false
                )
                scheduledCount++
            }
        return scheduledCount
    }

    /**
     * جدولة منبه أذان تجريبي بعد 10 ثوانٍ حتى يختبر المستخدم بنفسه عمل المنبه عند قفل الشاشة
     */
    fun scheduleTestAlarmInSeconds(
        context: Context,
        seconds: Int = 10,
        prayerType: PrayerType = PrayerType.FAJR,
        cityName: String = "الرياض"
    ): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + (seconds * 1000L)
        return scheduleSingleExactAlarm(
            context = context,
            alarmManager = alarmManager,
            triggerAtMillis = triggerAt,
            prayerType = prayerType,
            cityName = cityName,
            requestCode = 999,
            isTest = true
        )
    }

    private fun scheduleSingleExactAlarm(
        context: Context,
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        prayerType: PrayerType,
        cityName: String,
        requestCode: Int,
        isTest: Boolean
    ): Boolean {
        val alarmIntent = Intent(context, AdhanAlarmReceiver::class.java).apply {
            putExtra(EXTRA_PRAYER_ID, prayerType.id)
            putExtra(EXTRA_CITY_NAME, cityName)
            putExtra(EXTRA_IS_TEST, isTest)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showActivityIntent = Intent(context, MainActivity::class.java)
        val showPending = PendingIntent.getActivity(
            context,
            requestCode,
            showActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return try {
            val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                alarmManager.canScheduleExactAlarms()
            } else {
                true
            }

            if (canExact) {
                val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showPending)
                alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
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

    fun playMuazzinPreviewSound(context: Context, voiceId: String) {
        try {
            val streamType = AudioManager.STREAM_MUSIC
            val toneGen = ToneGenerator(streamType, 85)
            when (voiceId) {
                "makkah" -> {
                    toneGen.startTone(ToneGenerator.TONE_PROP_PROMPT, 600)
                    Handler(Looper.getMainLooper()).postDelayed({
                        try {
                            toneGen.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 700)
                            Handler(Looper.getMainLooper()).postDelayed({ toneGen.release() }, 800L)
                        } catch (_: Exception) {}
                    }, 650L)
                }
                "madinah" -> {
                    toneGen.startTone(ToneGenerator.TONE_CDMA_MED_PBX_L, 700)
                    Handler(Looper.getMainLooper()).postDelayed({
                        try {
                            toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 600)
                            Handler(Looper.getMainLooper()).postDelayed({ toneGen.release() }, 700L)
                        } catch (_: Exception) {}
                    }, 750L)
                }
                "takbeerat" -> {
                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 500)
                    Handler(Looper.getMainLooper()).postDelayed({
                        try {
                            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 500)
                            Handler(Looper.getMainLooper()).postDelayed({ toneGen.release() }, 600L)
                        } catch (_: Exception) {}
                    }, 550L)
                }
                else -> {
                    toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 700)
                    Handler(Looper.getMainLooper()).postDelayed({
                        try { toneGen.release() } catch (_: Exception) {}
                    }, 800L)
                }
            }
        } catch (_: Exception) {
            try {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                RingtoneManager.getRingtone(context, uri)?.play()
            } catch (_: Exception) {}
        }
    }

    private fun playOfflineAdhanChime(context: Context) {
        try {
            // نغمة هادئة متدرجة تحاكي نداء التنبيه بدون الحاجة للإنترنت
            val toneGen = ToneGenerator(AudioManager.STREAM_ALARM, 85)
            toneGen.startTone(ToneGenerator.TONE_PROP_BEEP2, 450)
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    toneGen.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 600)
                } catch (_: Exception) {
                }
            }, 500L)
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    toneGen.release()
                } catch (_: Exception) {
                }
            }, 1300L)
        } catch (_: Exception) {
            try {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                RingtoneManager.getRingtone(context, uri)?.play()
            } catch (_: Exception) {
            }
        }
    }
}
