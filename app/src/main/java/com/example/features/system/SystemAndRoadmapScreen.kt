package com.example.features.system

import android.Manifest
import android.app.AlarmManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.core.protection.AppDeviceAdminReceiver
import com.example.core.protection.ProtectionManager
import com.example.features.DailyManagerUiState

@Composable
fun SystemAndRoadmapScreen(
    uiState: DailyManagerUiState,
    onGenerateBackupPreview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var refreshTrigger by remember { mutableIntStateOf(0) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        refreshTrigger++
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        refreshTrigger++
    }

    val isNotificationsGranted = remember(refreshTrigger) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    val isCameraGranted = remember(refreshTrigger) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    val isExactAlarmGranted = remember(refreshTrigger) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    val isBatteryUnrestricted = remember(refreshTrigger) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    val isOverlayGranted = remember(refreshTrigger) {
        Settings.canDrawOverlays(context)
    }

    val isAccessibilityGranted = remember(refreshTrigger) {
        ProtectionManager.isAccessibilityServiceEnabled(context)
    }

    val isDeviceAdminGranted = remember(refreshTrigger) {
        ProtectionManager.isDeviceAdminActive(context)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("system_roadmap_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "صحة التطبيق (سامسونج One UI) ونموذج البيانات",
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = "فحص مباشر لكل الصلاحيات المطلوبة للمراحل ١ و ٢ و ٣ (المنبه، الكاميرا، إمكانية الوصول، مسؤول الجهاز، والبطارية).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // ١. فحص الصلاحيات المباشر الشامل
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "فحص الصلاحيات الحية على جهازك (٧ صلاحيات)",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    PermissionStatusRow(
                        title = "إشعارات الأذان ومؤقت المذاكرة",
                        subtitle = "ضرورية لإظهار تنبيه الصلاة وإشعار مؤقت المذاكرة بالخلفية",
                        isGranted = isNotificationsGranted,
                        icon = Icons.Filled.Notifications,
                        actionLabel = "تفعيل",
                        onAction = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )

                    PermissionStatusRow(
                        title = "المنبهات الدقيقة (Exact Alarms)",
                        subtitle = "لضمان رنين الأذان ومنبه الاستيقاظ في الثانية المحددة حتى مع قفل الشاشة",
                        isGranted = isExactAlarmGranted,
                        icon = Icons.Filled.Schedule,
                        actionLabel = "فتح الإعداد",
                        onAction = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                try {
                                    context.startActivity(
                                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                    )
                                } catch (_: Exception) {
                                }
                            }
                            refreshTrigger++
                        }
                    )

                    PermissionStatusRow(
                        title = "الكاميرا (لمسح QR في منبه الاستيقاظ)",
                        subtitle = "تُستخدم فقط لمسح رمز QR المرجعي عند الاستيقاظ ولا ترفع أي صور",
                        isGranted = isCameraGranted,
                        icon = Icons.Filled.CameraAlt,
                        actionLabel = "منح الكاميرا",
                        onAction = {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    )

                    PermissionStatusRow(
                        title = "خدمة إمكانية الوصول (حجب التطبيقات)",
                        subtitle = "لحجب التطبيقات المشتتة أثناء جلسات المذاكرة ووضع النوم",
                        isGranted = isAccessibilityGranted,
                        icon = Icons.Filled.AccessibilityNew,
                        actionLabel = "تفعيل الخدمة",
                        onAction = {
                            try {
                                context.startActivity(
                                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                )
                            } catch (_: Exception) {
                            }
                            refreshTrigger++
                        }
                    )

                    PermissionStatusRow(
                        title = "الظهور فوق التطبيقات (Overlay)",
                        subtitle = "لعرض شاشة منبه الاستيقاظ وشاشة الحجب فوق التطبيقات المشتتة",
                        isGranted = isOverlayGranted,
                        icon = Icons.Filled.Layers,
                        actionLabel = "السماح",
                        onAction = {
                            try {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    ).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                )
                            } catch (_: Exception) {
                            }
                            refreshTrigger++
                        }
                    )

                    PermissionStatusRow(
                        title = "مسؤول الجهاز (Device Admin)",
                        subtitle = "لمنع حذف التطبيق وقت الاندفاع أثناء تفعيل الحماية",
                        isGranted = isDeviceAdminGranted,
                        icon = Icons.Filled.AdminPanelSettings,
                        actionLabel = "تفعيل",
                        onAction = {
                            try {
                                val comp = ComponentName(context, AppDeviceAdminReceiver::class.java)
                                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, comp)
                                    putExtra(
                                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                        "يمنع حذف تطبيق مدير يومي وقت الاندفاع."
                                    )
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                            }
                            refreshTrigger++
                        }
                    )

                    PermissionStatusRow(
                        title = "استثناء توفير البطارية (غير مقيّد)",
                        subtitle = "يمنع نظام سامسونج One UI من إيقاف خدمة المؤقت أو المنبهات بالخلفية",
                        isGranted = isBatteryUnrestricted,
                        icon = Icons.Filled.BatteryChargingFull,
                        actionLabel = "استثناء البطارية",
                        onAction = {
                            try {
                                context.startActivity(
                                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                )
                            } catch (_: Exception) {
                            }
                            refreshTrigger++
                        }
                    )
                }
            }
        }

        // ٢. دليل سامسونج One UI المصغر
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "خطوات سامسونج One UI لضمان عدم توقف المؤقت أو المنبه أو الحجب أبدًا:",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "١. الإعدادات ← البطارية ← حدود استخدام الخلفية ← «التطبيقات التي لا تنام أبدًا» ← أضف (مدير يومي).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "٢. افتح شاشة التطبيقات الأخيرة ← اضغط مطولًا على أيقونة «مدير يومي» بالأعلى ← اختر «إبقاء مفتوحًا» (القفل).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "٣. عند التثبيت من ملف APK مباشر (أندرويد ١٣+):لتفعيل إمكانية الوصول، افتح الإعدادات ← التطبيقات ← مدير يومي ← النقاط الثلاث بأعلى الشاشة ← «السماح بالإعدادات المقيدة».",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        // ٣. مراجعة نموذج البيانات المبدئي (١٦ جدولًا) والنسخ الاحتياطي
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Storage,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "نموذج البيانات المحلي المكتمل (SQLite Local-first v5 - ١٦ جدولًا)",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    Text(
                        text = "كل جدول يحتوي على أعمدة المزامنة القياسية: id (UUID), created_at, updated_at, deleted_at (حذف ناعم لحل التعارض بقاعدة آخر تعديل يفوز).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "جميع الجداول المفعّلة في التطبيق (١٦ جدولًا كاملًا):",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "• habits & habit_logs (العادات الـ ٥ التلقائية وسجلاتها والتقويم الحراري)\n• tasks (المهام الرئيسية والفرعية والتكرار)\n• prayer_logs (سجل الصلوات الخمس المربوط بالعادة)\n• subjects & study_sessions (المواد وجلسات المذاكرة)\n• blocked_apps & block_schedules (قواعد الحجب ومنبه QR ووضع النوم)\n• lectures (المحاضرات الجامعية اليدوية والمستوردة من .ics)\n• wird_progress (الورد القرآني والختمة وعلامة الوقف - ٦٠٤ صفحات)\n• azkar_logs (سجلات أذكار حصن المسلم اليومية)\n• health_snapshots (الخطوات والسعرات المحروقة والمستهلكة والنوم)\n• daily_summaries (الملخص اليومي الذكي وجدول الغد المقترح)\n• weekly_reviews (أهداف وتقييم الأسبوع)\n• year_week_goals (خريطة وأهداف أسابيع السنة الـ ٥٢)\n• settings (الإعدادات العامة)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = onGenerateBackupPreview,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_backup_preview_btn")
                    ) {
                        Text("معاينة ملخص النسخة الاحتياطية المحلية (JSON)")
                    }

                    if (uiState.backupJsonPreview != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = uiState.backupJsonPreview,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // ٤. خارطة المراحل (٠ إلى ٧)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "خارطة مراحل المشروع (اكتملت جميع المراحل بنجاح 🎉)",
                        style = MaterialTheme.typography.titleLarge
                    )
                    RoadmapStageItem("✅ المرحلة ٠ و ١:", "تجهيز المشروع، الثيم العربي RTL + الوضع الليلي، قاعدة البيانات المحلية، العادات، المهام، مواقيت أم القرى والعدّاد وتنبيه الأذان.")
                    RoadmapStageItem("✅ المرحلة ٢:", "المذاكرة ووضع Standby: المواد بألوان، مؤقت بومودورو وحر مع خدمة خلفية Foreground Service، حفظ الجلسات، التقارير، و٣ تصاميم Standby أفقية.")
                    RoadmapStageItem("✅ المرحلة ٣:", "منبه الاستيقاظ بالتحقق (QR أولًا + مخرج طوارئ)، وضع النوم، حجب التطبيقات عبر AccessibilityService، حماية التطبيق، وفحص الصلاحيات.")
                    RoadmapStageItem("⏭️ المرحلة ٤ (تم تخطيها بطلبك):", "المزامنة السحابية لحسابك الخاص (Local-first Sync) — جاهزة للتفعيل متى ما رغبت.")
                    RoadmapStageItem("✅ المرحلة ٥:", "اليوم الكامل: المحاضرات (يدوي + ICS)، الأذكار (حصن المسلم)، الورد والختمة (مصحف المدينة ٦٠٤ ص)، وربط Health Connect وحساب السعرات.")
                    RoadmapStageItem("✅ المرحلة ٦ و ٧ (المكتملة الآن):", "الملخص اليومي الذكي وبناء جدول اليوم التالي، التخطيط الأسبوعي، التايم لاين الشامل من الاستيقاظ للنوم، أسابيع السنة الـ ٥٢، وويدجت الشاشة الرئيسية لأندرويد.")
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusRow(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    actionLabel: String,
    onAction: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (isGranted) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مفعّل", style = MaterialTheme.typography.labelLarge)
                    }
                }
            } else {
                Button(onClick = onAction) {
                    Icon(
                        imageVector = Icons.Filled.WarningAmber,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun RoadmapStageItem(stageTitle: String, stageDesc: String) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = stageTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = stageDesc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
