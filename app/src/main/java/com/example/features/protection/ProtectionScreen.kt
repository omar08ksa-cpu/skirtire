package com.example.features.protection

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.core.prayer.UmmAlQuraCalculator
import com.example.core.protection.AppUsageMonitor
import com.example.core.protection.AlarmSoundManager
import com.example.core.protection.PlaceMatchResult
import com.example.core.protection.SystemAlarmSound
import com.example.core.protection.PlacePhotoHelper
import com.example.core.protection.ProtectionManager
import com.example.data.BlockScheduleEntity
import com.example.data.BlockedAppEntity
import com.example.features.DailyManagerUiState
import com.example.features.DailyManagerViewModel
import com.example.core.protection.AppUsageStatItem
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.PriorityHighRed
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.unit.sp
import java.util.Locale
import java.util.UUID

data class InstalledAppOption(
    val appName: String,
    val packageName: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProtectionScreen(
    uiState: DailyManagerUiState,
    viewModel: DailyManagerViewModel
) {
    val context = LocalContext.current
    var selectedSection by remember { mutableIntStateOf(0) } // 0 = التطبيقات ووقت الشاشة، 1 = الجداول والتركيز، 2 = الحظر الخارق، 3 = المنبه الخارق
    var selectedAppForDetail by remember { mutableStateOf<AppUsageStatItem?>(null) }
    var showPickInstalledAppDialog by remember { mutableStateOf(false) }
    var editingAppLimitTarget by remember { mutableStateOf<BlockedAppEntity?>(null) }
    var showAddScheduleDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.refreshAppUsageOverview()
            kotlinx.coroutines.delay(3000L)
        }
    }

    // إذا تم اختيار تطبيق لفتح تفاصيله
    if (selectedAppForDetail != null) {
        AppDetailScreen(
            appStat = selectedAppForDetail!!,
            blockedAppEntity = uiState.blockedApps.firstOrNull { it.packageName == selectedAppForDetail!!.packageName },
            uiState = uiState,
            viewModel = viewModel,
            onBack = { selectedAppForDetail = null }
        )
        return
    }

    // إذا تم اختيار قسم الحظر الخارق
    if (selectedSection == 2) {
        SuperProtectionScreen(
            uiState = uiState,
            viewModel = viewModel,
            onBack = { selectedSection = 0 }
        )
        return
    }

    val wakeAlarms = remember(uiState.blockSchedules) {
        uiState.blockSchedules.filter { it.scheduleType == "WAKE_ALARM" }
    }
    val sleepSchedule = remember(uiState.blockSchedules) {
        uiState.blockSchedules.firstOrNull { it.scheduleType == "SLEEP_MODE" }
    }
    val customSchedules = remember(uiState.blockSchedules) {
        uiState.blockSchedules.filter { it.scheduleType != "WAKE_ALARM" && it.scheduleType != "SLEEP_MODE" }
    }

    var showAddOrEditAlarmModal by remember { mutableStateOf(false) }
    var editingAlarmTarget by remember { mutableStateOf<BlockScheduleEntity?>(null) }
    var targetAlarmForPlacePhoto by remember { mutableStateOf<String?>(null) }
    var showPlaceCameraModal by remember { mutableStateOf(false) }
    var placeCameraModalModeIsTest by remember { mutableStateOf(false) }

    val hasAccessibilityEnabled: Boolean = remember(uiState.blockedApps, selectedSection) {
        ProtectionManager.isAccessibilityServiceEnabled(context)
    }
    val hasUsagePermission: Boolean = remember(uiState.usageOverview, selectedSection) {
        AppUsageMonitor.hasUsageStatsPermission(context)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp)
    ) {
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedSection == 0,
                    onClick = { selectedSection = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 4),
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(16.dp)) }
                ) {
                    Text("التطبيقات", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                SegmentedButton(
                    selected = selectedSection == 1,
                    onClick = { selectedSection = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 4),
                    icon = { Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp)) }
                ) {
                    Text("الجداول", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
                SegmentedButton(
                    selected = selectedSection == 2,
                    onClick = { selectedSection = 2 },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 4),
                    icon = { Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp)) }
                ) {
                    Text("الحظر الخارق", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                }
                SegmentedButton(
                    selected = selectedSection == 3,
                    onClick = { selectedSection = 3 },
                    shape = SegmentedButtonDefaults.itemShape(index = 3, count = 4),
                    icon = { Icon(Icons.Default.Alarm, contentDescription = null, modifier = Modifier.size(16.dp)) }
                ) {
                    Text("المنبه", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        when (selectedSection) {
            0 -> {
                // صلاحيات النظام الأساسية للحظر الفوري وتتبع وقت الشاشة
                if (!hasAccessibilityEnabled || !hasUsagePermission) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "صلاحيات نظام الحظر والرقابة",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (!hasAccessibilityEnabled) {
                                        FilledTonalButton(
                                            onClick = {
                                                context.startActivity(
                                                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                )
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("خدمة الحظر الفوري")
                                        }
                                    }
                                    if (!hasUsagePermission) {
                                        FilledTonalButton(
                                            onClick = {
                                                context.startActivity(
                                                    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                    }
                                                )
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.QueryStats, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("وقت الاستخدام")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // بطاقة وصول سريعة للحظر الخارق وحالته
                item {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (uiState.superProtectionEnabled) {
                            if (uiState.isSuperProtectionWindowOpenNow) EmeraldDeep.copy(alpha = 0.12f)
                            else PriorityHighRed.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedSection = 2 }
                            .testTag("super_protection_banner_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = if (uiState.superProtectionEnabled) {
                                        if (uiState.isSuperProtectionWindowOpenNow) EmeraldDeep else PriorityHighRed
                                    } else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (uiState.superProtectionEnabled) {
                                        if (uiState.isSuperProtectionWindowOpenNow) "الحظر الخارق: نافذة التعديل مفتوحة حالياً 🟢"
                                        else "الحظر الخارق مقفل 🔒 (${uiState.nextSuperProtectionWindowLabel})"
                                    } else "الحظر الخارق: اضغط لتعيين نافذة وقت تمنع التراجع والتعديل 🛡️",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // ملخص وقت الاستخدام السلبي والإيجابي وإجمالي الشاشة
                item {
                    NegativeAndPositiveScreenTimeCard(
                        uiState = uiState,
                        onRefresh = { viewModel.refreshAppUsageOverview() }
                    )
                }

                // الرسم البياني الأسبوعي لاستخدام الجوال مع التنقل بالأسابيع
                item {
                    WeeklyPhoneUsageCard()
                }

                // قائمة التطبيقات المستخدمة اليوم مع خيار عرض الكل والضغط للتفاصيل
                item {
                    ScreenTimeAppsListCard(
                        uiState = uiState,
                        onAppClick = { stat -> selectedAppForDetail = stat }
                    )
                }

                // قائمة التطبيقات المقيدة والمحظورة مع الحدود اليومية
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "التطبيقات المقيدة والمحظورة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Button(
                            onClick = { showPickInstalledAppDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.testTag("add_blocked_app_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة تطبيق", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (uiState.blockedApps.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "لا توجد تطبيقات مقيدة حالياً",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Button(onClick = { showPickInstalledAppDialog = true }) {
                                    Text("اختيار تطبيق وتحديد حده اليومي")
                                }
                            }
                        }
                    }
                } else {
                    items(uiState.blockedApps, key = { it.id }) { app ->
                        val activeReason = uiState.enforcedBlockReasonsByPackage[app.packageName]
                        val usedToday = uiState.usageOverview.usageByPackageMinutes[app.packageName] ?: 0
                        val usedMillis = uiState.usageOverview.usageByPackageMillis[app.packageName] ?: 0L
                        val formattedUsed = AppUsageMonitor.formatDurationArabic(usedMillis)
                        BlockedAppRuleCard(
                            app = app,
                            usedMinutesToday = usedToday,
                            usedFormattedToday = formattedUsed,
                            activeBlockReason = activeReason,
                            onEditLimit = { editingAppLimitTarget = app },
                            onOpenDetails = {
                                selectedAppForDetail = AppUsageStatItem(
                                    packageName = app.packageName,
                                    appName = app.appName,
                                    usedMinutesToday = usedToday,
                                    usedTimeMillisToday = usedMillis,
                                    formattedDuration = formattedUsed,
                                    launchCountToday = uiState.usageOverview.launchesByPackageToday[app.packageName] ?: 0
                                )
                            },
                            onToggleStudy = { viewModel.toggleBlockedAppStudy(app) },
                            onToggleSleep = { viewModel.toggleBlockedAppSleep(app) },
                            onDelete = { viewModel.deleteBlockedApp(app.id) }
                        )
                    }
                }
            }

            1 -> {
                // بطاقة حالة قفل العقاب النشط من العادات
                if (uiState.activePenaltyHabitsCount > 0 || uiState.enforcedBlockReasonsByPackage.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = PriorityHighRed.copy(alpha = 0.12f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = PriorityHighRed)
                                    Text(
                                        text = UmmAlQuraCalculator.toArabicDigits(
                                            "قفل العقاب والحظر نشط (${uiState.enforcedBlockReasonsByPackage.size} تطبيق مقفل)"
                                        ),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = PriorityHighRed
                                    )
                                }
                                Text(
                                    text = "يتوقف الحظر تلقائياً بمجرد إتمام العادات المرتبطة أو انتهاء وقت جدول الحظر.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // وضع النوم الليلي
                item {
                    val isSleepActive = uiState.isSleepModeActive
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSleepActive) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    Icon(
                                        Icons.Default.Bedtime,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Column {
                                        Text(
                                            text = "وضع النوم الليلي",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = "${sleepSchedule?.startTime24 ?: "23:00"} - ${sleepSchedule?.endTime24 ?: "04:30"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = isSleepActive,
                                    onCheckedChange = { active -> viewModel.toggleSleepMode(active) },
                                    modifier = Modifier.testTag("switch_sleep_mode_now")
                                )
                            }
                        }
                    }
                }

                // جداول الحظر المخصصة (قابلة للربط بالعادات)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "جداول الحظر المخصصة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        FilledTonalButton(onClick = { showAddScheduleDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة جدول", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (customSchedules.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "لا توجد جداول حظر مخصصة",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "أنشئ جدول حظر لتوقيت معين أو اربطه بقفل العقاب في العادات ليتوقف فور إنجاز عادتك.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(customSchedules, key = { it.id }) { schedule ->
                        RichScheduleCard(
                            schedule = schedule,
                            allBlockedApps = uiState.blockedApps,
                            uiState = uiState,
                            onToggle = { viewModel.toggleScheduleEnabled(schedule) },
                            onDelete = { viewModel.deleteBlockSchedule(schedule.id) }
                        )
                    }
                }
            }

            3 -> {
                // كارت الحماية القصوى وضمان عدم الإيقاف أو الغش (Anti-Cheat Security)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Alarm,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "المنبه الخارق (Anti-Cheat)",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "لا يمكن إيقافه من السرير إطلاقاً • قفل الصوت 100% ومنع التحايل",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            val isDeviceAdmin = ProtectionManager.isDeviceAdminActive(context)
                            val isAccessibility = ProtectionManager.isAccessibilityServiceEnabled(context)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isDeviceAdmin) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            if (!isDeviceAdmin) {
                                                val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                                    putExtra(
                                                        android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                                                        android.content.ComponentName(context, com.example.core.protection.AppDeviceAdminReceiver::class.java)
                                                    )
                                                    putExtra(
                                                        android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                                        "تفعيل مسؤول الجهاز لمنع حذف التطبيق والتحايل على المنبه الخارق"
                                                    )
                                                }
                                                context.startActivity(intent)
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            if (isDeviceAdmin) Icons.Default.CheckCircle else Icons.Default.GppMaybe,
                                            contentDescription = null,
                                            tint = if (isDeviceAdmin) Color(0xFF10B981) else Color(0xFFEF4444),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (isDeviceAdmin) "منع الحذف مفعّل" else "تفعيل حماية الحذف",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isAccessibility) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            if (!isAccessibility) {
                                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                })
                                            }
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            if (isAccessibility) Icons.Default.CheckCircle else Icons.Default.GppMaybe,
                                            contentDescription = null,
                                            tint = if (isAccessibility) Color(0xFF10B981) else Color(0xFFEF4444),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (isAccessibility) "منع الخروج مفعّل" else "تفعيل حماية الخروج",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // كارت أقرب منبه قادم مع حساب الوقت المتبقي
                item {
                    val activeAlarms = wakeAlarms.filter { it.isEnabled }
                    val nextUpcomingAlarm = activeAlarms.minByOrNull {
                        ProtectionManager.calculateNextTriggerMillis(it.startTime24, it.daysOfWeek)
                    }

                    if (nextUpcomingAlarm != null) {
                        val nextMillis = ProtectionManager.calculateNextTriggerMillis(nextUpcomingAlarm.startTime24, nextUpcomingAlarm.daysOfWeek)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.14f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        Icons.Default.AlarmOn,
                                        contentDescription = null,
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "المنبه القادم: ${nextUpcomingAlarm.title}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF065F46)
                                        )
                                        Text(
                                            text = "${formatTimeArabic(nextUpcomingAlarm.startTime24)} • ${calculateRemainingTimeText(nextMillis)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF047857)
                                        )
                                    }
                                }

                                Button(
                                    onClick = { viewModel.triggerWakeAlarmNow(nextUpcomingAlarm) },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                                ) {
                                    Text("تجربة الآن", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // ترويسة المنبهات وزر إضافة منبه جديد
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "المنبهات المجدولة (${wakeAlarms.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Button(
                            onClick = {
                                editingAlarmTarget = null
                                showAddOrEditAlarmModal = true
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("add_super_alarm_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إضافة منبه خارق", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (wakeAlarms.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    Icons.Default.AlarmAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "لا توجد منبهات خارقة حالياً",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "أضف منبهك الخارق الأول لصلاة الفجر أو العمل. لن يتوقف حتى تنهض من الفراش وتصوّر المكان المحدد!",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = {
                                        editingAlarmTarget = null
                                        showAddOrEditAlarmModal = true
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("إنشاء منبه خارق الآن")
                                }
                            }
                        }
                    }
                } else {
                    items(wakeAlarms, key = { it.id }) { alarm ->
                        SuperAlarmCard(
                            alarm = alarm,
                            onToggle = { viewModel.toggleWakeAlarm(alarm) },
                            onEdit = {
                                editingAlarmTarget = alarm
                                showAddOrEditAlarmModal = true
                            },
                            onDelete = { viewModel.deleteWakeAlarm(alarm.id) },
                            onTestNow = { viewModel.triggerWakeAlarmNow(alarm) }
                        )
                    }
                }
            }
        }
    }

    if (showPickInstalledAppDialog) {
        PickInstalledAppModal(
            context = context,
            existingPackages = uiState.blockedApps.map { it.packageName }.toSet(),
            onDismiss = { showPickInstalledAppDialog = false },
            onSelectApp = { appName, packageName, dailyLimit, isAlwaysBlocked ->
                viewModel.addOrUpdateBlockedApp(
                    appName = appName,
                    packageName = packageName,
                    dailyLimitMinutes = dailyLimit,
                    isAlwaysBlocked = isAlwaysBlocked,
                    blockInStudyMode = true,
                    blockInSleepMode = true
                )
                showPickInstalledAppDialog = false
            }
        )
    }

    editingAppLimitTarget?.let { targetApp ->
        EditAppLimitDialog(
            app = targetApp,
            uiState = uiState,
            onDismiss = { editingAppLimitTarget = null },
            onSave = { dailyLimit, isAlwaysBlocked ->
                viewModel.addOrUpdateBlockedApp(
                    appName = targetApp.appName,
                    packageName = targetApp.packageName,
                    category = targetApp.category,
                    dailyLimitMinutes = dailyLimit,
                    isAlwaysBlocked = isAlwaysBlocked,
                    blockInStudyMode = targetApp.blockInStudyMode,
                    blockInSleepMode = targetApp.blockInSleepMode
                )
                editingAppLimitTarget = null
            }
        )
    }

    if (showAddScheduleDialog) {
        AddRichBlockScheduleModal(
            blockedApps = uiState.blockedApps,
            existingSchedules = customSchedules,
            usageOverview = uiState.usageOverview,
            onDismiss = { showAddScheduleDialog = false },
            onConfirm = { title, scheduleType, start24, end24, daysCsv, packagesCsv, targetCategory, groupLimit, sessionDuration, sessionCooldown ->
                viewModel.saveRichBlockSchedule(
                    title = title,
                    scheduleType = scheduleType,
                    startTime24 = start24,
                    endTime24 = end24,
                    daysOfWeek = daysCsv,
                    blockedPackagesCsv = packagesCsv,
                    targetCategory = targetCategory,
                    groupLimitMinutes = groupLimit,
                    sessionDurationMinutes = sessionDuration,
                    sessionCooldownMinutes = sessionCooldown,
                    isEnabled = true
                )
                showAddScheduleDialog = false
            }
        )
    }

    if (showAddOrEditAlarmModal) {
        AddOrEditSuperAlarmModal(
            context = context,
            existingAlarm = editingAlarmTarget,
            onDismiss = { showAddOrEditAlarmModal = false },
            onSave = { alarm ->
                viewModel.saveWakeAlarm(alarm)
                showAddOrEditAlarmModal = false
            },
            onRequestCameraPlace = { alarmId, isTestMode ->
                targetAlarmForPlacePhoto = alarmId
                placeCameraModalModeIsTest = isTestMode
                showPlaceCameraModal = true
            }
        )
    }

    if (showPlaceCameraModal) {
        PlaceCameraModal(
            context = context,
            alarmId = targetAlarmForPlacePhoto.orEmpty(),
            isTestMode = placeCameraModalModeIsTest,
            onDismiss = { showPlaceCameraModal = false },
            onPhotoSaved = {
                // تم حفظ الصورة المرجعية لهذا المنبه بنجاح
            }
        )
    }
}

