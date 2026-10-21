package com.example.features.calendar

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.core.prayer.UmmAlQuraCalculator
import com.example.data.LifeMilestoneEntity
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.MintAccent
import com.example.ui.theme.PriorityHighRed
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * طريقة العرض الثانية الجديدة: عرض حياتي كاملة من يوم ولدت إلى اللحظة هذه مع اللحظات المهمة
 * (Life in Weeks & Milestones):
 * - إحصائيات دقيقة: كم عشت من السنوات والأشهر والأسابيع والأيام حتى اللحظة.
 * - سجل اللحظات المهمة في الحياة (الميلاد، التخرج، الوظيفة، الزواج، الإنجازات) وحساب عمرك وقتها.
 * - شبكة أسابيع الحياة ومراحل العمر.
 */
@Composable
fun LifeInWeeksView(
    userBirthDateKey: String,
    milestones: List<LifeMilestoneEntity>,
    onUpdateBirthDate: (String) -> Unit,
    onSaveMilestone: (LifeMilestoneEntity) -> Unit,
    onDeleteMilestone: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showEditBirthDateDialog by remember { mutableStateOf(false) }
    var showAddMilestoneDialog by remember { mutableStateOf(false) }
    var selectedMilestoneForDetail by remember { mutableStateOf<LifeMilestoneEntity?>(null) }

    // حساب إحصائيات العمر والحياة
    val lifeStats = remember(userBirthDateKey) {
        calculateLifeStatistics(userBirthDateKey)
    }

    // جميع اللحظات المهمة بما في ذلك لحظة الميلاد التلقائية
    val allMilestones = remember(milestones, userBirthDateKey) {
        val list = mutableListOf<LifeMilestoneEntity>()
        // اللحظة الأولى: الميلاد
        list.add(
            LifeMilestoneEntity(
                id = "milestone_birth",
                title = "ولادتي وبداية رحلتي في الحياة 👶",
                description = "اليوم الذي رأيت فيه النور وبدأت قصة حياتي على هذه الأرض.",
                dateKey = userBirthDateKey,
                emoji = "👶",
                category = "ولادة",
                ageAtMilestoneYears = 0f
            )
        )
        // دمج اللحظات المضافة من المستخدم وترتيبها زمنياً
        list.addAll(milestones.filter { it.id != "milestone_birth" })
        list.sortedBy { it.dateKey }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("life_in_weeks_content"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. بطاقة تاريخ الميلاد وتفاصيل العمر الحالي
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldDeep),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Cake,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "رحلة حياتي حتى اللحظة 🌱",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "تاريخ ميلادك: ${lifeStats.formattedBirthDate}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = { showEditBirthDateDialog = true },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = Color.White.copy(alpha = 0.22f),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تعديل الميلاد", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // العمر بالأرقام
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "عمرك الآن بفضل الله",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                            Text(
                                text = UmmAlQuraCalculator.toArabicDigits(
                                    "${lifeStats.yearsLived} سنة و ${lifeStats.monthsRemainder} أشهر و ${lifeStats.daysRemainder} يوماً"
                                ),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }

                    // إحصائيات الأسابيع والأيام
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        LifeStatBox(
                            value = "${lifeStats.weeksLived}",
                            label = "أسبوعاً عشتها حتى الآن",
                            icon = Icons.Outlined.Timeline
                        )
                        LifeStatBox(
                            value = "${lifeStats.daysLived}",
                            label = "يوماً في هذه الحياة",
                            icon = Icons.Default.Today
                        )
                        LifeStatBox(
                            value = "${lifeStats.estimatedWeeksRemaining}",
                            label = "أسبوعاً متبقية (تقدير ٨٠)",
                            icon = Icons.Default.HourglassTop
                        )
                    }

                    // شريط نسبة انقضاء الحياة المقدرة
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = UmmAlQuraCalculator.toArabicDigits("نسبة ما انقضى من العمر المقدر (٨٠ سنة): ${lifeStats.lifePercent}%"),
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { lifeStats.lifePercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = GoldWarm,
                            trackColor = Color.White.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }

        // 2. بطاقة اللحظات المهمة في حياتي (Key Milestones)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "اللحظات المهمة في حياتي 🌟",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = UmmAlQuraCalculator.toArabicDigits("مسجّل ${allMilestones.size} لحظة ومحطة فارقة"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Button(
                            onClick = { showAddMilestoneDialog = true },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة لحظة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // خط زمني للّحظات المهمة
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        allMilestones.forEachIndexed { index, milestone ->
                            val ageYearsAtMilestone = calculateAgeAtDate(userBirthDateKey, milestone.dateKey)
                            val isLast = index == allMilestones.lastIndex

                            MilestoneTimelineItem(
                                milestone = milestone,
                                ageYears = ageYearsAtMilestone,
                                isBirth = milestone.id == "milestone_birth",
                                isLast = isLast,
                                onDelete = { onDeleteMilestone(milestone.id) },
                                onClick = { selectedMilestoneForDetail = milestone }
                            )
                        }
                    }
                }
            }
        }

        // 3. خريطة أسابيع مراحل العمر (Decades of Life)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Flag,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "مراحل العمر وخريطة الأسابيع",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Text(
                        text = "كل مربع أو مرحلة يمثل جزءاً من رحلتك. اغتنم كل أسبوع في طاعة الله والإنجاز.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // مراحل العمر الستة الرئيسية
                    val stages = listOf(
                        LifeStageInfo("الطفولة المبكرة والابتدائية", 0..10, "🧸", "٥٢٠ أسبوعاً من البراءة والتعلم الأولى"),
                        LifeStageInfo("المتوسطة والثانوية وبداية الشباب", 10..20, "🎒", "٥٢٠ أسبوعاً من بناء الشخصية والوعي"),
                        LifeStageInfo("الجامعة وبداية العمل والمهنة", 20..30, "🎓", "٥٢٠ أسبوعاً من الانطلاق وتحقيق الذات"),
                        LifeStageInfo("النضج والإنتاجية وبناء الأسرة", 30..40, "💼", "٥٢٠ أسبوعاً من قمة العطاء والتمكين"),
                        LifeStageInfo("الخبرة والقيادة والإنجاز الراسخ", 40..50, "🏛️", "٥٢٠ أسبوعاً من الحكمة والتوجيه"),
                        LifeStageInfo("مرحلة البركة والأثر الدائم", 50..80, "🌱", "مرحلة الأثر المبارك والذكر الطيب")
                    )

                    stages.forEach { stage ->
                        val userAge = lifeStats.yearsLived
                        val isCurrentStage = userAge in stage.ageRange
                        val isCompletedStage = userAge > stage.ageRange.last

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = when {
                                isCurrentStage -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                isCompletedStage -> EmeraldDeep.copy(alpha = 0.08f)
                                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            },
                            border = if (isCurrentStage) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(stage.emoji, fontSize = 24.sp)

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = stage.title,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                        if (isCurrentStage) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primary
                                            ) {
                                                Text(
                                                    text = "أنت هنا الآن 📍",
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        } else if (isCompletedStage) {
                                            Text("✓", color = EmeraldDeep, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }
                                    Text(
                                        text = "${stage.description} (الأعمار ${stage.ageRange.first}-${stage.ageRange.last} سنة)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // وصية نبوية شريفة
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = GoldWarm.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "قال رسول الله ﷺ:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = GoldWarm
                            )
                            Text(
                                text = "«اغْتَنِمْ خَمْسًا قبْلَ خَمْسٍ: شَبابَكَ قَبْلَ هَرَمِكَ، وَصِحَّتَكَ قَبْلَ سَقَمِكَ، وَغِناءَكَ قَبْلَ فَقْرِكَ، وَفَراغَكَ قَبْلَ شُغْلِكَ، وَحَياتَكَ قَبْلَ مَوْتِكَ».",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }

    // نافذة تعديل تاريخ الميلاد
    if (showEditBirthDateDialog) {
        EditBirthDateDialog(
            currentBirthDate = userBirthDateKey,
            onDismiss = { showEditBirthDateDialog = false },
            onConfirm = { newDate ->
                onUpdateBirthDate(newDate)
                showEditBirthDateDialog = false
            }
        )
    }

    // نافذة إضافة لحظة مهمة
    if (showAddMilestoneDialog) {
        AddLifeMilestoneDialog(
            userBirthDateKey = userBirthDateKey,
            onDismiss = { showAddMilestoneDialog = false },
            onConfirm = { milestone ->
                onSaveMilestone(milestone)
                showAddMilestoneDialog = false
            }
        )
    }
}

@Composable
private fun LifeStatBox(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.14f),
        modifier = modifier.width(98.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = UmmAlQuraCalculator.toArabicDigits(value),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                text = label,
                fontSize = 9.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 11.sp
            )
        }
    }
}

