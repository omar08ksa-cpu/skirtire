package com.example.features.productivity

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.study.StudyTimerManager
import com.example.data.AppEventEntity
import com.example.data.HabitEntity
import com.example.data.TaskEntity
import com.example.features.DailyManagerUiState
import com.example.features.DailyManagerViewModel
import com.example.features.calendar.MainEventsCalendarScreen
import com.example.features.habits.HabitsScreen
import com.example.features.study.StudyScreen
import com.example.features.tasks.TasksScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductivityHub(
    uiState: DailyManagerUiState,
    viewModel: DailyManagerViewModel,
    initialTab: Int = 0,
    onTabSelected: (Int) -> Unit = {},
    onNavigateToReligious: () -> Unit = {},
    onNavigateToQuran: () -> Unit = onNavigateToReligious,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }
    var showScopedTimerDialog by remember { mutableStateOf(false) }
    val timerState by StudyTimerManager.timerState.collectAsStateWithLifecycle()

    LaunchedEffect(initialTab) {
        if (initialTab in 0..2) {
            selectedTab = initialTab
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // شريط التبويبات العلوي: [ العادات | المهام | التقويم ]
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("productivity_segmented_tabs")
            ) {
                SegmentedButton(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        onTabSelected(0)
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.TrackChanges,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("tab_habits")
                ) {
                    Text("العادات", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                SegmentedButton(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        onTabSelected(1)
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Checklist,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("tab_tasks")
                ) {
                    Text("المهام", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                SegmentedButton(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        onTabSelected(2)
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    modifier = Modifier.testTag("tab_calendar")
                ) {
                    Text("التقويم", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // شريط سريع عند وجود مؤقت نشط
        if (timerState.isActive) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clickable { showScopedTimerDialog = true }
                    .testTag("active_timer_banner"),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Column {
                            Text(
                                text = "مؤقت نشط: ${timerState.subjectName}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = timerState.formattedTime(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = { showScopedTimerDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("عرض المؤقت", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // المحتوى التفاعلي حسب التبويب
        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "productivity_tab_transition"
            ) { tab ->
                when (tab) {
                    0 -> HabitsScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onStartHabitTimer = { habit: HabitEntity ->
                            viewModel.startHabitTimer(habit)
                            showScopedTimerDialog = true
                        },
                        onNavigateToReligious = onNavigateToReligious,
                        onNavigateToQuran = onNavigateToQuran
                    )

                    1 -> TasksScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onStartTaskTimer = { task: TaskEntity ->
                            StudyTimerManager.startTimerForTarget(
                                context = context,
                                targetType = "TASK",
                                targetId = task.id,
                                targetTitle = task.title,
                                targetColorHex = "#3B82F6",
                                mode = "COUNT_UP",
                                targetMinutes = 25
                            )
                            showScopedTimerDialog = true
                        }
                    )

                    2 -> MainEventsCalendarScreen(
                        onBack = { selectedTab = 0 },
                        viewModel = viewModel,
                        uiState = uiState,
                        isEmbedded = true,
                        onStartEventTimer = { event: AppEventEntity ->
                            StudyTimerManager.startTimerForTarget(
                                context = context,
                                targetType = "EVENT",
                                targetId = event.id,
                                targetTitle = event.title,
                                targetColorHex = event.colorHex,
                                mode = "COUNT_UP",
                                targetMinutes = 30
                            )
                            showScopedTimerDialog = true
                        }
                    )
                }
            }
        }
    }

    // نافذة جلسة المؤقت المرتبطة بالعنصر
    if (showScopedTimerDialog) {
        Dialog(
            onDismissRequest = { showScopedTimerDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TopAppBar(
                        title = {
                            Text(
                                text = "جلسة التركيز والمؤقت: ${timerState.subjectName}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { showScopedTimerDialog = false }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "رجوع"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    Box(modifier = Modifier.weight(1f)) {
                        StudyScreen(
                            uiState = uiState,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}
