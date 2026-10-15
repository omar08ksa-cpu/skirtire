package com.example.features.standby

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.os.BatteryManager
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.core.prayer.PrayerType
import com.example.core.prayer.UmmAlQuraCalculator
import com.example.core.study.StudyTimerState
import com.example.data.HabitEntity
import com.example.data.TaskEntity
import com.example.features.DailyManagerUiState
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldSoft
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.MintAccent
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.cos
import kotlin.math.sin

enum class StandbyViewMode {
    SPLIT_WIDGETS, // شاشة مقسمة مزدوجة (مثل iOS StandBy)
    FULLSCREEN_CLOCK, // ساعة كاملة للشاشة
    DASHBOARD_HUB // لوحة القيادة الشاملة
}

enum class StandbyTheme(val title: String) {
    EMERALD_GOLD("زمردي وذهبي"),
    NIGHT_RED("ليلي أحمر خافت"),
    CYBERPUNK("سايبربانك نيون"),
    WARM_AMBER("عنبر كلاسيكي"),
    MONOCHROME_WHITE("أبيض مينيمال")
}

enum class ClockFaceType(val title: String) {
    NEON_DIGITAL("رقمية نيون"),
    ANALOG_LUXURY("عقارب فاخرة"),
    FLIP_CLOCK("أرقام متقلبة Flip"),
    DUO_STACKED("عمودية ضخمة"),
    ISLAMIC_SOLAR("فلكية إسلامية")
}

