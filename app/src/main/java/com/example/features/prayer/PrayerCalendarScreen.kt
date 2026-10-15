package com.example.features.prayer

import android.Manifest
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.core.prayer.CityLocation
import com.example.core.prayer.PrayerType
import com.example.core.prayer.UmmAlQuraCalculator
import com.example.data.AppEventEntity
import com.example.features.DailyManagerUiState
import com.example.features.DailyManagerViewModel
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldSoft
import com.example.ui.theme.MintAccent
import com.example.ui.theme.PriorityHighRed
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * شاشة التقويم الشامل ونظام الأحداث والمواعيد:
 * - تصفح تقويم الشهر والسنة بدقة.
 * - مزامنة وعرض أحداث تقويم الجوال (تقويم سامسونج وتقويم Google) بإذن مباشر.
 * - إضافة وتعديل وحذف أحداث ومواعيد خاصة بالتطبيق مع إمكانية حفظها في تقويم الجوال.
 * - عرض مواقيت الصلاة والتقويم الهجري لكل يوم محدد.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerCalendarScreen(
    currentCity: CityLocation,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DailyManagerViewModel? = null,
    uiState: DailyManagerUiState? = null
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    val tz = remember { TimeZone.getTimeZone("GMT+03:00") }
    val todayCal = remember { Calendar.getInstance(tz) }
    val todayYear = todayCal.get(Calendar.YEAR)
    val todayMonth = todayCal.get(Calendar.MONTH) + 1
    val todayDay = todayCal.get(Calendar.DAY_OF_MONTH)

    var selectedYear by remember { mutableIntStateOf(todayYear) }
    var selectedMonth by remember { mutableIntStateOf(todayMonth) }
    var selectedDay by remember { mutableIntStateOf(todayDay) }

    var showAddEventDialog by remember { mutableStateOf(false) }
    var deletingEventId by remember { mutableStateOf<String?>(null) }

    // فحص إذن تقويم الهاتف
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
            Toast.makeText(context, "تم منح إذن التقويم وجلب أحداث الهاتف بنجاح 📱", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "لم يتم منح إذن التقويم", Toast.LENGTH_SHORT).show()
        }
    }

    // مفتاح التاريخ المختار YYYY-MM-DD
    val selectedDateKey = remember(selectedYear, selectedMonth, selectedDay) {
        String.format(Locale.US, "%04d-%02d-%02d", selectedYear, selectedMonth, selectedDay)
    }

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

    // اليوم الأول في الشهر (أي يوم من أيام الأسبوع: 1=الأحد ... 7=السبت)
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

    // الأحداث المتاحة لليوم المختار وللشهر
    val appEvents = uiState?.appEvents.orEmpty()
    val deviceEvents = uiState?.deviceCalendarEvents.orEmpty()

    val todayAppEvents = remember(appEvents, selectedDateKey) {
        appEvents.filter { it.dateKey == selectedDateKey }
    }
    val todayDeviceEvents = remember(deviceEvents, selectedDateKey) {
        deviceEvents.filter { it.dateKey == selectedDateKey }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "التقويم ونظام الأحداث",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "تقويم الهاتف، الأحداث، ومواقيت الصلاة",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("calendar_screen_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                actions = {
                    // زر إضافة حدث
                    FilledTonalButton(
                        onClick = { showAddEventDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("calendar_add_event_top_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حدث", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // زر العودة لليوم الحالي
                    TextButton(
                        onClick = {
                            selectedYear = todayYear
                            selectedMonth = todayMonth
                            selectedDay = todayDay
                        }
                    ) {
                        Icon(Icons.Default.Today, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("اليوم", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                .testTag("calendar_and_events_content"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. بطاقة مزامنة تقويم الهاتف (سامسونج)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasCalendarPermission) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        } else {
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (hasCalendarPermission) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PhoneAndroid,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "مزامنة تقويم الهاتف (سامسونج / Google)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = if (hasCalendarPermission) {
                                    if (deviceEvents.isNotEmpty()) "تم جلب ${deviceEvents.size} موعد من تقويم الهاتف ✓" else "تقويم الهاتف متصل ومزامن"
                                } else {
                                    "اضغط للسماح بقراءة مواعيد تقويم سامسونج وعرضها بالتقويم"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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

            // 2. شريط اختيار الشهر والسنة
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
                                    text = "مدينة ${currentCity.arabicName} | مواعيد وأحداث",
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

                        // أزرار القفز السريع
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf(
                                "الشهر القادم" to 1,
                                "بعد شهرين" to 2,
                                "بعد 6 أشهر" to 6,
                                "رمضان" to 0
                            ).forEach { (label, addMonths) ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.White.copy(alpha = 0.16f),
                                    modifier = Modifier.clickable {
                                        if (addMonths == 0) {
                                            selectedMonth = 3
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

            // 3. أسماء أيام الأسبوع
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

            // 4. شبكة أيام الشهر مع مؤشرات الأحداث
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

                                        val cellDateKey = String.format(Locale.US, "%04d-%02d-%02d", selectedYear, selectedMonth, dayNum)
                                        val cellHasDeviceEvent = deviceEvents.any { it.dateKey == cellDateKey }
                                        val cellHasAppEvent = appEvents.any { it.dateKey == cellDateKey }
                                        val eventCount = (if (cellHasDeviceEvent) 1 else 0) + (if (cellHasAppEvent) 1 else 0)

                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = when {
                                                isSelected -> EmeraldDeep
                                                isToday -> MintAccent.copy(alpha = 0.25f)
                                                else -> Color.Transparent
                                            },
                                            border = if (isToday && !isSelected) BorderStroke(1.5.dp, EmeraldDeep) else null,
                                            modifier = Modifier
                                                .width(44.dp)
                                                .height(52.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { selectedDay = dayNum }
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxSize(),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = UmmAlQuraCalculator.toArabicDigits(dayNum.toString()),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.Medium,
                                                    color = when {
                                                        isSelected -> Color.White
                                                        isToday -> EmeraldDeep
                                                        else -> MaterialTheme.colorScheme.onSurface
                                                    }
                                                )

                                                // مؤشرات الأحداث تحت الرقم
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                ) {
                                                    if (cellHasDeviceEvent) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(5.dp)
                                                                .clip(CircleShape)
                                                                .background(if (isSelected) Color(0xFF93C5FD) else Color(0xFF2563EB))
                                                        )
                                                    }
                                                    if (cellHasAppEvent) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(5.dp)
                                                                .clip(CircleShape)
                                                                .background(if (isSelected) Color(0xFFA7F3D0) else Color(0xFF059669))
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.width(44.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. قسم أحداث ومواعيد اليوم المختار
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "أحداث ومواعيد اليوم",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "${daySchedule.hijriFormatted} • $selectedDay ${arabicMonths[selectedMonth - 1]} $selectedYear م",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = { showAddEventDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("add_event_for_selected_day_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة حدث", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // قائمة أحداث تقويم الهاتف (سامسونج)
            if (todayDeviceEvents.isNotEmpty()) {
                item {
                    Text(
                        text = "📱 مواعيد تقويم الهاتف (${todayDeviceEvents.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D4ED8)
                    )
                }

                items(todayDeviceEvents, key = { it.id }) { devEvent ->
                    DeviceCalendarEventCard(event = devEvent)
                }
            }

            // قائمة أحداث التطبيق المحلية
            if (todayAppEvents.isNotEmpty()) {
                item {
                    Text(
                        text = "📌 مواعيد وأحداث التطبيق (${todayAppEvents.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(todayAppEvents, key = { it.id }) { appEvent ->
                    AppEventCard(
                        event = appEvent,
                        onDelete = { deletingEventId = appEvent.id }
                    )
                }
            }

            // حالة عدم وجود أحداث
            if (todayDeviceEvents.isEmpty() && todayAppEvents.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Event,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "لا توجد مواعيد أو أحداث مجدولة لهذا اليوم",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "يمكنك إضافة حدث جديد أو مزامنة تقويم الهاتف لعرض مواعيدك هنا",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // 6. جدول مواقيت الصلاة لليوم المختار
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
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
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Mosque, contentDescription = null, tint = EmeraldDeep, modifier = Modifier.size(20.dp))
                                Text(
                                    text = "مواقيت الصلاة لليوم المختار",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = EmeraldDeep
                                )
                            }
                            Text(
                                text = currentCity.arabicName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // عرض الصلوات الخمس والشروق
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

    // نافذة إضافة حدث جديد
    if (showAddEventDialog) {
        AddEventDialog(
            initialDateKey = selectedDateKey,
            onDismiss = { showAddEventDialog = false },
            onConfirm = { title, desc, loc, dateKey, start, end, isAllDay, cat, colorHex, syncToDevice ->
                viewModel?.addAppEvent(
                    title = title,
                    description = desc,
                    location = loc,
                    dateKey = dateKey,
                    startTime = start,
                    endTime = end,
                    colorHex = colorHex,
                    isAllDay = isAllDay,
                    category = cat,
                    syncToDevice = syncToDevice,
                    context = context
                )
                showAddEventDialog = false
            }
        )
    }

    // تأكيد حذف الحدث
    deletingEventId?.let { id ->
        AlertDialog(
            onDismissRequest = { deletingEventId = null },
            title = { Text("حذف الحدث") },
            text = { Text("هل أنت متأكد من حذف هذا الحدث من التقويم؟") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel?.deleteAppEvent(id)
                        deletingEventId = null
                    }
                ) {
                    Text("حذف", color = PriorityHighRed, fontWeight = FontWeight.Bold)
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

/**
 * بطاقة عرض حدث تقويم الهاتف (سامسونج / Google)
 */
@Composable
private fun DeviceCalendarEventCard(event: CalendarEventItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFEFF6FF)
        ),
        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF2563EB)
                    ) {
                        Text(
                            text = event.calendarDisplayName.ifBlank { "تقويم سامسونج" },
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    if (event.isAllDay) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFDBEAFE)
                        ) {
                            Text(
                                text = "طوال اليوم",
                                color = Color(0xFF1E40AF),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (!event.isAllDay && event.startTimeFormatted.isNotBlank()) {
                    Text(
                        text = if (event.endTimeFormatted.isNotBlank()) "${event.startTimeFormatted} - ${event.endTimeFormatted}" else event.startTimeFormatted,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E40AF)
                    )
                }
            }

            Text(
                text = event.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E3A8A)
            )

            if (event.location.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(14.dp))
                    Text(event.location, style = MaterialTheme.typography.bodySmall, color = Color(0xFF1E40AF))
                }
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
        }
    }
}

/**
 * بطاقة عرض حدث محلي للتطبيق
 */
@Composable
private fun AppEventCard(
    event: AppEventEntity,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = event.category.ifBlank { "عام" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }

                    if (event.isAllDay) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "طوال اليوم",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!event.isAllDay && event.startTime.isNotBlank()) {
                        Text(
                            text = if (event.endTime.isNotBlank()) "${event.startTime} - ${event.endTime}" else event.startTime,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف الحدث", tint = PriorityHighRed, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Text(
                text = event.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (event.location.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                    Text(event.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
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
        }
    }
}

/**
 * نافذة حوار إضافة حدث جديد
 */
@Composable
private fun AddEventDialog(
    initialDateKey: String,
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
        syncToDevice: Boolean
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var dateKey by remember { mutableStateOf(initialDateKey) }
    var isAllDay by remember { mutableStateOf(false) }
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("10:00") }
    var category by remember { mutableStateOf("عام") }
    var syncToDevice by remember { mutableStateOf(true) }

    val categories = listOf("عام", "عمل", "دراسة", "ديني", "عائلي", "صحة")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "إضافة حدث جديد للتقويم 📅",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان الحدث *") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("event_title_input")
                )

                OutlinedTextField(
                    value = dateKey,
                    onValueChange = { dateKey = it },
                    label = { Text("التاريخ (YYYY-MM-DD)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("حدث طوال اليوم", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Switch(checked = isAllDay, onCheckedChange = { isAllDay = it })
                }

                if (!isAllDay) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            label = { Text("وقت البدء") },
                            placeholder = { Text("HH:mm") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = endTime,
                            onValueChange = { endTime = it },
                            label = { Text("وقت الانتهاء") },
                            placeholder = { Text("HH:mm") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("الموقع / المكان (اختياري)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("ملاحظات / وصف (اختياري)") },
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // اختيار الفئة
                Text("الفئة:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 12.sp) }
                        )
                    }
                }

                // خيار الحفظ في تقويم الهاتف
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("حفظ أيضاً في تقويم سامسونج / الهاتف 📱", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Checkbox(checked = syncToDevice, onCheckedChange = { syncToDevice = it })
                }

                // أزرار الحفظ والإلغاء
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onConfirm(
                                    title,
                                    description,
                                    location,
                                    dateKey,
                                    startTime,
                                    endTime,
                                    isAllDay,
                                    category,
                                    "#059669",
                                    syncToDevice
                                )
                            }
                        },
                        enabled = title.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_event_btn")
                    ) {
                        Text("حفظ الحدث", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