@Composable
private fun MilestoneTimelineItem(
    milestone: LifeMilestoneEntity,
    ageYears: Float,
    isBirth: Boolean,
    isLast: Boolean,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // أيقونة اللحظة
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (isBirth) EmeraldDeep.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(text = milestone.emoji, fontSize = 20.sp)
        }

        // تفاصيل اللحظة
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = milestone.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                if (!isBirth) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "حذف اللحظة",
                            tint = PriorityHighRed.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (isBirth) "عمرك: ٠ سنة" else UmmAlQuraCalculator.toArabicDigits("عمرك وقتها: ${String.format(Locale.US, "%.1f", ageYears)} سنة"),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = milestone.dateKey,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (milestone.description.isNotBlank()) {
                Text(
                    text = milestone.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

/**
 * نافذة حوار تعديل تاريخ الميلاد
 */
@Composable
private fun EditBirthDateDialog(
    currentBirthDate: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var birthDateText by remember { mutableStateOf(currentBirthDate) }
    var year by remember {
        mutableIntStateOf(
            currentBirthDate.split("-").getOrNull(0)?.toIntOrNull() ?: 2000
        )
    }
    var month by remember {
        mutableIntStateOf(
            currentBirthDate.split("-").getOrNull(1)?.toIntOrNull() ?: 1
        )
    }
    var day by remember {
        mutableIntStateOf(
            currentBirthDate.split("-").getOrNull(2)?.toIntOrNull() ?: 1
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تعديل تاريخ الميلاد 🎂",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                Text(
                    text = "أدخل تاريخ ميلادك لحساب أسابيع حياتك ولحظاتك المهمة بدقة متناهية:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = day.toString(),
                        onValueChange = { day = it.toIntOrNull()?.coerceIn(1, 31) ?: day },
                        label = { Text("اليوم") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = month.toString(),
                        onValueChange = { month = it.toIntOrNull()?.coerceIn(1, 12) ?: month },
                        label = { Text("الشهر") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = year.toString(),
                        onValueChange = { year = it.toIntOrNull()?.coerceIn(1920, 2026) ?: year },
                        label = { Text("السنة") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.4f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val formatted = String.format(Locale.US, "%04d-%02d-%02d", year, month, day)
                            onConfirm(formatted)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("حفظ تاريخ الميلاد", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * نافذة حوار إضافة لحظة فارقة في الحياة
 */
@Composable
private fun AddLifeMilestoneDialog(
    userBirthDateKey: String,
    onDismiss: () -> Unit,
    onConfirm: (LifeMilestoneEntity) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dateKey by remember { mutableStateOf(UmmAlQuraCalculator.todayDateKey()) }
    var selectedEmoji by remember { mutableStateOf("🎓") }
    var category by remember { mutableStateOf("دراسة") }

    val emojis = listOf("🎓", "💼", "💍", "👶", "🕋", "🎒", "🏠", "🚀", "🌟", "🏆", "❤️", "🩺")
    val categories = listOf("دراسة", "مهنة", "عائلة", "ديني", "إنجاز", "شخصي")

    val ageYears = remember(dateKey, userBirthDateKey) {
        calculateAgeAtDate(userBirthDateKey, dateKey)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
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
                        text = "إضافة لحظة مهمة في حياتي 🌟",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان اللحظة (مثال: تخرجي من الجامعة) *") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dateKey,
                    onValueChange = { dateKey = it },
                    label = { Text("تاريخ اللحظة (YYYY-MM-DD)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // عرض العمر المحسوب
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = UmmAlQuraCalculator.toArabicDigits("عمرك في هذه اللحظة: ${String.format(Locale.US, "%.1f", ageYears)} سنة"),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                // اختيار الرمز التعبيري
                Text("اختر رمز اللحظة:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(emojis) { em ->
                        Surface(
                            shape = CircleShape,
                            color = if (selectedEmoji == em) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = if (selectedEmoji == em) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
                            modifier = Modifier
                                .size(40.dp)
                                .clickable { selectedEmoji = em }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(em, fontSize = 20.sp)
                            }
                        }
                    }
                }

                // اختيار الفئة
                Text("المجال:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = category == cat,
                            onClick = { category = cat },
                            label = { Text(cat, fontSize = 12.sp) }
                        )
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("خاطرة أو تفاصيل عن هذا اليوم (اختياري)") },
                    maxLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إلغاء")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onConfirm(
                                    LifeMilestoneEntity(
                                        title = title.trim(),
                                        description = description.trim(),
                                        dateKey = dateKey.trim(),
                                        emoji = selectedEmoji,
                                        category = category,
                                        ageAtMilestoneYears = ageYears
                                    )
                                )
                            }
                        },
                        enabled = title.isNotBlank(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("حفظ اللحظة", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

data class LifeStatistics(
    val formattedBirthDate: String,
    val yearsLived: Int,
    val monthsRemainder: Int,
    val daysRemainder: Int,
    val weeksLived: Long,
    val daysLived: Long,
    val estimatedWeeksRemaining: Long,
    val lifePercent: Int
)

data class LifeStageInfo(
    val title: String,
    val ageRange: IntRange,
    val emoji: String,
    val description: String
)

fun calculateLifeStatistics(birthDateKey: String): LifeStatistics {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val birthDate = sdf.parse(birthDateKey) ?: Date()
        val now = Date()

        val birthCal = Calendar.getInstance().apply { time = birthDate }
        val nowCal = Calendar.getInstance().apply { time = now }

        val diffMillis = (now.time - birthDate.time).coerceAtLeast(0)
        val daysLived = diffMillis / (1000 * 60 * 60 * 24)
        val weeksLived = daysLived / 7

        var years = nowCal.get(Calendar.YEAR) - birthCal.get(Calendar.YEAR)
        var months = nowCal.get(Calendar.MONTH) - birthCal.get(Calendar.MONTH)
        var days = nowCal.get(Calendar.DAY_OF_MONTH) - birthCal.get(Calendar.DAY_OF_MONTH)

        if (days < 0) {
            months--
            days += 30
        }
        if (months < 0) {
            years--
            months += 12
        }

        // تقدير متوسط عمر 80 سنة = 80 * 52 = 4160 أسبوع
        val totalEstimatedWeeks = 80 * 52L
        val estimatedWeeksRemaining = (totalEstimatedWeeks - weeksLived).coerceAtLeast(0)
        val percent = ((weeksLived.toFloat() / totalEstimatedWeeks.toFloat()) * 100f).toInt().coerceIn(0, 100)

        val displaySdf = SimpleDateFormat("d MMMM yyyy", Locale("ar"))
        val formattedBirth = displaySdf.format(birthDate)

        LifeStatistics(
            formattedBirthDate = formattedBirth,
            yearsLived = years.coerceAtLeast(0),
            monthsRemainder = months.coerceAtLeast(0),
            daysRemainder = days.coerceAtLeast(0),
            weeksLived = weeksLived,
            daysLived = daysLived,
            estimatedWeeksRemaining = estimatedWeeksRemaining,
            lifePercent = percent
        )
    } catch (_: Exception) {
        LifeStatistics(
            formattedBirthDate = birthDateKey,
            yearsLived = 24,
            monthsRemainder = 0,
            daysRemainder = 0,
            weeksLived = 1248,
            daysLived = 8760,
            estimatedWeeksRemaining = 2912,
            lifePercent = 30
        )
    }
}

fun calculateAgeAtDate(birthDateKey: String, targetDateKey: String): Float {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val birthDate = sdf.parse(birthDateKey) ?: return 0f
        val targetDate = sdf.parse(targetDateKey) ?: return 0f
        val diffMillis = targetDate.time - birthDate.time
        val years = diffMillis.toDouble() / (1000.0 * 60 * 60 * 24 * 365.25)
        years.toFloat().coerceAtLeast(0f)
    } catch (_: Exception) {
        0f
    }
}
