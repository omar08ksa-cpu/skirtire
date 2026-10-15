package com.example.features.study

import android.app.DatePickerDialog
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.prayer.UmmAlQuraCalculator
import com.example.core.study.LastLinkInfo
import com.example.core.study.StudyTimerManager
import com.example.core.study.StudyTimerState
import com.example.data.HabitEntity
import com.example.data.StudySessionEntity
import com.example.data.TaskEntity
import com.example.features.DailyManagerUiState
import com.example.features.DailyManagerViewModel
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.MintAccent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    uiState: DailyManagerUiState,
    viewModel: DailyManagerViewModel
) {
    val context = LocalContext.current
    val timerState by StudyTimerManager.timerState.collectAsStateWithLifecycle()

    val todayKey = remember(uiState.prayerSchedule.dateKey) {
        uiState.prayerSchedule.dateKey.ifBlank {
            UmmAlQuraCalculator.todayDateKey(System.currentTimeMillis())
        }
    }

    var selectedDateKey by rememberSaveable { mutableStateOf(todayKey) }
    var showLinkTargetSheet by remember { mutableStateOf(false) }
    var showCustomPomodoroDialog by remember { mutableStateOf(false) }
    var dismissedLastLinkPrompt by rememberSaveable { mutableStateOf(false) }

    val lastLinkInfo = remember(context, timerState.isActive) {
        StudyTimerManager.getLastLink(context)
    }

    // فلترة جلسات اليوم المختار
    val filteredSessions = remember(uiState.studySessions, selectedDateKey) {
        uiState.studySessions.filter { it.dateKey == selectedDateKey }
            .sortedByDescending { it.endedAt }
    }
    val totalFocusMinutes = remember(filteredSessions) {
        filteredSessions.sumOf { it.durationMinutes }
    }

    // حساب مفتاح تاريخ الأمس
    val yesterdayKey = remember(todayKey) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.format(cal.time)
    }

    // العادة أو المهمة المرتبطة حالياً
    val currentLinkedHabit = remember(uiState.habits, timerState.linkedId, timerState.linkedType) {
        if (timerState.linkedType == "HABIT" && !timerState.linkedId.isNullOrBlank()) {
            uiState.habits.find { it.id == timerState.linkedId }
        } else null
    }
    val currentLinkedTask = remember(uiState.mainTasks, timerState.linkedId, timerState.linkedType) {
        if (timerState.linkedType == "TASK" && !timerState.linkedId.isNullOrBlank()) {
            uiState.mainTasks.find { it.id == timerState.linkedId }
        } else null
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "العداد",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "مؤقت التركيز والإنجاز الذكي",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // زر فتح وضع الستاند باي المباشر
                    FilledTonalButton(
                        onClick = { viewModel.openStandbyMode() },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier.testTag("open_standby_top_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ScreenRotation,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Standby", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp)
        ) {
            // 1. اقتراح الربط بآخر ربط تم (إذا كان المؤقت غير نشط وغير مرتبط حالياً)
            if (timerState.linkedType == "NONE" && !timerState.isActive && !dismissedLastLinkPrompt && lastLinkInfo != null) {
                item {
                    SmartLastLinkSuggestionCard(
                        lastLink = lastLinkInfo,
                        onAccept = {
                            StudyTimerManager.updateTargetSelection(
                                context = context,
                                targetType = lastLinkInfo.type,
                                targetId = lastLinkInfo.id,
                                targetTitle = lastLinkInfo.title,
                                targetColorHex = lastLinkInfo.colorHex
                            )
                        },
                        onDismiss = { dismissedLastLinkPrompt = true }
                    )
                }
            }

            // 2. بطاقة خيار الربط بعادة أو مهمة
            item {
                LinkedTargetStatusCard(
                    timerState = timerState,
                    linkedHabit = currentLinkedHabit,
                    linkedTask = currentLinkedTask,
                    onChangeLink = { showLinkTargetSheet = true },
                    onUnlink = { StudyTimerManager.unlinkTarget() }
                )
            }

            // 3. البطاقة الرئيسية للعداد
            item {
                MainCounterCard(
                    timerState = timerState,
                    onSelectMode = { mode ->
                        val targetMins = (timerState.targetDurationSeconds / 60).coerceAtLeast(15)
                        StudyTimerManager.setModeAndMinutes(mode, targetMins)
                    },
                    onSelectTargetMinutes = { mins ->
                        StudyTimerManager.setModeAndMinutes("COUNT_UP", mins)
                    },
                    onOpenPomodoroSettings = { showCustomPomodoroDialog = true },
                    onStart = {
                        StudyTimerManager.startTimerForTarget(
                            context = context,
                            targetType = timerState.linkedType,
                            targetId = timerState.linkedId,
                            targetTitle = timerState.subjectName,
                            targetColorHex = timerState.subjectColorHex,
                            mode = timerState.mode,
                            targetMinutes = (timerState.targetDurationSeconds / 60).coerceAtLeast(1),
                            pomodoroFocusMin = timerState.pomodoroFocusMinutes,
                            pomodoroBreakMin = timerState.pomodoroBreakMinutes,
                            pomodoroRounds = timerState.pomodoroTargetRounds
                        )
                    },
                    onTogglePause = { StudyTimerManager.togglePauseResume(context) },
                    onFinishAndSave = { StudyTimerManager.finishAndSaveSession(context) },
                    onCancel = { StudyTimerManager.cancelTimer(context) },
                    onOpenStandby = { viewModel.openStandbyMode() }
                )
            }

            // 4. خيار التاريخ وسجل الجلسات السابقة مع توقيت التوقف والإكمال
            item {
                HistorySectionHeader(
                    todayKey = todayKey,
                    yesterdayKey = yesterdayKey,
                    selectedDateKey = selectedDateKey,
                    onSelectDateKey = { selectedDateKey = it },
                    totalFocusMinutes = totalFocusMinutes,
                    sessionCount = filteredSessions.size
                )
            }

            if (filteredSessions.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "لا توجد جلسات تركيز في هذا التاريخ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ابدأ العداد الآن لحفظ جلساتك وتتبع أوقات التوقف والإكمال بدقة",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            } else {
                items(filteredSessions, key = { it.id }) { session ->
                    SessionDetailTimelineCard(session = session)
                }
            }
        }
    }

    // Modal تخصيص البومودورو
    if (showCustomPomodoroDialog) {
        CustomPomodoroDialog(
            initialFocus = timerState.pomodoroFocusMinutes,
            initialBreak = timerState.pomodoroBreakMinutes,
            initialRounds = timerState.pomodoroTargetRounds,
            onDismiss = { showCustomPomodoroDialog = false },
            onSave = { focus, breakMin, rounds ->
                StudyTimerManager.configureCustomPomodoro(focus, breakMin, rounds)
                showCustomPomodoroDialog = false
            }
        )
    }

    // Bottom Sheet اختيار ربط العداد بعادة أو مهمة
    if (showLinkTargetSheet) {
        LinkTargetBottomSheet(
            uiState = uiState,
            currentLinkedType = timerState.linkedType,
            currentLinkedId = timerState.linkedId,
            onSelectTarget = { type, id, title, colorHex ->
                StudyTimerManager.updateTargetSelection(
                    context = context,
                    targetType = type,
                    targetId = id,
                    targetTitle = title,
                    targetColorHex = colorHex
                )
                showLinkTargetSheet = false
            },
            onUnlink = {
                StudyTimerManager.unlinkTarget()
                showLinkTargetSheet = false
            },
            onDismiss = { showLinkTargetSheet = false }
        )
    }
}