@Composable
fun StandbyOverlay(
    uiState: DailyManagerUiState,
    timerState: StudyTimerState,
    onTogglePauseTimer: () -> Unit,
    onFinishAndSaveTimer: () -> Unit,
    onStartQuickTimerIfIdle: () -> Unit,
    onToggleTask: (String) -> Unit,
    onMarkPrayerDone: (PrayerType) -> Unit,
    onExitStandby: () -> Unit
) {
    val context = LocalContext.current

    var viewMode by rememberSaveable { mutableStateOf(StandbyViewMode.SPLIT_WIDGETS) }
    var selectedTheme by rememberSaveable { mutableStateOf(StandbyTheme.EMERALD_GOLD) }
    var clockFace by rememberSaveable { mutableStateOf(ClockFaceType.NEON_DIGITAL) }

    // مؤشرات تبديل الويدجت في الشاشة المقسمة
    var leftWidgetIndex by rememberSaveable { mutableIntStateOf(0) } // 0=Clock, 1=Battery, 2=YearProgress
    var rightWidgetIndex by rememberSaveable { mutableIntStateOf(0) } // 0=Prayer, 1=Tasks, 2=Habits, 3=Weather, 4=Music, 5=Counter

    var dimOverlayAlpha by rememberSaveable { mutableFloatStateOf(0f) }
    var isNightVisionActive by rememberSaveable { mutableStateOf(false) }

    // مشغل الصوتيات الهادئة للتركيز
    var isPlayingMusic by rememberSaveable { mutableStateOf(false) }
    var currentTrackIndex by rememberSaveable { mutableIntStateOf(0) }
    val ambientTracks = listOf(
        "صوت المطر الهادئ للتركيز 🌧️" to "طبيعة نقية • تركيز عميق",
        "أمواج البحر والسكينة 🌊" to "استرخاء وتأمل هادئ",
        "تلاوة خاشعة مرئقة للقلب 📖" to "سكينة وبركة في الوقت",
        "ضوضاء بيضاء عازلة للصوت 🎧" to "حجب المشتتات والضوضاء",
        "عصافير الفجر والهدوء 🌿" to "بداية يوم مفعمة بالحيوية"
    )

    BackHandler {
        onExitStandby()
    }

    // إبقاء الشاشة مضاءة + وضع الشاشة الكاملة
    DisposableEffect(context) {
        val activity = context as? Activity
        val window = activity?.window
        val originalOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val insetsController = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        insetsController?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController?.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            activity?.requestedOrientation = originalOrientation
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // الوقت الحالي الحي
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000L)
        }
    }

    val cal = remember(currentTimeMillis) {
        Calendar.getInstance(TimeZone.getTimeZone("GMT+03:00")).apply {
            timeInMillis = currentTimeMillis
        }
    }
    val hour24 = cal.get(Calendar.HOUR_OF_DAY)
    val minute = cal.get(Calendar.MINUTE)
    val second = cal.get(Calendar.SECOND)
    val hour12 = when {
        hour24 == 0 -> 12
        hour24 > 12 -> hour24 - 12
        else -> hour24
    }
    val period = if (hour24 < 12) "ص" else "م"
    val clockDigitsFormatted = UmmAlQuraCalculator.toArabicDigits(
        String.format(Locale.US, "%02d:%02d", hour12, minute)
    )
    val secondsDigitsFormatted = UmmAlQuraCalculator.toArabicDigits(
        String.format(Locale.US, ":%02d", second)
    )

    // قراءة بطارية الهاتف الحقيقية
    val batteryIntent = remember(currentTimeMillis) {
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }
    val batteryLevel = remember(batteryIntent) {
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        if (level >= 0 && scale > 0) (level * 100 / scale) else 82
    }
    val isBatteryCharging = remember(batteryIntent) {
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    // حساب كم اكتمل من السنة
    val (yearPercent, daysPassed, daysRemaining) = remember(currentTimeMillis) {
        val c = Calendar.getInstance()
        c.timeInMillis = currentTimeMillis
        val d = c.get(Calendar.DAY_OF_YEAR)
        val total = c.getActualMaximum(Calendar.DAY_OF_YEAR)
        val p = (d.toFloat() / total.toFloat() * 100).toInt()
        Triple(p, d, total - d)
    }

    // لوحة الألوان حسب السمة أو الوضع الليلي
    val effectiveTheme = if (isNightVisionActive) StandbyTheme.NIGHT_RED else selectedTheme
    val themeColors = remember(effectiveTheme) {
        when (effectiveTheme) {
            StandbyTheme.NIGHT_RED -> StandbyColorPalette(
                bg = Color(0xFF0C0202),
                cardBg = Color(0xFF220505).copy(alpha = 0.5f),
                accentPrimary = Color(0xFFFF3333),
                accentSecondary = Color(0xFFCC1111),
                textPrimary = Color(0xFFFF5555),
                textSecondary = Color(0xFF993333)
            )
            StandbyTheme.CYBERPUNK -> StandbyColorPalette(
                bg = Color(0xFF040612),
                cardBg = Color(0xFF0D142B).copy(alpha = 0.6f),
                accentPrimary = Color(0xFF00F0FF),
                accentSecondary = Color(0xFFFF007F),
                textPrimary = Color(0xFFE0F7FA),
                textSecondary = Color(0xFF80DEEA)
            )
            StandbyTheme.WARM_AMBER -> StandbyColorPalette(
                bg = Color(0xFF100A05),
                cardBg = Color(0xFF26180C).copy(alpha = 0.6f),
                accentPrimary = Color(0xFFFFB300),
                accentSecondary = Color(0xFFFF8F00),
                textPrimary = Color(0xFFFFECB3),
                textSecondary = Color(0xFFFFCC80)
            )
            StandbyTheme.MONOCHROME_WHITE -> StandbyColorPalette(
                bg = Color.Black,
                cardBg = Color.White.copy(alpha = 0.08f),
                accentPrimary = Color.White,
                accentSecondary = Color(0xFFB0BEC5),
                textPrimary = Color.White,
                textSecondary = Color(0xFF9E9E9E)
            )
            StandbyTheme.EMERALD_GOLD -> StandbyColorPalette(
                bg = Color(0xFF05120D),
                cardBg = Color.White.copy(alpha = 0.06f),
                accentPrimary = MintAccent,
                accentSecondary = GoldSoft,
                textPrimary = Color.White,
                textSecondary = Color.White.copy(alpha = 0.75f)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.bg)
            .testTag("standby_super_overlay")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // الشريط العلوي الاحترافي: اختيار العرض، تبديل السمة، الرؤية الليلية، التعتيم، الخروج
            StandbyTopBar(
                viewMode = viewMode,
                onSelectViewMode = { viewMode = it },
                clockFace = clockFace,
                onSelectClockFace = { clockFace = it },
                selectedTheme = selectedTheme,
                onSelectTheme = { selectedTheme = it },
                isNightVision = isNightVisionActive,
                onToggleNightVision = { isNightVisionActive = !isNightVisionActive },
                dimAlpha = dimOverlayAlpha,
                onToggleDim = {
                    dimOverlayAlpha = when {
                        dimOverlayAlpha < 0.2f -> 0.30f
                        dimOverlayAlpha < 0.5f -> 0.60f
                        dimOverlayAlpha < 0.7f -> 0.75f
                        else -> 0f
                    }
                },
                palette = themeColors,
                onExit = onExitStandby
            )

            // محتوى الشاشة بناءً على نوع العرض المختار
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                when (viewMode) {
                    StandbyViewMode.SPLIT_WIDGETS -> {
                        StandbySplitWidgetsView(
                            clockFace = clockFace,
                            clockDigits = clockDigitsFormatted,
                            secondsDigits = secondsDigitsFormatted,
                            period = period,
                            hour = hour12,
                            minute = minute,
                            second = second,
                            leftIndex = leftWidgetIndex,
                            onCycleLeft = { leftWidgetIndex = (leftWidgetIndex + 1) % 3 },
                            rightIndex = rightWidgetIndex,
                            onCycleRight = { rightWidgetIndex = (rightWidgetIndex + 1) % 6 },
                            uiState = uiState,
                            timerState = timerState,
                            batteryLevel = batteryLevel,
                            isBatteryCharging = isBatteryCharging,
                            yearPercent = yearPercent,
                            daysPassed = daysPassed,
                            daysRemaining = daysRemaining,
                            ambientTracks = ambientTracks,
                            currentTrackIndex = currentTrackIndex,
                            isPlayingMusic = isPlayingMusic,
                            onTogglePlayMusic = { isPlayingMusic = !isPlayingMusic },
                            onNextTrack = { currentTrackIndex = (currentTrackIndex + 1) % ambientTracks.size },
                            palette = themeColors,
                            onToggleTask = onToggleTask,
                            onMarkPrayerDone = onMarkPrayerDone,
                            onTogglePauseTimer = onTogglePauseTimer,
                            onFinishTimer = onFinishAndSaveTimer,
                            onStartTimer = onStartQuickTimerIfIdle
                        )
                    }

                    StandbyViewMode.FULLSCREEN_CLOCK -> {
                        StandbyFullscreenClockView(
                            clockFace = clockFace,
                            clockDigits = clockDigitsFormatted,
                            secondsDigits = secondsDigitsFormatted,
                            period = period,
                            hour = hour12,
                            minute = minute,
                            second = second,
                            hijriDate = uiState.prayerSchedule.hijriFormatted,
                            palette = themeColors,
                            onCycleClockFace = {
                                val all = ClockFaceType.entries
                                clockFace = all[(clockFace.ordinal + 1) % all.size]
                            }
                        )
                    }

                    StandbyViewMode.DASHBOARD_HUB -> {
                        StandbyDashboardHubView(
                            clockDigits = clockDigitsFormatted,
                            secondsDigits = secondsDigitsFormatted,
                            period = period,
                            uiState = uiState,
                            timerState = timerState,
                            batteryLevel = batteryLevel,
                            isBatteryCharging = isBatteryCharging,
                            yearPercent = yearPercent,
                            daysRemaining = daysRemaining,
                            ambientTracks = ambientTracks,
                            currentTrackIndex = currentTrackIndex,
                            isPlayingMusic = isPlayingMusic,
                            onTogglePlayMusic = { isPlayingMusic = !isPlayingMusic },
                            palette = themeColors,
                            onToggleTask = onToggleTask,
                            onMarkPrayerDone = onMarkPrayerDone,
                            onTogglePauseTimer = onTogglePauseTimer
                        )
                    }
                }
            }

            // شريط المعلومات السريع أسفل الشاشة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🌙 ${uiState.prayerSchedule.hijriFormatted} • ${uiState.prayerSchedule.dateKey}",
                    style = MaterialTheme.typography.labelMedium,
                    color = themeColors.textSecondary
                )
                Text(
                    text = "🔋 البطارية: ${UmmAlQuraCalculator.toArabicDigits("$batteryLevel٪")}${if (isBatteryCharging) " ⚡" else ""} • العام: ${UmmAlQuraCalculator.toArabicDigits("$yearPercent٪")}",
                    style = MaterialTheme.typography.labelMedium,
                    color = themeColors.textSecondary
                )
            }
        }

        // طبقة التعتيم الليلي المريحة للعين
        if (dimOverlayAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dimOverlayAlpha))
            )
        }
    }
}

