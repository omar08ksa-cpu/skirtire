package com.example.core.prayer

import android.os.Build
import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

enum class PrayerType(
    val id: String,
    val arabicName: String,
    val isObligatory: Boolean = true
) {
    FAJR("FAJR", "الفجر", true),
    SUNRISE("SUNRISE", "الشروق", false),
    DHUHR("DHUHR", "الظهر", true),
    ASR("ASR", "العصر", true),
    MAGHRIB("MAGHRIB", "المغرب", true),
    ISHA("ISHA", "العشاء", true),
    MIDNIGHT("MIDNIGHT", "منتصف الليل", false),
    LAST_THIRD("LAST_THIRD", "الثلث الأخير", false);

    companion object {
        val obligatoryPrayers = listOf(FAJR, DHUHR, ASR, MAGHRIB, ISHA)
        val allDisplayPrayers = listOf(FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA)
        val allSchedulePrayers = listOf(FAJR, SUNRISE, DHUHR, ASR, MAGHRIB, ISHA, MIDNIGHT, LAST_THIRD)

        fun fromId(id: String): PrayerType = entries.firstOrNull { it.id == id } ?: FAJR
    }
}

data class CityLocation(
    val id: String,
    val arabicName: String,
    val regionName: String,
    val latitude: Double,
    val longitude: Double,
    val timeZoneOffsetHours: Double = 3.0
)

data class PrayerTimeSlot(
    val type: PrayerType,
    val hour24: Int,
    val minute: Int,
    val timeMillisToday: Long,
    val formatted12h: String
)

data class DailyPrayerSchedule(
    val dateKey: String,
    val hijriFormatted: String,
    val city: CityLocation,
    val qiblaDegreesFromNorth: Double,
    val prayers: List<PrayerTimeSlot>,
    val nextPrayer: PrayerTimeSlot,
    val previousPrayer: PrayerTimeSlot,
    val isNextPrayerTomorrowFajr: Boolean,
    val remainingSecondsToNext: Long,
    val progressRatio: Float = 0.5f,
    val isDaytime: Boolean = true,
    val midnightSlot: PrayerTimeSlot? = null,
    val lastThirdSlot: PrayerTimeSlot? = null
) {
    val countdownFormatted: String
        get() = UmmAlQuraCalculator.formatCountdown(remainingSecondsToNext)

    val qiblaBearingDegrees: Double
        get() = qiblaDegreesFromNorth
}

object UmmAlQuraCalculator {

    private const val KAABA_LAT = 21.422487
    private const val KAABA_LNG = 39.826206

    val saudiCities = listOf(
        CityLocation("riyadh", "الرياض", "منطقة الرياض", 24.7136, 46.6753, 3.0),
        CityLocation("makkah", "مكة المكرمة", "منطقة مكة المكرمة", 21.3891, 39.8579, 3.0),
        CityLocation("madinah", "المدينة المنورة", "منطقة المدينة المنورة", 24.5247, 39.5692, 3.0),
        CityLocation("jeddah", "جدة", "منطقة مكة المكرمة", 21.4858, 39.1925, 3.0),
        CityLocation("dammam", "الدمام", "المنطقة الشرقية", 26.4207, 50.0888, 3.0),
        CityLocation("khobar", "الخبر", "المنطقة الشرقية", 26.2172, 50.1971, 3.0),
        CityLocation("abha", "أبها", "منطقة عسير", 18.2164, 42.5053, 3.0),
        CityLocation("buraidah", "بريدة", "منطقة القصيم", 26.3260, 43.9750, 3.0),
        CityLocation("tabuk", "تبوك", "منطقة تبوك", 28.3838, 36.5550, 3.0),
        CityLocation("hail", "حائل", "منطقة حائل", 27.5114, 41.7208, 3.0),
        CityLocation("taif", "الطائف", "منطقة مكة المكرمة", 21.2703, 40.4158, 3.0),
        CityLocation("jizan", "جازان", "منطقة جازان", 16.8892, 42.5511, 3.0),
        CityLocation("hofuf", "الأحساء (الهفوف)", "المنطقة الشرقية", 25.3800, 49.5888, 3.0),
        CityLocation("najran", "نجران", "منطقة نجران", 17.4924, 44.1277, 3.0),
        CityLocation("yanbu", "ينبع", "منطقة المدينة المنورة", 24.0895, 38.0637, 3.0),
        CityLocation("jouf", "الجوف (سكاكا)", "منطقة الجوف", 29.9697, 40.2064, 3.0),
        CityLocation("arar", "عرعر", "منطقة الحدود الشمالية", 30.9753, 41.0381, 3.0),
        CityLocation("baha", "الباحة", "منطقة الباحة", 20.0129, 41.4677, 3.0)
    )

