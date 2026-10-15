package com.example

import com.example.core.fullday.IcsCalendarParser
import com.example.core.insights.DailyInsightsEngine
import com.example.core.prayer.PrayerType
import com.example.core.prayer.UmmAlQuraCalculator
import com.example.core.protection.ProtectionManager
import com.example.core.study.StudyTimerState
import com.example.data.HealthSnapshotEntity
import com.example.data.WirdProgressEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun stage6_7_dailyInsightsEngine_generatesSummaryTomorrowScheduleAndTimeline() {
        val summary = DailyInsightsEngine.generateSmartDailySummary(
            dateKey = "2026-10-11",
            prayerStatusMap = mapOf(
                PrayerType.FAJR to "ON_TIME",
                PrayerType.DHUHR to "ON_TIME",
                PrayerType.ASR to "ON_TIME"
            ),
            azkarLogs = emptyList(),
            wird = WirdProgressEntity(currentPage = 44, currentJuz = 3, todayPagesRead = 2),
            studySessions = emptyList(),
            subjects = emptyList(),
            tasks = emptyList(),
            habits = emptyList(),
            habitLogsToday = emptyMap(),
            health = HealthSnapshotEntity(id = "h1", dateKey = "2026-10-11", stepsCount = 6500, stepsGoal = 8000),
            lectures = IcsCalendarParser.parseIcsToLectures(IcsCalendarParser.sampleUniversityIcs),
            customFocusNote = "التركيز على مراجعة الخوارزميات"
        )
        assertTrue(summary.overallScore in 35..100)
        assertTrue(summary.suggestedTomorrowSchedule.contains("٠٤:٣٠ ص"))
        assertTrue(summary.improvementsText.contains("التركيز على مراجعة الخوارزميات"))

        val riyadh = UmmAlQuraCalculator.findCityById("riyadh")
        val schedule = UmmAlQuraCalculator.calculateScheduleForNow(riyadh)
        val timeline = DailyInsightsEngine.buildChronologicalTimeline(
            schedule = schedule,
            prayerStatusMap = mapOf(PrayerType.FAJR to "ON_TIME"),
            azkarLogs = emptyList(),
            wird = WirdProgressEntity(),
            lectures = emptyList(),
            studySessions = emptyList(),
            tasks = emptyList(),
            health = HealthSnapshotEntity(id = "h1", dateKey = "2026-10-11"),
            isSleepModeActive = false
        )
        assertTrue("التايم لاين يجب أن يحتوي على الاستيقاظ والصلوات الخمس والمذاكرة والنوم", timeline.size >= 8)
        assertEquals("tl_wake", timeline.first().id)
        assertEquals("tl_night_sleep", timeline.last().id)
    }

    @Test
    fun stage5_icsParserAndMadinahMushafJuzCalculation_areValid() {
        val lectures = IcsCalendarParser.parseIcsToLectures(IcsCalendarParser.sampleUniversityIcs)
        assertEquals(2, lectures.size)
        assertEquals("هندسة البرمجيات المتقدمة", lectures[0].title)
        assertEquals("SWE-312", lectures[0].courseCode)
        assertEquals("10:00", lectures[0].startTime24)
        assertEquals("11:30", lectures[0].endTime24)
        assertTrue(lectures[0].isImportedFromIcs)

        assertEquals(1, IcsCalendarParser.calculateJuzForMadinahPage(1))
        assertEquals(1, IcsCalendarParser.calculateJuzForMadinahPage(21))
        assertEquals(2, IcsCalendarParser.calculateJuzForMadinahPage(22))
        assertEquals(3, IcsCalendarParser.calculateJuzForMadinahPage(42))
        assertEquals(30, IcsCalendarParser.calculateJuzForMadinahPage(604))
    }

    @Test
    fun ummAlQura_prayerTimesOrderAndIshaOffset_areValid() {
        val riyadh = UmmAlQuraCalculator.findCityById("riyadh")
        val schedule = UmmAlQuraCalculator.calculateScheduleForNow(riyadh)

        assertEquals(6, schedule.prayers.size)
        val fajr = schedule.prayers.first { it.type == PrayerType.FAJR }
        val sunrise = schedule.prayers.first { it.type == PrayerType.SUNRISE }
        val dhuhr = schedule.prayers.first { it.type == PrayerType.DHUHR }
        val asr = schedule.prayers.first { it.type == PrayerType.ASR }
        val maghrib = schedule.prayers.first { it.type == PrayerType.MAGHRIB }
        val isha = schedule.prayers.first { it.type == PrayerType.ISHA }

        assertTrue("الفجر يجب أن يكون قبل الشروق", fajr.timeMillisToday < sunrise.timeMillisToday)
        assertTrue("الشروق يجب أن يكون قبل الظهر", sunrise.timeMillisToday < dhuhr.timeMillisToday)
        assertTrue("الظهر يجب أن يكون قبل العصر", dhuhr.timeMillisToday < asr.timeMillisToday)
        assertTrue("العصر يجب أن يكون قبل المغرب", asr.timeMillisToday < maghrib.timeMillisToday)
        assertTrue("المغرب يجب أن يكون قبل العشاء", maghrib.timeMillisToday < isha.timeMillisToday)
    }

    @Test
    fun qiblaAngle_fromRiyadh_pointsWestSouthwestTowardsMakkah() {
        val riyadh = UmmAlQuraCalculator.findCityById("riyadh")
        val qibla = UmmAlQuraCalculator.calculateQiblaAngle(riyadh.latitude, riyadh.longitude)
        assertTrue("زاوية القبلة من الرياض يجب أن تكون بين 240 و 248 درجة", qibla in 240.0..248.0)
    }

    @Test
    fun countdownFormatting_convertsToArabicNumerals() {
        val formatted = UmmAlQuraCalculator.formatCountdown(3665L) // 01:01:05
        assertEquals("٠١:٠١:٠٥", formatted)
    }

    @Test
    fun studyTimerState_pomodoroAndFreeFormatting_areCorrect() {
        val pomodoroState = StudyTimerState(
            isActive = true,
            mode = "POMODORO",
            targetDurationSeconds = 1500,
            elapsedSeconds = 300
        )
        assertEquals(1200, pomodoroState.displaySeconds)
        assertEquals("٢٠:٠٠", pomodoroState.formattedTime())
        assertEquals(0.2f, pomodoroState.progressFraction, 0.01f)

        val freeState = StudyTimerState(
            isActive = true,
            mode = "FREE",
            elapsedSeconds = 125
        )
        assertEquals(125, freeState.displaySeconds)
        assertEquals("٠٢:٠٥", freeState.formattedTime())
    }

    @Test
    fun protectionManager_exemptsDialerAlarmAndEmergencyAppsAlways() {
        val exempt = ProtectionManager.permanentExemptPackages
        assertTrue("تطبيق الاتصال يجب أن يكون مستثنى دائمًا", exempt.contains("com.samsung.android.dialer"))
        assertTrue("تطبيق الساعة والمنبه يجب أن يكون مستثنى دائمًا", exempt.contains("com.sec.android.app.clockpackage"))
        assertFalse("يوتيوب لا يجب أن يكون في قائمة الاستثناء الدائم", exempt.contains("com.google.android.youtube"))
    }
}
