package com.example.core.fullday

import com.example.data.LectureEntity
import java.util.UUID

object IcsCalendarParser {

    /**
     * يحلل ملفات التقويم القياسية (iCalendar .ics) ويستخرج المحاضرات منها
     * يدعم الحقول: SUMMARY, LOCATION, DTSTART, DTEND, DESCRIPTION
     */
    fun parseIcsToLectures(icsContent: String, reminderMinutes: Int = 15): List<LectureEntity> {
        val lectures = mutableListOf<LectureEntity>()
        val blocks = icsContent.split("BEGIN:VEVENT")
        if (blocks.size <= 1) return emptyList()

        for (i in 1 until blocks.size) {
            val eventBlock = blocks[i].substringBefore("END:VEVENT")
            val lines = eventBlock.lines().map { it.trim() }

            var summary = ""
            var location = "القاعة الجامعية"
            var courseCode = ""
            var startTime24 = "09:00"
            var endTime24 = "10:30"
            var dayOfWeek = ((i - 1) % 5) + 1 // 1=الأحد .. 5=الخميس

            for (line in lines) {
                when {
                    line.startsWith("SUMMARY") -> {
                        summary = line.substringAfter(":").trim()
                    }
                    line.startsWith("LOCATION") -> {
                        location = line.substringAfter(":").trim().ifBlank { "القاعة الجامعية" }
                    }
                    line.startsWith("DESCRIPTION") -> {
                        courseCode = line.substringAfter(":").trim().take(15)
                    }
                    line.startsWith("DTSTART") -> {
                        val raw = line.substringAfter(":").trim()
                        extractTime24(raw)?.let { startTime24 = it }
                        extractDayOfWeek1To5(raw)?.let { dayOfWeek = it }
                    }
                    line.startsWith("DTEND") -> {
                        val raw = line.substringAfter(":").trim()
                        extractTime24(raw)?.let { endTime24 = it }
                    }
                }
            }

            if (summary.isNotBlank()) {
                lectures.add(
                    LectureEntity(
                        id = UUID.randomUUID().toString(),
                        title = summary,
                        courseCode = courseCode.ifBlank { "ICS" },
                        location = location,
                        dayOfWeek = dayOfWeek,
                        startTime24 = startTime24,
                        endTime24 = endTime24,
                        reminderMinutesBefore = reminderMinutes,
                        isImportedFromIcs = true
                    )
                )
            }
        }
        return lectures
    }

    private fun extractTime24(dtValue: String): String? {
        // مثال: 20261012T083000 أو 20261012T083000Z
        val tPart = dtValue.substringAfter("T", "")
        if (tPart.length >= 4) {
            val hh = tPart.substring(0, 2).toIntOrNull()?.coerceIn(0, 23) ?: return null
            val mm = tPart.substring(2, 4).toIntOrNull()?.coerceIn(0, 59) ?: return null
            return String.format(java.util.Locale.US, "%02d:%02d", hh, mm)
        }
        return null
    }

    private fun extractDayOfWeek1To5(dtValue: String): Int? {
        val dPart = dtValue.substringBefore("T")
        if (dPart.length == 8) {
            val dayOfMonth = dPart.substring(6, 8).toIntOrNull() ?: return null
            return ((dayOfMonth % 5) + 1).coerceIn(1, 5)
        }
        return null
    }

    /**
     * حساب رقم الجزء (١ إلى ٣٠) بناءً على رقم الصفحة (١ إلى ٦٠٤) في مصحف المدينة المنورة (مجمع الملك فهد)
     */
    fun calculateJuzForMadinahPage(page: Int): Int {
        val clamped = page.coerceIn(1, 604)
        if (clamped <= 21) return 1
        if (clamped >= 582) return 30
        return (((clamped - 2) / 20) + 1).coerceIn(1, 30)
    }

    val sampleUniversityIcs = """
        BEGIN:VCALENDAR
        VERSION:2.0
        BEGIN:VEVENT
        SUMMARY:هندسة البرمجيات المتقدمة
        DESCRIPTION:SWE-312
        LOCATION:مبنى كلية الحاسب - قاعة 204
        DTSTART:20261012T100000
        DTEND:20261012T113000
        END:VEVENT
        BEGIN:VEVENT
        SUMMARY:أمن المعلومات والشبكات
        DESCRIPTION:CYS-401
        LOCATION:المعمل الرقمي B12
        DTSTART:20261014T130000
        DTEND:20261014T143000
        END:VEVENT
        END:VCALENDAR
    """.trimIndent()
}
