package com.example.features.calendar

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.prayer.UmmAlQuraCalculator
import com.example.data.AppEventEntity
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.MintAccent
import com.example.ui.theme.PriorityHighRed
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * قسم العدادات التنازلية للأحداث في التقويم (المطلب ٢):
 * يعرض كم باقي على حدث معين بالثواني والدقائق والساعات والأيام.
 */
@Composable
fun EventCountdownSection(
    events: List<AppEventEntity>,
    onToggleCountdown: (AppEventEntity) -> Unit,
    onSelectEvent: (AppEventEntity) -> Unit,
    onAddNewEventWithCountdown: () -> Unit,
    modifier: Modifier = Modifier
) {
    // نبض دوري كل ثانية لتحديث العدادات التنازلية مباشرة
    var currentMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            currentMillis = System.currentTimeMillis()
        }
    }

    val countdownEvents = remember(events) {
        events.filter { it.isCountdown }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                            imageVector = Icons.Outlined.HourglassBottom,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "العداد التنازلي للأحداث والمواعيد ⏳",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = if (countdownEvents.isNotEmpty()) {
                                UmmAlQuraCalculator.toArabicDigits("لديك ${countdownEvents.size} أحداث مفعلة بالعد التنازلي")
                            } else {
                                "فعل العد التنازلي لأي حدث لمعرفة كم باقي عليه بالدقة"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FilledTonalButton(
                    onClick = onAddNewEventWithCountdown,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حدث بتنازلي", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (countdownEvents.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.HourglassEmpty,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "لا توجد أحداث مفعلة بالعد التنازلي حالياً. يمكنك الضغط على أي حدث في التقويم وتفعيل خيار «العداد التنازلي» لمعرفة كم باقي عليه لحظة بلحظة!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(countdownEvents, key = { it.id }) { event ->
                        CountdownEventCard(
                            event = event,
                            currentMillis = currentMillis,
                            onToggleCountdown = { onToggleCountdown(event) },
                            onClick = { onSelectEvent(event) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * بطاقة عداد تنازلي لحدث فردي
 */
@Composable
fun CountdownEventCard(
    event: AppEventEntity,
    currentMillis: Long,
    onToggleCountdown: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetMillis = remember(event.dateKey, event.startTime) {
        calculateEventMillis(event.dateKey, event.startTime)
    }

    val diffMillis = targetMillis - currentMillis
    val isPast = diffMillis <= 0

    val remainingDays = (diffMillis / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
    val remainingHours = ((diffMillis / (1000 * 60 * 60)) % 24).coerceAtLeast(0)
    val remainingMinutes = ((diffMillis / (1000 * 60)) % 60).coerceAtLeast(0)
    val remainingSeconds = ((diffMillis / 1000) % 60).coerceAtLeast(0)

    val eventColor = remember(event.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(event.colorHex))
        } catch (_: Exception) {
            EmeraldDeep
        }
    }

    Card(
        modifier = modifier
            .width(260.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPast) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                eventColor.copy(alpha = 0.08f)
            }
        ),
        border = BorderStroke(
            1.5.dp,
            if (isPast) MaterialTheme.colorScheme.outlineVariant else eventColor.copy(alpha = 0.4f)
        )
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
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = eventColor.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = event.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = eventColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleCountdown,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إلغاء العد التنازلي",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Text(
                text = event.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "${event.dateKey} ${if (event.startTime.isNotBlank()) "• ${event.startTime}" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // عرض مربعات العد التنازلي بالأرقام
            if (isPast) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = GoldWarm.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎉 حان موعد الحدث أو مضى وقته",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = GoldWarm
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CountdownUnitBox(value = remainingDays.toInt(), unit = "يوم", color = eventColor)
                    CountdownUnitBox(value = remainingHours.toInt(), unit = "ساعة", color = eventColor)
                    CountdownUnitBox(value = remainingMinutes.toInt(), unit = "دقيقة", color = eventColor)
                    CountdownUnitBox(value = remainingSeconds.toInt(), unit = "ثانية", color = eventColor)
                }
            }
        }
    }
}

@Composable
private fun CountdownUnitBox(
    value: Int,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier.width(52.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = UmmAlQuraCalculator.toArabicDigits(String.format(Locale.US, "%02d", value)),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * حساب الوقت بالملي ثانية لتاريخ ووقت الحدث
 */
fun calculateEventMillis(dateKey: String, startTime: String): Long {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        val timeStr = if (startTime.isNotBlank()) startTime else "00:00"
        sdf.parse("$dateKey $timeStr")?.time ?: System.currentTimeMillis()
    } catch (_: Exception) {
        try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateKey)?.time ?: System.currentTimeMillis()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }
}
