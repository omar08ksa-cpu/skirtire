package com.example.features.prayer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.core.prayer.*
import com.example.features.DailyManagerUiState
import com.example.features.DailyManagerViewModel
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldSoft
import com.example.ui.theme.MintAccent
import com.example.ui.theme.PriorityHighRed

/**
 * شاشة «الضبط والمنبهات» للقسم الديني:
 * - قائمة عامودية مرتبة: الفجر، الشروق، الظهر، العصر، المغرب، العشاء، منتصف الليل، الثلث الأخير من الليل
 * - خيار جرس ثلاثي (صامت، اهتزاز، صوت أذان)
 * - خيار العمل حتى لو كان الجوال صامت (تجاوز وضع الصامت)
 * - اختيار صوت الأذان / المؤذن مع نافذة استماع وتجربة الأصوات
 * - زر إضافة منبه مخصص بجميع الخيارات الستة
 * - منبهات خروج وقت الصلاة، الجمعة، الإقامة، قلب الجهاز، والعداد الدائم
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerSettingsAndAlarmsScreen(
    uiState: DailyManagerUiState,
    viewModel: DailyManagerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var showAddCustomAlarmDialog by remember { mutableStateOf(false) }
    var selectedMuazzinPickerPrayer by remember { mutableStateOf<PrayerType?>(null) }

    val schedule = uiState.prayerSchedule
    val displayPrayers = listOf(
        PrayerType.FAJR,
        PrayerType.SUNRISE,
        PrayerType.DHUHR,
        PrayerType.ASR,
        PrayerType.MAGHRIB,
        PrayerType.ISHA,
        PrayerType.MIDNIGHT,
        PrayerType.LAST_THIRD
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "الضبط والمنبهات",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("prayer_settings_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { showAddCustomAlarmDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("add_custom_prayer_alarm_top_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("منبه مخصص", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("prayer_settings_scroll_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // بطاقة توضيحية لسهولة التحكم
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = EmeraldDeep.copy(alpha = 0.08f),
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = EmeraldDeep,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "تحكّم في نوع التنبيه لكل صلاة وصوت المؤذن، أو أنشئ تنبيهات مسبقة قبل الأذان أو بعده.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // قائمة الصلوات العامودية
            item {
                Text(
                    text = "مواقيت وتنبيهات الصلوات",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            items(displayPrayers, key = { it.id }) { prayerType ->
                val slot = schedule.prayers.find { it.type == prayerType }
                val currentBellMode = uiState.prayerBellModesMap[prayerType] ?: if (prayerType.isObligatory) PrayerBellMode.AZAN else PrayerBellMode.SILENT
                val isOverrideSilent = uiState.prayerOverrideSilentMap[prayerType] ?: false
                val selectedVoiceId = uiState.prayerMuazzinMap[prayerType] ?: "makkah"
                val voice = MuazzinCatalog.findVoiceById(selectedVoiceId)

                PrayerSettingRowCard(
                    prayerType = prayerType,
                    timeFormatted = slot?.formatted12h ?: "--:--",
                    bellMode = currentBellMode,
                    overrideSilent = isOverrideSilent,
                    muazzinVoice = voice,
                    onBellModeChange = { nextMode ->
                        viewModel.setPrayerBellMode(prayerType, nextMode)
                    },
                    onOverrideSilentChange = { nextOverride ->
                        viewModel.setPrayerOverrideSilent(prayerType, nextOverride)
                    },
                    onSelectMuazzinClick = {
                        selectedMuazzinPickerPrayer = prayerType
                    }
                )
            }

            // المنبهات المخصصة
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "المنبهات والتذكيرات المخصصة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TextButton(onClick = { showAddCustomAlarmDialog = true }) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (uiState.customPrayerAlarms.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.AlarmAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("لا توجد منبهات مخصصة بعد", fontWeight = FontWeight.Bold)
                            Text(
                                "اضغط «إضافة» لجدولة منبه قبل الفجر أو بعد المغرب بدقائق محددة لأيام معينة.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(uiState.customPrayerAlarms, key = { it.id }) { customAlarm ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = customAlarm.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${customAlarm.timingFormatted} • ${customAlarm.prayersFormatted}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "الأيام: ${customAlarm.daysFormatted} • الصوت: ${customAlarm.soundType}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldDeep
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = customAlarm.isEnabled,
                                    onCheckedChange = { viewModel.toggleCustomPrayerAlarm(customAlarm.id) }
                                )
                                IconButton(onClick = { viewModel.deleteCustomPrayerAlarm(customAlarm.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "حذف",
                                        tint = PriorityHighRed
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ميزات وتنبيهات الأوقات الخاصة
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "منبهات الأوقات والسنن النبوية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // منبه خروج وقت الصلاة
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("تنبيه قرب خروج وقت الصلاة", fontWeight = FontWeight.Bold)
                                Text("تذكير قبل خروج وقت الصلاة بـ 15 دقيقة لتداركها", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = uiState.prayerEndTimeAlarmEnabled,
                                onCheckedChange = { viewModel.togglePrayerEndTimeAlarm() }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // منبه منتصف الليل
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("منبه منتصف الليل الشرعي", fontWeight = FontWeight.Bold)
                                Text("تنبيه بانتهاء وقت العشاء الاختياري", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = uiState.midnightAlarmEnabled,
                                onCheckedChange = { viewModel.toggleMidnightAlarm() }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // منبه الثلث الأخير من الليل (قيام الليل)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("منبه الثلث الأخير من الليل", fontWeight = FontWeight.Bold)
                                Text("للاستيقاظ لقيام الليل والوتر والاستغفار بالأسحار", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = uiState.lastThirdAlarmEnabled,
                                onCheckedChange = { viewModel.toggleLastThirdAlarm() }
                            )
                        }
                    }
                }
            }

            // إعدادات صلاة الجمعة
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "إعدادات وسنن يوم الجمعة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("صامت تلقائي أثناء خطبة وصلاة الجمعة", fontWeight = FontWeight.Bold)
                                Text("تحويل الجوال لوضع الصامت تلقائياً منعاً للإحراج بالمسجد", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = uiState.fridaySilentEnabled,
                                onCheckedChange = { viewModel.toggleFridaySilent() }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("تذكير بآخر ساعة من يوم الجمعة", fontWeight = FontWeight.Bold)
                                Text("التذكير بساعة الإجابة المباركة قبيل مغرب الجمعة", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = uiState.fridayLastHourReminderEnabled,
                                onCheckedChange = { viewModel.toggleFridayLastHourReminder() }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("تذكير قراءة سورة الكهف والتبكير", fontWeight = FontWeight.Bold)
                                Text("تنبيه صباح الجمعة لنيل أجر التبكير وقراءة السورة", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = uiState.fridaySurahKahfReminderEnabled,
                                onCheckedChange = { viewModel.toggleFridaySurahKahfReminder() }
                            )
                        }
                    }
                }
            }

            // إعدادات وضع الصامت عند الإقامة وقلب الجهاز
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "الإسكات الذكي والإشعارات",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // إسكات الجوال وقت الإقامة
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("إسكات الجوال وقت الإقامة تلقائياً", fontWeight = FontWeight.Bold)
                                Text("وضع الجوال على الصامت لمدة محددة عند بدء الصلاة", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = uiState.iqamaSilentEnabled,
                                onCheckedChange = { viewModel.toggleIqamaSilent() }
                            )
                        }

                        if (uiState.iqamaSilentEnabled) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("مدة الصامت:", style = MaterialTheme.typography.labelMedium)
                                listOf(15, 20, 30, 45).forEach { mins ->
                                    FilterChip(
                                        selected = uiState.iqamaSilentDurationMinutes == mins,
                                        onClick = { viewModel.setIqamaSilentDuration(mins) },
                                        label = { Text("$mins دقيقة") }
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // إيقاف الأذان بقلب الجهاز
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("إيقاف أو كتم الأذان بقلب الجهاز", fontWeight = FontWeight.Bold)
                                Text("اقلب شاشة الهاتف للأسفل لكتم نداء الأذان فوراً", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = uiState.flipToSnoozeAzanEnabled,
                                onCheckedChange = { viewModel.toggleFlipToSnoozeAzan() }
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        // عداد الصلاة الدائم في لوحة الإشعارات
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("عداد الصلاة الدائم في الإشعارات", fontWeight = FontWeight.Bold)
                                Text("إشعار دائم أعلى لوحة الإشعارات يعرض الوقت المتبقي", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = uiState.persistentPrayerNotificationEnabled,
                                onCheckedChange = { viewModel.togglePersistentPrayerNotification() }
                            )
                        }
                    }
                }
            }
        }
    }

    // حوار اختيار صوت المؤذن
    if (selectedMuazzinPickerPrayer != null) {
        val targetPrayer = selectedMuazzinPickerPrayer!!
        MuazzinVoicePickerDialog(
            currentVoiceId = uiState.prayerMuazzinMap[targetPrayer] ?: "makkah",
            prayerName = targetPrayer.arabicName,
            onVoiceSelected = { voiceId ->
                viewModel.setPrayerMuazzin(targetPrayer, voiceId)
                selectedMuazzinPickerPrayer = null
            },
            onPreviewClick = { voiceId ->
                viewModel.previewMuazzinVoice(voiceId)
            },
            onDismiss = { selectedMuazzinPickerPrayer = null }
        )
    }

    // حوار إضافة منبه مخصص
    if (showAddCustomAlarmDialog) {
        AddCustomPrayerAlarmDialog(
            onSave = { name, prayers, isBefore, offsetMins, days, soundType ->
                viewModel.addCustomPrayerAlarm(name, prayers, isBefore, offsetMins, days, soundType)
                showAddCustomAlarmDialog = false
            },
            onDismiss = { showAddCustomAlarmDialog = false }
        )
    }
}

/**
 * عنصر صف الصلاة في القائمة العامودية مع خيارات الجرس وتجاوز الصامت واسم الأذان
 */
