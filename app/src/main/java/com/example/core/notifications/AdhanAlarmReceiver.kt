package com.example.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.core.prayer.MosqueLocationVerifier
import com.example.core.prayer.PrayerType
import com.example.core.prayer.UmmAlQuraCalculator
import com.example.data.AppDatabase
import com.example.data.DailyManagerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * مستقبل وقت الصلاة: يرسل إشعار الأذان ويتحقق تلقائياً في الخلفية من وجود المستخدم في المسجد لتسجيل الصلاة تلقائياً
 */
class AdhanAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val prayerId = intent?.getStringExtra(AdhanNotificationHelper.EXTRA_PRAYER_ID) ?: PrayerType.FAJR.id
        val cityName = intent?.getStringExtra(AdhanNotificationHelper.EXTRA_CITY_NAME) ?: "الرياض"
        val isTest = intent?.getBooleanExtra(AdhanNotificationHelper.EXTRA_IS_TEST, false) ?: false
        val bellMode = intent?.getStringExtra(AdhanNotificationHelper.EXTRA_BELL_MODE)

        val prayerType = PrayerType.fromId(prayerId)
        AdhanNotificationHelper.showAdhanNotification(
            context = context,
            prayerType = prayerType,
            cityName = cityName,
            isTest = isTest,
            explicitBellMode = bellMode
        )

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context.applicationContext)
                val repo = DailyManagerRepository(db)
                val mosques = repo.getApprovedMosquesOnce()
                if (mosques.isNotEmpty()) {
                    val result = MosqueLocationVerifier.verifyUserAtMosque(
                        context = context.applicationContext,
                        approvedMosques = mosques
                    )
                    if (result.isVerifiedInsideMosque && result.matchedMosque != null) {
                        repo.logPrayerVerifiedAtMosque(
                            dateKey = UmmAlQuraCalculator.todayDateKey(),
                            prayerType = prayerType,
                            mosqueName = result.matchedMosque.name,
                            distanceMeters = result.distanceMeters ?: 0
                        )
                    }
                }
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }
}
