package com.example.spatial

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.features.DailyManagerUiState
import com.example.features.DailyManagerViewModel
import com.example.features.ai.AiChatScreen
import com.example.features.calendar.MainEventsCalendarScreen
import com.example.features.dashboard.TodayScreen
import com.example.features.habits.HabitsScreen
import com.example.features.prayer.PrayerScreen
import com.example.features.protection.ProtectionScreen
import com.example.features.study.StudyScreen
import com.example.features.tasks.TasksScreen
import kotlin.math.abs

/**
 * المحرك الرئيسي للتنقل الفراغي ثنائي الأبعاد (Pure 2D Spatial Swipe Navigation)
 * بدون أي أشرطة سفلية تقليدية (No bottom bars)
 *
 * الخريطة الفراغية:
 * (-1,+1) عداد التركيز      | (0,+1) المهام         | (+1,+1) التقويم الموحد
 * (-1, 0) العادات          | (0, 0) الرئيسية (Hub) | (+1, 0) الصلوات       | (+2, 0) القرآن
 * (-1,-1) الحظر والمشتتات  | (0,-1) الإعدادات      | (+1,-1) الأذكار
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpatialNavigationEngine(
    uiState: DailyManagerUiState,
    viewModel: DailyManagerViewModel,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // وضع الاستعداد الأفقي التلقائي (Landscape Standby Mode)
    if (isLandscape) {
        LandscapeStandbyScreen(
            uiState = uiState,
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    var currentRoom by remember { mutableStateOf(SpatialRoom.HOME) }
    var previousRoom by remember { mutableStateOf(SpatialRoom.HOME) }
    var showMinimap by remember { mutableStateOf(false) }
    var showAiChatSheet by remember { mutableStateOf(false) }
    var showPrayerSettingsDialog by remember { mutableStateOf(false) }
    var showKhatmahPlannerDialog by remember { mutableStateOf(false) }

    // التحكم بالرجوع: إذا لم تكن في الرئيسية (0, 0)، زر الرجوع يعيدك إلى الرئيسية
    if (currentRoom != SpatialRoom.HOME) {
        BackHandler {
            previousRoom = currentRoom
            currentRoom = SpatialRoom.HOME
        }
    }

    // حساب عتبات السحب والمسافات
    val thresholdPx = with(density) { 60.dp.toPx() }
    val edgeMarginPx = with(density) { 36.dp.toPx() }
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }

    // تتبع إيماءات التحويل (السحب والتكبير/التصغير للمينيماب)
    var accumulatedPan by remember { mutableStateOf(Offset.Zero) }
    var accumulatedZoom by remember { mutableFloatStateOf(1f) }
    var gestureValidFromCenter by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(currentRoom, uiState.diagonalSwipeEnabled) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    // 1. ميزة التكبير للخارج (Pinch-out) لفتح الخريطة المصغرة
                    accumulatedZoom *= zoom
                    if (accumulatedZoom > 1.25f) {
                        showMinimap = true
                        accumulatedZoom = 1f
                        accumulatedPan = Offset.Zero
                        return@detectTransformGestures
                    }

                    // 2. التحقق من السحب في وسط الشاشة بعيداً عن حواف النظام (Center-screen swipes)
                    if (accumulatedPan == Offset.Zero) {
                        gestureValidFromCenter = centroid.x in edgeMarginPx..(screenWidthPx - edgeMarginPx)
                    }

                    if (!gestureValidFromCenter) return@detectTransformGestures

                    // إذا كانت الغرفة هي غرفة القرآن (+2, 0)، قفل الإيماءات:
                    // السحب العادي مخصص لتقليب المصحف ولا ينتقل إلا بالسحب القوي بإصبعين أو الزر
                    if (currentRoom == SpatialRoom.QURAN) {
                        return@detectTransformGestures
                    }

                    accumulatedPan += pan
                    val dx = accumulatedPan.x
                    val dy = accumulatedPan.y

                    val absDx = abs(dx)
                    val absDy = abs(dy)

                    // هل تم تجاوز عتبة المسافة؟
                    if (absDx > thresholdPx || absDy > thresholdPx) {
                        val isDiagonalCandidate = uiState.diagonalSwipeEnabled &&
                                absDx > (thresholdPx * 0.75f) &&
                                absDy > (thresholdPx * 0.75f) &&
                                (absDx / absDy in 0.5f..2.0f)

                        val direction = when {
                            isDiagonalCandidate -> {
                                when {
                                    dx < 0 && dy < 0 -> SpatialDirection.UP_LEFT
                                    dx > 0 && dy < 0 -> SpatialDirection.UP_RIGHT
                                    dx < 0 && dy > 0 -> SpatialDirection.DOWN_LEFT
                                    else -> SpatialDirection.DOWN_RIGHT
                                }
                            }
                            absDx > absDy -> {
                                if (dx < 0) SpatialDirection.LEFT else SpatialDirection.RIGHT
                            }
                            else -> {
                                if (dy < 0) SpatialDirection.UP else SpatialDirection.DOWN
                            }
                        }

                        val target = SpatialRoom.getTargetRoom(
                            current = currentRoom,
                            direction = direction,
                            allowDiagonal = uiState.diagonalSwipeEnabled
                        )

                        if (target != null && target != currentRoom) {
                            previousRoom = currentRoom
                            currentRoom = target
                        }

                        // تصفير التراكم بعد المعالجة
                        accumulatedPan = Offset.Zero
                        accumulatedZoom = 1f
                    }
                }
            }
            .testTag("spatial_navigation_container")
    ) {
        // طبقة محتوى الغرف مع الانتقال الحركي الارتدادي التفاعلي المتوافق مع المتجه الفضائي
        val bounceSpec = spring<androidx.compose.ui.unit.IntOffset>(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
        )
        val fadeBounce = spring<Float>(
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
        )

        AnimatedContent(
            targetState = currentRoom,
            transitionSpec = {
                val dx = targetState.x - initialState.x
                val dy = targetState.y - initialState.y

                when {
                    dx > 0 -> {
                        // حركة لليمين في الخريطة: المحتوى يدخل بارتداد من اليمين
                        (slideInHorizontally(animationSpec = bounceSpec) { it } + fadeIn(animationSpec = fadeBounce))
                            .togetherWith(slideOutHorizontally(animationSpec = bounceSpec) { -it } + fadeOut(animationSpec = fadeBounce))
                    }
                    dx < 0 -> {
                        // حركة لليسار في الخريطة: المحتوى يدخل بارتداد من اليسار
                        (slideInHorizontally(animationSpec = bounceSpec) { -it } + fadeIn(animationSpec = fadeBounce))
                            .togetherWith(slideOutHorizontally(animationSpec = bounceSpec) { it } + fadeOut(animationSpec = fadeBounce))
                    }
                    dy > 0 -> {
                        // حركة لأعلى في الخريطة: المحتوى يدخل بارتداد من الأعلى
                        (slideInVertically(animationSpec = bounceSpec) { -it } + fadeIn(animationSpec = fadeBounce))
                            .togetherWith(slideOutVertically(animationSpec = bounceSpec) { it } + fadeOut(animationSpec = fadeBounce))
                    }
                    dy < 0 -> {
                        // حركة لأسفل في الخريطة: المحتوى يدخل بارتداد من الأسفل
                        (slideInVertically(animationSpec = bounceSpec) { it } + fadeIn(animationSpec = fadeBounce))
                            .togetherWith(slideOutVertically(animationSpec = bounceSpec) { -it } + fadeOut(animationSpec = fadeBounce))
                    }
                    else -> fadeIn(animationSpec = fadeBounce) togetherWith fadeOut(animationSpec = fadeBounce)
                }
            },
            label = "spatial_room_transition",
            modifier = Modifier.fillMaxSize()
        ) { targetRoom ->
            Box(modifier = Modifier.fillMaxSize()) {
                when (targetRoom) {
                    // (0, 0) الرئيسية: ملخص اليوم وزر المساعد الذكي البارز
                    SpatialRoom.HOME -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            SpatialTopHud(
                                currentRoom = SpatialRoom.HOME,
                                onOpenMinimap = { showMinimap = true }
                            )
                            TodayScreen(
                                uiState = uiState,
                                onOpenReligiousTab = {
                                    previousRoom = currentRoom
                                    currentRoom = SpatialRoom.PRAYERS
                                },
                                onSetCheckboxHabit = { habit, checked ->
                                    viewModel.setCheckboxHabitState(habit, checked)
                                },
                                onIncrementValueHabit = { habit ->
                                    viewModel.incrementValueHabit(habit, 1)
                                },
                                onStartHabitTimer = { habit ->
                                    viewModel.startHabitTimer(habit)
                                    previousRoom = currentRoom
                                    currentRoom = SpatialRoom.FOCUS_TIMER
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
                                onHardwareStepsDetected = { raw ->
                                    viewModel.onHardwareSensorStepsDetected(raw)
                                },
                                onNavigateToTab = { tabIdx ->
                                    previousRoom = currentRoom
                                    currentRoom = when (tabIdx) {
                                        1 -> SpatialRoom.PRAYERS
                                        2 -> SpatialRoom.HABITS
                                        3 -> SpatialRoom.TASKS
                                        4 -> SpatialRoom.FOCUS_TIMER
                                        5 -> SpatialRoom.BLOCKER
                                        else -> SpatialRoom.HOME
                                    }
                                },
                                onOpenAiAssistant = { showAiChatSheet = true },
                                onOpenCalendar = {
                                    previousRoom = currentRoom
                                    currentRoom = SpatialRoom.UNIFIED_CALENDAR
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // (0, +1) المهام
                    SpatialRoom.TASKS -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            SpatialTopHud(
                                currentRoom = SpatialRoom.TASKS,
                                onOpenMinimap = { showMinimap = true }
                            )
                            TasksScreen(
                                uiState = uiState,
                                viewModel = viewModel,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // (0, -1) الإعدادات والإحصائيات: 3 أقسام
                    SpatialRoom.SETTINGS_STATS -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            SpatialTopHud(
                                currentRoom = SpatialRoom.SETTINGS_STATS,
                                onOpenMinimap = { showMinimap = true }
                            )
                            SettingsStatsSpatialRoom(
                                uiState = uiState,
                                viewModel = viewModel,
                                onOpenPrayerSettings = { showPrayerSettingsDialog = true },
                                onOpenKhatmahPlan = { showKhatmahPlannerDialog = true },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // (-1, 0) العادات
                    SpatialRoom.HABITS -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            SpatialTopHud(
                                currentRoom = SpatialRoom.HABITS,
                                onOpenMinimap = { showMinimap = true }
                            )
                            HabitsScreen(
                                uiState = uiState,
                                viewModel = viewModel,
                                onStartHabitTimer = { habit ->
                                    viewModel.startHabitTimer(habit)
                                    previousRoom = currentRoom
                                    currentRoom = SpatialRoom.FOCUS_TIMER
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // (-1, +1) عداد التركيز
                    SpatialRoom.FOCUS_TIMER -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            SpatialTopHud(
                                currentRoom = SpatialRoom.FOCUS_TIMER,
                                onOpenMinimap = { showMinimap = true }
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                StudyScreen(
                                    uiState = uiState,
                                    viewModel = viewModel
                                )
                            }
                        }
                    }

                    // (-1, -1) الحظر والمشتتات
                    SpatialRoom.BLOCKER -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            SpatialTopHud(
                                currentRoom = SpatialRoom.BLOCKER,
                                onOpenMinimap = { showMinimap = true }
                            )
                            Box(modifier = Modifier.weight(1f)) {
                                ProtectionScreen(
                                    uiState = uiState,
                                    viewModel = viewModel
                                )
                            }
                        }
                    }

                    // (+1, 0) الصلوات
                    SpatialRoom.PRAYERS -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            SpatialTopHud(
                                currentRoom = SpatialRoom.PRAYERS,
                                onOpenMinimap = { showMinimap = true }
                            )
                            PrayerScreen(
                                uiState = uiState,
                                viewModel = viewModel,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // (+1, +1) التقويم الموحد
                    SpatialRoom.UNIFIED_CALENDAR -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            SpatialTopHud(
                                currentRoom = SpatialRoom.UNIFIED_CALENDAR,
                                onOpenMinimap = { showMinimap = true }
                            )
                            MainEventsCalendarScreen(
                                onBack = {
                                    previousRoom = currentRoom
                                    currentRoom = SpatialRoom.HOME
                                },
                                viewModel = viewModel,
                                uiState = uiState,
                                isEmbedded = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // (+1, -1) الأذكار
                    SpatialRoom.ADHKAR -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            SpatialTopHud(
                                currentRoom = SpatialRoom.ADHKAR,
                                onOpenMinimap = { showMinimap = true }
                            )
                            AdhkarSpatialRoom(
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // (+2, 0) القرآن والورد
                    SpatialRoom.QURAN -> {
                        QuranWirdSpatialRoom(
                            uiState = uiState,
                            viewModel = viewModel,
                            onBackToPrayers = {
                                previousRoom = currentRoom
                                currentRoom = SpatialRoom.PRAYERS
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        // طبقة توهج الحواف التلميحية (Edge hints)
        SpatialEdgeHintsOverlay(currentRoom = currentRoom)

        // الخريطة المصغرة التفاعلية (Minimap)
        if (showMinimap) {
            SpatialMinimapModal(
                currentRoom = currentRoom,
                onTeleportTo = { target ->
                    previousRoom = currentRoom
                    currentRoom = target
                    showMinimap = false
                },
                onDismiss = { showMinimap = false }
            )
        }

        // نافذة المساعد الذكي المنبثقة
        if (showAiChatSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAiChatSheet = false },
                modifier = Modifier.fillMaxHeight(0.85f)
            ) {
                AiChatScreen(
                    uiState = uiState,
                    viewModel = viewModel,
                    onBack = { showAiChatSheet = false }
                )
            }
        }

        // نافذة إعدادات الصلاة
        if (showPrayerSettingsDialog) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showPrayerSettingsDialog = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    com.example.features.prayer.PrayerSettingsAndAlarmsScreen(
                        uiState = uiState,
                        viewModel = viewModel,
                        onBack = { showPrayerSettingsDialog = false }
                    )
                }
            }
        }

        // نافذة خطة الختمة
        if (showKhatmahPlannerDialog) {
            com.example.features.prayer.KhatmahPlannerDialog(
                wirdProgress = uiState.wirdProgress,
                onSavePlan = { targetPages, planType, title, startPage ->
                    viewModel.configureKhatmahPlan(targetPages, planType, title, startPage)
                    showKhatmahPlannerDialog = false
                },
                onDismiss = { showKhatmahPlannerDialog = false }
            )
        }
    }
}