@Composable
fun PrayerSettingRowCard(
    prayerType: PrayerType,
    timeFormatted: String,
    bellMode: PrayerBellMode,
    overrideSilent: Boolean,
    muazzinVoice: MuazzinVoice,
    onBellModeChange: (PrayerBellMode) -> Unit,
    onOverrideSilentChange: (Boolean) -> Unit,
    onSelectMuazzinClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("prayer_row_${prayerType.id}"),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // الصف العلوي: اسم الصلاة ووقتها في اليمين، وخيارات الجرس الثلاثة في اليسار
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
                        shape = RoundedCornerShape(10.dp),
                        color = when (prayerType) {
                            PrayerType.FAJR -> Color(0xFF1E3A8A).copy(alpha = 0.15f)
                            PrayerType.SUNRISE -> Color(0xFFD97706).copy(alpha = 0.15f)
                            PrayerType.DHUHR -> Color(0xFF047857).copy(alpha = 0.15f)
                            PrayerType.ASR -> Color(0xFFB45309).copy(alpha = 0.15f)
                            PrayerType.MAGHRIB -> Color(0xFFB91C1C).copy(alpha = 0.15f)
                            PrayerType.ISHA -> Color(0xFF312E81).copy(alpha = 0.15f)
                            PrayerType.MIDNIGHT -> Color(0xFF0F172A).copy(alpha = 0.15f)
                            PrayerType.LAST_THIRD -> Color(0xFF4C1D95).copy(alpha = 0.15f)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when (prayerType) {
                                    PrayerType.FAJR -> Icons.Default.WbTwilight
                                    PrayerType.SUNRISE -> Icons.Default.WbSunny
                                    PrayerType.DHUHR -> Icons.Default.LightMode
                                    PrayerType.ASR -> Icons.Default.Brightness5
                                    PrayerType.MAGHRIB -> Icons.Default.NightsStay
                                    PrayerType.ISHA -> Icons.Default.DarkMode
                                    PrayerType.MIDNIGHT -> Icons.Default.Bedtime
                                    PrayerType.LAST_THIRD -> Icons.Default.Stars
                                },
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Column {
                        Text(
                            text = prayerType.arabicName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = timeFormatted,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // خيار الجرس في أقصى اليسار: 3 خيارات (صامت | اهتزاز | صوت أذان)
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = bellMode == PrayerBellMode.SILENT,
                        onClick = { onBellModeChange(PrayerBellMode.SILENT) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                        icon = { Icon(Icons.Default.NotificationsOff, contentDescription = "صامت", modifier = Modifier.size(16.dp)) }
                    ) {
                        Text("صامت", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    SegmentedButton(
                        selected = bellMode == PrayerBellMode.VIBRATE,
                        onClick = { onBellModeChange(PrayerBellMode.VIBRATE) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                        icon = { Icon(Icons.Default.Vibration, contentDescription = "اهتزاز", modifier = Modifier.size(16.dp)) }
                    ) {
                        Text("اهتزاز", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    SegmentedButton(
                        selected = bellMode == PrayerBellMode.AZAN,
                        onClick = { onBellModeChange(PrayerBellMode.AZAN) },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                        icon = { Icon(Icons.Default.NotificationsActive, contentDescription = "صوت أذان", modifier = Modifier.size(16.dp)) }
                    ) {
                        Text("أذان", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // خيار: العمل حتى لو كان الجوال صامت
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "العمل حتى لو كان الجوال صامت",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Switch(
                    checked = overrideSilent,
                    onCheckedChange = onOverrideSilentChange,
                    modifier = Modifier.scaleSwitch()
                )
            }

            // خيار: اسم صوت الأذان / المؤذن
            if (bellMode == PrayerBellMode.AZAN) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSelectMuazzinClick)
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
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = EmeraldDeep,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = muazzinVoice.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = muazzinVoice.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "اختيار المؤذن",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * نافذة اختيار صوت المؤذن
 */
@Composable
fun MuazzinVoicePickerDialog(
    currentVoiceId: String,
    prayerName: String,
    onVoiceSelected: (String) -> Unit,
    onPreviewClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "صوت أذان $prayerName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Text(
                    text = "اختر المؤذن المفضل ويمكنك الاستماع إلى نبرة التنبيه قبل الاعتماد:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(MuazzinCatalog.allVoices, key = { it.id }) { voice ->
                        val isSelected = voice.id == currentVoiceId
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) EmeraldDeep.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = if (isSelected) CardDefaults.outlinedCardBorder() else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onVoiceSelected(voice.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { onVoiceSelected(voice.id) }
                                    )
                                    Column {
                                        Text(voice.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text(voice.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                IconButton(
                                    onClick = { onPreviewClick(voice.id) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "استماع",
                                        tint = EmeraldDeep
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * نافذة إضافة منبه مخصص بجميع الخيارات الستة
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddCustomPrayerAlarmDialog(
    onSave: (
        name: String,
        prayers: List<String>,
        isBefore: Boolean,
        offsetMins: Int,
        days: List<Int>,
        soundType: String
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var alarmName by remember { mutableStateOf("تذكير قبل الأذان") }
    val selectedPrayers = remember { mutableStateListOf("FAJR", "DHUHR", "ASR", "MAGHRIB", "ISHA") }
    var isBeforeAzan by remember { mutableStateOf(true) }
    var offsetMinutes by remember { mutableIntStateOf(10) }
    val selectedDays = remember { mutableStateListOf(1, 2, 3, 4, 5, 6, 7) }
    var soundType by remember { mutableStateOf("تكبيرات الأذان") }

    val allDays = listOf(
        1 to "الأحد",
        2 to "الإثنين",
        3 to "الثلاثاء",
        4 to "الأربعاء",
        5 to "الخميس",
        6 to "الجمعة",
        7 to "السبت"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "إضافة منبه مخصص",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )

                // 1. اسم المنبه
                OutlinedTextField(
                    value = alarmName,
                    onValueChange = { alarmName = it },
                    label = { Text("اسم المنبه") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 2. الصلوات المشمولة
                Text("اختر الصلوات:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PrayerType.obligatoryPrayers.forEach { p ->
                        val isChosen = selectedPrayers.contains(p.id)
                        FilterChip(
                            selected = isChosen,
                            onClick = {
                                if (isChosen) selectedPrayers.remove(p.id) else selectedPrayers.add(p.id)
                            },
                            label = { Text(p.arabicName) }
                        )
                    }
                }

                // 3 & 4. قبل أو بعد الأذان + الدقائق
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                        SegmentedButton(
                            selected = isBeforeAzan,
                            onClick = { isBeforeAzan = true },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                        ) {
                            Text("قبل الأذان")
                        }
                        SegmentedButton(
                            selected = !isBeforeAzan,
                            onClick = { isBeforeAzan = false },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                        ) {
                            Text("بعد الأذان")
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("بـ :", style = MaterialTheme.typography.labelMedium)
                    listOf(5, 10, 15, 20, 30).forEach { m ->
                        FilterChip(
                            selected = offsetMinutes == m,
                            onClick = { offsetMinutes = m },
                            label = { Text("$m د") }
                        )
                    }
                }

                // 5. أيام الأسبوع
                Text("أيام التكرار:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    allDays.forEach { (d, name) ->
                        val active = selectedDays.contains(d)
                        FilterChip(
                            selected = active,
                            onClick = {
                                if (active) selectedDays.remove(d) else selectedDays.add(d)
                            },
                            label = { Text(name, fontSize = 11.sp) }
                        )
                    }
                }

                // 6. نوع الصوت
                Text("نوع الصوت:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("تكبيرات الأذان", "تنبيه هادئ", "أذان كامل", "اهتزاز فقط").forEach { snd ->
                        FilterChip(
                            selected = soundType == snd,
                            onClick = { soundType = snd },
                            label = { Text(snd, fontSize = 12.sp) }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("إلغاء") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(
                                alarmName,
                                selectedPrayers.toList(),
                                isBeforeAzan,
                                offsetMinutes,
                                selectedDays.toList(),
                                soundType
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep)
                    ) {
                        Text("حفظ المنبه")
                    }
                }
            }
        }
    }
}

private fun Modifier.scaleSwitch(): Modifier = this.size(width = 44.dp, height = 28.dp)