    val worldCities = listOf(
        CityLocation("jerusalem", "القدس الشريف", "فلسطين", 31.7683, 35.2137, 2.0),
        CityLocation("cairo", "القاهرة", "مصر", 30.0444, 31.2357, 2.0),
        CityLocation("dubai", "دبي", "الإمارات العربية المتحدة", 25.2048, 55.2708, 4.0),
        CityLocation("abudhabi", "أبوظبي", "الإمارات العربية المتحدة", 24.4539, 54.3773, 4.0),
        CityLocation("doha", "الدوحة", "قطر", 25.2854, 51.5310, 3.0),
        CityLocation("kuwait", "الكويت", "دولة الكويت", 29.3759, 47.9774, 3.0),
        CityLocation("manama", "المنامة", "مملكة البحرين", 26.2285, 50.5860, 3.0),
        CityLocation("muscat", "مسقط", "سلطنة عمان", 23.5880, 58.3829, 4.0),
        CityLocation("amman", "عمّان", "الأردن", 31.9454, 35.9284, 3.0),
        CityLocation("beirut", "بيروت", "لبنان", 33.8938, 35.5018, 2.0),
        CityLocation("baghdad", "بغداد", "العراق", 33.3152, 44.3661, 3.0),
        CityLocation("istanbul", "إسطنبول", "تركيا", 41.0082, 28.9784, 3.0),
        CityLocation("jakarta", "جاكرتا", "إندونيسيا", -6.2088, 106.8456, 7.0),
        CityLocation("kualalumpur", "كوالالمبور", "ماليزيا", 3.1390, 101.6869, 8.0),
        CityLocation("london", "لندن", "المملكة المتحدة", 51.5074, -0.1278, 0.0),
        CityLocation("paris", "باريس", "فرنسا", 48.8566, 2.3522, 1.0)
    )

    val allCities: List<CityLocation> = saudiCities + worldCities

    fun findCityById(id: String): CityLocation {
        return allCities.firstOrNull { it.id == id } ?: saudiCities.first()
    }

