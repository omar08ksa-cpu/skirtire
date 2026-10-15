package com.example.features.prayer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.prayer.CityLocation
import com.example.core.prayer.PrayerType
import com.example.core.prayer.UmmAlQuraCalculator
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.GoldSoft
import com.example.ui.theme.MintAccent

/**
 * شاشة مواقيت الصلاة في مختلف المدن:
 * تمكن المستخدم من استعراض مواقيت الصلاة في مدن المملكة والعواصم الإسلامية واختيار المدينة الرئيسية.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrayerCitiesScreen(
    currentCityId: String,
    onSelectCity: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0=مدن المملكة، 1=العالم الإسلامي

    val citiesSource = if (selectedTab == 0) UmmAlQuraCalculator.saudiCities else UmmAlQuraCalculator.worldCities
    val filteredCities = remember(citiesSource, searchQuery) {
        if (searchQuery.isBlank()) {
            citiesSource
        } else {
            citiesSource.filter {
                it.arabicName.contains(searchQuery.trim(), ignoreCase = true) ||
                    it.regionName.contains(searchQuery.trim(), ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "مواقيت الصلاة في المدن",
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleLarge
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // شريط البحث
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("ابحث عن مدينة أو منطقة...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("prayer_cities_search_input")
                )
            }

            // تبويبات التصنيف
            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("مدن المملكة", fontWeight = FontWeight.Bold)
                    }
                    SegmentedButton(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("عواصم إسلامية وعالمية", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // بطاقات المدن مع مواقيت كل مدينة
            items(filteredCities, key = { it.id }) { city ->
                val citySchedule = remember(city) {
                    UmmAlQuraCalculator.calculateScheduleForNow(city)
                }
                val isCurrentSelected = city.id == currentCityId

                CityPrayerCard(
                    city = city,
                    schedule = citySchedule,
                    isSelected = isCurrentSelected,
                    onSelect = { onSelectCity(city.id) }
                )
            }
        }
    }
}

@Composable
fun CityPrayerCard(
    city: CityLocation,
    schedule: com.example.core.prayer.DailyPrayerSchedule,
    isSelected: Boolean,
    onSelect: () -> Unit
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