/**
 * بطاقة اقتراح الربط بآخر ربط:
 * "هي ترا آخر مرة ربطت العداد بشي ذا تبغى نسويها مرة ثانية؟"
 */
@Composable
private fun SmartLastLinkSuggestionCard(
    lastLink: LastLinkInfo,
    onAccept: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("smart_last_link_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.75f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "ربط تلقائي مقترح",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "آخر مرة ربطت بـ «${lastLink.title}» (${if (lastLink.type == "HABIT") "عادة" else "مهمة"}). نربط بها مرة ثانية؟",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = onAccept,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("نعم، اربط", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "تجاهل",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * بطاقة عرض الهدف المرتبط حالياً (عادة أو مهمة أو تركيز حر) مع زر التغيير
 */
@Composable
private fun LinkedTargetStatusCard(
    timerState: StudyTimerState,
    linkedHabit: HabitEntity?,
    linkedTask: TaskEntity?,
    onChangeLink: () -> Unit,
    onUnlink: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                val icon = when (timerState.linkedType) {
                    "HABIT" -> Icons.Default.TrackChanges
                    "TASK" -> Icons.Default.Checklist
                    else -> Icons.Default.Timer
                }
                val iconBg = when (timerState.linkedType) {
                    "HABIT" -> MintAccent.copy(alpha = 0.2f)
                    "TASK" -> Color(0xFF3B82F6).copy(alpha = 0.2f)
                    else -> MaterialTheme.colorScheme.primaryContainer
                }
                val iconTint = when (timerState.linkedType) {
                    "HABIT" -> MintAccent
                    "TASK" -> Color(0xFF2563EB)
                    else -> MaterialTheme.colorScheme.primary
                }

                Surface(
                    shape = CircleShape,
                    color = iconBg,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                    }
                }

                Column {
                    val typeLabel = when (timerState.linkedType) {
                        "HABIT" -> "مرتبط بعادة"
                        "TASK" -> "مرتبط بمهمة"
                        else -> "الربط الحالي"
                    }
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (timerState.linkedType != "NONE") timerState.subjectName else "تركيز حر (بدون ربط)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (timerState.linkedType != "NONE" && !timerState.isActive) {
                    OutlinedButton(
                        onClick = onUnlink,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("فك الربط", fontSize = 12.sp)
                    }
                }
                FilledTonalButton(
                    onClick = onChangeLink,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("change_counter_link_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (timerState.linkedType == "NONE") "ربط بعادة أو مهمة" else "تغيير",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/**
 * البطاقة الرئيسية للعداد الدائري مع مؤشرات التوقف والاستئناف المباشرة
 */
@Composable
private fun MainCounterCard(
    timerState: StudyTimerState,
    onSelectMode: (String) -> Unit,
    onSelectTargetMinutes: (Int) -> Unit,
    onOpenPomodoroSettings: () -> Unit,
    onStart: () -> Unit,
    onTogglePause: () -> Unit,
    onFinishAndSave: () -> Unit,
    onCancel: () -> Unit,
    onOpenStandby: () -> Unit
) {
    val targetMins = (timerState.targetDurationSeconds / 60).coerceAtLeast(1)
    val extraMinutes = timerState.extraSecondsBeyondGoal / 60

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("main_counter_card"),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // اختيار نمط العداد: مؤقت تصاعدي أو بومودورو مخصص + زر وضع Standby
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = timerState.mode == "COUNT_UP",
                        onClick = { if (!timerState.isActive) onSelectMode("COUNT_UP") },
                        label = { Text("مؤقت تصاعدي", fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                    FilterChip(
                        selected = timerState.mode == "POMODORO",
                        onClick = { if (!timerState.isActive) onSelectMode("POMODORO") },
                        label = { Text("بومودورو", fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(Icons.Default.HourglassTop, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }

                FilledTonalIconButton(
                    onClick = onOpenStandby,
                    modifier = Modifier.testTag("standby_icon_btn")
                ) {
                    Icon(Icons.Default.ScreenRotation, contentDescription = "وضع Standby")
                }
            }

            // حلقة العداد الدائرية
            val primaryColor = MaterialTheme.colorScheme.primary
            val trackColor = MaterialTheme.colorScheme.surfaceVariant
            Box(
                modifier = Modifier.size(220.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 14.dp.toPx()
                    drawCircle(
                        color = trackColor,
                        style = Stroke(width = strokeWidth)
                    )
                    drawArc(
                        color = if (timerState.isGoalReached) MintAccent else primaryColor,
                        startAngle = -90f,
                        sweepAngle = 360f * timerState.progressFraction,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (timerState.mode == "POMODORO") {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (timerState.isBreakPhase) GoldWarm.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = if (timerState.isBreakPhase) {
                                    "فترة راحة ☕"
                                } else {
                                    "جولة ${UmmAlQuraCalculator.toArabicDigits(timerState.currentRound.toString())}/${UmmAlQuraCalculator.toArabicDigits(timerState.pomodoroTargetRounds.toString())}"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (timerState.isBreakPhase) GoldWarm else MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = timerState.formattedTime(),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )

                    Text(
                        text = if (timerState.isGoalReached && extraMinutes > 0) {
                            UmmAlQuraCalculator.toArabicDigits("تم الهدف ($targetMins د) • +$extraMinutes د إضافية 🔥")
                        } else if (timerState.isGoalReached) {
                            UmmAlQuraCalculator.toArabicDigits("اكتمل الهدف المطلوب ($targetMins د) ✅")
                        } else {
                            UmmAlQuraCalculator.toArabicDigits("الهدف: $targetMins دقيقة")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (timerState.isGoalReached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // مؤشرات التوقف والاستئناف المباشرة أثناء الجلسة (المطلب ٣)
            if (timerState.isActive) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (timerState.isPaused) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (timerState.isPaused) {
                            Text(
                                text = "⏸️ تم الإيقاف المؤقت الساعة: ${timerState.formattedPauseTime()}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        } else if (!timerState.resumeTimeMillis.isNullOrZero()) {
                            Text(
                                text = "▶️ تم الاستئناف الساعة: ${timerState.formattedResumeTime()} (بدأت: ${timerState.formattedStartTime()})",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        } else {
                            Text(
                                text = "🟢 الجلسة جارية • بدأت الساعة: ${timerState.formattedStartTime()}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }

            // تخصيص الهدف قبل البدء
            if (!timerState.isActive) {
                if (timerState.mode == "COUNT_UP") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        listOf(15, 25, 30, 45, 60, 90, 120).forEach { mins ->
                            FilterChip(
                                selected = targetMins == mins,
                                onClick = { onSelectTargetMinutes(mins) },
                                label = { Text(UmmAlQuraCalculator.toArabicDigits("$mins د"), fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                } else {
                    OutlinedButton(onClick = onOpenPomodoroSettings) {
                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            UmmAlQuraCalculator.toArabicDigits(
                                "تخصيص البومودورو: ${timerState.pomodoroFocusMinutes}د تركيز • ${timerState.pomodoroBreakMinutes}د راحة • ${timerState.pomodoroTargetRounds} جولات"
                            )
                        )
                    }
                }
            }

            // أزرار التحكم
            if (!timerState.isActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onStart,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("start_timer_btn")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("بدء الجلسة", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }

                    FilledTonalButton(
                        onClick = onOpenStandby,
                        modifier = Modifier.height(52.dp)
                    ) {
                        Icon(Icons.Default.ScreenRotation, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("وضع Standby", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onTogglePause,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("pause_resume_timer_btn")
                    ) {
                        Icon(
                            imageVector = if (timerState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (timerState.isPaused) "استئناف" else "إيقاف مؤقت", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onFinishAndSave,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("finish_save_timer_btn")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إنهاء وحفظ", fontWeight = FontWeight.Bold)
                    }

                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إلغاء",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

private fun Long?.isNullOrZero(): Boolean = this == null || this == 0L

/**
 * شريط تصفية التاريخ وإحصائيات الجلسات السابقة (المطلب ٣)
 */
@Composable
private fun HistorySectionHeader(
    todayKey: String,
    yesterdayKey: String,
    selectedDateKey: String,
    onSelectDateKey: (String) -> Unit,
    totalFocusMinutes: Int,
    sessionCount: Int
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "سجل الجلسات السابقة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = UmmAlQuraCalculator.toArabicDigits("الإجمالي: $totalFocusMinutes دقيقة ($sessionCount جلسات)"),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        // شريط الأيام السريعة + اختيار تاريخ مخصص
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = selectedDateKey == todayKey,
                onClick = { onSelectDateKey(todayKey) },
                label = { Text("اليوم", fontWeight = FontWeight.Bold) }
            )
            FilterChip(
                selected = selectedDateKey == yesterdayKey,
                onClick = { onSelectDateKey(yesterdayKey) },
                label = { Text("الأمس", fontWeight = FontWeight.Bold) }
            )

            val isCustomDate = selectedDateKey != todayKey && selectedDateKey != yesterdayKey
            FilterChip(
                selected = isCustomDate,
                onClick = {
                    val cal = Calendar.getInstance()
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val chosenKey = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                            onSelectDateKey(chosenKey)
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                },
                leadingIcon = {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                },
                label = {
                    Text(
                        if (isCustomDate) UmmAlQuraCalculator.toArabicDigits(selectedDateKey) else "اختر تاريخ 📅",
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    }
}

/**
 * بطاقة عرض الجلسة في السجل مع إيضاح متى توقفت ومتى اكتملت بدقة (المطلب ٣)
 */
@Composable
private fun SessionDetailTimelineCard(session: StudySessionEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val badgeColor = when (session.linkedType) {
                        "HABIT" -> MintAccent
                        "TASK" -> Color(0xFF3B82F6)
                        else -> MaterialTheme.colorScheme.primary
                    }
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(badgeColor)
                    )
                    Text(
                        text = session.subjectName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = when (session.linkedType) {
                                "HABIT" -> "عادة"
                                "TASK" -> "مهمة"
                                else -> "تركيز عام"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = UmmAlQuraCalculator.toArabicDigits("${session.durationMinutes} دقيقة"),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // السجل الزمني التفصيلي: متى بدأت، متى توقفت، متى اكتملت
            val sdf = remember { SimpleDateFormat("hh:mm a", Locale("ar")) }
            val startTimeStr = remember(session.startedAt) { sdf.format(Date(session.startedAt)) }
            val endTimeStr = remember(session.endedAt) { sdf.format(Date(session.endedAt)) }
            val pauseTimeStr = remember(session.pausedAt) {
                session.pausedAt?.let { sdf.format(Date(it)) }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🟢 بدأت: $startTimeStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (pauseTimeStr != null) {
                    Text(
                        text = "⏸️ توقفت: $pauseTimeStr",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "✅ اكتملت: $endTimeStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            if (session.notes.isNotBlank()) {
                Text(
                    text = session.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Bottom Sheet لاختيار ربط العداد بعادة أو مهمة (المطلب ٢)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LinkTargetBottomSheet(
    uiState: DailyManagerUiState,
    currentLinkedType: String,
    currentLinkedId: String?,
    onSelectTarget: (type: String, id: String, title: String, colorHex: String) -> Unit,
    onUnlink: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) } // 0 = عادات، 1 = مهام

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = Modifier.testTag("link_target_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "ربط العداد بعادة أو مهمة",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "اختر العادة أو المهمة التي ترغب في احتساب دقائق التركيز لها ومتابعة تقدمها",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = { Icon(Icons.Default.TrackChanges, contentDescription = null, modifier = Modifier.size(16.dp)) }
                ) {
                    Text("العادات (${uiState.habits.size})", fontWeight = FontWeight.Bold)
                }
                SegmentedButton(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = { Icon(Icons.Default.Checklist, contentDescription = null, modifier = Modifier.size(16.dp)) }
                ) {
                    val uncompletedCount = uiState.mainTasks.count { !it.isCompleted }
                    Text("المهام ($uncompletedCount)", fontWeight = FontWeight.Bold)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 340.dp)
            ) {
                if (selectedTab == 0) {
                    if (uiState.habits.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("لا توجد عادات مضافة بعد", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(uiState.habits, key = { it.id }) { habit ->
                                val isSelected = currentLinkedType == "HABIT" && currentLinkedId == habit.id
                                val cat = uiState.categories.find { it.id == habit.categoryId }
                                val catColor = cat?.colorHex ?: "#10B981"
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelectTarget("HABIT", habit.id, habit.title, catColor)
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(Icons.Default.TrackChanges, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Column {
                                                Text(habit.title, fontWeight = FontWeight.Bold)
                                                Text(
                                                    text = UmmAlQuraCalculator.toArabicDigits("الهدف: ${habit.targetCount} ${habit.unitLabel} • التكرار: ${habit.frequencyType}"),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        if (isSelected) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val activeTasks = uiState.mainTasks.filter { !it.isCompleted }
                    if (activeTasks.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("لا توجد مهام نشطة حالياً", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(activeTasks, key = { it.id }) { task ->
                                val isSelected = currentLinkedType == "TASK" && currentLinkedId == task.id
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelectTarget("TASK", task.id, task.title, "#3B82F6")
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(Icons.Outlined.RadioButtonUnchecked, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Column {
                                                Text(task.title, fontWeight = FontWeight.Bold)
                                                Text(
                                                    text = "القائمة: ${task.listCategory} • الأولوية: ${task.priority}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        if (isSelected) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // زر فك الربط والعودة إلى تركيز حر
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onUnlink,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("تركيز حر بدون ربط")
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("إغلاق")
                }
            }
        }
    }
}

/**
 * حوار تخصيص البومودورو
 */
@Composable
private fun CustomPomodoroDialog(
    initialFocus: Int,
    initialBreak: Int,
    initialRounds: Int,
    onDismiss: () -> Unit,
    onSave: (Int, Int, Int) -> Unit
) {
    var focusText by remember { mutableStateOf(initialFocus.toString()) }
    var breakText by remember { mutableStateOf(initialBreak.toString()) }
    var roundsText by remember { mutableStateOf(initialRounds.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تخصيص البومودورو", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = focusText,
                    onValueChange = { focusText = it.filter { c -> c.isDigit() } },
                    label = { Text("دقائق التركيز لكل جولة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = breakText,
                    onValueChange = { breakText = it.filter { c -> c.isDigit() } },
                    label = { Text("دقائق الراحة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = roundsText,
                    onValueChange = { roundsText = it.filter { c -> c.isDigit() } },
                    label = { Text("عدد الجولات المطلوبة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        (focusText.toIntOrNull() ?: 25).coerceAtLeast(5),
                        (breakText.toIntOrNull() ?: 5).coerceAtLeast(1),
                        (roundsText.toIntOrNull() ?: 2).coerceAtLeast(1)
                    )
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
