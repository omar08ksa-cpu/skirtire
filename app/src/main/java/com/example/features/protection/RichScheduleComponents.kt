package com.example.features.protection

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.prayer.UmmAlQuraCalculator
import com.example.core.protection.AppUsageMonitor
import com.example.core.protection.AppUsageStatItem
import com.example.data.BlockScheduleEntity
import com.example.data.BlockedAppEntity
import com.example.data.HabitEntity
import com.example.features.DailyManagerUiState
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.PriorityHighRed

/**
 * بطاقة عرض جدول الحظر المتقدم مع شارة نوع الحظر وقفل الحظر الخارق
 */
@Composable
fun RichScheduleCard(
    schedule: BlockScheduleEntity,
    allBlockedApps: List<BlockedAppEntity>,
    uiState: DailyManagerUiState,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showSuperProtectionBlockAlert by remember { mutableStateOf(false) }

    fun checkAndPerform(action: () -> Unit) {
        if (uiState.superProtectionEnabled && !uiState.isSuperProtectionWindowOpenNow && uiState.superProtectionPreventModifyingSchedules) {
            showSuperProtectionBlockAlert = true
        } else {
            action()
        }
    }

    val typeColor = when (schedule.scheduleType) {
        "GROUP_USAGE_LIMIT" -> Color(0xFF0284C7) // Sky blue
        "CUSTOM_BLOCK" -> MaterialTheme.colorScheme.primary
        "PERMANENT_BLOCK" -> PriorityHighRed
        "APP_TIMER_COOLDOWN" -> Color(0xFFD97706) // Amber
        "FOCUS_COUNTER" -> Color(0xFF7C3AED) // Purple
        "HABIT" -> Color(0xFF059669) // Emerald
        else -> MaterialTheme.colorScheme.secondary
    }

    val typeTitle = when (schedule.scheduleType) {
        "GROUP_USAGE_LIMIT" -> "حد استخدام جماعي"
        "CUSTOM_BLOCK" -> "إغلاق بجدولة زمنية"
        "PERMANENT_BLOCK" -> "إغلاق دائم"
        "APP_TIMER_COOLDOWN" -> "مؤقت مع فترة إغلاق"
        "FOCUS_COUNTER" -> "تركيز العداد"
        "HABIT" -> "عادة"
        else -> "جدول حظر"
    }

    val packagesList = remember(schedule.blockedPackagesCsv) {
        schedule.blockedPackagesCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = typeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = typeTitle,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = typeColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    if (schedule.targetCategory.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "فئة: ${schedule.targetCategory}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = schedule.isEnabled,
                        onCheckedChange = { checkAndPerform { onToggle() } },
                        modifier = Modifier.testTag("toggle_schedule_${schedule.id}")
                    )
                    IconButton(onClick = { checkAndPerform { onEdit() } }) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "تعديل الجدول",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { checkAndPerform { onDelete() } }) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "حذف الجدول",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Text(
                text = schedule.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )

            // تفاصيل نوع الجدول
            when (schedule.scheduleType) {
                "GROUP_USAGE_LIMIT" -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = typeColor, modifier = Modifier.size(16.dp))
                        Text(
                            text = UmmAlQuraCalculator.toArabicDigits("الحد المشترك: ${schedule.groupLimitMinutes} دقيقة يومياً"),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                "CUSTOM_BLOCK" -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = typeColor, modifier = Modifier.size(16.dp))
                        Text(
                            text = UmmAlQuraCalculator.toArabicDigits("من ${schedule.startTime24} إلى ${schedule.endTime24}"),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                "PERMANENT_BLOCK" -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = PriorityHighRed, modifier = Modifier.size(16.dp))
                        Text(
                            text = "التطبيقات مغلقة تماماً ولا يمكن فتحها",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = PriorityHighRed
                        )
                    }
                }
                "APP_TIMER_COOLDOWN" -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = typeColor, modifier = Modifier.size(16.dp))
                        Text(
                            text = UmmAlQuraCalculator.toArabicDigits("استخدام: ${schedule.sessionDurationMinutes} د • فترة الإغلاق: ${schedule.sessionCooldownMinutes} د"),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                "FOCUS_COUNTER" -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.PlayCircle, contentDescription = null, tint = typeColor, modifier = Modifier.size(16.dp))
                        Text(
                            text = "تغلق التطبيقات تلقائياً عند بدء عداد التركيز أو الدراسة",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // التطبيقات المشمولة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (schedule.targetCategory.isNotBlank()) {
                        "يشمل جميع تطبيقات «${schedule.targetCategory}»"
                    } else if (packagesList.isNotEmpty()) {
                        UmmAlQuraCalculator.toArabicDigits("يشمل ${packagesList.size} تطبيق محدد")
                    } else {
                        "يشمل كافة التطبيقات المقيدة"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (schedule.daysOfWeek.isNotBlank() && schedule.scheduleType == "CUSTOM_BLOCK") {
                    val daysCount = schedule.daysOfWeek.split(",").filter { it.isNotBlank() }.size
                    Text(
                        text = if (daysCount >= 7) "كل الأيام" else UmmAlQuraCalculator.toArabicDigits("$daysCount أيام بالأسبوع"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showSuperProtectionBlockAlert) {
        AlertDialog(
            onDismissRequest = { showSuperProtectionBlockAlert = false },
            icon = { Icon(Icons.Default.Shield, contentDescription = null, tint = PriorityHighRed) },
            title = { Text("الحظر الخارق مفعل", fontWeight = FontWeight.Bold) },
            text = {
                Text("لا يمكن تعطيل أو تعديل أو حذف جداول الحظر خارج نافذة التعديل المحددة (${uiState.nextSuperProtectionWindowLabel}).\nيمكنك إضافة جداول جديدة بحرية.")
            },
            confirmButton = {
                Button(
                    onClick = { showSuperProtectionBlockAlert = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PriorityHighRed)
                ) {
                    Text("حسناً")
                }
            }
        )
    }
}

/**
 * نافذة إضافة جدول حظر متطور وشامل
 * تفي بجميع متطلبات المستخدم بدقة:
 * 1- اختيار نوع الحظر من بين 5 أنواع:
 *    - مدة استخدام معينة (مجموع استخدام التطبيقات تقفل كلها)
 *    - إغلاق التطبيقات بجدولة معينة (من إلى وقت محدد مع اختيار الأيام)
 *    - إغلاق دائم ما عاد تفتح معك
 *    - تحظر بعد مدة معينة (تسألك كم تبغى تستخدم + فترة إغلاق)
 *    - تركيز العداد (تقفل أثناء العداد وترجع عند انتهائه)
 * 2- اختيار التطبيقات المحظورة:
 *    - تطبيقات معينة مرتبة من الأكثر استعمالاً للأقل ويسار اسمها مدة الاستعمال اليوم
 *    - اختيار فئة (تواصل - ترفيهي - إنتاجية - وغيرها)
 *    - اختيار نفس جدول آخر (نسخ أسماء الحزم فقط بدون ربط علاقة مباشرة)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRichBlockScheduleModal(
    existingSchedule: BlockScheduleEntity? = null,
    habits: List<HabitEntity> = emptyList(),
    blockedApps: List<BlockedAppEntity>,
    existingSchedules: List<BlockScheduleEntity>,
    usageOverview: com.example.core.protection.DeviceUsageOverview,
    onDismiss: () -> Unit,
    onConfirm: (
        id: String?,
        title: String,
        scheduleType: String,
        startTime24: String,
        endTime24: String,
        daysOfWeek: String,
        blockedPackagesCsv: String,
        targetCategory: String,
        groupLimitMinutes: Int,
        sessionDurationMinutes: Int,
        sessionCooldownMinutes: Int,
        qrReferenceCode: String
    ) -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(1) } // 1: نوع الحظر والإعدادات، 2: اختيار التطبيقات

    // بيانات الجدول
    var title by remember { mutableStateOf(existingSchedule?.title.orEmpty()) }
    var selectedScheduleType by remember { mutableStateOf(existingSchedule?.scheduleType ?: "GROUP_USAGE_LIMIT") }
    var startTime by remember { mutableStateOf(existingSchedule?.startTime24 ?: "16:00") }
    var endTime by remember { mutableStateOf(existingSchedule?.endTime24 ?: "20:00") }
    var selectedDays by remember {
        val initialDays = existingSchedule?.daysOfWeek?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.toSet()
        mutableStateOf(if (initialDays.isNullOrEmpty()) setOf(1, 2, 3, 4, 5, 6, 7) else initialDays)
    }
    var groupLimitMinutesText by remember { mutableStateOf(existingSchedule?.groupLimitMinutes?.takeIf { it > 0 }?.toString() ?: "60") }
    var sessionDurationMinutesText by remember { mutableStateOf(existingSchedule?.sessionDurationMinutes?.takeIf { it > 0 }?.toString() ?: "15") }
    var sessionCooldownMinutesText by remember { mutableStateOf(existingSchedule?.sessionCooldownMinutes?.takeIf { it > 0 }?.toString() ?: "30") }
    var selectedHabitId by remember {
        mutableStateOf(existingSchedule?.qrReferenceCode.orEmpty().ifEmpty { habits.firstOrNull()?.id.orEmpty() })
    }

    // نمط اختيار التطبيقات: 0 = تطبيقات معينة، 1 = فئة كاملة، 2 = نسخ من جدول آخر
    var appSelectionMode by remember {
        mutableIntStateOf(if (!existingSchedule?.targetCategory.isNullOrBlank() && existingSchedule?.scheduleType != "HABIT") 1 else 0)
    }
    var selectedCategory by remember { mutableStateOf(existingSchedule?.targetCategory.orEmpty().ifEmpty { "SOCIAL" }) }
    var selectedSourceScheduleId by remember { mutableStateOf("") }
    val selectedPackages = remember {
        mutableStateListOf<String>().apply {
            existingSchedule?.blockedPackagesCsv?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }?.let { addAll(it) }
        }
    }

    // قائمة جميع التطبيقات المثبتة مرتبة من الأكثر استعمالاً اليوم للأقل
    val candidateApps = remember(usageOverview.topUsedApps, blockedApps) {
        val pm = context.packageManager
        val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null && it.packageName != context.packageName }

        val appsUsageMap = usageOverview.usageByPackageMillis
        val formattedUsageMap = usageOverview.topUsedApps.associate { it.packageName to it.formattedDuration }

        installed.map { appInfo ->
            val pkg = appInfo.packageName
            val label = pm.getApplicationLabel(appInfo).toString()
            val usedMillis = appsUsageMap[pkg] ?: 0L
            val formatted = formattedUsageMap[pkg] ?: AppUsageMonitor.formatDurationArabic(usedMillis)
            InstalledAppWithUsage(
                appName = label,
                packageName = pkg,
                usedMillisToday = usedMillis,
                formattedDuration = formatted
            )
        }.sortedByDescending { it.usedMillisToday }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = if (existingSchedule != null) {
                        if (currentStep == 1) "تعديل جدول الحظر (الخطوة 1/2)" else "تحديد التطبيقات المحظورة (الخطوة 2/2)"
                    } else {
                        if (currentStep == 1) "إضافة جدول حظر جديد (الخطوة 1/2)" else "تحديد التطبيقات المحظورة (الخطوة 2/2)"
                    },
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (currentStep == 1) "اختر نوع الحظر وحدد مدته أو أوقاته" else "اختر التطبيقات أو الفئة أو انسخ من جدول آخر",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (currentStep == 1) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("اسم الجدول (مثال: حظر السوشيال ميديا)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("schedule_title_input")
                    )

                    Text(
                        text = "1. نوع الحظر:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold
                    )

                    val typesList = listOf(
                        "GROUP_USAGE_LIMIT" to "مدة استخدام معينة (مجموع الاستخدام)",
                        "CUSTOM_BLOCK" to "إغلاق بجدولة زمنية (من - إلى وقت)",
                        "PERMANENT_BLOCK" to "إغلاق دائم (لا تفتح معك)",
                        "APP_TIMER_COOLDOWN" to "حظر بعد مدة (نافذة استخدام + فترة إغلاق)",
                        "FOCUS_COUNTER" to "تركيز العداد (قفل أثناء تشغيل العداد)",
                        "HABIT" to "عادة (قفل حتى إنجاز العادة)"
                    )

                    typesList.forEach { (typeKey, typeLabel) ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedScheduleType == typeKey) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedScheduleType = typeKey }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                RadioButton(
                                    selected = selectedScheduleType == typeKey,
                                    onClick = { selectedScheduleType = typeKey }
                                )
                                Column {
                                    Text(
                                        text = typeLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = when (typeKey) {
                                            "GROUP_USAGE_LIMIT" -> "إذا صار مجموع استخدام التطبيقات المضافة أكثر من الحد تقفل كلها"
                                            "CUSTOM_BLOCK" -> "قفل يومي من ساعة محددة إلى ساعة محددة في الأيام المختارة"
                                            "PERMANENT_BLOCK" -> "تطبيقات مغلقة تماماً بلا عودة للمساعدة على الإقلاع"
                                            "APP_TIMER_COOLDOWN" -> "نافذة منبثقة تسألك كم تبغى تستخدم ثم يغلق وتبدأ فترة الانتظار"
                                            "FOCUS_COUNTER" -> "تقفل كل التطبيقات أثناء تشغيل عداد الدراسة والتركيز وتفتح عند انتهائه"
                                            "HABIT" -> "تقفل التطبيقات ولا ينفك الحظر إلا بعد إنجاز العادة المحددة لليوم"
                                            else -> ""
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // مدخلات خاصة بكل نوع حظر
                    when (selectedScheduleType) {
                        "HABIT" -> {
                            Text("اختر العادة المرتبطة بهذا الحظر:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            if (habits.isEmpty()) {
                                Text("لا توجد عادات مسجلة حالياً. يرجى إضافة عادة أولاً من قسم العادات.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    habits.forEach { habit ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (selectedHabitId == habit.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedHabitId = habit.id
                                                    if (title.isBlank() || title.startsWith("حظر عادة:")) {
                                                        title = "حظر عادة: ${habit.title}"
                                                    }
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                RadioButton(
                                                    selected = selectedHabitId == habit.id,
                                                    onClick = {
                                                        selectedHabitId = habit.id
                                                        if (title.isBlank() || title.startsWith("حظر عادة:")) {
                                                            title = "حظر عادة: ${habit.title}"
                                                        }
                                                    }
                                                )
                                                Text(habit.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        "GROUP_USAGE_LIMIT" -> {
                            OutlinedTextField(
                                value = groupLimitMinutesText,
                                onValueChange = { groupLimitMinutesText = it.filter { ch -> ch.isDigit() } },
                                label = { Text("الحد المشترك لمجموع التطبيقات (بالدقائق)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        "CUSTOM_BLOCK" -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = startTime,
                                    onValueChange = { startTime = it },
                                    label = { Text("من الساعة (24س)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = endTime,
                                    onValueChange = { endTime = it },
                                    label = { Text("إلى الساعة (24س)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Text("أيام التفعيل:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            val dayNames = listOf(
                                1 to "السبت", 2 to "الأحد", 3 to "الاثنين", 4 to "الثلاثاء",
                                5 to "الأربعاء", 6 to "الخميس", 7 to "الجمعة"
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                dayNames.forEach { (dNum, dName) ->
                                    FilterChip(
                                        selected = dNum in selectedDays,
                                        onClick = {
                                            selectedDays = if (dNum in selectedDays) selectedDays - dNum else selectedDays + dNum
                                        },
                                        label = { Text(dName, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                        "APP_TIMER_COOLDOWN" -> {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = sessionDurationMinutesText,
                                    onValueChange = { sessionDurationMinutesText = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("مدة الجلسة (دقائق)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = sessionCooldownMinutesText,
                                    onValueChange = { sessionCooldownMinutesText = it.filter { ch -> ch.isDigit() } },
                                    label = { Text("فترة الإغلاق (دقائق)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                } else {
                    // الخطوة 2: اختيار التطبيقات
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = appSelectionMode == 0,
                            onClick = { appSelectionMode = 0 },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                        ) {
                            Text("تطبيقات", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        SegmentedButton(
                            selected = appSelectionMode == 1,
                            onClick = { appSelectionMode = 1 },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                        ) {
                            Text("فئة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        SegmentedButton(
                            selected = appSelectionMode == 2,
                            onClick = { appSelectionMode = 2 },
                            shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                        ) {
                            Text("نسخ جدول", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    when (appSelectionMode) {
                        0 -> {
                            // تطبيقات معينة مرتبة من الأكثر استعمالاً للأقل ويسار اسمها مدة الاستعمال
                            Text(
                                text = "اختر التطبيقات (مرتبة بالأكثر استخداماً اليوم):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            candidateApps.take(35).forEach { app ->
                                val isSelected = app.packageName in selectedPackages
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isSelected) selectedPackages.remove(app.packageName)
                                            else selectedPackages.add(app.packageName)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
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
                                                onCheckedChange = {
                                                    if (isSelected) selectedPackages.remove(app.packageName)
                                                    else selectedPackages.add(app.packageName)
                                                }
                                            )
                                            Text(
                                                text = app.appName,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        // يسار اسمها مدة استعمال التطبيق
                                        Text(
                                            text = UmmAlQuraCalculator.toArabicDigits(app.formattedDuration),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (app.usedMillisToday > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        1 -> {
                            // اختيار فئة
                            Text(
                                text = "اختر الفئة لشمل جميع تطبيقاتها تلقائياً:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            val catList = listOf(
                                "SOCIAL" to "شبكات التواصل الاجتماعي",
                                "ENTERTAINMENT" to "ترفيه وألعاب وفيديو",
                                "PRODUCTIVITY" to "إنتاجية وتعليم",
                                "TOOLS" to "أدوات ونظام",
                                "OTHER" to "أخرى"
                            )
                            catList.forEach { (catKey, catLabel) ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selectedCategory == catKey) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedCategory = catKey }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        RadioButton(
                                            selected = selectedCategory == catKey,
                                            onClick = { selectedCategory = catKey }
                                        )
                                        Text(catLabel, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                        2 -> {
                            // اختيار نفس جدول آخر (أخذ أسماء التطبيقات فقط بدون ربط علاقة مباشرة)
                            Text(
                                text = "اختر جدولاً لنسخ قائمة تطبيقاته (مستقل تماماً):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (existingSchedules.isEmpty()) {
                                Text(
                                    text = "لا توجد جداول أخرى سابقة لنسخ التطبيقات منها.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                existingSchedules.forEach { sched ->
                                    val count = sched.blockedPackagesCsv.split(",").filter { it.isNotBlank() }.size
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (selectedSourceScheduleId == sched.id) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedSourceScheduleId = sched.id
                                                // نسخ التطبيقات بدون ربط علاقة
                                                val pkgs = sched.blockedPackagesCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                                selectedPackages.clear()
                                                selectedPackages.addAll(pkgs)
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(sched.title, fontWeight = FontWeight.Bold)
                                                Text(
                                                    text = UmmAlQuraCalculator.toArabicDigits("يحتوي على $count تطبيق"),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (selectedSourceScheduleId == sched.id) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (currentStep == 1) {
                Button(
                    onClick = {
                        if (title.isBlank()) {
                            title = when (selectedScheduleType) {
                                "GROUP_USAGE_LIMIT" -> "حد استخدام جماعي"
                                "CUSTOM_BLOCK" -> "جدول حظر مسائي"
                                "PERMANENT_BLOCK" -> "إغلاق دائم"
                                "APP_TIMER_COOLDOWN" -> "جلسات مؤقتة"
                                "FOCUS_COUNTER" -> "تركيز العداد"
                                "HABIT" -> "حظر حتى إنجاز العادة"
                                else -> "جدول حظر"
                            }
                        }
                        currentStep = 2
                    },
                    modifier = Modifier.testTag("next_step_schedule_btn")
                ) {
                    Text("التالي: اختيار التطبيقات")
                }
            } else {
                Button(
                    onClick = {
                        val finalPackagesCsv = if (appSelectionMode == 1 && selectedScheduleType != "HABIT") "" else selectedPackages.distinct().joinToString(",")
                        val finalCategory = if (selectedScheduleType == "HABIT") {
                            habits.find { it.id == selectedHabitId }?.title.orEmpty()
                        } else if (appSelectionMode == 1) selectedCategory else ""
                        val finalQrRef = if (selectedScheduleType == "HABIT") selectedHabitId else existingSchedule?.qrReferenceCode.orEmpty()

                        onConfirm(
                            existingSchedule?.id,
                            title.trim(),
                            selectedScheduleType,
                            startTime.trim(),
                            endTime.trim(),
                            selectedDays.sorted().joinToString(","),
                            finalPackagesCsv,
                            finalCategory,
                            groupLimitMinutesText.toIntOrNull() ?: 60,
                            sessionDurationMinutesText.toIntOrNull() ?: 15,
                            sessionCooldownMinutesText.toIntOrNull() ?: 30,
                            finalQrRef
                        )
                    },
                    modifier = Modifier.testTag("save_rich_schedule_btn")
                ) {
                    Text(if (existingSchedule != null) "حفظ التعديلات" else "حفظ الجدول")
                }
            }
        },
        dismissButton = {
            if (currentStep == 2) {
                TextButton(onClick = { currentStep = 1 }) {
                    Text("السابق")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("إلغاء")
                }
            }
        }
    )
}

data class InstalledAppWithUsage(
    val appName: String,
    val packageName: String,
    val usedMillisToday: Long,
    val formattedDuration: String
)
