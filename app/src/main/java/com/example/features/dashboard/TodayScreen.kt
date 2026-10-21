package com.example.features.dashboard

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.health.HealthGoalMetric
import com.example.core.prayer.UmmAlQuraCalculator
import com.example.data.HabitEntity
import com.example.data.HabitLogEntity
import com.example.data.TaskEntity
import com.example.features.DailyManagerUiState
import com.example.features.tasks.QuickAddTaskBottomSheet
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldSoft
import com.example.ui.theme.MintAccent
import com.example.ui.theme.PriorityHighRed
import com.example.ui.theme.PriorityLowGreen
import com.example.ui.theme.PriorityMediumAmber
import com.example.core.prayer.AthkarDataProvider
import com.example.core.prayer.PrayerType
import com.example.features.prayer.InteractiveAthkarReaderDialog
import java.util.Calendar
import java.util.TimeZone

@Composable
fun TodayScreen(
    uiState: DailyManagerUiState,
    onOpenReligiousTab: () -> Unit,
    onSetCheckboxHabit: (HabitEntity, Boolean) -> Unit,
    onIncrementValueHabit: (HabitEntity) -> Unit,
    onStartHabitTimer: (HabitEntity) -> Unit,
    onToggleTask: (String) -> Unit,
    onAddTaskDirect: (
        title: String,
        notes: String,
        category: String,
        priority: String,
        alarmTime24: String,
        alarmEnabled: Boolean,
        alarmType: String,
        verifyMethod: String,
        placeName: String,
        qrCode: String,
        categoryId: String,
        dueDateText: String
    ) -> Unit,
    onHardwareStepsDetected: (Int) -> Unit,
    onNavigateToTab: (Int) -> Unit,
    onOpenAiAssistant: () -> Unit = {},
    onOpenCalendar: () -> Unit = {},
    onOpenProductivityTab: (Int) -> Unit = {},
    onCompleteAthkar: (String) -> Unit = {},
    onOpenQuran: () -> Unit = onOpenReligiousTab,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val todayKey = uiState.prayerSchedule.dateKey
    var showAddTaskModal by remember { mutableStateOf(false) }
    var activeAthkarCategory by remember { mutableStateOf<String?>(null) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val rawValue = event?.values?.firstOrNull()?.toInt() ?: return
                onHardwareStepsDetected(rawValue)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (stepSensor != null) {
            sensorManager.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    val nowCal = remember { Calendar.getInstance(TimeZone.getTimeZone("GMT+03:00")) }
    val currentMinutesOfDay = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)
    val todayDayOfWeek = nowCal.get(Calendar.DAY_OF_WEEK)

    val fajrSlot = uiState.prayerSchedule.prayers.firstOrNull { it.type == PrayerType.FAJR }
    val asrSlot = uiState.prayerSchedule.prayers.firstOrNull { it.type == PrayerType.ASR }

    val fajrMinutes = fajrSlot?.let { it.hour24 * 60 + it.minute } ?: (4 * 60 + 30)
    val asrMinutes = asrSlot?.let { it.hour24 * 60 + it.minute } ?: (15 * 60 + 30)
    val eightPmMinutes = 20 * 60

    val morningDone = uiState.todayAzkarLogs.firstOrNull { it.categoryCode == "MORNING" }?.isCompleted == true
    val eveningDone = uiState.todayAzkarLogs.firstOrNull { it.categoryCode == "EVENING" }?.isCompleted == true

    val isMorningWindow = currentMinutesOfDay in fajrMinutes until asrMinutes
    val showMorningAdhkar = isMorningWindow && !morningDone

    val isEveningWindow = currentMinutesOfDay in asrMinutes until eightPmMinutes
    val showEveningAdhkar = isEveningWindow && !eveningDone

    val todayAppEvents = remember(uiState.appEvents, todayKey) {
        uiState.appEvents.filter { it.dateKey == todayKey }
    }
    val todayDeviceEvents = remember(uiState.deviceCalendarEvents, todayKey) {
        uiState.deviceCalendarEvents.filter { it.dateKey == todayKey }
    }
    val hasEventToday = todayAppEvents.isNotEmpty() || todayDeviceEvents.isNotEmpty()

    // استثناء العادات المنجزة لليوم وإظهار غير المنجزة فقط المجدولة لليوم حتى حد 3 عادات
    val habitsSnapshot = remember(uiState.habits, uiState.habitLogsByHabitAndDate, todayKey, todayDayOfWeek) {
        uiState.habits.filter { habit ->
            if (habit.deletedAt != null) return@filter false
            val isScheduledToday = when (habit.frequencyType) {
                "DAILY" -> true
                "SPECIFIC_DAYS_WEEK" -> {
                    val days = habit.frequencyDaysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }
                    days.isEmpty() || days.contains(todayDayOfWeek)
                }
                else -> true
            }
            val log = uiState.habitLogsByHabitAndDate[habit.id]?.get(todayKey)
            val isCompleted = log?.isCompleted == true
            isScheduledToday && !isCompleted
        }.sortedWith(
            compareBy<HabitEntity> {
                if (it.alarmTime24.isNotBlank()) it.alarmTime24 else "99:99"
            }
        ).take(3)
    }

    val tasksSnapshot = remember(uiState.mainTasks) {
        uiState.mainTasks.filter { !it.isCompleted }
            .sortedWith(
                compareBy<TaskEntity> {
                    when (it.priority) {
                        "HIGH" -> 0
                        "MEDIUM" -> 1
                        else -> 2
                    }
                }.thenBy {
                    if (it.alarmTime24.isNotBlank()) it.alarmTime24 else "99:99"
                }
            ).take(3)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("today_screen_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. بطاقة الصلاة المصغرة (الوقت المتبقي هو العنصر الأبرز، واسم الصلاة فقط)
        item {
            CompactPrayerCard(
                uiState = uiState,
                onOpenReligiousTab = onOpenReligiousTab
            )
        }

        // بطاقة الأذكار الذكية (مشروطة بنافذة الصباح أو المساء وعدم الإنجاز فقط)
        if (showMorningAdhkar) {
            item {
                SmartAdhkarCard(
                    categoryTitle = "أذكار الصباح",
                    subtitle = "ابدأ يومك بذكر الله وحصنك اليومي",
                    onClick = { activeAthkarCategory = "MORNING" }
                )
            }
        } else if (showEveningAdhkar) {
            item {
                SmartAdhkarCard(
                    categoryTitle = "أذكار المساء",
                    subtitle = "حافظ على أذكار المساء لتحصين نفسك حتى الصباح",
                    onClick = { activeAthkarCategory = "EVENING" }
                )
            }
        }

        // بطاقة المساعد الذكي التعبيري (M3 Expressive AI Hero Card - دون أي تعديل)
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenAiAssistant() }
                    .testTag("today_ai_assistant_banner"),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f),
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.tertiary,
                                        Color(0xFF8B5CF6),
                                        Color(0xFFEC4899)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "مساعد الذكاء الاصطناعي",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Surface(
                                shape = RoundedCornerShape(100.dp),
                                color = MaterialTheme.colorScheme.tertiary
                            ) {
                                Text(
                                    text = "صوت وكتابة",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "مساعدك الذكي لإدارة يومك",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "فتح المحادثة",
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // بطاقة موعد اليوم في التقويم (مشروطة بوجود موعد مسجل لليوم، وتختفي تماماً عند عدم وجود مواعيد)
        if (hasEventToday) {
            item {
                val firstEventTitle = todayAppEvents.firstOrNull()?.title
                    ?: todayDeviceEvents.firstOrNull()?.title.orEmpty()
                val firstEventTime = todayAppEvents.firstOrNull()?.let {
                    if (it.isAllDay) "طوال اليوم" else "${it.startTime} - ${it.endTime}"
                } ?: todayDeviceEvents.firstOrNull()?.let {
                    if (it.isAllDay) "طوال اليوم" else it.startTimeFormatted
                }.orEmpty()
                val moreCount = (todayAppEvents.size + todayDeviceEvents.size - 1).coerceAtLeast(0)

                TodayCalendarAndEventsCard(
                    eventTitle = firstEventTitle,
                    eventTime = firstEventTime,
                    moreEventsCount = moreCount,
                    onOpenCalendar = {
                        onOpenProductivityTab(2)
                        onOpenCalendar()
                    }
                )
            }
        }

        // عادات اليوم (بحد أقصى 3 عادات مرتبة بالأولوية وأقرب موعد)
        item {
            SectionHeaderRow(
                title = "العادات",
                actionText = "عرض الكل",
                onActionClick = {
                    onOpenProductivityTab(0)
                    onNavigateToTab(2)
                }
            )
        }

        if (habitsSnapshot.isEmpty()) {
            item {
                EmptySectionCard(
                    title = "لا توجد عادات مجدولة لليوم",
                    buttonText = "إضافة عادة",
                    onButtonClick = {
                        onOpenProductivityTab(0)
                        onNavigateToTab(2)
                    }
                )
            }
        } else {
            items(habitsSnapshot, key = { "today_habit_${it.id}" }) { habit ->
                val todayLog = uiState.habitLogsByHabitAndDate[habit.id]?.get(todayKey)
                TodayHabitRowCard(
                    habit = habit,
                    todayLog = todayLog,
                    onSetCheckbox = { checked -> onSetCheckboxHabit(habit, checked) },
                    onIncrementValue = { onIncrementValueHabit(habit) },
                    onOpenTimer = { onStartHabitTimer(habit) },
                    onOpenHabitsTab = {
                        onOpenProductivityTab(0)
                        onNavigateToTab(2)
                    },
                    onOpenReligiousTab = onOpenReligiousTab,
                    onOpenQuran = onOpenQuran
                )
            }
        }

        // مهام اليوم (بحد أقصى 3 مهام عاجلة/قادمة مرتبة بالأولوية والأقرب موعداً)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "المهام",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                TextButton(onClick = {
                    onOpenProductivityTab(1)
                    onNavigateToTab(2)
                }) {
                    Text("عرض الكل", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (tasksSnapshot.isEmpty()) {
            item {
                EmptySectionCard(
                    title = "لا توجد مهام قادمة",
                    buttonText = "إضافة مهمة",
                    onButtonClick = { showAddTaskModal = true }
                )
            }
        } else {
            items(tasksSnapshot, key = { "today_task_${it.id}" }) { task ->
                TodayTaskCard(
                    task = task,
                    onToggle = { onToggleTask(task.id) }
                )
            }
        }
    }

    if (showAddTaskModal) {
        QuickAddTaskBottomSheet(
            parentTask = null,
            categories = uiState.categories,
            onDismiss = { showAddTaskModal = false },
            onConfirm = { title, notes, category, priority, alarmTime, alarmEnabled, alarmType, verifyMethod, placeName, qrCode, categoryId, dueDateText ->
                onAddTaskDirect(
                    title,
                    notes,
                    category,
                    priority,
                    alarmTime,
                    alarmEnabled,
                    alarmType,
                    verifyMethod,
                    placeName,
                    qrCode,
                    categoryId,
                    dueDateText
                )
                showAddTaskModal = false
            }
        )
    }

    activeAthkarCategory?.let { catCode ->
        val catTitle = if (catCode == "MORNING") "أذكار الصباح" else "أذكار المساء"
        val dhikrList = AthkarDataProvider.getAthkarByCategory(catCode)
        InteractiveAthkarReaderDialog(
            categoryCode = catCode,
            categoryTitle = catTitle,
            items = dhikrList,
            onFinish = {
                onCompleteAthkar(catCode)
                activeAthkarCategory = null
            },
            onDismiss = {
                activeAthkarCategory = null
            }
        )
    }
}

/**
 * بطاقة الصلاة المصغرة:
 * - الوقت المتبقي على الصلاة التالية هو العنصر الأكبر والأهم
 * - اسم الصلاة فقط بدون جملة «الصلاة القادمة» وبدون اسم المدينة
 * - أي صلاة مضى وقتها دون تسجيل تظهر بحالة «متأخر»
 */
@Composable
fun CompactPrayerCard(
    uiState: DailyManagerUiState,
    onOpenReligiousTab: () -> Unit
) {
    val schedule = uiState.prayerSchedule

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenReligiousTab() }
            .testTag("prayer_hero_widget"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = EmeraldDeep)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(EmeraldDeep, Color(0xFF0A2E23))
                    )
                )
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // المتبقي على الصلاة
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = schedule.countdownFormatted,
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 32.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = MintAccent
                    )
                    Text(
                        text = "المتبقي على الأذان",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Bold
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.12f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = schedule.nextPrayer.type.arabicName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = schedule.nextPrayer.formatted12h,
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldSoft,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayHabitRowCard(
    habit: HabitEntity,
    todayLog: HabitLogEntity?,
    onSetCheckbox: (Boolean) -> Unit,
    onIncrementValue: () -> Unit,
    onOpenTimer: () -> Unit,
    onOpenHabitsTab: () -> Unit,
    onOpenReligiousTab: () -> Unit = {},
    onOpenQuran: () -> Unit = onOpenReligiousTab
) {
    val completedCount = todayLog?.completedCount ?: 0
    val targetCount = habit.targetCount.coerceAtLeast(1)
    val isDone = todayLog?.isCompleted == true
    val progress = (completedCount.toFloat() / targetCount.toFloat()).coerceIn(0f, 1f)

    val isQuranWirdHabit = habit.autoSource in setOf("QURAN_WIRD", "WIRD")
    val isAzkarHabit = habit.autoSource in setOf("AZKAR_MORNING", "AZKAR_EVENING", "AZKAR")
    val isCheckbox = habit.autoSource == "CHECKBOX" && !isAzkarHabit && !isQuranWirdHabit
    val isValueHabit = habit.autoSource == "TARGET_VALUE"
    val isTimerHabit = habit.autoSource == "STUDY_TIMER" || habit.autoSource == "STUDY_MINUTES"
    val isHealthHabit = HealthGoalMetric.isHealthSource(habit.autoSource)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                when {
                    isQuranWirdHabit -> onOpenQuran()
                    isAzkarHabit -> onOpenReligiousTab()
                    isCheckbox -> onSetCheckbox(!isDone)
                    isTimerHabit -> onOpenTimer()
                    isValueHabit -> onIncrementValue()
                    else -> onOpenHabitsTab()
                }
            }
            .testTag("today_habit_card_${habit.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = habit.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (isQuranWirdHabit) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = EmeraldDeep.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "المصحف",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = EmeraldDeep,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        if (habit.penaltyLockApps) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "قفل العقاب",
                                tint = PriorityHighRed,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        if (habit.alarmEnabled && habit.alarmTime24.isNotBlank()) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    if (isQuranWirdHabit) {
                        Text(
                            text = if (isDone) "تم إنجاز الورد بوضع علامة التوقف بالمصحف" else "يكتمل آلياً عند وضع علامة التوقف بالمصحف",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDone) EmeraldDeep else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (!isCheckbox) {
                        Text(
                            text = UmmAlQuraCalculator.toArabicDigits("$completedCount / $targetCount ${habit.unitLabel}"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                when {
                    isQuranWirdHabit -> {
                        if (isDone) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = EmeraldDeep.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldDeep, modifier = Modifier.size(14.dp))
                                    Text("تم", color = EmeraldDeep, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        } else {
                            FilledTonalButton(
                                onClick = onOpenQuran,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = EmeraldDeep.copy(alpha = 0.15f),
                                    contentColor = EmeraldDeep
                                )
                            ) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("اذهب للمصحف", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    isAzkarHabit -> {
                        if (isDone) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = EmeraldDeep.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldDeep, modifier = Modifier.size(14.dp))
                                    Text("تم", color = EmeraldDeep, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        } else {
                            FilledTonalButton(
                                onClick = onOpenReligiousTab,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = EmeraldDeep.copy(alpha = 0.15f),
                                    contentColor = EmeraldDeep
                                )
                            ) {
                                Icon(Icons.Default.Mosque, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("الأذكار", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    isCheckbox -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = { onSetCheckbox(true) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "إنجاز",
                                    tint = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            }
                            IconButton(
                                onClick = { onSetCheckbox(false) },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Cancel,
                                    contentDescription = "عدم إنجاز",
                                    tint = if (!isDone && todayLog != null) PriorityHighRed else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                    isValueHabit -> {
                        FilledTonalButton(
                            onClick = onIncrementValue,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    isTimerHabit -> {
                        FilledTonalButton(
                            onClick = onOpenTimer,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("العداد", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                    isHealthHabit -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "تلقائي",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            if (!isCheckbox) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(CircleShape)
                )
            }
        }
    }
}

@Composable
private fun TodayTaskCard(
    task: TaskEntity,
    onToggle: () -> Unit
) {
    val priorityColor = when (task.priority) {
        "HIGH" -> PriorityHighRed
        "MEDIUM" -> PriorityMediumAmber
        else -> PriorityLowGreen
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .testTag("today_task_card_${task.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) {
                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = if (task.isCompleted) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (task.isCompleted) MaterialTheme.colorScheme.primary else priorityColor
                )
                Column {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                    if (task.alarmEnabled && task.alarmTime24.isNotBlank()) {
                        Text(
                            text = "منبه: ${task.alarmTime24}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(priorityColor)
            )
        }
    }
}

@Composable
private fun SectionHeaderRow(
    title: String,
    actionText: String,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold
        )
        TextButton(onClick = onActionClick) {
            Text(actionText, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptySectionCard(
    title: String,
    buttonText: String,
    onButtonClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FilledTonalButton(onClick = onButtonClick) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(buttonText, fontWeight = FontWeight.Bold)
            }
        }
    }
}



@Composable
private fun TodayCalendarAndEventsCard(
    eventTitle: String,
    eventTime: String,
    moreEventsCount: Int,
    onOpenCalendar: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenCalendar() }
            .testTag("today_calendar_events_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = if (eventTitle.isNotBlank()) eventTitle else "موعد اليوم في التقويم",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (eventTime.isNotBlank()) eventTime else "عرض التفاصيل في التقويم",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (moreEventsCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "+$moreEventsCount",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SmartAdhkarCard(
    categoryTitle: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("smart_adhkar_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.secondary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mosque,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = categoryTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

