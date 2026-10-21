package com.example.features.prayer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.prayer.CityLocation
import com.example.core.prayer.UmmAlQuraCalculator
import com.example.features.DailyManagerUiState
import com.example.features.DailyManagerViewModel
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldSoft
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * تقويم الصلوات المخصص المستقل:
 * - تقويم شهري فقط
 * - تحته مباشرة مواقيت الصلاة لليوم المختار
 * - مفصول تماماً عن الأحداث وتقويم الهاتف
 */
@Composable
fun PrayerCalendarScreen(
    currentCity: CityLocation,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DailyManagerViewModel? = null,
    uiState: DailyManagerUiState? = null
) {
    BackHandler { onBack() }

    val tz = remember { TimeZone.getTimeZone("GMT+03:00") }
    val todayCal = remember { Calendar.getInstance(tz) }
    val todayYear = todayCal.get(Calendar.YEAR)
    val todayMonth = todayCal.get(Calendar.MONTH) + 1
    val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

    var selectedYear by remember { mutableIntStateOf(todayYear) }
    var selectedMonth by remember { mutableIntStateOf(todayMonth) }
    var selectedDay by remember { mutableIntStateOf(todayDay) }

    // حساب مواقيت اليوم المحدد في التقويم
    val daySchedule = remember(currentCity, selectedYear, selectedMonth, selectedDay) {
        UmmAlQuraCalculator.calculateScheduleForDate(
            city = currentCity,
            year = selectedYear,
            month = selectedMonth,
            day = selectedDay
        )
    }

    val daysInMonth = remember(selectedYear, selectedMonth) {
        val cal = Calendar.getInstance(tz).apply {
            set(selectedYear, selectedMonth - 1, 1)
        }
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    val firstDayOfWeek = remember(selectedYear, selectedMonth) {
        val cal = Calendar.getInstance(tz).apply {
            set(selectedYear, selectedMonth - 1, 1)
        }
        cal.get(Calendar.DAY_OF_WEEK)
    }

    val arabicMonths = listOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // شريط علوي ملتصق بأعلى الشاشة بدون مساحة فارغة
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("prayer_calendar_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع"
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                "تقويم الصلوات",
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.weight(1f))

            TextButton(
                onClick = {
                    selectedYear = todayYear
                    selectedMonth = todayMonth
                    selectedDay = todayDay
                }
            ) {
                Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("اليوم", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("prayer_calendar_content"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. شريط اختيار الشهر والسنة
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldDeep)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    if (selectedMonth == 12) {
                                        selectedMonth = 1
                                        selectedYear++
                                    } else {
                                        selectedMonth++
                                    }
                                    selectedDay = 1
                                }
                            ) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "الشهر التالي", tint = Color.White)
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${arabicMonths[selectedMonth - 1]} $selectedYear",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "مدينة ${currentCity.arabicName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GoldSoft
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (selectedMonth == 1) {
                                        selectedMonth = 12
                                        selectedYear--
                                    } else {
                                        selectedMonth--
                                    }
                                    selectedDay = 1
                                }
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "الشهر السابق", tint = Color.White)
                            }
                        }
                    }
                }
            }

            // 2. أسماء أيام الأسبوع
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    listOf("أحد", "إثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة", "سبت").forEach { dName ->
                        Text(
                            text = dName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(42.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 3. شبكة أيام الشهر
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    ) {
                        val totalCells = (firstDayOfWeek - 1) + daysInMonth
                        val rows = (totalCells + 6) / 7

                        for (r in 0 until rows) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                for (c in 0 until 7) {
                                    val cellIndex = r * 7 + c
                                    val dayNum = cellIndex - (firstDayOfWeek - 1) + 1
                                    if (dayNum in 1..daysInMonth) {
                                        val isSelected = dayNum == selectedDay
                                        val isToday = dayNum == todayDay && selectedMonth == todayMonth && selectedYear == todayYear

                                        Surface(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clickable { selectedDay = dayNum }
                                                .testTag("prayer_day_cell_${dayNum}"),
                                            shape = RoundedCornerShape(12.dp),
                                            color = when {
                                                isSelected -> EmeraldDeep
                                                isToday -> EmeraldDeep.copy(alpha = 0.15f)
                                                else -> Color.Transparent
                                            },
                                            border = if (isToday && !isSelected) BorderStroke(1.5.dp, EmeraldDeep) else null
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier.fillMaxSize()
                                            ) {
                                                Text(
                                                    text = UmmAlQuraCalculator.toArabicDigits(dayNum.toString()),
                                                    fontWeight = if (isSelected || isToday) FontWeight.Black else FontWeight.Medium,
                                                    color = when {
                                                        isSelected -> Color.White
                                                        isToday -> EmeraldDeep
                                                        else -> MaterialTheme.colorScheme.onSurface
                                                    },
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(42.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. مواقيت الصلاة لليوم المختار (فقط أوقات الصلاة في ذاك اليوم)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                Icon(Icons.Default.Mosque, contentDescription = null, tint = EmeraldDeep, modifier = Modifier.size(20.dp))
                                Text(
                                    text = "مواقيت الصلاة لليوم المختار",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldDeep
                                )
                            }
                            Text(
                                text = "${daySchedule.hijriFormatted} • $selectedDay ${arabicMonths[selectedMonth - 1]}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // عرض الصلوات الخمس والشروق
                        daySchedule.prayers.forEach { slot ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = slot.type.arabicName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (slot.type.isObligatory) FontWeight.Bold else FontWeight.Normal,
                                    color = if (slot.type.isObligatory) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = slot.formatted12h,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (slot.type.isObligatory) EmeraldDeep else GoldSoft
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
