package com.example.features.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.calendar.CalendarEventItem
import com.example.core.prayer.UmmAlQuraCalculator
import com.example.data.AppEventEntity
import com.example.ui.animation.interactiveBounce
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

enum class GoogleCalendarMode(val label: String) {
    MONTH("شهر"),
    WEEK("أسبوع"),
    THREE_DAYS("3 أيام"),
    DAY("يوم"),
    SCHEDULE("جدول")
}

/**
 * الشريط العلوي لتقويم قوقل (Google Calendar Top Bar) مطابق تماماً للصور
 */
@Composable
fun GoogleCalendarTopBar(
    currentMonthName: String,
    currentYear: Int,
    todayDay: Int,
    selectedMode: GoogleCalendarMode,
    onSelectMode: (GoogleCalendarMode) -> Unit,
    onTodayClick: () -> Unit,
    onBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showModeMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // الجانب الأيمن (القائمة واسم الشهر المنسدل)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(Icons.Default.Menu, contentDescription = "القائمة")
                }
            }

            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { showModeMenu = true }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "$currentMonthName $currentYear",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "تغيير العرض",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showModeMenu,
                    onDismissRequest = { showModeMenu = false }
                ) {
                    GoogleCalendarMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = mode.label,
                                    fontWeight = if (selectedMode == mode) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                if (selectedMode == mode) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            },
                            onClick = {
                                onSelectMode(mode)
                                showModeMenu = false
                            }
                        )
                    }
                }
            }
        }

        // الجانب الأيسر (أيقونات البحث، اليوم، المهام، الصورة الشخصية)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = { /* بحث */ },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Search, contentDescription = "بحث", modifier = Modifier.size(20.dp))
            }

            // أيقونة اليوم (مربع التقويم الذي يحمل رقم اليوم الحالي e.g. 20)
            Surface(
                onClick = onTodayClick,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.onSurfaceVariant),
                color = Color.Transparent,
                modifier = Modifier
                    .size(28.dp)
                    .interactiveBounce()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = todayDay.toString(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = { /* قائمة المهام */ },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.CheckCircleOutline, contentDescription = "المهام", modifier = Modifier.size(20.dp))
            }

            // أيقونة الحساب الدائرية (كوكب أو رمز شخصي)
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .size(32.dp)
                    .interactiveBounce()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Public,
                        contentDescription = "الحساب",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * 1. عرض الشهر الكامل (Google Calendar Month Grid) مطابق للصورة رقم 2
 */