@Composable
private fun BlockedAppRuleCard(
    app: BlockedAppEntity,
    usedMinutesToday: Int,
    usedFormattedToday: String,
    activeBlockReason: String?,
    onEditLimit: () -> Unit,
    onOpenDetails: (() -> Unit)? = null,
    onToggleStudy: () -> Unit,
    onToggleSleep: () -> Unit,
    onDelete: () -> Unit
) {
    val limitProgress = if (app.dailyLimitMinutes > 0) {
        (usedMinutesToday.toFloat() / app.dailyLimitMinutes.coerceAtLeast(1)).coerceIn(0f, 1f)
    } else 0f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onOpenDetails != null) Modifier.clickable { onOpenDetails() } else Modifier),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    val subtitle = when {
                        app.isAlwaysBlocked -> UmmAlQuraCalculator.toArabicDigits(
                            "حظر دائم • استُخدم اليوم: $usedFormattedToday"
                        )
                        app.dailyLimitMinutes > 0 -> UmmAlQuraCalculator.toArabicDigits(
                            "الاستخدام اليومي: $usedFormattedToday من أصل ${app.dailyLimitMinutes} دقيقة"
                        )
                        else -> UmmAlQuraCalculator.toArabicDigits("المستخدم اليوم: $usedFormattedToday")
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (activeBlockReason != null) PriorityHighRed else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onEditLimit) {
                        Icon(
                            Icons.Outlined.Timer,
                            contentDescription = "تعديل الحد اليومي",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Outlined.DeleteOutline,
                            contentDescription = "حذف",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (app.dailyLimitMinutes > 0) {
                LinearProgressIndicator(
                    progress = { limitProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50)),
                    color = if (limitProgress >= 1f) PriorityHighRed else MaterialTheme.colorScheme.primary
                )
            }

            if (activeBlockReason != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PriorityHighRed.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "محظور الآن: $activeBlockReason",
                        style = MaterialTheme.typography.labelSmall,
                        color = PriorityHighRed,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = app.isAlwaysBlocked || app.dailyLimitMinutes > 0,
                    onClick = onEditLimit,
                    label = {
                        Text(
                            when {
                                app.isAlwaysBlocked -> "محظور دائماً"
                                app.dailyLimitMinutes > 0 -> UmmAlQuraCalculator.toArabicDigits("حد ${app.dailyLimitMinutes} د")
                                else -> "تحديد حد يومي"
                            }
                        )
                    }
                )
                FilterChip(
                    selected = app.blockInStudyMode,
                    onClick = onToggleStudy,
                    label = { Text("أثناء العداد") }
                )
                FilterChip(
                    selected = app.blockInSleepMode,
                    onClick = onToggleSleep,
                    label = { Text("وقت النوم") }
                )
            }
        }
    }
}

