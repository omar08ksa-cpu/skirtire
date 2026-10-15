package com.example.core.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.core.prayer.UmmAlQuraCalculator
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class DailyManagerAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateSingleWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET) {
            triggerWidgetRefresh(context)
        }
    }

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.example.core.widget.ACTION_REFRESH_WIDGET"

        fun triggerWidgetRefresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, DailyManagerAppWidgetProvider::class.java)
            val ids = manager.getAppWidgetIds(component)
            if (ids != null && ids.isNotEmpty()) {
                for (id in ids) {
                    updateSingleWidget(context, manager, id)
                }
            }
        }

        fun pushLiveUpdateToHomeWidget(
            context: Context,
            cityName: String,
            nextPrayerName: String,
            nextPrayerTime12: String,
            completedHabits: Int,
            totalHabits: Int,
            wirdPage: Int,
            studyMinutesToday: Int
        ) {
            try {
                val manager = AppWidgetManager.getInstance(context)
                val component = ComponentName(context, DailyManagerAppWidgetProvider::class.java)
                val ids = manager.getAppWidgetIds(component)
                if (ids == null || ids.isEmpty()) return

                val habitsPct = if (totalHabits > 0) ((completedHabits * 100) / totalHabits) else 0
                val views = RemoteViews(context.packageName, R.layout.widget_daily_manager)
                views.setTextViewText(
                    R.id.widget_next_prayer,
                    "$nextPrayerName • $nextPrayerTime12"
                )
                views.setTextViewText(
                    R.id.widget_habits_summary,
                    "العادات: $completedHabits / $totalHabits ($habitsPct%)"
                )
                views.setTextViewText(R.id.widget_study_mins, "$studyMinutesToday د")
                views.setTextViewText(R.id.widget_streak_banner, "الورد: صفحة $wirdPage • $cityName")

                val openAppIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val openPending = PendingIntent.getActivity(
                    context,
                    0,
                    openAppIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(R.id.widget_root_container, openPending)

                for (id in ids) {
                    manager.updateAppWidget(id, views)
                }
            } catch (_: Exception) {
            }
        }

        private fun updateSingleWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    val todayKey = UmmAlQuraCalculator.todayDateKey()

                    val cityId = db.settingDao().getSettingValue("city_id") ?: "riyadh"
                    val city = UmmAlQuraCalculator.findCityById(cityId)
                    val schedule = UmmAlQuraCalculator.calculateScheduleForNow(city)

                    val prayerLogs = db.prayerLogDao().getPrayerLogsForDateOnce(todayKey)
                    val completedPrayers = prayerLogs.count { it.status == "ON_TIME" }

                    val studySessions = db.studyDao().getStudySessionsOnce()
                    val studyMins = studySessions.filter { it.dateKey == todayKey }.sumOf { it.durationMinutes }

                    val healthToday = db.fullDayDao().getHealthSnapshotForDateOnce(todayKey)
                    val stepsCount = healthToday?.stepsCount ?: 0

                    val habits = db.habitDao().getActiveHabitsOnce()
                    val habitLogs = db.habitDao().observeAllHabitLogs().firstOrNull().orEmpty()
                        .filter { it.dateKey == todayKey }
                        .associateBy { it.habitId }
                    val completedHabits = habits.count { h -> habitLogs[h.id]?.isCompleted == true }
                    val habitsPct = if (habits.isNotEmpty()) ((completedHabits * 100) / habits.size) else 0

                    val views = RemoteViews(context.packageName, R.layout.widget_daily_manager)

                    views.setTextViewText(
                        R.id.widget_next_prayer,
                        "${schedule.nextPrayer.type.arabicName} (${schedule.nextPrayer.formatted12h}) • المتبقي ${schedule.countdownFormatted}"
                    )
                    views.setTextViewText(
                        R.id.widget_habits_summary,
                        "العادات المكتملة: $completedHabits / ${habits.size} ($habitsPct%)"
                    )
                    views.setTextViewText(R.id.widget_prayer_score, "$completedPrayers/5")
                    views.setTextViewText(R.id.widget_study_mins, "$studyMins د")
                    views.setTextViewText(R.id.widget_tasks_done, "$stepsCount")

                    val openAppIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val openPending = PendingIntent.getActivity(
                        context,
                        0,
                        openAppIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_root_container, openPending)

                    val refreshIntent = Intent(context, DailyManagerAppWidgetProvider::class.java).apply {
                        action = ACTION_REFRESH_WIDGET
                    }
                    val refreshPending = PendingIntent.getBroadcast(
                        context,
                        1,
                        refreshIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_refresh_btn, refreshPending)

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                } catch (_: Exception) {
                }
            }
        }
    }
}
