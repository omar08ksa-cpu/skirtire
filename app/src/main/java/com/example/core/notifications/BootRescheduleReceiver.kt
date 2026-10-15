package com.example.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.core.prayer.UmmAlQuraCalculator
import com.example.data.AppDatabase
import com.example.data.DailyManagerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * يعيد جدولة منبهات الصلاة تلقائيًا عند إعادة تشغيل الجوال أو تحديث التطبيق
 * كما هو مطلوب في قسم (الأندرويد وسامسونج One UI)
 */
class BootRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    val repo = DailyManagerRepository(db)
                    val cityId = repo.getSelectedCityId()
                    val city = UmmAlQuraCalculator.findCityById(cityId)
                    val schedule = UmmAlQuraCalculator.calculateScheduleForNow(city)
                    AdhanNotificationHelper.scheduleDailyPrayerAlarms(context, schedule)

                    val wakeAlarms = db.protectionDao().getBlockSchedulesOnce()
                    com.example.core.protection.ProtectionManager.rescheduleAllWakeAlarms(context, wakeAlarms)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
