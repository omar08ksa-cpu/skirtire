package com.example.features.fullday

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.fullday.IcsCalendarParser
import com.example.core.prayer.UmmAlQuraCalculator
import com.example.data.AzkarLogEntity
import com.example.data.LectureEntity
import com.example.features.DailyManagerUiState

@Composable
fun FullDayScreen(
    uiState: DailyManagerUiState,
    onLogWirdReading: (pagesRead: Int, surah: String, ayah: Int, note: String) -> Unit,
    onProgressAzkar: (AzkarLogEntity, Boolean) -> Unit,
    onAddLecture: (title: String, code: String, location: String, day: Int, start: String, end: String, reminder: Int) -> Unit,
    onImportSampleIcs: () -> Unit,
    onImportCustomIcs: (String) -> Unit,
    onDeleteLecture: (String) -> Unit,
    onTestLectureReminder: (LectureEntity) -> Unit,
    onSyncHealthData: (steps: Int, burnedCal: Int, sleepHours: Float) -> Unit,
    onAddMealCalorie: (mealDesc: String, calories: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedSection by rememberSaveable { mutableIntStateOf(0) } // 0=الكل، 1=الورد والأذكار، 2=المحاضرات، 3=الصحة
    var showWirdDialog by remember { mutableStateOf(false) }
    var showAddLectureDialog by remember { mutableStateOf(false) }
    var showIcsImportDialog by remember { mutableStateOf(false) }
    var showAddMealDialog by remember { mutableStateOf(false) }
    var showHealthSyncDialog by remember { mutableStateOf(false) }

    // قراءة حساس الخطوات الحقيقي في الجوال إن وجد
    var hardwareSensorSteps by remember { mutableStateOf<Int?>(null) }
    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                val count = event?.values?.firstOrNull()?.toInt()
                if (count != null && count > 0) {
                    hardwareSensorSteps = count
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (stepSensor != null) {
            sensorManager.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose {
            try {
                sensorManager?.unregisterListener(listener)
            } catch (_: Exception) {
            }
        }
    }

    val wird = uiState.wirdProgress
    val khatmahPercent = ((wird.currentPage.toFloat() / 604f) * 100f).toInt().coerceIn(1, 100)
    val health = uiState.todayHealth

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("fullday_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "المرحلة ٥: اليوم الكامل (الورد، الأذكار، المحاضرات، والصحة)",
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = "كل قسم مربوط تلقائيًا بعاداته في قاعدة البيانات المحلية (٥ مصادر تلقائية: الصلاة، الأذكار، الورد، المذاكرة، والخطوات).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // شريط فلترة سريع لأقسام المرحلة الخامسة
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tabs = listOf(
                    0 to "الكل (شامل)",
                    1 to "📖 الورد والأذكار",
                    2 to "🎓 المحاضرات (.ics)",
                    3 to "🏃 الصحة والسعرات"
                )
                tabs.forEach { (idx, title) ->
                    FilterChip(
                        selected = selectedSection == idx,
                        onClick = { selectedSection = idx },
                        label = { Text(title) },
                        modifier = Modifier.testTag("fullday_filter_$idx")
                    )
                }
            }
        }

        // ١. بطاقة الورد القرآني والختمة (مصحف المدينة المنورة - ٦٠٤ صفحات)
        if (selectedSection == 0 || selectedSection == 1) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF0D3B2E),
                                        Color(0xFF1B5E4A)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoStories,
                                        contentDescription = null,
                                        tint = Color(0xFFE6B85C)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "الورد القرآني ومتابعة الختمة",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Surface(
                                    color = Color(0xFFE6B85C),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "الختمة رقم ${UmmAlQuraCalculator.toArabicDigits((wird.completedKhatmahCount + 1).toString())}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color(0xFF1A160B),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Text(
                                text = "فهرس مصحف المدينة المنورة الموثق (٦٠٤ صفحات • ٣٠ جزءًا) — يحفظ علامة الوقف ويربط إتمام الورد بعادتك تلقائيًا.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFE5DEC9)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column {
                                    Text(
                                        text = "الصفحة ${UmmAlQuraCalculator.toArabicDigits(wird.currentPage.toString())} من ٦٠٤",
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = Color(0xFFE6B85C),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "الجزء ${UmmAlQuraCalculator.toArabicDigits(wird.currentJuz.toString())} • قراءة اليوم: ${UmmAlQuraCalculator.toArabicDigits(wird.todayPagesRead.toString())} صفحات",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = "${UmmAlQuraCalculator.toArabicDigits(khatmahPercent.toString())}٪",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color(0xFFE6B85C),
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            LinearProgressIndicator(
                                progress = { (wird.currentPage.toFloat() / 604f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = Color(0xFFE6B85C),
                                trackColor = Color.White.copy(alpha = 0.2f)
                            )

                            // العلامة المرجعية (Bookmark)
                            Surface(
                                color = Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Bookmark,
                                        contentDescription = null,
                                        tint = Color(0xFFE6B85C)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "علامة الوقف الحالية: ${wird.bookmarkSurahName} (الآية ${UmmAlQuraCalculator.toArabicDigits(wird.bookmarkAyahNumber.toString())})",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (wird.bookmarkNote.isNotBlank()) {
                                            Text(
                                                text = wird.bookmarkNote,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color(0xFFE5DEC9)
                                            )
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        onLogWirdReading(
                                            wird.dailyTargetPages,
                                            wird.bookmarkSurahName,
                                            wird.bookmarkAyahNumber + 10,
                                            "تم إتمام الورد اليومي"
                                        )
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("log_daily_wird_quick_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("أتممت ورد اليوم (+${UmmAlQuraCalculator.toArabicDigits(wird.dailyTargetPages.toString())} ص)")
                                }

                                FilledTonalButton(
                                    onClick = { showWirdDialog = true },
                                    modifier = Modifier.testTag("open_wird_bookmark_dialog_btn")
                                ) {
                                    Text("تحديث الوقف")
                                }
                            }
                        }
                    }
                }
            }

            // ٢. بطاقة الأذكار اليومية (فهرس حصن المسلم الموثق)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "الأذكار اليومية (فهرس حصن المسلم)",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "مرجع موثق حسب أبواب كتاب «حصن المسلم» — تتسجل كعادة تلقائيًا فور إتمام العدّاد.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        uiState.todayAzkarLogs.forEach { azkar ->
                            AzkarSectionCard(
                                azkar = azkar,
                                onStepCount = { onProgressAzkar(azkar, false) },
                                onCompleteAll = { onProgressAzkar(azkar, true) }
                            )
                        }
                    }
                }
            }
        }

        // ٣. بطاقة المحاضرات الجامعية (إدخال يدوي + استيراد .ics + تنبيه مسبق)
        if (selectedSection == 0 || selectedSection == 2) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.School,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "جدول المحاضرات والتنبيه المسبق",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "إدخال يدوي أو استيراد تقويم جامعي (.ics) مع إشعار قبل كل محاضرة",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { showAddLectureDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("add_lecture_manual_btn")
                            ) {
                                Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إضافة محاضرة")
                            }

                            FilledTonalButton(
                                onClick = { showIcsImportDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("import_ics_dialog_btn")
                            ) {
                                Icon(imageVector = Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("استيراد ملف .ics")
                            }
                        }

                        uiState.lectures.forEach { lecture ->
                            LectureItemRow(
                                lecture = lecture,
                                onTestReminder = { onTestLectureReminder(lecture) },
                                onDelete = { onDeleteLecture(lecture.id) }
                            )
                        }
                    }
                }
            }
        }

        // ٤. بطاقة الصحة والنشاط (Health Connect + الخطوات + السعرات والنوم)
        if (selectedSection == 0 || selectedSection == 3) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "الصحة والنشاط (Health Connect)",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "الخطوات مربوطة تلقائيًا بعادة المشي + حساب السعرات المحروقة والمستهلكة والنوم",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // شريط تقدم الخطوات
                        val stepsProgress = (health.stepsCount.toFloat() / health.stepsGoal.coerceAtLeast(1)).coerceIn(0f, 1f)
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.DirectionsWalk,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "خطوات اليوم: ${UmmAlQuraCalculator.toArabicDigits(health.stepsCount.toString())} / ${UmmAlQuraCalculator.toArabicDigits(health.stepsGoal.toString())}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "${UmmAlQuraCalculator.toArabicDigits((stepsProgress * 100).toInt().toString())}٪",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                LinearProgressIndicator(
                                    progress = { stepsProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(50))
                                )

                                if (hardwareSensorSteps != null) {
                                    Text(
                                        text = "حساس الخطوات في جوالك متصل الآن: قراءة الحساس (${UmmAlQuraCalculator.toArabicDigits(hardwareSensorSteps.toString())} خطوة)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        // بطاقات السعرات المحروقة، السعرات المستهلكة، وساعات النوم
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HealthMetricTile(
                                title = "سعرات محروقة",
                                value = "${UmmAlQuraCalculator.toArabicDigits(health.burnedCalories.toString())} سعرة",
                                icon = Icons.Filled.LocalFireDepartment,
                                modifier = Modifier.weight(1f)
                            )
                            HealthMetricTile(
                                title = "سعرات الأكل",
                                value = "${UmmAlQuraCalculator.toArabicDigits(health.consumedCalories.toString())} / ${UmmAlQuraCalculator.toArabicDigits(health.dailyCalorieLimit.toString())}",
                                icon = Icons.Filled.Restaurant,
                                modifier = Modifier.weight(1f)
                            )
                            HealthMetricTile(
                                title = "نوم الليلة",
                                value = "${UmmAlQuraCalculator.toArabicDigits(health.sleepHours.toString())} س",
                                icon = Icons.Filled.Bedtime,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // سجل الوجبات اليومي
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "سجل الوجبات والسعرات المستهلكة اليوم:",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = health.mealsLogJson.ifBlank { "لم يتم تسجيل وجبات بعد." },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onSyncHealthData(
                                        health.stepsCount + 1000,
                                        health.burnedCalories + 55,
                                        health.sleepHours
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("quick_add_steps_sync_btn")
                            ) {
                                Text("+١٠٠٠ خطوة (مزامنة)")
                            }

                            FilledTonalButton(
                                onClick = { showAddMealDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("add_meal_calorie_btn")
                            ) {
                                Text("+ وجبة / سعرات")
                            }

                            OutlinedButton(
                                onClick = { showHealthSyncDialog = true },
                                modifier = Modifier.testTag("edit_health_metrics_btn")
                            ) {
                                Text("تعديل")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showWirdDialog) {
        UpdateWirdBookmarkDialog(
            currentWird = wird,
            onDismiss = { showWirdDialog = false },
            onSave = { pagesRead, surah, ayah, note ->
                onLogWirdReading(pagesRead, surah, ayah, note)
                showWirdDialog = false
            }
        )
    }

    if (showAddLectureDialog) {
        AddLectureDialog(
            onDismiss = { showAddLectureDialog = false },
            onAdd = { title, code, loc, day, start, end, rem ->
                onAddLecture(title, code, loc, day, start, end, rem)
                showAddLectureDialog = false
            }
        )
    }

    if (showIcsImportDialog) {
        ImportIcsLecturesDialog(
            onDismiss = { showIcsImportDialog = false },
            onImportSample = {
                onImportSampleIcs()
                showIcsImportDialog = false
            },
            onImportCustom = { content ->
                onImportCustomIcs(content)
                showIcsImportDialog = false
            }
        )
    }

    if (showAddMealDialog) {
        AddMealCalorieDialog(
            onDismiss = { showAddMealDialog = false },
            onAdd = { desc, cal ->
                onAddMealCalorie(desc, cal)
                showAddMealDialog = false
            }
        )
    }

    if (showHealthSyncDialog) {
        EditHealthSnapshotDialog(
            currentSteps = health.stepsCount,
            currentBurned = health.burnedCalories,
            currentSleep = health.sleepHours,
            onDismiss = { showHealthSyncDialog = false },
            onSave = { steps, burned, sleep ->
                onSyncHealthData(steps, burned, sleep)
                showHealthSyncDialog = false
            }
        )
    }
}

@Composable
private fun AzkarSectionCard(
    azkar: AzkarLogEntity,
    onStepCount: () -> Unit,
    onCompleteAll: () -> Unit
) {
    val progress = (azkar.currentCount.toFloat() / azkar.targetCount.coerceAtLeast(1)).coerceIn(0f, 1f)
    Surface(
        color = if (azkar.isCompleted) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        },
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onStepCount() }
            .testTag("azkar_card_${azkar.categoryCode}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = azkar.categoryTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "العدّاد: ${UmmAlQuraCalculator.toArabicDigits(azkar.currentCount.toString())} من ${UmmAlQuraCalculator.toArabicDigits(azkar.targetCount.toString())}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (azkar.isCompleted) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "مكتمل ✅",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledTonalButton(
                            onClick = onStepCount,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.TouchApp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تسبيح +")
                        }
                        Button(
                            onClick = onCompleteAll,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("azkar_complete_btn_${azkar.categoryCode}")
                        ) {
                            Text("إتمام")
                        }
                    }
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
            )
        }
    }
}

