package com.example.features.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.insights.TimelineEventItem
import com.example.core.prayer.UmmAlQuraCalculator
import com.example.data.YearWeekGoalEntity
import com.example.features.DailyManagerUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InsightsAndPlanningScreen(
    uiState: DailyManagerUiState,
    onRegenerateSummary: (customFocusNote: String) -> Unit,
    onApplyTomorrowTasks: () -> Unit,
    onSaveWeeklyReview: (goal1: String, goal2: String, goal3: String, reflection: String) -> Unit,
    onSaveYearWeekGoal: (weekNumber: Int, title: String, status: String, percent: Int) -> Unit,
    onForceRefreshWidget: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSection by rememberSaveable { mutableIntStateOf(0) } // 0=الكل، 1=الملخص، 2=التايم لاين، 3=أسابيع السنة، 4=الأسبوعي والويدجت
    var selectedWeekNumber by rememberSaveable { mutableIntStateOf(41) }
    var showCustomFocusDialog by remember { mutableStateOf(false) }
    var showWeeklyReviewDialog by remember { mutableStateOf(false) }
    var editingWeekGoal by remember { mutableStateOf<YearWeekGoalEntity?>(null) }

    val summary = uiState.dailySummary
    val weekly = uiState.weeklyReview
    val selectedWeekObj = uiState.yearWeekGoals.firstOrNull { it.weekNumber == selectedWeekNumber }
        ?: YearWeekGoalEntity(
            weekNumber = selectedWeekNumber,
            focusTitle = "الأسبوع رقم $selectedWeekNumber من السنة",
            status = if (selectedWeekNumber < 41) "COMPLETED" else if (selectedWeekNumber == 41) "CURRENT" else "UPCOMING",
            completionPercent = if (selectedWeekNumber <= 41) 85 else 0
        )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("insights_planning_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "المرحلة ٦ و ٧: الملخص الذكي، التايم لاين، أسابيع السنة، والويدجت",
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = "تحليل شامل ليومك وبناء جدول الغد تلقائيًا، مع التايم لاين من الاستيقاظ للنوم، وخريطة الـ ٥٢ أسبوعًا، وويدجت الشاشة الرئيسية.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // شريط فلترة الأقسام
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    0 to "الكل (شامل)",
                    1 to "🤖 الملخص وجدول الغد",
                    2 to "🕒 تايم لاين اليوم",
                    3 to "🗓️ أسابيع السنة (٥٢)",
                    4 to "📱 التخطيط والويدجت"
                )
                filters.forEach { (idx, label) ->
                    FilterChip(
                        selected = selectedSection == idx,
                        onClick = { selectedSection = idx },
                        label = { Text(label) },
                        modifier = Modifier.testTag("insights_filter_$idx")
                    )
                }
            }
        }

        // ١. بطاقة الملخص اليومي الذكي وبناء جدول اليوم التالي (المرحلة ٦)
        if (selectedSection == 0 || selectedSection == 1) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF0D3B2E),
                                        Color(0xFF164E3E)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFFE6B85C)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "الملخص اليومي الذكي وخطة الغد",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = summary.engineSource,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFE5DEC9)
                                        )
                                    }
                                }

                                Surface(
                                    color = Color(0xFFE6B85C),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text(
                                        text = "${UmmAlQuraCalculator.toArabicDigits(summary.overallScore.toString())}٪",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = Color(0xFF1A160B),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Text(
                                text = summary.coachHeadline,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFE6B85C),
                                fontWeight = FontWeight.Bold
                            )

                            // نقاط القوة اليوم
                            if (summary.strengthsText.isNotBlank()) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "🌟 أبرز إنجازاتك اليوم:",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color(0xFFE6B85C)
                                        )
                                        Text(
                                            text = summary.strengthsText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            // فرص التحسين لبكرة
                            if (summary.improvementsText.isNotBlank()) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.10f),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "🎯 نقاط التركيز والتحسين لغدٍ:",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color(0xFFE5DEC9)
                                        )
                                        Text(
                                            text = summary.improvementsText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            // جدول اليوم التالي المقترح
                            Surface(
                                color = Color(0xFF092920),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "📅 جدول اليوم التالي المقترح (من الاستيقاظ إلى النوم):",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color(0xFFE6B85C),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = summary.suggestedTomorrowSchedule.ifBlank {
                                            "اضغط (تحديث التحليل) لتوليد جدول الغد المفصّل حسب محاضراتك ومهامك."
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = onApplyTomorrowTasks,
                                    enabled = !summary.isTomorrowTasksApplied,
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("apply_tomorrow_schedule_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.TaskAlt,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        if (summary.isTomorrowTasksApplied) {
                                            "تم اعتماد مهام الغد ✅"
                                        } else {
                                            "اعتماد جدول الغد في المهام"
                                        }
                                    )
                                }

                                FilledTonalButton(
                                    onClick = { showCustomFocusDialog = true },
                                    modifier = Modifier.testTag("regenerate_summary_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تحديث التحليل")
                                }
                            }
                        }
                    }
                }
            }
        }

        // ٢. التايم لاين اليومي الشامل (من الاستيقاظ ٠٤:٣٠ ص إلى النوم ١١:٠٠ م)
        if (selectedSection == 0 || selectedSection == 2) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "التايم لاين اليومي (من الاستيقاظ إلى النوم)",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "مسارك الزمني المتكامل لليوم: المنبه، الصلوات الخمس، الورد، المحاضرات، المذاكرة، المشي، ووضع النوم",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        uiState.timelineEvents.forEachIndexed { index, event ->
                            TimelineEventRow(
                                event = event,
                                isLast = index == uiState.timelineEvents.lastIndex
                            )
                        }
                    }
                }
            }
        }

        // ٣. خريطة أسابيع السنة الـ ٥٢ (Year Weeks Grid)
        if (selectedSection == 0 || selectedSection == 3) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.CalendarViewWeek,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "خريطة أسابيع السنة (٥٢ أسبوعًا)",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "انقضى ٤٠ أسبوعًا • أنت الآن في الأسبوع ٤١ • متبقي ١١ أسبوعًا لنهاية السنة",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // شبكة الـ ٥٢ أسبوعًا التفاعلية
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            (1..52).forEach { weekNum ->
                                val goal = uiState.yearWeekGoals.firstOrNull { it.weekNumber == weekNum }
                                val status = goal?.status ?: when {
                                    weekNum < 41 -> "COMPLETED"
                                    weekNum == 41 -> "CURRENT"
                                    else -> "UPCOMING"
                                }
                                val isSelected = selectedWeekNumber == weekNum

                                val bgColor = when (status) {
                                    "COMPLETED" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                    "CURRENT" -> Color(0xFFE6B85C)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                                val textColor = when (status) {
                                    "COMPLETED" -> MaterialTheme.colorScheme.onPrimary
                                    "CURRENT" -> Color(0xFF1A160B)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(bgColor)
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(
                                                    width = 2.dp,
                                                    color = MaterialTheme.colorScheme.onBackground,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .clickable { selectedWeekNumber = weekNum }
                                        .testTag("year_week_cell_$weekNum"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = UmmAlQuraCalculator.toArabicDigits(weekNum.toString()),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = textColor,
                                        fontWeight = if (status == "CURRENT" || isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        // تفاصيل الأسبوع المختار من الـ ٥٢ أسبوعًا
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "الأسبوع رقم ${UmmAlQuraCalculator.toArabicDigits(selectedWeekObj.weekNumber.toString())} • نسبة الإنجاز: ${UmmAlQuraCalculator.toArabicDigits(selectedWeekObj.completionPercent.toString())}٪",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = selectedWeekObj.focusTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                FilledTonalButton(
                                    onClick = { editingWeekGoal = selectedWeekObj },
                                    modifier = Modifier.testTag("edit_selected_year_week_btn")
                                ) {
                                    Icon(imageVector = Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تحديد هدف")
                                }
                            }
                        }
                    }
                }
            }
        }

        // ٤. التخطيط الأسبوعي + ويدجت الشاشة الرئيسية لأندرويد (AppWidget)
        if (selectedSection == 0 || selectedSection == 4) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.EventNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "التخطيط والمراجعة الأسبوعية",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "أهم ٣ أهداف للأسبوع الحالي وتقييم الأداء",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            FilledTonalButton(
                                onClick = { showWeeklyReviewDialog = true },
                                modifier = Modifier.testTag("edit_weekly_plan_btn")
                            ) {
                                Text("تعديل الخطة")
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text("١. ${weekly.topGoal1}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text("٢. ${weekly.topGoal2}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text("٣. ${weekly.topGoal3}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "📝 مراجعة الأسبوع: ${weekly.reflectionNote}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // بطاقة معاينة وتحديث ويدجت الشاشة الرئيسية لأندرويد (Native AppWidget)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Widgets,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "ويدجت الشاشة الرئيسية لأندرويد (Samsung One UI)",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "يمكنك إضافته لشاشة جوالك الرئيسية بالضغط المطول على الشاشة ← التطبيقات المصغرة (Widgets) ← مدير يومي.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // معاينة حية مطابقة لتصميم الويدجت الأصلي
                        val nextP = uiState.prayerSchedule.nextPrayer
                        val completedHabits = uiState.habits.count { h ->
                            uiState.habitLogsByHabitAndDate[h.id]?.values?.any { it.isCompleted } == true
                        }
                        val todayStudy = uiState.studySessions.sumOf { it.durationMinutes }

                        Surface(
                            color = Color(0xFF0D3B2E),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "مدير يومي • تقويم أم القرى (${uiState.selectedCity.arabicName})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFFE6B85C),
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "🕌 الصلاة القادمة: ${nextP.type.arabicName} (${nextP.formatted12h})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = UmmAlQuraCalculator.toArabicDigits(
                                        "🔥 العادات: $completedHabits/${uiState.habits.size} • 📖 الختمة: ص ${uiState.wirdProgress.currentPage}/604 • ⏱️ المذاكرة: $todayStudy د"
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFE5DEC9)
                                )
                            }
                        }

                        Button(
                            onClick = onForceRefreshWidget,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("force_refresh_home_widget_btn")
                        ) {
                            Icon(imageVector = Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تحديث بيانات ويدجت الشاشة الرئيسية الآن")
                        }
                    }
                }
            }
        }
    }

    if (showCustomFocusDialog) {
        RegenerateSummaryDialog(
            onDismiss = { showCustomFocusDialog = false },
            onConfirm = { note ->
                onRegenerateSummary(note)
                showCustomFocusDialog = false
            }
        )
    }

    if (showWeeklyReviewDialog) {
        EditWeeklyReviewDialog(
            current = weekly,
            onDismiss = { showWeeklyReviewDialog = false },
            onSave = { g1, g2, g3, ref ->
                onSaveWeeklyReview(g1, g2, g3, ref)
                showWeeklyReviewDialog = false
            }
        )
    }

    editingWeekGoal?.let { weekGoal ->
        EditYearWeekGoalDialog(
            goal = weekGoal,
            onDismiss = { editingWeekGoal = null },
            onSave = { title, status, pct ->
                onSaveYearWeekGoal(weekGoal.weekNumber, title, status, pct)
                editingWeekGoal = null
            }
        )
    }
}

