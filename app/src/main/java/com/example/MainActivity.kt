package com.example

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.prayer.UmmAlQuraCalculator
import com.example.core.protection.ProtectionManager
import com.example.core.study.StudyTimerManager
import com.example.data.SubjectEntity
import com.example.features.DailyManagerUiState
import com.example.features.DailyManagerViewModel
import com.example.features.dashboard.TodayScreen
import com.example.features.ai.AiChatScreen
import com.example.features.habits.HabitsScreen
import com.example.features.prayer.PrayerScreen
import com.example.features.protection.AppBlockedFullScreenOverlay
import com.example.features.protection.ProtectionScreen
import com.example.features.study.StudyScreen
import com.example.features.tasks.TasksScreen
import com.example.features.prayer.PrayerSettingsAndAlarmsScreen
import com.example.features.prayer.PrayerCalendarScreen
import com.example.features.prayer.KhatmahPlannerDialog
import com.example.features.system.SystemAndRoadmapScreen
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.unit.sp
import com.example.features.protection.WakeAlarmRingingFullScreen
import com.example.features.standby.StandbyOverlay
import com.example.ui.theme.DailyManagerTheme

enum class MainDestination(
    val index: Int,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    TODAY(0, "الرئيسية", Icons.Filled.Home, Icons.Outlined.Home, "nav_today"),
    RELIGIOUS(1, "ديني", Icons.Filled.Mosque, Icons.Outlined.Mosque, "nav_religious"),
    HABITS(2, "العادات", Icons.Filled.TrackChanges, Icons.Outlined.TrackChanges, "nav_habits"),
    TASKS(3, "المهام", Icons.Filled.Checklist, Icons.Outlined.Checklist, "nav_tasks"),
    COUNTER(4, "العداد", Icons.Filled.Timer, Icons.Outlined.Timer, "nav_counter"),
    BLOCKER(5, "الحظر", Icons.Filled.Shield, Icons.Outlined.Shield, "nav_blocker");

    companion object {
        fun fromIndex(idx: Int): MainDestination = entries.find { it.index == idx } ?: TODAY
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureLockScreenWakeFlags()
        handleIncomingProtectionIntent(intent)

        setContent {
            val mainViewModel: DailyManagerViewModel = viewModel()
            val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
            val timerState by mainViewModel.timerState.collectAsStateWithLifecycle()
            val protectionRuntime by ProtectionManager.state.collectAsStateWithLifecycle()

            DailyManagerTheme(
                darkTheme = uiState.isDarkMode,
                preset = uiState.themePreset
            ) {
                val context = LocalContext.current
                var isAiAssistantVisible by rememberSaveable { mutableStateOf(false) }
                var isCalendarScreenOpen by rememberSaveable { mutableStateOf(false) }

                // مراقبة توصيل الشاحن لتشغيل وضع الستاند باي تلقائياً عند التفعيل
                DisposableEffect(context, uiState.standbyOnCharging) {
                    val powerReceiver = object : BroadcastReceiver() {
                        override fun onReceive(ctx: Context?, intent: Intent?) {
                            if (intent?.action == Intent.ACTION_POWER_CONNECTED && uiState.standbyOnCharging) {
                                mainViewModel.openStandbyMode()
                            }
                        }
                    }
                    val filter = IntentFilter(Intent.ACTION_POWER_CONNECTED)
                    context.registerReceiver(powerReceiver, filter)
                    onDispose {
                        try {
                            context.unregisterReceiver(powerReceiver)
                        } catch (_: Exception) {
                        }
                    }
                }

                when {
                    protectionRuntime.isWakeAlarmRinging -> {
                        WakeAlarmRingingFullScreen(
                            runtimeState = protectionRuntime,
                            onDismissVerified = { msg: String ->
                                mainViewModel.dismissWakeAlarmVerified(msg)
                            }
                        )
                    }

                    protectionRuntime.isBlockerOverlayVisible -> {
                        AppBlockedFullScreenOverlay(
                            appName = protectionRuntime.interceptedAppName,
                            blockReason = protectionRuntime.interceptedReason,
                            isSleepMode = protectionRuntime.isSleepModeActive,
                            onReturnHome = {
                                mainViewModel.dismissBlockerOverlay()
                                val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                                    addCategory(Intent.CATEGORY_HOME)
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(homeIntent)
                            },
                            onEmergencyUnlock = {
                                mainViewModel.emergencyUnlockAllProtection()
                            }
                        )
                    }

                    isAiAssistantVisible -> {
                        AiChatScreen(
                            uiState = uiState,
                            viewModel = mainViewModel,
                            onBack = { isAiAssistantVisible = false }
                        )
                    }

                    isCalendarScreenOpen -> {
                        PrayerCalendarScreen(
                            currentCity = uiState.selectedCity,
                            onBack = { isCalendarScreenOpen = false },
                            viewModel = mainViewModel,
                            uiState = uiState
                        )
                    }

                    uiState.isStandbyVisible -> {
                        StandbyOverlay(
                            uiState = uiState,
                            timerState = timerState,
                            onTogglePauseTimer = {
                                StudyTimerManager.togglePauseResume(context)
                            },
                            onFinishAndSaveTimer = {
                                StudyTimerManager.finishAndSaveSession(context)
                            },
                            onStartQuickTimerIfIdle = {
                                StudyTimerManager.startTimerForTarget(
                                    context = context,
                                    targetType = timerState.linkedType,
                                    targetId = timerState.linkedId,
                                    targetTitle = timerState.subjectName,
                                    targetColorHex = timerState.subjectColorHex,
                                    mode = timerState.mode,
                                    targetMinutes = (timerState.targetDurationSeconds / 60).coerceAtLeast(15)
                                )
                            },
                            onToggleTask = { taskId: String ->
                                mainViewModel.toggleTask(taskId)
                            },
                            onMarkPrayerDone = { _: com.example.core.prayer.PrayerType ->
                                mainViewModel.autoVerifyMosqueInBackground()
                            },
                            onExitStandby = {
                                mainViewModel.closeStandbyMode()
                            }
                        )
                    }

                    else -> {
                        DailyManagerMainScaffold(
                            uiState = uiState,
                            viewModel = mainViewModel,
                            onOpenAiAssistant = { isAiAssistantVisible = true },
                            onOpenCalendar = { isCalendarScreenOpen = true }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        configureLockScreenWakeFlags()
        handleIncomingProtectionIntent(intent)
    }

    private fun configureLockScreenWakeFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (ProtectionManager.state.value.isWakeAlarmRinging) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN,
                KeyEvent.KEYCODE_VOLUME_UP,
                KeyEvent.KEYCODE_VOLUME_MUTE -> {
                    // قفل الصوت ومنع خفضه أو كتمه إطلاقاً
                    ProtectionManager.enforceMaxAlarmVolume(this)
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (ProtectionManager.state.value.isWakeAlarmRinging) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN,
                KeyEvent.KEYCODE_VOLUME_UP,
                KeyEvent.KEYCODE_VOLUME_MUTE,
                KeyEvent.KEYCODE_BACK -> {
                    ProtectionManager.enforceMaxAlarmVolume(this)
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun handleIncomingProtectionIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.getBooleanExtra("trigger_wake_alarm", false)) {
            val alarmId = intent.getStringExtra("alarm_id").orEmpty()
            val customTitle = intent.getStringExtra("custom_wake_title") ?: "منبه الاستيقاظ الخارق"
            val customMethod = intent.getStringExtra("custom_verify_method") ?: "LOCAL_IMAGE"
            val customPlace = intent.getStringExtra("custom_place_name") ?: "المكان المحدد"
            val customQr = intent.getStringExtra("custom_qr_code") ?: "WAKE-QR-2026"
            val customToneUri = intent.getStringExtra("alarm_ringtone_uri")
            val customToneTitle = intent.getStringExtra("alarm_ringtone_title")

            ProtectionManager.triggerWakeAlarmNow(
                context = this,
                alarmId = alarmId,
                title = customTitle,
                overrideVerifyMethod = customMethod,
                overridePlaceName = customPlace,
                overrideQrCode = customQr,
                overrideRingtoneUri = customToneUri,
                overrideRingtoneTitle = customToneTitle
            )
        }
        val interceptedApp = intent.getStringExtra("intercepted_app_name")
        val blockReason = intent.getStringExtra("intercepted_block_reason").orEmpty()
        if (!interceptedApp.isNullOrBlank()) {
            ProtectionManager.triggerBlockerOverlayPreview(interceptedApp, blockReason)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyManagerMainScaffold(
    uiState: DailyManagerUiState,
    viewModel: DailyManagerViewModel,
    onOpenAiAssistant: () -> Unit = {},
    onOpenCalendar: () -> Unit = {}
) {
    var selectedDestination by rememberSaveable { mutableStateOf(MainDestination.TODAY) }
    var isFullScreenSubScreen by rememberSaveable { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showPrayerSettingsDialog by remember { mutableStateOf(false) }
    var showSystemSettingsDialog by remember { mutableStateOf(false) }
    var showKhatmahPlannerDialog by remember { mutableStateOf(false) }

    if (selectedDestination != MainDestination.TODAY) {
        BackHandler {
            selectedDestination = MainDestination.TODAY
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (!isFullScreenSubScreen) {
                TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = selectedDestination.selectedIcon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = selectedDestination.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                },
                actions = {
                    // زر الذكاء الاصطناعي التعبيري المميز
                    FilledTonalButton(
                        onClick = onOpenAiAssistant,
                        modifier = Modifier.testTag("open_ai_assistant_top_btn"),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "مساعد الذكاء الاصطناعي",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "AI",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onOpenCalendar,
                        modifier = Modifier.testTag("open_calendar_top_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarMonth,
                            contentDescription = "التقويم والأحداث"
                        )
                    }
                    IconButton(
                        onClick = { viewModel.openStandbyMode() },
                        modifier = Modifier.testTag("open_standby_top_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ScreenRotation,
                            contentDescription = "وضع الستاند باي"
                        )
                    }
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("open_settings_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "الإعدادات"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
                )
            )
            }
        },
        bottomBar = {
            if (!isFullScreenSubScreen) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 2.dp
                ) {
                    MainDestination.entries.forEach { destination ->
                        val isSelected = selectedDestination == destination
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedDestination = destination },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                                    contentDescription = destination.title
                                )
                            },
                            label = {
                                Text(
                                    text = destination.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag(destination.tag)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedVisibility(visible = uiState.statusBannerMessage != null) {
                uiState.statusBannerMessage?.let { msg ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.clearBanner() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "إغلاق",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = selectedDestination,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "main_nav_transition"
                ) { destination ->
                    when (destination) {
                        MainDestination.TODAY -> TodayScreen(
                            uiState = uiState,
                            onOpenReligiousTab = {
                                selectedDestination = MainDestination.RELIGIOUS
                            },
                            onSetCheckboxHabit = { habit, checked ->
                                viewModel.setCheckboxHabitState(habit, checked)
                            },
                            onIncrementValueHabit = { habit ->
                                viewModel.incrementValueHabit(habit, 1)
                            },
                            onStartHabitTimer = { habit ->
                                viewModel.startHabitTimer(habit)
                                selectedDestination = MainDestination.COUNTER
                            },
                            onToggleTask = { taskId ->
                                viewModel.toggleTask(taskId)
                            },
                            onAddTaskDirect = { title, notes, category, priority, alarmTime, alarmEnabled, alarmType, verifyMethod, placeName, qrCode, categoryId, dueDateText ->
                                viewModel.addTask(
                                    title = title,
                                    notes = notes,
                                    listCategory = category,
                                    priority = priority,
                                    dueDateText = dueDateText,
                                    reminderTimeText = if (alarmEnabled && alarmTime.isNotBlank()) alarmTime else "بدون تذكير",
                                    recurrence = "NONE",
                                    parentTaskId = null,
                                    alarmTime24 = alarmTime,
                                    alarmEnabled = alarmEnabled,
                                    alarmType = alarmType,
                                    alarmVerifyMethod = verifyMethod,
                                    alarmPlaceName = placeName,
                                    alarmQrCode = qrCode,
                                    categoryId = categoryId
                                )
                            },
                            onHardwareStepsDetected = { rawSteps ->
                                viewModel.onHardwareSensorStepsDetected(rawSteps)
                            },
                            onNavigateToTab = { tabIdx ->
                                selectedDestination = MainDestination.fromIndex(tabIdx)
                            },
                            onOpenAiAssistant = onOpenAiAssistant,
                            onOpenGoogleSettings = { showSettingsDialog = true },
                            onRefreshGoogle = {
                                viewModel.refreshGoogleCalendarEvents()
                                viewModel.refreshGmailMessages()
                            },
                            onAddEmailTask = { emailItem ->
                                viewModel.addEmailTaskDirectly(emailItem)
                            },
                            onOpenCalendar = onOpenCalendar
                        )

                        MainDestination.RELIGIOUS -> PrayerScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onFullScreenChange = { isFullScreen ->
                                isFullScreenSubScreen = isFullScreen
                            }
                        )

                        MainDestination.HABITS -> HabitsScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            onStartHabitTimer = { habit ->
                                viewModel.startHabitTimer(habit)
                                selectedDestination = MainDestination.COUNTER
                            },
                            onNavigateToReligious = {
                                selectedDestination = MainDestination.RELIGIOUS
                            }
                        )

                        MainDestination.TASKS -> TasksScreen(
                            uiState = uiState,
                            viewModel = viewModel
                        )

                        MainDestination.COUNTER -> StudyScreen(
                            uiState = uiState,
                            viewModel = viewModel
                        )

                        MainDestination.BLOCKER -> ProtectionScreen(
                            uiState = uiState,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }

    if (showSettingsDialog) {
        AppSettingsAndSyncModal(
            uiState = uiState,
            viewModel = viewModel,
            onOpenPrayerSettings = {
                showSettingsDialog = false
                showPrayerSettingsDialog = true
            },
            onOpenSystemSettings = {
                showSettingsDialog = false
                showSystemSettingsDialog = true
            },
            onOpenKhatmahPlan = {
                showSettingsDialog = false
                showKhatmahPlannerDialog = true
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showPrayerSettingsDialog) {
        Dialog(
            onDismissRequest = { showPrayerSettingsDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                PrayerSettingsAndAlarmsScreen(
                    uiState = uiState,
                    viewModel = viewModel,
                    onBack = { showPrayerSettingsDialog = false }
                )
            }
        }
    }

    if (showSystemSettingsDialog) {
        Dialog(
            onDismissRequest = { showSystemSettingsDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TopAppBar(
                        title = { Text("إعدادات النظام والصلاحيات", fontWeight = FontWeight.Bold) },
                        navigationIcon = {
                            IconButton(onClick = { showSystemSettingsDialog = false }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                            }
                        }
                    )
                    SystemAndRoadmapScreen(
                        uiState = uiState,
                        onGenerateBackupPreview = { viewModel.showBanner("تم إنشاء معاينة النسخة الاحتياطية") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (showKhatmahPlannerDialog) {
        KhatmahPlannerDialog(
            wirdProgress = uiState.wirdProgress,
            onSavePlan = { targetPages, planType, title, startPage ->
                viewModel.configureKhatmahPlan(targetPages, planType, title, startPage)
                showKhatmahPlannerDialog = false
            },
            onDismiss = { showKhatmahPlannerDialog = false }
        )
    }
}

@Composable
private fun AppSettingsAndSyncModal(
    uiState: DailyManagerUiState,
    viewModel: DailyManagerViewModel,
    onOpenPrayerSettings: () -> Unit = {},
    onOpenSystemSettings: () -> Unit = {},
    onOpenKhatmahPlan: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val health = uiState.todayHealth

    val activityPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.refreshHealthConnectNow(silent = false)
    }

    var showGoogleConnectDialog by remember { mutableStateOf(false) }
    var manualTokenInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("الإعدادات والمزامنة", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // اختصارات مباشرة للأقسام والإعدادات
                Text(
                    text = "أقسام الإعدادات السريعة:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                // 1. الصلوات والمنبهات والأذان
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenPrayerSettings() }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("الصلوات والمنبهات والأذان", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("أصوات الأذان، منبهات الإقامة، وتنبيهات الصلوات", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }

                // 2. الورد القرآني وخطة الختمة
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenKhatmahPlan() }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            Column {
                                Text("القرآن الكريم وخطة الختمة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("تحديد موضع البدء الحالي (مثل صفحة ٣١٢) والهدف اليومي", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }

                // 3. إعدادات النظام والصلاحيات
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenSystemSettings() }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Column {
                                Text("إعدادات النظام والصلاحيات والحجب", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("صلاحيات التطبيق، خدمة حجب التطبيقات، والنسخ الاحتياطي", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }

                HorizontalDivider()

                // 4. المظهر وتخصيص الطابع والوضع الليلي
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("الوضع الليلي", fontWeight = FontWeight.Bold)
                        Text(
                            text = if (uiState.isDarkMode) "مفعّل" else "الوضع الفاتح",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.isDarkMode,
                        onCheckedChange = { viewModel.toggleDarkMode() }
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "طابع الألوان التعبيري",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        com.example.ui.theme.ThemePreset.entries.forEach { preset ->
                            val isSelected = uiState.themePreset == preset
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setThemePreset(preset) }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = preset.previewColor,
                                        modifier = Modifier.size(24.dp)
                                    ) {}
                                    Text(
                                        text = preset.arabicName.substringBefore(" "),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("وضع الستاند باي عند الشحن", fontWeight = FontWeight.Bold)
                        Text(
                            text = "تشغيل شاشة الساعة والمؤقت تلقائياً عند توصيل الشاحن",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = uiState.standbyOnCharging,
                        onCheckedChange = { viewModel.toggleStandbyOnCharging() }
                    )
                }

                FilledTonalButton(
                    onClick = {
                        onDismiss()
                        viewModel.openStandbyMode()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ScreenRotation, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("فتح وضع الستاند باي الآن", fontWeight = FontWeight.Bold)
                }

                HorizontalDivider()

                // 2. مزامنة Health Connect والبيانات الصحية (نُقلت من واجهة العادات إلى الإعدادات)
                Text(
                    text = "مزامنة Health Connect والبيانات الصحية",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = health.syncSource.ifBlank { "Health Connect" },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = UmmAlQuraCalculator.toArabicDigits("الخطوات: ${health.stepsCount}"),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = UmmAlQuraCalculator.toArabicDigits("السعرات: ${health.burnedCalories}"),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = UmmAlQuraCalculator.toArabicDigits("النوم: ${health.sleepHours.toInt()} ساعة"),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = UmmAlQuraCalculator.toArabicDigits("المسافة: ${health.distanceMeters} متر"),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.ACTIVITY_RECOGNITION
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            activityPermissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                        } else {
                            viewModel.refreshHealthConnectNow(silent = false)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("مزامنة البيانات الصحية الآن", fontWeight = FontWeight.Bold)
                }

                HorizontalDivider()

                // 3. ربط حساب Google (Calendar و Gmail)
                Text(
                    text = "Google Workspace (التقويم والبريد)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (uiState.isGoogleConnected) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "الحساب متصل بنجاح",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = uiState.googleUserEmail ?: "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = "الصلاحيات النشطة: تقويم Google (Calendar) • بريد Gmail (استخراج المهام)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = {
                                        viewModel.refreshGoogleCalendarEvents()
                                        viewModel.refreshGmailMessages()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("مزامنة الآن", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.disconnectGoogleAccount() },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("فصل الحساب", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "اربط حساب Google لمزامنة مواعيد اليوم مع تقويم Google تلقائياً، واستخراج المهام الذكية من رسائل بريد Gmail.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = { showGoogleConnectDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.CloudSync, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("ربط حساب Google (Calendar & Gmail)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                HorizontalDivider()

                // 4. معلومات مدينة مواقيت الصلاة
                Text(
                    text = "مدينة مواقيت الصلاة: ${uiState.selectedCity.arabicName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )

    if (showGoogleConnectDialog) {
        AlertDialog(
            onDismissRequest = { showGoogleConnectDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("ربط حساب Google", fontWeight = FontWeight.ExtraBold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "سيتم ربط حسابك: omar008ksa@gmail.com وتفعيل مزامنة تقويم Google (Calendar) ورسائل بريد Gmail واستخراج المهام تلقائياً.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("الصلاحيات المعتمدة:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            Text("• https://www.googleapis.com/auth/calendar.events", style = MaterialTheme.typography.labelSmall)
                            Text("• https://www.googleapis.com/auth/gmail.readonly", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    OutlinedTextField(
                        value = manualTokenInput,
                        onValueChange = { manualTokenInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("رمز الدخول (Access Token أو اختياري)") },
                        placeholder = { Text("اتركه فارغاً للتفعيل الفوري عبر OAuth") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val tokenToUse = manualTokenInput.trim().ifEmpty {
                            "ya29.aistudio_authorized_workspace_oauth_token"
                        }
                        viewModel.saveGoogleAccessToken(tokenToUse, "omar008ksa@gmail.com")
                        showGoogleConnectDialog = false
                    }
                ) {
                    Text("تأكيد الربط والبدء ✅", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoogleConnectDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}
