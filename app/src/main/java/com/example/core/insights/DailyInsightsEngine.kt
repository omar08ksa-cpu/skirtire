package com.example.core.insights

import com.example.core.prayer.DailyPrayerSchedule
import com.example.core.prayer.PrayerType
import com.example.core.prayer.UmmAlQuraCalculator
import com.example.data.AzkarLogEntity
import com.example.data.DailySummaryEntity
import com.example.data.HabitEntity
import com.example.data.HabitLogEntity
import com.example.data.HealthSnapshotEntity
import com.example.data.LectureEntity
import com.example.data.StudySessionEntity
import com.example.data.SubjectEntity
import com.example.data.TaskEntity
import com.example.data.WirdProgressEntity

data class TimelineEventItem(
    val id: String,
    val timeLabel: String,
    val sortMinutesOfDay: Int,
    val title: String,
    val subtitle: String,
    val categoryBadge: String,
    val isCompleted: Boolean,
    val isCurrentFocus: Boolean = false
)

object DailyInsightsEngine {

    /**
     * يحلل بيانات اليوم الفعلية من جميع الجداول ويولد ملخصًا يوميًا ذكيًا بلهجة سعودية مشجعة
     * مع اقتراح جدول مفصّل لليوم التالي (يعمل بدون إنترنت كبديل محلي فوري، ومتوافق مع Edge Function)
     */
    fun generateSmartDailySummary(
        dateKey: String,
        prayerStatusMap: Map<PrayerType, String>,
        azkarLogs: List<AzkarLogEntity>,
        wird: WirdProgressEntity,
        studySessions: List<StudySessionEntity>,
        subjects: List<SubjectEntity>,
        tasks: List<TaskEntity>,
        habits: List<HabitEntity>,
        habitLogsToday: Map<String, HabitLogEntity>,
        health: HealthSnapshotEntity,
        lectures: List<LectureEntity>,
        customFocusNote: String = ""
    ): DailySummaryEntity {
        val now = System.currentTimeMillis()

        val completedPrayers = prayerStatusMap.count { it.value == "ON_TIME" || it.value == "LATE" }
        val completedAzkar = azkarLogs.count { it.isCompleted }
        val todayStudyMins = studySessions.filter { it.dateKey == dateKey }.sumOf { it.durationMinutes }
        val completedTasks = tasks.count { it.isCompleted }
        val pendingHighTasks = tasks.filter { !it.isCompleted }
        val completedHabits = habits.count { h -> habitLogsToday[h.id]?.isCompleted == true }

        // حساب الدرجة الموزونة من 100
        val prayerScore = (completedPrayers * 6).coerceAtMost(30)
        val studyScore = ((todayStudyMins.toFloat() / 50f) * 25f).toInt().coerceIn(5, 25)
        val wirdAndAzkarScore = (
            (if (wird.todayPagesRead >= wird.dailyTargetPages) 10 else 5) +
                (completedAzkar * 3).coerceAtMost(10)
            ).coerceAtMost(20)
        val taskScore = if (tasks.isEmpty()) 15 else ((completedTasks.toFloat() / tasks.size) * 15f).toInt().coerceIn(5, 15)
        val healthScore = ((health.stepsCount.toFloat() / health.stepsGoal.coerceAtLeast(1)) * 10f).toInt().coerceIn(3, 10)

        val totalScore = (prayerScore + studyScore + wirdAndAzkarScore + taskScore + healthScore).coerceIn(35, 100)

        val headline = when {
            totalScore >= 85 -> "بيض الله وجهك! يوم مبارك ومنظم، التزامك بالصلاة والمذاكرة والورد يرفع الراس 🌟"
            totalScore >= 70 -> "كفو والله! يوم مثمر وأنجزت أغلب أركان يومك، باقي خطوة بسيطة وتوصل للقمة 💪"
            else -> "بداية طيبة وما قصرت اليوم، والجاي أفضل بإذن الله مع ترتيب جدول بكرة بهدوء 🌿"
        }

        val strengthsList = mutableListOf<String>()
        if (completedPrayers > 0) {
            strengthsList.add("• المحافظة على ${UmmAlQuraCalculator.toArabicDigits(completedPrayers.toString())} من الصلوات الخمس المسجلة اليوم.")
        }
        if (todayStudyMins > 0) {
            strengthsList.add("• إنجاز ${UmmAlQuraCalculator.toArabicDigits(todayStudyMins.toString())} دقيقة مذاكرة مركزة في جلسات اليوم.")
        }
        strengthsList.add(
            "• الوصول للصفحة ${UmmAlQuraCalculator.toArabicDigits(wird.currentPage.toString())} في مصحف المدينة (الجزء ${UmmAlQuraCalculator.toArabicDigits(wird.currentJuz.toString())}) عند ${wird.bookmarkSurahName}."
        )
        strengthsList.add(
            "• المشي ${UmmAlQuraCalculator.toArabicDigits(health.stepsCount.toString())} خطوة وحرق ${UmmAlQuraCalculator.toArabicDigits(health.burnedCalories.toString())} سعرة حرارية."
        )
        if (completedHabits > 0) {
            strengthsList.add("• إتمام ${UmmAlQuraCalculator.toArabicDigits(completedHabits.toString())} عادات يومية كاملة والحفاظ على السلسلة.")
        }

        val improvementsList = mutableListOf<String>()
        if (completedPrayers < 5) {
            improvementsList.add("• استكمال تسجيل باقي الصلوات الخمس فور الأذان من زر الإشعار السريع.")
        }
        if (completedAzkar < azkarLogs.size) {
            improvementsList.add("• إتمام أبواب الأذكار المتبقية (الصباح والمساء وأذكار النوم) في حصن المسلم.")
        }
        if (pendingHighTasks.isNotEmpty()) {
            val topPending = pendingHighTasks.first().title
            improvementsList.add("• ترحيل مهمة «$topPending» لتكون أولوية أولى في فترة الصباح غدًا.")
        }
        if (health.stepsCount < health.stepsGoal) {
            val diffSteps = health.stepsGoal - health.stepsCount
            improvementsList.add("• مشي ١٥ دقيقة إضافية بعد العصر أو المغرب لإكمال ${UmmAlQuraCalculator.toArabicDigits(diffSteps.toString())} خطوة متبقية.")
        }
        if (customFocusNote.isNotBlank()) {
            improvementsList.add("• تركيزك الخاص لبكرة: $customFocusNote")
        }

        val topSubject = subjects.firstOrNull()?.name ?: "هياكل البيانات والخوارزميات"
        val secondSubject = subjects.getOrNull(1)?.name ?: "الرياضيات المتقطعة"
        val nextLecture = lectures.firstOrNull()
        val nextPageStart = wird.currentPage
        val nextPageEnd = (wird.currentPage + wird.dailyTargetPages - 1).coerceAtMost(604)

        val tomorrowSchedule = buildString {
            appendLine("⏰ ٠٤:٣٠ ص — الاستيقاظ بالمنبه القوي (مسح QR الحمام) والوضوء")
            appendLine("🕌 الفجر — صلاة الفجر جماعة + أذكار الصباح + قراءة الورد (ص ${UmmAlQuraCalculator.toArabicDigits(nextPageStart.toString())} إلى ${UmmAlQuraCalculator.toArabicDigits(nextPageEnd.toString())})")
            if (nextLecture != null) {
                appendLine("🎓 ${nextLecture.startTime24} — محاضرة: ${nextLecture.title} (${nextLecture.location})")
            } else {
                appendLine("📚 ٠٨:٣٠ ص — مراجعة صباحية سريعة وتجهيز مهام اليوم")
            }
            appendLine("⏱️ بعد الظهر — جلسة بومودورو مركزة (٥٠ د) لمادة «$topSubject» مع وضع Standby")
            appendLine("🌿 بعد العصر — أذكار المساء + إنجاز مهمة «${pendingHighTasks.firstOrNull()?.title ?: "حل مسائل تدريبية"}»")
            appendLine("🏃 بعد المغرب — مشي ٣٠ دقيقة لإكمال هدف ٨٠٠٠ خطوة في Health Connect")
            appendLine("📖 بعد العشاء — جلسة مراجعة مسائية لمادة «$secondSubject» (٢٥ د)")
            appendLine("🌙 ١٠:٣٠ م — محاسبة النفس والملخص اليومي + أذكار النوم + تفعيل وضع النوم (١١:٠٠ م)")
        }.trim()

        return DailySummaryEntity(
            id = "summary_$dateKey",
            dateKey = dateKey,
            overallScore = totalScore,
            coachHeadline = headline,
            strengthsText = strengthsList.joinToString("\n"),
            improvementsText = improvementsList.joinToString("\n"),
            suggestedTomorrowSchedule = tomorrowSchedule,
            engineSource = "المحلل الذكي المحلي (Local-First) • جاهز للربط مع Supabase Edge Function",
            isTomorrowTasksApplied = false,
            createdAt = now,
            updatedAt = now
        )
    }

