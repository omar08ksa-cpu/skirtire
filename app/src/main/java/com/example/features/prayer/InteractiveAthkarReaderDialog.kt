package com.example.features.prayer

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.prayer.DhikrItem
import com.example.core.prayer.UmmAlQuraCalculator
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldSoft
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.MintAccent

@Composable
fun InteractiveAthkarReaderDialog(
    categoryCode: String,
    categoryTitle: String,
    items: List<DhikrItem>,
    onFinish: () -> Unit,
    onDismiss: () -> Unit
) {
    if (items.isEmpty()) {
        onDismiss()
        return
    }

    val view = LocalView.current
    var currentIndex by remember { mutableIntStateOf(0) }
    var currentItemCount by remember { mutableIntStateOf(0) }
    val completedItemIds = remember { mutableStateListOf<String>() }
    var isSessionCompleted by remember { mutableStateOf(false) }

    val currentDhikr = items[currentIndex.coerceIn(0, items.size - 1)]
    val isMorning = categoryCode == "MORNING"

    val headerGradient = if (isMorning) {
        listOf(Color(0xFF0F766E), Color(0xFF14B8A6))
    } else {
        listOf(Color(0xFF1E1B4B), Color(0xFF312E81))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .testTag("athkar_reader_dialog"),
            color = MaterialTheme.colorScheme.background
        ) {
            if (isSessionCompleted) {
                // شاشة الاحتفال بإتمام الأذكار كاملة
                AthkarCompletionView(
                    categoryTitle = categoryTitle,
                    isMorning = isMorning,
                    totalItems = items.size,
                    onDone = {
                        onFinish()
                        onDismiss()
                    }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    // 1. الشريط العلوي: العنوان، العداد، زر الإغلاق، وشريط التقدم
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                        shape = CircleShape,
                                        color = if (isMorning) GoldSoft.copy(alpha = 0.2f) else Color(0xFF6366F1).copy(alpha = 0.2f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (isMorning) Icons.Default.WbSunny else Icons.Default.NightlightRound,
                                                contentDescription = null,
                                                tint = if (isMorning) GoldWarm else Color(0xFFA5B4FC),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Column {
                                        Text(
                                            text = categoryTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            text = "الذكر ${UmmAlQuraCalculator.toArabicDigits((currentIndex + 1).toString())} من ${UmmAlQuraCalculator.toArabicDigits(items.size.toString())}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.testTag("close_athkar_reader_btn")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "إغلاق")
                                }
                            }

                            // شريط التقدم العام للأذكار
                            val overallProgress = (completedItemIds.size.toFloat() / items.size.toFloat()).coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { overallProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = if (isMorning) EmeraldDeep else Color(0xFF6366F1),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }

                    // 2. بطاقة الذكر المركزية التفاعلية
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // شارة اسم الذكر والمطلوب
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isMorning) EmeraldDeep.copy(alpha = 0.12f) else Color(0xFF6366F1).copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = currentDhikr.title,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (isMorning) EmeraldDeep else Color(0xFF818CF8),
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        val reqText = if (currentDhikr.targetCount == 1) {
                                            "مرة واحدة"
                                        } else {
                                            "${UmmAlQuraCalculator.toArabicDigits(currentDhikr.targetCount.toString())} مرات"
                                        }
                                        Text(
                                            text = "المطلوب: $reqText",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                // نص الذكر القرآني والنبوي الشريف
                                Text(
                                    text = currentDhikr.arabicText,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontSize = 19.sp,
                                        lineHeight = 32.sp
                                    ),
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                )

                                // فضل الذكر أو الملاحظة إن وجدت
                                if (currentDhikr.virtueOrNote.isNotEmpty()) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.AutoAwesome,
                                                contentDescription = null,
                                                tint = GoldWarm,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = currentDhikr.virtueOrNote,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                lineHeight = 20.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // الجزء التفاعلي:
                                // إذا كان المطلوب مرة واحدة: زر علامة صح للانتقال للتالي مباشرة
                                // إذا كان أكثر من مرة: نص يوضح كم خلصت وكم باقي لك + زر التسبيح التفاعلي
                                if (currentDhikr.targetCount == 1) {
                                    val isCurrentCompleted = completedItemIds.contains(currentDhikr.id)

                                    Button(
                                        onClick = {
                                            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                            if (!completedItemIds.contains(currentDhikr.id)) {
                                                completedItemIds.add(currentDhikr.id)
                                            }
                                            // الانتقال للذكر التالي أو إتمام الجلسة
                                            if (currentIndex < items.size - 1) {
                                                currentIndex++
                                                currentItemCount = 0
                                            } else {
                                                isSessionCompleted = true
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                            .testTag("dhikr_single_complete_btn"),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isMorning) EmeraldDeep else Color(0xFF4F46E5)
                                        )
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(22.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (currentIndex < items.size - 1) "قرأت هذا الذكر (التالي ✓)" else "إتمام وقراءة آخر ذكر ✓",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                } else {
                                    // الذكر مطلوب أكثر من مرة (3، 10، 100)
                                    val remaining = (currentDhikr.targetCount - currentItemCount).coerceAtLeast(0)
                                    val isTargetReached = currentItemCount >= currentDhikr.targetCount

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // النص المطلوب بالحرف: يوضح كم خلصت وكم باقي لك
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isTargetReached) {
                                                EmeraldDeep.copy(alpha = 0.15f)
                                            } else {
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                            }
                                        ) {
                                            Text(
                                                text = if (isTargetReached) {
                                                    "اكتمل العدد بنجاح! (${UmmAlQuraCalculator.toArabicDigits(currentDhikr.targetCount.toString())} من ${UmmAlQuraCalculator.toArabicDigits(currentDhikr.targetCount.toString())})"
                                                } else {
                                                    "تم: ${UmmAlQuraCalculator.toArabicDigits(currentItemCount.toString())} من ${UmmAlQuraCalculator.toArabicDigits(currentDhikr.targetCount.toString())} — المتبقي: ${UmmAlQuraCalculator.toArabicDigits(remaining.toString())}"
                                                },
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isTargetReached) EmeraldDeep else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                            )
                                        }

                                        // زر التسبيح والعد التفاعلي
                                        if (!isTargetReached) {
                                            Surface(
                                                modifier = Modifier
                                                    .size(110.dp)
                                                    .clickable {
                                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                                        val next = currentItemCount + 1
                                                        currentItemCount = next
                                                        if (next >= currentDhikr.targetCount) {
                                                            if (!completedItemIds.contains(currentDhikr.id)) {
                                                                completedItemIds.add(currentDhikr.id)
                                                            }
                                                        }
                                                    }
                                                    .testTag("dhikr_counter_tap_btn"),
                                                shape = CircleShape,
                                                color = if (isMorning) EmeraldDeep else Color(0xFF4F46E5),
                                                shadowElevation = 6.dp
                                            ) {
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier.padding(8.dp)
                                                ) {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        Text(
                                                            text = UmmAlQuraCalculator.toArabicDigits(remaining.toString()),
                                                            style = MaterialTheme.typography.headlineMedium,
                                                            fontWeight = FontWeight.Black,
                                                            color = Color.White
                                                        )
                                                        Text(
                                                            text = "اضغط للعد",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color.White.copy(alpha = 0.85f),
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            // اكتمل العدد لهذا الذكر — زر الانتقال للتالي
                                            Button(
                                                onClick = {
                                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                                    if (!completedItemIds.contains(currentDhikr.id)) {
                                                        completedItemIds.add(currentDhikr.id)
                                                    }
                                                    if (currentIndex < items.size - 1) {
                                                        currentIndex++
                                                        currentItemCount = 0
                                                    } else {
                                                        isSessionCompleted = true
                                                    }
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(56.dp)
                                                    .testTag("dhikr_next_after_target_btn"),
                                                shape = RoundedCornerShape(16.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF10B981)
                                                )
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(22.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = if (currentIndex < items.size - 1) "اكتمل الذكر — الانتقال للتالي ➔" else "اكتملت جميع الأذكار — إنهاء 🎉",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. شريط التنقل السفلي: السابق، إعادة تعيين، التالي
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    if (currentIndex > 0) {
                                        currentIndex--
                                        currentItemCount = 0
                                    }
                                },
                                enabled = currentIndex > 0,
                                modifier = Modifier.testTag("athkar_prev_btn")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("السابق", fontWeight = FontWeight.Bold)
                            }

                            IconButton(
                                onClick = {
                                    currentItemCount = 0
                                    completedItemIds.remove(currentDhikr.id)
                                },
                                modifier = Modifier.testTag("athkar_reset_current_btn")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "إعادة عد هذا الذكر", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            TextButton(
                                onClick = {
                                    if (!completedItemIds.contains(currentDhikr.id)) {
                                        completedItemIds.add(currentDhikr.id)
                                    }
                                    if (currentIndex < items.size - 1) {
                                        currentIndex++
                                        currentItemCount = 0
                                    } else {
                                        isSessionCompleted = true
                                    }
                                },
                                modifier = Modifier.testTag("athkar_next_btn")
                            ) {
                                Text(
                                    text = if (currentIndex < items.size - 1) "تخطي / التالي" else "إنهاء",
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AthkarCompletionView(
    categoryTitle: String,
    isMorning: Boolean,
    totalItems: Int,
    onDone: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    if (isMorning) {
                        listOf(Color(0xFF0F766E), Color(0xFF042F2C))
                    } else {
                        listOf(Color(0xFF1E1B4B), Color(0xFF0F0E2A))
                    }
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(26.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isMorning) GoldWarm.copy(alpha = 0.2f) else Color(0xFF818CF8).copy(alpha = 0.2f),
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Verified,
                            contentDescription = null,
                            tint = if (isMorning) GoldWarm else Color(0xFF818CF8),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Text(
                    text = "تقبّل الله طاعتك!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "أتممت قراءة $categoryTitle كاملة (${UmmAlQuraCalculator.toArabicDigits(totalItems.toString())} ذكراً مأثوراً).\nاللهم اجعلها حفظاً وبركة ونوراً في يومك وليلتك.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = EmeraldDeep.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldDeep, modifier = Modifier.size(24.dp))
                        Text(
                            text = "تم توثيق إنجاز العادة اليومية تلقائياً في صفحة العادات ✅",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDeep
                        )
                    }
                }

                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("athkar_completion_done_btn"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isMorning) EmeraldDeep else Color(0xFF4F46E5)
                    )
                ) {
                    Text("الحمد لله (إنهاء)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}
