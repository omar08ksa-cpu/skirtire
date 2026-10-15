package com.example.features.habits

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.core.habits.HabitChecklistItem
import com.example.core.habits.HabitDayStatus
import com.example.core.habits.HabitFrequencyType
import com.example.core.habits.HabitGoalComparison
import com.example.core.habits.HabitHelper
import com.example.core.health.HealthGoalMetric
import com.example.core.prayer.UmmAlQuraCalculator
import com.example.core.protection.AppUsageMonitor
import com.example.core.protection.AppUsageStatItem
import com.example.core.protection.DeviceUsageOverview
import com.example.core.protection.PlacePhotoHelper
import com.example.data.BlockScheduleEntity
import com.example.data.BlockedAppEntity
import com.example.data.CategoryEntity
import com.example.data.HabitEntity
import com.example.data.HabitLogEntity
import com.example.data.SubjectEntity
import com.example.features.DailyManagerUiState
import com.example.features.DailyManagerViewModel
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.MintAccent
import com.example.ui.theme.PriorityHighRed
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitsScreen(
    uiState: DailyManagerUiState,
    viewModel: DailyManagerViewModel,
    onStartHabitTimer: (HabitEntity) -> Unit,
    onNavigateToReligious: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedSection by rememberSaveable { mutableIntStateOf(0) } // 0 = العادات اليومية، 1 = السجل والإحصائيات
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAlarmHabit by remember { mutableStateOf<HabitEntity?>(null) }
    var editingPenaltyHabit by remember { mutableStateOf<HabitEntity?>(null) }
    var editingValueHabit by remember { mutableStateOf<HabitEntity?>(null) }
    var selectedHabitForDetail by remember { mutableStateOf<HabitEntity?>(null) }
    var showCategoryManagerDialog by remember { mutableStateOf(false) }
    var showQuranWirdManualBlockDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshAppUsageOverview()
    }

    val last14Days = remember(uiState.prayerSchedule.dateKey) {
        UmmAlQuraCalculator.lastNDaysDateKeys(14)
    }
    val todayKey = uiState.prayerSchedule.dateKey

    // إذا تم اختيار عادة لعرض صفحتها الخاصة الكاملة والتقويم
    selectedHabitForDetail?.let { detailHabit ->
        val currentHabit = uiState.habits.find { it.id == detailHabit.id } ?: detailHabit
        val logsForHabit = uiState.habitLogsByHabitAndDate[currentHabit.id].orEmpty()
        HabitDetailScreen(
            habit = currentHabit,
            logsByDate = logsForHabit,
            categories = uiState.categories,
            todayKey = todayKey,
            onBack = { selectedHabitForDetail = null },
            onSetCheckbox = { checked -> viewModel.setCheckboxHabitState(currentHabit, checked) },
            onUpdateValueProgress = { v -> viewModel.updateValueHabitExact(currentHabit, v) },
            onRecordRestSkip = { viewModel.recordHabitRestSkip(currentHabit, todayKey) },
            onUpdateChecklistItem = { itemId, chk, delta, sVal ->
                viewModel.updateHabitChecklistItem(currentHabit, todayKey, itemId, chk, delta, sVal)
            },
            onStartTimer = { onStartHabitTimer(currentHabit) },
            onDeleteHabit = {
                viewModel.deleteHabit(currentHabit.id)
                selectedHabitForDetail = null
            }
        )
        return
    }

    val filteredHabits = remember(uiState.habits, uiState.selectedCategoryId) {
        if (uiState.selectedCategoryId.isBlank()) uiState.habits
        else uiState.habits.filter { it.categoryId == uiState.selectedCategoryId }
    }
    val hasQuranWirdHabit = remember(uiState.habits) {
        uiState.habits.any { it.autoSource in setOf("QURAN_WIRD", "WIRD") }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            if (selectedSection == 0) {
                ExtendedFloatingActionButton(
                    onClick = {
                        viewModel.refreshAppUsageOverview()
                        showAddDialog = true
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("إضافة عادة", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_habit_fab")
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("habits_screen_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // شريط التبديل بين العادات اليومية والسجل والإحصائيات
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedSection == 0,
                        onClick = { selectedSection = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = {
                            Icon(
                                Icons.Default.TrackChanges,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    ) {
                        Text("العادات اليومية", fontWeight = FontWeight.Bold)
                    }
                    SegmentedButton(
                        selected = selectedSection == 1,
                        onClick = { selectedSection = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = {
                            Icon(
                                Icons.Default.QueryStats,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    ) {
                        Text("السجل والإحصائيات", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // شريط الفئات (يونيفرسل)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = uiState.selectedCategoryId.isBlank(),
                        onClick = { viewModel.selectCategoryFilter("") },
                        label = { Text("الكل", fontWeight = FontWeight.Bold) }
                    )
                    for (cat in uiState.categories) {
                        val isSelected = uiState.selectedCategoryId == cat.id
                        val catColor = CategoryVisuals.parseColor(cat.colorHex)
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectCategoryFilter(cat.id) },
                            label = { Text(cat.name, fontWeight = FontWeight.Bold) },
                            leadingIcon = {
                                Icon(
                                    CategoryVisuals.getIcon(cat.iconKey),
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else catColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                    InputChip(
                        selected = false,
                        onClick = { showCategoryManagerDialog = true },
                        label = { Text("+ إدارة الفئات", fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Category,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }

            if (selectedSection == 0) {
                if (!hasQuranWirdHabit && uiState.selectedCategoryId.isBlank()) {
                    item {
                        QuranWirdHabitPromoCard(
                            onAddWirdHabit = { viewModel.toggleQuranWirdHabit(true) },
                            onNavigateToQuran = onNavigateToReligious
                        )
                    }
                }

                if (filteredHabits.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrackChanges,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = if (uiState.selectedCategoryId.isNotBlank()) "لا توجد عادات في هذه الفئة" else "لا توجد عادات مضافة",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Button(
                                    onClick = {
                                        viewModel.refreshAppUsageOverview()
                                        showAddDialog = true
                                    },
                                    modifier = Modifier.testTag("empty_add_habit_btn")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("إضافة عادة جديدة", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    items(filteredHabits, key = { it.id }) { habit ->
                        val logsForHabit = uiState.habitLogsByHabitAndDate[habit.id].orEmpty()
                        val todayLog = logsForHabit[todayKey]
                        HabitItemCard(
                            habit = habit,
                            todayLog = todayLog,
                            logsByDate = logsForHabit,
                            last14Days = last14Days,
                            subjects = uiState.subjects,
                            schedules = uiState.blockSchedules,
                            onOpenDetail = {
                                selectedHabitForDetail = habit
                            },
                            onSetCheckbox = { checked ->
                                if (habit.autoSource in setOf("QURAN_WIRD", "WIRD")) {
                                    showQuranWirdManualBlockDialog = true
                                } else {
                                    viewModel.setCheckboxHabitState(habit, checked)
                                }
                            },
                            onIncrementValue = {
                                viewModel.incrementValueHabit(habit, 1)
                            },
                            onEditExactValue = {
                                editingValueHabit = habit
                            },
                            onStartTimer = {
                                onStartHabitTimer(habit)
                            },
                            onEditPenalty = {
                                viewModel.refreshAppUsageOverview()
                                editingPenaltyHabit = habit
                            },
                            onEditAlarm = {
                                editingAlarmHabit = habit
                            },
                            onDelete = {
                                viewModel.deleteHabit(habit.id)
                            },
                            onNavigateToReligious = onNavigateToReligious,
                            onShowWirdBlockNotice = {
                                showQuranWirdManualBlockDialog = true
                            }
                        )
                    }
                }
            } else {
                // قسم السجل والإحصائيات الشامل للعادات
                item {
                    HabitsOverallAnalyticsCard(
                        habits = uiState.habits,
                        logsByHabitAndDate = uiState.habitLogsByHabitAndDate,
                        last14Days = last14Days,
                        todayKey = todayKey
                    )
                }

                if (uiState.habits.isNotEmpty()) {
                    item {
                        Text(
                            text = "إحصائيات كل عادة (آخر ١٤ يوماً)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    items(uiState.habits, key = { "stat_${it.id}" }) { habit ->
                        val logsForHabit = uiState.habitLogsByHabitAndDate[habit.id].orEmpty()
                        HabitSingleStatCard(
                            habit = habit,
                            logsByDate = logsForHabit,
                            last14Days = last14Days
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "السجل اليومي التفصيلي",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    items(last14Days.reversed(), key = { "history_day_$it" }) { dateKey ->
                        HabitDailyHistoryRowCard(
                            dateKey = dateKey,
                            isToday = dateKey == todayKey,
                            habits = uiState.habits,
                            logsByHabitAndDate = uiState.habitLogsByHabitAndDate
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        OrganizedAddHabitDialog(
            categories = uiState.categories,
            subjects = uiState.subjects,
            schedules = uiState.blockSchedules.filter { it.scheduleType != "WAKE_ALARM" },
            blockedApps = uiState.blockedApps,
            usageOverview = uiState.usageOverview,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, freq, target, unit, source, timerType, focusMin, breakMin, rounds, subjectId,
                          penaltyLock, penaltySchedId, penaltyPkgs,
                          alarmEnabled, alarmTime, alarmType, verifyMethod, placeName, qrCode,
                          categoryId, comparisonOperator, frequencyDaysOfWeek, frequencyDaysOfMonth,
                          frequencyDaysOfYear, frequencyPeriodType, frequencyPeriodTimes, frequencyIntervalUnit,
                          frequencyIntervalValue, cycleWorkDays, cycleRestDays, allowedSkipDays,
                          startDateKey, endDateKey, checklistItemsJson, checklistRequiredCount ->
                viewModel.addHabit(
                    title = title,
                    description = "",
                    frequencyType = freq,
                    targetCount = target,
                    unitLabel = unit,
                    autoSource = source,
                    alarmTime24 = alarmTime,
                    alarmEnabled = alarmEnabled,
                    goalMode = "ACHIEVE",
                    timerType = timerType,
                    pomodoroFocusMin = focusMin,
                    pomodoroBreakMin = breakMin,
                    pomodoroRounds = rounds,
                    linkedSubjectId = subjectId,
                    penaltyLockApps = penaltyLock,
                    penaltyScheduleId = penaltySchedId,
                    penaltyBlockedPackages = penaltyPkgs,
                    alarmType = alarmType,
                    alarmVerifyMethod = verifyMethod,
                    alarmPlaceName = placeName,
                    alarmQrCode = qrCode,
                    categoryId = categoryId,
                    comparisonOperator = comparisonOperator,
                    frequencyDaysOfWeek = frequencyDaysOfWeek,
                    frequencyDaysOfMonth = frequencyDaysOfMonth,
                    frequencyDaysOfYear = frequencyDaysOfYear,
                    frequencyPeriodType = frequencyPeriodType,
                    frequencyPeriodTimes = frequencyPeriodTimes,
                    frequencyIntervalUnit = frequencyIntervalUnit,
                    frequencyIntervalValue = frequencyIntervalValue,
                    cycleWorkDays = cycleWorkDays,
                    cycleRestDays = cycleRestDays,
                    cycleStartDateKey = startDateKey,
                    allowedSkipDaysPerWeek = allowedSkipDays,
                    startDateKey = startDateKey,
                    endDateKey = endDateKey,
                    checklistItemsJson = checklistItemsJson,
                    checklistRequiredCount = checklistRequiredCount
                )
                showAddDialog = false
            }
        )
    }

    if (showCategoryManagerDialog) {
        UniversalCategoryManagementDialog(
            categories = uiState.categories,
            onDismiss = { showCategoryManagerDialog = false },
            onAddCategory = { name, color, icon -> viewModel.addCategory(name, color, icon) },
            onUpdateCategory = { id, name, color, icon -> viewModel.updateCategory(id, name, color, icon) },
            onDeleteCategory = { id -> viewModel.deleteCategory(id) }
        )
    }

    if (showQuranWirdManualBlockDialog) {
        AlertDialog(
            onDismissRequest = { showQuranWirdManualBlockDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = EmeraldDeep,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "عادة الورد القرآني اليومي 📖",
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "🚫 التعبئة اليدوية غير مسموحة!",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "وفقاً للنظام، لا يمكن التأشير اليدوي على عادة الورد القرآني إطلاقاً. تكتمل العادة فقط بشكل آلي عندما تقرأ وردك وتضع «علامة التوقف» عند الآية المطلوب منك إنهاؤها في المصحف الشريف.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = EmeraldDeep.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "💡 طريقة الإنجاز: افتح المصحف، واقرأ وردك لليوم، ثم اضغط على الآية التي وقفت عندها واختر «🏁 وضع علامة توقف نهاية ورد اليوم وإنجاز العادة».",
                            style = MaterialTheme.typography.bodySmall,
                            color = EmeraldDeep,
                            modifier = Modifier.padding(10.dp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showQuranWirdManualBlockDialog = false
                        onNavigateToReligious()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep)
                ) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("فتح المصحف للورد 📖", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuranWirdManualBlockDialog = false }) {
                    Text("حسناً، فهمت")
                }
            }
        )
    }

    editingAlarmHabit?.let { habit ->
        CustomizableAlarmDialog(
            title = "منبه العادة: ${habit.title}",
            initialEnabled = habit.alarmEnabled,
            initialTime24 = habit.alarmTime24,
            initialAlarmType = habit.alarmType,
            initialVerifyMethod = habit.alarmVerifyMethod,
            initialPlaceName = habit.alarmPlaceName,
            initialQrCode = habit.alarmQrCode,
            onDismiss = { editingAlarmHabit = null },
            onSave = { time24, enabled, alarmType, verifyMethod, placeName, qrCode ->
                viewModel.updateHabitAlarm(
                    habitId = habit.id,
                    alarmTime24 = time24,
                    alarmEnabled = enabled,
                    alarmType = alarmType,
                    alarmVerifyMethod = verifyMethod,
                    alarmPlaceName = placeName,
                    alarmQrCode = qrCode
                )
                editingAlarmHabit = null
            }
        )
    }

    editingPenaltyHabit?.let { habit ->
        HabitPenaltyConfigDialog(
            habit = habit,
            schedules = uiState.blockSchedules.filter { it.scheduleType != "WAKE_ALARM" },
            blockedApps = uiState.blockedApps,
            usageOverview = uiState.usageOverview,
            onDismiss = { editingPenaltyHabit = null },
            onSave = { enabled, scheduleId, packagesCsv ->
                viewModel.updateHabitPenaltyConfig(
                    habitId = habit.id,
                    penaltyLockApps = enabled,
                    penaltyScheduleId = scheduleId,
                    penaltyBlockedPackages = packagesCsv
                )
                editingPenaltyHabit = null
            }
        )
    }

    editingValueHabit?.let { habit ->
        val currentVal = uiState.habitLogsByHabitAndDate[habit.id]?.get(todayKey)?.completedCount ?: 0
        var valueInput by remember(habit.id) { mutableStateOf(currentVal.toString()) }
        AlertDialog(
            onDismissRequest = { editingValueHabit = null },
            title = { Text(habit.title, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = valueInput,
                    onValueChange = { valueInput = it.filter { c -> c.isDigit() } },
                    label = { Text("القيمة المنجزة اليوم (${habit.unitLabel})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateValueHabitExact(habit, valueInput.toIntOrNull() ?: 0)
                        editingValueHabit = null
                    }
                ) {
                    Text("حفظ")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingValueHabit = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

/**
 * بطاقة الإحصائيات العامة للعادات
 */
@Composable
private fun HabitsOverallAnalyticsCard(
    habits: List<HabitEntity>,
    logsByHabitAndDate: Map<String, Map<String, HabitLogEntity>>,
    last14Days: List<String>,
    todayKey: String
) {
    val totalHabits = habits.size
    val completedToday = habits.count { h ->
        logsByHabitAndDate[h.id]?.get(todayKey)?.isCompleted == true
    }

    val totalPossible14 = (totalHabits * last14Days.size).coerceAtLeast(1)
    val totalCompleted14 = habits.sumOf { h ->
        val map = logsByHabitAndDate[h.id].orEmpty()
        last14Days.count { day -> map[day]?.isCompleted == true }
    }
    val overallRatePercent = if (totalHabits == 0) 0 else ((totalCompleted14 * 100) / totalPossible14)

    val bestStreak = habits.maxOfOrNull { h ->
        calculateCurrentStreak(logsByHabitAndDate[h.id].orEmpty(), last14Days)
    } ?: 0

    val allTimeCompletedCount = logsByHabitAndDate.values.sumOf { dateMap ->
        dateMap.values.count { it.isCompleted }
    }

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
            Text(
                text = "ملخص أداء العادات",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatMetricBox(
                    title = "نسبة الالتزام",
                    value = UmmAlQuraCalculator.toArabicDigits("$overallRatePercent%"),
                    subtitle = "آخر ١٤ يوماً",
                    modifier = Modifier.weight(1f)
                )
                StatMetricBox(
                    title = "إنجاز اليوم",
                    value = UmmAlQuraCalculator.toArabicDigits("$completedToday / $totalHabits"),
                    subtitle = "عادة مكتملة",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatMetricBox(
                    title = "أطول سلسلة",
                    value = UmmAlQuraCalculator.toArabicDigits("$bestStreak"),
                    subtitle = "يوم متتالي",
                    modifier = Modifier.weight(1f)
                )
                StatMetricBox(
                    title = "إجمالي السجلات",
                    value = UmmAlQuraCalculator.toArabicDigits("$allTimeCompletedCount"),
                    subtitle = "إنجاز موثق",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatMetricBox(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HabitSingleStatCard(
    habit: HabitEntity,
    logsByDate: Map<String, HabitLogEntity>,
    last14Days: List<String>
) {
    val completedDays14 = last14Days.count { logsByDate[it]?.isCompleted == true }
    val rate14 = (completedDays14 * 100) / last14Days.size.coerceAtLeast(1)
    val streak = calculateCurrentStreak(logsByDate, last14Days)
    val totalValue14 = last14Days.sumOf { logsByDate[it]?.completedCount ?: 0 }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = UmmAlQuraCalculator.toArabicDigits(
                            "أيام الإنجاز: $completedDays14 من ${last14Days.size} • السلسلة الحالية: $streak يوم"
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = UmmAlQuraCalculator.toArabicDigits("$rate14%"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { (rate14 / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
            )

            if (habit.autoSource != "CHECKBOX") {
                Text(
                    text = UmmAlQuraCalculator.toArabicDigits(
                        "مجموع القيم المسجلة خلال ١٤ يوماً: $totalValue14 ${habit.unitLabel}"
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun HabitDailyHistoryRowCard(
    dateKey: String,
    isToday: Boolean,
    habits: List<HabitEntity>,
    logsByHabitAndDate: Map<String, Map<String, HabitLogEntity>>
) {
    val completedHabits = habits.filter { h ->
        logsByHabitAndDate[h.id]?.get(dateKey)?.isCompleted == true
    }
    val missedHabits = habits.filter { h ->
        logsByHabitAndDate[h.id]?.get(dateKey)?.isCompleted != true
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isToday) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
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
                Text(
                    text = if (isToday) "اليوم ($dateKey)" else UmmAlQuraCalculator.toArabicDigits(dateKey),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = UmmAlQuraCalculator.toArabicDigits(
                        "مكتمل ${completedHabits.size} من ${habits.size}"
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (completedHabits.size == habits.size && habits.isNotEmpty()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = FontWeight.Bold
                )
            }

            if (completedHabits.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    completedHabits.forEach { h ->
                        val log = logsByHabitAndDate[h.id]?.get(dateKey)
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
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = h.title,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = if (h.autoSource == "CHECKBOX") {
                                    "تم الإنجاز"
                                } else {
                                    UmmAlQuraCalculator.toArabicDigits("${log?.completedCount ?: h.targetCount} ${h.unitLabel}")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (missedHabits.isNotEmpty()) {
                Text(
                    text = "غير مكتمل: " + missedHabits.joinToString("، ") { it.title },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun calculateCurrentStreak(
    logsByDate: Map<String, HabitLogEntity>,
    orderedDaysOldestToNewest: List<String>
): Int {
    var streak = 0
    for (day in orderedDaysOldestToNewest.reversed()) {
        if (logsByDate[day]?.isCompleted == true) {
            streak++
        } else {
            break
        }
    }
    return streak
}

@Composable
private fun HabitItemCard(
    habit: HabitEntity,
    todayLog: HabitLogEntity?,
    logsByDate: Map<String, HabitLogEntity>,
    last14Days: List<String>,
    subjects: List<SubjectEntity>,
    schedules: List<BlockScheduleEntity>,
    onOpenDetail: () -> Unit,
    onSetCheckbox: (Boolean) -> Unit,
    onIncrementValue: () -> Unit,
    onEditExactValue: () -> Unit,
    onStartTimer: () -> Unit,
    onEditPenalty: () -> Unit,
    onEditAlarm: () -> Unit,
    onDelete: () -> Unit,
    onNavigateToReligious: () -> Unit = {},
    onShowWirdBlockNotice: () -> Unit = {}
) {
    val completedCount = todayLog?.completedCount ?: 0
    val targetCount = habit.targetCount.coerceAtLeast(1)
    val isDoneToday = todayLog?.isCompleted == true
    val progress = (completedCount.toFloat() / targetCount.toFloat()).coerceIn(0f, 1f)
    val extraBeyondTarget = (completedCount - targetCount).coerceAtLeast(0)

    val isQuranWirdHabit = habit.autoSource in setOf("QURAN_WIRD", "WIRD")
    val isAzkarHabit = habit.autoSource in setOf("AZKAR_MORNING", "AZKAR_EVENING", "AZKAR")
    val isCheckbox = habit.autoSource == "CHECKBOX" && !isAzkarHabit && !isQuranWirdHabit
    val isValueHabit = habit.autoSource == "TARGET_VALUE"
    val isChecklist = habit.autoSource == "CHECKLIST"
    val isTimerHabit = habit.autoSource == "STUDY_TIMER" || habit.autoSource == "STUDY_MINUTES"
    val isHealthHabit = HealthGoalMetric.isHealthSource(habit.autoSource)

    val linkedSubject = remember(subjects, habit.linkedSubjectId) {
        subjects.find { it.id == habit.linkedSubjectId }
    }
    val linkedSchedule = remember(schedules, habit.penaltyScheduleId) {
        schedules.find { it.id == habit.penaltyScheduleId }
    }

    val typeBadge = when {
        isQuranWirdHabit -> "ورد القرآن 📖"
        habit.autoSource == "AZKAR_MORNING" -> "أذكار الصباح ☀️"
        habit.autoSource == "AZKAR_EVENING" -> "أذكار المساء 🌙"
        habit.autoSource == "AZKAR" -> "أذكار دينية 🕌"
        isCheckbox -> "صح / خطأ"
        isValueHabit -> "هدف رقمي"
        isChecklist -> "قائمة تحقق"
        habit.autoSource == "MOSQUE_PRAYER" -> "تحقق المسجد"
        isTimerHabit -> if (habit.timerType == "POMODORO") "بومودورو" else "مؤقت تصاعدي"
        isHealthHabit -> "Health Connect"
        else -> "تلقائي"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDetail() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDoneToday) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = habit.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = typeBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (linkedSubject != null) {
                        Text(
                            text = "المادة: ${linkedSubject.name}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (habit.penaltyLockApps && !isDoneToday) {
                        val lockTarget = linkedSchedule?.title ?: "تطبيقات محددة"
                        Text(
                            text = "قفل العقاب مفعل ($lockTarget)",
                            style = MaterialTheme.typography.labelSmall,
                            color = PriorityHighRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (!isCheckbox) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = UmmAlQuraCalculator.toArabicDigits("$completedCount / $targetCount ${habit.unitLabel}"),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (extraBeyondTarget > 0) {
                            Text(
                                text = UmmAlQuraCalculator.toArabicDigits("+$extraBeyondTarget إضافي"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold
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
                        .height(7.dp)
                        .clip(CircleShape)
                )
            }

            // شريط أيام الأسبوع السبعة للعادة (السبت..الجمعة) مع تلوين حالة كل يوم
            val currentWeekDays = remember(logsByDate, habit) {
                val today = LocalDate.now()
                val daysSinceSat = when (today.dayOfWeek.value) {
                    6 -> 0
                    7 -> 1
                    else -> today.dayOfWeek.value + 1
                }
                val saturday = today.minusDays(daysSinceSat.toLong())
                val dayLetters = listOf("س", "ح", "ن", "ث", "ر", "خ", "ج")
                (0..6).map { idx ->
                    val date = saturday.plusDays(idx.toLong())
                    val dKey = date.toString()
                    val log = logsByDate[dKey]
                    val status = HabitHelper.resolveDayStatus(habit, log, dKey, today.toString())
                    val isToday = date == today
                    Triple(dayLetters[idx], status, isToday)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                currentWeekDays.forEach { (letter, status, isToday) ->
                    val (bgColor, textColor, borderColor) = when (status) {
                        HabitDayStatus.COMPLETED -> Triple(Color(0xFF10B981), Color.White, Color(0xFF10B981))
                        HabitDayStatus.PARTIAL -> Triple(Color(0xFFF59E0B), Color.White, Color(0xFFF59E0B))
                        HabitDayStatus.FAILED -> Triple(Color(0xFFEF4444), Color.White, Color(0xFFEF4444))
                        HabitDayStatus.REST_SKIP -> Triple(Color(0xFF3B82F6), Color.White, Color(0xFF3B82F6))
                        HabitDayStatus.UNRECORDED -> Triple(Color.Transparent, MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.outlineVariant)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(bgColor)
                            .border(
                                width = if (isToday) 2.dp else 1.dp,
                                color = if (isToday) MaterialTheme.colorScheme.primary else borderColor,
                                shape = RoundedCornerShape(6.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = letter,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Bold,
                            color = textColor
                        )
                    }
                }
            }

            // أزرار الإنجاز، قفل العقاب، والمنبه الذكي
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when {
                    isQuranWirdHabit -> {
                        if (isDoneToday) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = EmeraldDeep.copy(alpha = 0.15f),
                                modifier = Modifier.clickable { onShowWirdBlockNotice() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldDeep, modifier = Modifier.size(16.dp))
                                    Text("تم إتمام الورد ووضع علامة التوقف ✓", color = EmeraldDeep, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = onNavigateToReligious,
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = EmeraldDeep.copy(alpha = 0.15f),
                                        contentColor = EmeraldDeep
                                    )
                                ) {
                                    Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("قراءة الورد بالمصحف 📖", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.clickable { onShowWirdBlockNotice() }
                                ) {
                                    Text(
                                        text = "🔒 يكتمل بعلامة التوقف",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                    isAzkarHabit -> {
                        if (isDoneToday) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = EmeraldDeep.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldDeep, modifier = Modifier.size(16.dp))
                                    Text("تمت القراءة بالقسم الديني ✓", color = EmeraldDeep, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        } else {
                            FilledTonalButton(
                                onClick = onNavigateToReligious,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = EmeraldDeep.copy(alpha = 0.15f),
                                    contentColor = EmeraldDeep
                                )
                            ) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("قراءة في القسم الديني 🕌", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                            }
                        }
                    }
                    isCheckbox -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = isDoneToday,
                                onClick = { onSetCheckbox(true) },
                                label = { Text("تم", fontWeight = FontWeight.Bold) },
                                leadingIcon = {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )
                            FilterChip(
                                selected = !isDoneToday && todayLog != null,
                                onClick = { onSetCheckbox(false) },
                                label = { Text("لم يتم") },
                                leadingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                    isValueHabit -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilledTonalButton(
                                onClick = onIncrementValue,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("زيادة", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = onEditExactValue,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text("تحديد القيمة")
                            }
                        }
                    }
                    isTimerHabit -> {
                        FilledTonalButton(
                            onClick = onStartTimer,
                            modifier = Modifier.testTag("verify_habit_action_${habit.id}"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("بدء العداد", fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(onClick = onEditPenalty) {
                        Icon(
                            imageVector = if (habit.penaltyLockApps) Icons.Default.Lock else Icons.Outlined.LockOpen,
                            contentDescription = "قفل العقاب",
                            tint = if (habit.penaltyLockApps) PriorityHighRed else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onEditAlarm,
                        modifier = Modifier.testTag("habit_alarm_btn_${habit.id}")
                    ) {
                        Icon(
                            imageVector = if (habit.alarmEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                            contentDescription = "منبه العادة",
                            tint = if (habit.alarmEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_habit_${habit.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "حذف",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

/**
 * نافذة إضافة عادة جديدة بتصميم منسق وشامل لكافة أنواع التحقق والجدولة
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OrganizedAddHabitDialog(
    categories: List<CategoryEntity>,
    subjects: List<SubjectEntity>,
    schedules: List<BlockScheduleEntity>,
    blockedApps: List<BlockedAppEntity>,
    usageOverview: DeviceUsageOverview,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        frequencyType: String,
        targetCount: Int,
        unitLabel: String,
        autoSource: String,
        timerType: String,
        pomodoroFocusMin: Int,
        pomodoroBreakMin: Int,
        pomodoroRounds: Int,
        linkedSubjectId: String,
        penaltyLockApps: Boolean,
        penaltyScheduleId: String,
        penaltyBlockedPackages: String,
        alarmEnabled: Boolean,
        alarmTime24: String,
        alarmType: String,
        alarmVerifyMethod: String,
        alarmPlaceName: String,
        alarmQrCode: String,
        categoryId: String,
        comparisonOperator: String,
        frequencyDaysOfWeek: String,
        frequencyDaysOfMonth: String,
        frequencyDaysOfYear: String,
        frequencyPeriodType: String,
        frequencyPeriodTimes: Int,
        frequencyIntervalUnit: String,
        frequencyIntervalValue: Int,
        cycleWorkDays: Int,
        cycleRestDays: Int,
        allowedSkipDays: Int,
        startDateKey: String,
        endDateKey: String,
        checklistItemsJson: String,
        checklistRequiredCount: Int
    ) -> Unit
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("CHECKBOX") } // CHECKBOX, TARGET_VALUE, CHECKLIST, STUDY_TIMER, HEALTH
    var selectedHealthMetric by remember { mutableStateOf(HealthGoalMetric.STEPS) }

    var title by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf("") }
    var targetText by remember { mutableStateOf("10") }
    var unitLabel by remember { mutableStateOf("مرة") }
    var comparisonOperator by remember { mutableStateOf("GTE") } // GTE, EQ, LTE

    // قائمة التحقق داخل العادة
    var checklistItems by remember {
        mutableStateOf(
            listOf(
                HabitChecklistItem(title = "البند الأول", type = "CHECKBOX"),
                HabitChecklistItem(title = "البند الثاني", type = "CHECKBOX")
            )
        )
    }
    var checklistRequiredText by remember { mutableStateOf("2") }

    // إعدادات التكرار والجدولة (كل كم تسوي العادة)
    var selectedFrequencyType by remember { mutableStateOf(HabitFrequencyType.DAILY) }
    var selectedWeekDays by remember { mutableStateOf(setOf(1, 2, 3, 4, 5, 6, 7)) } // 1..7
    var daysOfMonthText by remember { mutableStateOf("1, 15, 30") }
    var daysOfYearText by remember { mutableStateOf("01-01") }
    var periodType by remember { mutableStateOf("WEEK") } // WEEK, MONTH, YEAR
    var timesPerPeriodText by remember { mutableStateOf("3") }
    var intervalUnit by remember { mutableStateOf("DAYS") } // DAYS, WEEKS, MONTHS
    var intervalValText by remember { mutableStateOf("2") }
    var cycleWorkDaysText by remember { mutableStateOf("3") }
    var cycleRestDaysText by remember { mutableStateOf("1") }
    var allowedSkipDaysText by remember { mutableStateOf("1") }
    var startDateText by remember { mutableStateOf(LocalDate.now().toString()) }
    var endDateText by remember { mutableStateOf("") }

    // إعدادات المؤقت
    var timerType by remember { mutableStateOf("COUNT_UP") }
    var pomodoroFocusText by remember { mutableStateOf("25") }
    var pomodoroBreakText by remember { mutableStateOf("5") }
    var pomodoroRoundsText by remember { mutableStateOf("2") }
    var selectedSubjectId by remember { mutableStateOf(subjects.firstOrNull()?.id ?: "") }

    // إعدادات قفل العقاب
    var penaltyLockApps by remember { mutableStateOf(false) }
    var penaltyScheduleId by remember { mutableStateOf("") }
    var selectedPenaltyPackages by remember { mutableStateOf(setOf<String>()) }

    // إعدادات المنبه
    var alarmEnabled by remember { mutableStateOf(false) }
    var alarmTime24 by remember { mutableStateOf("06:00") }
    var alarmType by remember { mutableStateOf("NORMAL") }
    var alarmVerifyMethod by remember { mutableStateOf("LOCAL_IMAGE") }
    var alarmPlaceName by remember { mutableStateOf("المكان المحدد") }
    var alarmQrCode by remember { mutableStateOf("WAKE-QR-2026") }
    var hasSavedPlacePhoto by remember { mutableStateOf(PlacePhotoHelper.hasReferencePlacePhoto(context)) }

    val cameraPreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            PlacePhotoHelper.saveReferencePlaceBitmap(context, bmp)
            hasSavedPlacePhoto = true
        }
    }

    val cameraPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) cameraPreviewLauncher.launch(null)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة عادة جديدة", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("اسم العادة") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("habit_title_input")
                )

                // تصنيف الفئة (يونيفرسل)
                if (categories.isNotEmpty()) {
                    Text(
                        text = "تصنيف الفئة (يونيفرسل)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategoryId.isBlank(),
                            onClick = { selectedCategoryId = "" },
                            label = { Text("بدون فئة") }
                        )
                        categories.forEach { cat ->
                            val isSel = selectedCategoryId == cat.id
                            val col = CategoryVisuals.parseColor(cat.colorHex)
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedCategoryId = cat.id },
                                label = { Text(cat.name) },
                                leadingIcon = {
                                    Icon(
                                        CategoryVisuals.getIcon(cat.iconKey),
                                        contentDescription = null,
                                        tint = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else col,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }

                Text(
                    text = "نوع العادة والتحقق",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategory == "CHECKBOX",
                            onClick = { selectedCategory = "CHECKBOX" },
                            label = { Text("صح / خطأ", fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.CheckCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedCategory == "TARGET_VALUE",
                            onClick = {
                                selectedCategory = "TARGET_VALUE"
                                if (targetText == "1") targetText = "10"
                            },
                            label = { Text("قيمة رقمية", fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.Numbers, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategory == "CHECKLIST",
                            onClick = {
                                selectedCategory = "CHECKLIST"
                            },
                            label = { Text("قائمة تحقق", fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.Checklist, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = selectedCategory == "STUDY_TIMER",
                            onClick = {
                                selectedCategory = "STUDY_TIMER"
                                unitLabel = "دقيقة"
                                targetText = "30"
                            },
                            label = { Text("مؤقت تركيز", fontWeight = FontWeight.Bold) },
                            leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    FilterChip(
                        selected = selectedCategory == "HEALTH",
                        onClick = {
                            selectedCategory = "HEALTH"
                            unitLabel = selectedHealthMetric.unitLabel
                            targetText = selectedHealthMetric.defaultTarget.toString()
                            if (title.isBlank()) title = selectedHealthMetric.arabicTitle.substringBefore(" (")
                        },
                        label = { Text("هدف صحي تلقائي (Health Connect)", fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.MonitorHeart, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    FilterChip(
                        selected = selectedCategory == "QURAN_WIRD",
                        onClick = {
                            selectedCategory = "QURAN_WIRD"
                            if (title.isBlank() || title.startsWith("هدف") || title == "عادة جديدة") {
                                title = "الورد القرآني اليومي"
                            }
                        },
                        label = { Text("الورد القرآني (تتبع تلقائي بالمصحف 📖)", fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                when (selectedCategory) {
                    "QURAN_WIRD" -> {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = EmeraldDeep.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldDeep.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = EmeraldDeep)
                                    Text("تتبع تلقائي وإنجاز إلزامي بالمصحف", fontWeight = FontWeight.Bold, color = EmeraldDeep)
                                }
                                Text(
                                    text = "وفقاً للنظام، لا يمكن التأشير اليدوي على هذه العادة في قائمة العادات. تكتمل فقط عندما تقرأ الورد وتضع علامة التوقف عند الآية المطلوب إنهاؤها بالمصحف 🏁.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    "TARGET_VALUE" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = targetText,
                                    onValueChange = { targetText = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("القيمة المطلوبة") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = unitLabel,
                                    onValueChange = { unitLabel = it },
                                    label = { Text("الوحدة") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Text(
                                text = "شرط تحقيق الهدف:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                SegmentedButton(
                                    selected = comparisonOperator == "GTE",
                                    onClick = { comparisonOperator = "GTE" },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                                ) {
                                    Text("مثل أو أكثر (≥)", style = MaterialTheme.typography.labelSmall)
                                }
                                SegmentedButton(
                                    selected = comparisonOperator == "EQ",
                                    onClick = { comparisonOperator = "EQ" },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                                ) {
                                    Text("مثله تماماً (=)", style = MaterialTheme.typography.labelSmall)
                                }
                                SegmentedButton(
                                    selected = comparisonOperator == "LTE",
                                    onClick = { comparisonOperator = "LTE" },
                                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                                ) {
                                    Text("أقل منه (≤)", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    "CHECKLIST" -> {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("بنود قائمة التحقق للعادة", fontWeight = FontWeight.Bold)
                                    IconButton(
                                        onClick = {
                                            checklistItems = checklistItems + HabitChecklistItem(
                                                title = "بند ${checklistItems.size + 1}",
                                                type = "CHECKBOX"
                                            )
                                        }
                                    ) {
                                        Icon(Icons.Default.AddCircle, contentDescription = "إضافة بند", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }

                                checklistItems.forEachIndexed { index, item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = item.title,
                                            onValueChange = { newT ->
                                                checklistItems = checklistItems.toMutableList().also {
                                                    it[index] = item.copy(title = newT)
                                                }
                                            },
                                            label = { Text("بند ${index + 1}") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )

                                        var typeMenuExpanded by remember { mutableStateOf(false) }
                                        Box {
                                            AssistChip(
                                                onClick = { typeMenuExpanded = true },
                                                label = {
                                                    Text(
                                                        when (item.type) {
                                                            "VALUE" -> "قيمة"
                                                            "TIMER" -> "مؤقت"
                                                            else -> "صح/خطأ"
                                                        },
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                }
                                            )
                                            DropdownMenu(
                                                expanded = typeMenuExpanded,
                                                onDismissRequest = { typeMenuExpanded = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("صح وخطأ") },
                                                    onClick = {
                                                        checklistItems = checklistItems.toMutableList().also {
                                                            it[index] = item.copy(type = "CHECKBOX")
                                                        }
                                                        typeMenuExpanded = false
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("قيمة رقمية") },
                                                    onClick = {
                                                        checklistItems = checklistItems.toMutableList().also {
                                                            it[index] = item.copy(type = "VALUE", targetValue = 5, unitLabel = "مرة")
                                                        }
                                                        typeMenuExpanded = false
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("مؤقت تايمر") },
                                                    onClick = {
                                                        checklistItems = checklistItems.toMutableList().also {
                                                            it[index] = item.copy(type = "TIMER", targetValue = 15, unitLabel = "دقيقة")
                                                        }
                                                        typeMenuExpanded = false
                                                    }
                                                )
                                            }
                                        }

                                        if (item.type != "CHECKBOX") {
                                            OutlinedTextField(
                                                value = item.targetValue.toString(),
                                                onValueChange = { v ->
                                                    val n = v.filter { it.isDigit() }.toIntOrNull() ?: 1
                                                    checklistItems = checklistItems.toMutableList().also {
                                                        it[index] = item.copy(targetValue = n)
                                                    }
                                                },
                                                label = { Text(if (item.type == "TIMER") "دقيقة" else "قيمة") },
                                                singleLine = true,
                                                modifier = Modifier.width(65.dp)
                                            )
                                        }

                                        IconButton(
                                            onClick = {
                                                if (checklistItems.size > 1) {
                                                    checklistItems = checklistItems.filterIndexed { i, _ -> i != index }
                                                }
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = checklistRequiredText,
                                    onValueChange = { checklistRequiredText = it.filter { c -> c.isDigit() } },
                                    label = { Text("كم بند مطلوب تنفيذه لاعتبار العادة مكتملة؟") },
                                    supportingText = { Text("المتاح حالياً: ${checklistItems.size} بنود") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    "STUDY_TIMER" -> {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = timerType == "COUNT_UP",
                                        onClick = { timerType = "COUNT_UP" },
                                        label = { Text("مؤقت تصاعدي") }
                                    )
                                    FilterChip(
                                        selected = timerType == "POMODORO",
                                        onClick = { timerType = "POMODORO" },
                                        label = { Text("بومودورو مخصص") }
                                    )
                                }

                                if (timerType == "COUNT_UP") {
                                    OutlinedTextField(
                                        value = targetText,
                                        onValueChange = { targetText = it.filter { c -> c.isDigit() } },
                                        label = { Text("الدقائق المطلوبة") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                } else {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedTextField(
                                            value = pomodoroFocusText,
                                            onValueChange = { pomodoroFocusText = it.filter { c -> c.isDigit() } },
                                            label = { Text("تركيز") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = pomodoroBreakText,
                                            onValueChange = { pomodoroBreakText = it.filter { c -> c.isDigit() } },
                                            label = { Text("راحة") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = pomodoroRoundsText,
                                            onValueChange = { pomodoroRoundsText = it.filter { c -> c.isDigit() } },
                                            label = { Text("الجولات") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                if (subjects.isNotEmpty()) {
                                    Text("ربط بمادة", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        subjects.forEach { subj ->
                                            FilterChip(
                                                selected = selectedSubjectId == subj.id,
                                                onClick = { selectedSubjectId = subj.id },
                                                label = { Text(subj.name) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "HEALTH" -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                HealthGoalMetric.entries.forEach { metric ->
                                    FilterChip(
                                        selected = selectedHealthMetric == metric,
                                        onClick = {
                                            selectedHealthMetric = metric
                                            unitLabel = metric.unitLabel
                                            targetText = metric.defaultTarget.toString()
                                            title = metric.arabicTitle.substringBefore(" (")
                                        },
                                        label = { Text(metric.arabicTitle.substringBefore(" (")) }
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = targetText,
                                onValueChange = { targetText = it.filter { c -> c.isDigit() } },
                                label = { Text("الهدف اليومي (${selectedHealthMetric.unitLabel})") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                HorizontalDivider()

                // خيارات الجدولة والتكرار الثمانية ("كل كم تسوي العادة")
                Text(
                    text = "تكرار العادة (كل كم تسوي العادة)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HabitFrequencyType.entries.forEach { fType ->
                        FilterChip(
                            selected = selectedFrequencyType == fType,
                            onClick = { selectedFrequencyType = fType },
                            label = { Text(fType.title, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                when (selectedFrequencyType) {
                    HabitFrequencyType.DAILY -> {
                        Text("يتم تكرار العادة يومياً.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    HabitFrequencyType.SPECIFIC_DAYS_WEEK -> {
                        Text("اختر الأيام من الأسبوع:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        val weekDaysList = listOf(6 to "السبت", 7 to "الأحد", 1 to "الإثنين", 2 to "الثلاثاء", 3 to "الأربعاء", 4 to "الخميس", 5 to "الجمعة")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            weekDaysList.forEach { (dNum, dName) ->
                                val isSelected = dNum in selectedWeekDays
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedWeekDays = if (isSelected) selectedWeekDays - dNum else selectedWeekDays + dNum
                                    },
                                    label = { Text(dName, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }

                    HabitFrequencyType.SPECIFIC_DAYS_MONTH -> {
                        OutlinedTextField(
                            value = daysOfMonthText,
                            onValueChange = { daysOfMonthText = it },
                            label = { Text("أيام محددة بالشهر (مفصولة بفواصل)") },
                            placeholder = { Text("1, 15, 30") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    HabitFrequencyType.SPECIFIC_DAYS_YEAR -> {
                        OutlinedTextField(
                            value = daysOfYearText,
                            onValueChange = { daysOfYearText = it },
                            label = { Text("أيام محددة بالسنة (صيغة MM-DD)") },
                            placeholder = { Text("01-01, 12-25") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    HabitFrequencyType.TIMES_PER_PERIOD -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = timesPerPeriodText,
                                onValueChange = { timesPerPeriodText = it.filter { c -> c.isDigit() } },
                                label = { Text("عدد المرات (X)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            var pExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { pExpanded = true },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                ) {
                                    Text(
                                        when (periodType) {
                                            "MONTH" -> "في الشهر"
                                            "YEAR" -> "في السنة"
                                            else -> "في الأسبوع"
                                        }
                                    )
                                }
                                DropdownMenu(expanded = pExpanded, onDismissRequest = { pExpanded = false }) {
                                    DropdownMenuItem(text = { Text("في الأسبوع") }, onClick = { periodType = "WEEK"; pExpanded = false })
                                    DropdownMenuItem(text = { Text("في الشهر") }, onClick = { periodType = "MONTH"; pExpanded = false })
                                    DropdownMenuItem(text = { Text("في السنة") }, onClick = { periodType = "YEAR"; pExpanded = false })
                                }
                            }
                        }
                    }

                    HabitFrequencyType.INTERVAL_EVERY_X -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = intervalValText,
                                onValueChange = { intervalValText = it.filter { c -> c.isDigit() } },
                                label = { Text("كل كم (X)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            var iExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { iExpanded = true },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                ) {
                                    Text(
                                        when (intervalUnit) {
                                            "WEEKS" -> "أسابيع"
                                            "MONTHS" -> "شهور"
                                            else -> "أيام"
                                        }
                                    )
                                }
                                DropdownMenu(expanded = iExpanded, onDismissRequest = { iExpanded = false }) {
                                    DropdownMenuItem(text = { Text("أيام") }, onClick = { intervalUnit = "DAYS"; iExpanded = false })
                                    DropdownMenuItem(text = { Text("أسابيع") }, onClick = { intervalUnit = "WEEKS"; iExpanded = false })
                                    DropdownMenuItem(text = { Text("شهور") }, onClick = { intervalUnit = "MONTHS"; iExpanded = false })
                                }
                            }
                        }
                    }

                    HabitFrequencyType.WORKOUT_CYCLE -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = cycleWorkDaysText,
                                onValueChange = { cycleWorkDaysText = it.filter { c -> c.isDigit() } },
                                label = { Text("أيام التمرين (X)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = cycleRestDaysText,
                                onValueChange = { cycleRestDaysText = it.filter { c -> c.isDigit() } },
                                label = { Text("أيام الراحة (Y)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // عدد أيام الراحة والتخطي (أزرق) المسموحة
                OutlinedTextField(
                    value = allowedSkipDaysText,
                    onValueChange = { allowedSkipDaysText = it.filter { c -> c.isDigit() } },
                    label = { Text("كم يوم راحة مسموح أسبوعياً (تخطي أزرق 🏖️)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // تاريخ البدء والانتهاء
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startDateText,
                        onValueChange = { startDateText = it },
                        label = { Text("تاريخ البدء") },
                        placeholder = { Text("YYYY-MM-DD") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endDateText,
                        onValueChange = { endDateText = it },
                        label = { Text("تاريخ الانتهاء") },
                        placeholder = { Text("مستمر") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider()

                // قسم قفل العقاب
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("قفل العقاب (حظر تطبيقات حتى الإنجاز)", fontWeight = FontWeight.Bold)
                    Switch(checked = penaltyLockApps, onCheckedChange = { penaltyLockApps = it })
                }

                if (penaltyLockApps) {
                    PenaltySelectionSection(
                        schedules = schedules,
                        blockedApps = blockedApps,
                        usageOverview = usageOverview,
                        selectedScheduleId = penaltyScheduleId,
                        onSelectScheduleId = { penaltyScheduleId = it },
                        selectedPackages = selectedPenaltyPackages,
                        onTogglePackage = { pkg ->
                            selectedPenaltyPackages = if (pkg in selectedPenaltyPackages) {
                                selectedPenaltyPackages - pkg
                            } else {
                                selectedPenaltyPackages + pkg
                            }
                        }
                    )
                }

                HorizontalDivider()

                // قسم المنبه
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تفعيل المنبه", fontWeight = FontWeight.Bold)
                    Switch(checked = alarmEnabled, onCheckedChange = { alarmEnabled = it })
                }

                if (alarmEnabled) {
                    AlarmCustomizationFields(
                        time24 = alarmTime24,
                        onTimeChange = { alarmTime24 = it },
                        alarmType = alarmType,
                        onAlarmTypeChange = { alarmType = it },
                        verifyMethod = alarmVerifyMethod,
                        onVerifyMethodChange = { alarmVerifyMethod = it },
                        placeName = alarmPlaceName,
                        onPlaceNameChange = { alarmPlaceName = it },
                        qrCode = alarmQrCode,
                        onQrCodeChange = { alarmQrCode = it },
                        hasSavedPlacePhoto = hasSavedPlacePhoto,
                        onCaptureReferencePhoto = {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) {
                                cameraPreviewLauncher.launch(null)
                            } else {
                                cameraPermLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val finalSource = when (selectedCategory) {
                            "CHECKBOX" -> "CHECKBOX"
                            "QURAN_WIRD" -> "QURAN_WIRD"
                            "TARGET_VALUE" -> "TARGET_VALUE"
                            "CHECKLIST" -> "CHECKLIST"
                            "STUDY_TIMER" -> "STUDY_TIMER"
                            else -> selectedHealthMetric.sourceCode
                        }
                        val focusMin = (pomodoroFocusText.toIntOrNull() ?: 25).coerceAtLeast(5)
                        val breakMin = (pomodoroBreakText.toIntOrNull() ?: 5).coerceAtLeast(1)
                        val rounds = (pomodoroRoundsText.toIntOrNull() ?: 2).coerceAtLeast(1)
                        val finalTarget = when {
                            selectedCategory == "CHECKBOX" || selectedCategory == "QURAN_WIRD" -> 1
                            selectedCategory == "CHECKLIST" -> (checklistRequiredText.toIntOrNull() ?: 1).coerceAtLeast(1)
                            selectedCategory == "STUDY_TIMER" && timerType == "POMODORO" -> focusMin * rounds
                            else -> (targetText.toIntOrNull() ?: 1).coerceAtLeast(1)
                        }
                        val finalUnit = when (selectedCategory) {
                            "CHECKBOX" -> "إنجاز"
                            "QURAN_WIRD" -> "ورد"
                            "CHECKLIST" -> "عنصر"
                            "STUDY_TIMER" -> "دقيقة"
                            "HEALTH" -> selectedHealthMetric.unitLabel
                            else -> unitLabel.ifBlank { "مرة" }
                        }

                        val checklistJson = if (selectedCategory == "CHECKLIST") {
                            HabitHelper.serializeChecklistItems(checklistItems)
                        } else "[]"
                        val checklistReq = (checklistRequiredText.toIntOrNull() ?: 1).coerceAtLeast(1)

                        onConfirm(
                            title.trim(),
                            selectedFrequencyType.code,
                            finalTarget,
                            finalUnit,
                            finalSource,
                            timerType,
                            focusMin,
                            breakMin,
                            rounds,
                            if (selectedCategory == "STUDY_TIMER") selectedSubjectId else "",
                            penaltyLockApps,
                            if (penaltyLockApps) penaltyScheduleId else "",
                            if (penaltyLockApps) selectedPenaltyPackages.joinToString(",") else "",
                            alarmEnabled,
                            if (alarmEnabled) alarmTime24.trim() else "",
                            alarmType,
                            alarmVerifyMethod,
                            alarmPlaceName,
                            alarmQrCode,
                            selectedCategoryId,
                            comparisonOperator,
                            selectedWeekDays.joinToString(","),
                            daysOfMonthText.trim(),
                            daysOfYearText.trim(),
                            periodType,
                            timesPerPeriodText.toIntOrNull() ?: 3,
                            intervalUnit,
                            intervalValText.toIntOrNull() ?: 1,
                            cycleWorkDaysText.toIntOrNull() ?: 3,
                            cycleRestDaysText.toIntOrNull() ?: 1,
                            allowedSkipDaysText.toIntOrNull() ?: 1,
                            startDateText.trim(),
                            endDateText.trim(),
                            checklistJson,
                            checklistReq
                        )
                    }
                },
                modifier = Modifier.testTag("confirm_add_habit_btn")
            ) {
                Text("حفظ العادة", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

/**
 * قسم اختيار تطبيقات قفل العقاب:
 * - ترتيب طولي (عمودي) للتطبيقات
 * - مرتبة تنازلياً حسب مدة الاستعمال اليومي
 * - يظهر بجانب كل تطبيق مدة استعماله اليوم
 */
@Composable
internal fun PenaltySelectionSection(
    schedules: List<BlockScheduleEntity>,
    blockedApps: List<BlockedAppEntity>,
    usageOverview: DeviceUsageOverview,
    selectedScheduleId: String,
    onSelectScheduleId: (String) -> Unit,
    selectedPackages: Set<String>,
    onTogglePackage: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (schedules.isNotEmpty()) {
                Text(
                    text = "ربط بجدولة حظر (يتوقف الحظر عند إتمام العادة)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedScheduleId.isBlank(),
                        onClick = { onSelectScheduleId("") },
                        label = { Text("تطبيقات مخصصة") }
                    )
                    schedules.forEach { sched ->
                        FilterChip(
                            selected = selectedScheduleId == sched.id,
                            onClick = { onSelectScheduleId(sched.id) },
                            label = { Text(sched.title) }
                        )
                    }
                }
            }

            Text(
                text = "اختر التطبيقات المحظورة (مرتبة حسب مدة الاستعمال اليوم)",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )

            // دمج التطبيقات المثبتة والمقيدة وترتيبها تنازلياً حسب مدة الاستعمال الفعلي
            val sortedAppsWithUsage: List<AppUsageStatItem> = remember(blockedApps, usageOverview) {
                val byPkg = mutableMapOf<String, AppUsageStatItem>()
                usageOverview.installedUserApps.forEach { item ->
                    byPkg[item.packageName] = item
                }
                blockedApps.forEach { b ->
                    if (b.packageName !in byPkg) {
                        val ms = usageOverview.usageByPackageMillis[b.packageName] ?: 0L
                        val mins = usageOverview.usageByPackageMinutes[b.packageName] ?: 0
                        byPkg[b.packageName] = AppUsageStatItem(
                            packageName = b.packageName,
                            appName = b.appName,
                            usedMinutesToday = mins,
                            usedTimeMillisToday = ms,
                            formattedDuration = AppUsageMonitor.formatDurationArabic(ms)
                        )
                    }
                }
                byPkg.values.sortedWith(
                    compareByDescending<AppUsageStatItem> { it.usedTimeMillisToday }
                        .thenBy { it.appName }
                )
            }

            if (sortedAppsWithUsage.isEmpty()) {
                Text(
                    text = "سيتم حظر جميع التطبيقات المضافة في قائمة الحظر",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // قائمة طولية (عمودية) تعرض اسم التطبيق وبجانبه مدة استعماله اليوم
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sortedAppsWithUsage.forEach { appItem ->
                        val isSelected = appItem.packageName in selectedPackages
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onTogglePackage(appItem.packageName) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = null
                                    )
                                    Text(
                                        text = appItem.appName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (appItem.usedTimeMillisToday > 0L) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ) {
                                    Text(
                                        text = UmmAlQuraCalculator.toArabicDigits(appItem.formattedDuration),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (appItem.usedTimeMillisToday > 0L) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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

@Composable
internal fun AlarmCustomizationFields(
    time24: String,
    onTimeChange: (String) -> Unit,
    alarmType: String,
    onAlarmTypeChange: (String) -> Unit,
    verifyMethod: String,
    onVerifyMethodChange: (String) -> Unit,
    placeName: String,
    onPlaceNameChange: (String) -> Unit,
    qrCode: String,
    onQrCodeChange: (String) -> Unit,
    hasSavedPlacePhoto: Boolean,
    onCaptureReferencePhoto: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = time24,
                onValueChange = onTimeChange,
                label = { Text("وقت المنبه") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "نوع المنبه",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = alarmType == "NORMAL",
                    onClick = { onAlarmTypeChange("NORMAL") },
                    label = { Text("منبه عادي") }
                )
                FilterChip(
                    selected = alarmType == "SUPER_WAKE",
                    onClick = { onAlarmTypeChange("SUPER_WAKE") },
                    label = { Text("منبه ذكي بتحقق") }
                )
            }

            if (alarmType == "SUPER_WAKE") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = verifyMethod == "LOCAL_IMAGE",
                        onClick = { onVerifyMethodChange("LOCAL_IMAGE") },
                        label = { Text("تصوير مكان") }
                    )
                    FilterChip(
                        selected = verifyMethod == "QR",
                        onClick = { onVerifyMethodChange("QR") },
                        label = { Text("مسح رمز QR") }
                    )
                }

                if (verifyMethod == "LOCAL_IMAGE") {
                    OutlinedTextField(
                        value = placeName,
                        onValueChange = onPlaceNameChange,
                        label = { Text("اسم المكان المطلوب تصويره") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    FilledTonalButton(
                        onClick = onCaptureReferencePhoto,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (hasSavedPlacePhoto) "تحديث الصورة المرجعية للمكان" else "التقاط صورة مرجعية للمكان")
                    }
                } else {
                    OutlinedTextField(
                        value = qrCode,
                        onValueChange = onQrCodeChange,
                        label = { Text("نص رمز QR المطلوب") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun CustomizableAlarmDialog(
    title: String,
    initialEnabled: Boolean,
    initialTime24: String,
    initialAlarmType: String,
    initialVerifyMethod: String,
    initialPlaceName: String,
    initialQrCode: String,
    onDismiss: () -> Unit,
    onSave: (
        time24: String,
        enabled: Boolean,
        alarmType: String,
        verifyMethod: String,
        placeName: String,
        qrCode: String
    ) -> Unit
) {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(initialEnabled || initialTime24.isBlank()) }
    var timeText by remember { mutableStateOf(initialTime24.ifBlank { "06:00" }) }
    var alarmType by remember { mutableStateOf(initialAlarmType.ifBlank { "NORMAL" }) }
    var verifyMethod by remember { mutableStateOf(initialVerifyMethod.ifBlank { "LOCAL_IMAGE" }) }
    var placeName by remember { mutableStateOf(initialPlaceName.ifBlank { "المكان المحدد" }) }
    var qrCode by remember { mutableStateOf(initialQrCode.ifBlank { "WAKE-QR-2026" }) }
    var hasSavedPhoto by remember { mutableStateOf(PlacePhotoHelper.hasReferencePlacePhoto(context)) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            PlacePhotoHelper.saveReferencePlaceBitmap(context, bmp)
            hasSavedPhoto = true
        }
    }

    val permLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) cameraLauncher.launch(null)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تفعيل المنبه", fontWeight = FontWeight.Bold)
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }

                if (enabled) {
                    AlarmCustomizationFields(
                        time24 = timeText,
                        onTimeChange = { timeText = it },
                        alarmType = alarmType,
                        onAlarmTypeChange = { alarmType = it },
                        verifyMethod = verifyMethod,
                        onVerifyMethodChange = { verifyMethod = it },
                        placeName = placeName,
                        onPlaceNameChange = { placeName = it },
                        qrCode = qrCode,
                        onQrCodeChange = { qrCode = it },
                        hasSavedPlacePhoto = hasSavedPhoto,
                        onCaptureReferencePhoto = {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) cameraLauncher.launch(null)
                            else permLauncher.launch(Manifest.permission.CAMERA)
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(timeText.trim(), enabled, alarmType, verifyMethod, placeName.trim(), qrCode.trim())
                }
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun HabitPenaltyConfigDialog(
    habit: HabitEntity,
    schedules: List<BlockScheduleEntity>,
    blockedApps: List<BlockedAppEntity>,
    usageOverview: DeviceUsageOverview,
    onDismiss: () -> Unit,
    onSave: (Boolean, String, String) -> Unit
) {
    var enabled by remember { mutableStateOf(habit.penaltyLockApps) }
    var scheduleId by remember { mutableStateOf(habit.penaltyScheduleId) }
    var selectedPackages by remember {
        mutableStateOf(
            habit.penaltyBlockedPackages.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("قفل العقاب: ${habit.title}", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("تفعيل حظر التطبيقات حتى إتمام العادة", fontWeight = FontWeight.Bold)
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }

                if (enabled) {
                    PenaltySelectionSection(
                        schedules = schedules,
                        blockedApps = blockedApps,
                        usageOverview = usageOverview,
                        selectedScheduleId = scheduleId,
                        onSelectScheduleId = { scheduleId = it },
                        selectedPackages = selectedPackages,
                        onTogglePackage = { pkg ->
                            selectedPackages = if (pkg in selectedPackages) {
                                selectedPackages - pkg
                            } else {
                                selectedPackages + pkg
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(enabled, scheduleId, selectedPackages.joinToString(","))
                }
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun QuranWirdHabitPromoCard(
    onAddWirdHabit: () -> Unit,
    onNavigateToQuran: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("quran_wird_promo_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = EmeraldDeep.copy(alpha = 0.1f)
        ),
        border = BorderStroke(1.5.dp, EmeraldDeep.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(EmeraldDeep),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "📖 تفعيل عادة الورد القرآني اليومي",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = EmeraldDeep
                    )
                    Text(
                        text = "تتبع ختمتك ووردك اليومي كعادة في قائمتك",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "اربط وردك القرآني بقائمة العادات اليومية تلقائياً. تظهر العادة كباقي العادات، ولكن التعبئة اليدوية ممنوعة تماماً؛ تكتمل العادة فقط بشكل آلي عند قراءة الورد في المصحف ووضع علامة التوقف عند الآية المطلوبة منك إنهاؤها.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onNavigateToQuran,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("المصحف 📖", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onAddWirdHabit,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("activate_quran_wird_habit_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إضافة عادة الورد", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

