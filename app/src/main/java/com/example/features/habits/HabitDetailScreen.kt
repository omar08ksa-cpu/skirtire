package com.example.features.habits

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.unit.dp
import com.example.core.habits.HabitDayStatus
import com.example.core.habits.HabitGoalComparison
import com.example.core.habits.HabitHelper
import com.example.core.prayer.UmmAlQuraCalculator
import com.example.data.CategoryEntity
import com.example.data.HabitEntity
import com.example.data.HabitLogEntity
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    habit: HabitEntity,
    logsByDate: Map<String, HabitLogEntity>,
    categories: List<CategoryEntity>,
    todayKey: String,
    onBack: () -> Unit,
    onSetCheckbox: (Boolean) -> Unit,
    onUpdateValueProgress: (Int) -> Unit,
    onRecordRestSkip: () -> Unit,
    onUpdateChecklistItem: (itemId: String, checked: Boolean?, delta: Int?, setValue: Int?) -> Unit,
    onStartTimer: () -> Unit,
    onDeleteHabit: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val todayLog = logsByDate[todayKey]
    val isDoneToday = todayLog?.isCompleted == true
    val isRestToday = todayLog?.status == "REST_SKIP"

    val category = remember(categories, habit.categoryId) {
        categories.find { it.id == habit.categoryId }
    }
    val categoryColor = remember(category) {
        CategoryVisuals.parseColor(category?.colorHex ?: "#10B981")
    }

    // التاريخ المعروض في التقويم (الشهر والسنة)
    var calendarYearMonth by remember {
        mutableStateOf(YearMonth.now())
    }

    val isChecklist = habit.autoSource == "CHECKLIST"
    val isCheckbox = habit.autoSource == "CHECKBOX"
    val isValue = habit.autoSource == "TARGET_VALUE"
    val isTimer = habit.autoSource == "STUDY_TIMER"

    val checklistItems = remember(habit.checklistItemsJson) {
        HabitHelper.parseChecklistItems(habit.checklistItemsJson)
    }
    val checklistProgressMap = remember(todayLog?.checklistProgressJson) {
        HabitHelper.parseChecklistProgress(todayLog?.checklistProgressJson)
    }

    // حساب الإحصائيات (هذا الأسبوع، هذا الشهر، هذه السنة، الكل)
    val now = remember { LocalDate.now() }
    val startOfWeek = remember(now) { now.minusDays(now.dayOfWeek.value.toLong() % 7) } // بداية الأسبوع (السبت/الأحد)
    val startOfMonth = remember(now) { now.withDayOfMonth(1) }
    val startOfYear = remember(now) { now.withDayOfYear(1) }

    var completionsThisWeek by remember { mutableIntStateOf(0) }
    var completionsThisMonth by remember { mutableIntStateOf(0) }
    var completionsThisYear by remember { mutableIntStateOf(0) }
    var completionsAllTime by remember { mutableIntStateOf(0) }
    var daysScheduledThisMonth by remember { mutableIntStateOf(0) }

    LaunchedEffect(logsByDate, habit) {
        var wCount = 0
        var mCount = 0
        var yCount = 0
        var allCount = 0
        var scheduledDaysInCurrentMonth = 0

        // حساب الأيام المجدولة للشهر الحالي
        val daysInMonth = now.lengthOfMonth()
        for (day in 1..daysInMonth) {
            val dKey = String.format("%04d-%02d-%02d", now.year, now.monthValue, day)
            if (HabitHelper.isHabitScheduledForDate(habit, dKey)) {
                scheduledDaysInCurrentMonth++
            }
        }
        daysScheduledThisMonth = scheduledDaysInCurrentMonth

        logsByDate.forEach { (dateStr, log) ->
            if (log.isCompleted) {
                allCount++
                try {
                    val logDate = LocalDate.parse(dateStr)
                    if (!logDate.isBefore(startOfWeek)) wCount++
                    if (!logDate.isBefore(startOfMonth)) mCount++
                    if (!logDate.isBefore(startOfYear)) yCount++
                } catch (_: Exception) {}
            }
        }
        completionsThisWeek = wCount
        completionsThisMonth = mCount
        completionsThisYear = yCount
        completionsAllTime = allCount
    }

    val monthlyRatePercent = if (daysScheduledThisMonth > 0) {
        ((completionsThisMonth.toFloat() / daysScheduledThisMonth.toFloat()) * 100).toInt().coerceIn(0, 100)
    } else 0

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = habit.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (category != null) {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = categoryColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = onDeleteHabit) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف العادة", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("habit_detail_scroll"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // كرت إنجاز اليوم والتحقق السريع
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            isDoneToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                            isRestToday -> Color(0xFF3B82F6).copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        }
                    )
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = when {
                                        isDoneToday -> Icons.Default.CheckCircle
                                        isRestToday -> Icons.Default.BeachAccess
                                        else -> Icons.Default.Today
                                    },
                                    contentDescription = null,
                                    tint = when {
                                        isDoneToday -> MaterialTheme.colorScheme.primary
                                        isRestToday -> Color(0xFF3B82F6)
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                                Text(
                                    text = "إنجاز اليوم (${UmmAlQuraCalculator.toArabicDigits(todayKey)})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }

                            // زر راحة / تخطي اليوم (أزرق)
                            OutlinedButton(
                                onClick = onRecordRestSkip,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (isRestToday) Color.White else Color(0xFF3B82F6),
                                    containerColor = if (isRestToday) Color(0xFF3B82F6) else Color.Transparent
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.BeachAccess, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isRestToday) "إلغاء الراحة" else "راحة / تخطي 🏖️",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (habit.allowedSkipDaysPerWeek > 0) {
                            Text(
                                text = "أيام الراحة والتخطي المسموحة: ${UmmAlQuraCalculator.toArabicDigits(habit.allowedSkipDaysPerWeek)} يوم أسبوعياً",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2563EB),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // تحكم إنجاز اليوم حسب نوع العادة
                        if (isCheckbox) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .clickable { onSetCheckbox(!isDoneToday) }
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isDoneToday) "تم الإنجاز بنجاح اليوم 🎉" else "اضغط هنا لتسجيل الإنجاز",
                                    fontWeight = FontWeight.Bold
                                )
                                Checkbox(
                                    checked = isDoneToday,
                                    onCheckedChange = { onSetCheckbox(it) }
                                )
                            }
                        } else if (isValue) {
                            val currentCount = todayLog?.completedCount ?: 0
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "المحقق: ${UmmAlQuraCalculator.toArabicDigits(currentCount)} من ${UmmAlQuraCalculator.toArabicDigits(habit.targetCount)} ${habit.unitLabel}",
                                        fontWeight = FontWeight.Bold
                                    )
                                    val cmp = HabitGoalComparison.fromCode(habit.comparisonOperator)
                                    Text(
                                        text = "الشرط: ${cmp.title}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = { onUpdateValueProgress((currentCount - 1).coerceAtLeast(0)) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("-1", fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { onUpdateValueProgress(currentCount + 1) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("+1", fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { onUpdateValueProgress(currentCount + 5) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("+5", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else if (isChecklist) {
                            // بنود قائمة التحقق
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val completedCount = checklistProgressMap.values.count { it.isCompleted }
                                Text(
                                    text = "قائمة التحقق (أنجزت ${UmmAlQuraCalculator.toArabicDigits(completedCount)} من ${UmmAlQuraCalculator.toArabicDigits(habit.checklistRequiredCount)} المطلوب):",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                checklistItems.forEach { item ->
                                    val prog = checklistProgressMap[item.id]
                                    val isItemDone = prog?.isCompleted == true

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.title,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            val typeLabel = when (item.type) {
                                                "VALUE" -> "قيمة رقمية (${item.targetValue} ${item.unitLabel})"
                                                "TIMER" -> "مؤقت (${item.targetValue} دقيقة)"
                                                else -> "صح وخطأ"
                                            }
                                            Text(
                                                text = typeLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        when (item.type) {
                                            "CHECKBOX" -> {
                                                Checkbox(
                                                    checked = isItemDone,
                                                    onCheckedChange = { chk ->
                                                        onUpdateChecklistItem(item.id, chk, null, null)
                                                    }
                                                )
                                            }
                                            "VALUE" -> {
                                                val cVal = prog?.currentValue ?: 0
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    IconButton(
                                                        onClick = { onUpdateChecklistItem(item.id, null, -1, null) },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    }
                                                    Text(
                                                        text = UmmAlQuraCalculator.toArabicDigits("$cVal/${item.targetValue}"),
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    IconButton(
                                                        onClick = { onUpdateChecklistItem(item.id, null, 1, null) },
                                                        modifier = Modifier.size(32.dp)
                                                    ) {
                                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    }
                                                }
                                            }
                                            "TIMER" -> {
                                                Button(
                                                    onClick = {
                                                        onUpdateChecklistItem(item.id, !isItemDone, null, null)
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                                ) {
                                                    Icon(
                                                        if (isItemDone) Icons.Default.Check else Icons.Default.PlayArrow,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(if (isItemDone) "منجز" else "بدء", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                }
                            }
                        } else if (isTimer) {
                            Button(
                                onClick = onStartTimer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("بدء مؤقت التركيز الآن", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // التقويم التفاعلي للعادة (تقويم شهري كامل)
            item {
                HabitInteractiveMonthlyCalendar(
                    habit = habit,
                    yearMonth = calendarYearMonth,
                    logsByDate = logsByDate,
                    todayKey = todayKey,
                    onChangeYearMonth = { calendarYearMonth = it }
                )
            }

            // مؤشر الستريك (تحت التقويم مباشرة كما طلب المستخدم)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🔥", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = UmmAlQuraCalculator.toArabicDigits("${habit.currentStreak} يوم"),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFF97316)
                                )
                            }
                            Text(
                                text = "الستريك الحالي",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        VerticalDivider(modifier = Modifier.height(36.dp))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "🏆", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = UmmAlQuraCalculator.toArabicDigits("${habit.bestStreak} يوم"),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFEAB308)
                                )
                            }
                            Text(
                                text = "أطول ستريك",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // قسم الإحصائيات الشاملة
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "الإحصائيات ونقاط العادة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )

                        // نقاط العادة والمستوى
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFF59E0B))
                                Column {
                                    Text(
                                        text = "نقاط العادة: ${UmmAlQuraCalculator.toArabicDigits(habit.xp)} XP",
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "المستوى ${UmmAlQuraCalculator.toArabicDigits(habit.level)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // نسبة إنجاز هذا الشهر
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${UmmAlQuraCalculator.toArabicDigits(monthlyRatePercent)}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "نسبة إنجاز هذا الشهر",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // مرات الإكمال الأربعة (هذا الأسبوع، هذا الشهر، هذي السنة، الكل)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatBox(
                                title = "هذا الأسبوع",
                                value = UmmAlQuraCalculator.toArabicDigits(completionsThisWeek),
                                modifier = Modifier.weight(1f)
                            )
                            StatBox(
                                title = "هذا الشهر",
                                value = UmmAlQuraCalculator.toArabicDigits(completionsThisMonth),
                                modifier = Modifier.weight(1f)
                            )
                            StatBox(
                                title = "هذه السنة",
                                value = UmmAlQuraCalculator.toArabicDigits(completionsThisYear),
                                modifier = Modifier.weight(1f)
                            )
                            StatBox(
                                title = "الكل",
                                value = UmmAlQuraCalculator.toArabicDigits(completionsAllTime),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBox(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * التقويم التفاعلي الشهري للعادة بالألوان المطلوبة بدقة:
 * أخضر: مكتمل
 * أصفر: جزء من العادة بنسبة معينة
 * أحمر: لم يكتمل
 * أزرق: راحة / تخطي
 * أبيض / محايد: ما سجل شيء
 */
@Composable
fun HabitInteractiveMonthlyCalendar(
    habit: HabitEntity,
    yearMonth: YearMonth,
    logsByDate: Map<String, HabitLogEntity>,
    todayKey: String,
    onChangeYearMonth: (YearMonth) -> Unit
) {
    val monthTitle = remember(yearMonth) {
        val monthArabic = yearMonth.month.getDisplayName(TextStyle.FULL, Locale("ar"))
        "$monthArabic ${UmmAlQuraCalculator.toArabicDigits(yearMonth.year)}"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // شريط التنقل بين الشهور
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { onChangeYearMonth(yearMonth.minusMonths(1)) }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "الشهر السابق")
                }
                Text(
                    text = monthTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                IconButton(onClick = { onChangeYearMonth(yearMonth.plusMonths(1)) }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "الشهر التالي")
                }
            }

            // أسماء أيام الأسبوع (السبت..الجمعة)
            val weekDayLetters = listOf("سبت", "أحد", "اثنين", "ثلاثاء", "أربعاء", "خميس", "جمعة")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                weekDayLetters.forEach { letter ->
                    Text(
                        text = letter,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // شبكة الأيام
            val firstOfMonth = yearMonth.atDay(1)
            // في Java DayOfWeek: 1=Mon, 2=Tue.. 6=Sat, 7=Sun
            // لنجعل السبت هو العمود 0:
            // Sat (6) -> 0, Sun (7) -> 1, Mon (1) -> 2, Tue (2) -> 3, Wed (3) -> 4, Thu (4) -> 5, Fri (5) -> 6
            val firstDayOffset = when (firstOfMonth.dayOfWeek.value) {
                6 -> 0
                7 -> 1
                else -> firstOfMonth.dayOfWeek.value + 1
            }
            val daysInMonth = yearMonth.lengthOfMonth()
            val totalCells = ((firstDayOffset + daysInMonth + 6) / 7) * 7

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (weekIndex in 0 until (totalCells / 7)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (dayIndex in 0 until 7) {
                            val cellIndex = weekIndex * 7 + dayIndex
                            val dayNumber = cellIndex - firstDayOffset + 1

                            if (dayNumber in 1..daysInMonth) {
                                val dateKey = String.format("%04d-%02d-%02d", yearMonth.year, yearMonth.monthValue, dayNumber)
                                val log = logsByDate[dateKey]
                                val status = HabitHelper.resolveDayStatus(habit, log, dateKey, todayKey)
                                val isToday = dateKey == todayKey

                                val completionPercent = if (log != null && log.completionPercent > 0) {
                                    log.completionPercent
                                } else if (log != null && log.isCompleted) {
                                    100
                                } else if (log != null && log.completedCount > 0) {
                                    HabitHelper.calculatePercent(habit.comparisonOperator, log.completedCount, habit.targetCount)
                                } else 0

                                val (bgColor, textColor, borderColor) = when (status) {
                                    HabitDayStatus.COMPLETED -> Triple(Color(0xFF10B981), Color.White, Color(0xFF10B981)) // أخضر
                                    HabitDayStatus.PARTIAL -> Triple(Color(0xFFF59E0B), Color.White, Color(0xFFF59E0B)) // أصفر
                                    HabitDayStatus.FAILED -> Triple(Color(0xFFEF4444), Color.White, Color(0xFFEF4444)) // أحمر
                                    HabitDayStatus.REST_SKIP -> Triple(Color(0xFF3B82F6), Color.White, Color(0xFF3B82F6)) // أزرق
                                    HabitDayStatus.UNRECORDED -> Triple(Color.Transparent, MaterialTheme.colorScheme.onSurface, MaterialTheme.colorScheme.outlineVariant) // أبيض / محايد
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(bgColor)
                                        .border(
                                            width = if (isToday) 2.dp else 1.dp,
                                            color = if (isToday) MaterialTheme.colorScheme.primary else borderColor,
                                            shape = RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = UmmAlQuraCalculator.toArabicDigits(dayNumber),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Bold,
                                            color = textColor
                                        )
                                        if (status == HabitDayStatus.PARTIAL) {
                                            Text(
                                                text = "${UmmAlQuraCalculator.toArabicDigits(completionPercent)}%",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = androidx.compose.ui.unit.TextUnit(8f, androidx.compose.ui.unit.TextUnitType.Sp)),
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // دليل الألوان (Legend)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = Color(0xFF10B981), label = "مكتمل")
                LegendItem(color = Color(0xFFF59E0B), label = "جزئي")
                LegendItem(color = Color(0xFFEF4444), label = "فائت")
                LegendItem(color = Color(0xFF3B82F6), label = "راحة/تخطي")
                LegendItem(color = Color.LightGray, label = "غير مسجل")
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
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
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