data class StandbyColorPalette(
    val bg: Color,
    val cardBg: Color,
    val accentPrimary: Color,
    val accentSecondary: Color,
    val textPrimary: Color,
    val textSecondary: Color
)

/**
 * الشريط العلوي لوضع الستاند باي
 */
@Composable
private fun StandbyTopBar(
    viewMode: StandbyViewMode,
    onSelectViewMode: (StandbyViewMode) -> Unit,
    clockFace: ClockFaceType,
    onSelectClockFace: (ClockFaceType) -> Unit,
    selectedTheme: StandbyTheme,
    onSelectTheme: (StandbyTheme) -> Unit,
    isNightVision: Boolean,
    onToggleNightVision: () -> Unit,
    dimAlpha: Float,
    onToggleDim: () -> Unit,
    palette: StandbyColorPalette,
    onExit: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // مفاتيح نمط العرض (شاشة مقسمة / ساعة كاملة / لوحة تحكم)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StandbyChip(
                text = "المقسمة ◫",
                isSelected = viewMode == StandbyViewMode.SPLIT_WIDGETS,
                onClick = { onSelectViewMode(StandbyViewMode.SPLIT_WIDGETS) },
                palette = palette
            )
            StandbyChip(
                text = "ساعة كاملة 🕒",
                isSelected = viewMode == StandbyViewMode.FULLSCREEN_CLOCK,
                onClick = { onSelectViewMode(StandbyViewMode.FULLSCREEN_CLOCK) },
                palette = palette
            )
            StandbyChip(
                text = "اللوحة الشاملة ☵",
                isSelected = viewMode == StandbyViewMode.DASHBOARD_HUB,
                onClick = { onSelectViewMode(StandbyViewMode.DASHBOARD_HUB) },
                palette = palette
            )
        }

        // أزرار التحكم: سمات الألوان، الوضع الليلي الأحمر، التعتيم، الخروج
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // زر الرؤية الليلية الحمراء
            Surface(
                color = if (isNightVision) Color(0xFFFF2222) else palette.cardBg,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onToggleNightVision() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Nightlight,
                        contentDescription = "الوضع الليلي الأحمر",
                        tint = if (isNightVision) Color.White else palette.accentPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ليلي أحمر",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isNightVision) Color.White else palette.textPrimary
                    )
                }
            }

            // زر التعتيم
            Surface(
                color = palette.cardBg,
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onToggleDim() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.BrightnessLow,
                        contentDescription = "تعتيم",
                        tint = palette.accentSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    val percent = (dimAlpha * 100).toInt()
                    Text(
                        text = if (percent == 0) "إضاءة 100%" else UmmAlQuraCalculator.toArabicDigits("تعتيم $percent٪"),
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textPrimary
                    )
                }
            }

            // زر الخروج السهل
            FilledTonalButton(
                onClick = onExit,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = palette.cardBg,
                    contentColor = palette.textPrimary
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "خروج", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("خروج", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun StandbyChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    palette: StandbyColorPalette
) {
    Surface(
        color = if (isSelected) palette.accentPrimary else palette.cardBg,
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable { onClick() }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) palette.bg else palette.textPrimary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

/**
 * العرض المقسم المزدوج (Split Widgets) - تماماً مثل تطبيق StandBy الأصلي
 */
@Composable
private fun StandbySplitWidgetsView(
    clockFace: ClockFaceType,
    clockDigits: String,
    secondsDigits: String,
    period: String,
    hour: Int,
    minute: Int,
    second: Int,
    leftIndex: Int,
    onCycleLeft: () -> Unit,
    rightIndex: Int,
    onCycleRight: () -> Unit,
    uiState: DailyManagerUiState,
    timerState: StudyTimerState,
    batteryLevel: Int,
    isBatteryCharging: Boolean,
    yearPercent: Int,
    daysPassed: Int,
    daysRemaining: Int,
    ambientTracks: List<Pair<String, String>>,
    currentTrackIndex: Int,
    isPlayingMusic: Boolean,
    onTogglePlayMusic: () -> Unit,
    onNextTrack: () -> Unit,
    palette: StandbyColorPalette,
    onToggleTask: (String) -> Unit,
    onMarkPrayerDone: (PrayerType) -> Unit,
    onTogglePauseTimer: () -> Unit,
    onFinishTimer: () -> Unit,
    onStartTimer: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // الجانب الأيمن (ويدجت الساعة أو البطارية أو نسبة اكتمال العام)
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = palette.cardBg)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // محتوى الويدجت الأيسر
                when (leftIndex) {
                    0 -> StandbyClockWidget(
                        clockFace = clockFace,
                        clockDigits = clockDigits,
                        secondsDigits = secondsDigits,
                        period = period,
                        hour = hour,
                        minute = minute,
                        second = second,
                        palette = palette
                    )
                    1 -> StandbyBatteryWidget(
                        level = batteryLevel,
                        isCharging = isBatteryCharging,
                        palette = palette
                    )
                    else -> StandbyYearProgressWidget(
                        percent = yearPercent,
                        daysPassed = daysPassed,
                        daysRemaining = daysRemaining,
                        palette = palette
                    )
                }

                // زر التبديل السريع باللمس في زاوية الويدجت
                Surface(
                    color = palette.accentSecondary.copy(alpha = 0.2f),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(CircleShape)
                        .clickable { onCycleLeft() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "تبديل",
                            tint = palette.accentSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = when (leftIndex) {
                                0 -> "الساعة"
                                1 -> "البطارية"
                                else -> "سنة 2026"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.accentSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // الجانب الأيسر (ويدجت الصلاة / المهام / العادات / الطقس / مشغل الموسيقى / العداد)
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = palette.cardBg)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (rightIndex) {
                    0 -> StandbyPrayerWidget(
                        uiState = uiState,
                        palette = palette,
                        onMarkDone = onMarkPrayerDone
                    )
                    1 -> StandbyTasksWidget(
                        tasks = uiState.mainTasks.filter { !it.isCompleted },
                        palette = palette,
                        onToggleTask = onToggleTask
                    )
                    2 -> StandbyHabitsWidget(
                        habits = uiState.habits,
                        palette = palette
                    )
                    3 -> StandbyWeatherWidget(
                        cityName = uiState.selectedCity.arabicName,
                        palette = palette
                    )
                    4 -> StandbyMusicWidget(
                        tracks = ambientTracks,
                        currentIndex = currentTrackIndex,
                        isPlaying = isPlayingMusic,
                        onTogglePlay = onTogglePlayMusic,
                        onNext = onNextTrack,
                        palette = palette
                    )
                    else -> StandbyCounterWidget(
                        timerState = timerState,
                        palette = palette,
                        onTogglePause = onTogglePauseTimer,
                        onFinish = onFinishTimer,
                        onStart = onStartTimer
                    )
                }

                // زر تبديل الويدجت الأيمن
                Surface(
                    color = palette.accentPrimary.copy(alpha = 0.2f),
                    shape = CircleShape,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .clip(CircleShape)
                        .clickable { onCycleRight() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "تبديل",
                            tint = palette.accentPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = when (rightIndex) {
                                0 -> "الصلاة"
                                1 -> "المهام"
                                2 -> "العادات"
                                3 -> "الطقس"
                                4 -> "مشغل التركيز"
                                else -> "العداد"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.accentPrimary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * 1. ويدجت الساعة بأنواعها المختلفة (Neon, Analog, Flip, Duo)
 */
@Composable
private fun StandbyClockWidget(
    clockFace: ClockFaceType,
    clockDigits: String,
    secondsDigits: String,
    period: String,
    hour: Int,
    minute: Int,
    second: Int,
    palette: StandbyColorPalette
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        when (clockFace) {
            ClockFaceType.ANALOG_LUXURY -> {
                StandbyAnalogClockCanvas(hour = hour, minute = minute, second = second, palette = palette)
            }
            ClockFaceType.FLIP_CLOCK -> {
                StandbyFlipClockDisplay(clockDigits = clockDigits, period = period, palette = palette)
            }
            ClockFaceType.DUO_STACKED -> {
                val parts = clockDigits.split(":")
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = parts.getOrNull(0) ?: "12",
                        fontSize = 82.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.accentPrimary,
                        lineHeight = 78.sp
                    )
                    Text(
                        text = parts.getOrNull(1) ?: "00",
                        fontSize = 82.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.accentSecondary,
                        lineHeight = 78.sp
                    )
                }
            }
            else -> {
                // Neon Digital
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = clockDigits,
                            fontSize = 72.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accentPrimary,
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$secondsDigits $period",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accentSecondary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                    Text(
                        text = "وضع الستاند باي • تركيز وإنجاز",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.textSecondary
                    )
                }
            }
        }
    }
}

/**
 * رسم ساعة العقارب الفاخرة عبر Canvas
 */
@Composable
private fun StandbyAnalogClockCanvas(
    hour: Int,
    minute: Int,
    second: Int,
    palette: StandbyColorPalette
) {
    Box(
        modifier = Modifier.size(190.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2 - 8.dp.toPx()

            // قرص الساعة الخارجي
            drawCircle(
                color = palette.accentPrimary.copy(alpha = 0.2f),
                radius = radius,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )

            // علامات الساعات الـ ١٢
            for (i in 0 until 12) {
                val angle = Math.toRadians((i * 30 - 90).toDouble())
                val start = Offset(
                    (center.x + (radius - 12.dp.toPx()) * cos(angle)).toFloat(),
                    (center.y + (radius - 12.dp.toPx()) * sin(angle)).toFloat()
                )
                val end = Offset(
                    (center.x + radius * cos(angle)).toFloat(),
                    (center.y + radius * sin(angle)).toFloat()
                )
                drawLine(
                    color = if (i % 3 == 0) palette.accentPrimary else palette.textSecondary,
                    start = start,
                    end = end,
                    strokeWidth = if (i % 3 == 0) 3.dp.toPx() else 1.5.dp.toPx()
                )
            }

            // عقرب الساعات
            val hourAngle = Math.toRadians(((hour % 12 + minute / 60f) * 30 - 90).toDouble())
            val hourHandEnd = Offset(
                (center.x + (radius * 0.52f) * cos(hourAngle)).toFloat(),
                (center.y + (radius * 0.52f) * sin(hourAngle)).toFloat()
            )
            drawLine(
                color = palette.accentPrimary,
                start = center,
                end = hourHandEnd,
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // عقرب الدقائق
            val minuteAngle = Math.toRadians(((minute + second / 60f) * 6 - 90).toDouble())
            val minuteHandEnd = Offset(
                (center.x + (radius * 0.74f) * cos(minuteAngle)).toFloat(),
                (center.y + (radius * 0.74f) * sin(minuteAngle)).toFloat()
            )
            drawLine(
                color = palette.textPrimary,
                start = center,
                end = minuteHandEnd,
                strokeWidth = 3.5.dp.toPx(),
                cap = StrokeCap.Round
            )

            // عقرب الثواني
            val secondAngle = Math.toRadians((second * 6 - 90).toDouble())
            val secondHandEnd = Offset(
                (center.x + (radius * 0.86f) * cos(secondAngle)).toFloat(),
                (center.y + (radius * 0.86f) * sin(secondAngle)).toFloat()
            )
            drawLine(
                color = palette.accentSecondary,
                start = center,
                end = secondHandEnd,
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )

            // نقطة المركز الذهبية
            drawCircle(
                color = palette.accentSecondary,
                radius = 5.dp.toPx(),
                center = center
            )
        }
    }
}

/**
 * تصميم ساعة الفليب ريترو (Flip Clock Display)
 */
@Composable
private fun StandbyFlipClockDisplay(
    clockDigits: String,
    period: String,
    palette: StandbyColorPalette
) {
    val parts = clockDigits.split(":")
    val h = parts.getOrNull(0) ?: "12"
    val m = parts.getOrNull(1) ?: "00"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        FlipCardTile(number = h, palette = palette)
        Text(
            text = ":",
            fontSize = 44.sp,
            fontWeight = FontWeight.Bold,
            color = palette.accentSecondary
        )
        FlipCardTile(number = m, palette = palette)
        Text(
            text = period,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = palette.accentPrimary
        )
    }
}

@Composable
private fun FlipCardTile(number: String, palette: StandbyColorPalette) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
        modifier = Modifier
            .size(width = 90.dp, height = 110.dp)
            .border(1.dp, palette.accentPrimary.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // خط انقسام بطاقة الفليب في المنتصف
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(Color.Black.copy(alpha = 0.8f))
            )
            Text(
                text = number,
                fontSize = 58.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                color = palette.accentPrimary
            )
        }
    }
}

