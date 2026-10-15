package com.example.core.prayer

import java.util.UUID

enum class PrayerBellMode(val id: String, val arabicTitle: String) {
    SILENT("SILENT", "صامت"),
    VIBRATE("VIBRATE", "اهتزاز"),
    AZAN("AZAN", "صوت أذان");

    companion object {
        fun fromId(id: String): PrayerBellMode = entries.firstOrNull { it.id == id } ?: AZAN
    }
}

data class MuazzinVoice(
    val id: String,
    val name: String,
    val description: String
)

object MuazzinCatalog {
    val allVoices = listOf(
        MuazzinVoice("makkah", "أذان المسجد الحرام بمكة المكرمة", "الشيخ علي أحمد ملا"),
        MuazzinVoice("madinah", "أذان المسجد النبوي الشريف", "الشيخ عصام بخاري"),
        MuazzinVoice("aqsa", "أذان المسجد الأقصى المبارك", "نداء الأقصى الشريف"),
        MuazzinVoice("afasy", "أذان الشيخ مشاري بن راشد العفاسي", "صوت شجي ومؤثر"),
        MuazzinVoice("abdulbasit", "أذان الشيخ عبدالباسط عبدالصمد", "نبرة كلاسيكية خاشعة"),
        MuazzinVoice("qetami", "أذان الشيخ ناصر القطامي", "تلاوة ونداء هادئ"),
        MuazzinVoice("takbeerat", "تكبيرات الأذان فقط", "تكبيرات قصيرة ومركزة"),
        MuazzinVoice("calm_chime", "تنبيه إسلامي هادئ", "نغمة روحانية بدون رفع الصوت")
    )

    fun findVoiceById(id: String): MuazzinVoice {
        return allVoices.firstOrNull { it.id == id } ?: allVoices.first()
    }
}

data class CustomPrayerAlarm(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val prayerIds: List<String> = listOf("FAJR", "DHUHR", "ASR", "MAGHRIB", "ISHA"),
    val isBeforeAzan: Boolean = true, // قبل أو بعد الأذان
    val offsetMinutes: Int = 10, // كم دقيقة قبل أو بعد الأذان
    val daysOfWeek: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7), // 1=الأحد ... 7=السبت
    val soundType: String = "تكبيرات الأذان",
    val isEnabled: Boolean = true
) {
    val prayersFormatted: String
        get() = prayerIds.mapNotNull { id ->
            PrayerType.entries.firstOrNull { it.id == id }?.arabicName
        }.joinToString("، ")

    val timingFormatted: String
        get() = if (isBeforeAzan) {
            "قبل الأذان بـ $offsetMinutes دقيقة"
        } else {
            "بعد الأذان بـ $offsetMinutes دقيقة"
        }

    val daysFormatted: String
        get() = if (daysOfWeek.size == 7) "كل يوم" else {
            val names = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
            daysOfWeek.mapNotNull { d -> names.getOrNull((d - 1).coerceIn(0, 6)) }.joinToString("، ")
        }
}
