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
                .padding(horizontal = 18.dp, vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // الشريط العلوي داخل البطاقة: اسم المدينة والتاريخ وزر ضبط المنبهات
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.12f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = if (isDaytime) GoldSoft else MintAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = schedule.city.arabicName,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = schedule.hijriFormatted,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = Color.White.copy(alpha = 0.70f)
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenSettingsClick,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.12f))
                            .testTag("open_prayer_settings_icon_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "الضبط والمنبهات",
                            tint = GoldSoft,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // عنوان باقي على الصلاة + العدّاد الرقمي الأبرز
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "باقي على صلاة ${schedule.nextPrayer.type.arabicName}",
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp),
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.90f)
                    )

                    Text(
                        text = schedule.countdownFormatted,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = 38.sp,
                            letterSpacing = 1.sp
                        ),
                        fontWeight = FontWeight.Black,
                        color = if (isDaytime) MintAccent else Color(0xFF67E8F9)
                    )
                }

                // المنحنى الفلكي السماوي مع الشمس أو القمر
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
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
                                    radius = 2.dp.toPx(),
                                    center = pt
                                )
                            }
                        }

                        // مسار القوس المنحني (Curved arc from right (Prev) to left (Next))
                        // في العربية من اليمين (الصلاة السابقة) لليسار (الصلاة التالية)
                        val startX = w * 0.92f
                        val startY = h * 0.78f
                        val endX = w * 0.08f
                        val endY = h * 0.78f
                        val controlX = w * 0.50f
                        val controlY = h * 0.08f

                        val arcPath = Path().apply {
                            moveTo(startX, startY)
                            quadraticBezierTo(controlX, controlY, endX, endY)
                        }

                        // رسم المسار الخلفي الخافت (منقط أو شفاف)
                        drawPath(
                            path = arcPath,
                            color = Color.White.copy(alpha = 0.18f),
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
                            )
                        )

                        // حساب موضع الشمس أو القمر على المنحنى التربيعي (t = animatedProgress)
                        // B(t) = (1-t)^2 * P0 + 2(1-t)t * P1 + t^2 * P2
                        // حيث يبدأ من اليمين P0 (t=0) وينتهي عند اليسار P2 (t=1)
                        val t = animatedProgress
                        val oneMinusT = 1.0f - t
                        val celestialX = (oneMinusT * oneMinusT * startX) + (2f * oneMinusT * t * controlX) + (t * t * endX)
                        val celestialY = (oneMinusT * oneMinusT * startY) + (2f * oneMinusT * t * controlY) + (t * t * endY)

                        // مسار التعبئة المتوهج حتى موضع الجرم السماوي
                        val filledPath = Path().apply {
                            moveTo(startX, startY)
                            // تقسيم مسار بيزيه لنقاط حتى t
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
                            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        )

                        // هالة التوهج للجرم السماوي
                        val glowColor = if (isDaytime) GoldWarm.copy(alpha = 0.40f) else Color(0xFF38BDF8).copy(alpha = 0.40f)
                        drawCircle(
                            color = glowColor,
                            radius = 18.dp.toPx(),
                            center = Offset(celestialX, celestialY)
                        )

                        val coreColor = if (isDaytime) Color(0xFFFBBF24) else Color(0xFFE2E8F0)
                        drawCircle(
                            color = coreColor,
                            radius = 9.dp.toPx(),
                            center = Offset(celestialX, celestialY)
                        )

                        if (isDaytime) {
                            // أشعة شمسية صغيرة
                            for (angle in 0 until 360 step 45) {
                                val rad = Math.toRadians(angle.toDouble())
                                val rx1 = celestialX + (cos(rad) * 11.dp.toPx()).toFloat()
                                val ry1 = celestialY + (sin(rad) * 11.dp.toPx()).toFloat()
                                val rx2 = celestialX + (cos(rad) * 15.dp.toPx()).toFloat()
                                val ry2 = celestialY + (sin(rad) * 15.dp.toPx()).toFloat()
                                drawLine(
                                    color = GoldSoft,
                                    start = Offset(rx1, ry1),
                                    end = Offset(rx2, ry2),
                                    strokeWidth = 2.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                        } else {
                            // ظل الهلال لإعطاء شكل قمر حقيقي
                            drawCircle(
                                color = cardGradient[1],
                                radius = 7.dp.toPx(),
                                center = Offset(celestialX + 3.dp.toPx(), celestialY - 2.dp.toPx())
                            )
                        }
                    }
                }

                // أقصى اليمين: الصلاة السابقة • أقصى اليسار: الصلاة التالية
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // أقصى اليمين: الصلاة السابقة
                    Column(
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "الصلاة السابقة",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = schedule.previousPrayer.type.arabicName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = schedule.previousPrayer.formatted12h,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.80f)
                            )
                        }
                    }

                    // أقصى اليسار: الصلاة التالية
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "الصلاة التالية",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = schedule.nextPrayer.type.arabicName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDaytime) GoldSoft else MintAccent
                            )
                            Text(
                                text = schedule.nextPrayer.formatted12h,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