@Composable
private fun CustomScheduleCard(
    schedule: BlockScheduleEntity,
    blockedApps: List<BlockedAppEntity>,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val packagesCount = schedule.blockedPackagesCsv
        .split(",")
        .map { it.trim() }
        .count { it.isNotEmpty() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = schedule.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "${schedule.startTime24} - ${schedule.endTime24}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (packagesCount > 0) {
                        UmmAlQuraCalculator.toArabicDigits("يشمل $packagesCount تطبيق")
                    } else {
                        UmmAlQuraCalculator.toArabicDigits("يشمل جميع التطبيقات المقيدة (${blockedApps.size})")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = schedule.isEnabled,
                    onCheckedChange = { onToggle() }
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = "حذف الجدول",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun EditAppLimitDialog(
    app: BlockedAppEntity,
    uiState: DailyManagerUiState,
    onDismiss: () -> Unit,
    onSave: (dailyLimitMinutes: Int, isAlwaysBlocked: Boolean) -> Unit
) {
    var isAlways by remember { mutableStateOf(app.isAlwaysBlocked) }
    var limitText by remember {
        mutableStateOf(if (app.dailyLimitMinutes > 0) app.dailyLimitMinutes.toString() else "30")
    }
    var hasLimit by remember { mutableStateOf(app.dailyLimitMinutes > 0 && !app.isAlwaysBlocked) }
    var showSuperProtectionAlert by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("قيود تطبيق ${app.appName}", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isAlways,
                        onClick = {
                            isAlways = true
                            hasLimit = false
                        },
                        label = { Text("حظر دائم") }
                    )
                    FilterChip(
                        selected = hasLimit && !isAlways,
                        onClick = {
                            isAlways = false
                            hasLimit = true
                        },
                        label = { Text("حد يومي") }
                    )
                    FilterChip(
                        selected = !isAlways && !hasLimit,
                        onClick = {
                            isAlways = false
                            hasLimit = false
                        },
                        label = { Text("في الجداول فقط") }
                    )
                }

                if (hasLimit && !isAlways) {
                    OutlinedTextField(
                        value = limitText,
                        onValueChange = { limitText = it.filter { c -> c.isDigit() } },
                        label = { Text("الحد اليومي بالدقائق") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(15, 30, 60, 90).forEach { mins ->
                            AssistChip(
                                onClick = { limitText = mins.toString() },
                                label = { Text(UmmAlQuraCalculator.toArabicDigits("$mins د")) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val mins = if (hasLimit && !isAlways) (limitText.toIntOrNull() ?: 30).coerceAtLeast(1) else 0
                    if (uiState.superProtectionEnabled && !uiState.isSuperProtectionWindowOpenNow && uiState.superProtectionPreventRaisingUsageLimit) {
                        if (app.dailyLimitMinutes > 0 && mins > app.dailyLimitMinutes) {
                            showSuperProtectionAlert = true
                            return@Button
                        }
                    }
                    onSave(mins, isAlways)
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

    if (showSuperProtectionAlert) {
        AlertDialog(
            onDismissRequest = { showSuperProtectionAlert = false },
            icon = { Icon(Icons.Default.Shield, contentDescription = null, tint = PriorityHighRed) },
            title = { Text("الحظر الخارق مفعل 🛡️", fontWeight = FontWeight.Bold) },
            text = {
                Text("يمنع رفع الحد اليومي للتطبيق خارج نافذة التعديل المحددة (${uiState.nextSuperProtectionWindowLabel}).\nيمكنك تقليل الحد فقط لحماية وقتك.")
            },
            confirmButton = {
                Button(
                    onClick = { showSuperProtectionAlert = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PriorityHighRed)
                ) {
                    Text("حسناً")
                }
            }
        )
    }
}

@Composable
private fun AddCustomBlockScheduleModal(
    blockedApps: List<BlockedAppEntity>,
    onDismiss: () -> Unit,
    onConfirm: (title: String, start24: String, end24: String, packagesCsv: String) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var startTime by remember { mutableStateOf("16:00") }
    var endTime by remember { mutableStateOf("20:00") }
    val selectedPackages = remember {
        mutableStateListOf<String>().apply {
            addAll(blockedApps.map { it.packageName })
        }
    }

    val allCandidateApps = remember(blockedApps) {
        val pm = context.packageManager
        val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null && it.packageName != context.packageName }
            .map { InstalledAppOption(pm.getApplicationLabel(it).toString(), it.packageName) }
            .take(25)
        val fromBlocked = blockedApps.map { InstalledAppOption(it.appName, it.packageName) }
        (fromBlocked + installed).distinctBy { it.packageName }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة جدول حظر", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("اسم الجدول") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("وقت البدء") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("وقت الانتهاء") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Text(
                    text = "التطبيقات المشمولة بالجدول",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    allCandidateApps.forEach { app ->
                        val isSelected = app.packageName in selectedPackages
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) selectedPackages.remove(app.packageName)
                                else selectedPackages.add(app.packageName)
                            },
                            label = { Text(app.appName) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(
                            title.trim(),
                            startTime.trim(),
                            endTime.trim(),
                            selectedPackages.joinToString(",")
                        )
                    }
                }
            ) {
                Text("حفظ الجدول")
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
private fun CaptureReferencePlaceModal(
    placeName: String,
    onDismiss: () -> Unit,
    onPhotoCaptured: (Bitmap) -> Unit
) {
    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تصوير المكان المرجعي: $placeName", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "وجه الكاميرا نحو المكان المطلوب ثم اضغط التقاط وحفظ.",
                    style = MaterialTheme.typography.bodySmall
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                ) {
                    LivePlaceCameraPreview(
                        onPreviewReady = { previewViewRef = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val bmp = previewViewRef?.bitmap
                    if (bmp != null) {
                        onPhotoCaptured(bmp)
                    }
                }
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("التقاط وحفظ")
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
private fun PickInstalledAppModal(
    context: Context,
    existingPackages: Set<String>,
    onDismiss: () -> Unit,
    onSelectApp: (appName: String, packageName: String, dailyLimitMinutes: Int, isAlwaysBlocked: Boolean) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedLimitMode by remember { mutableIntStateOf(30) } // -1 = حظر دائم، 0 = في الجداول فقط، >0 = حد يومي بالدقائق

    val installedApps = remember {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { appInfo ->
                pm.getLaunchIntentForPackage(appInfo.packageName) != null &&
                    appInfo.packageName != context.packageName
            }
            .map { appInfo ->
                InstalledAppOption(
                    appName = pm.getApplicationLabel(appInfo).toString(),
                    packageName = appInfo.packageName
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.appName }

        val popularApps = listOf(
            InstalledAppOption("تيك توك", "com.zhiliaoapp.musically"),
            InstalledAppOption("إنستغرام", "com.instagram.android"),
            InstalledAppOption("يوتيوب", "com.google.android.youtube"),
            InstalledAppOption("منصة إكس", "com.twitter.android"),
            InstalledAppOption("سناب شات", "com.snapchat.android")
        )
        (popularApps + apps).distinctBy { it.packageName }
    }

    val filteredApps = remember(installedApps, searchQuery, existingPackages) {
        installedApps.filter { app ->
            app.packageName !in existingPackages &&
                (searchQuery.isBlank() ||
                    app.appName.contains(searchQuery, ignoreCase = true) ||
                    app.packageName.contains(searchQuery, ignoreCase = true))
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة تطبيق لقائمة القيود", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "نوع القيد عند الإضافة",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedLimitMode == -1,
                        onClick = { selectedLimitMode = -1 },
                        label = { Text("حظر دائم") }
                    )
                    FilterChip(
                        selected = selectedLimitMode == 15,
                        onClick = { selectedLimitMode = 15 },
                        label = { Text("حد ١٥ د") }
                    )
                    FilterChip(
                        selected = selectedLimitMode == 30,
                        onClick = { selectedLimitMode = 30 },
                        label = { Text("حد ٣٠ د") }
                    )
                    FilterChip(
                        selected = selectedLimitMode == 60,
                        onClick = { selectedLimitMode = 60 },
                        label = { Text("حد ٦٠ د") }
                    )
                    FilterChip(
                        selected = selectedLimitMode == 0,
                        onClick = { selectedLimitMode = 0 },
                        label = { Text("في الجداول فقط") }
                    )
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("بحث باسم التطبيق") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredApps, key = { it.packageName }) { app ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectApp(
                                        app.appName,
                                        app.packageName,
                                        if (selectedLimitMode > 0) selectedLimitMode else 0,
                                        selectedLimitMode == -1
                                    )
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(app.appName, fontWeight = FontWeight.Bold)
                                Icon(
                                    Icons.Default.AddCircleOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}