/**
 * 2. ويدجت شحن الهاتف والبطارية (Phone Battery Widget)
 */
@Composable
private fun StandbyBatteryWidget(
    level: Int,
    isCharging: Boolean,
    palette: StandbyColorPalette
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(130.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 12.dp.toPx()
                drawCircle(
                    color = Color.White.copy(alpha = 0.1f),
                    style = Stroke(stroke)
                )
                drawArc(
                    color = if (isCharging) MintAccent else palette.accentPrimary,
                    startAngle = -90f,
                    sweepAngle = 360f * (level / 100f),
                    useCenter = false,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isCharging) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "شحن",
                        tint = GoldSoft,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = UmmAlQuraCalculator.toArabicDigits("$level٪"),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = palette.textPrimary
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = if (isCharging) "⚡ متصل بالشاحن السريع" else "🔋 على طاقة البطارية",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (isCharging) MintAccent else palette.textSecondary
        )
    }
}

/**
 * 3. ويدجت كم اكتمل من السنة (Year Progress Widget)
 */
@Composable
private fun StandbyYearProgressWidget(
    percent: Int,
    daysPassed: Int,
    daysRemaining: Int,
    palette: StandbyColorPalette
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "عام 2026",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = palette.accentSecondary
        )
        Text(
            text = UmmAlQuraCalculator.toArabicDigits("اكتمل $percent٪"),
            fontSize = 46.sp,
            fontWeight = FontWeight.ExtraBold,
            color = palette.accentPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // شريط التقدم العريض
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(14.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.12f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percent / 100f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(palette.accentPrimary)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = UmmAlQuraCalculator.toArabicDigits("مضى $daysPassed يوماً • متبقي $daysRemaining يوماً"),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = palette.textSecondary
        )
    }
}

