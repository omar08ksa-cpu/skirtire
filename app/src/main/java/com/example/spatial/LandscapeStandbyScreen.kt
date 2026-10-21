package com.example.spatial

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.prayer.UmmAlQuraCalculator
import com.example.features.DailyManagerUiState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * وضع الاستعداد الأفقي (Landscape Standby Mode)
 * يتحول إليه التطبيق تلقائياً عند تدوير الجهاز أفقياً
 * يعرض ساعة رقمية كبيرة ومواقيت الصلاة ومؤقت التركيز النشط
 */
@Composable
fun LandscapeStandbyScreen(
    uiState: DailyManagerUiState,
    modifier: Modifier = Modifier
) {
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val timeStr = remember(currentTimeMillis) { timeFormat.format(Date(currentTimeMillis)) }
    val nextSlot = uiState.prayerSchedule.nextPrayer

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F14)) // خلفية داكنة مريحة للعين
            .padding(24.dp)
            .testTag("landscape_standby_screen"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // الجانب الأيمن: الساعة الكبيرة والتاريخ
            Column(
                modifier = Modifier.weight(1.3f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = UmmAlQuraCalculator.toArabicDigits(timeStr),
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "${uiState.prayerSchedule.hijriFormatted} • تقويم أم القرى",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF34D399) // Emerald
                )
            }

            // الجانب الأيسر: الصلاة القادمة ومؤقت التركيز
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                // بطاقة الصلاة القادمة
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF131D26),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2D3D))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "الصلاة القادمة: ${nextSlot.type.arabicName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = UmmAlQuraCalculator.formatCountdown(uiState.prayerSchedule.remainingSecondsToNext),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFF59E0B) // Amber
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E293B)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mosque,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // مؤشر وضع الاستعداد
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF1E293B).copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF38BDF8))
                        )
                        Text(
                            text = "وضع الاستعداد الأفقي • أدر الجهاز للرجوع للغرف",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}