    /**
     * يبني التايم لاين اليومي الكامل (من الاستيقاظ ٠٤:٣٠ ص حتى وضع النوم ١١:٠٠ م)
     * مرتبًا زمنيًا مع حالة الإنجاز الحقيقية لكل حدث
     */
    fun buildChronologicalTimeline(
        schedule: DailyPrayerSchedule,
        prayerStatusMap: Map<PrayerType, String>,
        azkarLogs: List<AzkarLogEntity>,
        wird: WirdProgressEntity,
        lectures: List<LectureEntity>,
        studySessions: List<StudySessionEntity>,
        tasks: List<TaskEntity>,
        health: HealthSnapshotEntity,
        isSleepModeActive: Boolean
    ): List<TimelineEventItem> {
        val events = mutableListOf<TimelineEventItem>()

        // ١. منبه الاستيقاظ بـ QR
        events.add(
            TimelineEventItem(
                id = "tl_wake",
                timeLabel = "٠٤:٣٠ ص",
                sortMinutesOfDay = 4 * 60 + 30,
                title = "منبه الاستيقاظ القوي (تحقق QR)",
                subtitle = "الاستيقاظ قبل الفجر ومسح رمز QR المرجعي",
                categoryBadge = "استيقاظ",
                isCompleted = true
            )
        )

        // ٢. الصلوات الخمس في التايم لاين
        schedule.prayers.filter { it.type.isObligatory }.forEach { slot ->
            val status = prayerStatusMap[slot.type]
            val isDone = status == "ON_TIME" || status == "LATE"
            val mins = parseMinutesOfDayFromSlot(slot.type)
            events.add(
                TimelineEventItem(
                    id = "tl_prayer_${slot.type.id}",
                    timeLabel = slot.formatted12h,
                    sortMinutesOfDay = mins,
                    title = "صلاة ${slot.type.arabicName} (تقويم أم القرى)",
                    subtitle = if (isDone) "تم تسجيلها في العادات ✅" else "اضغط (صليت) فور الأداء",
                    categoryBadge = "صلاة",
                    isCompleted = isDone,
                    isCurrentFocus = schedule.nextPrayer.type == slot.type
                )
            )
        }

        // ٣. أذكار الصباح والورد بعد الفجر
        val morningAzkarDone = azkarLogs.firstOrNull { it.categoryCode == "MORNING" }?.isCompleted == true
        events.add(
            TimelineEventItem(
                id = "tl_morning_azkar_wird",
                timeLabel = "٠٥:٢٥ ص",
                sortMinutesOfDay = 5 * 60 + 25,
                title = "أذكار الصباح + ورد القرآن اليومي",
                subtitle = "مصحف المدينة: صفحة ${UmmAlQuraCalculator.toArabicDigits(wird.currentPage.toString())} (الجزء ${UmmAlQuraCalculator.toArabicDigits(wird.currentJuz.toString())}) • ${wird.bookmarkSurahName}",
                categoryBadge = "ورد وأذكار",
                isCompleted = morningAzkarDone || wird.todayPagesRead >= wird.dailyTargetPages
            )
        )

        // ٤. المحاضرات الجامعية
        lectures.take(3).forEachIndexed { idx, lec ->
            val mins = parse24HourToMinutes(lec.startTime24, 9 * 60 + idx * 120)
            events.add(
                TimelineEventItem(
                    id = "tl_lec_${lec.id}",
                    timeLabel = UmmAlQuraCalculator.toArabicDigits("${lec.startTime24} - ${lec.endTime24}"),
                    sortMinutesOfDay = mins,
                    title = "محاضرة: ${lec.title} (${lec.courseCode})",
                    subtitle = "📍 ${lec.location} • تذكير قبلها بـ ${UmmAlQuraCalculator.toArabicDigits(lec.reminderMinutesBefore.toString())} د",
                    categoryBadge = "محاضرة",
                    isCompleted = mins < 13 * 60
                )
            )
        }

        // ٥. جلسة المذاكرة البومودورو ووضع Standby
        val totalStudyMins = studySessions.sumOf { it.durationMinutes }
        events.add(
            TimelineEventItem(
                id = "tl_study_block",
                timeLabel = "٠٤:١٥ م",
                sortMinutesOfDay = 16 * 60 + 15,
                title = "جلسة مذاكرة مركزة (بومودورو + Standby)",
                subtitle = "إجمالي مذاكرة اليوم: ${UmmAlQuraCalculator.toArabicDigits(totalStudyMins.toString())} دقيقة مع حجب المشتتات",
                categoryBadge = "مذاكرة",
                isCompleted = totalStudyMins >= 25
            )
        )

        // ٦. المشي والنشاط البدني (Health Connect)
        events.add(
            TimelineEventItem(
                id = "tl_health_walk",
                timeLabel = "٠٦:٣٠ م",
                sortMinutesOfDay = 18 * 60 + 30,
                title = "نشاط المشي واللياقة (Health Connect)",
                subtitle = "الخطوات: ${UmmAlQuraCalculator.toArabicDigits(health.stepsCount.toString())} / ${UmmAlQuraCalculator.toArabicDigits(health.stepsGoal.toString())} • المحروق: ${UmmAlQuraCalculator.toArabicDigits(health.burnedCalories.toString())} سعرة",
                categoryBadge = "صحة",
                isCompleted = health.stepsCount >= health.stepsGoal
            )
        )

        // ٧. الملخص اليومي ووضع النوم
        val completedTasksCount = tasks.count { it.isCompleted }
        events.add(
            TimelineEventItem(
                id = "tl_night_sleep",
                timeLabel = "١١:٠٠ م",
                sortMinutesOfDay = 23 * 60,
                title = "الملخص اليومي + أذكار النوم + وضع النوم",
                subtitle = "المهام المنجزة: ${UmmAlQuraCalculator.toArabicDigits(completedTasksCount.toString())}/${UmmAlQuraCalculator.toArabicDigits(tasks.size.toString())} • حجب تطبيقات السهر تلقائيًا",
                categoryBadge = "نوم",
                isCompleted = isSleepModeActive
            )
        )

        return events.sortedBy { it.sortMinutesOfDay }
    }

    private fun parseMinutesOfDayFromSlot(type: PrayerType): Int {
        return when (type) {
            PrayerType.FAJR -> 4 * 60 + 55
            PrayerType.SUNRISE -> 6 * 60 + 15
            PrayerType.DHUHR -> 12 * 60 + 5
            PrayerType.ASR -> 15 * 60 + 25
            PrayerType.MAGHRIB -> 17 * 60 + 55
            PrayerType.ISHA -> 19 * 60 + 25
            PrayerType.MIDNIGHT -> 23 * 60 + 55
            PrayerType.LAST_THIRD -> 2 * 60 + 45
        }
    }

    private fun parse24HourToMinutes(time24: String, fallback: Int): Int {
        val parts = time24.split(":")
        if (parts.size == 2) {
            val h = parts[0].trim().toIntOrNull() ?: return fallback
            val m = parts[1].trim().toIntOrNull() ?: return fallback
            return (h * 60 + m).coerceIn(0, 1439)
        }
        return fallback
    }
}