/**
 * 4. ويدجت الصلاة القادمة (Next Prayer Widget)
 */
@Composable
private fun StandbyPrayerWidget(
    uiState: DailyManagerUiState,
    palette: StandbyColorPalette,
    onMarkDone: (PrayerType) -> Unit
) {
    val nextPrayer = uiState.prayerSchedule.nextPrayer
    val countdown = UmmAlQuraCalculator.formatCountdown(uiState.prayerSchedule.remainingSecondsToNext)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Mosque, contentDescription = null, tint = palette.accentSecondary, modifier = Modifier.size(26.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("الصلاة القادمة", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
            }
            Surface(
                color = palette.accentPrimary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(50),
                modifier = Modifier.clickable { onMarkDone(nextPrayer.type) }
            ) {
                Text(
                    text = "صليت ✅",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.accentPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Column {
            Text(
                text = "${nextPrayer.type.arabicName} • ${nextPrayer.formatted12h}",
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = palette.accentPrimary
            )
            Text(
                text = "المتبقي للأذان: $countdown",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = palette.accentSecondary
            )
        }

        // أوقات الصلوات الخمس اليوم
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            uiState.prayerSchedule.prayers.forEach { p ->
                val isNext = p.type == nextPrayer.type
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = p.type.arabicName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isNext) palette.accentPrimary else palette.textSecondary,
                        fontWeight = if (isNext) FontWeight.Bold else FontWeight.Normal
                    )
                    Text(
                        text = p.formatted12h,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isNext) palette.accentSecondary else palette.textPrimary,
                        fontWeight = if (isNext) FontWeight.ExtraBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

/**
 * 5. ويدجت المهام اليومية مع إمكانية التحديد المباشر
 */
@Composable
private fun StandbyTasksWidget(
    tasks: List<com.example.data.TaskEntity>,
    palette: StandbyColorPalette,
    onToggleTask: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "المهام اليومية",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.accentPrimary
            )
            Text(
                text = UmmAlQuraCalculator.toArabicDigits("${tasks.size} متبقية"),
                style = MaterialTheme.typography.labelSmall,
                color = palette.textSecondary
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (tasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("🎉 تم إنجاز جميع مهام اليوم بنجاح!", color = palette.accentPrimary, fontWeight = FontWeight.Bold)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tasks.take(4), key = { "standby_t_${it.id}" }) { t ->
                    Surface(
                        color = Color.White.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleTask(t.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (t.isCompleted) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (t.isCompleted) palette.accentPrimary else palette.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = t.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.textPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 6. ويدجت العادات اليومية
 */
@Composable
private fun StandbyHabitsWidget(
    habits: List<HabitEntity>,
    palette: StandbyColorPalette
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp)
    ) {
        Text(
            text = "عادات اليوم والستريك",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = palette.accentPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (habits.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("لا توجد عادات مسجلة", color = palette.textSecondary)
            }
        } else {
            val topHabits = habits.take(3)
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(topHabits, key = { "standby_h_${it.id}" }) { h ->
                    Surface(
                        color = Color.White.copy(alpha = 0.06f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.TrackChanges, contentDescription = null, tint = palette.accentPrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(h.title, fontWeight = FontWeight.Bold, color = palette.textPrimary)
                                    Text(
                                        text = UmmAlQuraCalculator.toArabicDigits("الهدف: ${h.targetCount} ${h.unitLabel}"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.textSecondary
                                    )
                                }
                            }
                            Text("🔥 هدف يومي", color = GoldWarm, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 7. ويدجت الطقس والمناخ (Weather Widget)
 */
@Composable
private fun StandbyWeatherWidget(
    cityName: String,
    palette: StandbyColorPalette
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WbSunny, contentDescription = null, tint = GoldWarm, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(cityName.ifBlank { "الرياض" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = palette.textPrimary)
            }
            Text("طقس معتدل 🌤️", style = MaterialTheme.typography.labelMedium, color = palette.accentSecondary)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = UmmAlQuraCalculator.toArabicDigits("28°"),
                fontSize = 58.sp,
                fontWeight = FontWeight.ExtraBold,
                color = palette.accentPrimary
            )
            Column(horizontalAlignment = Alignment.End) {
                Text("الرطوبة: 24%", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                Text("الرياح: 14 كم/س", style = MaterialTheme.typography.bodySmall, color = palette.textSecondary)
                Text("عظمى 32° • صغرى 21°", style = MaterialTheme.typography.bodySmall, color = palette.accentSecondary)
            }
        }

        Text(
            text = "أجواء مثالية للتركيز والإنجاز والأنشطة اليومية",
            style = MaterialTheme.typography.labelSmall,
            color = palette.textSecondary
        )
    }
}

/**
 * 8. ويدجت مشغل الموسيقى والصوتيات للتركيز مع موجات مرئية متحركة
 */
@Composable
private fun StandbyMusicWidget(
    tracks: List<Pair<String, String>>,
    currentIndex: Int,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    palette: StandbyColorPalette
) {
    val currentTrack = tracks.getOrElse(currentIndex) { tracks.first() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Headphones, contentDescription = null, tint = palette.accentPrimary, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("أصوات التركيز والسكينة", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.textPrimary)
            }

            // موجات صوتية متحركة
            if (isPlaying) {
                StandbyAudioVisualizerBars(palette = palette)
            }
        }

        Column {
            Text(
                text = currentTrack.first,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = palette.accentPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = currentTrack.second,
                style = MaterialTheme.typography.bodySmall,
                color = palette.textSecondary
            )
        }

        // أزرار التحكم في الصوتيات
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNext) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "السابق", tint = palette.textPrimary)
            }
            Surface(
                shape = CircleShape,
                color = palette.accentPrimary,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .clickable { onTogglePlay() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "تشغيل/إيقاف",
                        tint = palette.bg
                    )
                }
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Default.SkipNext, contentDescription = "التالي", tint = palette.textPrimary)
            }
        }
    }
}

/**
 * موجات صوتية متذبذبة لطيفة (Visualizer)
 */
@Composable
private fun StandbyAudioVisualizerBars(palette: StandbyColorPalette) {
    val infiniteTransition = rememberInfiniteTransition(label = "audio_vis")
    val h1 by infiniteTransition.animateFloat(initialValue = 4f, targetValue = 16f, animationSpec = infiniteRepeatable(tween(400), RepeatMode.Reverse), label = "h1")
    val h2 by infiniteTransition.animateFloat(initialValue = 14f, targetValue = 6f, animationSpec = infiniteRepeatable(tween(350), RepeatMode.Reverse), label = "h2")
    val h3 by infiniteTransition.animateFloat(initialValue = 6f, targetValue = 18f, animationSpec = infiniteRepeatable(tween(450), RepeatMode.Reverse), label = "h3")

    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
        Box(modifier = Modifier.width(3.dp).height(h1.dp).background(palette.accentPrimary, RoundedCornerShape(50)))
        Box(modifier = Modifier.width(3.dp).height(h2.dp).background(palette.accentSecondary, RoundedCornerShape(50)))
        Box(modifier = Modifier.width(3.dp).height(h3.dp).background(palette.accentPrimary, RoundedCornerShape(50)))
    }
}

