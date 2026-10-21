package com.example.features.prayer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.QuranBookmarkEntity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.prayer.AthkarDataProvider
import com.example.core.prayer.MosqueLocationVerifier
import com.example.core.prayer.PrayerBellMode
import com.example.core.prayer.PrayerType
import com.example.core.prayer.UmmAlQuraCalculator
import com.example.data.AzkarLogEntity
import com.example.data.MosqueLocationEntity
import com.example.features.DailyManagerUiState
import com.example.features.DailyManagerViewModel
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldSoft
import com.example.ui.theme.GoldWarm
import com.example.ui.theme.MintAccent
import com.example.ui.theme.PriorityHighRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerScreen(
    uiState: DailyManagerUiState,
    viewModel: DailyManagerViewModel,
    initialSubScreen: String = "MAIN",
    onSubScreenConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bookmarks: List<QuranBookmarkEntity> by viewModel.quranBookmarks.collectAsStateWithLifecycle()
    var currentSubScreen by rememberSaveable { mutableStateOf("MAIN") }

    LaunchedEffect(initialSubScreen) {
        if (initialSubScreen != "MAIN") {
            currentSubScreen = initialSubScreen
            onSubScreenConsumed()
        }
    }

    var selectedSubTab by rememberSaveable { mutableIntStateOf(0) } // 0=الصلاة، 1=الختمة، 2=الأذكار
    var showMapPickerDialog by remember { mutableStateOf(false) }
    var showWirdDialog by remember { mutableStateOf(false) }
    var showKhatmahPlannerDialog by remember { mutableStateOf(false) }
    var showBookmarksDrawer by remember { mutableStateOf(false) }
    var quranInitialPage by rememberSaveable { mutableIntStateOf(1) }
    var activeAthkarCategory by remember { mutableStateOf<String?>(null) } // "MORNING", "EVENING", "AFTER_PRAYER", "SLEEP"
    var visibleAthkarCategories by rememberSaveable {
        mutableStateOf(setOf("MORNING", "EVENING", "AFTER_PRAYER", "SLEEP"))
    }
    var showCustomizeAthkarDialog by remember { mutableStateOf(false) }

    when (currentSubScreen) {
        "QURAN_READER" -> {
            QuranReaderScreen(
                initialPage = quranInitialPage,
                uiState = uiState,
                viewModel = viewModel,
                onBack = { currentSubScreen = "MAIN" }
            )
            return
        }
        "SETTINGS_AND_ALARMS" -> {
            PrayerSettingsAndAlarmsScreen(
                uiState = uiState,
                viewModel = viewModel,
                onBack = { currentSubScreen = "MAIN" }
            )
            return
        }
        "CALENDAR" -> {
            PrayerCalendarScreen(
                currentCity = uiState.selectedCity,
                onBack = { currentSubScreen = "MAIN" },
                viewModel = viewModel,
                uiState = uiState
            )
            return
        }
        "CITIES" -> {
            PrayerCitiesScreen(
                currentCityId = uiState.selectedCity.id,
                savedCityIds = uiState.savedCityIds,
                onUpdateSavedCities = { newIds -> viewModel.updateSavedCities(newIds) },
                onSelectCity = { cityId ->
                    viewModel.selectCity(cityId)
                    currentSubScreen = "MAIN"
                },
                onBack = { currentSubScreen = "MAIN" }
            )
            return
        }
    }

    val schedule = uiState.prayerSchedule
    val wird = uiState.wirdProgress

    var hasLocationPermission by remember {
        mutableStateOf(MosqueLocationVerifier.hasLocationPermission(context))
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        hasLocationPermission = granted
        if (granted) {
            viewModel.autoVerifyMosqueInBackground()
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            viewModel.autoVerifyMosqueInBackground()
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("prayer_screen_list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedSubTab == 0,
                    onClick = { selectedSubTab = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                    icon = { Icon(Icons.Default.Mosque, contentDescription = null, modifier = Modifier.size(18.dp)) }
                ) {
                    Text("الصلاة", fontWeight = FontWeight.Bold)
                }
                SegmentedButton(
                    selected = selectedSubTab == 1,
                    onClick = { selectedSubTab = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                    icon = { Icon(Icons.Default.AutoStories, contentDescription = null, modifier = Modifier.size(18.dp)) }
                ) {
                    Text("الختمة", fontWeight = FontWeight.Bold)
                }
                SegmentedButton(
                    selected = selectedSubTab == 2,
                    onClick = { selectedSubTab = 2 },
                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                    icon = { Icon(Icons.Default.Spa, contentDescription = null, modifier = Modifier.size(18.dp)) }
                ) {
                    Text("الأذكار", fontWeight = FontWeight.Bold)
                }
            }
        }

        when (selectedSubTab) {
            0 -> {
                // 1. المستطيل الفلكي السماوي مع شمس/قمر والعد التنازلي والصلاة السابقة والتالية
                item {
                    PrayerCelestialArcCard(
                        schedule = schedule,
                        onOpenSettingsClick = { currentSubScreen = "SETTINGS_AND_ALARMS" }
                    )
                }

                // 2. أزرار الوصول السريع: الضبط والمنبهات، تقويم الصلوات، مواقيت المدن
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { currentSubScreen = "SETTINGS_AND_ALARMS" }
                                .testTag("quick_open_settings_alarms_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("الضبط والتنبيهات", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { currentSubScreen = "CALENDAR" }
                                .testTag("quick_open_calendar_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تقويم الصلوات", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { currentSubScreen = "CITIES" }
                                .testTag("quick_open_cities_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("مواقيت المدن", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // 3. كل الصلوات الخمس في سطر واحد بدون الشروق وبدون إيموجيات أو منبهات
                item {
                    val displayPrayers = listOf(
                        PrayerType.FAJR,
                        PrayerType.DHUHR,
                        PrayerType.ASR,
                        PrayerType.MAGHRIB,
                        PrayerType.ISHA
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "مواقيت اليوم",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // الصلوات الخمس في سطر واحد
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            displayPrayers.forEach { pType ->
                                val slot = schedule.prayers.find { it.type == pType }
                                val status = uiState.todayPrayerStatusMap[pType] ?: "PENDING"
                                val isNext = pType == schedule.nextPrayer.type
                                val bellMode = uiState.prayerBellModesMap[pType] ?: if (pType.isObligatory) com.example.core.prayer.PrayerBellMode.AZAN else com.example.core.prayer.PrayerBellMode.SILENT

                                PrayerSquareCard(
                                    prayerType = pType,
                                    timeFormatted = slot?.formatted12h ?: "--:--",
                                    status = status,
                                    isNext = isNext,
                                    bellMode = bellMode,
                                    onClick = { currentSubScreen = "SETTINGS_AND_ALARMS" },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // 4. بوصلة القبلة
                item {
                    QiblaCompassCard(qiblaBearingDegrees = schedule.qiblaBearingDegrees.toFloat())
                }
            }

            1 -> {
                // قسم الورد القرآني والمصحف الشريف
                item {
                    val progressFraction = (wird.currentPage.toFloat() / 604f).coerceIn(0f, 1f)
                    val percentDone = (progressFraction * 100).toInt()
                    val totalRemainingPages = (604 - wird.currentPage + 1).coerceAtLeast(1)
                    val daysRemaining = ((totalRemainingPages + wird.dailyTargetPages - 1) / wird.dailyTargetPages).coerceAtLeast(1)

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
                            // Header: Title & Percent
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "الختمة الحالية",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = EmeraldDeep.copy(alpha = 0.12f)
                                        ) {
                                            Text(
                                                text = wird.khatmahTitle.ifBlank { "الختمة الحالية" },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = EmeraldDeep,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = UmmAlQuraCalculator.toArabicDigits(
                                            "الصفحة ${wird.currentPage} من 604 • الجزء ${wird.currentJuz}"
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Text(
                                    text = UmmAlQuraCalculator.toArabicDigits("$percentDone%"),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = EmeraldDeep
                                )
                            }

                            // Progress Bar
                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(CircleShape),
                                color = EmeraldDeep,
                                trackColor = EmeraldDeep.copy(alpha = 0.15f)
                            )

                            // 3 Information Pills
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("الهدف اليومي", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = UmmAlQuraCalculator.toArabicDigits("${wird.dailyTargetPages} ص/يوم"),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = EmeraldDeep
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("قراءة اليوم", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = UmmAlQuraCalculator.toArabicDigits("${wird.todayPagesRead} / ${wird.dailyTargetPages}"),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (wird.todayPagesRead >= wird.dailyTargetPages) EmeraldDeep else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text("المدة المقدرة", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            text = UmmAlQuraCalculator.toArabicDigits("$daysRemaining يوماً"),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = GoldWarm
                                        )
                                    }
                                }
                            }

                            // Large Primary Action: Open Quran Mushaf
                            Button(
                                onClick = {
                                    quranInitialPage = wird.currentPage
                                    currentSubScreen = "QURAN_READER"
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("open_mushaf_btn")
                            ) {
                                Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("فتح المصحف الشريف والتلاوة", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            // Secondary Quick Actions Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = { showKhatmahPlannerDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("خطة الختمة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                FilledTonalButton(
                                    onClick = { showBookmarksDrawer = true },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Bookmarks, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = UmmAlQuraCalculator.toArabicDigits("الفواصل (${bookmarks.size})"),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                OutlinedButton(
                                    onClick = { showWirdDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تسجيل يدوي", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Recent Bookmarks & Reflections List
                if (bookmarks.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Bookmark, contentDescription = null, tint = EmeraldDeep, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "علامات التوقف والتدبرات المحفوظة",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                    TextButton(onClick = { showBookmarksDrawer = true }) {
                                        Text("عرض الكل", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                bookmarks.take(3).forEach { bm ->
                                    val bmColor = try {
                                        Color(android.graphics.Color.parseColor(bm.colorHex))
                                    } catch (_: Exception) {
                                        EmeraldDeep
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                quranInitialPage = bm.pageNumber
                                                currentSubScreen = "QURAN_READER"
                                            }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(bmColor)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Column {
                                                    Text(bm.title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                    Text(
                                                        text = UmmAlQuraCalculator.toArabicDigits("صفحة ${bm.pageNumber} • ${bm.surahName}"),
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Icon(
                                                Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // قسم الأذكار
                item {
                    val isMorningHabitOn = uiState.habits.any { it.autoSource == "AZKAR_MORNING" && it.deletedAt == null }
                    val isEveningHabitOn = uiState.habits.any { it.autoSource == "AZKAR_EVENING" && it.deletedAt == null }
                    val isAfterPrayerHabitOn = uiState.habits.any { it.autoSource == "AZKAR_AFTER_PRAYER" && it.deletedAt == null }
                    val isSleepHabitOn = uiState.habits.any { it.autoSource == "AZKAR_SLEEP" && it.deletedAt == null }

                    val morningLog = uiState.todayAzkarLogs.find { it.categoryCode == "MORNING" }
                    val eveningLog = uiState.todayAzkarLogs.find { it.categoryCode == "EVENING" }
                    val afterPrayerLog = uiState.todayAzkarLogs.find { it.categoryCode == "AFTER_PRAYER" }
                    val sleepLog = uiState.todayAzkarLogs.find { it.categoryCode == "SLEEP" }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // كرت تمهيدي أنيق مع زر تخصيص الأذكار
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                        Icon(Icons.Default.Spa, contentDescription = null, tint = EmeraldDeep)
                                        Text(
                                            text = "أذكار حصن المسلم المأثورة",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Black
                                        )
                                    }

                                    // زر تخصيص الأذكار التي تظهر للمستخدم
                                    FilledTonalButton(
                                        onClick = { showCustomizeAthkarDialog = true },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = EmeraldDeep.copy(alpha = 0.12f),
                                            contentColor = EmeraldDeep
                                        ),
                                        modifier = Modifier.testTag("customize_athkar_btn")
                                    ) {
                                        Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تخصيص الأذكار", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(
                                    text = "اقرأ أذكارك المأثورة بالبطاقات التفاعلية مع عداد التسبيح الذكي. يمكنك تخصيص الأبواب التي تظهر لك وتفعيلها كعادات يومية.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 20.sp
                                )
                            }
                        }

                        // 1. بطاقة أذكار الصباح
                        if (visibleAthkarCategories.contains("MORNING")) {
                            HeroAthkarCategoryCard(
                                title = "أذكار الصباح",
                                subtitle = "١٣ ذكراً مأثوراً لحفظ اليوم والبركة والنشاط",
                                isMorning = true,
                                icon = Icons.Default.WbSunny,
                                iconColor = GoldWarm,
                                iconBgColor = GoldSoft.copy(alpha = 0.25f),
                                isCompletedToday = morningLog?.isCompleted == true,
                                isHabitEnabled = isMorningHabitOn,
                                onToggleHabit = { enabled ->
                                    viewModel.toggleAthkarHabit("MORNING", enabled)
                                },
                                onOpenReader = {
                                    activeAthkarCategory = "MORNING"
                                }
                            )
                        }

                        // 2. بطاقة أذكار المساء
                        if (visibleAthkarCategories.contains("EVENING")) {
                            HeroAthkarCategoryCard(
                                title = "أذكار المساء",
                                subtitle = "١٣ ذكراً مأثوراً لتحصين النفس قبل الغروب وحفظ الليلة",
                                isMorning = false,
                                icon = Icons.Default.NightlightRound,
                                iconColor = Color(0xFF818CF8),
                                iconBgColor = Color(0xFF6366F1).copy(alpha = 0.2f),
                                isCompletedToday = eveningLog?.isCompleted == true,
                                isHabitEnabled = isEveningHabitOn,
                                onToggleHabit = { enabled ->
                                    viewModel.toggleAthkarHabit("EVENING", enabled)
                                },
                                onOpenReader = {
                                    activeAthkarCategory = "EVENING"
                                }
                            )
                        }

                        // 3. بطاقة أذكار بعد الصلاة المكتوبة
                        if (visibleAthkarCategories.contains("AFTER_PRAYER")) {
                            HeroAthkarCategoryCard(
                                title = "أذكار بعد الصلاة",
                                subtitle = "٧ أذكار نبوية مأثورة دبر كل صلاة مكتوبة",
                                isMorning = true,
                                icon = Icons.Default.Mosque,
                                iconColor = EmeraldDeep,
                                iconBgColor = EmeraldDeep.copy(alpha = 0.15f),
                                isCompletedToday = afterPrayerLog?.isCompleted == true,
                                isHabitEnabled = isAfterPrayerHabitOn,
                                onToggleHabit = { enabled ->
                                    viewModel.toggleAthkarHabit("AFTER_PRAYER", enabled)
                                },
                                onOpenReader = {
                                    activeAthkarCategory = "AFTER_PRAYER"
                                }
                            )
                        }

                        // 4. بطاقة أذكار النوم
                        if (visibleAthkarCategories.contains("SLEEP")) {
                            HeroAthkarCategoryCard(
                                title = "أذكار النوم",
                                subtitle = "٧ أذكار نبوية شريفة قبل النوم للتحصين والسكينة",
                                isMorning = false,
                                icon = Icons.Default.Bedtime,
                                iconColor = Color(0xFFF59E0B),
                                iconBgColor = Color(0xFFF59E0B).copy(alpha = 0.18f),
                                isCompletedToday = sleepLog?.isCompleted == true,
                                isHabitEnabled = isSleepHabitOn,
                                onToggleHabit = { enabled ->
                                    viewModel.toggleAthkarHabit("SLEEP", enabled)
                                },
                                onOpenReader = {
                                    activeAthkarCategory = "SLEEP"
                                }
                            )
                        }

                        // باقي الأذكار الحرة
                        val otherAzkar = uiState.todayAzkarLogs.filter {
                            it.categoryCode != "MORNING" && it.categoryCode != "EVENING" && it.categoryCode != "AFTER_PRAYER" && it.categoryCode != "SLEEP"
                        }
                        if (otherAzkar.isNotEmpty()) {
                            Text(
                                text = "أبواب الذكر والتسبيح الأخرى",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                            )
                            otherAzkar.forEach { azkar ->
                                AzkarCounterCard(
                                    azkar = azkar,
                                    onTapCount = { viewModel.progressAzkar(azkar, completeImmediately = false) },
                                    onReset = { viewModel.resetAzkar(azkar) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (activeAthkarCategory != null) {
        val catCode = activeAthkarCategory!!
        val catTitle = when (catCode) {
            "MORNING" -> "أذكار الصباح"
            "EVENING" -> "أذكار المساء"
            "AFTER_PRAYER" -> "أذكار بعد الصلاة"
            "SLEEP" -> "أذكار النوم"
            else -> "الأذكار المأثورة"
        }
        val dhikrList = AthkarDataProvider.getAthkarByCategory(catCode)

        InteractiveAthkarReaderDialog(
            categoryCode = catCode,
            categoryTitle = catTitle,
            items = dhikrList,
            onFinish = {
                viewModel.completeAthkarCategory(catCode)
                activeAthkarCategory = null
            },
            onDismiss = {
                activeAthkarCategory = null
            }
        )
    }

    // نافذة تخصيص الأذكار التي تظهر للمستخدم
    if (showCustomizeAthkarDialog) {
        Dialog(
            onDismissRequest = { showCustomizeAthkarDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
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
                            text = "تخصيص الأذكار المعروضة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        IconButton(onClick = { showCustomizeAthkarDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "إغلاق")
                        }
                    }

                    Text(
                        text = "اختر أبواب الأذكار التي ترغب في ظهورها في قائمتك اليومية:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val categoriesOptions = listOf(
                        Triple("MORNING", "أذكار الصباح", "١٣ ذكراً مأثوراً لحفظ اليوم"),
                        Triple("EVENING", "أذكار المساء", "١٣ ذكراً مأثوراً لحفظ الليلة"),
                        Triple("AFTER_PRAYER", "أذكار بعد الصلاة", "الأذكار والتسبيحات دبر الصلوات الخمس"),
                        Triple("SLEEP", "أذكار النوم", "الأذكار والتحصينات النبوية عند المنام")
                    )

                    categoriesOptions.forEach { (code, title, desc) ->
                        val isChecked = visibleAthkarCategories.contains(code)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    visibleAthkarCategories = if (isChecked) {
                                        if (visibleAthkarCategories.size > 1) visibleAthkarCategories - code else visibleAthkarCategories
                                    } else {
                                        visibleAthkarCategories + code
                                    }
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isChecked) EmeraldDeep.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            border = if (isChecked) androidx.compose.foundation.BorderStroke(1.dp, EmeraldDeep) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(text = desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        visibleAthkarCategories = if (checked) {
                                            visibleAthkarCategories + code
                                        } else {
                                            if (visibleAthkarCategories.size > 1) visibleAthkarCategories - code else visibleAthkarCategories
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { showCustomizeAthkarDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("تم وحفظ الاختيارات", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showMapPickerDialog) {
        val userLat = uiState.userGpsPoint?.latitude ?: uiState.selectedCity.latitude
        val userLng = uiState.userGpsPoint?.longitude ?: uiState.selectedCity.longitude
        InteractiveMosqueMapPickerDialog(
            userLat = userLat,
            userLng = userLng,
            existingMosques = uiState.mosqueLocations,
            onDismiss = { showMapPickerDialog = false },
            onConfirmMosque = { name, lat, lng, radius ->
                viewModel.addMosqueWithCoordinates(name, lat, lng, radius)
                showMapPickerDialog = false
            }
        )
    }

    if (showWirdDialog) {
        WirdLoggingDialog(
            currentSurah = wird.bookmarkSurahName,
            currentAyah = wird.bookmarkAyahNumber,
            onDismiss = { showWirdDialog = false },
            onConfirm = { pages, surah, ayah ->
                viewModel.logWirdReading(pages, surah, ayah, "")
                showWirdDialog = false
            }
        )
    }

    if (showKhatmahPlannerDialog) {
        KhatmahPlannerDialog(
            wirdProgress = wird,
            onSavePlan = { targetPages, planType, title, startPage ->
                viewModel.configureKhatmahPlan(targetPages, planType, title, startPage)
                quranInitialPage = startPage
                showKhatmahPlannerDialog = false
            },
            onDismiss = { showKhatmahPlannerDialog = false }
        )
    }

    if (showBookmarksDrawer) {
        BookmarksListDialog(
            bookmarks = bookmarks,
            onSelectBookmark = { bookmark ->
                quranInitialPage = bookmark.pageNumber
                currentSubScreen = "QURAN_READER"
                showBookmarksDrawer = false
            },
            onDeleteBookmark = { bookmarkId ->
                viewModel.deleteQuranBookmark(bookmarkId)
            },
            onDismiss = { showBookmarksDrawer = false }
        )
    }
}

@Composable
private fun HeroAthkarCategoryCard(
    title: String,
    subtitle: String,
    isMorning: Boolean = true,
    icon: androidx.compose.ui.graphics.vector.ImageVector = if (isMorning) Icons.Default.WbSunny else Icons.Default.NightlightRound,
    iconColor: Color = if (isMorning) GoldWarm else Color(0xFF818CF8),
    iconBgColor: Color = if (isMorning) GoldSoft.copy(alpha = 0.25f) else Color(0xFF6366F1).copy(alpha = 0.2f),
    isCompletedToday: Boolean,
    isHabitEnabled: Boolean,
    onToggleHabit: (Boolean) -> Unit,
    onOpenReader: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCompletedToday) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = iconBgColor,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )
                            if (isCompletedToday) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = EmeraldDeep.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "مكتملة اليوم",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldDeep,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // خيار تفعيل كعادة في صفحة العادات
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.TrackChanges,
                        contentDescription = null,
                        tint = if (isHabitEnabled) EmeraldDeep else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "تتبع كعادة يومية في صفحة العادات",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isHabitEnabled) "مفعلة — ستُنجز تلقائياً بقراءتها هنا" else "غير مفعلة في صفحة العادات",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isHabitEnabled,
                    onCheckedChange = onToggleHabit,
                    modifier = Modifier.testTag("toggle_habit_${if (isMorning) "morning" else "evening"}")
                )
            }

            // زر فتح قارئ الأذكار التفاعلي
            Button(
                onClick = onOpenReader,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("open_reader_${if (isMorning) "morning" else "evening"}"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMorning) EmeraldDeep else Color(0xFF4F46E5)
                )
            ) {
                Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isCompletedToday) "إعادة قراءة $title" else "ابدأ قراءة $title الآن",
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
fun MosqueLocationsSectionCard(
    mosques: List<MosqueLocationEntity>,
    hasLocationPermission: Boolean,
    onRequestPermission: () -> Unit,
    onOpenMapPicker: () -> Unit,
    onDeleteMosque: (String) -> Unit,
    onOpenInGoogleMaps: (MosqueLocationEntity) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "المساجد المعتمدة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "يتحقق التطبيق تلقائياً في الخلفية عند دخول وقت الصلاة",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onOpenMapPicker,
                    modifier = Modifier.testTag("open_mosque_map_picker_btn")
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تحديد من الخريطة", fontWeight = FontWeight.Bold)
                }
            }

            if (!hasLocationPermission) {
                FilledTonalButton(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تفعيل صلاحية الموقع للتحقق التلقائي", fontWeight = FontWeight.Bold)
                }
            }

            if (mosques.isEmpty()) {
                Text(
                    text = "لم يتم تحديد مسجد بعد. اضغط «تحديد من الخريطة» لاختيار موقع المسجد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                mosques.forEach { mosque ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mosque,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column {
                                    Text(
                                        text = mosque.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = UmmAlQuraCalculator.toArabicDigits("نطاق التحقق: ${mosque.radiusMeters} متر"),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row {
                                IconButton(onClick = { onOpenInGoogleMaps(mosque) }) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInNew,
                                        contentDescription = "فتح في خرائط جوجل",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = { onDeleteMosque(mosque.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "حذف",
                                        tint = MaterialTheme.colorScheme.error
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
 * ذاكرة تخزين مؤقت لبلاطات الخريطة الحية (Google Maps / OpenStreetMap)
 */
private object SlippyTileCache {
    val memoryCache = ConcurrentHashMap<String, Bitmap>()

    suspend fun fetchTile(z: Int, x: Int, y: Int): Bitmap? {
        val maxTile = 1 shl z
        val wrappedX = ((x % maxTile) + maxTile) % maxTile
        if (y < 0 || y >= maxTile) return null
        val key = "$z/$wrappedX/$y"
        memoryCache[key]?.let { return it }

        return withContext(Dispatchers.IO) {
            val urls = listOf(
                "https://mt1.google.com/vt/lyrs=m&hl=ar&x=$wrappedX&y=$y&z=$z",
                "https://tile.openstreetmap.org/$z/$wrappedX/$y.png"
            )
            for (urlStr in urls) {
                try {
                    val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 4000
                        readTimeout = 4000
                        setRequestProperty("User-Agent", "DailyManagerAndroid/2.0")
                    }
                    if (conn.responseCode == 200) {
                        val bmp = conn.inputStream.use { BitmapFactory.decodeStream(it) }
                        if (bmp != null) {
                            memoryCache[key] = bmp
                            return@withContext bmp
                        }
                    }
                } catch (_: Exception) {
                }
            }
            null
        }
    }
}

/**
 * نافذة خريطة تفاعلية حقيقية مرسومة مباشرة بـ Compose Canvas + بلاطات خرائط جوجل الحية:
 * - لا تظهر شاشة بيضاء أبداً حتى عند ضعف الاتصال
 * - تدعم السحب الحر باللمس، التكبير والتصغير، والبحث المباشر عن اسم المسجد أو الحي
 * - تعرض موقع المستخدم الحالي (نقطة زرقاء) وموقع المسجد المختار والمسافة الفعلية بينهما بالمتر
 */
@Composable
fun InteractiveMosqueMapPickerDialog(
    userLat: Double,
    userLng: Double,
    existingMosques: List<MosqueLocationEntity>,
    onDismiss: () -> Unit,
    onConfirmMosque: (name: String, lat: Double, lng: Double, radiusMeters: Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var mosqueName by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var searchFeedback by remember { mutableStateOf<String?>(null) }

    // يبدأ دبوس المسجد على بُعد ~180 متر شمال شرق موقع المستخدم حتى لا يُحفظ فوق موقع المنزل بالخطأ،
    // ويمكن للمستخدم سحب الخريطة أو النقر على موقعه إن كان داخل المسجد فعلياً الآن
    var selectedLat by remember { mutableDoubleStateOf(userLat + 0.0014) }
    var selectedLng by remember { mutableDoubleStateOf(userLng + 0.0014) }
    var zoom by remember { mutableIntStateOf(16) }
    var radiusMeters by remember { mutableFloatStateOf(100f) }
    var tileTrigger by remember { mutableIntStateOf(0) }

    val distanceFromUserMeters = remember(userLat, userLng, selectedLat, selectedLng) {
        MosqueLocationVerifier.calculateDistanceMeters(
            userLat,
            userLng,
            selectedLat,
            selectedLng
        )
    }

    // تحميل بلاطات الخريطة المحيطة بالمركز الحالي
    LaunchedEffect(selectedLat, selectedLng, zoom) {
        val centerTileX = lonToTileX(selectedLng, zoom).toInt()
        val centerTileY = latToTileY(selectedLat, zoom).toInt()
        for (dx in -2..2) {
            for (dy in -2..2) {
                launch {
                    val bmp = SlippyTileCache.fetchTile(zoom, centerTileX + dx, centerTileY + dy)
                    if (bmp != null) {
                        tileTrigger++
                    }
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تحديد موقع المسجد على الخريطة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                // شريط البحث عن المسجد أو الحي (يعمل عبر Android Geocoder + Nominatim)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("ابحث باسم المسجد أو الحي") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalIconButton(
                        onClick = {
                            val q = searchQuery.trim()
                            if (q.isNotEmpty()) {
                                scope.launch {
                                    val found = searchLocationByQuery(context, q)
                                    if (found != null) {
                                        selectedLat = found.first
                                        selectedLng = found.second
                                        if (mosqueName.isBlank()) mosqueName = q
                                        searchFeedback = null
                                    } else {
                                        searchFeedback = "تعذر العثور على الاسم، اسحب الخريطة مباشرة إلى موقع المسجد"
                                    }
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "بحث")
                    }
                }

                searchFeedback?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                // لوحة الخريطة التفاعلية المباشرة (300dp ارتفاع ثابت ومضمون)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(290.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFFE5ECE9))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
                ) {
                    // قراءة tileTrigger لإعادة رسم البلاطات فور وصولها
                    val redrawCount = tileTrigger

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(zoom) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val worldSize = 256.0 * (1 shl zoom)
                                    val cTileX = lonToTileX(selectedLng, zoom) - (dragAmount.x / 256.0)
                                    val cTileY = latToTileY(selectedLat, zoom) - (dragAmount.y / 256.0)
                                    selectedLng = tileXToLon(cTileX, zoom)
                                    selectedLat = tileYToLat(cTileY, zoom).coerceIn(-85.0, 85.0)
                                    val _ignore = worldSize
                                }
                            }
                            .pointerInput(zoom) {
                                detectTapGestures { tapOffset ->
                                    val cx = size.width / 2.0
                                    val cy = size.height / 2.0
                                    val dxPixels = tapOffset.x - cx
                                    val dyPixels = tapOffset.y - cy
                                    val cTileX = lonToTileX(selectedLng, zoom) + (dxPixels / 256.0)
                                    val cTileY = latToTileY(selectedLat, zoom) + (dyPixels / 256.0)
                                    selectedLng = tileXToLon(cTileX, zoom)
                                    selectedLat = tileYToLat(cTileY, zoom).coerceIn(-85.0, 85.0)
                                }
                            }
                    ) {
                        val _keep = redrawCount
                        val w = size.width
                        val h = size.height
                        val centerX = w / 2f
                        val centerY = h / 2f

                        // 1. خلفية خريطة الشوارع الاحتياطية (تظهر فوراً قبل اكتمال تحميل البلاطات)
                        drawRect(color = Color(0xFFEDF2EF))
                        val gridStep = 64f
                        val offsetX = ((lonToTileX(selectedLng, zoom) * 256.0) % gridStep).toFloat()
                        val offsetY = ((latToTileY(selectedLat, zoom) * 256.0) % gridStep).toFloat()
                        var gx = -offsetX
                        while (gx < w) {
                            drawLine(
                                color = Color(0xFFD5E0DA),
                                start = Offset(gx, 0f),
                                end = Offset(gx, h),
                                strokeWidth = 2f
                            )
                            gx += gridStep
                        }
                        var gy = -offsetY
                        while (gy < h) {
                            drawLine(
                                color = Color(0xFFD5E0DA),
                                start = Offset(0f, gy),
                                end = Offset(w, gy),
                                strokeWidth = 2f
                            )
                            gy += gridStep
                        }

                        // 2. رسم بلاطات الخريطة الحقيقية (Google Maps / OSM)
                        val exactCenterTileX = lonToTileX(selectedLng, zoom)
                        val exactCenterTileY = latToTileY(selectedLat, zoom)
                        val baseTileX = floor(exactCenterTileX).toInt()
                        val baseTileY = floor(exactCenterTileY).toInt()
                        val maxTile = 1 shl zoom

                        for (dx in -2..2) {
                            for (dy in -2..2) {
                                val tx = baseTileX + dx
                                val ty = baseTileY + dy
                                val wrappedX = ((tx % maxTile) + maxTile) % maxTile
                                val key = "$zoom/$wrappedX/$ty"
                                val bmp = SlippyTileCache.memoryCache[key]
                                if (bmp != null) {
                                    val screenX = (centerX + ((tx - exactCenterTileX) * 256.0)).roundToInt()
                                    val screenY = (centerY + ((ty - exactCenterTileY) * 256.0)).roundToInt()
                                    drawImage(
                                        image = bmp.asImageBitmap(),
                                        dstOffset = IntOffset(screenX, screenY),
                                        dstSize = IntSize(256, 256)
                                    )
                                }
                            }
                        }

                        // 3. رسم المساجد المحفوظة مسبقاً على الخريطة
                        val metersPerPixel = (156543.03392 * cos(Math.toRadians(selectedLat))) / (1 shl zoom)
                        existingMosques.forEach { m ->
                            val mx = (centerX + ((lonToTileX(m.longitude, zoom) - exactCenterTileX) * 256.0)).toFloat()
                            val my = (centerY + ((latToTileY(m.latitude, zoom) - exactCenterTileY) * 256.0)).toFloat()
                            if (mx in 0f..w && my in 0f..h) {
                                drawCircle(
                                    color = Color(0xFF0D9488).copy(alpha = 0.25f),
                                    radius = (m.radiusMeters / metersPerPixel).toFloat().coerceIn(14f, 120f),
                                    center = Offset(mx, my)
                                )
                                drawCircle(
                                    color = Color(0xFF0D9488),
                                    radius = 7f,
                                    center = Offset(mx, my)
                                )
                            }
                        }

                        // 4. رسم موقع المستخدم الحالي (نقطة GPS الزرقاء)
                        val userScreenX = (centerX + ((lonToTileX(userLng, zoom) - exactCenterTileX) * 256.0)).toFloat()
                        val userScreenY = (centerY + ((latToTileY(userLat, zoom) - exactCenterTileY) * 256.0)).toFloat()
                        if (userScreenX in -40f..(w + 40f) && userScreenY in -40f..(h + 40f)) {
                            drawCircle(
                                color = Color(0xFF2563EB).copy(alpha = 0.22f),
                                radius = 22f,
                                center = Offset(userScreenX, userScreenY)
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 10f,
                                center = Offset(userScreenX, userScreenY)
                            )
                            drawCircle(
                                color = Color(0xFF2563EB),
                                radius = 7f,
                                center = Offset(userScreenX, userScreenY)
                            )
                        }

                        // 5. رسم دائرة نطاق المسجد المختار في منتصف الخريطة + دبوس المسجد
                        val radiusPixels = (radiusMeters / metersPerPixel).toFloat().coerceIn(22f, min(w, h) * 0.45f)
                        drawCircle(
                            color = Color(0xFF145946).copy(alpha = 0.22f),
                            radius = radiusPixels,
                            center = Offset(centerX, centerY)
                        )
                        drawCircle(
                            color = Color(0xFF145946),
                            radius = radiusPixels,
                            center = Offset(centerX, centerY),
                            style = Stroke(width = 3f)
                        )
                        // دبوس المسجد في المركز
                        drawCircle(
                            color = Color.White,
                            radius = 15f,
                            center = Offset(centerX, centerY)
                        )
                        drawCircle(
                            color = Color(0xFF145946),
                            radius = 11f,
                            center = Offset(centerX, centerY)
                        )
                    }

                    // شريط علوي عائم فوق الخريطة يعرض المسافة بين موقعك والدبوس المحدد
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0A2E23).copy(alpha = 0.88f),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp)
                    ) {
                        Text(
                            text = UmmAlQuraCalculator.toArabicDigits(
                                "اسحب الخريطة لموقع المسجد • يبعد عن موقعك: $distanceFromUserMeters متر"
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    // أزرار التكبير والتصغير والانتقال لموقعي الحالي (في حال كان المستخدم داخل المسجد الآن)
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SmallFloatingActionButton(
                            onClick = { zoom = (zoom + 1).coerceAtMost(19) },
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "تكبير")
                        }
                        SmallFloatingActionButton(
                            onClick = { zoom = (zoom - 1).coerceAtLeast(11) },
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "تصغير")
                        }
                        SmallFloatingActionButton(
                            onClick = {
                                selectedLat = userLat
                                selectedLng = userLng
                            },
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Icon(Icons.Default.MyLocation, contentDescription = "موقعي الحالي")
                        }
                    }
                }

                OutlinedTextField(
                    value = mosqueName,
                    onValueChange = { mosqueName = it },
                    label = { Text("اسم المسجد") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "نطاق المسجد المسموح",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = UmmAlQuraCalculator.toArabicDigits("${radiusMeters.roundToInt()} متر"),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = radiusMeters,
                        onValueChange = { radiusMeters = it },
                        valueRange = 50f..250f
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء")
                    }
                    Button(
                        onClick = {
                            onConfirmMosque(
                                mosqueName.trim().ifBlank { "مسجد معتمد" },
                                selectedLat,
                                selectedLng,
                                radiusMeters.roundToInt()
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("confirm_save_mosque_btn")
                    ) {
                        Text("حفظ المسجد", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

private fun lonToTileX(lon: Double, zoom: Int): Double {
    return ((lon + 180.0) / 360.0) * (1 shl zoom)
}

private fun latToTileY(lat: Double, zoom: Int): Double {
    val latRad = Math.toRadians(lat.coerceIn(-85.0511, 85.0511))
    return (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / PI) / 2.0 * (1 shl zoom)
}

private fun tileXToLon(tileX: Double, zoom: Int): Double {
    return tileX / (1 shl zoom) * 360.0 - 180.0
}

private fun tileYToLat(tileY: Double, zoom: Int): Double {
    val n = PI - 2.0 * PI * tileY / (1 shl zoom)
    return Math.toDegrees(atan(0.5 * (exp(n) - exp(-n))))
}

private suspend fun searchLocationByQuery(context: Context, query: String): Pair<Double, Double>? {
    return withContext(Dispatchers.IO) {
        try {
            if (Geocoder.isPresent()) {
                @Suppress("DEPRECATION")
                val results = Geocoder(context, Locale("ar")).getFromLocationName(query, 1)
                val first = results?.firstOrNull()
                if (first != null) {
                    return@withContext first.latitude to first.longitude
                }
            }
        } catch (_: Exception) {
        }

        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = URL("https://nominatim.openstreetmap.org/search?format=json&q=$encoded&limit=1")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 4000
                setRequestProperty("User-Agent", "DailyManagerAndroid/2.0")
            }
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val arr = JSONArray(body)
                if (arr.length() > 0) {
                    val obj = arr.getJSONObject(0)
                    val lat = obj.getString("lat").toDoubleOrNull()
                    val lon = obj.getString("lon").toDoubleOrNull()
                    if (lat != null && lon != null) {
                        return@withContext lat to lon
                    }
                }
            }
        } catch (_: Exception) {
        }
        null
    }
}

@Composable
private fun QiblaCompassCard(qiblaBearingDegrees: Float) {
    val context = LocalContext.current
    var deviceAzimuth by remember { mutableFloatStateOf(0f) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotMatrix = FloatArray(9)
                    val orientation = FloatArray(3)
                    SensorManager.getRotationMatrixFromVector(rotMatrix, event.values)
                    SensorManager.getOrientation(rotMatrix, orientation)
                    val deg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                    deviceAzimuth = (deg + 360f) % 360f
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose {
            sensorManager?.unregisterListener(listener)
        }
    }

    val relativeAngle = (qiblaBearingDegrees - deviceAzimuth + 360f) % 360f

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "اتجاه القبلة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = UmmAlQuraCalculator.toArabicDigits("زاوية القبلة: ${qiblaBearingDegrees.toInt()}° من الشمال"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val primaryColor = MaterialTheme.colorScheme.primary
            Canvas(modifier = Modifier.size(72.dp)) {
                val radius = size.minDimension / 2f
                drawCircle(
                    color = primaryColor.copy(alpha = 0.15f),
                    radius = radius,
                    style = Stroke(width = 6f)
                )
                val rad = Math.toRadians((relativeAngle - 90f).toDouble())
                val endX = center.x + (radius * 0.75f * cos(rad)).toFloat()
                val endY = center.y + (radius * 0.75f * sin(rad)).toFloat()
                drawLine(
                    color = primaryColor,
                    start = center,
                    end = Offset(endX, endY),
                    strokeWidth = 8f,
                    cap = StrokeCap.Round
                )
                drawCircle(color = GoldWarm, radius = 6f, center = Offset(endX, endY))
            }
        }
    }
}

@Composable
private fun AzkarCounterCard(
    azkar: AzkarLogEntity,
    onTapCount: () -> Unit,
    onReset: () -> Unit
) {
    val progress = (azkar.currentCount.toFloat() / azkar.targetCount.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onTapCount() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (azkar.isCompleted) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = azkar.categoryTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = UmmAlQuraCalculator.toArabicDigits("${azkar.currentCount} / ${azkar.targetCount}"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    IconButton(onClick = onReset, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Refresh, contentDescription = "إعادة", modifier = Modifier.size(18.dp))
                    }
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(CircleShape)
            )
        }
    }
}

@Composable
private fun WirdLoggingDialog(
    currentSurah: String,
    currentAyah: Int,
    onDismiss: () -> Unit,
    onConfirm: (pages: Int, surah: String, ayah: Int) -> Unit
) {
    var pagesText by remember { mutableStateOf("2") }
    var surahText by remember { mutableStateOf(currentSurah) }
    var ayahText by remember { mutableStateOf(currentAyah.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسجيل قراءة الورد", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = pagesText,
                    onValueChange = { pagesText = it.filter { c -> c.isDigit() } },
                    label = { Text("عدد الصفحات المقروءة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = surahText,
                    onValueChange = { surahText = it },
                    label = { Text("اسم السورة الحالية") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ayahText,
                    onValueChange = { ayahText = it.filter { c -> c.isDigit() } },
                    label = { Text("رقم الآية") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        (pagesText.toIntOrNull() ?: 1).coerceAtLeast(1),
                        surahText.trim(),
                        (ayahText.toIntOrNull() ?: 1).coerceAtLeast(1)
                    )
                }
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
fun PrayerSquareCard(
    prayerType: PrayerType,
    timeFormatted: String,
    status: String,
    isNext: Boolean,
    bellMode: com.example.core.prayer.PrayerBellMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDone = status == "ON_TIME" || status == "DONE" || status == "PRAYED"

    Card(
        modifier = modifier
            .height(48.dp)
            .clickable(onClick = onClick)
            .testTag("prayer_square_${prayerType.id}"),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) MintAccent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isDone) 1.5.dp else 1.dp,
            color = if (isDone) MintAccent else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 2.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = prayerType.arabicName,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                fontWeight = FontWeight.ExtraBold,
                color = if (isDone) MintAccent else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = timeFormatted,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Bold,
                color = if (isDone) MintAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

