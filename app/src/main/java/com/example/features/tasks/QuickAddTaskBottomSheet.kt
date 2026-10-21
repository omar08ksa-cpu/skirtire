package com.example.features.tasks

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.example.core.protection.PlacePhotoHelper
import com.example.data.CategoryEntity
import com.example.data.TaskEntity
import com.example.features.habits.CategoryVisuals
import kotlinx.coroutines.delay

/**
 * نافذة الإضافة السريعة للمهمة المستوحاة من TickTick:
 * - شيت سفلي مرن يظهر أعلى لوحة المفاتيح
 * - إدخال سلس للعنوان والملاحظات بدون حدود مربكة
 * - شريط أدوات سريع: التاريخ، الأولوية، الفئة، القائمة، الخيارات الإضافية
 * - زر إرسال ديناميكي عند كتابة العنوان
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddTaskBottomSheet(
    parentTask: TaskEntity? = null,
    categories: List<CategoryEntity> = emptyList(),
    defaultListCategory: String = "اليوم",
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
        qrCode: String,
        categoryId: String,
        dueDateText: String
    ) -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var listCategory by remember { mutableStateOf(parentTask?.listCategory ?: defaultListCategory) }
    var priority by remember { mutableStateOf("MEDIUM") } // افتراضياً متوسطة مع علم أصفر كما في الصورة
    var selectedCategoryId by remember { mutableStateOf("") }
    var dueDateText by remember { mutableStateOf("اليوم") }

    // إعدادات المنبه والتذكير المتقدمة
    var alarmEnabled by remember { mutableStateOf(false) }
    var alarmTime24 by remember { mutableStateOf("16:00") }
    var alarmType by remember { mutableStateOf("NORMAL") }
    var verifyMethod by remember { mutableStateOf("LOCAL_IMAGE") }
    var placeName by remember { mutableStateOf("المكان المحدد") }
    var qrCode by remember { mutableStateOf("WAKE-QR-2026") }
    var hasSavedPhoto by remember { mutableStateOf(PlacePhotoHelper.hasReferencePlacePhoto(context)) }

    // قوائم وحوارات فرعية سريعة
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showPriorityMenu by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showListCategoryMenu by remember { mutableStateOf(false) }
    var showMoreOptionsDialog by remember { mutableStateOf(false) }

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

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()
    var isInputVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        keyboardController?.show()
        delay(100)
        focusRequester.requestFocus()
        delay(50)
        isInputVisible = true
    }

    val submitTask: () -> Unit = {
        if (title.isNotBlank()) {
            coroutineScope.launch {
                // مستطيل المهمة يختفي أولاً ثم الكيبورد
                isInputVisible = false
                delay(160)
                keyboardController?.hide()
                delay(80)
                onConfirm(
                    title.trim(),
                    notes.trim(),
                    listCategory,
                    priority,
                    if (alarmEnabled) alarmTime24.trim() else "",
                    alarmEnabled,
                    alarmType,
                    verifyMethod,
                    placeName.trim(),
                    qrCode.trim(),
                    selectedCategoryId,
                    dueDateText
                )
                onDismiss()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        AnimatedVisibility(
            visible = isInputVisible,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(durationMillis = 160)
            ) + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .navigationBarsPadding()
            ) {
                // مؤشر المهمة الفرعية إن وجدت
            if (parentTask != null) {
                Row(
                    modifier = Modifier
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.SubdirectoryArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "مهمة فرعية تابعة لـ: ${parentTask.title}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 1. حقل العنوان الرئيسي (ماذا تريد أن تفعل؟)
            Box(modifier = Modifier.fillMaxWidth()) {
                if (title.isEmpty()) {
                    Text(
                        text = "ماذا تريد أن تنجز؟",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        fontWeight = FontWeight.Medium
                    )
                }
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .testTag("quick_add_task_title_input"),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { /* انتقال للوصف */ })
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. حقل الملاحظات والوصف
            Box(modifier = Modifier.fillMaxWidth()) {
                if (notes.isEmpty()) {
                    Text(
                        text = "الوصف أو الملاحظات...",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    )
                }
                BasicTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_add_task_notes_input"),
                    textStyle = TextStyle(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    ),
                    maxLines = 3,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboardController?.hide()
                        submitTask()
                    })
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. شريط الأدوات السريع (مطابق تماماً لصورة TickTick)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // الأزرار من اليسار إلى اليمين
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // زر كبسولة التاريخ (بنفسجي/أرجواني مميز كما في الصورة)
                    Surface(
                        onClick = { showDatePickerDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF2E2640),
                        contentColor = Color(0xFFC4B5FD),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("quick_add_date_chip")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = "تحديد التاريخ",
                                modifier = Modifier.size(15.dp),
                                tint = Color(0xFFC4B5FD)
                            )
                            Text(
                                text = dueDateText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC4B5FD)
                            )
                        }
                    }

                    // أيقونة علم الأولوية (أصفر/برتقالي للمتوسطة، أحمر للعالية كما في الصورة)
                    Box {
                        val flagColor = when (priority) {
                            "HIGH" -> Color(0xFFEF4444)
                            "MEDIUM" -> Color(0xFFF59E0B) // أصفر مائل للبرتقالي كما في الصورة المرفقة
                            "LOW" -> Color(0xFF3B82F6)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        IconButton(
                            onClick = { showPriorityMenu = true },
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("quick_add_priority_button")
                        ) {
                            Icon(
                                if (priority == "NONE") Icons.Outlined.Flag else Icons.Default.Flag,
                                contentDescription = "الأولوية",
                                tint = flagColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showPriorityMenu,
                            onDismissRequest = { showPriorityMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("أولوية عالية", fontWeight = FontWeight.Bold) },
                                onClick = {
                                    priority = "HIGH"
                                    showPriorityMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("أولوية متوسطة", fontWeight = FontWeight.Bold) },
                                onClick = {
                                    priority = "MEDIUM"
                                    showPriorityMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("أولوية منخفضة", fontWeight = FontWeight.Bold) },
                                onClick = {
                                    priority = "LOW"
                                    showPriorityMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("بدون أولوية") },
                                onClick = {
                                    priority = "NONE"
                                    showPriorityMenu = false
                                }
                            )
                        }
                    }

                    // أيقونة الوسم / الفئة اليونيفرسل (Tag icon كما في الصورة)
                    Box {
                        val activeCategory = categories.firstOrNull { it.id == selectedCategoryId }
                        IconButton(
                            onClick = { showCategoryMenu = true },
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("quick_add_category_button")
                        ) {
                            if (activeCategory != null) {
                                val catColor = CategoryVisuals.parseColor(activeCategory.colorHex)
                                Icon(
                                    CategoryVisuals.getIcon(activeCategory.iconKey),
                                    contentDescription = "الفئة: ${activeCategory.name}",
                                    tint = catColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.LocalOffer,
                                    contentDescription = "الفئة",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showCategoryMenu,
                            onDismissRequest = { showCategoryMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("بدون فئة") },
                                onClick = {
                                    selectedCategoryId = ""
                                    showCategoryMenu = false
                                }
                            )
                            categories.forEach { cat ->
                                val catColor = CategoryVisuals.parseColor(cat.colorHex)
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            CategoryVisuals.getIcon(cat.iconKey),
                                            contentDescription = null,
                                            tint = catColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    text = { Text(cat.name, fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        selectedCategoryId = cat.id
                                        listCategory = cat.name
                                        showCategoryMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // أيقونة القائمة / المجلد
                    Box {
                        IconButton(
                            onClick = { showListCategoryMenu = true },
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("quick_add_list_category_button")
                        ) {
                            Icon(
                                Icons.Outlined.Folder,
                                contentDescription = "قائمة الوجهة: $listCategory",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showListCategoryMenu,
                            onDismissRequest = { showListCategoryMenu = false }
                        ) {
                            val availableListCategories = buildList {
                                add("اليوم")
                                categories.forEach { c ->
                                    val name = c.name.trim()
                                    if (name.isNotBlank() && !contains(name)) {
                                        add(name)
                                    }
                                }
                            }
                            availableListCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat, fontWeight = if (listCategory == cat) FontWeight.Bold else FontWeight.Normal) },
                                    leadingIcon = {
                                        if (listCategory == cat) {
                                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    },
                                    onClick = {
                                        listCategory = cat
                                        val match = categories.find { it.name.trim() == cat }
                                        if (match != null) {
                                            selectedCategoryId = match.id
                                        }
                                        showListCategoryMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // أيقونة النقاط الثلاث (المزيد: المنبه الخارق، التحقق، إلخ)
                    IconButton(
                        onClick = { showMoreOptionsDialog = true },
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("quick_add_more_options_button")
                    ) {
                        Icon(
                            Icons.Default.MoreHoriz,
                            contentDescription = "خيارات إضافية والمنبه الخارق",
                            tint = if (alarmEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // الزر في أقصى اليمين (ميكروفون عند الفراغ، وسهم إرسال بارز عند وجود نص)
                if (title.isBlank()) {
                    IconButton(
                        onClick = { /* إشعار جاهزية الصوت أو التركيز */ focusRequester.requestFocus() },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = "إملاء صوتي",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                } else {
                    FilledIconButton(
                        onClick = submitTask,
                        shape = CircleShape,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("quick_add_submit_button")
                    ) {
                        Icon(
                            Icons.Default.ArrowUpward,
                            contentDescription = "إضافة المهمة",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

    // نافذة اختيار موعد المهمة والتذكير
    if (showDatePickerDialog) {
        QuickDatePickerDialog(
            currentDueDate = dueDateText,
            currentAlarmTime = alarmTime24,
            alarmEnabled = alarmEnabled,
            onSelectDate = { selectedDate ->
                dueDateText = selectedDate
            },
            onSelectAlarm = { enabled, time ->
                alarmEnabled = enabled
                alarmTime24 = time
            },
            onDismiss = { showDatePickerDialog = false }
        )
    }

    // نافذة الخيارات المتقدمة (المنبه الخارق، التحقق بالصورة، رمز QR)
    if (showMoreOptionsDialog) {
        AdvancedTaskOptionsDialog(
            alarmEnabled = alarmEnabled,
            onAlarmEnabledChange = { alarmEnabled = it },
            alarmTime24 = alarmTime24,
            onAlarmTimeChange = { alarmTime24 = it },
            alarmType = alarmType,
            onAlarmTypeChange = { alarmType = it },
            verifyMethod = verifyMethod,
            onVerifyMethodChange = { verifyMethod = it },
            placeName = placeName,
            onPlaceNameChange = { placeName = it },
            qrCode = qrCode,
            onQrCodeChange = { qrCode = it },
            hasSavedPhoto = hasSavedPhoto,
            onCapturePhoto = {
                val hasPerm = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPerm) cameraLauncher.launch(null)
                else permLauncher.launch(Manifest.permission.CAMERA)
            },
            onDismiss = { showMoreOptionsDialog = false }
        )
    }
}

/**
 * نافذة سريعة لتحديد موعد إنجاز المهمة ووقت التنبيه:
 * - موعد المهمة: تقويم ثم ساعة
 * - منبه المهمة: ساعة بدون كتابة نص
 */
@Composable
private fun QuickDatePickerDialog(
    currentDueDate: String,
    currentAlarmTime: String,
    alarmEnabled: Boolean,
    onSelectDate: (String) -> Unit,
    onSelectAlarm: (Boolean, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var tempDate by remember { mutableStateOf(currentDueDate) }
    var tempAlarmEnabled by remember { mutableStateOf(alarmEnabled) }
    var tempTime by remember { mutableStateOf(currentAlarmTime.ifBlank { "09:00" }) }

    // فتح التقويم ثم الساعة لموعد المهمة
    val openCalendarThenClock = {
        val cal = Calendar.getInstance()
        val datePicker = android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val timePicker = android.app.TimePickerDialog(
                    context,
                    { _, hourOfDay, minute ->
                        tempDate = String.format(Locale.ENGLISH, "%04d-%02d-%02d %02d:%02d", year, month + 1, dayOfMonth, hourOfDay, minute)
                    },
                    cal.get(Calendar.HOUR_OF_DAY),
                    cal.get(Calendar.MINUTE),
                    false
                )
                timePicker.show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    // فتح الساعة لمنبه المهمة بدون كتابة
    val openClockForAlarm = {
        val parts = tempTime.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val timePicker = android.app.TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                tempTime = String.format(Locale.ENGLISH, "%02d:%02d", hourOfDay, minute)
                tempAlarmEnabled = true
            },
            h,
            m,
            false
        )
        timePicker.show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("موعد المهمة والمنبه", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("موعد المهمة (تقويم ثم ساعة):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                Surface(
                    onClick = openCalendarThenClock,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("اختر تاريخاً ثم ساعة", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Text(
                            text = tempDate,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                val quickDates = listOf("اليوم", "غداً", "نهاية الأسبوع", "بدون موعد")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickDates.forEach { opt ->
                        FilterChip(
                            selected = tempDate == opt,
                            onClick = { tempDate = opt },
                            label = { Text(opt, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("منبه المهمة (ساعة)", fontWeight = FontWeight.Bold)
                    Switch(checked = tempAlarmEnabled, onCheckedChange = { tempAlarmEnabled = it })
                }

                if (tempAlarmEnabled) {
                    Surface(
                        onClick = openClockForAlarm,
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("اضغط لضبط الساعة", fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = tempTime,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSelectDate(tempDate)
                    onSelectAlarm(tempAlarmEnabled, tempTime)
                    onDismiss()
                }
            ) {
                Text("تطبيق")
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
 * نافذة الخيارات المتقدمة والميزات الخارقة (المنبه، والتحقق، وطرق الإيقاف)
 */
@Composable
private fun AdvancedTaskOptionsDialog(
    alarmEnabled: Boolean,
    onAlarmEnabledChange: (Boolean) -> Unit,
    alarmTime24: String,
    onAlarmTimeChange: (String) -> Unit,
    alarmType: String,
    onAlarmTypeChange: (String) -> Unit,
    verifyMethod: String,
    onVerifyMethodChange: (String) -> Unit,
    placeName: String,
    onPlaceNameChange: (String) -> Unit,
    qrCode: String,
    onQrCodeChange: (String) -> Unit,
    hasSavedPhoto: Boolean,
    onCapturePhoto: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("الخيارات المتقدمة والمنبه", fontWeight = FontWeight.Bold)
            }
        },
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
                    Text("تفعيل المنبه للمهمة", fontWeight = FontWeight.Bold)
                    Switch(checked = alarmEnabled, onCheckedChange = onAlarmEnabledChange)
                }

                if (alarmEnabled) {
                    Surface(
                        onClick = {
                            val parts = alarmTime24.split(":")
                            val h = parts.getOrNull(0)?.toIntOrNull() ?: 16
                            val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                            val timePicker = android.app.TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    onAlarmTimeChange(String.format(Locale.ENGLISH, "%02d:%02d", hourOfDay, minute))
                                },
                                h,
                                m,
                                false
                            )
                            timePicker.show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("وقت رنين المنبه (الساعة)", fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = alarmTime24,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Text("نوع المنبه:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = alarmType == "NORMAL",
                            onClick = { onAlarmTypeChange("NORMAL") },
                            label = { Text("عادي") }
                        )
                        FilterChip(
                            selected = alarmType == "SUPER",
                            onClick = { onAlarmTypeChange("SUPER") },
                            label = { Text("خارق (لا يتوقف)") }
                        )
                    }

                    if (alarmType == "SUPER") {
                        Text("طريقة التحقق لإيقاف المنبه:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = verifyMethod == "LOCAL_IMAGE",
                                onClick = { onVerifyMethodChange("LOCAL_IMAGE") },
                                label = { Text("صورة المكان") }
                            )
                            FilterChip(
                                selected = verifyMethod == "QR_CODE",
                                onClick = { onVerifyMethodChange("QR_CODE") },
                                label = { Text("رمز QR") }
                            )
                        }

                        if (verifyMethod == "LOCAL_IMAGE") {
                            OutlinedTextField(
                                value = placeName,
                                onValueChange = onPlaceNameChange,
                                label = { Text("اسم المكان (مثال: المغسلة)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = onCapturePhoto,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (hasSavedPhoto) "تحديث صورة المكان المرجعية" else "التقاط صورة المكان المرجعية")
                            }
                        } else {
                            OutlinedTextField(
                                value = qrCode,
                                onValueChange = onQrCodeChange,
                                label = { Text("كود الـ QR لإيقاف المنبه") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("حفظ")
            }
        }
    )
}
