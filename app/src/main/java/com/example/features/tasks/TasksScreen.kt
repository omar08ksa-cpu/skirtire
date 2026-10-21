package com.example.features.tasks

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.example.features.calendar.MainEventsCalendarScreen
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.core.protection.PlacePhotoHelper
import com.example.data.CategoryEntity
import com.example.data.TaskEntity
import com.example.features.DailyManagerUiState
import com.example.features.DailyManagerViewModel
import com.example.features.habits.AlarmCustomizationFields
import com.example.features.habits.CustomizableAlarmDialog
import com.example.ui.theme.PriorityHighRed
import com.example.ui.theme.PriorityLowGreen
import com.example.ui.theme.PriorityMediumAmber

@Composable
fun TasksScreen(
    uiState: DailyManagerUiState,
    viewModel: DailyManagerViewModel,
    modifier: Modifier = Modifier,
    onStartTaskTimer: (TaskEntity) -> Unit = {}
) {
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var parentForSubtask by remember { mutableStateOf<TaskEntity?>(null) }
    var editingAlarmTask by remember { mutableStateOf<TaskEntity?>(null) }
    var showCategoryManagerDialog by remember { mutableStateOf(false) }

    val userCategories = remember(uiState.categories) {
        val list = mutableListOf("الكل")
        uiState.categories.forEach { cat ->
            if (cat.name.isNotBlank() && !list.contains(cat.name)) {
                list.add(cat.name)
            }
        }
        if (list.size == 1) {
            // إضافة فئات افتراضية إذا لم تكن هناك فئات مضافة بعد
            list.addAll(listOf("اليوم", "الدراسة", "العمل", "شخصي"))
        }
        list
    }

    Box(modifier = modifier.fillMaxSize()) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0.dp),
                    floatingActionButton = {
                        ExtendedFloatingActionButton(
                            onClick = {
                                parentForSubtask = null
                                showAddTaskDialog = true
                            },
                            icon = { Icon(Icons.Default.Add, contentDescription = null) },
                            text = { Text("إضافة مهمة", fontWeight = FontWeight.Bold) },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.testTag("add_task_fab")
                        )
                    }
                ) { innerPadding ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .testTag("tasks_screen_list"),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    userCategories.forEach { cat ->
                        FilterChip(
                            selected = uiState.selectedTaskCategory == cat,
                            onClick = { viewModel.selectTaskCategory(cat) },
                            label = { Text(cat, fontWeight = FontWeight.Bold) }
                        )
                    }

                    OutlinedButton(
                        onClick = { showCategoryManagerDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("فئة جديدة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (uiState.mainTasks.isEmpty()) {
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
                                imageVector = Icons.Default.Checklist,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "لا توجد مهام حالياً",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Button(
                                onClick = {
                                    parentForSubtask = null
                                    showAddTaskDialog = true
                                }
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إضافة مهمة جديدة", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(uiState.mainTasks, key = { it.id }) { task ->
                    val subtasks = uiState.subtasksByParentId[task.id].orEmpty()
                    TaskItemCard(
                        task = task,
                        subtasks = subtasks,
                        onToggleMain = { viewModel.toggleTask(task.id) },
                        onToggleSubtask = { subId -> viewModel.toggleTask(subId) },
                        onAddSubtask = {
                            parentForSubtask = task
                            showAddTaskDialog = true
                        },
                        onEditAlarm = { editingAlarmTask = task },
                        onDeleteMain = { viewModel.deleteTask(task.id) },
                        onStartTimer = { onStartTaskTimer(task) }
                    )
                }
            }
        }
    }

    if (showAddTaskDialog) {
        QuickAddTaskBottomSheet(
            parentTask = parentForSubtask,
            categories = uiState.categories,
            defaultListCategory = if (uiState.selectedTaskCategory != "الكل") uiState.selectedTaskCategory else "اليوم",
            onDismiss = { showAddTaskDialog = false },
            onConfirm = { title, notes, category, priority, alarmTime, alarmEnabled, alarmType, verifyMethod, placeName, qrCode, categoryId, dueDateText ->
                viewModel.addTask(
                    title = title,
                    notes = notes,
                    listCategory = category,
                    priority = priority,
                    dueDateText = dueDateText,
                    reminderTimeText = if (alarmEnabled && alarmTime.isNotBlank()) alarmTime else "بدون تذكير",
                    recurrence = "NONE",
                    parentTaskId = parentForSubtask?.id,
                    alarmTime24 = alarmTime,
                    alarmEnabled = alarmEnabled,
                    alarmType = alarmType,
                    alarmVerifyMethod = verifyMethod,
                    alarmPlaceName = placeName,
                    alarmQrCode = qrCode,
                    categoryId = categoryId
                )
                showAddTaskDialog = false
            }
        )
    }

    editingAlarmTask?.let { task ->
        CustomizableAlarmDialog(
            title = "منبه المهمة: ${task.title}",
            initialEnabled = task.alarmEnabled,
            initialTime24 = task.alarmTime24,
            initialAlarmType = task.alarmType,
            initialVerifyMethod = task.alarmVerifyMethod,
            initialPlaceName = task.alarmPlaceName,
            initialQrCode = task.alarmQrCode,
            onDismiss = { editingAlarmTask = null },
            onSave = { time24, enabled, alarmType, verifyMethod, placeName, qrCode ->
                viewModel.updateTaskAlarm(
                    taskId = task.id,
                    alarmTime24 = time24,
                    alarmEnabled = enabled,
                    alarmType = alarmType,
                    alarmVerifyMethod = verifyMethod,
                    alarmPlaceName = placeName,
                    alarmQrCode = qrCode
                )
                editingAlarmTask = null
            }
        )
    }
    if (showCategoryManagerDialog) {
        com.example.features.habits.UniversalCategoryManagementDialog(
            categories = uiState.categories,
            onDismiss = { showCategoryManagerDialog = false },
            onAddCategory = { name, colorHex, iconKey ->
                viewModel.addCategory(name, colorHex, iconKey)
            },
            onUpdateCategory = { id, name, colorHex, iconKey ->
                viewModel.updateCategory(id, name, colorHex, iconKey)
            },
            onDeleteCategory = { catId ->
                viewModel.deleteCategory(catId)
            }
        )
    }
    }
}

