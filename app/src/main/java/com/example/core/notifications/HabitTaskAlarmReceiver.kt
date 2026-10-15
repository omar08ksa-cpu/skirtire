package com.example.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.MainActivity
import com.example.core.protection.ProtectionManager

/**
 * مستقبل منبهات العادات والمهام:
 * يدعم المنبه العادي (إشعار صوتي) والمنبه الذكي الخارق (شاشة كاملة تتطلب تصوير المكان أو QR)
 */
class HabitTaskAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val title = intent?.getStringExtra(AdhanNotificationHelper.EXTRA_REMINDER_TITLE) ?: "تذكير"
        val isHabit = intent?.getBooleanExtra(AdhanNotificationHelper.EXTRA_REMINDER_IS_HABIT, true) ?: true
        val itemId = intent?.getStringExtra(AdhanNotificationHelper.EXTRA_REMINDER_ID) ?: "default"
        val alarmType = intent?.getStringExtra(AdhanNotificationHelper.EXTRA_ALARM_TYPE) ?: "NORMAL"
        val verifyMethod = intent?.getStringExtra(AdhanNotificationHelper.EXTRA_ALARM_VERIFY_METHOD) ?: "LOCAL_IMAGE"
        val placeName = intent?.getStringExtra(AdhanNotificationHelper.EXTRA_ALARM_PLACE_NAME) ?: "المكان المحدد"
        val qrCode = intent?.getStringExtra(AdhanNotificationHelper.EXTRA_ALARM_QR_CODE) ?: "WAKE-QR-2026"

        if (alarmType == "SUPER_WAKE") {
            ProtectionManager.triggerWakeAlarmNow(
                context = context,
                title = title,
                overrideVerifyMethod = verifyMethod,
                overridePlaceName = placeName,
                overrideQrCode = qrCode
            )
            try {
                val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("trigger_wake_alarm", true)
                }
                context.startActivity(fullScreenIntent)
            } catch (_: Exception) {
            }
        }

        AdhanNotificationHelper.showHabitOrTaskAlarmNotification(
            context = context,
            itemId = itemId,
            title = title,
            isHabit = isHabit
        )
    }
}
