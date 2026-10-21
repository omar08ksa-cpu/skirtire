package com.example.features.prayer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.core.prayer.CityLocation
import com.example.core.prayer.UmmAlQuraCalculator
import com.example.ui.theme.EmeraldDeep

/**
 * شاشة مواقيت الصلاة في المدن:
 * تفتح بقائمة فارغة لتمكين المستخدم من إضافة المدن والمناطق التي يرغب بمتابعتها بنفسه.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerCitiesScreen(
    currentCityId: String,
    savedCityIds: Set<String> = emptySet(),
    onUpdateSavedCities: (Set<String>) -> Unit = {},
    onSelectCity: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val initialCityIds = remember(savedCityIds, currentCityId) {
        if (savedCityIds.isNotEmpty()) {
            savedCityIds
        } else {
            val prefs = context.getSharedPreferences("prayer_cities_prefs", Context.MODE_PRIVATE)
            val csv = prefs.getString("saved_city_ids", null).orEmpty()
            if (csv.isNotBlank()) {
                csv.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
            } else {
                setOf(currentCityId)
            }
        }
    }
    var userAddedCityIds by rememberSaveable { mutableStateOf(initialCityIds) }
    var showAddCityDialog by remember { mutableStateOf(false) }

    val persistCities: (Set<String>) -> Unit = { updated ->
        userAddedCityIds = updated
        val prefs = context.getSharedPreferences("prayer_cities_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("saved_city_ids", updated.joinToString(",")).commit()
        onUpdateSavedCities(updated)
    }

    LaunchedEffect(savedCityIds) {
        if (savedCityIds.isNotEmpty()) {
            userAddedCityIds = savedCityIds
        }
    }

    val allAvailableCities = remember {
        UmmAlQuraCalculator.saudiCities + UmmAlQuraCalculator.worldCities
    }

    val addedCities = remember(userAddedCityIds) {
        allAvailableCities.filter { userAddedCityIds.contains(it.id) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "مواقيت الصلاة في المدن",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("prayer_cities_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { showAddCityDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("add_city_top_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة مدينة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("prayer_cities_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (addedCities.isEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = EmeraldDeep.copy(alpha = 0.12f),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AddLocationAlt,
                                        contentDescription = null,
                                        tint = EmeraldDeep,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Text(
                                text = "لا توجد مدن مضافة",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "القائمة فارغة. اضغط «إضافة مدينة» للبحث واختيار المدن والمناطق التي تود متابعة مواقيتها وحفظها في قائمتك الشخصية.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                lineHeight = 20.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { showAddCityDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("إضافة مدينة الآن", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(addedCities, key = { it.id }) { city ->
                    val citySchedule = remember(city) {
                        UmmAlQuraCalculator.calculateScheduleForNow(city)
                    }
                    val isCurrentSelected = city.id == currentCityId

                    CityPrayerCard(
                        city = city,
                        schedule = citySchedule,
                        isSelected = isCurrentSelected,
                        onSelect = { onSelectCity(city.id) },
                        onRemove = {
                            val updated = userAddedCityIds - city.id
                            persistCities(updated)
                        }
                    )
                }
            }
        }
    }

    if (showAddCityDialog) {
        AddCityPickerDialog(
            allCities = allAvailableCities,
            alreadyAddedIds = userAddedCityIds,
            onAddCity = { cityId ->
                val updated = userAddedCityIds + cityId
                persistCities(updated)
            },
            onRemoveCity = { cityId ->
                val updated = userAddedCityIds - cityId
                persistCities(updated)
            },
            onDismiss = { showAddCityDialog = false }
        )
    }
}

@Composable
fun CityPrayerCard(
    city: CityLocation,
    schedule: com.example.core.prayer.DailyPrayerSchedule,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .testTag("city_card_${city.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) EmeraldDeep.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface
        ),
        border = if (isSelected) CardDefaults.outlinedCardBorder() else null
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) EmeraldDeep else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.LocationCity,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = city.arabicName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = EmeraldDeep
                                ) {
                                    Text(
                                        text = "المدينة المختارة",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = city.regionName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "القادمة: ${schedule.nextPrayer.type.arabicName}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldDeep
                        )
                        Text(
                            text = schedule.countdownFormatted,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onRemove) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "حذف المدينة",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // شبكة مواقيت الصلوات الخمس في هذه المدينة
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                schedule.prayers.filter { it.type.isObligatory }.forEach { slot ->
                    val isNext = slot.type == schedule.nextPrayer.type
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = slot.type.arabicName,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isNext) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isNext) EmeraldDeep else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = slot.formatted12h.substringBefore(" "),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isNext) EmeraldDeep else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddCityPickerDialog(
    allCities: List<CityLocation>,
    alreadyAddedIds: Set<String>,
    onAddCity: (String) -> Unit,
    onRemoveCity: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0=المملكة، 1=العالم الإسلامي

    val categoryCities = remember(selectedTab) {
        if (selectedTab == 0) UmmAlQuraCalculator.saudiCities else UmmAlQuraCalculator.worldCities
    }

    val filteredList = remember(categoryCities, searchQuery) {
        if (searchQuery.isBlank()) {
            categoryCities
        } else {
            categoryCities.filter {
                it.arabicName.contains(searchQuery.trim(), ignoreCase = true) ||
                    it.regionName.contains(searchQuery.trim(), ignoreCase = true)
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "إضافة مدينة لمواقيت الصلاة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }

                // تبويبات
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("مدن المملكة", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    SegmentedButton(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("عواصم إسلامية", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                // حقل البحث
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث عن مدينة أو منطقة...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredList, key = { it.id }) { city ->
                        val isAdded = alreadyAddedIds.contains(city.id)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isAdded) EmeraldDeep.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isAdded) onRemoveCity(city.id) else onAddCity(city.id)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(city.arabicName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(city.regionName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (isAdded) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("مضافة", fontSize = 11.sp, color = EmeraldDeep, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldDeep, modifier = Modifier.size(20.dp))
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { onAddCity(city.id) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("إضافة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeep)
                ) {
                    Text("تم", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