@Composable
private fun TimelineEventRow(
    event: TimelineEventItem,
    isLast: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            event.isCompleted -> MaterialTheme.colorScheme.primary
                            event.isCurrentFocus -> Color(0xFFE6B85C)
                            else -> MaterialTheme.colorScheme.outlineVariant
                        }
                    )
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(54.dp)
                        .background(
                            if (event.isCompleted) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            }
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            color = when {
                event.isCurrentFocus -> MaterialTheme.colorScheme.secondaryContainer
                event.isCompleted -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = event.timeLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = event.categoryBadge,
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = event.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (event.isCompleted) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "مكتمل",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RegenerateSummaryDialog(
    onDismiss: () -> Unit,
    onConfirm: (customNote: String) -> Unit
) {
    var focusNote by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("توليد الملخص اليومي وبناء جدول الغد") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "سيقوم المحرك بتحليل صلواتك، وردك، أذكارك، جلسات المذاكرة، المهام، وخطوات Health Connect اليوم لتوليد التقييم وجدول الغد:",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = focusNote,
                    onValueChange = { focusNote = it },
                    label = { Text("ملاحظة أو أولوية خاصة ليوم غد (اختياري)") },
                    placeholder = { Text("مثال: التركيز على اختبار الخوارزميات") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(focusNote) }) {
                Text("تحليل اليوم وبناء الجدول")
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
private fun EditWeeklyReviewDialog(
    current: com.example.data.WeeklyReviewEntity,
    onDismiss: () -> Unit,
    onSave: (g1: String, g2: String, g3: String, ref: String) -> Unit
) {
    var g1 by remember { mutableStateOf(current.topGoal1) }
    var g2 by remember { mutableStateOf(current.topGoal2) }
    var g3 by remember { mutableStateOf(current.topGoal3) }
    var ref by remember { mutableStateOf(current.reflectionNote) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل خطة ومراجعة الأسبوع") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = g1,
                    onValueChange = { g1 = it },
                    label = { Text("الهدف الأسبوعي الأول") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = g2,
                    onValueChange = { g2 = it },
                    label = { Text("الهدف الأسبوعي الثاني") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = g3,
                    onValueChange = { g3 = it },
                    label = { Text("الهدف الأسبوعي الثالث") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ref,
                    onValueChange = { ref = it },
                    label = { Text("ملاحظات التقييم الأسبوعي") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(g1, g2, g3, ref) }) {
                Text("حفظ الخطة")
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
private fun EditYearWeekGoalDialog(
    goal: YearWeekGoalEntity,
    onDismiss: () -> Unit,
    onSave: (title: String, status: String, percent: Int) -> Unit
) {
    var title by remember { mutableStateOf(goal.focusTitle) }
    var status by remember { mutableStateOf(goal.status) }
    var percentText by remember { mutableStateOf(goal.completionPercent.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("هدف الأسبوع رقم ${goal.weekNumber} من ٥٢") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان التركيز لهذا الأسبوع") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "COMPLETED" to "مكتمل",
                        "CURRENT" to "الحالي",
                        "UPCOMING" to "قادم"
                    ).forEach { (code, label) ->
                        FilterChip(
                            selected = status == code,
                            onClick = { status = code },
                            label = { Text(label) }
                        )
                    }
                }
                OutlinedTextField(
                    value = percentText,
                    onValueChange = { percentText = it },
                    label = { Text("نسبة الإنجاز (0 - 100)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pct = percentText.toIntOrNull()?.coerceIn(0, 100) ?: goal.completionPercent
                    onSave(title, status, pct)
                }
            ) {
                Text("حفظ الأسبوع")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
