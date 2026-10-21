package com.example.features.calendar

import android.Manifest
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.ViewWeek
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.core.calendar.CalendarEventItem
import com.example.core.calendar.DeviceCalendarManager
import com.example.core.prayer.UmmAlQuraCalculator
import com.example.data.AppEventEntity
import com.example.features.DailyManagerUiState
import com.example.features.DailyManagerViewModel
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldSoft
import com.example.ui.theme.MintAccent
import com.example.ui.theme.PriorityHighRed
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * شاشة التقويم ونظام الأحداث الأساسي المستقل:
 * - أحداث التطبيق وتقويم الهاتف
 * - المواعيد والعداد التنازلي
 * - أسابيع السنة ورحلة حياتي
 * - لا يتضمن أوقات الصلاة بتاتاً
 */
@Composable
fun MainEventsCalendarScreen(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: DailyManagerViewModel? = null,
    uiState: DailyManagerUiState? = null,
    isEmbedded: Boolean = false,
    onStartEventTimer: (AppEventEntity) -> Unit = {}
) {
    if (!isEmbedded) {
        BackHandler { onBack() }
    }
    val context = LocalContext.current

    val tz = remember { TimeZone.getTimeZone("GMT+03:00") }
    val todayCal = remember { Calendar.getInstance(tz) }
    val todayYear = todayCal.get(Calendar.YEAR)
    val todayMonth = todayCal.get(Calendar.MONTH) + 1
    val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

    var selectedYear by remember { mutableIntStateOf(todayYear) }
    var selectedMonth by remember { mutableIntStateOf(todayMonth) }
    var selectedDay by remember { mutableIntStateOf(todayDay) }

    var calendarViewMode by rememberSaveable { mutableIntStateOf(0) } // 0=الشهري, 1=أسابيع السنة, 2=رحلة حياتي
    var newEventCountdownDefault by remember { mutableStateOf(false) }
    var showAddEventDialog by remember { mutableStateOf(false) }
    var deletingEventId by remember { mutableStateOf<String?>(null) }

    var hasCalendarPermission by remember {
        mutableStateOf(DeviceCalendarManager.hasCalendarPermission(context))
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.READ_CALENDAR] == true
        hasCalendarPermission = granted
        if (granted) {
            viewModel?.refreshDeviceCalendarEvents(context)
            Toast.makeText(context, "تم منح إذن التقويم وجلب مواعيد الهاتف بنجاح", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "لم يتم منح إذن التقويم", Toast.LENGTH_SHORT).show()
        }
    }

    val selectedDateKey = remember(selectedYear, selectedMonth, selectedDay) {
        String.format(Locale.US, "%04d-%02d-%02d", selectedYear, selectedMonth, selectedDay)
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

    val appEvents = uiState?.appEvents.orEmpty()
    val deviceEvents = uiState?.deviceCalendarEvents.orEmpty()

    val todayAppEvents = remember(appEvents, selectedDateKey) {
        appEvents.filter { it.dateKey == selectedDateKey }
    }
    val todayDeviceEvents = remember(deviceEvents, selectedDateKey) {
        deviceEvents.filter { it.dateKey == selectedDateKey }
    }

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
            if (!isEmbedded) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("calendar_screen_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع"
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                "التقويم ونظام الأحداث",
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.weight(1f))

            FilledTonalButton(
                onClick = { showAddEventDialog = true },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.testTag("calendar_add_event_top_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("حدث", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(4.dp))

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

        // أشرطة التبديل بين طرق العرض
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .testTag("calendar_view_modes_row")
        ) {
            SegmentedButton(
                selected = calendarViewMode == 0,
                onClick = { calendarViewMode = 0 },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(14.dp)) }
            ) {
                Text("تقويم الشهر", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            SegmentedButton(
                selected = calendarViewMode == 1,
                onClick = { calendarViewMode = 1 },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                icon = { Icon(Icons.Outlined.ViewWeek, contentDescription = null, modifier = Modifier.size(14.dp)) }
            ) {
                Text("أسابيع السنة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            SegmentedButton(
                selected = calendarViewMode == 2,
                onClick = { calendarViewMode = 2 },
                shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                icon = { Icon(Icons.Outlined.Cake, contentDescription = null, modifier = Modifier.size(14.dp)) }
            ) {
                Text("رحلة حياتي", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        when (calendarViewMode) {
            1 -> {
                YearInWeeksView(
                    appEvents = appEvents,
                    onSelectDate = { y, m, d ->
                        selectedYear = y
                        selectedMonth = m
                        selectedDay = d
                        calendarViewMode = 0
                    },
                    onAddEventForDate = { dateKey ->
                        try {
                            val parts = dateKey.split("-")
                            selectedYear = parts[0].toInt()
                            selectedMonth = parts[1].toInt()
                            selectedDay = parts[2].toInt()
                        } catch (_: Exception) {}
                        showAddEventDialog = true
                    }
                )
            }
            2 -> {
                LifeInWeeksView(
                    userBirthDateKey = uiState?.userBirthDateKey ?: "2000-01-01",
                    milestones = uiState?.lifeMilestones.orEmpty(),
                    onUpdateBirthDate = { newDate ->
                        viewModel?.updateUserBirthDate(newDate)
                    },
                    onSaveMilestone = { milestone ->
                        viewModel?.saveLifeMilestone(milestone)
                    },
                    onDeleteMilestone = { id ->
                        viewModel?.deleteLifeMilestone(id)
                    }
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("calendar_and_events_content"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 48.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. العداد التنازلي للأحداث القادمة
                    item {
                        EventCountdownSection(
                            events = appEvents,
                            onToggleCountdown = { event ->
                                viewModel?.toggleEventCountdown(event)
                            },
                            onSelectEvent = { event ->
                                try {
                                    val parts = event.dateKey.split("-")
                                    selectedYear = parts[0].toInt()
                                    selectedMonth = parts[1].toInt()
                                    selectedDay = parts[2].toInt()
                                } catch (_: Exception) {}
                            },
                            onAddNewEventWithCountdown = {
                                newEventCountdownDefault = true
                                showAddEventDialog = true
                            }
                        )
                    }

                    // 2. بطاقة مزامنة تقويم الهاتف
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("device_calendar_sync_card"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (hasCalendarPermission) {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhoneAndroid,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "مزامنة تقويم الهاتف",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = if (hasCalendarPermission) {
                                                "تم جلب ${UmmAlQuraCalculator.toArabicDigits(deviceEvents.size.toString())} موعداً من الهاتف"
                                            } else {
                                                "اضغط للسماح بقراءة مواعيد الهاتف وعرضها هنا"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (!hasCalendarPermission) {
                                    Button(
                                        onClick = {
                                            permissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.READ_CALENDAR,
                                                    Manifest.permission.WRITE_CALENDAR
                                                )
                                            )
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("تفعيل", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    IconButton(
                                        onClick = { viewModel?.refreshDeviceCalendarEvents(context) },
                                        modifier = Modifier.testTag("refresh_device_calendar_btn")
                                    ) {
                                        if (uiState?.isDeviceCalendarSyncing == true) {
                                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                        } else {
                                            Icon(Icons.Default.Refresh, contentDescription = "تحديث تقويم الهاتف")
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. شريط اختيار الشهر والسنة
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
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
                                        Icon(Icons.Default.ChevronRight, contentDescription = "الشهر التالي")
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "${arabicMonths[selectedMonth - 1]} $selectedYear",
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = "مواعيد وأحداث",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                        Icon(Icons.Default.ChevronLeft, contentDescription = "الشهر السابق")
                                    }
                                }
                            }
                        }
                    }

                    // 4. أيام الأسبوع
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

                    // 5. شبكة أيام الشهر
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

                                                val cellKey = String.format(Locale.US, "%04d-%02d-%02d", selectedYear, selectedMonth, dayNum)
                                                val hasAppEvents = appEvents.any { it.dateKey == cellKey }
                                                val hasDeviceEvents = deviceEvents.any { it.dateKey == cellKey }

                                                Surface(
                                                    modifier = Modifier
                                                        .size(42.dp)
                                                        .clickable { selectedDay = dayNum }
                                                        .testTag("day_cell_${dayNum}"),
                                                    shape = RoundedCornerShape(12.dp),
                                                    color = when {
                                                        isSelected -> MaterialTheme.colorScheme.primary
                                                        isToday -> MaterialTheme.colorScheme.primaryContainer
                                                        else -> Color.Transparent
                                                    },
                                                    border = if (isToday && !isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                                                ) {
                                                    Column(
                                                        modifier = Modifier.fillMaxSize(),
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        Text(
                                                            text = UmmAlQuraCalculator.toArabicDigits(dayNum.toString()),
                                                            fontWeight = if (isSelected || isToday) FontWeight.Black else FontWeight.Medium,
                                                            color = when {
                                                                isSelected -> Color.White
                                                                isToday -> MaterialTheme.colorScheme.primary
                                                                else -> MaterialTheme.colorScheme.onSurface
                                                            },
                                                            fontSize = 13.sp
                                                        )

                                                        if (hasAppEvents || hasDeviceEvents) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(4.dp)
                                                                    .clip(CircleShape)
                                                                    .background(if (isSelected) Color.White else MaterialTheme.colorScheme.primary)
                                                            )
                                                        }
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

                    // 6. أحداث اليوم المختار
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "أحداث ومواعيد اليوم",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )

                            Button(
                                onClick = { showAddEventDialog = true },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إضافة موعد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // قائمة أحداث التطبيق
                    if (todayAppEvents.isNotEmpty()) {
                        items(todayAppEvents, key = { "app_ev_${it.id}" }) { event ->
                            AppEventCard(
                                event = event,
                                onDelete = { deletingEventId = event.id },
                                onStartTimer = { onStartEventTimer(event) }
                            )
                        }
                    }

                    // قائمة أحداث الهاتف
                    if (todayDeviceEvents.isNotEmpty()) {
                        items(todayDeviceEvents, key = { "dev_ev_${it.id}_${it.startTimestamp}" }) { event ->
                            DeviceEventCard(
                                event = event,
                                onStartTimer = {
                                    val dummyEvent = AppEventEntity(
                                        id = event.id.toString(),
                                        title = event.title,
                                        dateKey = event.dateKey,
                                        startTime = event.startTimeFormatted
                                    )
                                    onStartEventTimer(dummyEvent)
                                }
                            )
                        }
                    }

                    if (todayAppEvents.isEmpty() && todayDeviceEvents.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "لا توجد مواعيد أو أحداث مسجلة في هذا اليوم",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddEventDialog) {
        AddEventDialog(
            initialDateKey = selectedDateKey,
            initialIsCountdown = newEventCountdownDefault,
            onDismiss = {
                showAddEventDialog = false
                newEventCountdownDefault = false
            },
            onConfirm = { title, desc, loc, dateKey, start, end, isAllDay, cat, colorHex, isCountdown, syncToDevice, isRecurring, recurringDays, recurringWeeks ->
                if (isRecurring && recurringDays.isNotEmpty()) {
                    viewModel?.addRecurringAppEvents(
                        title = title,
                        description = desc,
                        location = loc,
                        startDateKey = dateKey,
                        startTime = start,
                        endTime = end,
                        colorHex = colorHex,
                        isAllDay = isAllDay,
                        category = cat,
                        reminderMinutes = 15,
                        isCountdown = isCountdown,
                        syncToDevice = syncToDevice,
                        daysOfWeek = recurringDays,
                        durationWeeks = recurringWeeks,
                        context = context
                    )
                } else {
                    viewModel?.addAppEvent(
                        title = title,
                        description = desc,
                        location = loc,
                        dateKey = dateKey,
                        startTime = start,
                        endTime = end,
                        isAllDay = isAllDay,
                        category = cat,
                        colorHex = colorHex,
                        isCountdown = isCountdown,
                        syncToDevice = syncToDevice,
                        context = context
                    )
                }
                showAddEventDialog = false
                newEventCountdownDefault = false
            }
        )
    }

    if (deletingEventId != null) {
        val evId = deletingEventId!!
        AlertDialog(
            onDismissRequest = { deletingEventId = null },
            title = { Text("حذف الموعد", fontWeight = FontWeight.Bold) },
            text = { Text("هل أنت متأكد من رغبتك في حذف هذا الموعد من التقويم؟") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel?.deleteAppEvent(evId)
                        deletingEventId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PriorityHighRed)
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingEventId = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
private fun AppEventCard(
    event: AppEventEntity,
    onDelete: () -> Unit,
    onStartTimer: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = event.category,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (event.description.isNotBlank()) {
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = if (event.isAllDay) "طوال اليوم" else "${event.startTime} - ${event.endTime}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onStartTimer,
                    modifier = Modifier.testTag("event_timer_btn_${event.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "بدء المؤقت",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun DeviceEventCard(
    event: CalendarEventItem,
    onStartTimer: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.PhoneAndroid, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (event.isAllDay) "طوال اليوم" else event.startTimeFormatted,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onStartTimer) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "بدء المؤقت",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun AddEventDialog(
    initialDateKey: String,
    initialIsCountdown: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        description: String,
        location: String,
        dateKey: String,
        startTime: String,
        endTime: String,
        isAllDay: Boolean,
        category: String,
        colorHex: String,
        isCountdown: Boolean,
        syncToDevice: Boolean,
        isRecurring: Boolean,
        recurringDays: Set<Int>,
        recurringWeeks: Int
    ) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var dateKey by remember { mutableStateOf(initialDateKey) }
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("10:00") }
    var isAllDay by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("عام") }
    var colorHex by remember { mutableStateOf("#3B82F6") }
    var isCountdown by remember { mutableStateOf(initialIsCountdown) }
    var syncToDevice by remember { mutableStateOf(false) }

    // إعدادات المنبه الخارق للموعد
    var superAlarmEnabled by remember { mutableStateOf(false) }
    var alarmVerifyMethod by remember { mutableStateOf("LOCAL_IMAGE") }
    var alarmPlaceName by remember { mutableStateOf("المكان المحدد") }
    var alarmQrCode by remember { mutableStateOf("WAKE-QR-2026") }

    // خيارات التكرار (المطلب: كم مرة بالأسبوع ولمدة كم)
    var isRecurring by remember { mutableStateOf(false) }
    var recurringWeeks by remember { mutableIntStateOf(4) }
    val selectedDaysOfWeek = remember { mutableStateOf(setOf(1, 2, 3, 4, 5)) } // افتراضياً أيام العمل/الدراسة

    fun showTimePicker(currentTime: String, onTimePicked: (String) -> Unit) {
        val parts = currentTime.split(":")
        val initialHour = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val initialMin = parts.getOrNull(1)?.toIntOrNull() ?: 0
        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val formatted = String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
                onTimePicked(formatted)
            },
            initialHour,
            initialMin,
            true
        ).show()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (isCountdown) "إضافة حدث مع عداد تنازلي" else "إضافة موعد جديد",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان الموعد أو الحدث") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("تفاصيل وملاحظات الحدث") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = dateKey,
                    onValueChange = { dateKey = it },
                    label = { Text("التاريخ (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // اختيار وقت البدء والانتهاء بالساعة التفاعلية
                Text(
                    text = "وقت الموعد:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showTimePicker(startTime) { startTime = it } }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "وقت البدء",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = startTime,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showTimePicker(endTime) { endTime = it } }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "وقت الانتهاء",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = endTime,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }

                // قسم التكرار (كم مرة في الأسبوع ولمدة كم)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("تكرار الحدث", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                Text("تكرار أسبوعي خلال مدة محددة", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = isRecurring, onCheckedChange = { isRecurring = it })
                        }

                        if (isRecurring) {
                            Text(
                                text = "الأيام في الأسبوع:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )

                            val daysLabels = listOf(
                                1 to "أحد",
                                2 to "إثنين",
                                3 to "ثلاثاء",
                                4 to "أربعاء",
                                5 to "خميس",
                                6 to "جمعة",
                                7 to "سبت"
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                daysLabels.forEach { (dow, label) ->
                                    val isSelected = selectedDaysOfWeek.value.contains(dow)
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            val current = selectedDaysOfWeek.value.toMutableSet()
                                            if (isSelected) {
                                                if (current.size > 1) current.remove(dow)
                                            } else {
                                                current.add(dow)
                                            }
                                            selectedDaysOfWeek.value = current
                                        },
                                        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    )
                                }
                            }

                            Text(
                                text = "لمدة كم:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )

                            val durationOptions = listOf(
                                1 to "أسبوع",
                                2 to "أسبوعين",
                                4 to "شهر",
                                8 to "شهران",
                                12 to "3 أشهر"
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                durationOptions.forEach { (w, label) ->
                                    FilterChip(
                                        selected = recurringWeeks == w,
                                        onClick = { recurringWeeks = w },
                                        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("عداد تنازلي للحدث", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isCountdown, onCheckedChange = { isCountdown = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("حفظ في تقويم الهاتف", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = syncToDevice, onCheckedChange = { syncToDevice = it })
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("المنبه الخارق للموعد", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("تنبيه صارم لا يتوقف إلا بالتحقق", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = superAlarmEnabled, onCheckedChange = { superAlarmEnabled = it })
                }

                if (superAlarmEnabled) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = alarmVerifyMethod == "LOCAL_IMAGE",
                            onClick = { alarmVerifyMethod = "LOCAL_IMAGE" },
                            shape = SegmentedButtonDefaults.itemShape(0, 2)
                        ) {
                            Text("صورة المكان", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        SegmentedButton(
                            selected = alarmVerifyMethod == "QR",
                            onClick = { alarmVerifyMethod = "QR" },
                            shape = SegmentedButtonDefaults.itemShape(1, 2)
                        ) {
                            Text("رمز QR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                if (superAlarmEnabled) {
                                    com.example.core.notifications.AdhanNotificationHelper.scheduleHabitOrTaskDailyAlarm(
                                        context = context,
                                        itemId = java.util.UUID.randomUUID().toString(),
                                        title = "موعد: $title",
                                        isHabit = false,
                                        time24 = startTime,
                                        alarmType = "SUPER_WAKE",
                                        verifyMethod = alarmVerifyMethod,
                                        placeName = alarmPlaceName,
                                        qrCode = alarmQrCode
                                    )
                                }
                                onConfirm(
                                    title,
                                    description,
                                    location,
                                    dateKey,
                                    startTime,
                                    endTime,
                                    isAllDay,
                                    category,
                                    colorHex,
                                    isCountdown,
                                    syncToDevice,
                                    isRecurring,
                                    selectedDaysOfWeek.value,
                                    recurringWeeks
                                )
                            }
                        },
                        enabled = title.isNotBlank()
                    ) {
                        Text("حفظ")
                    }
                }
            }
        }
    }
}
