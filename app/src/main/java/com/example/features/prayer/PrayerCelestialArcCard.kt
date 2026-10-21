package com.example.features.prayer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.prayer.DailyPrayerSchedule
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldSoft
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.MintAccent
import kotlin.math.cos
import kotlin.math.sin

/**
 * البطاقة الفلكية المنحنية (Celestial Arc Card):
 * - مستطيل فيه "باقي على [اسم الصلاة]"
 * - عداد رقمي عريض وبارز
 * - منحنى سماوي مقوّس فيه شمس لصلوات النهار وقمر لصلوات الليل
 * - أقصى يمين تحت المنحنى: الصلاة السابقة ووقتها
 * - أقصى يسار تحت المنحنى: الصلاة التالية ووقتها
 */
@Composable
fun PrayerCelestialArcCard(
    schedule: DailyPrayerSchedule,
    onOpenSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDaytime = schedule.isDaytime
    val progress = schedule.progressRatio
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0.02f, 0.98f),
        animationSpec = tween(durationMillis = 600),
        label = "celestial_progress"
    )

    // ألوان البطاقة: نهارية دافئة / ليلية كحلية ملكية
    val cardGradient = if (isDaytime) {
        listOf(
            Color(0xFF0F3A2E),
            Color(0xFF07241C),
            Color(0xFF041712)
        )
    } else {
        listOf(
            Color(0xFF0D1B2A),
            Color(0xFF1B263B),
            Color(0xFF0F172A)
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("prayer_celestial_arc_card"),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = cardGradient.first()),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(colors = cardGradient))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // الشريط العلوي داخل البطاقة: اسم المدينة والتاريخ المصغر وبدون جرس
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.12f),
                            modifier = Modifier.size(22.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = if (isDaytime) GoldSoft else MintAccent,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = schedule.city.arabicName,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = schedule.hijriFormatted,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = Color.White.copy(alpha = 0.70f)
                            )
                        }
                    }
                }

                // عنوان باقي على الصلاة + العدّاد الرقمي الأبرز
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "باقي على صلاة ${schedule.nextPrayer.type.arabicName}",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.90f)
                    )

                    Text(
                        text = schedule.countdownFormatted,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = 28.sp,
                            letterSpacing = 1.sp
                        ),
                        fontWeight = FontWeight.Black,
                        color = if (isDaytime) MintAccent else Color(0xFF67E8F9)
                    )
                }

                // المنحنى الفلكي السماوي مع الشمس أو القمر مرفوع ومقصر
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // رسم نجوم خافتة إذا كان ليلاً
                        if (!isDaytime) {
                            val starPoints = listOf(
                                Offset(w * 0.15f, h * 0.25f),
                                Offset(w * 0.28f, h * 0.15f),
                                Offset(w * 0.50f, h * 0.10f),
                                Offset(w * 0.72f, h * 0.18f),
                                Offset(w * 0.85f, h * 0.30f),
                                Offset(w * 0.38f, h * 0.35f),
                                Offset(w * 0.62f, h * 0.32f)
                            )
                            starPoints.forEach { pt ->
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.35f),
                                    radius = 1.5.dp.toPx(),
                                    center = pt
                                )
                            }
                        }

                        // مسار القوس المنحني المرفوع لأعلى
                        val startX = w * 0.90f
                        val startY = h * 0.70f
                        val endX = w * 0.10f
                        val endY = h * 0.70f
                        val controlX = w * 0.50f
                        val controlY = h * 0.02f

                        val arcPath = Path().apply {
                            moveTo(startX, startY)
                            quadraticBezierTo(controlX, controlY, endX, endY)
                        }

                        // رسم المسار الخلفي الخافت
                        drawPath(
                            path = arcPath,
                            color = Color.White.copy(alpha = 0.18f),
                            style = Stroke(
                                width = 2.5.dp.toPx(),
                                cap = StrokeCap.Round,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                            )
                        )

                        val t = animatedProgress
                        val oneMinusT = 1.0f - t
                        val celestialX = (oneMinusT * oneMinusT * startX) + (2f * oneMinusT * t * controlX) + (t * t * endX)
                        val celestialY = (oneMinusT * oneMinusT * startY) + (2f * oneMinusT * t * controlY) + (t * t * endY)

                        val filledPath = Path().apply {
                            moveTo(startX, startY)
                            val steps = 24
                            for (i in 1..steps) {
                                val subT = (t * (i.toFloat() / steps))
                                val s1mT = 1f - subT
                                val px = (s1mT * s1mT * startX) + (2f * s1mT * subT * controlX) + (subT * subT * endX)
                                val py = (s1mT * s1mT * startY) + (2f * s1mT * subT * controlY) + (subT * subT * endY)
                                lineTo(px, py)
                            }
                        }

                        val trackColor = if (isDaytime) GoldSoft else MintAccent
                        drawPath(
                            path = filledPath,
                            color = trackColor,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // هالة التوهج للجرم السماوي
                        val glowColor = if (isDaytime) GoldWarm.copy(alpha = 0.40f) else Color(0xFF38BDF8).copy(alpha = 0.40f)
                        drawCircle(
                            color = glowColor,
                            radius = 14.dp.toPx(),
                            center = Offset(celestialX, celestialY)
                        )

                        val coreColor = if (isDaytime) Color(0xFFFBBF24) else Color(0xFFE2E8F0)
                        drawCircle(
                            color = coreColor,
                            radius = 7.dp.toPx(),
                            center = Offset(celestialX, celestialY)
                        )

                        if (isDaytime) {
                            for (angle in 0 until 360 step 45) {
                                val rad = Math.toRadians(angle.toDouble())
                                val rx1 = celestialX + (cos(rad) * 9.dp.toPx()).toFloat()
                                val ry1 = celestialY + (sin(rad) * 9.dp.toPx()).toFloat()
                                val rx2 = celestialX + (cos(rad) * 12.dp.toPx()).toFloat()
                                val ry2 = celestialY + (sin(rad) * 12.dp.toPx()).toFloat()
                                drawLine(
                                    color = GoldSoft,
                                    start = Offset(rx1, ry1),
                                    end = Offset(rx2, ry2),
                                    strokeWidth = 1.8.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                        } else {
                            drawCircle(
                                color = cardGradient[1],
                                radius = 5.dp.toPx(),
                                center = Offset(celestialX + 2.5.dp.toPx(), celestialY - 1.5.dp.toPx())
                            )
                        }
                    }
                }

                // أقصى اليمين واليسار: أسماء وأوقات الصلوات السابقة والتالية بدون كلمات "الصلاة السابقة" و "الصلاة التالية"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // أقصى اليمين: الصلاة السابقة بدون كلمة "الصلاة السابقة"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = schedule.previousPrayer.type.arabicName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = schedule.previousPrayer.formatted12h,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.80f)
                        )
                    }

                    // أقصى اليسار: الصلاة التالية بدون كلمة "الصلاة التالية"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = schedule.nextPrayer.type.arabicName,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDaytime) GoldSoft else MintAccent
                        )
                        Text(
                            text = schedule.nextPrayer.formatted12h,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
