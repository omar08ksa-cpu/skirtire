package com.example.features.protection

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.prayer.UmmAlQuraCalculator
import com.example.core.protection.AppUsageMonitor
import com.example.core.protection.AppUsageStatItem
import com.example.core.protection.DayUsageItem
import com.example.core.protection.WeeklyUsageData
import com.example.data.BlockedAppEntity
import com.example.features.DailyManagerUiState
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.PriorityHighRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * أيقونة التطبيق الحقيقية المستخرجة من نظام أندرويد
 */
@Composable
fun AppIconImage(
    packageName: String,
    modifier: Modifier = Modifier,
    fallbackText: String = ""
) {
    val context = LocalContext.current
    val bitmap = remember(packageName) {
        try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            if (drawable is android.graphics.drawable.BitmapDrawable) {
                drawable.bitmap.asImageBitmap()
            } else {
                val bmp = android.graphics.Bitmap.createBitmap(
                    drawable.intrinsicWidth.coerceAtLeast(1),
                    drawable.intrinsicHeight.coerceAtLeast(1),
                    android.graphics.Bitmap.Config.ARGB_8888
                )
                val canvas = android.graphics.Canvas(bmp)
                drawable.setBounds(0, 0, canvas.width, canvas.height)
                drawable.draw(canvas)
                bmp.asImageBitmap()
            }
        } catch (_: Exception) {
            null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            modifier = modifier.clip(RoundedCornerShape(8.dp))
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = fallbackText.take(1).uppercase(),
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp
            )
        }
    }
}

/**
 * بطاقة وقت الاستخدام السلبي والإيجابي وإجمالي وقت الشاشة اليوم
 * تحسب الاستخدام السلبي فقط من التطبيقات المعينة كـ NEGATIVE
 */
