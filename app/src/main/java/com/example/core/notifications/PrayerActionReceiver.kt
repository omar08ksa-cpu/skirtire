package com.example.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.MainActivity
import com.example.core.prayer.MosqueLocationVerifier
import com.example.core.prayer.PrayerType
import com.example.core.prayer.UmmAlQuraCalculator
import com.example.data.AppDatabase
import com.example.data.DailyManagerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * يستقبل ضغطة زر التحقق المكاني من داخل إشعار الأذان:
 * يتحقق فعليًا عبر GPS من وجود المستخدم داخل نطاق المساجد المحددة قبل تسجيل الصلاة كعادة منجزة
 */
class PrayerActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val prayerId = intent?.getStringExtra(AdhanNotificationHelper.EXTRA_PRAYER_ID) ?: PrayerType.FAJR.id
        val prayerType = PrayerType.fromId(prayerId)
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val repo = DailyManagerRepository(db)
                val mosques = repo.getApprovedMosquesOnce()
                val verification = MosqueLocationVerifier.verifyUserAtMosque(context, mosques)

                if (verification.isVerifiedInsideMosque) {
                    val todayKey = UmmAlQuraCalculator.todayDateKey()
                    repo.logPrayerVerifiedAtMosque(
                        dateKey = todayKey,
                        prayerType = prayerType,
                        mosqueName = verification.matchedMosque?.name.orEmpty(),
                        distanceMeters = verification.distanceMeters ?: 0
                    )
                    AdhanNotificationHelper.showPrayerConfirmedNotification(context, prayerType)
                } else {
                    val openApp = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(AdhanNotificationHelper.EXTRA_PRAYER_ID, prayerType.id)
                    }
                    context.startActivity(openApp)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