    /**
     * حساب مواقيت الصلاة لليوم ولحظة الآن مع حساب المنحنى السماوي والصلاتين السابقة والتالية
     */
    fun calculateScheduleForNow(
        city: CityLocation,
        nowMillis: Long = System.currentTimeMillis()
    ): DailyPrayerSchedule {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+03:00")).apply {
            timeInMillis = nowMillis
        }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)

        return calculateScheduleForDate(city, year, month, day, nowMillis)
    }

    /**
     * حساب مواقيت الصلاة لأي تاريخ مستقبلي أو ماضٍ في التقويم (التقويم الكامل)
     */
    fun calculateScheduleForDate(
        city: CityLocation,
        year: Int,
        month: Int,
        day: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): DailyPrayerSchedule {
        val dateKey = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
        val isRamadan = isHijriRamadan(year, month, day)
        val baseSlots = computeDayPrayerSlots(year, month, day, city, isRamadan)

        // حساب مواقيت اليوم التالي لحساب صلاة الفجر الغد ومنتصف الليل والثلث الأخير
        val nextDayCal = Calendar.getInstance(TimeZone.getTimeZone("GMT+03:00")).apply {
            set(year, month - 1, day)
            add(Calendar.DAY_OF_MONTH, 1)
        }
        val tYear = nextDayCal.get(Calendar.YEAR)
        val tMonth = nextDayCal.get(Calendar.MONTH) + 1
        val tDay = nextDayCal.get(Calendar.DAY_OF_MONTH)
        val tomorrowSlots = computeDayPrayerSlots(
            tYear,
            tMonth,
            tDay,
            city,
            isHijriRamadan(tYear, tMonth, tDay)
        )
        val tomorrowFajr = tomorrowSlots.first { it.type == PrayerType.FAJR }

        // حساب مواقيت اليوم السابق لحساب صلاة العشاء السابقة إن كنا قبل فجر اليوم
        val prevDayCal = Calendar.getInstance(TimeZone.getTimeZone("GMT+03:00")).apply {
            set(year, month - 1, day)
            add(Calendar.DAY_OF_MONTH, -1)
        }
        val pYear = prevDayCal.get(Calendar.YEAR)
        val pMonth = prevDayCal.get(Calendar.MONTH) + 1
        val pDay = prevDayCal.get(Calendar.DAY_OF_MONTH)
        val yesterdaySlots = computeDayPrayerSlots(
            pYear,
            pMonth,
            pDay,
            city,
            isHijriRamadan(pYear, pMonth, pDay)
        )
        val yesterdayIsha = yesterdaySlots.first { it.type == PrayerType.ISHA }

        val maghribSlot = baseSlots.first { it.type == PrayerType.MAGHRIB }
        val nightDurationMillis = (tomorrowFajr.timeMillisToday - maghribSlot.timeMillisToday).coerceAtLeast(1000L)

        // منتصف الليل الشرعي: منتصف الوقت بين المغرب والفجر
        val midnightMillis = maghribSlot.timeMillisToday + (nightDurationMillis / 2)
        val midnightSlot = buildSlotFromMillis(midnightMillis, PrayerType.MIDNIGHT)

        // الثلث الأخير من الليل: يبدأ عند انقضاء ثلثي الليل من المغرب (أي قبل الفجر بثلث الليل)
        val lastThirdMillis = maghribSlot.timeMillisToday + (nightDurationMillis * 2 / 3)
        val lastThirdSlot = buildSlotFromMillis(lastThirdMillis, PrayerType.LAST_THIRD)

        val fullSlots = baseSlots + listOf(midnightSlot, lastThirdSlot)

        // تحديد الصلوات الخمس الإلزامية للمقارنة
        val obligatorySlots = baseSlots.filter { it.type.isObligatory }
        val upcomingToday = obligatorySlots.firstOrNull { it.timeMillisToday > nowMillis }

        val nextSlot: PrayerTimeSlot
        val prevSlot: PrayerTimeSlot
        val isTomorrowFajr: Boolean

        if (upcomingToday != null) {
            nextSlot = upcomingToday
            isTomorrowFajr = false
            val idx = obligatorySlots.indexOf(upcomingToday)
            prevSlot = if (idx > 0) {
                obligatorySlots[idx - 1]
            } else {
                yesterdayIsha
            }
        } else {
            nextSlot = tomorrowFajr
            prevSlot = obligatorySlots.last { it.type == PrayerType.ISHA }
            isTomorrowFajr = true
        }

        val remainingSec = ((nextSlot.timeMillisToday - nowMillis) / 1000L).coerceAtLeast(0L)
        val totalWindow = (nextSlot.timeMillisToday - prevSlot.timeMillisToday).coerceAtLeast(1000L)
        val elapsed = (nowMillis - prevSlot.timeMillisToday).coerceAtLeast(0L)
        val progressRatio = (elapsed.toDouble() / totalWindow.toDouble()).toFloat().coerceIn(0f, 1f)

        // هل الوقت نهار أم ليل؟
        // النهار: من وقت الفجر حتى أذان المغرب
        val fajrSlot = baseSlots.first { it.type == PrayerType.FAJR }
        val isDaytime = nowMillis in fajrSlot.timeMillisToday until maghribSlot.timeMillisToday

        val qiblaAngle = calculateQiblaAngle(city.latitude, city.longitude)

        return DailyPrayerSchedule(
            dateKey = dateKey,
            hijriFormatted = formatHijriDate(year, month, day),
            city = city,
            qiblaDegreesFromNorth = qiblaAngle,
            prayers = baseSlots,
            nextPrayer = nextSlot,
            previousPrayer = prevSlot,
            isNextPrayerTomorrowFajr = isTomorrowFajr,
            remainingSecondsToNext = remainingSec,
            progressRatio = progressRatio,
            isDaytime = isDaytime,
            midnightSlot = midnightSlot,
            lastThirdSlot = lastThirdSlot
        )
    }

    private fun computeDayPrayerSlots(
        year: Int,
        month: Int,
        day: Int,
        city: CityLocation,
        isRamadan: Boolean
    ): List<PrayerTimeSlot> {
        val jd = julianDate(year, month, day) - city.longitude / (15.0 * 24.0)
        val (declination, eqTime) = sunPosition(jd + 0.5)

        val lat = city.latitude
        val tz = city.timeZoneOffsetHours

        val noon = 12.0 + tz - (city.longitude / 15.0) - eqTime
        val dhuhrHours = noon + (2.0 / 60.0)

        val fajrArc = hourAngleForAltitude(-18.5, lat, declination)
        val sunriseArc = hourAngleForAltitude(-0.833, lat, declination)
        val asrArc = asrHourAngle(1.0, lat, declination)

        val fajrHours = noon - fajrArc
        val sunriseHours = noon - sunriseArc
        val asrHours = noon + asrArc
        val maghribHours = noon + sunriseArc + (2.0 / 60.0)
        val ishaOffsetHours = if (isRamadan) 2.0 else 1.5
        val ishaHours = maghribHours + ishaOffsetHours

        val rawMap = listOf(
            PrayerType.FAJR to fajrHours,
            PrayerType.SUNRISE to sunriseHours,
            PrayerType.DHUHR to dhuhrHours,
            PrayerType.ASR to asrHours,
            PrayerType.MAGHRIB to maghribHours,
            PrayerType.ISHA to ishaHours
        )

        return rawMap.map { (type, fractionalHours) ->
            buildSlot(year, month, day, type, fractionalHours)
        }
    }

    private fun buildSlot(
        year: Int,
        month: Int,
        day: Int,
        type: PrayerType,
        fractionalHours: Double
    ): PrayerTimeSlot {
        val normalized = ((fractionalHours % 24.0) + 24.0) % 24.0
        val totalMinutes = (normalized * 60.0).roundToInt()
        val hour24 = (totalMinutes / 60) % 24
        val minute = totalMinutes % 60

        val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+03:00")).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour24)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val period = if (hour24 < 12) "ص" else "م"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        val formatted = String.format(Locale.US, "%d:%02d %s", hour12, minute, period)

        return PrayerTimeSlot(
            type = type,
            hour24 = hour24,
            minute = minute,
            timeMillisToday = cal.timeInMillis,
            formatted12h = toArabicDigits(formatted)
        )
    }

    private fun buildSlotFromMillis(timeMillis: Long, type: PrayerType): PrayerTimeSlot {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+03:00")).apply {
            this.timeInMillis = timeMillis
        }
        val hour24 = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val period = if (hour24 < 12) "ص" else "م"
        val hour12 = when {
            hour24 == 0 -> 12
            hour24 > 12 -> hour24 - 12
            else -> hour24
        }
        val formatted = String.format(Locale.US, "%d:%02d %s", hour12, minute, period)
        return PrayerTimeSlot(
            type = type,
            hour24 = hour24,
            minute = minute,
            timeMillisToday = timeMillis,
            formatted12h = toArabicDigits(formatted)
        )
    }

    fun calculateQiblaAngle(latitude: Double, longitude: Double): Double {
        val latRad = Math.toRadians(latitude)
        val lngRad = Math.toRadians(longitude)
        val kaabaLatRad = Math.toRadians(KAABA_LAT)
        val kaabaLngRad = Math.toRadians(KAABA_LNG)

        val deltaLng = kaabaLngRad - lngRad
        val y = sin(deltaLng)
        val x = cos(latRad) * tan(kaabaLatRad) - sin(latRad) * cos(deltaLng)
        val bearing = Math.toDegrees(atan2(y, x))
        return (bearing + 360.0) % 360.0
    }

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2.0 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun sunPosition(jd: Double): Pair<Double, Double> {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2.0 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = datan2(dcos(e) * dsin(l), dcos(l)) / 15.0
        val eqTime = q / 15.0 - fixHour(ra)
        val declination = dasin(dsin(e) * dsin(l))
        return declination to eqTime
    }

    private fun hourAngleForAltitude(altitudeDeg: Double, latDeg: Double, declDeg: Double): Double {
        val num = dsin(altitudeDeg) - dsin(latDeg) * dsin(declDeg)
        val den = dcos(latDeg) * dcos(declDeg)
        val ratio = (num / den).coerceIn(-1.0, 1.0)
        return dacos(ratio) / 15.0
    }

    private fun asrHourAngle(shadowFactor: Double, latDeg: Double, declDeg: Double): Double {
        val angleRad = atan(1.0 / (shadowFactor + tan(Math.toRadians(abs(latDeg - declDeg)))))
        val altitudeDeg = Math.toDegrees(angleRad)
        return hourAngleForAltitude(altitudeDeg, latDeg, declDeg)
    }

    private fun fixAngle(a: Double): Double {
        val r = a % 360.0
        return if (r < 0) r + 360.0 else r
    }

    private fun fixHour(a: Double): Double {
        val r = a % 24.0
        return if (r < 0) r + 24.0 else r
    }

    private fun dsin(d: Double): Double = sin(Math.toRadians(d))
    private fun dcos(d: Double): Double = cos(Math.toRadians(d))
    private fun dasin(x: Double): Double = Math.toDegrees(asin(x.coerceIn(-1.0, 1.0)))
    private fun dacos(x: Double): Double = Math.toDegrees(acos(x.coerceIn(-1.0, 1.0)))
    private fun datan2(y: Double, x: Double): Double = Math.toDegrees(atan2(y, x))

    private fun isHijriRamadan(year: Int, month: Int, day: Int): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val hijri = HijrahDate.from(LocalDate.of(year, month, day))
                hijri.get(ChronoField.MONTH_OF_YEAR) == 9
            } catch (_: Exception) {
                false
            }
        } else {
            false
        }
    }

    fun formatHijriDate(year: Int, month: Int, day: Int): String {
        val hijriMonths = listOf(
            "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
            "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
            "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
        )
        val weekDays = listOf(
            "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"
        )
        val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+03:00")).apply {
            set(year, month - 1, day)
        }
        val dayOfWeekName = weekDays[(cal.get(Calendar.DAY_OF_WEEK) - 1).coerceIn(0, 6)]

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val hijri = HijrahDate.from(LocalDate.of(year, month, day))
                val hDay = hijri.get(ChronoField.DAY_OF_MONTH)
                val hMonth = hijri.get(ChronoField.MONTH_OF_YEAR)
                val hYear = hijri.get(ChronoField.YEAR)
                val mName = hijriMonths.getOrElse(hMonth - 1) { "رمضان" }
                return toArabicDigits("$dayOfWeekName، $hDay $mName $hYear هـ")
            } catch (_: Exception) {
            }
        }
        return toArabicDigits("$dayOfWeekName، $day/$month/$year م")
    }

    fun formatCountdown(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        val raw = String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        return toArabicDigits(raw)
    }

    fun toArabicDigits(number: Number): String {
        return toArabicDigits(number.toString())
    }

    fun toArabicDigits(input: String): String {
        val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        val sb = StringBuilder(input.length)
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(arabicDigits[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun todayDateKey(nowMillis: Long = System.currentTimeMillis()): String {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+03:00")).apply {
            timeInMillis = nowMillis
        }
        return String.format(
            Locale.US,
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun lastNDaysDateKeys(daysCount: Int, nowMillis: Long = System.currentTimeMillis()): List<String> {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+03:00")).apply {
            timeInMillis = nowMillis
        }
        val list = mutableListOf<String>()
        for (i in (daysCount - 1) downTo 0) {
            val c = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, -i) }
            list.add(
                String.format(
                    Locale.US,
                    "%04d-%02d-%02d",
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH) + 1,
                    c.get(Calendar.DAY_OF_MONTH)
                )
            )
        }
        return list
    }
}