@Composable
fun GoogleCalendarMonthView(
    year: Int,
    month: Int,
    selectedDay: Int,
    todayDay: Int,
    isCurrentMonth: Boolean,
    firstDayOfWeek: Int,
    daysInMonth: Int,
    appEvents: List<AppEventEntity>,
    deviceEvents: List<CalendarEventItem>,
    onDayClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val dayHeaders = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    Column(modifier = modifier.fillMaxSize()) {
        // صف أيام الأسبوع
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            dayHeaders.forEach { header ->
                Text(
                    text = header,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

        // شبكة أيام الشهر مع خلايا مستطيلة
        val totalCells = (firstDayOfWeek - 1) + daysInMonth
        val rows = (totalCells + 6) / 7

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            for (r in 0 until rows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    for (c in 0 until 7) {
                        val cellIndex = r * 7 + c
                        val dayNum = cellIndex - (firstDayOfWeek - 1) + 1
                        val isValidDay = dayNum in 1..daysInMonth
                        val isToday = isValidDay && isCurrentMonth && dayNum == todayDay
                        val isSelected = isValidDay && dayNum == selectedDay

                        val dateKey = if (isValidDay) String.format(Locale.US, "%04d-%02d-%02d", year, month, dayNum) else ""
                        val eventsThisDay = if (isValidDay) appEvents.filter { it.dateKey == dateKey } else emptyList()
                        val devEventsThisDay = if (isValidDay) deviceEvents.filter { it.dateKey == dateKey } else emptyList()

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                    else Color.Transparent
                                )
                                .then(
                                    if (isValidDay) Modifier.clickable { onDayClick(dayNum) } else Modifier
                                )
                                .padding(4.dp)
                        ) {
                            if (isValidDay) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // رقم اليوم
                                    if (isToday) {
                                        Surface(
                                            shape = RoundedCornerShape(100.dp),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            contentColor = MaterialTheme.colorScheme.surface,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = dayNum.toString(),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = dayNum.toString(),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))

                                    // كبسولات الأحداث (Event Pills)
                                    eventsThisDay.take(2).forEach { ev ->
                                        Surface(
                                            shape = RoundedCornerShape(3.dp),
                                            color = Color(0xFF5B8DEF),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = ev.title,
                                                fontSize = 9.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 2.dp, vertical = 0.5.dp)
                                            )
                                        }
                                    }

                                    devEventsThisDay.take(1).forEach { dev ->
                                        Surface(
                                            shape = RoundedCornerShape(3.dp),
                                            color = Color(0xFF0D9488),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = dev.title,
                                                fontSize = 9.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 2.dp, vertical = 0.5.dp)
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

/**
 * 2. العرض الزمني بالساعات (للأسبوع، 3 أيام، اليوم الواحد) مطابق للصور 3 و 4 و 5
 */
@Composable
fun GoogleCalendarTimeGridView(
    columnCount: Int, // 1 لليوم، 3 لـ 3 أيام، 7 للأسبوع
    startDayOffset: Int = 0,
    year: Int,
    month: Int,
    todayDay: Int,
    daysInMonth: Int,
    appEvents: List<AppEventEntity>,
    deviceEvents: List<CalendarEventItem>,
    onDayClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val hours = (1..23).toList()

    val currentCal = Calendar.getInstance()
    val currentHour = currentCal.get(Calendar.HOUR_OF_DAY)
    val currentMinute = currentCal.get(Calendar.MINUTE)

    val displayedDays = remember(columnCount, startDayOffset, todayDay) {
        (0 until columnCount).map { i ->
            val d = ((todayDay + startDayOffset + i - 1) % daysInMonth) + 1
            d
        }
    }

    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    Column(modifier = modifier.fillMaxSize()) {
        // رؤوس الأعمدة بالأيام
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // مساحة فارغة فوق عمود الساعات
            Spacer(modifier = Modifier.width(54.dp))

            displayedDays.forEachIndexed { idx, day ->
                val isToday = day == todayDay
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onDayClick(day) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val dayOfWeekIdx = (day + 2) % 7
                    Text(
                        text = dayNames[dayOfWeekIdx],
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (isToday) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = day.toString(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        Text(
                            text = day.toString(),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

        // شبكة الساعات العمودية التفاعلية
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                hours.forEach { hour ->
                    val hourLabel = when {
                        hour < 12 -> "$hour AM"
                        hour == 12 -> "12 PM"
                        else -> "${hour - 12} PM"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // عمود الوقت
                        Text(
                            text = hourLabel,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .width(50.dp)
                                .padding(end = 6.dp, top = 2.dp),
                            textAlign = TextAlign.End
                        )

                        // خلايا الأعمدة
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)))
                        ) {
                            displayedDays.forEach { day ->
                                val dateKey = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
                                val eventsAtHour = appEvents.filter {
                                    it.dateKey == dateKey && it.startTime.startsWith(String.format(Locale.ENGLISH, "%02d", hour))
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .border(BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f)))
                                        .padding(2.dp)
                                ) {
                                    eventsAtHour.forEach { ev ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF4C6EF5),
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Text(
                                                text = ev.title,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 2,
                                                modifier = Modifier.padding(4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // مؤشر الوقت الحالي (خط أفقي أبيض مع نقطة كما في الصور 3 و 4 و 5)
            val currentLineOffsetY = ((currentHour - 1) * 56 + (currentMinute * 56 / 60)).dp.coerceAtLeast(0.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = currentLineOffsetY),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // النقطة البيضاء على مقياس الوقت
                Box(
                    modifier = Modifier
                        .padding(start = 44.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                )
                // الخط الأفقي المستمر
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .background(Color.White)
                )
            }
        }
    }
}