@Composable
private fun LectureItemRow(
    lecture: LectureEntity,
    onTestReminder: () -> Unit,
    onDelete: () -> Unit
) {
    val dayName = when (lecture.dayOfWeek) {
        1 -> "الأحد"
        2 -> "الإثنين"
        3 -> "الثلاثاء"
        4 -> "الأربعاء"
        5 -> "الخميس"
        else -> "الأحد"
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "$dayName • ${lecture.startTime24} - ${lecture.endTime24}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    if (lecture.isImportedFromIcs) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "مستورد .ics",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "${lecture.title} (${lecture.courseCode})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "📍 ${lecture.location} • 🔔 تنبيه قبلها بـ ${UmmAlQuraCalculator.toArabicDigits(lecture.reminderMinutesBefore.toString())} د",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onTestReminder,
                    modifier = Modifier.testTag("test_lecture_reminder_${lecture.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = "تجربة إشعار المحاضرة",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "حذف المحاضرة",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthMetricTile(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun UpdateWirdBookmarkDialog(
    currentWird: com.example.data.WirdProgressEntity,
    onDismiss: () -> Unit,
    onSave: (pagesRead: Int, surah: String, ayah: Int, note: String) -> Unit
) {
    var pagesReadText by remember { mutableStateOf(currentWird.dailyTargetPages.toString()) }
    var surahText by remember { mutableStateOf(currentWird.bookmarkSurahName) }
    var ayahText by remember { mutableStateOf(currentWird.bookmarkAyahNumber.toString()) }
    var noteText by remember { mutableStateOf(currentWird.bookmarkNote) }

    val verifiedSurahs = listOf(
        "سورة البقرة",
        "سورة آل عمران",
        "سورة النساء",
        "سورة المائدة",
        "سورة الكهف",
        "سورة يس",
        "سورة الملك"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسجيل قراءة الورد وتحديث علامة الوقف") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "الصفحة الحالية في مصحف المدينة: ${currentWird.currentPage} من 604 (الجزء ${currentWird.currentJuz})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedTextField(
                    value = pagesReadText,
                    onValueChange = { pagesReadText = it },
                    label = { Text("عدد الصفحات المقروءة الآن") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    verifiedSurahs.forEach { s ->
                        FilterChip(
                            selected = surahText == s,
                            onClick = { surahText = s },
                            label = { Text(s) }
                        )
                    }
                }
                OutlinedTextField(
                    value = surahText,
                    onValueChange = { surahText = it },
                    label = { Text("اسم السورة (علامة الوقف)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ayahText,
                    onValueChange = { ayahText = it },
                    label = { Text("رقم الآية التي وقفت عندها") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("ملاحظة الوقف أو التدبر") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val pages = pagesReadText.toIntOrNull()?.coerceIn(1, 100) ?: 2
                    val ayah = ayahText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    onSave(pages, surahText, ayah, noteText)
                }
            ) {
                Text("حفظ وتحديث العادة")
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
private fun AddLectureDialog(
    onDismiss: () -> Unit,
    onAdd: (title: String, code: String, location: String, day: Int, start: String, end: String, reminder: Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("CS-301") }
    var location by remember { mutableStateOf("مبنى الحاسب - قاعة 201") }
    var selectedDay by remember { mutableIntStateOf(1) }
    var startTime by remember { mutableStateOf("09:00") }
    var endTime by remember { mutableStateOf("10:15") }
    var reminderMins by remember { mutableStateOf("15") }

    val days = listOf(1 to "الأحد", 2 to "الإثنين", 3 to "الثلاثاء", 4 to "الأربعاء", 5 to "الخميس")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة محاضرة جامعية جديدة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("اسم المادة / المحاضرة") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("رمز المقرر") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = reminderMins,
                        onValueChange = { reminderMins = it },
                        label = { Text("التنبيه قبلها (د)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("القاعة / المبنى") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    days.forEach { (d, label) ->
                        FilterChip(
                            selected = selectedDay == d,
                            onClick = { selectedDay = d },
                            label = { Text(label) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("وقت البدء (24h)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("وقت الانتهاء") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onAdd(
                            title,
                            code,
                            location,
                            selectedDay,
                            startTime,
                            endTime,
                            reminderMins.toIntOrNull()?.coerceIn(5, 120) ?: 15
                        )
                    }
                }
            ) {
                Text("حفظ المحاضرة")
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
private fun ImportIcsLecturesDialog(
    onDismiss: () -> Unit,
    onImportSample: () -> Unit,
    onImportCustom: (String) -> Unit
) {
    var icsText by remember { mutableStateOf(IcsCalendarParser.sampleUniversityIcs) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("استيراد جدول المحاضرات من ملف تقويم (.ics)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "يمكنك لصق محتوى ملف التقويم الجامعي (iCalendar .ics) أو تجربة النموذج الجامعي الجاهز بالأسفل:",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = icsText,
                    onValueChange = { icsText = it },
                    label = { Text("محتوى ملف .ics (VEVENT)") },
                    minLines = 6,
                    maxLines = 9,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onImportCustom(icsText) },
                modifier = Modifier.testTag("confirm_import_ics_btn")
            ) {
                Text("تحليل واستيراد المحاضرات")
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
private fun AddMealCalorieDialog(
    onDismiss: () -> Unit,
    onAdd: (desc: String, cal: Int) -> Unit
) {
    var desc by remember { mutableStateOf("") }
    var calText by remember { mutableStateOf("350") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تسجيل وجبة أو مشروب في حاسبة السعرات") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("وصف الوجبة (مثال: عشاء بيض مسلوق وخبز بر)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = calText,
                    onValueChange = { calText = it },
                    label = { Text("السعرات الحرارية التقديرية (kcal)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cal = calText.toIntOrNull()?.coerceIn(10, 3000) ?: 250
                    onAdd(desc.ifBlank { "وجبة خفيفة" }, cal)
                }
            ) {
                Text("إضافة للسجل")
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
private fun EditHealthSnapshotDialog(
    currentSteps: Int,
    currentBurned: Int,
    currentSleep: Float,
    onDismiss: () -> Unit,
    onSave: (steps: Int, burned: Int, sleep: Float) -> Unit
) {
    var stepsText by remember { mutableStateOf(currentSteps.toString()) }
    var burnedText by remember { mutableStateOf(currentBurned.toString()) }
    var sleepText by remember { mutableStateOf(currentSleep.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("مزامنة وتحديث قراءات Health Connect") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = stepsText,
                    onValueChange = { stepsText = it },
                    label = { Text("إجمالي الخطوات اليوم (يرتبط بعادة المشي)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = burnedText,
                    onValueChange = { burnedText = it },
                    label = { Text("السعرات المحروقة النشطة (kcal)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = sleepText,
                    onValueChange = { sleepText = it },
                    label = { Text("ساعات النوم الليلة الماضية") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s = stepsText.toIntOrNull()?.coerceAtLeast(0) ?: currentSteps
                    val b = burnedText.toIntOrNull()?.coerceAtLeast(0) ?: currentBurned
                    val sl = sleepText.toFloatOrNull()?.coerceIn(0f, 16f) ?: currentSleep
                    onSave(s, b, sl)
                }
            ) {
                Text("حفظ ومزامنة العادة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
