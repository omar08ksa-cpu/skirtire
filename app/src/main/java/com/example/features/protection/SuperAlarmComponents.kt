package com.example.features.protection

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import com.example.core.protection.AlarmSoundManager
import com.example.core.protection.PlaceMatchResult
import com.example.core.protection.PlacePhotoHelper
import com.example.core.protection.ProtectionManager
import com.example.core.protection.SystemAlarmSound
import com.example.data.BlockScheduleEntity
import com.example.features.DailyManagerViewModel
import java.util.Locale
import java.util.UUID

fun formatDaysOfWeekArabic(daysCsv: String): String {
    val days = daysCsv.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    return when {
        days.size >= 7 -> "كل يوم"
        days == setOf(2, 3, 4, 5, 6) -> "أيام العمل (الأحد - الخميس)"
        days == setOf(1, 7) -> "عطلة نهاية الأسبوع (الجمعة والسبت)"
        days.isEmpty() -> "بدون تكرار"
        else -> {
            val names = listOf(
                1 to "السبت",
                2 to "الأحد",
                3 to "الاثنين",
                4 to "الثلاثاء",
                5 to "الأربعاء",
                6 to "الخميس",
                7 to "الجمعة"
            )
            names.filter { it.first in days }.joinToString("، ") { it.second }
        }
    }
}

fun formatTimeArabic(time24: String): String {
    val parts = time24.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    val period = if (hour < 12) "ص" else "م"
    val displayHour = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    return String.format(Locale.US, "%02d:%02d %s", displayHour, minute, period)
}

fun calculateRemainingTimeText(targetTimeMillis: Long): String {
    val diffMs = targetTimeMillis - System.currentTimeMillis()
    if (diffMs <= 0) return "الآن"
    val totalMins = diffMs / 60_000L
    val hours = totalMins / 60
    val mins = totalMins % 60
    return when {
        hours > 0 && mins > 0 -> "يرن بعد $hours س و $mins د"
        hours > 0 -> "يرن بعد $hours س"
        else -> "يرن بعد $mins د"
    }
}

