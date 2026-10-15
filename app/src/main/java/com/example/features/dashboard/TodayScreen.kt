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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.health.HealthGoalMetric
import com.example.core.prayer.UmmAlQuraCalculator
import com.example.data.HabitEntity
import com.example.data.HabitLogEntity
import com.example.data.TaskEntity
import com.example.features.DailyManagerUiState
import com.example.features.tasks.AddTaskModal
import com.example.features.tasks.QuickAddTaskBottomSheet
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldSoft
import com.example.ui.theme.MintAccent
import com.example.core.google.GmailEmailItem
import com.example.core.google.GoogleCalendarEvent
import com.example.ui.theme.PriorityHighRed
import com.example.ui.theme.PriorityLowGreen
import com.example.ui.theme.PriorityMediumAmber

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
    onOpenGoogleSettings: () -> Unit = {},
    onRefreshGoogle: () -> Unit = {},
    onAddEmailTask: (GmailEmailItem) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val todayKey = uiState.prayerSchedule.dateKey
    var showAddTaskModal by remember { mutableStateOf(false) }

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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("today_screen_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. بطاقة الصلاة المصغرة (الوقت المتبقي هو العنصر الأبرز، واسم الصلاة فقط بدون كلمة الصلاة القادمة أو اسم المدينة)
        item {
            CompactPrayerCard(
                uiState = uiState,
                onOpenReligiousTab = onOpenReligiousTab
            )
        }

        // بطاقة المساعد الذكي التعبيري (M3 Expressive AI Hero Card)
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
                            text = "لخص يومي • وش أسوي الحين؟ • إدارة المهام والعادات",
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

        // بطاقة Google Workspace (Calendar & Gmail)
        item {
            GoogleWorkspaceTodayCard(
                uiState = uiState,
                onOpenSettings = onOpenGoogleSettings,
                onRefresh = onRefreshGoogle,
                onAddEmailTask = onAddEmailTask
            )
        }

        // 2. عادات اليوم
        item {
            SectionHeaderRow(
                title = "العادات",
                actionText = "عرض الكل",
                onActionClick = { onNavigateToTab(2) }
            )
        }

        if (uiState.habits.isEmpty()) {
            item {
                EmptySectionCard(
                    title = "لا توجد عادات مضافة",
                    buttonText = "إضافة عادة",
                    onButtonClick = { onNavigateToTab(2) }
                )
            }
        } else {
            items(uiState.habits, key = { "today_habit_${it.id}" }) { habit ->
                val todayLog = uiState.habitLogsByHabitAndDate[habit.id]?.get(todayKey)
                TodayHabitRowCard(
                    habit = habit,
                    todayLog = todayLog,
                    onSetCheckbox = { checked -> onSetCheckboxHabit(habit, checked) },
                    onIncrementValue = { onIncrementValueHabit(habit) },
                    onOpenTimer = { onStartHabitTimer(habit) },
                    onOpenHabitsTab = { onNavigateToTab(2) }
                )
            }
        }

        // 3. مهام اليوم (زر إضافة مهمة يفتح نافذة إنشاء مهمة جديدة مباشرة في الرئيسية)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilledTonalButton(
                        onClick = { showAddTaskModal = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("today_add_task_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة مهمة", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                    if (uiState.mainTasks.isNotEmpty()) {
                        TextButton(onClick = { onNavigateToTab(3) }) {
                            Text("الكل", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (uiState.mainTasks.isEmpty()) {
            item {
                EmptySectionCard(
                    title = "لا توجد مهام مضافة",
                    buttonText = "إضافة مهمة",
                    onButtonClick = { showAddTaskModal = true }
                )
            }
        } else {
            items(uiState.mainTasks.take(6), key = { "today_task_${it.id}" }) { task ->
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
    val obligatorySlots = schedule.prayers.filter { it.type.isObligatory }

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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // العنصر الأهم والأكبر: كم باقي على الصلاة التالية
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = schedule.countdownFormatted,
                        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = MintAccent
                    )
                    Text(
                        text = "المتبقي على الأذان",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }

                // اسم الصلاة فقط وموعدها بدون «الصلاة القادمة» وبدون اسم المدينة
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.12f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                obligatorySlots.forEach { slot ->
                    val status = uiState.todayPrayerStatusMap[slot.type] ?: "PENDING"
                    val isPrayedOnTime = status == "ON_TIME"
                    val isLate = status == "LATE"
                    val isNext = slot.type == schedule.nextPrayer.type

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    isPrayedOnTime -> MintAccent.copy(alpha = 0.28f)
                                    isLate -> PriorityHighRed.copy(alpha = 0.24f)
                                    else -> Color.White.copy(alpha = 0.08f)
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = when {
                                    isPrayedOnTime -> MintAccent
                                    isLate -> PriorityHighRed.copy(alpha = 0.85f)
                                    isNext -> GoldSoft
                                    else -> Color.Transparent
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(vertical = 6.dp, horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = slot.type.arabicName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = slot.formatted12h.substringBefore(" "),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.78f)
                        )
                        when {
                            isPrayedOnTime -> {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "تمت الصلاة",
                                    tint = MintAccent,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            isLate -> {
                                Text(
                                    text = "متأخر",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFCA5A5)
                                )
                            }
                        }
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
    onOpenHabitsTab: () -> Unit
) {
    val completedCount = todayLog?.completedCount ?: 0
    val targetCount = habit.targetCount.coerceAtLeast(1)
    val isDone = todayLog?.isCompleted == true
    val progress = (completedCount.toFloat() / targetCount.toFloat()).coerceIn(0f, 1f)

    val isCheckbox = habit.autoSource == "CHECKBOX"
    val isValueHabit = habit.autoSource == "TARGET_VALUE"
    val isTimerHabit = habit.autoSource == "STUDY_TIMER" || habit.autoSource == "STUDY_MINUTES"
    val isHealthHabit = HealthGoalMetric.isHealthSource(habit.autoSource)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                when {
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

                    if (!isCheckbox) {
                        Text(
                            text = UmmAlQuraCalculator.toArabicDigits("$completedCount / $targetCount ${habit.unitLabel}"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                when {
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
fun GoogleWorkspaceTodayCard(
    uiState: DailyManagerUiState,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    onAddEmailTask: (GmailEmailItem) -> Unit
) {
    if (uiState.isGoogleConnected) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("google_workspace_today_card"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Event,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "تقويم Google وبريد Gmail",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            uiState.googleUserEmail?.let { email ->
                                Text(
                                    text = email,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "تحديث",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // أحداث التقويم لليوم
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📅 مواعيد اليوم في التقويم (${uiState.googleCalendarEvents.size})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (uiState.googleCalendarEvents.isEmpty()) {
                    Text(
                        text = "لا توجد مواعيد مجدولة لليوم في تقويمك ✨",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        uiState.googleCalendarEvents.take(4).forEach { event ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = event.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                    ) {
                                        Text(
                                            text = event.startTimeFormatted.ifBlank { "طوال اليوم" },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // رسائل Gmail والمهام المقترحة
                val actionableEmails = uiState.gmailMessages.filter { it.extractedTaskTitle != null }
                if (actionableEmails.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    Text(
                        text = "✉️ مهام مستخرجة من Gmail (${actionableEmails.size})",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        actionableEmails.take(3).forEach { mail ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = mail.extractedTaskTitle ?: mail.subject,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            modifier = Modifier.weight(1f)
                                        )
                                        FilledTonalButton(
                                            onClick = { onAddEmailTask(mail) },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("إضافة كمهمة", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                    Text(
                                        text = "من: ${mail.sender}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // بطاقة دعوة للربط
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenSettings() }
                .testTag("google_workspace_connect_banner"),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "ربط تقويم Google و Gmail",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "مزامنة مواعيد اليوم في التقويم واستخراج المهام الذكية من بريدك تلقائياً.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = onOpenSettings,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("ربط", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