@Composable
private fun TaskItemCard(
    task: TaskEntity,
    subtasks: List<TaskEntity>,
    onToggleMain: () -> Unit,
    onToggleSubtask: (String) -> Unit,
    onAddSubtask: () -> Unit,
    onEditAlarm: () -> Unit,
    onDeleteMain: () -> Unit,
    onStartTimer: () -> Unit = {}
) {
    val priorityColor = when (task.priority) {
        "HIGH" -> PriorityHighRed
        "MEDIUM" -> PriorityMediumAmber
        else -> PriorityLowGreen
    }
    val priorityLabel = when (task.priority) {
        "HIGH" -> "عالية"
        "MEDIUM" -> "متوسطة"
        else -> "عادية"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("task_item_card_${task.id}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) {
                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onToggleMain() }
                ) {
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (task.isCompleted) MaterialTheme.colorScheme.primary else priorityColor
                    )

                    Column {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${task.listCategory} • أولوية $priorityLabel",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (task.alarmEnabled && task.alarmTime24.isNotBlank()) {
                                val modeTag = if (task.alarmType == "SUPER_WAKE") "منبه ذكي" else "منبه"
                                Text(
                                    text = "$modeTag: ${task.alarmTime24}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onStartTimer,
                        modifier = Modifier.testTag("task_timer_btn_${task.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "بدء المؤقت",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onEditAlarm) {
                        Icon(
                            imageVector = if (task.alarmEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                            contentDescription = "منبه المهمة",
                            tint = if (task.alarmEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onAddSubtask) {
                        Icon(
                            imageVector = Icons.Default.SubdirectoryArrowLeft,
                            contentDescription = "مهمة فرعية"
                        )
                    }
                    IconButton(onClick = onDeleteMain) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "حذف",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            AnimatedVisibility(visible = subtasks.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    subtasks.forEach { sub ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .clickable { onToggleSubtask(sub.id) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (sub.isCompleted) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = sub.title,
                                style = MaterialTheme.typography.bodySmall,
                                textDecoration = if (sub.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddTaskModal(
    parentTask: TaskEntity?,
    categories: List<CategoryEntity> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        notes: String,
        category: String,
        priority: String,
        alarmTime24: String,
        alarmEnabled: Boolean,
        alarmType: String,
        verifyMethod: String,
        placeName: String,
        qrCode: String
    ) -> Unit
) {
    QuickAddTaskBottomSheet(
        parentTask = parentTask,
        categories = categories,
        onDismiss = onDismiss,
        onConfirm = { title, notes, category, priority, alarmTime24, alarmEnabled, alarmType, verifyMethod, placeName, qrCode, _, _ ->
            onConfirm(
                title,
                notes,
                category,
                priority,
                alarmTime24,
                alarmEnabled,
                alarmType,
                verifyMethod,
                placeName,
                qrCode
            )
        }
    )
}