@Composable
fun SuperAlarmCard(
    alarm: BlockScheduleEntity,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTestNow: () -> Unit
) {
    val context = LocalContext.current
    val nextMillis = remember(alarm.startTime24, alarm.daysOfWeek, alarm.isEnabled) {
        if (alarm.isEnabled) {
            ProtectionManager.calculateNextTriggerMillis(alarm.startTime24, alarm.daysOfWeek)
        } else 0L
    }
    val referenceBitmap: Bitmap? = remember(alarm.id) {
        PlacePhotoHelper.loadReferencePlaceBitmap(context, alarm.id)
    }

    val activeDays = remember(alarm.daysOfWeek) {
        alarm.daysOfWeek.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("super_alarm_card_${alarm.id}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (alarm.isEnabled) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (alarm.isEnabled) 3.dp else 1.dp)
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formatTimeArabic(alarm.startTime24),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (alarm.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ) {
                            Text(
                                text = alarm.startTime24,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = alarm.title.ifBlank { "منبه الاستيقاظ الخارق" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.testTag("switch_alarm_${alarm.id}")
                )
            }

            // أيام الأسبوع: أزرار دائرية ومختصر عربي
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val daysList = listOf(
                    1 to "س", 2 to "ح", 3 to "ن", 4 to "ث", 5 to "ر", 6 to "خ", 7 to "ج"
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    daysList.forEach { (dNum, dLabel) ->
                        val isDayActive = dNum in activeDays
                        Surface(
                            shape = CircleShape,
                            color = if (isDayActive && alarm.isEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else if (isDayActive) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = dLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDayActive && alarm.isEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                if (alarm.isEnabled && nextMillis > 0L) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = calculateRemainingTimeText(nextMillis),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // معلومات التحقق والنغمة
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (alarm.verificationMethod == "LOCAL_IMAGE") {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                            Text(
                                text = "تصوير: ${alarm.placeName}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (referenceBitmap != null) {
                                Image(
                                    bitmap = referenceBitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        } else {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(14.dp))
                            Text(
                                text = "رمز QR: ${alarm.qrReferenceCode}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(14.dp))
                        Text(
                            text = alarm.ringtoneTitle.ifBlank { "نغمة المنبه الافتراضية" },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // أزرار العمليات
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = onTestNow,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("test_alarm_btn_${alarm.id}")
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تجربة الرنين الآن", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("edit_alarm_btn_${alarm.id}")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "تعديل المنبه", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.testTag("delete_alarm_btn_${alarm.id}")
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "حذف المنبه", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

/**
 * نافذة إضافة أو تعديل منبه خارق
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditSuperAlarmModal(
    context: Context,
    existingAlarm: BlockScheduleEntity?,
    onDismiss: () -> Unit,
    onSave: (BlockScheduleEntity) -> Unit,
    onRequestCameraPlace: (alarmId: String, isTestMode: Boolean) -> Unit
) {
    var title by remember { mutableStateOf(existingAlarm?.title ?: "منبه الاستيقاظ الخارق") }
    val initialTimeParts = (existingAlarm?.startTime24 ?: "05:00").split(":")
    var selectedHour by remember { mutableIntStateOf(initialTimeParts.getOrNull(0)?.toIntOrNull() ?: 5) }
    var selectedMinute by remember { mutableIntStateOf(initialTimeParts.getOrNull(1)?.toIntOrNull() ?: 0) }

    val daysSet = remember {
        val raw = existingAlarm?.daysOfWeek ?: "1,2,3,4,5,6,7"
        mutableStateListOf<Int>().apply {
            addAll(raw.split(",").mapNotNull { it.trim().toIntOrNull() }.filter { it in 1..7 })
        }
    }

    var verifyMethod by remember { mutableStateOf(existingAlarm?.verificationMethod ?: "LOCAL_IMAGE") }
    var placeName by remember { mutableStateOf(existingAlarm?.placeName ?: "مغسلة الحمام") }
    var qrCode by remember { mutableStateOf(existingAlarm?.qrReferenceCode ?: "WAKE-QR-2026") }

    val currentAlarmId = remember { existingAlarm?.id ?: UUID.randomUUID().toString() }

    val availableSounds = remember { AlarmSoundManager.getAvailableSystemAlarmSounds(context) }
    var selectedRingtoneUri by remember {
        mutableStateOf(existingAlarm?.ringtoneUri ?: (availableSounds.firstOrNull()?.uriString ?: ""))
    }
    var selectedRingtoneTitle by remember {
        mutableStateOf(existingAlarm?.ringtoneTitle ?: (availableSounds.firstOrNull()?.title ?: "نغمة المنبه الافتراضية"))
    }
    var showSoundPickerModal by remember { mutableStateOf(false) }

    val referenceBitmap: Bitmap? = remember(currentAlarmId) {
        PlacePhotoHelper.loadReferencePlaceBitmap(context, currentAlarmId)
    }

    AlertDialog(
        onDismissRequest = {
            AlarmSoundManager.stopPreview()
            onDismiss()
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Alarm, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    text = if (existingAlarm == null) "إضافة منبه خارق جديد" else "تعديل المنبه الخارق",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("اسم المنبه") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                // محدد الوقت الدقيق (ساعة ودقيقة) مع حساب الوقت المتبقي
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "تحديد الوقت الدقيق للمنبه",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // الساعات
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(onClick = { selectedHour = (selectedHour + 1) % 24 }) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "زيادة الساعة")
                                }
                                Text(
                                    text = String.format(Locale.US, "%02d", selectedHour),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(onClick = { selectedHour = (selectedHour + 23) % 24 }) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "إنقاص الساعة")
                                }
                                Text("ساعة", style = MaterialTheme.typography.labelSmall)
                            }

                            Text(":", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold)

                            // الدقائق
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(onClick = { selectedMinute = (selectedMinute + 1) % 60 }) {
                                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "زيادة الدقيقة")
                                }
                                Text(
                                    text = String.format(Locale.US, "%02d", selectedMinute),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                IconButton(onClick = { selectedMinute = (selectedMinute + 59) % 60 }) {
                                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "إنقاص الدقيقة")
                                }
                                Text("دقيقة", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        val timeString = String.format(Locale.US, "%02d:%02d", selectedHour, selectedMinute)
                        val daysCsv = daysSet.sorted().joinToString(",")
                        val nextMillis = ProtectionManager.calculateNextTriggerMillis(timeString, daysCsv)
                        Text(
                            text = "⏰ ${formatTimeArabic(timeString)} • ${calculateRemainingTimeText(nextMillis)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF059669)
                        )
                    }
                }

                // اختيار أيام الأسبوع
                Text(
                    text = "أيام عمل المنبه",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val weekDays = listOf(
                        1 to "السبت", 2 to "الأحد", 3 to "الاثنين", 4 to "الثلاثاء", 5 to "الأربعاء", 6 to "الخميس", 7 to "الجمعة"
                    )
                    weekDays.forEach { (dNum, dName) ->
                        val isSelected = dNum in daysSet
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (isSelected) {
                                    if (daysSet.size > 1) daysSet.remove(dNum)
                                } else {
                                    daysSet.add(dNum)
                                }
                            },
                            label = { Text(dName.first().toString()) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AssistChip(
                        onClick = {
                            daysSet.clear()
                            daysSet.addAll(listOf(1, 2, 3, 4, 5, 6, 7))
                        },
                        label = { Text("كل يوم", style = MaterialTheme.typography.labelSmall) }
                    )
                    AssistChip(
                        onClick = {
                            daysSet.clear()
                            daysSet.addAll(listOf(2, 3, 4, 5, 6)) // أحد - خميس
                        },
                        label = { Text("أيام العمل", style = MaterialTheme.typography.labelSmall) }
                    )
                    AssistChip(
                        onClick = {
                            daysSet.clear()
                            daysSet.addAll(listOf(1, 7)) // جمعة وسبت
                        },
                        label = { Text("نهاية الأسبوع", style = MaterialTheme.typography.labelSmall) }
                    )
                }

                HorizontalDivider()

                // نغمة المنبه الحقيقية من نظام الجوال
                Text(
                    text = "صوت ونغمة المنبه (نغمات نظام الهاتف)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSoundPickerModal = true }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text(
                                    text = selectedRingtoneTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "نغمة رسمية من جهازك",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = { showSoundPickerModal = true },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("تغيير النغمة")
                        }
                    }
                }

                HorizontalDivider()

                // طريقة إيقاف المنبه الصارمة (Anti-Cheat Verification)
                Text(
                    text = "طريقة إيقاف المنبه الصارمة",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = verifyMethod == "LOCAL_IMAGE",
                        onClick = { verifyMethod = "LOCAL_IMAGE" },
                        label = { Text("تصوير مكان محدد") },
                        leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null) }
                    )
                    FilterChip(
                        selected = verifyMethod == "QR",
                        onClick = { verifyMethod = "QR" },
                        label = { Text("رمز QR") },
                        leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) }
                    )
                }

                if (verifyMethod == "LOCAL_IMAGE") {
                    OutlinedTextField(
                        value = placeName,
                        onValueChange = { placeName = it },
                        label = { Text("اسم المكان (مثال: مغسلة الحمام)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (referenceBitmap != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Image(
                                        bitmap = referenceBitmap.asImageBitmap(),
                                        contentDescription = "الصورة المرجعية للمكان",
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(10.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "الصورة المرجعية محفوظة بنجاح ✅",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                        Text(
                                            text = "يجب مطابقتها بدقة 72%+ لإيقاف المنبه صباحاً.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "⚠️ لم يتم حفظ صورة مرجعية لهذا المكان بعد. التقط صورة للمكان الآن ليتمكن المنبه من مقارنتها صباحاً بدقة فائقة.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onRequestCameraPlace(currentAlarmId, false) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (referenceBitmap == null) "التقاط الصورة الآن" else "تحديث الصورة")
                                }

                                if (referenceBitmap != null) {
                                    OutlinedButton(
                                        onClick = { onRequestCameraPlace(currentAlarmId, true) },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("🧪 فحص الدقة")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = qrCode,
                        onValueChange = { qrCode = it },
                        label = { Text("رمز QR المطلوب مسحه") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    AlarmSoundManager.stopPreview()
                    val timeString = String.format(Locale.US, "%02d:%02d", selectedHour, selectedMinute)
                    val daysCsv = daysSet.sorted().joinToString(",")
                    val alarmToSave = (existingAlarm ?: BlockScheduleEntity(
                        id = currentAlarmId,
                        title = title,
                        scheduleType = "WAKE_ALARM"
                    )).copy(
                        title = title.ifBlank { "منبه الاستيقاظ الخارق" },
                        scheduleType = "WAKE_ALARM",
                        startTime24 = timeString,
                        daysOfWeek = daysCsv,
                        verificationMethod = verifyMethod,
                        placeName = placeName,
                        qrReferenceCode = qrCode,
                        ringtoneUri = selectedRingtoneUri,
                        ringtoneTitle = selectedRingtoneTitle,
                        isEnabled = true
                    )
                    onSave(alarmToSave)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("حفظ المنبه ✅", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = {
                AlarmSoundManager.stopPreview()
                onDismiss()
            }) {
                Text("إلغاء")
            }
        }
    )

    if (showSoundPickerModal) {
        val isPreviewingUri by AlarmSoundManager.isPlayingPreview.collectAsState()

        AlertDialog(
            onDismissRequest = {
                AlarmSoundManager.stopPreview()
                showSoundPickerModal = false
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.LibraryMusic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("اختر نغمة منبه النظام", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "نغمات منبه حقيقية مستخرجة مباشرة من جهازك عبر RingtoneManager:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    availableSounds.forEach { sound ->
                        val isSelected = selectedRingtoneUri == sound.uriString
                        val isCurrentlyPreviewing = isPreviewingUri == sound.uriString

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedRingtoneUri = sound.uriString
                                    selectedRingtoneTitle = sound.title
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
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
                                        onClick = {
                                            selectedRingtoneUri = sound.uriString
                                            selectedRingtoneTitle = sound.title
                                        }
                                    )
                                    Column {
                                        Text(
                                            text = sound.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold
                                        )
                                        if (sound.isDefault) {
                                            Text(
                                                text = "النغمة الافتراضية للجهاز",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        if (isCurrentlyPreviewing) {
                                            AlarmSoundManager.stopPreview()
                                        } else {
                                            AlarmSoundManager.previewSound(context, sound.uriString)
                                        }
                                    }
                                ) {
                                    Icon(
                                        if (isCurrentlyPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = "استماع للنغمة",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        AlarmSoundManager.stopPreview()
                        showSoundPickerModal = false
                    }
                ) {
                    Text("تم الاختيار")
                }
            }
        )
    }
}

/**
 * نافذة التقاط صورة المكان أو فحص دقة المطابقة الصارمة بالكاميرا
 */
@Composable
fun PlaceCameraModal(
    context: Context,
    alarmId: String,
    isTestMode: Boolean,
    onDismiss: () -> Unit,
    onPhotoSaved: () -> Unit
) {
    var previewViewRef by remember { mutableStateOf<androidx.camera.view.PreviewView?>(null) }
    var matchResult by remember { mutableStateOf<PlaceMatchResult?>(null) }
    var isSuccessSaved by remember { mutableStateOf(false) }

    val fallbackSystemLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { captured: Bitmap? ->
        if (captured != null) {
            if (isTestMode) {
                matchResult = PlacePhotoHelper.compareWithReferencePlace(context, captured, alarmId)
            } else {
                PlacePhotoHelper.saveReferencePlaceBitmap(context, captured, alarmId)
                isSuccessSaved = true
                onPhotoSaved()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    if (isTestMode) Icons.Default.CheckCircle else Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isTestMode) "فحص دقة مطابقة المكان 🧪" else "التقاط وحفظ صورة المكان 📷",
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isTestMode) {
                        "وجه الكاميرا نحو نفس زاوية المكان واضغط فحص للتحقق من النسبة المطلوبة (72%+)."
                    } else {
                        "التقط صورة واضحة ومضاءة لمعالم المكان المطلوب (مثل مغسلة الحمام أو المطبخ)."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(230.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                ) {
                    LivePlaceCameraPreview(onPreviewReady = { previewViewRef = it })
                }

                matchResult?.let { res ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (res.isMatch) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "نسبة المطابقة: ${res.similarityPercent}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (res.isMatch) Color(0xFF10B981) else Color(0xFFEF4444)
                            )
                            Text(
                                text = res.feedbackMessage,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                if (isSuccessSaved) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "تم حفظ صورة المكان بنجاح! جاهز للاستيقاظ الخارق ✅",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981),
                            modifier = Modifier.padding(10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Button(
                    onClick = {
                        val captured = previewViewRef?.bitmap
                        if (captured != null) {
                            if (isTestMode) {
                                matchResult = PlacePhotoHelper.compareWithReferencePlace(context, captured, alarmId)
                            } else {
                                PlacePhotoHelper.saveReferencePlaceBitmap(context, captured, alarmId)
                                isSuccessSaved = true
                                onPhotoSaved()
                            }
                        } else {
                            fallbackSystemLauncher.launch(null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(if (isTestMode) Icons.Default.Science else Icons.Default.PhotoCamera, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isTestMode) "فحص المطابقة الآن" else "التقاط وحفظ الصورة")
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}
