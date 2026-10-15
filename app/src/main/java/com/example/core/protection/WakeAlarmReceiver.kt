package com.example.core.protection

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * يستقبل رنين منبه الاستيقاظ الخارق الدقيق (AlarmManager.setAlarmClock)
 * ويفتح شاشة الإيقاف الكاملة التي لا تتوقف إلا بتصوير المكان ومطابقته أو رمز QR
 * كما يقوم تلقائياً بجدولة المنبه للمرة القادمة
 */
class WakeAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val alarmId = intent?.getStringExtra("alarm_id").orEmpty()
        val alarmTitle = intent?.getStringExtra("alarm_title") ?: "منبه الاستيقاظ الخارق"
        val verifyMethod = intent?.getStringExtra("alarm_verify_method") ?: "LOCAL_IMAGE"
        val placeName = intent?.getStringExtra("alarm_place_name") ?: "المكان المحدد"
        val qrCode = intent?.getStringExtra("alarm_qr_code") ?: "WAKE-QR-2026"
        val ringtoneUri = intent?.getStringExtra("alarm_ringtone_uri")
        val ringtoneTitle = intent?.getStringExtra("alarm_ringtone_title")

        ProtectionManager.triggerWakeAlarmNow(
            context = context,
            alarmId = alarmId,
            title = alarmTitle,
            overrideVerifyMethod = verifyMethod,
            overridePlaceName = placeName,
            overrideQrCode = qrCode,
            overrideRingtoneUri = ringtoneUri,
            overrideRingtoneTitle = ringtoneTitle
        )

        ensureWakeChannel(context)

        val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("trigger_wake_alarm", true)
            putExtra("custom_wake_title", alarmTitle)
            putExtra("custom_verify_method", verifyMethod)
            putExtra("custom_place_name", placeName)
            putExtra("custom_qr_code", qrCode)
        }
        val fullScreenPending = PendingIntent.getActivity(
            context,
            710,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            val verificationDesc = if (verifyMethod == "LOCAL_IMAGE") {
                "وجه الكاميرا نحو $placeName والتقط صورة مطابقة لإيقاف المنبه"
            } else {
                "امسح رمز QR المخصص لإيقاف المنبه"
            }

            val notification = NotificationCompat.Builder(context, WAKE_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("⏰ $alarmTitle (رنين مستمر!)")
                .setContentText(verificationDesc)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setFullScreenIntent(fullScreenPending, true)
                .setContentIntent(fullScreenPending)
                .build()
            NotificationManagerCompat.from(context).notify(7700, notification)
        }

        try {
            context.startActivity(fullScreenIntent)
        } catch (_: Exception) {
        }

        // إعادة جدولة المنبه القادم في الخلفية تلقائياً للأيام القادمة
        if (alarmId.isNotBlank()) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    val schedule = db.protectionDao().getBlockScheduleById(alarmId)
                    if (schedule != null && schedule.isEnabled) {
                        ProtectionManager.scheduleWakeAlarm(context, schedule)
                    }
                } catch (_: Exception) {
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun ensureWakeChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                WAKE_CHANNEL_ID,
                "منبه الاستيقاظ القوي بشاشة كاملة",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "منبه الاستيقاظ الذي يتطلب تصوير المكان أو مسح QR"
                enableVibration(true)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val WAKE_CHANNEL_ID = "wake_alarm_fullscreen_channel_v1"
    }
}
