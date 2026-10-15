package com.example.features.protection

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.example.core.protection.DayUsageItem
import com.example.core.protection.WeeklyUsageData
import com.example.data.BlockScheduleEntity
import com.example.data.BlockedAppEntity
import com.example.features.DailyManagerUiState
import com.example.features.DailyManagerViewModel
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.PriorityHighRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    appStat: AppUsageStatItem,
    blockedAppEntity: BlockedAppEntity?,
    uiState: DailyManagerUiState,
    viewModel: DailyManagerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    // إعدادات التطبيق الحالية من الكيان أو قيم افتراضية
    val currentEntity = blockedAppEntity ?: remember(appStat.packageName) {
        BlockedAppEntity(
            packageName = appStat.packageName,
            appName = appStat.appName,
            category = "OTHER",
            usageImpact = "NEUTRAL"
        )
    }

    var selectedCategory by remember(currentEntity.category) {
        mutableStateOf(currentEntity.category.ifBlank { "OTHER" })
    }
    var selectedImpact by remember(currentEntity.usageImpact) {
        mutableStateOf(currentEntity.usageImpact.ifBlank { "NEUTRAL" })
    }
    var dailyLimitMinutesText by remember(currentEntity.dailyLimitMinutes) {
        mutableStateOf(if (currentEntity.dailyLimitMinutes > 0) currentEntity.dailyLimitMinutes.toString() else "")
    }
    var launchLimitText by remember(currentEntity.dailyLaunchLimit) {
        mutableStateOf(if (currentEntity.dailyLaunchLimit > 0) currentEntity.dailyLaunchLimit.toString() else "")
    }
    var specificBlockEnabled by remember(currentEntity.specificBlockEnabled) {
        mutableStateOf(currentEntity.specificBlockEnabled)
    }
    var specificStartTime by remember(currentEntity.specificBlockStartTime24) {
        mutableStateOf(currentEntity.specificBlockStartTime24.ifBlank { "23:00" })
    }
    var specificEndTime by remember(currentEntity.specificBlockEndTime24) {
        mutableStateOf(currentEntity.specificBlockEndTime24.ifBlank { "07:00" })
    }
    var specificDaysSet by remember(currentEntity.specificBlockDays) {
        val days = currentEntity.specificBlockDays.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
        mutableStateOf(if (days.isEmpty()) setOf(1, 2, 3, 4, 5, 6, 7) else days)
    }

    // تنقل الأسابيع في الرسم البياني
    var weekOffset by remember { mutableIntStateOf(0) }
    var weeklyData by remember { mutableStateOf<WeeklyUsageData?>(null) }
    var isLoadingWeekly by remember { mutableStateOf(true) }

    LaunchedEffect(appStat.packageName, weekOffset) {
        isLoadingWeekly = true
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val res = AppUsageMonitor.queryWeeklyUsage(context, appStat.packageName, weekOffset)
            weeklyData = res
            isLoadingWeekly = false
        }
    }

    // جداول الحظر التي يشارك فيها هذا التطبيق
    val participatingSchedules = remember(uiState.blockSchedules, appStat.packageName) {
        uiState.blockSchedules.filter { sched ->
            sched.blockedPackagesCsv.split(",").map { it.trim() }.contains(appStat.packageName) ||
                (sched.targetCategory.isNotBlank() && sched.targetCategory == selectedCategory)
        }
    }

    // حوار تنبيه الحظر الخارق عند محاولة كسر القواعد
    var showSuperProtectionBlockNotice by remember { mutableStateOf<String?>(null) }

    fun checkSuperProtectionViolation(actionType: String, newValue: Int, oldValue: Int): Boolean {
        if (!uiState.superProtectionEnabled || uiState.isSuperProtectionWindowOpenNow) {
            return false // الميزة غير مفعلة أو النافذة مفتوحة
        }
        return when (actionType) {
            "USAGE_LIMIT_INCREASE" -> {
                // ممنوع زيادة الحد (إذا كان صفر يعني كان غير مقيد، أو القيمة الجديدة أكبر من القديمة)
                if (oldValue > 0 && newValue > oldValue) true
                else false
            }
            "LAUNCH_LIMIT_INCREASE" -> {
                if (oldValue > 0 && newValue > oldValue) true
                else false
            }
            "CATEGORY_CHANGE" -> uiState.superProtectionPreventChangingCategory
            "IMPACT_CHANGE" -> uiState.superProtectionPreventChangingCategory
            else -> false
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = appStat.appName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = appStat.packageName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = {
                            val newLimit = dailyLimitMinutesText.toIntOrNull() ?: 0
                            val oldLimit = currentEntity.dailyLimitMinutes
                            if (checkSuperProtectionViolation("USAGE_LIMIT_INCREASE", newLimit, oldLimit)) {
                                showSuperProtectionBlockNotice = "الحظر الخارق مفعل! يمنع رفع مدة استخدام التطبيق خارج نافذة التعديل (${uiState.nextSuperProtectionWindowLabel})."
                                return@FilledTonalButton
                            }

                            val newLaunch = launchLimitText.toIntOrNull() ?: 0
                            val oldLaunch = currentEntity.dailyLaunchLimit
                            if (checkSuperProtectionViolation("LAUNCH_LIMIT_INCREASE", newLaunch, oldLaunch)) {
                                showSuperProtectionBlockNotice = "الحظر الخارق مفعل! يمنع زيادة عدد مرات فتح التطبيق خارج نافذة التعديل."
                                return@FilledTonalButton
                            }

                            viewModel.updateAppCustomDetails(
                                packageName = appStat.packageName,
                                appName = appStat.appName,
                                category = selectedCategory,
                                usageImpact = selectedImpact,
                                dailyLimitMinutes = newLimit,
                                dailyLaunchLimit = newLaunch,
                                specificStartTime24 = specificStartTime,
                                specificEndTime24 = specificEndTime,
                                specificDays = specificDaysSet.sorted().joinToString(","),
                                specificEnabled = specificBlockEnabled
                            )
                            Toast.makeText(context, "تم حفظ إعدادات ${appStat.appName} بنجاح ✓", Toast.LENGTH_SHORT).show()
                            onBack()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 8.dp).testTag("save_app_settings_btn")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حفظ التغييرات", fontWeight = FontWeight.Bold)
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 10.dp, bottom = 48.dp)
        ) {
            // تنبيه الحظر الخارق إن وجد
            if (uiState.superProtectionEnabled) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (uiState.isSuperProtectionWindowOpenNow) EmeraldDeep.copy(alpha = 0.12f) else PriorityHighRed.copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (uiState.isSuperProtectionWindowOpenNow) EmeraldDeep.copy(alpha = 0.4f) else PriorityHighRed.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.isSuperProtectionWindowOpenNow) Icons.Default.LockOpen else Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (uiState.isSuperProtectionWindowOpenNow) EmeraldDeep else PriorityHighRed
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (uiState.isSuperProtectionWindowOpenNow) "نافذة التعديل مفتوحة حالياً 🟢" else "الحظر الخارق نشط ومقفل 🔒",
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.isSuperProtectionWindowOpenNow) EmeraldDeep else PriorityHighRed,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = if (uiState.isSuperProtectionWindowOpenNow)
                                        "يمكنك تعديل الحدود أو تقليلها الآن قبل انتهاء النافذة."
                                    else
                                        "الحدود لا يمكن رفعها أو تعطيلها حتى حلول نافذة التعديل (${uiState.nextSuperProtectionWindowLabel}).",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 1. بطاقة ملخص مدة الاستعمال اليوم ومرات الفتح
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = appStat.appName.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column {
                                    Text(
                                        text = "استخدام التطبيق اليوم",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = UmmAlQuraCalculator.toArabicDigits(appStat.formattedDuration),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            // شارة التأثير الحالية
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = when (selectedImpact) {
                                    "POSITIVE" -> EmeraldDeep.copy(alpha = 0.15f)
                                    "NEGATIVE" -> PriorityHighRed.copy(alpha = 0.15f)
                                    "IGNORED" -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Text(
                                    text = when (selectedImpact) {
                                        "POSITIVE" -> "إيجابي 🟢"
                                        "NEGATIVE" -> "سلبي 🔴"
                                        "IGNORED" -> "غير مشمول 🚫"
                                        else -> "رمادي ⚪"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    color = when (selectedImpact) {
                                        "POSITIVE" -> EmeraldDeep
                                        "NEGATIVE" -> PriorityHighRed
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                        // عدد مرات الفتح اليوم
                        val launchCount = appStat.launchCountToday
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
                                    imageVector = Icons.Default.Launch,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "مرات فتح التطبيق اليوم:",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = UmmAlQuraCalculator.toArabicDigits(
                                    if (launchCount > 0) "$launchCount مرة" else "لم يُفتح اليوم بعد"
                                ),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }

            // 2. الرسم البياني للاستعمال في الأسبوع مع التنقل بالأسابيع
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // شريط رأس الرسم البياني والتنقل بين الأسابيع
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "الاستخدام الأسبوعي",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = weeklyData?.weekRangeLabel ?: "جاري التحميل...",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { weekOffset -= 1 },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "الأسبوع السابق",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                if (weekOffset < 0) {
                                    IconButton(
                                        onClick = { weekOffset += 1 },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "الأسبوع التالي",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (isLoadingWeekly) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp))
                            }
                        } else {
                            val data = weeklyData
                            if (data != null && data.days.isNotEmpty()) {
                                WeeklyBarChart(
                                    days = data.days,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "المجموع: ${UmmAlQuraCalculator.toArabicDigits(AppUsageMonitor.formatDurationArabic(data.totalWeekMinutes * 60_000L))}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "المعدل اليومي: ${UmmAlQuraCalculator.toArabicDigits(AppUsageMonitor.formatDurationArabic(data.averageDailyMinutes * 60_000L))}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. حد استخدام التطبيق + حد مرات الفتح
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "الحدود اليومية (الاستخدام ومرات الفتح)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )

                        // حد وقت الاستخدام
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "حد وقت الاستخدام اليومي (بالدقائق):",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = dailyLimitMinutesText,
                                    onValueChange = { input ->
                                        dailyLimitMinutesText = input.filter { it.isDigit() }
                                    },
                                    placeholder = { Text("بدون حد (0)") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).testTag("app_daily_limit_input")
                                )
                                Text("دقيقة", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            }

                            // أزرار سريعة
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(15, 30, 45, 60, 90, 120).forEach { mins ->
                                    FilterChip(
                                        selected = dailyLimitMinutesText == mins.toString(),
                                        onClick = { dailyLimitMinutesText = mins.toString() },
                                        label = { Text("$mins د") }
                                    )
                                }
                                FilterChip(
                                    selected = dailyLimitMinutesText.isBlank() || dailyLimitMinutesText == "0",
                                    onClick = { dailyLimitMinutesText = "" },
                                    label = { Text("إلغاء الحد") }
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // حد مرات الفتح
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "أقصى عدد لمرات فتح التطبيق يومياً:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = launchLimitText,
                                    onValueChange = { input ->
                                        launchLimitText = input.filter { it.isDigit() }
                                    },
                                    placeholder = { Text("بدون حد (0)") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).testTag("app_launch_limit_input")
                                )
                                Text("مرة", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            }

                            // أزرار سريعة لمرات الفتح
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(3, 5, 10, 15, 20).forEach { cnt ->
                                    FilterChip(
                                        selected = launchLimitText == cnt.toString(),
                                        onClick = { launchLimitText = cnt.toString() },
                                        label = { Text("$cnt مرات") }
                                    )
                                }
                                FilterChip(
                                    selected = launchLimitText.isBlank() || launchLimitText == "0",
                                    onClick = { launchLimitText = "" },
                                    label = { Text("بدون حد") }
                                )
                            }
                        }
                    }
                }
            }

            // 4. جدول حظر خاص بهذا التطبيق
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                            Column {
                                Text(
                                    text = "جدول حظر خاص بالتطبيق ⏱️",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "يقفل التطبيق تلقائياً خلال هذه الساعات والأيام",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = specificBlockEnabled,
                                onCheckedChange = { specificBlockEnabled = it }
                            )
                        }

                        if (specificBlockEnabled) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = specificStartTime,
                                    onValueChange = { specificStartTime = it },
                                    label = { Text("من الساعة (24س)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = specificEndTime,
                                    onValueChange = { specificEndTime = it },
                                    label = { Text("إلى الساعة (24س)") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // أيام الأسبوع
                            Text(
                                text = "أيام تطبيق الحظر:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            val dayTitles = listOf(
                                1 to "سبت",
                                2 to "أحد",
                                3 to "اثنين",
                                4 to "ثلاثاء",
                                5 to "أربعاء",
                                6 to "خميس",
                                7 to "جمعة"
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                dayTitles.forEach { (d, label) ->
                                    val isSelected = d in specificDaysSet
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            specificDaysSet = if (isSelected) {
                                                if (specificDaysSet.size > 1) specificDaysSet - d else specificDaysSet
                                            } else {
                                                specificDaysSet + d
                                            }
                                        },
                                        label = { Text(label, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. الجداول التي التطبيق داخل فيها
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "الجداول المشمول فيها التطبيق (${participatingSchedules.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )

                        if (participatingSchedules.isEmpty()) {
                            Text(
                                text = "هذا التطبيق غير مدرج في أي جدول حظر عام حتى الآن.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            participatingSchedules.forEach { sched ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = sched.title,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            val typeLabel = when (sched.scheduleType) {
                                                "GROUP_USAGE_LIMIT" -> "حد جماعي: ${sched.groupLimitMinutes} دقيقة"
                                                "PERMANENT" -> "حظر دائم"
                                                "SESSION_LIMIT" -> "جلسة ${sched.sessionDurationMinutes} د / إغلاق ${sched.sessionCooldownMinutes} د"
                                                "FOCUS_TIMER" -> "تركيز العداد"
                                                else -> "من ${sched.startTime24} إلى ${sched.endTime24}"
                                            }
                                            Text(
                                                text = typeLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (sched.isEnabled) EmeraldDeep.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = if (sched.isEnabled) "مفعل ✓" else "معطل",
                                                color = if (sched.isEnabled) EmeraldDeep else MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.labelSmall,
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

            // 6. تصنيف تأثير الاستخدام (إيجابي - سلبي - رمادي - عدم شمل)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "تصنيف تأثير الاستخدام على الإنتاجية",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "يساعدك على احتساب إجمالي وقت الاستخدام السلبي والإيجابي بدقة:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = selectedImpact == "NEGATIVE",
                                onClick = {
                                    if (checkSuperProtectionViolation("IMPACT_CHANGE", 0, 0)) {
                                        showSuperProtectionBlockNotice = "الحظر الخارق مفعل! يمنع تغيير تصنيف التطبيق خارج نافذة التعديل."
                                    } else {
                                        selectedImpact = "NEGATIVE"
                                    }
                                },
                                label = { Text("تطبيق سلبي 🔴", fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PriorityHighRed.copy(alpha = 0.2f),
                                    selectedLabelColor = PriorityHighRed
                                )
                            )

                            FilterChip(
                                selected = selectedImpact == "POSITIVE",
                                onClick = {
                                    if (checkSuperProtectionViolation("IMPACT_CHANGE", 0, 0)) {
                                        showSuperProtectionBlockNotice = "الحظر الخارق مفعل! يمنع تغيير تصنيف التطبيق خارج نافذة التعديل."
                                    } else {
                                        selectedImpact = "POSITIVE"
                                    }
                                },
                                label = { Text("تطبيق إيجابي 🟢", fontWeight = FontWeight.Bold) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldDeep.copy(alpha = 0.2f),
                                    selectedLabelColor = EmeraldDeep
                                )
                            )

                            FilterChip(
                                selected = selectedImpact == "NEUTRAL",
                                onClick = {
                                    if (checkSuperProtectionViolation("IMPACT_CHANGE", 0, 0)) {
                                        showSuperProtectionBlockNotice = "الحظر الخارق مفعل! يمنع تغيير تصنيف التطبيق خارج نافذة التعديل."
                                    } else {
                                        selectedImpact = "NEUTRAL"
                                    }
                                },
                                label = { Text("رمادي / محايد ⚪", fontWeight = FontWeight.Bold) }
                            )

                            FilterChip(
                                selected = selectedImpact == "IGNORED",
                                onClick = {
                                    if (checkSuperProtectionViolation("IMPACT_CHANGE", 0, 0)) {
                                        showSuperProtectionBlockNotice = "الحظر الخارق مفعل! يمنع تغيير تصنيف التطبيق خارج نافذة التعديل."
                                    } else {
                                        selectedImpact = "IGNORED"
                                    }
                                },
                                label = { Text("عدم الشمل في المدة 🚫", fontWeight = FontWeight.Bold) }
                            )
                        }
                    }
                }
            }

            // 7. فئة التطبيق (تواصل - ترفيهي - انتاجية - وغيرها)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "فئة وتصنيف التطبيق",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )

                        val categoriesList = listOf(
                            "SOCIAL" to "تواصل اجتماعي 💬",
                            "ENTERTAINMENT" to "ترفيه وألعاب 🎮",
                            "PRODUCTIVITY" to "إنتاجية وتعليم 📚",
                            "TOOLS" to "أدوات ونظام ⚙️",
                            "OTHER" to "أخرى 📱"
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categoriesList.forEach { (catKey, catLabel) ->
                                FilterChip(
                                    selected = selectedCategory == catKey,
                                    onClick = {
                                        if (checkSuperProtectionViolation("CATEGORY_CHANGE", 0, 0)) {
                                            showSuperProtectionBlockNotice = "الحظر الخارق مفعل! يمنع تغيير فئة التطبيق خارج نافذة التعديل."
                                        } else {
                                            selectedCategory = catKey
                                        }
                                    },
                                    label = { Text(catLabel, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // نافذة تنبيه انتهاك الحظر الخارق
    showSuperProtectionBlockNotice?.let { notice ->
        AlertDialog(
            onDismissRequest = { showSuperProtectionBlockNotice = null },
            icon = { Icon(Icons.Default.Shield, contentDescription = null, tint = PriorityHighRed, modifier = Modifier.size(32.dp)) },
            title = { Text("الحظر الخارق يمنع هذا التعديل 🛡️", fontWeight = FontWeight.ExtraBold) },
            text = { Text(notice) },
            confirmButton = {
                Button(
                    onClick = { showSuperProtectionBlockNotice = null },
                    colors = ButtonDefaults.buttonColors(containerColor = PriorityHighRed)
                ) {
                    Text("حسناً، فهمت")
                }
            }
        )
    }
}

/**
 * رسم بياني أسبوعي يعرض أيام الأسبوع بارات متناسقة
 */
@Composable
fun WeeklyBarChart(
    days: List<DayUsageItem>,
    modifier: Modifier = Modifier
) {
    val maxMinutes = remember(days) {
        days.maxOfOrNull { it.totalMinutes }?.coerceAtLeast(30) ?: 60
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEach { day ->
            val ratio = (day.totalMinutes.toFloat() / maxMinutes.toFloat()).coerceIn(0.04f, 1f)
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                // قيمة الدقائق/الساعات أعلى العمود
                Text(
                    text = if (day.totalMinutes > 0) {
                        val h = day.totalMinutes / 60
                        val m = day.totalMinutes % 60
                        if (h > 0) "${h}س" else "${m}د"
                    } else "0",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                // عمود البار
                Box(
                    modifier = Modifier
                        .width(22.dp)
                        .fillMaxHeight(ratio * 0.72f)
                        .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                        .background(
                            if (day.isToday) MaterialTheme.colorScheme.primary
                            else if (day.totalMinutes > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // اسم اليوم
                Text(
                    text = day.dayNameArabic.take(3),
                    fontSize = 11.sp,
                    fontWeight = if (day.isToday) FontWeight.ExtraBold else FontWeight.Medium,
                    color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
