package com.example.features.prayer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Today
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
import com.example.core.prayer.CityLocation
import com.example.core.prayer.PrayerType
import com.example.core.prayer.UmmAlQuraCalculator
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldSoft
import com.example.ui.theme.MintAccent
import java.util.Calendar
import java.util.TimeZone

/**
 * شاشة تقويم الصلوات الشامل:
 * تتيح تصفح أي شهر وسنة مستقبلية ومعرفة مواقيت الصلوات بدقة لأي يوم بعد عدة أشهر.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerCalendarScreen(
    currentCity: CityLocation,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
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

    // عدد أيام الشهر المحدد
    val daysInMonth = remember(selectedYear, selectedMonth) {
        val cal = Calendar.getInstance(tz).apply {
            set(selectedYear, selectedMonth - 1, 1)
        }
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    // اليوم الأول في الشهر (أي يوم من أيام الأسبوع)
    val firstDayOfWeek = remember(selectedYear, selectedMonth) {
        val cal = Calendar.getInstance(tz).apply {
            set(selectedYear, selectedMonth - 1, 1)
        }
        cal.get(Calendar.DAY_OF_WEEK) // 1=الأحد ... 7=السبت
    }

    val arabicMonths = listOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "تقويم مواقيت الصلاة",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("prayer_calendar_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            selectedYear = todayYear
                            selectedMonth = todayMonth
                            selectedDay = todayDay
                        }
                    ) {
                        Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("اليوم", fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("prayer_calendar_content"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // شريط اختيار الشهر والسنة
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = EmeraldDeep)
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

                        // أزرار القفز السريع للمستقبل
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf(
                                "الشهر القادم" to 1,
                                "بعد 3 أشهر" to 3,
                                "بعد 6 أشهر" to 6,
                                "رمضان" to 0
                            ).forEach { (label, addMonths) ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White.copy(alpha = 0.16f),
                                    modifier = Modifier.clickable {
                                        if (addMonths == 0) {
                                            selectedMonth = 3 // تقديري لرمضان في السنوات الحالية
                                        } else {
                                            val nextM = selectedMonth + addMonths
                                            if (nextM > 12) {
                                                selectedMonth = nextM - 12
                                                selectedYear++
                                            } else {
                                                selectedMonth = nextM
                                            }
                                        }
                                        selectedDay = 1
                                    }
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // أسماء أيام الأسبوع
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
                            modifier = Modifier.width(40.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // شبكة أيام الشهر
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
                                            shape = CircleShape,
                                            color = when {
                                                isSelected -> EmeraldDeep
                                                isToday -> MintAccent.copy(alpha = 0.25f)
                                                else -> Color.Transparent
                                            },
                                            border = if (isToday && !isSelected) BorderStroke(1.dp, EmeraldDeep) else null,
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .clickable { selectedDay = dayNum }
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = UmmAlQuraCalculator.toArabicDigits(dayNum.toString()),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.Normal,
                                                    color = when {
                                                        isSelected -> Color.White
                                                        isToday -> EmeraldDeep
                                                        else -> MaterialTheme.colorScheme.onSurface
                                                    }
                                                )
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(38.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // جدول مواقيت اليوم المختار بالكامل
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
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
                            Column {
                                Text(
                                    text = daySchedule.hijriFormatted,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldDeep
                                )
                                Text(
                                    text = "$selectedDay ${arabicMonths[selectedMonth - 1]} $selectedYear م",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // عرض الصلوات الأساسية والسنن
                        daySchedule.prayers.forEach { slot ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = slot.type.arabicName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (slot.type.isObligatory) FontWeight.Bold else FontWeight.Normal,
                                    color = if (slot.type.isObligatory) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = slot.formatted12h,
                                    style = MaterialTheme.typography.bodyMedium,
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