/**
 * 9. ويدجت عداد التركيز الجاري
 */
@Composable
private fun StandbyCounterWidget(
    timerState: StudyTimerState,
    palette: StandbyColorPalette,
    onTogglePause: () -> Unit,
    onFinish: () -> Unit,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(18.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "عداد التركيز الجاري",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = palette.accentSecondary
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = timerState.formattedTime(),
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                color = palette.accentPrimary
            )
            Text(
                text = timerState.subjectName,
                style = MaterialTheme.typography.titleSmall,
                color = palette.textPrimary
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!timerState.isActive) {
                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accentPrimary, contentColor = palette.bg)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("بدء المؤقت")
                }
            } else {
                FilledTonalButton(onClick = onTogglePause) {
                    Icon(if (timerState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (timerState.isPaused) "استئناف" else "إيقاف مؤقت")
                }
                Button(
                    onClick = onFinish,
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accentPrimary, contentColor = palette.bg)
                ) {
                    Text("حفظ الجلسة")
                }
            }
        }
    }
}

/**
 * شاشة الساعة الكاملة (Fullscreen Clock View)
 */
@Composable
private fun StandbyFullscreenClockView(
    clockFace: ClockFaceType,
    clockDigits: String,
    secondsDigits: String,
    period: String,
    hour: Int,
    minute: Int,
    second: Int,
    hijriDate: String,
    palette: StandbyColorPalette,
    onCycleClockFace: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onCycleClockFace() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (clockFace) {
                ClockFaceType.ANALOG_LUXURY -> {
                    StandbyAnalogClockCanvas(hour = hour, minute = minute, second = second, palette = palette)
                }
                ClockFaceType.FLIP_CLOCK -> {
                    StandbyFlipClockDisplay(clockDigits = clockDigits, period = period, palette = palette)
                }
                ClockFaceType.DUO_STACKED -> {
                    val parts = clockDigits.split(":")
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = parts.getOrNull(0) ?: "12",
                            fontSize = 130.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.accentPrimary,
                            lineHeight = 130.sp
                        )
                        Text(
                            text = parts.getOrNull(1) ?: "00",
                            fontSize = 130.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.accentSecondary,
                            lineHeight = 130.sp
                        )
                    }
                }
                else -> {
                    Text(
                        text = "$clockDigits$secondsDigits",
                        fontSize = 110.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.accentPrimary,
                        letterSpacing = 4.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "$hijriDate • اضغط لتغيير شكل الساعة",
                style = MaterialTheme.typography.titleMedium,
                color = palette.textSecondary
            )
        }
    }
}

