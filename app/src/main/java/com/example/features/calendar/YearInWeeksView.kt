package com.example.features.calendar

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.prayer.UmmAlQuraCalculator
import com.example.data.AppEventEntity
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.MintAccent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * طريقة العرض الأولى الجديدة: عرض السنة كاملة على شكل مربعات أسابيع (٥٢ أسبوعاً)
 * يعرف المستخدم كم مشى أسبوع من السنة، وكم باقي، والأسبوع الحالي بدقة، مع الأحداث المرتبطة بكل أسبوع.
 */
@Composable
fun YearInWeeksView(
    appEvents: List<AppEventEntity>,
    onSelectDate: (year: Int, month: Int, day: Int) -> Unit,
    onAddEventForDate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentCal = remember { Calendar.getInstance() }
    val currentYear = currentCal.get(Calendar.YEAR)
    val currentWeekOfYear = currentCal.get(Calendar.WEEK_OF_YEAR)

    var selectedYear by remember { mutableIntStateOf(currentYear) }
    var selectedWeekNum by remember { mutableIntStateOf(currentWeekOfYear) }

    // حساب إحصائيات السنة المختارة
    val isCurrentYear = selectedYear == currentYear
    val weeksPassed = if (isCurrentYear) currentWeekOfYear else if (selectedYear < currentYear) 52 else 0
    val weeksRemaining = (52 - weeksPassed).coerceAtLeast(0)
    val percentElapsed = ((weeksPassed.toFloat() / 52f) * 100f).toInt().coerceIn(0, 100)

    // حساب نطاق تواريخ الأسبوع المختار
    val weekDateRange = remember(selectedYear, selectedWeekNum) {
        getWeekDateRange(selectedYear, selectedWeekNum)
    }

    // فلترة الأحداث التي تقع في هذا الأسبوع
    val eventsInSelectedWeek = remember(appEvents, weekDateRange) {
        appEvents.filter { event ->
            event.dateKey in weekDateRange.allDatesInWeek
        }
    }

    // ماب يوضح أي الأسابيع تحتوي على أحداث
    val weeksWithEvents = remember(appEvents, selectedYear) {
        val weekSet = mutableSetOf<Int>()
        appEvents.forEach { event ->
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val date = sdf.parse(event.dateKey)
                if (date != null) {
                    val cal = Calendar.getInstance().apply { time = date }
                    if (cal.get(Calendar.YEAR) == selectedYear) {
                        weekSet.add(cal.get(Calendar.WEEK_OF_YEAR))
                    }
                }
            } catch (_: Exception) {}
        }
        weekSet
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("year_in_weeks_content"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. بطاقة اختيار السنة وشريط التقدم
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // مبدل السنة
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectedYear++ }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "السنة القادمة")
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "سنة ${UmmAlQuraCalculator.toArabicDigits(selectedYear.toString())}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "مربعات أسابيع السنة الـ ٥٢",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { selectedYear-- }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "السنة السابقة")
                        }
                    }

                    // شريط نسبة انقضاء السنة
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = UmmAlQuraCalculator.toArabicDigits("انقضى: $weeksPassed أسبوعاً ($percentElapsed%)"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = UmmAlQuraCalculator.toArabicDigits("متبقي: $weeksRemaining أسبوعاً"),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        LinearProgressIndicator(
                            progress = { percentElapsed / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }

                    // إحصائيات سريعة
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        YearStatBadge(
                            title = "الأسبوع الحالي",
                            value = if (isCurrentYear) "$currentWeekOfYear" else "-",
                            isHighlight = true
                        )
                        YearStatBadge(
                            title = "مضى من السنة",
                            value = "$weeksPassed أسبوع",
                            isHighlight = false
                        )
                        YearStatBadge(
                            title = "باقي لنهايتها",
                            value = "$weeksRemaining أسبوع",
                            isHighlight = false
                        )
                    }
                }
            }
        }

        // 2. شبكة مربعات الـ ٥٢ أسبوعاً
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ViewWeek,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "خريطة الـ ٥٢ أسبوعاً",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // دليل الألوان
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ColorLegendItem(color = EmeraldDeep, label = "مضى")
                            ColorLegendItem(color = GoldWarm, label = "الحالي")
                            ColorLegendItem(color = MaterialTheme.colorScheme.surfaceVariant, label = "قادم")
                        }
                    }

                    // تقسيم الأسابيع إلى ٤ أرباع (Q1, Q2, Q3, Q4) - ١٣ أسبوع لكل ربع
                    listOf(
                        "الربع الأول (يناير - مارس)" to (1..13),
                        "الربع الثاني (أبريل - يونيو)" to (14..26),
                        "الربع الثالث (يوليو - سبتمبر)" to (27..39),
                        "الربع الرابع (أكتوبر - ديسمبر)" to (40..52)
                    ).forEach { (quarterTitle, range) ->
                        Text(
                            text = quarterTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            range.forEach { weekNum ->
                                val isSelected = weekNum == selectedWeekNum
                                val isCurrentWeek = isCurrentYear && weekNum == currentWeekOfYear
                                val isPastWeek = if (isCurrentYear) weekNum < currentWeekOfYear else selectedYear < currentYear
                                val hasEvents = weekNum in weeksWithEvents

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        isCurrentWeek -> GoldWarm
                                        isPastWeek -> EmeraldDeep.copy(alpha = 0.85f)
                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    },
                                    border = if (isCurrentWeek && !isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { selectedWeekNum = weekNum }
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = UmmAlQuraCalculator.toArabicDigits(weekNum.toString()),
                                                fontSize = 11.sp,
                                                fontWeight = if (isCurrentWeek || isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                                color = when {
                                                    isSelected || isPastWeek || isCurrentWeek -> Color.White
                                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                            )
                                            if (hasEvents) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(4.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSelected || isPastWeek) Color.Yellow else MaterialTheme.colorScheme.primary)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. بطاقة تفاصيل الأسبوع المختار وأحداثه
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "الأسبوع رقم ${UmmAlQuraCalculator.toArabicDigits(selectedWeekNum.toString())}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                if (isCurrentYear && selectedWeekNum == currentWeekOfYear) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = GoldWarm.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "الأسبوع الحالي 📍",
                                            color = GoldWarm,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "من ${weekDateRange.startDateFormatted} إلى ${weekDateRange.endDateFormatted}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { onAddEventForDate(weekDateRange.startDateKey) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة حدث", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // قائمة أحداث هذا الأسبوع
                    if (eventsInSelectedWeek.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "لا توجد أحداث أو مواعيد مجدولة في هذا الأسبوع حتى الآن. يمكنك إضافة حدث بالضغط على زر الإضافة أعلاه.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            eventsInSelectedWeek.forEach { event ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = event.title,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleSmall
                                            )
                                            Text(
                                                text = "${event.dateKey} ${if (event.startTime.isNotBlank()) "• ${event.startTime}" else ""}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        if (event.isCountdown) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primary
                                            ) {
                                                Text(
                                                    text = "تنازلي ⏳",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YearStatBadge(
    title: String,
    value: String,
    isHighlight: Boolean
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isHighlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = UmmAlQuraCalculator.toArabicDigits(value),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isHighlight) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ColorLegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
    }
}

data class WeekDateRange(
    val startDateKey: String,
    val endDateKey: String,
    val startDateFormatted: String,
    val endDateFormatted: String,
    val allDatesInWeek: List<String>
)

fun getWeekDateRange(year: Int, weekOfYear: Int): WeekDateRange {
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.WEEK_OF_YEAR, weekOfYear)
        set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
    }

    val sdfKey = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val sdfDisplay = SimpleDateFormat("d MMMM", Locale("ar"))

    val startCal = cal.clone() as Calendar
    val startDateKey = sdfKey.format(startCal.time)
    val startDateFormatted = sdfDisplay.format(startCal.time)

    val dates = mutableListOf<String>()
    for (i in 0 until 7) {
        dates.add(sdfKey.format(cal.time))
        cal.add(Calendar.DAY_OF_MONTH, 1)
    }

    cal.add(Calendar.DAY_OF_MONTH, -1)
    val endDateKey = sdfKey.format(cal.time)
    val endDateFormatted = sdfDisplay.format(cal.time)

    return WeekDateRange(
        startDateKey = startDateKey,
        endDateKey = endDateKey,
        startDateFormatted = startDateFormatted,
        endDateFormatted = endDateFormatted,
        allDatesInWeek = dates
    )
}