@Composable
fun NegativeAndPositiveScreenTimeCard(
    uiState: DailyManagerUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalMins = uiState.usageOverview.totalScreenMinutesToday
    val totalMillis = uiState.usageOverview.totalScreenTimeMillisToday
    val negativeMins = uiState.negativeScreenMinutesToday
    val positiveMins = uiState.positiveScreenMinutesToday

    val negativePercent = if (totalMins > 0) ((negativeMins.toFloat() / totalMins.toFloat()) * 100).toInt() else 0
    val positivePercent = if (totalMins > 0) ((positiveMins.toFloat() / totalMins.toFloat()) * 100).toInt() else 0

    Card(
        modifier = modifier.fillMaxWidth(),
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
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.PhoneAndroid,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "وقت استخدام الهاتف اليوم",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = UmmAlQuraCalculator.toArabicDigits(
                                AppUsageMonitor.formatDurationArabic(totalMillis)
                            ),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                IconButton(onClick = onRefresh, modifier = Modifier.testTag("refresh_usage_btn")) {
                    Icon(Icons.Default.Refresh, contentDescription = "تحديث البيانات")
                }
            }

            // بطاقتان متجاورتان: الاستخدام السلبي والاستخدام الإيجابي
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // بطاقة الاستخدام السلبي
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = PriorityHighRed.copy(alpha = 0.10f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PriorityHighRed.copy(alpha = 0.25f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = PriorityHighRed,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "استخدام سلبي",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = PriorityHighRed
                            )
                        }
                        Text(
                            text = UmmAlQuraCalculator.toArabicDigits(
                                AppUsageMonitor.formatDurationArabic(negativeMins * 60_000L)
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = PriorityHighRed
                        )
                        Text(
                            text = UmmAlQuraCalculator.toArabicDigits("$negativePercent% من إجمالي الشاشة"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }

                // بطاقة الاستخدام الإيجابي
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = EmeraldDeep.copy(alpha = 0.10f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldDeep.copy(alpha = 0.25f)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = EmeraldDeep,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "استخدام إيجابي",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = EmeraldDeep
                            )
                        }
                        Text(
                            text = UmmAlQuraCalculator.toArabicDigits(
                                AppUsageMonitor.formatDurationArabic(positiveMins * 60_000L)
                            ),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = EmeraldDeep
                        )
                        Text(
                            text = UmmAlQuraCalculator.toArabicDigits("$positivePercent% من إجمالي الشاشة"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // مؤشر التوزيع
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LinearProgressIndicator(
                    progress = { if (totalMins > 0) (negativeMins.toFloat() / totalMins.toFloat()).coerceIn(0f, 1f) else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50)),
                    color = PriorityHighRed,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text(
                    text = "يتم حساب الوقت السلبي من التطبيقات المحددة كـ «استخدام سلبي». اضغط على أي تطبيق لتعيين نوع تأثيره.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * بطاقة الرسم البياني الأسبوعي لاستخدام الهاتف ككل مع التنقل بين الأسابيع
 */
@Composable
fun WeeklyPhoneUsageCard(
    selectedDateKey: String? = null,
    onSelectDay: ((DayUsageItem) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var weekOffset by remember { mutableIntStateOf(0) }
    var weeklyData by remember { mutableStateOf<WeeklyUsageData?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(weekOffset) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val res = AppUsageMonitor.queryWeeklyUsage(context, null, weekOffset)
            weeklyData = res
            isLoading = false
        }
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
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "الرسم البياني لاستخدام الجوال في الأسبوع",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = weeklyData?.weekRangeLabel ?: "جاري التحميل...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { weekOffset -= 1 },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "الأسبوع السابق",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (weekOffset < 0) {
                        IconButton(
                            onClick = { weekOffset += 1 },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "الأسبوع التالي",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
            } else {
                val data = weeklyData
                if (data != null && data.days.isNotEmpty()) {
                    WeeklyBarChart(
                        days = data.days,
                        selectedDateKey = selectedDateKey,
                        onDayClick = onSelectDay,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "إجمالي الأسبوع: ${UmmAlQuraCalculator.toArabicDigits(AppUsageMonitor.formatDurationArabic(data.totalWeekMinutes * 60_000L))}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "المعدل اليومي: ${UmmAlQuraCalculator.toArabicDigits(AppUsageMonitor.formatDurationArabic(data.averageDailyMinutes * 60_000L))}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

/**
 * بطاقة قائمة التطبيقات المستخدمة اليوم فقط مع زر "عرض الكل"
 * والضغط على أي تطبيق يفتح صفحة التطبيق التفصيلية
 */
@Composable
fun ScreenTimeAppsListCard(
    uiState: DailyManagerUiState,
    onAppClick: (AppUsageStatItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAllApps by remember { mutableStateOf(false) }

    // فقط التطبيقات التي استعملت اليوم
    val usedAppsToday = remember(uiState.usageOverview.topUsedApps) {
        uiState.usageOverview.topUsedApps.filter { it.usedMinutesToday > 0 || it.usedTimeMillisToday > 0 }
    }

    val totalMillis = uiState.usageOverview.totalScreenTimeMillisToday
    val blockedAppsMap = remember(uiState.blockedApps) {
        uiState.blockedApps.associateBy { it.packageName }
    }

    val appsToShow = if (showAllApps) usedAppsToday else usedAppsToday.take(5)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                Column {
                    Text(
                        text = "التطبيقات المستخدمة اليوم",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (showAllApps) "عرض جميع التطبيقات المستخدمة اليوم (${usedAppsToday.size})"
                        else "أعلى 5 تطبيقات استخداماً",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (usedAppsToday.size > 5) {
                    TextButton(
                        onClick = { showAllApps = !showAllApps },
                        modifier = Modifier.testTag("toggle_show_all_apps_btn")
                    ) {
                        Text(
                            text = if (showAllApps) "عرض أقل ▴" else "عرض الكل (${usedAppsToday.size}) ▾",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            if (usedAppsToday.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لم يتم تسجيل استخدام لأي تطبيق اليوم حتى الآن.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                appsToShow.forEach { stat ->
                    val ratio = if (totalMillis > 0L) {
                        (stat.usedTimeMillisToday.toFloat() / totalMillis.toFloat()).coerceIn(0.04f, 1f)
                    } else {
                        0.05f
                    }

                    val blockedEntity = blockedAppsMap[stat.packageName]
                    val impact = blockedEntity?.usageImpact ?: "NEUTRAL"

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAppClick(stat) }
                            .testTag("app_row_${stat.packageName}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stat.appName.take(1).uppercase(),
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = stat.appName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            when (impact) {
                                                "NEGATIVE" -> Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = PriorityHighRed.copy(alpha = 0.15f)
                                                ) {
                                                    Text("سلبي", color = PriorityHighRed, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                                                }
                                                "POSITIVE" -> Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = EmeraldDeep.copy(alpha = 0.15f)
                                                ) {
                                                    Text("إيجابي", color = EmeraldDeep, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                                                }
                                                "IGNORED" -> Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                                ) {
                                                    Text("مستبعد", fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
                                                }
                                            }
                                        }

                                        if (blockedEntity != null && blockedEntity.dailyLimitMinutes > 0) {
                                            Text(
                                                text = UmmAlQuraCalculator.toArabicDigits("الحد اليومي: ${blockedEntity.dailyLimitMinutes} د"),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.secondary,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = UmmAlQuraCalculator.toArabicDigits(stat.formattedDuration),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "تفاصيل التطبيق",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            LinearProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = when (impact) {
                                    "NEGATIVE" -> PriorityHighRed
                                    "POSITIVE" -> EmeraldDeep
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