/**
 * لوحة القيادة الشاملة (Dashboard Hub View)
 */
@Composable
private fun StandbyDashboardHubView(
    clockDigits: String,
    secondsDigits: String,
    period: String,
    uiState: DailyManagerUiState,
    timerState: StudyTimerState,
    batteryLevel: Int,
    isBatteryCharging: Boolean,
    yearPercent: Int,
    daysRemaining: Int,
    ambientTracks: List<Pair<String, String>>,
    currentTrackIndex: Int,
    isPlayingMusic: Boolean,
    onTogglePlayMusic: () -> Unit,
    palette: StandbyColorPalette,
    onToggleTask: (String) -> Unit,
    onMarkPrayerDone: (PrayerType) -> Unit,
    onTogglePauseTimer: () -> Unit
) {
    val nextPrayer = uiState.prayerSchedule.nextPrayer

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // بطاقة الساعة الكبيرة + الصلاة
        Card(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = palette.cardBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "$clockDigits $period",
                        fontSize = 54.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = palette.accentPrimary
                    )
                    Text(
                        text = "الصلاة القادمة: ${nextPrayer.type.arabicName} (${nextPrayer.formatted12h})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.accentSecondary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔋 $batteryLevel٪${if (isBatteryCharging) " ⚡" else ""} • العام: $yearPercent٪",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.textSecondary
                    )
                    Surface(
                        color = palette.accentPrimary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.clickable { onMarkPrayerDone(nextPrayer.type) }
                    ) {
                        Text(
                            text = "صليت ✅",
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.accentPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // بطاقة المهام والموسيقى
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = palette.cardBg)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("مهام اليوم السريعة", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = palette.accentPrimary)
                    val activeTasks = uiState.mainTasks.filter { !it.isCompleted }.take(2)
                    for (t in activeTasks) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onToggleTask(t.id) }
                        ) {
                            Icon(Icons.Outlined.RadioButtonUnchecked, contentDescription = null, tint = palette.textSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis, color = palette.textPrimary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // مشغل الموسيقى المصغر
                Surface(
                    color = Color.White.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ambientTracks[currentTrackIndex].first, maxLines = 1, overflow = TextOverflow.Ellipsis, color = palette.accentPrimary, style = MaterialTheme.typography.labelMedium)
                            Text("أصوات التركيز", style = MaterialTheme.typography.labelSmall, color = palette.textSecondary)
                        }
                        IconButton(onClick = onTogglePlayMusic, modifier = Modifier.size(34.dp)) {
                            Icon(if (isPlayingMusic) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null, tint = palette.accentPrimary)
                        }
                    }
                }
            }
        }
    }
}
