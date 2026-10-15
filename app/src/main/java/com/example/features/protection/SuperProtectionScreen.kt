package com.example.features.protection

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.prayer.UmmAlQuraCalculator
import com.example.features.DailyManagerUiState
import com.example.features.DailyManagerViewModel
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.PriorityHighRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuperProtectionScreen(
    uiState: DailyManagerUiState,
    viewModel: DailyManagerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    var selectedWindowTime by remember(uiState.superProtectionWindowTime24) {
        mutableStateOf(uiState.superProtectionWindowTime24.ifBlank { "13:00" })
    }
    var preventRaisingUsage by remember(uiState.superProtectionPreventRaisingUsageLimit) {
        mutableStateOf(uiState.superProtectionPreventRaisingUsageLimit)
    }
    var preventRaisingLaunch by remember(uiState.superProtectionPreventRaisingLaunchLimit) {
        mutableStateOf(uiState.superProtectionPreventRaisingLaunchLimit)
    }
    var preventCategoryChange by remember(uiState.superProtectionPreventChangingCategory) {
        mutableStateOf(uiState.superProtectionPreventChangingCategory)
    }
    var preventScheduleMod by remember(uiState.superProtectionPreventModifyingSchedules) {
        mutableStateOf(uiState.superProtectionPreventModifyingSchedules)
    }

    var showTimePickerModal by remember { mutableStateOf(false) }
    var showLockedDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = PriorityHighRed)
                        Text("الحظر الخارق 🛡️", fontWeight = FontWeight.ExtraBold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
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
            // بطاقة الشرح والتعريف بالفكرة
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(PriorityHighRed.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LockReset,
                                    contentDescription = null,
                                    tint = PriorityHighRed,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "ما هو الحظر الخارق؟",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "منع التراجع اللحظي وإغلاق ثغرات التسويف",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "كثير من المستخدمين عند تفعيل حظر تطبيق معين (مثلاً ساعة واحدة) ويقفل التطبيق، يذهب بكل بساطة ويرفع مدة الاستخدام! نظام الحظر الخارق يمنع أي تغيير سلبي إطلاقاً ولا يفتح لك نافذة التعديل إلا في نافذة وقتية واحدة مدتها 10 دقائق فقط في اليوم.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            // بطاقة التفعيل الرئيسية وحالة القفل
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.superProtectionEnabled) {
                            if (uiState.isSuperProtectionWindowOpenNow) EmeraldDeep.copy(alpha = 0.12f)
                            else PriorityHighRed.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        }
                    ),
                    border = if (uiState.superProtectionEnabled) {
                        androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (uiState.isSuperProtectionWindowOpenNow) EmeraldDeep.copy(alpha = 0.5f) else PriorityHighRed.copy(alpha = 0.5f)
                        )
                    } else null,
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
                                Icon(
                                    imageVector = if (uiState.superProtectionEnabled) Icons.Default.Shield else Icons.Outlined.Shield,
                                    contentDescription = null,
                                    tint = if (uiState.superProtectionEnabled) PriorityHighRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column {
                                    Text(
                                        text = "تفعيل ميزة الحظر الخارق",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = if (uiState.superProtectionEnabled) {
                                            if (uiState.isSuperProtectionWindowOpenNow) "الحماية مفعلة (النافذة مفتوحة الآن 🟢)"
                                            else "الحماية مفعلة ومقفلة ضد التعديل 🔒"
                                        } else "غير مفعلة حالياً",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (uiState.superProtectionEnabled) PriorityHighRed else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Switch(
                                checked = uiState.superProtectionEnabled,
                                onCheckedChange = { willEnable ->
                                    if (!willEnable) {
                                        // محاولة إيقاف الميزة
                                        if (uiState.superProtectionEnabled && !uiState.isSuperProtectionWindowOpenNow) {
                                            showLockedDialog = true
                                            return@Switch
                                        }
                                    }
                                    viewModel.toggleSuperProtection(
                                        enabled = willEnable,
                                        windowTime24 = selectedWindowTime,
                                        preventUsageRaise = preventRaisingUsage,
                                        preventLaunchRaise = preventRaisingLaunch,
                                        preventCategoryChange = preventCategoryChange,
                                        preventScheduleMod = preventScheduleMod
                                    )
                                    val msg = if (willEnable) "تم تفعيل الحظر الخارق بنجاح 🛡️" else "تم إيقاف الحظر الخارق"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.testTag("super_protection_main_switch")
                            )
                        }

                        if (uiState.superProtectionEnabled) {
                            HorizontalDivider()

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
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = if (uiState.isSuperProtectionWindowOpenNow) EmeraldDeep else PriorityHighRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "نافذة التعديل اليومية (10 دقائق):",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Text(
                                        text = uiState.nextSuperProtectionWindowLabel,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }

                            if (!uiState.isSuperProtectionWindowOpenNow) {
                                val remainingHours = uiState.timeUntilSuperProtectionWindowMinutes / 60
                                val remainingMins = uiState.timeUntilSuperProtectionWindowMinutes % 60
                                Text(
                                    text = "⏳ متبقي على نافذة التعديل القادمة: ${UmmAlQuraCalculator.toArabicDigits(if (remainingHours > 0) "$remainingHours ساعة و $remainingMins دقيقة" else "$remainingMins دقيقة")}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // إعداد نافذة وقت إغلاق وتعديل الميزة (10 دقائق)
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
                            text = "تحديد وقت نافذة التعديل (10 دقائق)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "حدد الساعة التي تفتح فيها نافذة التعديل يومياً لمدة 10 دقائق (مثلاً 01:00 م فتكون مفتوحة حتى 01:10 م):",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = selectedWindowTime,
                                onValueChange = { selectedWindowTime = it },
                                label = { Text("وقت بدء النافذة (صيغة 24س e.g. 13:00)") },
                                placeholder = { Text("13:00") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f).testTag("window_time_input")
                            )

                            Button(
                                onClick = {
                                    if (uiState.superProtectionEnabled && !uiState.isSuperProtectionWindowOpenNow) {
                                        showLockedDialog = true
                                        return@Button
                                    }
                                    viewModel.toggleSuperProtection(
                                        enabled = uiState.superProtectionEnabled,
                                        windowTime24 = selectedWindowTime,
                                        preventUsageRaise = preventRaisingUsage,
                                        preventLaunchRaise = preventRaisingLaunch,
                                        preventCategoryChange = preventCategoryChange,
                                        preventScheduleMod = preventScheduleMod
                                    )
                                    Toast.makeText(context, "تم حفظ وقت نافذة التعديل: $selectedWindowTime ✓", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("حفظ الوقت", fontWeight = FontWeight.Bold)
                            }
                        }

                        // أوقات مقترحة سريعة
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("06:00" to "6:00 ص", "13:00" to "1:00 م", "18:00" to "6:00 م", "21:00" to "9:00 م").forEach { (timeVal, label) ->
                                FilterChip(
                                    selected = selectedWindowTime == timeVal,
                                    onClick = {
                                        if (uiState.superProtectionEnabled && !uiState.isSuperProtectionWindowOpenNow) {
                                            showLockedDialog = true
                                        } else {
                                            selectedWindowTime = timeVal
                                        }
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }

            // قائمة قواعد وميزات الحظر الخارق
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
                            text = "قواعد وميزات الحظر الخارق المطبقة",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )

                        // 1. مدة الاستعمال فقط تنزل
                        SuperRuleRow(
                            icon = Icons.Default.TrendingDown,
                            title = "مدة الاستعمال تنزل فقط ولا ترتفع",
                            description = "إذا وضعت حد 45 دقيقة واستنفدته، يمنع النظام رفع الحد، ويسمح فقط بتقليله لزيادة الانضباط.",
                            checked = preventRaisingUsage,
                            onCheckedChange = {
                                if (uiState.superProtectionEnabled && !uiState.isSuperProtectionWindowOpenNow) showLockedDialog = true
                                else preventRaisingUsage = it
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // 2. حد مرات الفتح يقل فقط
                        SuperRuleRow(
                            icon = Icons.Default.VerticalAlignBottom,
                            title = "حد مرات فتح التطبيق يقل ولا يزيد",
                            description = "لا يمكنك زيادة عدد مرات فتح التطبيق المسموحة يومياً خارج نافذة التعديل.",
                            checked = preventRaisingLaunch,
                            onCheckedChange = {
                                if (uiState.superProtectionEnabled && !uiState.isSuperProtectionWindowOpenNow) showLockedDialog = true
                                else preventRaisingLaunch = it
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // 3. منع تغيير الفئة
                        SuperRuleRow(
                            icon = Icons.Default.Category,
                            title = "ممنوع تغيير فئة أو تصنيف التطبيقات",
                            description = "يمنع نقل تطبيق تواصل أو ترفيهي إلى فئة إنتاجية أو تغيير تصنيفه لتفادي قيود الجداول.",
                            checked = preventCategoryChange,
                            onCheckedChange = {
                                if (uiState.superProtectionEnabled && !uiState.isSuperProtectionWindowOpenNow) showLockedDialog = true
                                else preventCategoryChange = it
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // 4. منع تعديل أو حذف الجداول
                        SuperRuleRow(
                            icon = Icons.Default.LockClock,
                            title = "ممنوع إغلاق أي جدول أو حذفه أو تعديله",
                            description = "جميع جداول الحظر القائمة تصبح ثابتة ولا يمكن تعطيلها، بينما يُسمح دائماً بإضافة جداول جديدة.",
                            checked = preventScheduleMod,
                            onCheckedChange = {
                                if (uiState.superProtectionEnabled && !uiState.isSuperProtectionWindowOpenNow) showLockedDialog = true
                                else preventScheduleMod = it
                            }
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                        // 5. مسموح إضافة جداول
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null, tint = EmeraldDeep)
                            Column {
                                Text(
                                    text = "مسموح دائماً إضافة جداول حظر جديدة ✓",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = EmeraldDeep
                                )
                                Text(
                                    text = "يمكنك دائماً زيادة قيود الحظر وإضافة جداول جديدة في أي وقت.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // حوار تنبيه إقفال الميزة خارج النافذة
    if (showLockedDialog) {
        AlertDialog(
            onDismissRequest = { showLockedDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = PriorityHighRed,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "الحظر الخارق نشط ومقفل! 🛡️",
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "🔒 لا يمكنك تعديل القواعد أو إيقاف الميزة الآن!",
                        fontWeight = FontWeight.Bold,
                        color = PriorityHighRed,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "وفقاً لنظام الحظر الخارق، نافذة التعديل اليومية مفتوحة فقط في الموعد المحدد:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = PriorityHighRed.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "⏰ موعد نافذة التعديل القادمة: ${uiState.nextSuperProtectionWindowLabel} (لمدة 10 دقائق فقط)",
                            style = MaterialTheme.typography.bodySmall,
                            color = PriorityHighRed,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showLockedDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PriorityHighRed)
                ) {
                    Text("فهمت، التزم بالانضباط 💪")
                }
            }
        )
    }
}

@Composable
private fun SuperRuleRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 2.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
